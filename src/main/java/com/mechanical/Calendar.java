package com.mechanical;

import java.util.Optional;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The year (docs/seasons-and-tribes.md). A day lasts an hour: the overworld clock runs at a third of vanilla speed on
 * average, faster or slower so that the day is 40 min and the night 20 min at midsummer and the other way round at
 * midwinter (NeoForge clock rate, synced to clients, so the sky follows). The calendar is read off the clock: 30 days a
 * month, 3 months a season, 360 days a year, a new world starting on 15 Sowing (mid spring) of year 1.
 * <p>
 * Two kinds of day: {@link #CLOCK_DAY} clock ticks (time of day, day number: anything read from
 * {@code getOverworldClockTime()}), and {@link #DAY} game ticks (durations counted with {@code getGameTime()} or tick
 * counters, such as spoiling, which should still last "a day").
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Calendar {
    /** Clock ticks per day (vanilla). */
    public static final int CLOCK_DAY = 24000;
    /** Game ticks per day: one hour. Day plus night always add up to this, whatever the season. */
    public static final int DAY = 72000;
    /** How much longer a day is than vanilla's: durations tuned for vanilla days (crop growth) divide by this. */
    public static final int SLOWDOWN = DAY / CLOCK_DAY;
    public static final int MONTH_DAYS = 30, SEASON_DAYS = 90, YEAR_DAYS = 360;
    /** Day of the year a new world starts on (0-based): 15 Sowing. */
    public static final int START_DAY = SEASON_DAYS / 3 + 14;
    /** Day of the year with the longest day: the middle of summer. */
    static final int MIDSUMMER = SEASON_DAYS + SEASON_DAYS / 2;
    /** Day length in minutes: average and how far it swings at midsummer and midwinter. */
    static final float DAY_MINUTES = 30, SWING_MINUTES = 10;
    public static final String[] SEASONS = {"spring", "summer", "autumn", "winter"};
    public static final String[] MONTHS = {"thaw", "sowing", "blossom", "midsun", "haymonth", "reaping",
            "leaffall", "hunting", "frostfall", "deepwinter", "wolfmoon", "snowmelt"};

    private Calendar() {}

    /** Days since the start of year 1 (0-based). */
    public static long day(long clock) {
        return Math.floorDiv(clock, CLOCK_DAY) + START_DAY;
    }

    public static int dayOfYear(long clock) {
        return (int) Math.floorMod(day(clock), YEAR_DAYS);
    }

    /** 1-based. */
    public static long year(long clock) {
        return Math.floorDiv(day(clock), YEAR_DAYS) + 1;
    }

    /** 0 spring, 1 summer, 2 autumn, 3 winter. */
    public static int season(long clock) {
        return dayOfYear(clock) / SEASON_DAYS;
    }

    /** 0..11, index into {@link #MONTHS}. */
    public static int month(long clock) {
        return dayOfYear(clock) / MONTH_DAYS;
    }

    /** 1..30. */
    public static int dayOfMonth(long clock) {
        return dayOfYear(clock) % MONTH_DAYS + 1;
    }

    /** Clock 0 is 06:00, as in vanilla. */
    public static int hour(long clock) {
        return (int) (Math.floorMod(clock, CLOCK_DAY) / 1000 + 6) % 24;
    }

    public static int minute(long clock) {
        return (int) (Math.floorMod(clock, CLOCK_DAY) % 1000 * 60 / 1000);
    }

    /** Minutes of daylight on the day of {@code clock}; the night gets the rest of the hour. */
    public static float dayMinutes(long clock) {
        return DAY_MINUTES + SWING_MINUTES * Mth.cos((dayOfYear(clock) - MIDSUMMER) * Mth.TWO_PI / YEAR_DAYS);
    }

    /** Clock ticks per game tick: the first half of the clock day (0..12000) is day, the second half night. */
    public static float rate(long clock) {
        float day = dayMinutes(clock);
        float minutes = Math.floorMod(clock, CLOCK_DAY) < CLOCK_DAY / 2 ? day : 2 * DAY_MINUTES - day;
        return CLOCK_DAY / 2F / (minutes * 60 * 20);
    }

    /** The clock time of the same time of day on {@code dayOfYear} of the current year (next year if that is before the world began). */
    static long clockOn(long clock, int dayOfYear) {
        long target = (year(clock) - 1) * YEAR_DAYS + dayOfYear - START_DAY;
        if (target < 0) target += YEAR_DAYS;
        return target * CLOCK_DAY + Math.floorMod(clock, CLOCK_DAY);
    }

    /** "Spring · 15 Sowing, Year 1 · 06:30". */
    public static Component describe(long clock) {
        return Component.translatable("calendar.mechanical.date",
                Component.translatable("calendar.mechanical.season." + SEASONS[season(clock)]),
                dayOfMonth(clock),
                Component.translatable("calendar.mechanical.month." + MONTHS[month(clock)]),
                year(clock),
                String.format("%02d:%02d", hour(clock), minute(clock)));
    }

    private static Optional<Holder.Reference<WorldClock>> overworldClock(MinecraftServer server) {
        return server.registryAccess().get(WorldClocks.OVERWORLD);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        overworldClock(level.getServer()).ifPresent(clock -> {
            var clocks = level.getServer().clockManager();
            float rate = rate(clocks.getInstance(clock).totalTicks());
            // setRate broadcasts a packet: only when it changes (twice a day)
            if (Math.abs(clocks.getInstance(clock).rate() - rate) > 1e-5F) clocks.setRate(clock, rate);
        });
    }

    /** /calendar: today's date and day length. /calendar set season|month|day ...: jump there, same time of day (ops). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> season = Commands.literal("season"), month = Commands.literal("month");
        for (int i = 0; i < SEASONS.length; i++) {
            int doy = i * SEASON_DAYS;
            season.then(Commands.literal(SEASONS[i]).executes(ctx -> jump(ctx, doy)));
        }
        for (int i = 0; i < MONTHS.length; i++) {
            int doy = i * MONTH_DAYS;
            month.then(Commands.literal(MONTHS[i]).executes(ctx -> jump(ctx, doy)));
        }
        event.getDispatcher().register(Commands.literal("calendar").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> show(ctx.getSource()))
                .then(Commands.literal("set").then(season).then(month)
                        .then(Commands.literal("day").then(Commands.argument("day", IntegerArgumentType.integer(1, MONTH_DAYS)).executes(ctx -> {
                            long clock = ctx.getSource().getLevel().getOverworldClockTime();
                            return jump(ctx, month(clock) * MONTH_DAYS + IntegerArgumentType.getInteger(ctx, "day") - 1);
                        })))));
    }

    private static int jump(CommandContext<CommandSourceStack> ctx, int dayOfYear) {
        MinecraftServer server = ctx.getSource().getServer();
        long clock = clockOn(ctx.getSource().getLevel().getOverworldClockTime(), dayOfYear);
        overworldClock(server).ifPresent(c -> server.clockManager().setTotalTicks(c, clock));
        return show(ctx.getSource());
    }

    private static int show(CommandSourceStack source) {
        long clock = source.getLevel().getOverworldClockTime();
        int day = Math.round(dayMinutes(clock));
        source.sendSuccess(() -> describe(clock).copy().append(" ").append(Component.translatable("calendar.mechanical.day_length", day, 60 - day)), false);
        return 1;
    }
}
