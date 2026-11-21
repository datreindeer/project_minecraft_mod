package com.mechanical.wildlife.underground;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Butchery;
import com.mechanical.wildlife.mountain.MountainFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the underground fauna: the darkness rule (a sealed stone room spawns, the open air beside it and the
 * same room lit by a torch don't, nor a spot outside the band), the depth bands (hyena/bear above Y 0, centipede below)
 * and the butchering yields (each animal its own hide, meat and parts).
 */
@EventBusSubscriber(modid = MODID)
public final class UndergroundTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"underground_spawn_rule", "underground_yields"};

    static {
        TEST_FUNCTIONS.register(TESTS[0], () -> UndergroundTest::spawnRule);
        TEST_FUNCTIONS.register(TESTS[1], () -> UndergroundTest::yields);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : TESTS) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, name), new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)),
                    new TestData<>(env, Identifier.withDefaultNamespace("empty"), 400, 0, true)));
        }
    }

    private static void spawnRule(GameTestHelper h) {
        // a sealed stone room: walls x/z 2..6, floor y 1, roof y 5; inside x/z 3..5, y 2..4
        for (int x = 2; x <= 6; x++) for (int y = 1; y <= 5; y++) for (int z = 2; z <= 6; z++) {
            boolean inside = x > 2 && x < 6 && y > 1 && y < 5 && z > 2 && z < 6;
            h.setBlock(x, y, z, inside ? Blocks.AIR : Blocks.STONE);
        }
        h.setBlock(9, 0, 9, Blocks.STONE);
        BlockPos room = h.absolutePos(new BlockPos(4, 2, 4)), open = h.absolutePos(new BlockPos(9, 1, 9));
        int y = room.getY();
        var level = h.getLevel();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(Underground.canSpawn(level, room, y, y + 1), "the dark sealed room is a spawn spot (block " + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, room) + ", sky " + level.getBrightness(net.minecraft.world.level.LightLayer.SKY, room) + ", floor " + level.getBlockState(room.below()) + ", at " + level.getBlockState(room) + ")"))
                .thenExecute(() -> {
                    h.assertTrue(!Underground.canSpawn(level, room, y + 1, y + 10), "not outside the depth band");
                    h.assertTrue(!Underground.canSpawn(level, open, open.getY(), open.getY() + 1), "not under the open sky");
                    h.setBlock(3, 2, 3, Blocks.TORCH);
                })
                .thenWaitUntil(() -> h.assertTrue(!Underground.canSpawn(level, room, y, y + 1), "a torch in the room stops spawns"))
                .thenExecute(() -> bands(h))
                .thenSucceed();
    }

    /** Hyena or bear in Y 0..39, centipede in -30..-1, nothing below -30 or above 39. */
    private static void bands(GameTestHelper h) {
        RandomSource random = RandomSource.create(7);
        boolean hyena = false, bear = false;
        for (int i = 0; i < 200; i++) {
            Underground.Band upper = Underground.pick(20, random), lower = Underground.pick(-10, random);
            h.assertTrue(upper != null && lower != null, "a band at Y 20 and at Y -10");
            h.assertTrue(upper.type().get() != UndergroundFauna.GIANT_CENTIPEDE.get(), "no centipede at Y 20");
            hyena |= upper.type().get() == MountainFauna.CAVE_HYENA.get();
            bear |= upper.type().get() == MountainFauna.CAVE_BEAR.get();
            h.assertTrue(lower.type().get() == UndergroundFauna.GIANT_CENTIPEDE.get(), "only centipedes at Y -10");
        }
        h.assertTrue(hyena && bear, "both hyenas and cave bears come at Y 20");
        h.assertTrue(Underground.pick(-31, random) == null && Underground.pick(40, random) == null, "nothing outside the bands");
    }

    private static boolean has(List<Butchery.Extra> extras, Item item) {
        return extras.stream().anyMatch(e -> e.item().get() == item);
    }

    private static void yields(GameTestHelper h) {
        var c = Butchery.yieldFor(UndergroundFauna.GIANT_CENTIPEDE.get());
        h.assertTrue(c.meat().get() == UndergroundFauna.RAW_CENTIPEDE.get(), "the centipede gives its own meat");
        h.assertTrue(has(c.outer(), UndergroundFauna.CENTIPEDE_CHITIN.get()), "chitin comes off first");
        h.assertTrue(has(c.inner(), UndergroundFauna.CENTIPEDE_FANG.get()) && has(c.inner(), UndergroundFauna.CENTIPEDE_VENOM_SAC.get()),
                "then fangs and the venom sac");
        var bear = Butchery.yieldFor(MountainFauna.CAVE_BEAR.get());
        h.assertTrue(bear.meat().get() == MountainFauna.CAVE_BEAR_MEAT.get() && has(bear.outer(), MountainFauna.RAW_CAVE_BEAR_PELT.get())
                && has(bear.inner(), Items.BONE), "the cave bear: its meat, pelt and bones");
        var hyena = Butchery.yieldFor(MountainFauna.CAVE_HYENA.get());
        h.assertTrue(hyena.meat().get() == MountainFauna.HYENA_MEAT.get() && has(hyena.outer(), MountainFauna.RAW_HYENA_PELT.get())
                && has(hyena.inner(), MountainFauna.HYENA_FANG.get()) && has(hyena.inner(), Items.BONE), "the cave hyena: meat, pelt, fangs, bones");
        h.succeed();
    }

    private UndergroundTest() {}
}
