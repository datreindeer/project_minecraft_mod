package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for the calendar maths: start date, month and year roll-over, clock, and that day + night is one hour. */
@EventBusSubscriber(modid = MODID)
public final class CalendarTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "calendar"));

    static {
        TEST_FUNCTIONS.register("calendar", () -> CalendarTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "calendar"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "calendar"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("calendar: " + what);
    }

    private static void test(GameTestHelper h) {
        int d = Calendar.CLOCK_DAY;
        // a new world: 15 Sowing (spring), year 1, 06:00
        check(h, Calendar.season(0) == 0 && Calendar.month(0) == 1 && Calendar.dayOfMonth(0) == 15 && Calendar.year(0) == 1, "start date");
        check(h, Calendar.hour(0) == 6 && Calendar.minute(0) == 0 && Calendar.hour(18000) == 0 && Calendar.minute(6500) == 30, "clock");
        check(h, Calendar.dayOfMonth(15L * d) == 30 && Calendar.dayOfMonth(16L * d) == 1 && Calendar.month(16L * d) == 2, "month roll-over");
        long nextYear = (Calendar.YEAR_DAYS - Calendar.START_DAY) * (long) d;
        check(h, Calendar.year(nextYear - 1) == 1 && Calendar.year(nextYear) == 2 && Calendar.month(nextYear) == 0 && Calendar.dayOfMonth(nextYear) == 1, "year roll-over");
        // day + night is always one hour of game ticks; longest day at midsummer, shortest at midwinter
        for (int doy = 0; doy < Calendar.YEAR_DAYS; doy += 7) {
            long clock = Calendar.clockOn(0, doy);
            float ticks = d / 2F / Calendar.rate(clock) + d / 2F / Calendar.rate(clock + d / 2);
            check(h, Math.abs(ticks - Calendar.DAY) < 1, "day " + doy + " lasts " + ticks + " ticks");
        }
        check(h, Math.round(Calendar.dayMinutes(Calendar.clockOn(0, Calendar.MIDSUMMER))) == 40, "midsummer day 40 min");
        check(h, Math.round(Calendar.dayMinutes(Calendar.clockOn(0, Calendar.MIDSUMMER + Calendar.YEAR_DAYS / 2))) == 20, "midwinter day 20 min");
        check(h, Calendar.dayOfYear(Calendar.clockOn(5000, 0)) == 0 && Calendar.clockOn(5000, 0) % d == 5000, "jump keeps time of day");
        h.succeed();
    }
}
