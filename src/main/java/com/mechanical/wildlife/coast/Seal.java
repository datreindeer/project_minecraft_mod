package com.mechanical.wildlife.coast;

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
 * Seal of the cold shores: lies about in groups on beaches, stony shores and ice floes, lifts head and tail while it
 * basks, and humps into the water as soon as someone comes near (sneak to get close). Fast and agile in the water.
 * Pups are white.
 */
public class Seal extends Pinniped {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/seal.png");
    private static final Identifier PUP_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/seal_pup.png");

    public Seal(EntityType<? extends Seal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.12).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected float swimFactor() {
        return 3.2F;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? PUP_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    /** Wary: notices you from further off than most game. */
    @Override
    public double fearMultiplier() {
        return 1.3;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.POLAR_BEAR_AMBIENT_BABY;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.15F, 0.8F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.5F : 1.2F);
    }
}
