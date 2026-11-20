package com.mechanical;

import java.util.Locale;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A fired clay trough. Flat, it joins up with the Clay Channels, Clay Molds and the spout of a Furnace Tap beside it
 * (straight, corner, T or cross; the model is the trough's middle plus an arm or an end wall per side). Placed one block
 * below a channel (or a spout) beside it, with no level channel on that side, it becomes a 45° SLOPE climbing toward it
 * (like a rail on a hill), joining that one with whatever is on its low side; ports and flow: {@link ChannelFlow}.
 * It holds metal ({@link ClayChannelBlockEntity}); an empty Crucible used on it scoops the network's metal up.
 */
public class ClayChannelBlock extends Block implements EntityBlock {
    public enum Slope implements StringRepresentable {
        NONE(null), NORTH(Direction.NORTH), EAST(Direction.EAST), SOUTH(Direction.SOUTH), WEST(Direction.WEST);

        /** The side it climbs toward, null when flat. */
        public final @Nullable Direction up;

        Slope(@Nullable Direction up) {
            this.up = up;
        }

        static Slope of(Direction d) {
            return switch (d) {
                case NORTH -> NORTH;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                default -> WEST;
            };
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, PipeBlock.NORTH, Direction.EAST, PipeBlock.EAST,
            Direction.SOUTH, PipeBlock.SOUTH, Direction.WEST, PipeBlock.WEST);
    public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
    /** Trough floor top and wall top, in pixels. */
    public static final int FLOOR = 2, WALL = 5;
    private static final VoxelShape MIDDLE = box(3, 0, 3, 13, WALL, 13);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(Direction.NORTH, box(3, 0, 0, 13, WALL, 3), Direction.SOUTH, box(3, 0, 13, 13, WALL, 16),
            Direction.WEST, box(0, 0, 3, 3, WALL, 13), Direction.EAST, box(13, 0, 3, 16, WALL, 13));
    /** By the bits of the joined sides (2D data values: south 0, west 1, north 2, east 3). */
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    /** Slopes as four steps, by the 2D data value of the side they climb toward. */
    private static final VoxelShape[] SLOPES = new VoxelShape[4];

    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = MIDDLE;
            for (Direction d : Direction.Plane.HORIZONTAL) if ((mask & 1 << d.get2DDataValue()) != 0) shape = Shapes.or(shape, ARMS.get(d));
            SHAPES[mask] = shape;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < 4; i++) {
                int a = 4 * i, b = 4 * i + 4, top = Math.min(16, b + 1); // t from the low side (0) to the high side (16)
                shape = Shapes.or(shape, switch (d) {
                    case NORTH -> box(3, 0, 16 - b, 13, top, 16 - a);
                    case SOUTH -> box(3, 0, a, 13, top, b);
                    case EAST -> box(a, 0, 3, b, top, 13);
                    default -> box(16 - b, 0, 3, 16 - a, top, 13);
                });
            }
            SLOPES[d.get2DDataValue()] = shape;
        }
    }

    public ClayChannelBlock(Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any().setValue(SLOPE, Slope.NONE);
        for (BooleanProperty p : SIDES.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, SLOPE);
    }

    /** The shape this channel takes here: a slope up to a channel / spout one block up beside it, else flat joined to what shares its ports. */
    public BlockState stateAt(LevelReader level, BlockPos pos) {
        BlockState s = defaultBlockState();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).getBlock() instanceof ClayChannelBlock) continue; // a level channel there: stay flat
            BlockPos high = pos.relative(d).above();
            if (ChannelFlow.ports(high, level.getBlockState(high)).contains(ChannelFlow.port(pos, d, pos.getY() + 1))) return s.setValue(SLOPE, Slope.of(d));
        }
        for (var e : SIDES.entrySet()) s = s.setValue(e.getValue(), ChannelFlow.across(level, pos, ChannelFlow.port(pos, e.getKey(), pos.getY())) != null);
        return s;
    }

    /**
     * Re-shapes the channels diagonally above and below (they may join or slope to this one; their direct neighbours
     * update themselves through {@link #updateShape}). Also called by the tap and molds' neighbours via the level.
     */
    static void refreshAround(Level level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy += 2) {
                BlockPos q = pos.relative(d).above(dy);
                BlockState s = level.getBlockState(q);
                if (s.getBlock() instanceof ClayChannelBlock channel) {
                    BlockState t = channel.stateAt(level, q);
                    if (t != s) level.setBlock(q, t, Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateAt(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && state != oldState) refreshAround(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        refreshAround(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return stateAt(level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Mechanical.CRUCIBLE.get()) || !CrucibleItem.contents(stack).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack full = ChannelFlow.scoop(server, pos);
        if (full.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.channel.nothing_to_scoop"));
            return InteractionResult.SUCCESS;
        }
        if (stack.getCount() == 1) player.setItemInHand(hand, full);
        else {
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(full, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        server.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.7F, 1.2F);
        Smelting.award(server, pos, Smelting.SCOOP_ADVANCEMENT, 6);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction up = state.getValue(SLOPE).up;
        if (up != null) return SLOPES[up.get2DDataValue()];
        int mask = 0;
        for (var e : SIDES.entrySet()) if (state.getValue(e.getValue())) mask |= 1 << e.getKey().get2DDataValue();
        return SHAPES[mask];
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState out = state;
        for (var e : SIDES.entrySet()) out = out.setValue(SIDES.get(rotation.rotate(e.getKey())), state.getValue(e.getValue()));
        Direction up = state.getValue(SLOPE).up;
        return up == null ? out : out.setValue(SLOPE, Slope.of(rotation.rotate(up)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState out = state;
        for (var e : SIDES.entrySet()) out = out.setValue(SIDES.get(mirror.mirror(e.getKey())), state.getValue(e.getValue()));
        Direction up = state.getValue(SLOPE).up;
        return up == null ? out : out.setValue(SLOPE, Slope.of(mirror.mirror(up)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClayChannelBlockEntity(pos, state);
    }
}
