package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.WildBerryBushBlock;
import com.mechanical.WildMushroomBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Gatherers walk to a ripe wild berry bush (or a mushroom in a patch) near camp, kneel, pick it, then head home. */
class GatherGoal extends Goal {
    private static final int RANGE = 14, PICK_TICKS = 50;
    private final Tribesperson mob;
    private BlockPos target;
    private int ticks, kneel;

    GatherGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    static boolean worthPicking(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (WildBerryBushBlock.isRipe(s)) return true;
        if (!(s.getBlock() instanceof WildMushroomBlock)) return false;
        // leave lone mushrooms so the patch can spread back
        int same = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3))) if (level.getBlockState(p).is(s.getBlock())) same++;
        return same >= 3;
    }

    @Override
    public boolean canUse() {
        if (!mob.canWork(TribeRole.GATHERER) || mob.getRandom().nextInt(80) != 0) return false;
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos c = mob.workCenter();
        target = null;
        double best = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-RANGE, -4, -RANGE), c.offset(RANGE, 4, RANGE))) {
            double d = p.distSqr(mob.blockPosition());
            if (d < best && worthPicking(level, p)) { best = d; target = p.immutable(); }
        }
        return target != null;
    }

    @Override
    public void start() {
        ticks = kneel = 0;
        mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.7);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < 400 && kneel <= PICK_TICKS && mob.getTarget() == null && worthPicking((ServerLevel) mob.level(), target);
    }

    @Override
    public void tick() {
        ticks++;
        mob.getLookControl().setLookAt(Vec3.atCenterOf(target));
        if (mob.distanceToSqr(Vec3.atBottomCenterOf(target)) > 2.2 * 2.2) {
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.7);
            return;
        }
        mob.getNavigation().stop();
        if (kneel == 0) mob.setWorking(true);
        if (++kneel % 15 == 0) mob.swing(InteractionHand.OFF_HAND, SwingAnimation.DEFAULT);
        if (kneel < PICK_TICKS) return;
        ServerLevel level = (ServerLevel) mob.level();
        BlockState s = level.getBlockState(target);
        if (s.getBlock() instanceof WildBerryBushBlock bush) {
            bush.pick(level, target, s); // the berries go into the gatherer's basket
        } else {
            level.removeBlock(target, false);
            level.playSound(null, target, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.7F, 1.3F);
        }
        mob.wantsHome = true;
        kneel++;
    }

    @Override
    public void stop() {
        mob.setWorking(false);
        target = null;
    }
}
