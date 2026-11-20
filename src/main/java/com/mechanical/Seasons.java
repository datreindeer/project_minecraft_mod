package com.mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The seasons' weather (docs/seasons-and-tribes.md). Temperature follows a smooth curve over the year ({@link #curve}):
 * a little warmer at midsummer, much colder at midwinter in temperate and cold biomes, a little colder in warm ones and
 * hardly at all in deserts ({@link #offset}). Every biome's temperature is shifted by it (mixin.BiomeSeasonMixin), so
 * vanilla brings snow instead of rain, snow cover and frozen lakes; the body temperature (Temperature.ambient) and with
 * it crops follow too. Rain comes more often and lasts longer in autumn and winter, less in summer: each new rain or dry
 * spell vanilla rolls is stretched or shortened by season. Advancements: First Frost, Through the Winter, A Full Year.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Seasons {
    /** Biome temperature change at midsummer, and at midwinter for temperate/cold, warm and arid biomes. */
    static final float SUMMER = 0.1F, WINTER = 0.8F, WINTER_WARM = 0.3F, WINTER_ARID = 0.2F;
    /** Base temperatures from which a biome counts as warm, and as arid (desert, badlands, savanna 2.0). */
    static final float WARM_FROM = 1.0F, ARID_FROM = 1.5F;
    /** How long vanilla's rain and dry spells last, per season (spring, summer, autumn, winter). */
    static final float[] RAIN = {1.0F, 0.7F, 1.3F, 1.2F}, DRY = {0.9F, 1.4F, 0.6F, 0.8F};
    static final Identifier FIRST_FROST = id("wilderness/first_frost"), THROUGH_WINTER = id("wilderness/through_winter"),
            FULL_YEAR = id("wilderness/full_year");

    /** The curve now, kept up to date on each side for the biome mixin, which has no level at hand. */
    private static volatile float current = 0;

    private Seasons() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, path);
    }

    /** +1 at midsummer, -1 at midwinter, smooth in between (also within a day). */
    public static float curve(long clock) {
        float day = Calendar.dayOfYear(clock) + Math.floorMod(clock, Calendar.CLOCK_DAY) / (float) Calendar.CLOCK_DAY;
        return Mth.cos((day - Calendar.MIDSUMMER) * Mth.TWO_PI / Calendar.YEAR_DAYS);
    }

    /** Change to a biome's temperature (vanilla scale, snow below 0.15) for this point of the year. */
    public static float offset(float baseTemperature, float curve) {
        if (curve >= 0) return SUMMER * curve;
        return curve * (baseTemperature < WARM_FROM ? WINTER : baseTemperature < ARID_FROM ? WINTER_WARM : WINTER_ARID);
    }

    public static float offset(float baseTemperature) {
        return offset(baseTemperature, current);
    }

    // ---------------------------------------------------------------- server

    private static MinecraftServer seen;
    private static int lastRainTime = -1, lastSeason = -1;
    private static long lastYear = -1;

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        long clock = level.getOverworldClockTime();
        current = curve(clock);
        if (seen != level.getServer()) { // a new world (singleplayer): start counting afresh
            seen = level.getServer();
            lastRainTime = lastSeason = -1;
            lastYear = -1;
        }
        int season = Calendar.season(clock);
        stretchWeather(level, season);
        if (level.getGameTime() % 100 == 0) seasonAdvancements(level, season, Calendar.year(clock));
    }

    /** Vanilla just rolled a new rain or dry spell (the countdown went from 0 to a fresh value): scale it by season. */
    private static void stretchWeather(ServerLevel level, int season) {
        WeatherData w = level.getWeatherData();
        int rainTime = w.getRainTime();
        if (lastRainTime == 0 && rainTime > 0 && w.getClearWeatherTime() == 0 && level.getGameRules().get(GameRules.ADVANCE_WEATHER)) {
            rainTime = Math.max(1, Math.round(rainTime * (w.isRaining() ? RAIN : DRY)[season]));
            w.setRainTime(rainTime);
        }
        lastRainTime = rainTime;
    }

    private static void seasonAdvancements(ServerLevel level, int season, long year) {
        for (ServerPlayer player : level.players()) {
            if (season == 3) Kettle.award(player, FIRST_FROST);
            if (lastSeason == 3 && season == 0) Kettle.award(player, THROUGH_WINTER);
            if (lastYear > 0 && year > lastYear) Kettle.award(player, FULL_YEAR);
        }
        lastSeason = season;
        lastYear = year;
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Client {
        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.getSingleplayerServer() == null) current = curve(mc.level.getOverworldClockTime());
        }
    }
}
