package com.mechanical.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * A camp member's walk to another camp and back (Settlements picks who and where): a trader to the player camp's fire,
 * where they wait a while for someone to talk to (TribeDialog), or a visit to another tribe's camp nearby. On the way
 * they wear the ground (Tribesperson#travelling), so trodden paths form between the camps. Night or a long delay sends
 * them home.
 */
class ErrandGoal extends Goal {
    static final int GO = 0, STAY = 1, BACK = 2;
    static final long TRADE_STAY = 2400, VISIT_STAY = 1200, TIMEOUT = 12000;

    static final class Errand {
        final BlockPos target;
        private final boolean trade;
        final long since;
        int phase;
        long arrived;
        boolean greeted;

        Errand(BlockPos target, boolean trade, long since) {
            this.target = target;
            this.trade = trade;
            this.since = since;
        }

        boolean trade() {
            return trade && phase != BACK;
        }
    }

    private final Tribesperson mob;
    private int idle;

    ErrandGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.errand != null && mob.getTarget() == null && mob.home() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        Errand e = mob.errand;
        ServerLevel level = (ServerLevel) mob.level();
        long now = level.getGameTime();
        if (e.phase != BACK && (level.isDarkOutside() || now - e.since > TIMEOUT)) e.phase = BACK;
        switch (e.phase) {
            case GO -> {
                mob.travelling = true;
                if (mob.distanceToSqr(Vec3.atBottomCenterOf(e.target)) < 6 * 6) {
                    e.phase = STAY;
                    e.arrived = now;
                    mob.getNavigation().stop();
                } else walk(mob, e.target, 0.75);
            }
            case STAY -> {
                mob.travelling = false;
                if (now - e.arrived > (e.trade ? TRADE_STAY : VISIT_STAY)) e.phase = BACK;
                if (e.trade && !e.greeted) {
                    e.greeted = true;
                    for (ServerPlayer p : TribeVisits.playersNear(level, e.target, 32))
                        mob.say(p, Component.translatable("chat.mechanical.settlement.trader", mob.tribe().name()));
                }
                if (mob.getTradingPlayer() != null) mob.getNavigation().stop();
                else if (mob.getNavigation().isDone() && ++idle % 8 == 0) VisitGoal.mill(mob, e.target, 3, 7);
            }
            default -> {
                mob.travelling = true;
                if (mob.distanceToSqr(Vec3.atBottomCenterOf(mob.home())) < 8 * 8) mob.errand = null;
                else walk(mob, mob.home(), 0.75);
            }
        }
    }

    @Override
    public void stop() {
        mob.travelling = false;
        mob.getNavigation().stop();
    }

    /**
     * Walks toward a spot that may be far off: paths are found 16 blocks at a time along the straight line (navigation
     * only plans within follow range), with a random step toward it when the ground there won't do.
     */
    static void walk(Tribesperson mob, BlockPos target, double speed) {
        if (!mob.getNavigation().isDone()) return;
        Vec3 to = Vec3.atBottomCenterOf(target);
        Vec3 d = to.subtract(mob.position());
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        if (dist < 20) {
            mob.getNavigation().moveTo(to.x, to.y, to.z, speed);
            return;
        }
        BlockPos wp = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(mob.getX() + d.x / dist * 16, 0, mob.getZ() + d.z / dist * 16));
        if (!mob.getNavigation().moveTo(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5, speed)) {
            Vec3 r = LandRandomPos.getPosTowards(mob, 16, 7, to);
            if (r != null) mob.getNavigation().moveTo(r.x, r.y, r.z, speed);
        }
    }
}
