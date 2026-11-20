package com.mechanical.wildlife.meadow;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.CatSoundVariants;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for the snakes (Adder, Grass Snake). A snake is no mammal, but it lives like the other small wild animals here
 * (an {@link net.minecraft.world.entity.animal.Animal}: it is hunted, snared, butchered and shares their fear and sleep),
 * just without young and herd: it is always alone. It basks coiled in the sun on clear days ({@link #isCoiled()}) and
 * lies coiled through the night. Leaves a light carcass (snakeskin, meat).
 */
public abstract class Snake extends WildAnimal {
    /** Lying coiled: basking, asleep, or (grass snake) playing dead. Synced for the model. */
    private static final EntityDataAccessor<Boolean> BASKING = SynchedEntityData.defineId(Snake.class, EntityDataSerializers.BOOLEAN);

    protected Snake(EntityType<? extends Snake> type, Level level) {
        super(type, level);
    }

    /** Snakes keep vanilla water handling (the grass snake goes in readily, see GrassSnake). */
    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.OWN;
    }

    public boolean isCoiled() {
        return entityData.get(BASKING) || isDozing();
    }

    public boolean isBasking() {
        return entityData.get(BASKING);
    }

    protected void setBasking(boolean basking) {
        entityData.set(BASKING, basking);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BASKING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        MeadowFauna.removeGoals(goalSelector, "FollowHerdGoal", "FollowParentGoal");
        goalSelector.addGoal(5, new BaskGoal(this));
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    /** Snakes slide over the ground: no footsteps. */
    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, BlockState state) {}

    protected static SoundEvent hiss() {
        return SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC).adultSounds().hissSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    /** In the sun on a clear day, it lies coiled and still for a minute or so. */
    static class BaskGoal extends Goal {
        private final Snake snake;
        private int time;

        BaskGoal(Snake snake) {
            this.snake = snake;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean sunny() {
            Level level = snake.level();
            return level.isBrightOutside() && !level.isRaining() && level.canSeeSky(snake.blockPosition()) && !snake.isInWater();
        }

        @Override
        public boolean canUse() {
            return snake.onGround() && snake.getTarget() == null && !AnimalFear.isPanicking(snake) && sunny()
                    && snake.getRandom().nextInt(120) == 0;
        }

        @Override
        public void start() {
            time = 600 + snake.getRandom().nextInt(1200);
            snake.getNavigation().stop();
            snake.setBasking(true);
        }

        @Override
        public boolean canContinueToUse() {
            return time > 0 && snake.getTarget() == null && !AnimalFear.isPanicking(snake) && snake.hurtTime == 0 && sunny();
        }

        @Override
        public void tick() {
            time -= 2; // goals tick every other game tick
            snake.getNavigation().stop();
        }

        @Override
        public void stop() {
            snake.setBasking(false);
        }
    }
}
