package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Blood on the ground, left by a bleeding animal. More blood on the same spot makes a bigger pool ({@link #AMOUNT} 1-4);
 * a full pool runs over to the blocks around it (see {@link Blood}). Dries up step by step (random ticks).
 */
public class BloodBlock extends Block {
    public static final int MAX_AMOUNT = 4;
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, MAX_AMOUNT);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 0.25, 16);
    private static final float FADE_CHANCE = 0.25F;

    public BloodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AMOUNT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMOUNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= FADE_CHANCE) return;
        int amount = state.getValue(AMOUNT);
        if (amount > 1) level.setBlock(pos, state.setValue(AMOUNT, amount - 1), UPDATE_ALL);
        else level.removeBlock(pos, false);
    }
}
