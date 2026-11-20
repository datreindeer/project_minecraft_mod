package com.mechanical.wildlife;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Lone, fast hare. Bounds along when running and jinks sideways now and then while fleeing, making it hard to hit. */
public class Hare extends WildAnimal {
    private static final Identifier BABY_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/hare_leveret.png");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/hare.png");

    public Hare(EntityType<? extends Hare> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.34);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.15F;
    }

    @Override
    public double fearMultiplier() {
        return 1.2;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !onGround()) return;
        Vec3 v = getDeltaMovement();
        double speed = v.horizontalDistance();
        if (speed > 0.15 && random.nextInt(4) == 0) {
            // bounding run, with a sideways jink every so often
            Vec3 side = random.nextInt(3) == 0 ? new Vec3(-v.z, 0, v.x).normalize().scale((random.nextBoolean() ? 1 : -1) * 0.35) : Vec3.ZERO;
            setDeltaMovement(v.x + side.x, 0.32, v.z + side.z);
        }
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
}
