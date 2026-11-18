package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A grassy barrow: a stone-lined passage leads into a small dark chamber of bones and grave goods. */
final class BurialMound extends StoneAgeFeature {
    static final MapCodec<BurialMound> CODEC = MapCodec.unit(BurialMound::new);
    private static final double RADIUS = 5.5;
    private static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();

    @Override
    public MapCodec<BurialMound> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 5, 2)) return false;
        // the mound: a low dome of dirt under grass, 4 high in the middle
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS) continue;
                int top = c.getY() + (int) Math.round(4 * (1 - (d / RADIUS) * (d / RADIUS)));
                BlockPos col = surface(level, c.getX() + dx, c.getZ() + dz);
                for (int y = col.getY(); y < top; y++)
                    set(level, col.atY(y), y == top - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
            }
        }
        // the chamber: 3x3x2 inside, stone walls, floor and ceiling
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    boolean inside = Math.abs(dx) < 2 && Math.abs(dz) < 2 && dy >= 0 && dy <= 1;
                    set(level, c.offset(dx, dy, dz), inside ? CAVE_AIR : stone(random));
                }
            }
        }
        // the passage out, through the chamber wall to just past the mound's edge
        Direction out = randomSide(random), p = out.getClockWise();
        for (int step = 2; step <= 6; step++) {
            BlockPos mid = c.relative(out, step);
            foundation(level, mid.below(), stone(random));
            set(level, mid, CAVE_AIR);
            set(level, mid.above(), CAVE_AIR);
            set(level, mid.above(2), stone(random));
            for (int s = -1; s <= 1; s += 2) {
                for (int y = 0; y <= 1; y++) set(level, mid.relative(p, s).above(y), stone(random));
                foundation(level, mid.relative(p, s).below(), stone(random));
            }
        }
        BlockPos back = c.relative(out, -1);
        set(level, back, Blocks.SKELETON_SKULL.defaultBlockState());
        set(level, back.relative(p, -1), Mechanical.BONE_PILE.get().defaultBlockState());
        set(level, c.relative(p, 1), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, out.getAxis()));
        if (random.nextBoolean()) set(level, c.relative(p, -1).relative(out, 1), Mechanical.BONE_PILE.get().defaultBlockState());
        pot(level, random, back.relative(p, 1), "burial_mound");
        if (random.nextBoolean()) pot(level, random, c.relative(out, 1).relative(p, 1), "burial_mound"); // a pot holds one stack
        return true;
    }

    private static BlockState stone(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 1 -> Blocks.STONE.defaultBlockState();
            default -> Blocks.COBBLESTONE.defaultBlockState();
        };
    }
}
