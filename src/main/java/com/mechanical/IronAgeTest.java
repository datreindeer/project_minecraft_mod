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
        clearItems(level, h);

        // ---- smithing: the bloomery heats an ingot, the anvil forges it by its pattern before it cools
        bloomery.setItem(BloomeryBlockEntity.INPUT, new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()));
        for (int i = 0; i < 400 && bloomery.getItem(BloomeryBlockEntity.OUTPUT).isEmpty(); i++) {
            bloomery.forceTemperature(1500);
            BloomeryBlockEntity.serverTick(level, bloomery);
        }
        ItemStack hot = bloomery.removeItemNoUpdate(BloomeryBlockEntity.OUTPUT);
        h.assertTrue(hot.is(Smithing.HOT_WROUGHT_IRON_INGOT.get()) && Ironworking.isHot(hot, level.getGameTime()), "the bloomery heats Wrought Iron to forging heat");
        h.assertTrue(KilnBlockEntity.canBake(new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get())), "so does the Clay Kiln");
        anvil.setPattern(Smithing.Pattern.PLATE);
        anvil.place(level, new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()));
        anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(Ironworking.hits(anvil.item()) == 0, "a cold ingot takes no blows");
        anvil.take();
        anvil.place(level, hot);
        for (int i = 0; i < Smithing.Pattern.PLATE.hits - 1; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(dropped(level, h, anvilAt, Smithing.IRON_PLATE.get()) == 0 && Ironworking.hits(anvil.item()) == Smithing.Pattern.PLATE.hits - 1, "not yet");
        anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(dropped(level, h, anvilAt, Smithing.IRON_PLATE.get()) == 1 && anvil.item().isEmpty(), "4 blows: an Iron Plate");
        // a cooled ingot keeps its blows
        ItemStack half = Ironworking.heated(new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()), Smithing.HOT_WROUGHT_IRON_INGOT.get(), level.getGameTime());
        half.set(Ironworking.BLOOM_HITS.get(), 3);
        ItemStack cold = HotMetalItem.cooled(half);
        h.assertTrue(cold.is(Ironworking.WROUGHT_IRON_INGOT.get()) && Ironworking.hits(cold) == 3, "cooled, it keeps 3 blows");
        h.assertTrue(Ironworking.hits(Ironworking.heated(cold, Smithing.HOT_WROUGHT_IRON_INGOT.get(), level.getGameTime())) == 3, "and reheated too");
        // steel: blister folds into a Steel Ingot; a hot Steel Ingot has no Tool Rod pattern but a pickaxe head
        anvil.place(level, Ironworking.heated(new ItemStack(Smithing.BLISTER_STEEL.get()), Smithing.HOT_BLISTER_STEEL.get(), level.getGameTime()));
        for (int i = 0; i < Smithing.BLISTER_HITS; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(dropped(level, h, anvilAt, Smithing.STEEL_INGOT.get()) == 1, "Blister Steel folds into a Steel Ingot");
        anvil.setPattern(Smithing.Pattern.TOOL_ROD);
        anvil.place(level, Ironworking.heated(new ItemStack(Smithing.STEEL_INGOT.get()), Smithing.HOT_STEEL_INGOT.get(), level.getGameTime()));
        anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(Ironworking.hits(anvil.item()) == 0, "no steel tool rod");
        anvil.setPattern(Smithing.Pattern.PICKAXE_HEAD);
        for (int i = 0; i < Smithing.Pattern.PICKAXE_HEAD.hits; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(dropped(level, h, anvilAt, Smithing.STEEL_PICKAXE_HEAD.get()) == 1, "a Steel Pickaxe Head");
        ItemStack ironHammer = new ItemStack(Smithing.IRON_SMITHING_HAMMER.get());
        h.assertTrue(Smithing.wear(ironHammer) == 1 && Smithing.wear(hammer) == Ironworking.HAMMER_WEAR, "an iron hammer wears half as fast");

        // ---- the parts make vanilla's iron tools and armour; forged iron outlasts bronze
        h.assertTrue(ToolStationRecipes.ALL.stream().anyMatch(r -> r.result().get() == Items.IRON_PICKAXE && r.head().get() == Smithing.IRON_PICKAXE_HEAD.get()),
                "Tool Station: Iron Pickaxe from an Iron Pickaxe Head");
        h.assertTrue(ToolStationRecipes.ALL.stream().anyMatch(r -> r.result().get() == Smithing.STEEL_PICKAXE.get()), "and a Steel Pickaxe");
        h.assertTrue(ArmorStationRecipes.ALL.stream().anyMatch(r -> r.result().get() == Items.IRON_CHESTPLATE), "Armor Station: Iron Chestplate");
        h.assertTrue(new ItemStack(Items.IRON_PICKAXE).getMaxDamage() == Smithing.IRON_TOOL_DURABILITY
                && Smithing.IRON_TOOL_DURABILITY > new ItemStack(Bronze.BRONZE_PICKAXE.get()).getMaxDamage(), "iron tools outlast bronze");
        h.assertTrue(new ItemStack(Smithing.STEEL_PICKAXE.get()).getMaxDamage() > Smithing.IRON_TOOL_DURABILITY
                && new ItemStack(Smithing.STEEL_PICKAXE.get()).isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()),
                "steel outlasts iron and mines obsidian");
        var plates = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, List.of(new ItemStack(Smithing.IRON_PLATE.get()), ItemStack.EMPTY,
                new ItemStack(Smithing.IRON_PLATE.get()), ItemStack.EMPTY, new ItemStack(Smithing.IRON_PLATE.get()), ItemStack.EMPTY));
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, plates, level).map(r -> r.value().assemble(plates).is(Items.BUCKET)).orElse(false),
                "3 Iron Plates make a Bucket");

        // ---- charcoal clamp: a firewood pile and a rough log pile, covered with dirt but for the vent on top of the first
        for (int x = 7; x <= 13; x++) for (int z = 3; z <= 9; z++) {
            h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
        }
        BlockPos fw = new BlockPos(10, 1, 6), logs = new BlockPos(10, 1, 7);
        h.setBlock(fw, Mechanical.FIREWOOD_PILES.get("oak").get().defaultBlockState().setValue(FirewoodPileBlock.LAYERS, FirewoodPileBlock.MAX));
        h.setBlock(logs, Mechanical.ROUGH_LOG_PILE.get().defaultBlockState().setValue(RoughLogPileBlock.LOGS, RoughLogPileBlock.PER_BLOCK));
        BlockPos fwAbs = h.absolutePos(fw);
        h.assertFalse(CharcoalClamp.tryLight(level, fwAbs, net.minecraft.core.Direction.UP, null, true), "an uncovered pile won't light");
        for (BlockPos p : List.of(fw, logs)) for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos n = p.relative(d);
            if (!n.equals(fw) && !n.equals(logs)) h.setBlock(n, net.minecraft.world.level.block.Blocks.DIRT);
        }
        h.setBlock(logs.above(), net.minecraft.world.level.block.Blocks.COARSE_DIRT);
        h.assertTrue(CharcoalClamp.tryLight(level, fwAbs, net.minecraft.core.Direction.UP, null, true), "covered but for the vent, it lights");
        h.assertFalse(CharcoalClamp.tryLight(level, fwAbs, net.minecraft.core.Direction.UP, null, true), "not twice");
        CharcoalClamp.get(level).finishAll(level);
        var heap = level.getBlockState(fwAbs);
        h.assertTrue(heap.is(CharcoalClamp.CHARCOAL_HEAP.get()) && heap.getValue(FirewoodPileBlock.LAYERS) == FirewoodPileBlock.MAX, "8 firewood: 8 Charcoal");
        var heap2 = level.getBlockState(h.absolutePos(logs));
        h.assertTrue(heap2.is(CharcoalClamp.CHARCOAL_HEAP.get()) && heap2.getValue(FirewoodPileBlock.LAYERS) == RoughLogPileBlock.PER_BLOCK * CharcoalClamp.CHARCOAL_PER_LOG,
                "4 rough logs: 8 Charcoal");
        h.assertTrue(net.minecraft.world.level.block.Block.getDrops(heap, level, fwAbs, null).stream().mapToInt(ItemStack::getCount).sum() == FirewoodPileBlock.MAX,
                "the heap drops its Charcoal");
        // opened while it smoulders, a pile flares up
        h.setBlock(fw, Mechanical.FIREWOOD_PILES.get("birch").get().defaultBlockState().setValue(FirewoodPileBlock.LAYERS, 4));
        h.setBlock(logs, net.minecraft.world.level.block.Blocks.DIRT);
        h.assertTrue(CharcoalClamp.tryLight(level, fwAbs, net.minecraft.core.Direction.UP, null, true), "a one-pile clamp");
        h.setBlock(fw.east(), net.minecraft.world.level.block.Blocks.AIR);
        CharcoalClamp.get(level).checkNow(level);
        h.assertTrue(level.getBlockState(fwAbs).isAir(), "opened, it burns away");
        CharcoalClamp.get(level).finishAll(level);
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

    static void clearItems(ServerLevel level, GameTestHelper h) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(12)).forEach(ItemEntity::discard);
    }

    static int dropped(ServerLevel level, GameTestHelper h, BlockPos rel, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(2)).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }
}
