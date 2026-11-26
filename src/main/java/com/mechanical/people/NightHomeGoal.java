package com.mechanical.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Home before dark: from the evening on, camp folk far from their fire drop what they are doing (errands, road work) and
 * walk home. Who cannot make it, getting no nearer for {@link #STUCK} ticks of the night or still out after {@link #GRACE}
 * ticks of it, is moved home to the fire. DailyRhythmGoal takes over once they are there.
 * <p>
 * shortcut: someone in an unloaded chunk does not tick, so they set off (or are moved) when it is loaded again.
 */
class NightHomeGoal extends Goal {
    /** Further from the fire than FAR they head home, until within NEAR. */
    static final int FAR = 20, NEAR = 12, STUCK = 400, GRACE = 1800;
    private final Tribesperson mob;
    private long nightSince, progressAt;
    private double best;

    NightHomeGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private double dist() {
        return Math.sqrt(mob.distanceToSqr(Vec3.atBottomCenterOf(mob.home())));
    }

    @Override
    public boolean canUse() {
        return mob.curfew() && mob.getTarget() == null && mob.getTradingPlayer() == null && !mob.isPassenger() && !mob.isLeashed() && dist() > FAR;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.curfew() && mob.getTarget() == null && mob.getTradingPlayer() == null && !mob.isPassenger() && !mob.isLeashed() && dist() > NEAR;
    }

    @Override
    public void start() {
        nightSince = -1;
        mob.errand = null; // whatever they were out for, it is over
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        long now = mob.level().getGameTime();
        double d = dist();
        mob.travelling = true;
        if (d < FAR) {
            if (mob.getNavigation().isDone()) mob.walkTo(mob.home(), 0.9); // in through the gate
        } else ErrandGoal.walk(mob, mob.home(), 0.9);
        if (mob.life.phase() != CampLife.NIGHT) {
            nightSince = -1;
            return;
        }
        if (nightSince < 0) {
            nightSince = progressAt = now;
            best = d;
        }
        if (d < best - 4) {
            best = d;
            progressAt = now;
        }
        if (now - progressAt > STUCK || now - nightSince > GRACE) bringHome();
    }

    /** Puts them on free ground a few blocks from the fire. */
    private void bringHome() {
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos home = mob.home();
        level.getChunk(home.getX() >> 4, home.getZ() >> 4);
        for (int tries = 0; tries < 40; tries++) {
            double a = mob.getRandom().nextDouble() * Math.PI * 2, r = 2 + mob.getRandom().nextDouble() * 6;
            BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(home.getX() + 0.5 + Math.cos(a) * r, 0, home.getZ() + 0.5 + Math.sin(a) * r));
            if (Math.abs(p.getY() - home.getY()) > 6 || !level.getFluidState(p.below()).isEmpty()
                    || level.getBlockState(p.below()).getBlock() instanceof CampfireBlock || level.getBlockState(p).is(Blocks.FIRE)) continue;
            Vec3 to = Vec3.atBottomCenterOf(p);
            if (!level.noCollision(mob, mob.getBoundingBox().move(to.subtract(mob.position())))) continue;
            mob.getNavigation().stop();
            mob.teleportTo(to.x, to.y, to.z);
            return;
        }
        progressAt = nightSince = mob.level().getGameTime(); // no free spot now: try again later
    }

    @Override
    public void stop() {
        mob.travelling = false;
        mob.getNavigation().stop();
    }
}
