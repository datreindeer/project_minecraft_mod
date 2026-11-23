package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
import net.minecraft.world.level.block.Blocks;
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
 */
final class Trails extends StoneAgeFeature {
    static final MapCodec<Trails> CODEC = MapCodec.unit(Trails::new);
    static final TagKey<Structure> CONNECTED = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "trail_connected"));
    /** Longest link, largest sideways wobble of a curve, trail-free radius around each endpoint, cairn spacing. */
    static final int MAX_LEN = 640, MAX_AMP = 24, END_R = 8, CAIRN_EVERY = 96;
    /** Half width of a footbridge deck across the trail's main axis: 3 blocks. */
    static final double BRIDGE_HALF = 1.5;
    private static final int SEARCH = 2 * MAX_LEN + MAX_AMP + 32;

    record Node(long id, int x, int z) {}

    private record Spread(StructureSet set, RandomSpreadStructurePlacement placement, Map<Long, Optional<Node>> nodes) {}

    /** Per-world caches: structure starts are expensive (villages run jigsaw assembly), so each is evaluated once. */
    private record Cache(ChunkGeneratorStructureState state, List<Spread> spreads, Map<Long, Boolean> routes) {}

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
                if (d > MAX_LEN || d < 3 * END_R || segmentDist(mx, mz, a, b) > MAX_AMP + 16 || !linked(a, b, d, nodes)) continue;
                Route r = a.id < b.id ? new Route(a, b) : new Route(b, a);
                if (!c.routes.computeIfAbsent(r.seed, k -> r.overLand(generator, c.state.randomState()))) continue;
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
        c = new Cache(state, spreads, new ConcurrentHashMap<>());
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
                    BlockPos gate = village.trailEnd(END_R);
                    return Optional.of(new Node(mix(p.pack() * 31 + index), gate.getX(), gate.getZ()));
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
            if (px < x0 - 3 || px > x0 + 19 || pz < z0 - 3 || pz > z0 + 19) continue;
            for (int x = Math.max(x0, (int) Math.floor(px - 2.5)); x <= Math.min(x0 + 15, (int) Math.floor(px + 2.5)); x++) {
                for (int z = Math.max(z0, (int) Math.floor(pz - 2.5)); z <= Math.min(z0 + 15, (int) Math.floor(pz + 2.5)); z++) {
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
                if (d <= Math.max(w, BRIDGE_HALF + 0.1) && s > END_R && r.len - s > END_R) column(level, x0 + k / 16, z0 + k % 16, d, w, s, r, soil);
            }
            // worldgen runs no updateShape: join up the paths here, and their neighbours in chunks drawn earlier
            for (BlockPos p : soil) {
                connect(level, p);
                for (Direction d : Direction.Plane.HORIZONTAL) connect(level, p.relative(d));
            }
        }
        for (double s = CAIRN_EVERY / 2.0; s < r.len - CAIRN_EVERY / 2.0; s += CAIRN_EVERY) {
            double t = s / r.len, off = (r.width(t) + 1.8) * ((mix(r.seed + (long) s) & 1) == 0 ? 1 : -1);
            int x = (int) Math.floor(r.x(t) + r.nx * off), z = (int) Math.floor(r.z(t) + r.nz * off);
            if (x >> 4 == chunk.x() && z >> 4 == chunk.z()) cairn(level, x, z);
        }
        return hit;
    }

    /** One trail column: worn soil on land, stepping stones in shallow water, a plank deck on posts over deep water. */
    private static void column(WorldGenLevel level, int x, int z, double d, double w, double s, Route r, List<BlockPos> soilPlaced) {
        BlockPos g = top(level, x, z);
        BlockState gs = level.getBlockState(g);
        if (gs.is(BlockTags.LOGS)) return; // the trail just squeezes past trees
        if (level.getFluidState(g).is(FluidTags.WATER) || gs.is(Blocks.ICE) && level.getFluidState(g.below()).is(FluidTags.WATER)) {
            water(level, g, d, w, s, r);
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
        step(level, g, r);
    }

    /**
     * No cliffs on a trail (docs/living-world-plan.md A7, as RoadBuilding does for tribe roads): where the ground ahead or
     * behind along the trail rises 2 blocks or more, an earth stair (Coarse Dirt or Dirt Stairs) on this column makes it a walk up and one step. Decided from the
     * neighbours' ground alone, like every trail column.
     */
    static void step(WorldGenLevel level, BlockPos g, Route r) {
        for (Direction d : r.alongX ? new Direction[] {Direction.EAST, Direction.WEST} : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
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

    private static void water(WorldGenLevel level, BlockPos top, double d, double w, double s, Route r) {
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
        double across = d * r.axisStretch;
        if (across > BRIDGE_HALF || !there.isAir() && !there.canBeReplaced()) return;
        boolean grounded = !level.getFluidState(floor.below()).is(FluidTags.WATER);
        int along = r.alongX ? top.getX() : top.getZ();
        if (grounded && across > BRIDGE_HALF - 1 && Math.floorMod(along, 4) == 0) {
            BlockState log = r.log().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (BlockPos p = floor; p.getY() <= deck.getY(); p = p.above()) set(level, p, log);
            set(level, deck.above(), r.fence());
        } else {
            set(level, deck, r.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
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

    /** A deterministic wobbly curve from a to b; a always has the lower id, so both ends agree on the same curve. */
    static final class Route {
        final long seed;
        final double ax, az, dx, dz, len, nx, nz, a1, k1, p1, a2, k2, p2, p3, p4;
        final boolean spruce, alongX;
        /** Distance to the curve times this = distance across the main (x or z) axis, for the bridge deck. */
        final double axisStretch;

        Route(Node a, Node b) {
            seed = mix(a.id * 31 + b.id);
            ax = a.x + 0.5;
            az = a.z + 0.5;
            dx = b.x - a.x;
            dz = b.z - a.z;
            len = Math.sqrt(dx * dx + dz * dz);
            nx = -dz / len;
            nz = dx / len;
            a1 = Math.min(MAX_AMP - 7, len * 0.07) * (unit(1) * 2 - 1);
            k1 = 0.5 + unit(2);
            p1 = unit(3) * Math.PI * 2;
            a2 = 5 * unit(4);
            k2 = len / 120;
            p2 = unit(5) * Math.PI * 2;
            p3 = unit(6) * Math.PI * 2;
            p4 = unit(7) * Math.PI * 2;
            spruce = unit(8) < 0.5;
            alongX = Math.abs(dx) >= Math.abs(dz);
            axisStretch = len / Math.max(Math.abs(dx), Math.abs(dz));
        }

        private double unit(int salt) {
            return (mix(seed + salt) >>> 11) * 0x1.0p-53;
        }

        double offset(double t) {
            double s = t * len, taper = Math.min(1, Math.min(s, len - s) / 12);
            return Math.sin(Math.PI * t) * (a1 * Math.sin(Math.PI * 2 * k1 * t + p1) + a2 * Math.sin(Math.PI * 2 * k2 * t + p2))
                    + 1.2 * taper * Math.sin(s / 7 + p3);
        }

        double x(double t) {
            return ax + dx * t + nx * offset(t);
        }

        double z(double t) {
            return az + dz * t + nz * offset(t);
        }

        /** Half width: the trail narrows to a single track and widens to three blocks (0.75 always covers a column). */
        double width(double t) {
            return 1.25 + 0.5 * Math.sin(t * len / 23 + p4);
        }

        BlockState log() {
            return (spruce ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG).defaultBlockState();
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
