package com.mechanical.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A stag, bear or boar hewn from stone, standing in the grass as if frozen mid-step: a hunters' marker. */
final class AnimalCarving extends StoneAgeFeature {
    static final MapCodec<AnimalCarving> CODEC = MapCodec.unit(AnimalCarving::new);
    // voxels as {a (forward), y, b (right), kind}: kind 0 = stone, 1 = wall post (thin legs, antler tines)
    private static final int[][] STAG = {
            {-2, 0, 0, 1}, {-2, 1, 0, 1}, {1, 0, 0, 1}, {1, 1, 0, 1},
            {-2, 2, 0, 0}, {-1, 2, 0, 0}, {0, 2, 0, 0}, {1, 2, 0, 0}, {1, 3, 0, 0}, {1, 4, 0, 0}, {2, 4, 0, 0},
            {1, 5, 0, 1}, {0, 6, -1, 1}, {0, 6, 1, 1}, {1, 6, -1, 1}, {1, 6, 1, 1}, {0, 7, -1, 1}, {0, 7, 1, 1}};
    private static final int[][] BEAR = {
            {-2, 0, 0, 0}, {-2, 0, 1, 0}, {1, 0, 0, 0}, {1, 0, 1, 0},
            {-2, 1, 0, 0}, {-1, 1, 0, 0}, {0, 1, 0, 0}, {1, 1, 0, 0}, {-2, 1, 1, 0}, {-1, 1, 1, 0}, {0, 1, 1, 0}, {1, 1, 1, 0},
            {-1, 2, 0, 0}, {0, 2, 0, 0}, {1, 2, 0, 0}, {-1, 2, 1, 0}, {0, 2, 1, 0}, {1, 2, 1, 0},
            {2, 1, 0, 0}, {2, 1, 1, 0}, {2, 2, 0, 0}, {2, 2, 1, 0}, {3, 1, 0, 0}, {3, 1, 1, 0}};
    private static final int[][] BOAR = {
            {-2, 0, 0, 1}, {1, 0, 0, 1},
            {-2, 1, 0, 0}, {-1, 1, 0, 0}, {0, 1, 0, 0}, {1, 1, 0, 0}, {0, 2, 0, 0}, {1, 2, 0, 0}, {2, 1, 0, 0}, {3, 1, 0, 1}};

    @Override
    public MapCodec<AnimalCarving> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 3, 1)) return false;
        int[][] shape = switch (random.nextInt(3)) {
            case 0 -> STAG;
            case 1 -> BEAR;
            default -> BOAR;
        };
        Direction facing = randomSide(random);
        BlockState stone = switch (random.nextInt(3)) {
            case 0 -> Blocks.ANDESITE.defaultBlockState();
            case 1 -> Blocks.TUFF.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
        BlockState post = (stone.is(Blocks.TUFF) ? Blocks.TUFF_WALL : stone.is(Blocks.ANDESITE) ? Blocks.ANDESITE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState();
        Decor.scuff(level, random, c, 3.5); // before building: it would turn the stone figure's top into dirt
        // a low plinth of flat stones it stands on
        for (int a = -2; a <= 3; a++) foundation(level, Decor.at(c, facing, a, -1, 0), Decor.weathered(random, Blocks.COBBLESTONE.defaultBlockState()));
        if (shape == BEAR) for (int a = -2; a <= 3; a++) foundation(level, Decor.at(c, facing, a, -1, 1), Decor.weathered(random, Blocks.COBBLESTONE.defaultBlockState()));
        for (int[] v : shape) {
            BlockPos p = Decor.at(c, facing, v[0], v[1], v[2]);
            set(level, p, v[3] == 1 ? post : Decor.weathered(random, stone));
        }
        Decor.rocks(level, random, c, 2 + random.nextInt(3), 3, 5, Decor.looseFor(stone));
        return true;
    }
}
