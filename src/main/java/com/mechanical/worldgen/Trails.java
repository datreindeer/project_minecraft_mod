package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

import com.mechanical.EarthBlocks;
import com.mechanical.Mechanical;
import com.mechanical.TrampledPathBlock;
import com.mechanical.TrampledPaths;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Trodden Path footpaths (worn at the edges) between nearby structures tagged {@code mechanical:trail_connected}, with plank footbridges over deep
 * water, stepping stones through shallow water and small stone cairns along the way.
 *
 * <p>Runs once per overworld chunk and only touches that chunk. Endpoints come from the seed alone: for random_spread
 * structure sets the start chunks are computed and the structure's own generation point (biome, terrain, jigsaw) is
 * evaluated exactly like vanilla does, so a trail only leads to structures that really generate. Nodes are linked as a
 * relative neighbourhood graph (no shortcut past a closer third node), each link is a deterministic wobbly curve, and
 * every chunk draws just the part of each curve that crosses it, deciding each column from that column alone. That
 * keeps the trail seamless across chunk borders.
 *
 * <p>Where a river runs in a cut, the footbridge spans it at bank height instead of the trail climbing down to the water
 * (deck height from the noise terrain along the curve, so every chunk agrees). Trails go round a Tribe Village's palisade
 * and an Ancient Oak's trunk to the gate or the hollow's opening instead of through them.
 */
final class Trails extends StoneAgeFeature {
    static final MapCodec<Trails> CODEC = MapCodec.unit(Trails::new);
    static final TagKey<Structure> CONNECTED = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "trail_connected"));
    /** Longest link, largest sideways wobble of a curve, trail-free radius around each endpoint, cairn spacing. */
    static final int MAX_LEN = 640, MAX_AMP = 24, END_R = 8, CAIRN_EVERY = 96;
    /** Trail-free stretch at a walled end: its node sits where the way in starts, and the trail runs right up to it. */
    static final int WALL_END = 2;
    /** Half width of a footbridge deck across the trail's main axis: 3 blocks. */
    static final double BRIDGE_HALF = 1.5;
    /**
     * High footbridges: the deck stands at most DECK_RISE above the water, banks count within BANK_REACH blocks of the
     * water's edge, and wider water than RIVER_MAX (a lake) keeps the low deck.
     */
    static final int DECK_RISE = 12, BANK_REACH = 24, RIVER_MAX = 32;
    /** A trail keeps this far outside a wall (palisade, trunk) and starts bending away this much further out. */
    static final double WALL_GAP = 4, WALL_EASE = 4;
    private static final int SEARCH = 2 * MAX_LEN + MAX_AMP + 32;

    /** A trail end; {@code wall > 0} for a walled structure (a palisade round cx,cz) that trails go round to reach x,z outside its way in. */
    record Node(long id, int x, int z, int cx, int cz, int wall) {
        Node(long id, int x, int z) {
            this(id, x, z, x, z, 0);
        }
    }

    private record Spread(StructureSet set, RandomSpreadStructurePlacement placement, Map<Long, Optional<Node>> nodes) {}

    /** Per-world caches: structure starts are expensive (villages run jigsaw assembly), so each is evaluated once; so are a route's sea check and bridges. */
    private record Cache(ChunkGeneratorStructureState state, List<Spread> spreads, Map<Long, Boolean> routes, Map<Long, List<Span>> spans) {}

    private static volatile Cache cache;

    @Override
    public MapCodec<Trails> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        Cache c = cache(level);
        if (c.spreads.isEmpty()) return false;
        ChunkPos chunk = ChunkPos.containing(origin);
        double mx = chunk.getMiddleBlockX(), mz = chunk.getMiddleBlockZ();
        List<Node> nodes = nodesNear(c, level, generator, chunk);
        boolean any = false;
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node a = nodes.get(i), b = nodes.get(j);
                double d = dist(a.x, a.z, b.x, b.z);
                // a trail bending round a palisade strays further from the straight line than its wobble
                if (d > MAX_LEN || d < 3 * END_R || segmentDist(mx, mz, a, b) > MAX_AMP + 48 || !linked(a, b, d, nodes)) continue;
                List<Node> walls = new ArrayList<>();
                for (Node n : nodes) if (n.wall > 0 && segmentDist(n.cx + 0.5, n.cz + 0.5, a, b) < n.wall + MAX_AMP + 12) walls.add(n);
                walls.sort(Comparator.comparingLong(Node::id)); // the same order in every chunk
                Route r = a.id < b.id ? new Route(a, b, walls) : new Route(b, a, walls);
                RandomState rs = c.state.randomState();
                if (!c.routes.computeIfAbsent(r.seed, k -> r.overLand(generator, rs))) continue;
                if (c.spans.size() > 50_000) c.spans.clear(); // shortcut: crude bound like the node cache
                r.bridge(c.spans.computeIfAbsent(r.seed, k -> r.findSpans(generator, rs, level)));
                any |= draw(level, chunk, r);
            }
        }
        return any;
    }

    private static Cache cache(WorldGenLevel level) {
        ChunkGeneratorStructureState state = level.getLevel().getChunkSource().getGeneratorState();
        Cache c = cache;
        if (c != null && c.state == state) return c;
        List<Spread> spreads = new ArrayList<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (set.value().placement() instanceof RandomSpreadStructurePlacement p
                    && set.value().structures().stream().anyMatch(e -> e.structure().is(CONNECTED)))
                spreads.add(new Spread(set.value(), p, new ConcurrentHashMap<>()));
        }
        c = new Cache(state, spreads, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        cache = c;
        return c;
    }

    /** Every connected structure that generates within SEARCH blocks of the chunk. */
    private static List<Node> nodesNear(Cache c, WorldGenLevel level, ChunkGenerator gen, ChunkPos chunk) {
        List<Node> out = new ArrayList<>();
        int r = SEARCH / 16 + 1;
        long seed = c.state.getLevelSeed();
        for (int s = 0; s < c.spreads.size(); s++) {
            Spread sp = c.spreads.get(s);
            int spacing = sp.placement.spacing();
            for (int gx = Math.floorDiv(chunk.x() - r, spacing); gx <= Math.floorDiv(chunk.x() + r, spacing); gx++) {
                for (int gz = Math.floorDiv(chunk.z() - r, spacing); gz <= Math.floorDiv(chunk.z() + r, spacing); gz++) {
                    ChunkPos p = sp.placement.getPotentialStructureChunk(seed, gx * spacing, gz * spacing);
                    if (Math.abs(p.x() - chunk.x()) > r || Math.abs(p.z() - chunk.z()) > r) continue;
                    if (sp.nodes.size() > 50_000) sp.nodes.clear(); // shortcut: crude bound, an LRU would keep hot entries
                    final int index = s;
                    sp.nodes.computeIfAbsent(p.pack(), k -> start(c, sp, index, level, gen, p)).ifPresent(out::add);
                }
            }
        }
        return out;
    }

    /** Runs the structure set's own selection and generation point check, as ChunkGenerator.createStructures does. */
    private static Optional<Node> start(Cache c, Spread sp, int index, WorldGenLevel level, ChunkGenerator gen, ChunkPos p) {
        if (!sp.placement.isStructureChunk(c.state, p.x(), p.z())) return Optional.empty();
        RandomState rs = c.state.randomState();
        long seed = c.state.getLevelSeed();
        List<StructureSet.StructureSelectionEntry> options = new ArrayList<>(sp.set.structures());
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(seed, p.x(), p.z());
        int total = options.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
        while (!options.isEmpty()) {
            int index2 = 0;
            if (sp.set.structures().size() > 1) {
                int choice = random.nextInt(total);
                for (StructureSet.StructureSelectionEntry o : options) {
                    choice -= o.weight();
                    if (choice < 0) break;
                    index2++;
                }
            }
            StructureSet.StructureSelectionEntry sel = options.get(index2);
            Structure structure = sel.structure().value();
            StructureStart start = structure.generate(sel.structure(), level.getLevel().dimension(), level.registryAccess(), gen,
                    gen.getBiomeSource(), rs.createClimateSampler(SamplerContext.EMPTY_UNCACHED), rs,
                    level.getLevel().getStructureTemplateManager(), seed, p, 0, level, structure.biomes()::contains);
            if (start.isValid()) {
                if (!sel.structure().is(CONNECTED)) return Optional.empty();
                // the first piece is the heart of the structure (a village's meeting point, the oak's trunk)
                var first = start.getPieces().getFirst();
                if (first instanceof TribeVillageStructure.Piece village) { // a palisade: meet the village path at the gate
                    BlockPos gate = village.trailEnd(0), c0 = village.centre();
                    return Optional.of(new Node(mix(p.pack() * 31 + index), gate.getX(), gate.getZ(), c0.getX(), c0.getZ(), TribeVillageStructure.WALL_R));
                }
                if (first instanceof AncientOakStructure.Piece oak) { // round the trunk to the hollow's opening
                    BlockPos door = oak.trailEnd(0), foot = oak.foot();
                    return Optional.of(new Node(mix(p.pack() * 31 + index), door.getX(), door.getZ(), foot.getX(), foot.getZ(), AncientOakStructure.TRUNK_R));
                }
                BoundingBox box = first.getBoundingBox();
                return Optional.of(new Node(mix(p.pack() * 31 + index), box.getCenter().getX(), box.getCenter().getZ()));
            }
            options.remove(index2);
            total -= sel.weight();
        }
        return Optional.empty();
    }

    /** Relative neighbourhood graph: a and b link unless some third node is closer to both of them. */
    private static boolean linked(Node a, Node b, double d, List<Node> nodes) {
        for (Node n : nodes) {
            if (n != a && n != b && Math.max(dist(a.x, a.z, n.x, n.z), dist(b.x, b.z, n.x, n.z)) < d) return false;
        }
        return true;
    }

    /** Draws the part of the route inside this chunk. Each column is decided from its distance to the curve alone. */
    private static boolean draw(WorldGenLevel level, ChunkPos chunk, Route r) {
        int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
        float[] best = new float[256], at = new float[256];
        Arrays.fill(best, Float.MAX_VALUE);
        int n = (int) Math.ceil(r.len * 2);
        boolean hit = false;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n, px = r.x(t), pz = r.z(t);
            if (px < x0 - 4 || px > x0 + 20 || pz < z0 - 4 || pz > z0 + 20) continue;
            // 3.5 out: a high bridge's rails stand up to 3 blocks off the line where a slanting deck steps sideways
            for (int x = Math.max(x0, (int) Math.floor(px - 3.5)); x <= Math.min(x0 + 15, (int) Math.floor(px + 3.5)); x++) {
                for (int z = Math.max(z0, (int) Math.floor(pz - 3.5)); z <= Math.min(z0 + 15, (int) Math.floor(pz + 3.5)); z++) {
                    double dx = x + 0.5 - px, dz = z + 0.5 - pz;
                    int k = (x - x0) * 16 + (z - z0);
                    float d2 = (float) (dx * dx + dz * dz);
                    if (d2 < best[k]) {
                        best[k] = d2;
                        at[k] = (float) t;
                        hit = true;
                    }
                }
            }
        }
        if (hit) {
            List<BlockPos> soil = new ArrayList<>();
            for (int k = 0; k < 256; k++) {
                if (best[k] == Float.MAX_VALUE) continue;
                double t = at[k], s = t * r.len, w = r.width(t), d = Math.sqrt(best[k]);
                int x = x0 + k / 16, z = z0 + k % 16;
                if (r.atEnd(s)) continue;
                Span sp = r.span(x, z);
                if (sp != null) bridge(level, x, z, sp, r);
                else if (d <= Math.max(w, BRIDGE_HALF + 0.1)) column(level, x, z, d, w, t, r, soil);
            }
            // worldgen runs no updateShape: join up the paths here, and their neighbours in chunks drawn earlier
            for (BlockPos p : soil) {
                connect(level, p);
                for (Direction d : Direction.Plane.HORIZONTAL) connect(level, p.relative(d));
            }
        }
        for (double s = CAIRN_EVERY / 2.0; s < r.len - CAIRN_EVERY / 2.0; s += CAIRN_EVERY) {
            double t = s / r.len, off = (r.width(t) + 1.8) * ((mix(r.seed + (long) s) & 1) == 0 ? 1 : -1), h = 3 / r.len;
            double tx = r.x(t + h) - r.x(t - h), tz = r.z(t + h) - r.z(t - h), l = Math.hypot(tx, tz); // beside the trail where it bends too
            int x = (int) Math.floor(r.x(t) - tz / l * off), z = (int) Math.floor(r.z(t) + tx / l * off);
            if (x >> 4 == chunk.x() && z >> 4 == chunk.z()) cairn(level, x, z);
        }
        return hit;
    }

    /** One trail column: worn soil on land, stepping stones in shallow water, a plank deck on posts over deep water. */
    private static void column(WorldGenLevel level, int x, int z, double d, double w, double t, Route r, List<BlockPos> soilPlaced) {
        BlockPos g = top(level, x, z);
        BlockState gs = level.getBlockState(g);
        if (gs.is(BlockTags.LOGS)) return; // the trail just squeezes past trees
        if (level.getFluidState(g).is(FluidTags.WATER) || gs.is(Blocks.ICE) && level.getFluidState(g.below()).is(FluidTags.WATER)) {
            water(level, g, d, w, t, r);
            return;
        }
        if (d > w) return; // only bridges are wider than the trail
        if (!gs.is(BlockTags.SUBSTRATE_OVERWORLD) || gs.is(Blocks.MUDDY_MANGROVE_ROOTS)) return; // houses, fields, sand and rock stay
        double h = hash(x, z, 1);
        boolean edge = d > 0.8 && d > w - 0.6;
        if (edge && h < 0.45) return; // ragged, grassy edges (the middle track is never broken)
        for (int y = 1; y <= 2; y++) {
            BlockState above = level.getBlockState(g.above(y));
            if (!above.isAir() && above.canBeReplaced() && above.getFluidState().isEmpty()) set(level, g.above(y), AIR);
        }
        double m = hash(x, z, 2);
        // Worn Grass along the edges, full-height earth down the middle (coarse dirt, rooted dirt, packed mud, a little gravel): no Trodden Path (its fringe takes the season tint and goes dark) and no Dirt Path (lower than its neighbours, it gets dark edge shading)
        BlockState soil = edge ? path(TrampledPaths.WORN_GRASS) : m < 0.45 ? Blocks.COARSE_DIRT.defaultBlockState()
                : m < 0.7 ? Blocks.ROOTED_DIRT.defaultBlockState() : m < 0.93 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        set(level, g, soil);
        soilPlaced.add(g);
        step(level, g, r.alongX(t));
    }

    /**
     * No cliffs on a trail (docs/living-world-plan.md A7, as RoadBuilding does for tribe roads): where the ground ahead or
     * behind along the trail rises 2 blocks or more, an earth stair (Coarse Dirt or Dirt Stairs) on this column makes it a walk up and one step. Decided from the
     * neighbours' ground alone, like every trail column.
     */
    static void step(WorldGenLevel level, BlockPos g, boolean alongX) {
        for (Direction d : alongX ? new Direction[] {Direction.EAST, Direction.WEST} : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
            BlockPos n = top(level, g.getX() + d.getStepX(), g.getZ() + d.getStepZ());
            if (n.getY() - g.getY() < 2 || !natural(level.getBlockState(n)) || !level.getFluidState(n).isEmpty()) continue;
            if (!level.getBlockState(g.above()).isAir()) return;
            BlockState stair = (hash(g.getX(), g.getZ(), 7) < 0.5 ? EarthBlocks.COARSE_DIRT_STAIRS : EarthBlocks.DIRT_STAIRS).get().defaultBlockState();
            set(level, g.above(), stair.setValue(StairBlock.FACING, d));
            return;
        }
    }

    private static BlockState path(DeferredBlock<TrampledPathBlock> block) {
        return block.get().defaultBlockState().setValue(TrampledPathBlock.PERSISTENT, true); // worldgen trails never grow back
    }

    private static void connect(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!(s.getBlock() instanceof TrampledPathBlock)) return;
        BlockState c = TrampledPathBlock.connect(s, level, p);
        if (c != s) set(level, p, c);
    }

    private static void water(WorldGenLevel level, BlockPos top, double d, double w, double t, Route r) {
        int depth = 1;
        while (depth < 14 && level.getFluidState(top.below(depth)).is(FluidTags.WATER)) depth++;
        BlockPos floor = top.below(depth - 1);
        if (depth <= 2) {
            // stepping stones, flush with the water surface
            if (d > w || d > Math.max(0.7, w - 0.7) || hash(top.getX(), top.getZ(), 3) > 0.65) return;
            for (BlockPos p = floor; p.getY() <= top.getY(); p = p.above())
                set(level, p, (p.equals(top) && hash(top.getX(), top.getZ(), 4) < 0.5 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState());
            return;
        }
        // a 3-wide deck measured across the trail's main axis, so a slanting bridge stays one solid deck, not a diagonal
        // of single slabs; posts stand in pairs on both edges every 4 blocks along that axis
        BlockPos deck = top.above();
        BlockState there = level.getBlockState(deck);
        double across = d * r.stretch(t);
        if (across > BRIDGE_HALF || !there.isAir() && !there.canBeReplaced()) return;
        boolean grounded = !level.getFluidState(floor.below()).is(FluidTags.WATER);
        int along = r.alongX(t) ? top.getX() : top.getZ();
        if (grounded && across > BRIDGE_HALF - 1 && Math.floorMod(along, 4) == 0) {
            BlockState log = r.log().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (BlockPos p = floor; p.getY() <= deck.getY(); p = p.above()) set(level, p, log);
            set(level, deck.above(), r.fence());
        } else {
            set(level, deck, r.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
    }

    /**
     * One column of a high footbridge: a plank deck between beams of bark-clad wood carrying a fence rail, and every 4 blocks a pair
     * of log posts down to the ground or the riverbed with a beam across under the deck. Where the bank comes up to the
     * deck it is cut a notch so the deck runs in level.
     */
    private static void bridge(WorldGenLevel level, int x, int z, Span sp, Route r) {
        boolean deck = sp.deck(x, z), ax = sp.alongX();
        BlockPos at = new BlockPos(x, sp.y(), z), g = top(level, x, z);
        if (g.getY() >= sp.y()) {
            if (!deck || g.getY() > sp.y() + 2) return; // the bank is the rail here
            for (BlockPos p = at; p.getY() <= g.getY(); p = p.above()) if (!natural(level.getBlockState(p)) || !level.getFluidState(p).isEmpty()) return;
            for (int k = 1; k <= 3; k++) clear(level, at.above(k), true);
            set(level, at, r.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            return;
        }
        boolean post = Math.floorMod(ax ? x : z, 4) == 0;
        if (deck) {
            if (post && g.getY() < sp.y() - 1) set(level, at.below(), r.log().setValue(RotatedPillarBlock.AXIS, ax ? Direction.Axis.Z : Direction.Axis.X));
            set(level, at, r.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            for (int k = 1; k <= 3; k++) clear(level, at.above(k), false);
            return;
        }
        if (post && Math.abs(sp.across(x, z)) <= BRIDGE_HALF + 1) { // one post per side, not on the corners where the rail steps sideways
            BlockPos p = g;
            while (p.getY() > level.getMinY() && (!level.getFluidState(p).isEmpty() || level.getBlockState(p).is(Blocks.ICE))) p = p.below();
            BlockState log = r.log().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (p = p.above(); p.getY() <= sp.y(); p = p.above()) set(level, p, log);
        } else {
            set(level, at, r.wood()); // bark all round: a stepping rail shows no log ends
        }
        // worldgen runs no updateShape: the rail joins its neighbours by the same geometry every chunk uses
        set(level, at.above(), r.fence().setValue(FenceBlock.NORTH, sp.rail(x, z - 1)).setValue(FenceBlock.SOUTH, sp.rail(x, z + 1))
                .setValue(FenceBlock.WEST, sp.rail(x - 1, z)).setValue(FenceBlock.EAST, sp.rail(x + 1, z)));
        clear(level, at.above(2), false);
    }

    /** Clears leaves, plants and snow (and with {@code ground}, natural ground) off a bridge. */
    private static void clear(WorldGenLevel level, BlockPos p, boolean ground) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty()) return;
        if (s.is(BlockTags.LEAVES) || s.canBeReplaced() || ground && natural(s)) set(level, p, AIR);
    }

    /** A knee-high trail marker beside the path: a stone on a stone. */
    private static void cairn(WorldGenLevel level, int x, int z) {
        BlockPos g = top(level, x, z);
        BlockState gs = level.getBlockState(g);
        if (!natural(gs) || !level.getFluidState(g).isEmpty() || !level.getBlockState(g.above()).canBeReplaced()
                || !level.getFluidState(g.above()).isEmpty()) return;
        set(level, g.above(), (hash(x, z, 5) < 0.5 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState());
        BlockState cap = level.getBlockState(g.above(2));
        if (cap.isAir() || cap.canBeReplaced() && cap.getFluidState().isEmpty())
            set(level, g.above(2), (hash(x, z, 6) < 0.5 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState());
        if (level.getBlockState(g.above(3)).canBeReplaced() && level.getFluidState(g.above(3)).isEmpty()) set(level, g.above(3), AIR);
    }

    /** The topmost ground, water or ice block at x,z: below air, leaves, plants and snow. */
    static BlockPos top(WorldGenLevel level, int x, int z) {
        int y = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z)) - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        for (int i = 0; i < 48; i++, p.move(Direction.DOWN)) {
            BlockState s = level.getBlockState(p);
            if (!(s.isAir() || s.is(BlockTags.LEAVES) || s.canBeReplaced() && s.getFluidState().isEmpty())) break;
        }
        return p.immutable();
    }

    /**
     * A footbridge high over a river in a cut: the deck (a slab at y) runs straight from (x0,z0) to (x1,z1), the curve's
     * points at t0 and t1 on the two banks. The deck is 3 blocks across the main (x or z) axis, so a slanting bridge steps
     * sideways in whole slices; the rail runs round its sides.
     */
    record Span(double t0, double t1, int y, double x0, double z0, double x1, double z1) {
        boolean alongX() {
            return Math.abs(x1 - x0) >= Math.abs(z1 - z0);
        }

        /** How far the column's centre lies across the main axis from the deck's centre line; NaN past the ends. */
        double across(int x, int z) {
            boolean ax = alongX();
            double m0 = ax ? x0 : z0, m1 = ax ? x1 : z1, v0 = ax ? z0 : x0, v1 = ax ? z1 : x1;
            int m = ax ? x : z, k = ax ? z : x;
            if (m < Math.floor(Math.min(m0, m1)) || m > Math.floor(Math.max(m0, m1))) return Double.NaN;
            return k + 0.5 - (v0 + (v1 - v0) * (m + 0.5 - m0) / (m1 - m0));
        }

        /** Exactly 3 columns per slice (half open, so a centre line on a block edge doesn't give 4). */
        boolean deck(int x, int z) {
            double a = across(x, z);
            return a >= -BRIDGE_HALF && a < BRIDGE_HALF;
        }

        /** The ring of columns round the deck (diagonal neighbours too, so the rail closes where the deck steps sideways). */
        boolean rail(int x, int z) {
            if (Double.isNaN(across(x, z)) || deck(x, z)) return false;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (deck(x + dx, z + dz)) return true;
            return false;
        }
    }

    /**
     * High footbridges along a height profile, one sample per block ({@code floor}: top solid block, {@code surface}: top
     * block counting water). Over each stretch of water the deck goes as high as both banks reach, at most DECK_RISE above
     * the water, as long as each bank climbs at least about 1 block in 2 (a gentle valley takes the trail down to a low
     * bridge instead) within BANK_REACH. A deck less than 3 above the water is no high bridge: the low deck or stepping
     * stones stay. Returns {bank sample before, bank sample after, deck y} for each bridge; the deck covers the samples
     * between, whose ground is all below it.
     */
    static List<int[]> highBridges(int[] floor, int[] surface) {
        List<int[]> out = new ArrayList<>();
        int n = floor.length;
        for (int i = 0; i < n; i++) {
            if (surface[i] <= floor[i]) continue;
            int j = i, water = surface[i];
            while (j + 1 < n && surface[j + 1] > floor[j + 1]) water = Math.max(water, surface[++j]);
            if (j - i < RIVER_MAX) {
                for (int y = water + DECK_RISE; y >= water + 3; y--) {
                    int reach = Math.min(BANK_REACH, 3 + 2 * (y - 1 - water));
                    int a = bank(floor, surface, i, -1, y, reach), b = bank(floor, surface, j, 1, y, reach);
                    if (a < 0 || b < 0) continue;
                    out.add(new int[] {a, b, y});
                    j = b;
                    break;
                }
            }
            i = j;
        }
        return out;
    }

    /** The first dry sample from the water's edge on whose ground a deck at y lands (ground 1 below it or higher). */
    private static int bank(int[] floor, int[] surface, int edge, int dir, int y, int reach) {
        for (int k = edge + dir, steps = 1; k >= 0 && k < floor.length && steps <= reach; k += dir, steps++)
            if (surface[k] <= floor[k] && floor[k] >= y - 1) return k;
        return -1;
    }

    /**
     * A deterministic wobbly curve from a to b; a always has the lower id, so both ends agree on the same curve. Kept as
     * points at even steps of t; bent round walls (as a smoothed polar push out to WALL_GAP past the wall) and straightened
     * over high bridges.
     */
    static final class Route {
        final long seed;
        final double ax, az, dx, dz, chord, nx, nz, a1, k1, p1, a2, k2, p2, p3, p4;
        final boolean spruce;
        /** Length along the curve: the chord unless bent round a wall (then the bent curve's own length). */
        double len;
        private double[] px, pz;
        private boolean bent;
        /** Trail-free stretch at each end (END_R, or WALL_END at a walled structure's way in). */
        private final double endA, endB;
        private List<Span> spans = List.of();

        Route(Node a, Node b) {
            this(a, b, List.of(a, b));
        }

        Route(Node a, Node b, List<Node> walls) {
            seed = mix(a.id * 31 + b.id);
            ax = a.x + 0.5;
            az = a.z + 0.5;
            dx = b.x - a.x;
            dz = b.z - a.z;
            chord = Math.sqrt(dx * dx + dz * dz);
            nx = -dz / chord;
            nz = dx / chord;
            a1 = Math.min(MAX_AMP - 7, chord * 0.07) * (unit(1) * 2 - 1);
            k1 = 0.5 + unit(2);
            p1 = unit(3) * Math.PI * 2;
            a2 = 5 * unit(4);
            k2 = chord / 120;
            p2 = unit(5) * Math.PI * 2;
            p3 = unit(6) * Math.PI * 2;
            p4 = unit(7) * Math.PI * 2;
            spruce = unit(8) < 0.5;
            endA = a.wall > 0 ? WALL_END : END_R;
            endB = b.wall > 0 ? WALL_END : END_R;
            len = chord;
            int n = (int) Math.ceil(chord * 2);
            px = new double[n + 1];
            pz = new double[n + 1];
            for (int i = 0; i <= n; i++) {
                double t = i / (double) n, o = offset(t);
                px[i] = ax + dx * t + nx * o;
                pz[i] = az + dz * t + nz * o;
            }
            for (Node w : walls) if (w.wall > 0) around(w);
            if (bent) smooth(walls);
        }

        private double unit(int salt) {
            return (mix(seed + salt) >>> 11) * 0x1.0p-53;
        }

        private double offset(double t) {
            double s = t * chord, taper = Math.min(1, Math.min(s, chord - s) / 12);
            return Math.sin(Math.PI * t) * (a1 * Math.sin(Math.PI * 2 * k1 * t + p1) + a2 * Math.sin(Math.PI * 2 * k2 * t + p2))
                    + 1.2 * taper * Math.sin(s / 7 + p3);
        }

        /**
         * Pushes the curve out of the circle WALL_GAP outside the wall: every point keeps its angle round the centre (unwrapped
         * along the curve, so the trail goes round the side it passes the centre on) and its radius r becomes at least
         * rc, easing in from rc + WALL_EASE (r' = rc + e·exp((r - rc)/e - 1), which meets r' = r smoothly). A straight line
         * through the circle becomes the arc round it.
         */
        private void around(Node w) {
            double cx = w.cx + 0.5, cz = w.cz + 0.5, rc = w.wall + WALL_GAP, e = WALL_EASE;
            boolean near = false;
            for (int i = 0; i < px.length && !near; i++) near = Math.hypot(px[i] - cx, pz[i] - cz) < rc + e;
            if (!near) return;
            bent = true;
            List<double[]> out = new ArrayList<>(px.length * 2);
            double pa = 0, pr = 0;
            for (int i = 0; i < px.length; i++) {
                double r = Math.hypot(px[i] - cx, pz[i] - cz), a = Math.atan2(pz[i] - cz, px[i] - cx);
                if (i > 0) a += Math.PI * 2 * Math.rint((pa - a) / (Math.PI * 2));
                double rr = r >= rc + e ? r : rc + e * Math.exp((r - rc) / e - 1);
                if (i > 0) { // fill a fast sweep round the wall with points along the arc
                    int k = (int) Math.ceil(Math.abs(a - pa) * Math.max(rr, pr) / 0.5);
                    for (int j = 1; j < k; j++) {
                        double f = j / (double) k, r2 = pr + (rr - pr) * f, a2 = pa + (a - pa) * f;
                        out.add(new double[] {cx + r2 * Math.cos(a2), cz + r2 * Math.sin(a2)});
                    }
                }
                out.add(new double[] {cx + rr * Math.cos(a), cz + rr * Math.sin(a)});
                pa = a;
                pr = rr;
            }
            resample(out);
        }

        /** Evens the bent curve out to half-block steps along its length; len becomes that length. */
        private void resample(List<double[]> pts) {
            double[] cum = new double[pts.size()];
            for (int i = 1; i < pts.size(); i++)
                cum[i] = cum[i - 1] + Math.hypot(pts.get(i)[0] - pts.get(i - 1)[0], pts.get(i)[1] - pts.get(i - 1)[1]);
            len = cum[cum.length - 1];
            int n = Math.max(1, (int) Math.ceil(len * 2));
            px = new double[n + 1];
            pz = new double[n + 1];
            for (int i = 0, j = 0; i <= n; i++) {
                double s = len * i / n;
                while (j < cum.length - 2 && cum[j + 1] < s) j++;
                double f = cum[j + 1] > cum[j] ? Math.min(1, (s - cum[j]) / (cum[j + 1] - cum[j])) : 0;
                px[i] = pts.get(j)[0] + (pts.get(j + 1)[0] - pts.get(j)[0]) * f;
                pz[i] = pts.get(j)[1] + (pts.get(j + 1)[1] - pts.get(j)[1]) * f;
            }
        }

        /** Rounds off the corners where a bent curve meets its arc (3 passes of a 4-block moving average, ends fixed), never back inside a wall. */
        private void smooth(List<Node> walls) {
            int n = px.length - 1, rad = 8;
            for (int pass = 0; pass < 3; pass++) {
                double[] qx = px.clone(), qz = pz.clone();
                for (int i = 1; i < n; i++) {
                    int h = Math.min(rad, Math.min(i, n - i));
                    double sx = 0, sz = 0;
                    for (int j = i - h; j <= i + h; j++) {
                        sx += px[j];
                        sz += pz[j];
                    }
                    qx[i] = sx / (2 * h + 1);
                    qz[i] = sz / (2 * h + 1);
                }
                px = qx;
                pz = qz;
            }
            for (Node w : walls) {
                if (w.wall <= 0) continue;
                double cx = w.cx + 0.5, cz = w.cz + 0.5, min = w.wall + WALL_GAP - 0.5;
                for (int i = 0; i <= n; i++) {
                    double r = Math.hypot(px[i] - cx, pz[i] - cz);
                    if (r < min && r > 0) {
                        px[i] = cx + (px[i] - cx) * min / r;
                        pz[i] = cz + (pz[i] - cz) * min / r;
                    }
                }
            }
            double l = 0;
            for (int i = 1; i <= n; i++) l += Math.hypot(px[i] - px[i - 1], pz[i] - pz[i - 1]);
            len = l;
        }

        double x(double t) {
            return lerp(px, t);
        }

        double z(double t) {
            return lerp(pz, t);
        }

        private static double lerp(double[] a, double t) {
            double f = Math.max(0, Math.min(1, t)) * (a.length - 1);
            int i = Math.min((int) f, a.length - 2);
            return a[i] + (a[i + 1] - a[i]) * (f - i);
        }

        /** Whether the trail runs mostly along x here: the whole route's axis, or the local one where the curve bends round a wall. */
        boolean alongX(double t) {
            if (!bent) return Math.abs(dx) >= Math.abs(dz);
            double h = 6 / len;
            return Math.abs(x(t + h) - x(t - h)) >= Math.abs(z(t + h) - z(t - h));
        }

        /** Distance to the curve times this = distance across the main (x or z) axis, for the low bridge deck. */
        double stretch(double t) {
            if (!bent) return chord / Math.max(Math.abs(dx), Math.abs(dz));
            double h = 6 / len, tx = x(t + h) - x(t - h), tz = z(t + h) - z(t - h);
            return Math.hypot(tx, tz) / Math.max(Math.abs(tx), Math.abs(tz));
        }

        /** Whether s blocks along the curve lies in a trail-free end. */
        boolean atEnd(double s) {
            return s <= endA || len - s <= endB;
        }

        /** Half width: the trail narrows to a single track and widens to three blocks (0.75 always covers a column). */
        double width(double t) {
            return 1.25 + 0.5 * Math.sin(t * len / 23 + p4);
        }

        /** The high bridges along this curve, from the noise terrain (never from placed blocks, so every chunk finds the same). */
        List<Span> findSpans(ChunkGenerator gen, RandomState rs, LevelHeightAccessor heights) {
            int n = Math.max(1, (int) Math.ceil(len));
            int[] floor = new int[n + 1], surface = new int[n + 1];
            Arrays.fill(floor, Integer.MIN_VALUE); // not sampled: dry, and no bank
            Arrays.fill(surface, Integer.MIN_VALUE);
            IntPredicate wet = i -> {
                if (floor[i] == Integer.MIN_VALUE) {
                    int x = (int) Math.floor(x(i / (double) n)), z = (int) Math.floor(z(i / (double) n));
                    floor[i] = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, rs) - 1;
                    surface[i] = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, rs) - 1;
                }
                return surface[i] > floor[i];
            };
            // noise columns are slow: look for water every 2 blocks, then measure each stretch of it and its banks block by block
            // (shortcut: a stream 1 wide can slip between the samples and keep the low deck; sample every block if that shows)
            for (int i = 0; i <= n; i += 2) {
                if (!wet.test(i)) continue;
                int lo = i, hi = i;
                while (lo > 0 && wet.test(lo - 1)) lo--;
                while (hi < n && wet.test(hi + 1)) hi++;
                if (hi - lo >= RIVER_MAX) continue; // a lake gets no high bridge: its shores needn't be measured
                for (int k = Math.max(0, lo - BANK_REACH - 1); k <= Math.min(n, hi + BANK_REACH + 1); k++) wet.test(k);
            }
            List<Span> out = new ArrayList<>();
            for (int[] b : highBridges(floor, surface)) {
                double t0 = b[0] / (double) n, t1 = b[1] / (double) n;
                if (atEnd(t0 * len - 2) || atEnd(t1 * len + 2)) continue; // all of it outside the trail-free ends
                out.add(new Span(t0, t1, b[2], x(t0), z(t0), x(t1), z(t1)));
            }
            return List.copyOf(out);
        }

        /** Lays the high bridges on this route: the curve runs straight across each, bank to bank. */
        void bridge(List<Span> found) {
            spans = found;
            int n = px.length - 1;
            for (Span sp : found) {
                for (int i = (int) Math.ceil(sp.t0 * n); i <= sp.t1 * n; i++) {
                    double f = (i / (double) n - sp.t0) / (sp.t1 - sp.t0);
                    px[i] = sp.x0 + (sp.x1 - sp.x0) * f;
                    pz[i] = sp.z0 + (sp.z1 - sp.z0) * f;
                }
            }
        }

        /** The high bridge this column belongs to (deck or rail), if any. */
        Span span(int x, int z) {
            for (Span sp : spans) if (sp.deck(x, z) || sp.rail(x, z)) return sp;
            return null;
        }

        BlockState log() {
            return (spruce ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG).defaultBlockState();
        }

        BlockState wood() {
            return (spruce ? Blocks.SPRUCE_WOOD : Blocks.OAK_WOOD).defaultBlockState();
        }

        BlockState slab() {
            return (spruce ? Blocks.SPRUCE_SLAB : Blocks.OAK_SLAB).defaultBlockState();
        }

        BlockState fence() {
            return (spruce ? Blocks.SPRUCE_FENCE : Blocks.OAK_FENCE).defaultBlockState();
        }

        /** False if the curve runs over open sea for more than a couple of samples (no trail crosses the ocean). */
        boolean overLand(ChunkGenerator gen, RandomState rs) {
            BiomeResolver biomes = gen.getBiomeSource().createUncachedResolver(rs);
            int y = QuartPos.fromBlock(gen.getSeaLevel()), run = 0;
            for (double s = 0; s < len; s += 8) {
                double t = s / len;
                boolean sea = biomes.getNoiseBiome(QuartPos.fromBlock((int) x(t)), y, QuartPos.fromBlock((int) z(t))).is(BiomeTags.IS_OCEAN);
                run = sea ? run + 1 : 0;
                if (run >= 3) return false;
            }
            return true;
        }
    }

    static double dist(double ax, double az, double bx, double bz) {
        return Math.sqrt((ax - bx) * (ax - bx) + (az - bz) * (az - bz));
    }

    private static double segmentDist(double px, double pz, Node a, Node b) {
        double dx = b.x - a.x, dz = b.z - a.z, t = ((px - a.x) * dx + (pz - a.z) * dz) / (dx * dx + dz * dz);
        t = Math.max(0, Math.min(1, t));
        return dist(px, pz, a.x + dx * t, a.z + dz * t);
    }

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** Position noise in [0,1), shared by all routes so overlapping trails agree. */
    private static double hash(int x, int z, int salt) {
        return (mix(x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL + salt) >>> 11) * 0x1.0p-53;
    }
}
