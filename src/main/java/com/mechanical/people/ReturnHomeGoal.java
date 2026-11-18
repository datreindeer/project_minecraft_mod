package com.mechanical.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Camp folk walk back to their campfire: at night, after gathering or chopping, or when they've strayed too far. They
 * stop on a spot two or three blocks from the fire, never in it (fire is also blocked for their pathfinding).
 */
class ReturnHomeGoal extends Goal {
    private final Tribesperson mob;

    ReturnHomeGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private double distSqr() {
        return mob.distanceToSqr(Vec3.atBottomCenterOf(mob.home()));
    }

    @Override
    public boolean canUse() {
        BlockPos home = mob.home();
        if (home == null || mob.getTarget() != null || mob.getTradingPlayer() != null) return false;
        boolean close = mob.level().isDarkOutside() || mob.wantsHome || mob.carryingWood;
        double r = close ? 3.5 : mob.role().roam + 6;
        return distSqr() > r * r && mob.getRandom().nextInt(close ? 10 : 40) == 0;
    }

    @Override
    public void start() {
        BlockPos home = mob.home();
        double a = mob.getRandom().nextDouble() * Math.PI * 2, d = 2.0 + mob.getRandom().nextDouble();
        mob.getNavigation().moveTo(home.getX() + 0.5 + Math.cos(a) * d, home.getY(), home.getZ() + 0.5 + Math.sin(a) * d, 0.8);
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone() && mob.getTarget() == null;
    }

    @Override
    public void stop() {
        if (distSqr() < 5 * 5) mob.wantsHome = false;
        mob.getNavigation().stop();
    }
}
