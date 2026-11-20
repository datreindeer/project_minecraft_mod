package com.mechanical.wildlife.coldhunters;

import java.util.Comparator;
import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Hare;
import com.mechanical.wildlife.Pheasant;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.Ptarmigan;
import com.mechanical.wildlife.RedDeer;
import com.mechanical.wildlife.Reindeer;
import com.mechanical.wildlife.RoeDeer;
import com.mechanical.wildlife.Squirrel;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.feline.CatSoundVariants;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Eurasian lynx: a long-legged, short-tailed cat of the taiga and the old forests, alone or a mother with her kitten.
 * Sleeps by day and slips away from people then. At night it stalks roe deer, hares, grouse and squirrels: creeps up
 * crouched, freezes while its quarry looks its way, and pounces from 2-6 blocks. A player on his own (nobody else within
 * {@link #COMPANY} blocks) is quarry too; company, fire or a torch keep it off (a {@link Predator}).
 */
public class Lynx extends Predator {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/lynx.png");
    private static final Identifier KITTEN = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/lynx_kitten.png");
    private static final float POUNCE_DAMAGE = 6.0F;
    /** A player with another within this many blocks is left alone. */
    static final double COMPANY = 24;
    private static final double SIGHT = 16, SIGHT_SNEAKING = 6, PREY_RANGE = 20, POUNCE_MIN = 2.0, POUNCE_MAX = 6.0;

    public Lynx(EntityType<? extends Lynx> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal"));
        goalSelector.addGoal(2, new AmbushGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new StalkTargetGoal());
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        if (isBaby()) return false;
        return e instanceof Hare || e instanceof Rabbit || e instanceof Squirrel || e instanceof Ptarmigan || e instanceof Pheasant
                || e instanceof Lemming || e instanceof RoeDeer && !e.isBaby() || (e instanceof Reindeer || e instanceof RedDeer) && e.isBaby();
    }

    /** No one else about: someone out alone at night. */
    static boolean alone(Player p) {
        return p.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(COMPANY), o -> o != p && !o.isSpectator()).isEmpty();
    }

    @Override
    protected boolean canHunt(LivingEntity e) {
        if (e instanceof Player p && !alone(p)) return false;
        return super.canHunt(e);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? KITTEN : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.6F; // the second of a pair is her kitten
    }

    @Override
    public double fearMultiplier() {
        return 1.5;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && getTarget() != null && getHealth() < getMaxHealth() * 0.4F && !retreating()) retreat(getTarget().position(), 600);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // the hoarse mating call carries through the night forest; silent while stalking
        return isDozing() || isCrouching() || !huntingTime() ? null : SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC).adultSounds().strayAmbientSound().value();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC).adultSounds().deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.1F : 0.6F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // big soft paws: barely a sound
    }

    private void hiss() {
        playSound(SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC).adultSounds().hissSound().value(), 1.0F, 0.55F);
    }

    /** At hunting time: a lone player it can see within 16 blocks (6 if he sneaks), else the nearest prey within 20. */
    class StalkTargetGoal extends TargetGoal {
        StalkTargetGoal() {
            super(Lynx.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || random.nextInt(10) != 0 || !huntingTime() || retreating() || isDozing() || getTarget() != null) return false;
            for (Player p : level().players()) {
                if (canHunt(p) && distanceTo(p) < (p.isShiftKeyDown() ? SIGHT_SNEAKING : SIGHT) && hasLineOfSight(p)) {
                    targetMob = p;
                    return true;
                }
            }
            targetMob = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PREY_RANGE), Lynx.this::canHunt)
                    .stream().min(Comparator.comparingDouble(Lynx.this::distanceToSqr)).orElse(null);
            return targetMob != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = getTarget();
            if (!canHunt(t) || retreating()) return false;
            if (!huntingTime() && getLastHurtByMob() != t) return false;
            return distanceTo(t) < getFollowDistance();
        }

        @Override
        public void start() {
            setTarget(targetMob);
            super.start();
        }
    }

    /**
     * Creep crouched to just behind the quarry, freezing while it looks this way; pounce from 2-6 blocks when unwatched,
     * then fight with claws. Fire or a torch hold it off (it hisses and backs away).
     */
    class AmbushGoal extends Goal {
        private int pounceCooldown, attackCooldown, repath;
        private boolean open;

        AmbushGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
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
            open = getLastHurtByMob() == getTarget();
            repath = 0;
        }

        @Override
        public void stop() {
            setCrouching(false);
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private boolean watched(LivingEntity t) {
            Vec3 look = t.getViewVector(1.0F).multiply(1, 0, 1).normalize();
            Vec3 toMe = position().subtract(t.position()).multiply(1, 0, 1).normalize();
            return look.dot(toMe) > 0.7 && t.hasLineOfSight(Lynx.this);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            if (pounceCooldown > 0) pounceCooldown--;
            if (attackCooldown > 0) attackCooldown--;
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = distanceTo(t);
            if (isLeaping()) {
                if (d < 1.8 && attackCooldown == 0) {
                    t.hurtServer((ServerLevel) level(), damageSources().mobAttack(Lynx.this), POUNCE_DAMAGE);
                    if (t instanceof net.minecraft.world.entity.animal.Animal) AnimalFear.panic(t, position());
                    attackCooldown = 20;
                    hiss();
                }
                return;
            }
            if (prowlRoundFire(t, 0.8)) {
                setCrouching(false);
                return;
            }
            if (torchKeepsOff(t)) {
                setCrouching(false);
                if (d < 6 && --repath <= 0) {
                    Vec3 away = DefaultRandomPos.getPosAway(Lynx.this, 10, 4, t.position());
                    if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.2);
                    repath = 20;
                    if (random.nextInt(3) == 0) hiss();
                } else if (d >= 6) getNavigation().stop();
                return;
            }
            boolean watched = t instanceof Player && watched(t);
            if (!open && pounceCooldown == 0 && d > POUNCE_MIN && d < POUNCE_MAX && !watched && onGround() && hasLineOfSight(t)) {
                pounce(t);
                return;
            }
            if (open || pounceCooldown > 0) {
                setCrouching(false);
                if (--repath <= 0) {
                    getNavigation().moveTo(t, 1.5);
                    repath = 5;
                }
                if (attackCooldown == 0 && isWithinMeleeAttackRange(t)) {
                    doHurtTarget((ServerLevel) level(), t);
                    attackCooldown = 20;
                }
                return;
            }
            setCrouching(true);
            if (watched && d < 14) {
                getNavigation().stop(); // freeze
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                Vec3 look = t.getViewVector(1.0F).multiply(1, 0, 1).normalize();
                Vec3 spot = d > POUNCE_MAX ? t.position().subtract(look.scale(3)) : t.position();
                getNavigation().moveTo(spot.x, t.getY(), spot.z, 0.75);
            }
        }

        private void pounce(LivingEntity t) {
            setCrouching(false);
            setLeaping(true);
            pounceCooldown = 80;
            attackCooldown = 0;
            getNavigation().stop();
            Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1);
            double reach = Math.min(1.2, dir.length() * 0.18);
            dir = dir.normalize().scale(reach);
            setDeltaMovement(dir.x, 0.42, dir.z);
            hiss();
        }
    }
}
