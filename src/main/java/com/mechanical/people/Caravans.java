package com.mechanical.people;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mechanical.Bronze;
import com.mechanical.BronzeOres;
import com.mechanical.Butchery;
import com.mechanical.CarcassEntity;
import com.mechanical.CartEntity;
import com.mechanical.Carting;
import com.mechanical.Casting;
import com.mechanical.Coins;
import com.mechanical.Ironworking;
import com.mechanical.Mechanical;
import com.mechanical.Spinning;
import com.mechanical.wildlife.TameHorse;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Trader Caravans replace vanilla's Wandering Trader and its llamas. Vanilla's spawner keeps its own timing (a try every
 * 20 min, 25-75 % chance, near a random player); the trader it makes is cancelled on joining the world and a caravan of a
 * trading tribe comes in its place: an old {@link TribeRole#TRADER} and 2-3 armoured {@link TribeRole#GUARD}s (no helmets,
 * no banners), and 1-2 tame horses on leads held by the trader, each pulling a covered cart with a chest (now and then a
 * trophy carcass instead, see {@link #trophy}) that stays hitched (CartEntity#hitchCaravan). They roam as a hunting band does (FollowBandLeaderGoal)
 * and move on after {@link #STAY} like vanilla's trader. Guards are armed for their tribe's age (WorldAge.npcAge: the
 * world's age, or one behind); the trader sells a mix up to one
 * age ahead for coins, changes coins and buys gemstones (#mechanical:gemstones, diamonds, emeralds, amethyst) for silver
 * and gold. Talk: TribeVisits.openDialog.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Caravans {
    /** Vanilla's wandering trader stays 48000 ticks (2 days). */
    static final long STAY = 48000;
    public static final TagKey<Item> GEMSTONES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "gemstones"));

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e.getType() != EntityTypes.WANDERING_TRADER && e.getType() != EntityTypes.TRADER_LLAMA) return;
        event.setCanceled(true); // never in the world, also not from old saves
        if (e.getType() == EntityTypes.WANDERING_TRADER && !event.loadedFromDisk() && event.getLevel() instanceof ServerLevel level) spawn(level, e.blockPosition());
    }

    /** A caravan at {@code at}: returns the trader (null if it couldn't be made). */
    static Tribesperson spawn(ServerLevel level, BlockPos at) {
        RandomSource r = level.getRandom();
        UUID tribe;
        do tribe = new UUID(r.nextLong(), r.nextLong()); while (Tribe.of(tribe).temperament != Tribe.Temperament.TRADERS);
        int age = com.mechanical.WorldAge.npcAge(level.getServer(), tribe);
        long leaveAt = level.getGameTime() + STAY;
        Tribesperson trader = null;
        int size = 3 + r.nextInt(2);
        for (int i = 0; i < size; i++) {
            Tribesperson t = TribeVisits.spawn(level, at.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2), i == 0 ? TribeRole.TRADER : TribeRole.GUARD, null, tribe, i == 0);
            if (t == null) continue;
            t.leaveAt = leaveAt;
            t.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            if (i == 0) {
                trader = t;
                t.offers = offers(age, r);
            } else arm(t, Math.min(age, 1), r);
        }
        if (trader == null) return null;
        int carts = 1 + r.nextInt(2);
        for (int c = 0; c < carts; c++) {
            BlockPos hp = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.offset(4 * c - 2, 0, 4));
            TameHorse horse = Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.EVENT);
            CartEntity cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
            if (horse == null || cart == null) continue;
            horse.setPos(hp.getX() + 0.5, hp.getY(), hp.getZ() + 0.5);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(hp), EntitySpawnReason.EVENT, null);
            horse.setTamed(true);
            horse.setPersistenceRequired();
            level.addFreshEntity(horse);
            horse.setLeashedTo(trader, true);
            boolean carcass = r.nextInt(4) == 0;
            cart.setPos(horse.position().add(Vec3.directionFromRotation(0, horse.getYRot()).scale(-CartEntity.LEAD_HORSE)));
            cart.hitchCaravan(horse, !carcass);
            level.addFreshEntity(cart);
            if (carcass) trophy(level, cart, trader, r);
        }
        return trader;
    }

    /** Big game a caravan brings in as trade goods. */
    static final List<Supplier<? extends EntityType<? extends LivingEntity>>> TROPHIES = List.of(Wildlife.CAVE_LION, Wildlife.AUROCHS, Wildlife.BROWN_BEAR);

    /**
     * A trophy from the road on {@code cart}: dressed and lashed, the caravan's property (CarcassEntity#isCaravanProperty),
     * and the trader sells what it yields ({@link #goodsOf}). Returns it (null if it couldn't be made).
     */
    static CarcassEntity trophy(ServerLevel level, CartEntity cart, Tribesperson trader, RandomSource r) {
        LivingEntity animal = TROPHIES.get(r.nextInt(TROPHIES.size())).get().create(level, EntitySpawnReason.EVENT);
        if (animal == null) return null;
        animal.setPos(cart.position());
        CarcassEntity body = CarcassEntity.of(level, animal);
        body.setPrepared();
        level.addFreshEntity(body);
        body.startRiding(cart, true, true);
        trader.offers.addAll(0, goodsOf(body.animal()));
        return body;
    }

    /** Copper a piece of a trophy's products is worth (on the scale of {@link #sales}); other pieces 1, meat 2 scraps a copper. */
    private static int unitPrice(Item item) {
        return item == Wildlife.RAW_BEAR_PELT.get() ? 16
                : item == Mechanical.RAW_HIDE.get() || item == Wildlife.HORN.get() ? 3 : item == Wildlife.BEAR_CLAW.get() ? 2 : 1;
    }

    /** One offer per product of {@code animal} (Butchery: the meat, then each extra at its most), each sold once for copper. */
    static List<MerchantOffer> goodsOf(EntityType<?> animal) {
        Butchery.Yield y = Butchery.yieldFor(animal);
        Map<Item, Integer> goods = new LinkedHashMap<>();
        goods.put(y.meat().get(), y.maxMeat());
        for (Butchery.Extra e : y.outer()) goods.merge(e.item().get(), e.max(), Integer::sum);
        for (Butchery.Extra e : y.inner()) goods.merge(e.item().get(), e.max(), Integer::sum);
        return goods.entrySet().stream().map(g -> new MerchantOffer(new ItemCost(Coins.COPPER_COIN.get(),
                Math.max(2, g.getKey() == y.meat().get() ? g.getValue() / 2 : g.getValue() * unitPrice(g.getKey()))), new ItemStack(g.getKey(), g.getValue()), 1, 0, 0)).toList();
    }

    /** The age of the players about: 0 Stone, 1 Bronze (made Bronze), 2 Iron (made Wrought Iron). */
    /**
     * A guard's gear for {@code tier} (0 Stone, 1 Bronze; no iron weapons or armour exist yet): a weapon, and hide armour in
     * the tribe's colour, Bronze Greaves from the Bronze Age. Never a helmet.
     */
    static void arm(Tribesperson t, int tier, RandomSource r) {
        Item[] weapons = tier == 0 ? new Item[] {Mechanical.FLINT_SPEAR.get(), Mechanical.CRUDE_BOW.get(), Mechanical.FLINT_AXE.get()}
                : new Item[] {Bronze.BRONZE_SPEAR.get(), Bronze.BRONZE_SWORD.get(), Bronze.BRONZE_AXE.get(), Mechanical.CRUDE_BOW.get()};
        t.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapons[r.nextInt(weapons.length)]));
        int colour = t.tribe().baseColour().getTextureDiffuseColor();
        t.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, colour));
        t.setItemSlot(EquipmentSlot.LEGS, tier == 0 ? dyed(Items.LEATHER_LEGGINGS, colour) : new ItemStack(Bronze.BRONZE_GREAVES.get()));
        t.setItemSlot(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, colour));
    }

    private static ItemStack dyed(Item item, int colour) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.DYED_COLOR, new DyedItemColor(colour));
        return s;
    }

    private record Sale(int tier, Supplier<ItemStack> goods, Supplier<Item> coin, int price) {}

    private static ItemStack of(Supplier<? extends net.minecraft.world.level.ItemLike> item, int n) {
        return new ItemStack(item.get(), n);
    }

    /** Everything a caravan may sell: tier 0 Stone, 1 Bronze, 2 Iron Age. Prices on the villagers' scale (a Bronze Sword 36 copper). */
    private static List<Sale> sales() {
        Supplier<Item> cu = Coins.COPPER_COIN, ag = Coins.SILVER_COIN, au = Coins.GOLD_COIN;
        return List.of(
                new Sale(0, () -> new ItemStack(Items.POTATO, 3), cu, 8),
                new Sale(0, () -> of(Mechanical.FLINT_KNIFE, 1), cu, 4),
                new Sale(0, () -> of(Mechanical.CRUDE_BOW, 1), cu, 8),
                new Sale(0, () -> of(Mechanical.BONE_ARROW, 8), cu, 6),
                new Sale(0, () -> new ItemStack(Items.LEATHER, 2), cu, 6),
                new Sale(0, () -> of(Mechanical.CORDAGE, 4), cu, 4),
                new Sale(0, () -> of(Wildlife.SINEW, 3), cu, 4),
                new Sale(0, () -> of(Mechanical.TUSK_DAGGER, 1), cu, 10),
                new Sale(0, () -> of(Wildlife.COOKED_VENISON_SCRAPS, 6), cu, 4),
                new Sale(0, () -> of(Mechanical.BONE_NEEDLE, 1), cu, 3),
                new Sale(1, () -> of(BronzeOres.MALACHITE, 3), cu, 6),
                new Sale(1, () -> of(BronzeOres.CASSITERITE, 2), cu, 8),
                new Sale(1, () -> of(Casting.COPPER_KNIFE, 1), cu, 16),
                new Sale(1, () -> of(Bronze.BRONZE_KNIFE, 1), cu, 24),
                new Sale(1, () -> of(Bronze.BRONZE_ARROW, 8), ag, 2),
                new Sale(1, () -> of(Bronze.BRONZE_INGOT, 2), ag, 3),
                new Sale(1, () -> of(Mechanical.TIN_INGOT, 1), ag, 2),
                new Sale(1, () -> of(Carting.ROPE_HALTER, 1), cu, 12),
                new Sale(1, () -> of(Carting.HIDE_SADDLE_BLANKET, 1), ag, 2),
                new Sale(1, () -> of(Spinning.LINEN_THREAD, 4), cu, 10),
                new Sale(1, () -> of(Bronze.BRONZE_GREAVES, 1), ag, 5),
                new Sale(2, () -> new ItemStack(Items.RAW_IRON, 4), ag, 6),
                new Sale(2, () -> of(Mechanical.IRON_KNIFE, 1), ag, 8),
                new Sale(2, () -> of(Ironworking.WROUGHT_IRON_INGOT, 1), au, 1));
    }

    static final int SALES = 8;

    /**
     * The trader's offers for players of {@code age}: {@link #SALES} goods of at most one age ahead, coin changing both ways,
     * then every gemstone it buys.
     */
    static MerchantOffers offers(int age, RandomSource r) {
        MerchantOffers offers = new MerchantOffers();
        List<Sale> pool = new ArrayList<>(sales().stream().filter(s -> s.tier <= age + 1).toList());
        for (int i = 0; i < SALES && !pool.isEmpty(); i++) {
            Sale s = pool.remove(r.nextInt(pool.size()));
            offers.add(new MerchantOffer(new ItemCost(s.coin.get(), s.price), s.goods.get(), 4, 0, 0));
        }
        offers.add(new MerchantOffer(new ItemCost(Coins.COPPER_COIN.get(), Coins.SILVER_IN_COPPER), new ItemStack(Coins.SILVER_COIN.get()), 16, 0, 0));
        offers.add(new MerchantOffer(new ItemCost(Coins.SILVER_COIN.get(), 1), new ItemStack(Coins.COPPER_COIN.get(), Coins.SILVER_IN_COPPER), 16, 0, 0));
        offers.add(new MerchantOffer(new ItemCost(Coins.SILVER_COIN.get(), Coins.GOLD_IN_SILVER), new ItemStack(Coins.GOLD_COIN.get()), 16, 0, 0));
        offers.add(new MerchantOffer(new ItemCost(Coins.GOLD_COIN.get(), 1), new ItemStack(Coins.SILVER_COIN.get(), Coins.GOLD_IN_SILVER), 16, 0, 0));
        // they love gemstones: the gemstone branch's cut stones (empty tag = none), then vanilla's
        for (var gem : BuiltInRegistries.ITEM.getTagOrEmpty(GEMSTONES))
            offers.add(new MerchantOffer(new ItemCost(gem.value(), 1), new ItemStack(Coins.SILVER_COIN.get(), 4), 12, 0, 0));
        offers.add(new MerchantOffer(new ItemCost(Items.DIAMOND, 1), new ItemStack(Coins.GOLD_COIN.get(), 2), 12, 0, 0));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Coins.GOLD_COIN.get(), 1), 12, 0, 0));
        offers.add(new MerchantOffer(new ItemCost(Items.AMETHYST_SHARD, 1), new ItemStack(Coins.SILVER_COIN.get(), 2), 12, 0, 0));
        return offers;
    }

    static boolean isCoin(ItemStack s) {
        return s.is(Coins.COPPER_COIN.get()) || s.is(Coins.SILVER_COIN.get()) || s.is(Coins.GOLD_COIN.get());
    }

    /** Coins for coins: no standing discount (it would mint money). */
    static boolean isExchange(MerchantOffer offer) {
        return isCoin(offer.getBaseCostA()) && isCoin(offer.getResult());
    }

    static boolean isGem(ItemStack s) {
        return s.is(GEMSTONES) || s.is(Items.DIAMOND) || s.is(Items.EMERALD) || s.is(Items.AMETHYST_SHARD);
    }

    /** Time is up: the member goes, and with it the horses it leads and their carts and loads. */
    static void leave(Tribesperson t) {
        for (AbstractHorse h : t.level().getEntitiesOfClass(AbstractHorse.class, t.getBoundingBox().inflate(16), h -> h.getLeashHolder() == t)) {
            for (CartEntity c : t.level().getEntitiesOfClass(CartEntity.class, h.getBoundingBox().inflate(8), c -> c.harness() == h)) {
                c.getPassengers().forEach(Entity::discard);
                c.discard();
            }
            h.discard();
        }
        t.discard();
    }

    private Caravans() {}
}
