package com.mechanical.wildlife.elkwight;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Elkwight's Antler Crown as a wall trophy: placed against the side of a block, skull plate on the wall and the
 * two palmate antlers spreading three blocks wide (the model reaches past its block; only the plate is solid to clicks).
 * {@code FACING} points out of the wall.
 */
public class AntlerCrownBlock extends HorizontalDirectionalBlock {
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, box(2, 2, 12, 14, 14, 16), Direction.SOUTH, box(2, 2, 0, 14, 14, 4),
            Direction.WEST, box(12, 2, 2, 16, 14, 14), Direction.EAST, box(0, 2, 2, 4, 14, 14));

    public AntlerCrownBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Only on a wall: the face clicked is the way it looks out. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical()) return null;
        BlockState state = defaultBlockState().setValue(FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos wall = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }
}
