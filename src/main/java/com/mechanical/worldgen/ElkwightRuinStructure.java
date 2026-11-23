package com.mechanical.worldgen;

import java.util.Optional;

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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
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
 * The Elkwight's ruin (docs/elkwight-plan.md phase 2): a roofless-in-places round arena of Blackstone in the Scorched
 * Barrens, never closer than {@link ScorchedBarrens#MIN_DISTANCE} to 0,0. Floor radius 17, walls to 20, four gates 7 wide
 * and 10 high (the antler crown is ~4 wide), tiered stands, eight pillars, a broken roof 15 up over the stands, a rune-ringed
 * dais in the middle where the boss waits ({@link ElkwightRuins#spawner}), soul fires, Rune Blackstone in walls and
 * pillars and rune monoliths outside, three chests on the stands. One piece; every block is a pure function of the seed
 * and the centre, so each chunk draws its own slice. Placement: scripts/gen_elkwight.py.
 */
public final class ElkwightRuinStructure extends Structure {
    public static final MapCodec<ElkwightRuinStructure> CODEC = simpleCodec(ElkwightRuinStructure::new);
    public static final StructureType<ElkwightRuinStructure> TYPE = () -> CODEC;
    public static final StructurePieceType.ContextlessType PIECE = Piece::new;
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_ruin"));
    static final int FLOOR = 17, WALL = 20, OUTER = 25, UP = 18, DOWN = 14, GATE_HEIGHT = 10, ROOF = 15;
    /** Blocks of Scorched Barrens round the centre: the arena and its monoliths, and burnt forest past them. */
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
            super(PIECE, 0, new BoundingBox(centre.getX() - OUTER, centre.getY() - DOWN, centre.getZ() - OUTER,
                    centre.getX() + OUTER, centre.getY() + UP, centre.getZ() + OUTER));
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

    /** Draws the part of the arena inside {@code box} (a chunk's slice in worldgen); spawns the boss if the box holds the centre. */
    public static void build(WorldGenLevel level, BoundingBox box, long seed, BlockPos centre, RandomSource random) {
        Arena arena = new Arena(seed, centre);
        int g = centre.getY();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), centre.getX() - OUTER); x <= Math.min(box.maxX(), centre.getX() + OUTER); x++) {
            for (int z = Math.max(box.minZ(), centre.getZ() - OUTER); z <= Math.min(box.maxZ(), centre.getZ() + OUTER); z++) {
                int dx = x - centre.getX(), dz = z - centre.getZ();
                double r = Math.sqrt(dx * dx + dz * dz);
                boolean inside = r < WALL + 0.5, monolith = arena.monolith(dx, dz) >= 0;
                if (!inside && !monolith) continue;
                for (int h = -1; h < UP; h++) {
                    int y = g + h;
                    if (y < box.minY() || y > box.maxY()) continue;
                    BlockState s = arena.at(dx, h, dz, r);
                    p.set(x, y, z);
                    if (s != null) level.setBlock(p, s, Block.UPDATE_CLIENTS);
                    else if (inside && !level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    if (s != null && s.is(Blocks.CHEST)) RandomizableContainer.setBlockEntityLootTable(level, random, p.immutable(),
                            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/elkwight_ruin")));
                }
                // foundation: nothing floats over a dip
                for (int y = g - 2; y >= g - DOWN && y >= box.minY(); y--) {
                    if (y > box.maxY()) continue;
                    BlockState old = level.getBlockState(p.set(x, y, z));
                    if (old.isSolid() && old.getFluidState().isEmpty()) break;
                    level.setBlock(p, (arena.hash(dx, y, dz) < 0.3 ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos boss = centre.above();
        if (box.isInside(boss)) ElkwightRuins.spawner.accept(level, boss);
    }

    /** Every block of one arena as a function of position (dx, dz from the centre, h above the floor). */
    static final class Arena {
        private final long seed;
        private final double rot;
        private final int[] pillarX = new int[8], pillarZ = new int[8], pillarTop = new int[8];

        Arena(long seed, BlockPos centre) {
            this.seed = seed;
            RandomSource rnd = RandomSource.create(seed);
            rot = rnd.nextDouble() * Math.PI / 2;
            for (int i = 0; i < 8; i++) {
                double a = rot + Math.PI / 8 + i * Math.PI / 4;
                pillarX[i] = (int) Math.round(Math.cos(a) * 9.5);
                pillarZ[i] = (int) Math.round(Math.sin(a) * 9.5);
                pillarTop[i] = rnd.nextInt(10) < 3 ? 4 + rnd.nextInt(5) : ROOF + 1; // some broken, the rest carry the roof
            }
        }

        double hash(int x, int y, int z) {
            long k = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ y * 0x165667B19E3779F9L;
            k = (k ^ k >>> 31) * 0xBF58476D1CE4E5B9L;
            k = (k ^ k >>> 27) * 0x94D049BB133111EBL;
            return ((k ^ k >>> 31) >>> 11) * 0x1.0p-53;
        }

        private double angle(int dx, int dz) {
            double a = Math.atan2(dz, dx) - rot;
            return a - Math.floor(a / (2 * Math.PI)) * 2 * Math.PI;
        }

        /** Angular distance in blocks (at the wall) to the nearest of the four gates. */
        private double fromGate(int dx, int dz) {
            double a = angle(dx, dz) % (Math.PI / 2);
            return Math.min(a, Math.PI / 2 - a) * (WALL - 1.5);
        }

        /** The wall's sector: 0 fallen low, 1 half height, 2 full height with the roof over the stands. */
        private int sector(int dx, int dz) {
            int s = (int) (angle(dx, dz) / (Math.PI / 12));
            double v = hash(s, 999, 777);
            return v < 0.3 ? 0 : v < 0.6 ? 1 : 2;
        }

        private int wallTop(int dx, int dz) {
            int s = sector(dx, dz);
            int base = s == 0 ? 3 + (int) (hash(dx, 1, dz) * 4) : s == 1 ? 9 + (int) (hash(dx, 2, dz) * 4) : ROOF + 1;
            return base - (hash(dx, 3, dz) < 0.25 ? 1 : 0);
        }

        private int tier(double r, int dx, int dz) {
            if (r < 12.5 || r >= FLOOR || fromGate(dx, dz) < 4.5) return 0;
            return Math.min(4, (int) ((r - 12.5) / 1.1) + 1);
        }

        /** Index of the rune monolith outside the walls at this column, or -1. */
        int monolith(int dx, int dz) {
            for (int i = 0; i < 4; i++) {
                double a = rot + Math.PI / 4 + i * Math.PI / 2;
                double cx = Math.cos(a) * 23, cz = Math.sin(a) * 23;
                double along = (dx - cx) * -Math.sin(a) + (dz - cz) * Math.cos(a), across = (dx - cx) * Math.cos(a) + (dz - cz) * Math.sin(a);
                if (Math.abs(along) < 1.0 && Math.abs(across) < 0.5) return i;
            }
            return -1;
        }

        private static Direction outward(int dx, int dz) {
            return Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
        }

        private static BlockState b(Block block) {
            return block.defaultBlockState();
        }

        private BlockState brick(int dx, int h, int dz) {
            double v = hash(dx, h, dz);
            return v < 0.55 ? b(Blocks.POLISHED_BLACKSTONE_BRICKS) : v < 0.78 ? b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)
                    : v < 0.9 ? b(Blocks.BLACKSTONE) : v < 0.93 ? b(Blocks.GILDED_BLACKSTONE) : v < 0.975 ? b(Blocks.POLISHED_BLACKSTONE) : b(Blocks.MAGMA_BLOCK);
        }

        /** The block at (dx, h, dz), r = horizontal distance; null = air inside the walls, untouched outside. */
        BlockState at(int dx, int h, int dz, double r) {
            int m = monolith(dx, dz);
            if (r >= WALL + 0.5) {
                if (m < 0) return null;
                if (h < 0) return b(Blocks.BLACKSTONE);
                int top = 4 + (int) (hash(m, 5, 5) * 2);
                return h >= top ? null : h < 2 ? b(Blocks.POLISHED_BLACKSTONE) : h == top - 1 && hash(dx, h, dz) < 0.5 ? b(Blocks.CHISELED_POLISHED_BLACKSTONE)
                        : b(ScorchedBarrens.RUNE_BLACKSTONE.get());
            }
            if (h == -1) return floor(dx, dz, r);
            // walls, with four tall gates
            if (r >= FLOOR) {
                int top = wallTop(dx, dz);
                if (h >= top || fromGate(dx, dz) < 3.5 && h < GATE_HEIGHT) return null;
                if (h == 0) return b(Blocks.BASALT);
                boolean innerFace = r < FLOOR + 0.9;
                if (innerFace && h >= 2 && h <= 7 && hash(dx, h, dz) < 0.09) return b(ScorchedBarrens.RUNE_BLACKSTONE.get());
                if (innerFace && h == 5) return b(Blocks.CHISELED_POLISHED_BLACKSTONE);
                return brick(dx, h, dz);
            }
            // the roof over the stands, only where the wall stands to full height; holes where it fell in
            if (h == ROOF && r >= 10.5 && sector(dx, dz) == 2 && hash(dx, ROOF, dz) > 0.22)
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
            // tiered stands with steps facing the middle, three chests on top
            int tier = tier(r, dx, dz);
            if (tier > 0) {
                if (h >= tier) return chest(dx, h, dz, tier) ? b(Blocks.CHEST).setValue(ChestBlock.FACING, outward(dx, dz).getOpposite()) : null;
                if (h < tier - 1) return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
                if (hash(dx, 31, dz) < 0.07) return null; // a missing step
                Direction out = outward(dx, dz);
                int inner = tier(Math.hypot(dx - out.getStepX(), dz - out.getStepZ()), dx - out.getStepX(), dz - out.getStepZ());
                return inner < tier ? b(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS).setValue(StairBlock.FACING, out) : b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            // the dais: rune ring round the boss's place
            if (r < 4.6) {
                if (h != 0) return null;
                if (r >= 3.4) return (int) (angle(dx, dz) * 8 / Math.PI) % 2 == 0 ? b(ScorchedBarrens.RUNE_BLACKSTONE.get()) : b(Blocks.CHISELED_POLISHED_BLACKSTONE);
                return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            // soul campfires on the four sides of the dais, fire bowls between the pillars
            if (h == 0 && (dx == 0 || dz == 0) && Math.abs(dx + dz) == 6)
                return b(Blocks.SOUL_CAMPFIRE).setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.FACING, outward(dx, dz));
            for (int i = 0; i < 4; i++) {
                double a = rot + Math.PI / 4 + i * Math.PI / 2;
                int bx = (int) Math.round(Math.cos(a) * 7), bz = (int) Math.round(Math.sin(a) * 7);
                int ox = dx - bx, oz = dz - bz;
                if (ox == 0 && oz == 0) return h == 0 ? b(Blocks.POLISHED_BLACKSTONE_BRICKS) : h == 1 ? b(Blocks.SOUL_SOIL) : h == 2 ? b(Blocks.SOUL_FIRE) : null;
                if (h == 1 && Math.abs(ox) + Math.abs(oz) == 1) return b(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB).setValue(SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
            }
            // rubble from the fallen roof and pillars
            if (h == 0 && hash(dx, 11, dz) < 0.035) return hash(dx, 12, dz) < 0.5 ? b(Blocks.BLACKSTONE) : b(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
            return null;
        }

        private boolean chest(int dx, int h, int dz, int tier) {
            if (h != tier || tier != 3) return false;
            for (int i = 0; i < 3; i++) {
                double a = rot + Math.PI / 3 + i * Math.PI / 2; // 60, 150, 240 degrees from the first gate
                int cx = (int) Math.round(Math.cos(a) * 15), cz = (int) Math.round(Math.sin(a) * 15);
                if (cx == dx && cz == dz) return true;
            }
            return false;
        }

        private BlockState floor(int dx, int dz, double r) {
            if (r >= 5 && r < 6) return b(Blocks.CHISELED_POLISHED_BLACKSTONE);
            double v = hash(dx, -1, dz);
            if (r > 8 && r < FLOOR && v < 0.05) return b(Blocks.MAGMA_BLOCK);
            if (v < 0.25) return b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            if (v < 0.33) return b(Blocks.POLISHED_BASALT).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            if (v < 0.38) return b(Blocks.BASALT);
            return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
    }
}
