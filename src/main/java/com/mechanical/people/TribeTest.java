package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.BarkRoofBlock;
import com.mechanical.Foraging;
import com.mechanical.Mechanical;
import com.mechanical.WildBerryBushBlock;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for tribe people and their buildings.
 * <ul>
 * <li>tribe_chop: a woodcutter may fell a natural tree outside the camp, but not a log wall, a stripped pole, a log build
 * under placed leaves, or a natural tree inside the camp; Bark Roof slopes join into stair-like corners.</li>
 * <li>tribe_gate: inside a log ring with a gate on the far side, a gatherer walks out through the gate and picks a bush
 * behind the wall, and a fisher finds a shore beyond the wall.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class TribeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> CHOP = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_chop"));
    private static final ResourceKey<Consumer<GameTestHelper>> GATE = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_gate"));
    private static final int C = 20, WALL = 6; // the ring's centre (relative x, z) and radius in tribe_gate

    static {
        TEST_FUNCTIONS.register("tribe_chop", () -> TribeTest::chop);
        TEST_FUNCTIONS.register("tribe_gate", () -> TribeTest::gate);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "tribe"), new TestEnvironmentDefinition.AllOf(List.of()));
        // padded so the 41x41 test area stays clear of the other tests
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_chop"), new FunctionGameTestInstance(CHOP,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 40, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_gate"), new FunctionGameTestInstance(GATE,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 2400, 0, true, Rotation.NONE, false, 1, 1, false, 48)));
    }

    private static void guarded(GameTestHelper h, Runnable test) {
        try {
            test.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static Tribesperson person(GameTestHelper h, TribeRole role, BlockPos at, BlockPos home) {
        Tribesperson t = h.spawn(People.TRIBESPERSON.get(), at);
        t.setup(role, h.absolutePos(home), UUID.randomUUID(), false);
        return t;
    }

    /** A column of logs on dirt at x, z; leaves round its top if {@code leaves} (natural, or placed when persistent). */
    private static BlockPos column(GameTestHelper h, int x, int z, BlockState log, BlockState leaves) {
        h.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
        for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), log);
        if (leaves != null) {
            for (Direction d : Direction.Plane.HORIZONTAL) h.setBlock(new BlockPos(x, 4, z).relative(d), leaves);
            h.setBlock(new BlockPos(x, 5, z), leaves);
        }
        return h.absolutePos(new BlockPos(x, 1, z));
    }

    private static void chop(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            BlockState oak = Blocks.OAK_LOG.defaultBlockState();
            BlockState natural = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
            BlockState placed = natural.setValue(LeavesBlock.PERSISTENT, true);
            BlockPos home = new BlockPos(1, 1, 1);
            Tribesperson w = person(h, TribeRole.WOODCUTTER, home.above(), home);
            w.setWall(new Tribesperson.Wall(4, h.absolutePos(new BlockPos(7, 1, 1)))); // camp radius 4 + 2
            h.assertTrue(w.campRadius() == 6, "a wall of radius 4 makes a camp of 6 (got " + w.campRadius() + ")");

            BlockPos tree = column(h, 12, 1, oak, natural);
            BlockPos wall = column(h, 12, 6, oak, null);
            BlockPos pole = column(h, 12, 11, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), null);
            BlockPos build = column(h, 16, 6, oak, placed);
            BlockPos inside = column(h, 4, 1, oak, natural);
            h.assertTrue(ChopWoodGoal.naturalTrunk(level, tree) && ChopWoodGoal.choppable(w, level, tree), "a natural tree outside the camp is felled");
            h.assertFalse(ChopWoodGoal.choppable(w, level, tree.above()), "only from the foot of the trunk");
            h.assertFalse(ChopWoodGoal.choppable(w, level, wall), "a log wall without leaves is left alone");
            h.assertFalse(ChopWoodGoal.choppable(w, level, pole), "a stripped pole is left alone");
            h.assertFalse(ChopWoodGoal.choppable(w, level, build), "a log build under placed leaves is left alone");
            h.assertTrue(ChopWoodGoal.naturalTrunk(level, inside), "the tree inside the camp is natural");
            h.assertFalse(ChopWoodGoal.choppable(w, level, inside), "but it stands inside the camp, so it is left alone");

            // bark roof: a slope rising north with one rising west behind it becomes an outer corner peaking north-west
            BlockState roof = Mechanical.BARK_ROOFS.get("oak").get().defaultBlockState();
            BlockPos a = h.absolutePos(new BlockPos(20, 2, 20));
            level.setBlock(a, roof.setValue(BarkRoofBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            level.setBlock(a.north(), roof.setValue(BarkRoofBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
            h.assertTrue(level.getBlockState(a).getValue(BarkRoofBlock.SHAPE) == StairsShape.OUTER_LEFT,
                    "outer corner (got " + level.getBlockState(a).getValue(BarkRoofBlock.SHAPE) + ")");
            // and one rising north with one rising east in front of it: an inner corner, high north and east
            BlockPos b = h.absolutePos(new BlockPos(24, 2, 20));
            level.setBlock(b, roof.setValue(BarkRoofBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            level.setBlock(b.south(), roof.setValue(BarkRoofBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
            h.assertTrue(level.getBlockState(b).getValue(BarkRoofBlock.SHAPE) == StairsShape.INNER_RIGHT,
                    "inner corner (got " + level.getBlockState(b).getValue(BarkRoofBlock.SHAPE) + ")");
            BlockState flat = roof.setValue(BarkRoofBlock.FLAT, true);
            h.assertTrue(flat.getShape(level, a).max(Direction.Axis.Y) <= 0.125, "a flat bark roof is a thin layer");
            h.succeed();
        });
    }

    private static void gate(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            h.setTime(1000); // morning: people work by day
            // the area reaches past the test's own (1-block) structure: keep its chunks ticking so the people move
            BlockPos lo = h.absolutePos(new BlockPos(-16, 0, -16)), hi = h.absolutePos(new BlockPos(2 * C + 16, 0, 2 * C + 16));
            java.util.List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
            for (int cx = Math.min(lo.getX(), hi.getX()) >> 4; cx <= Math.max(lo.getX(), hi.getX()) >> 4; cx++)
                for (int cz = Math.min(lo.getZ(), hi.getZ()) >> 4; cz <= Math.max(lo.getZ(), hi.getZ()) >> 4; cz++)
                    if (level.setChunkForced(cx, cz, true)) forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
            h.runBeforeTestEnd(() -> forced.forEach(c -> level.setChunkForced(c.x(), c.z(), false)));
            // a grass floor 41x41, clear air above, a ring of logs 3 high round the middle with a 3-wide gate to the east
            for (int x = 0; x <= 2 * C; x++) {
                for (int z = 0; z <= 2 * C; z++) {
                    h.setBlock(new BlockPos(x, -1, z), Blocks.STONE.defaultBlockState());
                    h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    int dx = x - C, dz = z - C;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (Math.abs(d - WALL) < 0.5 && !(dx > 0 && Math.abs(dz) <= 1))
                        for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.OAK_LOG.defaultBlockState());
                }
            }
            BlockPos home = new BlockPos(C, 1, C);
            h.setBlock(home, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            // a ripe bush straight behind the wall on the far side from the gate, and a pond out west
            BlockPos bush = new BlockPos(C - 11, 1, C);
            h.setBlock(bush, Foraging.BUSH_BLOCKS.get("raspberry").get().defaultBlockState());
            for (int x = C - 15; x <= C - 11; x++) for (int z = C + 6; z <= C + 10; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.WATER.defaultBlockState());
            Tribesperson.Wall wall = new Tribesperson.Wall(WALL, h.absolutePos(new BlockPos(C + WALL + 2, 1, C)));

            Tribesperson fisher = person(h, TribeRole.FISHER, home.offset(-2, 0, 0), home);
            fisher.setWall(wall);
            FishGoal fish = new FishGoal(fisher);
            boolean found = false;
            for (int i = 0; i < 200 && !found; i++) found = fish.findSpot(level);
            h.assertTrue(found, "the fisher finds the pond beyond the wall");
            BlockPos stand = h.relativePos(fish.stand);
            double fd = Math.hypot(stand.getX() - C, stand.getZ() - C);
            h.assertTrue(fd > WALL, "the fishing spot is outside the wall (" + fd + " from the fire)");
            fisher.discard();

            Tribesperson gatherer = person(h, TribeRole.GATHERER, home.offset(2, 0, 1), home);
            gatherer.setWall(wall);
            BlockPos bushAbs = h.absolutePos(bush);
            h.succeedWhen(() -> h.assertFalse(WildBerryBushBlock.isRipe(level.getBlockState(bushAbs)),
                    "the gatherer has not picked the bush behind the wall yet (at " + h.relativePos(gatherer.blockPosition()) + ", dark "
                            + level.isDarkOutside() + ", can work " + gatherer.canWork(TribeRole.GATHERER) + ", goals "
                            + gatherer.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList() + ")"));
        });
    }

    private TribeTest() {}
}
