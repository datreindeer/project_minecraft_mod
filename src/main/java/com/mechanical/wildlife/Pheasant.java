package com.mechanical.wildlife;

import com.mechanical.AnimalFear;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ground bird that sits tight in cover and bursts into the air when a player comes close or it panics, gliding away. */
public class Pheasant extends WildAnimal {
    private static final Identifier BABY_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/pheasant_chick.png");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/pheasant.png");
    private static final double FLUSH_DISTANCE = 5;
    private int flushCooldown;
    // client-side wing animation
    private float flap, flapO;

    public Pheasant(EntityType<? extends Pheasant> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 7.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    /** Wing-flap phase (radians) for the model. */
    public float flapPhase(float partialTick) {
        return flapO + (flap - flapO) * partialTick;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        flapO = flap;
        if (!onGround()) flap += 1.2F;
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < 0) setDeltaMovement(v.x, v.y * 0.6, v.z); // glide down
        if (level().isClientSide()) return;
        if (flushCooldown > 0) flushCooldown--;
        if (!onGround() || flushCooldown > 0) return;
        Player near = level().getNearestPlayer(this, FLUSH_DISTANCE);
        boolean threatened = near != null && !near.isCreative() && !near.isSpectator() && !near.isCrouching();
        if (threatened || AnimalFear.isPanicking(this)) {
            // flush: burst up and away from the player
            Vec3 away = near != null ? position().subtract(near.position()).multiply(1, 0, 1).normalize() : new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize();
            setDeltaMovement(away.x * 0.6, 0.75, away.z * 0.6);
            playSound(SoundEvents.PARROT_FLY, 1.0F, 0.8F);
            flushCooldown = 80;
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }
}
