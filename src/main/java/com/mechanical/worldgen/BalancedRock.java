package com.mechanical.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A wide rock balanced on a thin, weathered stem: the softer stone beneath has worn away. */
final class BalancedRock extends StoneAgeFeature {
    static final MapCodec<BalancedRock> CODEC = MapCodec.unit(BalancedRock::new);

    @Override
    public MapCodec<BalancedRock> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 2, 1)) return false;
        boolean sand = level.getBlockState(c.below()).is(BlockTags.SAND);
        boolean red = level.getBlockState(c.below()).is(Blocks.RED_SAND);
        Block stem = sand ? (red ? Blocks.RED_SANDSTONE : Blocks.SANDSTONE) : random.nextBoolean() ? Blocks.STONE : Blocks.ANDESITE;
        Block cap = sand ? (red ? Blocks.SMOOTH_RED_SANDSTONE : Blocks.SMOOTH_SANDSTONE)
                : switch (random.nextInt(4)) {
                    case 0 -> Blocks.GRANITE;
                    case 1 -> Blocks.DIORITE;
                    default -> Blocks.STONE;
                };
        int height = 1 + random.nextInt(3);
        // a broad foot, then the thin stem
        foundation(level, c.below(), stem.defaultBlockState());
        for (int i = 0; i < 4; i++) if (random.nextBoolean()) set(level, Decor.at(c, randomSide(random), 1, 0, 0), stem.defaultBlockState());
        for (int y = 0; y < height; y++) set(level, c.above(y), stem.defaultBlockState());
        double rx = 1.5 + random.nextDouble(), rz = 1.5 + random.nextDouble(), ry = 1 + random.nextDouble() * 0.6;
        BlockPos center = c.above(height + (int) Math.ceil(ry) - 1);
        Decor.blob(level, random, center, rx, ry, rz, r -> cap.defaultBlockState());
        set(level, center.below((int) Math.ceil(ry) - 1), cap.defaultBlockState()); // the jittered edge must never cut it off the stem
        for (int y = 0; y < height; y++) set(level, c.above(y), stem.defaultBlockState());
        Decor.rocks(level, random, c, 2 + random.nextInt(3), 2, 4, Decor.looseFor(cap.defaultBlockState()));
        return true;
    }
}
