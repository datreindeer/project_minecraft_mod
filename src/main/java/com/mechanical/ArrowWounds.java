package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Arrows stick in animals. Every stuck arrow makes the animal bleed: it keeps running, sprays more and more blood and
 * loses health faster and faster until it dies. Then the arrows fall out next to the body; poor arrows may have broken
 * (a feather is left instead).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class ArrowWounds {
    /** The arrows stuck in an animal (one stack of 1 per arrow). */
    public static final Supplier<AttachmentType<List<ItemStack>>> STUCK = Mechanical.ATTACHMENTS.register("stuck_arrows", () -> AttachmentType
            .<List<ItemStack>>builder(() -> List.of())
            .serialize(ItemStack.CODEC.listOf().fieldOf("arrows"))
            .build());
    /** Set by a real wound (Blood#wounds), cleared when stanched or healed: low health from anything else doesn't bleed (Blood#isBleeding). */
    public static final Supplier<AttachmentType<Boolean>> WOUNDED = Mechanical.ATTACHMENTS.register("wounded", () -> AttachmentType
            .builder(() -> false)
            .serialize(com.mojang.serialization.Codec.BOOL.fieldOf("wounded"))
            .build());

    private static final float BLEED_PER_ARROW = 1.0F;
    /** Ticks between bleed hits: starts slow, gets faster the longer the wound bleeds. */
    private static final int BLEED_START_INTERVAL = 60, BLEED_MIN_INTERVAL = 20;
    /** Server-side: when each animal got its first arrow. */
    private static final Map<LivingEntity, Long> WOUNDED_AT = new WeakHashMap<>();

    public static void init() {}

    /** Arrows stick in animals and in tribespeople. */
    private static boolean stick(LivingEntity entity) {
        return entity instanceof Animal || entity instanceof com.mechanical.people.Tribesperson;
    }

    public static int count(LivingEntity entity) {
        return entity.getData(STUCK).size();
    }

    @SubscribeEvent
    static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!stick(entity) || entity.level().isClientSide()
                || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || arrow.pickup != AbstractArrow.Pickup.ALLOWED) return;
        List<ItemStack> stuck = new ArrayList<>(entity.getData(STUCK));
        stuck.add(arrow.getPickupItemStackOrigin().copyWithCount(1));
        entity.setData(STUCK, List.copyOf(stuck));
        WOUNDED_AT.putIfAbsent(entity, entity.level().getGameTime());
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity animal) || !stick(animal) || !(animal.level() instanceof ServerLevel level) || !animal.isAlive()) return;
        List<ItemStack> stuck = animal.getData(STUCK);
        if (stuck.isEmpty()) return;
        // vanilla lets stuck arrows fall out over time; ours stay until death
        if (animal.getArrowCount() != stuck.size()) animal.setArrowCount(stuck.size());
        long bleeding = level.getGameTime() - WOUNDED_AT.computeIfAbsent(animal, a -> level.getGameTime());
        if (animal.tickCount % 20 == 0) {
            Player near = level.getNearestPlayer(animal, 48);
            AnimalFear.panic(animal, near != null ? near.position() : animal.position());
        }
        boolean hidesBlood = animal instanceof com.mechanical.wildlife.WildAnimal wild && wild.bloodOnlyWhenBleeding() && !Blood.isBleeding(animal);
        if (animal.tickCount % 10 == 0 && !hidesBlood) {
            int drops = (int) Math.min(14, 1 + bleeding / 80) * stuck.size();
            Blood.spray(level, animal.getX(), animal.getY() + animal.getBbHeight() * 0.6, animal.getZ(), Math.min(drops, 24));
        }
        int interval = (int) Math.max(BLEED_MIN_INTERVAL, BLEED_START_INTERVAL - bleeding / 20);
        if (animal.tickCount % interval == 0) {
            float damage = 0;
            for (ItemStack arrow : stuck) damage += arrow.is(Mechanical.BARBED_ARROW.get()) ? BLEED_PER_ARROW * 2 : BLEED_PER_ARROW;
            // people bind their wounds: arrows bleed them down to one heart, never to death
            if (animal instanceof com.mechanical.people.Tribesperson) damage = Math.min(damage, animal.getHealth() - 2.0F);
            if (damage > 0) animal.hurtServer(level, level.damageSources().generic(), damage);
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) pullOut(entity, level);
    }

    /** The stuck arrows fall out next to it (poor ones may have broken) and the wound stops bleeding. */
    static void pullOut(LivingEntity entity, ServerLevel level) {
        List<ItemStack> stuck = entity.getData(STUCK);
        if (stuck.isEmpty()) return;
        entity.setArrowCount(0);
        for (ItemStack arrow : stuck) {
            ItemStack drop = level.getRandom().nextFloat() < breakChance(arrow.getItem()) ? new ItemStack(Items.FEATHER) : arrow.copy();
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY() + 0.3, entity.getZ(), drop);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        entity.setData(STUCK, List.of());
        WOUNDED_AT.remove(entity);
    }

    /** Poor arrows break more often. */
    static float breakChance(Item arrow) {
        if (arrow == Mechanical.BONE_ARROW.get()) return 0.5F;
        if (arrow == Mechanical.FIRE_ARROW.get()) return 0.7F;
        if (arrow == Mechanical.BARBED_ARROW.get()) return 0.15F;
        if (arrow == Bronze.BRONZE_ARROW.get() || arrow == Smithing.IRON_BOLT.get()) return 0.1F; // a metal head: the shaft goes before it does
        return 0.3F;
    }

    private ArrowWounds() {}
}
