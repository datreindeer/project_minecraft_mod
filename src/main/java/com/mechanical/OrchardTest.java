package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The orchard (Trees.FRUIT_KINDS, docs/flora-and-food-plan.md "Orchard"): a Pear sapling grows into a pear tree with pear
 * wood and fruit leaves; ripe leaves of every orchard tree give their own fruit; the citrus and pomegranate saplings take
 * only in a warm biome while almond grows anywhere; the nut oils press, the preserves boil and pickle, the new pies bake.
 */
@EventBusSubscriber(modid = MODID)
public final class OrchardTest {
    static final List<String> ORCHARD = List.of("pear", "sweet_cherry", "lemon", "clementine", "walnut", "chestnut", "quince", "mulberry",
            "medlar", "peach", "apricot", "almond", "pomegranate");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "orchard"));

    static {
        TEST_FUNCTIONS.register("orchard", () -> OrchardTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "orchard"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "orchard"), new FunctionGameTestInstance(TEST, TestArea.of(env, 200, true)));
    }

    private static ItemStack of(net.minecraft.world.level.ItemLike item) {
        return new ItemStack(item);
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();

        // ---- a pear sapling grows into a pear tree: pear log where it stood, pear fruit leaves in the crown ----
        BlockPos sap = new BlockPos(8, 1, 8);
        h.setBlock(sap.below(), Blocks.GRASS_BLOCK);
        var pear = Trees.WOODS.get("pear");
        h.setBlock(sap, pear.sapling().get().defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        for (int i = 0; i < 5 && h.getBlockState(sap).is(pear.sapling().get()); i++)
            pear.sapling().get().advanceTree(level, h.absolutePos(sap), h.getBlockState(sap), level.getRandom());
        h.assertTrue(h.getBlockState(sap).is(pear.log().get()), "the pear sapling grew a pear trunk: " + h.getBlockState(sap));
        int leaves = 0;
        for (BlockPos p : BlockPos.betweenClosed(sap.offset(-4, 2, -4), sap.offset(4, 12, 4)))
            if (h.getBlockState(p).is(pear.leaves().get())) leaves++;
        h.assertTrue(leaves > 10 && pear.leaves().get() instanceof FruitLeavesBlock, "and a crown of pear fruit leaves: " + leaves);

        // ---- ripe leaves of every orchard tree give that tree's fruit; the seasons it ripens in ----
        BlockPos leaf = new BlockPos(1, 2, 1);
        for (String tree : ORCHARD) {
            FruitLeavesBlock fl = (FruitLeavesBlock) Trees.WOODS.get(tree).leaves().get();
            h.setBlock(leaf, fl.defaultBlockState().setValue(FruitLeavesBlock.STAGE, FruitLeavesBlock.RIPE).setValue(LeavesBlock.PERSISTENT, true));
            ItemStack fruit = fl.pick(level, h.absolutePos(leaf), h.getBlockState(leaf));
            h.assertTrue(fruit.is(Trees.FRUITS.get(tree).get()) && fruit.has(net.minecraft.core.component.DataComponents.FOOD),
                    tree + " leaves give their own edible fruit: " + fruit);
        }
        FruitLeavesBlock cherry = (FruitLeavesBlock) Trees.WOODS.get("sweet_cherry").leaves().get();
        FruitLeavesBlock clementine = (FruitLeavesBlock) Trees.WOODS.get("clementine").leaves().get();
        h.assertTrue(cherry.target(1) == FruitLeavesBlock.RIPE && cherry.target(2) == 0, "sweet cherries ripen in summer and are gone by autumn");
        h.assertTrue(clementine.target(3) == FruitLeavesBlock.RIPE, "clementines are ripe in winter");
        h.assertTrue(StumpBlock.LOG.getPossibleValues().size() == Mechanical.WOODS.size() + 1, "a stump has a log state for every wood");

        // ---- lemons want warmth, almonds don't care ----
        boolean warm = Trees.warmEnoughForCitrus(level, h.absolutePos(new BlockPos(3, 1, 13)));
        for (String tree : List.of("lemon", "almond")) {
            BlockPos at = new BlockPos(tree.equals("lemon") ? 3 : 13, 1, 13);
            h.setBlock(at.below(), Blocks.GRASS_BLOCK);
            SaplingBlock s = Trees.WOODS.get(tree).sapling().get();
            h.setBlock(at, s.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
            for (int i = 0; i < 5 && h.getBlockState(at).is(s); i++) s.advanceTree(level, h.absolutePos(at), h.getBlockState(at), level.getRandom());
            boolean grew = h.getBlockState(at).is(Trees.WOODS.get(tree).log().get());
            h.assertTrue(tree.equals("lemon") ? grew == warm : grew, tree + " sapling grew: " + grew + " (warm here: " + warm + ")");
        }

        // ---- the kitchen: nut oil, quince paste, cherry jam, pickled lemons, pear pie ----
        Item walnuts = Trees.FRUITS.get("walnut").get(), quinces = Trees.FRUITS.get("quince").get(), lemons = Trees.FRUITS.get("lemon").get();
        Kitchen.Match oil = Kitchen.match(Kitchen.Tool.PRESS, 2, List.of(of(walnuts), of(walnuts), of(walnuts), of(walnuts)));
        h.assertTrue(oil != null && oil.dish().result().get() == Kitchen.WALNUT_OIL.get(), "four walnuts press into Walnut Oil: " + oil);
        Kitchen.Match fish = Kitchen.match(Kitchen.Tool.PAN, 2, List.of(of(Items.COD), of(Kitchen.ALMOND_OIL.get()), of(Items.SALMON)));
        h.assertTrue(fish != null && fish.dish().result().get() == Kitchen.FRIED_FISH.get(), "almond oil fries fish: " + fish);
        Kitchen.Match paste = Kitchen.match(Kitchen.Tool.POT, 1, List.of(of(quinces), of(quinces), of(quinces), of(Items.HONEY_BOTTLE)));
        h.assertTrue(paste != null && paste.dish().result().get() == Kitchen.QUINCE_PASTE.get(), "the kettle boils quince paste: " + paste);
        Item cherries = Trees.FRUITS.get("sweet_cherry").get();
        Kitchen.Match jam = Kitchen.match(Kitchen.Tool.POT, 1, List.of(of(cherries), of(cherries), of(cherries), of(Items.HONEY_BOTTLE)));
        h.assertTrue(jam != null && jam.dish().result().get() == Kitchen.CHERRY_JAM.get(), "and cherry jam: " + jam);
        Kitchen.Match pickled = Kitchen.match(Kitchen.Tool.BARREL, 2, List.of(of(lemons), of(IronOres.SALT.get()), of(lemons), of(lemons)));
        h.assertTrue(pickled != null && pickled.dish().result().get() == Kitchen.PICKLED_LEMONS.get(), "lemons pickle in salt: " + pickled);
        h.assertTrue(Kitchen.PIES.containsKey("pear") && Kitchen.PIES.containsKey("peach"), "pear and peach pies exist");
        h.assertTrue(of(Trees.ROASTED_CHESTNUTS.get()).has(net.minecraft.core.component.DataComponents.FOOD), "roasted chestnuts are food");
        h.succeed();
    }

    private OrchardTest() {}
}
