package com.mechanical.people;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A band at a player's fire (VisitGoal, AT_CAMP or STAYING; docs/living-world-plan.md C): everyone sits round the fire
 * instead of wandering. With water near, the fishers go and fish there and some of the others go along and sit by them.
 * The leader now and then gets up and walks over to a player for a while. Through a granted night (STAYING) all but the
 * warriors sleep on Straw Beds by the fire and the warriors keep watch (CampLife).
 */
final class VisitRest {
    static void tick(Tribesperson mob, ServerLevel level, BlockPos fire, boolean staying) {
        CampLife life = mob.life;
        Vec3 flame = Vec3.atCenterOf(fire);
        if (staying && life.phase() == CampLife.NIGHT) {
            if (life.warrior()) life.guard(fire);
            else life.sleep(fire);
            return;
        }
        life.wake(); // morning (or a visit that wasn't granted the night)
        if (life.fish()) return;
        if (mob.isLeader() && leaderWalk(mob, level, fire)) return;
        if (!mob.isLeader() && !life.warrior() && mob.getId() % 3 == 0) {
            Tribesperson fisher = life.fisherNear();
            BlockPos by = fisher == null ? null : besides(level, fisher.life.fishStand, mob.getId());
            if (by != null) {
                life.sit(Vec3.atBottomCenterOf(by), Vec3.atCenterOf(fisher.life.fishSpot));
                return;
            }
        }
        life.sit(life.seat(fire, CampLife.SEAT_R), flame);
    }

    /** Now and then (about every minute while seated) the leader walks over to a player near the fire and stays a little. */
    private static boolean leaderWalk(Tribesperson mob, ServerLevel level, BlockPos fire) {
        CampLife life = mob.life;
        long now = level.getGameTime();
        Player p = life.visiting;
        if (p != null && (!p.isAlive() || p.isSpectator() || now > life.visitUntil || p.distanceToSqr(Vec3.atCenterOf(fire)) > 24 * 24)) p = life.visiting = null;
        if (p == null && life.sitting() && mob.getRandom().nextInt(1200) == 0) {
            p = level.getNearestPlayer(fire.getX() + 0.5, fire.getY(), fire.getZ() + 0.5, 24, false);
            if (p != null && !p.isSpectator()) {
                life.visiting = p;
                life.visitUntil = now + 300;
            } else p = null;
        }
        if (p == null) return false;
        life.stand();
        mob.getLookControl().setLookAt(p);
        if (mob.distanceToSqr(p) > 3 * 3) {
            if (mob.getNavigation().isDone() || mob.tickCount % 40 == 0) mob.getNavigation().moveTo(p, 0.6);
        } else mob.getNavigation().stop();
        return true;
    }

    /** A dry, standable block beside {@code stand} (which side by {@code seed}), or null. */
    private static BlockPos besides(ServerLevel level, BlockPos stand, int seed) {
        for (int i = 0; i < 4; i++) {
            BlockPos p = stand.relative(Direction.from2DDataValue((seed + i) & 3));
            if (CampLife.standable(level, p)) return p;
        }
        return null;
    }

    private VisitRest() {}
}
