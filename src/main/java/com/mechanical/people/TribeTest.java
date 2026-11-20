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
 * <li>tribe_caravan: a Wandering Trader never joins the world, a Trader Caravan comes instead: 3-4 people of one trading
 * tribe (one trader, guards armed and armoured without helmets), the trader changes coins and buys diamonds, every horse
 * is led by the trader and pulls a covered cart that is still hitched 5 seconds on; when its time is up all of it goes.
 * A trophy cart's carcass is big game (Giant Deer, Cave Lion, Aurochs, Brown Bear), the caravan's property (a player can't
 * take or butcher it, and trying angers them), and the trader sells its products for copper.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class TribeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> CHOP = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_chop"));
    private static final ResourceKey<Consumer<GameTestHelper>> GATE = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_gate"));
    private static final ResourceKey<Consumer<GameTestHelper>> CLIMATE = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_climate"));
    private static final ResourceKey<Consumer<GameTestHelper>> CARAVAN = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_caravan"));
    private static final int C = 20, WALL = 6; // the ring's centre (relative x, z) and radius in tribe_gate

    static {
        TEST_FUNCTIONS.register("tribe_chop", () -> TribeTest::chop);
        TEST_FUNCTIONS.register("tribe_gate", () -> TribeTest::gate);
        TEST_FUNCTIONS.register("tribe_climate", () -> TribeTest::climate);
        TEST_FUNCTIONS.register("tribe_caravan", () -> TribeTest::caravan);
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
                        h.absolutePos(mid), Direction.EAST, 0, 0, true, List.of(), List.of(), 0, 0, List.of(), java.util.Optional.empty());
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


    private static void caravan(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, h.absolutePos(new BlockPos(10, 1, 10)));
            var vanilla = net.minecraft.world.entity.EntityTypes.WANDERING_TRADER.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            vanilla.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            h.assertFalse(level.addFreshEntity(vanilla), "the Wandering Trader joined the world");
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(at).inflate(16);
            List<Tribesperson> people = level.getEntitiesOfClass(Tribesperson.class, box);
            Tribesperson trader = people.stream().filter(t -> t.role() == TribeRole.TRADER).findFirst().orElse(null);
            h.assertTrue(trader != null, "no caravan came in the trader's place");
            h.assertTrue(people.size() >= 3 && people.size() <= 4 && people.stream().allMatch(trader::inGroup), "caravan of 3-4 of one tribe, got " + people.size());
            h.assertTrue(trader.tribe().temperament == Tribe.Temperament.TRADERS, "a caravan is of a trading tribe");
            for (Tribesperson g : people) {
                if (g == trader) continue;
                h.assertTrue(g.role() == TribeRole.GUARD && g.isArmed(), "guards carry weapons");
                h.assertTrue(g.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()
                        && !g.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty(), "guards wear armour, no helmet or banner");
            }
            var offers = trader.getOffers();
            h.assertTrue(offers.stream().anyMatch(Caravans::isExchange), "the trader changes coins");
            h.assertTrue(offers.stream().anyMatch(o -> o.getBaseCostA().is(net.minecraft.world.item.Items.DIAMOND)
                    && o.getResult().is(com.mechanical.Coins.GOLD_COIN.get())), "the trader buys diamonds for gold");
            List<net.minecraft.world.entity.animal.equine.AbstractHorse> horses = level.getEntitiesOfClass(
                    net.minecraft.world.entity.animal.equine.AbstractHorse.class, box, x -> x.getLeashHolder() == trader);
            h.assertTrue(!horses.isEmpty() && horses.size() <= 2, "1-2 horses led by the trader, got " + horses.size());
            // a trophy cart (forced): big game, the caravan's property, its products on sale
            com.mechanical.CartEntity cart = com.mechanical.Carting.CART.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            cart.setPos(horses.get(0).position());
            cart.hitchCaravan(horses.get(0), false);
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
            var goods = trader.getOffers().stream().filter(o -> o.getMaxUses() == 1 && o.getBaseCostA().is(com.mechanical.Coins.COPPER_COIN.get())).toList();
            h.assertTrue(goods.size() >= 2 && goods.stream().anyMatch(o -> o.getResult().is(meat)), "the trader sells the trophy's products, got " + goods.size());
            cart.getPassengers().forEach(net.minecraft.world.entity.Entity::discard);
            cart.discard();
            h.runAfterDelay(100, () -> guarded(h, () -> {
                List<com.mechanical.CartEntity> carts = level.getEntitiesOfClass(com.mechanical.CartEntity.class, box.inflate(16));
                h.assertTrue(carts.size() == horses.size(), "a cart per horse, got " + carts.size());
                for (com.mechanical.CartEntity c : carts)
                    h.assertTrue(c.isCaravan() && horses.contains(c.harness()), "the caravan's cart is still hitched to its horse");
                List<Tribesperson> all = level.getEntitiesOfClass(Tribesperson.class, box.inflate(16), t -> t.inGroup(trader));
                all.forEach(Caravans::leave);
                h.assertTrue(level.getEntitiesOfClass(com.mechanical.CartEntity.class, box.inflate(16)).isEmpty()
                        && horses.stream().allMatch(x -> x.isRemoved()), "carts and horses leave with the caravan");
                h.succeed();
            }));
        });
    }

    private TribeTest() {}
}
