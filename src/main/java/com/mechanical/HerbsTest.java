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
 */
@EventBusSubscriber(modid = MODID)
public final class HerbsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "herbs"));

    static {
        TEST_FUNCTIONS.register("herbs", () -> HerbsTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "herbs"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "herbs"), new FunctionGameTestInstance(TEST, TestArea.of(env, 200, true)));
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
