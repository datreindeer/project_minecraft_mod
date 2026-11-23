package com.mechanical.worldgen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import com.mechanical.Mechanical;

import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * Dev only, BiomeMap's terrain mode (flag line 5 {@code terrain}): the overworld's terrain shape for one seed straight
 * from the chunk generator's base height (no chunks generated), {@link #BLOCKS} blocks per pixel, as a shaded-relief
 * height map ({@code <prefix>_seed<seed>_relief.png}), a slope map ({@code _slope.png}), raw heights ({@code _heights.bin})
 * and statistics in {@code _terrain.txt}: land above y 150 / 200 / 250, the highest peak, flat ground per biome and per
 * erosion band, river valley depth, relief, the structure starts that pass their own placement checks, and a few spots
 * worth photographing (TerrainShots).
 */
final class TerrainMap {
    static final int BLOCKS = 8, SEA = 63;
    /** Structure sets counted (the count area is twice the map side). */
    private static final String[] SETS = {"minecraft:villages", "mechanical:tribe_village", "mechanical:ancient_oak", "mechanical:elkwight_ruin"};
    /** Biomes that should be flat (docs/terrain-plan.md): their flat share is summed up, and the calmest spot is picked among them. */
    static final Set<String> FLAT = Set.of("plains", "sunflower_plains", "savanna", "desert", "snowy_plains", "mammoth_steppe", "peat_bog", "chalk_downs",
            "gypsum_flats", "hazel_meadow", "pine_heath");

    static void render(MinecraftServer server, long seed, int side, File out, String prefix) throws Exception {
        long start = System.currentTimeMillis();
        var access = server.registryAccess();
        var source = MultiNoiseBiomeSource.createFromPreset(access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var gen = new NoiseBasedChunkGenerator(source, settings);
        RandomState rs = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, settings.value());
        Climate.Sampler sampler = rs.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        LevelHeightAccessor world = LevelHeightAccessor.create(-64, 384);
        int px = side / BLOCKS, min = -side / 2;
        // heights (solid ground) and surface biomes, one per pixel
        int[] h = new int[px * px];
        IntStream.range(0, px).parallel().forEach(z -> {
            for (int x = 0; x < px; x++)
                h[x + z * px] = gen.getBaseHeight(min + x * BLOCKS, min + z * BLOCKS, Heightmap.Types.OCEAN_FLOOR_WG, world, rs);
        });
        String[] biome = new String[px * px];
        int tile = 256, step = BLOCKS / 4, qmin = QuartPos.fromBlock(min);
        for (int tx = 0; tx < px; tx += tile) {
            for (int tz = 0; tz < px; tz += tile) {
                int w = Math.min(tile, px - tx), d = Math.min(tile, px - tz);
                SurfaceGrid g = new SurfaceGrid(source, sampler, qmin + tx * step, qmin + tz * step, w, d, step);
                for (int x = 0; x < w; x++)
                    for (int z = 0; z < d; z++) biome[tx + x + (tz + z) * px] = BiomeMap.name(g.get(qmin + (tx + x) * step, qmin + (tz + z) * step));
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("seed " + seed + ", " + side + " x " + side + " blocks round 0,0, " + BLOCKS + " blocks per pixel");
        // land above 150 / 200, the highest peak
        int land = 0, above150 = 0, above200 = 0, above250 = 0, capped = 0, peak = Integer.MIN_VALUE, peakAt = 0;
        for (int i = 0; i < h.length; i++) {
            if (h[i] < SEA) continue;
            land++;
            if (h[i] >= 150) above150++;
            if (h[i] >= 200) above200++;
            if (h[i] >= 250) above250++;
            if (h[i] >= 289) capped++; // squeezed flat by the top slide
            if (h[i] > peak) {
                peak = h[i];
                peakAt = i;
            }
        }
        lines.add(String.format("land %.1f %%, of it above y150 %.2f %%, above y200 %.3f %%, above y250 %.4f %% (%d pixels at y289+); highest peak y %d at %d %d",
                100.0 * land / h.length, 100.0 * above150 / land, 100.0 * above200 / land, 100.0 * above250 / land, capped, peak, min + peakAt % px * BLOCKS,
                min + peakAt / px * BLOCKS));
        // flat ground per biome: random points, flat when the ground 4 blocks east and 4 south differs by at most 1; bumps:
        // the mean second difference over 4 blocks (a steady slope scores 0, small hills high)
        Random r = new Random(seed);
        int n = 24000;
        int[][] pts = new int[n][2];
        for (int[] p : pts) {
            p[0] = min + 4 + r.nextInt(side - 8);
            p[1] = min + 4 + r.nextInt(side - 8);
        }
        int[][] res = new int[n][5];
        int[][] offs = {{0, 0}, {4, 0}, {0, 4}, {-4, 0}, {0, -4}};
        IntStream.range(0, n).parallel().forEach(i -> {
            for (int k = 0; k < 5; k++) res[i][k] = gen.getBaseHeight(pts[i][0] + offs[k][0], pts[i][1] + offs[k][1], Heightmap.Types.OCEAN_FLOOR_WG, world, rs);
        });
        Map<String, int[]> flat = new TreeMap<>(); // samples, flat, bumps
        var flatFn = access.lookupOrThrow(Registries.DENSITY_FUNCTION).get(ResourceKey.create(Registries.DENSITY_FUNCTION,
                Identifier.parse("mechanical:flatness")));
        Map<String, double[]> fl = new TreeMap<>();
        int flatAll = 0, landAll = 0;
        int[][] band = new int[7][2]; // the flat biomes by vanilla erosion band: samples, flat
        float[] edges = {-0.78F, -0.375F, -0.2225F, 0.05F, 0.45F, 0.55F};
        for (int i = 0; i < n; i++) {
            int[] v = res[i];
            if (Arrays.stream(v).min().getAsInt() < SEA) continue;
            Climate.TargetPoint t = sampler.sample(QuartPos.fromBlock(pts[i][0]), 0, QuartPos.fromBlock(pts[i][1]));
            String b = BiomeMap.name(source.getNoiseBiome(new Climate.TargetPoint(t.temperature(), t.humidity(), t.continentalness(), t.erosion(), 0L,
                    t.weirdness())));
            boolean f = Math.abs(v[1] - v[0]) <= 1 && Math.abs(v[2] - v[0]) <= 1;
            int[] c = flat.computeIfAbsent(b, k -> new int[3]);
            c[0]++;
            c[2] += Math.abs(v[1] + v[3] - 2 * v[0]) + Math.abs(v[2] + v[4] - 2 * v[0]);
            if (flatFn.isPresent()) {
                double[] q = fl.computeIfAbsent(b, k -> new double[3]);
                double w = rs.sampleBlockValueUncached(flatFn.get().value(), pts[i][0], v[0], pts[i][1]);
                q[0] += w;
                q[1]++;
                if (w > 0.9) q[2]++;
            }
            landAll++;
            if (f) {
                c[1]++;
                flatAll++;
            }
            if (FLAT.contains(b)) {
                int e = 0;
                while (e < 6 && t.erosion() / 10000F > edges[e]) e++;
                band[e][0]++;
                if (f) band[e][1]++;
            }
        }
        int fb = Arrays.stream(band).mapToInt(x -> x[0]).sum(), ff = Arrays.stream(band).mapToInt(x -> x[1]).sum();
        lines.add(String.format("flat land (slope <= 1 over 4 blocks): %.1f %% of %d land samples; in the flat biomes %.1f %% of %d", 100.0 * flatAll / landAll,
                landAll, 100.0 * ff / fb, fb));
        StringBuilder bands = new StringBuilder("  flat biomes by erosion band E0..E6 (samples flat%):");
        for (int[] x : band) bands.append(String.format(" %d %.0f;", x[0], x[0] == 0 ? 0.0 : 100.0 * x[1] / x[0]));
        lines.add(bands.toString());
        lines.add("  per biome: samples, flat %, bumps (mean 2nd difference over 4 blocks, x and z summed)");
        flat.entrySet().stream().filter(e -> e.getValue()[0] >= 60).sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                .forEach(e -> lines.add(String.format("  %-26s %5d %5.1f %5.2f%s", e.getKey(), e.getValue()[0], 100.0 * e.getValue()[1] / e.getValue()[0],
                        (double) e.getValue()[2] / e.getValue()[0], fl.containsKey(e.getKey()) ? String.format("  flatness %.2f (%.0f %% full)",
                                fl.get(e.getKey())[0] / fl.get(e.getKey())[1], 100 * fl.get(e.getKey())[2] / fl.get(e.getKey())[1]) : "")));
        // river valley depth: along each of 4 axes, the lower of the two banks' highest ground within 96 blocks, above the water
        List<Integer> valley = new ArrayList<>(), mountain = new ArrayList<>();
        int deepest = -1, deepestAt = 0, reach = 96 / BLOCKS;
        int[][] axes = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int z = reach; z < px - reach; z++) {
            for (int x = reach; x < px - reach; x++) {
                int i = x + z * px;
                if (!biome[i].contains("river")) continue;
                int level = Math.max(h[i], SEA), depth = 0;
                for (int[] a : axes) {
                    int ma = Integer.MIN_VALUE, mb = Integer.MIN_VALUE;
                    for (int k = 1; k <= reach; k++) {
                        ma = Math.max(ma, h[x + a[0] * k + (z + a[1] * k) * px]);
                        mb = Math.max(mb, h[x - a[0] * k + (z - a[1] * k) * px]);
                    }
                    depth = Math.max(depth, Math.min(ma, mb) - level);
                }
                valley.add(depth);
                int high = 0;
                for (int dz = -reach; dz <= reach; dz++)
                    for (int dx = -reach; dx <= reach; dx++) high = Math.max(high, h[x + dx + (z + dz) * px]);
                if (high >= 130) mountain.add(depth);
                if (depth > deepest) {
                    deepest = depth;
                    deepestAt = i;
                }
            }
        }
        int[] vd = valley.stream().mapToInt(Integer::intValue).sorted().toArray();
        if (vd.length > 0)
            lines.add(String.format("river valley depth (%d river pixels): median %d, p75 %d, p90 %d, 30+ deep %.1f %%; deepest %d at %d %d", vd.length,
                    vd[vd.length / 2], vd[vd.length * 3 / 4], vd[vd.length * 9 / 10], 100.0 * Arrays.stream(vd).filter(v -> v >= 30).count() / vd.length,
                    deepest, min + deepestAt % px * BLOCKS, min + deepestAt / px * BLOCKS));
        int[] md = mountain.stream().mapToInt(Integer::intValue).sorted().toArray();
        if (md.length > 0)
            lines.add(String.format("  rivers with ground above y130 within 96 blocks (%d pixels): valley depth median %d, p25 %d, p75 %d, 30-60 deep %.1f %%",
                    md.length, md[md.length / 2], md[md.length / 4], md[md.length * 3 / 4],
                    100.0 * Arrays.stream(md).filter(v -> v >= 30 && v <= 60).count() / md.length));
        // relief: highest minus lowest ground in 128-block squares that are mostly land
        List<Integer> relief = new ArrayList<>();
        int win = 128 / BLOCKS, calmest = Integer.MAX_VALUE, calmAt = 0;
        for (int wz = 0; wz + win <= px; wz += win) {
            for (int wx = 0; wx + win <= px; wx += win) {
                int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, dry = 0, plains = 0;
                for (int z = wz; z < wz + win; z++) {
                    for (int x = wx; x < wx + win; x++) {
                        int v = h[x + z * px];
                        lo = Math.min(lo, v);
                        hi = Math.max(hi, v);
                        if (v >= SEA) dry++;
                        if (FLAT.contains(biome[x + z * px])) plains++;
                    }
                }
                if (dry < win * win * 0.9) continue;
                relief.add(hi - lo);
                if (plains >= win * win * 0.8 && hi - lo < calmest) {
                    calmest = hi - lo;
                    calmAt = wx + win / 2 + (wz + win / 2) * px;
                }
            }
        }
        int[] rl = relief.stream().mapToInt(Integer::intValue).sorted().toArray();
        if (rl.length > 0)
            lines.add(String.format("relief in 128-block squares (%d mostly land): median %d, p25 %d, p75 %d, p90 %d", rl.length, rl[rl.length / 2],
                    rl[rl.length / 4], rl[rl.length * 3 / 4], rl[rl.length * 9 / 10]));
        lines.add(String.format("spots: peak %d %d (y %d); deepest river valley %d %d; calmest flat-biome square %d %d (relief %d)", min + peakAt % px * BLOCKS,
                min + peakAt / px * BLOCKS, peak, min + deepestAt % px * BLOCKS, min + deepestAt / px * BLOCKS, min + calmAt % px * BLOCKS,
                min + calmAt / px * BLOCKS, calmest));
        structures(server, gen, source, sampler, rs, seed, side, world, lines);
        lines.add("took " + (System.currentTimeMillis() - start) + " ms");
        ImageIO.write(relief(h, px, seed, lines), "png", new File(out, prefix + "_seed" + seed + "_relief.png"));
        ImageIO.write(slope(h, px, seed), "png", new File(out, prefix + "_seed" + seed + "_slope.png"));
        // raw heights (little-endian shorts, row by row) for comparing runs
        var buf = java.nio.ByteBuffer.allocate(h.length * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (int v : h) buf.putShort((short) v);
        Files.write(new File(out, prefix + "_seed" + seed + "_heights.bin").toPath(), buf.array());
        File f = new File(out, prefix + "_seed" + seed + "_terrain.txt");
        Files.write(f.toPath(), lines);
        for (String l : lines) Mechanical.LOGGER.info("terrainmap: {}", l);
    }

    /** Starts of each counted set within side blocks of 0,0 that pass the structure's own placement checks (like /locate). */
    private static void structures(MinecraftServer server, NoiseBasedChunkGenerator gen, MultiNoiseBiomeSource source, Climate.Sampler sampler,
            RandomState rs, long seed, int side, LevelHeightAccessor world, List<String> lines) {
        var access = server.registryAccess();
        var sets = access.lookupOrThrow(Registries.STRUCTURE_SET);
        var state = ChunkGeneratorStructureState.createForNormal(rs, seed, ChunkPos.ZERO, source, sets);
        int chunks = side / 16; // half the side of the count area, in chunks
        StringBuilder sb = new StringBuilder("structure starts within " + side + " blocks (square " + 2 * side + " wide):");
        for (String id : SETS) {
            var holder = sets.get(ResourceKey.create(Registries.STRUCTURE_SET, Identifier.parse(id)));
            if (holder.isEmpty()) {
                sb.append(' ').append(id).append(" missing;");
                continue;
            }
            StructureSet set = holder.get().value();
            if (!(set.placement() instanceof RandomSpreadStructurePlacement p)) continue;
            int tried = 0, valid = 0;
            for (int rx = -chunks / p.spacing() - 1; rx <= chunks / p.spacing(); rx++) {
                for (int rz = -chunks / p.spacing() - 1; rz <= chunks / p.spacing(); rz++) {
                    ChunkPos c = p.getPotentialStructureChunk(seed, rx * p.spacing(), rz * p.spacing());
                    if (Math.abs(c.x()) > chunks || Math.abs(c.z()) > chunks || !p.isStructureChunk(state, c.x(), c.z())) continue;
                    tried++;
                    for (StructureSet.StructureSelectionEntry e : set.structures()) {
                        var s = e.structure().value();
                        if (s.generate(e.structure(), Level.OVERWORLD, access, gen, source, sampler, rs, server.getStructureTemplateManager(), seed, c, 0,
                                world, s.biomes()::contains).isValid()) {
                            valid++;
                            break;
                        }
                    }
                }
            }
            sb.append(' ').append(id).append(' ').append(valid).append(" of ").append(tried).append(';');
        }
        lines.add(sb.toString());
    }

    /** Height colours (y, rgb) between which the relief map blends. */
    private static final int[][] TINTS = {{SEA, 0x5d8a46}, {80, 0x7fa055}, {100, 0xa7b06a}, {125, 0xc2ad74}, {150, 0xa9845a}, {180, 0x8c7562},
            {210, 0x9a948e}, {240, 0xd6d6d6}, {280, 0xffffff}};

    private static int tint(int y) {
        if (y < SEA) {
            float d = Math.min(1F, (SEA - y) / 40F);
            return mix(0x4f86c6, 0x1c3d78, d);
        }
        for (int k = 1; k < TINTS.length; k++)
            if (y < TINTS[k][0]) return mix(TINTS[k - 1][1], TINTS[k][1], (float) (y - TINTS[k - 1][0]) / (TINTS[k][0] - TINTS[k - 1][0]));
        return TINTS[TINTS.length - 1][1];
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t),
                bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int scale(int c, float s) {
        return s <= 1F ? mix(0, c, s) : mix(c, 0xffffff, Math.min(1F, s - 1F));
    }

    /** Shaded relief (light from the north-west, heights x2), hypsometric tint, contours at y 150 and 200, and the stats. */
    private static BufferedImage relief(int[] h, int px, long seed, List<String> lines) {
        int legend = 420;
        BufferedImage img = new BufferedImage(px + legend, Math.max(px, 600), BufferedImage.TYPE_INT_RGB);
        double lx = -0.5, lz = -0.5, ly = 0.7071; // towards the light
        for (int z = 0; z < px; z++) {
            for (int x = 0; x < px; x++) {
                int y = h[x + z * px], c = tint(y);
                double gx = (h[Math.min(px - 1, x + 1) + z * px] - h[Math.max(0, x - 1) + z * px]) * 2.0 / (2 * BLOCKS),
                        gz = (h[x + Math.min(px - 1, z + 1) * px] - h[x + Math.max(0, z - 1) * px]) * 2.0 / (2 * BLOCKS);
                double len = Math.sqrt(gx * gx + gz * gz + 1), shade = (-gx * lx - gz * lz + ly) / len; // normal (-gx, 1, -gz)
                float s = (float) (0.45 + 0.75 * Math.max(0, shade));
                c = y < SEA ? scale(c, 0.85F + 0.15F * s) : scale(c, s);
                for (int line : new int[] {150, 200, 250}) {
                    boolean edge = y >= line && (x > 0 && h[x - 1 + z * px] < line || z > 0 && h[x + (z - 1) * px] < line);
                    if (edge) c = line == 150 ? 0x704020 : line == 200 ? 0x400000 : 0xa00000;
                }
                img.setRGB(x, z, c);
            }
        }
        Graphics2D gr = img.createGraphics();
        gr.setColor(Color.WHITE);
        gr.fillRect(px, 0, legend, img.getHeight());
        gr.fillRect(0, px, px, img.getHeight() - px);
        gr.setColor(Color.BLACK);
        gr.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        int y = 12;
        for (String l : lines) {
            for (int i = 0; i < l.length(); i += 66) {
                gr.drawString(l.substring(i, Math.min(l.length(), i + 66)), px + 6, y);
                y += 11;
            }
        }
        y += 6;
        for (int[] t : TINTS) {
            gr.setColor(new Color(t[1]));
            gr.fillRect(px + 6, y - 9, 12, 10);
            gr.setColor(Color.BLACK);
            gr.drawString("y " + t[0] + (t[0] == 150 ? " (brown line)" : t[0] == 210 ? " (dark red line: 200, red: 250)" : ""), px + 24, y);
            y += 12;
        }
        gr.dispose();
        return img;
    }

    /** Steepest step to a neighbour, per 4 blocks: green flat (<= 1), yellow, orange, red, purple cliffs; water blue. */
    private static BufferedImage slope(int[] h, int px, long seed) {
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < px; z++) {
            for (int x = 0; x < px; x++) {
                int y = h[x + z * px];
                if (y < SEA) {
                    img.setRGB(x, z, 0x3a6ab0);
                    continue;
                }
                int d = 0;
                if (x + 1 < px) d = Math.max(d, Math.abs(h[x + 1 + z * px] - y));
                if (z + 1 < px) d = Math.max(d, Math.abs(h[x + (z + 1) * px] - y));
                float s = d * 4F / BLOCKS;
                img.setRGB(x, z, s <= 1 ? 0x6fb04a : s <= 2 ? 0xd8d040 : s <= 4 ? 0xe08a30 : s <= 8 ? 0xc02a20 : 0x6a1a6a);
            }
        }
        return img;
    }

    private TerrainMap() {}
}
