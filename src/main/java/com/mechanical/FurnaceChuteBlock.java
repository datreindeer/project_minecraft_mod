package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Smelting Furnace's roof: a firebrick block with a funnel (like a hopper's: a wide basin narrowing to a hole down
 * into the chamber), the middle of the top layer. Items thrown onto it drop through and the furnace's tap takes them
 * in ({@link FurnaceTapBlockEntity}); the molten pool glows up through the hole. LIT (set by the tap): its rim glows.
 */
public class FurnaceChuteBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** The basin x/z 3..13 from y 6 up, the hole 5..11 below it. Items fall through; clicking still hits the full block. */
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.or(box(3, 6, 3, 13, 16, 13), box(5, 0, 5, 11, 6, 11)), BooleanOp.ONLY_FIRST);

    public FurnaceChuteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }
}
