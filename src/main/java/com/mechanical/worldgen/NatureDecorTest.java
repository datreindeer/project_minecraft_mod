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
 * stays in its runnel. Phase 4 (MountainDecor): a waterfall's water stays in its fall and plunge pool, and a rock arch
 * stands over open ground with rock on both sides, as the Under the Arch advancement expects.
 */
@EventBusSubscriber(modid = MODID)
public final class NatureDecorTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final List<String> TESTS = List.of("log_bridge", "worldgen_beaver_dam", "hillside_spring", "valley_waterfall", "rock_arch", "snag");

    static {
        TEST_FUNCTIONS.register("log_bridge", () -> NatureDecorTest::logBridge);
        TEST_FUNCTIONS.register("worldgen_beaver_dam", () -> NatureDecorTest::beaverDam);
        TEST_FUNCTIONS.register("hillside_spring", () -> NatureDecorTest::spring);
        TEST_FUNCTIONS.register("valley_waterfall", () -> NatureDecorTest::waterfall);
        TEST_FUNCTIONS.register("rock_arch", () -> NatureDecorTest::arch);
        TEST_FUNCTIONS.register("snag", () -> NatureDecorTest::snag);
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

    /** Grass at y 2 west of x 8, a stone wall up to y 12 from x 8 east: the waterfall comes out of the wall's face. */
    private static void waterfall(GameTestHelper h) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++)
                for (int y = 0; y <= (x >= 8 ? 12 : 2); y++) h.setBlock(new BlockPos(x, y, z), x >= 8 || y < 2 ? Blocks.STONE : Blocks.GRASS_BLOCK);
        prime(h);
        h.assertTrue(MountainDecor.WATERFALL.place(h.getLevel(), null, RandomSource.create(4), h.absolutePos(new BlockPos(4, 3, 8))), "a waterfall is placed");
        h.runAfterDelay(100, () -> {
            boolean fall = false, pool = false;
            for (int x = 0; x <= 16; x++)
                for (int z = 0; z <= 16; z++)
                    for (int y = 1; y <= 13; y++) {
                        if (h.getBlockState(new BlockPos(x, y, z)).getFluidState().isEmpty()) continue;
                        if (y == 2 && (x == 6 || x == 7)) pool = true;
                        else if (y >= 3 && x == 7) fall = fall || y == 4;
                        else if (!(y >= 6 && x == 8)) h.fail("waterfall water outside its fall and pool at " + x + " " + y + " " + z);
                    }
            h.assertTrue(fall && pool, "water falls down the face (x 7) into the plunge pool (y 2)");
            h.succeed();
        });
    }

    /** Stone under a grass top at y 3: an arch stands over y 4 with rock 3-5 above the feet and on both sides. */
    private static void arch(GameTestHelper h) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++)
                for (int y = 0; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), y == 3 ? Blocks.GRASS_BLOCK : Blocks.STONE);
        prime(h);
        h.assertTrue(MountainDecor.ROCK_ARCH.place(h.getLevel(), null, RandomSource.create(2), h.absolutePos(new BlockPos(8, 4, 8))), "an arch is built");
        h.assertTrue(h.getBlockState(new BlockPos(8, 4, 8)).isAir() && h.getBlockState(new BlockPos(8, 5, 8)).isAir(), "the way through is open");
        boolean ok = false;
        for (Direction across : new Direction[] {Direction.EAST, Direction.SOUTH}) {
            BlockPos c = new BlockPos(8, 4, 8);
            boolean over = java.util.stream.IntStream.of(3, 4, 5).anyMatch(y -> h.getBlockState(c.above(y)).is(Blocks.STONE));
            boolean a = java.util.stream.IntStream.of(2, 3, 4).anyMatch(d -> h.getBlockState(c.relative(across, d).above()).is(Blocks.STONE));
            boolean b = java.util.stream.IntStream.of(2, 3, 4).anyMatch(d -> h.getBlockState(c.relative(across, -d).above()).is(Blocks.STONE));
            ok |= over && a && b;
        }
        h.assertTrue(ok, "rock over the way through and a foot on each side");
        h.succeed();
    }

    /**
     * Snags next to an oak, one seed after another: bark (Oak Log) only at the foot, Deadwood above, one Broken Deadwood
     * as the top, every branch stub joined to the trunk or to another stub, Shelf Fungus only on wood.
     */
    private static void snag(GameTestHelper h) {
        ground(h, 2, x -> false, z -> false);
        for (int y = 3; y <= 7; y++) h.setBlock(new BlockPos(2, y, 2), Blocks.OAK_LOG);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) h.setBlock(new BlockPos(2 + dx, 8, 2 + dz), Blocks.OAK_LEAVES);
        int snags = 0;
        for (int seed = 0; seed < 24; seed++) {
            for (int x = 5; x <= 15; x++)
                for (int z = 5; z <= 15; z++)
                    for (int y = 3; y <= 13; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            prime(h);
            if (!ForestDecor.SNAG.place(h.getLevel(), null, RandomSource.create(seed), h.absolutePos(new BlockPos(10, 3, 10)))) continue;
            int tops = 0, wood = 0, topY = 0, brokenY = -1;
            for (BlockPos p : BlockPos.betweenClosed(new BlockPos(5, 3, 5), new BlockPos(15, 13, 15))) {
                var st = h.getBlockState(p);
                if (st.is(Blocks.OAK_LOG) && p.getY() > 4 && Math.abs(p.getX() - 10) + Math.abs(p.getZ() - 10) <= 1)
                    h.fail("seed " + seed + ": bark (Oak Log) up the trunk at " + p);
                if (st.is(com.mechanical.Deadwood.DEADWOOD.get())) wood++;
                if (st.is(com.mechanical.Deadwood.DEADWOOD.get()) || st.is(com.mechanical.Deadwood.BROKEN_DEADWOOD.get())) topY = Math.max(topY, p.getY());
                if (st.is(com.mechanical.Deadwood.BROKEN_DEADWOOD.get())) {
                    tops++;
                    brokenY = p.getY();
                }
                if (st.is(com.mechanical.Deadwood.DEADWOOD_BRANCH.get())) {
                    boolean joined = false;
                    for (Direction d : Direction.values()) {
                        if (!st.getValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(d))) continue;
                        var n = h.getBlockState(p.relative(d));
                        joined |= n.is(com.mechanical.Deadwood.DEADWOOD.get()) || n.is(com.mechanical.Deadwood.DEADWOOD_BRANCH.get());
                    }
                    h.assertTrue(joined, "seed " + seed + ": branch stub at " + p + " is joined to the trunk");
                }
                if (st.is(com.mechanical.Deadwood.SHELF_FUNGUS.get()))
                    h.assertTrue(st.canSurvive(h.getLevel(), h.absolutePos(p)), "seed " + seed + ": shelf fungus at " + p + " grows on wood");
            }
            if (tops == 0 && wood == 0) continue; // a stump
            h.assertTrue(tops == 1 && brokenY == topY, "seed " + seed + ": one Broken Deadwood, at the top of the trunk (" + tops + " at y " + brokenY + ", top " + topY + ")");
            h.assertTrue(wood >= 2, "seed " + seed + ": a Deadwood trunk (" + wood + ")");
            h.assertTrue(h.getBlockState(new BlockPos(10, 3, 10)).is(Blocks.OAK_LOG), "seed " + seed + ": oak bark at the foot");
            h.assertTrue(h.getBlockState(new BlockPos(10, topY, 10)).is(com.mechanical.Deadwood.BROKEN_DEADWOOD.get()), "seed " + seed + ": a straight trunk");
            snags++;
        }
        h.assertTrue(snags >= 8, "most seeds give a whole snag (" + snags + ")");
        h.succeed();
    }

    private NatureDecorTest() {}
}
