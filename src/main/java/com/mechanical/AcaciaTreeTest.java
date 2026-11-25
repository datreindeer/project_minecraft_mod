package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The umbrella acacia (TreeShapes): over many seeds no leaf is far enough from a log to decay, every log hangs together
 * (so felling takes the whole tree), small and big trees both turn up, and one felled leaves no wood in the air.
 */
@EventBusSubscriber(modid = MODID)
public final class AcaciaTreeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "acacia_tree"));

    static {
        TEST_FUNCTIONS.register("acacia_tree", () -> AcaciaTreeTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "acacia_tree"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "acacia_tree"), new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var acacia = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValueOrThrow(TreeFeatures.ACACIA);
        BlockPos rel = new BlockPos(8, 2, 8), origin = h.absolutePos(rel);
        int small = 0, big = 0;
        for (int seed = 0; seed < 60; seed++) {
            for (BlockPos p : BlockPos.betweenClosed(0, 1, 0, 16, 16, 16)) h.setBlock(p, p.getY() == 1 ? Blocks.GRASS_BLOCK : Blocks.AIR);
            h.assertTrue(acacia.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin), "acacia grows, seed " + seed);
            Set<BlockPos> logs = new HashSet<>();
            int leaves = 0;
            for (BlockPos p : BlockPos.betweenClosed(0, 2, 0, 16, 16, 16)) {
                var state = h.getBlockState(p);
                if (state.is(Blocks.ACACIA_LOG)) logs.add(p.immutable());
                if (state.is(Blocks.ACACIA_LEAVES)) {
                    leaves++;
                    h.assertTrue(state.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE, "no leaf would decay, seed " + seed + " at " + p);
                }
            }
            // joined face to face: no log hangs by an edge or a corner
            Set<BlockPos> seen = new HashSet<>(List.of(rel));
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(seen);
            while (!queue.isEmpty()) {
                BlockPos at = queue.poll();
                for (Direction a : Direction.values()) {
                    BlockPos next = at.relative(a);
                    if (logs.contains(next) && seen.add(next)) queue.add(next);
                }
            }
            h.assertTrue(seen.size() == logs.size(), "all " + logs.size() + " logs hang together, " + seen.size() + " do, seed " + seed);
            var tree = TreeFelling.find(level, origin);
            h.assertTrue(tree != null && tree.logs().size() == logs.size(), "felling finds the whole tree, seed " + seed);
            h.assertTrue(leaves >= 15, "a crown, " + leaves + " leaves, seed " + seed);
            if (logs.size() <= 7) small++;
            if (logs.size() >= 14) big++;
        }
        h.assertTrue(small > 0 && big > 0, "young and big trees both grow: " + small + " young, " + big + " big");
        // fell the last one: nothing but the stump stays standing
        var tree = TreeFelling.find(level, origin);
        TreeFelling.fell(level, origin, tree, Direction.EAST, null, net.minecraft.world.item.ItemStack.EMPTY);
        h.runAfterDelay(30, () -> {
            for (BlockPos p : BlockPos.betweenClosed(0, 5, 0, 16, 16, 16))
                h.assertFalse(h.getBlockState(p).is(Blocks.ACACIA_LOG), "no log left in the air at " + p);
            h.succeed();
        });
    }
}
