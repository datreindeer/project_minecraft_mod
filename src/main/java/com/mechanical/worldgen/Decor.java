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
import net.minecraft.world.level.levelgen.GenerationStep;

/** Helpers for the small decorations (boulders, carvings, statues). Everything stays within 8 blocks of the origin. */
public final class Decor {
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

    /** No village, tribe village or other surface structure reaches into the chunk at pos (mineshafts and the like don't count). */
    static boolean noStructure(WorldGenLevel level, BlockPos pos) {
        return level.getChunk(pos).getAllReferences().keySet().stream().noneMatch(s -> s.step() == GenerationStep.Decoration.SURFACE_STRUCTURES);
    }

    // dev only: with a file decorlog.flag in the game directory, every placed nature decoration is logged (counting and finding them)
    private static final boolean LOG_PLACED = java.nio.file.Files.exists(java.nio.file.Path.of("decorlog.flag"));

    /** Dev only (DecorShots): the last decoration placed by a command (on the server thread) and the angle it logged to view it from. */
    public static volatile BlockPos lastPlaced;
    public static volatile int lastAngle, placedCount;

    static void logPlaced(String what, BlockPos p) {
        if (!LOG_PLACED) return;
        Mechanical.LOGGER.info("decor {} at {} {} {}", what, p.getX(), p.getY(), p.getZ());
        if (!Thread.currentThread().getName().equals("Server thread")) return;
        int dir = what.indexOf("_dir_");
        if (dir >= 0) lastAngle = Integer.parseInt(what.substring(dir + 5));
        else {
            lastPlaced = p.immutable();
            placedCount++;
        }
    }

    private static final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> REJECTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger REJECTED = new java.util.concurrent.atomic.AtomicInteger();

    /** Dev only (decorlog.flag): counts why a decoration gave up ("talus:no_cliff"); the whole tally is logged every 2000. Returns null. */
    static BlockPos reject(String why) {
        if (!LOG_PLACED) return null;
        REJECTS.computeIfAbsent(why, k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
        if (REJECTED.incrementAndGet() % 2000 == 0) Mechanical.LOGGER.info("decor rejects {}", new java.util.TreeMap<>(REJECTS));
        return null;
    }

    /** For logPlaced names: the DecorShots angle (degrees, 0 = camera to the south) that puts a camera on the side of d. */
    static int viewAngle(net.minecraft.core.Direction d) {
        return Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-d.getStepX(), d.getStepZ()))), 360);
    }

    private static double sq(double v) {
        return v * v;
    }

    private Decor() {}
}
