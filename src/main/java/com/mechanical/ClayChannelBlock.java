package com.mechanical;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fired clay trough. It joins up with the Clay Channels, Clay Molds and the spout of a Furnace Tap beside it
 * (straight, corner, T or cross; the model is the trough's middle plus an arm or an end wall per side). Holds no metal:
 * {@link ClayChannelBlockEntity} only shows the metal running through while a tap pours along it.
 */
public class ClayChannelBlock extends Block implements EntityBlock {
    public static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, PipeBlock.NORTH, Direction.EAST, PipeBlock.EAST,
            Direction.SOUTH, PipeBlock.SOUTH, Direction.WEST, PipeBlock.WEST);
    /** Trough floor top and wall top, in pixels. */
    public static final int FLOOR = 2, WALL = 5;
    private static final VoxelShape MIDDLE = box(3, 0, 3, 13, WALL, 13);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(Direction.NORTH, box(3, 0, 0, 13, WALL, 3), Direction.SOUTH, box(3, 0, 13, 13, WALL, 16),
            Direction.WEST, box(0, 0, 3, 3, WALL, 13), Direction.EAST, box(13, 0, 3, 16, WALL, 13));
    /** By the bits of the joined sides (2D data values: south 0, west 1, north 2, east 3). */
    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = MIDDLE;
            for (Direction d : Direction.Plane.HORIZONTAL) if ((mask & 1 << d.get2DDataValue()) != 0) shape = Shapes.or(shape, ARMS.get(d));
            SHAPES[mask] = shape;
        }
    }

    public ClayChannelBlock(Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any();
        for (BooleanProperty p : SIDES.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST);
    }

    /** Joins a channel, a mold, or a tap whose spout points here. */
    static boolean joins(BlockState other, Direction towardOther) {
        if (other.getBlock() instanceof ClayChannelBlock || other.getBlock() instanceof ClayMoldBlock) return true;
        return other.getBlock() instanceof FurnaceTapBlock && other.getValue(FurnaceTapBlock.FACING) == towardOther.getOpposite();
    }

    private BlockState connect(BlockState state, LevelReader level, BlockPos pos) {
        for (var e : SIDES.entrySet()) state = state.setValue(e.getValue(), joins(level.getBlockState(pos.relative(e.getKey())), e.getKey()));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction.getAxis().isHorizontal()) return state.setValue(SIDES.get(direction), joins(neighbourState, direction));
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (var e : SIDES.entrySet()) if (state.getValue(e.getValue())) mask |= 1 << e.getKey().get2DDataValue();
        return SHAPES[mask];
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState out = state;
        for (var e : SIDES.entrySet()) out = out.setValue(SIDES.get(rotation.rotate(e.getKey())), state.getValue(e.getValue()));
        return out;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState out = state;
        for (var e : SIDES.entrySet()) out = out.setValue(SIDES.get(mirror.mirror(e.getKey())), state.getValue(e.getValue()));
        return out;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClayChannelBlockEntity(pos, state);
    }
}
