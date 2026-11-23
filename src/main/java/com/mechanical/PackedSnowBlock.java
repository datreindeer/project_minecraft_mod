package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Packed Snow: snow trodden hard on a road or trail (people.RoadBuilding lays it on snowy land). It follows the thaw
 * (Thaw): where the column is warm enough for snow to melt it goes back to the Coarse Dirt it was trodden into, a slab
 * to a Coarse Dirt Slab.
 */
public class PackedSnowBlock extends Block {
    public PackedSnowBlock(Properties properties) {
        super(properties);
    }

    static boolean melts(ServerLevel level, BlockPos pos) {
        return Thaw.temperature(level, level.getBiome(pos).value(), pos) >= Thaw.MELT_AT;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (melts(level, pos)) level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
    }

    /** The slab: melts to a Coarse Dirt Slab of the same type. */
    public static class Slab extends SlabBlock {
        public Slab(Properties properties) {
            super(properties);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (melts(level, pos)) level.setBlockAndUpdate(pos, EarthBlocks.COARSE_DIRT_SLAB.get().withPropertiesOf(state));
        }
    }
}
