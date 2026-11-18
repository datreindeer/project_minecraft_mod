package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Animals bleed: blood sprays when hit, and below half health they drip, leave a trail of blood on the ground and
 * slowly lose health (never below one heart, so the last blow is still yours).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Blood {
    private static final float BLEED_BELOW = 0.5F;       // fraction of max health
    private static final float BLEED_DAMAGE = 1.0F;      // half a heart
    private static final float BLEED_FLOOR = 2.0F;       // one heart: bleeding stops here
    private static final int BLEED_INTERVAL = 100, TRAIL_INTERVAL = 15;

    public static boolean isBleeding(LivingEntity entity) {
        return entity.isAlive() && entity.getHealth() < entity.getMaxHealth() * BLEED_BELOW;
    }

    @SubscribeEvent
    static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Animal) || !(entity.level() instanceof ServerLevel level)) return;
        int amount = Math.min(20, 3 + Math.round(event.getOriginalDamage() * 3));
        spray(level, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), amount);
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || !(animal.level() instanceof ServerLevel level) || !isBleeding(animal)) return;
        if (animal.tickCount % 5 == 0) {
            drip(level, animal.getX() + (level.getRandom().nextDouble() - 0.5) * 0.4, animal.getY() + animal.getBbHeight() * 0.4,
                    animal.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.4);
        }
        if (animal.tickCount % TRAIL_INTERVAL == 0 && animal.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) leaveTrail(level, animal);
        if (animal.tickCount % BLEED_INTERVAL == 0 && animal.getHealth() > BLEED_FLOOR) {
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
        BlockState blood = Mechanical.BLOOD.get().defaultBlockState();
        BlockState here = level.getBlockState(pos);
        // stains the ground under grass, leaf litter, snow layers and the like; never in water
        if (!here.canBeReplaced() || !here.getFluidState().isEmpty() || here.is(blood.getBlock()) || !blood.canSurvive(level, pos)) return false;
        level.setBlock(pos, blood, Block.UPDATE_ALL);
        return true;
    }

    private Blood() {}
}
