package com.mechanical.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The Elkwight's ruin (docs/elkwight-plan.md phase 2): the burnt hall of the elk-crowned, a round arena of Blackstone in
 * the Scorched Barrens, never closer than {@link ScorchedBarrens#MIN_DISTANCE} to 0,0. Floor radius 17, walls to 20 with a
 * broken crest that rises and falls all round (fallen to rubble banks in places, high and crenellated in others),
 * buttresses and a stepped plinth outside, a main gate and two side gates 7 wide under arches 10 high (the antler crown is
 * ~4 wide) between towers with soul fires on top. Opposite the main gate the wall rises into the Antler Throne: a gable 25
 * high crowned by two palmate antlers of bone, about 30 blocks tip to tip, over a raised stone throne. Inside: tiered stands,
 * eight pillars, two great arches across the arena (one fallen in, one with a soul lantern on a chain over the dais), a
 * broken roof 15 up over the stands with charred beams, a processional way with rune inlays from the main gate to the
 * rune-ringed dais where the boss waits ({@link ElkwightRuins#spawner}), soul fires, three chests on the stands. Outside an
 * avenue of rune stones leads to the main gate. One piece; every block is a pure function of the seed and the centre, so
 * each chunk draws its own slice. Placement: scripts/gen_elkwight.py.
 */
public final class ElkwightRuinStructure extends Structure {
    public static final MapCodec<ElkwightRuinStructure> CODEC = simpleCodec(ElkwightRuinStructure::new);
    public static final StructureType<ElkwightRuinStructure> TYPE = () -> CODEC;
    public static final StructurePieceType.ContextlessType PIECE = Piece::new;
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_ruin"));
    static final int FLOOR = 17, WALL = 20, OUTER = 25, UP = 40, DOWN = 14, GATE_HEIGHT = 10, ROOF = 15;
    /** How far from the centre anything is built: the walls, buttresses, the antler crown and the avenue before the gate. */
    static final int REACH = 36;
    /** Blocks of Scorched Barrens round the centre: the arena and its buttresses, and burnt forest past them. */
    static final int CLEAR = OUTER + 4;

    public ElkwightRuinStructure(StructureSettings settings) {
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
            Identifier id = KEY.identifier();
            event.register(Registries.STRUCTURE_TYPE, id, () -> TYPE);
            event.register(Registries.STRUCTURE_PIECE, id, () -> PIECE);
        }
    }

    /** The placement rule on top of the biome: never within MIN_DISTANCE of 0,0, counting the whole arena. */
    public static boolean allowedAt(int x, int z) {
        long r = ScorchedBarrens.MIN_DISTANCE + OUTER;
        return (long) x * x + (long) z * z >= r * r;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        if (!allowedAt(x, z) || !context.couldValidBiomeExistOnTopOfChunkCenter()) return Optional.empty();
        // well inside the barrens: burnt forest all round the arena, never at an edge
        if (context.biomeSource() instanceof MultiNoiseBiomeSource source && !ScorchedBarrens.deepInside(source, context.climateSampler(), x, z, CLEAR))
            return Optional.empty();
        ChunkGenerator gen = context.chunkGenerator();
        int g = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        // dry and fairly level under the walls and stands
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8, r = i % 2 == 0 ? WALL : 11;
            int px = x + (int) Math.round(Math.cos(a) * r), pz = z + (int) Math.round(Math.sin(a) * r);
            int floor = gen.getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            int surface = gen.getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            if (floor != surface || Math.abs(floor - g) > 6) return Optional.empty();
        }
        long seed = context.random().nextLong();
        BlockPos centre = new BlockPos(x, g, z);
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new Piece(seed, centre))));
    }

    public static final class Piece extends StructurePiece {
        private final long seed;
        private final BlockPos centre;

        public Piece(long seed, BlockPos centre) {
            super(PIECE, 0, new BoundingBox(centre.getX() - REACH, centre.getY() - DOWN, centre.getZ() - REACH,
                    centre.getX() + REACH, centre.getY() + UP, centre.getZ() + REACH));
            this.seed = seed;
            this.centre = centre;
        }

        Piece(CompoundTag tag) {
            super(PIECE, tag);
            seed = tag.getLongOr("seed", 0);
            centre = BlockPos.of(tag.getLongOr("centre", 0));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putLong("seed", seed);
            tag.putLong("centre", centre.asLong());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                BoundingBox box, ChunkPos chunkPos, BlockPos referencePos) {
            build(level, box, seed, centre, random);
        }
    }

    // ------------------------------------------------------------------ the arena

    /** Draws the part of the ruin inside {@code box} (a chunk's slice in worldgen); spawns the boss if the box holds the centre. */
    public static void build(WorldGenLevel level, BoundingBox box, long seed, BlockPos centre, RandomSource random) {
        Arena arena = new Arena(seed);
        int g = centre.getY();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), centre.getX() - REACH); x <= Math.min(box.maxX(), centre.getX() + REACH); x++) {
            for (int z = Math.max(box.minZ(), centre.getZ() - REACH); z <= Math.min(box.maxZ(), centre.getZ() + REACH); z++) {
                int dx = x - centre.getX(), dz = z - centre.getZ();
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r < WALL + 0.5) {
                    for (int h = -1; h < UP; h++) {
                        int y = g + h;
                        if (y < box.minY() || y > box.maxY()) continue;
                        BlockState s = arena.at(dx, h, dz, r);
                        p.set(x, y, z);
                        if (s != null) level.setBlock(p, s, Block.UPDATE_CLIENTS);
                        else if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        if (s != null && s.is(Blocks.CHEST)) RandomizableContainer.setBlockEntityLootTable(level, random, p.immutable(),
                                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/elkwight_ruin")));
                    }
                    foundation(level, box, arena, p, x, z, g - 2, g, dx, dz);
                } else if (r <= REACH) {
                    // outside the walls: buttresses, gate towers, the plinth, fallen rubble and the avenue, on the ground as it lies
                    int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, gh = ground - g;
                    boolean structural = arena.structural(dx, dz, r), any = false;
                    for (int h = Math.min(gh + 1, -1); h < UP; h++) {
                        int y = g + h;
                        if (y < box.minY() || y > box.maxY()) continue;
                        BlockState s = arena.outside(dx, h, dz, r, gh);
                        if (s == null) continue;
                        level.setBlock(p.set(x, y, z), s, Block.UPDATE_CLIENTS);
                        any = true;
                    }
                    if (any && structural) foundation(level, box, arena, p, x, z, Math.min(ground, g - 2), g, dx, dz);
                }
            }
        }
        // the shapes off the column grid: the antler crown, the great arches, chains and beams
        for (var e : arena.extra.entrySet()) {
            long k = e.getKey();
            BlockPos at = centre.offset(unpackX(k), unpackY(k), unpackZ(k));
            if (box.isInside(at)) level.setBlock(at, e.getValue(), Block.UPDATE_CLIENTS);
        }
        BlockPos boss = centre.above();
        if (box.isInside(boss)) ElkwightRuins.spawner.accept(level, boss);
    }

    /** Nothing floats over a dip: blackstone down from {@code from} to the first solid ground. */
    private static void foundation(WorldGenLevel level, BoundingBox box, Arena arena, BlockPos.MutableBlockPos p, int x, int z, int from, int g, int dx, int dz) {
        for (int y = from; y >= g - DOWN && y >= box.minY(); y--) {
            if (y > box.maxY()) continue;
            BlockState old = level.getBlockState(p.set(x, y, z));
            if (old.isSolid() && old.getFluidState().isEmpty()) break;
            level.setBlock(p, (arena.hash(dx, y, dz) < 0.3 ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    static long pack(int dx, int h, int dz) {
        return (long) (dx + 512) << 22 | (long) (h + 512) << 11 | (dz + 512);
    }

    private static int unpackX(long k) {
        return (int) (k >> 22) - 512;
    }

    private static int unpackY(long k) {
        return (int) (k >> 11 & 0x7FF) - 512;
    }

    private static int unpackZ(long k) {
        return (int) (k & 0x7FF) - 512;
    }

    /**
     * Every block of one ruin as a function of position (dx, dz from the centre, h above the floor). The main gate faces
     * {@code rot}; the side gates are a quarter turn either way; opposite the main gate the wall rises into the Antler Throne,
     * a gable crowned by two palmate antlers of bone that stand over the burnt forest.
     */
    static final class Arena {
        private final long seed;
        private final double rot, p1, p2, p3;
        private final int[] pillarX = new int[8], pillarZ = new int[8], pillarTop = new int[8];
        /** Blocks off the column grid: antlers, arches, chains, beams. Keys from {@link #pack}. */
        final Map<Long, BlockState> extra = new HashMap<>();

        Arena(long seed) {
            this.seed = seed;
            RandomSource rnd = RandomSource.create(seed);
            rot = rnd.nextDouble() * Math.PI * 2;
            p1 = rnd.nextDouble() * Math.PI * 2;
            p2 = rnd.nextDouble() * Math.PI * 2;
            p3 = rnd.nextDouble() * Math.PI * 2;
            for (int i = 0; i < 8; i++) {
                double a = rot + Math.PI / 8 + i * Math.PI / 4;
                pillarX[i] = (int) Math.round(Math.cos(a) * 9.5);
                pillarZ[i] = (int) Math.round(Math.sin(a) * 9.5);
                // the four that carry the arches stand whole; of the rest some are broken off
                pillarTop[i] = i % 2 == 0 || rnd.nextInt(10) >= 4 ? ROOF + 1 : 4 + rnd.nextInt(6);
            }
            crown();
            gateAntlers();
            arch(0, false);
            arch(2, true);
            chains();
            beams();
        }

        double hash(int x, int y, int z) {
            long k = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ y * 0x165667B19E3779F9L;
            k = (k ^ k >>> 31) * 0xBF58476D1CE4E5B9L;
            k = (k ^ k >>> 27) * 0x94D049BB133111EBL;
            return ((k ^ k >>> 31) >>> 11) * 0x1.0p-53;
        }

        /** Angle round the centre, 0 at the main gate. */
        private double angle(double dx, double dz) {
            double a = Math.atan2(dz, dx) - rot;
            return a - Math.floor(a / (2 * Math.PI)) * 2 * Math.PI;
        }

        /** Distance in blocks along the wall from angle a to angle t. */
        private static double arc(double a, double t) {
            double d = Math.abs(a - t) % (2 * Math.PI);
            return Math.min(d, 2 * Math.PI - d) * (WALL - 1.5);
        }

        private static final double[] GATES = {0, Math.PI / 2, Math.PI * 3 / 2};

        private static double fromGate(double a) {
            double best = Double.MAX_VALUE;
            for (double t : GATES) best = Math.min(best, arc(a, t));
            return best;
        }

        private static boolean mainGate(double a) {
            return arc(a, 0) < arc(a, Math.PI / 2) && arc(a, 0) < arc(a, Math.PI * 3 / 2);
        }

        private static double fromThrone(double a) {
            return arc(a, Math.PI);
        }

        private static boolean tower(double a) {
            double fg = fromGate(a);
            return fg >= 3.5 && fg < 7;
        }

        /** The wall's height at an angle, before the per-column jitter: a broken crest that rises and falls all round. */
        int wallTop(double a) {
            double ft = fromThrone(a);
            if (ft < 6) return 25 - (int) Math.round(ft * 1.2); // the throne gable, stepped down to the wall
            if (tower(a)) return mainGate(a) ? 22 : 18;
            double prof = 12 + 5 * Math.sin(2 * a + p1) + 4 * Math.sin(3 * a + p2) + 3 * Math.sin(7 * a + p3);
            if (ft < 11) prof = Math.max(prof, 16); // the wall stands whole beside the throne
            if (prof < 6.5) return 2 + (int) (hash((int) (a * 40), 9, 9) * 3); // fallen to a rubble bank
            return Math.min(21, (int) prof);
        }

        private int wallTop(int dx, int dz) {
            double a = angle(dx, dz);
            int top = wallTop(a);
            if (fromThrone(a) < 6 || tower(a)) return top;
            double v = hash(dx, 3, dz);
            return top + (v < 0.2 ? -1 : v > 0.88 ? 1 : 0) - (top > 8 && hash(dx, 4, dz) < 0.08 ? 3 : 0); // ragged, a few bites out
        }

        private static int tier(double r, double a) {
            if (r < 12.5 || r >= FLOOR || fromGate(a) < 4.5) return 0;
            return Math.min(4, (int) ((r - 12.5) / 1.1) + 1);
        }

        private static Direction outward(double dx, double dz) {
            return Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
        }

        private static BlockState b(Block block) {
            return block.defaultBlockState();
        }

        private static BlockState slab(Block block) {
            return block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }

        private BlockState brick(int dx, int h, int dz) {
            double v = hash(dx, h, dz);
            return v < 0.5 ? b(Blocks.POLISHED_BLACKSTONE_BRICKS) : v < 0.74 ? b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)
                    : v < 0.86 ? b(Blocks.BLACKSTONE) : v < 0.9 ? b(Blocks.DEEPSLATE_TILES) : v < 0.93 ? b(Blocks.GILDED_BLACKSTONE)
                    : v < 0.993 ? b(Blocks.POLISHED_BLACKSTONE) : b(Blocks.MAGMA_BLOCK); // a rare ember still glowing in the stone
        }

        /** The block at (dx, h, dz) inside the walls, r = horizontal distance; null = air. */
        BlockState at(int dx, int h, int dz, double r) {
            BlockState x = extra.get(pack(dx, h, dz));
            if (x != null) return x;
            double a = angle(dx, dz);
            if (h == -1) return floor(dx, dz, r);
            if (r >= FLOOR) return wall(dx, h, dz, r, a);
            // the roof over the stands where the wall stands high; a ragged inner edge and holes where it fell in
            if (h == ROOF && wallTop(a) >= ROOF + 1 && fromGate(a) >= 4.5 && r >= 10.5 + hash(dx, 88, dz) * 2.5 && hash(dx, ROOF, dz) > 0.22)
                return hash(dx, 77, dz) < 0.5 ? b(Blocks.POLISHED_BLACKSTONE_BRICKS) : b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            // pillars: basalt corners, rune faces, a chiselled cap
            for (int i = 0; i < 8; i++) {
                int ox = dx - pillarX[i], oz = dz - pillarZ[i];
                if (Math.abs(ox) > 1 || Math.abs(oz) > 1) continue;
                if (h >= pillarTop[i]) return null;
                if (h == pillarTop[i] - 1) return b(Blocks.CHISELED_POLISHED_BLACKSTONE);
                if (ox != 0 && oz != 0) return b(Blocks.BASALT);
                if (ox == 0 && oz == 0) return b(Blocks.BLACKSTONE);
                return h == 3 || h == 4 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : brick(dx, h, dz);
            }
            BlockState throne = throne(dx, h, dz, r);
            if (throne != null) return throne.isAir() ? null : throne;
            // tiered stands with steps facing the middle, three chests on top
            int tier = tier(r, a);
            if (tier > 0) {
                if (h >= tier) return chest(dx, h, dz, tier) ? b(Blocks.CHEST).setValue(ChestBlock.FACING, outward(dx, dz).getOpposite()) : null;
                if (h < tier - 1) return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
                if (hash(dx, 31, dz) < 0.07) return null; // a missing step
                Direction out = outward(dx, dz);
                int nx = dx - out.getStepX(), nz = dz - out.getStepZ();
                int inner = tier(Math.hypot(nx, nz), angle(nx, nz));
                return inner < tier ? b(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS).setValue(StairBlock.FACING, out) : b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            // the dais: rune ring round the boss's place
            if (r < 4.6) {
                if (h != 0) return null;
                if (r >= 3.4) return (int) (a * 8 / Math.PI) % 2 == 0 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : b(Blocks.CHISELED_POLISHED_BLACKSTONE);
                return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            // soul campfires on the four sides of the dais, fire bowls between the pillars
            if (h == 0 && (dx == 0 || dz == 0) && Math.abs(dx + dz) == 6)
                return b(Blocks.SOUL_CAMPFIRE).setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.FACING, outward(dx, dz));
            for (int i = 0; i < 4; i++) {
                double ba = rot + Math.PI / 4 + i * Math.PI / 2;
                int bx = (int) Math.round(Math.cos(ba) * 7), bz = (int) Math.round(Math.sin(ba) * 7);
                int ox = dx - bx, oz = dz - bz;
                if (ox == 0 && oz == 0) return h == 0 ? b(Blocks.POLISHED_BLACKSTONE_BRICKS) : h == 1 ? b(Blocks.SOUL_SOIL) : h == 2 ? b(Blocks.SOUL_FIRE) : null;
                if (h == 1 && Math.abs(ox) + Math.abs(oz) == 1) return slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
            }
            // rubble from the fallen roof and pillars, cobwebs high under the roof
            if (h == 0 && hash(dx, 11, dz) < 0.035) return hash(dx, 12, dz) < 0.5 ? b(Blocks.BLACKSTONE) : slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
            if (h == ROOF - 1 && r > 15 && wallTop(a) >= ROOF + 1 && hash(dx, 61, dz) < 0.12) return b(Blocks.COBWEB);
            return null;
        }

        /** The wall ring (FLOOR to WALL): a broken crest, gate towers, arched gates, the throne gable. */
        private BlockState wall(int dx, int h, int dz, double r, double a) {
            int top = wallTop(dx, dz);
            double fg = fromGate(a);
            boolean outer = r >= WALL - 0.8, inner = r < FLOOR + 0.9;
            if (fg < 3.5) {
                double arch = GATE_HEIGHT - 2.2 * (fg / 3.5) * (fg / 3.5);
                if (h < arch) return null;
                if (h == GATE_HEIGHT && fg < 0.7 && outer) return b(Blocks.CHISELED_POLISHED_BLACKSTONE); // keystone
            }
            if (h > top) return null;
            if (h == top) { // the crest: a soul fire on each tower, merlons along the high wall's outer edge
                if (tower(a) && Math.abs(fg - 5.25) < 0.6 && Math.abs(r - (FLOOR + WALL) / 2.0) < 0.75)
                    return b(Blocks.SOUL_CAMPFIRE).setValue(CampfireBlock.LIT, true);
                if (top >= 12 && outer && fromThrone(a) >= 6 && Math.floorMod((int) Math.floor(a * WALL), 2) == 0)
                    return b(Blocks.POLISHED_BLACKSTONE_WALL);
                return null;
            }
            if (h == 0) return b(Blocks.BASALT);
            if (fromThrone(a) < 6 && inner && h >= 17 && h <= 22) // the throne's glowing face
                return Math.floorMod(dx + dz + h, 3) == 0 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : b(Blocks.CHISELED_POLISHED_BLACKSTONE);
            if (inner && h >= 2 && h <= 7 && hash(dx, h, dz) < 0.09) return b(ScorchedBarrens.RUNE_BLACKSTONE.get());
            if (inner && h == 5) return b(Blocks.CHISELED_POLISHED_BLACKSTONE);
            if (outer && h == 7) return b(Blocks.POLISHED_BLACKSTONE); // string courses
            if (outer && h == 14) return hash(dx, 14, dz) < 0.5 ? b(Blocks.CHISELED_POLISHED_BLACKSTONE) : b(Blocks.POLISHED_BLACKSTONE);
            return brick(dx, h, dz);
        }

        /** The raised throne before the gable, facing the main gate: a two-step platform, the seat, armrests and a tall back. */
        private BlockState throne(int dx, int h, int dz, double r) {
            double ta = rot + Math.PI, nx = Math.cos(ta), nz = Math.sin(ta);
            double radial = dx * nx + dz * nz, tang = -dx * nz + dz * nx;
            if (Math.abs(tang) > 3.5 || radial < 12.5 || r >= FLOOR) return null;
            Direction toWall = outward(nx, nz);
            BlockState air = b(Blocks.AIR);
            if (radial < 13.5) return h == 0 ? b(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS).setValue(StairBlock.FACING, toWall) : air;
            if (radial < 14.5) return h == 0 ? b(Blocks.POLISHED_BLACKSTONE_BRICKS) : h == 1 ? b(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS).setValue(StairBlock.FACING, toWall) : air;
            if (h < 2) return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            if (radial < 16 && Math.abs(tang) < 0.5) return h == 2 ? b(Blocks.POLISHED_BLACKSTONE_STAIRS).setValue(StairBlock.FACING, toWall) : air;
            if (radial < 16 && Math.abs(tang) < 1.5) return h == 2 ? slab(Blocks.POLISHED_BLACKSTONE_SLAB) : air;
            if (radial >= 16 && Math.abs(tang) < 1.5) // the back
                return h > 6 ? air : h == 6 ? b(Blocks.CHISELED_POLISHED_BLACKSTONE) : h == 5 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : b(Blocks.POLISHED_BLACKSTONE);
            return air;
        }

        private boolean chest(int dx, int h, int dz, int tier) {
            if (h != tier || tier != 3) return false;
            for (int i = 0; i < 3; i++) {
                double a = rot + Math.PI / 3 + i * Math.PI / 2; // 60, 150, 240 degrees from the main gate
                int cx = (int) Math.round(Math.cos(a) * 15), cz = (int) Math.round(Math.sin(a) * 15);
                if (cx == dx && cz == dz) return true;
            }
            return false;
        }

        private BlockState floor(int dx, int dz, double r) {
            // the processional way from the main gate to the dais, rune inlays down its middle
            double along = dx * Math.cos(rot) + dz * Math.sin(rot), across = -dx * Math.sin(rot) + dz * Math.cos(rot);
            if (along > 5.5 && Math.abs(across) < 1.6)
                return Math.abs(across) < 0.5 && Math.floorMod((int) Math.round(along), 4) == 0 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : b(Blocks.POLISHED_BLACKSTONE);
            if (r >= 5 && r < 6) return b(Blocks.CHISELED_POLISHED_BLACKSTONE);
            double v = hash(dx, -1, dz);
            if (r > 8 && r < FLOOR && v < 0.05) return b(Blocks.MAGMA_BLOCK);
            if (v < 0.25) return b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            if (v < 0.33) return b(Blocks.POLISHED_BASALT).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            if (v < 0.38) return b(Blocks.BASALT);
            return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
        }

        // ---------------------------------------------------------------- outside the walls

        /** Buttresses every 30 degrees, skipping the gates and the throne. */
        private static final double[] BUTTRESSES = {Math.PI / 6, Math.PI / 3, Math.PI * 2 / 3, Math.PI * 5 / 6, Math.PI * 7 / 6, Math.PI * 4 / 3,
                Math.PI * 5 / 3, Math.PI * 11 / 6};

        /** Height above the floor of a buttress or gate tower at this outside column, 0 if none. */
        private int structureTop(int dx, int dz, double r) {
            double a = angle(dx, dz);
            if (tower(a) && r < WALL + 2.5) return wallTop(a); // the towers stand out from the wall
            if (r >= WALL + 4.5) return 0;
            for (double t : BUTTRESSES) {
                double d = Math.abs(a - t) % (2 * Math.PI);
                if (Math.min(d, 2 * Math.PI - d) * r > 1.05) continue;
                return Math.max(2, wallTop(t) - 1 - (int) ((r - WALL) * 2.6));
            }
            return 0;
        }

        boolean structural(int dx, int dz, double r) {
            return structureTop(dx, dz, r) > 0 || plinth(dx, dz, r);
        }

        private boolean plinth(int dx, int dz, double r) {
            return r < WALL + 1.5 && fromGate(angle(dx, dz)) >= 3.5;
        }

        /** The block at an outside column, h above the arena floor, gh = the ground's height there; null leaves the world alone. */
        BlockState outside(int dx, int h, int dz, double r, int gh) {
            double a = angle(dx, dz);
            int top = structureTop(dx, dz, r);
            if (top > 0) {
                if (h >= top) return null;
                if (h <= gh && h >= 0) return h == gh && gh < top ? brick(dx, h, dz) : null;
                boolean tw = tower(a) && r < WALL + 2.5;
                if (!tw && h == top - 1) return slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
                if (tw && h == top - 1) return Math.floorMod((int) Math.floor(a * (WALL + 1)), 2) == 0 ? b(Blocks.POLISHED_BLACKSTONE_WALL) : null;
                if (h == 7 || h == 14) return b(Blocks.POLISHED_BLACKSTONE);
                return h < 1 ? b(Blocks.BASALT) : brick(dx, h, dz);
            }
            if (plinth(dx, dz, r)) {
                if (h > 0) return null;
                if (h == 0) return gh < 0 ? b(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS).setValue(StairBlock.FACING, outward(dx, dz).getOpposite()) : null;
                return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            if (h <= gh) return null;
            // rubble where the wall came down, thinning out away from it
            double fr = r - WALL;
            if (wallTop(a) <= 5 && fr < 7) {
                double pr = 0.42 * (1 - fr / 7), v = hash(dx, 41, dz);
                if (v < pr && (h == gh + 1 || h == gh + 2 && v < pr * 0.3))
                    return switch ((int) (hash(dx, 42 + h, dz) * 4)) {
                        case 0 -> b(Blocks.BLACKSTONE);
                        case 1 -> b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
                        case 2 -> b(Blocks.BASALT);
                        default -> slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
                    };
            }
            return avenue(dx, h - gh - 1, dz);
        }

        /**
         * The avenue before the main gate: three pairs of rune stones, 2 wide and 1 thick, 5 to 7 high, either side of the way.
         * Some lean outward at the top; one may lie where it fell. hs = height above the local ground.
         */
        private BlockState avenue(int dx, int hs, int dz) {
            if (hs < 0) return null;
            double along = dx * Math.cos(rot) + dz * Math.sin(rot), across = -dx * Math.sin(rot) + dz * Math.cos(rot);
            for (int j = 0; j < 3; j++) {
                double at = 25 + 4.5 * j;
                for (int side = -1; side <= 1; side += 2) {
                    double v = hash(j, 501, side);
                    int height = 5 + (int) (v * 3);
                    double off = across * side - 5.5; // 0..2 = the stone's width, outward of the way
                    if (j == 2 && side == 1 && v < 0.5) { // fallen: lying flat away from the way
                        if (hs == 0 && Math.abs(along - at) < 1.0 && off >= 0 && off < height) return off < 1 ? b(Blocks.POLISHED_BLACKSTONE) : b(Blocks.BLACKSTONE);
                        continue;
                    }
                    if (hash(j, 502, side) < 0.45 && hs >= height - 2) off -= 1; // leaning outward
                    if (Math.abs(along - at) >= 0.5 || off < 0 || off >= 2 || hs >= height) continue;
                    if (hs < 2) return b(Blocks.POLISHED_BLACKSTONE);
                    if (hs == height - 1) return b(Blocks.CHISELED_POLISHED_BLACKSTONE);
                    return hs == 2 || hs == 3 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : b(Blocks.BLACKSTONE);
                }
            }
            return null;
        }

        // ---------------------------------------------------------------- shapes off the grid

        private void put(double dx, double h, double dz, BlockState s) {
            extra.put(pack((int) Math.floor(dx), (int) Math.floor(h), (int) Math.floor(dz)), s);
        }

        /** A round tube of blocks from one point to another (dx, h, dz), radius r0 tapering to r1. */
        private void tube(double[] p0, double[] p1, double r0, double r1, Function<Direction.Axis, BlockState> block) {
            double ex = p1[0] - p0[0], ey = p1[1] - p0[1], ez = p1[2] - p0[2];
            double len = Math.sqrt(ex * ex + ey * ey + ez * ez);
            Direction.Axis axis = Math.abs(ey) >= Math.abs(ex) && Math.abs(ey) >= Math.abs(ez) ? Direction.Axis.Y
                    : Math.abs(ex) >= Math.abs(ez) ? Direction.Axis.X : Direction.Axis.Z;
            BlockState s = block.apply(axis);
            for (double t = 0; t <= len; t += 0.2) {
                double f = len == 0 ? 0 : t / len, rr = r0 + (r1 - r0) * f;
                double cx = p0[0] + ex * f, cy = p0[1] + ey * f, cz = p0[2] + ez * f;
                for (int x = (int) Math.floor(cx - rr); x <= (int) Math.floor(cx + rr); x++)
                    for (int y = (int) Math.floor(cy - rr); y <= (int) Math.floor(cy + rr); y++)
                        for (int z = (int) Math.floor(cz - rr); z <= (int) Math.floor(cz + rr); z++) {
                            double qx = x + 0.5 - cx, qy = y + 0.5 - cy, qz = z + 0.5 - cz;
                            if (qx * qx + qy * qy + qz * qz <= rr * rr + 0.2) extra.put(pack(x, y, z), s);
                        }
            }
        }

        private static BlockState bone(Direction.Axis axis) {
            return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        }

        /** A point given along the wall (tangent), out from the wall's middle line and height, at the wall's angle t. */
        private double[] wallPoint(double t, double along, double out, double h) {
            double a = rot + t, nx = Math.cos(a), nz = Math.sin(a), r0 = WALL - 1.5 + out;
            return new double[] {nx * r0 - nz * along + 0.5, h, nz * r0 + nx * along + 0.5};
        }

        /**
         * The Antler Throne's crown: two palmate antlers of bone springing from the gable top like a moose's, each a short thick
         * beam opening into a broad cupped palm with seven points splayed round its rim and a forked brow tine reaching over the
         * arena; about 34 blocks tip to tip, they stand over the burnt forest and show from afar.
         */
        private void crown() {
            int base = wallTop(Math.PI);
            for (int s = -1; s <= 1; s += 2) {
                // the pedicle and beam: thick bone from the gable's crest out to where the palm opens
                double[] root = wallPoint(Math.PI, s * 4.0, -0.3, base + 2.5);
                tube(wallPoint(Math.PI, s * 0.8, 0, base - 1), wallPoint(Math.PI, s * 2.2, 0, base + 1.2), 1.35, 1.2, Arena::bone);
                tube(wallPoint(Math.PI, s * 2.2, 0, base + 1.2), root, 1.2, 1.05, Arena::bone);
                // the palm: a broad cupped fan opening up and out from the root, thinner toward its rim
                double reach = 9.5;
                for (double th = 0.08; th <= 1.45; th += 0.03) {
                    double rim = reach - 1.2 * Math.abs(th - 0.75) - 0.8 * Math.sin(th * 7.3 + s);
                    for (double rr = 0; rr <= rim; rr += 0.35) {
                        double along = 4.0 + Math.cos(th) * rr, h = base + 2.5 + Math.sin(th) * rr;
                        double cup = -0.3 - 0.022 * rr * rr; // the palm bends toward the arena like a cupped hand
                        double[] q = wallPoint(Math.PI, s * along, cup, h);
                        put(q[0], q[1], q[2], bone(Direction.Axis.Y));
                        if (rr < rim - 2.5) { // thicker toward the root
                            double[] q2 = wallPoint(Math.PI, s * along, cup + 0.9, h);
                            put(q2[0], q2[1], q2[2], bone(Direction.Axis.Y));
                        }
                    }
                }
                // points round the rim, splayed outward along the fan, longest at the top outer corner
                double[] angles = {0.12, 0.33, 0.55, 0.78, 1.0, 1.2, 1.42};
                double[] lengths = {2.4, 3.0, 3.6, 4.2, 3.8, 3.0, 2.2};
                for (int i = 0; i < angles.length; i++) {
                    double th = angles[i], rim = reach - 1.2 * Math.abs(th - 0.75) - 0.8 * Math.sin(th * 7.3 + s) - 0.4;
                    double len = lengths[i] * (0.85 + 0.3 * hash(i, 610, s)), bend = th + 0.18; // each curls a little upward
                    double r0 = rim, r1 = rim + len;
                    double cup0 = -0.3 - 0.022 * r0 * r0, cup1 = -0.3 - 0.022 * r0 * r0 - 0.4;
                    tube(wallPoint(Math.PI, s * (4.0 + Math.cos(th) * r0), cup0, base + 2.5 + Math.sin(th) * r0),
                            wallPoint(Math.PI, s * (4.0 + Math.cos(bend) * r1), cup1, base + 2.5 + Math.sin(bend) * r1), 0.62, 0.32, Arena::bone);
                }
                // the brow tine: a forked point reaching forward over the arena from the beam
                double[] brow = wallPoint(Math.PI, s * 3.2, -3.4, base + 3.2);
                tube(wallPoint(Math.PI, s * 2.4, -0.4, base + 1.4), brow, 0.75, 0.5, Arena::bone);
                tube(brow, wallPoint(Math.PI, s * 4.4, -5.0, base + 4.6), 0.5, 0.3, Arena::bone);
                tube(brow, wallPoint(Math.PI, s * 2.6, -4.8, base + 5.4), 0.45, 0.3, Arena::bone);
            }
            // where the antlers spring from the gable: a carved boss with a glowing rune
            for (int s = -2; s <= 2; s++) {
                double[] q = wallPoint(Math.PI, s, -0.6, base);
                put(q[0], q[1], q[2], s == 0 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : b(Blocks.CHISELED_POLISHED_BLACKSTONE));
            }
        }

        /** Small antlers of bone above the main gate's keystone: the sign of whose hall this was. */
        private void gateAntlers() {
            for (int s = -1; s <= 1; s += 2) {
                double[] a = wallPoint(0, s * 1.0, 1.4, GATE_HEIGHT + 1), b1 = wallPoint(0, s * 3.0, 1.6, GATE_HEIGHT + 3),
                        c = wallPoint(0, s * 4.5, 1.6, GATE_HEIGHT + 6), t = wallPoint(0, s * 2.6, 1.8, GATE_HEIGHT + 6);
                tube(a, b1, 0.48, 0.42, Arena::bone);
                tube(b1, c, 0.42, 0.3, Arena::bone);
                tube(b1, t, 0.38, 0.3, Arena::bone);
            }
        }

        /** A great arch from pillar i to the pillar across the arena; a broken one has lost its middle. */
        private void arch(int i, boolean broken) {
            double x0 = pillarX[i] + 0.5, z0 = pillarZ[i] + 0.5, x1 = pillarX[i + 4] + 0.5, z1 = pillarZ[i + 4] + 0.5;
            double spring = ROOF - 0.5, rise = 8;
            double[] prev = null;
            for (int k = 0; k <= 24; k++) {
                double th = Math.PI * k / 24, f = (1 - Math.cos(th)) / 2;
                double[] q = {x0 + (x1 - x0) * f, spring + rise * Math.sin(th), z0 + (z1 - z0) * f};
                boolean gap = broken && th > 0.36 * Math.PI && th < 0.66 * Math.PI;
                if (prev != null && !gap) {
                    int key = k;
                    tube(prev, q, 0.95, 0.95, axis -> hash(key, 71, i) < 0.65 ? b(Blocks.POLISHED_BLACKSTONE_BRICKS) : b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS));
                }
                prev = q;
            }
            if (broken) { // what fell lies on the floor under the gap
                double mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
                for (int k = 0; k < 9; k++) {
                    int px = (int) Math.floor(mx + (hash(k, 72, i) - 0.5) * 8), pz = (int) Math.floor(mz + (hash(k, 73, i) - 0.5) * 8);
                    if (px * px + pz * pz < 25) continue; // keep the dais clear
                    extra.put(pack(px, 0, pz), k % 3 == 0 ? slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB) : b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS));
                }
            } else { // a chain from the crown of the arch, a soul lantern high over the dais
                int cx = (int) Math.floor((x0 + x1) / 2), cz = (int) Math.floor((z0 + z1) / 2);
                int topH = (int) Math.floor(spring + rise) - 1;
                for (int h = topH; h > 12; h--) extra.put(pack(cx, h, cz), b(Blocks.IRON_CHAIN));
                extra.put(pack(cx, 12, cz), b(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true));
            }
        }

        /** Chains with soul lanterns hanging from the roof's edge on the diagonals, where the roof still stands. */
        private void chains() {
            for (int k = 0; k < 4; k++) {
                double a = Math.PI / 4 + k * Math.PI / 2;
                if (wallTop(a) < ROOF + 1) continue;
                int cx = (int) Math.round(Math.cos(rot + a) * 12.5), cz = (int) Math.round(Math.sin(rot + a) * 12.5);
                int len = 2 + (int) (hash(k, 81, 0) * 3);
                for (int h = ROOF - 1; h > ROOF - 1 - len; h--) extra.put(pack(cx, h, cz), b(Blocks.IRON_CHAIN));
                extra.put(pack(cx, ROOF - 1 - len, cz), b(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true));
            }
        }

        /** Burnt roof beams of Charred Log jutting from the wall under the roof line where the wall stands high. */
        private void beams() {
            for (int k = 0; k < 10; k++) {
                double a = (k + 0.5) * Math.PI / 5 + (hash(k, 91, 0) - 0.5) * 0.2;
                if (wallTop(a) < ROOF + 1 || fromGate(a) < 5 || fromThrone(a) < 6) continue;
                double ca = Math.cos(rot + a), sa = Math.sin(rot + a);
                Direction.Axis axis = Math.abs(ca) >= Math.abs(sa) ? Direction.Axis.X : Direction.Axis.Z;
                double len = 3 + hash(k, 92, 0) * 3;
                for (double r = FLOOR; r > FLOOR - len; r -= 0.5)
                    put(ca * r + 0.5, ROOF - 1, sa * r + 0.5, ScorchedBarrens.CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
            }
        }
    }
}
