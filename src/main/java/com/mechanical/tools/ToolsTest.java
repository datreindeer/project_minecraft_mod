package com.mechanical.tools;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Smithing;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game tests for the dynamic tools. */
@EventBusSubscriber(modid = MODID)
public final class ToolsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tools"));

    static {
        TEST_FUNCTIONS.register("tools", () -> ToolsTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "tools"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tools"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            rodsExist(h);
            h.succeed();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    /** Each rod is registered and made: bone, copper and bronze by a crafting recipe, steel by the anvil's Tool Rod pattern. */
    private static void rodsExist(GameTestHelper h) {
        var level = h.getLevel();
        Item bronzeIngot = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, "bronze_ingot"));
        for (var c : List.of(
                new Object[] {Smithing.BONE_TOOL_ROD.get(), List.of(com.mechanical.Mechanical.IRON_KNIFE.get(), Items.BONE, Items.BONE)},
                new Object[] {Smithing.COPPER_TOOL_ROD.get(), List.of(Items.COPPER_INGOT, Items.COPPER_INGOT)},
                new Object[] {Smithing.BRONZE_TOOL_ROD.get(), List.of(bronzeIngot, bronzeIngot)})) {
            Item rod = (Item) c[0];
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(rod).getNamespace().equals(MODID), rod + " registered");
            @SuppressWarnings("unchecked")
            List<Item> in = (List<Item>) c[1];
            var input = CraftingInput.of(1, in.size(), in.stream().map(ItemStack::new).toList());
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow(() -> h.assertionException("no recipe for " + rod));
            h.assertTrue(recipe.value().assemble(input).is(rod), "recipe makes " + rod);
            if (in.size() == 3) h.assertTrue(recipe.value().getRemainingItems(input).get(0).is(com.mechanical.Mechanical.IRON_KNIFE.get()), "the knife stays");
        }
        h.assertTrue(Smithing.Pattern.TOOL_ROD.result(new ItemStack(Smithing.HOT_STEEL_INGOT.get())).is(Smithing.STEEL_TOOL_ROD.get()), "a hot Steel Ingot forges a Steel Tool Rod");
    }

    private ToolsTest() {}
}
