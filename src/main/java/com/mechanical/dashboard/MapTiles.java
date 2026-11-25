package com.mechanical.dashboard;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import com.mechanical.Mechanical;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The dashboard's map: overworld chunks are drawn as players load them, into 512x512 region PNGs (1 px per block) under
 * {@code <world>/mechanical_map}. Loaded chunks wait in a de-duplicated queue and at most {@link #PER_TICK} are read per tick
 * on the server thread (256 columns each); merging into the region images and writing PNGs happens on one worker thread,
 * so fast travel never costs TPS. Runs only while the {@link Dashboard} does.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MapTiles {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    /** Chunks read per server tick, at most. */
    static final int PER_TICK = 8;
    private static final int OPEN_REGIONS = 16;
    private static final long WRITE_EVERY_MS = 5000;
    /** Loaded chunks not yet read, packed ChunkPos; a set so a chunk queued twice is read once. */
    static final Set<Long> PENDING = new LinkedHashSet<>();

    private static volatile ScheduledExecutorService worker;
    private static volatile Path folder;
    /** Worker-thread only: open region images, least recently used first. */
    private static final LinkedHashMap<Long, Region> OPEN = new LinkedHashMap<>(OPEN_REGIONS, 0.75f, true);

    private static final class Region {
        final BufferedImage image;
        boolean dirty;
        long written;

        Region(BufferedImage image) {
            this.image = image;
        }
    }

    private MapTiles() {}

    /** Starts the worker; called by {@link Dashboard} once it listens. */
    static synchronized void start(MinecraftServer server) {
        stop();
        folder = server.getWorldPath(LevelResource.ROOT).resolve("mechanical_map");
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "mechanical-map");
            t.setDaemon(true);
            return t;
        });
        exec.scheduleWithFixedDelay(() -> flush(false), 1, 1, TimeUnit.SECONDS);
        worker = exec;
    }

    /** Writes every changed region (waiting at most 10 s), then ends the worker; chunks still queued are drawn next time they load. */
    static synchronized void stop() {
        ScheduledExecutorService exec = worker;
        if (exec == null) return;
        worker = null;
        synchronized (PENDING) {
            PENDING.clear();
        }
        exec.execute(() -> {
            flush(true);
            OPEN.clear();
        });
        exec.shutdown(); // cancels the periodic flush, still runs the final one
        try {
            if (!exec.awaitTermination(10, TimeUnit.SECONDS)) LOGGER.warn("Mechanical map: regions not written within 10 s on stop");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean running() {
        return worker != null && Dashboard.isRunning();
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!running() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        ChunkPos pos = event.getChunk().getPos();
        synchronized (PENDING) {
            PENDING.add(ChunkPos.pack(pos.x(), pos.z()));
        }
    }

    /** A chunk leaving memory may have changed since it was drawn, and can't be read later: read it now. No cap here, it's a fraction of the chunk's own save. */
    @SubscribeEvent
    static void onChunkUnload(ChunkEvent.Unload event) {
        if (!running() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        LevelChunk chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();
        synchronized (PENDING) {
            PENDING.remove(ChunkPos.pack(pos.x(), pos.z()));
        }
        submit(pos, colours(chunk));
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (!running()) return;
        List<Long> keys;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            keys = drain(PENDING, PER_TICK);
        }
        ServerLevel level = event.getServer().overworld();
        for (long key : keys) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk != null) submit(chunk.getPos(), colours(chunk));
        }
    }

    /** Takes up to {@code max} keys off the front of the queue. */
    static List<Long> drain(Set<Long> queue, int max) {
        List<Long> out = new ArrayList<>(Math.min(max, queue.size()));
        for (Iterator<Long> it = queue.iterator(); it.hasNext() && out.size() < max;) {
            out.add(it.next());
            it.remove();
        }
        return out;
    }

    /**
     * The chunk's 256 map colours (ARGB, index {@code z * 16 + x}), like a vanilla map at 1:1: the top block with a map colour,
     * brighter/darker when higher/lower than the block to its north, water shaded by its depth instead.
     * Never loads a chunk: the north neighbour of row 0 is used only if already loaded, else row 0 compares with itself.
     */
    static int[] colours(LevelChunk chunk) {
        Level level = chunk.getLevel();
        ChunkPos pos = chunk.getPos();
        LevelChunk north = level.getChunkSource().getChunkNow(pos.x(), pos.z() - 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int[] heights = new int[16]; // the previous row's surface heights
        for (int x = 0; x < 16; x++) heights[x] = north != null ? surface(north, x, 15, p) : surface(chunk, x, 0, p);
        int[] out = new int[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int y = surface(chunk, x, z, p);
            BlockState state = chunk.getBlockState(p);
            MapColor.Brightness b;
            if (!state.getFluidState().isEmpty() && !state.isFaceSturdy(level, p, Direction.UP)) {
                state = state.getFluidState().createLegacyBlock();
                int depth = 1;
                while (p.getY() > level.getMinY() && !chunk.getBlockState(p.move(Direction.DOWN)).getFluidState().isEmpty()) depth++;
                p.setY(y);
                b = depth <= 2 ? MapColor.Brightness.HIGH : depth <= 6 ? MapColor.Brightness.NORMAL : MapColor.Brightness.LOW;
            } else {
                b = y > heights[x] ? MapColor.Brightness.HIGH : y < heights[x] ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
            }
            out[z * 16 + x] = state.getMapColor(level, p).calculateARGBColor(b);
            heights[x] = y;
        }
        return out;
    }

    /** Height of the top block with a map colour in a column (local x/z), leaving {@code p} on it. */
    private static int surface(LevelChunk chunk, int x, int z, BlockPos.MutableBlockPos p) {
        int minY = chunk.getMinY();
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        p.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
        while (y > minY && chunk.getBlockState(p).getMapColor(chunk.getLevel(), p) == MapColor.NONE) p.setY(--y);
        return y;
    }

    private static void submit(ChunkPos pos, int[] colours) {
        ScheduledExecutorService exec = worker;
        if (exec == null) return;
        try {
            exec.execute(() -> merge(pos.x(), pos.z(), colours));
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            // stopping
        }
    }

    // ---------------------------------------------------------------- worker thread

    private static void merge(int cx, int cz, int[] colours) {
        long key = ChunkPos.pack(cx >> 5, cz >> 5);
        Region r = OPEN.get(key);
        if (r == null) {
            r = new Region(open(cx >> 5, cz >> 5));
            OPEN.put(key, r);
            if (OPEN.size() > OPEN_REGIONS) {
                Map.Entry<Long, Region> eldest = OPEN.entrySet().iterator().next();
                if (eldest.getValue().dirty) write(eldest.getKey(), eldest.getValue());
                OPEN.remove(eldest.getKey());
            }
        }
        r.image.setRGB((cx & 31) * 16, (cz & 31) * 16, 16, 16, colours, 0, 16);
        r.dirty = true;
    }

    private static BufferedImage open(int rx, int rz) {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Path file = file(rx, rz);
        if (Files.exists(file)) {
            try {
                BufferedImage old = ImageIO.read(file.toFile());
                if (old != null) image.getGraphics().drawImage(old, 0, 0, null);
            } catch (IOException e) {
                LOGGER.warn("Mechanical map: could not read {} ({}), drawing it anew", file, e.getMessage());
            }
        }
        return image;
    }

    /** Writes dirty regions: all of them, or only those not written in the last 5 s. */
    private static void flush(boolean all) {
        long now = System.currentTimeMillis();
        OPEN.forEach((key, r) -> {
            if (r.dirty && (all || now - r.written >= WRITE_EVERY_MS)) write(key, r);
        });
    }

    /** Writes to a temp file and moves it in place, so the web server never reads half a PNG. */
    private static void write(long key, Region r) {
        Path file = file(ChunkPos.getX(key), ChunkPos.getZ(key));
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            ImageIO.write(r.image, "png", tmp.toFile());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            r.dirty = false;
            r.written = System.currentTimeMillis();
        } catch (IOException e) {
            LOGGER.warn("Mechanical map: could not write {} ({}), trying again later", file, e.getMessage());
        }
    }

    private static Path file(int rx, int rz) {
        return folder.resolve(rx + "." + rz + ".png");
    }

    // ---------------------------------------------------------------- web threads

    /** The region PNG's bytes, or null when the map is off or that region was never drawn. */
    static byte[] tile(int rx, int rz) throws IOException {
        Path f = folder;
        if (f == null || !running()) return null;
        try {
            return Files.readAllBytes(f.resolve(rx + "." + rz + ".png"));
        } catch (NoSuchFileException e) {
            return null;
        }
    }
}
