package com.mechanical.people;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A Trader Caravan at its stop (Caravans, docs/traders-plan.md C). The handler stands at the stop holding the animals'
 * leads, turning now and then to look at them. The leader potters round the handler within {@link #LEADER_RANGE}, slowly
 * like the old man he is ({@link #LEADER_SPEED}), and stays put while a player stands close (a customer). The guards walk
 * a wide ring round the leader ({@link #GUARD_MIN}-{@link #GUARD_MAX} out) as slowly, stopping at each point to watch
 * outwards. On the road (Caravans.ARRIVING, LEAVING) they walk as a column: the handler in front leading the animals
 * (the cart or wagon behind them), the leader behind the bed, the guards on its flanks, one at the rear. A fight (the
 * target selectors) overrides all of it.
 */
class CaravanGoal extends Goal {
    static final double LEADER_RANGE = 6, LEADER_SPEED = 0.38, GUARD_MIN = 8, GUARD_MAX = 11, GUARD_SPEED = 0.5;
    /** Ticks a leader rests between walks, and a guard watches at each point (plus a random share as much again). */
    static final int LEADER_REST = 120, GUARD_WATCH = 100;
    /** On the road: the handler's pace, how far the animals may fall behind before he waits, how long he tries to arrive. */
    static final double ROAD_SPEED = 0.6;
    static final int WAIT_FOR_TEAM = 6, ARRIVE_TICKS = 2400;
    private int travelled;
    private final Tribesperson mob;
    private int wait;
    private Vec3 watch;

    CaravanGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.caravanCamp != null && mob.role().caravan() && mob.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        wait = mob.getRandom().nextInt(40);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (mob.caravanPhase != Caravans.STOPPED) {
            road();
            return;
        }
        switch (mob.role()) {
            case HANDLER -> handler();
            case TRADER -> leader();
            default -> guard();
        }
    }

    /** Stands at the stop; looks at one of the animals now and then. */
    private void handler() {
        Vec3 stop = Vec3.atBottomCenterOf(mob.caravanCamp);
        if (mob.position().distanceToSqr(stop) > 1.0) { // jostled off it (by a guard or the animal): straight back
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(stop.x, stop.y, stop.z, LEADER_SPEED + 0.2);
            return;
        }
        mob.getNavigation().stop();
        if (--wait <= 0) {
            wait = 80 + mob.getRandom().nextInt(120);
            var animals = mob.level().getEntitiesOfClass(net.minecraft.world.entity.animal.equine.AbstractHorse.class, mob.getBoundingBox().inflate(8),
                    h -> h.getLeashHolder() == mob);
            watch = animals.isEmpty() ? null : animals.get(mob.getRandom().nextInt(animals.size())).getEyePosition();
        }
        if (watch != null) mob.getLookControl().setLookAt(watch);
    }

    /** Pottering round the handler, slow; still while a customer is near. */
    private void leader() {
        Player customer = mob.level().getNearestPlayer(mob, 4.5);
        if (customer != null && !customer.isSpectator()) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(customer);
            return;
        }
        if (!mob.getNavigation().isDone() || --wait > 0) return;
        wait = LEADER_REST + mob.getRandom().nextInt(LEADER_REST);
        Vec3 c = centre(TribeRole.HANDLER);
        double a = mob.getRandom().nextDouble() * Math.PI * 2, d = 2 + mob.getRandom().nextDouble() * (LEADER_RANGE - 2);
        mob.getNavigation().moveTo(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d, LEADER_SPEED);
    }

    /** A slow round on a wide ring about the leader, watching outwards at each point. */
    private void guard() {
        if (!mob.getNavigation().isDone()) return;
        if (watch != null) mob.getLookControl().setLookAt(watch.x, mob.getEyeY(), watch.z);
        if (--wait > 0) return;
        Vec3 c = centre(TribeRole.TRADER);
        wait = GUARD_WATCH + mob.getRandom().nextInt(GUARD_WATCH);
        // the next point a quarter turn or so on round the ring from where it stands
        double now = Math.atan2(mob.getZ() - c.z, mob.getX() - c.x), a = now + (0.6 + mob.getRandom().nextDouble() * 0.9) * (mob.getId() % 2 == 0 ? 1 : -1);
        double d = GUARD_MIN + mob.getRandom().nextDouble() * (GUARD_MAX - GUARD_MIN);
        Vec3 to = new Vec3(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
        watch = to.add(to.subtract(c).normalize().scale(10)); // out, away from the leader
        mob.getNavigation().moveTo(to.x, to.y, to.z, GUARD_SPEED);
    }

    /** The column on the road, every member's part in it. */
    private void road() {
        if (mob.role() == TribeRole.HANDLER) {
            travelled++;
            Vec3 to = Vec3.atBottomCenterOf(mob.caravanCamp);
            boolean there = mob.position().distanceToSqr(to) < 2.5 * 2.5;
            if (mob.caravanPhase == Caravans.ARRIVING && (there || travelled > ARRIVE_TICKS)) {
                Caravans.phase(mob, Caravans.STOPPED, mob.blockPosition()); // here (or as near as the way allowed)
                travelled = 0;
                return;
            }
            if (mob.caravanPhase == Caravans.LEAVING && (there || mob.level().getNearestPlayer(mob, Caravans.OUT_OF_SIGHT) == null
                    || travelled > ARRIVE_TICKS)) {
                Caravans.leaveAll(mob);
                return;
            }
            var animals = mob.level().getEntitiesOfClass(net.minecraft.world.entity.animal.equine.AbstractHorse.class, mob.getBoundingBox().inflate(16),
                    h -> h.getLeashHolder() == mob);
            if (animals.stream().anyMatch(h -> h.distanceTo(mob) > WAIT_FOR_TEAM)) {
                mob.getNavigation().stop(); // let the team catch up
                return;
            }
            if (mob.getNavigation().isDone() || travelled % 40 == 0) mob.getNavigation().moveTo(to.x, to.y, to.z, ROAD_SPEED);
            return;
        }
        Tribesperson handler = null;
        for (Tribesperson t : mob.groupNearby(48)) if (t.role() == TribeRole.HANDLER) handler = t;
        if (handler == null) return;
        Vec3 heading = Vec3.atBottomCenterOf(mob.caravanCamp).subtract(handler.position()).multiply(1, 0, 1);
        heading = heading.lengthSqr() < 0.01 ? Vec3.directionFromRotation(0, handler.getYRot()) : heading.normalize();
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        Vec3 spot = mob.role() == TribeRole.TRADER ? handler.position().subtract(heading.scale(7)) // behind the bed
                : mob.getId() % 3 == 0 ? handler.position().subtract(heading.scale(10)) // the rear guard
                : handler.position().add(side.scale(mob.getId() % 2 == 0 ? 3.5 : -3.5)).subtract(heading.scale(3)); // a flank
        if (mob.position().distanceToSqr(spot) > 2.0 * 2.0) {
            if (mob.getNavigation().isDone() || mob.tickCount % 20 == 0)
                mob.getNavigation().moveTo(spot.x, spot.y, spot.z, mob.role() == TribeRole.TRADER ? LEADER_SPEED + 0.25 : ROAD_SPEED + 0.05);
        } else mob.getNavigation().stop();
    }

    /** Where the group member with {@code role} is (the stop if they are gone). */
    private Vec3 centre(TribeRole role) {
        for (Tribesperson t : mob.groupNearby(32)) if (t.role() == role && t.isAlive()) return t.position();
        return Vec3.atBottomCenterOf(mob.caravanCamp);
    }
}
