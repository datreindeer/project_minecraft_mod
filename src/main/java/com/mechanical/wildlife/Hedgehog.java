package com.mechanical.wildlife;

import java.util.EnumSet;

import com.mechanical.AnimalFear;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Hedgehog: small, slow, out and about at dusk and night. It never runs: when threatened it rolls into a spiny ball and
 * stays put until the danger has gone, and by day it sleeps rolled up. Hitting it with a bare hand pricks you.
 */
public class Hedgehog extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/hedgehog.png");
    private static final EntityDataAccessor<Boolean> CURLED = SynchedEntityData.defineId(Hedgehog.class, EntityDataSerializers.BOOLEAN);
    private static final float PRICK_DAMAGE = 1.0F;

    public Hedgehog(EntityType<? extends Hedgehog> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.16);
    }

    public boolean isCurled() {
        return entityData.get(CURLED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURLED, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // nocturnal: no night doze and no herd; it sleeps curled up by day instead (CurlGoal)
        goalSelector.removeAllGoals(g -> g instanceof DozeGoal || g instanceof FollowHerdGoal);
        // above AnimalFear's flee goal (priority 0): a frightened hedgehog curls up instead of running
        goalSelector.addGoal(-1, new CurlGoal(this));
    }

    /** A bare-handed blow lands on the spines. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (source.getDirectEntity() instanceof Player player && player.getMainHandItem().isEmpty() && !player.isCreative()) {
            player.hurtServer(level, damageSources().thorns(this), PRICK_DAMAGE);
        }
        return hurt;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 0.0; // never flees from a noticed player (it curls up, see CurlGoal)
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isCurled() ? SoundEvents.ARMADILLO_HURT_REDUCED : SoundEvents.ARMADILLO_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMADILLO_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.4F;
    }

    /** Rolled up: while a player is close or it is panicking, and through the day (asleep). */
    static class CurlGoal extends Goal {
        private static final double THREAT = 5, THREAT_SNEAK = 2;
        /** Stays curled this long after the last threat before peeking out. */
        private static final int CALM_TICKS = 80;
        private final Hedgehog mob;
        private int calm;

        CurlGoal(Hedgehog mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean threatened() {
            if (AnimalFear.isPanicking(mob) || mob.hurtTime > 0) return true;
            Player p = mob.level().getNearestPlayer(mob, THREAT);
            return p != null && !p.isCreative() && !p.isSpectator() && mob.distanceTo(p) < (p.isCrouching() ? THREAT_SNEAK : THREAT);
        }

        private boolean sleepy() {
            return mob.level().isBrightOutside() && !mob.isInWater();
        }

        @Override
        public boolean canUse() {
            return threatened() || sleepy() && mob.onGround() && mob.getRandom().nextInt(200) == 0;
        }

        @Override
        public void start() {
            calm = CALM_TICKS;
            mob.getNavigation().stop();
            mob.entityData.set(CURLED, true);
            mob.playSound(SoundEvents.ARMADILLO_ROLL, 0.6F, 1.4F);
        }

        @Override
        public boolean canContinueToUse() {
            return calm > 0 || sleepy();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            mob.getNavigation().stop();
            if (threatened()) calm = CALM_TICKS;
            else calm--;
        }

        @Override
        public void stop() {
            mob.entityData.set(CURLED, false);
            mob.playSound(SoundEvents.ARMADILLO_UNROLL_FINISH, 0.6F, 1.4F);
        }
    }
}
