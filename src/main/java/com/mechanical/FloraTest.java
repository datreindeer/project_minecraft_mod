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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Flora phase 1: grown thicket bushes side by side join up, young ones don't; a top grows only on a grown bush and falls
 * with it; picking a ripe bush gives berries and leaves it unripe (or bare in winter); the seasons each kind wants; when
 * flowers bloom (the Fern Flower only on summer nights, the Frost Lily only in winter); raw elderberries are poisonous.
 */
@EventBusSubscriber(modid = MODID)
public final class FloraTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "flora"));

    static {
        TEST_FUNCTIONS.register("flora", () -> FloraTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "flora"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "flora"), new FunctionGameTestInstance(TEST, TestArea.of(env, 100, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
        ThicketBushBlock bush = Flora.THICKETS.get("blackthorn").get();
        BlockState grown = bush.defaultBlockState();

        // three grown bushes in a row join up; the middle one joins both sides
        BlockPos a = new BlockPos(2, 1, 4), b = new BlockPos(3, 1, 4), c = new BlockPos(4, 1, 4);
        for (BlockPos p : List.of(a, b, c)) h.setBlock(p, grown);
        for (BlockPos p : List.of(a, b, c)) h.setBlock(p, bush.withSides(level, h.absolutePos(p), grown)); // as placed by a player
        BlockState mid = h.getBlockState(b);
        h.assertTrue(mid.getValue(ThicketBushBlock.SIDES.get(Direction.EAST)) && mid.getValue(ThicketBushBlock.SIDES.get(Direction.WEST))
                && ThicketBushBlock.sides(mid) == 2, "the middle bush joins both neighbours: " + mid);
        // a young bush beside it doesn't join
        BlockPos young = new BlockPos(3, 1, 5);
        h.setBlock(young, grown.setValue(ThicketBushBlock.YOUNG, true));
        h.assertFalse(h.getBlockState(b).getValue(ThicketBushBlock.SIDES.get(Direction.SOUTH)), "a young bush doesn't join the thicket");

        // a top stands on a grown bush, not on a young one, and falls when the bush goes
        h.assertTrue(grown.setValue(ThicketBushBlock.HALF, DoubleBlockHalf.UPPER).canSurvive(level, h.absolutePos(b.above())), "a top stands on a grown bush");
        h.assertFalse(grown.setValue(ThicketBushBlock.HALF, DoubleBlockHalf.UPPER).canSurvive(level, h.absolutePos(young.above())), "no top on a young bush");
        h.setBlock(b.above(), grown.setValue(ThicketBushBlock.HALF, DoubleBlockHalf.UPPER));
        h.setBlock(b, Blocks.AIR);
        h.assertTrue(h.getBlockState(b.above()).isAir(), "the top falls with the bush under it");

        // picking a ripe bush: berries, then unripe (or bare in winter)
        h.setBlock(a, h.getBlockState(a).setValue(ThicketBushBlock.AGE, ThicketBushBlock.RIPE));
        h.assertTrue(WildBerryBushBlock.isRipe(h.getBlockState(a)), "a ripe thicket bush counts as ripe for bears and tribes");
        ItemStack picked = bush.pick(level, h.absolutePos(a), h.getBlockState(a));
        int season = Calendar.season(level.getOverworldClockTime());
        h.assertTrue(picked.is(Flora.THICKET_BERRIES.get("blackthorn").get()) && h.getBlockState(a).getValue(ThicketBushBlock.AGE) == (season == 3 ? 0 : 2),
                "picked sloes and the bush went back: " + picked + " " + h.getBlockState(a));
        h.assertTrue(bush.target(0) == 1 && bush.target(2) == 3 && Flora.THICKETS.get("jujube").get().target(3) == 0,
                "blackthorn flowers in spring and is ripe in autumn; jujubes are gone in winter");
        h.assertTrue(bush.suits(0.8F) && !bush.suits(0.0F) && Flora.THICKETS.get("sea_buckthorn").get().suits(0.0F)
                && !Flora.THICKETS.get("jujube").get().suits(0.5F), "each kind grows in its own climate");

        // flowers' seasons
        var fern = Flora.FERN_FLOWER.get().bloom();
        h.assertTrue(fern.at(1, true) && !fern.at(1, false) && !fern.at(0, true), "the Fern Flower blooms only on summer nights");
        var lily = Flora.FROST_LILY.get().bloom();
        h.assertTrue(lily.at(3, false) && !lily.at(1, false), "the Frost Lily blooms only in winter");
        var pasque = ((WildFlowerBlock) Flora.FLOWER_BLOCKS.get("pasque_flower").get()).bloom();
        h.assertTrue(pasque.at(0, false) && !pasque.at(2, false), "the Pasque Flower blooms in spring");

        // raw elderberries make you sick, cooked ones don't
        var raw = new ItemStack(Foraging.BERRIES.get("elderberry").get()).get(DataComponents.CONSUMABLE);
        var cooked = new ItemStack(Flora.COOKED_ELDERBERRIES.get()).get(DataComponents.CONSUMABLE);
        h.assertTrue(raw != null && !raw.onConsumeEffects().isEmpty() && (cooked == null || cooked.onConsumeEffects().isEmpty()),
                "raw elderberries are poisonous, cooked ones are not");
        // every flower and bush can be planted on grass, and on a Grass Slab (it becomes a full grass block first)
        BlockPos onGrass = new BlockPos(6, 1, 1);
        List<BlockState> plants = new java.util.ArrayList<>();
        Flora.FLOWER_BLOCKS.values().forEach(k -> plants.add(k.get().defaultBlockState()));
        Flora.THICKETS.values().forEach(k -> plants.add(k.get().defaultBlockState()));
        plants.add(Flora.FERN_FLOWER.get().defaultBlockState());
        plants.add(Flora.FROST_LILY.get().defaultBlockState());
        for (BlockState plant : plants) h.assertTrue(plant.canSurvive(level, h.absolutePos(onGrass)), plant.getBlock() + " grows on grass");
        BlockPos slab = new BlockPos(7, 1, 7);
        h.setBlock(slab, EarthBlocks.GRASS_SLAB.get());
        h.assertTrue(SlabPlanting.fill(level, h.absolutePos(slab), Flora.FLOWER_BLOCKS.get("pasque_flower").get().defaultBlockState())
                && h.getBlockState(slab).is(Blocks.GRASS_BLOCK), "planting on a Grass Slab makes it a grass block");
        h.setBlock(slab, EarthBlocks.SAND_SLAB.get());
        h.assertTrue(!SlabPlanting.fill(level, h.absolutePos(slab), Flora.FLOWER_BLOCKS.get("pasque_flower").get().defaultBlockState())
                && h.getBlockState(slab).is(EarthBlocks.SAND_SLAB.get()), "a Sand Slab stays a slab for a plant that can't grow on sand");
        h.assertTrue(SlabPlanting.fill(level, h.absolutePos(slab), Flora.FLOWER_BLOCKS.get("sand_verbena").get().defaultBlockState())
                && h.getBlockState(slab).is(Blocks.SAND), "but becomes sand for Sand Verbena");
        // phase 2: low mats grow no top and have no thorns; evergreen kinds keep their leaves; old bushes ripen only in season
        ThicketBushBlock crowberry = Flora.THICKETS.get("crowberry").get();
        h.assertTrue(crowberry.low() && crowberry.evergreen() && !Flora.THICKETS.get("rosehip").get().low(), "crowberry is a low evergreen mat");
        WildBerryBushBlock raspberry = Foraging.BUSH_BLOCKS.get("raspberry").get();
        h.assertTrue(raspberry.ripensIn(1) && !raspberry.ripensIn(0) && Foraging.BUSH_BLOCKS.get("lingonberry").get().ripensIn(3),
                "raspberries ripen in summer, lingonberries into winter");
        var moonwort = Flora.MOONWORT.get().bloom();
        h.assertTrue(moonwort.at(1, true, true) && !moonwort.at(1, true, false) && !moonwort.at(1, false, true), "moonwort blooms on full-moon nights");
        // phase 3: fruit leaves give fruit when ripe and go back to unripe; stumps hold a rough log of every wood
        FruitLeavesBlock rowan = (FruitLeavesBlock) Trees.WOODS.get("rowan").leaves().get();
        BlockPos leaf = new BlockPos(1, 3, 1);
        h.setBlock(leaf, rowan.defaultBlockState().setValue(FruitLeavesBlock.STAGE, FruitLeavesBlock.RIPE).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        ItemStack fruit = rowan.pick(level, h.absolutePos(leaf), h.getBlockState(leaf));
        h.assertTrue(fruit.is(Trees.FRUITS.get("rowan").get()) && h.getBlockState(leaf).getValue(FruitLeavesBlock.STAGE) < FruitLeavesBlock.RIPE,
                "picked rowan berries: " + fruit + " " + h.getBlockState(leaf));
        h.assertTrue(rowan.target(2) == 3 && rowan.target(3) == 0, "rowan berries ripen in autumn and are gone in winter");
        h.assertTrue(StumpBlock.LOG.getPossibleValues().size() == Mechanical.WOODS.size() + 1, "a stump has a log state for every wood");
        var rawRowan = new ItemStack(Trees.FRUITS.get("rowan").get()).get(DataComponents.CONSUMABLE);
        h.assertTrue(rawRowan != null && !rawRowan.onConsumeEffects().isEmpty(), "raw rowan berries upset the stomach");
        h.succeed();
    }

    private FloraTest() {}
}
