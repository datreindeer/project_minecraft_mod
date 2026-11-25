package com.mechanical.clan;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The chunk sets behind a tribe's territory (Clans#recomputeTerritory), as packed {@link ChunkPos#pack} longs: a square
 * round the Fire Pit, and for a big pit a flood fill over its biome. Pure apart from {@link #surfaceBiome}, so the shapes
 * can be tested without a world.
 */
public final class Territory {
    private Territory() {}

    /** The (2r+1)x(2r+1) chunks round {@code centre}. */
    public static Set<Long> square(ChunkPos centre, int r) {
        Set<Long> out = new HashSet<>();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) out.add(ChunkPos.pack(centre.x() + x, centre.z() + z));
        return out;
    }

    /**
     * Chunks 4-connected to {@code start} through {@code sameBiome} chunks, within {@code max} of it each way, never entering
     * a {@code blocked} one. Holds {@code start} itself unless it is blocked (whatever its biome).
     */
    public static Set<Long> fill(ChunkPos start, int max, Predicate<ChunkPos> sameBiome, Predicate<ChunkPos> blocked) {
        Set<Long> out = new HashSet<>();
        if (blocked.test(start)) return out;
        ArrayDeque<ChunkPos> todo = new ArrayDeque<>();
        out.add(start.pack());
        todo.add(start);
        while (!todo.isEmpty()) {
            ChunkPos c = todo.poll();
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                ChunkPos n = new ChunkPos(c.x() + d[0], c.z() + d[1]);
                if (Math.abs(n.x() - start.x()) > max || Math.abs(n.z() - start.z()) > max || out.contains(n.pack())) continue;
                if (blocked.test(n) || !sameBiome.test(n)) continue;
                out.add(n.pack());
                todo.add(n);
            }
        }
        return out;
    }

    /**
     * The biome at the surface in the middle of a chunk. A loaded chunk answers itself; any other is asked of the generator
     * (its biome source's resolver, so the mod's biome rules apply, at the generator's surface height), which never loads
     * or generates a chunk.
     */
    public static Holder<Biome> surfaceBiome(ServerLevel level, ChunkPos c) {
        int x = c.getMiddleBlockX(), z = c.getMiddleBlockZ();
        if (level.hasChunk(c.x(), c.z())) return level.getBiome(new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z));
        var source = level.getChunkSource();
        int y = source.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
        return source.getGenerator().getBiomeSource().createUncachedResolver(source.randomState())
                .getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
    }
}
