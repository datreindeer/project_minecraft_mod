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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Desert Lizard: a quick, sand-coloured agama of desert, badlands and savanna rocks. By day it basks flat on sand or
 * stone in the sun (sluggish to notice you while it warms up), then darts off at the first fright. Grabbed by a blow
 * that would not kill it, it sheds its tail and escapes: the blow does nothing, a Lizard Tail drops and it runs off
 * fast; the tail grows back in a day ({@link #REGROW_TICKS}). Small enough for a snare.
 */
public class DesertLizard extends WildAnimal {
    private static final EntityDataAccessor<Boolean> TAILLESS = SynchedEntityData.defineId(DesertLizard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BASKING = SynchedEntityData.defineId(DesertLizard.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/desert_lizard.png");
    private static final int REGROW_TICKS = 24000;
    private int regrow;

    public DesertLizard(EntityType<? extends DesertLizard> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.36);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAILLESS, false);
        builder.define(BASKING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof net.minecraft.world.entity.ai.goal.FollowParentGoal); // hatchlings fend for themselves
        goalSelector.addGoal(5, new BaskGoal());
    }

    public boolean isTailless() {
        return entityData.get(TAILLESS);
    }

    public boolean isBasking() {
        return entityData.get(BASKING);
    }

    /** Still cold from basking: notices you later. */
    @Override
    public double fearMultiplier() {
        return isBasking() ? 0.7 : 1.4;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // autotomy: the tail stays in the hand, the lizard gets away (once, and not from a killing blow)
        if (!isTailless() && damage < getHealth() && source.getEntity() != null && random.nextFloat() < 0.7F) {
            entityData.set(TAILLESS, true);
            entityData.set(BASKING, false);
            regrow = REGROW_TICKS;
            spawnAtLocation(level, new ItemStack(WarmFauna.LIZARD_TAIL.get()));
            addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 2));
            AnimalFear.panic(this, source.getEntity().position());
            playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.8F, 1.6F);
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && isTailless() && --regrow <= 0) entityData.set(TAILLESS, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TailRegrow", isTailless() ? regrow : 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        regrow = input.getIntOr("TailRegrow", 0);
        entityData.set(TAILLESS, regrow > 0);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.2F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.1F, 1.4F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.3F;
    }

    /** By day in full sun: lies flat for 10-30 s. Ends at once on a scare (AnimalFear's flee goal outranks it). */
    class BaskGoal extends Goal {
        private int ticks;

        BaskGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return onGround() && level().isBrightOutside() && level().canSeeSky(blockPosition()) && random.nextInt(120) == 0
                    && !AnimalFear.isPanicking(DesertLizard.this);
        }

        @Override
        public void start() {
            ticks = 200 + random.nextInt(400);
            getNavigation().stop();
            entityData.set(BASKING, true);
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && level().isBrightOutside() && !AnimalFear.isPanicking(DesertLizard.this);
        }

        @Override
        public void stop() {
            entityData.set(BASKING, false);
        }
    }
}
