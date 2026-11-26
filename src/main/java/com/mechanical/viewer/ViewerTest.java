package com.mechanical.viewer;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Mechanical;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for the recipe viewer: what a player has seen, and the backfill for old players. */
@EventBusSubscriber(modid = MODID)
public final class ViewerTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "viewer"));

    static {
        TEST_FUNCTIONS.register("viewer", () -> ViewerTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "viewer"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "viewer"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            seenItems(h);
            stationSources(h);
            toolStationKeyPart(h);
            altOptionCountsAsSeen(h);
            codecRoundTrip(h);
            sourcesRebuildAfterInvalidate(h);
            h.succeed();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static void seenItems(GameTestHelper h) {
        // a FakePlayer, because backfill takes a ServerPlayer
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "viewer"));
        p.getInventory().setItem(0, new ItemStack(Items.FLINT));
        SeenItems.scan(p);
        h.assertTrue(SeenItems.seen(p).contains("minecraft:flint"), "a carried flint is seen");
        h.assertTrue(!SeenItems.seen(p).contains("minecraft:iron_ingot"), "an iron ingot not carried is not seen");

        p.setData(Mechanical.UNLOCKED_TOOLS, List.of("mechanical:bronze_pickaxe"));
        p.getEnderChestInventory().setItem(0, new ItemStack(Items.DIAMOND));
        SeenItems.backfill(p);
        var seen = SeenItems.seen(p);
        h.assertTrue(seen.contains("mechanical:bronze_pickaxe_head"), "backfill: the unlocked tool's head is seen");
        h.assertTrue(seen.contains("minecraft:diamond"), "backfill: the Ender Chest contents are seen");
        h.assertTrue(!seen.contains("mechanical:bronze_pickaxe"), "backfill: never the result");

        p.getEnderChestInventory().setItem(1, new ItemStack(Items.EMERALD));
        SeenItems.backfill(p);
        h.assertTrue(!SeenItems.seen(p).contains("minecraft:emerald"), "a second backfill does nothing (guard)");
    }

    private static void stationSources(GameTestHelper h) {
        var all = StationSources.all();
        for (Station st : List.of(Station.TOOL_STATION, Station.FLETCHING, Station.ARMOR_STATION, Station.LEATHERWORKING, Station.MOLD_BENCH,
                Station.PATTERN_BENCH, Station.KILN, Station.BLOOMERY)) {
            h.assertTrue(all.stream().anyMatch(r -> r.station() == st), "recipes for " + st);
        }
    }

    private static ViewerRecipe bronzePickaxe() {
        return StationSources.all().stream().filter(r -> r.station() == Station.TOOL_STATION && r.unlockId().equals("mechanical:bronze_pickaxe")).findFirst().orElseThrow();
    }

    private static void toolStationKeyPart(GameTestHelper h) {
        var r = bronzePickaxe();
        var seen = java.util.Set.of("mechanical:cordage", "mechanical:tool_rod");
        h.assertTrue(!r.unlocked(seen, List.of()), "no unlock without the recipe id");
        h.assertTrue(r.unlocked(seen, List.of("mechanical:bronze_pickaxe")), "unlocked by the recipe id");
    }

    private static void altOptionCountsAsSeen(GameTestHelper h) {
        var r = new ViewerRecipe(Station.CRAFTING_SHAPELESS, List.of(new Slot(List.of(Items.FLINT, Items.STICK), 1, false)), new ItemStack(Items.TORCH), 0, UnlockKind.ALL_SEEN, "");
        h.assertTrue(r.unlocked(java.util.Set.of("minecraft:stick"), List.of()), "an alternative option counts as seen");
        h.assertTrue(!r.unlocked(java.util.Set.of(), List.of()), "nothing seen is locked");
    }

    private static void codecRoundTrip(GameTestHelper h) {
        var r = bronzePickaxe();
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        ViewerRecipe.CODEC.encode(buf, r);
        var d = ViewerRecipe.CODEC.decode(buf);
        h.assertTrue(d.station() == r.station() && d.inputs().equals(r.inputs()) && ItemStack.matches(d.output(), r.output())
                && d.width() == r.width() && d.unlock() == r.unlock() && d.unlockId().equals(r.unlockId()), "codec round trip");
    }

    private static void sourcesRebuildAfterInvalidate(GameTestHelper h) {
        var before = StationSources.all();
        StationSources.invalidate();
        var after = StationSources.all();
        h.assertTrue(before != after && before.size() == after.size(), "invalidate gives a fresh list of the same size");
    }
}
