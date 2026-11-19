package com.mechanical.wildlife.river;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mechanical.FallenLogBlock;
import com.mechanical.TreeFelling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Where a beaver works and what it builds (all bounded scans, run at most about once a second per beaver, the area
 * scans at most once a minute):
 * <ul>
 * <li>Dam: a straight line across the narrowest water near its home ({@link #MAX_DAM} wide at most, solid bank at both
 * ends), two courses: one in the water surface, one on top. When it is whole, the water behind it rises one block, but
 * only if that pond is closed in (a flood fill of at most {@link #MAX_FLOOD} blocks that never spills over a drop);
 * a river that runs on just gets the dam.</li>
 * <li>Lodge: a dome of 3x3 + 3x3 + 1 in open water a few blocks from home, with a wet chamber inside.</li>
 * <li>Trees: only small natural trees (at most {@link #MAX_TREE_LOGS} logs) within {@link #TREE_RANGE} of home.</li>
 * </ul>
 */
final class BeaverWorks {
    static final int MAX_DAM = 7, MAX_FLOOD = 64, MAX_TREE_LOGS = 12, TREE_RANGE = 10, SITE_RANGE = 12, SITE_SEARCH_TICKS = 1200;

    /** Open water surface: a water block with air (or a plant) above. */
    static boolean surfaceWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) && level.getBlockState(pos.above()).getFluidState().isEmpty()
                && level.getBlockState(pos.above()).canBeReplaced();
    }

    /** The top block of a column if it is open water, else null. Only loaded chunks. */
    private static BlockPos surfaceAt(Level level, int x, int z) {
        if (!level.isLoaded(new BlockPos(x, 0, z))) return null;
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
        return surfaceWater(level, top) ? top : null;
    }

    /** The nearest open water within {@code r} columns, or null. */
    static BlockPos findWater(Level level, BlockPos from, int r) {
        BlockPos best = null;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos w = surfaceAt(level, from.getX() + dx, from.getZ() + dz);
                if (w != null && (best == null || w.distSqr(from) < best.distSqr(from))) best = w;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- trees and logs

    /** A small natural tree to gnaw: the lowest log of a trunk standing on the ground, within range of home (a random one of them). */
    static BlockPos findTree(ServerLevel level, BlockPos home, RandomSource random) {
        List<BlockPos> trunks = new ArrayList<>();
        for (int dx = -TREE_RANGE; dx <= TREE_RANGE; dx++) {
            for (int dz = -TREE_RANGE; dz <= TREE_RANGE; dz++) {
                int x = home.getX() + dx, z = home.getZ() + dz;
                if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
                BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                if (!level.getBlockState(pos).is(BlockTags.LOGS)) continue;
                for (int down = 0; down < 16 && level.getBlockState(pos.below()).is(BlockTags.LOGS); down++) pos = pos.below();
                if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && !level.getBlockState(pos.below()).is(BlockTags.LOGS)) trunks.add(pos);
            }
        }
        while (!trunks.isEmpty()) {
            BlockPos pos = trunks.remove(random.nextInt(trunks.size()));
            if (smallTree(level, pos) != null) return pos;
        }
        return null;
    }

    /** The tree cut at {@code base} if it is natural and small enough for a beaver, else null. */
    static TreeFelling.Tree smallTree(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base).is(BlockTags.LOGS)) return null;
        TreeFelling.Tree tree = TreeFelling.find(level, base);
        return tree != null && tree.logs().size() <= MAX_TREE_LOGS ? tree : null;
    }

    /** A fallen log near where the beaver felled its last tree (so it doesn't eat a player's logs elsewhere). */
    static BlockPos findFallenLog(ServerLevel level, BlockPos felled) {
        int r = 7;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int x = felled.getX() + dx, z = felled.getZ() + dz;
                if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                for (BlockPos p : new BlockPos[] {top, top.below()}) {
                    if (level.getBlockState(p).getBlock() instanceof FallenLogBlock) return p;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- dam and lodge

    /** The dam's cells, course 0 (in the water) then course 1 (on top), each from both banks inwards. */
    static List<BlockPos> damCells(Beaver b) {
        List<BlockPos> cells = new ArrayList<>();
        if (b.damStart == null) return cells;
        for (int course = 0; course < 2; course++) {
            for (int i = 0, j = b.damLength - 1; i <= j; i++, j--) {
                cells.add(b.damStart.relative(b.damDir, i).above(course));
                if (j != i) cells.add(b.damStart.relative(b.damDir, j).above(course));
            }
        }
        return cells;
    }

    /** Lodge cells: the ring in the water (the chamber stays open in the middle), the 3x3 roof, the cap. */
    static List<BlockPos> lodgeCells(BlockPos c) {
        List<BlockPos> cells = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) cells.add(c.offset(dx, 0, dz));
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) cells.add(c.offset(dx, 1, dz));
        cells.add(c.above(2));
        return cells;
    }

    /**
     * The narrowest straight line across open water near home: water cells with solid bank at both ends, 2 to
     * {@link #MAX_DAM} long. Sets the beaver's dam line; false if there is none.
     */
    static boolean findDamSite(Level level, Beaver b) {
        BlockPos home = b.getHomePosition();
        BlockPos bestStart = null;
        Direction bestDir = null;
        int bestLen = Integer.MAX_VALUE;
        double bestDist = 0;
        for (int dx = -SITE_RANGE; dx <= SITE_RANGE; dx++) {
            for (int dz = -SITE_RANGE; dz <= SITE_RANGE; dz++) {
                BlockPos w = surfaceAt(level, home.getX() + dx, home.getZ() + dz);
                if (w == null) continue;
                for (Direction dir : new Direction[] {Direction.EAST, Direction.SOUTH}) {
                    int back = run(level, w, dir.getOpposite()), fwd = run(level, w, dir);
                    int len = back + fwd + 1;
                    if (len < 2 || len > MAX_DAM) continue;
                    BlockPos start = w.relative(dir, -back);
                    if (!bank(level, start.relative(dir, -1)) || !bank(level, start.relative(dir, len))) continue;
                    double dist = start.distSqr(home);
                    if (len < bestLen || len == bestLen && dist < bestDist) {
                        bestStart = start;
                        bestDir = dir;
                        bestLen = len;
                        bestDist = dist;
                    }
                }
            }
        }
        if (bestStart == null) return false;
        b.damStart = bestStart;
        b.damDir = bestDir;
        b.damLength = bestLen;
        return true;
    }

    /** Open water cells in a row from {@code from} (not counted) towards {@code dir}, up to MAX_DAM + 1. */
    private static int run(Level level, BlockPos from, Direction dir) {
        int n = 0;
        for (BlockPos p = from.relative(dir); n <= MAX_DAM && surfaceWater(level, p); p = p.relative(dir)) n++;
        return n;
    }

    /** Firm ground a dam can lean on. */
    private static boolean bank(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getFluidState().isEmpty() && !s.canBeReplaced() && s.isFaceSturdy(level, pos, Direction.UP);
    }

    /** A 3x3 of open water 3 to 8 blocks from home and clear of the dam, with air above for the roof. */
    static BlockPos findLodgeSite(Level level, Beaver b) {
        BlockPos home = b.getHomePosition();
        List<BlockPos> dam = damCells(b);
        for (int r = 2; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // ring by ring, nearest first
                    BlockPos c = surfaceAt(level, home.getX() + dx, home.getZ() + dz);
                    if (c == null || dam.stream().anyMatch(d -> d.distManhattan(c) < 4)) continue;
                    boolean ok = true;
                    for (int x = -1; x <= 1 && ok; x++) {
                        for (int z = -1; z <= 1 && ok; z++) {
                            BlockPos p = c.offset(x, 0, z);
                            ok = surfaceWater(level, p) && level.getBlockState(p.above(2)).canBeReplaced();
                        }
                    }
                    if (ok) return c;
                }
            }
        }
        return null;
    }

    /** Can a beaver pack its stuff in here: water, air or a plant, no beaver block yet. */
    private static boolean open(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.is(RiverFauna.BEAVER_DAM.get()) && s.canBeReplaced(); // air, water, plants
    }

    /** The next dam or lodge cell to build, or null when both are whole (or have no site). Finds the sites when due. */
    static BlockPos nextBuild(ServerLevel level, Beaver b) {
        long now = level.getGameTime();
        boolean search = now >= b.nextSiteSearch;
        if (b.damStart == null && search) {
            b.nextSiteSearch = now + SITE_SEARCH_TICKS;
            findDamSite(level, b);
        }
        for (BlockPos p : damCells(b)) {
            // the top course goes only on the bottom one
            if (p.getY() > b.damStart.getY() && !level.getBlockState(p.below()).is(RiverFauna.BEAVER_DAM.get())) continue;
            if (open(level, p)) return p;
        }
        if (b.lodge == null && search) {
            b.nextSiteSearch = now + SITE_SEARCH_TICKS;
            b.lodge = findLodgeSite(level, b);
        }
        if (b.lodge != null) {
            for (BlockPos p : lodgeCells(b.lodge)) if (open(level, p)) return p;
        }
        return null;
    }

    /** Packs a dam block in at {@code pos} (if nothing stands there); a finished dam raises its pond. */
    static boolean place(ServerLevel level, Beaver b, BlockPos pos) {
        if (!open(level, pos) || !level.getEntities(b, new AABB(pos)).isEmpty()) return false;
        // standing in the spot itself: it climbs onto what it packs in
        if (b.getBoundingBox().intersects(new AABB(pos))) b.setPos(b.getX(), pos.getY() + 1, b.getZ());
        if (!level.getBlockState(pos).getFluidState().is(FluidTags.WATER) && !level.getBlockState(pos).isAir()) level.destroyBlock(pos, true, b);
        level.setBlock(pos, RiverFauna.BEAVER_DAM.get().defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
        if (!b.raised && b.damStart != null && damCells(b).stream().allMatch(p -> level.getBlockState(p).is(RiverFauna.BEAVER_DAM.get()))) {
            b.raised = true;
            raisePond(level, b);
        }
        return true;
    }

    /** The water behind a finished dam rises to its top course, on whichever side is closed in. */
    static int raisePond(ServerLevel level, Beaver b) {
        Direction across = b.damDir.getClockWise();
        for (int side : new int[] {1, -1}) {
            Set<BlockPos> pond = flood(level, b, across, side);
            if (pond != null && pond.size() >= 2) {
                for (BlockPos p : pond) {
                    if (!level.getBlockState(p).isAir() && !level.getBlockState(p).getFluidState().is(FluidTags.WATER)) level.destroyBlock(p, true);
                    level.setBlock(p, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                }
                return pond.size();
            }
        }
        return 0;
    }

    /**
     * Cells one above the water on {@code side} of the dam that the raised water would fill, or null if it would run off:
     * more than {@link #MAX_FLOOD} cells, or a cell with nothing to hold it up (a drop).
     */
    static Set<BlockPos> flood(Level level, Beaver b, Direction across, int side) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (int i = 0; i < b.damLength; i++) {
            BlockPos p = b.damStart.relative(b.damDir, i).above().relative(across, side);
            if (seen.add(p)) queue.add(p);
        }
        Set<BlockPos> fill = new HashSet<>();
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            boolean water = s.getFluidState().is(FluidTags.WATER);
            if (!water && !s.canBeReplaced() || s.is(RiverFauna.BEAVER_DAM.get())) continue; // wall
            if (!level.isLoaded(p)) return null;
            BlockState below = level.getBlockState(p.below());
            if (!below.getFluidState().is(FluidTags.WATER) && below.canBeReplaced()) return null; // would spill down
            if (!water || !s.getFluidState().isSource()) fill.add(p);
            if (fill.size() > MAX_FLOOD) return null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        return fill;
    }

    private BeaverWorks() {}
}
