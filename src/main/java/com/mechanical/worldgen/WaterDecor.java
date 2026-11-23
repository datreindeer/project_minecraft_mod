package com.mechanical.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mechanical.EarthBlocks;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.river.RiverFauna;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * Small things by the water (docs/terrain-plan.md phase 3), placed by scripts/gen_nature_decor.py: log bridges over
 * creeks, beaver dams with their pond and lodge, driftwood on beaches and banks, gravel bars in river shallows, reed belts
 * along lake and creek shores, and hillside springs that run a few blocks down a cut runnel. All of it stays within 15
 * blocks of the origin, and water is only ever added where it is closed in.
 */
final class WaterDecor extends StoneAgeFeature {
    enum Kind { LOG_BRIDGE, BEAVER_DAM, DRIFTWOOD, GRAVEL_BAR, REED_BELT, SPRING }

    static final WaterDecor LOG_BRIDGE = new WaterDecor(Kind.LOG_BRIDGE), BEAVER_DAM = new WaterDecor(Kind.BEAVER_DAM),
            DRIFTWOOD = new WaterDecor(Kind.DRIFTWOOD), GRAVEL_BAR = new WaterDecor(Kind.GRAVEL_BAR), REED_BELT = new WaterDecor(Kind.REED_BELT),
            SPRING = new WaterDecor(Kind.SPRING);
    /** Pale, barkless wood washed up: mostly birch and pale oak, some oak and spruce. */
    private static final Block[] DRIFT = {Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG,
            Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG};
    /** The most cells a beaver pond may rise over. */
    static final int MAX_POND = 96;

    /** A straight crossing of open water: len water cells from start along dir. */
    record Span(BlockPos start, Direction dir, int len) {}

    private final Kind kind;
    private final MapCodec<WaterDecor> codec;

    private WaterDecor(Kind kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<WaterDecor> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!Decor.noStructure(level, c)) return false;
        BlockPos at = switch (kind) {
            case LOG_BRIDGE -> logBridge(level, random, c);
            case BEAVER_DAM -> beaverDam(level, random, c);
            case DRIFTWOOD -> driftwood(level, random, c);
            case GRAVEL_BAR -> gravelBar(level, random, c);
            case REED_BELT -> reedBelt(level, random, c);
            case SPRING -> spring(level, random, c);
        };
        if (at != null) Decor.logPlaced(kind.name().toLowerCase(), at);
        return at != null;
    }

    // ------------------------------------------------------------------ the kinds (each returns where it was built, or null)

    /** A tree fallen across a creek, resting on both banks; sometimes its root plate on one bank and its crown on the other. */
    private static BlockPos logBridge(WorldGenLevel level, RandomSource random, BlockPos c) {
        Span s = crossing(level, c, 5, 2, 6, 1);
        if (s == null) return null;
        ForestDecor.Wood w = ForestDecor.woodNear(level, new BlockPos(c.getX(), s.start().getY() + 1, c.getZ()), 10);
        if (w == null) return null;
        Direction dir = s.dir();
        BlockPos a = s.start().relative(dir, -1), b = s.start().relative(dir, s.len());
        int ya = surface(level, a.getX(), a.getZ()).getY(), yb = surface(level, b.getX(), b.getZ()).getY();
        if (Math.abs(ya - yb) > 1) return null;
        int y = Math.max(ya, yb);
        List<BlockPos> over = new ArrayList<>();
        for (int i = 0; i < s.len(); i++) {
            BlockPos p = s.start().relative(dir, i).atY(y);
            if (!level.getBlockState(p).isAir()) return null;
            over.add(p);
        }
        // onto each bank, the lower one may be a block short of the log at first
        List<BlockPos> bankA = ForestDecor.logCells(level, a.atY(y), dir.getOpposite(), 1 + random.nextInt(3), true);
        List<BlockPos> bankB = ForestDecor.logCells(level, b.atY(y), dir, 1 + random.nextInt(3), true);
        if (bankA.isEmpty() || bankB.isEmpty()) return null;
        List<BlockPos> cells = new ArrayList<>(bankA.reversed());
        cells.addAll(over);
        cells.addAll(bankB);
        ForestDecor.lay(level, cells, random.nextInt(4) == 0 ? w.stripped() : w.log(), dir.getAxis());
        ForestDecor.dressLog(level, random, cells, dir.getAxis(), 0.25F);
        if (random.nextInt(5) < 2) ForestDecor.plate(level, random, bankA.getLast().relative(dir.getOpposite()), dir, 1.6 + random.nextDouble() * 0.8);
        if (w.leaves() != null && random.nextInt(5) < 3) ForestDecor.crown(level, random, bankB.getLast().relative(dir), w.leaves());
        return over.get(over.size() / 2);
    }

    /**
     * A beaver dam across a creek: a wall of sticks and mud (Beaver Dam blocks, some Mud) three courses high, keyed into
     * both banks and bowed a block downstream in the middle, a log or two on top. Where the water above is closed in the
     * pond rises a block, so it stands fuller behind the dam. In the pond a lodge: a low rounded mound 5 across and 3
     * high (3 across and 2 high if the pond is small) with a wet chamber inside. A beaver or two keep it all up.
     */
    private static BlockPos beaverDam(WorldGenLevel level, RandomSource random, BlockPos c) {
        Span s = crossing(level, c, 5, 2, 6, 2);
        if (s == null) return null;
        ForestDecor.Wood w = ForestDecor.woodNear(level, new BlockPos(c.getX(), s.start().getY() + 1, c.getZ()), 10);
        Direction dir = s.dir(), across = dir.getClockWise();
        int wy = s.start().getY();
        for (int course = 0; course < 2; course++)
            for (int i = 0; i < s.len(); i++) set(level, s.start().relative(dir, i).above(course), dam(random, course));
        // the pond: one block up on whichever side is closed in (a river that runs on just gets the dam)
        int side = 0;
        for (int sd : new int[] {1, -1}) {
            Set<BlockPos> pond = pond(level, s, across, sd, c);
            if (pond != null && pond.size() >= 2) {
                side = sd;
                for (BlockPos p : pond) set(level, p, Blocks.WATER.defaultBlockState());
                break;
            }
        }
        int up = side != 0 ? side : 1;
        // the top course, standing above the pond, ends keyed into the banks; the middle bowed out downstream
        for (int i = -1; i <= s.len(); i++) {
            BlockPos p = s.start().relative(dir, i).atY(wy + 2);
            boolean bank = i < 0 || i == s.len();
            if (level.getBlockState(p).canBeReplaced() && level.getFluidState(p).isEmpty() && (!bank || solid(level, p.below())))
                set(level, p, w != null && !bank && random.nextInt(5) == 0 ? ForestDecor.pillar(w.log(), dir.getAxis()) : dam(random, 2));
            if (bank || s.len() < 3 || i == 0 || i == s.len() - 1) continue;
            for (int course = 0; course < 2; course++) {
                BlockPos q = s.start().relative(dir, i).relative(across, -up).above(course);
                if (level.getBlockState(q).canBeReplaced()) set(level, q, dam(random, course));
            }
        }
        int waterY = wy + (side != 0 ? 1 : 0);
        BlockPos mid = s.start().relative(dir, s.len() / 2);
        BlockPos lodge = lodgeSite(level, mid, across, up, waterY, c, 2);
        boolean big = lodge != null;
        if (lodge == null) lodge = lodgeSite(level, mid, across, up, waterY, c, 1);
        if (lodge != null) lodge(level, random, lodge, big, w);
        if (lodge != null) // dev log: the spot between dam and lodge, seen from downstream
            Decor.logPlaced("beaver_view" + (big ? "_big_" : "_small_") + Decor.viewAngle(up > 0 ? across.getOpposite() : across),
                    new BlockPos((mid.getX() + lodge.getX()) / 2, waterY, (mid.getZ() + lodge.getZ()) / 2));
        BlockPos home = lodge != null ? lodge : mid.atY(waterY), lodgeAt = lodge;
        BlockPos stand = mid.atY(wy + 3);
        for (int i = random.nextInt(3) == 0 ? 2 : 1; i > 0; i--)
            spawn(level, random, RiverFauna.BEAVER.get(), stand, b -> b.settle(home, s.start(), dir, s.len(), lodgeAt));
        return mid;
    }

    /** Dam stuff: Beaver Dam blocks (sticks packed with mud), now and then plain Mud low down. */
    private static BlockState dam(RandomSource random, int course) {
        return course == 0 && random.nextInt(5) == 0 ? Blocks.MUD.defaultBlockState() : RiverFauna.BEAVER_DAM.get().defaultBlockState();
    }

    /**
     * The lodge at the water surface centre: a rounded mound (big: 5 across, 3 high; else 3 across, 2 high), its outer
     * ring built up from the pond floor, the chamber in the middle at water level left wet.
     */
    private static void lodge(WorldGenLevel level, RandomSource random, BlockPos centre, boolean big, ForestDecor.Wood w) {
        int[] reach = big ? new int[] {5, 4, 1} : new int[] {2, 1}; // squared radius of each layer
        for (int layer = 0; layer < reach.length; layer++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int d = dx * dx + dz * dz;
                    if (d > reach[layer] || layer == 0 && d == 0) continue;
                    BlockPos p = centre.offset(dx, layer, dz);
                    if (!level.getBlockState(p).canBeReplaced() && !natural(level.getBlockState(p))) continue; // never into a tree
                    set(level, p, dam(random, layer));
                    if (layer == 0 && d >= reach[0] - 1)
                        for (BlockPos q = p.below(); q.getY() > centre.getY() - 4 && level.getFluidState(q).is(FluidTags.WATER); q = q.below()) set(level, q, dam(random, 0));
                }
            }
        }
        if (w != null && random.nextBoolean()) { // a peeled stick poking out of the side
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos p = centre.above().relative(d, big ? 3 : 2);
            if (level.getBlockState(p).isAir()) set(level, p, ForestDecor.pillar(w.stripped(), d.getAxis()));
        }
    }

    /** One to three bleached, barkless logs lying on a beach or bank within 3 blocks of the water. */
    private static BlockPos driftwood(WorldGenLevel level, RandomSource random, BlockPos c) {
        BlockState g = level.getBlockState(c.below());
        if (!(g.is(BlockTags.SAND) || g.is(Blocks.GRAVEL) || g.is(BlockTags.SUBSTRATE_OVERWORLD)) || !level.getBlockState(c).canBeReplaced()
                || !level.getFluidState(c).isEmpty() || !nearWater(level, c, 3)) return null;
        Block wood = DRIFT[random.nextInt(DRIFT.length)];
        boolean any = false;
        for (int i = random.nextInt(3) == 0 ? 2 + random.nextInt(2) : 1; i > 0; i--) {
            BlockPos start = any ? surface(level, c.getX() + random.nextInt(5) - 2, c.getZ() + random.nextInt(5) - 2) : c;
            Direction dir = randomSide(random);
            List<BlockPos> cells = ForestDecor.logCells(level, start, dir, 1 + random.nextInt(4), false);
            if (cells.isEmpty() || !natural(level.getBlockState(start.below()))) continue;
            ForestDecor.lay(level, cells, wood, dir.getAxis());
            any = true;
        }
        return any ? c : null;
    }

    /** A bar of gravel and sand in river shallows next to a bank, its middle just breaking the surface. */
    private static BlockPos gravelBar(WorldGenLevel level, RandomSource random, BlockPos c) {
        BlockPos w = waterAt(level, c.getX(), c.getZ());
        if (w == null || w.getY() - c.getY() > 1 || !landNear(level, w, 4)) return null;
        Direction dir = randomSide(random), across = dir.getClockWise();
        double rx = 4 + random.nextDouble() * 3, rz = 2 + random.nextDouble() * 1.5;
        int broke = 0;
        for (int a = -(int) Math.ceil(rx); a <= rx; a++) {
            for (int b = -(int) Math.ceil(rz); b <= rz; b++) {
                double t = (a / rx) * (a / rx) + (b / rz) * (b / rz);
                if (t > 1 + (random.nextDouble() - 0.5) * 0.3) continue;
                BlockPos col = w.relative(dir, a).relative(across, b);
                BlockPos top = waterAt(level, col.getX(), col.getZ());
                if (top == null || top.getY() != w.getY()) continue;
                int floor = surface(level, col.getX(), col.getZ()).getY();
                if (w.getY() - floor > 2) continue; // shallows only
                int topY = t < 0.6 ? w.getY() : w.getY() - 1;
                for (int y = floor; y <= topY; y++)
                    set(level, col.atY(y), (random.nextInt(5) == 0 ? Blocks.SAND : Blocks.GRAVEL).defaultBlockState());
                if (topY == w.getY()) {
                    broke++;
                    if (random.nextInt(6) == 0) rock(level, random, col.atY(topY + 1), Mechanical.LOOSE_STONE.get());
                }
            }
        }
        return broke >= 3 ? w : null;
    }

    /** A belt of Reed along a still shore, a few lily pads off it. Not where reeds grow already. */
    private static BlockPos reedBelt(WorldGenLevel level, RandomSource random, BlockPos c) {
        Direction along = randomSide(random), across = along.getClockWise();
        BlockState reed = EarthBlocks.REED.get().defaultBlockState();
        List<BlockPos> spots = new ArrayList<>();
        for (int a = -7; a <= 7; a++) {
            for (int b = -3; b <= 3; b++) {
                if ((a / 7.5) * (a / 7.5) + (b / 3.5) * (b / 3.5) > 1) continue;
                BlockPos col = c.relative(along, a).relative(across, b);
                BlockPos top = surface(level, col.getX(), col.getZ());
                if (level.getBlockState(top).is(reed.getBlock()) || level.getBlockState(top.below()).is(reed.getBlock())) return null;
                if (level.getBlockState(top).isAir() && reed.canSurvive(level, top)) spots.add(top);
            }
        }
        if (spots.size() < 5) return null;
        for (BlockPos p : spots) {
            if (random.nextInt(10) < 3) continue;
            for (int k = 0, h = 1 + random.nextInt(3); k < h && level.getBlockState(p.above(k)).isAir(); k++) set(level, p.above(k), reed);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos water = p.below().relative(d);
                if (random.nextInt(8) == 0 && level.getFluidState(water).isSource() && level.getBlockState(water.above()).isAir())
                    set(level, water.above(), Blocks.LILY_PAD.defaultBlockState());
            }
        }
        return c;
    }

    /**
     * A spring on a hillside: a source in the top ground block of the slope and a runnel cut a block deep that steps down
     * at most a block at a time, banked by solid ground on both sides, ending where the ground levels out. Every cell is a
     * source; the overflow at each step just falls into the next, so nothing spreads. Moss and ferns round the spring.
     */
    private static BlockPos spring(WorldGenLevel level, RandomSource random, BlockPos c) {
        BlockPos src = c.below();
        if (!level.getBlockState(src).is(BlockTags.SUBSTRATE_OVERWORLD) || !level.getFluidState(c).isEmpty()) return null;
        Direction down = null;
        for (Direction d : Direction.Plane.HORIZONTAL)
            if (surface(level, c.getX() + d.getStepX() * 3, c.getZ() + d.getStepZ() * 3).getY() <= c.getY() - 2) down = d;
        if (down == null) return null;
        if (!banked(level, src, down) || !solid(level, src.relative(down.getOpposite()))) return null;
        List<BlockPos> cells = new ArrayList<>(List.of(src));
        BlockPos p = src;
        for (int i = 0; ; i++) {
            if (i >= 8) return null; // still running downhill: too long
            BlockPos q = surface(level, p.getX() + down.getStepX(), p.getZ() + down.getStepZ()).below();
            int drop = p.getY() - q.getY();
            if (drop < 0) break; // the ground rises again: the runnel ends at p, against it
            if (drop > 1 || !natural(level.getBlockState(q)) || !banked(level, q, down)) return null;
            cells.add(q);
            p = q;
            if (drop == 0 && solid(level, q.relative(down))) break; // levelled out
        }
        if (cells.size() < 3 || src.getY() - p.getY() < 2 || !solid(level, p.relative(down))) return null;
        for (BlockPos q : cells) {
            BlockState up = level.getBlockState(q.above());
            if (!up.isAir() && up.canBeReplaced()) set(level, q.above(), AIR);
            set(level, q, Blocks.WATER.defaultBlockState());
            level.scheduleTick(q, Fluids.WATER, 0);
        }
        Direction across = down.getClockWise();
        for (Direction d : new Direction[] {across, across.getOpposite(), down.getOpposite()})
            if (random.nextBoolean() && natural(level.getBlockState(src.relative(d))))
                set(level, src.relative(d), (random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSS_BLOCK).defaultBlockState());
        ForestDecor.groundCover(level, random, c, 2, 6);
        return src;
    }

    // ------------------------------------------------------------------ helpers

    /** Open water at the top of column x,z: a water block with no fluid above. */
    static BlockPos waterAt(WorldGenLevel level, int x, int z) {
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
        return level.getFluidState(top).is(FluidTags.WATER) && level.getFluidState(top.above()).isEmpty() ? top : null;
    }

    static boolean surfaceWater(WorldGenLevel level, BlockPos p) {
        return level.getFluidState(p).is(FluidTags.WATER) && level.getFluidState(p.above()).isEmpty() && level.getBlockState(p.above()).canBeReplaced();
    }

    /**
     * The narrowest straight crossing of open water near c: min..max water cells (searched within r of c) with dry natural
     * land at both ends whose top lies bankMin..3 blocks above the water, and water running on to both sides of its middle
     * (a creek, not a puddle).
     */
    static Span crossing(WorldGenLevel level, BlockPos c, int r, int min, int max, int bankMin) {
        Span best = null;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos w = waterAt(level, c.getX() + dx, c.getZ() + dz);
                if (w == null) continue;
                for (Direction dir : new Direction[] {Direction.EAST, Direction.SOUTH}) {
                    int back = run(level, w, dir.getOpposite(), max), fwd = run(level, w, dir, max);
                    int len = back + fwd + 1;
                    if (len < min || len > max || best != null && len >= best.len()) continue;
                    BlockPos start = w.relative(dir, -back), mid = start.relative(dir, len / 2);
                    Direction along = dir.getClockWise();
                    if (bank(level, start.relative(dir, -1), w.getY(), bankMin) && bank(level, start.relative(dir, len), w.getY(), bankMin)
                            && surfaceWater(level, mid.relative(along)) && surfaceWater(level, mid.relative(along, -1)))
                        best = new Span(start, dir, len);
                }
            }
        }
        return best;
    }

    private static int run(WorldGenLevel level, BlockPos from, Direction dir, int max) {
        int n = 0;
        for (BlockPos p = from.relative(dir); n <= max && surfaceWater(level, p); p = p.relative(dir)) n++;
        return n;
    }

    private static boolean bank(WorldGenLevel level, BlockPos p, int waterY, int bankMin) {
        BlockPos top = surface(level, p.getX(), p.getZ());
        int h = top.getY() - waterY;
        return h >= bankMin && h <= 3 && level.getFluidState(top).isEmpty() && natural(level.getBlockState(top.below()));
    }

    /**
     * Cells one above the water on side of the dam the risen pond would fill, or null if it would run off (a cell with
     * nothing under it, more than MAX_POND cells, or reaching more than 14 blocks from c).
     */
    private static Set<BlockPos> pond(WorldGenLevel level, Span s, Direction across, int side, BlockPos c) {
        Set<BlockPos> seen = new HashSet<>(), fill = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (int i = 0; i < s.len(); i++) {
            BlockPos p = s.start().relative(s.dir(), i).above().relative(across, side);
            if (seen.add(p)) queue.add(p);
        }
        Block dam = RiverFauna.BEAVER_DAM.get();
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockState st = level.getBlockState(p);
            boolean water = st.getFluidState().is(FluidTags.WATER);
            if (!water && !st.canBeReplaced() || st.is(dam)) continue; // a wall
            if (Math.abs(p.getX() - c.getX()) > 14 || Math.abs(p.getZ() - c.getZ()) > 14) return null;
            BlockState below = level.getBlockState(p.below());
            if (!below.getFluidState().is(FluidTags.WATER) && below.canBeReplaced()) return null; // would spill down
            if (!water || !st.getFluidState().isSource()) fill.add(p);
            if (fill.size() > MAX_POND) return null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        return fill;
    }

    /**
     * Open water at waterY for a lodge of radius r (2: 5x5 without its corners, 1: 3x3) on side of the dam, its centre 3
     * to 7 blocks out, with room above for the mound.
     */
    private static BlockPos lodgeSite(WorldGenLevel level, BlockPos mid, Direction across, int side, int waterY, BlockPos c, int r) {
        for (int out = r + 2; out <= 7; out++) {
            for (int shift = 0; shift <= 4; shift++) {
                for (int sign : new int[] {1, -1}) {
                    BlockPos centre = mid.relative(across, side * out).relative(across.getClockWise(), shift * sign).atY(waterY);
                    if (Math.abs(centre.getX() - c.getX()) > 15 - r || Math.abs(centre.getZ() - c.getZ()) > 15 - r) continue;
                    boolean ok = level.getBlockState(centre.above(3)).isAir();
                    for (int dx = -r; dx <= r && ok; dx++)
                        for (int dz = -r; dz <= r && ok; dz++) {
                            int d = dx * dx + dz * dz;
                            BlockPos p = centre.offset(dx, 0, dz);
                            // the middle (the chamber) in open water, the rest may lean on the bank (a bank lodge)
                            ok = d > r * r + 1 || surfaceWater(level, p) || d > 0 && lodgeBank(level, p);
                        }
                    if (ok) return centre;
                }
            }
        }
        return null;
    }

    /** Bank a lodge may be built against: natural ground at the water's height, or open air right above such ground. */
    private static boolean lodgeBank(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return level.getFluidState(p).isEmpty() && level.getBlockState(p.above()).canBeReplaced()
                && (natural(s) || s.canBeReplaced() && natural(level.getBlockState(p.below())));
    }

    /** Solid, natural ground on both sides of p across dir, and under it. */
    private static boolean banked(WorldGenLevel level, BlockPos p, Direction dir) {
        Direction across = dir.getClockWise();
        return solid(level, p.relative(across)) && solid(level, p.relative(across.getOpposite())) && solid(level, p.below());
    }

    private static boolean solid(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.canBeReplaced() && s.getFluidState().isEmpty() && s.isSolid();
    }

    private static boolean nearWater(WorldGenLevel level, BlockPos c, int r) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                BlockPos w = waterAt(level, c.getX() + dx, c.getZ() + dz);
                if (w != null && w.getY() <= c.getY() && w.getY() >= c.getY() - 2) return true;
            }
        return false;
    }

    /** Dry ground 1-3 above the water within r of w. */
    private static boolean landNear(WorldGenLevel level, BlockPos w, int r) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                BlockPos top = surface(level, w.getX() + dx, w.getZ() + dz);
                if (level.getFluidState(top).isEmpty() && top.getY() > w.getY() && top.getY() <= w.getY() + 3) return true;
            }
        return false;
    }
}
