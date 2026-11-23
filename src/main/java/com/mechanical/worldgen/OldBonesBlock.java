package com.mechanical.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Old bones lying out in the wild (docs/terrain-plan.md phase 4), thin bone models instead of full Bone Blocks. Rib Cage:
 * a pair of ribs arching over the backbone, which runs along FACING; LOW sinks it half into the ground (a skeleton in the
 * sand, only the rib tips showing). Mammoth Tusk: a curved tusk rising out of the ground and bending toward FACING;
 * LOW is its root (the lower block), the other its upper, curving end. Passable, like a Bone Pile. Models and loot:
 * scripts/gen_land_decor.py.
 */
public class OldBonesBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty LOW = BooleanProperty.create("low");
    private static final VoxelShape FULL = box(1, 0, 1, 15, 13, 15), SUNK = box(1, 0, 1, 15, 6, 15), TUSK = box(4, 0, 4, 12, 16, 12);
    private final boolean tusk;

    public OldBonesBlock(boolean tusk, Properties properties) {
        super(properties);
        this.tusk = tusk;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LOW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LOW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(LOW, tusk);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return tusk ? TUSK : state.getValue(LOW) ? SUNK : FULL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
