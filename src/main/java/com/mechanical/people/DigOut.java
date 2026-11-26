package com.mechanical.people;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tribesperson shut in a hole (a pit, a sinkhole, a cave pocket: nowhere to walk to) digs a way up, one step a second:
 * clear the room above the block beside them and climb onto it. Only wild ground, rock, plants and leaves are dug, never
 * anything built, and never next to lava or water.
 */
final class DigOut {
    /** Reaching this many spots, or this far off, is not being shut in. Checks (a second apart) before the first dig. */
    private static final int ROOM = 80, FAR = 10, SURE = 5;

    static void tick(Tribesperson mob, ServerLevel level) {
        if ((mob.tickCount + mob.getId()) % 20 != 0) return;
        if (mob.role() == TribeRole.MINER || mob.isPassenger() || mob.isSleeping() || mob.isInWater() || !mob.onGround()
                || !enclosed(level, mob.blockPosition())) {
            mob.trapped = 0;
            return;
        }
        if (++mob.trapped >= SURE) step(mob, level);
    }

    /** Whether walking (a block up, three down) from {@code start} only ever leads round a small closed space. */
    static boolean enclosed(ServerLevel level, BlockPos start) {
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        todo.add(start);
        seen.add(start);
        while (!todo.isEmpty()) {
            BlockPos p = todo.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d), to = null;
                if (stand(level, n)) to = n;
                else if (open(level, p.above(2)) && stand(level, n.above())) to = n.above();
                else if (open(level, n) && open(level, n.above()))
                    for (int k = 1; k <= 3 && to == null && open(level, n.below(k - 1)); k++)
                        if (stand(level, n.below(k))) to = n.below(k);
                if (to == null || !seen.add(to)) continue;
                if (seen.size() >= ROOM || Math.abs(to.getX() - start.getX()) + Math.abs(to.getZ() - start.getZ()) > FAR) return false;
                todo.add(to);
            }
        }
        return true;
    }

    /** Room to walk through: nothing to bump into (or no higher than a slab), or a door or gate. */
    private static boolean open(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof FenceGateBlock) return true;
        VoxelShape shape = s.getCollisionShape(level, p);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.5;
    }

    /** A spot to stand in: room for body and head, something under the feet (or water to swim in). */
    private static boolean stand(ServerLevel level, BlockPos p) {
        return open(level, p) && open(level, p.above())
                && (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !open(level, p.below()) || level.getFluidState(p).is(FluidTags.WATER));
    }

    private static boolean diggable(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!RoadBuilding.natural(s) && !s.is(BlockTags.BASE_STONE_OVERWORLD) && !s.is(BlockTags.LEAVES) && !RoadBuilding.plant(s)) return false;
        for (Direction d : Direction.values()) if (!level.getFluidState(p.relative(d)).isEmpty()) return false; // no flooding the hole
        return true;
    }

    /** One step up, toward home first. */
    private static void step(Tribesperson mob, ServerLevel level) {
        BlockPos at = mob.blockPosition();
        BlockPos home = mob.home();
        Direction first = home == null ? mob.getDirection()
                : Direction.getApproximateNearest(home.getX() - at.getX(), 0, home.getZ() - at.getZ());
        if (first.getAxis() == Direction.Axis.Y) first = Direction.NORTH;
        next:
        for (Direction d : new Direction[] {first, first.getClockWise(), first.getCounterClockWise(), first.getOpposite()}) {
            BlockPos base = at.relative(d);
            if (open(level, base)) continue; // nothing to climb onto
            BlockPos[] room = {at.above(2), base.above(), base.above(2)};
            for (BlockPos p : room) if (!open(level, p) && !diggable(level, p)) continue next;
            for (BlockPos p : room) if (!open(level, p)) level.destroyBlock(p, false, mob);
            mob.getNavigation().stop();
            mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
            mob.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5); // the hop up
            return;
        }
    }

    private DigOut() {}
}
