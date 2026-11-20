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
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Thaw {
    /** Melting starts a little above the snow line (0.15), so snow doesn't flicker at the turn of a day. */
    static final float MELT_AT = 0.2F;
    private static final int PER_TICK = 4;

    public static final Supplier<AttachmentType<Long>> UPDATED = Mechanical.ATTACHMENTS.register("season_day", () -> AttachmentType
            .builder(() -> -1L)
            .serialize(Codec.LONG.fieldOf("value"))
            .build());

    private static final LinkedHashSet<Long> QUEUE = new LinkedHashSet<>();

    private Thaw() {}

    static float temperature(ServerLevel level, Biome biome, BlockPos pos) {
        return ((BiomeTemperatureInvoker) (Object) biome).mechanical$temperature(pos, level.getSeaLevel());
    }

    /** Vanilla's snow-and-ice tick at a random column: melt one step there if it's warm. */
    public static void tick(ServerLevel level, BlockPos pos) {
        melt(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos), false);
    }

    /**
     * Melt at the top of a column, if warm enough: a snow layer (on top, or one block down where a thick layer blocks the
     * heightmap) loses a layer, or all of them when catching up; ice just below the top turns to water.
     */
    static boolean melt(ServerLevel level, BlockPos top, boolean all) {
        Biome biome = level.getBiome(top).value();
        if (temperature(level, biome, top) < MELT_AT) return false;
        boolean changed = false;
        for (BlockPos p : new BlockPos[] {top, top.below()}) {
            BlockState s = level.getBlockState(p);
            if (!s.is(Blocks.SNOW)) continue;
            int layers = s.getValue(SnowLayerBlock.LAYERS);
            if (all || layers == 1) level.removeBlock(p, false);
            else level.setBlockAndUpdate(p, s.setValue(SnowLayerBlock.LAYERS, layers - 1));
            changed = true;
            break;
        }
        BlockPos below = top.below();
        if (level.getBlockState(below).is(Blocks.ICE)) {
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
