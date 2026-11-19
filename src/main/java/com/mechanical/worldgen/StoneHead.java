package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * A huge weathered stone face sunk to the chin in the ground, staring out over the land: brow, deep eye sockets,
 * a long nose and a mouth line. One in four is twice the size. The ground before it is trodden bare.
 */
final class StoneHead extends StoneAgeFeature {
    static final MapCodec<StoneHead> CODEC = MapCodec.unit(StoneHead::new);
    // the face as rows from the chin up, seen from the front; columns are b = -1, 0, 1.
    // S stone, N nose (sticks out one step), E eye socket (dark, under the overhanging brow), M mouth (dark line)
    private static final String[] FACE = {"SSS", "SMS", "SNS", "ENE", "SSS"};

    @Override
    public MapCodec<StoneHead> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        int s = random.nextInt(4) == 0 ? 2 : 1;
        if (!flat(level, c, 2 * s, 1)) return false;
        Direction face = randomSide(random);
        BlockState stone = (random.nextInt(3) == 0 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState();
        BlockState dark = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        BlockPos base = c.below(s * (1 + random.nextInt(2))); // buried to the chin, some deeper
        Decor.scuff(level, random, Decor.at(c, face, 3 * s, 0, 0), 2.5 * s);
        for (int row = 0; row < FACE.length + 1; row++) {
            for (int b = -1; b <= 1; b++) {
                // head block: three deep behind the face, the crown is one narrower at the back
                for (int a = -2; a <= 0; a++) {
                    if (row == FACE.length && (a == -2 || b != 0 && random.nextBoolean())) continue;
                    voxel(level, random, base, face, s, a, row, b, stone);
                }
                if (row == FACE.length) continue;
                char k = FACE[row].charAt(b + 1);
                if (k == 'N') voxel(level, random, base, face, s, 1, row, b, stone);
                if (k == 'E' || k == 'M') cube(level, base, face, s, 0, row, b, dark);
            }
        }
        // the brow juts out over the eyes
        for (int b = -1; b <= 1; b++) voxel(level, random, base, face, s, 1, FACE.length - 1, b, stone);
        Decor.rocks(level, random, c, 3 + random.nextInt(3), 3 * s, 3 * s + 2, Mechanical.LOOSE_STONE.get());
        return true;
    }

    private static void voxel(WorldGenLevel level, RandomSource random, BlockPos base, Direction face, int s, int a, int y, int b, BlockState stone) {
        cube(level, base, face, s, a, y, b, Decor.weathered(random, stone));
    }

    /** One face voxel: an s*s*s cube of blocks. */
    private static void cube(WorldGenLevel level, BlockPos base, Direction face, int s, int a, int y, int b, BlockState state) {
        for (int i = 0; i < s; i++)
            for (int j = 0; j < s; j++)
                for (int k = 0; k < s; k++) set(level, Decor.at(base, face, a * s + i, y * s + j, b * s + k), state);
    }
}
