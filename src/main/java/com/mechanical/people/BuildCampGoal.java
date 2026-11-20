package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.people.Neighbours.Placement;
import com.mechanical.people.Neighbours.Settlement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

/**
 * Settlers at work (Settlements): before the camp has a fire they walk to the chosen site and the first to get there
 * makes it (then the camp is everyone's home); after that, by day, one member at a time (not a hunter) walks to the next
 * block of the plan and puts it down after a few swings, as long as the stockpile pays for it. A member whose camp got
 * its fire while they were away just moves in. A tribe rebuilding in a new age with a player near (AgeRebuild) has one
 * member (village or camp) take down and put up its blocks one by one, before any other building.
 */
class BuildCampGoal extends Goal {
    private static final int WORK = 30, REBUILD_WORK = 10, GIVE_UP = 600;
    private final Tribesperson mob;
    private Settlement camp;
    private Placement job;
    private Neighbours.Job rebuild;
    private int ticks, work;

    BuildCampGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if ((mob.tickCount + mob.getId()) % 20 != 0 || mob.getTarget() != null || mob.getTradingPlayer() != null || mob.errand != null || mob.visit != null) return false;
        ServerLevel level = (ServerLevel) mob.level();
        camp = Settlements.of(mob);
        if (mob.home() != null && !level.isDarkOutside() && mob.role() != TribeRole.HUNTER) {
            rebuild = AgeRebuild.nextJob(level, mob);
            if (rebuild != null) return true;
        }
        if (camp == null) return false;
        if (!camp.hasFire()) {
            // founding: everyone makes for the site (whoever arrives first lights the fire)
            job = camp.plan.isEmpty() ? null : camp.plan.get(0);
            return job != null;
        }
        if (mob.home() == null) {
            mob.settleAt(camp.centre);
            return false;
        }
        if (level.isDarkOutside() || mob.role() == TribeRole.HUNTER || !Settlements.claim(camp, mob, level.getGameTime())) return false;
        job = Settlements.nextJob(level, camp);
        return job != null;
    }

    @Override
    public void start() {
        ticks = work = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return (job != null || rebuild != null) && ticks < GIVE_UP && mob.getTarget() == null && Settlements.of(mob) == camp;
    }

    @Override
    public void tick() {
        ticks++;
        ServerLevel level = (ServerLevel) mob.level();
        if (rebuild != null) {
            tickRebuild(level);
            return;
        }
        boolean founding = !camp.hasFire();
        if (!founding) Settlements.claim(camp, mob, level.getGameTime());
        else if (job != camp.plan.get(0) || camp.hasFire()) { // someone else lit it
            job = null;
            return;
        }
        Vec3 c = Vec3.atCenterOf(job.pos());
        double dx = mob.getX() - c.x, dz = mob.getZ() - c.z, reach = founding ? 3.0 : 3.5;
        if (dx * dx + dz * dz > reach * reach || Math.abs(mob.getY() - job.pos().getY()) > 3) {
            mob.travelling = founding;
            ErrandGoal.walk(mob, job.pos(), founding ? 0.8 : 0.65);
            return;
        }
        mob.travelling = false;
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(c);
        if (++work % 10 == 0) mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        if (work >= WORK) {
            Settlements.place(level, camp, job, mob);
            job = null;
        }
    }

    /** Walks to the rebuild's next block, takes it down or puts it up, and goes on to the next. */
    private void tickRebuild(ServerLevel level) {
        if (!AgeRebuild.current(level, mob, rebuild)) { // swapped all at once meanwhile
            rebuild = null;
            return;
        }
        Settlements.claim(mob.group(), mob, level.getGameTime());
        Vec3 c = Vec3.atCenterOf(rebuild.pos());
        double dx = mob.getX() - c.x, dz = mob.getZ() - c.z;
        if (dx * dx + dz * dz > 4.5 * 4.5 || Math.abs(mob.getY() - rebuild.pos().getY()) > 4) {
            ErrandGoal.walk(mob, rebuild.pos(), 0.7);
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(c);
        if (++work % 5 == 0) mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        if (work >= REBUILD_WORK) {
            AgeRebuild.finish(level, mob, rebuild);
            rebuild = AgeRebuild.nextJob(level, mob);
            work = ticks = 0;
        }
    }

    @Override
    public void stop() {
        mob.travelling = false;
        rebuild = null;
        job = null;
        camp = null;
    }
}
