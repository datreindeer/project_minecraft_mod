package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mechanical.BarkRoofBlock;
import com.mechanical.BearRugBlock;
import com.mechanical.BronzeAnvilBlock;
import com.mechanical.Farming;
import com.mechanical.Ironworking;
import com.mechanical.KilnBlock;
import com.mechanical.Smelting;
import com.mechanical.WorldAge;
import com.mechanical.wildlife.mountain.MountainFauna;
import com.mechanical.Campfires;
import com.mechanical.DryingRackBlock;
import com.mechanical.Mechanical;
import com.mechanical.RockBlock;
import com.mechanical.people.ClimateZone;
import com.mechanical.people.People;
import com.mechanical.people.Tribe;
import com.mechanical.people.TribeRole;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.StairsShape;
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
 * A tribe village (/locate structure mechanical:tribe_village): a ring of sharpened log stakes about 43 blocks across with
 * a gate flying the tribe's banner, a longhouse for the elder, round huts and hide tents facing a central fire, a flag pole,
 * drying racks, food storage pots and work spots for each role, and ten or more people of one tribe. The tribe (and so its
 * temperament) decides the wood, hide colours, walls, roofs (bark shingles, turf, hide) and the mix of roles; raiders put skulls by their gate.
 * The climate at the centre ({@link ClimateZone}, saved in the piece) sets the style: turf houses and mammoth-bone huts in
 * the cold, mud-brick halls, mud huts and hide shades where it is warm, stilt houses under leaf roofs in the jungle.
 * <p>
 * One piece holding a seed and the centre; the whole layout follows from those, so each chunk draws only its own slice and
 * never reads terrain outside it. The structure's beard_box terrain adaptation levels the ground under the piece's box at
 * noise time; postProcess clears what grew there and patches the ground. Placement data: scripts/gen_tribe_village.py.
 */
public final class TribeVillageStructure extends Structure {
    public static final MapCodec<TribeVillageStructure> CODEC = simpleCodec(TribeVillageStructure::new);
    public static final StructureType<TribeVillageStructure> TYPE = () -> CODEC;
    public static final StructurePieceType.ContextlessType PIECE = Piece::new;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_village");
    public static final int WALL_R = 21;
    public static final int HALF = 26;
    static final int CLEAR_R = 25, HEIGHT = 24;

    public TribeVillageStructure(StructureSettings settings) {
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
            event.register(Registries.STRUCTURE_TYPE, ID, () -> TYPE);
            event.register(Registries.STRUCTURE_PIECE, ID, () -> PIECE);
        }
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!context.couldValidBiomeExistOnTopOfChunkCenter()) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        ChunkGenerator gen = context.chunkGenerator();
        // dry, and no more than 8 blocks of rise across the whole site (beard_box levels the rest)
        List<Integer> heights = new ArrayList<>();
        for (int dx = -24; dx <= 24; dx += 6) {
            for (int dz = -24; dz <= 24; dz += 6) {
                if (dx * dx + dz * dz > 24 * 24) continue;
                int floor = gen.getBaseHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                int surface = gen.getBaseHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (floor != surface) return Optional.empty();
                heights.add(floor);
            }
        }
        heights.sort(null);
        if (heights.getLast() - heights.getFirst() > 8) return Optional.empty();
        BlockPos centre = new BlockPos(x, heights.get(heights.size() / 2), z);
        long seed = context.random().nextLong();
        // the climate at the centre decides how the whole village is built (the layout never reads biomes itself)
        ClimateZone climate = ClimateZone.of(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(centre.getY()),
                QuartPos.fromBlock(z)));
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new Piece(seed, centre, climate))));
    }

    /** The tribe village whose bounds hold {@code pos}, or null. */
    public static Piece villageAt(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        Structure village = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(ID);
        if (village == null) return null;
        var start = level.structureManager().getStructureAt(pos, village);
        return start.isValid() && start.getPieces().getFirst() instanceof Piece piece ? piece : null;
    }

    // ------------------------------------------------------------------ the piece

    /** Every block a village with this seed and centre would place in {@code zone}, in placing order (for tests). */
    public static Map<BlockPos, BlockState> plan(long seed, BlockPos centre, ClimateZone zone) {
        return plan(seed, centre, zone, com.mechanical.WorldAge.STONE);
    }

    /** The same village as built in {@code age} (WorldAge): what its tribe tears down and rebuilds as it goes up an age (AgeRebuild). */
    public static Map<BlockPos, BlockState> plan(long seed, BlockPos centre, ClimateZone zone, int age) {
        return new Layout(seed, centre, zone, age).blocks;
    }

    /** The tribe of a village seed: the layout's first two draws. */
    public static UUID tribeOf(long seed) {
        RandomSource r = RandomSource.create(seed);
        return new UUID(r.nextLong(), r.nextLong());
    }

    /** The gate's direction (radians) for a layout seed: the third draw of the layout's random, after the tribe's id. */
    public static double gateAngle(long seed) {
        RandomSource r = RandomSource.create(seed);
        r.nextLong();
        r.nextLong();
        return r.nextDouble() * Math.PI * 2;
    }

    public static final class Piece extends StructurePiece {
        private final long seed;
        private final BlockPos centre;
        private final ClimateZone climate;

        /** The climate the village is built for (villages saved before climates were temperate). */
        public ClimateZone climate() {
            return climate;
        }

        public long seed() {
            return seed;
        }

        public BlockPos centre() {
            return centre;
        }

        /** Where a trail from outside should end: on the gate's line, {@code beyond} blocks past the start of the village path. */
        public BlockPos trailEnd(int beyond) {
            double a = gateAngle(seed), r = WALL_R + 3 + beyond;
            return new BlockPos(centre.getX() + (int) Math.round(Math.cos(a) * r), centre.getY(), centre.getZ() + (int) Math.round(Math.sin(a) * r));
        }

        Piece(long seed, BlockPos centre, ClimateZone climate) {
            // the box's floor is the village floor: beard_box fills below it and carves above it
            super(PIECE, 0, new BoundingBox(centre.getX() - HALF, centre.getY(), centre.getZ() - HALF,
                    centre.getX() + HALF, centre.getY() + HEIGHT, centre.getZ() + HALF));
            this.seed = seed;
            this.centre = centre;
            this.climate = climate;
        }

        Piece(CompoundTag tag) {
            super(PIECE, tag);
            seed = tag.getLongOr("seed", 0);
            centre = BlockPos.of(tag.getLongOr("centre", 0));
            climate = ClimateZone.byId(tag.getIntOr("climate", ClimateZone.TEMPERATE.ordinal()));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putLong("seed", seed);
            tag.putLong("centre", centre.asLong());
            tag.putInt("climate", climate.ordinal());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                BoundingBox box, ChunkPos chunkPos, BlockPos referencePos) {
            Layout layout = new Layout(seed, centre, climate, com.mechanical.WorldAge.STONE); // generated in the Stone Age, rebuilt later (AgeRebuild)
            prepareGround(level, box);
            for (Map.Entry<BlockPos, BlockState> e : layout.blocks.entrySet()) {
                if (box.isInside(e.getKey())) level.setBlock(e.getKey(), e.getValue(), Block.UPDATE_CLIENTS);
            }
            for (BlockPos p : layout.banners) {
                if (box.isInside(p) && level.getBlockEntity(p) instanceof BannerBlockEntity banner) {
                    banner.applyComponentsFromItemStack(layout.tribe.banner(level.registryAccess()));
                }
            }
            ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/tribe_village"));
            for (BlockPos p : layout.storage) if (box.isInside(p)) RandomizableContainer.setBlockEntityLootTable(level, random, p, loot);
            if (box.isInside(centre)) layout.spawnPeople(level, random);
        }

        /** Inside the clearing: everything above the floor goes (trees, boulders, plants), dips are filled, the top is soil. */
        private void prepareGround(WorldGenLevel level, BoundingBox box) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int y0 = centre.getY();
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    int dx = x - centre.getX(), dz = z - centre.getZ();
                    if (dx * dx + dz * dz > CLEAR_R * CLEAR_R + CLEAR_R) continue;
                    for (int y = y0; y <= y0 + 32; y++) {
                        if (!level.getBlockState(p.set(x, y, z)).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    BlockState top = level.getBlockState(p.set(x, y0 - 1, z));
                    boolean dry = climate == ClimateZone.WARM && (top.is(BlockTags.SAND) || top.is(BlockTags.TERRACOTTA)); // desert floors stay
                    if (!(top.is(BlockTags.SUBSTRATE_OVERWORLD) || dry) || !top.getFluidState().isEmpty()) {
                        level.setBlock(p, (climate == ClimateZone.WARM ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    for (int y = y0 - 2; y > y0 - 14; y--) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (s.isSolid() && s.getFluidState().isEmpty()) break;
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ the layout

    /**
     * Every block of the village (in placing order), the banners and storage to fill in, and the people. The climate picks
     * the building style ({@link ClimateZone}); the temperament still varies the wood, walls, roofs and hides within it.
     * Every style keeps the same plan (palisade of radius {@link #WALL_R}, gate, longhouse across the fire, six huts and
     * tents on a ring of 14), so the gate and wall lookups work the same everywhere.
     */
    static final class Layout {
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        final List<BlockPos> banners = new ArrayList<>(), storage = new ArrayList<>();
        final Tribe tribe;
        final ClimateZone zone;
        /** The age it is built in (WorldAge): Stone Age as generated, Bronze and Iron Age as its tribe rebuilds it. */
        private final int age;
        private final UUID group;
        private final RandomSource rnd;
        private final BlockPos c;
        private final int y;
        private final double gate;
        private final boolean spruce, daub;
        private final DyeColor[] hides;
        private final BlockState log, stripped, fence, post, stake, roof;

        Layout(long seed, BlockPos centre, ClimateZone zone, int age) {
            this.zone = zone;
            this.age = age;
            rnd = RandomSource.create(seed);
            c = centre;
            y = centre.getY();
            group = new UUID(rnd.nextLong(), rnd.nextLong());
            tribe = Tribe.of(group);
            Tribe.Temperament t = tribe.temperament;
            spruce = t == Tribe.Temperament.WARY || t == Tribe.Temperament.RAIDERS;
            daub = t == Tribe.Temperament.PEACEFUL || t == Tribe.Temperament.TRADERS;
            hides = switch (t) {
                case PEACEFUL -> new DyeColor[] {DyeColor.WHITE, DyeColor.BROWN, DyeColor.LIGHT_GRAY};
                case WARY -> new DyeColor[] {DyeColor.BROWN, DyeColor.GRAY, DyeColor.BROWN};
                case RAIDERS -> new DyeColor[] {DyeColor.BLACK, DyeColor.GRAY, DyeColor.RED};
                case TRADERS -> new DyeColor[] {DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.WHITE};
            };
            log = zone.log(spruce);
            stripped = zone.stripped(spruce);
            fence = zone.fence(spruce);
            post = zone.post(spruce);
            stake = zone.stake(spruce);
            roof = zone.barkRoof(spruce);
            gate = gateAngle(seed);
            rnd.nextDouble(); // the draw gateAngle repeats, so the rest of the layout stays as it was

            palisade();
            path();
            centre();
            // the longhouse faces the gate across the fire; huts and tents around the rest of the ring
            BlockPos hall = at(gate + Math.PI, 12);
            switch (zone) {
                case TEMPERATE, COLD -> longhouse(hall);
                case WARM -> mudLonghouse(hall);
                case JUNGLE -> stiltLonghouse(hall);
            }
            double[] slots = {60, 100, 140, 220, 260, 300};
            for (int i = 0; i < slots.length; i++) {
                BlockPos p = at(gate + Math.toRadians(slots[i] + rnd.nextInt(11) - 5), 14);
                if (i % 2 == 0 || rnd.nextInt(3) == 0) hut(p, facing(p), hides[i % hides.length], i);
                else shelter(p, facing(p), hides[(i + 1) % hides.length]);
            }
            racks();
            foodStore(at(gate + Math.toRadians(150), 8));
            if (t == Tribe.Temperament.TRADERS) foodStore(at(gate + Math.toRadians(210), 8));
            workplaces();
        }

        /** A spot at angle a (radians, 0 = gate direction when offset by gate), distance r from the fire, on the floor. */
        private BlockPos at(double a, double r) {
            return new BlockPos(c.getX() + (int) Math.round(Math.cos(a) * r), y, c.getZ() + (int) Math.round(Math.sin(a) * r));
        }

        /** The side of a building at p that faces the fire. */
        private Direction facing(BlockPos p) {
            return Direction.getApproximateNearest((double) (c.getX() - p.getX()), 0.0, (double) (c.getZ() - p.getZ()));
        }

        private void put(BlockPos p, BlockState s) {
            blocks.remove(p); // re-insert so the later block also places later
            blocks.put(p.immutable(), s);
        }

        private void banner(BlockPos p, double faceX, double faceZ) {
            float yaw = (float) Math.toDegrees(Math.atan2(-faceX, faceZ));
            put(p, Blocks.BANNER.pick(tribe.baseColour()).defaultBlockState().setValue(BannerBlock.ROTATION, RotationSegment.convertToSegment(yaw)));
            banners.add(p.immutable());
        }

        private static int hash(int x, int z) {
            int h = x * 73428767 ^ z * 912931;
            return (h ^ (h >>> 13)) & 0x7fffffff;
        }

        private double gateArc(int dx, int dz) {
            double d = Math.atan2(dz, dx) - gate;
            d = Math.abs(Math.atan2(Math.sin(d), Math.cos(d)));
            return d * WALL_R;
        }

        /** Stakes 4-5 high with pointed tops in a ring (bamboo bundles in the jungle), a 3-wide gate between two tall posts flying the banner. */
        private void palisade() {
            for (int dx = -WALL_R - 1; dx <= WALL_R + 1; dx++) {
                for (int dz = -WALL_R - 1; dz <= WALL_R + 1; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (Math.abs(d - WALL_R) >= 0.5) continue;
                    double arc = gateArc(dx, dz);
                    if (arc < 1.6) continue;
                    BlockPos base = new BlockPos(c.getX() + dx, y, c.getZ() + dz);
                    put(base.below(), log);
                    if (arc < 2.7) {
                        for (int h = 0; h < 6; h++) put(base.above(h), log);
                        banner(base.above(6), dx, dz);
                        continue;
                    }
                    int h = 3 + hash(base.getX(), base.getZ()) % 2 + (age >= WorldAge.IRON ? 1 : 0); // Iron Age walls a stake higher
                    for (int i = 0; i < h; i++) put(base.above(i), post);
                    put(base.above(h), stake);
                }
            }
            if (tribe.temperament == Tribe.Temperament.RAIDERS) {
                // skulls on stakes outside the gate
                for (int side = -1; side <= 1; side += 2) {
                    double a = gate + side * 3.5 / WALL_R;
                    BlockPos p = at(a, WALL_R + 2);
                    put(p, fence);
                    put(p.above(), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,
                            RotationSegment.convertToSegment((float) Math.toDegrees(Math.atan2(-Math.cos(gate), Math.sin(gate))))));
                }
            }
        }

        /** A worn path from outside the gate to the fire. */
        private void path() {
            for (double r = WALL_R + 3; r >= 3; r -= 0.5) {
                for (int w = -1; w <= 1; w++) {
                    double a = gate + w / Math.max(r, 3.0);
                    BlockPos p = at(a, r).below();
                    put(p, rnd.nextInt(6) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
                }
            }
        }

        /** The communal fire in a ring of stones, log benches, and the flag pole. */
        private void centre() {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx * dx + dz * dz <= 10 && rnd.nextInt(3) != 0) put(c.offset(dx, -1, dz), Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
            BlockState fire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
            if (fire.hasProperty(Campfires.EMBERS)) fire = fire.setValue(Campfires.EMBERS, false);
            put(c, fire);
            int[][] ring = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}};
            for (int i = 0; i < 4; i++) {
                int[] o = ring[i];
                put(c.offset(o[0], 0, o[1]), Mechanical.LOOSE_STONE.get().defaultBlockState().setValue(RockBlock.SIZE, 1 + rnd.nextInt(2)));
            }
            for (double a : new double[] {Math.PI / 2, Math.PI, Math.PI * 1.5}) {
                BlockPos b = at(gate + a, 3.6);
                Direction.Axis axis = Math.abs(b.getX() - c.getX()) >= Math.abs(b.getZ() - c.getZ()) ? Direction.Axis.Z : Direction.Axis.X;
                put(b, log.setValue(RotatedPillarBlock.AXIS, axis));
            }
            BlockPos pole = at(gate + Math.PI * 0.75, 5);
            for (int h = 0; h < 5; h++) put(pole.above(h), stripped);
            banner(pole.above(5), Math.cos(gate), Math.sin(gate));
        }

        /** A bark roof slope rising toward {@code up}, or an outer corner of one peaking toward up and {@code side}. */
        private BlockState slope(Direction up, Direction side) {
            BlockState s = roof.setValue(BarkRoofBlock.FACING, up);
            if (side == null) return s;
            return s.setValue(BarkRoofBlock.SHAPE, up.getCounterClockWise() == side ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT);
        }

        private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState(), SNOWY_GRASS = GRASS.setValue(SnowyBlock.SNOWY, true),
                SNOW = Blocks.SNOW.defaultBlockState();

        /** Turf at p: sod, under a skin of snow in the cold. */
        private void turf(BlockPos p) {
            if (zone == ClimateZone.COLD) {
                put(p, SNOWY_GRASS);
                put(p.above(), SNOW);
            } else put(p, GRASS);
        }

        /** Leaves for a roof that never decays. */
        private BlockState leaves() {
            Block b = tribe.temperament == Tribe.Temperament.WARY || tribe.temperament == Tribe.Temperament.RAIDERS ? Blocks.MANGROVE_LEAVES : Blocks.JUNGLE_LEAVES;
            return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        }

        /**
         * The elder's longhouse: 5 deep, 9 long, log walls, the door toward the fire. A 45-degree gable roof of bark shingles
         * with a ridge log; wary tribes lay turf over it above the bark eaves. The gable ends are daub, hide or split logs.
         * In the cold the walls stand on a footing of field stone and every tribe lays turf (under snow) over the bark.
         * Bronze Age: a course taller on a stone footing, timber-framed (stripped posts with daub between, horn windows),
         * daub gables, thatch over the bark eaves (turf in the cold and for wary tribes). Iron Age: another course, of stone.
         */
        private void longhouse(BlockPos m) {
            Direction front = facing(m), side = front.getClockWise();
            BlockState wall = log.setValue(RotatedPillarBlock.AXIS, side.getAxis());
            boolean cold = zone == ClimateZone.COLD, metal = age >= WorldAge.BRONZE;
            int up = age, footing = Math.max(age, cold ? 1 : 0);
            boolean turf = tribe.temperament == Tribe.Temperament.WARY || cold, thatch = metal && !turf;
            BlockState daub = Blocks.PACKED_MUD.defaultBlockState(), hay = Blocks.HAY_BLOCK.defaultBlockState();
            BlockState gable = metal ? daub : switch (tribe.temperament) {
                case PEACEFUL -> cold ? Blocks.WOOL.pick(hides[0]).defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
                case WARY -> stripped;
                case RAIDERS, TRADERS -> Blocks.WOOL.pick(hides[1]).defaultBlockState();
            };
            for (int a = -3; a <= 3; a++) { // a: toward the fire, b: along the house
                for (int b = -5; b <= 5; b++) {
                    BlockPos p = m.relative(front, a).relative(side, b);
                    boolean in = Math.abs(a) <= 2 && Math.abs(b) <= 4;
                    if (in) put(p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                    if (in && (Math.abs(a) == 2 || Math.abs(b) == 4)) {
                        boolean corner = Math.abs(a) == 2 && Math.abs(b) == 4;
                        boolean door = a == 2 && b == 0;
                        for (int h = 0; h < 2 + up; h++) {
                            if (door && h < 2) continue;
                            BlockState w = corner ? log : Math.abs(b) == 4 ? log : wall;
                            if (metal && !corner) w = door ? wall : Math.abs(a) == 2 && b % 2 == 0 ? stripped : daub; // the lintel over the door
                            if (metal && h == footing && Math.abs(a) == 2 && Math.abs(b) == 3) w = MountainFauna.HORN_WINDOW.get().defaultBlockState();
                            if (h < footing) w = hash(p.getX(), p.getZ() + h) % 3 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
                            put(p.above(h), w);
                        }
                    }
                    // the gable ends under the slopes
                    if (Math.abs(b) == 4) for (int h = 2 + up; h < 5 + up - Math.abs(a); h++) put(p.above(h), gable);
                    // the roof: one row of slopes per step, rising toward the ridge (the eaves 2 high, the ridge log 5)
                    int h = 5 + up - Math.abs(a);
                    if (a == 0) {
                        if (cold) turf(p.above(h));
                        else put(p.above(h), turf ? Blocks.MOSS_BLOCK.defaultBlockState() : wall);
                    } else if ((turf || thatch) && Math.abs(a) < 3) {
                        put(p.above(h - 1), thatch ? hay : Blocks.ROOTED_DIRT.defaultBlockState()); // sod (or thatch) in steps over the bark eaves
                        if (cold) turf(p.above(h));
                        else put(p.above(h), thatch ? hay : Blocks.GRASS_BLOCK.defaultBlockState());
                    } else put(p.above(h), slope(a > 0 ? front.getOpposite() : front, null));
                }
            }
            interior(m, front, side, 3, -1);
        }

        /** Hides on the floor, a storage pot, a drying rack and a pile of hides to sleep on, along the back of a long hall. */
        private void interior(BlockPos m, Direction front, Direction side, int len, int up) {
            BlockPos floor = m.above(up + 1);
            for (int b = -len; b <= len; b++) put(floor.relative(side, b), Blocks.CARPET.pick(hides[2]).defaultBlockState());
            BlockPos back = floor.relative(front, -1);
            put(back.relative(side, -3), Blocks.DECORATED_POT.defaultBlockState());
            storage.add(back.relative(side, -3));
            put(back.relative(side, 3), Mechanical.DRYING_RACK.get().defaultBlockState().setValue(DryingRackBlock.FACING, front)
                    .setValue(DryingRackBlock.HIDE, 2));
            put(back.relative(side, 2), People.HIDE_CANVAS.get().defaultBlockState()); // a pile of hides to sleep on
        }

        /**
         * Warm: the elder's hall of mud brick (packed mud for wary tribes, sandstone for traders, red sandstone for raiders),
         * three high with acacia corner posts and windows, under a flat roof of packed mud on acacia beams whose ends stick
         * out of the walls, a hide shade on poles before the door. Bronze and Iron Age: a course taller, a porch roofed with packed mud.
         */
        private void mudLonghouse(BlockPos m) {
            Direction front = facing(m), side = front.getClockWise();
            BlockState wall = warmWall();
            int up = age >= WorldAge.BRONZE ? 1 : 0;
            BlockState beam = stripped.setValue(RotatedPillarBlock.AXIS, front.getAxis());
            for (int a = -3; a <= 3; a++) {
                for (int b = -5; b <= 5; b++) {
                    BlockPos p = m.relative(front, a).relative(side, b);
                    boolean in = Math.abs(a) <= 2 && Math.abs(b) <= 4;
                    if (in) {
                        put(p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                        put(p.above(3 + up), Blocks.PACKED_MUD.defaultBlockState());
                    }
                    if (in && (Math.abs(a) == 2 || Math.abs(b) == 4)) {
                        boolean corner = Math.abs(a) == 2 && Math.abs(b) == 4;
                        boolean door = a == 2 && b == 0, window = Math.abs(a) == 2 && Math.abs(b) == 2;
                        for (int h = 0; h < 3 + up; h++) {
                            if (door && h < 2 || window && h == 1) continue;
                            put(p.above(h), corner ? log : wall);
                        }
                        if (Math.abs(b) < 4 && b % 2 == 0 || corner) put(p.above(4 + up), wall); // a low parapet, notched
                    }
                    if (Math.abs(a) == 3 && Math.abs(b) <= 4 && b % 2 == 0) put(p.above(2 + up), beam); // beam ends
                }
            }
            // the shade before the door: two poles and a hide stretched back to the wall
            for (int a = 3; a <= 4; a++) {
                for (int b = -2; b <= 2; b++) {
                    BlockPos p = m.relative(front, a).relative(side, b);
                    if (a == 4 && Math.abs(b) == 2) for (int h = 0; h < 2 + up; h++) put(p.above(h), fence);
                    put(p.above(2 + up), up > 0 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.WOOL.pick(hides[0]).defaultBlockState());
                }
            }
            interior(m, front, side, 3, -1);
        }

        private BlockState warmWall() {
            return switch (tribe.temperament) {
                case PEACEFUL -> Blocks.MUD_BRICKS.defaultBlockState();
                case WARY -> Blocks.PACKED_MUD.defaultBlockState();
                case RAIDERS -> Blocks.RED_SANDSTONE.defaultBlockState();
                case TRADERS -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            };
        }

        /**
         * Jungle: a platform of jungle planks three blocks up on log posts, reached by a ladder on the fire side, walls of
         * bamboo bundles with bamboo-fence windows, a door over the ladder. Corner and side posts run up to the eaves.
         */
        private void stilts(BlockPos m, Direction front, int ra, int rb) {
            Direction side = front.getClockWise();
            // Bronze and Iron Age: plank walls (sawn with metal tools) in place of bamboo bundles
            BlockState bamboo = (age >= WorldAge.BRONZE ? Blocks.JUNGLE_PLANKS : Blocks.BAMBOO_BLOCK).defaultBlockState(), rail = Blocks.BAMBOO_FENCE.defaultBlockState();
            for (int a = -ra; a <= ra; a++) {
                for (int b = -rb; b <= rb; b++) {
                    BlockPos p = m.relative(front, a).relative(side, b);
                    put(p.above(2), Blocks.JUNGLE_PLANKS.defaultBlockState());
                    boolean edge = Math.abs(a) == ra || Math.abs(b) == rb;
                    boolean postHere = Math.abs(a) == ra && (Math.abs(b) == rb || b == 0 && a < 0) || Math.abs(b) == rb && a == 0;
                    if (postHere) for (int h = -1; h < 5; h++) put(p.above(h), log);
                    else if (edge) {
                        boolean door = a == ra && b == 0;
                        if (!door) {
                            put(p.above(3), bamboo);
                            put(p.above(4), (a + b) % 2 == 0 ? bamboo : rail);
                        }
                    }
                }
            }
            // the door's posts and the ladder below it
            BlockPos door = m.relative(front, ra);
            for (int h = 0; h < 2; h++) put(door.above(h), log);
            for (int h = 0; h < 3; h++) put(door.relative(front).above(h), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, front));
        }

        /** Jungle: the elder's hall on stilts (5 by 9) under a steep gable roof of leaves, the gable ends planked. */
        private void stiltLonghouse(BlockPos m) {
            Direction front = facing(m), side = front.getClockWise();
            stilts(m, front, 2, 4);
            BlockState leaf = leaves();
            for (int a = -3; a <= 3; a++) {
                for (int b = -5; b <= 5; b++) {
                    BlockPos p = m.relative(front, a).relative(side, b);
                    int h = 8 - Math.abs(a);
                    put(p.above(h), leaf);
                    if (Math.abs(a) < 3) put(p.above(h - 1), leaf); // two layers thick
                    if (Math.abs(b) == 4 && Math.abs(a) < 3) for (int k = 5; k < h - 1; k++) put(p.above(k), Blocks.JUNGLE_PLANKS.defaultBlockState());
                }
            }
            interior(m, front, side, 1, 2);
        }

        /** Hut slot i: a round hut, in the cold a mammoth-bone hut or a turf hut, a mud hut where it is warm, a stilt hut in the jungle. */
        private void hut(BlockPos m, Direction front, DyeColor hide, int i) {
            switch (zone) {
                case TEMPERATE -> roundHut(m, front, hide);
                case COLD -> {
                    // raiders and wary tribes are hunters: more bone huts; the others build more turf
                    boolean bones = tribe.temperament == Tribe.Temperament.RAIDERS || tribe.temperament == Tribe.Temperament.WARY ? i != 2 : i == 2;
                    if (bones && age == WorldAge.STONE) boneHut(m, front, hide); // the mammoths are gone by the Bronze Age
                    else roundHut(m, front, hide);
                }
                case WARM -> mudHut(m, front, hide);
                case JUNGLE -> stiltHut(m, front, hide);
            }
        }

        /** A tent slot: a hide tent (white and grey hides in the cold), a hide shade where it is warm, a leaf-roofed lean-to in the jungle. */
        private void shelter(BlockPos t, Direction front, DyeColor hide) {
            if (age >= WorldAge.BRONZE) {
                pen(t, front);
                return;
            }
            switch (zone) {
                case TEMPERATE -> tent(t, front, hide);
                case COLD -> tent(t, front, hide == DyeColor.BROWN || hide == DyeColor.BLACK ? DyeColor.LIGHT_GRAY : DyeColor.WHITE);
                case WARM -> shade(t, front, hide);
                case JUNGLE -> leanTo(t, front, hide);
            }
        }

        /**
         * A round hut (5 across) of daub or hides, the door toward the fire, under a hipped bark roof (7 across at the eaves)
         * rising in three rings to a cap: turf for peaceful and wary tribes, hide for traders, a log and smoke pole for raiders.
         * In the cold it is a turf hut: a wall of field stone under a mound of turf and snow.
         */
        private void roundHut(BlockPos m, Direction front, DyeColor hide) {
            boolean cold = zone == ClimateZone.COLD, metal = age >= WorldAge.BRONZE;
            int up = metal ? 1 : 0; // Bronze and Iron Age: a roundhouse a course taller, daub on a stone footing, thatched
            BlockState wall = daub || metal ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.WOOL.pick(hide).defaultBlockState();
            if (cold) wall = Blocks.COBBLESTONE.defaultBlockState();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    BlockPos p = m.offset(dx, 0, dz);
                    if (d <= 2.6) put(p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                    if (d > 1.8 && d <= 2.6) {
                        int ahead = dx * front.getStepX() + dz * front.getStepZ(), across = dx * front.getStepZ() - dz * front.getStepX();
                        boolean door = ahead >= 2 && across == 0;
                        if (!door) {
                            put(p, metal ? Blocks.COBBLESTONE.defaultBlockState() : wall);
                            put(p.above(), cold && hash(p.getX(), p.getZ()) % 3 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : wall);
                        }
                        if (metal) put(p.above(2), wall);
                    }
                    int ring = Math.max(Math.abs(dx), Math.abs(dz));
                    if (ring == 0) continue;
                    if (cold) {
                        if (ring < 3) put(p.above(4 - ring + up), Blocks.ROOTED_DIRT.defaultBlockState());
                        turf(p.above(5 - ring + up));
                        continue;
                    }
                    // each ring one higher toward the middle: on a side it rises toward the middle, at a corner it peaks inward
                    Direction xIn = dx > 0 ? Direction.WEST : Direction.EAST, zIn = dz > 0 ? Direction.NORTH : Direction.SOUTH;
                    BlockState s = Math.abs(dx) == Math.abs(dz) ? slope(zIn, xIn) : Math.abs(dx) > Math.abs(dz) ? slope(xIn, null) : slope(zIn, null);
                    put(p.above(5 - ring + up), metal && ring < 3 ? Blocks.HAY_BLOCK.defaultBlockState() : s); // thatch above the bark eaves
                }
            }
            if (cold) {
                turf(m.above(4 + up));
                put(m.above(4 + up), stripped); // the smoke hole's frame
            } else if (metal && tribe.temperament != Tribe.Temperament.RAIDERS) {
                put(m.above(5), Blocks.HAY_BLOCK.defaultBlockState());
            } else if (metal) {
                put(m.above(5), log);
                put(m.above(6), fence);
            } else {
                switch (tribe.temperament) {
                    case PEACEFUL -> put(m.above(4), Blocks.MOSS_BLOCK.defaultBlockState());
                    case WARY -> put(m.above(4), Blocks.GRASS_BLOCK.defaultBlockState());
                    case TRADERS -> put(m.above(4), Blocks.WOOL.pick(hide).defaultBlockState());
                    case RAIDERS -> {
                        put(m.above(4), log);
                        put(m.above(5), fence);
                    }
                }
            }
            put(m, Blocks.CARPET.pick(hide == DyeColor.BROWN ? DyeColor.WHITE : DyeColor.BROWN).defaultBlockState());
            BlockPos back = m.relative(front, -1).relative(front.getClockWise());
            put(back, Blocks.DECORATED_POT.defaultBlockState());
            if (rnd.nextBoolean()) storage.add(back.immutable());
        }

        /** Cold: a dome of mammoth-bone arches (as worldgen MammothBoneHut), the gaps packed with hides and snow, a bear rug inside. */
        private void boneHut(BlockPos m, Direction front, DyeColor hide) {
            BlockState fill = Blocks.WOOL.pick(hide == DyeColor.BLACK ? DyeColor.GRAY : hide).defaultBlockState();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx + dz * dz <= 7) put(m.offset(dx, -1, dz), Blocks.COARSE_DIRT.defaultBlockState());
                    for (int dy = 0; dy <= 3; dy++) {
                        double dist = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.8);
                        if (dist >= 3.5 || dist < 2.4) continue;
                        boolean arch = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
                        BlockPos p = m.offset(dx, dy, dz);
                        put(p, arch ? Blocks.BONE_BLOCK.defaultBlockState() : hash(p.getX(), p.getY() * 31 + p.getZ()) % 3 == 0 ? fill : Blocks.SNOW_BLOCK.defaultBlockState());
                    }
                }
            }
            for (int step = 2; step <= 3; step++) { // the doorway toward the fire
                blocks.remove(m.relative(front, step));
                blocks.remove(m.relative(front, step).above());
            }
            put(m, Mechanical.BEAR_RUG.get().defaultBlockState().setValue(BearRugBlock.FACING, front));
            BlockPos back = m.relative(front, -1).relative(front.getClockWise());
            put(back, Blocks.DECORATED_POT.defaultBlockState());
            if (rnd.nextBoolean()) storage.add(back.immutable());
            put(m.relative(front, -1).relative(front.getCounterClockWise()), Mechanical.BONE_PILE.get().defaultBlockState());
        }

        /** Warm: a round hut of mud brick or packed mud (5 across) under a flat roof of packed mud, or of hide for traders. */
        private void mudHut(BlockPos m, Direction front, DyeColor hide) {
            BlockState wall = warmWall();
            BlockState top = tribe.temperament == Tribe.Temperament.TRADERS && age == WorldAge.STONE ? Blocks.WOOL.pick(hide).defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
            int up = age >= WorldAge.BRONZE ? 1 : 0; // Bronze and Iron Age: a course taller
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    BlockPos p = m.offset(dx, 0, dz);
                    if (d > 2.6) continue;
                    put(p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                    put(p.above(2 + up), top);
                    if (d > 1.8) {
                        int ahead = dx * front.getStepX() + dz * front.getStepZ(), across = dx * front.getStepZ() - dz * front.getStepX();
                        if (!(ahead >= 2 && across == 0)) {
                            put(p, wall);
                            put(p.above(), wall);
                        }
                        if (up > 0) put(p.above(2), wall);
                        if ((dx + dz) % 2 == 0) put(p.above(3 + up), wall); // notched rim round the roof
                    }
                }
            }
            put(m, Blocks.CARPET.pick(hide == DyeColor.ORANGE ? DyeColor.BROWN : DyeColor.ORANGE).defaultBlockState());
            BlockPos back = m.relative(front, -1).relative(front.getClockWise());
            put(back, Blocks.DECORATED_POT.defaultBlockState());
            if (rnd.nextBoolean()) storage.add(back.immutable());
        }

        /** Jungle: a hut on stilts (5 by 5) under a hipped roof of leaves rising in rings to a point. */
        private void stiltHut(BlockPos m, Direction front, DyeColor hide) {
            stilts(m, front, 2, 2);
            BlockState leaf = leaves();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int ring = Math.max(Math.abs(dx), Math.abs(dz));
                    put(m.offset(dx, 8 - ring, dz), leaf);
                    if (ring > 0 && ring < 3) put(m.offset(dx, 7 - ring, dz), leaf);
                }
            }
            put(m.above(3), Blocks.CARPET.pick(hide == DyeColor.BROWN ? DyeColor.GREEN : DyeColor.BROWN).defaultBlockState());
            BlockPos back = m.relative(front, -1).relative(front.getClockWise()).above(3);
            put(back, Blocks.DECORATED_POT.defaultBlockState());
            if (rnd.nextBoolean()) storage.add(back.immutable());
        }

        /** A small A-frame hide tent (3x3), open toward the fire. */
        private void tent(BlockPos t, Direction front, DyeColor hide) {
            BlockState wall = Blocks.WOOL.pick(hide).defaultBlockState();
            BlockState slope = Blocks.CARPET.pick(hide == DyeColor.WHITE ? DyeColor.BROWN : DyeColor.LIGHT_GRAY).defaultBlockState();
            Direction side = front.getClockWise();
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos p = t.relative(front, a).relative(side, b);
                    if (b != 0) {
                        put(p, wall);
                        put(p.above(), slope);
                    } else {
                        if (a == -1) put(p, wall);
                        else if (a == 0) put(p, Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState());
                        put(p.above(), wall);
                        if (a != 0) put(p.above(2), fence);
                    }
                }
            }
        }

        /** Bronze and Iron Age: a fenced pen (5x5) in a tent's place, its gate toward the fire, hay inside in temperate lands. */
        private void pen(BlockPos t, Direction front) {
            Direction side = front.getClockWise();
            BlockState gateBlock = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(zone.wood(spruce) + "_fence_gate"))
                    .defaultBlockState().setValue(FenceGateBlock.FACING, front);
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    BlockPos p = t.relative(front, a).relative(side, b);
                    put(p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                    if (Math.abs(a) == 2 || Math.abs(b) == 2) put(p, a == 2 && b == 0 ? gateBlock : fence);
                }
            }
            if (zone == ClimateZone.TEMPERATE) put(t.relative(front, -1), Blocks.HAY_BLOCK.defaultBlockState());
        }

        /** Warm: a hide stretched flat over four acacia poles (3x3), open all round, a mat under it. */
        private void shade(BlockPos t, Direction front, DyeColor hide) {
            Direction side = front.getClockWise();
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos p = t.relative(front, a).relative(side, b);
                    if (Math.abs(a) == 1 && Math.abs(b) == 1) for (int h = 0; h < 2; h++) put(p.above(h), fence);
                    put(p.above(2), Blocks.WOOL.pick(hide).defaultBlockState());
                }
            }
            put(t, Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState());
        }

        /** Jungle: a lean-to (3x3): a back wall of bamboo, two front poles and a roof of leaves, a mat on the floor. */
        private void leanTo(BlockPos t, Direction front, DyeColor hide) {
            Direction side = front.getClockWise();
            BlockState leaf = leaves();
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos p = t.relative(front, a).relative(side, b);
                    if (a == -1) for (int h = 0; h < 2; h++) put(p.above(h), Blocks.BAMBOO_BLOCK.defaultBlockState());
                    else if (a == 1 && Math.abs(b) == 1) for (int h = 0; h < 2; h++) put(p.above(h), fence);
                    put(p.above(2), leaf);
                }
            }
            put(t, Blocks.CARPET.pick(hide).defaultBlockState());
        }

        /** Drying racks with hides between the huts. */
        private void racks() {
            for (double a : new double[] {80, 120, 240, 280}) {
                if (rnd.nextInt(4) == 0) continue;
                BlockPos p = at(gate + Math.toRadians(a), 9);
                put(p, Mechanical.DRYING_RACK.get().defaultBlockState().setValue(DryingRackBlock.FACING, facing(p))
                        .setValue(DryingRackBlock.HIDE, 1 + rnd.nextInt(2)));
            }
        }

        /** Food storage: a cluster of sealed pots and a barrel, with a hay bale (ice, jars or melons in other climates). */
        private void foodStore(BlockPos m) {
            int[][] spots = {{0, 0}, {1, 0}, {0, 1}, {-1, 0}, {1, 1}};
            for (int i = 0; i < spots.length; i++) {
                BlockPos p = m.offset(spots[i][0], 0, spots[i][1]);
                if (i == 4) {
                    put(p, zone.store());
                    continue;
                }
                put(p, i == 0 ? Blocks.BARREL.defaultBlockState() : Blocks.DECORATED_POT.defaultBlockState());
                storage.add(p.immutable());
            }
        }

        /** Role work spots by the wall: the woodcutters' pile and chopping stump, the hunters' fletching and hide rack, the gatherers' compost. */
        private void workplaces() {
            BlockPos wood = at(gate + Math.toRadians(35), 17);
            Direction along = facing(wood).getClockWise();
            for (int i = -1; i <= 1; i++) {
                for (int h = 0; h < 2; h++) put(wood.relative(along, i).above(h), log.setValue(RotatedPillarBlock.AXIS, along.getAxis()));
            }
            put(wood.relative(facing(wood), 2), zone.stump(spruce));
            BlockPos hunt = at(gate - Math.toRadians(35), 17);
            put(hunt, Blocks.FLETCHING_TABLE.defaultBlockState());
            put(hunt.relative(facing(hunt).getClockWise(), 2), Mechanical.DRYING_RACK.get().defaultBlockState()
                    .setValue(DryingRackBlock.FACING, facing(hunt)).setValue(DryingRackBlock.HIDE, 1));
            put(hunt.relative(facing(hunt).getCounterClockWise(), 2), Mechanical.BONE_PILE.get().defaultBlockState());
            BlockPos gather = at(gate + Math.toRadians(200), 18.5);
            put(gather, Blocks.COMPOSTER.defaultBlockState());
            put(gather.relative(facing(gather).getClockWise()), zone.store());
            if (age >= WorldAge.BRONZE) {
                // the potters' clay kiln and a quern; in the Iron Age a smith's anvil and a firebrick hearth beside them
                BlockPos kiln = at(gate + Math.toRadians(165), 17);
                Direction f = facing(kiln);
                BlockState k = Mechanical.KILN.get().defaultBlockState().setValue(KilnBlock.FACING, f);
                put(kiln, k.setValue(KilnBlock.HALF, DoubleBlockHalf.LOWER));
                put(kiln.above(), k.setValue(KilnBlock.HALF, DoubleBlockHalf.UPPER));
                put(kiln.relative(f.getClockWise(), 2), Farming.QUERN.get().defaultBlockState());
                if (age >= WorldAge.IRON) {
                    BlockPos smith = kiln.relative(f.getCounterClockWise(), 2);
                    put(smith, Ironworking.BRONZE_ANVIL.get().defaultBlockState().setValue(BronzeAnvilBlock.FACING, f));
                    put(smith.relative(f.getCounterClockWise()), Smelting.FURNACE_BRICKS.get().defaultBlockState());
                }
            }
        }

        /** The tribe's roles by temperament; everyone lives by the central fire. */
        private List<TribeRole> roles() {
            TribeRole H = TribeRole.HUNTER, G = TribeRole.GATHERER, W = TribeRole.WOODCUTTER, F = TribeRole.FISHER, E = TribeRole.ELDER;
            List<TribeRole> list = new ArrayList<>(List.of(E));
            list.addAll(Arrays.asList(switch (tribe.temperament) {
                case PEACEFUL -> new TribeRole[] {G, G, G, G, W, W, F, F, H};
                case WARY -> new TribeRole[] {H, H, H, H, G, G, W, W, F};
                case RAIDERS -> new TribeRole[] {H, H, H, H, H, W, W, G, G};
                case TRADERS -> new TribeRole[] {E, G, G, G, H, H, W, W, F};
            }));
            int extra = rnd.nextInt(3);
            for (int i = 0; i < extra; i++) list.add(list.get(1 + rnd.nextInt(list.size() - 1)));
            return list;
        }

        void spawnPeople(WorldGenLevel level, RandomSource random) {
            List<TribeRole> roles = roles();
            for (int i = 0; i < roles.size(); i++) {
                TribeRole role = roles.get(i);
                BlockPos p = null;
                for (int tries = 0; tries < 8 && p == null; tries++) {
                    BlockPos q = at(random.nextDouble() * Math.PI * 2, 4.5 + random.nextDouble() * 1.5);
                    if (level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()) p = q;
                }
                if (p == null) p = c.offset(1, 0, 2);
                boolean chief = i == 0;
                StoneAgeFeature.spawn(level, random, People.TRIBESPERSON.get(), p, t -> t.setup(role, c, group, chief, zone));
            }
        }
    }
}
