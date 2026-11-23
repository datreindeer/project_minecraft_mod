package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Work horses (living-world-plan D2-D4).
 * <ul>
 * <li>Stamina ("ork", {@link #STAMINA}, 0..{@link #MAX}, saved and synced on every horse): walking drains it by the
 * block, faster with load ({@link #load}: a rider, the wagon or cart it pulls and what is on it, the bodies on its sides).
 * Load also takes a little speed ({@link #LOAD_SLOW}); a tired horse ({@link #TIRED}) goes slower still. At 0 it is
 * {@link #EXHAUSTED}: it stops, throws its rider and lies down (client/wildlife/TameHorseRenderer) until it has
 * rested back to {@link #RISE}. Standing still rests it; water fills it fast: a Water Bucket used on it, or standing in
 * water it can drink without swimming. A horse in a wagon team holds the whole team to the slower of the two.</li>
 * <li>Sneak + right-click a horse hitched to a wagon: it comes out of the team ({@link WagonEntity#release}).</li>
 * <li>Right-click a tamed horse while carrying a body on your shoulders: it hangs on the horse's side, one per side
 * ({@link CarcassEntity#slingOn}); sneak + right-click the body to take it back onto your shoulders.</li>
 * </ul>
 * HUD: client/HorseStaminaHud while you ride a horse or sit on a wagon.
 */
@EventBusSubscriber(modid = MODID)
public final class HorseWork {
    public static final float MAX = 100, TIRED = 25, RISE = 30;
    /** Stamina per block walked, times (1 + {@link #LOAD_DRAIN} x load): ~1300 blocks with a rider, ~600 hauling a full wagon. */
    static final float DRAIN_PER_BLOCK = 0.045F, LOAD_DRAIN = 3;
    /** Per {@link #INTERVAL} ticks: standing still (0 to full in 50 s), drinking in shallow water; a bucket at once. */
    static final float REST = 1.0F, DRINK = 5.0F, BUCKET = 40.0F;
    /** Load: a rider, an empty wagon (shared by a pair), a full one on top; a cart empty and full; anything else pulled; each room of body on its sides. */
    static final double RIDER = 0.25, WAGON = 0.4, CART = 0.2, CART_FULL = 0.5, OTHER = 0.3, SIDE_ROOM = 0.15;
    /** Speed lost at load 1 (capped), and when tired. */
    static final double LOAD_SLOW = 0.1, TIRED_SLOW = 0.3;
    static final int INTERVAL = 10;
    /** Below this many blocks per {@link #INTERVAL} the horse is standing (resting). */
    static final double STILL = 0.3;
    /** Camels (the tame camel of wildlife/desert, vanilla's camel) go twice as far on their stamina: drain at this share. */
    static final float CAMEL_DRAIN = 0.5F;
    private static final Identifier SLOW = Identifier.fromNamespaceAndPath(MODID, "horse_work");
    static final Identifier WATER_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "bronze_age/water_the_horse"),
            PACK_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "bronze_age/pack_horse");

    public static final Supplier<AttachmentType<Float>> STAMINA = Mechanical.ATTACHMENTS.register("horse_stamina", () -> AttachmentType
            .builder(() -> MAX).serialize(Codec.FLOAT.fieldOf("value")).sync(ByteBufCodecs.FLOAT).build());
    public static final Supplier<AttachmentType<Boolean>> EXHAUSTED = Mechanical.ATTACHMENTS.register("horse_exhausted", () -> AttachmentType
            .builder(() -> false).serialize(Codec.BOOL.fieldOf("value")).sync(ByteBufCodecs.BOOL).build());
    /** Server: where each horse was at its last check. */
    private static final java.util.Map<Entity, Vec3> LAST_POS = new java.util.WeakHashMap<>();

    public static void init() {
    }

    /** A camel: made for the desert, it goes far without water (here and in Husbandry). */
    public static boolean hardy(Entity e) {
        return e instanceof net.minecraft.world.entity.animal.camel.Camel || e instanceof com.mechanical.wildlife.desert.TameCamel;
    }

    /** Stamina per block walked at {@code load}. */
    public static float drain(AbstractHorse horse, double load) {
        return (float) (DRAIN_PER_BLOCK * (1 + LOAD_DRAIN * load)) * (hardy(horse) ? CAMEL_DRAIN : 1);
    }

    public static float stamina(AbstractHorse horse) {
        return horse.getData(STAMINA);
    }

    public static boolean exhausted(AbstractHorse horse) {
        return horse.getData(EXHAUSTED);
    }

    public static void setStamina(AbstractHorse horse, float value) {
        float v = Math.clamp(value, 0, MAX);
        if (v != stamina(horse)) horse.setData(STAMINA, v);
    }

    /** The bodies hanging on {@code horse}'s sides. */
    public static List<CarcassEntity> sideCarcasses(AbstractHorse horse) {
        return horse.level().getEntitiesOfClass(CarcassEntity.class, horse.getBoundingBox().inflate(3), c -> c.slungHorse() == horse);
    }

    /** What {@code horse} carries and pulls, in "loads" (a full wagon on one horse is about 1.4). */
    public static double load(AbstractHorse horse) {
        double load = horse.getControllingPassenger() != null ? RIDER : 0;
        TravoisEntity pulled = TravoisEntity.pulling(horse);
        if (pulled instanceof WagonEntity w) load += (WAGON + w.loadFraction()) / (w.harness() != null && w.partner() != null ? 2 : 1);
        else if (pulled instanceof CartEntity c) load += CART + CART_FULL * c.loadFraction();
        else if (pulled != null) load += OTHER;
        for (CarcassEntity c : sideCarcasses(horse)) load += SIDE_ROOM * c.loadUnits();
        return load;
    }

    /** Speed share {@code horse} loses by its load and tiredness (1: it can't move). */
    static double slowdown(AbstractHorse horse, double load) {
        if (exhausted(horse)) return 1;
        return LOAD_SLOW * Math.min(1, load) + (stamina(horse) < TIRED ? TIRED_SLOW : 0);
    }

    /** Server, every {@link #INTERVAL} ticks: drain by the blocks walked, or rest / drink; exhaustion; the speed modifier. */
    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide() || (horse.tickCount + horse.getId()) % INTERVAL != 0) return;
        if (!horse.isAlive()) return;
        tick(horse);
    }

    /**
     * Server, before a horse's AI runs: hitched to a cart, wagon or plough with nobody riding or leading it, it stands in
     * the traces, calmly: no wandering, grazing walks, panicking off with the vehicle or looking round (a wagon's second
     * horse copies the first one's turn, a head turned by its own goals would fight that and shake). Vanilla turns the
     * goals back on by itself (Mob#updateControlFlags) once it is ridden or free.
     */
    @SubscribeEvent
    static void standInTraces(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide() || !TravoisEntity.standsHitched(horse)) return;
        horse.goalSelector.disableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE);
        horse.goalSelector.disableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK);
        horse.getNavigation().stop();
        horse.getMoveControl().setWait();
        horse.setZza(0);
        horse.setXxa(0);
    }

    /** Server, after its tick: a horse in the traces doesn't graze or rear (vanilla starts both at random): the pair stays in step. */
    @SubscribeEvent
    static void noGrazingInTraces(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide() || !TravoisEntity.standsHitched(horse)) return;
        if (horse.isEating()) horse.setEating(false);
        if (horse.isStanding()) horse.clearStanding();
    }

    static void tick(AbstractHorse horse) {
        Vec3 last = LAST_POS.put(horse, horse.position());
        double walked = last == null ? 0 : horse.position().subtract(last).horizontalDistance();
        if (walked > 8) walked = 0; // a teleport
        double load = load(horse);
        float stamina = stamina(horse);
        boolean drinking = horse.isInWater() && horse.getFluidHeight(FluidTags.WATER) <= horse.getBbHeight() * 0.6;
        if (drinking) {
            if (stamina < MAX && horse.getRandom().nextInt(4) == 0) horse.playSound(SoundEvents.GENERIC_DRINK.value(), 0.6F, 0.8F);
            stamina += DRINK;
        }
        if (walked >= STILL) stamina -= (float) walked * drain(horse, load);
        else stamina += REST;
        setStamina(horse, stamina);
        stamina = stamina(horse);
        if (!exhausted(horse) && stamina <= 0) exhaust(horse);
        else if (exhausted(horse) && stamina >= RISE) horse.setData(EXHAUSTED, false);
        if (exhausted(horse)) horse.getNavigation().stop();
        double slow = slowdown(horse, load);
        // a pair goes at the pace of the slower horse (the lead horse moves the team)
        if (TravoisEntity.pulling(horse) instanceof WagonEntity w && w.harness() == horse && w.partner() != null)
            slow = Math.max(slow, slowdown(w.partner(), load(w.partner())));
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (slow <= 0) speed.removeModifier(SLOW);
        else speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW, -Math.min(1, slow), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Server: all in: it throws its rider and lies down. */
    static void exhaust(AbstractHorse horse) {
        horse.setData(EXHAUSTED, true);
        if (horse.getControllingPassenger() instanceof Player rider)
            rider.sendOverlayMessage(Component.translatable("message.mechanical.horse.exhausted"));
        horse.ejectPassengers();
        horse.playSound(SoundEvents.HORSE_BREATHE, 1.0F, 0.6F);
    }

    /**
     * An exhausted horse won't take a rider. Nor will a wagon's second horse while someone drives the lead one: the team
     * has one driver (the second horse is placed beside the first, it can't be steered too).
     */
    @SubscribeEvent
    static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityBeingMounted() instanceof AbstractHorse horse && TravoisEntity.pulling(horse) instanceof WagonEntity w
                && w.partner() == horse && w.harness() != null && w.harness().getControllingPassenger() != null) {
            event.setCanceled(true);
            return;
        }
        if (event.isMounting() && event.getEntityBeingMounted() instanceof AbstractHorse horse && exhausted(horse)) {
            event.setCanceled(true);
            if (event.getEntityMounting() instanceof Player p && !p.level().isClientSide())
                p.sendOverlayMessage(Component.translatable("message.mechanical.horse.resting"));
        }
    }

    /** Sneak + right-click a hitched horse: unhitch it. A Water Bucket on a horse: it drinks. (Bodies: CarcassHauling.) */
    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractHorse horse) || event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        TravoisEntity pulled = TravoisEntity.pulling(horse);
        if (player.isShiftKeyDown() && pulled != null && (pulled instanceof WagonEntity wagon ? wagon.inTeam(horse) : pulled.harness() == horse)
                && !(pulled instanceof CartEntity cart && cart.isCaravan())) {
            if (!player.level().isClientSide()) pulled.release(horse, player);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.is(Items.WATER_BUCKET) || held.is(BronzeTools.BRONZE_WATER_BUCKET.get())) {
            if (!player.level().isClientSide()) water(horse, player, held);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Server: the horse drinks the bucket (iron or bronze). */
    static void water(AbstractHorse horse, Player player, ItemStack bucket) {
        boolean tired = stamina(horse) < TIRED;
        setStamina(horse, stamina(horse) + BUCKET);
        horse.playSound(SoundEvents.GENERIC_DRINK.value(), 1.0F, 0.8F);
        player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemUtils.createFilledResult(bucket, player,
                new ItemStack(bucket.is(Items.WATER_BUCKET) ? Items.BUCKET : BronzeTools.BRONZE_BUCKET.get())));
        if (tired && player instanceof ServerPlayer sp) Kettle.award(sp, WATER_ADVANCEMENT);
    }

    /** Server: {@code player} hangs the body on his shoulders on {@code horse}'s first free side. False: it can't take one. */
    static boolean sling(Player player, CarcassEntity carcass, AbstractHorse horse) {
        if (carcass.isLarge() || !horse.isTamed() || horse.isBaby()) return false;
        if (player.level().isClientSide()) return true;
        int used = 0;
        for (CarcassEntity c : sideCarcasses(horse)) used |= 1 << c.slungSide();
        if (used == 3) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.horse.sides_full"));
            return true;
        }
        carcass.slingOn(horse, (used & 1) == 0 ? 0 : 1);
        horse.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.7F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.horse.slung"));
        if (used != 0 && player instanceof ServerPlayer sp) Kettle.award(sp, PACK_ADVANCEMENT);
        return true;
    }

    private HorseWork() {}
}
