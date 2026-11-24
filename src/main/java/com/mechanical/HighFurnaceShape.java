package com.mechanical;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Finds a High Furnace from its Front (docs/steel-furnace-plan.md section 3, "Formen"): the empty inside right behind
 * the Front, 2..3 blocks wide and deep ({@link #MIN_WIDE}, {@link #MAX_WIDE}), Silica Bricks under all of it, and a ring
 * of wall round it in every layer, 5..8 layers ({@link #MIN_LAYERS}, {@link #MAX_LAYERS}), open on top. The corners are
 * not part of it. The bottom ring holds the Front (this one), the Fireboxes (one per started 24 inside blocks, at most
 * {@link #MAX_FIREBOXES}) and up to {@link #MAX_TAPHOLES} Tapholes; every other wall is Banded Furnace Casing. A block
 * in a chunk that isn't loaded pauses the furnace instead of breaking it ({@link Result#paused}).
 */
public final class HighFurnaceShape {
    public static final int MIN_WIDE = 2, MAX_WIDE = 3, MIN_LAYERS = 5, MAX_LAYERS = 8, MAX_FIREBOXES = 4, MAX_TAPHOLES = 2, BLOCKS_PER_FIREBOX = 24;

    /** A wall position with the side that faces out (where a bellows or a spout sits). */
    public record Wall(BlockPos pos, Direction out) {}

    /**
     * A formed furnace: the inside from {@code min} to {@code max} (corners, both included; min.y is the Front's layer,
     * max.y the top layer), its bottom ring and the special blocks in it.
     */
    public record Shape(BlockPos min, BlockPos max, List<Wall> ring, List<Wall> fireboxes, List<Wall> tapholes) {
        public int volume() {
            return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }

        public boolean inside(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                    && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
        }

        /** Is {@code p} one of the furnace's wall blocks (any layer, corners not)? */
        public boolean isWall(BlockPos p) {
            if (p.getY() < min.getY() || p.getY() > max.getY()) return false;
            boolean inX = p.getX() >= min.getX() && p.getX() <= max.getX(), inZ = p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
            return inX && (p.getZ() == min.getZ() - 1 || p.getZ() == max.getZ() + 1) || inZ && (p.getX() == min.getX() - 1 || p.getX() == max.getX() + 1);
        }
    }

    /** Exactly one of: a shape, a problem (what is wrong, for the player), or paused (part of it is in an unloaded chunk). */
    public record Result(@Nullable Shape shape, @Nullable Component problem, boolean paused) {
        static Result problem(String key, Object... args) {
            return new Result(null, Component.translatable("message.mechanical.high_furnace." + key, args), false);
        }

        static Result at(String key, BlockPos p, Object... more) {
            Object[] args = new Object[3 + more.length];
            System.arraycopy(more, 0, args, 0, more.length);
            args[more.length] = p.getX();
            args[more.length + 1] = p.getY();
            args[more.length + 2] = p.getZ();
            return problem(key, args);
        }
    }

    private static final Result PAUSED = new Result(null, null, true);

    /** Fireboxes a furnace with {@code volume} inside blocks needs. */
    public static int fireboxesNeeded(int volume) {
        return Math.min(MAX_FIREBOXES, (volume + BLOCKS_PER_FIREBOX - 1) / BLOCKS_PER_FIREBOX);
    }

    public static Result find(BlockGetter level, BlockPos front, Direction facing, Predicate<BlockPos> loaded) {
        Direction in = facing.getOpposite(), side = facing.getClockWise();
        BlockPos first = front.relative(in);
        if (!loaded.test(first)) return PAUSED;
        if (!level.getBlockState(first).isAir()) return Result.at("blocked", first);
        int left = run(level, first, side.getOpposite(), loaded), right = run(level, first, side, loaded), deep = 1 + run(level, first, in, loaded);
        if (left < 0 || right < 0 || deep <= 0) return PAUSED;
        int wide = left + right + 1;
        if (wide < MIN_WIDE || deep < MIN_WIDE) return Result.problem("too_narrow");
        if (wide > MAX_WIDE || deep > MAX_WIDE) return Result.problem("too_wide");
        // the footprint, and the ring round it (no corners)
        List<BlockPos> foot = new ArrayList<>();
        for (int a = -left; a <= right; a++) for (int d = 0; d < deep; d++) foot.add(first.relative(side, a).relative(in, d));
        Set<BlockPos> footSet = Set.copyOf(foot);
        Set<Wall> ringSet = new LinkedHashSet<>();
        for (BlockPos p : foot)
            for (Direction dir : Direction.Plane.HORIZONTAL)
                if (!footSet.contains(p.relative(dir))) ringSet.add(new Wall(p.relative(dir), dir));
        List<Wall> ring = List.copyOf(ringSet);
        for (BlockPos p : foot) {
            BlockPos floor = p.below();
            if (!loaded.test(floor)) return PAUSED;
            if (!level.getBlockState(floor).is(HighFurnace.SILICA_BRICKS.get())) return Result.at("floor", floor);
        }
        // layers: a layer with any wall block belongs to the furnace and must be whole
        int layers = 0;
        for (int dy = 0; dy <= MAX_LAYERS; dy++) {
            int walls = 0;
            BlockPos missing = null;
            for (Wall w : ring) {
                BlockPos p = w.pos().above(dy);
                if (!loaded.test(p)) return PAUSED;
                if (wall(level.getBlockState(p), dy == 0, p.equals(front))) walls++;
                else if (missing == null) missing = p;
            }
            if (walls == 0) break;
            if (missing != null) return Result.at("missing", missing, ring.size() - walls);
            layers++;
        }
        if (layers > MAX_LAYERS) return Result.problem("too_tall", MAX_LAYERS);
        for (int dy = 0; dy < Math.max(layers, 1); dy++)
            for (BlockPos p : foot) {
                BlockPos q = p.above(dy);
                if (!loaded.test(q)) return PAUSED;
                if (!level.getBlockState(q).isAir()) return Result.at("blocked", q);
            }
        if (layers < MIN_LAYERS) return Result.problem("too_low", MIN_LAYERS);
        List<Wall> fireboxes = new ArrayList<>(), tapholes = new ArrayList<>();
        for (Wall w : ring) {
            BlockState s = level.getBlockState(w.pos());
            if (s.is(HighFurnace.FIREBOX.get())) fireboxes.add(w);
            else if (s.is(HighFurnace.TAPHOLE.get())) tapholes.add(w);
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : foot) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        Shape shape = new Shape(new BlockPos(minX, front.getY(), minZ), new BlockPos(maxX, front.getY() + layers - 1, maxZ), ring, fireboxes, tapholes);
        int need = fireboxesNeeded(shape.volume());
        if (fireboxes.size() < need) return Result.problem("fireboxes", need, fireboxes.size());
        if (fireboxes.size() > MAX_FIREBOXES) return Result.problem("too_many_fireboxes", MAX_FIREBOXES);
        if (tapholes.size() > MAX_TAPHOLES) return Result.problem("too_many_tapholes", MAX_TAPHOLES);
        return new Result(shape, null, false);
    }

    /** Air blocks from {@code from} (not counted) going {@code dir}, up to one more than the widest furnace; -1 if unloaded. */
    private static int run(BlockGetter level, BlockPos from, Direction dir, Predicate<BlockPos> loaded) {
        int n = 0;
        for (BlockPos p = from.relative(dir); n <= MAX_WIDE; p = p.relative(dir), n++) {
            if (!loaded.test(p)) return -1;
            if (!level.getBlockState(p).isAir()) break;
        }
        return n;
    }

    /** May this block stand in the wall: casing anywhere; in the bottom layer also Fireboxes, Tapholes and this Front. */
    static boolean wall(BlockState s, boolean bottom, boolean isFront) {
        if (s.is(HighFurnace.BANDED_FURNACE_CASING.get())) return true;
        if (!bottom) return s.is(HighFurnace.SIGHT_GLASS.get()); // a window anywhere above the hearth
        return isFront || s.is(HighFurnace.FIREBOX.get()) || s.is(HighFurnace.TAPHOLE.get());
    }

    private HighFurnaceShape() {}
}
