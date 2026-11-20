package com.mechanical.wildlife;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.wildlife.WildAnimal.WaterHabit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the water habits (WildAnimal.WaterHabit): the groups and water malus are as set, and a red deer (swims
 * properly) and a hare (swims poorly) dropped in the middle of a pond with a one-block bank make it out onto dry land.
 */
@EventBusSubscriber(modid = MODID)
public final class WildlifeSwimTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "wildlife_swim"));

    static {
        TEST_FUNCTIONS.register("wildlife_swim", () -> WildlifeSwimTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "wildlife_swim"), new TestEnvironmentDefinition.AllOf(List.of()));
        // long timeout: fresh forced chunks can take a few hundred ticks to start ticking entities
        // padded so the pond and the animals climbing out of it stay clear of the other tests
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "wildlife_swim"), new FunctionGameTestInstance(TEST,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 1600, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
    }

    private static void test(GameTestHelper h) {
        Moose moose = h.spawn(Wildlife.MOOSE.get(), new Vec3(0.5, 1, 0.5));
        moose.setNoAi(true);
        h.assertTrue(moose.waterHabit() == WaterHabit.SWIMMER && moose.getPathfindingMalus(PathType.WATER) == 0.0F, "the moose takes to the water");
        moose.discard();

        // the area reaches past the test's own (1-block) structure: keep its chunks ticking so the animals move
        var level = h.getLevel();
        BlockPos lo = h.absolutePos(new BlockPos(-2, 0, -2)), hi = h.absolutePos(new BlockPos(16, 0, 16));
        List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
        for (int cx = Math.min(lo.getX(), hi.getX()) >> 4; cx <= Math.max(lo.getX(), hi.getX()) >> 4; cx++)
            for (int cz = Math.min(lo.getZ(), hi.getZ()) >> 4; cz <= Math.max(lo.getZ(), hi.getZ()) >> 4; cz++)
                if (level.setChunkForced(cx, cz, true)) forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
        h.runBeforeTestEnd(() -> forced.forEach(c -> level.setChunkForced(c.x(), c.z(), false)));
        // grass 15x15 with a pond two deep in the middle, its surface a block below the bank (stone basin, so nothing leaks)
        for (int x = 0; x <= 14; x++) for (int z = 0; z <= 14; z++) {
            boolean pond = x >= 3 && x <= 11 && z >= 3 && z <= 11;
            h.setBlock(new BlockPos(x, -3, z), Blocks.STONE.defaultBlockState());
            for (int y = -2; y <= -1; y++) h.setBlock(new BlockPos(x, y, z), pond ? Blocks.WATER.defaultBlockState() : Blocks.STONE.defaultBlockState());
            h.setBlock(new BlockPos(x, 0, z), pond ? Blocks.AIR.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        RedDeer deer = h.spawn(Wildlife.RED_DEER.get(), new Vec3(7.5, -1.5, 7.5));
        Hare hare = h.spawn(Wildlife.HARE.get(), new Vec3(6.5, -1.5, 8.5));
        h.assertTrue(deer.waterHabit() == WaterHabit.AVOIDS && deer.getPathfindingMalus(PathType.WATER) > 0.0F, "the red deer avoids water");
        h.assertTrue(hare.waterHabit() == WaterHabit.POOR, "the hare swims poorly");
        // each counts once it has stood on the bank (it may wander off into unticked chunks afterwards)
        double bank = h.absoluteVec(new Vec3(0, 1, 0)).y - 0.01;
        boolean[] out = new boolean[2];
        h.succeedWhen(() -> {
            out[0] |= !deer.isInWater() && deer.getY() >= bank;
            out[1] |= !hare.isInWater() && hare.getY() >= bank;
            h.assertTrue(out[0], "the red deer swims out of the pond onto the bank (at " + h.relativeVec(deer.position()) + ")");
            h.assertTrue(out[1], "the hare paddles out of the pond onto the bank (at " + h.relativeVec(hare.position()) + ")");
        });
    }

    private WildlifeSwimTest() {}
}
