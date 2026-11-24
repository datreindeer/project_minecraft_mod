package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.BarkRoofBlock;
import com.mechanical.Foraging;
import com.mechanical.Mechanical;
import com.mechanical.WildBerryBushBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for tribe people and their buildings.
 * <ul>
 * <li>tribe_chop: a woodcutter may fell a natural tree outside the camp, but not a log wall, a stripped pole, a log build
 * under placed leaves, or a natural tree inside the camp; Bark Roof slopes join into stair-like corners.</li>
 * <li>tribe_gate: inside a log ring with a gate on the far side, a gatherer walks out through the gate and picks a bush
 * behind the wall, and a fisher finds a shore beyond the wall.</li>
 * <li>tribe_climate: biomes map to the right climate; village layouts of every climate (16 seeds, so every temperament)
 * keep the gate open and use their climate's materials (no hay outside temperate lands, acacia in warm ones, bamboo
 * in the jungle, spruce and bone in the cold); a settled camp's three stages build in every climate and stay standing.</li>
 * <li>tribe_caravan: a Wandering Trader never joins the world, a Trader Caravan comes instead: 4-5 people of one trading
 * tribe, of a kind (TraderKinds) with goods, the animals led by its handler, pulling a cart or wagon that is still hitched
 * 5 seconds on; when its time is up all of it goes (more in CaravanTest).
 * A trophy cart's carcass is big game (Cave Lion, Aurochs, Brown Bear), the caravan's property (a player can't
 * take or butcher it, and trying angers them), and the trader sells its products for copper.</li>
 * <li>tribe_rebuild (AgeRebuild): village layouts of every climate in the Bronze and Iron Age keep the gate open and use their
 * age's buildings (kiln, quern, pens, no bone huts; anvil and firebrick in the Iron Age); a Stone Age village whose tribe
 * reaches the Bronze Age is swapped to the Bronze layout at once (nobody near), and a settled camp's tents become huts with
 * a kiln, both exactly once, leaving blocks a player put in their way alone.</li>
 * <li>tribe_settle: bands of a wary tribe (no chance to settle) that stay the night are counted at the camp; the 4th settles
 * anyway (count back to 0), and with no site at hand looks for one (SEEKING): it then lets other bands visit, keeps others
 * from settling, is never forgotten while about, and its state and the count survive saving (codecs).</li>
 * <li>tribe_miner: a Miner from his spawn egg is a lone MINER of a trading tribe at the campfire nearby, holding a pickaxe
 * and a torch, unarmed (so he flees monsters), no banner; with no ore round him he sells tin (Cassiterite), torches and
 * food, and buys raw copper and stone: for barter in the Stone Age, for coins once his tribe is in a later age.</li>
 * <li>tribe_swim: a tribesperson swims across a pool in the swimming pose, well faster than mobs crawl through water, and
 * keeps its head above water.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class TribeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> CHOP = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_chop"));
    private static final ResourceKey<Consumer<GameTestHelper>> GATE = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_gate"));
    private static final ResourceKey<Consumer<GameTestHelper>> CLIMATE = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_climate"));
    private static final ResourceKey<Consumer<GameTestHelper>> CARAVAN = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_caravan"));
    private static final ResourceKey<Consumer<GameTestHelper>> REBUILD = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_rebuild"));
    private static final ResourceKey<Consumer<GameTestHelper>> SETTLE = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_settle"));
    private static final ResourceKey<Consumer<GameTestHelper>> MINER = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_miner"));
    private static final ResourceKey<Consumer<GameTestHelper>> SWIM = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_swim"));
    private static final int C = 20, WALL = 6; // the ring's centre (relative x, z) and radius in tribe_gate

    static {
        TEST_FUNCTIONS.register("tribe_chop", () -> TribeTest::chop);
        TEST_FUNCTIONS.register("tribe_gate", () -> TribeTest::gate);
        TEST_FUNCTIONS.register("tribe_climate", () -> TribeTest::climate);
        TEST_FUNCTIONS.register("tribe_caravan", () -> TribeTest::caravan);
        TEST_FUNCTIONS.register("tribe_rebuild", () -> TribeTest::rebuild);
        TEST_FUNCTIONS.register("tribe_settle", () -> TribeTest::settle);
        TEST_FUNCTIONS.register("tribe_swim", () -> TribeTest::swim);
        TEST_FUNCTIONS.register("tribe_miner", () -> TribeTest::miner);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "tribe"), new TestEnvironmentDefinition.AllOf(List.of()));
        // padded so the 41x41 test area stays clear of the other tests
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_chop"), new FunctionGameTestInstance(CHOP,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 40, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_gate"), new FunctionGameTestInstance(GATE,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 2400, 0, true, Rotation.NONE, false, 1, 1, false, 48)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_climate"), new FunctionGameTestInstance(CLIMATE,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 40, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_caravan"), new FunctionGameTestInstance(CARAVAN,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 200, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
        // a whole village (53 across) is built in this one
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_rebuild"), new FunctionGameTestInstance(REBUILD,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 40, 0, true, Rotation.NONE, false, 1, 1, false, 64)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_settle"), new FunctionGameTestInstance(SETTLE,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 40, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_miner"), new FunctionGameTestInstance(MINER,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 40, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_swim"), new FunctionGameTestInstance(SWIM,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 600, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
    }

    private static void guarded(GameTestHelper h, Runnable test) {
        try {
            test.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static Tribesperson person(GameTestHelper h, TribeRole role, BlockPos at, BlockPos home) {
        Tribesperson t = h.spawn(People.TRIBESPERSON.get(), at);
        t.setup(role, h.absolutePos(home), UUID.randomUUID(), false);
        return t;
    }

    /** A column of logs on dirt at x, z; leaves round its top if {@code leaves} (natural, or placed when persistent). */
    private static BlockPos column(GameTestHelper h, int x, int z, BlockState log, BlockState leaves) {
        h.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
        for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), log);
        if (leaves != null) {
            for (Direction d : Direction.Plane.HORIZONTAL) h.setBlock(new BlockPos(x, 4, z).relative(d), leaves);
            h.setBlock(new BlockPos(x, 5, z), leaves);
        }
        return h.absolutePos(new BlockPos(x, 1, z));
    }

    private static void chop(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            BlockState oak = Blocks.OAK_LOG.defaultBlockState();
            BlockState natural = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
            BlockState placed = natural.setValue(LeavesBlock.PERSISTENT, true);
            BlockPos home = new BlockPos(1, 1, 1);
            Tribesperson w = person(h, TribeRole.WOODCUTTER, home.above(), home);
            w.setWall(new Tribesperson.Wall(4, h.absolutePos(new BlockPos(7, 1, 1)))); // camp radius 4 + 2
            h.assertTrue(w.campRadius() == 6, "a wall of radius 4 makes a camp of 6 (got " + w.campRadius() + ")");

            BlockPos tree = column(h, 12, 1, oak, natural);
            BlockPos wall = column(h, 12, 6, oak, null);
            BlockPos pole = column(h, 12, 11, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), null);
            BlockPos build = column(h, 16, 6, oak, placed);
            BlockPos inside = column(h, 4, 1, oak, natural);
            h.assertTrue(ChopWoodGoal.naturalTrunk(level, tree) && ChopWoodGoal.choppable(w, level, tree), "a natural tree outside the camp is felled");
            h.assertFalse(ChopWoodGoal.choppable(w, level, tree.above()), "only from the foot of the trunk");
            h.assertFalse(ChopWoodGoal.choppable(w, level, wall), "a log wall without leaves is left alone");
            h.assertFalse(ChopWoodGoal.choppable(w, level, pole), "a stripped pole is left alone");
            h.assertFalse(ChopWoodGoal.choppable(w, level, build), "a log build under placed leaves is left alone");
            h.assertTrue(ChopWoodGoal.naturalTrunk(level, inside), "the tree inside the camp is natural");
            h.assertFalse(ChopWoodGoal.choppable(w, level, inside), "but it stands inside the camp, so it is left alone");

            // bark roof: a slope rising north with one rising west behind it becomes an outer corner peaking north-west
            BlockState roof = Mechanical.BARK_ROOFS.get("oak").get().defaultBlockState();
            BlockPos a = h.absolutePos(new BlockPos(20, 2, 20));
            level.setBlock(a, roof.setValue(BarkRoofBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            level.setBlock(a.north(), roof.setValue(BarkRoofBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
            h.assertTrue(level.getBlockState(a).getValue(BarkRoofBlock.SHAPE) == StairsShape.OUTER_LEFT,
                    "outer corner (got " + level.getBlockState(a).getValue(BarkRoofBlock.SHAPE) + ")");
            // and one rising north with one rising east in front of it: an inner corner, high north and east
            BlockPos b = h.absolutePos(new BlockPos(24, 2, 20));
            level.setBlock(b, roof.setValue(BarkRoofBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            level.setBlock(b.south(), roof.setValue(BarkRoofBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
            h.assertTrue(level.getBlockState(b).getValue(BarkRoofBlock.SHAPE) == StairsShape.INNER_RIGHT,
                    "inner corner (got " + level.getBlockState(b).getValue(BarkRoofBlock.SHAPE) + ")");
            BlockState flat = roof.setValue(BarkRoofBlock.FLAT, true);
            h.assertTrue(flat.getShape(level, a).max(Direction.Axis.Y) <= 0.125, "a flat bark roof is a thin layer");
            h.succeed();
        });
    }

    private static void gate(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            h.setTime(1000); // morning: people work by day
            // the area reaches past the test's own (1-block) structure: keep its chunks ticking so the people move
            BlockPos lo = h.absolutePos(new BlockPos(-16, 0, -16)), hi = h.absolutePos(new BlockPos(2 * C + 16, 0, 2 * C + 16));
            java.util.List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
            for (int cx = Math.min(lo.getX(), hi.getX()) >> 4; cx <= Math.max(lo.getX(), hi.getX()) >> 4; cx++)
                for (int cz = Math.min(lo.getZ(), hi.getZ()) >> 4; cz <= Math.max(lo.getZ(), hi.getZ()) >> 4; cz++)
                    if (level.setChunkForced(cx, cz, true)) forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
            h.runBeforeTestEnd(() -> forced.forEach(c -> level.setChunkForced(c.x(), c.z(), false)));
            // a grass floor 41x41, clear air above, a ring of logs 3 high round the middle with a 3-wide gate to the east
            for (int x = 0; x <= 2 * C; x++) {
                for (int z = 0; z <= 2 * C; z++) {
                    h.setBlock(new BlockPos(x, -1, z), Blocks.STONE.defaultBlockState());
                    h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    int dx = x - C, dz = z - C;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (Math.abs(d - WALL) < 0.5 && !(dx > 0 && Math.abs(dz) <= 1))
                        for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.OAK_LOG.defaultBlockState());
                }
            }
            BlockPos home = new BlockPos(C, 1, C);
            h.setBlock(home, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            // a ripe bush straight behind the wall on the far side from the gate, and a pond out west
            BlockPos bush = new BlockPos(C - 11, 1, C);
            h.setBlock(bush, Foraging.BUSH_BLOCKS.get("raspberry").get().defaultBlockState());
            for (int x = C - 15; x <= C - 11; x++) for (int z = C + 6; z <= C + 10; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.WATER.defaultBlockState());
            Tribesperson.Wall wall = new Tribesperson.Wall(WALL, h.absolutePos(new BlockPos(C + WALL + 2, 1, C)));

            Tribesperson fisher = person(h, TribeRole.FISHER, home.offset(-2, 0, 0), home);
            fisher.setWall(wall);
            FishGoal fish = new FishGoal(fisher);
            boolean found = false;
            for (int i = 0; i < 200 && !found; i++) found = fish.findSpot(level);
            h.assertTrue(found, "the fisher finds the pond beyond the wall");
            BlockPos stand = h.relativePos(fish.stand);
            double fd = Math.hypot(stand.getX() - C, stand.getZ() - C);
            h.assertTrue(fd > WALL, "the fishing spot is outside the wall (" + fd + " from the fire)");
            fisher.discard();

            Tribesperson gatherer = person(h, TribeRole.GATHERER, home.offset(2, 0, 1), home);
            gatherer.setWall(wall);
            BlockPos bushAbs = h.absolutePos(bush);
            h.succeedWhen(() -> h.assertFalse(WildBerryBushBlock.isRipe(level.getBlockState(bushAbs)),
                    "the gatherer has not picked the bush behind the wall yet (at " + h.relativePos(gatherer.blockPosition()) + ", dark "
                            + level.isDarkOutside() + ", can work " + gatherer.canWork(TribeRole.GATHERER) + ", goals "
                            + gatherer.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList() + ")"));
        });
    }

    private static void climate(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            Object[][] cases = {{Biomes.PLAINS, ClimateZone.TEMPERATE}, {Biomes.FOREST, ClimateZone.TEMPERATE}, {Biomes.TAIGA, ClimateZone.TEMPERATE},
                    {Biomes.SNOWY_PLAINS, ClimateZone.COLD}, {Biomes.SNOWY_TAIGA, ClimateZone.COLD}, {Biomes.ICE_SPIKES, ClimateZone.COLD},
                    {Biomes.GROVE, ClimateZone.COLD}, {Biomes.DESERT, ClimateZone.WARM}, {Biomes.SAVANNA, ClimateZone.WARM},
                    {Biomes.BADLANDS, ClimateZone.WARM}, {Biomes.JUNGLE, ClimateZone.JUNGLE}, {Biomes.BAMBOO_JUNGLE, ClimateZone.JUNGLE},
                    {Biomes.SWAMP, ClimateZone.JUNGLE}, {Biomes.MANGROVE_SWAMP, ClimateZone.JUNGLE}};
            for (Object[] c : cases) {
                @SuppressWarnings("unchecked") ResourceKey<Biome> key = (ResourceKey<Biome>) c[0];
                ClimateZone got = ClimateZone.of(biomes.getOrThrow(key));
                h.assertTrue(got == c[1], key + " should be " + c[1] + ", is " + got);
            }

            // village layouts: every climate, 16 seeds (so every temperament)
            BlockPos centre = new BlockPos(0, 64, 0);
            int wall = com.mechanical.worldgen.TribeVillageStructure.WALL_R;
            for (ClimateZone zone : ClimateZone.values()) {
                java.util.Set<Block> seen = new java.util.HashSet<>();
                for (long seed = 0; seed < 16; seed++) {
                    var plan = com.mechanical.worldgen.TribeVillageStructure.plan(seed * 7919, centre, zone);
                    h.assertTrue(plan.size() > 500, zone + " village " + seed + " has only " + plan.size() + " blocks");
                    double a = com.mechanical.worldgen.TribeVillageStructure.gateAngle(seed * 7919);
                    for (int r = wall - 1; r <= wall + 1; r++) {
                        BlockPos g = centre.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
                        for (int y = 0; y < 3; y++)
                            h.assertTrue(!plan.containsKey(g.above(y)), zone + " village " + seed + ": the gate is blocked at " + g.above(y) + " by " + plan.get(g.above(y)));
                    }
                    for (BlockState b : plan.values()) {
                        seen.add(b.getBlock());
                        if (b.getBlock() instanceof LeavesBlock) h.assertTrue(b.getValue(LeavesBlock.PERSISTENT), zone + ": roof leaves must not decay");
                    }
                }
                switch (zone) {
                    case COLD -> {
                        h.assertTrue(seen.contains(Blocks.SPRUCE_LOG) && seen.contains(People.SHARPENED_SPRUCE_LOG.get()) && seen.contains(Blocks.BONE_BLOCK)
                                && seen.contains(Blocks.SNOW) && seen.contains(Blocks.COBBLESTONE), "cold villages: spruce, bone huts, turf under snow, stone footings");
                        h.assertFalse(seen.contains(Blocks.OAK_LOG) || seen.contains(Blocks.HAY_BLOCK), "no oak or hay in the cold");
                    }
                    case TEMPERATE -> h.assertTrue(seen.contains(Blocks.HAY_BLOCK) && seen.contains(Blocks.OAK_LOG) && seen.contains(Blocks.SPRUCE_LOG)
                            && seen.contains(Mechanical.BARK_ROOFS.get("oak").get()), "temperate villages as before: oak or spruce, bark roofs, hay");
                    case WARM -> {
                        h.assertTrue(seen.contains(Blocks.ACACIA_LOG) && seen.contains(People.SHARPENED_ACACIA_LOG.get()) && seen.contains(Blocks.MUD_BRICKS)
                                && seen.contains(Blocks.PACKED_MUD) && seen.contains(Blocks.ACACIA_FENCE), "warm villages: acacia, mud brick, packed mud, hide shades");
                        h.assertFalse(seen.contains(Blocks.HAY_BLOCK) || seen.contains(Blocks.OAK_LOG) || seen.contains(Blocks.SPRUCE_LOG), "no hay, oak or spruce where it is warm");
                    }
                    case JUNGLE -> {
                        h.assertTrue(seen.contains(Blocks.BAMBOO_BLOCK) && seen.contains(People.SHARPENED_BAMBOO_BLOCK.get()) && seen.contains(Blocks.JUNGLE_LOG)
                                && seen.contains(Blocks.JUNGLE_LEAVES) && seen.contains(Blocks.LADDER) && seen.contains(Blocks.JUNGLE_PLANKS),
                                "jungle villages: bamboo palisade, stilt houses of jungle wood with ladders, leaf roofs");
                        h.assertFalse(seen.contains(Blocks.HAY_BLOCK) || seen.contains(Blocks.OAK_LOG), "no hay or oak in the jungle");
                    }
                }
            }

            // a settled camp's three stages in every climate, built on a grass floor and left standing
            int R = Settlements.PALISADE_R + 2;
            BlockPos mid = new BlockPos(R, 1, R);
            for (ClimateZone zone : ClimateZone.values()) {
                for (int x = 0; x <= 2 * R; x++) {
                    for (int z = 0; z <= 2 * R; z++) {
                        h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
                        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
                Neighbours.Settlement s = new Neighbours.Settlement(UUID.nameUUIDFromBytes(zone.id().getBytes()), h.absolutePos(mid.east(60)),
                        h.absolutePos(mid), Direction.EAST, 0, 0, true, List.of(), List.of(), 0, 0, List.of(), java.util.Optional.empty(), 0, List.of());
                java.util.Set<Block> seen = new java.util.HashSet<>();
                java.util.Arrays.fill(s.stock, 999);
                for (int stage = 1; stage <= 3; stage++) {
                    List<Neighbours.Placement> plan = Settlements.plan(level, s, stage, zone);
                    h.assertTrue(!plan.isEmpty(), zone + " stage " + stage + " builds nothing");
                    if (stage == 3) h.assertTrue(Settlements.cost(plan, 0)[0] > 0, zone + " palisade costs wood");
                    for (Neighbours.Placement p : plan) Settlements.place(level, s, p, null); // as a builder puts them down
                    for (Neighbours.Placement p : plan) {
                        BlockState now = level.getBlockState(p.pos());
                        h.assertTrue(now.getBlock() == p.state().getBlock(), zone + " stage " + stage + ": " + p.state() + " at " + h.relativePos(p.pos()) + " did not stay (" + now + ")");
                        seen.add(now.getBlock());
                    }
                }
                boolean ok = switch (zone) {
                    case COLD -> seen.contains(Blocks.SPRUCE_LOG) && seen.contains(People.SHARPENED_SPRUCE_LOG.get()) && seen.contains(People.HIDE_TENT_SLOPE.get());
                    case TEMPERATE -> seen.contains(Blocks.OAK_LOG) && seen.contains(People.SHARPENED_OAK_LOG.get()) && seen.contains(People.HIDE_TENT_SLOPE.get());
                    case WARM -> seen.contains(Blocks.ACACIA_LOG) && seen.contains(People.SHARPENED_ACACIA_LOG.get()) && seen.contains(Blocks.ACACIA_FENCE)
                            && seen.contains(Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.ORANGE)) && !seen.contains(People.HIDE_TENT_SLOPE.get());
                    case JUNGLE -> seen.contains(Blocks.BAMBOO_BLOCK) && seen.contains(People.SHARPENED_BAMBOO_BLOCK.get()) && seen.contains(Blocks.JUNGLE_LEAVES)
                            && seen.contains(Blocks.JUNGLE_LOG);
                };
                h.assertTrue(ok, zone + " settled camp built of the wrong things: " + seen);
            }
            h.succeed();
        });
    }


    /** A tribe id that lags 0 ages behind the world (so its age is the world's). */
    private static UUID keenTribe(String name) {
        for (int i = 0; ; i++) {
            UUID id = UUID.nameUUIDFromBytes((name + i).getBytes());
            if (Tribe.of(id).ageLag() == 0) return id;
        }
    }

    /** Clears x, z in 0..size (relative) to a grass floor at y 0 with air above. */
    private static void floor(GameTestHelper h, int size, int height) {
        for (int x = 0; x <= size; x++) {
            for (int z = 0; z <= size; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 1; y <= height; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void rebuild(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            var server = level.getServer();
            // layouts of every age: the gate stays open, each age has its own buildings
            BlockPos origin = new BlockPos(0, 64, 0);
            int wall = com.mechanical.worldgen.TribeVillageStructure.WALL_R;
            for (ClimateZone zone : ClimateZone.values()) {
                for (long seed = 0; seed < 16; seed++) {
                    var stone = com.mechanical.worldgen.TribeVillageStructure.plan(seed * 7919, origin, zone, 0);
                    for (int age = 1; age <= 2; age++) {
                        var plan = com.mechanical.worldgen.TribeVillageStructure.plan(seed * 7919, origin, zone, age);
                        double a = com.mechanical.worldgen.TribeVillageStructure.gateAngle(seed * 7919);
                        for (int r = wall - 1; r <= wall + 1; r++) {
                            BlockPos g = origin.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
                            for (int y = 0; y < 3; y++)
                                h.assertTrue(!plan.containsKey(g.above(y)), zone + " age " + age + " village " + seed + ": the gate is blocked at " + g.above(y));
                        }
                        java.util.Set<Block> seen = new java.util.HashSet<>();
                        for (BlockState b : plan.values()) {
                            seen.add(b.getBlock());
                            if (b.getBlock() instanceof LeavesBlock) h.assertTrue(b.getValue(LeavesBlock.PERSISTENT), zone + ": roof leaves must not decay");
                        }
                        h.assertTrue(seen.contains(Mechanical.KILN.get()) && seen.contains(com.mechanical.Farming.QUERN.get()) && !seen.contains(Blocks.BONE_BLOCK), zone + " age " + age + ": kiln and quern, no bone huts: " + seen);
                        if (zone == ClimateZone.TEMPERATE) h.assertTrue(seen.contains(Blocks.PACKED_MUD) && seen.contains(Blocks.HAY_BLOCK), "temperate: daub and thatch");
                        if (zone == ClimateZone.COLD) h.assertFalse(seen.contains(Blocks.HAY_BLOCK) || seen.contains(Blocks.OAK_LOG), "no hay or oak in the cold");
                        h.assertTrue(age < 2 || seen.contains(com.mechanical.Ironworking.BRONZE_ANVIL.get()) && seen.contains(com.mechanical.Smelting.FURNACE_BRICKS.get()),
                                zone + ": an Iron Age village has a smith");
                        h.assertTrue(AgeRebuild.diff(stone, plan, origin.getY()).size() > 100, zone + " age " + age + ": hardly changes");
                    }
                    h.assertTrue(AgeRebuild.diff(stone, stone, origin.getY()).isEmpty(), "the same age changes nothing");
                }
            }

            int before = com.mechanical.WorldAge.of(server);
            try {
                // a Stone Age village (temperate) whose tribe reaches the Bronze Age, swapped at once (no player near)
                int size = 2 * com.mechanical.worldgen.TribeVillageStructure.HALF + 2;
                floor(h, size, 14);
                BlockPos centre = h.absolutePos(new BlockPos(size / 2, 1, size / 2));
                long seed = 0;
                while (Tribe.of(com.mechanical.worldgen.TribeVillageStructure.tribeOf(seed)).ageLag() != 0) seed++;
                var stone = com.mechanical.worldgen.TribeVillageStructure.plan(seed, centre, ClimateZone.TEMPERATE, 0);
                var bronze = com.mechanical.worldgen.TribeVillageStructure.plan(seed, centre, ClimateZone.TEMPERATE, 1);
                stone.forEach((p, st) -> level.setBlock(p, st, Block.UPDATE_CLIENTS));
                // a player's blocks: one in place of a tribe block that goes, one on a spot the Bronze village wants
                BlockPos gone = stone.keySet().stream().filter(p -> !bronze.containsKey(p) && p.getY() > centre.getY()).findFirst().orElseThrow();
                BlockPos wanted = bronze.keySet().stream().filter(p -> !stone.containsKey(p) && p.getY() >= centre.getY()).findFirst().orElseThrow();
                level.setBlock(gone, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(wanted, Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                AgeRebuild.Village v = new AgeRebuild.Village(centre, seed, ClimateZone.TEMPERATE.ordinal(), 0, List.of());
                com.mechanical.WorldAge.set(server, 1);
                h.assertTrue(AgeRebuild.update(level, v), "the village was not rebuilt");
                h.assertTrue(v.age == 1 && v.jobs.isEmpty(), "the village should stand in the Bronze Age, done: age " + v.age + ", " + v.jobs.size() + " jobs left");
                h.assertTrue(level.getBlockState(gone).is(Blocks.GOLD_BLOCK) && level.getBlockState(wanted).is(Blocks.DIAMOND_BLOCK), "a player's blocks were touched");
                for (var e : bronze.entrySet()) {
                    if (e.getKey().equals(wanted)) continue;
                    BlockState now = level.getBlockState(e.getKey());
                    h.assertTrue(now.getBlock() == e.getValue().getBlock(), "village: " + e.getValue() + " at " + h.relativePos(e.getKey()) + " is " + now);
                }
                for (var p : stone.keySet()) {
                    if (!bronze.containsKey(p) && !p.equals(gone) && p.getY() >= centre.getY())
                        h.assertTrue(level.getBlockState(p).isAir(), "village: the old " + stone.get(p) + " at " + h.relativePos(p) + " still stands");
                }
                h.assertFalse(AgeRebuild.update(level, v), "the village was rebuilt twice");

                // a camp built in the Stone Age (all three stages): its tents become huts with a kiln, once
                floor(h, size, 14);
                int R = Settlements.PALISADE_R + 2;
                BlockPos mid = new BlockPos(size / 2, 1, size / 2);
                com.mechanical.WorldAge.set(server, 0);
                Neighbours.Settlement s = new Neighbours.Settlement(keenTribe("rebuild"), h.absolutePos(mid.east(60)), h.absolutePos(mid), Direction.EAST,
                        0, 0, false, List.of(), List.of(), 0, 0, List.of(), java.util.Optional.empty(), 0, List.of());
                java.util.Arrays.fill(s.stock, 999);
                ClimateZone zone = ClimateZone.of(level.getBiome(s.centre));
                java.util.Set<BlockPos> palisade = new java.util.HashSet<>();
                for (int stage = 1; stage <= 3; stage++) {
                    List<Neighbours.Placement> plan = Settlements.plan(level, s, stage, zone);
                    for (Neighbours.Placement p : plan) Settlements.place(level, s, p, null);
                    if (stage == 3) plan.forEach(p -> palisade.add(p.pos()));
                }
                s.stage = 3;
                // a player took a block of the first shelter (east gate: it stands 5 south of the fire) and put their own there
                BlockPos ridge = s.built.stream().map(BlockPos::of).filter(p -> p.getY() == s.centre.getY() + 1 && Math.abs(p.getX() - s.centre.getX()) <= 1
                        && Math.abs(p.getZ() - s.centre.getZ() - 5) <= 1).findFirst().orElseThrow();
                s.built.remove(Long.valueOf(ridge.asLong()));
                level.setBlock(ridge, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                h.assertFalse(AgeRebuild.update(level, s), "a Stone Age camp in the Stone Age was rebuilt");
                com.mechanical.WorldAge.set(server, 1);
                h.assertTrue(AgeRebuild.update(level, s), "the camp was not rebuilt");
                h.assertTrue(s.age == 1 && s.rebuild.isEmpty(), "the camp should stand in the Bronze Age, done: age " + s.age + ", " + s.rebuild.size() + " jobs left");
                h.assertTrue(level.getBlockState(ridge).is(Blocks.GOLD_BLOCK), "the player's block was touched");
                int kilns = 0, huts = 0, tents = 0;
                for (int x = -R; x <= R; x++) {
                    for (int z = -R; z <= R; z++) {
                        for (int y = 0; y <= 4; y++) {
                            BlockState b = level.getBlockState(s.centre.offset(x, y, z));
                            if (b.is(Mechanical.KILN.get())) kilns++;
                            if (b.is(Blocks.PACKED_MUD) || b.is(Blocks.MUD_BRICKS) || b.is(Blocks.BAMBOO_BLOCK)) huts++;
                            if (b.getBlock() instanceof HideTentBlock || b.getBlock() instanceof HideTentBlock.Ridge || b.is(People.HIDE_CANVAS.get())) tents++;
                        }
                    }
                }
                h.assertTrue(kilns == 2 && huts > 10 && tents == 0, "camp in the Bronze Age: kiln halves " + kilns + ", hut walls " + huts + ", tent pieces left " + tents);
                h.assertTrue(level.getBlockState(s.centre).is(Blocks.CAMPFIRE), "the camp's fire went");
                for (BlockPos p : palisade) h.assertTrue(!level.getBlockState(p).isAir(), "the palisade lost a block at " + h.relativePos(p));
                h.assertFalse(AgeRebuild.update(level, s), "the camp was rebuilt twice");
            } finally {
                com.mechanical.WorldAge.set(server, before);
            }
            h.succeed();
        });
    }

    private static void caravan(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, h.absolutePos(new BlockPos(10, 1, 10)));
            var vanilla = net.minecraft.world.entity.EntityTypes.WANDERING_TRADER.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            vanilla.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            h.assertFalse(level.addFreshEntity(vanilla), "the Wandering Trader joined the world");
            // it sets out Caravans.ARRIVE_DIST away and walks in
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(at).inflate(Caravans.ARRIVE_DIST + 8);
            Tribesperson trader = level.getEntitiesOfClass(Tribesperson.class, box, t -> t.role() == TribeRole.TRADER && t.stock != null).stream().findFirst().orElse(null);
            h.assertTrue(trader != null, "no caravan came in the trader's place");
            List<Tribesperson> people = level.getEntitiesOfClass(Tribesperson.class, box, trader::inGroup);
            h.assertTrue(people.stream().allMatch(t -> t.caravanPhase == Caravans.ARRIVING),
                    "the caravan walks in");
            h.assertTrue(people.size() >= 4 && people.size() <= 5 && people.stream().allMatch(trader::inGroup), "caravan of 4-5 of one tribe, got " + people.size());
            h.assertTrue(trader.tribe().temperament == Tribe.Temperament.TRADERS, "a caravan is of a trading tribe");
            h.assertTrue(trader.stock != null && TraderKinds.get(trader.stock.kind) != null, "the caravan is of a kind, with goods");
            Tribesperson handler = people.stream().filter(t -> t.role() == TribeRole.HANDLER).findFirst().orElse(null);
            h.assertTrue(handler != null, "a handler holds the animals");
            List<net.minecraft.world.entity.animal.equine.AbstractHorse> horses = level.getEntitiesOfClass(
                    net.minecraft.world.entity.animal.equine.AbstractHorse.class, box, x -> x.getLeashHolder() == handler);
            h.assertTrue(!horses.isEmpty() && horses.size() <= 2, "1-2 animals led by the handler, got " + horses.size());
            // a trophy cart (forced, behind a horse of its own): big game, the caravan's property, its products on sale
            var horse = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            horse.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(at).add(0, 0, 6));
            horse.setTamed(true);
            level.addFreshEntity(horse);
            horse.setLeashedTo(handler, true);
            com.mechanical.CartEntity cart = com.mechanical.Carting.CART.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            cart.setPos(horse.position().add(0, 0, 2));
            cart.hitchCaravan(horse, false);
            level.addFreshEntity(cart);
            com.mechanical.CarcassEntity body = Caravans.trophy(level, cart, trader, level.getRandom());
            h.assertTrue(body != null && body.getVehicle() == cart && Caravans.TROPHIES.stream().anyMatch(t -> t.get() == body.animal()),
                    "the caravan's carcass is a trophy animal on its cart");
            h.assertTrue(body.isPrepared() && body.isCaravanProperty(), "the trophy is the caravan's property");
            net.minecraft.world.entity.player.Player thief = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            thief.setPos(body.position());
            thief.setShiftKeyDown(true);
            body.interact(thief, net.minecraft.world.InteractionHand.MAIN_HAND, body.position());
            h.assertTrue(body.haulerId() < 0 && body.getVehicle() == cart, "a player can't take the caravan's carcass");
            thief.setShiftKeyDown(false);
            thief.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.mechanical.Mechanical.FLINT_KNIFE.get()));
            h.assertTrue(body.interact(thief, net.minecraft.world.InteractionHand.MAIN_HAND, body.position()) == net.minecraft.world.InteractionResult.FAIL
                    && !thief.isUsingItem(), "a player can't butcher the caravan's carcass");
            h.assertTrue(trader.isAngryAt(thief), "trying angers the caravan as theft");
            var meat = com.mechanical.Butchery.yieldFor(body.animal()).meat().get();
            var goods = trader.stock.deals.stream().filter(d -> d.sale() && d.stock() == 1).toList();
            h.assertTrue(goods.size() >= 2 && goods.stream().anyMatch(d -> d.goods().is(meat)), "the leader sells the trophy's products, got " + goods.size());
            cart.getPassengers().forEach(net.minecraft.world.entity.Entity::discard);
            cart.discard();
            horse.discard();
            h.runAfterDelay(100, () -> guarded(h, () -> {
                List<com.mechanical.TravoisEntity> beds = level.getEntitiesOfClass(com.mechanical.TravoisEntity.class, box.inflate(16), b -> Caravans.owner(b) == handler);
                h.assertTrue(beds.size() == 1, "one cart or wagon, got " + beds.size());
                com.mechanical.TravoisEntity bed = beds.get(0);
                h.assertTrue(bed instanceof com.mechanical.WagonEntity w ? w.isCaravan() && horses.stream().allMatch(w::inTeam)
                        : ((com.mechanical.CartEntity) bed).isCaravan() && horses.contains(bed.harness()), "the caravan's cart or wagon is still hitched");
                List<Tribesperson> all = level.getEntitiesOfClass(Tribesperson.class, box.inflate(16), t -> t.inGroup(trader));
                all.forEach(Caravans::leave);
                h.assertTrue(beds.stream().allMatch(com.mechanical.TravoisEntity::isRemoved)
                        && horses.stream().allMatch(x -> x.isRemoved()), "carts and horses leave with the caravan");
                h.succeed();
            }));
        });
    }

    private static void settle(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            Neighbours data = Neighbours.get(level);
            BlockPos fire = h.absolutePos(new BlockPos(10, 1, 10));
            UUID wary = null;
            for (int i = 0; wary == null; i++) {
                UUID id = UUID.nameUUIDFromBytes(("settle" + i).getBytes());
                if (Tribe.of(id).temperament == Tribe.Temperament.WARY) wary = id; // starts below 0: never settles by chance
            }
            final UUID tribe = wary;
            Neighbours.CampInfo info = new Neighbours.CampInfo(fire, Long.MAX_VALUE, List.of(), -1, 0);
            data.camps.add(info);
            List<Neighbours.Visit> mine = new java.util.ArrayList<>();
            try {
                h.assertTrue(Settlements.canSettleNear(data, info, level.getGameTime()), "nobody has settled by the test camp");
                Neighbours.Visit v = null;
                for (int night = 1; night <= TribeVisits.SURE_SETTLE; night++) {
                    v = new Neighbours.Visit(UUID.randomUUID(), tribe, fire, fire, fire, Neighbours.Visit.STAYING, level.getGameTime(), 0, true, java.util.Optional.empty(), 0);
                    data.visits.add(v);
                    mine.add(v);
                    Tribesperson leader = h.spawn(People.TRIBESPERSON.get(), new BlockPos(12, 1, 10));
                    leader.setup(TribeRole.ELDER, null, tribe, true);
                    h.assertTrue(leader.getMainHandItem().is(com.mechanical.Mechanical.ELDERS_STAFF.get()), "the elder carries an Elder's Staff");
                    leader.visit = v.id;
                    TribeVisits.dawn(level, data, v, leader, List.of(leader), level.getGameTime());
                    if (night < TribeVisits.SURE_SETTLE) {
                        h.assertTrue(v.state == Neighbours.Visit.LEAVING && info.stays == night, "night " + night + ": they leave and are counted (stays " + info.stays + ")");
                        data.visits.remove(v);
                    }
                }
                final Neighbours.Visit seeking = v;
                h.assertTrue(info.stays == 0, "the 4th band settles: the count starts over (got " + info.stays + ")");
                Neighbours.Settlement founded = data.settlement(tribe);
                if (founded != null) { // a site was at hand (not in the test world as a rule): founded at once
                    data.settlements.remove(founded);
                    h.assertFalse(data.visits.contains(seeking), "a band that settled is no longer visiting");
                } else {
                    h.assertTrue(seeking.state == Neighbours.Visit.SEEKING, "no site at hand: the band looks for one (state " + seeking.state + ")");
                    h.assertTrue(data.visitAt(fire) == null, "a band looking for land lets other bands visit");
                    h.assertFalse(Settlements.canSettleNear(data, info, level.getGameTime()), "nobody else settles while a band looks for land");
                    Tribesperson leader = level.getEntitiesOfClass(Tribesperson.class, new net.minecraft.world.phys.AABB(fire).inflate(8),
                            t -> t.isLeader() && seeking.id.equals(t.visit)).get(0);
                    TribeVisits.seek(level, data, seeking, leader, List.of(leader), level.getGameTime() + 6000);
                    h.assertTrue(seeking.state == Neighbours.Visit.SEEKING || data.settlement(tribe) != null, "a seeking band goes on looking");
                    h.assertTrue(seeking.since == level.getGameTime() + 6000, "a seeking band is seen while about, so it is never forgotten");
                    // saved and loaded: still seeking, same spot and tries; the camp keeps its count
                    seeking.tries = 7;
                    info.stays = 2;
                    var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                    Neighbours.Visit back = Neighbours.Visit.CODEC.parse(ops, Neighbours.Visit.CODEC.encodeStart(ops, seeking).getOrThrow()).getOrThrow();
                    Neighbours.CampInfo backInfo = Neighbours.CampInfo.CODEC.parse(ops, Neighbours.CampInfo.CODEC.encodeStart(ops, info).getOrThrow()).getOrThrow();
                    h.assertTrue(back.state == Neighbours.Visit.SEEKING && back.tries == 7 && back.fire.equals(seeking.fire), "a seeking band survives a restart");
                    h.assertTrue(backInfo.stays == 2, "the camp's count survives a restart");
                    Neighbours.Settlement extra = data.settlement(tribe);
                    if (extra != null) data.settlements.remove(extra);
                }
            } finally {
                data.visits.removeAll(mine);
                data.camps.remove(info);
                data.setDirty();
                level.getEntitiesOfClass(Tribesperson.class, new net.minecraft.world.phys.AABB(fire).inflate(16)).forEach(Tribesperson::discard);
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(fire).inflate(16))
                        .forEach(net.minecraft.world.entity.Entity::discard);
            }
            h.succeed();
        });
    }

    private static void miner(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            var server = level.getServer();
            h.setBlock(new BlockPos(5, 1, 5), Blocks.CAMPFIRE.defaultBlockState());
            Tribesperson miner = People.MINER.get().spawn(level, h.absolutePos(new BlockPos(7, 1, 5)), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            h.assertTrue(miner != null && miner.role() == TribeRole.MINER, "the egg makes a Miner");
            h.assertTrue(h.absolutePos(new BlockPos(5, 1, 5)).equals(miner.home()), "he lives at the campfire nearby");
            h.assertTrue(miner.tribe().temperament == Tribe.Temperament.TRADERS && miner.skin() % 2 == 0, "a man of a trading tribe");
            h.assertTrue(miner.getMainHandItem().is(net.minecraft.tags.ItemTags.PICKAXES) && miner.getOffhandItem().is(net.minecraft.world.item.Items.TORCH),
                    "he holds a pickaxe and a torch");
            h.assertFalse(miner.isArmed(), "a pickaxe is no weapon: he flees monsters");
            h.assertTrue(miner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(), "no banner");
            int before = com.mechanical.WorldAge.of(server);
            try {
                for (int age : new int[] {com.mechanical.WorldAge.STONE, com.mechanical.WorldAge.IRON}) {
                    com.mechanical.WorldAge.set(server, age);
                    Tribesperson m = People.MINER.get().spawn(level, h.absolutePos(new BlockPos(9, 1, 9)), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
                    boolean coins = m.age() > com.mechanical.WorldAge.STONE;
                    h.assertTrue(age == com.mechanical.WorldAge.STONE ? !coins : coins, "his tribe's age follows the world (age " + age + ")");
                    var offers = m.getOffers();
                    var tin = offers.stream().filter(o -> o.getResult().is(com.mechanical.BronzeOres.CASSITERITE.get())).findFirst().orElse(null);
                    h.assertTrue(tin != null, "with no tin about he sells Cassiterite");
                    h.assertTrue(Caravans.isCoin(tin.getBaseCostA()) == coins, "tin for " + (coins ? "coins" : "barter") + ", got " + tin.getBaseCostA());
                    h.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(net.minecraft.world.item.Items.TORCH) && !o.getBaseCostA().is(net.minecraft.world.item.Items.RAW_COPPER)),
                            "he sells torches");
                    var copper = offers.stream().filter(o -> o.getBaseCostA().is(net.minecraft.world.item.Items.RAW_COPPER)).findFirst().orElse(null);
                    h.assertTrue(copper != null && (coins ? copper.getResult().is(com.mechanical.Coins.COPPER_COIN.get()) : copper.getResult().is(net.minecraft.world.item.Items.TORCH)),
                            "he buys raw copper");
                    h.assertTrue(offers.stream().anyMatch(o -> o.getBaseCostA().is(net.minecraft.world.item.Items.COBBLESTONE)), "he buys stone");
                    m.discard();
                }
            } finally {
                com.mechanical.WorldAge.set(server, before);
            }
            h.succeed();
        });
    }

    private static void swim(GameTestHelper h) {
        guarded(h, () -> {
            // a pool 3 deep and 24 long, walled in too high to climb out
            floor(h, 28, 6);
            for (int x = 1; x <= 26; x++) {
                for (int z = 1; z <= 9; z++) {
                    boolean wall = x == 1 || x == 26 || z == 1 || z == 9;
                    for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE.defaultBlockState() : y <= 3 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
            // the test area's own chunks may not tick entities: keep the pool's chunks loaded and ticking meanwhile
            ServerLevel level = h.getLevel();
            BlockPos lo = h.absolutePos(new BlockPos(0, 0, 0)), hi = h.absolutePos(new BlockPos(28, 0, 10));
            List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
            for (int cx = Math.min(lo.getX(), hi.getX()) >> 4; cx <= Math.max(lo.getX(), hi.getX()) >> 4; cx++)
                for (int cz = Math.min(lo.getZ(), hi.getZ()) >> 4; cz <= Math.max(lo.getZ(), hi.getZ()) >> 4; cz++)
                    if (level.setChunkForced(cx, cz, true)) forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
            Runnable unforce = () -> forced.forEach(c -> level.setChunkForced(c.x(), c.z(), false));
            Tribesperson t = h.spawn(People.TRIBESPERSON.get(), new BlockPos(3, 2, 5));
            t.setup(TribeRole.GATHERER, null, UUID.randomUUID(), false);
            BlockPos target = h.absolutePos(new BlockPos(24, 3, 5));
            double startX = t.getX();
            boolean[] swamPose = {false};
            boolean[] done = {false};
            h.onEachTick(() -> {
                if (done[0] || t.tickCount == 0) return; // its chunk may start ticking late
                // its own goals (a stroll, a look round) may take over the navigation now and then: keep it on the swim
                if (t.getNavigation().isDone() || !target.equals(t.getNavigation().getTargetPos())) t.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.8);
                if (t.getPose() == net.minecraft.world.entity.Pose.SWIMMING) swamPose[0] = true;
                if (t.tickCount < 140) return;
                done[0] = true;
                unforce.run();
                guarded(h, () -> {
                    double moved = t.getX() - startX;
                    h.assertTrue(moved >= 8, "swam only " + String.format("%.1f", moved) + " blocks in 7 s (at " + h.relativeVec(t.position()) + ", pose " + t.getPose()
                            + ", path " + t.getNavigation().getPath() + ", in water " + t.isInWater() + ")");
                    h.assertTrue(swamPose[0], "never in the swimming pose");
                    h.assertTrue(t.getAirSupply() >= t.getMaxAirSupply() - 20, "kept its head above water (air " + t.getAirSupply() + ")");
                    t.discard();
                    h.succeed();
                });
            });
        });
    }

    private TribeTest() {}
}
