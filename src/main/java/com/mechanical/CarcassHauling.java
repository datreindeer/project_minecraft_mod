package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
 * both arms up holding the body on the shoulders, or reaching back to the dragged one (client/HaulingClient).</li>
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

    /** Smoked Meat a strip on the Smoking Rack turns into: a whole cut gives one per meat unit. */
    public static int smokedPieces(ItemStack raw) {
        return raw.is(RAW_HAUNCH.get()) ? 3 : raw.is(RAW_RIBS.get()) || raw.is(RAW_LOIN.get()) ? 2 : 1;
    }

    /** The carcass {@code player} carries or drags, or null. Works on both sides (the hauler is synced). */
    public static CarcassEntity hauled(Player player) {
        for (CarcassEntity c : player.level().getEntitiesOfClass(CarcassEntity.class, player.getBoundingBox().inflate(12), c -> c.haulerId() == player.getId()))
            return c;
        return null;
    }

    /** Hands full of something heavy: no sprinting (see PlayerMixin). */
    public static boolean blocksSprint(Player player) {
        CarcassEntity c = hauled(player);
        if (c != null && !c.isLight()) return true;
        TravoisEntity t = TravoisEntity.harnessedTo(player);
        return t != null && t.blocksSprint();
    }

    /** Sneak + right-click with an empty hand: shoulder it or start dragging it; right-click what you drag to let go. */
    static InteractionResult pickUpOrLetGo(Player player, CarcassEntity carcass) {
        if (carcass.haulerId() == player.getId()) {
            if (!player.level().isClientSide()) letGo(carcass, player, null);
            return InteractionResult.SUCCESS;
        }
        if (hauled(player) != null) return InteractionResult.PASS;
        if (!player.level().isClientSide()) {
            carcass.stopRiding();
            if (carcass.hungAt() != null) carcass.unhang();
            carcass.setHauler(player);
            player.level().playSound(null, carcass.getX(), carcass.getY(), carcass.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
            player.sendOverlayMessage(Component.translatable(carcass.isLarge() ? "message.mechanical.carcass.drag" : "message.mechanical.carcass.carry"));
        }
        return InteractionResult.SUCCESS;
    }

    /** Server tick of a hauled carcass: on the shoulders it rides along with the player; a large one is dragged a body length behind. */
    static void follow(CarcassEntity c, Player p) {
        if (!c.isLarge()) {
            // the renderer draws it across the shoulders from the player's own position; this only keeps it tracked and near
            c.setPos(p.getX(), p.getY() + 1.0, p.getZ());
            c.setDeltaMovement(Vec3.ZERO);
            bleed(c, p.position(), false);
            return;
        }
        Vec3 off = c.position().subtract(p.position()).multiply(1, 0, 1);
        double d = off.length(), keep = 0.5 + c.getBbWidth() / 2;
        Vec3 pull = d > keep ? off.scale((keep - d) / d) : Vec3.ZERO;
        double fall = c.onGround() ? -0.01 : Math.max(-0.5, c.getDeltaMovement().y - 0.08);
        c.drag(new Vec3(pull.x, fall, pull.z));
        c.setDeltaMovement(0, c.onGround() ? 0 : fall, 0);
        // head first towards the one pulling it
        if (d > 0.3) c.setYRot((float) Math.toDegrees(Math.atan2(off.x, -off.z)));
        bleed(c, c.position(), true);
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

    /** Puts it down at {@code at} (a carried one; null: in front of the player). A dragged one stays where it is. */
    static void letGo(CarcassEntity c, Player p, Vec3 at) {
        c.setHauler(null);
        c.setDeltaMovement(Vec3.ZERO);
        if (c.isLarge()) return;
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
        }
        if (player.level().isClientSide() || player.tickCount % 5 != 0) return;
        double slow = 0, food = 0;
        CarcassEntity c = hauled(player);
        if (c != null) {
            slow += c.isLarge() ? SLOW_DRAG : c.isLight() ? 0 : SLOW_CARRY;
            food += c.isLarge() ? FOOD_DRAG : c.isLight() ? 0 : FOOD_CARRY;
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
