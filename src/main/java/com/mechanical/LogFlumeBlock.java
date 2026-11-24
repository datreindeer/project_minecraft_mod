package com.mechanical;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Log Flume (see {@link Sawmill}): an open plank trough lying along X or Z. It holds no real water; its {@link #WATER}
 * runs from whichever end has water next to it (a water block, or a flume or Saw Bench in line running into it) towards
 * the other, and is still when both ends have water. Running water carries dropped items along; a log laid in it upstream
 * of a Saw Bench floats to the saw. The water face is tinted like water (client/WaterPowerRenderers registers the tint).
 */
public class LogFlumeBlock extends Block {
    public enum Water implements StringRepresentable {
        DRY, STILL, POSITIVE, NEGATIVE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Running towards {@code d}. */
        public boolean runs(Direction d) {
            return this == (d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? POSITIVE : NEGATIVE);
        }
    }

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<Water> WATER = EnumProperty.create("water", Water.class);
    private static final VoxelShape Z = Shapes.or(box(1, 0, 0, 15, 3, 16), box(1, 3, 0, 3, 12, 16), box(13, 3, 0, 15, 12, 16));
    private static final VoxelShape X = Shapes.or(box(0, 0, 1, 16, 3, 15), box(0, 3, 1, 16, 12, 3), box(0, 3, 13, 16, 12, 15));
    /** How fast running water carries an item, blocks a tick. */
    static final double CURRENT = 0.07;

    public LogFlumeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(WATER, Water.DRY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WATER);
    }

    /** The line a flume or Saw Bench lies along, or null for anything else. */
    static Direction.Axis lineAxis(BlockState state) {
        if (state.getBlock() instanceof LogFlumeBlock) return state.getValue(AXIS);
        if (state.getBlock() instanceof SawBenchBlock) return state.getValue(SawBenchBlock.FACING).getAxis();
        return null;
    }

    /** The water in a flume or Saw Bench at {@code pos} lying along {@code axis}, from its two ends. */
    static Water water(LevelReader level, BlockPos pos, Direction.Axis axis) {
        boolean fromNeg = feeds(level, pos, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
        boolean fromPos = feeds(level, pos, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        return fromNeg && fromPos ? Water.STILL : fromNeg ? Water.POSITIVE : fromPos ? Water.NEGATIVE : Water.DRY;
    }

    /** Whether water comes in at the {@code side} end: a water block there, or a trough in line running this way. */
    private static boolean feeds(LevelReader level, BlockPos pos, Direction side) {
        BlockPos n = pos.relative(side);
        BlockState s = level.getBlockState(n);
        Direction.Axis a = lineAxis(s);
        if (a != null) return a == side.getAxis() && s.getValue(WATER).runs(side.getOpposite());
        return level.getFluidState(n).is(FluidTags.WATER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getHorizontalDirection().getAxis();
        // placed against the end of a flume or saw: continue its line
        Direction.Axis against = lineAxis(context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite())));
        if (against != null && context.getClickedFace().getAxis() == against) axis = against;
        return defaultBlockState().setValue(AXIS, axis).setValue(WATER, water(context.getLevel(), context.getClickedPos(), axis));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return dir.getAxis() == state.getValue(AXIS) ? state.setValue(WATER, water(level, pos, state.getValue(AXIS))) : state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
    }

    /** Running water carries items along the trough and keeps them in its middle. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        if (entity instanceof ItemEntity item) carry(state.getValue(AXIS), state.getValue(WATER), pos, item);
    }

    static void carry(Direction.Axis axis, Water water, BlockPos pos, ItemEntity item) {
        if (water != Water.POSITIVE && water != Water.NEGATIVE) return;
        double sign = water == Water.POSITIVE ? 1 : -1;
        Vec3 v = item.getDeltaMovement();
        double along = sign * CURRENT, toMid = 0.1 * ((axis == Direction.Axis.X ? pos.getZ() - item.getZ() : pos.getX() - item.getX()) + 0.5);
        item.setDeltaMovement(axis == Direction.Axis.X ? new Vec3(v.x + (along - v.x) * 0.3, v.y, toMid) : new Vec3(toMid, v.y, v.z + (along - v.z) * 0.3));
    }

    /** A log laid in the flume upstream of a Saw Bench (up to 3 blocks on) goes in at this spot. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (Sawmill.woodOf(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Direction.Axis axis = state.getValue(AXIS);
        for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
            Direction d = Direction.fromAxisAndDirection(axis, sign);
            for (int i = 1; i <= Sawmill.FLUMES; i++) {
                BlockState s = level.getBlockState(pos.relative(d, i));
                if (s.getBlock() instanceof SawBenchBlock && level.getBlockEntity(pos.relative(d, i)) instanceof SawBenchBlockEntity saw) {
                    if (s.getValue(SawBenchBlock.FACING) != d.getOpposite()) { // this flume is on the saw's out side
                        if (level instanceof ServerLevel) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.saw.out_side"));
                        return InteractionResult.SUCCESS;
                    }
                    if (level instanceof ServerLevel server) saw.lay(server, player, stack, -i);
                    return InteractionResult.SUCCESS;
                }
                if (!(s.getBlock() instanceof LogFlumeBlock) || s.getValue(AXIS) != axis) break;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
