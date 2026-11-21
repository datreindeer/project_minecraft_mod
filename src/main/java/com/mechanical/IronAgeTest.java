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
                new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
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
        // steel: too hard for the bronze hammer; with the iron one blister folds into a Steel Ingot; a hot Steel Ingot has no
        // Tool Rod pattern but a pickaxe head
        ItemStack steelHammer = new ItemStack(Smithing.IRON_SMITHING_HAMMER.get());
        anvil.place(level, Ironworking.heated(new ItemStack(Smithing.BLISTER_STEEL.get()), Smithing.HOT_BLISTER_STEEL.get(), level.getGameTime()));
        anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(Ironworking.hits(anvil.item()) == 0, "the bronze hammer can't work steel");
        for (int i = 0; i < Smithing.BLISTER_HITS; i++) anvil.strike(level, smith, steelHammer, InteractionHand.MAIN_HAND);
        h.assertTrue(dropped(level, h, anvilAt, Smithing.STEEL_INGOT.get()) == 1, "Blister Steel folds into a Steel Ingot");
        anvil.setPattern(Smithing.Pattern.TOOL_ROD);
        anvil.place(level, Ironworking.heated(new ItemStack(Smithing.STEEL_INGOT.get()), Smithing.HOT_STEEL_INGOT.get(), level.getGameTime()));
        anvil.strike(level, smith, steelHammer, InteractionHand.MAIN_HAND);
        h.assertTrue(Ironworking.hits(anvil.item()) == 0, "no steel tool rod");
        anvil.setPattern(Smithing.Pattern.PICKAXE_HEAD);
        anvil.strike(level, smith, steelHammer, InteractionHand.MAIN_HAND);
        h.assertTrue(Ironworking.hits(anvil.item()) == 0, "a pickaxe head takes 3 ingots: one alone takes no blows");
        h.assertTrue(anvil.addIngot(level, null) && anvil.addIngot(level, null) && !anvil.addIngot(level, null), "two more stack on, then it is full");
        h.assertTrue(anvil.pile() == 3, "3 ingots lie on the anvil");
        for (int i = 0; i < Smithing.Pattern.PICKAXE_HEAD.hits; i++) anvil.strike(level, smith, steelHammer, InteractionHand.MAIN_HAND);
        h.assertTrue(dropped(level, h, anvilAt, Smithing.STEEL_PICKAXE_HEAD.get()) == 1 && anvil.item().isEmpty(), "3 ingots: a Steel Pickaxe Head");
        // an ingot the part doesn't need stays on the anvil
        anvil.setPattern(Smithing.Pattern.PLATE);
        anvil.place(level, Ironworking.heated(new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()), Smithing.HOT_WROUGHT_IRON_INGOT.get(), level.getGameTime()));
        anvil.addIngot(level, null);
        for (int i = 0; i < Smithing.Pattern.PLATE.hits; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(anvil.pile() == 1 && anvil.item().is(Smithing.HOT_WROUGHT_IRON_INGOT.get()) && Ironworking.hits(anvil.item()) == 0,
                "a plate from 2 ingots: the other stays, unworked");
        h.assertTrue(anvil.progress() == 0 && anvil.take().is(Smithing.HOT_WROUGHT_IRON_INGOT.get()), "and can be taken back");
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

        // ---- cementation: pot fired, packed, held at 1000° in the bloomery, opened to Blister Steel
        h.assertTrue(KilnBlockEntity.canBake(new ItemStack(Smithing.UNFIRED_CEMENTATION_POT.get())), "the kiln fires the pot");
        h.assertFalse(KilnBlockEntity.canBake(new ItemStack(Smithing.PACKED_CEMENTATION_POT.get())), "cementation only in the bloomery");
        h.assertTrue(KilnBlockEntity.canBake(new ItemStack(Mechanical.ROUGH_LOGS.get("oak").get())), "the kiln chars a rough log");
        var packIn = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(new ItemStack(Smithing.CEMENTATION_POT.get()),
                new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()), new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()), new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()),
                new ItemStack(Items.CHARCOAL), new ItemStack(Items.CHARCOAL), new ItemStack(Mechanical.SOOT_ITEM.get()), ItemStack.EMPTY, ItemStack.EMPTY));
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, packIn, level)
                .map(r -> r.value().assemble(packIn).is(Smithing.PACKED_CEMENTATION_POT.get())).orElse(false), "pot + 3 Wrought Iron + 2 Charcoal + Soot: packed");
        bloomery.setItem(BloomeryBlockEntity.INPUT, new ItemStack(Smithing.PACKED_CEMENTATION_POT.get()));
        bloomery.setItem(BloomeryBlockEntity.FUEL, new ItemStack(Items.CHARCOAL, 16));
        bloomery.tryLight(level);
        for (int i = 0; i < Smithing.CEMENT_TICKS * 2 && bloomery.getItem(BloomeryBlockEntity.OUTPUT).isEmpty(); i++) {
            bloomery.forceTemperature(Smithing.CEMENT_TEMP + 100); // over its 800° it sinks back each tick
            BloomeryBlockEntity.serverTick(level, bloomery);
        }
        ItemStack cemented = bloomery.removeItemNoUpdate(BloomeryBlockEntity.OUTPUT);
        h.assertTrue(cemented.is(Smithing.CEMENTED_POT.get()) && !cemented.has(Casting.COOLS_AT.get()), "3 min at 1000°: a Cemented Pot (not hot metal), got " + cemented);
        var openIn = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(cemented));
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, openIn, level)
                .map(r -> { ItemStack o = r.value().assemble(openIn); return o.is(Smithing.BLISTER_STEEL.get()) && o.getCount() == Smithing.BLISTER_PER_POT; }).orElse(false),
                "opened: 3 Blister Steel");
        var flintIn = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(Items.FLINT), new ItemStack(Smithing.STEEL_PLATE.get())));
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, flintIn, level)
                .map(r -> r.value().assemble(flintIn).is(Items.FLINT_AND_STEEL)).orElse(false), "Flint + Steel Plate: Flint and Steel");
        h.assertTrue(ArmorStationRecipes.ALL.stream().anyMatch(r -> r.result().get() == Smithing.STEEL_CHESTPLATE.get()), "Armor Station: Steel Chestplate");
        h.assertTrue(new ItemStack(Smithing.STEEL_CHESTPLATE.get()).getMaxDamage() > new ItemStack(Items.IRON_CHESTPLATE).getMaxDamage(), "steel armour outlasts iron");
        ItemStack rod = new ItemStack(Smithing.IRON_TOOL_ROD.get());
        var barsIn = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, List.of(rod, rod.copy(), rod.copy(), rod.copy(), rod.copy(), rod.copy()));
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, barsIn, level)
                .map(r -> r.value().assemble(barsIn).is(Items.IRON_BARS)).orElse(false), "6 Iron Tool Rods: Iron Bars");

        // ---- Iron Bellows: twice the pump, and the heat holds after it (the wooden one's sinks back)
        bloomery.setItem(BloomeryBlockEntity.FUEL, new ItemStack(Items.CHARCOAL, 8));
        bloomery.tryLight(level);
        bloomery.forceTemperature(1000);
        bloomery.pump(true);
        h.assertTrue(Math.abs(bloomery.temperature() - (1000 + KilnBlockEntity.PUMP * KilnBlockEntity.IRON_PUMP_FACTOR)) < 1, "an iron pump: +80°");
        for (int i = 0; i < 200; i++) BloomeryBlockEntity.serverTick(level, bloomery);
        h.assertTrue(bloomery.temperature() > 1079, "held for 10 s: " + bloomery.temperature());
        bloomery.forceTemperature(1000);
        bloomery.pump(false);
        h.assertTrue(Math.abs(bloomery.temperature() - (1000 + KilnBlockEntity.PUMP)) < 1, "a wooden pump: +40°");

        // ---- Smithing Hearth: stocked and lit, it heats an ingot and keeps it hot
        BlockPos hearthAt = new BlockPos(6, 1, 1);
        h.setBlock(hearthAt, Smithing.SMITHING_HEARTH.get());
        SmithingHearthBlockEntity forge = (SmithingHearthBlockEntity) level.getBlockEntity(h.absolutePos(hearthAt));
        h.assertFalse(forge.light(level), "no charcoal: it won't light");
        h.assertTrue(forge.addFuel() && forge.light(level) && forge.lit(), "with charcoal it lights");
        h.assertTrue(forge.put(new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get())), "an ingot in the coals");
        for (int i = 0; i < SmithingHearthBlockEntity.HEAT_TICKS / 20; i++) forge.second(level);
        h.assertTrue(forge.item(0).is(Smithing.HOT_WROUGHT_IRON_INGOT.get()), "10 s in the coals: forging heat, got " + forge.item(0));
        forge.item(0).set(Casting.COOLS_AT.get(), level.getGameTime() - 1); // its time is up...
        forge.second(level);
        h.assertTrue(Ironworking.isHot(forge.item(0), level.getGameTime()), "...but in the burning hearth it stays hot");
        h.assertTrue(forge.take().is(Smithing.HOT_WROUGHT_IRON_INGOT.get()), "an empty hand takes it out hot");
        h.assertTrue(BronzeTools.isQuarryHammer(new ItemStack(Smithing.STEEL_QUARRY_HAMMER.get())), "the steel quarry hammer is a quarry hammer");
        h.assertTrue(new ItemStack(Smithing.STEEL_PICKAXE.get()).getMaxDamage() == 2200, "steel tools: 2200");

        // ---- forged saws: longer-lived and quicker than bronze; any saw in #mechanical:saws cuts planks
        h.assertTrue(new ItemStack(Smithing.IRON_SAW.get()).getMaxDamage() == 900 && new ItemStack(Smithing.STEEL_SAW.get()).getMaxDamage() == 1600,
                "saws: iron 900, steel 1600");
        h.assertTrue(Smithing.STEEL_SAW.get().strokeTicks < Smithing.IRON_SAW.get().strokeTicks
                && Smithing.IRON_SAW.get().strokeTicks < Bronze.BRONZE_SAW.get().strokeTicks, "steel saws faster than iron, iron than bronze");
        var saws = net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "saws"));
        for (var saw : List.of(Bronze.BRONZE_SAW.get(), Smithing.IRON_SAW.get(), Smithing.STEEL_SAW.get())) {
            ItemStack sawStack = new ItemStack(saw);
            h.assertTrue(sawStack.is(saws), saw + " in #mechanical:saws");
            var sawIn = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(Items.OAK_LOG), sawStack));
            var planks = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, sawIn, level);
            h.assertTrue(planks.map(r -> { ItemStack o = r.value().assemble(sawIn); return o.is(Items.OAK_PLANKS) && o.getCount() == 4; }).orElse(false),
                    "Oak Log + " + saw + ": 4 Oak Planks");
            var left = planks.get().value().getRemainingItems(sawIn);
            h.assertTrue(left.get(1).is(saw) && left.get(1).getDamageValue() == 1, saw + " stays, losing 1");
        }
        for (var saw : List.of(Smithing.IRON_SAW.get(), Smithing.STEEL_SAW.get()))
            h.assertTrue(ToolStationRecipes.ALL.stream().anyMatch(r -> r.result().get() == saw), "Tool Station: " + saw);
        h.assertTrue(Smithing.Pattern.SAW_BLADE.result(new ItemStack(Smithing.HOT_STEEL_INGOT.get())).is(Smithing.STEEL_SAW_BLADE.get()), "a steel saw blade");

        // ---- the crossbow from forged parts, and Iron Bolts: crossbow-only, harder-hitting than bronze, through armour
        ItemStack heads = Smithing.Pattern.BOLT_HEADS.result(new ItemStack(Smithing.HOT_WROUGHT_IRON_INGOT.get()));
        h.assertTrue(heads.is(Smithing.IRON_BOLT_HEAD.get()) && heads.getCount() == Smithing.BOLT_HEADS_PER_INGOT, "one hot ingot: 4 Iron Bolt Heads");
        h.assertTrue(FletchingRecipes.ALL.stream().anyMatch(r -> r.result().get() == Smithing.IRON_BOLT.get()), "Fletching Table: Iron Bolts");
        ItemStack plate = new ItemStack(Smithing.IRON_PLATE.get()), sinew = new ItemStack(com.mechanical.wildlife.Wildlife.SINEW.get()),
                stock = new ItemStack(Mechanical.TOOL_ROD.get());
        var bowIn = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(plate, rod.copy(), plate.copy(), sinew, stock, sinew.copy(),
                ItemStack.EMPTY, stock.copy(), ItemStack.EMPTY));
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, bowIn, level)
                .map(r -> r.value().assemble(bowIn).is(Items.CROSSBOW)).orElse(false), "Iron Plates, Iron Tool Rod, Sinew, Tool Rods: a Crossbow");
        ItemStack bolt = new ItemStack(Smithing.IRON_BOLT.get()), crossbow = new ItemStack(Items.CROSSBOW);
        h.assertTrue(((net.minecraft.world.item.ProjectileWeaponItem) Items.CROSSBOW).getAllSupportedProjectiles(crossbow).test(bolt), "the crossbow loads bolts");
        h.assertTrue(((net.minecraft.world.item.ProjectileWeaponItem) Items.CROSSBOW).getAllSupportedProjectiles(crossbow).test(new ItemStack(Items.ARROW)), "and arrows");
        h.assertFalse(((net.minecraft.world.item.ProjectileWeaponItem) Items.BOW).getAllSupportedProjectiles(new ItemStack(Items.BOW)).test(bolt), "a bow can't shoot bolts");
        h.assertTrue(com.mechanical.equipment.Quiver.ARROWS.test(bolt), "bolts go in the quiver");
        h.assertTrue(Smithing.IRON_BOLT.get().damage() > Bronze.BRONZE_ARROW.get().damage(), "bolts hit harder than Bronze Arrows");
        float[] lost = new float[2];
        for (int i = 0; i < 2; i++) {
            var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(1 + 3 * i, 2, 5));
            zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(15); // worn armour only counts after a tick
            ItemStack ammo = i == 0 ? bolt : new ItemStack(Items.ARROW);
            var arrow = ((net.minecraft.world.item.ArrowItem) ammo.getItem()).createArrow(level, ammo, smith, crossbow);
            float before = zombie.getHealth();
            zombie.hurtServer(level, level.damageSources().arrow(arrow, smith), 10.0F);
            lost[i] = before - zombie.getHealth();
            zombie.discard();
        }
        h.assertTrue(lost[0] > lost[1] + 0.5F, "a bolt goes through armour: " + lost[0] + " vs an arrow's " + lost[1]);

        // ---- forged versions of the bronze tools: every one at the Tool Station, its part from the anvil, in the right tags
        for (var tool : List.of(Smithing.IRON_SICKLE.get(), Smithing.STEEL_SICKLE.get(), Smithing.IRON_CHISEL.get(), Smithing.STEEL_CHISEL.get(),
                Smithing.IRON_TONGS.get(), Smithing.IRON_HOOK_FISHING_ROD.get(), Smithing.IRON_QUARRY_HAMMER.get(), Smithing.STEEL_SMITHING_HAMMER.get()))
            h.assertTrue(ToolStationRecipes.ALL.stream().anyMatch(r -> r.result().get() == tool), "Tool Station: " + tool);
        ItemStack hotIron = new ItemStack(Smithing.HOT_WROUGHT_IRON_INGOT.get()), hotSteel = new ItemStack(Smithing.HOT_STEEL_INGOT.get());
        h.assertTrue(Smithing.Pattern.SICKLE_BLADE.result(hotSteel).is(Smithing.STEEL_SICKLE_BLADE.get())
                && Smithing.Pattern.CHISEL_BLADE.result(hotIron).is(Smithing.IRON_CHISEL_BLADE.get())
                && Smithing.Pattern.TONG_ARMS.result(hotIron).is(Smithing.IRON_TONG_ARMS.get()), "sickle, chisel and tong patterns");
        ItemStack needles = Smithing.Pattern.NEEDLES.result(hotIron), hooks = Smithing.Pattern.FISH_HOOKS.result(hotIron);
        h.assertTrue(needles.is(Smithing.IRON_NEEDLE.get()) && needles.getCount() == 4 && hooks.is(Smithing.IRON_FISH_HOOK.get()) && hooks.getCount() == 4,
                "one hot ingot: 4 Iron Needles, 4 Iron Fish Hooks");
        h.assertTrue(Smithing.Pattern.TONG_ARMS.result(hotSteel).isEmpty(), "no steel tongs");
        var sickles = net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "sickles"));
        ItemStack ironSickle = new ItemStack(Smithing.IRON_SICKLE.get()), steelSickle = new ItemStack(Smithing.STEEL_SICKLE.get());
        h.assertTrue(ironSickle.is(sickles) && steelSickle.is(sickles), "both in #mechanical:sickles");
        h.assertTrue(ironSickle.getMaxDamage() == 900 && steelSickle.getMaxDamage() == 1600, "sickles: iron 900, steel 1600");
        h.assertTrue(Farming.sickleBonus(ironSickle) == 0.9F && Farming.sickleBonus(steelSickle) == 1.0F
                && Farming.sickleBonus(new ItemStack(BronzeTools.BRONZE_SICKLE.get())) == 0.8F, "extra sheaf: iron 90 %, steel 100 %");
        h.assertTrue(BronzeTools.sweepRadius(ironSickle) == 1 && BronzeTools.sweepRadius(steelSickle) == 2
                && BronzeTools.sweepRadius(new ItemStack(Farming.FLINT_SICKLE.get())) == 0, "sweep: iron 3x3, steel 5x5, flint none");
        h.assertTrue(new ItemStack(Smithing.IRON_TONGS.get()).is(BronzeTools.TONGS) && new ItemStack(BronzeTools.BRONZE_TONGS.get()).is(BronzeTools.TONGS), "#mechanical:tongs");
        h.assertTrue(new ItemStack(Smithing.IRON_NEEDLE.get()).is(BronzeTools.NEEDLES), "the Iron Needle sews");
        h.assertTrue(Smithing.STEEL_CHISEL.get().whetStrokes > Smithing.IRON_CHISEL.get().whetStrokes
                && new ItemStack(Smithing.STEEL_CHISEL.get()).getMaxDamage() > new ItemStack(Smithing.IRON_CHISEL.get()).getMaxDamage()
                && new ItemStack(Smithing.IRON_CHISEL.get()).getMaxDamage() > new ItemStack(BronzeTools.BRONZE_CHISEL.get()).getMaxDamage(), "chisels: steel > iron > bronze");
        h.assertTrue(BronzeTools.isQuarryHammer(new ItemStack(Smithing.IRON_QUARRY_HAMMER.get()))
                && new ItemStack(Smithing.IRON_QUARRY_HAMMER.get()).isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.GOLD_ORE.defaultBlockState())
                && !new ItemStack(BronzeTools.BRONZE_QUARRY_HAMMER.get()).isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.GOLD_ORE.defaultBlockState()),
                "the iron quarry hammer takes gold ore, the bronze one doesn't");
        ItemStack steelHammer2 = new ItemStack(Smithing.STEEL_SMITHING_HAMMER.get());
        h.assertTrue(Smithing.isHammer(steelHammer2) && Smithing.worksSteel(steelHammer2) && Smithing.wear(steelHammer2) == 1
                && Smithing.cooldown(steelHammer2) < Ironworking.HAMMER_COOLDOWN, "the steel smithing hammer works steel, wears 1, strikes quicker");
        h.assertTrue(Smithing.IRON_HOOK_FISHING_ROD.get().lureTicks > BronzeTools.BRONZE_HOOK_FISHING_ROD.get().lureTicks
                && Smithing.IRON_HOOK_FISHING_ROD.get().luck > BronzeTools.BRONZE_HOOK_FISHING_ROD.get().luck, "the iron hook rod beats the bronze one");
        // a steel sickle reaps a 5x5 of ripe wheat in one cut (x 12..16, z 0..4)
        var wheat = (net.minecraft.world.level.block.CropBlock) net.minecraft.world.level.block.Blocks.WHEAT;
        for (int x = 12; x <= 16; x++) for (int z = 0; z <= 4; z++) {
            h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.FARMLAND);
            h.setBlock(new BlockPos(x, 1, z), wheat.getStateForAge(wheat.getMaxAge()));
        }
        var reaper = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        reaper.setGameMode(GameType.SURVIVAL);
        reaper.setItemInHand(InteractionHand.MAIN_HAND, steelSickle);
        reaper.gameMode.destroyBlock(h.absolutePos(new BlockPos(14, 1, 2)));
        for (int x = 12; x <= 16; x++) for (int z = 0; z <= 4; z++) h.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.WHEAT, new BlockPos(x, 1, z));
        reaper.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
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
