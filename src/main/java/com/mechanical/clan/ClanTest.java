package com.mechanical.clan;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.serialization.JsonOps;

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

/** Game test for tribe lands (Lands.landAt), tribe votes (2/3 rule, offline counts as no, leadership vote by votes cast), the tribe's age and the world's, its colour, the Tab list roster, the Fire Pit's force-loading (A2), someone home at a camp (A3), standings with NPC tribes shared by the tribe (A4: average on joining, kept on leaving) and the empty server's tick rate (A1). */
@EventBusSubscriber(modid = MODID)
public final class ClanTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "clans"));

    static {
        TEST_FUNCTIONS.register("clans", () -> ClanTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "clans"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "clans"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("clans: " + what);
    }

    /** A tribe of offline players: index 0 leads. */
    private static Clans.Clan tribe(Clans data, UUID[] ids) {
        List<Clans.Member> members = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) members.add(new Clans.Member(ids[i], "p" + i, i == 0 ? Clans.LEADER : Clans.MEMBER, 0, 0));
        Clans.Clan c = new Clans.Clan(UUID.randomUUID(), "t", Optional.of(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO)), members, List.of(), Optional.empty(), Optional.empty());
        data.clans.add(c);
        return c;
    }

    private static UUID[] three() {
        return new UUID[] {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
    }

    private static void test(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Clans data = new Clans(List.of());

        // disband: 1 of 3 isn't enough, 2 of 3 passes at once
        UUID[] a = three();
        Clans.Clan c = tribe(data, a);
        c.vote = Optional.of(new Clans.Vote(Clans.VOTE_DISBAND, a[0], 100, Map.of(a[0], true)));
        data.settle(server, c, false);
        check(h, c.vote.isPresent() && data.clans.contains(c), "1 of 3 decides nothing");
        c.vote.get().ballots.put(a[1], true);
        data.settle(server, c, false);
        check(h, !data.clans.contains(c), "2 of 3 disbands");

        // move: two no votes make 2/3 impossible, it fails early and the pit stays
        UUID[] b = three();
        c = tribe(data, b);
        c.vote = Optional.of(new Clans.Vote(Clans.VOTE_MOVE, b[0], 100, Map.of(b[0], true, b[1], false, b[2], false)));
        data.settle(server, c, false);
        check(h, c.vote.isEmpty() && c.pit.isPresent(), "move fails with two no");

        // move: one yes and silence until time is up: offline counts as no
        UUID[] d = three();
        c = tribe(data, d);
        c.vote = Optional.of(new Clans.Vote(Clans.VOTE_MOVE, d[0], 100, Map.of(d[0], true)));
        data.settle(server, c, true);
        check(h, c.vote.isEmpty() && c.pit.isPresent(), "silence is no on a big question");

        // leadership: the absent leader doesn't count; 1 yes of 1 cast passes when time is up
        UUID[] e = three();
        c = tribe(data, e);
        c.vote = Optional.of(new Clans.Vote(Clans.VOTE_LEADER, e[1], 100, Map.of(e[1], true)));
        data.settle(server, c, false);
        check(h, c.vote.isPresent(), "1 of 2 voters is not 2/3 yet");
        data.settle(server, c, true);
        check(h, c.member(e[1]).rank == Clans.LEADER && c.member(e[0]).rank == Clans.ELDER, "new leader by votes cast");

        // a lone tribe decides alone
        UUID[] f = {UUID.randomUUID()};
        c = tribe(data, f);
        c.vote = Optional.of(new Clans.Vote(Clans.VOTE_MOVE, f[0], 100, Map.of(f[0], true)));
        data.settle(server, c, false);
        check(h, c.pit.isEmpty(), "lone member moves the pit");

        // age: the latest age root the tribe has
        check(h, c.age() == 0, "stone age by default");
        c.advancements.add(com.mechanical.WorldAge.MARKERS[1]);
        check(h, c.age() == 1, "making bronze starts the bronze age");

        // colour: saved, and a tribe saved before tribes had colours gets one from its id
        Clans.Clan k = tribe(data, three());
        k.colour = 5;
        var json = Clans.Clan.CODEC.encodeStart(JsonOps.INSTANCE, k).getOrThrow();
        check(h, Clans.Clan.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().colour == 5, "colour saved");
        json.getAsJsonObject().remove("colour");
        check(h, Clans.Clan.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().colour == Math.floorMod(k.id.hashCode(), 16), "old tribes get a colour");

        // the Tab list roster: members by rank then name, and it survives the trip to the client
        k.member(k.members.get(2).id).rank = Clans.ELDER;
        List<ClanNetwork.Row> rows = ClanNetwork.rows(server, k, null);
        check(h, rows.get(0).name().equals("p0") && rows.get(1).name().equals("p2") && !rows.get(0).online(), "roster rows by rank");
        ClanNetwork.Roster roster = new ClanNetwork.Roster(1, List.of(new ClanNetwork.Group(k.id, k.name, k.colour, k.age(), rows)), Map.of(UUID.randomUUID(), 2));
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), server.registryAccess());
        ClanNetwork.Roster.CODEC.encode(buf, roster);
        check(h, ClanNetwork.Roster.CODEC.decode(buf).equals(roster), "roster round trip");

        // world age: a high-water mark; NPC tribes lag 0 or 1 behind
        int before = com.mechanical.WorldAge.of(server);
        com.mechanical.WorldAge.set(server, 0);
        com.mechanical.WorldAge.reach(server, 1);
        com.mechanical.WorldAge.reach(server, 0);
        check(h, com.mechanical.WorldAge.of(server) == 1, "world age never drops");
        UUID lagging = null, keeping = null;
        while (lagging == null || keeping == null) {
            UUID t = UUID.randomUUID();
            if (com.mechanical.people.Tribe.of(t).ageLag() == 1) lagging = t; else keeping = t;
        }
        check(h, com.mechanical.WorldAge.npcAge(server, lagging) == 0 && com.mechanical.WorldAge.npcAge(server, keeping) == 1, "NPC tribes 0-1 behind");
        com.mechanical.WorldAge.set(server, before);

        // lands: an NPC camp round its fire, a player tribe's territory, 5x5 chunks round its pit (home for members), nothing far off
        Clans lands = new Clans(List.of());
        BlockPos fire = h.absolutePos(new BlockPos(8, 1, 8));
        UUID group = UUID.randomUUID(), me = UUID.randomUUID();
        var t = h.spawn(com.mechanical.people.People.TRIBESPERSON.get(), new BlockPos(8, 2, 8));
        t.setup(com.mechanical.people.TribeRole.GATHERER, fire, group, false);
        var level = h.getLevel();
        net.minecraft.world.phys.Vec3 near = net.minecraft.world.phys.Vec3.atCenterOf(fire.offset(0, 0, 4)), far = near.add(64, 0, 0);
        Lands.Land land = Lands.landAt(lands, level, me, near);
        check(h, land != null && land.kind() == Lands.TRIBE && group.equals(land.tribe()), "a camp's land round its fire");
        check(h, Lands.landAt(lands, level, me, far) == null, "the wild beyond it");
        t.discard(); // the camp would hold the pit's own chunk back from the territory
        Clans.Clan mine = new Clans.Clan(UUID.randomUUID(), "Hearth", Optional.of(GlobalPos.of(level.dimension(), fire)),
                List.of(new Clans.Member(me, "me", Clans.LEADER, 0, 0)), List.of(), Optional.empty(), Optional.of(3));
        lands.clans.add(mine);
        lands.recomputeTerritory(server, mine);
        net.minecraft.world.phys.Vec3 pitC = net.minecraft.world.phys.Vec3.atCenterOf(fire);
        land = Lands.landAt(lands, level, me, pitC.add(32, 0, 32));
        check(h, land != null && land.kind() == Lands.HOME && land.colour() == 3, "your own tribe's land is home, 2 chunks out each way (5x5)");
        land = Lands.landAt(lands, level, UUID.randomUUID(), pitC.add(16, 0, 16));
        check(h, land != null && land.kind() == Lands.CLAN && land.name().equals("Hearth"), "another player tribe's land, a chunk out");
        check(h, Lands.landAt(lands, level, me, pitC.add(48, 0, 0)) == null, "3 chunks out is the wild");
        check(h, Lands.landAt(lands, level, me, pitC.add(0, 0, -48)) == null, "and the same the other way");

        // a Hermit's Hut: the placed hut names the land round it, also for a hermit from before huts were marked
        com.mechanical.worldgen.HermitsHutTest.check(h, new Clans(List.of()));

        // the server list: the cached status JSON carries our field (MinecraftServerStatusMixin) and the client's codec reads it back
        String statusJson = server.getStatusJson();
        check(h, statusJson != null && statusJson.contains("\"" + ServerListInfo.KEY + "\""), "status JSON has the server list field");
        var status = ServerListInfo.reading(net.minecraft.network.protocol.status.ServerStatus.CODEC)
                .parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(statusJson)).result();
        var info = status.flatMap(net.minecraft.network.protocol.status.ServerStatus::players).map(ServerListInfo::of);
        check(h, info.isPresent() && info.get().shown().size() <= info.get().tribes(), "server list field read back");

        // the Fire Pit force-loads 5x5 chunks while the tribe has it, and lets them go when it hasn't (no leaked tickets)
        Clans camp = new Clans(List.of());
        BlockPos pitPos = h.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pitPos, com.mechanical.FirePit.FIRE_PIT.get().defaultBlockState(), 3);
        Clans.Clan home = new Clans.Clan(UUID.randomUUID(), "Camp", Optional.of(GlobalPos.of(level.dimension(), pitPos)),
                List.of(new Clans.Member(me, "me", Clans.LEADER, 0, 0)), List.of(), Optional.empty(), Optional.empty());
        GlobalPos homePit = home.pit.get();
        camp.clans.add(home);
        camp.keepPitsLoaded(server);
        net.minecraft.world.level.ChunkPos pc = net.minecraft.world.level.ChunkPos.containing(pitPos);
        check(h, Integer.valueOf(2).equals(camp.loaded.get(homePit)), "pit noted as loaded, radius 2");
        // adding a ticket that is already there changes nothing (returns false): all 25 are there
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            check(h, !Clans.PIT_TICKETS.forceChunk(level, pitPos, pc.x() + x, pc.z() + z, true, false), "ticket on chunk " + x + "," + z);
        // A3: a member in the zone is home; a stranger in it or a member outside isn't; a camp that is no tribe's takes anyone
        var member = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        member.setUUID(me);
        member.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pitPos.offset(10, 0, 0)));
        var stranger = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        stranger.setPos(member.position());
        check(h, camp.homeAt(level.dimension(), pitPos, List.of(member)), "member in the zone is home");
        check(h, !camp.homeAt(level.dimension(), pitPos, List.of(stranger)), "a stranger is nobody home");
        member.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pitPos.offset(30, 0, 0)));
        check(h, !camp.homeAt(level.dimension(), pitPos, List.of(member)), "a member outside the zone is nobody home");
        stranger.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pitPos.offset(205, 0, 0)));
        check(h, camp.homeAt(level.dimension(), pitPos.offset(200, 0, 0), List.of(stranger)), "anyone at a camp that is no tribe's");
        // disbanding lets the chunks go
        camp.clans.remove(home);
        camp.keepPitsLoaded(server);
        check(h, camp.loaded.isEmpty(), "disbanded pit no longer loaded");
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            check(h, !Clans.PIT_TICKETS.forceChunk(level, pitPos, pc.x() + x, pc.z() + z, false, false), "ticket released on chunk " + x + "," + z);
        // a broken pit lets them go too, and leaves the tribe without a centre
        camp.clans.add(home);
        camp.keepPitsLoaded(server);
        check(h, camp.loaded.containsKey(homePit), "loaded again");
        level.removeBlock(pitPos, false);
        camp.keepPitsLoaded(server);
        check(h, camp.loaded.isEmpty() && home.pit.isEmpty(), "broken pit let go");
        check(h, !Clans.PIT_TICKETS.forceChunk(level, pitPos, pc.x(), pc.z(), false, false), "broken pit's ticket released");

        // A4: a player tribe shares one standing with each NPC tribe; joining folds yours into the average, leaving keeps it
        Clans real = Clans.get(server);
        UUID npc = UUID.randomUUID();
        com.mechanical.people.Tribe tribe = com.mechanical.people.Tribe.of(npc);
        String key = npc.toString();
        var pa = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pb = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pc2 = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        pa.setData(com.mechanical.people.Tribe.REPUTATION, Map.of(key, 40));
        pb.setData(com.mechanical.people.Tribe.REPUTATION, Map.of(key, 0));
        Clans.Clan shared = new Clans.Clan(UUID.randomUUID(), "Shared", Optional.empty(),
                List.of(new Clans.Member(pa.getUUID(), "a", Clans.LEADER, 0, 0)), List.of(), Optional.empty(), Optional.empty());
        real.clans.add(shared);
        try {
            check(h, tribe.reputation(pa) == 40, "a lone founder keeps their standing");
            shared.members.add(new Clans.Member(pb.getUUID(), "b", Clans.NEWCOMER, 0, 0));
            check(h, tribe.reputation(pb) == 20 && tribe.reputation(pa) == 20, "joining: the average of the members (40, 0 -> 20)");
            tribe.addReputation(pa, 6);
            check(h, tribe.reputation(pb) == 26, "one member's deeds count for all");
            check(h, pb.getData(com.mechanical.people.Tribe.REPUTATION).get(key) == 0, "own standing untouched while in the tribe");
            shared.members.add(new Clans.Member(pc2.getUUID(), "c", Clans.NEWCOMER, 0, 0));
            int expect = Math.round((26 * 2 + tribe.temperament.start) / 3F);
            check(h, tribe.reputation(pc2) == expect, "a newcomer who never met them counts at the tribe's start");
            check(h, com.mechanical.people.Tribe.standings(pa).get(key) == expect, "the Relations tab reads the shared map");
            real.remove(server, shared, pb.getUUID(), "message.mechanical.clan.left");
            check(h, Integer.valueOf(expect).equals(real.leftWith.get(pb.getUUID()).get(key)), "a leaver (offline) keeps the tribe's standing at login");
            check(h, tribe.reputation(pa) == expect, "the others keep the shared standing");
            var saved = Clans.Clan.CODEC.encodeStart(JsonOps.INSTANCE, shared).getOrThrow();
            var back = Clans.Clan.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
            check(h, back.reputation.get(key) == expect && back.pooled.contains(pa.getUUID()) && !back.pooled.contains(pb.getUUID()), "shared standings saved");
        } finally {
            real.clans.remove(shared);
            real.leftWith.remove(pb.getUUID());
        }

        // A1: half the tick rate while empty, back when someone logs in; an op's own rate is left alone
        float n = com.mechanical.EmptyServer.NORMAL, half = com.mechanical.EmptyServer.EMPTY;
        check(h, half == n / 2 && com.mechanical.EmptyServer.target(n, true, false) == half, "empty halves");
        check(h, com.mechanical.EmptyServer.target(half, false, true) == n, "someone on restores");
        check(h, com.mechanical.EmptyServer.target(half, false, false) == half, "an op's 10 TPS stays");
        check(h, com.mechanical.EmptyServer.target(5, true, false) == 5 && com.mechanical.EmptyServer.target(5, false, true) == 5, "an op's other rate stays");
        check(h, com.mechanical.EmptyServer.target(half, true, true) == half, "stays half while empty");
        h.succeed();
    }
}
