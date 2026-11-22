package com.mechanical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A woodpile of Rough Logs ({@link RoughLogItem}): four logs to a block, two side by side along {@link #AXIS} and two rows
 * up, any woods mixed (kept per log in a {@link WoodPileEntity}, drawn each in its own wood). A column carries on in the
 * block above once this one is full.
 * <p>
 * Round logs don't stack like split firewood: a new row on top of other logs may stand at most one block above both
 * neighbouring columns across the logs ({@link #allowed}); higher than that it rolls off to the lower side, on down the
 * pile until it comes to rest on a lower column or the ground (or drops as an item when there's nowhere to lie). So a
 * pile gets higher the wider it is built, like a real one. A solid block beside the new log holds it in like a wall, so
 * between two walls a pile goes straight up to {@link #MAX_ROWS}. The first row lies on the ground and never rolls.
 * <p>
 * An empty hand takes the top log of a column back; breaking a block drops its logs; a block that loses its footing
 * tumbles. Logs taken away (or a block gone) leave the columns beside to {@link #settle}: one standing too high now sheds
 * its top log the same way, log by log, until the pile is at rest again.
 */
public class RoughLogPileBlock extends Block implements EntityBlock {
    public static final int PER_BLOCK = 4;
    /** Rows of logs a column stands at most (4 blocks), even between walls. */
    public static final int MAX_ROWS = 8;
    /** Blocks a log rolls (and falls) before it gives up and drops as an item. */
    static final int MAX_ROLL = 12;
    /** {@link #surface} of a solid block right beside the new log: it holds the log in. */
    static final int WALL = Integer.MAX_VALUE / 2;
    public static final IntegerProperty LOGS = IntegerProperty.create("logs", 1, PER_BLOCK);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** The top row's lone log lies on the east half (south once turned), not the usual west: it rolled in from that side. */
    public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");
    static final net.minecraft.resources.Identifier ADVANCEMENT = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/log_pile");
    private static final VoxelShape[] SHAPES_Z = new VoxelShape[PER_BLOCK + 1], SHAPES_X = new VoxelShape[PER_BLOCK + 1];
    /** {@link #FLIPPED}, an odd count: the last log in the other half. */
    private static final VoxelShape[] FLIPPED_Z = new VoxelShape[PER_BLOCK + 1], FLIPPED_X = new VoxelShape[PER_BLOCK + 1];

    static {
        // log i: column i % 2 across (the model's west half first, north once turned y:90), row i / 2 up
        SHAPES_Z[0] = SHAPES_X[0] = Shapes.empty();
        for (int i = 0; i < PER_BLOCK; i++) {
            int c = i % 2 * 8, r = i / 2 * 8;
            SHAPES_Z[i + 1] = Shapes.or(SHAPES_Z[i], box(c, r, 0, c + 8, r + 8, 16));
            SHAPES_X[i + 1] = Shapes.or(SHAPES_X[i], box(0, r, c, 16, r + 8, c + 8));
            FLIPPED_Z[i + 1] = c == 0 ? Shapes.or(SHAPES_Z[i], box(8, r, 0, 16, r + 8, 16)) : SHAPES_Z[i + 1];
            FLIPPED_X[i + 1] = c == 0 ? Shapes.or(SHAPES_X[i], box(0, r, 8, 16, r + 8, 16)) : SHAPES_X[i + 1];
        }
    }

    public RoughLogPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LOGS, 1).setValue(AXIS, Direction.Axis.Z).setValue(FLIPPED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOGS, AXIS, FLIPPED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodPileEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean x = state.getValue(AXIS) == Direction.Axis.X;
        return (state.getValue(FLIPPED) ? x ? FLIPPED_X : FLIPPED_Z : x ? SHAPES_X : SHAPES_Z)[state.getValue(LOGS)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** Lost its footing: tumbles (drops its logs) next tick. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) ticks.scheduleTick(pos, this, 1);
        return state;
    }

    /** Lost its footing: tumbles. Otherwise a nudge from a neighbour ({@link #nudgeSides}): settles one log if it stands too high. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
        else settle(level, pos);
    }

    /** Gone (broken, burnt, tumbled, last log taken): the columns beside may now stand too high. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        nudgeSides(level, pos, state.getValue(AXIS));
    }

    // ---------------------------------------------------------------- settling

    /** Ticks between one log settling and the next look: the pile slumps log by log, not all at once. */
    static final int SETTLE_TICKS = 4;

    /** The columns on both sides across the logs of {@code pos} take a look soon ({@link #settle}). */
    static void nudgeSides(Level level, BlockPos pos, Direction.Axis axis) {
        Direction left = axis == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        for (Direction d : new Direction[] {left, left.getOpposite()}) {
            BlockPos side = pos.relative(d);
            for (int dy : new int[] {0, 1, 2, 3, 4, -1, -2}) {
                BlockPos p = side.above(dy);
                if (level.getBlockState(p).getBlock() instanceof RoughLogPileBlock pile) {
                    level.scheduleTick(p, pile, SETTLE_TICKS);
                    break;
                }
            }
        }
    }

    /**
     * When the column at {@code pos} stands higher than {@link #allowed} (a neighbour lost logs), its top log rolls off to
     * the lower side like a newly put one, and this column and its sides look again soon. True if a log moved.
     */
    static boolean settle(ServerLevel level, BlockPos pos) {
        BlockPos bottom = bottom(level, pos), top = top(level, pos);
        int rows = (columnLogs(level, bottom) + 1) / 2;
        if (rows <= 1) return false; // the ground row never rolls
        Direction.Axis axis = level.getBlockState(top).getValue(AXIS);
        Direction left = axis == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        int height = bottom.getY() * 2 + rows;
        int a = surface(level, top.relative(left)), b = surface(level, top.relative(left.getOpposite()));
        if (allowed(rows, height, a, b)) return false;
        Direction way = lower(level.getRandom(), left, a, height, left.getOpposite(), b, height);
        if (way == null) return false;
        int wood = removeTop(level, top);
        BlockPos side = top.relative(way), landing = landing(level, side);
        Outcome out = landing == null ? null : add(level, landing, wood, axis, way, 1);
        if (out == null) drop(level, side, wood);
        level.playSound(null, side, SoundEvents.WOOD_FALL, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.scheduleTick(bottom, level.getBlockState(bottom).getBlock(), SETTLE_TICKS);
        nudgeSides(level, bottom, axis);
        return true;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        int[] woods = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof WoodPileEntity pile ? pile.woods() : new int[0];
        for (int i = 0; i < state.getValue(LOGS); i++) {
            ItemStack log = log(i < woods.length ? woods[i] : 0);
            drops.stream().filter(d -> ItemStack.isSameItem(d, log)).findFirst().ifPresentOrElse(d -> d.grow(1), () -> drops.add(log));
        }
        return drops;
    }

    private static ItemStack log(int wood) {
        return new ItemStack(Mechanical.ROUGH_LOGS.get(Mechanical.WOODS.get(wood)).get());
    }

    // ---------------------------------------------------------------- stacking

    /** A Rough Log on the clicked column: it stacks there or rolls off. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // an empty hand takes a log (useWithoutItem); other things don't, and go on to their own use
        if (!(stack.getItem() instanceof RoughLogItem log)) return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        return stack(level, pos, log.wood(), state.getValue(AXIS), player, stack);
    }

    /** Empty hand: takes the top log of the column. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) player.getInventory().placeItemBackInInventory(takeTop(level, pos), Prediction.SERVER_ONLY);
        return InteractionResult.SUCCESS;
    }

    /**
     * A log of {@code wood} from {@code player}'s {@code stack} put on the column at {@code pos} (a pile block, or the free
     * spot on the ground where a new pile starts, its logs lying along {@code axis}), with a word when it rolls off.
     */
    static InteractionResult stack(Level level, BlockPos pos, String wood, Direction.Axis axis, Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Outcome out = add(server, pos, Mechanical.WOODS.indexOf(wood), axis, null, 0);
        if (out == null) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.rough_log_pile.too_high"));
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        if (out.rolled()) {
            server.playSound(null, out.pos(), SoundEvents.WOOD_FALL, SoundSource.BLOCKS, 1.0F, 0.7F);
            player.sendOverlayMessage(Component.translatable(out.dropped() ? "message.mechanical.rough_log_pile.fell_off" : "message.mechanical.rough_log_pile.rolled"));
        }
        if (player instanceof ServerPlayer sp) Kettle.award(sp, ADVANCEMENT);
        return InteractionResult.SUCCESS;
    }

    /** Where a log came to rest: on a pile at {@code pos}, or dropped there as an item; rolled if not where it was put. */
    record Outcome(BlockPos pos, boolean rolled, boolean dropped) {}

    /**
     * Puts a log on the column at {@code pos} (a pile block, or a free spot on the ground), or rolls it off when the new
     * row would stand too high ({@link #allowed}). {@code rolling} is the way it is already rolling (null when just put
     * down): once rolling it only goes on that way. Null when it was just put down and can't go anywhere (too high
     * between walls, or no room above): nothing happens then.
     */
    static @Nullable Outcome add(ServerLevel level, BlockPos pos, int wood, Direction.Axis axis, @Nullable Direction rolling, int depth) {
        boolean pile = level.getBlockState(pos).getBlock() instanceof RoughLogPileBlock;
        BlockPos bottom = pile ? bottom(level, pos) : pos, top = pile ? top(level, pos) : pos;
        int logs = pile ? columnLogs(level, bottom) : 0;
        BlockState topState = level.getBlockState(top);
        Direction.Axis lying = pile ? topState.getValue(AXIS) : axis;
        boolean newBlock = !pile || topState.getValue(LOGS) == PER_BLOCK;
        BlockPos at = pile && newBlock ? top.above() : top;
        if (pile && newBlock && (!level.isInsideBuildHeight(at) || !level.getBlockState(at).canBeReplaced() || !level.getFluidState(at).isEmpty()))
            return rolling == null ? null : drop(level, at, wood);

        int rows = (logs + 2) / 2; // the column's rows with this log
        Direction left = lying == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        // rolled in heading west (north): it came from the east and stops on the east half, by the pile it came off
        boolean fromEast = rolling == left;
        if (logs % 2 == 0 && rows > 1) { // a new row, on other logs
            int height = bottom.getY() * 2 + rows;
            int a = surface(level, at.relative(left)), b = surface(level, at.relative(left.getOpposite()));
            if (!allowed(rows, height, a, b)) {
                Direction way = rolling != null ? (rollsTo(height, surface(level, at.relative(rolling))) ? rolling : null)
                        : lower(level.getRandom(), left, a, height, left.getOpposite(), b, height);
                if (way == null) return rolling == null ? null : drop(level, at, wood);
                if (depth >= MAX_ROLL) return drop(level, at, wood);
                BlockPos side = at.relative(way), landing = landing(level, side);
                if (landing == null) return drop(level, side, wood);
                Outcome out = add(level, landing, wood, lying, way, depth + 1);
                return out != null ? out : drop(level, side, wood);
            }
        }

        if (newBlock) {
            BlockState fresh = Mechanical.ROUGH_LOG_PILE.get().defaultBlockState().setValue(AXIS, lying).setValue(FLIPPED, fromEast);
            level.setBlock(at, fresh, Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof WoodPileEntity e) e.setWoods(new int[] {wood});
        } else {
            int[] woods = woods(level, at, topState);
            int[] more = Arrays.copyOf(woods, woods.length + 1);
            more[woods.length] = wood;
            boolean flipped = woods.length % 2 == 0 && fromEast;
            if (topState.getValue(FLIPPED)) { // the row's first log lies east: this one goes west, before it (woods are west first)
                more[woods.length] = woods[woods.length - 1];
                more[woods.length - 1] = wood;
            }
            level.setBlock(at, topState.setValue(LOGS, more.length).setValue(FLIPPED, flipped), Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof WoodPileEntity e) e.setWoods(more);
        }
        level.playSound(null, at, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F + level.getRandom().nextFloat() * 0.15F);
        return new Outcome(at, depth > 0, false);
    }

    /**
     * May a new row stand {@code height} (in half blocks from y = 0; {@code rows} up its own column) with the columns across
     * the logs topping out at {@code a} and {@code b} ({@link #surface})? At most one block above the lower one, and never
     * above {@link #MAX_ROWS}.
     */
    static boolean allowed(int rows, int height, int a, int b) {
        return rows <= MAX_ROWS && height <= Math.min(a, b) + 2;
    }

    /** Can a log at {@code height} roll off to a side that tops out at {@code surface}: no wall, and lower. */
    static boolean rollsTo(int height, int surface) {
        return surface != WALL && surface < height - 1;
    }

    /** The side a log rolls to: the lower of the two it can roll to (either when they're level), or null. */
    static @Nullable Direction lower(RandomSource random, Direction a, int sa, int ha, Direction b, int sb, int hb) {
        boolean ca = rollsTo(ha, sa), cb = rollsTo(hb, sb);
        if (ca && cb) return sa < sb ? a : sb < sa ? b : random.nextBoolean() ? a : b;
        return ca ? a : cb ? b : null;
    }

    /**
     * What a log rolling off into {@code side} lands on: the top of a pile below, or the free spot on solid ground; null
     * when it would end up in water, on something it can't lie on, or too far down.
     */
    static @Nullable BlockPos landing(Level level, BlockPos side) {
        for (int dy = 0; dy <= MAX_ROLL; dy++) {
            BlockPos p = side.below(dy);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof RoughLogPileBlock) return p;
            if (!level.getFluidState(p).isEmpty()) return null;
            if (s.canBeReplaced()) continue;
            return dy > 0 && !s.hasBlockEntity() && s.isFaceSturdy(level, p, Direction.UP) ? p.above() : null;
        }
        return null;
    }

    /**
     * How high the column at {@code side} tops out, in half blocks from y = 0, looking down from {@code side}'s own level:
     * a pile's top row, or the top of the ground. {@link #WALL} when {@code side} itself is solid (and not a pile).
     */
    static int surface(Level level, BlockPos side) {
        for (int dy = 0; dy <= MAX_ROLL; dy++) {
            BlockPos p = side.below(dy);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof RoughLogPileBlock) return p.getY() * 2 + (s.getValue(LOGS) + 1) / 2;
            if (!level.getFluidState(p).isEmpty()) return p.getY() * 2; // water: in it goes
            if (!s.canBeReplaced()) return dy == 0 ? WALL : (p.getY() + 1) * 2;
        }
        return (side.getY() - MAX_ROLL) * 2;
    }

    private static Outcome drop(ServerLevel level, BlockPos pos, int wood) {
        Block.popResource(level, pos, log(wood));
        return new Outcome(pos, true, true);
    }

    // ---------------------------------------------------------------- the column

    static BlockPos bottom(Level level, BlockPos pos) {
        while (level.getBlockState(pos.below()).getBlock() instanceof RoughLogPileBlock) pos = pos.below();
        return pos;
    }

    static BlockPos top(Level level, BlockPos pos) {
        while (level.getBlockState(pos.above()).getBlock() instanceof RoughLogPileBlock) pos = pos.above();
        return pos;
    }

    /** Logs in the column standing on {@code bottom}. */
    static int columnLogs(Level level, BlockPos bottom) {
        int n = 0;
        for (BlockPos p = bottom; level.getBlockState(p).getBlock() instanceof RoughLogPileBlock; p = p.above()) n += level.getBlockState(p).getValue(LOGS);
        return n;
    }

    /** The wood of each log of the pile block at {@code pos}, in order (oak for any not stored). */
    static int[] woods(Level level, BlockPos pos, BlockState state) {
        int[] stored = level.getBlockEntity(pos) instanceof WoodPileEntity e ? e.woods() : new int[0];
        return Arrays.copyOf(stored, state.getValue(LOGS));
    }

    /** Takes the top log off the column at {@code pos}; the columns beside settle if that leaves them too high. */
    static ItemStack takeTop(Level level, BlockPos pos) {
        BlockPos top = top(level, pos);
        Direction.Axis axis = level.getBlockState(top).getValue(AXIS);
        int wood = removeTop(level, top);
        level.playSound(null, top, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
        nudgeSides(level, top, axis);
        return log(wood);
    }

    /** Removes the last log of the pile block {@code top} (the top of its column); returns its wood. */
    private static int removeTop(Level level, BlockPos top) {
        BlockState state = level.getBlockState(top);
        int[] woods = woods(level, top, state);
        int n = woods.length;
        if (n > 1) {
            level.setBlock(top, state.setValue(LOGS, n - 1).setValue(FLIPPED, false), Block.UPDATE_ALL);
            if (level.getBlockEntity(top) instanceof WoodPileEntity e) e.setWoods(Arrays.copyOf(woods, n - 1));
        } else {
            level.setBlock(top, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return woods[n - 1];
    }
}
