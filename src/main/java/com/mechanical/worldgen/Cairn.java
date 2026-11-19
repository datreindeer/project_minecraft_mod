package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Stones piled up by people: either a tapering cairn (sometimes hiding a pot of grave goods under it)
 * or a thin tower of stacked stones with a loose rock balanced on top.
 */
final class Cairn extends StoneAgeFeature {
    static final MapCodec<Cairn> CODEC = MapCodec.unit(Cairn::new);

    @Override
    public MapCodec<Cairn> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 1, 1)) return false;
        if (random.nextInt(3) == 0) tower(level, random, c);
        else pile(level, random, c);
        Decor.rocks(level, random, c, 3 + random.nextInt(4), 2, 4, Mechanical.LOOSE_STONE.get());
        return true;
    }

    private static void pile(WorldGenLevel level, RandomSource random, BlockPos c) {
        boolean hoard = random.nextInt(3) == 0;
        int height = 2 + random.nextInt(3);
        for (int y = 0; y < height; y++) {
            double r = 1.8 * (1 - (double) y / (height + 1)) + 0.3;
            int ir = (int) Math.ceil(r);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    if (dx * dx + dz * dz > r * r + random.nextDouble() * 0.8) continue;
                    BlockPos p = surface(level, c.getX() + dx, c.getZ() + dz).atY(c.getY() + y);
                    if (y == 0) foundation(level, p, stone(random));
                    else if (level.getBlockState(p.below()).isSolid()) set(level, p, stone(random));
                }
            }
        }
        BlockPos top = c.above(height);
        if (level.getBlockState(top.below()).isSolid()) set(level, top, Blocks.COBBLESTONE_WALL.defaultBlockState());
        if (hoard) pot(level, random, c, "cairn"); // inside the pile, under the stones
    }

    private static void tower(WorldGenLevel level, RandomSource random, BlockPos c) {
        foundation(level, c.below(), Blocks.STONE.defaultBlockState());
        int height = 2 + random.nextInt(3);
        for (int y = 0; y < height; y++) set(level, c.above(y), stone(random));
        rock(level, random, c.above(height), Mechanical.LOOSE_STONE.get());
    }

    private static BlockState stone(RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 1 -> Blocks.ANDESITE.defaultBlockState();
            case 2 -> Blocks.STONE.defaultBlockState();
            default -> Blocks.COBBLESTONE.defaultBlockState();
        };
    }
}
