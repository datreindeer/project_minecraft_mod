package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.mechanical.FallingTreeEntity;
import com.mechanical.TreeFelling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The nature clock (terrain-plan phases 5-7) on hand-built ground, its days run by hand ({@link NatureClock#simulate}, only
 * inside the test's box): a fallen log left 10 days has rotted away into Moss Block and Podzol with a sapling beside it,
 * and a storm-felled tree comes to rest as a fallen log of log blocks.
 */
@EventBusSubscriber(modid = MODID)
public final class NatureClockTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final List<String> TESTS = List.of("nature_rot", "storm_felling");
    /** A day in early summer of year 2 (calendar day numbers count from the start of year 1). */
    static final long SUMMER = 360 + 95;

    static {
        TEST_FUNCTIONS.register("nature_rot", () -> NatureClockTest::rot);
        TEST_FUNCTIONS.register("storm_felling", () -> NatureClockTest::storm);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "nature_clock"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS)
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)), com.mechanical.TestArea.of(env, 200, true)));
    }

    /** Stone, dirt and grass up to y top in every column of 0..16, air above. */
    static void ground(GameTestHelper h, int top) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= top; y++) h.setBlock(new BlockPos(x, y, z), y == top ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                for (int y = top + 1; y <= top + 14; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
    }

    /** The test's own columns 0..16, every height: nature only changes blocks in here. */
    static BoundingBox box(GameTestHelper h) {
        BlockPos a = h.absolutePos(BlockPos.ZERO), b = h.absolutePos(new BlockPos(16, 0, 16));
        return new BoundingBox(Math.min(a.getX(), b.getX()), h.getLevel().getMinY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), h.getLevel().getMaxY(), Math.max(a.getZ(), b.getZ()));
    }

    private static void rot(GameTestHelper h) {
        ground(h, 3);
        for (int x = 4; x <= 8; x++) h.setBlock(new BlockPos(x, 4, 8), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        NatureClock.simulate(h.getLevel(), box(h), SUMMER, 10, Set.of("rot"));
        int soil = 0;
        for (int x = 4; x <= 8; x++) {
            for (int y = 3; y <= 4; y++) {
                BlockState s = h.getBlockState(new BlockPos(x, y, 8));
                if (s.is(BlockTags.LOGS)) h.fail("log still at " + x + " " + y + " after 10 days: " + s);
                if (s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.PODZOL)) soil++;
            }
        }
        h.assertTrue(soil >= 3, "the log sank into Moss Block and Podzol (" + soil + ")");
        boolean sapling = false;
        for (int x = 3; x <= 9; x++) for (int z = 7; z <= 9; z += 2) sapling |= h.getBlockState(new BlockPos(x, 4, z)).is(Blocks.OAK_SAPLING);
        h.assertTrue(sapling, "an oak sapling came up beside the log");
        h.succeed();
    }

    private static void storm(GameTestHelper h) {
        ground(h, 3);
        for (int y = 4; y <= 10; y++) h.setBlock(new BlockPos(4, y, 8), Blocks.OAK_LOG);
        for (int y = 9; y <= 11; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (y <= 10 && dx == 0 && dz == 0) continue;
                    int dist = y == 11 ? (dx == 0 && dz == 0 ? 1 : 2) : Math.abs(dx) + Math.abs(dz);
                    h.setBlock(new BlockPos(4 + dx, y, 8 + dz), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, dist));
                }
        BlockPos base = h.absolutePos(new BlockPos(4, 4, 8));
        TreeFelling.Tree tree = TreeFelling.find(h.getLevel(), base);
        h.assertTrue(tree != null, "the tree is found");
        Direction east = Direction.EAST;
        StormFelling.fell(h.getLevel(), base, tree, east);
        h.runAfterDelay(FallingTreeEntity.FALL_TICKS + 10, () -> {
            int lying = 0;
            for (int x = 5; x <= 16; x++) {
                BlockState s = h.getBlockState(new BlockPos(x, 4, 8));
                if (s.is(Blocks.OAK_LOG) && s.getValue(RotatedPillarBlock.AXIS) == east.getAxis()) lying++;
            }
            h.assertTrue(lying >= 5, "the trunk lies as a fallen log of oak logs (" + lying + ")");
            h.assertTrue(!h.getBlockState(new BlockPos(4, 6, 8)).is(Blocks.OAK_LOG), "the tree is down");
            h.assertTrue(h.getLevel().getEntitiesOfClass(com.mechanical.FelledLogEntity.class, new net.minecraft.world.phys.AABB(base).inflate(20)).isEmpty(),
                    "no felled log entity: the storm leaves log blocks");
            h.succeed();
        });
    }
}
