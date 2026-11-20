package com.mechanical.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Fishers walk out to a shore round the camp (through the gate of a walled one) and fish for a while: cast (swing), the line lands with a splash a few blocks
 * out, and now and then a fish bites (bubbles, splash). Visual only: no bobber entity, no catch.
 */
class FishGoal extends Goal {
    private static final int RANGE = 16;
    private final Tribesperson mob;
    BlockPos stand, spot;
    private int ticks, duration;
    private boolean arrived;

    FishGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static boolean openWater(ServerLevel level, BlockPos p) {
        return level.getFluidState(p).is(FluidTags.WATER) && level.getBlockState(p.above()).isAir();
    }

    /** Dry ground to stand on next to water, with water a couple of blocks out in that direction; beyond the camp. */
    boolean findSpot(ServerLevel level) {
        BlockPos c = mob.workCenter();
        int camp = mob.campRadius(), r = camp + RANGE;
        for (int i = 0; i < 60; i++) {
            int dx = mob.getRandom().nextInt(r * 2 + 1) - r, dz = mob.getRandom().nextInt(r * 2 + 1) - r;
            if (dx * dx + dz * dz <= camp * camp) continue;
            BlockPos col = c.offset(dx, 0, dz);
            if (!level.isLoaded(col)) continue;
            BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col); // the air over the ground (or water)
            if (Math.abs(p.getY() - c.getY()) > 12 || !level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()
                    || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (openWater(level, p.relative(d).below()) && openWater(level, p.relative(d, 3).below())) {
                    stand = p.immutable();
                    spot = p.relative(d, 3).below();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        return mob.canWork(TribeRole.FISHER) && mob.getRandom().nextInt(60) == 0 && findSpot((ServerLevel) mob.level());
    }

    @Override
    public void start() {
        ticks = 0;
        arrived = false;
        duration = 600 + mob.getRandom().nextInt(900);
        mob.walkTo(stand, 0.7);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < duration && mob.getTarget() == null && !mob.level().isDarkOutside();
    }

    @Override
    public void tick() {
        ticks++;
        ServerLevel level = (ServerLevel) mob.level();
        Vec3 bob = Vec3.atBottomCenterOf(spot.above()).add(0, -0.1, 0);
        if (mob.distanceToSqr(Vec3.atBottomCenterOf(stand)) > 1.5 * 1.5) {
            if (mob.getNavigation().isDone()) {
                if (ticks > 900) duration = 0; // can't get there
                else mob.walkTo(stand, 0.7);
            }
            return;
        }
        mob.getNavigation().stop();
        if (!arrived) { // the fishing time starts on the shore, however long the walk
            arrived = true;
            duration += ticks;
        }
        mob.getLookControl().setLookAt(bob);
        int t = ticks % 240;
        if (t == 1) {
            mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT); // cast
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.6F);
        } else if (t == 12) {
            level.sendParticles(ParticleTypes.SPLASH, bob.x, bob.y, bob.z, 6, 0.1, 0.0, 0.1, 0.0);
        } else if (t > 12 && t % 30 == 0) {
            level.sendParticles(ParticleTypes.FISHING, bob.x, bob.y, bob.z, 1, 0.05, 0.0, 0.05, 0.0);
        }
        if (t == 200 && mob.getRandom().nextInt(3) == 0) { // a bite
            level.sendParticles(ParticleTypes.BUBBLE, bob.x, bob.y - 0.2, bob.z, 8, 0.15, 0.1, 0.15, 0.05);
            level.sendParticles(ParticleTypes.SPLASH, bob.x, bob.y, bob.z, 12, 0.2, 0.0, 0.2, 0.0);
            level.playSound(null, bob.x, bob.y, bob.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5F, 1.0F);
            mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
