package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the faster farming (FarmTools, Farming growth constants): the growth rate factors, crops ripening about
 * twice vanilla's speed (measured through CropBlock.randomTick when the test world's weather and season let them grow),
 * the seed bags sowing 3x3 / 5x5 with one seed each, and the Bronze Ard tilling 3x3 and wearing 1 per block.
 */
@EventBusSubscriber(modid = MODID)
public final class FarmToolsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "farm_tools"));

    static {
        TEST_FUNCTIONS.register("farm_tools", () -> FarmToolsTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "farm_tools"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "farm_tools"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 20, true)));
    }

    private static void useOn(GameTestHelper h, Player p, BlockPos rel, ItemStack held) {
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos abs = h.absolutePos(rel);
        held.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
    }

    private static ItemStack bag(net.minecraft.world.item.Item bag, ItemStack... seeds) {
        ItemStack stack = new ItemStack(bag);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(seeds)));
        return stack;
    }

    private static int countIn(ItemStack bag, net.minecraft.world.item.Item item) {
        return bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void test(GameTestHelper h) {
        // growth factors: 2x vanilla on new farmland in the best spring temperature, 1.5x more on rich soil, nothing in winter
        Farming.Climate wheat = Farming.climate(Blocks.WHEAT);
        h.assertTrue(Math.abs(Farming.growthRate(wheat, 12, 0, 2) - Farming.GROWTH_SPEED) < 1e-4, "new field, spring: rate " + Farming.growthRate(wheat, 12, 0, 2));
        h.assertTrue(Math.abs(Farming.growthRate(wheat, 12, 0, 3) - Farming.GROWTH_SPEED * 1.5F) < 1e-4, "rich soil 1.5x: " + Farming.growthRate(wheat, 12, 0, 3));
        h.assertTrue(Math.abs(Farming.growthRate(wheat, 12, 0, 0) - Farming.GROWTH_SPEED * 0.5F) < 1e-4, "poor soil half: " + Farming.growthRate(wheat, 12, 0, 0));
        h.assertTrue(Farming.growthRate(wheat, 12, 3, 3) == 0, "winter rests");
        h.assertTrue(Farming.stakeFactor(Blocks.FARMLAND.defaultBlockState().setValue(Fertility.STAKED, true)) == 1.25F
                && Farming.stakeFactor(Blocks.FARMLAND.defaultBlockState()) == 1, "crop stakes grow 25% faster");
        RandomSource r = RandomSource.create(42);
        int sum = 0;
        for (int i = 0; i < 20000; i++) sum += Farming.stagesFor(2.5F, r);
        h.assertTrue(Math.abs(sum / 20000.0 - 2.5) < 0.05, "2.5x rate gives 2.5 stages a step on average: " + sum / 20000.0);

        measureRipening(h);

        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
        // Seed Bag: 3x3 farmland, the middle already sown: 8 seeds go in, wheat first, then peas
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, 1, z), farmland);
        h.setBlock(new BlockPos(1, 2, 1), Blocks.WHEAT.defaultBlockState());
        h.setBlock(new BlockPos(3, 1, 1), farmland); // outside the 3x3: stays bare
        ItemStack seedBag = bag(FarmTools.SEED_BAG.get(), new ItemStack(Items.WHEAT_SEEDS, 5), new ItemStack(Farming.PEAS.get(), 10));
        useOn(h, p, new BlockPos(1, 2, 1), seedBag); // clicking the crop sows around its farmland
        int sown = 0;
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) if (h.getBlockState(new BlockPos(x, 2, z)).getBlock() instanceof CropBlock) sown++;
        h.assertTrue(sown == 9, "seed bag sows 3x3: " + sown + " crops");
        h.assertTrue(countIn(seedBag, Items.WHEAT_SEEDS) == 0 && countIn(seedBag, Farming.PEAS.get()) == 7,
                "one seed per block: " + countIn(seedBag, Items.WHEAT_SEEDS) + " wheat, " + countIn(seedBag, Farming.PEAS.get()) + " peas left");
        h.assertTrue(h.getBlockState(new BlockPos(3, 2, 1)).isAir(), "outside the 3x3 stays bare");
        h.assertTrue(FarmTools.SEEDS.test(new ItemStack(Spinning.FLAX_SEEDS.get())) && FarmTools.SEEDS.test(new ItemStack(Items.CARROT))
                && FarmTools.SEEDS.test(new ItemStack(Items.MELON_SEEDS)) && !FarmTools.SEEDS.test(new ItemStack(Items.DIRT)), "what fits in a seed bag");

        // Linen Seed Bag: 5x5
        for (int x = 0; x < 5; x++) for (int z = 4; z < 9; z++) h.setBlock(new BlockPos(x, 1, z), farmland);
        ItemStack linen = bag(FarmTools.LINEN_SEED_BAG.get(), new ItemStack(Farming.GRAIN_ITEMS.get("einkorn").get(), 30));
        useOn(h, p, new BlockPos(2, 1, 6), linen);
        h.assertTrue(countIn(linen, Farming.GRAIN_ITEMS.get("einkorn").get()) == 5, "linen bag sows 5x5: " + countIn(linen, Farming.GRAIN_ITEMS.get("einkorn").get()) + " left of 30");

        // Bronze Ard: 3x3 dirt and grass, one covered by a stone: 8 tilled, 8 wear
        for (int x = 6; x < 9; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, 1, z), (x + z) % 2 == 0 ? Blocks.DIRT : Blocks.GRASS_BLOCK);
        h.setBlock(new BlockPos(8, 2, 2), Blocks.STONE);
        ItemStack ard = new ItemStack(FarmTools.BRONZE_ARD.get());
        h.assertTrue(ard.getMaxDamage() == FarmTools.ARD_DURABILITY, "ard durability " + ard.getMaxDamage());
        useOn(h, p, new BlockPos(7, 1, 1), ard);
        int tilled = 0;
        for (int x = 6; x < 9; x++) for (int z = 0; z < 3; z++) if (h.getBlockState(new BlockPos(x, 1, z)).is(Blocks.FARMLAND)) tilled++;
        h.assertTrue(tilled == 8 && !h.getBlockState(new BlockPos(8, 1, 2)).is(Blocks.FARMLAND), "ard tills 3x3 but not under a block: " + tilled);
        h.assertTrue(ard.getDamageValue() == 8, "ard wears 1 per tilled block: " + ard.getDamageValue());
        // sneaking: just the one
        for (int x = 10; x < 13; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
        p.setShiftKeyDown(true);
        useOn(h, p, new BlockPos(11, 1, 1), ard);
        p.setShiftKeyDown(false);
        h.assertTrue(h.getBlockState(new BlockPos(11, 1, 1)).is(Blocks.FARMLAND) && h.getBlockState(new BlockPos(10, 1, 1)).is(Blocks.DIRT)
                && ard.getDamageValue() == 9, "sneaking tills one");
        h.succeed();
    }

    /**
     * Wheat on 3x3 moist farmland (vanilla: 1 in 3 random ticks grows a stage there, 21 ticks to ripen) ripened 200 times
     * through the real randomTick, on new (2) and rich (3) soil. Only when nothing blocks growth here and now (season,
     * night, temperature), else it logs why and skips.
     */
    private static void measureRipening(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(16, 2, 4));
        // the test world may be too warm for wheat: go up a mountain, the air cools 0.05 a block above y 100 (Temperature.ambient)
        float warm = Temperature.ambient(level, base) - 12;
        BlockPos abs = warm > 1 ? new BlockPos(base.getX(), 100 + Math.round(warm / 0.05F), base.getZ()) : base;
        if (abs.getY() > level.getMaxY() - 2) {
            Mechanical.LOGGER.info("farm_tools: ripening not measured, too warm here ({})", warm + 12);
            return;
        }
        Farming.Climate c = Farming.climate(Blocks.WHEAT);
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        double[] avg = new double[2];
        try {
            for (int f = 2; f <= 3; f++) {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                    level.setBlock(abs.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7).setValue(Fertility.FERTILITY, f), 2);
                level.setBlock(abs, wheat.getStateForAge(0), 2);
                float rate = Farming.growthRate(level, abs, c);
                if (rate == 0) {
                    Mechanical.LOGGER.info("farm_tools: ripening not measured, wheat can't grow here now (temp {}, clock {})",
                            Temperature.ambient(level, abs), level.getOverworldClockTime());
                    return;
                }
                long ticks = 0;
                for (int run = 0; run < 200; run++) {
                    level.setBlock(abs, wheat.getStateForAge(0), 2);
                    while (!wheat.isMaxAge(level.getBlockState(abs)) && ticks < 1_000_000) {
                        level.getBlockState(abs).randomTick(level, abs, level.getRandom());
                        ticks++;
                    }
                }
                avg[f - 2] = ticks / 200.0;
                Mechanical.LOGGER.info("farm_tools: wheat at {} on fertility {} (rate {}x, temp {}) ripened in {} random ticks on average; vanilla 21",
                        abs.getY(), f, rate, Temperature.ambient(level, abs), avg[f - 2]);
            }
        } finally {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.setBlock(abs.offset(x, -1, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(abs, Blocks.AIR.defaultBlockState(), 2);
        }
        h.assertTrue(avg[0] < 21 / 1.5 && avg[1] < avg[0] / 1.2, "ripening ticks: new soil " + avg[0] + ", rich " + avg[1] + " (vanilla 21)");
    }

    private FarmToolsTest() {}
}
