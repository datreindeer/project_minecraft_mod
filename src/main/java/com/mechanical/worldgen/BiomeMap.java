package com.mechanical.worldgen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.mechanical.Mechanical;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Dev only: with a file {@code biomemap.flag} in the game directory (line 1: output folder, line 2: seeds, line 3: map
 * side in blocks, line 4: file name prefix), renders the overworld surface biome map (after the mod's rules) of each seed
 * centred on 0,0 to {@code <prefix>_seed<seed>.png}, plus a close-up of the first Scorched Barrens found, and logs how
 * often a cold biome lies within 32 blocks of a warm one ({@link #COLD}, {@link #WARM}; marked red on the map).
 * Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class BiomeMap {
    static final Set<String> COLD = Set.of("snowy_plains", "ice_spikes", "snowy_taiga", "snowy_slopes", "grove", "frozen_peaks", "jagged_peaks",
            "snowy_beach", "frozen_river", "mammoth_steppe", "glacial_moraine");
    static final Set<String> WARM = Set.of("desert", "savanna", "savanna_plateau", "windswept_savanna", "badlands", "eroded_badlands", "wooded_badlands",
            "jungle", "sparse_jungle", "bamboo_jungle", "mangrove_swamp", "gypsum_flats", "ochre_badlands", "marble_gorge");
    private static final Map<String, Integer> COLOURS = new LinkedHashMap<>();

    static {
        String[] c = {
            // cold
            "snowy_plains", "f4f8ff", "ice_spikes", "b4dcff", "snowy_taiga", "6f9a8f", "snowy_slopes", "dfe8f0", "grove", "8fb8a0", "frozen_peaks", "c8d8f8",
            "jagged_peaks", "e8e8f0", "snowy_beach", "e8eef0", "frozen_river", "a0c8ff", "frozen_ocean", "7090c0", "deep_frozen_ocean", "506898",
            "mammoth_steppe", "c8bb88", "glacial_moraine", "8a8f80",
            // cool / temperate
            "taiga", "3f6f50", "old_growth_spruce_taiga", "2f5a40", "old_growth_pine_taiga", "4a6a3a", "plains", "8db360", "sunflower_plains", "b5c860",
            "forest", "2f7d2f", "flower_forest", "5fa040", "birch_forest", "5d9e5d", "old_growth_birch_forest", "4f8f4f", "dark_forest", "1f4a1f",
            "pale_garden", "a0a8a0", "dappled_forest", "4f8a3a", "meadow", "9ccf70", "cherry_grove", "f0a0c8", "swamp", "4a5a30", "windswept_hills", "8a9a8a",
            "windswept_gravelly_hills", "9a9a9a", "windswept_forest", "557755", "stony_peaks", "a0a0a0", "stony_shore", "707070", "beach", "f0e0a0",
            "river", "3060ff", "ocean", "1030a0", "deep_ocean", "0a2070", "cold_ocean", "2040b0", "deep_cold_ocean", "183088", "lukewarm_ocean", "1048c0",
            "deep_lukewarm_ocean", "0c3890", "warm_ocean", "1060e0", "mushroom_fields", "ff00ff",
            // warm
            "desert", "f0d070", "savanna", "c8b020", "savanna_plateau", "b09010", "windswept_savanna", "d8c040", "badlands", "d06020", "eroded_badlands", "e07030",
            "wooded_badlands", "a05a20", "jungle", "00d060", "sparse_jungle", "40e090", "bamboo_jungle", "00b0a0", "mangrove_swamp", "308060",
            // the mod's surface biomes
            "deciduous_forest", "3f8a20", "hazel_meadow", "a8c070", "willow_fen", "5a7040", "pine_heath", "8a7a50", "chalk_downs", "e0e8d0",
            "peat_bog", "5a4a30", "slate_highlands", "5a6070", "soapstone_valley", "7a9a88", "limestone_karst", "c8c8b0", "marble_gorge", "d8b8a8",
            "ochre_badlands", "c09020", "basalt_coast", "303030", "shingle_beach", "b0a898", "reed_delta", "90a050", "gypsum_flats", "f0c8a0",
            "scorched_barrens", "200000"};
        for (int i = 0; i < c.length; i += 2) COLOURS.put(c[i], Integer.parseInt(c[i + 1], 16));
    }

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        File flag = new File("biomemap.flag");
        if (!flag.exists()) return;
        try {
            List<String> lines = Files.readAllLines(flag.toPath());
            File out = new File(lines.get(0).trim());
            out.mkdirs();
            int side = Integer.parseInt(lines.get(2).trim());
            String prefix = lines.get(3).trim();
            for (String s : lines.get(1).trim().split("\\s+")) render(event.getServer(), Long.parseLong(s), side, out, prefix);
        } catch (Exception e) {
            Mechanical.LOGGER.error("biomemap failed", e);
        }
    }

    static String name(Holder<Biome> h) {
        return h.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
    }

    private static int colour(String name) {
        Integer c = COLOURS.get(name);
        return c != null ? c : (name.hashCode() * 0x9E3779B1) & 0xFFFFFF;
    }

    private static void render(MinecraftServer server, long seed, int side, File out, String prefix) throws Exception {
        var access = server.registryAccess();
        var source = MultiNoiseBiomeSource.createFromPreset(access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        Climate.Sampler sampler = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, settings).createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        Holder<Biome> forest = access.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.FOREST);
        long start = System.currentTimeMillis();
        // overview: one pixel per 16 blocks (4 quarts)
        int px = side / 16, step = 4, min = -side / 8; // min in quarts
        String[] map = new String[px * px];
        int tile = 256;
        for (int tx = 0; tx < px; tx += tile) {
            for (int tz = 0; tz < px; tz += tile) {
                int w = Math.min(tile, px - tx), d = Math.min(tile, px - tz);
                SurfaceGrid g = new SurfaceGrid(source, sampler, min + tx * step, min + tz * step, w, d, step);
                for (int x = 0; x < w; x++) {
                    for (int z = 0; z < d; z++) {
                        int qx = min + (tx + x) * step, qz = min + (tz + z) * step;
                        Holder<Biome> h = g.get(qx, qz);
                        if (h.is(ModBiomes.SCORCHED_BARRENS) && !ScorchedBarrens.stays(SurfaceGrid.around(source, sampler, qx, qz, ScorchedBarrens.RING), qx, qz))
                            h = forest;
                        map[tx + x + (tz + z) * px] = name(h);
                    }
                }
            }
        }
        // distance (pixels, up to 16 = 256 blocks) from each pixel to the nearest warm one: a two-pass chamfer transform
        int[] dist = new int[map.length];
        for (int i = 0; i < map.length; i++) dist[i] = WARM.contains(map[i]) ? 0 : 1000;
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < map.length; k++) {
                int i = pass == 0 ? k : map.length - 1 - k, x = i % px, z = i / px, sgn = pass == 0 ? -1 : 1;
                for (int[] o : new int[][] {{sgn, 0, 3}, {0, sgn, 3}, {sgn, sgn, 4}, {-sgn, sgn, 4}}) {
                    int nx = x + o[0], nz = z + o[1];
                    if (nx >= 0 && nz >= 0 && nx < px && nz < px) dist[i] = Math.min(dist[i], dist[nx + nz * px] + o[2]);
                }
            }
        }
        // cold land pixels within 32 / 64 / 128 / 256 blocks of a warm one (chamfer units: 3 per pixel)
        int land = 0, cold = 0, barrens = 0, beyond = 0;
        int[] near = new int[4];
        boolean[] bad = new boolean[map.length];
        for (int x = 0; x < px; x++) {
            for (int z = 0; z < px; z++) {
                String n = map[x + z * px];
                if (!n.contains("ocean")) land++;
                long bx = (long) (x - px / 2) * 16, bz = (long) (z - px / 2) * 16;
                if (bx * bx + bz * bz >= 2600L * 2600L) {
                    beyond++;
                    if (n.equals("scorched_barrens")) barrens++;
                }
                if (!COLD.contains(n)) continue;
                cold++;
                int d = dist[x + z * px];
                for (int k = 0; k < 4; k++) if (d <= 3 * (2 << k)) near[k]++;
                bad[x + z * px] = d <= 3 * 4;
            }
        }
        int coldNearWarm = near[0];
        Map<String, Integer> present = new LinkedHashMap<>();
        for (String n : map) present.merge(n, 1, Integer::sum);
        int legend = 230;
        BufferedImage img = new BufferedImage(px + legend, Math.max(px, 14 * present.size() + 30), BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = img.createGraphics();
        gr.setColor(Color.WHITE);
        gr.fillRect(px, 0, legend, img.getHeight());
        for (int x = 0; x < px; x++) for (int z = 0; z < px; z++) img.setRGB(x, z, bad[x + z * px] ? 0xFF0000 : colour(map[x + z * px]));
        gr.setColor(Color.RED);
        int r = 2500 / 16;
        gr.drawOval(px / 2 - r, px / 2 - r, 2 * r, 2 * r);
        gr.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        int y = 14;
        gr.setColor(Color.BLACK);
        gr.drawString("seed " + seed + ", " + side + " blocks, 16/px", px + 6, y);
        List<String> names = new ArrayList<>(COLOURS.keySet());
        names.removeIf(n -> !present.containsKey(n));
        for (String n : present.keySet()) if (!names.contains(n)) names.add(n);
        for (String n : names) {
            y += 14;
            gr.setColor(new Color(colour(n)));
            gr.fillRect(px + 6, y - 10, 12, 11);
            gr.setColor(Color.BLACK);
            gr.drawRect(px + 6, y - 10, 12, 11);
            gr.drawString(n + (COLD.contains(n) ? " (cold)" : WARM.contains(n) ? " (warm)" : ""), px + 24, y);
        }
        gr.dispose();
        File f = new File(out, prefix + "_seed" + seed + ".png");
        ImageIO.write(img, "png", f);
        Mechanical.LOGGER.info("biomemap: seed {}: {} land pixels, {} cold; cold within 32/64/128/256 blocks of warm: {} {} {} {} (red: within 64); "
                + "beyond 2600: {} of {} Scorched Barrens ({}%); {} ms -> {}", seed, land, cold, coldNearWarm, near[1], near[2], near[3],
                barrens, beyond, String.format("%.3f", 100.0 * barrens / beyond), System.currentTimeMillis() - start, f);
        // close-up of the first barrens beyond 2600 (4 blocks per pixel, 2048 blocks)
        for (int i = 0; i < map.length; i++) {
            long bx = (long) (i % px - px / 2) * 16, bz = (long) (i / px - px / 2) * 16;
            if (!map[i].equals("scorched_barrens") || bx * bx + bz * bz < 2600L * 2600L) continue;
            closeUp(source, sampler, forest, (int) bx / 4, (int) bz / 4, new File(out, prefix + "_seed" + seed + "_barrens.png"));
            break;
        }
    }

    private static void closeUp(MultiNoiseBiomeSource source, Climate.Sampler sampler, Holder<Biome> forest, int cqx, int cqz, File f) throws Exception {
        int n = 512, pad = ScorchedBarrens.RING;
        SurfaceGrid g = new SurfaceGrid(source, sampler, cqx - n / 2 - pad, cqz - n / 2 - pad, n + 2 * pad, n + 2 * pad, 1);
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < n; x++) {
            for (int z = 0; z < n; z++) {
                int qx = cqx - n / 2 + x, qz = cqz - n / 2 + z;
                Holder<Biome> h = g.get(qx, qz);
                if (h.is(ModBiomes.SCORCHED_BARRENS) && !ScorchedBarrens.stays(g, qx, qz)) h = forest;
                img.setRGB(x, z, colour(name(h)));
            }
        }
        ImageIO.write(img, "png", f);
        Mechanical.LOGGER.info("biomemap: barrens close-up round {}, {} -> {}", cqx * 4, cqz * 4, f);
    }

    private BiomeMap() {}
}
