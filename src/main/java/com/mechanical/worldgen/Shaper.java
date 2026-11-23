package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

/**
 * Rounded rock shapes for the small decorations (arches, fallen blocks, outcrops, talus ...): a solid given as a density
 * (inside where it is above 0) is laid at half-block resolution. Each cell samples its eight octants; all or nearly all
 * inside is a full block, the lower or upper half a slab, a half and one or two quarters stairs (straight or outer corner,
 * upright or upside down). Slabs and stairs are the stone's own (SmoothGround.variant: Stone Slab, Granite
 * Stairs, Sand Slab ...); a stone without them uses the fallback's, or stays a full block or nothing.
 */
final class Shaper {
    /** Inside where above 0, in world coordinates. */
    interface Density {
        double at(double x, double y, double z);
    }

    /** The full block for a cell (its slab and stairs are derived from it). */
    interface Stone {
        Block at(BlockPos p);
    }

    /** One laid cell and what went there. */
    record Cell(BlockPos pos, BlockState state) {
        boolean full() {
            return !(state.getBlock() instanceof SlabBlock) && !(state.getBlock() instanceof StairBlock);
        }
    }

    /**
     * Lays the solid in the box from (x0,y0,z0) to (x1,y1,z1) inclusive. Only replaces air, small plants, snow and natural
     * ground (never water, trees or anything built). fallback: a block whose slab and stairs stand in for a stone without.
     */
    static List<Cell> build(WorldGenLevel level, int x0, int y0, int z0, int x1, int y1, int z1, Density d, Stone stone, Block fallback) {
        List<Cell> out = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                for (int y = y0; y <= y1; y++) {
                    int bits = 0;
                    for (int o = 0; o < 8; o++)
                        if (d.at(x + 0.25 + 0.5 * (o & 1), y + 0.25 + 0.5 * (o >> 2), z + 0.25 + 0.5 * ((o >> 1) & 1)) > 0) bits |= 1 << o;
                    if (Integer.bitCount(bits) <= 1) continue;
                    m.set(x, y, z);
                    BlockState here = level.getBlockState(m);
                    if (!free(here)) continue;
                    BlockPos p = m.immutable();
                    BlockState s = shape(bits, stone.at(p), fallback);
                    // a half piece in place of ground would dent it: the ground stays
                    if (s == null || !here.canBeReplaced() && (s.getBlock() instanceof SlabBlock || s.getBlock() instanceof StairBlock)) continue;
                    StoneAgeFeature.set(level, p, s);
                    out.add(new Cell(p, s));
                }
        return out;
    }

    /** Air, a small plant, snow or natural ground, and no water. */
    static boolean free(BlockState s) {
        return s.getFluidState().isEmpty() && (s.canBeReplaced() || StoneAgeFeature.natural(s) || s.is(Blocks.SNOW_BLOCK));
    }

    /**
     * The state for a cell whose octants inside are bits (bit (y << 2) | (z << 1) | x: x 0 west / 1 east, z 0 north / 1
     * south, y 0 lower / 1 upper half), or null for nothing.
     */
    static BlockState shape(int bits, Block base, Block fallback) {
        int lower = bits & 0xF, upper = bits >> 4, n = Integer.bitCount(bits);
        if (n >= 7) return base.defaultBlockState();
        Block slab = variant(base, fallback, true), stairs = variant(base, fallback, false);
        if (lower == 0xF || upper == 0xF) {
            boolean up = upper == 0xF; // the full half is the upper one: an upside-down piece
            int part = up ? lower : upper, k = Integer.bitCount(part);
            if (k == 0) return slab != null ? slab.defaultBlockState().setValue(SlabBlock.TYPE, up ? SlabType.TOP : SlabType.BOTTOM) : null;
            if (stairs != null && (k == 1 || part == 0b0011 || part == 0b1100 || part == 0b0101 || part == 0b1010)) {
                BlockState st = stairs.defaultBlockState().setValue(StairBlock.HALF, up ? Half.TOP : Half.BOTTOM);
                if (k == 2) { // straight: the side the two quarters are on
                    Direction f = part == 0b0011 ? Direction.NORTH : part == 0b1100 ? Direction.SOUTH : part == 0b0101 ? Direction.WEST : Direction.EAST;
                    return st.setValue(StairBlock.FACING, f);
                }
                // one quarter: an outer corner there (seven octants of eight already counted as a full block)
                int q = Integer.numberOfTrailingZeros(part);
                Direction zd = (q & 2) == 0 ? Direction.NORTH : Direction.SOUTH, xd = (q & 1) == 0 ? Direction.WEST : Direction.EAST;
                return st.setValue(StairBlock.FACING, zd).setValue(StairBlock.SHAPE, xd == zd.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT);
            }
            if (k >= 2) return base.defaultBlockState(); // diagonal quarters, or no stairs: round up
            return slab != null ? slab.defaultBlockState().setValue(SlabBlock.TYPE, up ? SlabType.TOP : SlabType.BOTTOM) : base.defaultBlockState();
        }
        int lo = Integer.bitCount(lower), hi = Integer.bitCount(upper);
        if (n >= 5) return base.defaultBlockState();
        if (n <= 2) return null;
        if (slab == null) return n >= 4 ? base.defaultBlockState() : null;
        return slab.defaultBlockState().setValue(SlabBlock.TYPE, hi > lo ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static Block variant(Block base, Block fallback, boolean slab) {
        Block v = slab ? SmoothGround.slab(base) : SmoothGround.stairs(base);
        if (v == Blocks.AIR && fallback != null) v = slab ? SmoothGround.slab(fallback) : SmoothGround.stairs(fallback);
        return v == Blocks.AIR ? null : v;
    }

    /** The full cells with open air above them (for moss, snow, plants on top). */
    static List<BlockPos> tops(WorldGenLevel level, List<Cell> cells) {
        return cells.stream().filter(c -> c.full() && level.getBlockState(c.pos().above()).isAir()).map(Cell::pos).toList();
    }

    /** Fills under each laid cell that has air (or a plant) below it down to the ground with its column's block, so nothing floats. */
    static void seat(WorldGenLevel level, List<Cell> cells, Stone stone, int max) {
        for (Cell c : cells) {
            if (!c.full()) continue;
            BlockPos q = c.pos().below();
            for (int i = 0; i < max && level.getBlockState(q).canBeReplaced() && level.getFluidState(q).isEmpty(); i++, q = q.below())
                StoneAgeFeature.set(level, q, stone.at(q).defaultBlockState());
        }
    }

    private Shaper() {}
}
