package com.mechanical.wildlife.mountain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chamois: small, light goat-antelope of mountain meadows and slopes above the trees, in groups of 2-4. Very agile and
 * skittish: notices you from far off and bounds away uphill, up rock faces where you can't follow ({@link MountainBovid}).
 * Both sexes have short black horns that rise straight and hook back at the tip; a white face with a dark band through
 * the eye. Its soft hide becomes Chamois Leather.
 */
public class Chamois extends MountainBovid {
    private static final Identifier ADULT = Ibex.tex("chamois"), KID = Ibex.tex("chamois_kid");

    public Chamois(EntityType<? extends Chamois> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.SAFE_FALL_DISTANCE, 10.0);
    }

    @Override
    protected float maleChance() {
        return 0.5F;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? KID : ADULT;
    }

    /** Both sexes carry the hooked horns; kids only nubs. */
    @Override
    public boolean showsHorns() {
        return !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public double fearMultiplier() {
        return 1.6;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.1F, 1.2F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.5F : 1.2F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 360;
    }
}
