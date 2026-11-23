package com.mechanical.wildlife.temperate;

import java.util.EnumSet;

import com.mechanical.wildlife.warm.Tortoise;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

/**
 * European pond turtle: a small, dark, yellow-speckled freshwater turtle of fens, swamps and slow rivers. A good swimmer.
 * On sunny days it climbs out onto a log by the water and basks there ({@link BaskGoal}); when someone comes near it
 * slides off into the water ({@link SlipAwayGoal}), and only pulls into its shell like the tortoise ({@link Tortoise})
 * when there is no water to slip into. Butchered, the shell comes off first: a shell that makes bowls.
 */
public class PondTurtle extends Tortoise {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/temperate/pond_turtle.png");
    static final int BASK_SEARCH = 10, WATER_SEARCH = 8;
    static final double SHY = 6.0, SHY_SNEAKING = 2.5;

    public PondTurtle(EntityType<? extends PondTurtle> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Tortoise.createAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.12).add(Attributes.ARMOR, 4.0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(-2, new SlipAwayGoal()); // before the shell (HideGoal, -1): into the water if it can
        goalSelector.addGoal(5, new BaskGoal());
    }

    /** A log (or a stripped one) with water right beside it and open air above: a basking spot. */
    static boolean baskingSpot(LevelReader level, BlockPos log) {
        if (!level.getBlockState(log).is(BlockTags.LOGS) || !level.getBlockState(log.above()).isAir()) return false;
        for (BlockPos p : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 0, 1))) {
            if (level.getFluidState(p).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    boolean threatened() {
        Player near = level().getNearestPlayer(this, SHY);
        return near != null && !near.isCreative() && !near.isSpectator() && (!near.isShiftKeyDown() || distanceTo(near) < SHY_SNEAKING);
    }

    /** Nearest water block within {@link #WATER_SEARCH}, or null. */
    BlockPos nearestWater() {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-WATER_SEARCH, -2, -WATER_SEARCH), blockPosition().offset(WATER_SEARCH, 1, WATER_SEARCH))) {
            if (level().getFluidState(p).is(FluidTags.WATER) && p.distSqr(blockPosition()) < bestDist) {
                bestDist = p.distSqr(blockPosition());
                best = p.immutable();
            }
        }
        return best;
    }

    /** Disturbed on land with water close by: plops into it (and swims off underwater) instead of hiding in the shell. */
    class SlipAwayGoal extends Goal {
        private BlockPos water;
        private int ticks;

        SlipAwayGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (isInWater() || !threatened()) return false;
            water = nearestWater();
            return water != null && getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 2.2);
        }

        @Override
        public void start() {
            ticks = 0;
            playSound(net.minecraft.sounds.SoundEvents.TURTLE_SHAMBLE, 0.8F, 1.2F);
        }

        @Override
        public boolean canContinueToUse() {
            return !isInWater() && ++ticks < 100 && !getNavigation().isDone();
        }

        @Override
        public void stop() {
            if (isInWater()) playSound(net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, 0.5F, 1.4F);
        }
    }

    /** On a fine day: walks (or swims) to a log at the water's edge and sits on top of it in the sun for a minute or two. */
    class BaskGoal extends Goal {
        private BlockPos log;
        private int ticks, repath;

        BaskGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || !level().isBrightOutside() || level().isRaining() || random.nextInt(200) != 0) return false;
            log = null;
            for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-BASK_SEARCH, -3, -BASK_SEARCH), blockPosition().offset(BASK_SEARCH, 3, BASK_SEARCH))) {
                if (baskingSpot(level(), p)) {
                    log = p.immutable();
                    break;
                }
            }
            return log != null;
        }

        /** Sends it to bask on {@code log} straight away (tests). */
        void baskOn(BlockPos log) {
            this.log = log;
            start();
        }

        @Override
        public void start() {
            ticks = 1200 + random.nextInt(1200);
            getNavigation().moveTo(log.getX() + 0.5, log.getY() + 1, log.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && level().isBrightOutside() && baskingSpot(level(), log);
        }

        @Override
        public void tick() {
            if (blockPosition().equals(log.above())) getNavigation().stop(); // up on the log: sit still
            else if (distanceToSqr(Vec3.atBottomCenterOf(log.above())) < 2.5) {
                // a path ends a block short of its goal: step the last bit up onto the log
                getMoveControl().setWantedPosition(log.getX() + 0.5, log.getY() + 1, log.getZ() + 0.5, 1.0);
                if (horizontalCollision && onGround()) getJumpControl().jump();
            } else if (++repath % 40 == 0 || getNavigation().isDone()) {
                getNavigation().moveTo(log.getX() + 0.5, log.getY() + 1, log.getZ() + 0.5, 1.0);
            }
        }
    }
}
