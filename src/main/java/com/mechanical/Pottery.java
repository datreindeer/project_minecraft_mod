package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.PotDecorations;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Bronze Age ceramics. Clay Balls are thrown on the {@link PottersWheelBlock} into greenware (Unfired Storage Jar /
 * Unfired Water Jug / Unfired Flower Pot / Unfired Decorated Pot), fired in the Clay Kiln at 600° ({@link KilnBlockEntity#RECIPES}). The {@link StorageJarBlock}
 * keeps food: sealed, it ages at {@link #JAR_RATE} of the normal rate. The {@link WaterJugBlock} holds
 * {@link WaterJugBlock#MAX} pours of water. Data: scripts/gen_pottery.py, textures: scripts/gen_pottery_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Pottery {
    /** Food in a sealed Storage Jar (cool, dark, airtight) ages at this share of the normal rate (Bark Basket: 1/3). */
    public static final float JAR_RATE = 1 / 4F;

    public static final DeferredBlock<PottersWheelBlock> POTTERS_WHEEL = BLOCKS.registerBlock("potters_wheel", PottersWheelBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> POTTERS_WHEEL_ITEM = ITEMS.registerSimpleBlockItem("potters_wheel", POTTERS_WHEEL);
    public static final Supplier<BlockEntityType<PottersWheelBlockEntity>> POTTERS_WHEEL_ENTITY = Mechanical.BLOCK_ENTITIES.register("potters_wheel",
            () -> new BlockEntityType<>(PottersWheelBlockEntity::new, POTTERS_WHEEL.get()));

    public static final DeferredItem<Item> UNFIRED_STORAGE_JAR = ITEMS.registerSimpleItem("unfired_storage_jar");
    public static final DeferredItem<Item> UNFIRED_WATER_JUG = ITEMS.registerSimpleItem("unfired_water_jug");
    /** Fire into vanilla's Flower Pot / Decorated Pot (their crafting recipes are disabled, scripts/gen_pots.py). */
    public static final DeferredItem<Item> UNFIRED_FLOWER_POT = ITEMS.registerSimpleItem("unfired_flower_pot");
    public static final DeferredItem<Item> UNFIRED_DECORATED_POT = ITEMS.registerSimpleItem("unfired_decorated_pot");
    /** Granted from code (criterion "done") on the first sherd pressed into a pot, see scripts/gen_advancements.py. */
    public static final Identifier SHERD_POT_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "bronze_age/sherd_pot");

    public static final DeferredBlock<StorageJarBlock> STORAGE_JAR = BLOCKS.registerBlock("storage_jar", StorageJarBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8F).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final DeferredItem<BlockItem> STORAGE_JAR_ITEM = ITEMS.registerItem("storage_jar",
            p -> new BlockItem(STORAGE_JAR.get(), p), p -> p.useBlockDescriptionPrefix().stacksTo(16));
    public static final Supplier<BlockEntityType<StorageJarBlockEntity>> STORAGE_JAR_ENTITY = Mechanical.BLOCK_ENTITIES.register("storage_jar",
            () -> new BlockEntityType<>(StorageJarBlockEntity::new, STORAGE_JAR.get()));

    public static final DeferredBlock<WaterJugBlock> WATER_JUG = BLOCKS.registerBlock("water_jug", WaterJugBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8F).sound(SoundType.DECORATED_POT).noOcclusion().randomTicks());
    /** One per stack: each carries its own water level (block_state component). */
    public static final DeferredItem<WaterJugItem> WATER_JUG_ITEM = ITEMS.registerItem("water_jug",
            p -> new WaterJugItem(WATER_JUG.get(), p), p -> p.useBlockDescriptionPrefix().stacksTo(1));

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "pots"));

    static {
        TEST_FUNCTIONS.register("pots", () -> Pottery::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    // ---- game test: throw a Decorated Pot, press two sherds, fire it; and the Flower Pot ----

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "pots"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "pots"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 20, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.setBlock(BlockPos.ZERO, POTTERS_WHEEL.get());
        PottersWheelBlockEntity wheel = h.getBlockEntity(BlockPos.ZERO, PottersWheelBlockEntity.class);
        while (wheel.shape() != PottersWheelBlockEntity.Shape.DECORATED_POT) wheel.cycle(level, player);
        while (wheel.addClay(level)) {}
        h.assertTrue(wheel.clay() == 4, "an Unfired Decorated Pot takes 4 Clay Balls");
        h.assertTrue(wheel.pressSherd(level, new ItemStack(Items.ARMS_UP_POTTERY_SHERD)) == null, "no sherds before the pot is thrown");
        while (!wheel.done()) wheel.throwStep(level);
        h.assertTrue("front".equals(wheel.pressSherd(level, new ItemStack(Items.ARMS_UP_POTTERY_SHERD))), "first sherd goes on the front");
        h.assertTrue("left".equals(wheel.pressSherd(level, new ItemStack(Items.SKULL_POTTERY_SHERD))), "second sherd goes on the left");
        ItemStack green = wheel.take();
        h.assertTrue(green.is(UNFIRED_DECORATED_POT.get()), "the wheel gives an Unfired Decorated Pot");
        PotDecorations deco = green.get(DataComponents.POT_DECORATIONS);
        h.assertTrue(deco != null && deco.front().map(t -> t.item().value() == Items.ARMS_UP_POTTERY_SHERD).orElse(false)
                && deco.left().map(t -> t.item().value() == Items.SKULL_POTTERY_SHERD).orElse(false) && deco.back().isEmpty(),
                "the greenware carries the pressed sherds");
        ItemStack fired = KilnBlockEntity.bakeFor(green).result;
        h.assertTrue(fired.is(Items.DECORATED_POT) && deco.equals(fired.get(DataComponents.POT_DECORATIONS)), "the kiln keeps the sherds on the Decorated Pot");
        ItemStack plain = KilnBlockEntity.bakeFor(new ItemStack(UNFIRED_DECORATED_POT.get())).result;
        h.assertTrue(ItemStack.isSameItemSameComponents(plain, new ItemStack(Items.DECORATED_POT)), "a pot without sherds fires into a plain Decorated Pot");
        h.assertTrue(KilnBlockEntity.bakeFor(new ItemStack(UNFIRED_FLOWER_POT.get())).result.is(Items.FLOWER_POT), "an Unfired Flower Pot fires into a Flower Pot");
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.BRICK), ItemStack.EMPTY, new ItemStack(Items.BRICK), ItemStack.EMPTY,
                        new ItemStack(Items.BRICK), ItemStack.EMPTY, new ItemStack(Items.BRICK), ItemStack.EMPTY)), level).isEmpty(),
                "4 Bricks no longer craft a Decorated Pot");
        h.succeed();
    }

    private Pottery() {}
}
