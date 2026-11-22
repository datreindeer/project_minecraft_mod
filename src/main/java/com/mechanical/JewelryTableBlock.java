package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Steel Casting Table ({@code casting}) and the Jeweler's Bench (see {@link Jewelry}): a work top that holds what is
 * laid on it ({@link JewelryTableBlockEntity}); the casting table ticks, drawing heat from a Clay Kiln beside it.
 */
public class JewelryTableBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 14, 16);
    public final boolean casting;

    public JewelryTableBlock(Properties properties, boolean casting) {
        super(properties);
        this.casting = casting;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof JewelryTableBlockEntity table) || !table.accepts(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) table.use(server, player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof JewelryTableBlockEntity table) || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) table.useEmptyHand(server, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JewelryTableBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!casting || level.isClientSide() || type != Jewelry.TABLE_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<JewelryTableBlockEntity>) (l, p, s, t) -> t.serverTick((ServerLevel) l, p);
    }
}
