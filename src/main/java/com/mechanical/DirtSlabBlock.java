package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Dirt Slab (living-world plan F11): grass next to it creeps onto it like onto dirt, making a {@link Grass} slab. A shovel
 * flattens either into a Dirt Path Slab (data/minecraft/block_transformer/shovel.json). All three drop a Dirt Slab.
 */
public class DirtSlabBlock extends SlabBlock {
    /** Grass blocks and slabs try this many random spots round them per random tick, as vanilla grass does. */
    static final int SPREAD_TRIES = 4;

    public DirtSlabBlock(Properties properties) {
        super(properties);
    }

    /** Light and nothing smothering it (TrampledPathBlock.canStayAlive, vanilla grass's rule) and not under water. */
    static boolean grassCanLive(BlockState state, LevelReader level, BlockPos pos) {
        return !state.getValue(WATERLOGGED) && TrampledPathBlock.canStayAlive(state, level, pos);
    }

    /** Grass from a Grass Block or Grass Slab round it (the spots vanilla grass spreads to, seen from the dirt) takes over. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(pos.above()) < 9 || !grassCanLive(state, level, pos)) return;
        for (int i = 0; i < SPREAD_TRIES; i++) {
            BlockState from = level.getBlockState(pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 1, random.nextInt(3) - 1));
            if (from.is(Blocks.GRASS_BLOCK) || from.is(EarthBlocks.GRASS_SLAB)) {
                level.setBlockAndUpdate(pos, EarthBlocks.GRASS_SLAB.get().withPropertiesOf(state)
                        .setValue(Grass.SNOWY, level.getBlockState(pos.above()).is(BlockTags.SNOW)));
                return;
            }
        }
    }

    /**
     * Grass Slab: grass-tinted by season like a Grass Block (client.SeasonalFoliage); smothered it turns back to a Dirt
     * Slab, and it spreads to dirt like grass. SNOWY with snow on top, and where none can lie on it (a bottom slab) while it
     * snows on it; it thaws where the column is warm enough to melt snow (Thaw).
     */
    public static class Grass extends DirtSlabBlock {
        public static final BooleanProperty SNOWY = SnowyBlock.SNOWY;

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
            state = super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
            return dir == Direction.UP && neighbourState.is(BlockTags.SNOW) ? state.setValue(SNOWY, true) : state;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!grassCanLive(state, level, pos)) {
                level.setBlockAndUpdate(pos, EarthBlocks.DIRT_SLAB.get().withPropertiesOf(state));
                return;
            }
            BlockPos above = pos.above();
            float temp = Thaw.temperature(level, level.getBiome(pos).value(), pos);
            boolean snowy = level.getBlockState(above).is(BlockTags.SNOW)
                    || (temp < Thaw.MELT_AT && (state.getValue(SNOWY) || temp < Thaw.SNOW_BELOW && level.isRaining() && level.canSeeSky(above)));
            if (snowy != state.getValue(SNOWY)) level.setBlockAndUpdate(pos, state = state.setValue(SNOWY, snowy));
            if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(above) < 9) return;
            for (int i = 0; i < SPREAD_TRIES; i++) {
                BlockPos to = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                BlockState dirt = level.getBlockState(to);
                if (dirt.is(Blocks.DIRT) && TrampledPathBlock.canStayAlive(dirt, level, to) && !level.getFluidState(to.above()).is(net.minecraft.tags.FluidTags.WATER))
                    level.setBlockAndUpdate(to, Blocks.GRASS_BLOCK.defaultBlockState().setValue(SNOWY, level.getBlockState(to.above()).is(BlockTags.SNOW)));
            }
        }
    }
}
