package com.mechanical.dashboard;

import static com.mechanical.Mechanical.MODID;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mechanical.clan.Clans;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for the admin dashboard's JSON (every status key, a tribe's chunks and colour), that an occupied port is a warning, not a crash, and the map tiles' colours, queue cap and tile names. */
@EventBusSubscriber(modid = MODID)
public final class DashboardTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "dashboard"));

    static {
        TEST_FUNCTIONS.register("dashboard", () -> DashboardTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "dashboard"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "dashboard"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("dashboard: " + what);
    }

    private static void test(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        try {
            Dashboard.guard("test", () -> { throw new IllegalStateException("expected by the test"); });
        } catch (RuntimeException e) {
            throw h.assertionException("dashboard: guard let an exception through");
        }

        JsonObject status = JsonParser.parseString(Snapshot.status(server)).getAsJsonObject();
        for (String k : List.of("name", "version", "uptimeSec", "tps", "mspt", "online", "max", "memUsedMb", "memMaxMb", "loadedChunks", "entities",
                "day", "time", "season", "month", "dayOfMonth", "year", "worldAge"))
            check(h, status.has(k), "status has " + k);

        Runnable remove = Clans.testTribeAt(server, GlobalPos.of(Level.OVERWORLD, h.absolutePos(new BlockPos(1, 1, 1))), UUID.randomUUID());
        try {
            JsonArray tribes = JsonParser.parseString(Snapshot.tribes(server)).getAsJsonArray();
            JsonObject t = tribes.get(tribes.size() - 1).getAsJsonObject();
            check(h, t.get("chunks").getAsInt() == 25, "a tribe holds 5x5 chunks");
            check(h, t.get("colour").getAsString().matches("^#[0-9a-f]{6}$"), "colour is #rrggbb");
            JsonObject m = JsonParser.parseString(Snapshot.map(server)).getAsJsonObject();
            JsonArray mt = m.getAsJsonArray("tribes");
            JsonObject mine = mt.get(mt.size() - 1).getAsJsonObject();
            check(h, mine.getAsJsonArray("rings").size() == 1 && mine.getAsJsonArray("rings").get(0).getAsJsonArray().size() == 8, "a 5x5 tribe is one ring of 4 corners");
            int lx = mine.getAsJsonObject("label").get("x").getAsInt() >> 4, lz = mine.getAsJsonObject("label").get("z").getAsInt() >> 4;
            check(h, Clans.get(server).all().stream().anyMatch(c -> c.territory().contains(ChunkPos.pack(lx, lz))), "the label is inside a tribe's land");
            check(h, m.getAsJsonArray("fires").size() >= 1 && m.has("players"), "the map lists the tribe's pit");
        } finally {
            remove.run();
        }

        try (ServerSocket busy = new ServerSocket(0, 1, InetAddress.getByAddress(new byte[] {127, 0, 0, 1}))) {
            Dashboard.bind(busy.getLocalPort()); // must only warn
            check(h, !Dashboard.isRunning(), "an occupied port leaves the dashboard off");
        } catch (java.io.IOException e) {
            throw h.assertionException("dashboard: " + e);
        } finally {
            Dashboard.stop();
        }
        check(h, Dashboard.allowedHost("127.0.0.1:8100", 8100) && Dashboard.allowedHost("localhost:8100", 8100) && Dashboard.allowedHost("[::1]:8100", 8100), "loopback hosts pass");
        check(h, !Dashboard.allowedHost("evil.example:8100", 8100) && !Dashboard.allowedHost(null, 8100) && !Dashboard.allowedHost("127.0.0.1:9999", 8100), "other hosts are refused");
        Set<Long> queue = new LinkedHashSet<>();
        for (int i = 0; i < 100; i++) queue.add((long) i);
        queue.add(0L);
        check(h, queue.size() == 100, "a chunk queued twice is in the queue once");
        check(h, MapTiles.drain(queue, MapTiles.PER_TICK).size() == MapTiles.PER_TICK && queue.size() == 100 - MapTiles.PER_TICK, "one tick reads only PER_TICK chunks");
        check(h, Dashboard.TILE.matcher("/tiles/-3.12.png").matches() && !Dashboard.TILE.matcher("/tiles/../x.png").matches()
                && !Dashboard.TILE.matcher("/tiles/1.2.png/x").matches() && !Dashboard.TILE.matcher("/tiles/1e3.2.png").matches(), "tile names are two signed integers only");
        Set<Long> loaded = new LinkedHashSet<>();
        MapTiles.queueLoaded(h.getLevel(), loaded);
        BlockPos here = h.absolutePos(BlockPos.ZERO);
        check(h, loaded.contains(ChunkPos.pack(here.getX() >> 4, here.getZ() >> 4)), "the map's start queues chunks that are already loaded");
        outlines(h);
        mapColours(h);
        failingWritesStayBounded(h);
        h.succeed();
    }

    private static Set<Long> chunks(int... xz) {
        Set<Long> s = new java.util.HashSet<>();
        for (int i = 0; i < xz.length; i += 2) s.add(ChunkPos.pack(xz[i], xz[i + 1]));
        return s;
    }

    /** One chunk is a square; a ring of 8 has a hole; an L has 6 corners; chunks touching only at a corner stay two rings. */
    private static void outlines(GameTestHelper h) {
        List<int[]> sq = Outline.of(chunks(0, 0));
        check(h, sq.size() == 1 && sq.get(0).length == 8, "one chunk is one ring of 4 corners");
        check(h, Outline.of(chunks(0, 0, 1, 0, 2, 0, 0, 1, 2, 1, 0, 2, 1, 2, 2, 2)).size() == 2, "a 3x3 with the middle missing has an outer ring and a hole");
        List<int[]> l = Outline.of(chunks(0, 0, 0, 1, 1, 1));
        check(h, l.size() == 1 && l.get(0).length == 12, "an L has 6 corners");
        List<int[]> diag = Outline.of(chunks(0, 0, 1, 1));
        check(h, diag.size() == 2 && diag.stream().allMatch(r -> r.length == 8), "chunks touching at a corner are two rings");
    }

    /** A flat grass patch draws grass at normal brightness; water 3 deep draws darker than water 1 deep. */
    private static void mapColours(GameTestHelper h) {
        for (int x = 2; x <= 6; x++) for (int z = 2; z <= 6; z++) h.setBlock(x, 1, z, Blocks.GRASS_BLOCK);
        h.setBlock(10, 0, 4, Blocks.STONE);
        for (int y = 1; y <= 3; y++) h.setBlock(10, y, 4, Blocks.WATER);
        h.setBlock(12, 2, 4, Blocks.STONE);
        h.setBlock(12, 3, 4, Blocks.WATER);
        BlockPos grass = h.absolutePos(new BlockPos(4, 1, 4)), deep = h.absolutePos(new BlockPos(10, 3, 4)), shallow = h.absolutePos(new BlockPos(12, 3, 4));
        check(h, colourAt(h, grass) == Blocks.GRASS_BLOCK.defaultBlockState().getMapColor(h.getLevel(), grass).calculateARGBColor(MapColor.Brightness.NORMAL), "flat grass is the grass map colour");
        check(h, colourAt(h, deep) == MapColor.WATER.calculateARGBColor(MapColor.Brightness.NORMAL), "water 3 deep is the normal water colour");
        check(h, colourAt(h, shallow) == MapColor.WATER.calculateARGBColor(MapColor.Brightness.HIGH), "water 1 deep is the bright water colour");
    }

    /** With every PNG write failing (full disk), exploring many regions keeps at most OPEN_REGIONS + KEPT_FAILED open. The map is off in game tests, so the worker never touches these. */
    private static void failingWritesStayBounded(GameTestHelper h) {
        try {
            MapTiles.folder = Files.createTempDirectory("mechanical_map_test");
            MapTiles.failWrites = true;
            for (int i = 0; i < 40; i++) MapTiles.draw(i * 32, 0, new int[256]);
            check(h, MapTiles.OPEN.size() <= MapTiles.OPEN_REGIONS + MapTiles.KEPT_FAILED, "failed writes keep at most " + (MapTiles.OPEN_REGIONS + MapTiles.KEPT_FAILED)
                    + " regions open, not " + MapTiles.OPEN.size());
        } catch (java.io.IOException e) {
            throw h.assertionException("dashboard: " + e);
        } finally {
            MapTiles.OPEN.clear();
            MapTiles.failWrites = false;
            try {
                if (MapTiles.folder != null) Files.deleteIfExists(MapTiles.folder);
            } catch (java.io.IOException ignored) {
                // temp dir, empty since every write failed
            }
            MapTiles.folder = null;
        }
    }

    private static int colourAt(GameTestHelper h, BlockPos pos) {
        return MapTiles.colours(h.getLevel().getChunkAt(pos))[(pos.getZ() & 15) * 16 + (pos.getX() & 15)];
    }
}
