package com.mechanical.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Huge deer of taiga and swamp, alone or in twos and threes. Bulls carry broad palmate antlers and, like the red deer
 * stag, turn on whoever hurts them (and hit much harder); cows and calves flee. Not very skittish.
 */
public class Moose extends WildAnimal {
    private static final EntityDataAccessor<Boolean> BULL = SynchedEntityData.defineId(Moose.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier BULL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/moose_bull.png");
    private static final Identifier COW_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/moose_cow.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/moose_calf.png");
    private static final float BULL_CHANCE = 0.45F;

    public Moose(EntityType<? extends Moose> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ATTACK_KNOCKBACK, 1.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public boolean isBull() {
        return entityData.get(BULL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BULL, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // only bulls fight back (the fear goal yields while they have a target)
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false) {
            @Override
            public boolean canUse() {
                return isBull() && !isBaby() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isBull() && !isBaby() && super.canUse();
            }
        });
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(BULL, level.getRandom().nextFloat() < BULL_CHANCE);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("bull", isBull());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(BULL, input.getBooleanOr("bull", false));
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : isBull() ? BULL_TEXTURE : COW_TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return isBull() && !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    public double fearMultiplier() {
        return 0.7;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
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
        playSound(SoundEvents.HORSE_STEP, 0.25F, 0.7F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }
}
