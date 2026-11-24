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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Flora and food phases 4-5: a ripe wild crop on farmland now and then gives its tamed seed (about a quarter of
 * harvests); dishes are found by their cookware and tier (the clay pot can't make bronze dishes, twice the ingredients
 * make twice the dish); the kettle cooks a named dish; a griddle cooks flatbread over a lit campfire and not over a cold
 * one; the barrel ferments sauerkraut with time alone; a herb cut with a knife grows back.
 * Test "kitchen" (phase 6 and the phase 4-5 leftovers): rice grows only with water beside it (a paddy); the mortar grinds
 * Kitchen Pepper from the caravan's spices in eight clicks, turning the pestle; the Steel Pan fries Peppered Steak over a
 * lit fire; cider, ale (hops or bog myrtle) and roast with herbs are found; pies bake in the kiln; the knife set beats a
 * steel knife.
 * Test "flora_uses" (flora3): the Cheese Press presses olive and rapeseed oil, the skillet fries with oil or lard,
 * olives pickle in the barrel, bitter orange marmalade needs the kettle, Frost Lily Tea the clay pot; the Wild Turnip
 * tames into both Turnip and Rapeseed; ale makes you Tipsy, a step more per drink, capped; a Wild Apple sapling grafts
 * into an Apple sapling at its stage and Apple leaves give apples; a Bitter Orange sapling won't grow in a cool biome;
 * Moonwort's lock-springing opens an iron door and an iron trapdoor; the Fern Flower Charm sits in the Charm slot.
 */
@EventBusSubscriber(modid = MODID)
public final class HerbsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "herbs"));
    private static final ResourceKey<Consumer<GameTestHelper>> KITCHEN = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "kitchen"));

    static {
        TEST_FUNCTIONS.register("herbs", () -> HerbsTest::test);
        TEST_FUNCTIONS.register("kitchen", () -> HerbsTest::kitchen);
        TEST_FUNCTIONS.register("flora_uses", () -> HerbsTest::floraUses);
    }
    private static final ResourceKey<Consumer<GameTestHelper>> FLORA_USES = ResourceKey.create(Registries.TEST_FUNCTION,
            Identifier.fromNamespaceAndPath(MODID, "flora_uses"));

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "herbs"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "herbs"), new FunctionGameTestInstance(TEST, TestArea.of(env, 200, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "kitchen"), new FunctionGameTestInstance(KITCHEN, TestArea.of(env, 200, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "flora_uses"), new FunctionGameTestInstance(FLORA_USES, TestArea.of(env, 200, true)));
    }

    private static void kitchen(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);

        // ---- rice needs a flooded paddy: water beside its farmland ----
        for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
        BlockPos soil = new BlockPos(2, 1, 2), rice = soil.above();
        h.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 7));
        h.setBlock(rice, Herbs.CROPS.get("rice").get().defaultBlockState());
        h.assertFalse(Herbs.paddyOk(level, h.absolutePos(rice)), "rice on plain farmland is not in a paddy");
        h.assertTrue(Farming.growthRate(level, h.absolutePos(rice), Farming.climate(Herbs.CROPS.get("rice").get())) == 0, "rice on dry-footed farmland doesn't grow");
        h.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        h.setBlock(new BlockPos(3, 1, 2), Blocks.WATER);
        h.assertTrue(Herbs.paddyOk(level, h.absolutePos(rice)), "water beside the farmland makes a paddy");
        h.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        BlockPos onion = new BlockPos(1, 2, 2);
        h.setBlock(new BlockPos(1, 1, 2), Blocks.FARMLAND);
        h.setBlock(onion, Herbs.CROPS.get("onion").get().defaultBlockState());
        h.assertTrue(Herbs.paddyOk(level, h.absolutePos(onion)), "other crops need no paddy");

        // ---- the mortar grinds the caravan's spices into Kitchen Pepper by hand ----
        BlockPos mortarPos = new BlockPos(6, 1, 6);
        h.setBlock(mortarPos.below(), Blocks.STONE);
        h.setBlock(mortarPos, Kitchen.MORTAR_AND_PESTLE.get().defaultBlockState());
        CookwareBlockEntity mortar = (CookwareBlockEntity) level.getBlockEntity(h.absolutePos(mortarPos));
        for (var item : List.of(Herbs.BLACK_PEPPER.get(), Herbs.ITEMS_BY_NAME.get("ginger").get(), Herbs.CINNAMON_BARK.get())) {
            ItemStack s = of(item);
            mortar.add(level, player, hand, s);
        }
        h.assertTrue(mortar.items().size() == 3, "pepper, ginger and cinnamon go in the mortar: " + mortar.items());
        for (int i = 0; i < 200; i++) mortar.serverTick(level, h.absolutePos(mortarPos), h.getBlockState(mortarPos));
        h.assertFalse(mortar.done(), "the mortar does nothing by itself");
        var facing = h.getBlockState(mortarPos).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        mortar.takeOut(level, player);
        h.assertTrue(h.getBlockState(mortarPos).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == facing.getClockWise(),
                "a click turns the pestle a quarter round");
        for (int i = 1; i < CookwareBlock.Kind.MORTAR.clicks; i++) mortar.takeOut(level, player);
        h.assertTrue(mortar.done() && mortar.items().getFirst().is(Kitchen.KITCHEN_PEPPER.get()) && mortar.items().getFirst().getCount() == 3,
                "eight clicks grind three Kitchen Pepper: " + mortar.items());
        h.assertTrue(Kitchen.match(Kitchen.Tool.MORTAR, 3, List.of(of(Herbs.BLACK_PEPPER.get()), of(Items.DIRT))) == null, "a stray item spoils the blend");

        // ---- the Steel Pan fries Peppered Steak over a lit campfire ----
        BlockPos fire = new BlockPos(9, 1, 6), pan = fire.above();
        h.setBlock(fire.below(), Blocks.STONE);
        h.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        h.setBlock(pan, Kitchen.STEEL_PAN.get().defaultBlockState());
        CookwareBlockEntity steel = (CookwareBlockEntity) level.getBlockEntity(h.absolutePos(pan));
        for (var item : List.of(Items.BEEF, Items.BEEF, Kitchen.KITCHEN_PEPPER.get())) {
            ItemStack s = of(item);
            steel.add(level, player, hand, s);
        }
        for (int i = 0; i < Kitchen.PAN_TIME + 5; i++) steel.serverTick(level, h.absolutePos(pan), h.getBlockState(pan));
        h.assertTrue(steel.done() && steel.items().getFirst().is(Kitchen.PEPPERED_STEAK.get()) && steel.items().getFirst().getCount() == 2,
                "two beef and Kitchen Pepper fry into two Peppered Steaks: " + steel.items());
        List<ItemStack> saffron = List.of(of(Herbs.ITEMS_BY_NAME.get("rice").get()), of(Herbs.ITEMS_BY_NAME.get("rice").get()), of(Kitchen.GOLDEN_SPICE.get()),
                of(Herbs.ITEMS_BY_NAME.get("onion").get()));
        h.assertTrue(Kitchen.match(Kitchen.Tool.PAN, 2, saffron) == null, "the iron skillet can't make the steel pan's saffron rice");
        Kitchen.Match sr = Kitchen.match(Kitchen.Tool.PAN, 3, saffron);
        h.assertTrue(sr != null && sr.dish().name().equals("saffron_rice"), "the steel pan makes saffron rice: " + sr);

        // ---- the leftovers: cider, ale with either bittering, roast with herbs, pies in the kiln ----
        Item apple = Trees.FRUITS.get("wild_apple").get();
        Kitchen.Match cider = Kitchen.match(Kitchen.Tool.BARREL, 2, List.of(of(apple), of(apple), of(apple), of(apple)));
        h.assertTrue(cider != null && cider.dish().name().equals("cider") && cider.count() == 3, "four crab apples ferment into cider: " + cider);
        for (Item bitter : List.of(Herbs.HERB_ITEMS.get("hop_cones").get(), Herbs.HERB_ITEMS.get("bog_myrtle_leaves").get())) {
            Item barley = Farming.GRAIN_ITEMS.get("barley").get();
            Kitchen.Match ale = Kitchen.match(Kitchen.Tool.BARREL, 2, List.of(of(barley), of(bitter), of(barley), of(barley)));
            h.assertTrue(ale != null && ale.dish().name().equals("ale"), "barley and " + bitter + " brew ale: " + ale);
        }
        Kitchen.Match roast = Kitchen.match(Kitchen.Tool.PAN, 2, List.of(of(Items.MUTTON), of(Herbs.HERB_ITEMS.get("thyme").get()), of(Items.MUTTON),
                of(Herbs.HERB_ITEMS.get("rosemary_sprigs").get())));
        h.assertTrue(roast != null && roast.dish().name().equals("roast_with_herbs"), "meat and herbs roast in the skillet: " + roast);
        for (String f : Kitchen.PIE_FRUITS) {
            Item raw = Kitchen.RAW_PIES.get(f).get(), baked = Kitchen.PIES.get(f).get();
            h.assertTrue(KilnBlockEntity.RECIPES.stream().anyMatch(r -> r.input().test(of(raw)) && r.result().get() == baked), "the kiln bakes the " + f + " pie");
        }
        h.assertTrue(Kitchen.STEEL_KNIFE_SET.get().yieldMultiplier() > Smithing.STEEL_KNIFE.get().yieldMultiplier()
                && of(Kitchen.STEEL_KNIFE_SET.get()).is(Watercraft.KNIVES), "the knife set gets more from a carcass than a steel knife, and is a knife");
        h.succeed();
    }

    private static void floraUses(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Item olives = Trees.FRUITS.get("olive").get(), rapeseed = Herbs.ITEMS_BY_NAME.get("rapeseed").get();

        // ---- oils from the press, frying with them, pickled olives, marmalade, Frost Lily Tea ----
        Kitchen.Match oil = Kitchen.match(Kitchen.Tool.PRESS, 2, List.of(of(olives), of(olives), of(olives), of(olives)));
        h.assertTrue(oil != null && oil.dish().result().get() == Kitchen.OLIVE_OIL.get() && oil.count() == 1, "four olives press into Olive Oil: " + oil);
        Kitchen.Match rape = Kitchen.match(Kitchen.Tool.PRESS, 2, List.of(of(rapeseed), of(rapeseed), of(rapeseed), of(rapeseed)));
        h.assertTrue(rape != null && rape.dish().result().get() == Kitchen.RAPESEED_OIL.get(), "four rapeseed press into Rapeseed Oil: " + rape);
        for (Item fat : List.of(Kitchen.OLIVE_OIL.get(), Kitchen.RAPESEED_OIL.get(), com.mechanical.wildlife.Wildlife.LARD.get())) {
            Kitchen.Match fish = Kitchen.match(Kitchen.Tool.PAN, 2, List.of(of(Items.COD), of(fat), of(Items.SALMON)));
            h.assertTrue(fish != null && fish.dish().result().get() == Kitchen.FRIED_FISH.get() && fish.count() == 2, "two fish fry in " + fat + ": " + fish);
        }
        h.assertTrue(Kitchen.match(Kitchen.Tool.PAN, 1, List.of(of(Items.COD), of(Kitchen.OLIVE_OIL.get()), of(Items.SALMON))) == null,
                "frying fish wants the iron skillet, not the bronze griddle");
        Kitchen.Match roots = Kitchen.match(Kitchen.Tool.PAN, 2, List.of(of(Items.POTATO), of(Herbs.ITEMS_BY_NAME.get("turnip").get()),
                of(Kitchen.RAPESEED_OIL.get()), of(Herbs.ITEMS_BY_NAME.get("onion").get())));
        h.assertTrue(roots != null && roots.dish().result().get() == Kitchen.FRIED_ROOTS.get(), "roots and onion fry in oil: " + roots);
        Kitchen.Match pickled = Kitchen.match(Kitchen.Tool.BARREL, 2, List.of(of(olives), of(IronOres.SALT.get()), of(olives), of(olives)));
        h.assertTrue(pickled != null && pickled.dish().result().get() == Kitchen.PICKLED_OLIVES.get(), "olives pickle in brine: " + pickled);
        Item orange = Trees.FRUITS.get("bitter_orange").get();
        List<ItemStack> marmalade = List.of(of(orange), of(orange), of(orange), of(Items.HONEY_BOTTLE));
        h.assertTrue(Kitchen.match(Kitchen.Tool.POT, 0, marmalade) == null, "the clay pot can't make marmalade");
        Kitchen.Match m = Kitchen.match(Kitchen.Tool.POT, 1, marmalade);
        h.assertTrue(m != null && m.dish().result().get() == Kitchen.BITTER_ORANGE_MARMALADE.get(), "the kettle boils bitter orange marmalade: " + m);
        Kitchen.Match tea = Kitchen.match(Kitchen.Tool.POT, 0, List.of(of(Flora.FROST_LILY.get())));
        h.assertTrue(tea != null && tea.dish().result().get() == Kitchen.FROST_LILY_TEA.get() && tea.count() == 2, "a Frost Lily brews two bowls of tea: " + tea);

        // ---- the Wild Turnip tames into Turnip and Rapeseed ----
        int turnips = 0, seeds = 0;
        for (int i = 0; i < 400; i++) {
            Item t = Herbs.tamedFrom(Herbs.WILD_CROPS.get("turnip").get(), level.getRandom().nextBoolean());
            if (t == Herbs.ITEMS_BY_NAME.get("turnip").get()) turnips++;
            if (t == rapeseed) seeds++;
        }
        h.assertTrue(turnips > 120 && seeds > 120 && turnips + seeds == 400, "the wild turnip tames into both: " + turnips + " turnips, " + seeds + " rapeseed");
        h.assertTrue(Herbs.tamedFrom(Herbs.WILD_CROPS.get("onion").get(), true) == Herbs.ITEMS_BY_NAME.get("onion").get(), "only the turnip has an oil-seed sister");
        h.assertTrue(Farming.climate(Herbs.CROPS.get("rapeseed").get()) != null, "rapeseed follows the farming rules");

        // ---- Tipsy: a step stronger per drink, capped ----
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        Kitchen.drinkAlcohol(player);
        h.assertTrue(player.getEffect(Kitchen.TIPSY).getAmplifier() == 0, "one ale: Tipsy I");
        Kitchen.drinkAlcohol(player);
        Kitchen.drinkAlcohol(player);
        h.assertTrue(player.getEffect(Kitchen.TIPSY).getAmplifier() == Kitchen.TIPSY_SWAY_FROM, "the third in a row: the view starts to sway");
        for (int i = 0; i < 5; i++) Kitchen.drinkAlcohol(player);
        h.assertTrue(player.getEffect(Kitchen.TIPSY).getAmplifier() == Kitchen.TIPSY_MAX && player.getEffect(Kitchen.TIPSY).getDuration() == Kitchen.TIPSY_TICKS,
                "it never gets worse than IV, and lasts two minutes from the last drink");

        // ---- grafting: a Wild Apple sapling becomes an Apple sapling at its stage; Apple leaves give apples ----
        h.setBlock(new BlockPos(2, 0, 2), Blocks.GRASS_BLOCK);
        BlockPos sap = new BlockPos(2, 1, 2);
        h.setBlock(sap, Trees.WOODS.get("wild_apple").sapling().get().defaultBlockState().setValue(net.minecraft.world.level.block.SaplingBlock.STAGE, 1));
        Trees.graftApple(level, h.absolutePos(sap), h.getBlockState(sap));
        h.assertTrue(h.getBlockState(sap).is(Trees.APPLE_SAPLING.get()) && h.getBlockState(sap).getValue(net.minecraft.world.level.block.SaplingBlock.STAGE) == 1,
                "the grafted sapling is an Apple sapling, as far grown as before");
        BlockPos leaves = new BlockPos(4, 2, 4);
        h.setBlock(leaves, Trees.APPLE_LEAVES.get().defaultBlockState().setValue(FruitLeavesBlock.STAGE, FruitLeavesBlock.RIPE)
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        ItemStack picked = ((FruitLeavesBlock) Trees.APPLE_LEAVES.get()).pick(level, h.absolutePos(leaves), h.getBlockState(leaves));
        h.assertTrue(picked.is(Items.APPLE), "ripe Apple leaves give apples, not crab apples: " + picked);

        // ---- citrus wants warmth: in this test world's biome a Bitter Orange sapling stays a sapling ----
        BlockPos citrus = new BlockPos(7, 1, 2);
        h.setBlock(citrus.below(), Blocks.GRASS_BLOCK);
        var orangeSapling = Trees.WOODS.get("bitter_orange").sapling().get();
        h.setBlock(citrus, orangeSapling.defaultBlockState().setValue(net.minecraft.world.level.block.SaplingBlock.STAGE, 1));
        boolean warm = Trees.warmEnoughForCitrus(level, h.absolutePos(citrus));
        for (int i = 0; i < 3 && h.getBlockState(citrus).is(orangeSapling); i++)
            orangeSapling.advanceTree(level, h.absolutePos(citrus), h.getBlockState(citrus), level.getRandom());
        h.assertTrue(warm != h.getBlockState(citrus).is(orangeSapling), "a bitter orange sapling grows in a warm biome and only there (warm here: " + warm + ")");

        // ---- Moonwort springs iron doors and trapdoors ----
        BlockPos door = new BlockPos(10, 1, 6);
        h.setBlock(door.below(), Blocks.STONE);
        h.setBlock(door, Blocks.IRON_DOOR.defaultBlockState());
        h.setBlock(door.above(), Blocks.IRON_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        h.assertTrue(Flora.springLock(level, h.absolutePos(door), h.getBlockState(door), player)
                && h.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "moonwort opens the iron door");
        BlockPos trap = new BlockPos(12, 1, 6);
        h.setBlock(trap, Blocks.IRON_TRAPDOOR.defaultBlockState());
        h.assertTrue(Flora.springLock(level, h.absolutePos(trap), h.getBlockState(trap), player)
                && h.getBlockState(trap).getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN), "and the iron trapdoor");
        h.assertFalse(Flora.springLock(level, h.absolutePos(door.below()), h.getBlockState(door.below()), player), "but not a stone");
        h.assertTrue(of(Flora.FERN_FLOWER_CHARM.get()).is(com.mechanical.equipment.Accessory.CHARM.tag), "the Fern Flower Charm is worn in the Charm slot");
        h.succeed();
    }

    private static ItemStack of(net.minecraft.world.level.ItemLike item) {
        return new ItemStack(item);
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();

        // ---- domestication: harvest a ripe Wild Onion 400 times through the real drops ----
        BlockPos soil = new BlockPos(2, 0, 2), crop = soil.above();
        FarmCropBlock wild = Herbs.WILD_CROPS.get("onion").get();
        int tamed = 0, wildOnions = 0;
        BlockPos abs = h.absolutePos(crop);
        for (int i = 0; i < 400; i++) {
            h.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(Fertility.FERTILITY, 1));
            h.setBlock(crop, wild.getStateForAge(wild.getMaxAge()));
            BlockState ripe = h.getBlockState(crop);
            Block.dropResources(ripe, level, abs, null, null, ItemStack.EMPTY);
            h.setBlock(crop, Blocks.AIR);
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(1.5))) {
                if (e.getItem().is(Herbs.ITEMS_BY_NAME.get("onion").get())) tamed += e.getItem().getCount();
                if (e.getItem().is(Herbs.WILD_ITEMS.get("onion").get())) wildOnions += e.getItem().getCount();
                e.discard();
            }
        }
        h.assertTrue(wildOnions >= 400, "a ripe wild crop always gives its wild produce, got " + wildOnions + " from 400");
        h.assertTrue(tamed >= 60 && tamed <= 150, "about a quarter of ripe wild onions give an Onion (25 %), got " + tamed + " of 400");
        h.assertTrue(Herbs.tamedFrom(Herbs.CROPS.get("onion").get()) == null, "a tamed crop doesn't tame further");
        h.assertTrue(Farming.climate(Herbs.CROPS.get("cabbage").get()) != null && Farming.climate(Herbs.CROPS.get("rice").get()) != null,
                "the new crops follow the farming rules");

        // ---- dishes by cookware and tier ----
        List<ItemStack> nettle = List.of(of(Spinning.NETTLE_STALKS.get()), of(Herbs.WILD_ITEMS.get("garlic").get()), of(Spinning.NETTLE_STALKS.get()));
        Kitchen.Match m = Kitchen.match(Kitchen.Tool.POT, 0, nettle);
        h.assertTrue(m != null && m.dish().name().equals("nettle_soup") && m.count() == 3, "nettle soup in the clay pot: " + m);
        List<ItemStack> peas = List.of(of(Farming.PEAS.get()), of(Farming.PEAS.get()), of(Farming.PEAS.get()), of(Herbs.ITEMS_BY_NAME.get("onion").get()));
        h.assertTrue(Kitchen.match(Kitchen.Tool.POT, 0, peas) == null, "the clay pot can't make the kettle's pea soup");
        Kitchen.Match pea = Kitchen.match(Kitchen.Tool.POT, 1, peas);
        h.assertTrue(pea != null && pea.dish().name().equals("pea_soup"), "the bronze kettle makes pea soup");
        List<ItemStack> twice = new java.util.ArrayList<>(peas);
        twice.addAll(peas);
        Kitchen.Match two = Kitchen.match(Kitchen.Tool.POT, 2, twice);
        h.assertTrue(two != null && two.times() == 2 && two.count() == 8, "twice the ingredients make twice the soup: " + two);
        h.assertTrue(Kitchen.match(Kitchen.Tool.POT, 2, List.of(of(Farming.PEAS.get()), of(Items.DIRT))) == null, "a stray item spoils the match");
        Kettle.Result soup = Kettle.finish(level, peas, of(Bronze.BRONZE_KETTLE.get()));
        h.assertTrue(soup.portion().is(Kitchen.PEA_SOUP.get()) && soup.portions() == 4, "the kettle ladles 4 bowls of pea soup, got "
                + soup.portion() + " x" + soup.portions());
        Kettle.Result clay = Kettle.finish(level, List.of(of(Items.CARROT), of(Items.BEEF)), of(Kitchen.CLAY_COOKING_POT_ITEM.get()));
        h.assertTrue(clay.portions() == 0 && clay.outputs().size() == 2, "the clay pot gives back what is no dish of its own");

        // ---- a griddle over a campfire: lit it cooks flatbread, cold it doesn't ----
        BlockPos fire = new BlockPos(6, 1, 6), pan = fire.above();
        h.setBlock(new BlockPos(6, 0, 6), Blocks.STONE);
        h.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        h.setBlock(pan, Kitchen.BRONZE_GRIDDLE.get().defaultBlockState());
        CookwareBlockEntity griddle = (CookwareBlockEntity) level.getBlockEntity(h.absolutePos(pan));
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack dough = new ItemStack(Farming.DOUGH.get(), 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dough);
        griddle.add(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, dough);
        griddle.add(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, dough);
        h.assertTrue(griddle.items().size() == 2 && dough.isEmpty(), "two dough go on the griddle");
        for (int i = 0; i < Kitchen.PAN_TIME + 5; i++) griddle.serverTick(level, h.absolutePos(pan), h.getBlockState(pan));
        h.assertFalse(griddle.done(), "a cold campfire cooks nothing");
        h.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (int i = 0; i < Kitchen.PAN_TIME + 5; i++) griddle.serverTick(level, h.absolutePos(pan), h.getBlockState(pan));
        h.assertTrue(griddle.done() && griddle.items().getFirst().is(Kitchen.FLATBREAD.get()) && griddle.items().getFirst().getCount() == 4,
                "two dough bake into four flatbreads over the fire: " + griddle.items());

        // ---- the clay pot in the embers: water, nettle soup ingredients, a lit fire, then a bowl ladles soup ----
        BlockPos potFire = new BlockPos(6, 1, 10), potPos = potFire.above();
        h.setBlock(new BlockPos(6, 0, 10), Blocks.STONE);
        h.setBlock(potFire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        h.setBlock(potPos, Kitchen.CLAY_COOKING_POT.get().defaultBlockState());
        TripodBlockEntity pot = (TripodBlockEntity) level.getBlockEntity(h.absolutePos(potPos));
        pot.setVessel(of(Kitchen.CLAY_COOKING_POT_ITEM.get()));
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        pot.use(level, player, hand, new ItemStack(Items.WATER_BUCKET));
        pot.use(level, player, hand, of(Items.BEEF));
        h.assertTrue(pot.contents().isEmpty(), "raw beef is no clay pot dish and stays out");
        for (var item : List.of(Spinning.NETTLE_STALKS.get(), Spinning.NETTLE_STALKS.get(), Herbs.WILD_ITEMS.get("onion").get())) pot.use(level, player, hand, of(item));
        h.assertTrue(pot.contents().size() == 3, "the nettle soup goes in: " + pot.contents());
        for (int i = 0; i < 2600; i++) pot.serverTick(level, h.absolutePos(potPos), h.getBlockState(potPos));
        player.getInventory().clearContent();
        pot.use(level, player, hand, new ItemStack(Items.BOWL));
        h.assertTrue(player.getInventory().contains(of(Kitchen.NETTLE_SOUP.get())), "a bowl ladles Nettle Soup out of the clay pot");

        // ---- the barrel ferments with time alone ----
        BlockPos barrelPos = new BlockPos(9, 1, 6);
        h.setBlock(new BlockPos(9, 0, 6), Blocks.STONE);
        h.setBlock(barrelPos, Kitchen.FERMENTING_BARREL.get().defaultBlockState());
        CookwareBlockEntity barrel = (CookwareBlockEntity) level.getBlockEntity(h.absolutePos(barrelPos));
        for (var item : List.of(Herbs.ITEMS_BY_NAME.get("cabbage").get(), Herbs.ITEMS_BY_NAME.get("cabbage").get(), IronOres.SALT.get())) {
            ItemStack s = of(item);
            barrel.add(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, s);
        }
        h.assertFalse(h.getBlockState(barrelPos).getValue(CookwareBlock.OPEN), "the barrel is shut while it ferments");
        for (int i = 0; i < Kitchen.BARREL_TIME + 5; i++) barrel.serverTick(level, h.absolutePos(barrelPos), h.getBlockState(barrelPos));
        h.assertTrue(barrel.done() && barrel.items().getFirst().is(Kitchen.SAUERKRAUT.get()) && barrel.items().getFirst().getCount() == 3,
                "two cabbages and salt ferment into three sauerkraut: " + barrel.items());

        // ---- a herb cut with a knife, then grown back ----
        BlockPos herbPos = new BlockPos(12, 1, 6);
        h.setBlock(new BlockPos(12, 0, 6), Blocks.GRASS_BLOCK);
        h.setBlock(herbPos, Herbs.HERBS.get("wild_thyme").get().defaultBlockState());
        HerbBlock thyme = Herbs.HERBS.get("wild_thyme").get();
        ItemStack cut = thyme.cut(level.getRandom(), true);
        h.assertTrue(cut.is(thyme.gives()) && cut.getCount() >= 1 && cut.getCount() <= 3, "a knife cuts 1-3 thyme: " + cut);
        h.setBlock(herbPos, h.getBlockState(herbPos).setValue(HerbBlock.CUT, true));
        for (int i = 0; i < 200 && h.getBlockState(herbPos).getValue(HerbBlock.CUT); i++) {
            h.getBlockState(herbPos).randomTick(level, h.absolutePos(herbPos), level.getRandom());
        }
        h.assertTrue(!h.getBlockState(herbPos).getValue(HerbBlock.CUT) || Calendar.season(level.getOverworldClockTime()) == 3, "a cut herb grows back");
        h.succeed();
    }
}
