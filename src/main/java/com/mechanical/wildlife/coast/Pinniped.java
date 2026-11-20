package com.mechanical.wildlife.coast;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Seals and walruses: clumsy on land, fast swimmers that dive and hold their breath for minutes. They haul out on the
 * shore (or ice) to rest by day and sleep there at night, go back into the water now and then, and when a threat comes
 * near on land they make for the water ({@link EscapeToWaterGoal}); once in, the shared fear goal (AnimalFear) carries
 * them off underwater. They surface to breathe.
 */
public abstract class Pinniped extends WildAnimal {
    /** Five minutes under water. */
    private static final int AIR = 6000;
    private static final int SHORE_SEARCH = 12;

    protected Pinniped(EntityType<? extends Pinniped> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0F);
        moveControl = new PinnipedMoveControl(this);
    }

    /** Swimming speed as a multiple of the (slow) land speed. */
    protected abstract float swimFactor();

    /** Does it make for the water when a threat comes near on land? */
    protected boolean escapesToWater() {
        return true;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public int getMaxAirSupply() {
        return AIR;
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        moveRelative(0.1F, input);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.9));
        // idle in the water it sinks slowly (rests on the bottom) and comes up again to breathe
        if (getNavigation().isDone()) setDeltaMovement(getDeltaMovement().add(0.0, getAirSupply() < AIR / 3 ? 0.02 : -0.004, 0.0));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // they dive (no floating at the surface) and the land stroll would always lead out of the water
        goalSelector.removeAllGoals(g -> g instanceof FloatGoal || g instanceof WaterAvoidingRandomStrollGoal);
        goalSelector.addGoal(0, new EscapeToWaterGoal(this));
        goalSelector.addGoal(1, new BreatheGoal(this));
        goalSelector.addGoal(6, new ChangeElementGoal(this));
        goalSelector.addGoal(7, new RandomSwimmingGoal(this, 1.0, 30) {
            @Override
            public boolean canUse() {
                return isInWater() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.7, 160) {
            @Override
            public boolean canUse() {
                return !isInWater() && super.canUse();
            }
        });
    }

    /** A dry shore block to haul out on (sand, stone, ice ... with air above), near water; null if none. */
    BlockPos findShore(int range) {
        BlockPos at = blockPosition();
        for (int i = 0; i < 24; i++) {
            BlockPos p = at.offset(random.nextInt(range * 2 + 1) - range, random.nextInt(7) - 3, random.nextInt(range * 2 + 1) - range);
            if (!level().hasChunkAt(p)) continue;
            if (level().getBlockState(p).isAir() && level().getFluidState(p.below()).isEmpty() && level().getBlockState(p.below()).isSolid()
                    && CoastFauna.isShore(level().getBlockState(p.below())) && nearWater(p)) return p;
        }
        return null;
    }

    private boolean nearWater(BlockPos p) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-3, -2, -3), p.offset(3, 0, 3))) {
            if (level().getFluidState(q).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    /** Open water to slip into: a water block with water above or air above (not a puddle under ice); null if none. */
    BlockPos findWater(int range) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos at = blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-range, -3, -range), at.offset(range, 2, range))) {
            if (!level().getFluidState(q).is(FluidTags.WATER) || !level().getFluidState(q.below()).is(FluidTags.WATER)) continue;
            double d = q.distSqr(at);
            if (d < bestDist) {
                bestDist = d;
                best = q.immutable();
            }
        }
        return best;
    }

    /** Swims fast, waddles slowly; steers in 3D under water (like the turtle). */
    static class PinnipedMoveControl extends MoveControl<Pinniped> {
        PinnipedMoveControl(Pinniped mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO || mob.getNavigation().isDone()) {
                mob.setSpeed(0.0F);
                if (!mob.isInWater()) super.tick(); // jumping and strafing as usual on land
                return;
            }
            if (!mob.isInWater()) {
                super.tick();
                return;
            }
            double dx = wantedX - mob.getX(), dy = wantedY - mob.getY(), dz = wantedZ - mob.getZ();
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 1.0E-5) {
                mob.setSpeed(0.0F);
                return;
            }
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            mob.setYRot(rotlerp(mob.getYRot(), yaw, 30.0F));
            mob.yBodyRot = mob.getYRot();
            float target = (float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * mob.swimFactor());
            mob.setSpeed(Mth.lerp(0.15F, mob.getSpeed(), target));
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0, mob.getSpeed() * dy / d * 0.12, 0.0));
        }
    }

    /** On land with a threat close by: hurry into the nearest open water. */
    static class EscapeToWaterGoal extends Goal {
        private static final double NOTICE = 10, NOTICE_SNEAK = 4;
        private final Pinniped mob;
        private BlockPos water;

        EscapeToWaterGoal(Pinniped mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean threatened() {
            if (AnimalFear.isPanicking(mob)) return true;
            Player p = mob.level().getNearestPlayer(mob, NOTICE * mob.fearMultiplier());
            if (p == null || p.isCreative() || p.isSpectator()) return false;
            return mob.distanceTo(p) < (p.isCrouching() ? NOTICE_SNEAK : NOTICE) * mob.fearMultiplier() && mob.hasLineOfSight(p);
        }

        @Override
        public boolean canUse() {
            if (mob.isInWater() || !mob.escapesToWater() || mob.getTarget() != null || mob.isDozing() && !AnimalFear.isPanicking(mob)) return false;
            if ((mob.tickCount + mob.getId()) % 4 != 0 || !threatened()) return false;
            water = mob.findWater(SHORE_SEARCH);
            return water != null;
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.8);
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.isInWater() && !mob.getNavigation().isDone();
        }
    }

    /** Short of air under water: straight up to the surface. */
    static class BreatheGoal extends Goal {
        private final Pinniped mob;

        BreatheGoal(Pinniped mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.isUnderWater() && mob.getAirSupply() < AIR / 4;
        }

        @Override
        public boolean canContinueToUse() {
            return mob.isUnderWater() && mob.getAirSupply() < AIR / 2;
        }

        @Override
        public void tick() {
            BlockPos up = mob.blockPosition();
            for (int i = 0; i < 24 && mob.level().getFluidState(up).is(FluidTags.WATER); i++) up = up.above();
            mob.getNavigation().moveTo(mob.getX(), up.getY(), mob.getZ(), 1.4);
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0, 0.02, 0.0));
        }
    }

    /** Now and then: haul out onto the shore to rest (mostly by day), or slip back into the water to fish. */
    static class ChangeElementGoal extends Goal {
        private final Pinniped mob;
        private BlockPos target;

        ChangeElementGoal(Pinniped mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.isDozing() || mob.getTarget() != null || AnimalFear.isPanicking(mob)) return false;
            if (mob.isInWater()) {
                // they come out to rest by day, and to sleep at night
                if (mob.getRandom().nextInt(mob.level().isBrightOutside() ? 300 : 120) != 0) return false;
                target = mob.findShore(SHORE_SEARCH);
            } else {
                if (mob.getRandom().nextInt(mob.level().isBrightOutside() ? 700 : 4000) != 0) return false;
                target = mob.findWater(SHORE_SEARCH);
            }
            return target != null;
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.getNavigation().isDone();
        }
    }
}
