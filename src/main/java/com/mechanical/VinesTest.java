package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Flora 4: a grape vine on a stone wall spreads along it as grape vine (never plain vine) and gives grapes when ripe; maize
 * puts up its upper half at age 4, which follows the lower half's age and goes with it; a crow taking a stage off a tall
 * stalk takes the top down; ripe maize gives corn cobs; teosinte tames into maize; the new thickets are what they should be.
 */
@EventBusSubscriber(modid = MODID)
public final class VinesTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> GRAPES = key("grape_vine"), MAIZE = key("maize");

    static {
        TEST_FUNCTIONS.register("grape_vine", () -> VinesTest::grapeVine);
        TEST_FUNCTIONS.register("maize", () -> VinesTest::maize);
    }

    private static ResourceKey<Consumer<GameTestHelper>> key(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "vines"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "grape_vine"), new FunctionGameTestInstance(GRAPES, TestArea.of(env, 100, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "maize"), new FunctionGameTestInstance(MAIZE, TestArea.of(env, 100, true)));
    }

    private static final BlockPos WALL_FROM = new BlockPos(1, 0, 6), WALL_TO = new BlockPos(9, 8, 6);

    private static void grapeVine(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // a stone wall 7 wide and 6 high at z = 5; the vine hangs on its south face (the vine's north side)
        for (int x = 2; x <= 8; x++) for (int y = 1; y <= 6; y++) h.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
        GrapeVineBlock vine = Vineyard.GRAPE_VINE.get();
        BlockPos start = new BlockPos(5, 2, 6);
        h.setBlock(start, vine.defaultBlockState().setValue(VineBlock.NORTH, true));
        h.assertTrue(h.getBlockState(start).canSurvive(level, h.absolutePos(start)), "a grape vine holds on a stone wall");
        var random = level.getRandom();
        for (int i = 0; i < 800 && count(h, vine) < 6; i++) {
            List<BlockPos> vines = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(WALL_FROM, WALL_TO)) if (h.getBlockState(p).is(vine)) vines.add(p.immutable());
            BlockPos p = vines.get(random.nextInt(vines.size()));
            vine.spread(h.getBlockState(p), level, h.absolutePos(p), random);
        }
        int n = count(h, vine);
        h.assertTrue(n >= 3, "the grape vine spread along the wall: " + n + " blocks");
        for (BlockPos p : BlockPos.betweenClosed(WALL_FROM, WALL_TO)) {
            h.assertTrue(!h.getBlockState(p).is(Blocks.VINE), "it spreads as grape vine, not as plain vine");
            if (h.getBlockState(p).is(vine)) h.assertTrue(h.getBlockState(p).canSurvive(level, h.absolutePos(p)), "every new piece holds on " + p);
        }
        // ripe: picking gives grapes and leaves green clusters
        h.setBlock(start, h.getBlockState(start).setValue(GrapeVineBlock.AGE, GrapeVineBlock.RIPE).setValue(GrapeVineBlock.LEAFLESS, false));
        ItemStack picked = vine.pick(level, h.absolutePos(start), h.getBlockState(start));
        h.assertTrue(picked.is(Vineyard.GRAPES.get()) && h.getBlockState(start).getValue(GrapeVineBlock.AGE) == 2, "picked grapes: " + picked);
        h.assertTrue(vine.pick(level, h.absolutePos(start), h.getBlockState(start)).isEmpty(), "nothing from green clusters");
        h.assertTrue(GrapeVineBlock.suits(0.8F) && !GrapeVineBlock.suits(0.2F), "grapes want warmth");
        h.assertTrue(DryingRackBlockEntity.of(new ItemStack(Vineyard.GRAPES.get())).output().get() == Vineyard.RAISINS.get(), "grapes dry into raisins");
        ItemStack g = new ItemStack(Vineyard.GRAPES.get());
        var wine = Kitchen.match(Kitchen.Tool.BARREL, 2, List.of(g.copy(), g.copy(), g.copy(), g.copy()));
        h.assertTrue(wine != null && wine.dish().result().get() == Vineyard.WINE.get(), "four grapes ferment into wine");
        // the new thickets
        ThicketBushBlock cranberry = Flora.THICKETS.get("cranberry").get();
        h.assertTrue(cranberry.low() && cranberry.evergreen() && cranberry.target(3) == 3, "cranberry is a low evergreen mat, berries kept into winter");
        h.assertTrue(!Flora.THICKETS.get("white_currant").get().low() && Flora.THICKETS.get("gooseberry").get().target(2) == 3, "currant and gooseberry bushes");
        h.succeed();
    }

    private static int count(GameTestHelper h, Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(WALL_FROM, WALL_TO)) if (h.getBlockState(p).is(block)) n++;
        return n;
    }

    private static void maize(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 2; x <= 6; x++) for (int z = 2; z <= 6; z++)
            h.setBlock(new BlockPos(x, 0, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
        MaizeCropBlock maize = Maize.MAIZE_CROP.get();
        BlockPos low = new BlockPos(4, 1, 4), up = low.above();
        h.setBlock(low, maize.getStateForAge(3));
        h.assertTrue(h.getBlockState(up).isAir(), "short maize is one block");
        h.setBlock(low, maize.getStateForAge(4));
        h.assertTrue(MaizeCropBlock.isUpper(h.getBlockState(up)) && maize.getAge(h.getBlockState(up)) == 4, "at age 4 it grows a top: " + h.getBlockState(up));
        h.setBlock(low, maize.getStateForAge(6));
        h.assertTrue(maize.getAge(h.getBlockState(up)) == 6, "the top follows the stalk's age");
        h.assertTrue(!Farming.isYoungCrop(h.getBlockState(up)) && Farming.isYoungCrop(h.getBlockState(low)), "only the lower half counts as the plant");
        // a crow takes a stage off a tall young stalk: down to 3, the top goes
        h.setBlock(low, maize.getStateForAge(4));
        h.assertTrue(Farming.crowPeck(level, h.absolutePos(low)) && h.getBlockState(up).isAir() && maize.getAge(h.getBlockState(low)) == 3,
                "a pecked stalk loses its top");
        // ripe: corn cobs from the lower half; breaking the top takes the whole plant
        BlockState ripe = maize.getStateForAge(7);
        List<ItemStack> drops = Block.getDrops(ripe, level, h.absolutePos(low), null);
        h.assertTrue(drops.stream().anyMatch(s -> s.is(Maize.CORN_COB.get())), "ripe maize gives corn cobs: " + drops);
        h.setBlock(low, ripe);
        h.assertTrue(MaizeCropBlock.isUpper(h.getBlockState(up)), "ripe maize is tall");
        level.destroyBlock(h.absolutePos(up), false);
        h.assertTrue(h.getBlockState(low).isAir(), "breaking the top takes the stalk");
        // real growth through random ticks, when today's weather and season let it grow here
        BlockPos grow = new BlockPos(2, 1, 2);
        h.setBlock(grow, maize.getStateForAge(3));
        if (Farming.growthRate(level, h.absolutePos(grow), Maize.CLIMATE) > 0) {
            for (int i = 0; i < 4000 && maize.getAge(h.getBlockState(grow)) < 4; i++) h.getBlockState(grow).randomTick(level, h.absolutePos(grow), level.getRandom());
            h.assertTrue(maize.getAge(h.getBlockState(grow)) >= 4 && MaizeCropBlock.isUpper(h.getBlockState(grow.above())), "maize grew tall by itself");
        }
        h.assertTrue(Herbs.tamedFrom(Maize.WILD_MAIZE_CROP.get()) == Maize.KERNELS.get(), "teosinte tames into maize");
        h.assertTrue(new ItemStack(Maize.KERNELS.get()).is(Farming.GRAINS), "maize kernels are grain (quern, porridge)");
        h.succeed();
    }

    private VinesTest() {}
}
