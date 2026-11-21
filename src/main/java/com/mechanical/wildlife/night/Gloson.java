package com.mechanical.wildlife.night;

import java.util.EnumSet;

import com.mechanical.Mechanical;
import com.mechanical.NightDangers;
import com.mechanical.wildlife.Predator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gloson, the grey sow of Swedish folklore with glowing eyes and a back sharp as a razor, that runs between people's legs
 * at night. Here a real animal (a {@link Predator}: it sleeps by day, fears fire and torches, leaves a carcass): at night it
 * picks out a player, circles him 10-16 blocks off in the dark for 20-50 s with only its eyes showing, then charges. The
 * hit cuts ({@link NightFauna#BLEEDING}) and throws him back, and it runs on past and off into the dark to circle again.
 * It charges at once when hurt or when someone comes within 4 blocks. Fire or a torch in hand keep it circling.
 */
public class Gloson extends Predator {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/gloson.png");
    static final double SIGHT = 32, RING_MIN = 10, RING_MAX = 16, CLOSE = 4;
    static final int CIRCLE_MIN = 400, CIRCLE_MAX = 1000, BLEED_TICKS = 200, RUN_OFF = 80;

    public Gloson(EntityType<? extends Gloson> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal"));
        goalSelector.addGoal(2, new ChargeGoal());
        goalSelector.addGoal(3, new CircleGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return e instanceof com.mechanical.people.Tribesperson; // only people
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    /** A night thing: gone again when nobody is near. */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return true;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity t) {
            t.addEffect(new MobEffectInstance(NightFauna.BLEEDING, BLEED_TICKS), this);
            playSound(SoundEvents.HOGLIN_ATTACK, 1.0F, 1.4F);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() ? null : SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.HOGLIN_STEP, 0.08F, 1.5F); // light trotters
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.5F; // a thin, sow-like squeal rather than the hoglin's bellow
    }

    /** Someone it may go for now: a player it can hunt, not sheltering by a fire. */
    boolean quarry(LivingEntity p) {
        return canHunt(p) && (fearless() || !NightDangers.nearFire(level(), p.position(), 8));
    }

    /** In the dark, keeps a ring 10-16 blocks round its quarry, watching; when the time is up, or he comes close, it charges. */
    class CircleGoal extends Goal {
        private LivingEntity prey;
        private long chargeAt;
        private int repath;

        CircleGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (random.nextInt(10) != 0 || !huntingTime() || retreating() || isDozing() || getTarget() != null) return false;
            prey = level().getNearestPlayer(Gloson.this, SIGHT);
            if (prey == null || !quarry(prey)) // no player about: tribespeople out after dark will do
                prey = level().getEntitiesOfClass(com.mechanical.people.Tribesperson.class, getBoundingBox().inflate(SIGHT), Gloson.this::quarry)
                        .stream().min(java.util.Comparator.comparingDouble(Gloson.this::distanceToSqr)).orElse(null);
            return prey != null && quarry(prey);
        }

        @Override
        public void start() {
            chargeAt = level().getGameTime() + CIRCLE_MIN + random.nextInt(CIRCLE_MAX - CIRCLE_MIN);
            repath = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return prey.isAlive() && canHunt(prey) && huntingTime() && !retreating() && getTarget() == null && distanceTo(prey) < SIGHT * 1.5;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(prey, 30, 30);
            if (prowlRoundFire(prey, 0.9)) return;
            boolean ready = level().getGameTime() >= chargeAt || distanceTo(prey) < CLOSE || getLastHurtByMob() == prey;
            if (ready && !torchKeepsOff(prey) && quarry(prey)) {
                setTarget(prey);
                return;
            }
            if (--repath > 0 && !getNavigation().isDone()) return;
            repath = 30;
            double angle = Math.atan2(getZ() - prey.getZ(), getX() - prey.getX()) + (random.nextBoolean() ? 0.7 : -0.7) * (0.5 + random.nextDouble());
            double r = RING_MIN + random.nextDouble() * (RING_MAX - RING_MIN);
            getNavigation().moveTo(prey.getX() + Math.cos(angle) * r, prey.getY(), prey.getZ() + Math.sin(angle) * r, 1.0);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** Straight at the target at a run; one cut and on past, then off into the dark (retreat) to circle again. */
    class ChargeGoal extends Goal {
        private int repath, cooldown;

        ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canHunt(getTarget()) && !retreating();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            repath = 0;
            playSound(SoundEvents.HOGLIN_ANGRY, 1.2F, 1.5F);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30, 30);
            if (cooldown > 0) cooldown--;
            if (!quarry(t) || torchKeepsOff(t)) {
                setTarget(null); // back to circling
                return;
            }
            if (--repath <= 0) {
                getNavigation().moveTo(t, 1.9);
                repath = 5;
            }
            if (cooldown == 0 && isWithinMeleeAttackRange(t) && doHurtTarget((ServerLevel) level(), t)) {
                cooldown = 20;
                retreat(t.position(), RUN_OFF);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
