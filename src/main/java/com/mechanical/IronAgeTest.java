package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the Iron Age chain (docs/ages/03-iron-age.md): roasting on a campfire, roasted ore blooming richer than
 * raw ore, flux in the hearth.
 */
@EventBusSubscriber(modid = MODID)
public final class IronAgeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "iron_age"));

    static {
        TEST_FUNCTIONS.register("iron_age", () -> IronAgeTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "iron_age"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "iron_age"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 40, 0, true)));
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
        ServerLevel level = h.getLevel();

        // ---- roasting: a campfire (and so a Fire Pit) roasts both ores
        for (var pair : List.of(List.of(new ItemStack(Items.RAW_IRON), new ItemStack(Ironworking.ROASTED_IRON_ORE.get())),
                List.of(new ItemStack(Ironworking.RAW_BOG_IRON.get()), new ItemStack(Ironworking.ROASTED_BOG_IRON.get())))) {
            var input = new SingleRecipeInput(pair.get(0));
            var roasted = level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level).map(r -> r.value().assemble(input));
            h.assertTrue(roasted.isPresent() && ItemStack.isSameItem(roasted.get(), pair.get(1)), pair.get(0) + " roasts on a campfire");
        }

        // ---- the bloomery: roasted ore blooms full, raw ore poor, flux one ingot more
        BlockPos hearth = new BlockPos(1, 1, 1);
        h.setBlock(hearth, Ironworking.BLOOMERY_HEARTH.get());
        h.setBlock(hearth.above(), Ironworking.BLOOMERY_STACK.get());
        h.setBlock(hearth.above(2), Ironworking.BLOOMERY_STACK.get());
        BloomeryBlockEntity bloomery = (BloomeryBlockEntity) level.getBlockEntity(h.absolutePos(hearth));
        h.assertTrue(bloomery.check(level) == null, "a whole bloomery");
        h.assertTrue(bloom(level, bloomery, new ItemStack(Ironworking.ROASTED_IRON_ORE.get(), 4)) == Ironworking.ROASTED_YIELD, "4 Roasted Iron Ore: a full bloom");
        h.assertTrue(bloom(level, bloomery, new ItemStack(Items.RAW_IRON, 4)) == Ironworking.RAW_YIELD, "4 Raw Iron: a poor bloom");
        h.assertTrue(bloom(level, bloomery, new ItemStack(Ironworking.ROASTED_BOG_IRON.get(), 6)) == Ironworking.ROASTED_YIELD, "6 Roasted Bog Iron: a full bloom");
        h.assertTrue(bloomery.addFlux(), "flux goes in the hearth");
        h.assertTrue(bloom(level, bloomery, new ItemStack(Ironworking.ROASTED_IRON_ORE.get(), 4)) == Ironworking.ROASTED_YIELD + 1, "with flux one ingot more");
        h.assertTrue(bloomery.flux() == 0, "the flux is used up");
        for (int i = 0; i < Ironworking.MAX_FLUX; i++) bloomery.addFlux();
        h.assertFalse(bloomery.addFlux(), "the hearth holds " + Ironworking.MAX_FLUX + " charges");

        // ---- the anvil: a poor bloom gives 1 Wrought Iron and 2 Slag
        BlockPos anvilAt = new BlockPos(4, 1, 1);
        h.setBlock(anvilAt, Ironworking.BRONZE_ANVIL.get());
        BronzeAnvilBlockEntity anvil = (BronzeAnvilBlockEntity) level.getBlockEntity(h.absolutePos(anvilAt));
        Player smith = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hammer = new ItemStack(Ironworking.BRONZE_SMITHING_HAMMER.get());
        smith.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        smith.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BronzeTools.BRONZE_TONGS.get()));
        ItemStack poor = Ironworking.hotBloom(new ItemStack(Ironworking.IRON_BLOOM.get()), level.getGameTime());
        poor.set(Ironworking.BLOOM_YIELD.get(), Ironworking.RAW_YIELD);
        anvil.place(level, poor);
        for (int i = 0; i < Ironworking.HITS; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(dropped(level, h, anvilAt, Ironworking.WROUGHT_IRON_INGOT.get()) == 1 && dropped(level, h, anvilAt, Ironworking.SLAG.get()) == 2,
                "a poor bloom: 1 Wrought Iron, 2 Slag");
        h.succeed();
    }

    /** Smelts {@code ore} into a bloom at 1500° and returns how many ingots it will give (clears the output). */
    private static int bloom(ServerLevel level, BloomeryBlockEntity bloomery, ItemStack ore) {
        bloomery.setItem(BloomeryBlockEntity.INPUT, ore);
        bloomery.setItem(BloomeryBlockEntity.FUEL, new ItemStack(Items.CHARCOAL, 8));
        bloomery.tryLight(level);
        bloomery.forceTemperature(1500);
        for (int i = 0; i < BloomeryBlockEntity.SMELT_TICKS && bloomery.getItem(BloomeryBlockEntity.OUTPUT).isEmpty(); i++) {
            bloomery.forceTemperature(1500);
            BloomeryBlockEntity.serverTick(level, bloomery);
        }
        ItemStack out = bloomery.removeItemNoUpdate(BloomeryBlockEntity.OUTPUT);
        return out.is(Ironworking.HOT_IRON_BLOOM.get()) ? Ironworking.bloomYield(out) : -1;
    }

    static int dropped(ServerLevel level, GameTestHelper h, BlockPos rel, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(2)).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }
}
