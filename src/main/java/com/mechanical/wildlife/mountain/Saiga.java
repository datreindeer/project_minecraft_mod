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
 * Saiga: a steppe antelope in herds of 4-8 on plains, savanna and the cold steppe. The most skittish game there is: it
 * notices a walking hunter at ~22 blocks and outruns anything on foot (fast base speed, AnimalFear's panic doubles it), so
 * it takes a bow and patience (or a snare line) to bring one down. The big drooping nose warms and filters the steppe air.
 * Bucks carry ringed, translucent amber horns (Saiga Horn).
 */
public class Saiga extends MountainBovid {
    private static final Identifier BUCK = Ibex.tex("saiga_buck"), DOE = Ibex.tex("saiga_doe"), CALF = Ibex.tex("saiga_calf");

    public Saiga(EntityType<? extends Saiga> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected boolean climbs() {
        return false;
    }

    @Override
    protected float maleChance() {
        return 0.35F;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF : isMale() ? BUCK : DOE;
    }

    @Override
    public boolean showsHorns() {
        return isMale() && !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    public double fearMultiplier() {
        return 2.2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
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
        playSound(SoundEvents.GOAT_STEP, 0.1F, 1.3F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.8F : 1.45F); // a nasal snort, not a camel's groan
    }

    @Override
    public int getAmbientSoundInterval() {
        return 320;
    }
}
