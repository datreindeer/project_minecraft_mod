package com.mechanical.wildlife.giants;

import java.util.EnumSet;
import java.util.List;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Woolly mammoth: a rare herd of two to four on the cold steppe, led by a bigger old matriarch (the herd leader, a
 * mini-boss with a boss bar). Peaceful at a distance, but the leader trumpets a warning at a player who comes close and
 * charges one who keeps coming; when any of the herd is hurt every grown mammoth turns on the attacker. Its attack is a
 * sweep of the trunk and tusks that hits everything in front of it and throws it far. Like the giant deer it doesn't
 * bleed or back off until it is down to a fifth of its health, then it runs. Always yields two Mammoth Ivory tusks.
 */
public class WoollyMammoth extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/woolly_mammoth.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/woolly_mammoth_calf.png");
    private static final EntityDataAccessor<Boolean> LEADER = SynchedEntityData.defineId(WoollyMammoth.class, EntityDataSerializers.BOOLEAN);
    /** Ticks left of the trunk sweep / the warning trumpet (synced for the model). */
    private static final EntityDataAccessor<Integer> SWIPE = SynchedEntityData.defineId(WoollyMammoth.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRUMPET = SynchedEntityData.defineId(WoollyMammoth.class, EntityDataSerializers.INT);
    public static final int SWIPE_TICKS = 12, TRUMPET_TICKS = 40;
    private static final float BREAK_POINT = 0.2F;
    static final double LEADER_HEALTH = 220, HEALTH = 150, LEADER_SCALE = 1.2;
    private static final double WARN_RANGE = 14, ATTACK_RANGE = 8, SWEEP_REACH = 4.5;
    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int warnCooldown;

    public WoollyMammoth(EntityType<? extends WoollyMammoth> type, Level level) {
        super(type, level);
        bossEvent.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.ATTACK_KNOCKBACK, 2.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.ARMOR, 4.0);
    }

    public boolean isLeader() {
        return entityData.get(LEADER);
    }

    private void setLeader(boolean leader) {
        entityData.set(LEADER, leader);
        getAttribute(Attributes.SCALE).setBaseValue(leader ? LEADER_SCALE : 1.0);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(leader ? LEADER_HEALTH : HEALTH);
        bossEvent.setVisible(leader);
    }

    /** 0..1 progress of the trunk sweep, for the model. */
    public float swipeAmount(float partialTick) {
        int t = entityData.get(SWIPE);
        return t <= 0 ? 0 : Mth.clamp((SWIPE_TICKS - t + partialTick) / SWIPE_TICKS, 0, 1);
    }

    /** 0..1: head and trunk raised for the warning trumpet. */
    public float trumpetAmount(float partialTick) {
        int t = entityData.get(TRUMPET);
        if (t <= 0) return 0;
        float edge = Math.min(t, TRUMPET_TICKS - t) + partialTick;
        return Mth.clamp(edge / 8.0F, 0, 1);
    }

    /** Down to a fifth of its health: it gives up and runs. */
    public boolean isBroken() {
        return getHealth() <= getMaxHealth() * BREAK_POINT;
    }

    @Override
    public float bleedThreshold() {
        return BREAK_POINT;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEADER, false);
        builder.define(SWIPE, 0);
        builder.define(TRUMPET, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, true) {
            @Override
            public boolean canUse() {
                return !isBaby() && !isBroken() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isBaby() && !isBroken() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new FollowLeaderGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isBaby() && !isBroken() && super.canUse();
            }
        });
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        // the first of a group (or one from an egg) is the herd's matriarch
        boolean first = groupData == null || groupData instanceof AgeableMob.AgeableMobGroupData g && g.getGroupSize() == 0;
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (first && !isBaby()) {
            setLeader(true);
            setHealth(getMaxHealth());
        }
        return data;
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        int s = entityData.get(SWIPE), t = entityData.get(TRUMPET);
        if (s > 0) entityData.set(SWIPE, s - 1);
        if (t > 0) entityData.set(TRUMPET, t - 1);
        if (level().isClientSide()) return;
        if (warnCooldown > 0) warnCooldown--;
        if (isBroken()) {
            // beaten: drops the fight and runs (AnimalFear takes over now that it bleeds)
            if (getTarget() != null) {
                AnimalFear.panic(this, getTarget().position());
                setTarget(null);
            }
            return;
        }
        if (isLeader() && !isBaby() && getTarget() == null && !isDozing() && (tickCount + getId()) % 10 == 0) watchForIntruders();
    }

    /** The matriarch trumpets at a player who comes close, and charges one who comes closer still. */
    private void watchForIntruders() {
        Player p = level().getNearestPlayer(this, WARN_RANGE);
        if (p == null || p.isCreative() || p.isSpectator() || !hasLineOfSight(p)) return;
        double d = distanceTo(p), close = p.isShiftKeyDown() ? ATTACK_RANGE * 0.5 : ATTACK_RANGE;
        if (d < close) {
            rallyHerd(p);
        } else if (warnCooldown == 0 && d < (p.isShiftKeyDown() ? WARN_RANGE * 0.6 : WARN_RANGE)) {
            trumpet();
            getLookControl().setLookAt(p);
            warnCooldown = 200;
        }
    }

    private void trumpet() {
        entityData.set(TRUMPET, TRUMPET_TICKS);
        playSound(SoundEvents.RAVAGER_ROAR, 4.0F, 1.45F + random.nextFloat() * 0.1F);
    }

    /** Every grown mammoth nearby turns on the attacker. */
    void rallyHerd(LivingEntity attacker) {
        if (attacker instanceof WoollyMammoth || attacker instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        if (getTarget() == null) trumpet();
        for (WoollyMammoth m : level().getEntitiesOfClass(WoollyMammoth.class, getBoundingBox().inflate(24), m -> !m.isBaby() && !m.isBroken())) {
            if (m.getTarget() == null) m.setTarget(attacker);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !isBroken()) rallyHerd(attacker);
        return hurt;
    }

    /** The trunk-and-tusk sweep: everything in a wide arc in front is hit and thrown. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        entityData.set(SWIPE, SWIPE_TICKS);
        playSound(SoundEvents.RAVAGER_ATTACK, 2.0F, 0.6F);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = damageSources().mobAttack(this);
        boolean any = false;
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double reach = SWEEP_REACH * getScale();
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach, 1.0, reach),
                e -> e != this && e.isAlive() && !(e instanceof WoollyMammoth) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        for (LivingEntity e : hit) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            boolean inFront = len < 1.0E-3 || (dx * fx + dz * fz) / len > 0.2; // about 160 degrees wide
            if (e != target && (!inFront || len > reach + getBbWidth() / 2)) continue;
            if (!e.hurtServer(level, source, e == target ? damage : damage * 0.7F)) continue;
            any = true;
            e.knockback(2.2, getX() - e.getX(), getZ() - e.getZ(), source, damage);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.45, 0));
            e.needsSync = true;
        }
        if (any) playSound(SoundEvents.RAVAGER_STUNNED, 1.0F, 0.7F);
        return any;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected void onWoken(Player near) {
        if (!isBaby() && !isBroken()) rallyHerd(near);
        else super.onWoken(near);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("leader", isLeader());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        float health = getHealth();
        setLeader(input.getBooleanOr("leader", false));
        setHealth(health); // the max health changed with the leader flag; keep what was saved
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return !isBaby();
    }

    /** Grown mammoths aren't put off by a player, only the leader's warning matters; calves and beaten ones run. */
    @Override
    public double fearMultiplier() {
        return isBroken() ? 2.0 : isBaby() ? 0.8 : 0.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.6F, 0.55F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.1F : 0.6F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    /** Non-leaders keep close to their matriarch. */
    static class FollowLeaderGoal extends Goal {
        private static final double NEAR = 8, SEARCH = 40;
        private final WoollyMammoth mob;
        private WoollyMammoth leader;

        FollowLeaderGoal(WoollyMammoth mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.isLeader() || mob.getTarget() != null || mob.getRandom().nextInt(20) != 0) return false;
            leader = mob.level().getEntitiesOfClass(WoollyMammoth.class, mob.getBoundingBox().inflate(SEARCH), WoollyMammoth::isLeader)
                    .stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
            return leader != null && mob.distanceTo(leader) > NEAR;
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(leader, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return leader.isAlive() && mob.getTarget() == null && mob.distanceTo(leader) > NEAR / 2 && !mob.getNavigation().isDone();
        }
    }
}
