package com.mechanical.people;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Hunting bands (no home) keep together: the leader roams, the others follow. If the leader is gone, the member with
 * the lowest id among those around takes over.
 */
class FollowBandLeaderGoal extends Goal {
    private static final double FAR = 7, NEAR = 3, SEARCH = 48;
    private final Tribesperson mob;
    private Tribesperson leader;

    FollowBandLeaderGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.home() != null || mob.isLeader() || mob.getTarget() != null || mob.getRandom().nextInt(20) != 0) return false;
        List<Tribesperson> band = mob.groupNearby(SEARCH);
        leader = band.stream().filter(Tribesperson::isLeader).findFirst().orElse(null);
        if (leader == null) {
            if (band.stream().allMatch(t -> t == mob || t.getId() > mob.getId())) mob.setLeader(true);
            return false;
        }
        return mob.distanceTo(leader) > FAR;
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(leader, 0.8);
    }

    @Override
    public boolean canContinueToUse() {
        return leader.isAlive() && mob.getTarget() == null && mob.distanceTo(leader) > NEAR && !mob.getNavigation().isDone();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        leader = null;
    }
}
