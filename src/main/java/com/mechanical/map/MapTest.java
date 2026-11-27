package com.mechanical.map;

import static com.mechanical.Mechanical.MODID;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.clan.Clans;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for the in-game map: who sees whose exploring and positions, the masking of tiles, and request limits. */
@EventBusSubscriber(modid = MODID)
public final class MapTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "world_map"));

    static {
        TEST_FUNCTIONS.register("world_map", () -> MapTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "world_map"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "world_map"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    private static void test(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ExploredMap map = ExploredMap.get(server);
        ServerPlayer me = player(h, "map_me"), mate = player(h, "map_mate"), stranger = player(h, "map_stranger");
        UUID offline = UUID.randomUUID();
        // far from anything other tests or the map's own tick could mark, one region each
        int far = 40_000;
        map.explore(me.getUUID(), far * 32 + 16, 16);
        map.explore(mate.getUUID(), (far + 1) * 32 + 16, 16);
        map.explore(offline, (far + 2) * 32 + 16, 16);
        map.explore(stranger.getUUID(), (far + 3) * 32 + 16, 16);

        long[] mine = map.mask(List.of(me.getUUID()), far, 0);
        h.assertTrue(ExploredMap.has(mine, 16 + 6, 16) && ExploredMap.has(mine, 16, 16 - 6) && ExploredMap.has(mine, 16 + 4, 16 + 4), "6 chunks round you are explored");
        h.assertTrue(!ExploredMap.has(mine, 16 + 7, 16) && !ExploredMap.has(mine, 16 + 5, 16 + 5), "farther chunks are not");
        map.explore(me.getUUID(), far * 32 - 1, -1);
        h.assertTrue(ExploredMap.has(map.mask(List.of(me.getUUID()), far - 1, -1), 31, 31) && ExploredMap.has(map.mask(List.of(me.getUUID()), far, 0), 0, 0),
                "exploring spreads across region borders, also at negative chunk coordinates");

        h.assertTrue(sees(server, me, far).equals(List.of(true, false, false, false)), "without a tribe you see only your own");
        List<ServerPlayer> online = List.of(me, mate, stranger);
        h.assertTrue(MapNetwork.people(me, online).mates().isEmpty(), "without a tribe you see nobody");
        Runnable remove = Clans.testTribe(server, me.getUUID(), mate.getUUID(), offline);
        try {
            h.assertTrue(sees(server, me, far).equals(List.of(true, true, true, false)), "a tribe shares exploring, offline mates too, strangers not");
            var people = MapNetwork.people(me, online);
            h.assertTrue(people.mates().size() == 1 && people.mates().getFirst().name().equals("map_mate") && people.colour() >= 0,
                    "only the online tribe mate's position, not yours or a stranger's: " + people);
            h.assertTrue(MapNetwork.people(stranger, online).mates().isEmpty(), "a stranger sees no tribe member");
        } finally {
            remove.run();
        }
        h.assertTrue(sees(server, me, far).equals(List.of(true, false, false, false)), "leaving the tribe takes its knowledge away");

        com.mechanical.dashboard.Landmarks.add(server.overworld(), "ancient_oak", far * 512 + 256, 256);
        com.mechanical.dashboard.Landmarks.add(server.overworld(), "ancient_oak", (far + 3) * 512 + 256, 256);
        String marked = MapNetwork.features(server, me.getUUID());
        h.assertTrue(marked.contains("\"x\":" + (far * 512 + 256)) && !marked.contains("\"x\":" + ((far + 3) * 512 + 256))
                && marked.contains("\"players\":[]"), "the map marks only places on explored land, and no players: " + marked);

        // a region held from before (stamp not 0) that you may no longer see is forgotten; one never seen gets no answer and no disk read
        List<MapNetwork.Tile> forget = new ArrayList<>();
        var jobs = MapNetwork.jobs(server, me.getUUID(), List.of(new MapNetwork.Want(far + 1, 0, 77), new MapNetwork.Want(far + 3, 0, 0)), forget);
        h.assertTrue(jobs.isEmpty() && forget.size() == 1 && forget.getFirst().stamp() == 0 && forget.getFirst().png().length == 0, "unexplored: nothing but forget");

        var saved = ExploredMap.CODEC.encodeStart(NbtOps.INSTANCE, map).getOrThrow();
        ExploredMap loaded = ExploredMap.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        h.assertTrue(java.util.Arrays.equals(loaded.mask(List.of(me.getUUID()), far, 0), map.mask(List.of(me.getUUID()), far, 0)), "explored chunks survive saving");

        masking(h);
        validation(h);
        h.succeed();
    }

    /** Which of the four regions from {@code first} the viewer gets drawn. */
    private static List<Boolean> sees(MinecraftServer server, ServerPlayer viewer, int first) {
        List<MapNetwork.Want> wants = new ArrayList<>();
        for (int i = 0; i < 4; i++) wants.add(new MapNetwork.Want(first + i, 0, 0));
        var jobs = MapNetwork.jobs(server, viewer.getUUID(), wants, new ArrayList<>());
        List<Boolean> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int x = first + i;
            out.add(jobs.stream().anyMatch(j -> j.want().x() == x));
        }
        return out;
    }

    private static void masking(GameTestHelper h) {
        BufferedImage full = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 512; x++) for (int z = 0; z < 512; z++) full.setRGB(x, z, 0xFF336699);
        long[] mask = new long[16];
        int i = 2 * 32 + 3; // chunk 3, 2
        mask[i >> 6] |= 1L << i;
        BufferedImage seen = MapNetwork.masked(full, mask);
        int leaked = 0, kept = 0;
        for (int x = 0; x < 512; x++)
            for (int z = 0; z < 512; z++) {
                boolean inside = x >> 4 == 3 && z >> 4 == 2;
                if (inside && seen.getRGB(x, z) == 0xFF336699) kept++;
                if (!inside && seen.getRGB(x, z) != 0) leaked++;
            }
        h.assertTrue(kept == 256 && leaked == 0, "only the explored chunk's 256 pixels remain: kept " + kept + ", leaked " + leaked);
        h.assertTrue(MapNetwork.masked(new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB), mask) == null, "nothing drawn there is no picture");
    }

    private static void validation(GameTestHelper h) {
        List<MapNetwork.Want> many = new ArrayList<>();
        many.add(new MapNetwork.Want(Integer.MAX_VALUE, 0, 0));
        many.add(new MapNetwork.Want(0, Integer.MIN_VALUE, 0));
        many.add(new MapNetwork.Want(1, 1, 0));
        many.add(new MapNetwork.Want(1, 1, 5));
        for (int i = 0; i < 200; i++) many.add(new MapNetwork.Want(i, -3, 0));
        var valid = MapNetwork.valid(many);
        h.assertTrue(valid.size() == MapNetwork.MAX_WANTS && valid.stream().filter(w -> w.x() == 1 && w.z() == 1).count() == 1
                && valid.stream().allMatch(w -> Math.abs((long) w.x()) <= MapNetwork.MAX_REGION && Math.abs((long) w.z()) <= MapNetwork.MAX_REGION),
                "requests are capped, deduplicated and kept inside the world");
        var limiter = new MapNetwork.Limiter();
        UUID id = UUID.randomUUID();
        h.assertTrue(limiter.accept(id, 100) && !limiter.accept(id, 200), "no second request while one is being drawn");
        limiter.done(id);
        h.assertTrue(!limiter.accept(id, 100 + MapNetwork.MIN_TICKS - 1) && limiter.accept(id, 100 + MapNetwork.MIN_TICKS), "requests are spaced out");
    }
}
