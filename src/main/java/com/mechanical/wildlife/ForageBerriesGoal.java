package com.mechanical.wildlife;

import java.util.EnumSet;

import com.mechanical.WildBerryBushBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** With nothing else to do, walks to a ripe wild berry bush nearby and eats the berries (the bush starts over). */
public class ForageBerriesGoal extends Goal {
    private static final int SEARCH = 12;
    private final PathfinderMob mob;
    private BlockPos bush;
    private int eating;

    public ForageBerriesGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null || mob.isBaby() || mob.getRandom().nextInt(200) != 0) return false;
        bush = findBush();
        return bush != null && mob.getNavigation().moveTo(bush.getX() + 0.5, bush.getY(), bush.getZ() + 0.5, 0.8);
    }

    private BlockPos findBush() {
        BlockPos origin = mob.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-SEARCH, -3, -SEARCH), origin.offset(SEARCH, 3, SEARCH))) {
            if (WildBerryBushBlock.isRipe(mob.level().getBlockState(p))) return p.immutable();
        }
        return null;
    }

    @Override
    public void start() {
        eating = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getTarget() == null && bush != null && WildBerryBushBlock.isRipe(mob.level().getBlockState(bush)) && eating < 60;
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(Vec3.atCenterOf(bush));
        if (mob.distanceToSqr(Vec3.atCenterOf(bush)) > 2.5 * 2.5) {
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(bush.getX() + 0.5, bush.getY(), bush.getZ() + 0.5, 0.8);
            return;
        }
        mob.getNavigation().stop();
        // a few seconds of munching, then the berries are gone
        if (++eating == 60 && mob.level() instanceof ServerLevel level && mob.level().getBlockState(bush).getBlock() instanceof WildBerryBushBlock block) {
            block.pick(level, bush, level.getBlockState(bush));
        }
    }
}
