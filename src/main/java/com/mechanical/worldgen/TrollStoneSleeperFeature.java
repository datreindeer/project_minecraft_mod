package com.mechanical.worldgen;

import com.mechanical.folklore.Beings;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A sleeping forest troll (folklore.TrollStone): two blocks of Mossy Troll Stone on taiga soil, moss at its foot. */
final class TrollStoneSleeperFeature extends StoneAgeFeature {
    static final MapCodec<TrollStoneSleeperFeature> CODEC = MapCodec.unit(TrollStoneSleeperFeature::new);

    @Override
    public MapCodec<TrollStoneSleeperFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!level.getBlockState(c.below()).is(BlockTags.DIRT) || !level.getBlockState(c).isAir() || !level.getBlockState(c.above()).isAir()) return false;
        set(level, c, Beings.MOSSY_TROLL_STONE.get().defaultBlockState());
        set(level, c.above(), Beings.MOSSY_TROLL_STONE.get().defaultBlockState());
        for (int i = 0; i < 4; i++) {
            BlockPos p = surface(level, c.getX() + random.nextInt(3) - 1, c.getZ() + random.nextInt(3) - 1);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(BlockTags.DIRT)) set(level, p, Blocks.MOSS_CARPET.defaultBlockState());
        }
        return true;
    }
}
