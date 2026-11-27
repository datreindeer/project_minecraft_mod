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

import com.mechanical.EarthBlocks;
import com.mechanical.Mechanical;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorMapColorUtil;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The map's tiles (web dashboard and the in-game map, com.mechanical.map): overworld chunks are drawn as players load them, into 512x512 region PNGs (1 px per block) under
 * {@code <world>/mechanical_map}. Loaded and saved chunks (and all loaded ones when the map starts) wait in a de-duplicated
 * queue and at most {@link #PER_TICK} are read per tick on the server thread (256 columns each); merging into the region
 * images and writing PNGs happens on one worker thread, so fast travel never costs TPS. Runs whenever a server does (also
 * singleplayer, dashboard or not), except the game test server.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MapTiles {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    /** Chunks read per server tick, at most. */
    static final int PER_TICK = 8;
    static final int OPEN_REGIONS = 16;
    /** Regions kept open past {@link #OPEN_REGIONS} because their write failed; beyond this the oldest is dropped (about 1 MB each). */
    static final int KEPT_FAILED = 4;
    private static final long WRITE_EVERY_MS = 5000;
    /** Loaded chunks not yet read, packed ChunkPos; a set so a chunk queued twice is read once. */
    static final Set<Long> PENDING = new LinkedHashSet<>();

    private static volatile ScheduledExecutorService worker;
    static volatile Path folder;
    /** Test hook: every PNG write fails, as on a full disk. */
    static volatile boolean failWrites;
    /** Worker-thread only (or the game test while the map is off): open region images, least recently used first. */
    static final LinkedHashMap<Long, Region> OPEN = new LinkedHashMap<>(OPEN_REGIONS, 0.75f, true);

    /** Worker-thread only: how often each region's pixels changed this session, so an open map screen only gets what is new. */
    private static final Map<Long, Integer> VERSIONS = new java.util.HashMap<>();

    private static final class Region {
        final BufferedImage image;
        boolean dirty;
        long written;
        int failures;

        Region(BufferedImage image) {
            this.image = image;
        }
    }

    private MapTiles() {}

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        if (!(event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)) start(event.getServer());
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        stop();
    }

    /** Starts the worker. */
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
        synchronized (PENDING) {
            queueLoaded(server.overworld(), PENDING); // spawn and force-loaded chunks (tribe pits) never fire Load again
        }
    }

    /**
     * Queues every chunk the level already holds in full. forEachReadyToSendChunk only walks ticking chunks, so the
     * non-ticking border round them (two chunks wide round a force-loaded tribe pit) is picked up via getChunkNow.
     */
    static void queueLoaded(ServerLevel level, Set<Long> queue) {
        Set<Long> ticking = new java.util.HashSet<>();
        level.getChunkSource().chunkMap.forEachReadyToSendChunk(c -> ticking.add(ChunkPos.pack(c.getPos().x(), c.getPos().z())));
        queue.addAll(ticking);
        for (long k : ticking)
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    int x = ChunkPos.getX(k) + dx, z = ChunkPos.getZ(k) + dz;
                    long n = ChunkPos.pack(x, z);
                    if (!queue.contains(n) && level.getChunkSource().getChunkNow(x, z) != null) queue.add(n);
                }
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
            VERSIONS.clear();
        });
        exec.shutdown(); // cancels the periodic flush, still runs the final one
        try {
            if (!exec.awaitTermination(10, TimeUnit.SECONDS)) LOGGER.warn("Mechanical map: regions not written within 10 s on stop");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean running() {
        return worker != null;
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!running() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        ChunkPos pos = event.getChunk().getPos();
        synchronized (PENDING) {
            PENDING.add(ChunkPos.pack(pos.x(), pos.z()));
        }
    }

    /** A saved chunk has changed since it was drawn (chunks that stay loaded only show edits this way); posted on the server thread by ChunkMap.save. */
    @SubscribeEvent
    static void onChunkSave(ChunkDataEvent.Save event) {
        if (!running() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || !(event.getChunk() instanceof LevelChunk)) return;
        ChunkPos pos = event.getChunk().getPos();
        synchronized (PENDING) {
            PENDING.add(ChunkPos.pack(pos.x(), pos.z()));
        }
    }

    /**
     * A chunk leaving memory may have changed since it was drawn, and can't be read later: read it now. Posted on the server
     * thread (ChunkMap.processUnloads runs the unload queue in the tick). No cap here, it's a fraction of the chunk's own save.
     */
    @SubscribeEvent
    static void onChunkUnload(ChunkEvent.Unload event) {
        if (!running() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        LevelChunk chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();
        synchronized (PENDING) {
            PENDING.remove(ChunkPos.pack(pos.x(), pos.z()));
        }
        Dashboard.guard("map chunk unload", () -> submit(pos, colours(chunk)));
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
            if (chunk != null) Dashboard.guard("map chunk", () -> submit(chunk.getPos(), colours(chunk)));
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
            out[z * 16 + x] = colour(level, state, p, b);
            heights[x] = y;
        }
        return out;
    }

    /** One pixel: grass, foliage and water in their biome's colour, everything else in its map colour. */
    private static int colour(Level level, BlockState state, BlockPos p, MapColor.Brightness b) {
        Tint tint = Tint.of(state);
        if (tint == null) return state.getMapColor(level, p).calculateARGBColor(b);
        Biome biome = level.getBiome(p).value(); // never loads a chunk: missing neighbours come from the biome source
        return ARGB.scaleRGB(ARGB.opaque(tint.colour(biome, p.getX(), p.getZ())), b.modifier);
    }

    /**
     * The blocks the game tints by biome (not Worn Grass and Trodden Path: roads must stand out from the grass round them).
     * No season tint: tiles are drawn whenever their chunk loads, so months would end up patched side by side.
     */
    enum Tint {
        GRASS, FOLIAGE, DRY_FOLIAGE, BIRCH, SPRUCE, WATER;

        // dedicated servers never load the game's colormaps (GrassColor.get is black there), so the mod carries copies
        private static final int[] GRASS_MAP = colormap("grass"), FOLIAGE_MAP = colormap("foliage"), DRY_MAP = colormap("dry_foliage");

        static Tint of(BlockState s) {
            if (s.is(Blocks.WATER) || s.is(Blocks.BUBBLE_COLUMN)) return WATER;
            if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
                    || s.is(Blocks.BUSH) || s.is(Blocks.SUGAR_CANE) || s.is(EarthBlocks.TURF.get()) || s.is(EarthBlocks.TURF_SLAB.get())
                    || s.is(EarthBlocks.TURF_STAIRS.get()) || s.is(EarthBlocks.GRASS_SLAB.get()) || s.is(EarthBlocks.GRASS_STAIRS.get())) return GRASS;
            if (s.is(Blocks.OAK_LEAVES) || s.is(Blocks.JUNGLE_LEAVES) || s.is(Blocks.ACACIA_LEAVES) || s.is(Blocks.DARK_OAK_LEAVES)
                    || s.is(Blocks.MANGROVE_LEAVES) || s.is(Blocks.VINE)) return FOLIAGE;
            if (s.is(Blocks.LEAF_LITTER)) return DRY_FOLIAGE;
            if (s.is(Blocks.BIRCH_LEAVES)) return BIRCH;
            return s.is(Blocks.SPRUCE_LEAVES) ? SPRUCE : null;
        }

        /** The map's RGB at full brightness. Leaves are darkened so woods stand out from the grass, as their textures do in game. */
        int colour(Biome biome, int x, int z) {
            var effects = biome.getSpecialEffects();
            var climate = biome.getModifiedClimateSettings();
            double temp = Mth.clamp(climate.temperature(), 0.0F, 1.0F), rain = Mth.clamp(climate.downfall(), 0.0F, 1.0F);
            return switch (this) {
                case GRASS -> effects.grassColorModifier().modifyColor(x, z,
                        effects.grassColorOverride().orElseGet(() -> ColorMapColorUtil.get(temp, rain, GRASS_MAP, 0)));
                case FOLIAGE -> leaves(effects.foliageColorOverride().orElseGet(() -> ColorMapColorUtil.get(temp, rain, FOLIAGE_MAP, FoliageColor.FOLIAGE_DEFAULT)));
                case DRY_FOLIAGE -> leaves(effects.dryFoliageColorOverride().orElseGet(() -> ColorMapColorUtil.get(temp, rain, DRY_MAP, 0xFF5A4A2A)));
                case BIRCH -> leaves(FoliageColor.FOLIAGE_BIRCH);
                case SPRUCE -> leaves(FoliageColor.FOLIAGE_EVERGREEN);
                case WATER -> biome.getWaterColor();
            };
        }

        private static int leaves(int rgb) {
            return ARGB.scaleRGB(rgb, 190);
        }

        private static int[] colormap(String name) {
            try (var in = MapTiles.class.getResourceAsStream("/assets/mechanical/dashboard/colormap/" + name + ".png")) {
                return ImageIO.read(java.util.Objects.requireNonNull(in, name)).getRGB(0, 0, 256, 256, null, 0, 256);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
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
        try {
            draw(cx, cz, colours);
        } catch (RuntimeException e) {
            LOGGER.warn("Mechanical map: could not draw chunk {} {}", cx, cz, e);
        }
    }

    /** Draws one chunk into its region, opening it (and closing the least recently used one) as needed. */
    static void draw(int cx, int cz, int[] colours) {
        long key = ChunkPos.pack(cx >> 5, cz >> 5);
        Region r = OPEN.get(key);
        if (r == null) {
            r = new Region(open(cx >> 5, cz >> 5));
            OPEN.put(key, r);
            if (OPEN.size() > OPEN_REGIONS) {
                Map.Entry<Long, Region> eldest = OPEN.entrySet().iterator().next();
                long k = eldest.getKey();
                if (!eldest.getValue().dirty || write(k, eldest.getValue())) OPEN.remove(k);
                else if (OPEN.size() > OPEN_REGIONS + KEPT_FAILED) { // a failed write stays open to retry, but only a few
                    OPEN.remove(k);
                    LOGGER.warn("Mechanical map: region {} {} could not be written, its unsaved edits are lost", ChunkPos.getX(k), ChunkPos.getZ(k));
                }
            }
        }
        if (!java.util.Arrays.equals(colours, r.image.getRGB((cx & 31) * 16, (cz & 31) * 16, 16, 16, null, 0, 16))) VERSIONS.merge(key, 1, Integer::sum);
        r.image.setRGB((cx & 31) * 16, (cz & 31) * 16, 16, 16, colours, 0, 16);
        r.dirty = true;
    }

    private static BufferedImage open(int rx, int rz) {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Path file = file(rx, rz);
        if (Files.exists(file)) {
            try {
                BufferedImage old = ImageIO.read(file.toFile());
                if (old != null) {
                    var g = image.createGraphics();
                    g.drawImage(old, 0, 0, null);
                    g.dispose();
                }
            } catch (IOException e) {
                LOGGER.warn("Mechanical map: could not read {} ({}), drawing it anew", file, e.getMessage());
            }
        }
        return image;
    }

    /** Writes dirty regions: all of them, or only those not written in the last 5 s. Never throws: that would end the periodic flush. */
    private static void flush(boolean all) {
        long now = System.currentTimeMillis();
        OPEN.forEach((key, r) -> {
            try {
                if (r.dirty && (all || now - r.written >= WRITE_EVERY_MS)) write(key, r);
            } catch (RuntimeException e) {
                LOGGER.warn("Mechanical map: could not write region {} {}", ChunkPos.getX(key), ChunkPos.getZ(key), e);
            }
        });
    }

    /** Writes to a temp file and moves it in place, so the web server never reads half a PNG. */
    private static boolean write(long key, Region r) {
        Path file = file(ChunkPos.getX(key), ChunkPos.getZ(key));
        try {
            if (failWrites) throw new IOException("test: writes fail");
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            ImageIO.write(r.image, "png", tmp.toFile());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            r.dirty = false;
            r.failures = 0;
            r.written = System.currentTimeMillis();
            return true;
        } catch (IOException e) {
            r.written = System.currentTimeMillis(); // next try in 5 s, not every second
            if (r.failures++ == 0) LOGGER.warn("Mechanical map: could not write {} ({}), trying again later", file, e.getMessage()); // once per failure streak
            return false;
        }
    }

    private static Path file(int rx, int rz) {
        return folder.resolve(rx + "." + rz + ".png");
    }

    // ---------------------------------------------------------------- the in-game map (com.mechanical.map)

    /** Runs {@code job} on the map's worker thread, after the drawing already queued; false when the map is off. */
    public static boolean onWorker(Runnable job) {
        ScheduledExecutorService exec = worker;
        if (exec == null) return false;
        try {
            exec.execute(job);
            return true;
        } catch (java.util.concurrent.RejectedExecutionException e) {
            return false;
        }
    }

    /** Worker thread only: the region's newest image (not to be changed), or null when it was never drawn. */
    public static BufferedImage region(int rx, int rz) {
        Region open = OPEN.get(ChunkPos.pack(rx, rz));
        if (open != null) return open.image;
        Path file = file(rx, rz);
        try {
            return Files.exists(file) ? ImageIO.read(file.toFile()) : null;
        } catch (IOException e) {
            LOGGER.warn("Mechanical map: could not read {} ({})", file, e.getMessage());
            return null;
        }
    }

    /** Worker thread only. */
    public static int version(int rx, int rz) {
        return VERSIONS.getOrDefault(ChunkPos.pack(rx, rz), 0);
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
