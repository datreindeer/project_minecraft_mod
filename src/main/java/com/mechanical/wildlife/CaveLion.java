package com.mechanical.wildlife;

import java.util.Comparator;
import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.CarcassEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Cave lion: alone or a pair, rare, in savanna, plains and windswept/stony land. Sleeps by day. At night an ambush hunter:
 * creeps up crouched (slow, quiet), freezes while its quarry looks its way, works round behind it, and from 3-7 blocks
 * pounces - a leap that hits hard (8). Then fights with swipes. Spotted up close for a while, it roars and charges openly.
 * Drags a fresh kill (an animal's carcass) away into the dark. Afraid of fire like the wolves (see {@link Predator}).
 */
public class CaveLion extends Predator {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/cave_lion.png");
    private static final float POUNCE_DAMAGE = 8.0F;
    /** Notices players this far (sneaking: closer); prey animals this far. */
    private static final double SIGHT = 24, SIGHT_SNEAKING = 10, PREY_RANGE = 24;
    private static final double POUNCE_MIN = 2.5, POUNCE_MAX = 7;
    private static final int POUNCE_COOLDOWN = 100, ROAR_TICKS = 40;

    private int roarCooldown;

    public CaveLion(EntityType<? extends CaveLion> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FollowHerdGoal);
        goalSelector.addGoal(2, new AmbushGoal());
        goalSelector.addGoal(4, new DragKillGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new StalkTargetGoal());
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return !e.isBaby() && (e instanceof RoeDeer || e instanceof RedDeer || e instanceof Reindeer || e instanceof WildHorse
                || e instanceof Aurochs && !((Aurochs) e).showsHorns() || e instanceof Mouflon || e instanceof WildBoar
                || e instanceof com.mechanical.people.Tribesperson);
    }

    /** Woken up close by: goes for whoever woke it. */
    @Override
    protected void onWoken(Player near) {
        setTarget(near);
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && roarCooldown > 0) roarCooldown--;
        if (!level().isClientSide() && getTarget() != null && getHealth() < getMaxHealth() * 0.25F && !retreating()) {
            retreat(getTarget().position(), 600);
        }
    }

    /** A deep roar: the ravager's roar, low, layered with a polar bear's warning growl. Heard far off. */
    void roar() {
        if (roarCooldown > 0) return;
        roarCooldown = 200;
        startCalling(ROAR_TICKS);
        level().playSound(null, getX(), getEyeY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.7F);
        level().playSound(null, getX(), getEyeY(), getZ(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.HOSTILE, 2.0F, 0.55F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // a low rumbling growl at night; silent while creeping up
        return isDozing() || isCrouching() || !huntingTime() ? null
                : SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.BIG).adultSounds().growlSound().value();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
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
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isCrouching()) playSound(SoundEvents.WOLF_STEP.value(), 0.2F, 0.6F);
    }

    /** Picks quarry at night: a player it can see within 24 blocks (10 if he sneaks), else prey. A pair shares it. */
    class StalkTargetGoal extends TargetGoal {
        StalkTargetGoal() {
            super(CaveLion.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (random.nextInt(10) != 0 || !huntingTime() || retreating() || isDozing() || getTarget() != null) return false;
            for (CaveLion mate : level().getEntitiesOfClass(CaveLion.class, getBoundingBox().inflate(24), l -> l != CaveLion.this)) {
                if (canHunt(mate.getTarget())) {
                    targetMob = mate.getTarget();
                    return true;
                }
            }
            for (Player p : level().players()) {
                if (canHunt(p) && distanceTo(p) < (p.isShiftKeyDown() ? SIGHT_SNEAKING : SIGHT) && hasLineOfSight(p)) {
                    targetMob = p;
                    return true;
                }
            }
            targetMob = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PREY_RANGE), CaveLion.this::canHunt)
                    .stream().min(Comparator.comparingDouble(CaveLion.this::distanceToSqr)).orElse(null);
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
     * Creep (crouched, slow) to a spot behind the quarry; freeze while it looks this way; pounce from 2.5-7 blocks when not
     * watched; then fight in the open. Spotted close for 3 s: roars and charges. A torch or fire holds it off (it snarls and
     * backs away, waiting).
     */
    class AmbushGoal extends Goal {
        private int pounceCooldown, watchedTicks, repath, attackCooldown;
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
            open = false;
            watchedTicks = 0;
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

        /** The quarry is looking roughly this way and can see it. */
        private boolean watched(LivingEntity t) {
            Vec3 look = t.getViewVector(1.0F).multiply(1, 0, 1).normalize();
            Vec3 toMe = position().subtract(t.position()).multiply(1, 0, 1).normalize();
            return look.dot(toMe) > 0.7 && t.hasLineOfSight(CaveLion.this);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            if (pounceCooldown > 0) pounceCooldown--;
            if (attackCooldown > 0) attackCooldown--;
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = distanceTo(t);

            if (isLeaping()) { // in the air: the pounce lands
                if (d < 2.2 && attackCooldown == 0) {
                    t.hurtServer((ServerLevel) level(), damageSources().mobAttack(CaveLion.this), POUNCE_DAMAGE);
                    if (t instanceof net.minecraft.world.entity.animal.Animal) AnimalFear.panic(t, position());
                    attackCooldown = 20;
                    playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.8F);
                }
                return;
            }
            if (prowlRoundFire(t, 0.8)) {
                setCrouching(false);
                return;
            }
            if (torchKeepsOff(t)) { // snarls and keeps its distance
                setCrouching(false);
                if (d < 7 && --repath <= 0) {
                    Vec3 away = DefaultRandomPos.getPosAway(CaveLion.this, 10, 4, t.position());
                    if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.2);
                    repath = 20;
                    if (random.nextInt(4) == 0) playSound(SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.ANGRY)
                            .adultSounds().growlSound().value(), 1.0F, 0.5F);
                } else if (d >= 7) getNavigation().stop();
                return;
            }
            boolean watched = t instanceof Player && watched(t);
            if (!open) {
                watchedTicks = watched && d < 12 ? watchedTicks + 1 : Math.max(0, watchedTicks - 1);
                if (watchedTicks > 60 || getLastHurtByMob() == t) {
                    open = true; // spotted: no point hiding any more
                    roar();
                }
            }
            if (!open && pounceCooldown == 0 && d > POUNCE_MIN && d < POUNCE_MAX && !watched && onGround() && hasLineOfSight(t)) {
                pounce(t);
                return;
            }
            if (open || pounceCooldown > 0) { // fight
                setCrouching(false);
                if (--repath <= 0) { getNavigation().moveTo(t, 1.45); repath = 5; }
                if (attackCooldown == 0 && isWithinMeleeAttackRange(t)) {
                    doHurtTarget((ServerLevel) level(), t);
                    attackCooldown = 25;
                }
                if (open && pounceCooldown == 0 && d > POUNCE_MIN && d < POUNCE_MAX && onGround() && random.nextInt(20) == 0) pounce(t);
                return;
            }
            // creep up
            setCrouching(true);
            if (watched && d < 16) {
                getNavigation().stop(); // freeze
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                Vec3 look = t.getViewVector(1.0F).multiply(1, 0, 1).normalize();
                Vec3 spot = d > POUNCE_MAX ? t.position().subtract(look.scale(4)) : t.position();
                getNavigation().moveTo(spot.x, t.getY(), spot.z, 0.75);
            }
        }

        private void pounce(LivingEntity t) {
            setCrouching(false);
            setLeaping(true);
            pounceCooldown = POUNCE_COOLDOWN;
            attackCooldown = 0;
            getNavigation().stop();
            Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1);
            double reach = Math.min(1.3, dir.length() * 0.17);
            dir = dir.normalize().scale(reach);
            setDeltaMovement(dir.x, 0.45, dir.z);
            playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 1.1F);
        }
    }

    /** With nothing to hunt, takes a fresh, untouched carcass lying within 4 blocks and drags it away from people. */
    class DragKillGoal extends Goal {
        private CarcassEntity carcass;
        private int ticks;

        DragKillGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || random.nextInt(10) != 0) return false;
            carcass = level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(4),
                    c -> c.stage() == 0 && c.tickCount < 1200 && c.animal() != Wildlife.CAVE_LION.get() && c.hungAt() == null && c.haulerId() < 0 && !c.isPassenger()).stream().findFirst().orElse(null);
            if (carcass == null) return false;
            Player near = level().getNearestPlayer(CaveLion.this, 32);
            Vec3 to = near != null ? DefaultRandomPos.getPosAway(CaveLion.this, 20, 6, near.position()) : LandRandomPos.getPos(CaveLion.this, 20, 6);
            return to != null && getNavigation().moveTo(to.x, to.y, to.z, 0.7);
        }

        @Override
        public void start() {
            ticks = 400;
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && carcass.isAlive() && getTarget() == null && !getNavigation().isDone() && carcass.distanceTo(CaveLion.this) < 4;
        }

        @Override
        public void tick() {
            // the body comes along in its jaws, dragging on the ground beside the head
            Vec3 mouth = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.4));
            Vec3 move = new Vec3(mouth.x, carcass.getY() + (getY() - carcass.getY()) * 0.5, mouth.z).subtract(carcass.position());
            if (level().noCollision(carcass, carcass.getBoundingBox().move(move))) carcass.setPos(carcass.position().add(move));
        }
    }
}
