package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Animals and monsters (not skeletons, slimes and the like) bleed: blood sprays when hit, and below half health they drip, leave a trail of blood on the ground and
 * slowly lose health (never below one heart, so the last blow is still yours).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Blood {
    private static final float BLEED_BELOW = 0.5F;       // fraction of max health
    private static final float BLEED_DAMAGE = 1.0F;      // half a heart
    private static final float BLEED_FLOOR = 2.0F;       // one heart: bleeding stops here
    private static final int BLEED_INTERVAL = 100, TRAIL_INTERVAL = 15;

    /** Mobs without blood: skeletons, slimes, blazes, creepers ... (entity type tag {@code mechanical:bloodless}). */
    public static final TagKey<EntityType<?>> BLOODLESS = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "bloodless"));

    /** Animals, monsters and tribespeople bleed, unless they have no blood. */
    public static boolean bleeds(LivingEntity entity) {
        return (entity instanceof Animal || entity instanceof Enemy || entity instanceof com.mechanical.people.Tribesperson) && !entity.getType().builtInRegistryHolder().is(BLOODLESS);
    }

    public static boolean isBleeding(LivingEntity entity) {
        if (entity instanceof com.mechanical.wildlife.WildAnimal wild && wild.bloodOnlyWhenBleeding()) {
            return entity.isAlive() && entity.getHealth() < entity.getMaxHealth() * wild.bleedThreshold();
        }
        float below = entity instanceof com.mechanical.wildlife.WildAnimal wild ? wild.bleedThreshold() : BLEED_BELOW;
        return entity.isAlive() && (entity.getHealth() < entity.getMaxHealth() * below || ArrowWounds.count(entity) > 0);
    }

    /** Feeding a bleeding animal its food calms it and stops the bleeding: arrows come out and it heals to just above the bleed line. */
    @SubscribeEvent
    static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Animal animal) || !(animal.level() instanceof ServerLevel level) || !bleeds(animal) || !isBleeding(animal)) return;
        ItemStack food = event.getItemStack();
        if (!animal.isFood(food)) return;
        ArrowWounds.pullOut(animal, level);
        float below = animal instanceof com.mechanical.wildlife.WildAnimal wild ? wild.bleedThreshold() : BLEED_BELOW;
        animal.setHealth(Math.max(animal.getHealth(), (float) Math.ceil(animal.getMaxHealth() * below)));
        food.consume(1, event.getEntity());
        level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight(), animal.getZ(), 3, 0.3, 0.2, 0.3, 0);
        level.playSound(null, animal, SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!bleeds(entity) || !(entity.level() instanceof ServerLevel level)) return;
        if (entity instanceof com.mechanical.wildlife.WildAnimal wild && wild.bloodOnlyWhenBleeding() && !isBleeding(entity)) return;
        int amount = Math.min(20, 3 + Math.round(event.getOriginalDamage() * 3));
        spray(level, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), amount);
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity animal) || !bleeds(animal) || !(animal.level() instanceof ServerLevel level) || !isBleeding(animal)) return;
        if (animal.tickCount % 5 == 0) {
            drip(level, animal.getX() + (level.getRandom().nextDouble() - 0.5) * 0.4, animal.getY() + animal.getBbHeight() * 0.4,
                    animal.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.4);
        }
        if (animal.tickCount % TRAIL_INTERVAL == 0 && animal.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) leaveTrail(level, animal);
        // arrow wounds bleed to death on their own (ArrowWounds); other wounds stop at one heart
        if (animal.tickCount % BLEED_INTERVAL == 0 && animal.getHealth() > BLEED_FLOOR && ArrowWounds.count(animal) == 0) {
            animal.hurtServer(level, level.damageSources().generic(), Math.min(BLEED_DAMAGE, animal.getHealth() - BLEED_FLOOR));
        }
    }

    private static void leaveTrail(ServerLevel level, LivingEntity animal) {
        place(level, animal.blockPosition());
    }

    /** Drops of blood flying out from a point (hits, butchering). */
    public static void spray(ServerLevel level, double x, double y, double z, int count) {
        RandomSource r = level.getRandom();
        for (int i = 0; i < count; i++) {
            // count 0 makes sendParticles use the offset as the velocity of one particle
            level.sendParticles(Mechanical.BLOOD_DROP.get(), x, y, z, 0, (r.nextDouble() - 0.5) * 0.3, 0.1 + r.nextDouble() * 0.15, (r.nextDouble() - 0.5) * 0.3, 1.0);
        }
    }

    /** One drop falling straight down (bleeding). */
    public static void drip(ServerLevel level, double x, double y, double z) {
        level.sendParticles(Mechanical.BLOOD_DROP.get(), x, y, z, 0, 0, 0, 0, 0);
    }

    /** {@code count} blood stains on the ground within {@code radius} blocks of {@code center}. */
    public static void splatter(ServerLevel level, BlockPos center, int radius, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos pos = center.offset(level.getRandom().nextInt(radius * 2 + 1) - radius, 0, level.getRandom().nextInt(radius * 2 + 1) - radius);
            // settle on the ground: try this level, one up and one down
            for (int dy : new int[] {0, 1, -1}) {
                if (place(level, pos.above(dy))) break;
            }
        }
    }

    private static boolean place(ServerLevel level, BlockPos pos) {
        return place(level, pos, 2);
    }

    /** Blood on blood makes a bigger pool; a full pool runs over to a neighbour (at most {@code overflow} blocks on). */
    private static boolean place(ServerLevel level, BlockPos pos, int overflow) {
        BlockState blood = Mechanical.BLOOD.get().defaultBlockState();
        BlockState here = level.getBlockState(pos);
        if (here.is(blood.getBlock())) {
            int amount = here.getValue(BloodBlock.AMOUNT);
            if (amount < BloodBlock.MAX_AMOUNT) {
                level.setBlock(pos, here.setValue(BloodBlock.AMOUNT, amount + 1), Block.UPDATE_ALL);
                return true;
            }
            if (overflow <= 0) return false;
            // runs over the edge: onto a neighbour at the same height, or down a step
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());
            BlockPos next = pos.relative(dir);
            return place(level, next, overflow - 1) || place(level, next.below(), overflow - 1);
        }
        // stains the ground under grass, leaf litter, snow layers and the like; never in water
        if (!here.canBeReplaced() || !here.getFluidState().isEmpty() || !blood.canSurvive(level, pos)) return false;
        level.setBlock(pos, blood, Block.UPDATE_ALL);
        return true;
    }

    private Blood() {}
}
