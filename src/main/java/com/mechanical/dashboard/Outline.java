package com.mechanical.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.level.ChunkPos;

/** Turns a set of chunks into closed polygons along their outer edges, so the map draws a tribe's land as one outline. */
public final class Outline {
    private static final int[] DX = {1, 0, -1, 0}, DZ = {0, 1, 0, -1}; // east, south, west, north

    private Outline() {}

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Closed rings (outer edges and holes) in block coordinates, each {@code x0,z0,x1,z1,...} with only the corners.
     * Every edge facing a missing neighbour is walked with the chunk on its right; where two chunks touch only at a corner the walk turns
     * right, so they stay separate rings instead of merging through the corner.
     */
    public static List<int[]> of(Set<Long> chunks) {
        Map<Long, Integer> out = new HashMap<>(); // vertex -> bitmask of outgoing directions
        for (long c : chunks) {
            int x = ChunkPos.getX(c), z = ChunkPos.getZ(c);
            if (!chunks.contains(ChunkPos.pack(x, z - 1))) out.merge(key(x, z), 1, (a, b) -> a | b);
            if (!chunks.contains(ChunkPos.pack(x + 1, z))) out.merge(key(x + 1, z), 2, (a, b) -> a | b);
            if (!chunks.contains(ChunkPos.pack(x, z + 1))) out.merge(key(x + 1, z + 1), 4, (a, b) -> a | b);
            if (!chunks.contains(ChunkPos.pack(x - 1, z))) out.merge(key(x, z + 1), 8, (a, b) -> a | b);
        }
        List<int[]> rings = new ArrayList<>();
        for (long first : new ArrayList<>(out.keySet())) {
            while (out.get(first) != 0) {
                int sx = (int) (first >> 32), sz = (int) first, d0 = Integer.numberOfTrailingZeros(out.get(first));
                int x = sx, z = sz, d = d0;
                List<int[]> walk = new ArrayList<>(); // x, z, direction of each edge
                while (true) {
                    walk.add(new int[] {x, z, d});
                    out.merge(key(x, z), ~(1 << d), (a, b) -> a & b);
                    x += DX[d];
                    z += DZ[d];
                    int mask = out.getOrDefault(key(x, z), 0) | (x == sx && z == sz ? 1 << d0 : 0), nd = -1;
                    for (int t : new int[] {1, 0, 3}) if (nd < 0 && (mask & (1 << ((d + t) % 4))) != 0) nd = (d + t) % 4;
                    if (x == sx && z == sz && nd == d0) break;
                    d = nd;
                }
                List<Integer> ring = new ArrayList<>();
                for (int i = 0; i < walk.size(); i++)
                    if (walk.get((i + walk.size() - 1) % walk.size())[2] != walk.get(i)[2]) { // a corner, not a point on a straight run
                        ring.add(walk.get(i)[0] * 16);
                        ring.add(walk.get(i)[1] * 16);
                    }
                rings.add(ring.stream().mapToInt(Integer::intValue).toArray());
            }
        }
        return rings;
    }
}
