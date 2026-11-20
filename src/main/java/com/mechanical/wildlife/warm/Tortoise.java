package com.mechanical.wildlife.warm;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tortoise: a slow, high-domed land tortoise of the desert, savanna and badlands, alone or in twos. It can't run, so it
 * doesn't flee: when a player comes close (sneaking: only right up to it) or anything hurts it, it pulls head, legs and
 * tail into its shell and sits tight for a while ({@link #HIDE_TICKS}). Withdrawn, the shell takes most of a blow
 * ({@link #SHELL_BLOCKS}); only crushing, fire and the like get through. Butchered, the shell comes off first.
 */
public class Tortoise extends WildAnimal {
    private static final EntityDataAccessor<Integer> HIDING = SynchedEntityData.defineId(Tortoise.class, EntityDataSerializers.INT);
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/tortoise.png");
    private static final int HIDE_TICKS = 160, WITHDRAW_TICKS = 8;
    /** Share of the damage the closed shell keeps off. */
    private static final float SHELL_BLOCKS = 0.8F;
    private static final double SHY_RANGE = 4.0, SHY_RANGE_SNEAKING = 1.5;

    public Tortoise(EntityType<? extends Tortoise> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.08)
                .add(Attributes.ARMOR, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDING, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // beats AnimalFear's flee goal (added at 0 on join): a tortoise hides, it doesn't run
        goalSelector.addGoal(-1, new HideGoal());
    }

    public boolean isHiding() {
        return entityData.get(HIDING) > 0;
    }

    /** 0..1: how far head and legs are drawn into the shell, for the model. */
    public float shellAmount(float partialTick) {
        int t = entityData.get(HIDING);
        if (t <= 0) return 0;
        // draws in quickly, comes out slowly over the last WITHDRAW_TICKS * 2
        float out = (t - partialTick) / (WITHDRAW_TICKS * 2.0F);
        return Math.clamp(Math.min(out, 1.0F), 0.0F, 1.0F);
    }

    private void hide() {
        if (!isHiding()) playSound(SoundEvents.TURTLE_SHAMBLE, 0.8F, 0.6F);
        entityData.set(HIDING, HIDE_TICKS);
        getNavigation().stop();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        int t = entityData.get(HIDING);
        if (t > 0) {
            Player near = level().getNearestPlayer(this, SHY_RANGE);
            // stays in while someone stands over it
            if (near != null && !near.isSpectator() && !near.isCreative() && t < HIDE_TICKS / 2) entityData.set(HIDING, HIDE_TICKS / 2);
            else entityData.set(HIDING, t - 1);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean shellHolds = isHiding() && !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.IS_FIRE)
                && !source.is(DamageTypeTags.IS_FALL);
        if (shellHolds) {
            damage *= 1.0F - SHELL_BLOCKS;
            playSound(SoundEvents.TURTLE_SHAMBLE, 1.0F, 1.4F);
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive()) hide();
        return hurt;
    }

    @Override
    public boolean isPushable() {
        return !isHiding() && super.isPushable();
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.35F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.TURTLE_SHAMBLE, 0.15F, 0.8F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    /** Pulls in and stays put while a player is close or it has just been hurt or scared. */
    class HideGoal extends Goal {
        HideGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean threatened() {
            Player near = level().getNearestPlayer(Tortoise.this, SHY_RANGE);
            boolean player = near != null && !near.isSpectator() && !near.isCreative()
                    && (!near.isShiftKeyDown() || distanceTo(near) < SHY_RANGE_SNEAKING);
            return player || AnimalFear.isPanicking(Tortoise.this);
        }

        @Override
        public boolean canUse() {
            if (threatened()) hide();
            return isHiding();
        }

        @Override
        public boolean canContinueToUse() {
            return isHiding();
        }

        @Override
        public void tick() {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        }
    }
}
