package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The short stump left behind when a tree is felled. It doubles as a chopping block: a Rough Log stood on it
 * ({@code LOG} = 1 + index in {@link Mechanical#WOODS}, 0 = none) is split into firewood with an axe (see {@link TreeChopping}).
 */
public class StumpBlock extends Block {
    public static final IntegerProperty LOG = IntegerProperty.create("log", 0, 26); // = Mechanical.WOODS.size()
    private static final VoxelShape SHAPE = column(12, 0, 7);
    private static final VoxelShape WITH_LOG = Shapes.or(SHAPE, column(10, 7, 16));

    public StumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LOG, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOG);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LOG) > 0 ? WITH_LOG : SHAPE;
    }
}
