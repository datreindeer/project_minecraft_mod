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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wild grain stands (WildGrainBlock): every kind ripens in late summer and stays ripe 11-12 days a plant, a stand over
 * about two weeks, then shatters and stands as straw till spring; a plant coming up again sows a new one beside it, but
 * not where the stand is already thick; cut ripe a plant gives its grain (a flint sickle adds Straw and its 50% bonus
 * grain), cut late mostly Straw; Emmer on the field now and then gives Wheat Grain, Einkorn doesn't.
 */
@EventBusSubscriber(modid = MODID)
public final class WildGrainTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "wild_grain"));

    static {
        TEST_FUNCTIONS.register("wild_grain", () -> WildGrainTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "wild_grain"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "wild_grain"), new FunctionGameTestInstance(TEST, TestArea.of(env, 200, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();

        // ---- shatter timing: by the calendar, the same every year ----
        for (var e : Farming.WILD_GRAINS.entrySet()) {
            WildGrainBlock g = e.getValue().get();
            WildGrainBlock.Timing t = g.timing();
            h.assertTrue(t.ripe() >= Calendar.SEASON_DAYS + 30 && t.ripe() < 2 * Calendar.SEASON_DAYS, e.getKey() + " ripens in late summer (day " + t.ripe() + ")");
            int first = Integer.MAX_VALUE, last = 0;
            for (int x = 0; x < 40; x++) {
                BlockPos p = new BlockPos(x * 7, 64, x * 3);
                int ripeDays = 0;
                for (int d = 0; d < Calendar.YEAR_DAYS; d++) {
                    if (g.stageOn(d, p) != WildGrainBlock.RIPE) continue;
                    ripeDays++;
                    first = Math.min(first, d);
                    last = Math.max(last, d);
                }
                h.assertTrue(ripeDays == t.shatter() - t.ripe() && ripeDays >= 10 && ripeDays <= 14, e.getKey() + ": a plant is ripe 10-14 days, got " + ripeDays);
                h.assertTrue(g.stageOn(t.ripe() + WildGrainBlock.JITTER + ripeDays, p) == WildGrainBlock.SHATTERED, e.getKey() + " has shattered when its window is over");
                h.assertTrue(g.stageOn(Calendar.YEAR_DAYS - 1, p) == WildGrainBlock.SHATTERED, e.getKey() + " stands as straw through the winter");
                h.assertTrue(g.stageOn(WildGrainBlock.JITTER + 2, p) == WildGrainBlock.YOUNG, e.getKey() + " comes up green in spring");
            }
            int window = last - first + 1;
            h.assertTrue(window >= 11 && window <= 18, e.getKey() + ": a stand can be cut over 1-2 weeks, got " + window + " days");
        }

        // ---- reseeding: a plant coming up sows one beside it, not in a thick stand ----
        WildGrainBlock einkorn = Farming.WILD_EINKORN.get();
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
        BlockPos mother = new BlockPos(2, 1, 2);
        h.setBlock(mother, einkorn.defaultBlockState().setValue(WildGrainBlock.STAGE, WildGrainBlock.SHATTERED));
        for (int i = 0; i < 4; i++) einkorn.reseed(level, h.absolutePos(mother), level.getRandom());
        int sown = count(h, einkorn, 0, 0, 4, 4) - 1;
        h.assertTrue(sown >= 1 && sown <= 4, "shed seed comes up beside the plant (" + sown + " new)");
        BlockPos thick = new BlockPos(7, 1, 7);
        for (int x = 5; x <= 8; x++) for (int z = 5; z <= 8; z++) if ((x + z) % 2 == 0 || x == 8 || x == 5) h.setBlock(new BlockPos(x, 1, z), einkorn.defaultBlockState());
        h.setBlock(thick, einkorn.defaultBlockState());
        int before = count(h, einkorn, 5, 5, 8, 8);
        h.assertTrue(before >= WildGrainBlock.CROWDED, "the thick patch is crowded (" + before + ")");
        for (int i = 0; i < 6; i++) einkorn.reseed(level, h.absolutePos(thick), level.getRandom());
        h.assertTrue(count(h, einkorn, 5, 5, 8, 8) == before, "no new plant where the stand is already thick");

        // ---- harvest: ripe gives grain, late mostly straw; the flint sickle's bonus ----
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos spot = new BlockPos(2, 1, 6);
        int n = 300;
        float[] ripeSickle = harvest(h, player, einkorn, WildGrainBlock.RIPE, Farming.FLINT_SICKLE.get(), spot, n);
        float[] ripeHand = harvest(h, player, einkorn, WildGrainBlock.RIPE, Items.AIR, spot, n);
        float[] lateSickle = harvest(h, player, einkorn, WildGrainBlock.SHATTERED, Farming.FLINT_SICKLE.get(), spot, n);
        float[] green = harvest(h, player, einkorn, WildGrainBlock.HEADING, Farming.FLINT_SICKLE.get(), spot, n);
        h.assertTrue(Math.abs(ripeSickle[0] - 1.5F) < 0.15F && ripeSickle[1] > 0.99F, "ripe, flint sickle: 1 grain + 50% bonus and a Straw, got " + ripeSickle[0] + " / " + ripeSickle[1]);
        h.assertTrue(Math.abs(ripeHand[0] - 0.5F) < 0.12F && ripeHand[1] == 0, "ripe, by hand: grain half the time, no straw, got " + ripeHand[0]);
        h.assertTrue(lateSickle[0] < 0.3F && lateSickle[0] > 0.1F && lateSickle[1] > 0.99F, "shattered: Straw and seldom a grain, got " + lateSickle[0] + " / " + lateSickle[1]);
        h.assertTrue(green[0] < 0.36F && green[0] > 0.14F, "green ears: now and then a grain, got " + green[0]);
        h.assertTrue(ripeSickle[0] > 5 * lateSickle[0], "cut ripe is far better than late");

        // ---- every kind gives its crop's grain; emmer tames into wheat, einkorn doesn't ----
        h.assertTrue(Farming.wildGrainItem("oats") == Farming.GRAIN_ITEMS.get("oat").get() && Farming.wildGrainItem("rice") == Herbs.ITEMS_BY_NAME.get("rice").get(),
                "oats give Oat Grain, rice Rice");
        h.assertTrue(Herbs.tamedFrom(Farming.CROPS.get("emmer").get()) == Items.WHEAT_SEEDS, "emmer on the field tames into wheat");
        h.assertTrue(Herbs.tamedFrom(Farming.CROPS.get("einkorn").get()) == null, "einkorn stays einkorn");
        // wild rice stands in shallow water
        BlockPos pool = new BlockPos(6, 1, 2);
        h.setBlock(pool.below(), Blocks.MUD);
        h.setBlock(pool, Blocks.WATER);
        WildGrainBlock rice = Farming.WILD_GRAINS.get("rice").get();
        h.assertTrue(rice.fluidOk(level, h.absolutePos(pool)) && rice.defaultBlockState().canSurvive(level, h.absolutePos(pool)), "wild rice takes shallow water on mud");
        h.assertTrue(rice.placed(level, h.absolutePos(pool), rice.defaultBlockState()).getValue(WildGrainBlock.Rice.WATERLOGGED), "and stands waterlogged");
        h.assertTrue(!einkorn.fluidOk(level, h.absolutePos(pool)), "land grain doesn't");
        h.succeed();
    }

    private static int count(GameTestHelper h, WildGrainBlock g, int x0, int z0, int x1, int z1) {
        int n = 0;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = 0; y <= 2; y++) if (h.getBlockState(new BlockPos(x, y, z)).is(g)) n++;
        return n;
    }

    /** Average grain and straw per plant of this stage broken n times with the tool (fresh each time), through the real drop path. */
    private static float[] harvest(GameTestHelper h, net.minecraft.world.entity.player.Player player, WildGrainBlock g, int stage, Item tool, BlockPos spot, int n) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(spot);
        BlockState state = g.defaultBlockState().setValue(WildGrainBlock.STAGE, stage);
        AABB box = new AABB(at).inflate(2);
        int grain = 0, straw = 0;
        for (int i = 0; i < n; i++) {
            net.minecraft.world.level.block.Block.dropResources(state, level, at, null, player, tool == Items.AIR ? ItemStack.EMPTY : new ItemStack(tool));
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
                if (e.getItem().is(Farming.GRAIN_ITEMS.get("einkorn").get())) grain += e.getItem().getCount();
                if (e.getItem().is(Farming.STRAW.get())) straw += e.getItem().getCount();
                e.discard();
            }
        }
        return new float[] {grain / (float) n, straw / (float) n};
    }
}
