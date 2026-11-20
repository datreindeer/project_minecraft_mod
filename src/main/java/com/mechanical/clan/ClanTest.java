package com.mechanical.clan;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
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

/** Game test for tribe votes (2/3 rule, offline counts as no, leadership vote by votes cast), the tribe's age and the world's. */
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
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("clans: " + what);
    }

    /** A tribe of offline players: index 0 leads. */
    private static Clans.Clan tribe(Clans data, UUID[] ids) {
        List<Clans.Member> members = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) members.add(new Clans.Member(ids[i], "p" + i, i == 0 ? Clans.LEADER : Clans.MEMBER, 0, 0));
        Clans.Clan c = new Clans.Clan(UUID.randomUUID(), "t", Optional.of(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO)), members, List.of(), Optional.empty());
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
        h.succeed();
    }
}
