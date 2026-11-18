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

/** Wild sheep of hills and rocky slopes. Rams carry big curled horns and the pale saddle patch; ewes are plainer. Skittish. */
public class Mouflon extends WildAnimal {
    private static final EntityDataAccessor<Boolean> RAM = SynchedEntityData.defineId(Mouflon.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier RAM_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mouflon_ram.png");
    private static final Identifier EWE_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mouflon_ewe.png");
    private static final Identifier LAMB_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mouflon_lamb.png");
    private static final float RAM_CHANCE = 0.4F;

    public Mouflon(EntityType<? extends Mouflon> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public boolean isRam() {
        return entityData.get(RAM);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAM, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(RAM, level.getRandom().nextFloat() < RAM_CHANCE);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("ram", isRam());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(RAM, input.getBooleanOr("ram", false));
    }

    @Override
    public Identifier texture() {
        return isBaby() ? LAMB_TEXTURE : isRam() ? RAM_TEXTURE : EWE_TEXTURE;
    }

    /** The ram's curled horns; ewes show small spikes instead. */
    @Override
    public boolean showsHorns() {
        return isRam() && !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.35F;
    }

    @Override
    public double fearMultiplier() {
        return 1.4;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.12F, 1.1F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.25F : isRam() ? 0.8F : 0.95F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}
