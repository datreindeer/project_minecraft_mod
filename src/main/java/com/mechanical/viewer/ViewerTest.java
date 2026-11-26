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
        h.succeed();
    }
}
