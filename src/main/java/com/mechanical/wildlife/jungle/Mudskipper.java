package com.mechanical.wildlife.jungle;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

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
import net.minecraft.world.phys.Vec3;

/**
 * Mudskipper: a little goggle-eyed fish that lives out on the mangrove mud, walking on its fins. Breathes in water and out
 * of it; scared, it skips away over the mud in long flips. Too small for a carcass: it drops itself, a whole Mudskipper,
 * eaten like a small fish or used as bait (snares, and the fishing hook: mechanical:snare_bait).
 */
public class Mudskipper extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mudskipper.png");

    public Mudskipper(EntityType<? extends Mudskipper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.18);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; // a fish: and it breathes air too
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return 1.3;
    }

    /** Skipping: a fleeing mudskipper flips itself forward in long hops. */
    public boolean skip() {
        if (!onGround()) return false;
        Vec3 d = getDeltaMovement().multiply(1, 0, 1);
        if (d.lengthSqr() < 1.0E-4) d = Vec3.directionFromRotation(0, getYRot()).scale(0.1);
        Vec3 v = d.normalize().scale(0.35);
        setDeltaMovement(v.x, 0.38, v.z);
        playSound(SoundEvents.TROPICAL_FISH_FLOP, 0.5F, 1.3F);
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && AnimalFear.isPanicking(this) && random.nextInt(6) == 0) skip();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.MUD_STEP, 0.15F, 1.6F);
    }
}
