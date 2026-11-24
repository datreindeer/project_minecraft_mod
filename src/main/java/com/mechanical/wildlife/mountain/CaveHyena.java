package com.mechanical.wildlife.mountain;

import java.util.Comparator;
import java.util.EnumSet;

import com.mechanical.CarcassEntity;
import com.mechanical.NightDangers;
import com.mechanical.wildlife.CaveLion;
import com.mechanical.wildlife.Hare;
import com.mechanical.wildlife.Mouflon;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.RedDeer;
import com.mechanical.wildlife.Reindeer;
import com.mechanical.wildlife.RoeDeer;
import com.mechanical.wildlife.WildBoar;
import com.mechanical.wildlife.WildHorse;

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
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Cave hyena: packs of 3-5 on savanna, plains and badlands; a night predator like the grey wolf ({@link Predator}: sleeps
 * by day, fears fire and torches, smells carcasses). A scavenger above all: day or night it runs in on any carcass lying
 * unwatched within 24 blocks - yours too, while you butcher something else - and drags it off away from people
 * (StealCarcassGoal), then the pack guards it (any player who comes within 7 blocks is driven off) and eats it at night.
 * At night a pack of three or more harasses a player: they circle at 5-6 blocks, giggling, and one at a time dart in to
 * bite from behind and back off (HarassGoal); a lone hyena or a pair won't take on a person. Big packs also mob a lone
 * Cave Lion, its old rival. Bad wounds, or the pack dropping below two, send them off.
 */
public class CaveHyena extends Predator {
    private static final Identifier TEXTURE = Ibex.tex("cave_hyena");
    private static final double SIGHT = 20, SIGHT_SNEAKING = 8, PACK_RANGE = 16, STEAL_RANGE = 24, GUARD_RANGE = 7;
    /** A stolen carcass is dragged at least this far. */
    private static final double DRAG_MIN = 6;
    /** A pack this big harasses players and mobs lions. */
    private static final int BOLD_PACK = 3;

    private CarcassEntity guarded;
    private long guardUntil;

    public CaveHyena(EntityType<? extends CaveHyena> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new HarassGoal());
        goalSelector.addGoal(4, new StealCarcassGoal());
        goalSelector.addGoal(5, new StayByKillGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new GuardKillTargetGoal());
        targetSelector.addGoal(3, new PackTargetGoal());
    }

    /** Hyenas within PACK_RANGE, itself included. */
    protected int pack() {
        return level().getEntitiesOfClass(CaveHyena.class, getBoundingBox().inflate(PACK_RANGE), CaveHyena::isAlive).size();
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        if (e instanceof CaveLion lion) // the old rivalry: only a big pack takes on a lion, and only one on its own
            return pack() >= BOLD_PACK && level().getEntitiesOfClass(CaveLion.class, lion.getBoundingBox().inflate(12)).size() == 1;
        if (e.isBaby()) return e instanceof WildHorse || e instanceof WildBoar || e instanceof RedDeer || e instanceof Ibex;
        return e instanceof RoeDeer || e instanceof Reindeer || e instanceof Mouflon || e instanceof Saiga || e instanceof Chamois
                || e instanceof Marmot || e instanceof Hare || e instanceof Ibex && !((Ibex) e).isMale();
    }

    @Override
    protected void onWoken(Player near) {
        if (pack() >= BOLD_PACK) setTarget(near);
        else super.onWoken(near);
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
        if (level().isClientSide()) return;
        if (guarded != null && (!guarded.isAlive() || level().getGameTime() > guardUntil || guarded.haulerId() >= 0 || guarded.hungAt() != null))
            guarded = null;
        LivingEntity t = getTarget();
        if (t != null && !retreating() && (getHealth() < getMaxHealth() * 0.3F || t instanceof Player && pack() < 2)) retreat(t.position(), 500);
    }

    /** The carcass this hyena or a pack mate is guarding, or null. */
    protected CarcassEntity packKill() {
        if (guarded != null) return guarded;
        for (CaveHyena mate : level().getEntitiesOfClass(CaveHyena.class, getBoundingBox().inflate(PACK_RANGE), h -> h != this && h.guarded != null))
            return mate.guarded;
        return null;
    }

    void giggle() {
        playSound(MountainFauna.HYENA_LAUGH, 1.6F, 0.9F + random.nextFloat() * 0.3F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() || !huntingTime() && packKill() == null ? null : MountainFauna.HYENA_LAUGH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.65F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP.value(), 0.15F, 0.8F);
    }

    /** Drives off any player who comes near the pack's carcass (day or night) and keeps after him while he lingers. */
    class GuardKillTargetGoal extends TargetGoal {
        private CarcassEntity kill;

        GuardKillTargetGoal() {
            super(CaveHyena.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || retreating() || random.nextInt(5) != 0) return false;
            kill = packKill();
            if (kill == null) return false;
            Player p = level().getNearestPlayer(kill, GUARD_RANGE);
            if (p == null || !canHunt(p) || torchKeepsOff(p)) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            setTarget(targetMob);
            giggle();
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = getTarget();
            return canHunt(t) && !retreating() && kill.isAlive() && t.distanceTo(kill) < GUARD_RANGE * 2;
        }
    }

    /** At night: a pack mate's quarry; a player in sight (only a big pack); a lone lion (big pack); else the nearest prey. */
    class PackTargetGoal extends TargetGoal {
        PackTargetGoal() {
            super(CaveHyena.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (random.nextInt(10) != 0 || !huntingTime() || retreating() || isDozing() || getTarget() != null) return false;
            for (CaveHyena mate : level().getEntitiesOfClass(CaveHyena.class, getBoundingBox().inflate(PACK_RANGE), h -> h != CaveHyena.this)) {
                if (canHunt(mate.getTarget())) {
                    targetMob = mate.getTarget();
                    return true;
                }
            }
            if (pack() >= BOLD_PACK) for (Player p : level().players()) {
                if (canHunt(p) && distanceTo(p) < (p.isShiftKeyDown() ? SIGHT_SNEAKING : SIGHT) && hasLineOfSight(p)) {
                    targetMob = p;
                    return true;
                }
            }
            targetMob = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SIGHT), e -> e != CaveHyena.this && !(e instanceof Player) && canHunt(e))
                    .stream().min(Comparator.comparingDouble(CaveHyena.this::distanceToSqr)).orElse(null);
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
     * The fight. Animals and lions: chase and bite. A player, with pack mates about: circle him at 5-6 blocks (each
     * hyena its own way round), and now and then - more often when he looks away - dart in, bite, and fall back out.
     * Fire and torches hold them off like the wolves (they keep 6 blocks away, giggling).
     */
    class HarassGoal extends Goal {
        private int attackCooldown, repath, backOff;
        private boolean darting;

        HarassGoal() {
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
            darting = false;
            backOff = 0;
            repath = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private boolean watched(LivingEntity t) {
            Vec3 look = t.getViewVector(1.0F).multiply(1, 0, 1).normalize();
            Vec3 toMe = position().subtract(t.position()).multiply(1, 0, 1).normalize();
            return look.dot(toMe) > 0.6 && t.hasLineOfSight(CaveHyena.this);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            if (attackCooldown > 0) attackCooldown--;
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = distanceTo(t);
            if (prowlRoundFire(t, 1.0)) return;
            if (torchKeepsOff(t)) {
                darting = false;
                if (d < 6 && --repath <= 0) {
                    Vec3 away = DefaultRandomPos.getPosAway(CaveHyena.this, 8, 4, t.position());
                    if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.2);
                    repath = 20;
                    if (random.nextInt(3) == 0) giggle();
                }
                return;
            }
            boolean harass = t instanceof Player && pack() >= 2;
            if (!harass || darting) {
                if (--repath <= 0) {
                    getNavigation().moveTo(t, harass ? 1.7 : 1.5);
                    repath = 5;
                }
                if (attackCooldown == 0 && isWithinMeleeAttackRange(t)) {
                    doHurtTarget((ServerLevel) level(), t);
                    attackCooldown = 20;
                    if (harass) { // bit him: back out to the ring
                        darting = false;
                        backOff = 40;
                        repath = 0;
                    }
                }
                return;
            }
            // circle round him, each one its own way, and now and then dart in
            if (backOff > 0) backOff--;
            if (backOff == 0 && attackCooldown == 0 && random.nextInt(watched(t) ? 120 : 40) == 0) {
                darting = true;
                giggle();
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                Vec3 off = position().subtract(t.position()).multiply(1, 0, 1);
                double a = Math.atan2(off.z, off.x) + (getId() % 2 == 0 ? 0.6 : -0.6);
                double r = 5.5;
                getNavigation().moveTo(t.getX() + Math.cos(a) * r, t.getY(), t.getZ() + Math.sin(a) * r, backOff > 0 ? 1.5 : 1.1);
                if (random.nextInt(8) == 0) giggle();
            }
        }
    }

    /**
     * A carcass lying unwatched within STEAL_RANGE (not its own kind, not carried, hung or on a travois, no fire by it):
     * runs in, takes it in its jaws and drags it 20 blocks away from the nearest player; then the pack guards it.
     */
    class StealCarcassGoal extends Goal {
        private CarcassEntity carcass;
        private boolean dragging;
        private int ticks;

        StealCarcassGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean takeable(CarcassEntity c) {
            return c.isAlive() && c.animal() != getType() && c.hungAt() == null && c.haulerId() < 0 && !c.isPassenger() && !c.isLarge()
                    && (fearless() || !NightDangers.nearFire(level(), c.position(), 8));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || retreating() || packKill() != null || random.nextInt(20) != 0) return false;
            carcass = level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(STEAL_RANGE), this::takeable).stream()
                    .filter(c -> level().getNearestPlayer(c, 3) == null)
                    .min(Comparator.comparingDouble(CaveHyena.this::distanceToSqr)).orElse(null);
            return carcass != null && getNavigation().moveTo(carcass, 1.5);
        }

        @Override
        public void start() {
            dragging = false;
            ticks = 600;
            giggle();
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && takeable(carcass) && getTarget() == null && (!dragging || !getNavigation().isDone() && carcass.distanceTo(CaveHyena.this) < 4);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!dragging) {
                double reach = 1.2 + (getBbWidth() + carcass.getBbWidth()) / 2;
                if (distanceToSqr(carcass) > reach * reach) {
                    if ((tickCount + getId()) % 20 == 0) getNavigation().moveTo(carcass, 1.5);
                    return;
                }
                Player near = level().getNearestPlayer(CaveHyena.this, 32);
                Vec3 to = near != null ? DefaultRandomPos.getPosAway(CaveHyena.this, 20, 6, near.position()) : LandRandomPos.getPos(CaveHyena.this, 20, 6);
                // somewhere well away: a spot a few steps off isn't dragging it off (it tries another next tick)
                if (to == null || to.distanceToSqr(carcass.position()) < DRAG_MIN * DRAG_MIN || !getNavigation().moveTo(to.x, to.y, to.z, 1.1)) return;
                dragging = true;
                return;
            }
            // in its jaws, dragging on the ground beside the head
            Vec3 mouth = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.2));
            Vec3 move = new Vec3(mouth.x, carcass.getY() + (getY() - carcass.getY()) * 0.5, mouth.z).subtract(carcass.position());
            if (level().noCollision(carcass, carcass.getBoundingBox().move(move))) carcass.setPos(carcass.position().add(move));
        }

        @Override
        public void stop() {
            if (dragging && carcass.isAlive()) {
                guarded = carcass;
                guardUntil = level().getGameTime() + 3600;
            }
            carcass = null;
        }
    }

    /** With nothing to fight, keeps within 5 blocks of the pack's carcass. */
    class StayByKillGoal extends Goal {
        StayByKillGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            CarcassEntity kill = packKill();
            return getTarget() == null && !isDozing() && kill != null && distanceTo(kill) > 5 && getNavigation().moveTo(kill, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && !getNavigation().isDone();
        }
    }
}
