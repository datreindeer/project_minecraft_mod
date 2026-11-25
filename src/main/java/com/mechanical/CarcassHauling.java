package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import net.minecraft.tags.FluidTags;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Getting a kill back to camp, and butchering it properly there.
 * <ul>
 * <li>Sneak + right-click a carcass with an empty hand: a body up to a sheep's size goes on your shoulders (all but small
 * ones slow you and stop you sprinting), a heavier one (pig, cow and up, {@link CarcassEntity#isDragged}) is dragged
 * along behind you (slower still). Both hands are full: no item can be
 * used or placed, nor used on an animal (only the Travois and the body itself react), an attack or a hard hit (4+ damage)
 * drops it, and right-clicking a block puts it down there. Right-click a dragged body again to let go. Players show it:
 * both arms up holding the body on the shoulders, or both hands on the dragged one's leg, tugging at every step
 * (client/HaulingClient).</li>
 * <li>Dragging ({@link #follow}): you pull it by a leg, the view stays free; it turns its head end towards its pullers and glides after them, smoothed on the client.
 * Up to {@link CarcassEntity#MAX_HAULERS} players can drag one large body together, each holding a limb: the slowdown and
 * food cost are shared ({@code SLOW_DRAG * needed / pullers}). A huge one needs {@link CarcassEntity#pullersNeeded} (3,
 * the mammoth 4): fewer only strain at it, it creeps ({@link #STRAIN_CREEP}) and holds them back ({@link #tether}).</li>
 * <li>A fresh body bleeds on the way ({@link #bleed}): a dragged one leaves a smear about every {@link #DRAG_TRAIL} blocks,
 * one on the shoulders drips and leaves a stain now and then. Aged, turning or spoiled ones no longer bleed.</li>
 * <li>{@link TravoisEntity}: a drag sledge that follows you when harnessed and holds 3 room of bodies (light 1, medium 2, large 3).
 * Hauling by hand or in the harness costs food by the block ({@link #exhaust}).</li>
 * <li>{@link HangingRackBlock}: right-click it with a hauled body to hang it; butchered there it gives twice the meat as
 * Haunches, Ribs and Loins (see {@link Butchery#dropInner}), the whole hide, and more fat, sinew and bone.</li>
 * </ul>
 * Bloody carcasses draw night predators ({@code Predator.CarcassScentGoal}). Data: scripts/gen_carcass_hauling.py,
 * textures: scripts/gen_carcass_hauling_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class CarcassHauling {
    private static final Identifier SLOW = Identifier.fromNamespaceAndPath(MODID, "hauling");
    /** Speed lost: a medium body on the shoulders, a large one dragged, a harnessed travois (plus per room of load; the Cart: CartEntity). */
    static final double SLOW_CARRY = 0.25, SLOW_DRAG = 0.5, SLOW_TRAVOIS = 0.1, SLOW_TRAVOIS_LOAD = 0.07;
    /**
     * Food exhaustion per block walked (4 = half a drumstick; vanilla sprinting costs 0.1): a body on the shoulders (not a
     * light one) or dragged by hand, and a travois or cart in the harness: empty, plus this much more when full. A horse
     * in the shafts pulls for free. 300 blocks with a full cart: ~24, three drumsticks.
     */
    static final double FOOD_CARRY = 0.03, FOOD_DRAG = 0.08, FOOD_TRAVOIS = 0.02, FOOD_TRAVOIS_FULL = 0.08, FOOD_CART = 0.01, FOOD_CART_FULL = 0.07;
    /** Server: where each hauling player was at the last food check. */
    private static final java.util.Map<Player, Vec3> LAST_POS = new java.util.WeakHashMap<>();
    /** A hit this hard makes you drop what you haul. */
    static final float DROP_DAMAGE = 4.0F;
    /** Blocks travelled between blood stains: dragged (always), carried (half the time, so one every ~6 blocks). */
    static final double DRAG_TRAIL = 2.0, CARRY_TRAIL = 3.0;
    /** Arm's length: how far a dragged body's grip trails its puller. Up to {@link #TETHER_SLACK} past it, a body too heavy to move holds him (client). */
    static final double GRIP_REACH = 1.0, TETHER_SLACK = 1.5;
    /** Too few pullers on a huge body: it moves only this share of their pull times pullers/needed (it barely creeps), and slows them this much. */
    static final double STRAIN_CREEP = 0.1, SLOW_STRAIN = 0.7;
    /** Most degrees a dragged body turns per tick towards its pullers (smooth, no snapping round). */
    static final float TURN_RATE = 8.0F;
    /** Fastest a dragged body goes (blocks a tick, 5 a second): however far ahead its pullers are, it never leaps after them. */
    static final double MAX_DRAG = 0.25;
    /**
     * Several pullers: it turns towards the middle of them, gently ({@link #MULTI_TURN_DAMPING} of what is left, at most
     * {@link #MULTI_TURN_RATE} degrees a tick) and not at all within {@link #MULTI_TURN_SLACK} degrees, or while that middle is
     * nearer than {@link #MULTI_TURN_REACH} (pullers on opposite sides: no way to face, turning there spun it round and round).
     */
    static final float MULTI_TURN_RATE = 3.0F, MULTI_TURN_DAMPING = 0.15F, MULTI_TURN_SLACK = 6.0F;
    static final double MULTI_TURN_REACH = 1.5;
    static final Identifier MANY_HANDS = Identifier.fromNamespaceAndPath(MODID, "wilderness/many_hands"),
            MOVING_A_MOUNTAIN = Identifier.fromNamespaceAndPath(MODID, "wilderness/moving_a_mountain"),
            ALL_HANDS = Identifier.fromNamespaceAndPath(MODID, "wilderness/elkwight_all_hands");

    public static final DeferredBlock<HangingRackBlock> HANGING_RACK = BLOCKS.registerBlock("hanging_rack", HangingRackBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> HANGING_RACK_ITEM = ITEMS.registerSimpleBlockItem("hanging_rack", HANGING_RACK);

    public static final DeferredHolder<EntityType<?>, EntityType<TravoisEntity>> TRAVOIS = ENTITY_TYPES.registerEntityType("travois",
            TravoisEntity::new, MobCategory.MISC, b -> b.noLootTable().sized(1.4F, 0.5F).clientTrackingRange(10));
    public static final DeferredItem<TravoisEntity.Placer> TRAVOIS_ITEM = ITEMS.registerItem("travois", p -> new TravoisEntity.Placer(p, TRAVOIS), p -> p.stacksTo(1));

    // big cuts from a hung carcass; 1 meat unit = 1 scrap = 1 Smoked Meat (Haunch 3 units, Ribs and Loin 2)
    public static final DeferredItem<Item> RAW_HAUNCH = food("raw_haunch", 3, 0.2F);
    public static final DeferredItem<Item> COOKED_HAUNCH = food("cooked_haunch", 9, 0.8F);
    public static final DeferredItem<Item> RAW_RIBS = food("raw_ribs", 2, 0.2F);
    public static final DeferredItem<Item> COOKED_RIBS = food("cooked_ribs", 6, 0.8F);
    public static final DeferredItem<Item> RAW_LOIN = food("raw_loin", 2, 0.2F);
    public static final DeferredItem<Item> COOKED_LOIN = food("cooked_loin", 6, 1.0F);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    public static void init() {
    }

    /** Meat units in a piece: a big cut holds several (the Smoked Meat or strips a knife slices it into). */
    public static int smokedPieces(ItemStack raw) {
        return raw.is(RAW_HAUNCH.get()) ? 3 : raw.is(RAW_RIBS.get()) || raw.is(RAW_LOIN.get()) ? 2 : 1;
    }

    /** The heavy load (a carcass, the Elkwight's axe ...) {@code player} carries or drags, or null. Both sides (the haulers are synced). */
    public static HeavyLoad heavy(Player player) {
        for (HeavyLoad c : player.level().getEntitiesOfClass(HeavyLoad.class, player.getBoundingBox().inflate(12), c -> c.slotOf(player) >= 0))
            return c;
        return null;
    }

    /** The carcass {@code player} carries or drags (alone or with others), or null. Works on both sides (the haulers are synced). */
    public static CarcassEntity hauled(Player player) {
        return heavy(player) instanceof CarcassEntity c ? c : null;
    }

    /** Fewer players on it than it takes to move it at full pace (both sides: drives the strain pose too). */
    public static boolean straining(HeavyLoad c) {
        return c.isDragged() && c.haulerCount() < c.pullersNeeded();
    }

    /**
     * How hard {@code n} pullers move {@code c}: 0 (it doesn't budge) below {@link HeavyLoad#pullersToBudge}, a creep of
     * {@link #STRAIN_CREEP} * n / needed below {@link HeavyLoad#pullersNeeded}, else 1.
     */
    public static double pullStrength(HeavyLoad c, int n) {
        int need = c.pullersNeeded();
        return n < c.pullersToBudge() ? 0.0 : n < need ? STRAIN_CREEP * n / need : 1.0;
    }

    /** Hands full of something heavy: no sprinting (see PlayerMixin). */
    public static boolean blocksSprint(Player player) {
        if (FelledLogEntity.liftedBy(player) != null) return true;
        HeavyLoad c = heavy(player);
        if (c != null && c.blocksSprint()) return true;
        TravoisEntity t = TravoisEntity.harnessedTo(player);
        return t != null && t.blocksSprint();
    }

    /** Sneak + right-click with an empty hand: shoulder it or start dragging it; right-click what you drag to let go. */
    public static InteractionResult pickUpOrLetGo(Player player, HeavyLoad carcass) {
        if (carcass.slotOf(player) >= 0) {
            if (!player.level().isClientSide()) {
                letGo(carcass, player, null);
                if (carcass instanceof CarcassEntity c) com.mechanical.people.CarcassGifts.left(c, player); // put down at an NPC camp: a gift (or a threat)
            }
            return InteractionResult.SUCCESS;
        }
        if (heavy(player) != null) return InteractionResult.PASS;
        boolean joining = carcass.haulerId() >= 0;
        if (!player.level().isClientSide()) {
            if (joining && !carcass.addHauler(player)) {
                // on someone's shoulders, or every limb of a big one already held
                player.sendOverlayMessage(Component.translatable("message.mechanical.carcass.taken"));
                return InteractionResult.SUCCESS;
            }
            if (!joining) {
                carcass.stopRiding();
                if (carcass instanceof CarcassEntity c) {
                    c.unsling(); // off a horse's side
                    if (c.hungAt() != null) c.unhang();
                }
                carcass.addHauler(player);
            }
            player.level().playSound(null, carcass.getX(), carcass.getY(), carcass.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
            if (carcass.isDragged()) tellPullers(carcass);
            else player.sendOverlayMessage(Component.translatable("message.mechanical.carcass.carry"));
        }
        return InteractionResult.SUCCESS;
    }

    /** Server: tells everyone dragging {@code c} how it goes: alone, together, or too few to move it. */
    private static void tellPullers(HeavyLoad c) {
        int n = c.haulerCount(), need = c.pullersNeeded();
        Component msg = n < c.pullersToBudge() ? Component.translatable("message.mechanical.heavy.wont_budge", c.pullersToBudge(), n)
                : n < need ? Component.translatable("message.mechanical.carcass.too_heavy", need, n)
                : Component.translatable(n > 1 ? "message.mechanical.carcass.drag_together" : "message.mechanical.carcass.drag", n);
        for (int i = 0; i < CarcassEntity.MAX_HAULERS; i++) {
            Player p = c.hauler(i);
            if (p != null) p.sendOverlayMessage(msg);
        }
    }

    /**
     * Server tick of a hauled carcass: on the shoulders it rides along with the player. A large one is dragged: each puller
     * draws his grip ({@link CarcassEntity#gripPoint}) to within {@link #GRIP_REACH} of himself, the body moving by the
     * average of those pulls (too few pullers: a creep of it) and turning its head end towards them a little each tick.
     */
    static void follow(HeavyLoad load) {
        if (!load.isDragged() && load instanceof CarcassEntity carried) {
            Player p = carried.hauler();
            // the renderer draws it across the shoulders from the player's own position; this only keeps it tracked and near
            carried.setPos(p.getX(), p.getY() + 1.0, p.getZ());
            carried.setDeltaMovement(Vec3.ZERO);
            bleed(carried, p.position(), false);
            return;
        }
        HeavyLoad c = load;
        Vec3 pull = Vec3.ZERO, centre = Vec3.ZERO;
        int n = 0, pulling = 0;
        for (int i = 0; i < CarcassEntity.MAX_HAULERS; i++) {
            Player p = c.hauler(i);
            if (p == null) continue;
            n++;
            centre = centre.add(p.position());
            Vec3 off = p.position().subtract(c.gripPoint(i, 1.0F)).multiply(1, 0, 1);
            double d = off.length();
            if (d > GRIP_REACH) {
                pull = pull.add(off.scale((d - GRIP_REACH) / d));
                pulling++;
            }
        }
        if (n == 0) return;
        int need = c.pullersNeeded();
        double strength = pullStrength(c, n);
        if (pulling > 0) pull = pull.scale(strength / pulling);
        if (pull.length() > MAX_DRAG) pull = pull.normalize().scale(MAX_DRAG);
        // in water it floats half under like a loose body (CarcassEntity#tick) instead of sinking
        boolean afloat = c.getFluidHeight(FluidTags.WATER) > c.getBbHeight() * 0.5;
        double fall = afloat ? Math.min(c.getDeltaMovement().y + 0.01, 0.06)
                : c.onGround() ? -0.01 : Math.max(-0.5, c.getDeltaMovement().y - 0.08);
        c.drag(new Vec3(pull.x, fall, pull.z));
        c.setDeltaMovement(0, afloat || !c.onGround() ? fall : 0, 0);
        // head end round towards the pullers, a little each tick
        Vec3 to = centre.scale(1.0 / n).subtract(c.position()).multiply(1, 0, 1);
        if (to.length() > (n > 1 ? MULTI_TURN_REACH : 0.3)) {
            float target = (float) Math.toDegrees(Math.atan2(-to.x, to.z)), left = Mth.wrapDegrees(target - c.getYRot());
            if (n > 1) left = Math.abs(left) < MULTI_TURN_SLACK ? 0 : left * MULTI_TURN_DAMPING;
            float turn = (float) ((n > 1 ? MULTI_TURN_RATE : TURN_RATE) * strength);
            c.setYRot(c.getYRot() + Mth.clamp(left, -turn, turn));
        }
        if (c instanceof CarcassEntity body) bleed(body, c.position(), true);
        if (c.tickCount % 40 == 0 && n < need && pulling > 0) tellPullers(c); // keeps telling them while they strain
        if (c.tickCount % 20 == 0 && n > 1 && n >= need && pulling > 0 && c instanceof CarcassEntity body) {
            boolean mammoth = body.animal() == com.mechanical.wildlife.giants.GiantsFauna.WOOLLY_MAMMOTH.get();
            boolean allHands = body.isColossal() && n >= HeavyLoad.MAX_HAULERS; // the Elkwight, with three others
            for (int i = 0; i < CarcassEntity.MAX_HAULERS; i++) {
                if (!(c.hauler(i) instanceof ServerPlayer sp)) continue;
                Kettle.award(sp, MANY_HANDS);
                if (mammoth) Kettle.award(sp, MOVING_A_MOUNTAIN);
                if (allHands) Kettle.award(sp, ALL_HANDS);
            }
        }
    }

    /**
     * Client, own player, after it moved: a body too heavy for its pullers, or dragged by several, holds him within {@link #GRIP_REACH} +
     * {@link #TETHER_SLACK} of his grip, like a rope (as TravoisEntity#tether; never pulled in from further than he was).
     */
    static void tether(Player mover, HeavyLoad c) {
        int slot = c.slotOf(mover);
        if (slot < 0 || c.haulerCount() < 2 && !straining(c)) return;
        Vec3 grip = c.gripPoint(slot, 1.0F);
        Vec3 off = mover.position().subtract(grip).multiply(1, 0, 1);
        double d = off.length(), max = GRIP_REACH + TETHER_SLACK;
        if (d <= max) return;
        Vec3 n = off.scale(1 / d);
        double limit = Math.max(max, new Vec3(mover.xo - grip.x, 0, mover.zo - grip.z).length());
        if (d > limit) mover.setPos(grip.x + n.x * limit, mover.getY(), grip.z + n.z * limit);
        Vec3 v = mover.getDeltaMovement();
        double out = v.x * n.x + v.z * n.z;
        if (out > 0) mover.setDeltaMovement(v.subtract(n.x * out, 0, n.z * out));
    }

    /** A fresh body leaves a sparse trail where it goes ({@code at}: the ground under it); counted by distance, so none while standing still. */
    private static void bleed(CarcassEntity c, Vec3 at, boolean dragged) {
        if (c.aged() || c.isTurning() || c.spoiled() || c.animal().builtInRegistryHolder().is(Blood.BLOODLESS) || !(c.level() instanceof ServerLevel level)) return;
        var r = c.getRandom();
        if (!dragged && c.tickCount % 20 == 0 && r.nextInt(2) == 0)
            Blood.drip(level, c.getX() + r.nextGaussian() * 0.2, c.getY() + 0.3, c.getZ() + r.nextGaussian() * 0.2);
        if (c.lastStain == null) c.lastStain = at;
        double step = dragged ? DRAG_TRAIL : CARRY_TRAIL;
        if (at.distanceToSqr(c.lastStain) < step * step) return;
        c.lastStain = at;
        if (dragged || r.nextInt(2) == 0) Blood.splatter(level, net.minecraft.core.BlockPos.containing(at), 0, 1);
    }

    /** {@code p} lets go. A carried one is put down at {@code at} (null: in front of the player); a dragged one stays where it is (or the others pull on). */
    static void letGo(HeavyLoad c, Player p, Vec3 at) {
        int slot = c.slotOf(p);
        if (slot >= 0) c.removeHauler(slot);
        c.setDeltaMovement(Vec3.ZERO);
        if (c.isDragged()) {
            if (c.haulerId() >= 0) tellPullers(c);
            return;
        }
        if (at == null) at = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(0.9)).add(0, 0.3, 0);
        Vec3 old = c.position();
        c.setPos(at);
        if (!c.level().noCollision(c)) c.setPos(p.getX(), p.getY() + 0.3, p.getZ());
        if (!c.level().noCollision(c)) c.setPos(old);
        p.level().playSound(null, c.getX(), c.getY(), c.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    /** Right-click a block while hauling: hang it on a Hanging Rack, or put it down on that block. */
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        HeavyLoad load = heavy(player);
        if (load == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getLevel() instanceof ServerLevel level)) return;
        var state = level.getBlockState(event.getPos());
        var face = event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace();
        if (!(load instanceof CarcassEntity c)) {
            if (!load.putDownOn(level, event.getPos(), face, player)) letGo(load, player, Vec3.atBottomCenterOf(event.getPos().relative(face)));
        } else if (state.is(HANGING_RACK.get())) HangingRackBlock.tryHang(level, event.getPos(), state, c, player);
        else if (FirePit.isBig(state) && FirePit.entity(level, event.getPos(), state) instanceof FirePitBlockEntity pit) pit.hangCarcass(level, player, c);
        else {
            letGo(c, player, Vec3.atBottomCenterOf(event.getPos().relative(face)));
            com.mechanical.people.CarcassGifts.left(c, player);
        }
    }

    /** No eating, drinking, shields, bows or throwing with a deer on your back (both sides, so the client doesn't predict it). */
    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (heavy(event.getEntity()) == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    /**
     * Nor shearing, leashing or feeding: only the Travois (loads it), the hauled body (lets go) and a tamed horse (a body
     * from the shoulders hangs on its side, HorseWork#sling) take a hauler's click.
     */
    @SubscribeEvent
    static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof TravoisEntity || event.getTarget() instanceof HeavyLoad) return;
        HeavyLoad load = heavy(event.getEntity());
        if (load == null) return;
        boolean slung = event.getHand() == InteractionHand.MAIN_HAND && event.getTarget() instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse
                && load instanceof CarcassEntity c && HorseWork.sling(event.getEntity(), c, horse);
        event.setCanceled(true);
        event.setCancellationResult(slung ? InteractionResult.SUCCESS : InteractionResult.FAIL);
    }

    /** Whatever other way an item gets used (a knife on another body ...): not with full hands. */
    @SubscribeEvent
    static void onUseItem(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && heavy(player) != null) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        dropHauled(event.getEntity());
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        dropHauled(event.getEntity());
    }

    @SubscribeEvent
    static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && event.getInflictedDamage() >= DROP_DAMAGE) dropHauled(player);
    }

    /** Server: puts down or lets go of whatever {@code player} hauls (also when he goes down: clan.Downed). */
    public static void dropHauled(Player player) {
        if (player.level().isClientSide()) return;
        HeavyLoad c = heavy(player);
        if (c != null) letGo(c, player, null);
    }

    /** Heavy loads slow you down; a stuck travois holds you back. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.isLocalInstanceAuthoritative() && !player.isPassenger()) {
            // client, own player, after it moved: a stuck travois holds it back
            TravoisEntity held = TravoisEntity.harnessedTo(player);
            if (held != null) TravoisEntity.tether(player, held);
            HeavyLoad dragged = heavy(player);
            if (dragged != null) tether(player, dragged);
        }
        if (player.level().isClientSide() || player.tickCount % 5 != 0) return;
        double slow = 0, food = 0;
        HeavyLoad c = heavy(player);
        if (c != null && c.isDragged()) {
            // the work is shared: two on a bear go at 75 % speed, four on the mammoth at 50 %; too few on a huge one barely move
            double share = straining(c) ? 1.0 : (double) c.pullersNeeded() / c.haulerCount();
            slow += straining(c) ? SLOW_STRAIN : SLOW_DRAG * share;
            food += FOOD_DRAG * share;
        } else if (c instanceof CarcassEntity body) {
            slow += body.isLight() ? 0 : SLOW_CARRY;
            food += body.isLight() ? 0 : FOOD_CARRY;
        }
        TravoisEntity t = TravoisEntity.harnessedTo(player);
        if (t != null) {
            slow += t.slowdown();
            food += t instanceof CartEntity ? FOOD_CART + FOOD_CART_FULL * t.loadFraction() : FOOD_TRAVOIS + FOOD_TRAVOIS_FULL * t.loadFraction();
        }
        exhaust(player, food);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (slow == 0) speed.removeModifier(SLOW);
        else speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW, -Math.min(0.7, slow), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Server, every 5 ticks: {@code perBlock} food exhaustion for each block walked since the last check (0: not hauling). */
    static void exhaust(Player player, double perBlock) {
        Vec3 last = perBlock > 0 ? LAST_POS.put(player, player.position()) : LAST_POS.remove(player);
        if (last == null || perBlock <= 0) return;
        double walked = player.position().subtract(last).horizontalDistance();
        if (walked < 5) player.causeFoodExhaustion((float) (walked * perBlock)); // more in 5 ticks: a teleport
    }

    private CarcassHauling() {}
}
