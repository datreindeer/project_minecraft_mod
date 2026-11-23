package com.mechanical.wildlife.temperate;

import com.mechanical.wildlife.WildAnimal;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Giant stag beetle: a big, slow, glossy chestnut-and-black beetle of old oak woods and the dark forest. The males carry
 * huge antler-like jaws and rear up at anything that comes close ({@link #REAR_RANGE}); the females are smaller-jawed. It
 * doesn't run and does no harm. No carcass: it leaves its Beetle Shell (ground into pigment) and now and then a Grub
 * (bait) (loot table).
 */
public class StagBeetle extends WildAnimal {
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(StagBeetle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> REARING = SynchedEntityData.defineId(StagBeetle.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier MALE_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/temperate/stag_beetle.png");
    private static final Identifier FEMALE_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/temperate/stag_beetle_female.png");
    static final double REAR_RANGE = 3.0;

    public StagBeetle(EntityType<? extends StagBeetle> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public boolean isMale() {
        return entityData.get(MALE);
    }

    /** Jaws up, front raised: for the model. */
    public boolean isRearing() {
        return entityData.get(REARING);
    }

    /** The model draws the rearing threat display from the charging flag. */
    @Override
    public boolean isCharging() {
        return isRearing();
    }

    @Override
    public boolean showsHorns() {
        return isMale();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MALE, true);
        builder.define(REARING, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(MALE, level.getRandom().nextFloat() < 0.6F);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("male", isMale());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(MALE, input.getBooleanOr("male", true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || (tickCount + getId()) % 10 != 0) return;
        Player near = level().getNearestPlayer(this, REAR_RANGE);
        boolean rear = near != null && !near.isSpectator();
        if (rear != isRearing()) {
            entityData.set(REARING, rear);
            if (rear) playSound(SoundEvents.SILVERFISH_AMBIENT, 0.5F, 0.6F);
        }
        if (rear) {
            getNavigation().stop();
            getLookControl().setLookAt(near);
        }
    }

    @Override
    public Identifier texture() {
        return isMale() ? MALE_TEXTURE : FEMALE_TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    /** It stands its ground. */
    @Override
    public double fearMultiplier() {
        return 0.2;
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
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.08F, 0.6F);
    }
}
