package com.mechanical.wildlife;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eurasian brown bear: solitary, or a mother with cubs. Attacks players that come close, rears up on its hind legs
 * and swipes down, and runs faster than a walking player for a few seconds before it tires. Does not flee.
 */
public class BrownBear extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/brown_bear.png");
    private static final Identifier CUB_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/brown_bear_cub.png");
    /** Rearing up before a swipe; synced for the animation. */
    private static final EntityDataAccessor<Boolean> STANDING = SynchedEntityData.defineId(BrownBear.class, EntityDataSerializers.BOOLEAN);
    private static final int STAND_TICKS = 6;
    /** Noticed players are attacked within this range (sneaking: closer); the chase ends beyond FOLLOW_RANGE (24). */
    private static final double AGGRO_RANGE = 12, AGGRO_RANGE_SNEAKING = 5;
    /** Charges at full speed this long, then slows down (a short sprint, not a marathon). */
    private static final int BURST_TICKS = 100;
    private static final Identifier TIRED_ID = Identifier.fromNamespaceAndPath("mechanical", "bear_tired");
    private static final AttributeModifier TIRED = new AttributeModifier(TIRED_ID, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private float standAnimO, standAnim;
    private int warningTicks, chaseTicks;

    public BrownBear(EntityType<? extends BrownBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STANDING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FollowHerdGoal); // solitary; cubs keep up with FollowParentGoal
        goalSelector.addGoal(1, new BearAttackGoal());
        goalSelector.addGoal(5, new ForageBerriesGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
        targetSelector.addGoal(2, new AttackNearbyPlayerGoal(this));
    }

    /** Woken from its sleep: goes for whoever woke it (called by WildAnimal once it has the onWoken hook). */
    @Override
    protected void onWoken(Player near) {
        if (!isBaby()) setTarget(near);
    }

    /** A hurt cub calls its mother (any grown bear nearby) onto the attacker. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isBaby() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof BrownBear)) {
            for (BrownBear adult : level.getEntitiesOfClass(BrownBear.class, getBoundingBox().inflate(20), b -> !b.isBaby())) adult.setTarget(attacker);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            standAnimO = standAnim;
            standAnim = Mth.clamp(standAnim + (isStanding() ? 1.0F : -1.0F), 0.0F, STAND_TICKS);
        } else {
            if (warningTicks > 0) warningTicks--;
            chaseTicks = getTarget() != null ? chaseTicks + 1 : 0;
            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            boolean tired = chaseTicks > BURST_TICKS;
            if (speed != null && tired != speed.hasModifier(TIRED_ID)) {
                if (tired) speed.addTransientModifier(TIRED);
                else speed.removeModifier(TIRED_ID);
            }
        }
    }

    public boolean isStanding() {
        return entityData.get(STANDING);
    }

    void setStanding(boolean standing) {
        entityData.set(STANDING, standing);
    }

    /** 0..1: how far it has reared up on its hind legs (client side). */
    public float standAmount(float partialTick) {
        return Mth.lerp(partialTick, standAnimO, standAnim) / STAND_TICKS;
    }

    @Override
    public boolean isCharging() {
        return (getTarget() != null || isAggressive()) && !isBaby();
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CUB_TEXTURE : TEXTURE;
    }

    /** Every group member after the first is a cub: a group is a mother with her cubs. */
    @Override
    protected float babyChance() {
        return 1.0F;
    }

    @Override
    public double fearMultiplier() {
        return 0.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBaby() ? SoundEvents.POLAR_BEAR_AMBIENT_BABY : SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.POLAR_BEAR_STEP, 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.85F; // deeper than a polar bear
    }

    private void playWarningSound() {
        if (warningTicks <= 0) {
            makeSound(SoundEvents.POLAR_BEAR_WARNING);
            warningTicks = 40;
        }
    }

    /** Like the polar bear: rears up as the next swipe gets ready, and the swipe comes down as it drops. */
    class BearAttackGoal extends MeleeAttackGoal {
        BearAttackGoal() {
            super(BrownBear.this, 1.5, true);
        }

        @Override
        public boolean canUse() {
            return !isBaby() && super.canUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (canPerformAttack(target)) {
                resetAttackCooldown();
                mob.doHurtTarget(getServerLevel(mob), target);
                setStanding(false);
            } else if (mob.distanceToSqr(target) < (target.getBbWidth() + 3.0F) * (target.getBbWidth() + 3.0F)) {
                if (isTimeToAttack()) {
                    setStanding(false);
                    resetAttackCooldown();
                }
                if (getTicksUntilNextAttack() <= 10) {
                    setStanding(true);
                    playWarningSound();
                }
            } else {
                resetAttackCooldown();
                setStanding(false);
            }
        }

        @Override
        public void stop() {
            setStanding(false);
            super.stop();
        }
    }

    /**
     * Picks a fight with a player that comes too close, and keeps an existing target (a cub's attacker, whoever woke it)
     * until it gets beyond the follow range (24) or out of sight for a while.
     */
    static class AttackNearbyPlayerGoal extends TargetGoal {
        private final BrownBear bear;

        AttackNearbyPlayerGoal(BrownBear bear) {
            super(bear, true);
            this.bear = bear;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (bear.isBaby() || bear.isDozing()) return false; // asleep: only WildAnimal's wake check rouses it
            if (bear.getTarget() != null) {
                targetMob = bear.getTarget();
                return true;
            }
            if (bear.getRandom().nextInt(10) != 0) return false;
            Player player = bear.level().getNearestPlayer(bear, AGGRO_RANGE);
            if (player == null || player.isCreative() || player.isSpectator()) return false;
            double range = player.isShiftKeyDown() ? AGGRO_RANGE_SNEAKING : AGGRO_RANGE;
            if (bear.distanceTo(player) > range || !bear.hasLineOfSight(player)) return false;
            targetMob = player;
            return true;
        }

        @Override
        public void start() {
            bear.setTarget(targetMob);
            super.start();
        }
    }
}
