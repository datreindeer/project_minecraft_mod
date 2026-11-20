package com.mechanical.wildlife.meadow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Molehill: a low mound of fresh crumbly soil pushed up by a {@link Mole}. Walkable (it is only a few pixels high), it
 * settles back into the ground after a while (random ticks). Digging one up flushes a burrowed mole nearby out of its
 * tunnel. Drops a little Dirt.
 */
public class MolehillBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 2, 15), box(3, 2, 3, 13, 4, 13), box(5, 4, 5, 11, 5, 11));
    /** Chance per random tick (~68 s apart on average) that it has settled: about half an hour. */
    private static final float SETTLE_CHANCE = 1 / 30F;
    /** A burrowed mole this close is flushed out when the hill is dug up. */
    private static final double FLUSH_RANGE = 6;

    public MolehillBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < SETTLE_CHANCE) level.removeBlock(pos, false);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) {
            for (Mole mole : server.getEntitiesOfClass(Mole.class, new AABB(pos).inflate(FLUSH_RANGE), Mole::isBurrowed)) {
                mole.flush(server, pos);
                break; // the one whose run this is
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
