package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for wood piles: Firewood Piles take any wood mixed (kept per layer, dropped per layer, the top piece taken back
 * is the last one put on) and stop at {@link FirewoodPileBlock#MAX_HEIGHT}; a lone Rough Log Pile column stands one block,
 * the next log rolls off to the side; three columns side by side let the middle one stand two blocks; between walls a
 * column goes up to {@link RoughLogPileBlock#MAX_ROWS} and no further.
 */
@EventBusSubscriber(modid = MODID)
public final class WoodPileTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "wood_piles"));
    private static final int OAK = Mechanical.WOODS.indexOf("oak"), BIRCH = Mechanical.WOODS.indexOf("birch"), PINE = Mechanical.WOODS.indexOf("pine");

    static {
        TEST_FUNCTIONS.register("wood_piles", () -> WoodPileTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "wood_piles"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "wood_piles"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 12; x++) for (int z = 0; z <= 6; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 6; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }

        // ---- firewood: mixed, taken back in order, dropped per layer, 3 blocks high at most
        BlockPos fw = h.absolutePos(new BlockPos(1, 1, 5));
        FirewoodPileBlock.place(level, fw, "oak", Direction.Axis.Z);
        h.assertTrue(FirewoodPileBlock.add(level, fw, "birch", null, ItemStack.EMPTY), "birch firewood goes on an oak pile");
        BlockState two = level.getBlockState(fw);
        h.assertTrue(two.getValue(FirewoodPileBlock.LAYERS) == 2, "two layers");
        int[] layers = FirewoodPileBlock.layers(level, fw, two);
        h.assertTrue(layers[0] == OAK && layers[1] == BIRCH, "kept per layer: oak, birch");
        List<ItemStack> drops = Block.getDrops(two, level, fw, level.getBlockEntity(fw));
        h.assertTrue(drops.stream().anyMatch(s -> s.is(Mechanical.FIREWOOD.get("birch").get()))
                && drops.stream().anyMatch(s -> s.is(Mechanical.FIREWOOD.get("oak").get())), "a mixed pile drops both woods");
        h.assertTrue(FirewoodPileBlock.takeOne(level, fw, two, null).is(Mechanical.FIREWOOD.get("birch").get()), "the top piece back is the birch");
        h.assertTrue(level.getBlockEntity(fw) instanceof WoodPileEntity e && e.woods().length == 0, "all oak again: nothing stored");
        int added = 0;
        while (FirewoodPileBlock.add(level, fw, added % 2 == 0 ? "pine" : "oak", null, ItemStack.EMPTY) && added < 100) added++;
        h.assertTrue(added == FirewoodPileBlock.MAX * FirewoodPileBlock.MAX_HEIGHT - 1, "stops at " + FirewoodPileBlock.MAX_HEIGHT + " blocks, took " + added);
        h.assertTrue(level.getBlockState(fw.above(2)).getBlock() instanceof FirewoodPileBlock && level.getBlockState(fw.above(3)).isAir(), "3 blocks high");

        // ---- rough logs: a lone column stands one block; the fifth log rolls off across the logs (axis z: east or west)
        BlockPos lone = h.absolutePos(new BlockPos(3, 1, 1));
        for (int i = 0; i < 4; i++) {
            RoughLogPileBlock.Outcome out = RoughLogPileBlock.add(level, lone, i % 2 == 0 ? OAK : BIRCH, Direction.Axis.Z, null, 0);
            h.assertTrue(out != null && !out.rolled() && out.pos().equals(lone), "log " + (i + 1) + " stays on the lone column");
        }
        int[] logs = RoughLogPileBlock.woods(level, lone, level.getBlockState(lone));
        h.assertTrue(logs.length == 4 && logs[0] == OAK && logs[1] == BIRCH, "the logs keep their woods");
        RoughLogPileBlock.Outcome rolled = RoughLogPileBlock.add(level, lone, PINE, Direction.Axis.Z, null, 0);
        h.assertTrue(rolled != null && rolled.rolled() && !rolled.dropped(), "the fifth log rolls off");
        h.assertTrue(rolled.pos().getY() == lone.getY() && rolled.pos().getZ() == lone.getZ() && Math.abs(rolled.pos().getX() - lone.getX()) == 1,
                "onto the ground right beside, across the logs: " + rolled.pos());
        h.assertTrue(level.getBlockState(lone.above()).isAir(), "the lone column is still one block");

        // ---- three columns side by side: the middle one stands two blocks
        BlockPos mid = h.absolutePos(new BlockPos(9, 1, 1));
        for (BlockPos col : List.of(mid.west(), mid, mid.east())) for (int i = 0; i < 4; i++) RoughLogPileBlock.add(level, col, OAK, Direction.Axis.Z, null, 0);
        for (int i = 0; i < 4; i++) {
            RoughLogPileBlock.Outcome out = RoughLogPileBlock.add(level, mid, OAK, Direction.Axis.Z, null, 0);
            h.assertTrue(out != null && !out.rolled(), "the middle column takes log " + (i + 5));
        }
        RoughLogPileBlock.Outcome off = RoughLogPileBlock.add(level, mid, OAK, Direction.Axis.Z, null, 0);
        h.assertTrue(off != null && off.rolled(), "but not a third block");

        // ---- between walls a column goes straight up to MAX_ROWS, then no further
        BlockPos walled = h.absolutePos(new BlockPos(6, 1, 4));
        for (int y = 0; y < 6; y++) {
            h.setBlock(new BlockPos(5, 1 + y, 4), Blocks.STONE_BRICKS);
            h.setBlock(new BlockPos(7, 1 + y, 4), Blocks.STONE_BRICKS);
        }
        for (int i = 0; i < RoughLogPileBlock.MAX_ROWS * 2; i++) {
            RoughLogPileBlock.Outcome out = RoughLogPileBlock.add(level, walled, OAK, Direction.Axis.Z, null, 0);
            h.assertTrue(out != null && !out.rolled(), "walls hold log " + (i + 1));
        }
        h.assertTrue(RoughLogPileBlock.add(level, walled, OAK, Direction.Axis.Z, null, 0) == null, "no log past " + RoughLogPileBlock.MAX_ROWS + " rows");
        h.assertTrue(RoughLogPileBlock.takeTop(level, walled).is(Mechanical.ROUGH_LOGS.get("oak").get()), "an empty hand takes the top log");
        h.succeed();
    }
}
