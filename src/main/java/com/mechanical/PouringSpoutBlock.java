package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Pouring Spout (docs/steel-furnace-plan.md section 6): a short riveted steel trough with a lever, set against the
 * outside of a High Furnace's Taphole ({@link #FACING} points away from it, the way it pours). Right-click opens and
 * closes it (the lever drops). Open, it pours the furnace's chosen layer straight down into what stands under its lip
 * ({@link PouringSpoutBlockEntity}); with nothing there that takes the melt, nothing flows.
 */
public class PouringSpoutBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Metal is running out of it now (it glows; set by its block entity). */
    public static final BooleanProperty POURING = BooleanProperty.create("pouring");
    /** Set at a trough's end (no taphole recess behind it): the flange stops at the block's face. */
    public static final BooleanProperty SHORT = BooleanProperty.create("short");
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) { // the trough along FACING, from the wall to the lip
            VoxelShape s = switch (d) {
                case NORTH -> box(3, 3, 5.5, 13, 11, 16);
                case SOUTH -> box(3, 3, 0, 13, 11, 10.5);
                case WEST -> box(5.5, 3, 3, 16, 11, 13);
                default -> box(0, 3, 3, 10.5, 11, 13);
            };
            SHAPES[d.get2DDataValue()] = s;
        }
    }

    public PouringSpoutBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(POURING, false).setValue(SHORT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POURING, SHORT);
    }

    /** It points away from the block it is set against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing)
                .setValue(SHORT, context.getLevel().getBlockState(context.getClickedPos().relative(facing.getOpposite())).getBlock() instanceof ClayChannelBlock);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, net.minecraft.util.RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() ? state.setValue(SHORT, neighbourState.getBlock() instanceof ClayChannelBlock) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        boolean open = !state.getValue(OPEN);
        server.setBlock(pos, state.setValue(OPEN, open).setValue(POURING, false), Block.UPDATE_ALL);
        server.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.8F);
        player.sendOverlayMessage(Component.translatable(open ? "message.mechanical.pouring_spout.open" : "message.mechanical.pouring_spout.closed"));
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
        return new PouringSpoutBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HighFurnace.POURING_SPOUT_ENTITY.get()) return null;
        if (level.isClientSide()) return (BlockEntityTicker<T>) (BlockEntityTicker<PouringSpoutBlockEntity>) (l, p, s, e) -> e.clientTick(l);
        return (BlockEntityTicker<T>) (BlockEntityTicker<PouringSpoutBlockEntity>) (l, p, s, e) -> e.serverTick((ServerLevel) l);
    }
}
