package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for FoodValues: raw food fills little, cooked more, meals (and Kettle stews) the most; special foods keep vanilla values. */
@EventBusSubscriber(modid = MODID)
public final class FoodValuesTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "food_values"));

    static {
        TEST_FUNCTIONS.register("food_values", () -> FoodValuesTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "food_values"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "food_values"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("food values: " + what);
    }

    private static float saturation(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)).components().get(DataComponents.FOOD).saturation();
    }

    private static void test(GameTestHelper h) {
        check(h, Math.abs(saturation("mechanical:raspberries") - 0.2F) < 1e-4, "raspberries");
        check(h, Math.abs(saturation("minecraft:beef") - 0.6F) < 1e-4, "raw beef");
        check(h, Math.abs(saturation("minecraft:cooked_beef") - 12.8F) < 1e-4, "cooked beef");
        check(h, Math.abs(saturation("mechanical:venison_jerky") - 4.8F) < 1e-4, "jerky");
        check(h, Math.abs(saturation("minecraft:bread") - 12F) < 1e-4, "bread");
        check(h, Math.abs(saturation("mechanical:porridge") - 12F) < 1e-4, "porridge");
        check(h, Math.abs(saturation("minecraft:golden_carrot") - 14.4F) < 1e-3, "golden carrot unchanged");
        var bowl = Kettle.finish(h.getLevel(), List.of(new ItemStack(Items.BEEF), new ItemStack(Items.CARROT), new ItemStack(Items.POTATO),
                new ItemStack(Items.MUTTON))).portion();
        var food = bowl.get(DataComponents.FOOD);
        check(h, food != null && Math.abs(food.saturation() - food.nutrition() * 2 * FoodValues.MEAL) < 1e-4, "stew is a meal: " + food);
        h.succeed();
    }
}
