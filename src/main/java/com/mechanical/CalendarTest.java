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

/** Game test for the calendar maths (start date, roll-overs, clock, day + night is one hour) and the seasons' temperature. */
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
        // seasons: warmest at midsummer, coldest at midwinter; temperate biomes snow in winter, deserts stay hot
        long midsummer = Calendar.clockOn(0, Calendar.MIDSUMMER), midwinter = Calendar.clockOn(0, Calendar.MIDSUMMER + Calendar.YEAR_DAYS / 2);
        check(h, Seasons.curve(midsummer) > 0.99F && Seasons.curve(midwinter) < -0.99F, "season curve");
        check(h, 0.7F + Seasons.offset(0.7F, -1) < 0.15F && 0.8F + Seasons.offset(0.8F, -1) < 0.15F, "forest and plains snow at midwinter");
        check(h, 0.7F + Seasons.offset(0.7F, Seasons.curve(Calendar.clockOn(0, 2 * Calendar.SEASON_DAYS))) >= 0.15F, "no snow in the forest at the start of autumn");
        check(h, 2.0F + Seasons.offset(2.0F, -1) > 1.5F && 0.95F + Seasons.offset(0.95F, 1) < 1.1F, "deserts stay hot, summer only a little warmer");
        // caves: colder the deeper, freezing near the bottom of the world
        check(h, Temperature.cave(Temperature.CAVE_TOP) == 10 && Temperature.cave(0) > Temperature.FREEZING_BELOW && Temperature.cave(-50) < Temperature.FREEZING_BELOW, "caves colder with depth");
        thaw(h);
        shelter(h);
        cropsAndBerries(h);
        seasonLook(h);
        h.succeed();
    }

    /** Colours step once a month; deciduous leaves fall late in autumn, stay bare in winter, bud then leaf out in spring. */
    /** Walls and roof keep warmth in: a turf hut is full shelter, stone less, no roof none. */
    private static void shelter(GameTestHelper h) {
        var level = h.getLevel();
        net.minecraft.core.BlockPos head = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        java.util.function.Consumer<net.minecraft.world.level.block.Block> hut = b -> {
            for (int x = 0; x <= 4; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z <= 4; z++)
                if (x == 0 || x == 4 || z == 0 || z == 4 || y == 4) h.setBlock(x, y, z, b);
        };
        hut.accept(EarthBlocks.TURF.get());
        check(h, Temperature.shelter(level, head) == 1, "turf hut is full shelter, got " + Temperature.shelter(level, head));
        hut.accept(net.minecraft.world.level.block.Blocks.STONE);
        check(h, Math.abs(Temperature.shelter(level, head) - Temperature.STONE_WARMTH) < 1e-4, "stone hut");
        h.setBlock(2, 2, 0, net.minecraft.world.level.block.Blocks.AIR); // a door gap lets warmth out
        check(h, Temperature.shelter(level, head) < Temperature.STONE_WARMTH, "a gap in the wall");
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) h.setBlock(x, 4, z, net.minecraft.world.level.block.Blocks.AIR);
        check(h, Temperature.shelter(level, head) == 0, "no roof, no shelter");
        for (int x = 0; x <= 4; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z <= 4; z++) h.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.AIR);
    }

    private static void seasonLook(GameTestHelper h) {
        int green = 0xFF48B518, summer = 3 * Calendar.MONTH_DAYS + 5, winter = 3 * Calendar.SEASON_DAYS + 10;
        check(h, SeasonLook.grass(green, 0.8F, summer / Calendar.MONTH_DAYS) == green && SeasonLook.grass(green, 0.8F, 9) != green, "grass green in summer, not in winter");
        check(h, SeasonLook.grass(green, 2.0F, 9) == green && SeasonLook.grass(green, 0.0F, 9) == green && SeasonLook.grass(green, 0.8F, -1) == green,
                "no colour change in deserts, snowy biomes or other dimensions");
        int hunting = SeasonLook.foliage(green, 0.7F, 7, false);
        check(h, (hunting >> 16 & 0xFF) > (hunting >> 8 & 0xFF), "oak leaves more red than green in Hunting");
        check(h, SeasonLook.autumn(0.7F, 5, true) == -1 && SeasonLook.autumn(0.7F, 6, true) != -1, "untinted leaves tinted from Leaffall");
        int bare = 0, leafy = 0;
        for (int t = 0; t < 100; t++) {
            check(h, SeasonLook.stage(summer, t) == SeasonLook.LEAVES && SeasonLook.stage(winter, t) == SeasonLook.BARE
                    && SeasonLook.stage(SeasonLook.FALL_START - 1, t) == SeasonLook.LEAVES && SeasonLook.stage(5, t) == SeasonLook.BARE
                    && SeasonLook.stage(SeasonLook.LEAF_START - 1, t) == SeasonLook.BUDS && SeasonLook.stage(Calendar.MONTH_DAYS, t) == SeasonLook.LEAVES,
                    "leaf stage " + t);
            if (SeasonLook.stage(SeasonLook.FALL_START + SeasonLook.CHANGE_DAYS / 2, t) == SeasonLook.BARE) bare++;
            if (SeasonLook.stage(SeasonLook.LEAF_START + SeasonLook.CHANGE_DAYS / 2, t) == SeasonLook.LEAVES) leafy++;
        }
        check(h, bare > 20 && bare < 80 && leafy > 20 && leafy < 80, "trees thin out and leaf out gradually");
        check(h, SeasonLook.key(summer) == SeasonLook.key(summer + 1) && SeasonLook.key(SeasonLook.FALL_START) != SeasonLook.key(SeasonLook.FALL_START + 1)
                && SeasonLook.key(Calendar.MONTH_DAYS - 1) != SeasonLook.key(Calendar.MONTH_DAYS), "re-render key changes only when the look does");
        check(h, SeasonLook.seasonal(0.7F) && !SeasonLook.seasonal(0.95F), "tropical trees keep their leaves");
    }

    /** Every crop rests in winter; bushes freeze only in a cold winter, and a frozen bush gives nothing until it thaws. */
    private static void cropsAndBerries(GameTestHelper h) {
        for (var crop : List.of(net.minecraft.world.level.block.Blocks.WHEAT, net.minecraft.world.level.block.Blocks.MELON_STEM,
                Farming.CROPS.get("barley").get(), Farming.PEA_CROP.get(), Spinning.FLAX_CROP.get())) {
            Farming.Climate c = Farming.climate(crop);
            check(h, c != null && c.season(3) == 0 && c.season(0) > 0 && c.season(1) > 0, crop + " season profile");
        }
        check(h, WildBerryBushBlock.wintry(3, 0.0F) && !WildBerryBushBlock.wintry(3, 0.5F) && !WildBerryBushBlock.wintry(0, -0.5F),
                "bushes freeze in a cold winter only");
        var level = h.getLevel();
        net.minecraft.core.BlockPos pos = h.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3));
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
        WildBerryBushBlock bush = Foraging.BUSH_BLOCKS.get("raspberry").get();
        var frozen = bush.defaultBlockState().setValue(WildBerryBushBlock.FROZEN, true);
        check(h, !WildBerryBushBlock.isRipe(frozen), "a frozen bush is not ripe");
        level.setBlock(pos, frozen, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        // the test world is in spring: picking checks the season first, so the bush thaws and gives berries
        check(h, !bush.pick(level, pos, frozen).isEmpty() && !level.getBlockState(pos).getValue(WildBerryBushBlock.FROZEN), "a bush thaws in spring when used");
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    /** Thaw where it is warm (the test world's spring): a thick snow layer melts a layer at a time, then the ice under it. */
    private static void thaw(GameTestHelper h) {
        var level = h.getLevel();
        net.minecraft.core.BlockPos ice = h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)), snow = ice.above(), top = snow.above();
        check(h, Thaw.temperature(level, level.getBiome(top).value(), top) >= Thaw.MELT_AT, "test world is warm in spring");
        level.setBlockAndUpdate(ice, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState());
        level.setBlockAndUpdate(snow, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 3));
        Thaw.melt(level, top, false);
        check(h, level.getBlockState(snow).is(net.minecraft.world.level.block.Blocks.SNOW) && level.getBlockState(snow).getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) == 2, "snow melts one layer");
        Thaw.melt(level, top, true);
        check(h, level.getBlockState(snow).isAir(), "catch-up melts all the snow");
        Thaw.melt(level, snow, false);
        check(h, level.getBlockState(ice).is(net.minecraft.world.level.block.Blocks.WATER), "ice turns back to water");
        level.setBlockAndUpdate(ice, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
}
