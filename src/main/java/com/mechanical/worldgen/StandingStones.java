package com.mechanical.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A ring of 6-9 rough stone pillars, some broken or leaning, around a flat altar stone. */
final class StandingStones extends StoneAgeFeature {
    static final MapCodec<StandingStones> CODEC = MapCodec.unit(StandingStones::new);
    private static final double RADIUS = 4.5;

    @Override
    public MapCodec<StandingStones> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 5, 2)) return false;
        int count = 6 + random.nextInt(4);
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double a = start + i * Math.PI * 2 / count + (random.nextDouble() - 0.5) * 0.3;
            int dx = (int) Math.round(Math.cos(a) * RADIUS), dz = (int) Math.round(Math.sin(a) * RADIUS);
            BlockPos base = surface(level, c.getX() + dx, c.getZ() + dz);
            BlockState stone = stone(random);
            foundation(level, base.below(), stone); // sunk one block into the ground
            int roll = random.nextInt(5), height = 2 + random.nextInt(3);
            if (roll == 0) {
                // broken: a stump with its top lying beside it
                set(level, base, stone);
                BlockPos fallen = surface(level, base.getX() + Integer.signum(dx), base.getZ() + Integer.signum(dz));
                if (level.getFluidState(fallen).isEmpty()) set(level, fallen, stone(random));
                continue;
            }
            for (int y = 0; y < height; y++) set(level, base.above(y), stone);
            if (roll == 1) {
                // a cap stone juts outward from the top
                set(level, base.above(height - 1).offset(Integer.signum(dx), 0, Integer.signum(dz)), stone);
            }
        }
        foundation(level, c.below(), Blocks.STONE.defaultBlockState());
        set(level, c, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        set(level, c.east(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        foundation(level, c.east().below(), Blocks.STONE.defaultBlockState());
        if (random.nextInt(3) == 0) pot(level, random, surface(level, c.getX(), c.getZ() - 1), "standing_stones");
        return true;
    }

    private static BlockState stone(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> Blocks.ANDESITE.defaultBlockState();
            case 1 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 2 -> Blocks.COBBLESTONE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }
}
