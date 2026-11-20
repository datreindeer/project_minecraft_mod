package com.mechanical.wildlife.mountain;

import com.mechanical.BearRugBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The cave bear's skull as a trophy: sits on any block, muzzle towards whoever placed it. Decoration. */
public class CaveBearSkullBlock extends BearRugBlock {
    private static final VoxelShape NS = box(4, 0, 1, 12, 8, 15), EW = box(1, 0, 4, 15, 8, 12);

    public CaveBearSkullBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty().equals(getShape(state, level, pos, context)) ? Shapes.empty() : getShape(state, level, pos, context);
    }
}
