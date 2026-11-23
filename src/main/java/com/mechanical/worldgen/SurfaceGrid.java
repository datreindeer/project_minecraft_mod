package com.mechanical.worldgen;

import java.util.function.Predicate;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;

/**
 * The raw surface biomes of a rectangle of quarts: the climate at depth 0 (the surface; the climate noises other than
 * depth do not vary with height), straight from the parameter list, before MultiNoiseBiomeSourceMixin's rules. Sampled
 * as one volume, like vanilla's chunk resolver. Used by the mixin's neighbourhood rule, the Elkwight ruin's placement
 * and BiomeMap.
 * <p>A climate exactly on the border of two slots ties, and vanilla's R-tree then keeps whichever its last search found,
 * so the real world can show either. {@link #is} and {@link #all} therefore also ask the climate nudged one step up and
 * one step down on every axis, which lands on both sides of such a border.
 */
public final class SurfaceGrid {
    private final int minX, minZ, sizeX, step;
    private final Holder<Biome>[] biomes, up, down;

    /** Quarts minQx.. (sizeX of them, every step quarts) by minQz.. */
    @SuppressWarnings("unchecked")
    public SurfaceGrid(MultiNoiseBiomeSource source, Climate.Sampler s, int minQx, int minQz, int sizeX, int sizeZ, int step) {
        this.minX = minQx;
        this.minZ = minQz;
        this.sizeX = sizeX;
        this.step = step;
        DensityVolume v = new DensityVolume(sizeX, 1, sizeZ, QuartPos.toBlock(minQx), 0, QuartPos.toBlock(minQz), step * 4, 4, step * 4);
        DensityBuffer t = DensityBuffer.createUnpooled(v.size()), h = DensityBuffer.createUnpooled(v.size()), c = DensityBuffer.createUnpooled(v.size()),
                e = DensityBuffer.createUnpooled(v.size()), w = DensityBuffer.createUnpooled(v.size());
        s.temperature().sampleVolume(t, v);
        s.humidity().sampleVolume(h, v);
        s.continentalness().sampleVolume(c, v);
        s.erosion().sampleVolume(e, v);
        s.weirdness().sampleVolume(w, v);
        biomes = new Holder[v.size()];
        up = new Holder[v.size()];
        down = new Holder[v.size()];
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                int i = v.indexUnchecked(x, 0, z), j = x + z * sizeX;
                Climate.TargetPoint p = Climate.target(t.get(i), h.get(i), c.get(i), e.get(i), 0.0F, w.get(i));
                biomes[j] = source.getNoiseBiome(p);
                up[j] = source.getNoiseBiome(nudge(p, 1));
                down[j] = source.getNoiseBiome(nudge(p, -1));
            }
        }
    }

    /** A square of side 2 * radius + 1 quarts round (qx, qz). */
    public static SurfaceGrid around(MultiNoiseBiomeSource source, Climate.Sampler s, int qx, int qz, int radius) {
        return new SurfaceGrid(source, s, qx - radius, qz - radius, 2 * radius + 1, 2 * radius + 1, 1);
    }

    // shortcut: a climate on two borders at once can still hide a third biome; one in tens of millions of quarts
    private static Climate.TargetPoint nudge(Climate.TargetPoint p, long d) {
        return new Climate.TargetPoint(p.temperature() + d, p.humidity() + d, p.continentalness() + d, p.erosion() + d, 0L, p.weirdness() + d);
    }

    private int index(int qx, int qz) {
        return (qx - minX) / step + (qz - minZ) / step * sizeX;
    }

    /** The raw biome at a grid quart. */
    public Holder<Biome> get(int qx, int qz) {
        return biomes[index(qx, qz)];
    }

    /** Whatever the world may show at a grid quart passes, ties on a slot border included. */
    public boolean is(int qx, int qz, Predicate<Holder<Biome>> ok) {
        int i = index(qx, qz);
        return ok.test(biomes[i]) && ok.test(up[i]) && ok.test(down[i]);
    }

    /** Every grid quart within radius quarts of (qx, qz) passes {@link #is} (step 1 grids). */
    public boolean all(int qx, int qz, int radius, Predicate<Holder<Biome>> ok) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= radius * radius && !is(qx + dx, qz + dz, ok)) return false;
        return true;
    }
}
