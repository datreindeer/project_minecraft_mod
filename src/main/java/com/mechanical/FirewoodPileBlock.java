package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A neat pile of split firewood, one block per wood type ({@link Mechanical#FIREWOOD_PILES}). Right-click the top of a
 * solid block with Firewood ({@link FirewoodItem}) to start one; each piece is a {@link #LAYERS layer} of two billets
 * lying side by side along {@link #AXIS} (end grain towards the player who stacked it), four billets to a row, four rows
 * to a block. More of the same firewood stacks on (and carries on in the block above once this one is full), an empty hand
 * takes the top piece back, breaking it drops them all (loot table). A Fire Pit next to a pile feeds itself from it
 * ({@link #takeNear}).
 */
public class FirewoodPileBlock extends Block {
    public static final int MAX = 8;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, MAX);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape[] SHAPES_Z = new VoxelShape[MAX + 1], SHAPES_X = new VoxelShape[MAX + 1];

    static {
        for (int n = 1; n <= MAX; n++) {
            int rows = n / 2, top = rows * 4;
            VoxelShape full = rows > 0 ? box(0, 0, 0, 16, top, 16) : Shapes.empty();
            // an odd piece starts the next row: two billets on one side (the model's west half, north once turned)
            SHAPES_Z[n] = n % 2 == 0 ? full : Shapes.or(full, box(0, top, 0, 8, top + 4, 16));
            SHAPES_X[n] = n % 2 == 0 ? full : Shapes.or(full, box(0, top, 0, 16, top + 4, 8));
        }
    }

    private final String wood;

    public FirewoodPileBlock(String wood, Properties properties) {
        super(properties);
        this.wood = wood;
        registerDefaultState(defaultBlockState().setValue(LAYERS, 1).setValue(AXIS, Direction.Axis.Z));
    }

    public String wood() {
        return wood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(AXIS) == Direction.Axis.X ? SHAPES_X : SHAPES_Z)[state.getValue(LAYERS)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** Lost its footing: tumbles (drops its firewood) next tick. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) ticks.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }

    /** Places a new pile of {@code wood} at {@code pos} (already checked free). */
    static void place(Level level, BlockPos pos, String wood, Direction.Axis axis) {
        level.setBlock(pos, Mechanical.FIREWOOD_PILES.get(wood).get().defaultBlockState().setValue(AXIS, axis), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
    }

    /** Same firewood: one more piece on top, here or (when this block is full) in the block above. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // an empty hand takes a piece (useWithoutItem); other things don't, and go on to their own use
        if (!stack.is(Mechanical.FIREWOOD.get(wood).get())) return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        BlockPos at = pos;
        while (level.getBlockState(at).is(this) && level.getBlockState(at).getValue(LAYERS) == MAX && level.getBlockState(at.above()).is(this)) at = at.above();
        BlockState top = level.getBlockState(at);
        if (top.getValue(LAYERS) == MAX) {
            BlockPos up = at.above();
            if (!level.isInsideBuildHeight(up) || !level.getBlockState(up).canBeReplaced() || !level.getFluidState(up).isEmpty())
                return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                place(level, up, wood, top.getValue(AXIS));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            level.setBlock(at, top.setValue(LAYERS, top.getValue(LAYERS) + 1), Block.UPDATE_ALL);
            level.playSound(null, at, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Empty hand: takes the top piece of the stack (the highest pile block above this one). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        BlockPos at = pos;
        while (level.getBlockState(at.above()).getBlock() instanceof FirewoodPileBlock) at = at.above();
        if (!level.isClientSide()) takeOne(level, at, level.getBlockState(at), player);
        return InteractionResult.SUCCESS;
    }

    /** Takes one piece off the pile at {@code pos}, into the player's inventory (or nowhere when {@code player} is null). */
    private static ItemStack takeOne(Level level, BlockPos pos, BlockState state, Player player) {
        FirewoodPileBlock pile = (FirewoodPileBlock) state.getBlock();
        int n = state.getValue(LAYERS);
        level.setBlock(pos, n > 1 ? state.setValue(LAYERS, n - 1) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
        ItemStack piece = new ItemStack(Mechanical.FIREWOOD.get(pile.wood).get());
        if (player != null) player.getInventory().placeItemBackInInventory(piece, Prediction.SERVER_ONLY);
        return piece;
    }

    /**
     * A Fire Pit's stock ran out: takes one piece from a pile within {@code reach} blocks sideways of {@code pos} (its
     * level or one above). Returns the piece, or empty if no pile is near.
     */
    public static ItemStack takeNear(Level level, BlockPos pos, int reach) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-reach, 0, -reach), pos.offset(reach, 1, reach))) {
            if (!(level.getBlockState(p).getBlock() instanceof FirewoodPileBlock)) continue;
            BlockPos at = p.immutable();
            while (level.getBlockState(at.above()).getBlock() instanceof FirewoodPileBlock) at = at.above();
            return takeOne(level, at, level.getBlockState(at), null);
        }
        return ItemStack.EMPTY;
    }
}
