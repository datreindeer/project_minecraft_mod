package com.mechanical;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wild grass of grain in its natural stand (docs/farming.md, "Vilda sädesbestånd"). It lives by the calendar
 * ({@link #stageOn}): green shoots in spring among last year's dead stalks ({@link #YOUNG}), green ears in early summer
 * ({@link #HEADING}), golden ears for about ten days in late summer ({@link #RIPE}), then the ears shatter and the seed
 * falls ({@link #SHATTERED}: empty, bleached ears, standing as straw through autumn and winter). Each plant ripens a few
 * days apart ({@link #JITTER}), so a stand shatters over a week or two. When a plant comes up again after shattering, the
 * seed it shed may put up a new plant beside it ({@link #reseed}), so a stand outlives its harvests unless it is cut
 * bare before the ears shatter. Drops by stage: loot tables (scripts/gen_wildgrain.py); sickle bonus, wear and
 * advancements: {@link Farming#onWildGrainDrops}.
 */
public class WildGrainBlock extends VegetationBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final int YOUNG = 0, HEADING = 1, RIPE = 2, SHATTERED = 3;
    /** Ripening is spread over this many days across a stand (by position). */
    public static final int JITTER = 4;
    /** Chance a plant coming up again puts up a new one from its shed seed; no new one when this many already stand within 2 blocks. */
    static final float RESEED = 0.35F;
    static final int CROWDED = 11;

    /** Days of the year (0 = 1 Thaw) the ears appear, ripen and shatter. */
    public record Timing(int head, int ripe, int shatter) {}

    private final Timing timing;
    private final VoxelShape shape;

    public WildGrainBlock(Timing timing, int height, Properties properties) {
        super(properties);
        this.timing = timing;
        this.shape = column(12, 0, height);
        registerDefaultState(defaultBlockState().setValue(STAGE, YOUNG));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    public Timing timing() {
        return timing;
    }

    /** This plant's ripening offset in days, 0..JITTER, the same for a spot every year. */
    static int jitter(BlockPos pos) {
        return Math.floorMod((int) Mth.getSeed(pos.getX(), 0, pos.getZ()) >> 7, JITTER + 1);
    }

    /** The stage on this day of the year; before its (jittered) spring a plant still stands as last year's straw. */
    public int stageOn(int dayOfYear, BlockPos pos) {
        int d = dayOfYear - jitter(pos);
        if (d < 0 || d >= timing.shatter()) return SHATTERED;
        if (d >= timing.ripe()) return RIPE;
        return d >= timing.head() ? HEADING : YOUNG;
    }

    public int stageNow(Level level, BlockPos pos) {
        return stageOn(Calendar.dayOfYear(level.getOverworldClockTime()), pos);
    }

    /** The state for this spot today (worldgen and planting). */
    public BlockState stateAt(Level level, BlockPos pos) {
        return defaultBlockState().setValue(STAGE, stageNow(level, pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateAt(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int now = stageNow(level, pos), was = state.getValue(STAGE);
        if (now == was) return;
        level.setBlock(pos, state.setValue(STAGE, now), Block.UPDATE_CLIENTS);
        if (was == SHATTERED && random.nextFloat() < RESEED) reseed(level, pos, random);
    }

    /** The seed shed last summer comes up: a new plant on free ground beside this one, unless the stand is thick here already. */
    void reseed(ServerLevel level, BlockPos pos, RandomSource random) {
        int near = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            if (level.getBlockState(p).is(this) && ++near >= CROWDED) return;
        }
        BlockPos side = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
        for (int dy : new int[] {0, 1, -1}) {
            BlockPos p = side.above(dy);
            BlockState here = level.getBlockState(p);
            BlockState plant = stateAt(level, p);
            if (here.canBeReplaced() && !here.is(this) && fluidOk(level, p) && !(here.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock)
                    && plant.canSurvive(level, p)) {
                level.setBlock(p, placed(level, p, plant), Block.UPDATE_ALL);
                return;
            }
        }
    }

    /** Land kinds only take dry ground; Wild Rice ({@link Rice}) also shallow water. */
    public boolean fluidOk(LevelReader level, BlockPos p) {
        return level.getFluidState(p).isEmpty();
    }

    /** The state to set at p (Wild Rice: waterlogged where it stands in water). */
    public BlockState placed(LevelReader level, BlockPos p, BlockState state) {
        return state;
    }

    /**
     * Wild Rice: stands in the shallow water at marsh and swamp edges (waterlogged, its ears above the surface) or on the wet
     * bank beside it, on mud, clay or soil.
     */
    public static class Rice extends WildGrainBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
        public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED =
                net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

        public Rice(Timing timing, int height, Properties properties) {
            super(timing, height, properties);
            registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || state.is(net.minecraft.world.level.block.Blocks.CLAY);
        }

        @Override
        public boolean fluidOk(LevelReader level, BlockPos p) {
            var f = level.getFluidState(p);
            return f.isEmpty() || f.is(net.minecraft.tags.FluidTags.WATER) && f.isSource() && level.getFluidState(p.above()).isEmpty();
        }

        @Override
        public BlockState placed(LevelReader level, BlockPos p, BlockState state) {
            return state.setValue(WATERLOGGED, level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER));
        }

        @Override
        public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
            return placed(context.getLevel(), context.getClickedPos(), super.getStateForPlacement(context));
        }

        @Override
        protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED) ? net.minecraft.world.level.material.Fluids.WATER.getSource(false) : super.getFluidState(state);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
                Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER,
                    net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
            return super.updateShape(state, level, ticks, pos, dir, neighborPos, neighbor, random);
        }
    }
}
