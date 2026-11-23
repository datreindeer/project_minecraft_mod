package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Dirt Slab (living-world plan F11): grass next to it creeps onto it like onto dirt, making a {@link Grass} slab. A shovel
 * flattens either into a Dirt Path Slab (data/minecraft/block_transformer/shovel.json). All three drop a Dirt Slab.
 * Dirt Stairs and Grass Stairs (terrain plan phase 2, {@link Stairs}, {@link GrassStairs}) grow and die the same way.
 */
public class DirtSlabBlock extends SlabBlock {
    /** Grass blocks and slabs try this many random spots round them per random tick, as vanilla grass does. */
    static final int SPREAD_TRIES = 4;
    public static final BooleanProperty SNOWY = SnowyBlock.SNOWY;

    public DirtSlabBlock(Properties properties) {
        super(properties);
    }

    /** Light and nothing smothering it (TrampledPathBlock.canStayAlive, vanilla grass's rule) and not under water. */
    static boolean grassCanLive(BlockState state, LevelReader level, BlockPos pos) {
        return !state.getValue(BlockStateProperties.WATERLOGGED) && TrampledPathBlock.canStayAlive(state, level, pos);
    }

    /** A Grass Block, Grass Slab or Grass Stairs: what spreads grass. */
    static boolean isGrass(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(EarthBlocks.GRASS_SLAB) || s.is(EarthBlocks.GRASS_STAIRS);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        dirtTick(state, level, pos, random, EarthBlocks.GRASS_SLAB.get());
    }

    /** Grass round the dirt (the spots vanilla grass spreads to, seen from the dirt) takes over: it becomes {@code grass}, shape kept. */
    static void dirtTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Block grass) {
        if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(pos.above()) < 9 || !grassCanLive(state, level, pos)) return;
        for (int i = 0; i < SPREAD_TRIES; i++) {
            if (isGrass(level.getBlockState(pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 1, random.nextInt(3) - 1)))) {
                level.setBlockAndUpdate(pos, grass.withPropertiesOf(state).setValue(SNOWY, level.getBlockState(pos.above()).is(BlockTags.SNOW)));
                return;
            }
        }
    }

    /** Snow on top makes it snowy at once (like a Grass Block). */
    static BlockState snowUpdate(BlockState state, Direction dir, BlockState neighbourState) {
        return dir == Direction.UP && neighbourState.is(BlockTags.SNOW) ? state.setValue(SNOWY, true) : state;
    }

    /**
     * Smothered it turns back to {@code dirt}. SNOWY with snow on top, and where none can lie on it while it snows on it; it
     * thaws where the column is warm enough to melt snow (Thaw). Spreads to Dirt like grass.
     */
    static void grassTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Block dirt) {
        if (!grassCanLive(state, level, pos)) {
            level.setBlockAndUpdate(pos, dirt.withPropertiesOf(state));
            return;
        }
        BlockPos above = pos.above();
        float temp = Thaw.temperature(level, level.getBiome(pos).value(), pos);
        boolean snowy = level.getBlockState(above).is(BlockTags.SNOW)
                || (temp < Thaw.MELT_AT && (state.getValue(SNOWY) || temp < Thaw.SNOW_BELOW && level.isRaining() && level.canSeeSky(above)));
        if (snowy != state.getValue(SNOWY)) level.setBlockAndUpdate(pos, state.setValue(SNOWY, snowy));
        if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(above) < 9) return;
        for (int i = 0; i < SPREAD_TRIES; i++) {
            BlockPos to = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            BlockState d = level.getBlockState(to);
            if (d.is(Blocks.DIRT) && TrampledPathBlock.canStayAlive(d, level, to) && !level.getFluidState(to.above()).is(FluidTags.WATER))
                level.setBlockAndUpdate(to, Blocks.GRASS_BLOCK.defaultBlockState().setValue(SNOWY, level.getBlockState(to.above()).is(BlockTags.SNOW)));
        }
    }

    /**
     * Grass Slab: grass-tinted by season like a Grass Block (client.SeasonalFoliage); smothered it turns back to a Dirt
     * Slab, and it spreads to dirt like grass ({@link #grassTick}).
     */
    public static class Grass extends DirtSlabBlock {
        public static final BooleanProperty SNOWY = DirtSlabBlock.SNOWY;

        public Grass(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(SNOWY, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(SNOWY);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
            return snowUpdate(super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random), dir, neighbourState);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            grassTick(state, level, pos, random, EarthBlocks.DIRT_SLAB.get());
        }
    }

    /** Dirt Stairs: grass creeps onto them like onto a Dirt Slab, making Grass Stairs. */
    public static class Stairs extends StairBlock {
        public Stairs(Properties properties) {
            super(Blocks.DIRT.defaultBlockState(), properties);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            dirtTick(state, level, pos, random, EarthBlocks.GRASS_STAIRS.get());
        }
    }

    /** Grass Stairs: the Grass Slab's colour, snow and spreading on a stair. */
    public static class GrassStairs extends StairBlock {
        public GrassStairs(Properties properties) {
            super(Blocks.GRASS_BLOCK.defaultBlockState(), properties);
            registerDefaultState(defaultBlockState().setValue(SNOWY, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(SNOWY);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
            return snowUpdate(super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random), dir, neighbourState);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            grassTick(state, level, pos, random, EarthBlocks.DIRT_STAIRS.get());
        }
    }
}
