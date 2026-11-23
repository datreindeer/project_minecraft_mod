package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mechanical.Deadwood;
import com.mechanical.Foraging;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Fallen logs rot (terrain-plan phase 5), a step on most days: fresh, then Moss Carpet on top, then ferns, moss and
 * mushrooms along its sides, then a sapling of its tree comes up beside it, then the bark falls off, and last the log sinks
 * into the forest floor as Moss Block and Podzol, from the ends in, in two days. About a week in all. A fallen log is what
 * docs/biomes.md says: logs (also Deadwood and Charred Log) lying along x or z, the lowest block of their column that
 * isn't leaves, on the ground, another log or over water, with nothing but air, moss, mushrooms, snow or litter on top.
 * <p>
 * Where nobody is, forests keep their deadfall: in biomes with fallen logs a tree now and then comes down unseen and lies
 * there as a new fallen log ({@link #windfall}, ForestDecor's own log).
 */
final class NatureRot {
    /** Chance a log takes its next step on a given day. */
    static final float STEP = 0.85F;
    /** Chance a day brings a new fallen log to a chunk of a forest with fallen logs, and of old growth. */
    static final float WINDFALL = 1 / 12F, WINDFALL_OLD = 1 / 5F;
    private static final int MAX_RUN = 16;

    private NatureRot() {}

    static void day(NatureClock.Day d) {
        if (d.runs("rot")) rot(d, d.random(1));
        if (d.runs("windfall")) windfall(d, d.random(2));
    }

    private static void rot(NatureClock.Day d, RandomSource r) {
        Set<BlockPos> seen = new HashSet<>();
        int x0 = d.pos.getMinBlockX(), z0 = d.pos.getMinBlockZ();
        for (int i = 0; i < 256; i++) {
            BlockPos p = d.top(x0 + (i & 15), z0 + (i >> 4));
            if (seen.contains(p)) continue;
            BlockState s = d.level.getBlockState(p);
            if (s.is(Blocks.SNOW) || s.is(Blocks.MOSS_CARPET)) s = d.level.getBlockState(p = p.below()); // both stop the heightmap
            if (!lying(d.level, p, s)) continue;
            List<BlockPos> run = run(d.level, p, s.getValue(RotatedPillarBlock.AXIS));
            seen.addAll(run);
            BlockPos first = run.getFirst();
            // a log reaching into the next chunk rots in the chunk of its first block only
            if (first.getX() >> 4 != d.pos.x() || first.getZ() >> 4 != d.pos.z() || !run.stream().allMatch(d::can)) continue;
            if (r.nextFloat() < STEP) step(d, r, run, s.getValue(RotatedPillarBlock.AXIS));
        }
    }

    /** Is this a log lying in the wild (not a branch up in a tree)? */
    static boolean lying(ServerLevel level, BlockPos p, BlockState s) {
        if (!wood(s) || s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y) return false;
        BlockState below = level.getBlockState(p.below()), above = level.getBlockState(p.above());
        boolean rests = below.is(BlockTags.SUBSTRATE_OVERWORLD) || wood(below) || below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL)
                || !level.getFluidState(p.below()).isEmpty() || below.isAir() && !level.getFluidState(p.below(2)).isEmpty();
        return rests && (above.isAir() || above.is(Blocks.MOSS_CARPET) || above.is(Blocks.SNOW) || above.is(Blocks.BROWN_MUSHROOM)
                || above.is(Blocks.RED_MUSHROOM) || above.getBlock() instanceof net.minecraft.world.level.block.LeafLitterBlock);
    }

    static boolean wood(BlockState s) {
        return s.is(BlockTags.LOGS) && s.hasProperty(RotatedPillarBlock.AXIS) || s.is(Deadwood.DEADWOOD.get()) || s.is(ScorchedBarrens.CHARRED_LOG.get());
    }

    /** The whole log through p: blocks of the same lie along the axis, stepping a block up or down where it bends to the ground. */
    static List<BlockPos> run(ServerLevel level, BlockPos p, Direction.Axis axis) {
        List<BlockPos> cells = new ArrayList<>(List.of(p));
        for (Direction dir : new Direction[] {Direction.get(Direction.AxisDirection.NEGATIVE, axis), Direction.get(Direction.AxisDirection.POSITIVE, axis)}) {
            BlockPos at = p;
            for (int i = 0; i < MAX_RUN; i++) {
                BlockPos next = null;
                for (int dy : new int[] {0, -1, 1}) {
                    BlockPos q = at.relative(dir).above(dy);
                    BlockState s = level.getBlockState(q);
                    if (wood(s) && s.getValue(RotatedPillarBlock.AXIS) == axis) {
                        next = q;
                        break;
                    }
                }
                if (next == null) break;
                if (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) cells.addFirst(next);
                else cells.add(next);
                at = next;
            }
        }
        return cells;
    }

    /** The log's next step, whichever it hasn't had yet. */
    static void step(NatureClock.Day d, RandomSource r, List<BlockPos> run, Direction.Axis axis) {
        ServerLevel level = d.level;
        // 1. moss on top
        int open = 0, moss = 0;
        for (BlockPos p : run) {
            BlockState up = level.getBlockState(p.above());
            if (up.isAir()) open++;
            else if (up.is(Blocks.MOSS_CARPET)) moss++;
        }
        if (open > 0 && moss < 0.6F * (open + moss)) {
            for (BlockPos p : run)
                if (level.getBlockState(p.above()).isAir() && d.can(p.above()) && r.nextFloat() < 0.75F)
                    d.set(p.above(), (r.nextInt(9) == 0 ? Blocks.BROWN_MUSHROOM : Blocks.MOSS_CARPET).defaultBlockState());
            return;
        }
        // 2. ferns, moss and mushrooms along the sides
        List<BlockPos> beside = beside(level, run, axis);
        long plants = beside.stream().filter(b -> plant(level.getBlockState(b))).count();
        List<BlockPos> bare = beside.stream().filter(b -> level.getBlockState(b).isAir() && d.can(b)).toList();
        if (plants < Math.max(2, run.size() / 2) && !bare.isEmpty()) {
            for (int i = 0; i < 2 + r.nextInt(2); i++) {
                BlockPos b = bare.get(r.nextInt(bare.size()));
                BlockState s = switch (r.nextInt(7)) {
                    case 0, 1, 2 -> Blocks.FERN.defaultBlockState();
                    case 3, 4 -> Blocks.MOSS_CARPET.defaultBlockState();
                    case 5 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
                    default -> Foraging.MUSHROOM_BLOCKS.get(r.nextBoolean() ? "porcini" : "chanterelle").get().defaultBlockState();
                };
                if (level.getBlockState(b).isAir() && (s.is(Blocks.MOSS_CARPET) || s.canSurvive(level, b))) d.set(b, s);
            }
            return;
        }
        // 3. a sapling of its tree comes up beside it (if none grows there yet)
        Block sapling = sapling(level.getBlockState(run.getFirst()));
        boolean young = beside.stream().anyMatch(b -> level.getBlockState(b).is(BlockTags.SAPLINGS));
        if (!young && sapling != null) {
            List<BlockPos> spots = new ArrayList<>(beside.stream().filter(b -> canSprout(level, b, sapling) && d.can(b)).toList());
            if (!spots.isEmpty()) {
                BlockPos b = spots.get(r.nextInt(spots.size()));
                d.set(b, sapling.defaultBlockState());
                return;
            }
        }
        // 4. the bark falls off
        boolean stripped = false;
        for (BlockPos p : run) {
            BlockState s = level.getBlockState(p), bare2 = stripped(s);
            if (bare2 != null) {
                d.set(p, bare2);
                stripped = true;
            }
        }
        if (stripped) return;
        // 5. it sinks into the ground from both ends, half on a day
        int n = Math.max(1, (run.size() + 1) / 2);
        for (int i = 0; i < n && !run.isEmpty(); i++) sink(d, i % 2 == 0 ? run.removeFirst() : run.removeLast());
    }

    /** The ground cells along both sides of the log, where plants can come up (air on natural ground, at or below the log). */
    static List<BlockPos> beside(ServerLevel level, List<BlockPos> run, Direction.Axis axis) {
        Direction side = axis == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : run)
            for (Direction s : new Direction[] {side, side.getOpposite()}) {
                BlockPos b = p.relative(s);
                for (int dy = 0; dy >= -1; dy--) {
                    BlockPos q = b.above(dy);
                    if (level.getBlockState(q.below()).is(BlockTags.SUBSTRATE_OVERWORLD) && level.getFluidState(q).isEmpty()) {
                        out.add(q);
                        break;
                    }
                }
            }
        return out;
    }

    static boolean plant(BlockState s) {
        return s.is(Blocks.FERN) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.BROWN_MUSHROOM) || s.is(Blocks.RED_MUSHROOM) || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.LARGE_FERN) || Foraging.MUSHROOM_BLOCKS.values().stream().anyMatch(m -> s.is(m.get()));
    }

    private static boolean canSprout(ServerLevel level, BlockPos b, Block sapling) {
        return level.getBlockState(b).isAir() && sapling.defaultBlockState().canSurvive(level, b) && level.getBlockState(b.above()).isAir();
    }

    /** One block of the log is gone into the ground: where it lay on the ground the ground turns to Moss Block or Podzol, a half-sunk block becomes one. */
    private static void sink(NatureClock.Day d, BlockPos p) {
        ServerLevel level = d.level;
        BlockState soil = (Math.floorMod(p.getX() * 31 + p.getZ() * 17, 5) < 3 ? Blocks.MOSS_BLOCK : Blocks.PODZOL).defaultBlockState();
        BlockState up = level.getBlockState(p.above());
        if (!up.isAir() && up.canBeReplaced() || up.is(Blocks.BROWN_MUSHROOM) || up.is(Blocks.RED_MUSHROOM)) d.set(p.above(), Blocks.AIR.defaultBlockState());
        BlockState below = level.getBlockState(p.below());
        boolean sunk = below.is(BlockTags.SUBSTRATE_OVERWORLD) && Direction.Plane.HORIZONTAL.stream()
                .filter(s -> s.getAxis() != level.getBlockState(p).getValue(RotatedPillarBlock.AXIS))
                .allMatch(s -> level.getBlockState(p.relative(s)).is(BlockTags.SUBSTRATE_OVERWORLD));
        if (sunk) {
            d.set(p, soil);
            return;
        }
        d.set(p, Blocks.AIR.defaultBlockState());
        if (below.is(BlockTags.SUBSTRATE_OVERWORLD) && d.can(p.below())) {
            d.set(p.below(), soil);
            if (soil.is(Blocks.MOSS_BLOCK) && Math.floorMod(p.getX() + p.getZ() * 7, 3) == 0) d.set(p, Blocks.MOSS_CARPET.defaultBlockState());
        }
    }

    /** The log without its bark (same lie), or null if it has none to lose. */
    static BlockState stripped(BlockState s) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
        if (id.getPath().startsWith("stripped_") || !s.is(BlockTags.LOGS)) return null;
        Block b = BuiltInRegistries.BLOCK.getValue(id.withPath("stripped_" + id.getPath()));
        return b == Blocks.AIR || b == s.getBlock() ? null : b.defaultBlockState().trySetValue(RotatedPillarBlock.AXIS, s.getValue(RotatedPillarBlock.AXIS));
    }

    /** The sapling of the log's tree (oak_log, stripped_oak_log -> oak_sapling); a birch for Deadwood and Charred Log, the first to come back. */
    static Block sapling(BlockState log) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        String wood = id.getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "");
        Block b = BuiltInRegistries.BLOCK.getValue(id.withPath(wood + "_sapling"));
        if (b instanceof net.minecraft.world.level.block.SaplingBlock) return b;
        return Blocks.BIRCH_SAPLING;
    }

    // ---------------------------------------------------------------- new deadfall where nobody is

    private static void windfall(NatureClock.Day d, RandomSource r) {
        if (d.limit != null) return; // touches blocks round the chunk: not in tests or /nature run boxes
        var registry = d.level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        PlacedFeature plain = registry.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, "fallen_log")),
                old = registry.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, "fallen_log_old"));
        BlockPos c = d.top(d.pos.getMiddleBlockX(), d.pos.getMiddleBlockZ());
        Biome biome = d.level.getBiome(c).value();
        float chance = old != null && biome.getGenerationSettings().hasFeature(old) ? WINDFALL_OLD
                : plain != null && biome.getGenerationSettings().hasFeature(plain) ? WINDFALL : 0;
        if (chance == 0 || r.nextFloat() >= chance) return;
        prime(d.level, d.pos.x(), d.pos.z(), 1);
        for (int i = 0; i < 4; i++) // a spot in the open between the trees (under a crown the worldgen ground is the leaves)
            if (ForestDecor.FALLEN_LOG.place(d.level, null, r, new BlockPos(d.pos.getMinBlockX() + 3 + r.nextInt(10), 0, d.pos.getMinBlockZ() + 3 + r.nextInt(10)))) {
                d.changes += 6;
                return;
            }
    }

    /** Live chunks don't keep the worldgen heightmaps ForestDecor reads: rebuild them round chunk cx, cz. */
    static void prime(ServerLevel level, int cx, int cz, int r) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                LevelChunk c = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (c != null) Heightmap.primeHeightmaps(c, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
            }
    }
}
