package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the player's 2x2 grid: 2 Flour (picked at different times) + a Clay Water Bucket makes Dough and the
 * empty Clay Bucket comes back, and the spoil-time merge leaves the flour in its own grid slots.
 */
@EventBusSubscriber(modid = MODID)
public final class DoughTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "dough"));

    static {
        TEST_FUNCTIONS.register("dough", () -> DoughTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "dough"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "dough"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static ItemStack flour(long spoilsAt) {
        ItemStack s = new ItemStack(Farming.FLOUR.get());
        s.set(Preserving.SPOILS_AT.get(), spoilsAt);
        return s;
    }

    /** A menu that only holds the grid: no network sync, so a plain mock player can craft in it. */
    private static AbstractContainerMenu quietMenu() {
        return new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static void run(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        long now = h.getLevel().getGameTime();

        // the spoil-time merge must leave flour in a crafting grid alone
        TransientCraftingContainer merge = new TransientCraftingContainer(quietMenu(), 2, 2);
        merge.setItem(0, flour(now + 720000));
        merge.setItem(1, flour(now + 765000));
        Preserving.mergeAged(List.of(new Slot(merge, 0, 0, 0), new Slot(merge, 1, 0, 0)), p);
        h.assertTrue(merge.getItem(0).getCount() == 1 && merge.getItem(1).getCount() == 1, "flour in the crafting grid stays in its own slots");

        // taking the Dough: same ResultSlot code as the 2x2 grid and the crafting table
        TransientCraftingContainer grid = new TransientCraftingContainer(quietMenu(), 2, 2);
        grid.setItem(0, flour(now + 720000));
        grid.setItem(1, flour(now + 765000));
        grid.setItem(2, new ItemStack(Mechanical.CLAY_WATER_BUCKET.get()));
        var recipe = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), h.getLevel())
                .orElseThrow(() -> h.assertionException("no recipe for 2 Flour + Clay Water Bucket"));
        ItemStack dough = recipe.value().assemble(grid.asCraftInput());
        h.assertTrue(dough.is(Farming.DOUGH.get()), "2 Flour + Clay Water Bucket makes Dough");
        ResultContainer result = new ResultContainer();
        result.setItem(0, dough.copy());
        ResultSlot slot = new ResultSlot(p, grid, result, 0, 0, 0);
        slot.onTake(p, dough);
        ItemStack left = grid.getItem(2);
        h.assertTrue(left.is(Mechanical.CLAY_BUCKET.get()) && left.getDamageValue() == 1,
                "the empty Clay Bucket stays in the grid, 1 pour worn (got " + left + ")");
        h.assertTrue(grid.getItem(0).isEmpty() && grid.getItem(1).isEmpty(), "both flour are used");
        h.succeed();
    }

    private DoughTest() {}
}
