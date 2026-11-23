package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import com.mechanical.wildlife.river.Beaver;
import com.mechanical.wildlife.river.RiverFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Terrain-plan phase 3 (ForestDecor, WaterDecor) on hand-built terrain: a log bridge rests across a creek next to a tree,
 * a beaver dam crosses a closed creek and raises the water behind it only inside the creek, and a hillside spring's water
 * stays in its runnel.
 */
@EventBusSubscriber(modid = MODID)
public final class NatureDecorTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final List<String> TESTS = List.of("log_bridge", "worldgen_beaver_dam", "hillside_spring");

    static {
        TEST_FUNCTIONS.register("log_bridge", () -> NatureDecorTest::logBridge);
        TEST_FUNCTIONS.register("worldgen_beaver_dam", () -> NatureDecorTest::beaverDam);
        TEST_FUNCTIONS.register("hillside_spring", () -> NatureDecorTest::spring);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "nature_decor"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS)
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)), com.mechanical.TestArea.of(env, 120, true)));
    }

    /** Stone up to top (grass on it) in every column of 0..16, except creek columns: water at y 1 on a stone bed. */
    private static void ground(GameTestHelper h, int top, java.util.function.IntPredicate creekX, java.util.function.IntPredicate creekZ) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) {
                boolean creek = creekX.test(x) && creekZ.test(z);
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= top; y++)
                    h.setBlock(new BlockPos(x, y, z), creek ? (y == 1 ? Blocks.WATER : Blocks.AIR) : y == top ? Blocks.GRASS_BLOCK : Blocks.DIRT);
            }
    }

    /** Live chunks don't keep the worldgen heightmaps the features read: rebuild them (as Retrogen does). */
    private static void prime(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ChunkPos c = ChunkPos.containing(h.absolutePos(new BlockPos(8, 0, 8)));
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                Heightmap.primeHeightmaps(level.getChunk(c.x() + dx, c.z() + dz), EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
    }

    private static void logBridge(GameTestHelper h) {
        ground(h, 2, x -> x >= 7 && x <= 9, z -> true);
        for (int y = 3; y <= 7; y++) h.setBlock(new BlockPos(3, y, 3), Blocks.OAK_LOG);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) h.setBlock(new BlockPos(3 + dx, 8, 3 + dz), Blocks.OAK_LEAVES);
        prime(h);
        h.assertTrue(WaterDecor.LOG_BRIDGE.place(h.getLevel(), null, RandomSource.create(7), h.absolutePos(new BlockPos(8, 3, 8))), "a log bridge is built");
        boolean found = false;
        for (int z = 0; z <= 16 && !found; z++) {
            int zz = z;
            found = java.util.stream.IntStream.rangeClosed(6, 10).allMatch(x -> {
                var st = h.getBlockState(new BlockPos(x, 3, zz));
                return st.is(Blocks.OAK_LOG) && st.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X;
            });
        }
        h.assertTrue(found, "an oak log lies across the creek (x 7-9) at bank height, resting on both banks");
        h.succeed();
    }

    private static void beaverDam(GameTestHelper h) {
        ground(h, 3, x -> x == 7 || x == 8, z -> z >= 2 && z <= 14);
        prime(h);
        h.assertTrue(WaterDecor.BEAVER_DAM.place(h.getLevel(), null, RandomSource.create(3), h.absolutePos(new BlockPos(8, 4, 8))), "a beaver dam is built");
        Block dam = RiverFauna.BEAVER_DAM.get();
        int damZ = -1, raised = 0;
        for (int z = 0; z <= 16; z++) {
            int fz = z;
            if (z >= 2 && z <= 14 && java.util.stream.IntStream.of(7, 8).allMatch(x -> isDam(h, x, 1, fz) && isDam(h, x, 2, fz))) damZ = z;
            for (int x = 0; x <= 16; x++) {
                boolean water = h.getBlockState(new BlockPos(x, 2, z)).is(Blocks.WATER);
                if (water && (x < 7 || x > 8)) h.fail("pond water outside the creek at " + x + ", " + z);
                if (water) raised++;
            }
        }
        h.assertTrue(damZ >= 0, "dam blocks across the creek in two courses");
        h.assertTrue(raised >= 2, "the closed-in water behind the dam rose a block (" + raised + " cells)");
        h.assertTrue(java.util.stream.IntStream.rangeClosed(0, 16).anyMatch(z -> h.getBlockState(new BlockPos(7, 1, z)).is(dam) || h.getBlockState(new BlockPos(8, 1, z)).is(dam)),
                "Beaver Dam blocks in the water");
        var beavers = h.getLevel().getEntitiesOfClass(Beaver.class, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(24));
        h.assertTrue(!beavers.isEmpty(), "a beaver lives at the dam");
        beavers.forEach(b -> b.discard());
        h.succeed();
    }

    /** Something packed in: not water, not air (Beaver Dam, Mud or a log). */
    private static boolean isDam(GameTestHelper h, int x, int y, int z) {
        var st = h.getBlockState(new BlockPos(x, y, z));
        return !st.isAir() && st.getFluidState().isEmpty();
    }

    private static void spring(GameTestHelper h) {
        int[] top = {8, 8, 8, 8, 8, 7, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5};
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++)
                for (int y = 0; y <= top[x]; y++) h.setBlock(new BlockPos(x, y, z), y == top[x] ? Blocks.GRASS_BLOCK : y == 0 ? Blocks.STONE : Blocks.DIRT);
        prime(h);
        h.assertTrue(WaterDecor.SPRING.place(h.getLevel(), null, RandomSource.create(5), h.absolutePos(new BlockPos(4, 9, 8))), "a spring is placed on the slope");
        h.assertTrue(h.getBlockState(new BlockPos(4, 8, 8)).is(Blocks.WATER) && h.getBlockState(new BlockPos(8, 5, 8)).is(Blocks.WATER),
                "water from the spring (x 4) down the runnel to the foot of the slope (x 8)");
        h.runAfterDelay(80, () -> {
            for (int x = 0; x <= 16; x++)
                for (int z = 0; z <= 16; z++)
                    for (int y = 1; y <= 10; y++)
                        if (!h.getBlockState(new BlockPos(x, y, z)).getFluidState().isEmpty() && (z != 8 || x < 4 || x > 8))
                            h.fail("spring water got out of its runnel at " + x + " " + y + " " + z);
            h.succeed();
        });
    }

    private NatureDecorTest() {}
}
