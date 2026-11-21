package com.mechanical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A neat pile of split firewood, one block per wood type ({@link Mechanical#FIREWOOD_PILES}). Right-click the top of a
 * solid block with Firewood ({@link FirewoodItem}) to start one; each piece is a {@link #LAYERS layer} of two billets
 * lying side by side along {@link #AXIS} (end grain towards the player who stacked it), four billets to a row, four rows
 * to a block. Any firewood stacks on (and carries on in the block above once this one is full), up to {@link #MAX_HEIGHT}
 * blocks; mixed woods are kept per layer in a {@link WoodPileEntity} and drawn each in its own wood. An empty hand takes the
 * top piece back, breaking it drops them all (loot table, or the mixed layers). A Fire Pit next to a pile feeds itself
 * from it ({@link #takeNear}).
 */
public class FirewoodPileBlock extends Block implements EntityBlock {
    public static final int MAX = 8;
    /** Blocks a firewood stack goes up: about as high as one can reach to stack it. */
    public static final int MAX_HEIGHT = 3;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, MAX);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape[] SHAPES_Z = new VoxelShape[MAX + 1], SHAPES_X = new VoxelShape[MAX + 1];

    static {
        for (int n = 1; n <= MAX; n++) {
            int rows = n / 2, top = rows * 4;
            VoxelShape full = rows > 0 ? box(0, 0, 0, 16, top, 16) : Shapes.empty();
            // an odd piece starts the next row: two billets on one side (the model's west half, north once turned);
            // every other row lies crosswise, so its half is the other way round
            // (turned y:90 the model's north half lands east)
            boolean cross = rows % 2 == 1;
            SHAPES_Z[n] = n % 2 == 0 ? full : Shapes.or(full, cross ? box(0, top, 0, 16, top + 4, 8) : box(0, top, 0, 8, top + 4, 16));
            SHAPES_X[n] = n % 2 == 0 ? full : Shapes.or(full, cross ? box(8, top, 0, 16, top + 4, 16) : box(0, top, 0, 16, top + 4, 8));
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodPileEntity(pos, state);
    }

    /** The wood of each layer of the pile block at {@code pos}, bottom first. */
    static int[] layers(Level level, BlockPos pos, BlockState state) {
        int n = state.getValue(LAYERS), own = Mechanical.WOODS.indexOf(((FirewoodPileBlock) state.getBlock()).wood);
        int[] stored = level.getBlockEntity(pos) instanceof WoodPileEntity pile ? pile.woods() : new int[0];
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = i < stored.length ? stored[i] : own;
        return out;
    }

    /** Sets the layers of the pile block at {@code pos} (their count is the state's): stored only while mixed. */
    static void setLayers(Level level, BlockPos pos, BlockState state, int[] woods) {
        int own = Mechanical.WOODS.indexOf(((FirewoodPileBlock) state.getBlock()).wood);
        boolean plain = Arrays.stream(woods).allMatch(w -> w == own);
        if (level.getBlockEntity(pos) instanceof WoodPileEntity pile) pile.setWoods(plain ? new int[0] : woods);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof WoodPileEntity pile) || pile.woods().length == 0)
            return super.getDrops(state, params);
        List<ItemStack> drops = new ArrayList<>();
        int[] woods = pile.woods();
        for (int i = 0; i < Math.min(woods.length, state.getValue(LAYERS)); i++) {
            ItemStack piece = new ItemStack(Mechanical.FIREWOOD.get(Mechanical.WOODS.get(woods[i])).get());
            drops.stream().filter(d -> ItemStack.isSameItem(d, piece)).findFirst().ifPresentOrElse(d -> d.grow(1), () -> drops.add(piece));
        }
        return drops;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(AXIS), state.getValue(LAYERS));
    }

    /** The pile's shape at {@code layers} (also the Charcoal Heap's, which keeps the billets' shape). */
    static VoxelShape shape(Direction.Axis axis, int layers) {
        return (axis == Direction.Axis.X ? SHAPES_X : SHAPES_Z)[layers];
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

    /** Any firewood: one more piece on top, here or (when this block is full) in the block above, up to {@link #MAX_HEIGHT}. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // an empty hand takes a piece (useWithoutItem); other things don't, and go on to their own use
        if (!(stack.getItem() instanceof FirewoodItem firewood)) return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        return add(level, pos, firewood.wood(), player, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /**
     * One piece of {@code wood} on top of the stack {@code pos} is in (from {@code player}'s hand when not null). False if the
     * stack is {@link #MAX_HEIGHT} high or there is no room above.
     */
    static boolean add(Level level, BlockPos pos, String wood, Player player, ItemStack stack) {
        BlockPos at = pos;
        while (level.getBlockState(at.above()).getBlock() instanceof FirewoodPileBlock) at = at.above();
        BlockState top = level.getBlockState(at);
        if (top.getValue(LAYERS) == MAX) {
            BlockPos up = at.above();
            int height = 1;
            while (level.getBlockState(at.below(height)).getBlock() instanceof FirewoodPileBlock) height++;
            if (height >= MAX_HEIGHT) {
                if (player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.firewood_pile.too_high"));
                return false;
            }
            if (!level.isInsideBuildHeight(up) || !level.getBlockState(up).canBeReplaced() || !level.getFluidState(up).isEmpty()) return false;
            if (!level.isClientSide()) {
                place(level, up, wood, top.getValue(AXIS));
                if (player != null) stack.consume(1, player);
            }
            return true;
        }
        if (!level.isClientSide()) {
            int[] woods = layers(level, at, top);
            int[] more = Arrays.copyOf(woods, woods.length + 1);
            more[woods.length] = Mechanical.WOODS.indexOf(wood);
            BlockState grown = top.setValue(LAYERS, more.length);
            level.setBlock(at, grown, Block.UPDATE_ALL);
            setLayers(level, at, grown, more);
            level.playSound(null, at, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            if (player != null) stack.consume(1, player);
        }
        return true;
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
    static ItemStack takeOne(Level level, BlockPos pos, BlockState state, Player player) {
        int[] woods = layers(level, pos, state);
        int n = woods.length;
        if (n > 1) {
            BlockState less = state.setValue(LAYERS, n - 1);
            level.setBlock(pos, less, Block.UPDATE_ALL);
            setLayers(level, pos, less, Arrays.copyOf(woods, n - 1));
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
        ItemStack piece = new ItemStack(Mechanical.FIREWOOD.get(Mechanical.WOODS.get(woods[n - 1])).get());
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
