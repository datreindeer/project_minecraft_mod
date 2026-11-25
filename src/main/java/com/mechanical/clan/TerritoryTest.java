package com.mechanical.clan;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.FirePit;
import com.mechanical.FirePitBlock;
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
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for tribe territories (Territory, Clans#recomputeTerritory): the 5x5 square, the biome fill on a made-up grid,
 * a small and a big pit next to another tribe (never overlapping, never less than the 5x5), old saves without territory,
 * clearing on a move vote and on disbanding, and 5x5 force-loading.
 */
@EventBusSubscriber(modid = MODID)
public final class TerritoryTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "territory"));

    static {
        TEST_FUNCTIONS.register("territory", () -> TerritoryTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "territory"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "territory"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("territory: " + what);
    }

    /** A one-member tribe with its pit at {@code pit} in the test's level. */
    private static Clans.Clan tribe(GameTestHelper h, Clans data, BlockPos pit) {
        UUID me = UUID.randomUUID();
        Clans.Clan c = new Clans.Clan(UUID.randomUUID(), "t", Optional.of(GlobalPos.of(h.getLevel().dimension(), pit)),
                List.of(new Clans.Member(me, "me", Clans.LEADER, 0, 0)), List.of(), Optional.empty(), Optional.empty());
        data.clans.add(c);
        return c;
    }

    private static int far(ChunkPos from, long c) {
        return Math.max(Math.abs(ChunkPos.getX(c) - from.x()), Math.abs(ChunkPos.getZ(c) - from.z()));
    }

    private static void test(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        var level = h.getLevel();
        check(h, Territory.square(new ChunkPos(0, 0), 2).size() == 25, "5x5 square");

        // fill over a made-up 40x40 grid: a 3-wide strip (x 18..20) of the same biome, from (19, 2)
        ChunkPos start = new ChunkPos(19, 2);
        java.util.function.Predicate<ChunkPos> strip = c -> c.x() >= 18 && c.x() <= 20 && c.z() >= 0 && c.z() < 40;
        Set<Long> filled = Territory.fill(start, Clans.TERRITORY_MAX, strip, c -> false);
        check(h, filled.stream().allMatch(c -> strip.test(ChunkPos.unpack(c))), "fill stays in the strip");
        check(h, filled.stream().allMatch(c -> far(start, c) <= Clans.TERRITORY_MAX), "fill within the cap");
        check(h, filled.size() == 3 * (start.z() + Clans.TERRITORY_MAX + 1), "fill takes the strip from z 0 up to the cap: " + filled.size());
        Set<Long> seen = new HashSet<>(Set.of(start.pack()));
        ArrayDeque<ChunkPos> todo = new ArrayDeque<>(List.of(start));
        while (!todo.isEmpty()) {
            ChunkPos c = todo.poll();
            for (ChunkPos n : List.of(new ChunkPos(c.x() + 1, c.z()), new ChunkPos(c.x() - 1, c.z()), new ChunkPos(c.x(), c.z() + 1), new ChunkPos(c.x(), c.z() - 1)))
                if (filled.contains(n.pack()) && seen.add(n.pack())) todo.add(n);
        }
        check(h, seen.size() == filled.size(), "fill all connected");
        Set<Long> half = Territory.fill(start, Clans.TERRITORY_MAX, strip, c -> c.z() >= 10);
        check(h, !half.isEmpty() && half.stream().noneMatch(c -> ChunkPos.getZ(c) >= 10), "fill never enters blocked chunks");

        // a small pit holds its 5x5
        BlockPos pitPos = h.absolutePos(new BlockPos(2, 1, 2));
        ChunkPos pc = ChunkPos.containing(pitPos);
        level.setBlock(pitPos, FirePit.FIRE_PIT.get().defaultBlockState(), 3);
        Clans data = new Clans(List.of());
        Clans.Clan a = tribe(h, data, pitPos);
        data.recomputeTerritory(server, a);
        check(h, a.territory().size() == 25, "small pit: 25 chunks, got " + a.territory().size());

        // a second tribe 3 chunks east, recomputed after: no chunk in both, the first keeps all 25
        Clans.Clan b = tribe(h, data, pitPos.east(48));
        data.recomputeTerritory(server, b);
        check(h, a.territory().size() == 25 && !b.territory().isEmpty() && b.territory().stream().noneMatch(a.territory()::contains), "two tribes never overlap");
        check(h, data.owner(level.dimension(), pc) == a && data.owner(level.dimension(), new ChunkPos(pc.x() + 4, pc.z())) == b, "owner of a chunk");

        // no overlap: a pit 4 chunks from a lone tribe is blocked by it, 5 chunks is free, and found() refuses and creates nothing
        Clans lone = new Clans(List.of());
        Clans.Clan only = tribe(h, lone, pitPos);
        lone.recomputeTerritory(server, only);
        var dim = level.dimension();
        check(h, lone.blockingTribe(GlobalPos.of(dim, pitPos.east(64))) == only, "pit 4 chunks away is blocked");
        check(h, lone.blockingTribe(GlobalPos.of(dim, pitPos.east(80))) == null, "pit 5 chunks away is free");
        BlockPos near = pitPos.east(64);
        level.setBlock(near, FirePit.FIRE_PIT.get().defaultBlockState(), 3);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(near.getX() + 0.5, near.getY() + 1, near.getZ() + 0.5);
        check(h, !lone.found(player, "newcomers") && lone.clans.size() == 1, "found next to another tribe's land is refused");
        Clans.Clan hopeful = new Clans.Clan(UUID.randomUUID(), "h", Optional.empty(),
                List.of(new Clans.Member(player.getUUID(), "me", Clans.LEADER, 0, 0)), List.of(), Optional.empty(), Optional.empty());
        lone.clans.add(hopeful);
        check(h, !lone.claimPit(player) && hopeful.pit.isEmpty(), "claimPit next to another tribe's land is refused");
        lone.clans.remove(hopeful);
        level.setBlock(near, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

        // a big pit with tribe B 3 chunks east: all its own 5x5 B doesn't hold, none of B's, nothing past the cap
        level.setBlock(pitPos, FirePit.FIRE_PIT.get().defaultBlockState().setValue(FirePitBlock.BIG, true), 2);
        check(h, a.isBigPit(server), "the pit is big");
        data.recomputeTerritory(server, a);
        Set<Long> own = Territory.square(pc, 2);
        own.removeAll(b.territory());
        check(h, a.territory().containsAll(own), "big pit keeps its 5x5");
        check(h, a.territory().stream().noneMatch(b.territory()::contains), "big pit takes nothing of B");
        check(h, a.territory().stream().allMatch(c -> far(pc, c) <= Clans.TERRITORY_MAX), "big pit within the cap");
        level.setBlock(pitPos, FirePit.FIRE_PIT.get().defaultBlockState(), 2);

        // an old save without territory loads empty, keepPitsLoaded fills it in and force-loads 5x5 (not 7x7)
        Clans old = new Clans(List.of());
        var json = Clans.Clan.CODEC.encodeStart(JsonOps.INSTANCE, tribe(h, new Clans(List.of()), pitPos)).getOrThrow();
        json.getAsJsonObject().remove("territory");
        Clans.Clan loaded = Clans.Clan.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        check(h, loaded.territory().isEmpty(), "old save: no territory");
        old.clans.add(loaded);
        old.keepPitsLoaded(server);
        try {
            check(h, loaded.territory().size() == 25, "territory computed on load: " + loaded.territory().size());
            check(h, !Clans.PIT_TICKETS.forceChunk(level, pitPos, pc.x() + 2, pc.z() + 2, true, false), "chunk at 2,2 force-loaded");
            boolean added = Clans.PIT_TICKETS.forceChunk(level, pitPos, pc.x() + 3, pc.z() + 3, true, false);
            if (added) Clans.PIT_TICKETS.forceChunk(level, pitPos, pc.x() + 3, pc.z() + 3, false, false);
            check(h, added, "chunk at 3,3 not force-loaded");
        } finally {
            old.clans.clear();
            old.keepPitsLoaded(server);
        }

        // a passed move vote and disbanding clear the territory at once
        Clans votes = new Clans(List.of());
        Clans.Clan mover = tribe(h, votes, pitPos);
        votes.recomputeTerritory(server, mover);
        UUID leader = mover.members.getFirst().id;
        mover.vote = Optional.of(new Clans.Vote(Clans.VOTE_MOVE, leader, 100, Map.of(leader, true)));
        votes.settle(server, mover, false);
        check(h, mover.territory().isEmpty() && votes.owner(level.dimension(), pc) == null, "move vote clears the territory");
        votes.clans.remove(mover);
        Clans.Clan gone = tribe(h, votes, pitPos);
        votes.recomputeTerritory(server, gone);
        UUID boss = gone.members.getFirst().id;
        gone.vote = Optional.of(new Clans.Vote(Clans.VOTE_DISBAND, boss, 100, Map.of(boss, true)));
        votes.settle(server, gone, false);
        check(h, gone.territory().isEmpty() && votes.owner(level.dimension(), pc) == null, "disbanding clears the territory");
        h.succeed();
    }
}
