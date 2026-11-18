package com.mechanical.wildlife;

import java.util.EnumSet;

import com.mechanical.AnimalFear;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Giant deer (Megaloceros): rare, solitary mini-boss with huge palmate antlers. Attacks players who come near with
 * heavy antler charges and doesn't bleed or back off until it is down to a quarter of its health; then it bleeds and
 * runs (AnimalFear takes over once {@link com.mechanical.Blood#isBleeding} says so). Always yields a giant antler.
 */
public class GiantDeer extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giant_deer.png");
    private static final float BREAK_POINT = 0.25F;
    private final ServerBossEvent bossEvent = new ServerBossEvent(net.minecraft.util.Mth.createInsecureUUID(random), getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    public GiantDeer(EntityType<? extends GiantDeer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 120.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Down to a quarter of its health: it gives up and runs. */
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
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FollowHerdGoal); // always alone
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.8, true) {
            @Override
            public boolean canUse() {
                return !isBroken() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isBroken() && super.canContinueToUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isBroken() && super.canUse();
            }
        });
        targetSelector.addGoal(2, new AttackNearbyPlayerGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (!isBroken()) return;
        // beaten: drops the fight and runs from whoever is near, for as long as it stays this weak
        if (getTarget() != null) {
            AnimalFear.panic(this, getTarget().position());
            setTarget(null);
        } else if (tickCount % 20 == 0 && !AnimalFear.isPanicking(this)) {
            Player near = level().getNearestPlayer(this, 24);
            if (near != null && !near.isCreative() && !near.isSpectator()) AnimalFear.panic(this, near.position());
        }
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

    /** Woken by a player: it goes for them instead of bolting. */
    @Override
    protected void onWoken(Player near) {
        if (!isBroken()) setTarget(near);
        else super.onWoken(near);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) playSound(SoundEvents.GOAT_RAM_IMPACT, 1.2F, 0.6F);
        return hit;
    }

    /** Charges any player that comes within range and sight (much closer when sneaking). */
    static class AttackNearbyPlayerGoal extends TargetGoal {
        private static final double RANGE = 14, RANGE_SNEAKING = 6;
        private final GiantDeer deer;

        AttackNearbyPlayerGoal(GiantDeer deer) {
            super(deer, false);
            this.deer = deer;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (deer.isBroken() || deer.isDozing() || deer.getTarget() != null || deer.getRandom().nextInt(10) != 0) return false;
            Player p = deer.level().getNearestPlayer(deer, RANGE);
            if (p == null || p.isCreative() || p.isSpectator() || !deer.hasLineOfSight(p)) return false;
            if (deer.distanceTo(p) > (p.isShiftKeyDown() ? RANGE_SNEAKING : RANGE)) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            deer.setTarget(targetMob);
            deer.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            return !deer.isBroken() && super.canContinueToUse();
        }
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return true;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return isBroken() ? 2.0 : 0.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.3F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }
}
