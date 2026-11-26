package com.mechanical.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

/**
 * A road builder at work (RoadBuilding): walks out to the next cell of the road, lays it after a few swings, and on to the
 * next, all day; from the evening they are at home (NightHomeGoal) and go on in the morning. When the road is done, walks home along it (the window of loaded chunks follows).
 */
class RoadBuildGoal extends Goal {
    static final int WORK = 20;
    private final Tribesperson mob;
    private RoadBuilding.Road road;
    private int work;

    RoadBuildGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if ((mob.tickCount + mob.getId()) % 10 != 0 || mob.getTarget() != null || mob.errand != null || mob.home() == null || mob.curfew()) return false;
        road = RoadBuilding.get(((ServerLevel) mob.level()).getServer()).roadOf(mob.getUUID());
        return road != null && (!road.done() || !mob.blockPosition().closerThan(mob.home(), RoadBuilding.HOME_R - 4));
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getTarget() == null && !mob.curfew() && road != null && RoadBuilding.get(((ServerLevel) mob.level()).getServer()).roadOf(mob.getUUID()) == road
                && (!road.done() || !mob.blockPosition().closerThan(mob.home(), RoadBuilding.HOME_R - 4));
    }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) mob.level();
        if (road.done()) {
            mob.travelling = true;
            ErrandGoal.walk(mob, mob.home(), 0.8);
            return;
        }
        int i = road.built;
        BlockPos target = i < road.cells.size() ? road.cell(i) : road.frontier();
        Vec3 c = Vec3.atBottomCenterOf(target);
        double dx = mob.getX() - c.x, dz = mob.getZ() - c.z;
        if (dx * dx + dz * dz > 4.5 * 4.5 || Math.abs(mob.getY() - c.y) > 3) {
            mob.travelling = true;
            // stand on the last laid cell, not on the one being dug or filled
            ErrandGoal.walk(mob, i > 0 && i <= road.cells.size() ? road.cell(i - 1).above() : target.above(), 0.8);
            return;
        }
        mob.travelling = false;
        mob.getNavigation().stop();
        if (i >= road.cells.size()) return; // the next section is not planned yet
        mob.getLookControl().setLookAt(c);
        if (++work % 10 == 0) mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        if (work >= WORK && road.built == i) {
            RoadBuilding.buildCell(level, road, i);
            road.built++;
            RoadBuilding.get(level.getServer()).setDirty();
            work = 0;
        }
    }

    @Override
    public void stop() {
        mob.travelling = false;
        mob.getNavigation().stop();
        road = null;
        work = 0;
    }
}
