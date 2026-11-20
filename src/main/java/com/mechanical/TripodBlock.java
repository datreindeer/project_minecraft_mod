package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Tripod of poles lashed with Cordage, standing in the block over a Campfire or a Fire Pit, its legs reaching down to the
 * ground round the fire. PIT 0: over a campfire, centred on it; 1-4: over a Fire Pit part (part + 1), drawn centred on the
 * whole 2x2 pit (the model's centre is the block corner the pit's middle is at, turned by the blockstate). Only one tripod
 * per pit. KETTLE: a Bronze Kettle hangs in it (Kettle, TripodBlockEntity); HOT: the water in it is boiling.
 */
public class TripodBlock extends Block implements EntityBlock {
    public static final IntegerProperty PIT = IntegerProperty.create("pit", 0, 4);
    public static final BooleanProperty KETTLE = BooleanProperty.create("kettle");
    public static final BooleanProperty HOT = BooleanProperty.create("hot");
    // around the kettle as drawn: over a campfire it hangs down into the fire's block, over a pit it hangs at the 2x2's centre
    private static final VoxelShape CAMPFIRE_SHAPE = box(1.5, -6, 1.5, 14.5, 16, 14.5);
    /** Over a pit: the quarter of the block next to the pit's middle (part 0's middle is at its south-east corner). */
    private static final VoxelShape[] PIT_SHAPES = {box(9.5, -2, 9.5, 22.5, 16, 22.5), box(-6.5, -2, 9.5, 6.5, 16, 22.5), box(9.5, -2, -6.5, 22.5, 16, 6.5), box(-6.5, -2, -6.5, 6.5, 16, 6.5)};

    public TripodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PIT, 0).setValue(KETTLE, false).setValue(HOT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PIT, KETTLE, HOT);
    }

    /** The PIT value for a tripod standing on this block, or -1 if it is no fire to stand over. */
    static int pitFor(BlockState below) {
        if (below.is(BlockTags.CAMPFIRES)) return 0;
        return FirePit.isPit(below) ? below.getValue(FirePitBlock.PART) + 1 : -1;
    }

    /** The middle of the fire under the kettle, in pixels from this block's corner. */
    public static float centreX(BlockState state) {
        int pit = state.getValue(PIT);
        return pit == 0 ? 8 : ((pit - 1) & 1) == 0 ? 16 : 0;
    }

    public static float centreZ(BlockState state) {
        int pit = state.getValue(PIT);
        return pit == 0 ? 8 : ((pit - 1) >> 1) == 0 ? 16 : 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        int pit = pitFor(level.getBlockState(pos.below()));
        String problem = pit < 0 ? "message.mechanical.tripod.no_fire" : pit > 0 && otherTripodOnPit(level, pos, pit) ? "message.mechanical.tripod.one_per_pit" : null;
        if (problem != null) {
            if (context.getPlayer() != null && level.isClientSide()) context.getPlayer().sendOverlayMessage(Component.translatable(problem));
            return null;
        }
        return defaultBlockState().setValue(PIT, pit);
    }

    /** Is another tripod already standing over one of the other three parts of this pit? */
    private static boolean otherTripodOnPit(LevelReader level, BlockPos pos, int pit) {
        int part = pit - 1;
        BlockPos corner = pos.offset(-(part & 1), 0, -(part >> 1));
        for (int p = 0; p < 4; p++) {
            BlockPos at = corner.offset(p & 1, 0, p >> 1);
            if (!at.equals(pos) && level.getBlockState(at).getBlock() instanceof TripodBlock) return true;
        }
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return pitFor(level.getBlockState(pos.below())) == state.getValue(PIT);
    }

    /** The fire under it gone: falls over on the next tick (dropping the tripod and the kettle). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) ticks.scheduleTick(pos, this, 1);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int pit = state.getValue(PIT);
        return pit == 0 ? CAMPFIRE_SHAPE : PIT_SHAPES[pit - 1];
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TripodBlockEntity tripod)) return InteractionResult.PASS;
        if (!state.getValue(KETTLE) && !stack.is(Bronze.BRONZE_KETTLE.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!tripod.accepts(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) tripod.use(server, player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    /** Empty hand: take out what is done, else the kettle's status; sneaking empties it (and then takes the kettle down). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TripodBlockEntity tripod) || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) tripod.useEmptyHand(server, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TripodBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != Kettle.TRIPOD_ENTITY.get()) return null;
        return level.isClientSide()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<TripodBlockEntity>) TripodBlockEntity::clientTick
                : (BlockEntityTicker<T>) (BlockEntityTicker<TripodBlockEntity>) (l, p, s, t) -> t.serverTick((ServerLevel) l, p, s);
    }
}
