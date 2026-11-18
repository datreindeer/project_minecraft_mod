package com.mechanical.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Wild cattle of the open grasslands, in herds. Bulls are near-black with big forward-curving horns and, when hurt, turn
 * and charge (and come to the aid of a hurt calf); cows are browner with smaller lyre horns and flee.
 */
public class Aurochs extends WildAnimal {
    private static final EntityDataAccessor<Boolean> BULL = SynchedEntityData.defineId(Aurochs.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier BULL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/aurochs_bull.png");
    private static final Identifier COW_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/aurochs_cow.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/aurochs_calf.png");
    private static final float BULL_CHANCE = 0.3F;

    public Aurochs(EntityType<? extends Aurochs> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
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

    /** A hurt calf calls the bulls nearby onto its attacker. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isBaby() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Aurochs)) {
            for (Aurochs bull : level.getEntitiesOfClass(Aurochs.class, getBoundingBox().inflate(16), a -> a.isBull() && !a.isBaby())) bull.setTarget(attacker);
        }
        return hurt;
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

    /** The bull's big horns and hump; cows show their smaller horns instead. */
    @Override
    public boolean showsHorns() {
        return isBull() && !isBaby();
    }

    @Override
    public double fearMultiplier() {
        return 0.8;
    }

    private static CowSoundVariant sounds() {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.MOODY);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return sounds().ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sounds().deathSound().value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(sounds().stepSound().value(), 0.2F, 0.8F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.1F : 0.75F);
    }
}
