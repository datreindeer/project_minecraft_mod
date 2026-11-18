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

/** Large deer in herds. Stags carry antlers and, when hurt, turn and charge instead of fleeing; hinds flee. */
public class RedDeer extends WildAnimal {
    private static final EntityDataAccessor<Boolean> STAG = SynchedEntityData.defineId(RedDeer.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier STAG_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/red_deer_stag.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/red_deer_calf.png");
    private static final Identifier HIND_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/red_deer_hind.png");
    private static final float STAG_CHANCE = 0.35F;

    public RedDeer(EntityType<? extends RedDeer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    public boolean isStag() {
        return entityData.get(STAG);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAG, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // only stags fight back (the fear goal yields while they have a target)
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, false) {
            @Override
            public boolean canUse() {
                return isStag() && !isBaby() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isStag() && !isBaby() && super.canUse();
            }
        });
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(STAG, level.getRandom().nextFloat() < STAG_CHANCE);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("stag", isStag());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(STAG, input.getBooleanOr("stag", false));
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : isStag() ? STAG_TEXTURE : HIND_TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return isStag() && !isBaby();
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
        playSound(SoundEvents.GOAT_STEP, 0.15F, 0.9F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }
}
