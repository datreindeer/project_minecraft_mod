package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.people.Neighbours.Visit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * A visiting band member (Tribesperson#visit) does what the visit's state says (TribeVisits): walks in to the player's
 * fire, then mills about it in the open (never into buildings: only spots under open sky), the leader coming up to a
 * player when they have asked to stay; through a granted night each kneels at their own spot round the fire; leaving,
 * they walk back the way they came and are gone once out of sight. With the visit forgotten they just walk off.
 */
class VisitGoal extends Goal {
    private final Tribesperson mob;
    private Visit visit;
    private int ticks;

    VisitGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.visit != null && mob.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        ticks = 0;
        visit = null;
    }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) mob.level();
        if (ticks++ % 20 == 0) visit = Neighbours.get(level).visit(mob.visit);
        if (visit == null) {
            walkOff(level);
            return;
        }
        BlockPos fire = visit.fire;
        switch (visit.state) {
            case Visit.ARRIVING -> {
                mob.travelling = true;
                ErrandGoal.walk(mob, fire, 0.8);
            }
            case Visit.AT_CAMP, Visit.STAYING -> {
                mob.travelling = false;
                if (mob.getTradingPlayer() != null) {
                    mob.getNavigation().stop();
                    return;
                }
                if (visit.state == Visit.STAYING && level.isDarkOutside()) {
                    sit(fire);
                    return;
                }
                if (mob.isWorking()) mob.setWorking(false);
                if (visit.state == Visit.AT_CAMP && visit.asked && mob.isLeader()) {
                    // come over and ask
                    Player p = level.getNearestPlayer(fire.getX() + 0.5, fire.getY(), fire.getZ() + 0.5, 24, false);
                    if (p != null && !p.isSpectator()) {
                        mob.getLookControl().setLookAt(p);
                        if (mob.distanceToSqr(p) > 3 * 3) {
                            if (mob.getNavigation().isDone() || ticks % 40 == 0) mob.getNavigation().moveTo(p, 0.6);
                        } else mob.getNavigation().stop();
                        return;
                    }
                }
                if (mob.distanceToSqr(Vec3.atBottomCenterOf(fire)) > 14 * 14) ErrandGoal.walk(mob, fire, 0.8);
                else if (mob.getNavigation().isDone() && (ticks + mob.getId()) % 160 == 0) mill(mob, fire, 3, 9);
            }
            default -> {
                mob.travelling = true;
                if (mob.isWorking()) mob.setWorking(false);
                if (mob.distanceToSqr(Vec3.atBottomCenterOf(visit.from)) < 6 * 6 || outOfSight(level)) mob.discard();
                else ErrandGoal.walk(mob, visit.from, 0.8);
            }
        }
    }

    /** Through the night: to a spot of their own two or three blocks from the fire, then kneel facing it. */
    private void sit(BlockPos fire) {
        double a = (mob.getId() * 2.399) % (Math.PI * 2); // golden angle: members spread round the fire
        Vec3 seat = new Vec3(fire.getX() + 0.5 + Math.cos(a) * 2.6, fire.getY(), fire.getZ() + 0.5 + Math.sin(a) * 2.6);
        double dx = mob.getX() - seat.x, dz = mob.getZ() - seat.z;
        if (dx * dx + dz * dz > 1.0) {
            if (mob.isWorking()) mob.setWorking(false);
            if (mob.getNavigation().isDone() || ticks % 60 == 0) mob.getNavigation().moveTo(seat.x, seat.y, seat.z, 0.6);
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(Vec3.atCenterOf(fire));
        if (!mob.isWorking()) mob.setWorking(true);
    }

    /** No player within 40 blocks, or one far off with them well away from the fire. */
    private boolean outOfSight(ServerLevel level) {
        Player p = level.getNearestPlayer(mob, 40);
        return p == null || mob.distanceToSqr(Vec3.atCenterOf(visit.fire)) > 64 * 64 && mob.distanceToSqr(p) > 24 * 24;
    }

    /** Visit over (forgotten): walk away from the nearest player and vanish out of sight. */
    private void walkOff(ServerLevel level) {
        mob.travelling = false;
        if (mob.isWorking()) mob.setWorking(false);
        Player p = level.getNearestPlayer(mob, 40);
        if (p == null) {
            mob.discard();
            return;
        }
        if (mob.getNavigation().isDone()) {
            Vec3 away = mob.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(16);
            BlockPos to = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(mob.position().add(away)));
            mob.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.8);
        }
    }

    /** A stroll to an open-sky spot {@code min}-{@code max} blocks from {@code centre} (never into a building). */
    static void mill(Tribesperson mob, BlockPos centre, int min, int max) {
        ServerLevel level = (ServerLevel) mob.level();
        for (int i = 0; i < 6; i++) {
            double a = mob.getRandom().nextDouble() * Math.PI * 2, d = min + mob.getRandom().nextDouble() * (max - min);
            BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.offset((int) Math.round(Math.cos(a) * d), 0, (int) Math.round(Math.sin(a) * d)));
            if (Math.abs(p.getY() - centre.getY()) > 3 || !level.canSeeSky(p) || !level.getFluidState(p).isEmpty()) continue;
            mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.5);
            return;
        }
    }

    @Override
    public void stop() {
        mob.travelling = false;
        if (mob.isWorking()) mob.setWorking(false);
        mob.getNavigation().stop();
    }
}
