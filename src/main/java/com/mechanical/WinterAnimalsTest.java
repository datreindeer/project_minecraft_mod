package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.waterfowl.WaterfowlFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for WinterAnimals: when the migratory birds are away, which species migrate, and that a pair in love makes no
 * young in winter but does in spring. The season jump is made and undone within the one tick (other tests run in spring).
 */
@EventBusSubscriber(modid = MODID)
public final class WinterAnimalsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "winter_animals"));

    static {
        TEST_FUNCTIONS.register("winter_animals", () -> WinterAnimalsTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "winter_animals"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "winter_animals"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("winter animals: " + what);
    }

    private static void setClock(MinecraftServer server, long clock) {
        Calendar.overworldClock(server).ifPresent(c -> server.clockManager().setTotalTicks(c, clock));
    }

    private static void test(GameTestHelper h) {
        int s = Calendar.SEASON_DAYS, m = Calendar.MONTH_DAYS;
        check(h, !WinterAnimals.birdsAway(Calendar.clockOn(0, 0)) && !WinterAnimals.birdsAway(Calendar.clockOn(0, s))
                && !WinterAnimals.birdsAway(Calendar.clockOn(0, 2 * s + m)), "birds here spring to mid autumn");
        check(h, WinterAnimals.birdsAway(Calendar.clockOn(0, 8 * m)) && WinterAnimals.birdsAway(Calendar.clockOn(0, 3 * s))
                && WinterAnimals.birdsAway(Calendar.clockOn(0, Calendar.YEAR_DAYS - 1)), "birds away late autumn and winter");
        check(h, WinterAnimals.migrates(WaterfowlFauna.COMMON_CRANE.get()) && WinterAnimals.migrates(Wildlife.GOOSE.get())
                && !WinterAnimals.migrates(WaterfowlFauna.MALLARD.get()) && !WinterAnimals.migrates(Wildlife.BROWN_BEAR.get()), "migrants");

        var level = h.getLevel();
        MinecraftServer server = level.getServer();
        long saved = level.getOverworldClockTime();
        AABB box = new AABB(h.absolutePos(BlockPos.ZERO)).inflate(6);
        Animal a = h.spawn(net.minecraft.world.entity.EntityTypes.COW, 1, 1, 1), b = h.spawn(net.minecraft.world.entity.EntityTypes.COW, 2, 1, 1);
        try {
            setClock(server, Calendar.clockOn(saved, 3 * s + 10));
            check(h, WinterAnimals.winter(level), "jumped to winter");
            a.setInLove(null);
            b.setInLove(null);
            a.spawnChildFromBreeding(level, b);
            check(h, level.getEntitiesOfClass(Animal.class, box).size() == 2 && !a.isInLove(), "no calf in winter");
            setClock(server, Calendar.clockOn(saved, 10));
            a.setAge(0);
            b.setAge(0);
            a.setInLove(null);
            b.setInLove(null);
            a.spawnChildFromBreeding(level, b);
            check(h, level.getEntitiesOfClass(Animal.class, box).size() == 3, "a calf in spring");
        } finally {
            setClock(server, saved);
            level.getEntitiesOfClass(Animal.class, box).forEach(e -> e.discard());
        }
        h.succeed();
    }
}
