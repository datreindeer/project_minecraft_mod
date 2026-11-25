package com.mechanical.dashboard;

import static com.mechanical.Mechanical.MODID;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
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
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for the admin dashboard's JSON (every status key, a tribe's chunks and colour) and that an occupied port is a warning, not a crash. */
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
        } finally {
            remove.run();
        }

        try (ServerSocket busy = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Dashboard.bind(busy.getLocalPort()); // must only warn
            check(h, !Dashboard.isRunning(), "an occupied port leaves the dashboard off");
        } catch (java.io.IOException e) {
            throw h.assertionException("dashboard: " + e);
        } finally {
            Dashboard.stop();
        }
        check(h, Dashboard.allowedHost("127.0.0.1:8100", 8100) && Dashboard.allowedHost("localhost:8100", 8100) && Dashboard.allowedHost("[::1]:8100", 8100), "loopback hosts pass");
        check(h, !Dashboard.allowedHost("evil.example:8100", 8100) && !Dashboard.allowedHost(null, 8100) && !Dashboard.allowedHost("127.0.0.1:9999", 8100), "other hosts are refused");
        h.succeed();
    }
}
