package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mechanical.Mechanical;

import static com.mechanical.worldgen.StoneAgeFeature.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Helpers for the small decorations (boulders, carvings, statues). Everything stays within 8 blocks of the origin. */
final class Decor {
    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET};

    /** A rough ellipsoid of stone; the edge is jittered so no two boulders look alike. Returns the placed positions. */
    static List<BlockPos> blob(WorldGenLevel level, RandomSource random, BlockPos center, double rx, double ry, double rz,
            Function<RandomSource, BlockState> stone) {
        List<BlockPos> placed = new ArrayList<>();
        int ix = (int) Math.ceil(rx), iy = (int) Math.ceil(ry), iz = (int) Math.ceil(rz);
        for (int dx = -ix; dx <= ix; dx++) {
            for (int dy = -iy; dy <= iy; dy++) {
                for (int dz = -iz; dz <= iz; dz++) {
                    double d = sq(dx / rx) + sq(dy / ry) + sq(dz / rz);
                    if (d > 1 + (random.nextDouble() - 0.5) * 0.45) continue;
                    BlockPos p = center.offset(dx, dy, dz);
                    set(level, p, stone.apply(random));
                    placed.add(p);
                }
            }
        }
        return placed;
    }

    /** Placed blocks with open air above them: the tops that get moss or snow. */
    static List<BlockPos> tops(WorldGenLevel level, List<BlockPos> placed) {
        return placed.stream().filter(p -> level.getBlockState(p.above()).isAir()).toList();
    }

    /** Trodden ground: swaps the natural top block in a rough disc for coarse dirt, gravel, path and moss. */
    static void scuff(WorldGenLevel level, RandomSource random, BlockPos c, double r) {
        int ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                if (dx * dx + dz * dz > r * r || random.nextInt(5) == 0) continue;
                BlockPos ground = surface(level, c.getX() + dx, c.getZ() + dz).below();
                if (!natural(level.getBlockState(ground)) || !level.getFluidState(ground.above()).isEmpty()) continue;
                set(level, ground, switch (random.nextInt(6)) {
                    case 0, 1 -> Blocks.COARSE_DIRT.defaultBlockState();
                    case 2 -> Blocks.GRAVEL.defaultBlockState();
                    case 3 -> Blocks.DIRT_PATH.defaultBlockState();
                    case 4 -> Blocks.MOSS_BLOCK.defaultBlockState();
                    default -> level.getBlockState(ground);
                });
                // gravel or path under a plant would leave it floating
                BlockState above = level.getBlockState(ground.above());
                if (!above.isAir() && !above.canSurvive(level, ground.above())) set(level, ground.above(), AIR);
            }
        }
    }

    /** A wild flower where one can grow. */
    static void flower(WorldGenLevel level, RandomSource random, BlockPos pos) {
        BlockState f = FLOWERS[random.nextInt(FLOWERS.length)].defaultBlockState();
        if (level.getBlockState(pos).isAir() && f.canSurvive(level, pos)) set(level, pos, f);
    }

    /** Loose rocks scattered on the ground in a ring between r0 and r1 around c. */
    static void rocks(WorldGenLevel level, RandomSource random, BlockPos c, int count, double r0, double r1, Block rock) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = r0 + random.nextDouble() * (r1 - r0);
            rock(level, random, surface(level, c.getX() + (int) Math.round(Math.cos(a) * d), c.getZ() + (int) Math.round(Math.sin(a) * d)), rock);
        }
    }

    /** Local coordinates: a steps toward facing, b steps to its right, y up. */
    static BlockPos at(BlockPos c, Direction facing, int a, int y, int b) {
        return c.relative(facing, a).relative(facing.getClockWise(), b).above(y);
    }

    /** Weathered carving stone: mostly the base, with mossy and cracked patches. */
    static BlockState weathered(RandomSource random, BlockState base) {
        return switch (random.nextInt(8)) {
            case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            default -> base;
        };
    }

    static Block looseFor(BlockState stone) {
        if (stone.is(Blocks.GRANITE)) return Mechanical.LOOSE_GRANITE.get();
        if (stone.is(Blocks.DIORITE) || stone.is(Blocks.CALCITE)) return Mechanical.LOOSE_DIORITE.get();
        if (stone.is(Blocks.ANDESITE) || stone.is(Blocks.TUFF)) return Mechanical.LOOSE_ANDESITE.get();
        return Mechanical.LOOSE_STONE.get();
    }

    private static double sq(double v) {
        return v * v;
    }

    private Decor() {}
}
