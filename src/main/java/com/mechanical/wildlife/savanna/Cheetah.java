package com.mechanical.wildlife.savanna;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.BloodMoon;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildHorse;
import com.mechanical.wildlife.warm.Onager;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cheetah: a slender day hunter of the open savanna, alone or a pair. It stalks crouched to within {@link #SPRINT_RANGE}
 * blocks of a gazelle, wild horse or onager, then bursts into the fastest sprint of any animal. The sprint lasts
 * {@link #SPRINT_TICKS} ticks: if the prey isn't down by then it gives up, panting, and won't hunt again for
 * {@link #TIRED_TICKS}. A bite at full speed trips the prey (slows it). It is shy: it keeps away from people, and only a
 * badly hurt one (3 hearts or less) is something it will run down. Sleeps at night.
 */
public class Cheetah extends Predator {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/cheetah.png");
    public static final int SPRINT_TICKS = 100, TIRED_TICKS = 600;
    private static final double SPRINT_RANGE = 16, SPRINT_SPEED = 2.6, STALK_SPEED = 0.6;
    private static final float WOUNDED_PLAYER = 6.0F;

    private int sprint, tired;

    public Cheetah(EntityType<? extends Cheetah> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal")); // a loner
        goalSelector.addGoal(2, new SprintGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (e, level) -> tired == 0 && huntingTime() && !retreating() && quarry(e)));
    }

    /** What it runs down: its prey, or a person in a bad way. */
    private boolean quarry(LivingEntity e) {
        if (e instanceof Player p) return canHunt(p) && p.getHealth() <= WOUNDED_PLAYER;
        return isPrey(e);
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return e instanceof Gazelle || e instanceof WildHorse || e instanceof Onager || e instanceof Ostrich && e.isBaby();
    }

    /** Shy of people unless it is already after one (see {@link #quarry}). */
    @Override
    public boolean fearsPlayers() {
        return retreating() || !(getTarget() instanceof Player) || !huntingTime();
    }

    @Override
    protected boolean huntingTime() {
        return level().isBrightOutside() || BloodMoon.isActive(level()) || starving() || entityTags().contains(SavannaFauna.ALWAYS_ACTIVE);
    }

    @Override
    protected boolean sleepTime() {
        return !huntingTime();
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    /** Worn out after a failed sprint: no hunting for a while. */
    public boolean isTired() {
        return tired > 0;
    }

    /** Ticks into the current sprint (0 when not sprinting). */
    public int sprintTicks() {
        return sprint;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (tired > 0) tired--;
        if (getTarget() != null && getHealth() < getMaxHealth() * 0.4F && !retreating()) retreat(getTarget().position(), 400);
    }

    /** Out of breath: drops the chase and rests. */
    void giveUp() {
        sprint = 0;
        tired = TIRED_TICKS;
        setTarget(null);
        setCrouching(false);
        getNavigation().stop();
        playSound(SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.CLASSIC).adultSounds().pantSound().value(), 0.8F, 0.8F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() || isCrouching() ? null : SoundEvents.OCELOT_AMBIENT; // chirps and purrs; it cannot roar
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.OCELOT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isCrouching()) playSound(SoundEvents.WOLF_STEP.value(), 0.15F, 0.9F);
    }

    /**
     * The hunt: creep up crouched while the quarry is farther than SPRINT_RANGE, then sprint flat out. The sprint counts
     * up every tick; at SPRINT_TICKS it gives up. Bites at full speed (and trips what it bites).
     */
    class SprintGoal extends Goal {
        private int repath, attackCooldown;

        SprintGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return canHunt(getTarget()) && !retreating() && tired == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            sprint = 0;
            repath = 0;
        }

        @Override
        public void stop() {
            sprint = 0;
            setCrouching(false);
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            if (attackCooldown > 0) attackCooldown--;
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = distanceTo(t);
            if (sprint == 0 && d > SPRINT_RANGE) { // stalk
                setCrouching(true);
                if (--repath <= 0) {
                    getNavigation().moveTo(t, STALK_SPEED);
                    repath = 10;
                }
                return;
            }
            setCrouching(false);
            if (++sprint >= SPRINT_TICKS) {
                giveUp();
                return;
            }
            if (sprint == 1 && t instanceof net.minecraft.world.entity.animal.Animal) AnimalFear.panic(t, position()); // the burst gives it away
            if (--repath <= 0) {
                getNavigation().moveTo(t, SPRINT_SPEED);
                repath = 4;
            }
            if (attackCooldown == 0 && isWithinMeleeAttackRange(t)) {
                doHurtTarget((ServerLevel) level(), t);
                t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2)); // tripped
                attackCooldown = 15;
            }
        }
    }
}
