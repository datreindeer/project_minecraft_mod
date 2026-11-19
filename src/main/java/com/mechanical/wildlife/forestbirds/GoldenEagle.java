package com.mechanical.wildlife.forestbirds;

import java.util.Comparator;

import com.mechanical.CarcassEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Golden eagle: the king of the mountains, alone or as a pair. Soars high in wide circles (SoaringBird); seeing a hare,
 * a ptarmigan or another small animal ({@link ForestBirdsFauna#EAGLE_PREY}) on open ground it folds its wings and dives,
 * kills it on the strike, comes down by it and feeds for half a minute before the kill is gone. Small game a hunter has
 * just shot (a light {@link CarcassEntity} lying free) is fair game too: it swoops, snatches the body up in its talons
 * (the carcass rides as its passenger, under it) and carries it off to eat somewhere safe. Shoot it down and the body
 * falls; scare it off its meal and the meal is yours. No carcass of its own (feathers, Eagle Feathers and Talons, loot).
 */
public class GoldenEagle extends SoaringBird {
    static final Kind KIND = new Kind("golden_eagle", 0.5, 16, 30, 900, 1800, 300, 900, false, false, true, 1.5,
            () -> SoundEvents.FOX_SCREECH, 1.25F);
    static final double SPOT_RANGE = 36, STRIKE = 1.6;
    /** Ticks it feeds at a kill before nothing is left of it. */
    static final int FEED_TICKS = 600;
    private LivingEntity prey;
    private CarcassEntity loot, meal;
    private int feeding, lookForMeal;

    public GoldenEagle(EntityType<? extends GoldenEagle> type, Level level) {
        super(type, level, KIND);
    }

    public boolean isCarrying() {
        return getFirstPassenger() instanceof CarcassEntity;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof CarcassEntity && getPassengers().isEmpty();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!isFlying() && meal == null && lookForMeal > 0 && --lookForMeal % 10 == 0) meal = findMeal();
        if (isFlying() || meal == null) return;
        // feeding at a kill on the ground
        if (!meal.isAlive() || meal.hauler() != null || meal.isPassenger() || distanceToSqr(meal) > 9) {
            meal = null;
            return;
        }
        restTimer = Math.max(restTimer, 40);
        getLookControl().setLookAt(meal);
        if (++feeding % 20 == 0) playSound(SoundEvents.GENERIC_EAT.value(), 0.7F, 0.8F);
        if (feeding >= FEED_TICKS) {
            com.mechanical.Blood.splatter(level, meal.blockPosition(), 1, 3);
            meal.discard();
            meal = null;
        }
    }

    @Override
    protected Vec3 cruise() {
        if (scared || isCarrying()) return super.cruise();
        if ((tickCount + getId()) % 20 == 0) spot();
        if (loot != null && (!loot.isAlive() || loot.hauler() != null || loot.isPassenger())) loot = null;
        if (prey != null && !prey.isAlive()) prey = null;
        Entity aim = loot != null ? loot : prey;
        if (aim == null) return super.cruise();
        Vec3 at = aim.position().add(0, aim.getBbHeight() * 0.5, 0);
        if (distanceToSqr(at) < STRIKE * STRIKE) {
            if (aim == loot) snatch(loot);
            else strike(prey);
            return getDeltaMovement();
        }
        double dist = Math.sqrt(horizontalDistSqr(at));
        // the stoop: wings folded, straight down at it, fast
        return seek(dist > 8 ? new Vec3(at.x, Math.max(at.y + 6, getY() - 0.3), at.z) : at, speed() * (dist > 8 ? 1.4 : 2.2));
    }

    /** Looks for something to dive on: game a hunter left lying first, else live prey in the open. */
    private void spot() {
        if (loot == null) loot = level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(SPOT_RANGE, 40, SPOT_RANGE),
                c -> c.isLight() && !c.isLarge() && c.hauler() == null && c.hungAt() == null && !c.isPassenger() && !nearPlayer(c, 4))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (loot == null && prey == null) prey = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SPOT_RANGE, 40, SPOT_RANGE),
                e -> e.isAlive() && e.getType().builtInRegistryHolder().is(ForestBirdsFauna.EAGLE_PREY) && e.onGround() && e.getY() < getY() && level().canSeeSky(e.blockPosition()))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private boolean nearPlayer(Entity e, double r) {
        Player p = level().getNearestPlayer(e, r);
        return p != null && !p.isSpectator();
    }

    private void snatch(CarcassEntity carcass) {
        loot = null;
        if (!carcass.startRiding(this, true, false)) return;
        playSound(SoundEvents.FOX_SCREECH, 1.5F, 1.4F);
        Player near = level().getNearestPlayer(this, 48);
        // off with it, away from the hunter, to eat it somewhere quiet
        target = pickTarget(near != null ? near.position() : position().add(random.nextGaussian(), 0, random.nextGaussian()));
        flightTimer = 240 + random.nextInt(160);
        landingSpot = null;
        setDeltaMovement(getDeltaMovement().add(0, 0.4, 0));
    }

    private void strike(LivingEntity victim) {
        prey = null;
        victim.hurtServer((ServerLevel) level(), damageSources().mobAttack(this), 40.0F);
        landingSpot = victim.position();
        landingTimer = 0;
    }

    @Override
    protected void land() {
        super.land();
        CarcassEntity carried = getFirstPassenger() instanceof CarcassEntity c ? c : null;
        if (carried != null) {
            ejectPassengers();
            meal = carried;
        } else {
            meal = findMeal();
            lookForMeal = 60; // came down by its own kill: the carcass appears once the death is over, so look a moment later too
        }
        feeding = 0;
    }

    private CarcassEntity findMeal() {
        return level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(3), c -> c.hauler() == null && !c.isPassenger() && c.hungAt() == null)
                .stream().findFirst().orElse(null);
    }

    @Override
    protected void takeOff(boolean frightened, Vec3 from) {
        meal = null; // scared off its meal: it stays where it lies
        lookForMeal = 0;
        super.takeOff(frightened, from);
    }

    @Override
    public void die(DamageSource source) {
        ejectPassengers(); // the snatched body falls
        super.die(source);
    }
}
