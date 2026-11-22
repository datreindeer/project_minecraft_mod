package com.mechanical.wildlife.jungle;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Poison Dart Frog: a thumb-sized frog, bright blue-and-black or yellow-and-black, on the jungle floor. It doesn't hide:
 * its colours warn. Its skin is poison: whoever strikes it with bare hands or a hand weapon is poisoned (Poison I, 5 s)
 * through the touch. It hops away when scared. Dead, it leaves Frog Poison: scraped off its back, it tips Darts for the
 * Blowpipe. No carcass (too small).
 */
public class DartFrog extends WildAnimal {
    private static final Identifier BLUE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/poison_dart_frog.png");
    private static final Identifier YELLOW = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/poison_dart_frog_yellow.png");
    static final int TOUCH_POISON = 100;

    public DartFrog(EntityType<? extends DartFrog> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    public Identifier texture() {
        return (getUUID().getLeastSignificantBits() & 1) == 0 ? BLUE : YELLOW;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return 0.5; // bold: its colours keep things off
    }

    /** The touch poisons whoever strikes it by hand (not arrows or darts). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker)
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, TOUCH_POISON, 0), this);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // hops: little jumps whenever it moves, longer ones when scared
        if (!level().isClientSide() && onGround() && getNavigation().isInProgress() && random.nextInt(AnimalFear.isPanicking(this) ? 4 : 10) == 0) {
            Vec3 d = getDeltaMovement();
            setDeltaMovement(d.x * 2.0, 0.32, d.z * 2.0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FROG_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.8F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
