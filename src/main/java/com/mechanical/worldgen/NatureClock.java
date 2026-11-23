package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The nature clock (docs/terrain-plan.md section 4). The world doesn't tick nature in every chunk: each overworld chunk
 * remembers the calendar day it last "lived" ({@link Data#day}). When it loads, and each new day while it stays loaded, it
 * catches up on what should have happened since, one day at a time ({@link #dayStep}), with a cap on changes per sweep
 * ({@link #CAP}) and at most one sweep per chunk a second; a chunk far behind goes on in the next sweeps. Each day's dice
 * are seeded by the world seed, the chunk and the day, so the outcome is the same whenever one comes back.
 * <p>
 * Rules for everything here: never a block a player set (each chunk keeps the spots players placed blocks on,
 * {@link Data#placed}; a chunk with more, or one that already had player builds or long stays before the clock existed,
 * counts as {@link Data#built} and is left alone, with its neighbours), nothing within a camp's zone, near a Fire Pit or
 * a road ({@link SmoothGround#protectedChunk}) or in a surface structure. Nothing happens in a chunk any player could be
 * looking at ({@link #inView}); the chunk waits until they look away. Storm felling ({@link StormFelling}) is the one
 * thing meant to be seen.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class NatureClock {
    /** Block changes per chunk sweep; days per sweep; sweeps per tick; ticks between two sweeps of one chunk. */
    static final int CAP = 48, DAYS_PER_SWEEP = 8, PER_TICK = 2, SWEEP_GAP = 20;
    /** A chunk behind by more than this many days catches up only this far (a year). */
    static final int MAX_BEHIND = Calendar.YEAR_DAYS;
    /** Spots of player-placed blocks a chunk keeps; past it the chunk counts as built. */
    static final int MAX_PLACED = 512;
    /** A chunk that predates the clock with more InhabitedTime than this (20 min) counts as built: someone lived there. */
    static final long LIVED_IN = 24000;
    /** How far a player sees nature change: chunks within this (blocks) and inside their view cone wait. */
    static final double VIEW = 192, VIEW_COS = 0.45;

    /** Per chunk: last day lived, player blocks, and what the slower processes keep (fire, flood). */
    public static final class Data {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("day").forGetter(d -> d.day),
                Codec.BOOL.optionalFieldOf("scanned", false).forGetter(d -> d.scanned),
                Codec.BOOL.optionalFieldOf("built", false).forGetter(d -> d.built),
                Codec.LONG.listOf().optionalFieldOf("placed", List.of()).forGetter(d -> new ArrayList<>(d.placed)),
                Codec.LONG.optionalFieldOf("burn", 0L).forGetter(d -> d.burnDay),
                Codec.BOOL.optionalFieldOf("regrown", false).forGetter(d -> d.regrown),
                Codec.LONG.listOf().optionalFieldOf("flood", List.of()).forGetter(d -> d.flood),
                Codec.INT.optionalFieldOf("flood_y", 0).forGetter(d -> d.floodY)
        ).apply(i, Data::new));

        /** Calendar day the chunk last lived; -1 = never seen by the clock. */
        long day;
        boolean scanned, built;
        final Set<Long> placed;
        /** Day of a wildfire in the chunk (0 none) and whether the burnt ground has grown into young forest. */
        long burnDay;
        boolean regrown;
        /** Water a spring flood put here (packed positions), and its level. */
        List<Long> flood;
        int floodY;

        Data() {
            this(-1, false, false, List.of(), 0, false, List.of(), 0);
        }

        private Data(long day, boolean scanned, boolean built, List<Long> placed, long burnDay, boolean regrown, List<Long> flood, int floodY) {
            this.day = day;
            this.scanned = scanned;
            this.built = built;
            this.placed = new HashSet<>(placed);
            this.burnDay = burnDay;
            this.regrown = regrown;
            this.flood = new ArrayList<>(flood);
            this.floodY = floodY;
        }
    }

    public static final Supplier<AttachmentType<Data>> DATA = Mechanical.ATTACHMENTS.register("nature", () -> AttachmentType
            .builder(Data::new)
            .serialize(Data.CODEC.fieldOf("value"))
            .build());

    private static final LinkedHashSet<Long> QUEUE = new LinkedHashSet<>();
    private static final Set<Long> LOADED = new HashSet<>();
    private static final Map<Long, Long> LAST_SWEEP = new HashMap<>();
    private static long lastDay = Long.MIN_VALUE, nextRunDay;
    /** /nature pause: the clock stands still (to measure what it costs). */
    static boolean paused;
    static long sweeps, days, changes, nanos;

    private NatureClock() {}

    static long today(ServerLevel level) {
        return Calendar.day(level.getOverworldClockTime());
    }

    static Data data(LevelChunk chunk) {
        return chunk.getData(DATA);
    }

    // ---------------------------------------------------------------- one day in one chunk

    /** One day of nature in one chunk: the dice, the rules and a count of changes. */
    static final class Day {
        final ServerLevel level;
        final LevelChunk chunk;
        final ChunkPos pos;
        final long day;
        /** Only touch blocks inside (tests and /nature run near the player), or null. */
        final BoundingBox limit;
        /** Ignore the rules about players' blocks (tests, /nature run). */
        final boolean force;
        /** Only these processes (tests), or null for all. */
        final Set<String> only;
        int changes;

        Day(ServerLevel level, LevelChunk chunk, long day, BoundingBox limit, boolean force, Set<String> only) {
            this.level = level;
            this.chunk = chunk;
            this.pos = chunk.getPos();
            this.day = day;
            this.limit = limit;
            this.force = force;
            this.only = only;
        }

        /** This day's dice for one process (salt), the same whatever else runs. */
        RandomSource random(int salt) {
            long seed = level.getSeed() ^ pos.x() * 341873128712L ^ pos.z() * 132897987541L ^ day * 0x9E3779B97F4A7C15L ^ salt * 0xC2B2AE3D27D4EB4FL;
            return RandomSource.create(seed);
        }

        boolean runs(String process) {
            return only == null || only.contains(process);
        }

        int dayOfYear() {
            return (int) Math.floorMod(day, Calendar.YEAR_DAYS);
        }

        int season() {
            return dayOfYear() / Calendar.SEASON_DAYS;
        }

        int month() {
            return dayOfYear() / Calendar.MONTH_DAYS;
        }

        /** May nature change the block at p? Inside the limit, in a loaded chunk, not a block a player set. */
        boolean can(BlockPos p) {
            if (limit != null && !limit.isInside(p)) return false;
            LevelChunk c = level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4);
            if (c == null) return false;
            if (force) return true;
            Data d = data(c);
            return !d.built && !d.placed.contains(p.asLong());
        }

        void set(BlockPos p, BlockState s) {
            level.setBlock(p, s, Block.UPDATE_ALL);
            changes++;
        }

        /** The top block of a column that isn't leaves (the ground, a log lying on it, a stone, water). */
        BlockPos top(int x, int z) {
            return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
        }
    }

    /** Everything nature does in a chunk in one day. Each process draws its own dice. */
    static void dayStep(Day d) {
        NatureRot.day(d);
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        LevelChunk chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();
        LOADED.add(ChunkPos.pack(pos.x(), pos.z()));
        if (event.isNewChunk()) {
            Data d = data(chunk);
            d.scanned = true;
            d.day = today(level);
            chunk.markUnsaved();
            return;
        }
        // this chunk, and neighbours that were only waiting for it (changes may reach a block into them)
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) QUEUE.add(ChunkPos.pack(pos.x() + dx, pos.z() + dz));
    }

    @SubscribeEvent
    static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        ChunkPos pos = event.getChunk().getPos();
        long key = ChunkPos.pack(pos.x(), pos.z());
        LOADED.remove(key);
        LAST_SWEEP.remove(key);
    }

    /** A player set a block: nature never touches that spot. Too many in one chunk and the whole chunk is theirs. */
    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || !(event.getEntity() instanceof net.minecraft.world.entity.player.Player)) return;
        BlockPos p = event.getPos();
        LevelChunk chunk = level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4);
        if (chunk == null) return;
        Data d = data(chunk);
        if (d.built) return;
        d.placed.add(p.asLong());
        if (d.placed.size() > MAX_PLACED) {
            d.built = true;
            d.placed.clear();
        }
        chunk.markUnsaved();
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        long today = today(level);
        if (today != lastDay) { // a new day: every loaded chunk lives it
            lastDay = today;
            QUEUE.addAll(LOADED);
        }
        if (paused || QUEUE.isEmpty()) return;
        long now = level.getGameTime(), start = System.nanoTime();
        int done = 0, looked = 0;
        List<Long> again = new ArrayList<>();
        for (Iterator<Long> it = QUEUE.iterator(); it.hasNext() && done < PER_TICK && looked++ < 64; ) {
            long key = it.next();
            it.remove();
            Long last = LAST_SWEEP.get(key);
            if (last != null && now - last < SWEEP_GAP) {
                again.add(key);
                continue;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) continue;
            Sweep result = sweep(level, chunk, today);
            if (result == Sweep.DONE || result == Sweep.MORE) done++;
            if (result == Sweep.MORE || result == Sweep.WAIT) { // behind, seen, or neighbours not loaded: again in a second
                LAST_SWEEP.put(key, now);
                again.add(key);
            }
        }
        QUEUE.addAll(again);
        nanos += System.nanoTime() - start;
    }

    enum Sweep { CURRENT, DONE, MORE, WAIT }

    /** Brings a chunk some days closer to today, if it may change now. */
    static Sweep sweep(ServerLevel level, LevelChunk chunk, long today) {
        Data d = data(chunk);
        if (d.day >= today) return Sweep.CURRENT;
        if (!d.scanned) { // predates the clock: did players build or live here before it could see?
            d.scanned = true;
            d.built = chunk.getInhabitedTime() > LIVED_IN || !Retrogen.noPlayerBlocks(chunk);
            if (d.day < 0) d.day = today - 1;
            chunk.markUnsaved();
        }
        ChunkPos pos = chunk.getPos();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (level.getChunkSource().getChunkNow(pos.x() + dx, pos.z() + dz) == null) return Sweep.WAIT;
        if (inView(level, pos)) return Sweep.WAIT;
        if (!free(level, pos)) { // left alone: just keep its day current
            d.day = today;
            chunk.markUnsaved();
            return Sweep.DONE;
        }
        d.day = Math.max(d.day, today - MAX_BEHIND);
        int changed = 0, n = 0;
        while (d.day < today && changed < CAP && n++ < DAYS_PER_SWEEP) {
            Day day = new Day(level, chunk, d.day + 1, null, false, null);
            dayStep(day);
            d.day++;
            changed += day.changes;
            days++;
        }
        changes += changed;
        sweeps++;
        chunk.markUnsaved();
        return d.day < today ? Sweep.MORE : Sweep.DONE;
    }

    /** No player builds in the chunk or round it, no camp, Fire Pit, road or surface structure. */
    static boolean free(ServerLevel level, ChunkPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            LevelChunk c = level.getChunkSource().getChunkNow(pos.x() + dx, pos.z() + dz);
            if (c != null && data(c).built) return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (chunk != null && chunk.getAllReferences().keySet().stream().anyMatch(s -> s.step() == GenerationStep.Decoration.SURFACE_STRUCTURES)) return false;
        return !SmoothGround.protectedChunk(level, pos);
    }

    /** Free for nature at this block (its chunk and round it), the chunk loaded. */
    static boolean free(ServerLevel level, BlockPos p) {
        return level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4) != null && free(level, ChunkPos.containing(p))
                && !data(level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4)).placed.contains(p.asLong());
    }

    /** Could a player be looking at any part of the chunk? Standing in it counts. Height is ignored: a cone in plan view. */
    static boolean inView(ServerLevel level, ChunkPos pos) {
        for (ServerPlayer p : level.players()) {
            Vec3 eye = p.getEyePosition();
            double x0 = pos.getMinBlockX(), z0 = pos.getMinBlockZ();
            if (eye.x >= x0 - 2 && eye.x < x0 + 18 && eye.z >= z0 - 2 && eye.z < z0 + 18) return true;
            Vec3 look = p.getLookAngle();
            double lx = look.x, lz = look.z, ll = Math.sqrt(lx * lx + lz * lz);
            if (ll < 1e-3) { // looking straight down or up: sees what is right round them
                if (Math.hypot(eye.x - x0 - 8, eye.z - z0 - 8) < 40) return true;
                continue;
            }
            lx /= ll;
            lz /= ll;
            for (int i = 0; i < 5; i++) {
                double x = i == 4 ? x0 + 8 : x0 + (i & 1) * 16, z = i == 4 ? z0 + 8 : z0 + (i >> 1) * 16;
                double dx = x - eye.x, dz = z - eye.z, dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < VIEW && (dx * lx + dz * lz) / dist > VIEW_COS) return true;
            }
        }
        return false;
    }

    /** Point in the view of some player (storm felling: seen or not). */
    static boolean seen(ServerLevel level, BlockPos at) {
        Vec3 c = Vec3.atCenterOf(at);
        for (ServerPlayer p : level.players()) {
            Vec3 to = c.subtract(p.getEyePosition());
            double dist = to.length();
            if (dist < VIEW && to.normalize().dot(p.getLookAngle()) > 0.5) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- running days by hand (tests, /nature run)

    /** Runs {@code days} days from {@code firstDay} in every chunk touching the box, changing blocks only inside it, rules off. */
    public static int simulate(ServerLevel level, BoundingBox box, long firstDay, int days, Set<String> only) {
        int changed = 0;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (int i = 0; i < days; i++) {
                    Day day = new Day(level, chunk, firstDay + i, box, true, only);
                    dayStep(day);
                    changed += day.changes;
                }
            }
        return changed;
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nature").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("pause").executes(ctx -> {
                    paused = !paused;
                    ctx.getSource().sendSuccess(() -> Component.literal("Nature clock " + (paused ? "paused" : "running")), false);
                    return 1;
                }))
                .then(Commands.literal("run").then(Commands.argument("days", IntegerArgumentType.integer(1, 2000))
                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "days"), 12, null))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "days"), IntegerArgumentType.getInteger(ctx, "radius"), null))
                                .then(Commands.argument("process", StringArgumentType.word())
                                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "days"), IntegerArgumentType.getInteger(ctx, "radius"),
                                                StringArgumentType.getString(ctx, "process")))))))
                .then(Commands.literal("demo").then(Commands.literal("log").executes(ctx -> {
                    boolean ok = demoLog(ctx.getSource());
                    say(ctx.getSource(), ok ? "A fresh log lies in front of you" : "No room for a log");
                    return ok ? 1 : 0;
                })))
                .then(Commands.literal("fell").executes(ctx -> {
                    boolean ok = StormFelling.fellInFront(ctx.getSource().getLevel(), ctx.getSource().getPosition(), ctx.getSource().getRotation().y);
                    say(ctx.getSource(), ok ? "A tree falls" : "No tall tree nearby");
                    return ok ? 1 : 0;
                })));
    }

    /** /nature run: days from today round the source, only inside the radius, rules off (shots, trying things out). */
    private static int run(CommandSourceStack source, int days, int radius, String process) {
        ServerLevel level = source.getLevel();
        BlockPos c = BlockPos.containing(source.getPosition());
        BoundingBox box = new BoundingBox(c.getX() - radius, level.getMinY(), c.getZ() - radius, c.getX() + radius, level.getMaxY(), c.getZ() + radius);
        long first = Math.max(today(level) + 1, nextRunDay);
        nextRunDay = first + days;
        int changed = simulate(level, box, first, days, process == null ? null : Set.of(process.split(",")));
        say(source, days + " days of nature: " + changed + " blocks changed");
        return 1;
    }

    /** /nature demo log: a fresh log of the nearest tree's wood on the ground 6 blocks in front, lying across the view (shots). */
    private static boolean demoLog(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        float yaw = source.getRotation().y;
        Vec3 look = Vec3.directionFromRotation(0, yaw);
        net.minecraft.core.Direction dir = net.minecraft.core.Direction.fromYRot(yaw).getClockWise();
        for (int dist = 6; dist <= 12; dist++) // the first spot in front with room for a log of 5+
            for (int shift = -3; shift <= -1; shift++) {
                BlockPos c = BlockPos.containing(source.getPosition().add(look.scale(dist))).relative(dir, shift);
                BlockPos start = new BlockPos(c.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ()), c.getZ());
                List<BlockPos> cells = ForestDecor.logCells(level, start, dir, 6, false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
                if (cells.size() < 5 || !level.getBlockState(start.below()).is(net.minecraft.tags.BlockTags.SUBSTRATE_OVERWORLD)) continue;
                ForestDecor.Wood w = ForestDecor.woodNear(level, start, 16);
                ForestDecor.lay(level, cells, w != null ? w.log() : net.minecraft.world.level.block.Blocks.OAK_LOG, dir.getAxis());
                return true;
            }
        return false;
    }

    /** Command feedback, also logged (the shot harness runs commands without a chat to read). */
    static void say(CommandSourceStack source, String text) {
        Mechanical.LOGGER.info("nature: {}", text);
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos c = BlockPos.containing(source.getPosition());
        LevelChunk chunk = level.getChunkAt(c);
        Data d = data(chunk);
        String s = "chunk " + chunk.getPos() + ": day " + d.day + " (today " + today(level) + "), built " + d.built + ", " + d.placed.size()
                + " player blocks, free " + free(level, chunk.getPos()) + ", burn " + d.burnDay + (d.regrown ? " (regrown)" : "") + ", flood " + d.flood.size()
                + "; total " + sweeps + " sweeps, " + days + " days, " + changes + " changes, " + nanos / 1_000_000 + " ms, queue " + QUEUE.size();
        source.sendSuccess(() -> Component.literal(s), false);
        return 1;
    }
}
