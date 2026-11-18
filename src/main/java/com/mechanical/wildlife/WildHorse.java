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

/** Stocky dun wild horse (tarpan / Przewalski type) in bands on the open plains. Very skittish and fast; never fights. */
public class WildHorse extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse.png");
    private static final Identifier FOAL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse_foal.png");

    public WildHorse(EntityType<? extends WildHorse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? FOAL_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    public double fearMultiplier() {
        return 1.8;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBaby() ? SoundEvents.HORSE_AMBIENT_BABY : SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isBaby() ? SoundEvents.HORSE_HURT_BABY : SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isBaby() ? SoundEvents.HORSE_DEATH_BABY : SoundEvents.HORSE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(isBaby() ? SoundEvents.HORSE_STEP_BABY : SoundEvents.HORSE_STEP, 0.15F, 1.0F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400; // the occasional whinny across the plain, not constant chatter
    }
}
