package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Saw Bench, middle of the Sawmill (see {@link Sawmill}): a trough like a Log Flume under a timber frame with an
 * up-and-down steel frame saw. {@link #FACING} is its in side, where the logs come from; its water runs like a flume's
 * along that line. A shaft from a Water Wheel meets it from the left or right. Right-click: a Steel Saw Blade fits a new
 * blade, a log is laid in the first flume, an empty hand says what the mill needs ({@link SawBenchBlockEntity#problem}).
 */
public class SawBenchBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape Z = Shapes.or(box(1, 0, 0, 15, 3, 16), box(1, 3, 0, 3, 12, 16), box(13, 3, 0, 15, 12, 16),
            box(0, 0, 6, 3, 16, 10), box(13, 0, 6, 16, 16, 10));
    private static final VoxelShape X = Shapes.or(box(0, 0, 1, 16, 3, 15), box(0, 3, 1, 16, 12, 3), box(0, 3, 13, 16, 12, 15),
            box(6, 0, 0, 10, 16, 3), box(6, 0, 13, 10, 16, 16));

    public SawBenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LogFlumeBlock.WATER, LogFlumeBlock.Water.DRY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LogFlumeBlock.WATER);
    }

    /** The in side faces the player: logs are laid from where they stand. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction front = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, front).setValue(LogFlumeBlock.WATER, LogFlumeBlock.water(context.getLevel(), context.getClickedPos(), front.getAxis()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        return dir.getAxis() == axis ? state.setValue(LogFlumeBlock.WATER, LogFlumeBlock.water(level, pos, axis)) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? X : Z;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        if (entity instanceof ItemEntity item) LogFlumeBlock.carry(state.getValue(FACING).getAxis(), state.getValue(LogFlumeBlock.WATER), pos, item);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SawBenchBlockEntity saw)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.is(Smithing.STEEL_SAW_BLADE.get())) {
            if (level instanceof ServerLevel server) saw.fitBlade(server, player, stack);
            return InteractionResult.SUCCESS;
        }
        if (Sawmill.woodOf(stack) != null) {
            if (level instanceof ServerLevel server) saw.lay(server, player, stack, -1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof SawBenchBlockEntity saw) saw.tellStatus(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SawBenchBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != Sawmill.SAW_BENCH_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<SawBenchBlockEntity>) (l, p, s, saw) -> saw.tick(l);
    }
}
