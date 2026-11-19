package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import com.mechanical.AncientOak;
import com.mechanical.Mechanical;
import com.mechanical.OfferingAltarBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The Ancient Oak: a huge, unbreakable, faintly magical oak, as a structure (find it with /locate structure mechanical:ancient_oak).
 * One piece holds a seed and the trunk's foot; the whole tree is computed from those alone (and the noise terrain height),
 * so every chunk draws its own slice of the same tree. Placement (rarity, biomes) is data from scripts/gen_ancient_oak.py.
 * <ul>
 * <li>trunk 12-13 wide at the foot, flaring into 7 root buttresses, tapering to about 7 wide at the fork (22-26 up)
 * <li>a hollow room (about 9 wide, 5 high) with a wide opening between two buttresses, a chimney above, the Offering Altar
 * <li>roots snaking 15-20 blocks out over and into the ground
 * <li>6-7 arching limbs (5 wide at the trunk) that split into branches and twigs, leaf clusters at the twig ends; ~50 high
 * <li>glow berry vines, spore blossoms, glow lichen, glowcaps, firefly bushes, moss and azalea round the foot, rune bark
 * </ul>
 */
public final class AncientOakStructure extends Structure {
    public static final MapCodec<AncientOakStructure> CODEC = simpleCodec(AncientOakStructure::new);
    public static final StructureType<AncientOakStructure> TYPE = () -> CODEC;
    public static final StructurePieceType.ContextlessType PIECE = Piece::new;
    private static final int RADIUS = 30, HEIGHT = 64;

    public AncientOakStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = Mechanical.MODID)
    static final class Registration {
        @SubscribeEvent
        static void register(RegisterEvent event) {
            Identifier id = Identifier.fromNamespaceAndPath(Mechanical.MODID, "ancient_oak");
            event.register(Registries.STRUCTURE_TYPE, id, () -> TYPE);
            event.register(Registries.STRUCTURE_PIECE, id, () -> PIECE);
        }
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!context.couldValidBiomeExistOnTopOfChunkCenter()) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        ChunkGenerator gen = context.chunkGenerator();
        int g = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        // dry and fairly level under the trunk and buttresses
        for (int i = 0; i < 13; i++) {
            double a = i * Math.PI * 2 / 12, r = i == 12 ? 0 : 10;
            int px = x + (int) (Math.cos(a) * r), pz = z + (int) (Math.sin(a) * r);
            int floor = gen.getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            int surface = gen.getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            if (floor != surface || Math.abs(floor - g) > 4) return Optional.empty();
        }
        long seed = context.random().nextLong();
        BlockPos foot = new BlockPos(x, g, z);
        return Optional.of(new GenerationStub(foot, builder -> builder.addPiece(new Piece(seed, foot))));
    }

    // ------------------------------------------------------------------ the piece

    public static final class Piece extends StructurePiece {
        private final long seed;
        private final BlockPos foot;

        Piece(long seed, BlockPos foot) {
            super(PIECE, 0, new BoundingBox(foot.getX() - RADIUS, foot.getY() - 8, foot.getZ() - RADIUS,
                    foot.getX() + RADIUS, foot.getY() + HEIGHT, foot.getZ() + RADIUS));
            this.seed = seed;
            this.foot = foot;
        }

        Piece(CompoundTag tag) {
            super(PIECE, tag);
            seed = tag.getLongOr("seed", 0);
            foot = BlockPos.of(tag.getLongOr("foot", 0));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putLong("seed", seed);
            tag.putLong("foot", foot.asLong());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                BoundingBox box, ChunkPos chunkPos, BlockPos referencePos) {
            RandomState rs = level.getLevel().getChunkSource().randomState();
            Shape shape = Shape.get(seed, foot, (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs));
            clearTrees(level, box);
            for (int mode = 0; mode < Shape.MODES; mode++) {
                for (Map.Entry<BlockPos, BlockState> e : shape.layers.get(mode).entrySet()) {
                    BlockPos p = e.getKey();
                    if (!box.isInside(p)) continue;
                    BlockState old = level.getBlockState(p);
                    boolean ok = switch (mode) {
                        case Shape.LEAF, Shape.DECOR -> old.isAir() || old.canBeReplaced() && old.getFluidState().isEmpty() || old.is(BlockTags.LEAVES) && mode == Shape.LEAF;
                        case Shape.GROUND -> old.is(BlockTags.DIRT);
                        default -> true;
                    };
                    if (ok) level.setBlock(p, e.getValue(), 2);
                }
            }
            if (box.isInside(foot) && level.getBlockEntity(foot) instanceof OfferingAltarBlockEntity altar) {
                altar.setEntrance(shape.entrance);
                if (random.nextBoolean()) StoneAgeFeature.spawn(level, random, EntityTypes.BAT, foot.above(6), bat -> bat.setResting(true));
            }
        }

        /** Trees from the biome are removed under the oak (trees cut at the edge of this circle are the price of chunk-wise drawing). */
        private void clearTrees(WorldGenLevel level, BoundingBox box) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    double d = Math.hypot(x - foot.getX(), z - foot.getZ());
                    if (d > 26 || x < boundingBox.minX() || x > boundingBox.maxX() || z < boundingBox.minZ() || z > boundingBox.maxZ()) continue;
                    for (int y = foot.getY() - 4; y <= foot.getY() + HEIGHT; y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (s.is(BlockTags.LOGS) || s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)
                                || s.is(Blocks.VINE) || s.is(Blocks.BEE_NEST) || s.is(Blocks.COCOA))
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ the shape

    /** The whole tree as block placements by layer, computed once per tree (small cache: chunks of one tree come in bursts). */
    static final class Shape {
        static final int SOLID = 0, AIR = 1, LEAF = 2, GROUND = 3, DECOR = 4, MODES = 5;
        private static final int TWIG = 0, BRANCH = 1, LOG = 2;
        private static final double LIMB_REACH = 20, BRANCH_REACH = 23, LEAF_REACH = 27;
        private static final Map<Long, Shape> CACHE = new LinkedHashMap<>(8, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Shape> eldest) {
                return size() > 4;
            }
        };

        static Shape get(long seed, BlockPos foot, IntBinaryOperator ground) {
            long key = seed ^ foot.asLong() * 31;
            synchronized (CACHE) {
                Shape s = CACHE.get(key);
                if (s != null) return s;
            }
            Shape s = new Shape(seed, foot, ground); // computed outside the lock; a rare duplicate is harmless
            synchronized (CACHE) {
                CACHE.put(key, s);
            }
            return s;
        }

        final List<Map<BlockPos, BlockState>> layers = new ArrayList<>();
        BlockPos entrance;
        private final RandomSource rnd;
        private final int x0, z0, g;
        private final IntBinaryOperator groundFn;
        private final Map<Long, Integer> groundCache = new HashMap<>();
        private final Set<BlockPos> trunk = new HashSet<>();
        private final Map<BlockPos, Integer> wood = new HashMap<>();
        private final Map<BlockPos, Direction.Axis> axis = new HashMap<>();
        private final Map<BlockPos, Integer> links = new HashMap<>();
        private final Set<BlockPos> leaves = new HashSet<>();
        private final double[] roots;
        private final double door, ph1, ph2, ph3;
        private final int fork;

        private Shape(long seed, BlockPos foot, IntBinaryOperator ground) {
            for (int i = 0; i < MODES; i++) layers.add(new HashMap<>());
            rnd = RandomSource.create(seed);
            x0 = foot.getX();
            z0 = foot.getZ();
            g = foot.getY();
            groundFn = ground;
            int n = 7;
            roots = new double[n];
            double base = rnd.nextDouble() * Math.PI * 2;
            for (int i = 0; i < n; i++) roots[i] = base + i * Math.PI * 2 / n + (rnd.nextDouble() - 0.5) * 0.3;
            door = (roots[0] + roots[1]) / 2;
            ph1 = rnd.nextDouble() * 6.3;
            ph2 = rnd.nextDouble() * 6.3;
            ph3 = rnd.nextDouble() * 6.3;
            fork = 25 + rnd.nextInt(5);
            entrance = BlockPos.containing(x0 + 0.5 + Math.cos(door) * 14, 0, z0 + 0.5 + Math.sin(door) * 14);
            entrance = new BlockPos(entrance.getX(), ground(entrance.getX(), entrance.getZ()), entrance.getZ());

            trunk();
            for (double a : roots) root(a, 15 + rnd.nextInt(6), 5.0, true);
            int limbs = 6 + rnd.nextInt(2);
            double lb = rnd.nextDouble() * Math.PI * 2;
            for (int i = 0; i < limbs; i++) {
                double az = lb + i * Math.PI * 2 / limbs + (rnd.nextDouble() - 0.5) * 0.4;
                int ys = fork - 7 + rnd.nextInt(9);
                limb(az, Math.toRadians(22 + rnd.nextInt(18)), ys, 17 + rnd.nextDouble() * 4);
            }
            for (int i = 0; i < 2; i++) limb(rnd.nextDouble() * Math.PI * 2, Math.toRadians(64 + rnd.nextInt(14)), fork + 1, 17 + rnd.nextDouble() * 5);
            emit();
        }

        int ground(int x, int z) {
            return groundCache.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> groundFn.applyAsInt(x, z));
        }

        private void put(int mode, BlockPos p, BlockState s) {
            layers.get(mode).put(p, s);
        }

        private boolean isTree(BlockPos p) {
            return trunk.contains(p) || wood.containsKey(p);
        }

        private static double hash(BlockPos p, int salt) {
            return (Mth.getSeed(p.getX() + salt * 7919, p.getY(), p.getZ()) >>> 20 & 1023) / 1024.0;
        }

        // ---- trunk ----

        double radius(int y, double th) {
            double base = y <= fork ? 6.3 - 2.8 * Math.pow((double) y / fork, 0.8) : Math.max(1.5, 3.5 - (y - fork) * 0.6);
            double r = base * (1 + 0.05 * Math.sin(3 * th + ph1) + 0.035 * Math.sin(5 * th + ph2) + 0.025 * Math.sin(7 * th + ph3));
            double flare = 0;
            for (double a : roots) flare = Math.max(flare, Math.pow(Math.max(0, Math.cos(th - a)), 12) * Math.max(0, 7 - y) * 0.9);
            return r + flare;
        }

        static double inner(int y) {
            return y < 5 ? 4.3 : y == 5 ? 3.5 : y == 6 ? 2.6 : 1.6;
        }

        private void trunk() {
            for (int y = 0; y <= fork + 4; y++)
                for (int dx = -13; dx <= 13; dx++)
                    for (int dz = -13; dz <= 13; dz++)
                        if (Math.hypot(dx, dz) <= radius(y, Math.atan2(dz, dx))) trunk.add(new BlockPos(x0 + dx, g + y, z0 + dz));
        }

        // ---- roots ----

        /** A root leaves the buttress, arches over the ground, thins and dives into the soil; side roots branch off. */
        private void root(double az, double len, double start, boolean main) {
            BlockPos prev = null;
            double wig = rnd.nextDouble() * 6.3, amp = 0.12 + rnd.nextDouble() * 0.1;
            int sides = 0;
            for (double s = 0; s <= len; s += 0.25) {
                double f = s / len, a = az + Math.sin(wig + s * 0.35) * amp;
                double r = start + s;
                double x = x0 + 0.5 + Math.cos(a) * r, z = z0 + 0.5 + Math.sin(a) * r;
                int gr = ground(Mth.floor(x), Mth.floor(z));
                double rise = main ? 3.2 : 0.8;
                double y = gr + rise * Math.pow(1 - f, 2) - 0.9 * f;
                BlockPos p = BlockPos.containing(x, y, z);
                boolean under = p.getY() < gr;
                Direction.Axis ax = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? Direction.Axis.X : Direction.Axis.Z;
                if (main && f < 0.45) stamp(new Vec3(x, y, z), 1.9 * (1 - f / 0.45) + 0.5, ax);
                int lvl = under ? LOG : main ? (f < 0.7 ? LOG : BRANCH) : (f < 0.4 ? LOG : f < 0.75 ? BRANCH : TWIG);
                prev = step(prev, p, lvl, ax);
                if (under && f > 0.6) break;
                if (main && sides < 2 && f > 0.3 + sides * 0.28) {
                    sides++;
                    root(a + (rnd.nextBoolean() ? 1 : -1) * (0.6 + rnd.nextDouble() * 0.4), 6 + rnd.nextInt(3), r, false);
                }
            }
        }

        // ---- crown ----

        /** A main limb: 5 wide at the trunk, arching out and drooping at the end, forking into branches. */
        private void limb(double az, double elev, int ys, double len) {
            double rs = Math.max(0.5, radius(ys, az) - 2.5);
            Vec3 a = new Vec3(x0 + 0.5 + Math.cos(az) * rs, g + ys + 0.5, z0 + 0.5 + Math.sin(az) * rs);
            Vec3 b = a.add(dir(az, elev + 0.25).scale(len * 0.55));
            Vec3 c = clamp(a.add(dir(az, elev * 0.45).scale(len)), LIMB_REACH);
            BlockPos prev = null;
            int forks = 0;
            for (double s = 0; s <= 1.0001; s += 0.012) {
                Vec3 p = bez(a, b, c, s);
                Vec3 t = bez(a, b, c, Math.min(1, s + 0.012)).subtract(p);
                Direction.Axis ax = axisOf(t);
                if (s < 0.55) stamp(p, 2.4 * (1 - s / 0.55) + 0.6, ax);
                prev = step(prev, BlockPos.containing(p), s < 0.85 ? LOG : BRANCH, ax);
                if (forks < 4 && s >= 0.32 + forks * 0.2) {
                    forks++;
                    double taz = Math.atan2(t.z, t.x), tel = Math.atan2(t.y, Math.hypot(t.x, t.z));
                    for (int k = 0; k < (forks % 2 == 0 ? 2 : 1); k++) {
                        double side = (rnd.nextBoolean() ? 1 : -1) * (0.45 + rnd.nextDouble() * 0.5);
                        branch(BlockPos.containing(p), p, taz + side, tel + rnd.nextDouble() * 0.35, 10.5 - forks * 1.2 + rnd.nextDouble() * 2);
                    }
                }
            }
            for (int k = -1; k <= 1; k += 2) twig(prev, c, az + k * 0.5, 0.25, 4 + rnd.nextDouble() * 2);
            cluster(c.add(0, 1.0, 0), 3.6);
        }

        /** A branch off a limb: log, then 8 px branch, then twigs with leaf clusters. */
        private void branch(BlockPos from, Vec3 p0, double az, double elev, double len) {
            Vec3 mid = p0.add(dir(az, elev + 0.2).scale(len * 0.5));
            Vec3 end = clamp(p0.add(dir(az, elev - 0.1).scale(len)), BRANCH_REACH);
            BlockPos prev = from;
            for (double s = 0.03; s <= 1.0001; s += 0.03) {
                Vec3 p = bez(p0, mid, end, s);
                prev = step(prev, BlockPos.containing(p), s < 0.3 ? LOG : BRANCH, axisOf(end.subtract(p0)));
                if (Math.abs(s - 0.57) < 0.016) twig(prev, p, az + (rnd.nextBoolean() ? 0.8 : -0.8), elev + 0.2, 3 + rnd.nextDouble() * 2);
            }
            twig(prev, end, az + 0.4, elev, 3 + rnd.nextDouble() * 2);
            twig(prev, end, az - 0.4, elev + 0.3, 3 + rnd.nextDouble() * 2);
        }

        private void twig(BlockPos from, Vec3 p0, double az, double elev, double len) {
            Vec3 end = clamp(p0.add(dir(az, elev).scale(len)), BRANCH_REACH + 1);
            BlockPos prev = from;
            for (double s = 0.08; s <= 1.0001; s += 0.08) prev = step(prev, BlockPos.containing(p0.lerp(end, s)), TWIG, null);
            cluster(end.add(0, 0.8, 0), 2.6 + rnd.nextDouble() * 1.2);
        }

        private static Direction.Axis axisOf(Vec3 t) {
            double ax = Math.abs(t.x), ay = Math.abs(t.y), az = Math.abs(t.z);
            return ay >= ax && ay >= az ? Direction.Axis.Y : ax >= az ? Direction.Axis.X : Direction.Axis.Z;
        }

        private static Vec3 dir(double az, double elev) {
            return new Vec3(Math.cos(az) * Math.cos(elev), Math.sin(elev), Math.sin(az) * Math.cos(elev));
        }

        private static Vec3 bez(Vec3 a, Vec3 b, Vec3 c, double s) {
            return a.scale((1 - s) * (1 - s)).add(b.scale(2 * s * (1 - s))).add(c.scale(s * s));
        }

        private Vec3 clamp(Vec3 p, double max) {
            double dx = p.x - x0 - 0.5, dz = p.z - z0 - 0.5, d = Math.hypot(dx, dz);
            if (d <= max) return p;
            return new Vec3(x0 + 0.5 + dx * max / d, p.y + (d - max) * 0.3, z0 + 0.5 + dz * max / d);
        }

        private void stamp(Vec3 p, double r, Direction.Axis ax) {
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dy = -ri; dy <= ri; dy++)
                    for (int dz = -ri; dz <= ri; dz++) {
                        BlockPos q = BlockPos.containing(p.x + dx, p.y + dy, p.z + dz);
                        if (Vec3.atCenterOf(q).distanceTo(p) <= r) addWood(q, LOG, ax);
                    }
        }

        private void addWood(BlockPos p, int lvl, Direction.Axis ax) {
            Integer old = wood.get(p);
            if (old == null || lvl > old) {
                wood.put(p, lvl);
                if (ax != null) axis.put(p, ax);
            }
        }

        /** Extends a path to p one face at a time so every link is face to face. */
        private BlockPos step(BlockPos prev, BlockPos p, int lvl, Direction.Axis ax) {
            if (prev == null) {
                addWood(p, lvl, ax);
                return p;
            }
            BlockPos cur = prev;
            while (!cur.equals(p)) {
                BlockPos next = cur.getX() != p.getX() ? cur.offset(Integer.signum(p.getX() - cur.getX()), 0, 0)
                        : cur.getY() != p.getY() ? cur.offset(0, Integer.signum(p.getY() - cur.getY()), 0)
                        : cur.offset(0, 0, Integer.signum(p.getZ() - cur.getZ()));
                addWood(next, lvl, ax);
                for (Direction d : Direction.values()) {
                    if (cur.relative(d).equals(next)) {
                        links.merge(cur, 1 << d.ordinal(), (x, y) -> x | y);
                        links.merge(next, 1 << d.getOpposite().ordinal(), (x, y) -> x | y);
                    }
                }
                cur = next;
            }
            return cur;
        }

        /** A flattened, lumpy cushion of leaves. */
        private void cluster(Vec3 c, double r) {
            double ry = r * 0.55;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dy = -ri; dy <= ri; dy++)
                    for (int dz = -ri; dz <= ri; dz++) {
                        BlockPos q = BlockPos.containing(c.x + dx, c.y + dy, c.z + dz);
                        Vec3 o = Vec3.atCenterOf(q).subtract(c);
                        double v = (o.x * o.x + o.z * o.z) / (r * r) + o.y * o.y / (ry * ry);
                        if (v < 1 - 0.35 * hash(q, 1) && Math.hypot(q.getX() - x0, q.getZ() - z0) <= LEAF_REACH) leaves.add(q);
                    }
        }

        // ---- turning it all into block placements ----

        private void emit() {
            BlockState bark = AncientOak.WOOD.get().defaultBlockState();
            // trunk: log, bark where nothing continues above, rune logs spiralling up, rooted into the ground
            Set<BlockPos> runes = new HashSet<>();
            for (int spiral = 0; spiral < 2; spiral++) {
                for (int y = 1; y <= fork; y++) {
                    double a = y * 0.42 + spiral * Math.PI + ph1;
                    BlockPos p = new BlockPos(x0, g + y, z0);
                    if (hash(p, 7 + spiral) > 0.55) continue;
                    for (double r = 13; r > 0; r -= 0.5) {
                        BlockPos q = BlockPos.containing(x0 + 0.5 + Math.cos(a) * r, g + y, z0 + 0.5 + Math.sin(a) * r);
                        if (trunk.contains(q)) {
                            runes.add(q);
                            break;
                        }
                    }
                }
            }
            for (BlockPos p : trunk) {
                BlockState s = runes.contains(p) ? AncientOak.RUNE_LOG.get().defaultBlockState()
                        : trunk.contains(p.above()) || wood.containsKey(p.above()) ? log(Direction.Axis.Y) : bark;
                put(SOLID, p, s);
                if (p.getY() == g) for (int i = 1; i <= 6; i++) put(SOLID, p.below(i), log(Direction.Axis.Y));
            }
            for (Map.Entry<BlockPos, Integer> e : wood.entrySet()) {
                BlockPos p = e.getKey();
                if (trunk.contains(p)) continue;
                if (e.getValue() == LOG) {
                    boolean rune = hash(p, 3) < 0.012;
                    put(SOLID, p, rune ? AncientOak.RUNE_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis.getOrDefault(p, Direction.Axis.Y))
                            : log(axis.getOrDefault(p, Direction.Axis.Y)));
                    continue;
                }
                BlockState s = (e.getValue() == BRANCH ? AncientOak.BRANCH : AncientOak.TWIG).get().defaultBlockState();
                int mask = links.getOrDefault(p, 0);
                for (Direction d : Direction.values())
                    if ((mask >> d.ordinal() & 1) != 0 && isTree(p.relative(d))) s = s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), true);
                put(SOLID, p, s);
            }
            hollow();
            BlockState leaf = AncientOak.LEAVES.get().defaultBlockState();
            for (BlockPos p : leaves) if (!isTree(p)) put(LEAF, p, leaf);
            magic();
        }

        private BlockState log(Direction.Axis ax) {
            return AncientOak.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, ax);
        }

        /** The room: carved out, with floor, opening, chimney and the altar in the middle. */
        private void hollow() {
            for (int y = 0; y <= 20; y++)
                for (int dx = -5; dx <= 5; dx++)
                    for (int dz = -5; dz <= 5; dz++) {
                        if (Math.hypot(dx, dz) > inner(y)) continue;
                        put(AIR, new BlockPos(x0 + dx, g + y, z0 + dz), Blocks.AIR.defaultBlockState());
                        if (y == 0) put(SOLID, new BlockPos(x0 + dx, g - 1, z0 + dz), floor(new BlockPos(dx, 0, dz)));
                    }
            // the opening: 4 wide, 4 high (5 in the middle), and a path out between the buttresses
            for (int dx = -16; dx <= 16; dx++)
                for (int dz = -16; dz <= 16; dz++) {
                    double d = Math.hypot(dx, dz), diff = Math.abs(Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx) - door)));
                    if (d < 3.5 || d > radius(0, door) + 4 || diff * d > 120) continue; // ~2 blocks either side of the centre line
                    int top = diff * d < 50 && d < 8 ? 5 : 4;
                    for (int y = 0; y < top; y++) {
                        BlockPos p = new BlockPos(x0 + dx, g + y, z0 + dz);
                        put(AIR, p, Blocks.AIR.defaultBlockState());
                        layers.get(SOLID).remove(p);
                    }
                    put(SOLID, new BlockPos(x0 + dx, g - 1, z0 + dz), floor(new BlockPos(dx, 1, dz)));
                }
            put(SOLID, new BlockPos(x0, g, z0), AncientOak.ALTAR.get().defaultBlockState());
            layers.get(AIR).remove(new BlockPos(x0, g, z0));
        }

        private static BlockState floor(BlockPos p) {
            double h = hash(p, 5);
            return h < 0.25 ? Blocks.ROOTED_DIRT.defaultBlockState() : h < 0.45 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.PODZOL.defaultBlockState();
        }

        /** The magic: glow berry vines and spore blossoms under the crown, lichen and glowcaps, the mossy ring round the foot. */
        private void magic() {
            Map<BlockPos, BlockState> decor = layers.get(DECOR);
            // glow berry vines hang from the undersides of leaf clusters
            for (BlockPos p : leaves) {
                if (isTree(p) || leaves.contains(p.below()) || isTree(p.below()) || hash(p, 11) > 0.04) continue;
                int len = 2 + (int) (hash(p, 12) * 5);
                for (int k = 1; k <= len; k++) {
                    BlockPos q = p.below(k);
                    if (leaves.contains(q.below()) || isTree(q.below())) break;
                    boolean tip = k == len;
                    BlockState v = (tip ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState().setValue(BlockStateProperties.BERRIES, hash(q, 13) < 0.55);
                    if (tip) v = v.setValue(GrowingPlantHeadBlock.AGE, 25);
                    decor.put(q, v);
                }
            }
            // spore blossoms under the big limbs
            int blossoms = 0;
            for (Map.Entry<BlockPos, Integer> e : wood.entrySet()) {
                BlockPos p = e.getKey(), b = p.below();
                if (blossoms < 16 && e.getValue() == LOG && !trunk.contains(p) && p.getY() > g + 8 && !isTree(b) && !leaves.contains(b) && hash(p, 14) < 0.03) {
                    decor.put(b, Blocks.SPORE_BLOSSOM.defaultBlockState());
                    blossoms++;
                }
            }
            // glow lichen on the bark, outside low down and on the walls of the room
            for (BlockPos p : trunk) {
                int y = p.getY() - g;
                if (y > 16) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos n = p.relative(d);
                    if (trunk.contains(n) || wood.containsKey(n)) continue;
                    boolean inside = Math.hypot(n.getX() - x0, n.getZ() - z0) < 5;
                    if (hash(n, 15 + d.ordinal()) < (inside ? 0.18 : 0.035))
                        decor.put(n, Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(d.getOpposite()), true));
                }
            }
            // inside the room: glowcaps and mushrooms round the wall, cobwebs high up, moss
            for (int dx = -5; dx <= 5; dx++)
                for (int dz = -5; dz <= 5; dz++) {
                    double d = Math.hypot(dx, dz);
                    if (d < 2.5 || d > inner(0)) continue;
                    BlockPos p = new BlockPos(x0 + dx, g, z0 + dz);
                    double h = hash(p, 21);
                    if (Math.abs(Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx) - door))) < 30) continue;
                    if (h < 0.22) decor.put(p, AncientOak.GLOWCAP.get().defaultBlockState());
                    else if (h < 0.3) decor.put(p, Blocks.BROWN_MUSHROOM.defaultBlockState());
                    else if (h < 0.34) decor.put(p, Blocks.RED_MUSHROOM.defaultBlockState());
                    else if (h < 0.5) decor.put(p, Blocks.MOSS_CARPET.defaultBlockState());
                    if (hash(p, 22) < 0.12) decor.put(p.above(4), Blocks.COBWEB.defaultBlockState());
                }
            decor.put(new BlockPos(x0, g + 9, z0), Blocks.COBWEB.defaultBlockState());
            // the ring round the foot: moss, podzol and rooted dirt, with moss carpet, azaleas, firefly bushes and glowcaps
            for (int dx = -17; dx <= 17; dx++)
                for (int dz = -17; dz <= 17; dz++) {
                    double d = Math.hypot(dx, dz);
                    int x = x0 + dx, z = z0 + dz;
                    BlockPos top = new BlockPos(x, ground(x, z), z);
                    if (d > 13 + hash(top, 30) * 4) continue;
                    double h = hash(top, 31);
                    BlockState soil = h < 0.4 ? Blocks.MOSS_BLOCK.defaultBlockState() : h < 0.8 ? Blocks.PODZOL.defaultBlockState()
                            : h < 0.9 ? Blocks.ROOTED_DIRT.defaultBlockState() : null;
                    if (soil != null && !isTree(top.below())) put(GROUND, top.below(), soil);
                    if (isTree(top) || decor.containsKey(top) || layers.get(AIR).containsKey(top) && d < 5) continue;
                    double k = hash(top, 32);
                    BlockState plant = k < 0.14 ? Blocks.MOSS_CARPET.defaultBlockState() : k < 0.17 ? Blocks.FLOWERING_AZALEA.defaultBlockState()
                            : k < 0.19 ? Blocks.AZALEA.defaultBlockState() : k < 0.215 ? Blocks.FIREFLY_BUSH.defaultBlockState()
                            : k < 0.25 && d < 11 ? AncientOak.GLOWCAP.get().defaultBlockState() : null;
                    if (plant != null) decor.put(top, plant);
                }
        }
    }
}
