package com.mechanical;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * How the overworld looks through the year (phase 7, docs/seasons-and-tribes.md, "Lövträd"). Grass and leaf colours
 * change once a month (3 steps a season): fresh green in spring, green in summer, yellow, orange and red in autumn, pale
 * brown in winter. Deciduous leaves fall over a few days late in autumn, stay bare through winter and in spring first
 * bud, then leaf out ({@link #stage}). Pure maths: client.SeasonalFoliage draws it, CalendarTest tests it.
 */
public final class SeasonLook {
    public static final int LEAVES = 0, BUDS = 1, BARE = 2;
    /** Days of the year: leaves fall from 6 Frostfall, buds come from 11 Thaw and leaves from 21 Thaw, each over {@link #CHANGE_DAYS}. */
    static final int FALL_START = 245, BUD_START = 10, LEAF_START = 20, CHANGE_DAYS = 10;
    /** Biome base temperatures: from {@code TROPICAL_FROM} up (jungle, savanna, desert) trees keep their leaves and colours stay; below {@code SNOWY_BELOW} (snowy all year) colours stay. */
    static final float TROPICAL_FROM = 0.95F, SNOWY_BELOW = 0.1F;

    // per month (thaw .. snowmelt): the colour grass and leaves move toward, and how far (0 = the biome's own colour)
    private static final int FRESH = 0xFF8CD048, DRY = 0xFFC0B050, TAN = 0xFFB09A58, BROWN = 0xFFA08A5C, PALE = 0xFFA09A80,
            DEAD = 0xFF8A6A40;
    private static final int[] GRASS_TO = {PALE, FRESH, FRESH, FRESH, FRESH, DRY, DRY, TAN, BROWN, PALE, PALE, PALE};
    private static final float[] GRASS_BY = {0.45F, 0.3F, 0.12F, 0, 0, 0.18F, 0.3F, 0.42F, 0.5F, 0.55F, 0.55F, 0.5F};
    private static final int[] RED_TO = {FRESH, FRESH, FRESH, FRESH, FRESH, DRY, 0xFFD8B030, 0xFFE07020, 0xFFB83418, DEAD, DEAD, DEAD};
    private static final int[] YELLOW_TO = {FRESH, FRESH, FRESH, FRESH, FRESH, DRY, 0xFFD0C840, 0xFFF0C030, 0xFFC89030, DEAD, DEAD, DEAD};
    private static final float[] LEAVES_BY = {0.45F, 0.3F, 0.12F, 0, 0, 0.12F, 0.6F, 0.8F, 0.85F, 0.8F, 0.8F, 0.8F};
    /** First month of autumn colours (Leaffall). */
    public static final int AUTUMN = 6;

    private SeasonLook() {}

    /** Grass colour in this month ({@code month} -1: unchanged, outside the overworld). */
    public static int grass(int base, float baseTemperature, int month) {
        return month < 0 || !coloured(baseTemperature) ? base : ARGB.srgbLerp(GRASS_BY[month], base, GRASS_TO[month]);
    }

    /** Colour of tinted deciduous leaves; birch-like ones go yellow in autumn, the rest red. */
    public static int foliage(int base, float baseTemperature, int month, boolean yellow) {
        return month < 0 || !coloured(baseTemperature) ? base : ARGB.srgbLerp(LEAVES_BY[month], base, (yellow ? YELLOW_TO : RED_TO)[month]);
    }

    /** Tint for the grey autumn model of untinted leaves (Leaffall to the end of the year); -1 when they keep their own colours. */
    public static int autumn(float baseTemperature, int month, boolean yellow) {
        return month < AUTUMN || !coloured(baseTemperature) ? -1 : (yellow ? YELLOW_TO : RED_TO)[month];
    }

    public static boolean coloured(float baseTemperature) {
        return baseTemperature >= SNOWY_BELOW && baseTemperature < TROPICAL_FROM;
    }

    /** Deciduous trees lose their leaves here (everywhere but the tropics and deserts). */
    public static boolean seasonal(float baseTemperature) {
        return baseTemperature < TROPICAL_FROM;
    }

    /** Percent of the way through a change that starts on day {@code start}: 0 before, 100 from its last day on. */
    static int progress(int dayOfYear, int start) {
        return Mth.clamp((dayOfYear - start + 1) * 100 / CHANGE_DAYS, 0, 100);
    }

    /** {@link #LEAVES}, {@link #BUDS} or {@link #BARE} for a leaves block whose fixed {@code threshold} (0..99, from its position) says how early it changes. */
    public static int stage(int dayOfYear, int threshold) {
        if (dayOfYear >= FALL_START) return threshold < progress(dayOfYear, FALL_START) ? BARE : LEAVES;
        if (threshold < progress(dayOfYear, LEAF_START)) return LEAVES;
        return threshold < progress(dayOfYear, BUD_START) ? BUDS : BARE;
    }

    /** Changes exactly when something drawn changes (a new month, or a day while leaves fall, bud or leaf out): the client then re-renders. */
    public static int key(int dayOfYear) {
        return dayOfYear / Calendar.MONTH_DAYS << 24 | progress(dayOfYear, FALL_START) << 16 | progress(dayOfYear, BUD_START) << 8
                | progress(dayOfYear, LEAF_START);
    }
}
