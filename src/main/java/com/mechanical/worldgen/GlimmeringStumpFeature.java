package com.mechanical.worldgen;

import com.mechanical.Foraging;
import com.mechanical.folklore.Folklore;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A lone Glimmering Stump (folklore) on dry forest soil, moss around its foot and now and then a Fly Agaric nearby. */
final class GlimmeringStumpFeature extends StoneAgeFeature {
    static final MapCodec<GlimmeringStumpFeature> CODEC = MapCodec.unit(GlimmeringStumpFeature::new);

    @Override
    public MapCodec<GlimmeringStumpFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!level.getBlockState(c.below()).is(BlockTags.DIRT) || !level.getBlockState(c).isAir()) return false;
        set(level, c, Folklore.GLIMMERING_STUMP.get().defaultBlockState());
        for (int i = 0; i < 6; i++) {
            BlockPos p = surface(level, c.getX() + random.nextInt(5) - 2, c.getZ() + random.nextInt(5) - 2);
            if (!level.getBlockState(p).isAir() || !level.getBlockState(p.below()).is(BlockTags.DIRT)) continue;
            set(level, p, i == 0 && random.nextInt(3) == 0 ? Foraging.MUSHROOM_BLOCKS.get("fly_agaric").get().defaultBlockState()
                    : Blocks.MOSS_CARPET.defaultBlockState());
        }
        return true;
    }
}
