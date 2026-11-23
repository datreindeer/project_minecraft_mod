package com.mechanical.wildlife.temperate;

import com.mechanical.wildlife.WildAnimal;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Water vole: a plump, dark brown, blunt-faced vole of reedy banks, twice the size of the field vole. At home in the
 * water: swims well and makes for it (and under it) when frightened, with a "plop" as it dives. Prey for foxes and wild
 * cats (TemperateFauna); snared like other small game. Leaves a tiny carcass (Raw Water Vole).
 */
public class WaterVole extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/temperate/water_vole.png");

    public WaterVole(EntityType<? extends WaterVole> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3);
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
    public void aiStep() {
        boolean wasDry = !isInWater();
        super.aiStep();
        if (!level().isClientSide() && wasDry && isInWater() && com.mechanical.AnimalFear.isPanicking(this)) playSound(SoundEvents.GENERIC_SPLASH, 0.4F, 1.8F);
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public double fearMultiplier() {
        return 1.3;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
