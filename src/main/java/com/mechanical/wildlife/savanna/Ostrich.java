package com.mechanical.wildlife.savanna;

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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Ostrich: the great flightless bird of the savanna, 2 blocks tall, in small flocks. It runs very fast and shies away
 * from people, but cornered - someone within {@link #KICK_RANGE} blocks while it panics - it turns and kicks forward
 * with a clawed foot (hard, with a big knockback). Black-and-white cocks and grey-brown hens; the hens lay an Ostrich
 * Egg on the ground by day now and then (a whole meal once roasted). Chicks are striped and fluffy.
 */
public class Ostrich extends WildAnimal {
    private static final Identifier MALE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/ostrich_male.png");
    private static final Identifier FEMALE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/ostrich_female.png");
    private static final Identifier CHICK = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/ostrich_chick.png");
    private static final EntityDataAccessor<Boolean> IS_MALE = SynchedEntityData.defineId(Ostrich.class, EntityDataSerializers.BOOLEAN);
    /** Counts down while kicking (synced for the leg-forward pose). */
    private static final EntityDataAccessor<Integer> KICK = SynchedEntityData.defineId(Ostrich.class, EntityDataSerializers.INT);
    public static final int KICK_TICKS = 10;
    private static final double KICK_RANGE = 2.5;
    private static final float KICK_DAMAGE = 6.0F;
    private int kickCooldown, layTime = 6000 + 6000;

    public Ostrich(EntityType<? extends Ostrich> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, KICK_DAMAGE).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_MALE, false);
        builder.define(KICK, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(IS_MALE, level.getRandom().nextFloat() < 0.4F);
        layTime = 3000 + level.getRandom().nextInt(9000);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    public boolean isMale() {
        return entityData.get(IS_MALE);
    }

    /** 0..1: the kicking leg thrown forward. */
    public float kickAmount() {
        int k = entityData.get(KICK);
        return k <= 0 ? 0 : (float) Math.sin(Math.PI * k / KICK_TICKS);
    }

    @Override
    public boolean showsHorns() {
        return isMale(); // the cock's black plumage and white wing plumes
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CHICK : isMale() ? MALE : FEMALE;
    }

    @Override
    public double fearMultiplier() {
        return 1.5;
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server)) return;
        int k = entityData.get(KICK);
        if (k > 0) entityData.set(KICK, k - 1);
        if (kickCooldown > 0) kickCooldown--;
        if (isBaby()) return;
        if (kickCooldown == 0 && (AnimalFear.isPanicking(this) || hurtTime > 0)) {
            Player p = level().getNearestPlayer(this, KICK_RANGE);
            if (p != null && !p.isCreative() && !p.isSpectator()) kick(server, p);
        }
        if (!isMale() && onGround() && level().isBrightOutside() && --layTime <= 0) {
            playSound(SoundEvents.CHICKEN_EGG, 1.0F, 0.5F);
            spawnAtLocation(server, SavannaFauna.OSTRICH_EGG.get());
            layTime = 12000 + random.nextInt(12000);
        }
    }

    /** Turns on {@code p} and kicks: hard, and it throws him back. */
    void kick(ServerLevel level, Player p) {
        kickCooldown = 30;
        entityData.set(KICK, KICK_TICKS);
        getLookControl().setLookAt(p);
        setYRot((float) (Math.atan2(p.getZ() - getZ(), p.getX() - getX()) * 180 / Math.PI) - 90);
        if (p.hurtServer(level, damageSources().mobAttack(this), KICK_DAMAGE)) {
            Vec3 push = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            p.push(push.x * 1.2, 0.4, push.z * 1.2);
            p.needsSync = true;
        }
        playSound(SoundEvents.RAVAGER_STEP, 1.0F, 1.4F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("male", isMale());
        output.putInt("lay_time", layTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(IS_MALE, input.getBooleanOr("male", false));
        layTime = input.getIntOr("lay_time", 12000);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isMale() && random.nextInt(3) == 0 ? SoundEvents.RAVAGER_AMBIENT : null; // the cock's deep booming
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.0F : 0.5F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHICKEN_STEP.value(), 0.3F, 0.6F);
    }
}
