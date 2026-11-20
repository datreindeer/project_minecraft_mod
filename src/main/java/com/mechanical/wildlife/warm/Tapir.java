package com.mechanical.wildlife.warm;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tapir: a shy, heavy, dark jungle browser with a short trunk, alone or a mother with her striped calf. Very wary; when
 * frightened it doesn't just run, it makes for the nearest water within {@link #WATER_RANGE} blocks, plunges in and
 * stays there, swimming low, until the danger has passed ({@link WaterRefugeGoal}).
 */
public class Tapir extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/tapir.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/tapir_calf.png");
    private static final int WATER_RANGE = 16;

    public Tapir(EntityType<? extends Tapir> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(-1, new WaterRefugeGoal()); // ahead of AnimalFear's plain flee goal (priority 0)
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 1.6;
    }

    @Override
    protected float babyChance() {
        return 0.4F;
    }

    private static net.minecraft.world.entity.animal.pig.PigSoundVariant.PigSoundSet pig() {
        return SoundEvents.PIG_SOUNDS.get(net.minecraft.world.entity.animal.pig.PigSoundVariants.SoundSet.BIG).adultSounds();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // tapirs whistle rarely; mostly silent browsers
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return pig().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return pig().deathSound().value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(pig().stepSound().value(), 0.15F, 0.7F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.2F : 0.7F);
    }

    /** Water deep enough to hide in: two blocks of it. */
    private boolean refuge(BlockPos pos) {
        return level().getFluidState(pos).is(FluidTags.WATER) && level().getFluidState(pos.below()).is(FluidTags.WATER);
    }

    /** Panicking: runs to the nearest deep water and stays in it while the panic lasts. */
    class WaterRefugeGoal extends Goal {
        private BlockPos water;

        WaterRefugeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!AnimalFear.isPanicking(Tapir.this) || (tickCount + getId()) % 10 != 0) return false;
            BlockPos me = blockPosition();
            if (isInWater()) {
                water = me;
                return true;
            }
            water = null;
            double best = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-WATER_RANGE, -4, -WATER_RANGE), me.offset(WATER_RANGE, 2, WATER_RANGE))) {
                if (!refuge(p)) continue;
                double d = p.distSqr(me);
                if (d < best) {
                    best = d;
                    water = p.immutable();
                }
            }
            return water != null && getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 2.0);
        }

        @Override
        public boolean canContinueToUse() {
            return AnimalFear.isPanicking(Tapir.this) && (isInWater() || !getNavigation().isDone());
        }

        @Override
        public void tick() {
            if (isInWater() && getNavigation().isDone()) {
                // keep low, drifting in the water away from the bank
                setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
            }
        }
    }
}
