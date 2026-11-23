package com.mechanical.people;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mechanical.Bronze;
import com.mechanical.Butchery;
import com.mechanical.CampAge;
import com.mechanical.CarcassEntity;
import com.mechanical.CartEntity;
import com.mechanical.Carting;
import com.mechanical.Coins;
import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;
import com.mechanical.WagonEntity;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.desert.DesertFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Trader Caravans replace vanilla's Wandering Trader and its llamas (docs/traders-plan.md). Vanilla's spawner keeps its own
 * timing (a try every 20 min, 25-75 % chance, near a random player); the trader it makes is cancelled on joining the world
 * and a caravan of a trading tribe comes in its place, at the edge of a player camp nearby if there is one. Each caravan is
 * of one kind ({@link TraderKinds}: wood, mining, jewelry, hunting, herbs, foraging ...) and is 4-5 people dressed for it:
 * an old {@link TribeRole#TRADER} (the leader, staff and coin purse), a {@link TribeRole#HANDLER} holding the draught animals'
 * leads, and 2-3 {@link TribeRole#GUARD}s in metal armour. A small caravan has one horse (camels for desert kinds) with a
 * covered cart; one in {@link #BIG_ODDS} is big: a Wagon with a pair, rare goods, dearer. The bed carries the kind's
 * cargo (TravoisEntity#cargo). Once there they keep still (CaravanGoal): the handler and the animals stand, the leader
 * potters round them, the guards walk a wide ring. Harming anyone of them, an animal or a cart angers them all. The
 * leader trades through his own screen (CaravanTrade) and they move on after {@link #STAY}. Talk: TribeVisits.openDialog.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Caravans {
    /** Vanilla's wandering trader stays 48000 ticks (2 days). */
    static final long STAY = 48000;
    public static final TagKey<Item> GEMSTONES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "gemstones"));
    /** One caravan in this many is big (a Wagon); its goods cost {@link #BIG_PRICE} times as much, its purse and stock are bigger. */
    static final int BIG_ODDS = 4;
    static final double BIG_PRICE = 1.25, BIG_PURSE = 2.5, BIG_STOCK = 1.5;
    /** Goods for sale: a small caravan's, a big one's (its rare goods first). */
    static final int SALES = 6, BIG_SALES = 9;
    /** A player camp this near the spot pulls the caravan to its edge, this far from the fire. */
    static final int CAMP_SEARCH = 80, CAMP_EDGE = 16;

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e.getType() != EntityTypes.WANDERING_TRADER && e.getType() != EntityTypes.TRADER_LLAMA) return;
        event.setCanceled(true); // never in the world, also not from old saves
        if (e.getType() == EntityTypes.WANDERING_TRADER && !event.loadedFromDisk() && event.getLevel() instanceof ServerLevel level) spawn(level, e.blockPosition());
    }

    /** A caravan of a random kind and size near {@code at}: returns the leader (null if none could be made). */
    static Tribesperson spawn(ServerLevel level, BlockPos at) {
        List<Identifier> kinds = List.copyOf(TraderKinds.all().keySet());
        if (kinds.isEmpty()) return null;
        RandomSource r = level.getRandom();
        return spawn(level, atCampEdge(level, at), kinds.get(r.nextInt(kinds.size())), r.nextInt(BIG_ODDS) == 0);
    }

    /** {@code at}, or the edge of the nearest player camp within {@link #CAMP_SEARCH}, on the side towards {@code at}. */
    static BlockPos atCampEdge(ServerLevel level, BlockPos at) {
        CampAge.Camp best = null;
        double bestDist = CAMP_SEARCH * CAMP_SEARCH;
        for (CampAge.Camp c : CampAge.get(level).camps()) {
            double d = c.centre.distSqr(at);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        if (best == null) return at;
        Vec3 dir = Vec3.atLowerCornerOf(at.subtract(best.centre)).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1 ? new Vec3(1, 0, 0) : dir.normalize();
        return best.centre.offset((int) Math.round(dir.x * CAMP_EDGE), 0, (int) Math.round(dir.z * CAMP_EDGE));
    }

    /** A caravan of {@code kind} at {@code at} (big: a Wagon and a pair): returns the leader (null if it couldn't be made). */
    static Tribesperson spawn(ServerLevel level, BlockPos at, Identifier kindId, boolean big) {
        TraderKinds.Kind kind = TraderKinds.get(kindId);
        if (kind == null) return null;
        RandomSource r = level.getRandom();
        UUID tribe;
        do tribe = new UUID(r.nextLong(), r.nextLong()); while (Tribe.of(tribe).temperament != Tribe.Temperament.TRADERS);
        int age = com.mechanical.WorldAge.npcAge(level.getServer(), tribe);
        long leaveAt = level.getGameTime() + STAY;
        BlockPos camp = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        int guards = 2 + r.nextInt(2);
        Tribesperson leader = null, handler = null;
        for (int i = 0; i < 2 + guards; i++) {
            TribeRole role = i == 0 ? TribeRole.TRADER : i == 1 ? TribeRole.HANDLER : TribeRole.GUARD;
            BlockPos p = i == 1 ? camp : camp.offset(r.nextInt(7) - 3, 0, r.nextInt(5) - 6);
            Tribesperson t = TribeVisits.spawn(level, p, role, null, tribe, i == 0);
            if (t == null) continue;
            t.leaveAt = leaveAt;
            t.caravanCamp = camp;
            t.setCaravanKind(kindId);
            if (role == TribeRole.TRADER) {
                leader = t;
                t.stock = stock(kindId, kind, age, big, r);
                t.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Coins.COIN_PURSE.get()));
            } else if (role == TribeRole.HANDLER) handler = t;
            else arm(t, Math.min(age + r.nextInt(2), 1), r);
        }
        if (leader == null || handler == null) {
            if (leader != null) leave(leader);
            return null;
        }
        // the team stands beside the handler, his hand at the lead animal's head; the bed behind them
        float yaw = r.nextFloat() * 360;
        Vec3 ahead = Vec3.directionFromRotation(0, yaw);
        Vec3 front = Vec3.atBottomCenterOf(camp).add(Vec3.directionFromRotation(0, yaw + 90).scale(1.6)).add(ahead.scale(-0.6));
        if (big) {
            Vec3 side = Vec3.directionFromRotation(0, yaw + 90).scale(WagonEntity.PAIR);
            AbstractHorse lead = animal(level, kind, front, yaw, handler), partner = animal(level, kind, front.add(side), yaw, handler);
            WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
            if (lead != null && partner != null && wagon != null) {
                wagon.setPos(front.add(side.scale(0.5)).add(ahead.scale(-WagonEntity.LEAD)));
                wagon.setYRot(yaw);
                wagon.hitchCaravan(lead, partner);
                wagon.setCargo(kind.cargo());
                level.addFreshEntity(wagon);
            }
        } else {
            AbstractHorse horse = animal(level, kind, front, yaw, handler);
            CartEntity cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
            if (horse != null && cart != null) {
                boolean carcass = kindId.getPath().equals("hunting") && r.nextInt(2) == 0;
                cart.setPos(horse.position().add(ahead.scale(-CartEntity.LEAD_HORSE)));
                cart.setYRot(yaw);
                cart.hitchCaravan(horse, false);
                if (!carcass) cart.setCargo(kind.cargo());
                level.addFreshEntity(cart);
                if (carcass) trophy(level, cart, leader, r);
            }
        }
        return leader;
    }

    /** A tame horse (or camel) for the caravan at {@code at}, its lead held by {@code handler}. */
    private static AbstractHorse animal(ServerLevel level, TraderKinds.Kind kind, Vec3 at, float yaw, Tribesperson handler) {
        AbstractHorse a = (kind.camels() ? DesertFauna.TAME_CAMEL.get() : Wildlife.TAME_HORSE.get()).create(level, EntitySpawnReason.EVENT);
        if (a == null) return null;
        BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
        a.snapTo(at.x, p.getY(), at.z, yaw, 0);
        a.setYHeadRot(yaw);
        a.setYBodyRot(yaw);
        a.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.EVENT, null);
        a.setTamed(true);
        a.setPersistenceRequired();
        level.addFreshEntity(a);
        a.setLeashedTo(handler, true);
        return a;
    }

    /**
     * The leader's goods for players of {@code age}: of the kind's sells up to one age ahead, {@link #SALES} at random (a big
     * caravan: its rare goods too, {@link #BIG_SALES} in all, dearer); everything it buys (a tag: each item in it); its purse.
     */
    static CaravanTrade.Stock stock(Identifier id, TraderKinds.Kind kind, int age, boolean big, RandomSource r) {
        List<CaravanTrade.Deal> deals = new ArrayList<>();
        List<TraderKinds.Good> rare = new ArrayList<>(), plain = new ArrayList<>();
        for (TraderKinds.Good g : kind.sells()) if (g.age() <= age + 1) (g.rare() ? rare : plain).add(g);
        List<TraderKinds.Good> pick = new ArrayList<>(big ? rare : List.of());
        while (pick.size() < (big ? BIG_SALES : SALES) && !plain.isEmpty()) pick.add(plain.remove(r.nextInt(plain.size())));
        for (TraderKinds.Good g : pick) {
            ItemStack goods = stack(g.item(), g.count());
            if (!goods.isEmpty()) deals.add(new CaravanTrade.Deal(goods, big ? (int) Math.ceil(g.price() * BIG_PRICE) : g.price(), stock(g, big), true));
        }
        for (TraderKinds.Good g : kind.buys()) {
            if (g.age() > age + 1) continue;
            if (g.item().startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(g.item().substring(1)));
                for (var item : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) deals.add(new CaravanTrade.Deal(new ItemStack(item.value(), g.count()), g.price(), stock(g, big), false));
            } else {
                ItemStack goods = stack(g.item(), g.count());
                if (!goods.isEmpty()) deals.add(new CaravanTrade.Deal(goods, g.price(), stock(g, big), false));
            }
        }
        int lo = kind.purse().get(0), hi = Math.max(lo, kind.purse().get(1));
        int purse = lo + r.nextInt(hi - lo + 1);
        return new CaravanTrade.Stock(id, big, big ? (int) (purse * BIG_PURSE) : purse, deals);
    }

    private static int stock(TraderKinds.Good g, boolean big) {
        return big ? (int) Math.ceil(g.stock() * BIG_STOCK) : g.stock();
    }

    /** {@code count} of the item with this id; empty for an unknown id (a datapack typo must not break the caravan). */
    private static ItemStack stack(String id, int count) {
        Identifier rl = Identifier.tryParse(id);
        return rl == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(rl).map(i -> new ItemStack(i, count)).orElse(ItemStack.EMPTY);
    }

    /** Big game a caravan brings in as trade goods. */
    static final List<Supplier<? extends EntityType<? extends LivingEntity>>> TROPHIES = List.of(Wildlife.CAVE_LION, Wildlife.AUROCHS, Wildlife.BROWN_BEAR);

    /**
     * A trophy from the road on {@code cart}: dressed and lashed, the caravan's property (CarcassEntity#isCaravanProperty),
     * and the leader sells what it yields ({@link #goodsOf}). Returns it (null if it couldn't be made).
     */
    static CarcassEntity trophy(ServerLevel level, CartEntity cart, Tribesperson leader, RandomSource r) {
        LivingEntity animal = TROPHIES.get(r.nextInt(TROPHIES.size())).get().create(level, EntitySpawnReason.EVENT);
        if (animal == null) return null;
        animal.setPos(cart.position());
        CarcassEntity body = CarcassEntity.of(level, animal);
        body.setPrepared();
        level.addFreshEntity(body);
        body.startRiding(cart, true, true);
        if (leader.stock != null) leader.stock.deals.addAll(0, goodsOf(body.animal()));
        return body;
    }

    /** Copper a piece of a trophy's products is worth; other pieces 1, meat 2 scraps a copper. */
    private static int unitPrice(Item item) {
        return item == Wildlife.RAW_BEAR_PELT.get() ? 16
                : item == Mechanical.RAW_HIDE.get() || item == Wildlife.HORN.get() ? 3 : item == Wildlife.BEAR_CLAW.get() ? 2 : 1;
    }

    /** One deal per product of {@code animal} (Butchery: the meat, then each extra at its most), each sold once. */
    static List<CaravanTrade.Deal> goodsOf(EntityType<?> animal) {
        Butchery.Yield y = Butchery.yieldFor(animal);
        Map<Item, Integer> goods = new LinkedHashMap<>();
        goods.put(y.meat().get(), y.maxMeat());
        for (Butchery.Extra e : y.outer()) goods.merge(e.item().get(), e.max(), Integer::sum);
        for (Butchery.Extra e : y.inner()) goods.merge(e.item().get(), e.max(), Integer::sum);
        return goods.entrySet().stream().map(g -> new CaravanTrade.Deal(new ItemStack(g.getKey(), g.getValue()),
                Math.max(2, g.getKey() == y.meat().get() ? g.getValue() / 2 : g.getValue() * unitPrice(g.getKey())), 1, true)).toList();
    }

    /** Armour points and health of a guard: metal armour painted on the skin (no armour items, never leather). */
    static final double GUARD_ARMOR = 12, GUARD_HEALTH = 30;

    /** A guard's weapon for {@code tier} (0 Stone, 1 Bronze; no iron weapons exist yet), and the toughness of their armour. Never a helmet. */
    static void arm(Tribesperson t, int tier, RandomSource r) {
        Item[] weapons = tier == 0 ? new Item[] {Mechanical.FLINT_SPEAR.get(), Mechanical.CRUDE_BOW.get(), Mechanical.FLINT_AXE.get()}
                : new Item[] {Bronze.BRONZE_SPEAR.get(), Bronze.BRONZE_SWORD.get(), Bronze.BRONZE_AXE.get(), Mechanical.CRUDE_BOW.get()};
        t.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapons[r.nextInt(weapons.length)]));
        t.getAttribute(Attributes.ARMOR).setBaseValue(GUARD_ARMOR);
        t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(GUARD_HEALTH);
        t.setHealth((float) GUARD_HEALTH);
    }

    static boolean isCoin(ItemStack s) {
        return s.is(Coins.COPPER_COIN.get()) || s.is(Coins.SILVER_COIN.get()) || s.is(Coins.GOLD_COIN.get());
    }

    /** Coins for coins: no standing discount (it would mint money). */
    static boolean isExchange(net.minecraft.world.item.trading.MerchantOffer offer) {
        return isCoin(offer.getBaseCostA()) && isCoin(offer.getResult());
    }

    static boolean isGem(ItemStack s) {
        return s.is(GEMSTONES) || s.is(Items.DIAMOND) || s.is(Items.EMERALD) || s.is(Items.AMETHYST_SHARD);
    }

    /** The caravan member leading {@code e}: a draught animal's handler, or the handler of a cart's or wagon's team; else null. */
    static Tribesperson owner(Entity e) {
        if (e instanceof TravoisEntity t) e = t.harness();
        return e instanceof AbstractHorse h && h.getLeashHolder() instanceof Tribesperson p && p.role().caravan() ? p : null;
    }

    /** A player hit a caravan's animal, cart or wagon: the whole caravan is as angry as if one of them was hit. */
    public static void offended(Entity e, Player player) {
        Tribesperson owner = owner(e);
        if (owner != null && !player.isCreative()) owner.provoked(player);
    }

    @SubscribeEvent
    static void onAnimalHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof AbstractHorse && !event.getEntity().level().isClientSide()
                && event.getSource().getEntity() instanceof Player player) offended(event.getEntity(), player);
    }

    /** A caravan's animals are theirs: no riding, feeding, cutting or taking their leads. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    static void onAnimalInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractHorse) || owner(event.getTarget()) == null) return;
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && !event.getLevel().isClientSide())
            event.getEntity().sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.caravan.animal"));
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
    }

    /** Server: a caravan's draught animals stand where the handler holds them, and now and then put their heads down to graze. */
    @SubscribeEvent
    static void graze(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide()
                || !(horse.getLeashHolder() instanceof Tribesperson h) || h.role() != TribeRole.HANDLER) return;
        horse.getNavigation().stop();
        horse.getMoveControl().setWait();
        horse.setZza(0);
        horse.setXxa(0);
        horse.setDeltaMovement(horse.getDeltaMovement().multiply(0, 1, 0));
        if (!horse.isEating() && horse.getRandom().nextInt(160) == 0) horse.setEating(true);
    }

    /** Time is up: the member goes, and with it the animals it leads and their carts, wagons and loads. */
    static void leave(Tribesperson t) {
        for (AbstractHorse h : t.level().getEntitiesOfClass(AbstractHorse.class, t.getBoundingBox().inflate(16), h -> h.getLeashHolder() == t)) {
            for (TravoisEntity c : t.level().getEntitiesOfClass(TravoisEntity.class, h.getBoundingBox().inflate(8),
                    c -> c.harness() == h || c instanceof WagonEntity w && w.inTeam(h))) {
                c.getPassengers().forEach(Entity::discard);
                c.discard();
            }
            h.discard();
        }
        t.discard();
    }

    private Caravans() {}
}
