package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.Coins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for tribe roads (RoadBuilding, docs/living-world-plan.md A5-A9):
 * <ul>
 * <li>road_route: the planned route goes round a wall someone built, and laying the road leaves the wall alone;</li>
 * <li>road_steps: a 3-block rise is cut into, so the road climbs 1 block a cell, each step a stair facing uphill;</li>
 * <li>road_builders: a tribesperson walks the road and lays it cell by cell over time;</li>
 * <li>road_price: free to 500 blocks, 1 Silver per started 50 beyond, double for a trade road, none past 1000; paying
 * takes silver first and changes a Gold Coin;</li>
 * <li>road_window: the force-loaded chunks follow a builder who moves far off, and the old ones are let go.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class RoadTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] NAMES = {"road_route", "road_steps", "road_builders", "road_price", "road_window"};
    private static final int[] TICKS = {20, 20, 600, 20, 40};

    static {
        TEST_FUNCTIONS.register("road_route", () -> RoadTest::route);
        TEST_FUNCTIONS.register("road_steps", () -> RoadTest::steps);
        TEST_FUNCTIONS.register("road_builders", () -> RoadTest::builders);
        TEST_FUNCTIONS.register("road_price", () -> RoadTest::price);
        TEST_FUNCTIONS.register("road_window", () -> RoadTest::window);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "roads"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (int i = 0; i < NAMES.length; i++) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, NAMES[i]));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, NAMES[i]), new FunctionGameTestInstance(key, com.mechanical.TestArea.of(env, TICKS[i], true)));
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static List<BlockPos> plan(GameTestHelper h, BlockPos from, BlockPos to) {
        BlockPos a = h.absolutePos(new BlockPos(0, 0, 0)), b = h.absolutePos(new BlockPos(16, 0, 16));
        return RoadBuilding.plan(h.getLevel(), h.absolutePos(from), h.absolutePos(to), Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()));
    }

    /** A road of this test's own (unique tribe), its cells from {@code path}. */
    private static RoadBuilding.Road road(GameTestHelper h, List<BlockPos> path, int kind) {
        BlockPos end = path.getLast();
        RoadBuilding.Road r = new RoadBuilding.Road(UUID.randomUUID(), UUID.randomUUID(), path.getFirst(), end, kind, List.of(), 0, true, List.of());
        RoadBuilding.append(r, path);
        return r;
    }

    private static void route(GameTestHelper h) {
        floor(h);
        for (int z = 0; z <= 12; z++)
            for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(8, y, z), Blocks.OAK_PLANKS.defaultBlockState());
        List<BlockPos> path = plan(h, new BlockPos(2, 0, 4), new BlockPos(14, 0, 4));
        h.assertTrue(path != null && path.getLast().equals(h.absolutePos(new BlockPos(14, 0, 4))), "route reaches the end: " + path);
        BlockPos wall = h.absolutePos(new BlockPos(8, 0, 0));
        for (BlockPos p : path)
            h.assertTrue(p.getX() != wall.getX() || Math.abs(p.getZ() - wall.getZ()) > 12, "route crosses the wall at " + p);
        RoadBuilding.Road r = road(h, path, RoadBuilding.PATH);
        for (int i = 0; i < r.cells.size(); i++) RoadBuilding.buildCell(h.getLevel(), r, i);
        for (int z = 0; z <= 12; z++)
            for (int y = 1; y <= 2; y++) h.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(8, y, z));
        h.assertTrue(!h.getLevel().getBlockState(h.absolutePos(new BlockPos(2, 0, 4))).is(Blocks.GRASS_BLOCK), "the road was laid");
        h.succeed();
    }

    private static void steps(GameTestHelper h) {
        floor(h);
        for (int x = 8; x <= 16; x++)
            for (int z = 0; z <= 16; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
                h.setBlock(new BlockPos(x, 2, z), Blocks.DIRT.defaultBlockState());
                h.setBlock(new BlockPos(x, 3, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        List<BlockPos> path = plan(h, new BlockPos(2, 0, 8), new BlockPos(14, 0, 8));
        h.assertTrue(path != null && path.size() > 10, "route over the rise: " + path);
        RoadBuilding.Road r = road(h, path, RoadBuilding.PATH);
        ServerLevel level = h.getLevel();
        for (int i = 0; i < r.cells.size(); i++) RoadBuilding.buildCell(level, r, i);
        int stairs = 0;
        for (int i = 1; i < r.cells.size(); i++) {
            BlockPos a = r.cell(i - 1), b = r.cell(i);
            int dy = b.getY() - a.getY();
            h.assertTrue(Math.abs(dy) <= 1, "step of " + dy + " between " + a + " and " + b);
            if (dy == 0) continue;
            BlockPos upper = dy > 0 ? b : a;
            BlockState s = level.getBlockState(upper);
            h.assertTrue(s.getBlock() instanceof StairBlock || s.getBlock() instanceof SlabBlock, "no stair on the step at " + upper + ": " + s);
            if (s.getBlock() instanceof StairBlock) stairs++;
        }
        h.assertTrue(stairs >= 3, "a 3-block rise gets 3 stairs, got " + stairs);
        h.assertTrue(r.cell(r.cells.size() - 1).getY() == h.absolutePos(new BlockPos(0, 3, 0)).getY(), "road ends up on the rise");
        h.succeed();
    }

    private static void builders(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        // not planned yet: the first section is planned from the camp edge (8 blocks out of a 16-block road) once its ground is loaded
        RoadBuilding.Road r = new RoadBuilding.Road(UUID.randomUUID(), UUID.randomUUID(), h.absolutePos(new BlockPos(0, 0, 8)),
                h.absolutePos(new BlockPos(16, 1, 8)), RoadBuilding.PATH, List.of(), 0, false, List.of());
        Tribesperson builder = h.spawn(People.TRIBESPERSON.get(), new BlockPos(2, 1, 6));
        builder.setup(TribeRole.GATHERER, h.absolutePos(new BlockPos(2, 1, 4)), r.tribe, false);
        r.walkers.add(builder.getUUID());
        RoadBuilding data = RoadBuilding.get(level.getServer());
        data.add(r);
        h.assertTrue(r.built == 0, "nothing laid at once");
        h.succeedWhen(() -> {
            if (!r.done() || r.cells.size() < 7) h.fail("planned " + r.planned + ", laid " + r.built + " of " + r.cells.size() + " cells, builder at " + builder.blockPosition() + ", dark " + level.isDarkOutside());
            h.assertTrue(!level.getBlockState(r.cell(r.cells.size() - 1)).is(Blocks.GRASS_BLOCK), "last cell laid");
            data.remove(level, r);
        });
    }

    private static void price(GameTestHelper h) {
        h.assertTrue(RoadBuilding.price(400, RoadBuilding.PATH) == 0 && RoadBuilding.price(500, RoadBuilding.PATH) == 0, "free up to 500");
        h.assertTrue(RoadBuilding.price(501, RoadBuilding.PATH) == 1 && RoadBuilding.price(550, RoadBuilding.PATH) == 1, "1 silver for the first 50 past 500");
        h.assertTrue(RoadBuilding.price(1000, RoadBuilding.PATH) == 10, "10 silver for 1000");
        h.assertTrue(RoadBuilding.price(750, RoadBuilding.TRADE) == 10, "trade road costs double");
        h.assertTrue(RoadBuilding.price(1001, RoadBuilding.PATH) < 0, "nothing past 1000");
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().add(new ItemStack(Coins.SILVER_COIN.get(), 3));
        p.getInventory().add(new ItemStack(Coins.GOLD_COIN.get(), 1));
        h.assertTrue(!RoadBuilding.pay(p, 14), "13 silver's worth can't pay 14");
        h.assertTrue(RoadBuilding.pay(p, 8), "pays 8");
        int silver = p.getInventory().countItem(Coins.SILVER_COIN.get()), gold = p.getInventory().countItem(Coins.GOLD_COIN.get());
        h.assertTrue(silver == 5 && gold == 0, "3 silver + 1 gold - 8 = 5 silver change, got " + silver + " silver " + gold + " gold");
        h.succeed();
    }

    private static void window(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        List<BlockPos> path = List.of(h.absolutePos(new BlockPos(4, 0, 8)), h.absolutePos(new BlockPos(5, 0, 8)));
        RoadBuilding.Road r = road(h, path, RoadBuilding.PATH);
        Tribesperson builder = h.spawn(People.TRIBESPERSON.get(), new BlockPos(8, 1, 8));
        builder.setup(TribeRole.GATHERER, h.absolutePos(new BlockPos(8, 1, 400)), r.tribe, false, ClimateZone.TEMPERATE); // home far off: still out on the road
        r.walkers.add(builder.getUUID());
        RoadBuilding data = RoadBuilding.get(level.getServer());
        data.add(r);
        r.built = r.cells.size(); // done: the window is only round the builder now
        data.tick(level, r);
        ChunkPos here = ChunkPos.containing(builder.blockPosition());
        h.assertTrue(!RoadBuilding.WINDOW.forceChunk(level, r.end, here.x() + 1, here.z() - 1, true, false), "the builder's 3x3 chunks are loaded");
        builder.setPos(builder.getX() + 16, builder.getY(), builder.getZ()); // one chunk east, inside the loaded window as a walk would be
        data.tick(level, r);
        ChunkPos there = ChunkPos.containing(builder.blockPosition());
        h.assertTrue(!RoadBuilding.WINDOW.forceChunk(level, r.end, there.x() + 1, there.z(), true, false), "the window followed the builder to " + there + ": forced " + r.forced.stream().map(ChunkPos::unpack).toList() + ", walkers " + r.walkers + ", found " + level.getEntity(builder.getUUID()));
        h.assertTrue(!RoadBuilding.WINDOW.forceChunk(level, r.end, here.x() - 1, here.z(), false, false), "the old chunks were let go");
        h.assertTrue(r.forced.size() == 9, "one 3x3 window, got " + r.forced.size());
        builder.discard();
        data.remove(level, r);
        h.assertTrue(!RoadBuilding.WINDOW.forceChunk(level, r.end, there.x(), there.z(), false, false), "removing the road lets go of its chunks");
        h.succeed();
    }

    private RoadTest() {}
}
