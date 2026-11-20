package com.mechanical.wildlife.giants;

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
 * Steppe bison: the classic Ice Age game, in herds on plains, savanna and the cold steppe. Bulls (bigger hump and horns)
 * turn and charge when hurt and come to a hurt calf's aid; cows and calves flee. A panicked herd stampedes: running bison
 * trample whatever stands in their way.
 */
public class SteppeBison extends WildAnimal {
    private static final EntityDataAccessor<Boolean> BULL = SynchedEntityData.defineId(SteppeBison.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier BULL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/steppe_bison_bull.png");
    private static final Identifier COW_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/steppe_bison_cow.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/steppe_bison_calf.png");
    private static final float BULL_CHANCE = 0.3F, TRAMPLE_DAMAGE = 4.0F;
    private static final double STAMPEDE_SPEED_SQR = 0.025;

    public SteppeBison(EntityType<? extends SteppeBison> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 45.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ATTACK_KNOCKBACK, 1.2).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
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
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, false) {
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof SteppeBison)) {
            if (isBull() && !isBaby()) setTarget(attacker);
            else if (isBaby()) for (SteppeBison bull : level.getEntitiesOfClass(SteppeBison.class, getBoundingBox().inflate(16), b -> b.isBull() && !b.isBaby())) bull.setTarget(attacker);
        }
        return hurt;
    }

    /** Stampede: a panicking bison running flat out tramples anything in its path (not its herd). */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || isBaby() || (tickCount + getId()) % 4 != 0 || !AnimalFear.isPanicking(this)
                || getDeltaMovement().horizontalDistanceSqr() < STAMPEDE_SPEED_SQR) return;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.2),
                e -> e.isAlive() && !(e instanceof SteppeBison) && e.getBbHeight() < getBbHeight() * 1.5)) {
            if (e.hurtServer(level, damageSources().mobAttack(this), TRAMPLE_DAMAGE)) {
                e.knockback(1.0, getX() - e.getX(), getZ() - e.getZ(), damageSources().mobAttack(this), TRAMPLE_DAMAGE);
                playSound(SoundEvents.RAVAGER_STEP, 1.0F, 0.9F);
            }
        }
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

    /** The bull's big horns; cows show slimmer ones (the model picks by this flag). */
    @Override
    public boolean showsHorns() {
        return isBull() && !isBaby();
    }

    @Override
    public double fearMultiplier() {
        return 1.1;
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
        playSound(sounds().stepSound().value(), 0.25F, 0.75F);
    }

    /** A deep grunt, lower than the aurochs. */
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.0F : 0.6F);
    }
}
