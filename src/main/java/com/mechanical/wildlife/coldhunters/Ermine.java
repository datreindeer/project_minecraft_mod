package com.mechanical.wildlife.coldhunters;

import java.util.EnumSet;

import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Ermine (stoat): a slim, quick little hunter of the snow and the taiga, black tail tip all year. Snow-white in the snow
 * biomes (like the Ptarmigan, decided where it is born), brown above and cream below elsewhere. Bounds along in arcs and
 * is gone at the sight of a player. Hunts lemmings: kills one and eats the little body where it fell.
 */
public class Ermine extends WildAnimal {
    private static final EntityDataAccessor<Boolean> WHITE = SynchedEntityData.defineId(Ermine.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier SUMMER = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/ermine.png");
    private static final Identifier WINTER = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/ermine_winter.png");

    public Ermine(EntityType<? extends Ermine> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    public boolean isWhite() {
        return entityData.get(WHITE);
    }

    @Override
    public Identifier texture() {
        return isWhite() ? WINTER : SUMMER;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WHITE, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(WHITE, level.getBiome(blockPosition()).is(BiomeTags.SPAWNS_SNOW_FOXES));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("white", isWhite());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(WHITE, input.getBooleanOr("white", false));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal"));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, false));
        goalSelector.addGoal(2, new EatSmallKillGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Lemming.class, true,
                (target, level) -> !isBaby() && random.nextInt(4) == 0));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !onGround()) return;
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistance() > 0.12 && random.nextInt(3) == 0) setDeltaMovement(v.x, 0.28, v.z); // the stoat's arching bound
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    public double fearMultiplier() {
        return 1.4;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_SCREECH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.7F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    /** Eats a fresh lemming carcass within 6 blocks (its own kill, or one from a snare) in 3 s. */
    class EatSmallKillGoal extends Goal {
        private CarcassEntity carcass;
        private int eaten;

        EatSmallKillGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isBaby() || random.nextInt(10) != 0) return false;
            carcass = level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(6),
                    c -> c.animal() == ColdhuntersFauna.LEMMING.get() && c.haulerId() < 0 && c.hungAt() == null && !c.isPassenger()).stream().findFirst().orElse(null);
            return carcass != null && getNavigation().moveTo(carcass, 1.2);
        }

        @Override
        public void start() {
            eaten = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return carcass.isAlive() && carcass.haulerId() < 0 && getTarget() == null && eaten < 60;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(carcass);
            if (distanceToSqr(carcass) > 1.2 * 1.2) {
                if ((tickCount + getId()) % 20 == 0) getNavigation().moveTo(carcass, 1.2);
                return;
            }
            getNavigation().stop();
            if (++eaten % 15 == 0) playSound(SoundEvents.FOX_EAT, 0.6F, 1.6F);
            if (eaten >= 60) carcass.discard();
        }
    }
}
