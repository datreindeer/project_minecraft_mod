package com.mechanical.wildlife;

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

/** Small, very skittish forest deer that keeps in little herds. */
public class RoeDeer extends WildAnimal {
    private static final Identifier BABY_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/roe_deer_fawn.png");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/roe_deer.png");

    public RoeDeer(EntityType<? extends RoeDeer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.35F;
    }

    @Override
    public double fearMultiplier() {
        return 1.5;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // deer are quiet
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
        return super.getVoicePitch() * 1.3F;
    }
}
