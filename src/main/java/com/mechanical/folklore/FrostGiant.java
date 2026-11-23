package com.mechanical.folklore;

import com.mechanical.Thaw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Frostjätte / Rimthurs: a rare mini-boss (boss bar) that walks out of a snowstorm ({@link Thaw#storm}) on snowy ground and
 * is gone with it: when the storm ends it goes back into the snow, so it is only ever seen in a storm. Heavy blows that
 * slow and chill; every {@link #BREATH_COOLDOWN} ticks a frost breath at whoever it hunts within {@link #BREATH_RANGE}
 * blocks (Slowness II, frozen). Killed, it leaves its Frost Heart (loot entities/frost_giant).
 */
public class FrostGiant extends Monster {
    static final double BREATH_RANGE = 8;
    static final int BREATH_COOLDOWN = 160, CHILL_TICKS = 140, FREEZE = 200, ALONE_RANGE = 128;
    private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), Component.translatable("entity.mechanical.frost_giant"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int breath = 80;

    public FrostGiant(EntityType<? extends FrostGiant> type, Level level) {
        super(type, level);
        xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 160.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 13.0).add(Attributes.ATTACK_KNOCKBACK, 1.2).add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** In a snowstorm, under open sky; very rare, never two within {@link #ALONE_RANGE} blocks. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevelAccessor s) || random.nextInt(40) != 0) return false;
        ServerLevel l = s.getLevel();
        return rule(Thaw.storm(l), level.canSeeSky(pos))
                && l.getEntitiesOfClass(FrostGiant.class, new net.minecraft.world.phys.AABB(pos).inflate(ALONE_RANGE)).isEmpty();
    }

    static boolean rule(boolean storm, boolean sky) {
        return storm && sky;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true; // the storm is its rule, not darkness
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount % 20 == 0 && !Thaw.storm(level)) {
            fade(level);
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (--breath <= 0 && getTarget() instanceof LivingEntity t && distanceTo(t) < BREATH_RANGE) {
            breath = BREATH_COOLDOWN;
            chill(t);
            var d = t.position().subtract(position()).normalize();
            for (int i = 1; i < 8; i++)
                level.sendParticles(ParticleTypes.SNOWFLAKE, getX() + d.x * i, getEyeY() - i * 0.25, getZ() + d.z * i, 6, 0.3, 0.3, 0.3, 0.01);
            level.playSound(null, blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
    }

    /** The storm is over: it goes back into the snow. */
    void fade(ServerLevel level) {
        level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2.5, getZ(), 80, 0.8, 2.0, 0.8, 0.05);
        discard();
    }

    static void chill(LivingEntity t) {
        t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, CHILL_TICKS, 1));
        t.setTicksFrozen(Math.max(t.getTicksFrozen(), FREEZE));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity l) l.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), this);
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false; // it leaves with the storm
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
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
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }
}
