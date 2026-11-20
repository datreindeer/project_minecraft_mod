package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for bark per wood: peeling a Fallen Birch Log drops Birch Bark, recipes that take bark take any wood's
 * (item tag mechanical:barks), 3 Birch Bark make 4 Birch Bark Roof, and the old mechanical:bark id loads as Oak Bark.
 */
@EventBusSubscriber(modid = MODID)
public final class BarkTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "bark"));

    static {
        TEST_FUNCTIONS.register("bark", () -> BarkTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "bark"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "bark"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static ItemStack bark(String wood) {
        return new ItemStack(Mechanical.BARKS.get(wood).get());
    }

    private static TransientCraftingContainer grid(int size, ItemStack... items) {
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, size, size);
        for (int i = 0; i < items.length; i++) grid.setItem(i, items[i]);
        return grid;
    }

    private static ItemStack craft(GameTestHelper h, TransientCraftingContainer grid, String what) {
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), h.getLevel())
                .orElseThrow(() -> h.assertionException("no recipe for " + what)).value().assemble(grid.asCraftInput());
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
        // peeling (stage 0 of a fallen log) drops that log's own bark
        BlockPos rel = new BlockPos(1, 1, 1);
        h.setBlock(rel, Mechanical.FALLEN_LOGS.get("birch").get());
        BlockPos pos = h.absolutePos(rel);
        TreeChopping.finishFallenLog(h.getLevel(), pos, h.getLevel().getBlockState(pos), new ItemStack(Mechanical.FLINT_AXE.get()),
                h.makeMockPlayer(GameType.SURVIVAL));
        h.assertItemEntityPresent(Mechanical.BARKS.get("birch").get(), rel, 2);
        h.assertItemEntityNotPresent(Mechanical.BARKS.get("oak").get(), rel, 2);

        // 3 Birch Bark in a slope -> 4 Birch Bark Roof
        ItemStack roof = craft(h, grid(2, bark("birch"), ItemStack.EMPTY, bark("birch"), bark("birch")), "3 Birch Bark");
        h.assertTrue(roof.is(Mechanical.BARK_ROOFS.get("birch").get().asItem()) && roof.getCount() == 4, "3 Birch Bark make 4 Birch Bark Roof (got " + roof + ")");

        // recipes and uses that took Bark take any wood's (mixed woods in one Bark Basket)
        ItemStack fibre = craft(h, grid(2, bark("yew"), new ItemStack(Mechanical.FLINT_KNIFE.get())), "Yew Bark + knife");
        h.assertTrue(fibre.is(Mechanical.PLANT_FIBRE.get()) && fibre.getCount() == 2, "Yew Bark + knife make 2 Plant Fibre (got " + fibre + ")");
        ItemStack basket = craft(h, grid(3, ItemStack.EMPTY, new ItemStack(Mechanical.CORDAGE.get()), ItemStack.EMPTY,
                bark("birch"), ItemStack.EMPTY, bark("pine"), bark("oak"), bark("mangrove"), bark("cherry")), "a Bark Basket of mixed bark");
        h.assertTrue(basket.is(Mechanical.BARK_BASKET.get()), "mixed bark makes a Bark Basket (got " + basket + ")");
        h.assertTrue(Tinder.isTinder(bark("aspen")), "Aspen Bark is tinder");
        h.assertTrue(Kettle.modeOf(h.getLevel(), bark("alder")) == Kettle.Mode.TAN, "Alder Bark tans in the Bronze Kettle");

        // stripping a standing log with an axe peels its bark (and only an unstripped log of a known wood)
        h.assertTrue("birch".equals(TreeChopping.barkWood(net.minecraft.world.level.block.Blocks.BIRCH_LOG.defaultBlockState())), "a birch log gives birch bark");
        h.assertTrue("oak".equals(TreeChopping.barkWood(net.minecraft.world.level.block.Blocks.OAK_WOOD.defaultBlockState())), "oak wood gives oak bark");
        h.assertTrue(TreeChopping.barkWood(net.minecraft.world.level.block.Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()) == null, "a stripped log has no bark");
        h.assertTrue(TreeChopping.barkWood(net.minecraft.world.level.block.Blocks.CRIMSON_STEM.defaultBlockState()) == null, "a nether stem has no bark");
        BlockPos logAt = new BlockPos(3, 1, 3);
        h.setBlock(logAt, net.minecraft.world.level.block.Blocks.BIRCH_LOG);
        net.minecraft.world.entity.player.Player stripper = h.makeMockPlayer(GameType.SURVIVAL);
        stripper.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STONE_AXE));
        h.useBlock(logAt, stripper);
        h.assertBlockPresent(net.minecraft.world.level.block.Blocks.STRIPPED_BIRCH_LOG, logAt);
        h.assertItemEntityPresent(Mechanical.BARKS.get("birch").get(), logAt, 2);

        // the single Bark of older versions loads as Oak Bark
        h.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, "bark")) == Mechanical.BARKS.get("oak").get(),
                "mechanical:bark is an alias of Oak Bark");
        h.succeed();
    }

    private BarkTest() {}
}
