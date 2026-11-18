package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A log lying on the ground where a felled tree landed. Chopped with an axe (hold right-click, see {@link TreeChopping}):
 * stage 0 has its bark, 1 is debarked, 2 and 3 are hewn thinner, and the last chop turns it into a Rough Log item.
 */
public class FallenLogBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final int LAST_STAGE = 3;
    private static final VoxelShape[] SHAPES_Z = {box(0, 0, 0, 16, 16, 16), box(0, 0, 0, 16, 16, 16), box(2, 0, 0, 14, 12, 16), box(4, 0, 0, 12, 8, 16)};
    private static final VoxelShape[] SHAPES_X = {box(0, 0, 0, 16, 16, 16), box(0, 0, 0, 16, 16, 16), box(0, 0, 2, 16, 12, 14), box(0, 0, 4, 16, 8, 12)};

    private final String wood;

    public FallenLogBlock(String wood, Properties properties) {
        super(properties);
        this.wood = wood;
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z).setValue(STAGE, 0));
    }

    public String wood() {
        return wood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, STAGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(AXIS) == Direction.Axis.X ? SHAPES_X : SHAPES_Z)[state.getValue(STAGE)];
    }
}
