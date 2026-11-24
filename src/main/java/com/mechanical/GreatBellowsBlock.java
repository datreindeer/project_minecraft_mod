package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Great Bellows (see {@link HighFurnace}): two wedge-shaped leather bellows on a timber frame, their boards lifted
 * in turn by cams on a shaft that comes in from a Water Wheel at its left or right side ({@link WaterPower.Driven}),
 * the bronze nozzle ({@link #FACING}) blowing into a Firebox's tuyere. Its blast is the power it gets
 * ({@link GreatBellowsBlockEntity}). The frame is the block model; client/GreatBellowsRenderer works the boards.
 * Right-click: the blast it gives.
 */
public class GreatBellowsBlock extends Block implements EntityBlock, WaterPower.Driven {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 4, 16), box(1, 4, 1, 15, 12, 15));

    public GreatBellowsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The nozzle points at the block it is set against, else away from the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face.getOpposite() : context.getHorizontalDirection());
    }

    @Override
    public boolean takesAxle(BlockState state, Direction dir) {
        return dir.getAxis().isHorizontal() && dir.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof RotorBlockEntity r)
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(r.power() > 0
                    ? "message.mechanical.great_bellows.blowing" : "message.mechanical.great_bellows.still", r.power(), HighFurnaceBlockEntity.FULL_BLAST));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GreatBellowsBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != HighFurnace.GREAT_BELLOWS_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<GreatBellowsBlockEntity>) (l, p, s, b) -> {
            if (l.getGameTime() % 20 == 7) b.lapse();
        };
    }
}
