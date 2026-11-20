package com.mechanical.wildlife.coast;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Walrus of the frozen seas: huge herds hauled out on ice floes and cold beaches. Barely afraid of people: it bellows
 * and rears its tusks at anyone who comes close, and when hurt it turns and gores (the whole herd joins in for a hurt
 * calf). Only a badly bleeding walrus flees into the water. Its thick hide needs a tusk-grade knife.
 */
public class Walrus extends Pinniped {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/walrus.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/walrus_calf.png");
    private static final double WARN_RANGE = 5;
    private int warnCooldown;

    public Walrus(EntityType<? extends Walrus> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.MOVEMENT_SPEED, 0.1).add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, false) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
    }

    @Override
    protected boolean escapesToWater() {
        return getTarget() == null && com.mechanical.AnimalFear.isPanicking(this);
    }

    /** A hurt calf brings the grown walruses nearby down on its attacker. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isBaby() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Walrus)) {
            for (Walrus w : level.getEntitiesOfClass(Walrus.class, getBoundingBox().inflate(16), w -> !w.isBaby())) w.setTarget(attacker);
        }
        return hurt;
    }

    /** Someone close who isn't sneaking: it rears its head and bellows a warning. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || isBaby() || isDozing() || --warnCooldown > 0) return;
        Player near = level().getNearestPlayer(this, WARN_RANGE);
        if (near != null && !near.isCreative() && !near.isSpectator() && !near.isCrouching() && getTarget() == null) {
            playSound(SoundEvents.POLAR_BEAR_WARNING, 1.2F, 0.6F);
            getLookControl().setLookAt(near);
            warnCooldown = 80;
        }
    }

    @Override
    protected float swimFactor() {
        return 3.0F;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.2F;
    }

    @Override
    public double fearMultiplier() {
        return 0.4;
    }

    @Override
    public float bleedThreshold() {
        return 0.3F;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true; // the thick hide and blubber soak up the first wounds
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.POLAR_BEAR_AMBIENT;
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
        playSound(SoundEvents.POLAR_BEAR_STEP, 0.3F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.0F : 0.55F);
    }
}
