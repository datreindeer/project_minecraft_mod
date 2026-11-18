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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Cold-biome deer in big herds. Both sexes carry antlers (the bull's much bigger); moderately skittish, never fights. */
public class Reindeer extends WildAnimal {
    private static final EntityDataAccessor<Boolean> BULL = SynchedEntityData.defineId(Reindeer.class, EntityDataSerializers.BOOLEAN);
    /** Public: ReindeerModel tells bull from cow by the texture (the render state has no sex field). */
    public static final Identifier BULL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/reindeer_bull.png");
    private static final Identifier COW_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/reindeer_cow.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/reindeer_calf.png");
    private static final float BULL_CHANCE = 0.35F;

    public Reindeer(EntityType<? extends Reindeer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.28);
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
        return !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public double fearMultiplier() {
        return 1.2;
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
        playSound(SoundEvents.GOAT_STEP, 0.15F, 0.8F); // the famous clicking hooves would need a custom sound
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }
}
