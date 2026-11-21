package com.mechanical.wildlife.underground;

import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Lindworm: the wingless serpent-dragon with two forelegs, a rare boss very deep down (below Y -45, one at a time). Fast,
 * armoured and relentless, and torchlight doesn't drive it off. Four attacks:
 * <ul>
 * <li><b>bite</b> (melee): heavy, poisons (Poison II) and throws back;</li>
 * <li><b>lunge</b>: from {@link #LUNGE_MIN}-{@link #LUNGE_MAX} blocks it springs at its prey and slams into it;</li>
 * <li><b>coil</b>: close in, it wraps round its prey for {@link #COIL_TICKS} ticks: held fast and crushed every second;</li>
 * <li><b>tail sweep</b>: everyone within {@link #SWEEP_RANGE} blocks is lashed and flung away.</li>
 * </ul>
 * Below half health it rages: faster, and the attacks come twice as often. It sleeps on a hoard: dying it spills
 * mechanical:entities/lindworm_hoard (gold, gems and rarer things) besides leaving its carcass. Boss bar while fought.
 */
public class Lindworm extends WildAnimal implements Underground.Fearless {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/lindworm.png");
    public static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> HOARD = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Mechanical.MODID, "entities/lindworm_hoard"));
    /** 0 moving, 1 lunging, 2 coiled round its prey (synced for the model). */
    private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(Lindworm.class, EntityDataSerializers.INT);
    public static final int MOVING = 0, LUNGING = 1, COILING = 2;
    static final double SIGHT = 32, LUNGE_MIN = 4, LUNGE_MAX = 14, COIL_RANGE = 3, SWEEP_RANGE = 4.5;
    static final int VENOM_TICKS = 120, COIL_TICKS = 60, LUNGE_COOLDOWN = 80, COIL_COOLDOWN = 220, SWEEP_COOLDOWN = 140;
    static final float LUNGE_DAMAGE = 10, CRUSH_DAMAGE = 4, SWEEP_DAMAGE = 8;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), Component.translatable("entity.mechanical.lindworm"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private int lungeCooldown = 40, coilCooldown = 100, sweepCooldown = 60, poseTicks;

    public Lindworm(EntityType<? extends Lindworm> type, Level level) {
        super(type, level);
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 260.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 14.0).add(Attributes.ATTACK_KNOCKBACK, 1.0).add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POSE, MOVING);
    }

    public int pose() {
        return entityData.get(POSE);
    }

    private void setPose(int pose, int ticks) {
        entityData.set(POSE, pose);
        poseTicks = ticks;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("GrazeGoal") || g.getClass().getSimpleName().equals("FollowHerdGoal"));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return pose() == MOVING && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, (t, level) -> distanceTo(t) < SIGHT));
    }

    /** Below half health: faster, attacks twice as often. */
    boolean raging() {
        return getHealth() < getMaxHealth() / 2;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        int step = raging() ? 2 : 1;
        lungeCooldown -= step;
        coilCooldown -= step;
        sweepCooldown -= step;
        LivingEntity t = getTarget();
        if (pose() == LUNGING) {
            if (t != null && getBoundingBox().inflate(0.6).intersects(t.getBoundingBox())) {
                strike(level, t, LUNGE_DAMAGE, 1.5);
                setPose(MOVING, 0);
            } else if (--poseTicks <= 0 || onGround() && poseTicks < 15) setPose(MOVING, 0);
            return;
        }
        if (pose() == COILING) {
            if (t == null || !t.isAlive() || distanceTo(t) > COIL_RANGE + 1.5 || --poseTicks <= 0) {
                setPose(MOVING, 0);
                return;
            }
            getNavigation().stop();
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 4), this);
            t.setDeltaMovement(t.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            t.needsSync = true;
            if (poseTicks % 20 == 0) t.hurtServer(level, damageSources().mobAttack(this), CRUSH_DAMAGE);
            return;
        }
        if (sweepCooldown <= 0) {
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SWEEP_RANGE),
                    e -> e != this && e.isAlive() && (e instanceof Player p ? !p.isCreative() && !p.isSpectator() : e == t));
            if (!near.isEmpty()) {
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.5F);
                for (LivingEntity e : near) strike(level, e, SWEEP_DAMAGE, 2.0);
                sweepCooldown = SWEEP_COOLDOWN;
                return;
            }
        }
        if (t == null) return;
        double d = distanceTo(t);
        if (coilCooldown <= 0 && d < COIL_RANGE) {
            setPose(COILING, COIL_TICKS);
            coilCooldown = COIL_COOLDOWN;
            playSound(SoundEvents.SPIDER_AMBIENT, 1.5F, 0.4F);
        } else if (lungeCooldown <= 0 && onGround() && d > LUNGE_MIN && d < LUNGE_MAX && hasLineOfSight(t)) {
            Vec3 dir = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ()).normalize();
            double reach = Math.min(1.8, 0.35 + d * 0.12);
            setDeltaMovement(dir.x * reach, 0.45, dir.z * reach);
            setYRot((float) (Math.atan2(dir.z, dir.x) * 180 / Math.PI) - 90);
            yBodyRot = getYRot();
            setPose(LUNGING, 30);
            lungeCooldown = LUNGE_COOLDOWN;
            playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.4F);
        }
    }

    /** Damages {@code e} and flings it away from the lindworm. */
    private void strike(ServerLevel level, LivingEntity e, float damage, double fling) {
        if (!e.hurtServer(level, damageSources().mobAttack(this), damage)) return;
        Vec3 away = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ()).normalize().scale(fling * (1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5));
        e.setDeltaMovement(e.getDeltaMovement().add(away.x, 0.4, away.z));
        e.needsSync = true;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean bit = super.doHurtTarget(level, target);
        if (bit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.POISON, VENOM_TICKS, 1), this);
        return bit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && (tickCount + getId()) % 40 == 0) {
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(raging() ? 0.4 : 0.32);
        }
    }

    /** The hoard it lay on: the loot table rolled at {@code at}. */
    public static List<ItemStack> rollHoard(ServerLevel level, Vec3 at) {
        return level.getServer().reloadableRegistries().getLootTable(HOARD)
                .getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at).create(LootContextParamSets.CHEST));
    }

    @Override
    public void die(DamageSource source) {
        // spilled before the carcass takes the place of the usual drops (Butchery cancels those)
        if (level() instanceof ServerLevel level && !isRemoved() && !dead) {
            for (ItemStack s : rollHoard(level, position())) spawnAtLocation(level, s);
        }
        super.die(source);
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
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return entityTags().contains(Predator.NIGHT_TAG); // like the others: the deep caves would fill up with bosses otherwise
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return 0.0;
    }

    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    public float bleedThreshold() {
        return 0.25F;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true;
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_GROWL;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.3F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.4F, 1.2F);
    }
}
