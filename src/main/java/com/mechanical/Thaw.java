package com.mechanical;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

import com.mechanical.mixin.BiomeTemperatureInvoker;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Spring thaw (docs/seasons-and-tribes.md). Vanilla never melts snow, so its snow-and-ice tick (mixin.ServerLevelThawMixin)
 * also melts: where a column's temperature (season included) is at least {@link #MELT_AT}, a snow layer goes down by one and
 * surface ice turns back to water, so the thaw creeps over a few minutes. Chunks that were not loaded meanwhile catch up when
 * they load (a few per tick): where it is warm all their snow and ice melts at once, where it snows they get a thin snow
 * cover, so a forest visited first in midwinter is white already. Each chunk remembers the calendar day it was last brought
 * up to date ({@link #UPDATED}). Seas only freeze where they are frozen-cold all year (no seasonal sea ice); rivers and
 * lakes do freeze.
 * <p>
 * Snowstorms: a thunderstorm in winter ({@link #storm}) snows hard. Each snow tick also drifts snow onto
 * {@link #STORM_COLUMNS} more columns of the chunk, piling it up to {@link #STORM_DEPTH} layers on open ground and
 * {@link #UNDER_TREES} under trees (on the ground below the leaves, which vanilla never snows on), wherever it is cold
 * enough to snow. Not under roofs. Advancement: Whiteout (Seasons).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Thaw {
    /** Melting starts a little above the snow line (0.15), so snow doesn't flicker at the turn of a day. */
    static final float MELT_AT = 0.2F;
    private static final int PER_TICK = 4;
    /** Snow falls where a column's temperature (season included) is below this, as in vanilla. */
    public static final float SNOW_BELOW = 0.15F;
    /** Extra columns per snow tick in a snowstorm, and how deep it piles snow in the open and under trees. */
    static final int STORM_COLUMNS = 4, STORM_DEPTH = 3, UNDER_TREES = 2;

    public static final Supplier<AttachmentType<Long>> UPDATED = Mechanical.ATTACHMENTS.register("season_day", () -> AttachmentType
            .builder(() -> -1L)
            .serialize(Codec.LONG.fieldOf("value"))
            .build());

    private static final LinkedHashSet<Long> QUEUE = new LinkedHashSet<>();

    private Thaw() {}

    public static float temperature(ServerLevel level, Biome biome, BlockPos pos) {
        return ((BiomeTemperatureInvoker) (Object) biome).mechanical$temperature(pos, level.getSeaLevel());
    }

    /** Snow lying here never melts, not even at midsummer (worldgen.SmoothGround lays snow steps only there). */
    public static boolean coldAllYear(ServerLevel level, Biome biome, BlockPos pos) {
        float base = biome.getBaseTemperature();
        return temperature(level, biome, pos) - Seasons.offset(base) + Seasons.offset(base, 1) < MELT_AT;
    }

    /** Vanilla's snow-and-ice tick at a random column: melt one step there if it's warm. */
    public static void tick(ServerLevel level, BlockPos pos) {
        melt(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos), false);
        if (!storm(level)) return;
        drift(level, pos);
        int x0 = pos.getX() & ~15, z0 = pos.getZ() & ~15;
        for (int i = 1; i < STORM_COLUMNS; i++) drift(level, new BlockPos(x0 + level.getRandom().nextInt(16), 0, z0 + level.getRandom().nextInt(16)));
    }

    /** A thunderstorm in winter is a snowstorm (where it is cold enough to snow). */
    public static boolean storm(ServerLevel level) {
        return level.isThundering() && Calendar.season(level.getOverworldClockTime()) == 3;
    }

    /** Snowstorm on one column: a layer more on the ground, under the leaves too, up to the open or under-tree depth. */
    static boolean drift(ServerLevel level, BlockPos column) {
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        boolean open = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column).getY() == ground.getY();
        if (level.getBlockState(ground.below()).is(Blocks.SNOW)) ground = ground.below(); // 2+ layers block the heightmap
        if (temperature(level, level.getBiome(ground).value(), ground) >= SNOW_BELOW) return false;
        return pile(level, ground, open ? STORM_DEPTH : UNDER_TREES);
    }

    /** One more snow layer at p (a new layer on air where snow can lie), up to max; false when nothing changed. */
    static boolean pile(ServerLevel level, BlockPos p, int max) {
        if (Magma.meltsSnow(level, p)) return false;
        BlockState s = level.getBlockState(p);
        if (s.is(Blocks.SNOW)) {
            int layers = s.getValue(SnowLayerBlock.LAYERS);
            if (layers >= max) return false;
            level.setBlockAndUpdate(p, s.setValue(SnowLayerBlock.LAYERS, layers + 1));
            return true;
        }
        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (!s.isAir() || !snow.canSurvive(level, p)) return false;
        level.setBlockAndUpdate(p, snow);
        return true;
    }

    /**
     * Melt at the top of a column, if warm enough (or a Magma Block warms the spot): a snow layer (on top, or one block down where a thick layer blocks the
     * heightmap) loses a layer, or all of them when catching up; ice just below the top turns to water.
     */
    static boolean melt(ServerLevel level, BlockPos top, boolean all) {
        Biome biome = level.getBiome(top).value();
        boolean warm = temperature(level, biome, top) >= MELT_AT, changed = false;
        for (BlockPos p : new BlockPos[] {top, top.below()}) {
            BlockState s = level.getBlockState(p);
            if (!s.is(Blocks.SNOW) || !warm && !Magma.meltsSnow(level, p)) continue;
            int layers = s.getValue(SnowLayerBlock.LAYERS);
            if (all || layers == 1) level.removeBlock(p, false);
            else level.setBlockAndUpdate(p, s.setValue(SnowLayerBlock.LAYERS, layers - 1));
            changed = true;
            break;
        }
        BlockPos below = top.below();
        if (level.getBlockState(below).is(Blocks.ICE) && (warm || Magma.meltsSnow(level, below)) && !com.mechanical.folklore.Beings.frostHeartNear(level, below)) {
            level.setBlockAndUpdate(below, Blocks.WATER.defaultBlockState());
            changed = true;
        }
        return changed;
    }

    /** Seas freeze only where they are cold all year (frozen oceans); others never get seasonal sea ice. */
    public static boolean mayFreeze(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return !biome.is(BiomeTags.IS_OCEAN) || biome.value().getBaseTemperature() < 0.15F;
    }

    // ---------------------------------------------------------------- catching up on load

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        // this chunk, and neighbours that were only waiting for it (setting blocks at an edge must not load the next chunk)
        ChunkPos pos = event.getChunk().getPos();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) QUEUE.add(ChunkPos.pack(pos.x() + dx, pos.z() + dz));
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || QUEUE.isEmpty()) return;
        long today = Calendar.day(level.getOverworldClockTime());
        int done = 0;
        for (Iterator<Long> it = QUEUE.iterator(); it.hasNext() && done < PER_TICK; ) {
            long packed = it.next();
            it.remove();
            int cx = ChunkPos.getX(packed), cz = ChunkPos.getZ(packed);
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null || chunk.getData(UPDATED) == today || !neighboursLoaded(level, cx, cz)) continue;
            catchUp(level, chunk);
            chunk.setData(UPDATED, today);
            chunk.markUnsaved();
            done++;
        }
    }

    private static boolean neighboursLoaded(ServerLevel level, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (level.getChunkSource().getChunkNow(cx + dx, cz + dz) == null) return false;
        return true;
    }

    /** Bring a chunk's snow and ice to the season: melt where warm, a thin snow cover where it snows. */
    static void catchUp(ServerLevel level, LevelChunk chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                column.set(cp.getMinBlockX() + x, 0, cp.getMinBlockZ() + z);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
                if (melt(level, top, true)) continue;
                Biome biome = level.getBiome(top).value();
                if (biome.shouldSnow(level, top) && level.getBlockState(top).isAir()) level.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
            }
        }
    }
}
