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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bark shingles of one wood ({@link Mechanical#BARK_ROOFS}). Placed against the side of a block it lies as a 45-degree
 * slope rising toward that block ({@link #FACING}); where slopes meet they join into inner and outer corners like stairs
 * ({@link #SHAPE}, same meaning as vanilla stairs). Placed on top of a block it lies {@link #FLAT}, a thin layer. Against
 * the side of another bark roof slope it carries on that slope's direction, so a row is laid by clicking along it.
 * Models and textures: scripts/gen_bark_roof.py, scripts/gen_bark_roof_textures.py.
 */
public class BarkRoofBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
    public static final BooleanProperty FLAT = BooleanProperty.create("flat");

    /** Facing north (high edge north): a wedge in four steps under the slope. */
    private static final VoxelShape NORTH = Shapes.or(box(0, 0, 0, 16, 4, 16), box(0, 4, 0, 16, 8, 12), box(0, 8, 0, 16, 12, 8), box(0, 12, 0, 16, 16, 4));
    private static final VoxelShape WEST = Shapes.or(box(0, 0, 0, 16, 4, 16), box(0, 4, 0, 12, 8, 16), box(0, 8, 0, 8, 12, 16), box(0, 12, 0, 4, 16, 16));
    private static final VoxelShape EAST = Shapes.or(box(0, 0, 0, 16, 4, 16), box(4, 4, 0, 16, 8, 16), box(8, 8, 0, 16, 12, 16), box(12, 12, 0, 16, 16, 16));
    // as vanilla stairs: the outer corner peaks north-west, the inner corner is high north and east
    private static final Map<Direction, VoxelShape> STRAIGHT = Shapes.rotateHorizontal(NORTH);
    private static final Map<Direction, VoxelShape> OUTER = Shapes.rotateHorizontal(Shapes.join(NORTH, WEST, BooleanOp.AND));
    private static final Map<Direction, VoxelShape> INNER = Shapes.rotateHorizontal(Shapes.or(NORTH, EAST));
    private static final VoxelShape FLAT_SHAPE = box(0, 0, 0, 16, 2, 16);

    public BarkRoofBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, StairsShape.STRAIGHT).setValue(FLAT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE, FLAT);
    }

    static boolean isSlope(BlockState s) {
        return s.getBlock() instanceof BarkRoofBlock && !s.getValue(FLAT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) return defaultBlockState().setValue(FLAT, true);
        BlockPos pos = context.getClickedPos();
        BlockState against = context.getLevel().getBlockState(pos.relative(face.getOpposite()));
        // the side of a slope that runs this way: carry on the row; anything else: rise toward the clicked wall
        Direction facing = isSlope(against) && against.getValue(FACING).getAxis() != face.getAxis() ? against.getValue(FACING) : face.getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.setValue(SHAPE, shape(state, context.getLevel(), pos));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction toNeighbour,
            BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return toNeighbour.getAxis().isHorizontal() && !state.getValue(FLAT) ? state.setValue(SHAPE, shape(state, level, pos)) : state;
    }

    /** Vanilla's stair corner rule, between bark roof slopes. */
    public static StairsShape shape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState behind = level.getBlockState(pos.relative(facing));
        if (isSlope(behind)) {
            Direction d = behind.getValue(FACING);
            if (d.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, d.getOpposite()))
                return d == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
        }
        BlockState front = level.getBlockState(pos.relative(facing.getOpposite()));
        if (isSlope(front)) {
            Direction d = front.getValue(FACING);
            if (d.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, d))
                return d == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        BlockState s = level.getBlockState(pos.relative(side));
        return !isSlope(s) || s.getValue(FACING) != state.getValue(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(FLAT)) return FLAT_SHAPE;
        Direction f = state.getValue(FACING);
        return switch (state.getValue(SHAPE)) {
            case STRAIGHT -> STRAIGHT.get(f);
            case OUTER_LEFT -> OUTER.get(f);
            case OUTER_RIGHT -> OUTER.get(f.getClockWise());
            case INNER_RIGHT -> INNER.get(f);
            case INNER_LEFT -> INNER.get(f.getCounterClockWise());
        };
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /** Mirrored like vanilla stairs: a left corner becomes a right one. */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState s = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        if (mirror == Mirror.NONE) return s;
        return s.setValue(SHAPE, switch (state.getValue(SHAPE)) {
            case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
            case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
            case INNER_LEFT -> StairsShape.INNER_RIGHT;
            case INNER_RIGHT -> StairsShape.INNER_LEFT;
            case STRAIGHT -> StairsShape.STRAIGHT;
        });
    }
}
