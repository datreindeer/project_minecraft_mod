package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Leaf litter from a felled tree, still the colour of its leaves. Dries out over time (random ticks) into vanilla's
 * brown leaf litter, keeping its size and facing. Breaking it gives vanilla Leaf Litter either way.
 */
public class FreshLeafLitterBlock extends LeafLitterBlock {
    /** About one random tick in 12: dries in roughly an hour of game time on average. */
    private static final float DRY_CHANCE = 0.08F;

    public FreshLeafLitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= DRY_CHANCE) return;
        level.setBlock(pos, Blocks.LEAF_LITTER.defaultBlockState()
                .setValue(FACING, state.getValue(FACING))
                .setValue(BlockStateProperties.SEGMENT_AMOUNT, state.getValue(BlockStateProperties.SEGMENT_AMOUNT)), UPDATE_ALL);
    }
}
