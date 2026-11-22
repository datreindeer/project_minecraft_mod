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
 * <li>Sneak + right-click a carcass with an empty hand: a small or medium body goes on your shoulders (medium ones slow
 * you and stop you sprinting), a large one is dragged along behind you (slower still). Both hands are full: no item can be
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
    static final Identifier MANY_HANDS = Identifier.fromNamespaceAndPath(MODID, "wilderness/many_hands"),
            MOVING_A_MOUNTAIN = Identifier.fromNamespaceAndPath(MODID, "wilderness/moving_a_mountain");

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

    /** The carcass {@code player} carries or drags (alone or with others), or null. Works on both sides (the haulers are synced). */
    public static CarcassEntity hauled(Player player) {
        for (CarcassEntity c : player.level().getEntitiesOfClass(CarcassEntity.class, player.getBoundingBox().inflate(12), c -> c.slotOf(player) >= 0))
            return c;
        return null;
    }

    /** Fewer players on it than it takes to move it (both sides: drives the strain pose too). */
    public static boolean straining(CarcassEntity c) {
        return c.isLarge() && c.haulerCount() < c.pullersNeeded();
    }

    /** Hands full of something heavy: no sprinting (see PlayerMixin). */
    public static boolean blocksSprint(Player player) {
        if (FelledLogEntity.liftedBy(player) != null) return true;
        CarcassEntity c = hauled(player);
        if (c != null && !c.isLight()) return true;
        TravoisEntity t = TravoisEntity.harnessedTo(player);
        return t != null && t.blocksSprint();
    }

    /** Sneak + right-click with an empty hand: shoulder it or start dragging it; right-click what you drag to let go. */
    static InteractionResult pickUpOrLetGo(Player player, CarcassEntity carcass) {
        if (carcass.slotOf(player) >= 0) {
            if (!player.level().isClientSide()) letGo(carcass, player, null);
            return InteractionResult.SUCCESS;
        }
        if (hauled(player) != null) return InteractionResult.PASS;
        boolean joining = carcass.haulerId() >= 0;
        if (!player.level().isClientSide()) {
            if (joining && !carcass.addHauler(player)) {
                // on someone's shoulders, or every limb of a big one already held
                player.sendOverlayMessage(Component.translatable("message.mechanical.carcass.taken"));
                return InteractionResult.SUCCESS;
            }
            if (!joining) {
                carcass.stopRiding();
                if (carcass.hungAt() != null) carcass.unhang();
                carcass.addHauler(player);
            }
            player.level().playSound(null, carcass.getX(), carcass.getY(), carcass.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
            if (carcass.isLarge()) tellPullers(carcass);
            else player.sendOverlayMessage(Component.translatable("message.mechanical.carcass.carry"));
        }
        return InteractionResult.SUCCESS;
    }

    /** Server: tells everyone dragging {@code c} how it goes: alone, together, or too few to move it. */
    private static void tellPullers(CarcassEntity c) {
        int n = c.haulerCount(), need = c.pullersNeeded();
        Component msg = n < need ? Component.translatable("message.mechanical.carcass.too_heavy", need, n)
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
    static void follow(CarcassEntity c) {
        if (!c.isLarge()) {
            Player p = c.hauler();
            // the renderer draws it across the shoulders from the player's own position; this only keeps it tracked and near
            c.setPos(p.getX(), p.getY() + 1.0, p.getZ());
            c.setDeltaMovement(Vec3.ZERO);
            bleed(c, p.position(), false);
            return;
        }
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
        double strength = n < need ? STRAIN_CREEP * n / need : 1.0;
        if (pulling > 0) pull = pull.scale(strength / pulling);
        // in water it floats half under like a loose body (CarcassEntity#tick) instead of sinking
        boolean afloat = c.getFluidHeight(FluidTags.WATER) > c.getBbHeight() * 0.5;
        double fall = afloat ? Math.min(c.getDeltaMovement().y + 0.01, 0.06)
                : c.onGround() ? -0.01 : Math.max(-0.5, c.getDeltaMovement().y - 0.08);
        c.drag(new Vec3(pull.x, fall, pull.z));
        c.setDeltaMovement(0, afloat || !c.onGround() ? fall : 0, 0);
        // head end round towards the pullers, a little each tick
        Vec3 to = centre.scale(1.0 / n).subtract(c.position()).multiply(1, 0, 1);
        if (to.length() > 0.3) {
            float target = (float) Math.toDegrees(Math.atan2(-to.x, to.z)), turn = (float) (TURN_RATE * strength);
            c.setYRot(c.getYRot() + Mth.clamp(Mth.wrapDegrees(target - c.getYRot()), -turn, turn));
        }
        bleed(c, c.position(), true);
        if (c.tickCount % 40 == 0 && n < need && pulling > 0) tellPullers(c); // keeps telling them while they strain
        if (c.tickCount % 20 == 0 && n > 1 && n >= need && pulling > 0) {
            boolean mammoth = c.animal() == com.mechanical.wildlife.giants.GiantsFauna.WOOLLY_MAMMOTH.get();
            for (int i = 0; i < CarcassEntity.MAX_HAULERS; i++) {
                if (!(c.hauler(i) instanceof ServerPlayer sp)) continue;
                Kettle.award(sp, MANY_HANDS);
                if (mammoth) Kettle.award(sp, MOVING_A_MOUNTAIN);
            }
        }
    }

    /**
     * Client, own player, after it moved: a body too heavy for its pullers holds him within {@link #GRIP_REACH} +
     * {@link #TETHER_SLACK} of his grip, like a rope (as TravoisEntity#tether; never pulled in from further than he was).
     */
    static void tether(Player mover, CarcassEntity c) {
        int slot = c.slotOf(mover);
        if (slot < 0 || !straining(c)) return;
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
    static void letGo(CarcassEntity c, Player p, Vec3 at) {
        int slot = c.slotOf(p);
        if (slot >= 0) c.removeHauler(slot);
        c.setDeltaMovement(Vec3.ZERO);
        if (c.isLarge()) {
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
        CarcassEntity c = hauled(player);
        if (c == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getLevel() instanceof ServerLevel level)) return;
        var state = level.getBlockState(event.getPos());
        if (state.is(HANGING_RACK.get())) HangingRackBlock.tryHang(level, event.getPos(), state, c, player);
        else letGo(c, player, Vec3.atBottomCenterOf(event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace())));
    }

    /** No eating, drinking, shields, bows or throwing with a deer on your back (both sides, so the client doesn't predict it). */
    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (hauled(event.getEntity()) == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    /** Nor shearing, leashing or feeding: only the Travois (loads it) and the hauled body (lets go) take a hauler's click. */
    @SubscribeEvent
    static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof TravoisEntity || event.getTarget() instanceof CarcassEntity || hauled(event.getEntity()) == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    /** Whatever other way an item gets used (a knife on another body ...): not with full hands. */
    @SubscribeEvent
    static void onUseItem(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && hauled(player) != null) event.setCanceled(true);
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

    private static void dropHauled(Player player) {
        if (player.level().isClientSide()) return;
        CarcassEntity c = hauled(player);
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
            CarcassEntity dragged = hauled(player);
            if (dragged != null) tether(player, dragged);
        }
        if (player.level().isClientSide() || player.tickCount % 5 != 0) return;
        double slow = 0, food = 0;
        CarcassEntity c = hauled(player);
        if (c != null && c.isLarge()) {
            // the work is shared: two on a bear go at 75 % speed, four on the mammoth at 50 %; too few on a huge one barely move
            double share = straining(c) ? 1.0 : (double) c.pullersNeeded() / c.haulerCount();
            slow += straining(c) ? SLOW_STRAIN : SLOW_DRAG * share;
            food += FOOD_DRAG * share;
        } else if (c != null) {
            slow += c.isLight() ? 0 : SLOW_CARRY;
            food += c.isLight() ? 0 : FOOD_CARRY;
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
