package com.mechanical.people;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.mechanical.ClientConfig;
import com.mechanical.Mechanical;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (edge blending, client.EdgeBlend): with {@code blendshots.flag} in the game directory the client opens (or
 * creates) a world and runs a script. Line 1: output folder; line 2: {@code flat|normal [seed=N] [world=Name] [fresh]};
 * line 3: file prefix; then one step per line: {@code cmd <command>}, {@code blend on|off} (rebuilds the chunks),
 * {@code road x1 z1 x2 z2 path|trade} (plans and lays a tribe road over the terrain, like RoadShots),
 * {@code rset dx dz block} (the ground block there), {@code tp x y z yaw pitch}, {@code tpg x dy z yaw pitch} (dy above the ground), {@code wait ticks}, {@code shot name} (waits for every chunk to render), {@code bench name}
 * (rebuilds all chunks three times and logs how long each took), {@code surface x z}, {@code origin <biome>} (x z of later steps count from the nearest spot inside that biome). Noon, clear, no HUD; writes
 * {@code <prefix>_result.txt} and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BlendShots {
    private static int stage = -1, ticks, step, benchRound;
    private static volatile int ox, oz;
    private static long benchStart;
    private static final long[] benchMs = new long[3], benchOurs = new long[3], benchCompile = new long[3], benchSections = new long[3];
    private static int benchBefore;
    private static boolean benchDipped;
    private static File out;
    private static String prefix, world = "BlendShots";
    private static boolean flat;
    private static long seed;
    private static final List<String[]> steps = new ArrayList<>();
    private static final List<String> result = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "blendshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                String[] w = lines.get(1).trim().split("\\s+");
                flat = w[0].equals("flat");
                for (String t : w) {
                    if (t.startsWith("seed=")) seed = Long.parseLong(t.substring(5));
                    if (t.startsWith("world=")) world = t.substring(6);
                }
                if (List.of(w).contains("fresh")) {
                    File dir = new File(mc.gameDirectory, "saves/" + world);
                    if (dir.exists()) try (var walk = Files.walk(dir.toPath())) {
                        walk.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
                    }
                }
                prefix = lines.get(2).trim();
                for (String l : lines.subList(3, lines.size())) if (!l.isBlank() && !l.startsWith("#")) steps.add(l.trim().split("\\s+", 2));
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.options.renderDistance().set(12);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    if (new File(mc.gameDirectory, "saves/" + world + "/level.dat").exists()) mc.createWorldOpenFlows().openWorld(world, () -> {});
                    else mc.createWorldOpenFlows().createFreshLevel(world,
                            new LevelSettings(world, GameType.SPECTATOR, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(seed, !flat, false),
                            reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(flat ? WorldPresets.FLAT : WorldPresets.NORMAL).value().createWorldDimensions(),
                            new TitleScreen());
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            if (stage == 1) {
                mc.options.pauseOnLostFocus = false;
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks == 2) mc.getWindow().setWindowed(1600, 900);
                com.mechanical.client.EdgeBlend.profile = true;
                if (ticks < 60) return;
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    cmd("gamemode spectator @a");
                    cmd("time set noon");
                    cmd("gamerule advance_time false");
                    cmd("gamerule advance_weather false");
                    cmd("gamerule spawn_mobs false");
                    cmd("weather clear");
                });
                stage = 2;
                ticks = 0;
                return;
            }
            if (step >= steps.size()) {
                Files.write(new File(out, prefix + "_result.txt").toPath(), result);
                stage = -2;
                mc.stop();
                return;
            }
            String[] s = steps.get(step);
            String arg = s.length > 1 ? s[1] : "";
            boolean rendered = mc.levelRenderer.hasRenderedAllSections();
            switch (s[0]) {
                case "cmd" -> {
                    server.execute(() -> cmd(arg));
                    next();
                }
                case "blend" -> {
                    ClientConfig.blendEdges = arg.equals("on");
                    mc.levelExtractor.allChanged();
                    next();
                }
                case "road" -> {
                    String[] a = arg.split("\\s+");
                    server.execute(() -> road(ox + Integer.parseInt(a[0]), oz + Integer.parseInt(a[1]), ox + Integer.parseInt(a[2]), oz + Integer.parseInt(a[3]),
                            a[4].equals("trade") ? RoadBuilding.TRADE : RoadBuilding.PATH));
                    next();
                }
                case "tp" -> {
                    String[] a = arg.split("\\s+");
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), ox + Double.parseDouble(a[0]), Double.parseDouble(a[1]), oz + Double.parseDouble(a[2]), java.util.Set.of(),
                                Float.parseFloat(a[3]), Float.parseFloat(a[4]), true);
                    });
                    next();
                }
                case "tpg" -> { // like tp, y counted from the ground at x z
                    String[] a = arg.split("\\s+");
                    server.execute(() -> {
                        ServerPlayer p = player();
                        double x = ox + Double.parseDouble(a[0]), z = oz + Double.parseDouble(a[2]);
                        double y = surface(p.level(), (int) Math.floor(x), (int) Math.floor(z)).getY() + 1 + Double.parseDouble(a[1]);
                        p.teleportTo(p.level(), x, y, z, java.util.Set.of(), Float.parseFloat(a[3]), Float.parseFloat(a[4]), true);
                    });
                    next();
                }
                case "origin" -> { // later x z count from the nearest spot with this biome all round (80 x 80)
                    server.execute(() -> origin(arg.trim()));
                    next();
                }
                case "rset" -> { // rset dx dz block: replaces the ground block there, clears what stands on it
                    String[] a = arg.split("\\s+");
                    server.execute(() -> {
                        BlockPos g = surface(player().level(), ox + Integer.parseInt(a[0]), oz + Integer.parseInt(a[1]));
                        cmd("setblock " + g.getX() + " " + (g.getY() + 1) + " " + g.getZ() + " air");
                        cmd("setblock " + g.getX() + " " + g.getY() + " " + g.getZ() + " " + a[2]);
                    });
                    next();
                }
                case "wait" -> {
                    if (ticks >= Integer.parseInt(arg)) next();
                }
                case "surface" -> {
                    String[] a = arg.split("\\s+");
                    server.execute(() -> {
                        int x = ox + Integer.parseInt(a[0]), z = oz + Integer.parseInt(a[1]);
                        ServerLevel level = player().level();
                        level.getChunk(x >> 4, z >> 4);
                        result.add("surface " + x + " " + z + ": y " + (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1) + " "
                                + level.getBlockState(new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z)));
                    });
                    next();
                }
                case "shot" -> {
                    if (ticks < 60 || ticks < 1200 && !rendered) return;
                    if (ticks < 1400 && ticks % 20 != 0) return;
                    String name = prefix + "_" + arg;
                    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                        try (image) {
                            image.writeToFile(new File(out, name + ".png"));
                        } catch (Exception e) {
                            result.add("screenshot " + name + " failed: " + e);
                        }
                    });
                    result.add("shot " + name + (rendered ? "" : " (not all chunks rendered)"));
                    next();
                }
                case "bench" -> { // from a full rebuild to every section compiled again (the count dips, then comes back)
                    int count = mc.levelExtractor.countRenderedSections();
                    if (ticks == 1) {
                        if (!rendered || count < 50) {
                            ticks = 0; // wait until the world is settled
                            return;
                        }
                        benchBefore = count;
                        benchDipped = false;
                        com.mechanical.client.EdgeBlend.NANOS.reset();
                        com.mechanical.client.EdgeBlend.COMPILE_NANOS.reset();
                        com.mechanical.client.EdgeBlend.COMPILES.reset();
                        mc.levelExtractor.allChanged();
                        benchStart = System.nanoTime();
                        return;
                    }
                    if (!benchDipped) {
                        benchDipped = count < benchBefore / 2;
                        if (ticks < 200) return;
                    }
                    if ((count < benchBefore * 0.98 || !rendered) && ticks < 1200) return;
                    benchMs[benchRound] = (System.nanoTime() - benchStart) / 1_000_000;
                    benchOurs[benchRound] = com.mechanical.client.EdgeBlend.NANOS.sum() / 1_000_000;
                    benchCompile[benchRound] = com.mechanical.client.EdgeBlend.COMPILE_NANOS.sum() / 1_000_000;
                    benchSections[benchRound++] = com.mechanical.client.EdgeBlend.COMPILES.sum();
                    ticks = 0;
                    if (benchRound == benchMs.length) {
                        result.add("bench " + arg + " (blend " + (ClientConfig.blendEdges ? "on" : "off") + ", " + benchBefore + " sections): " + benchMs[0] + " / " + benchMs[1]
                                + " / " + benchMs[2] + " ms to rebuild every section; blend code " + benchOurs[0] + " / " + benchOurs[1] + " / " + benchOurs[2]
                                + " ms CPU; all section meshing " + benchCompile[0] + " / " + benchCompile[1] + " / " + benchCompile[2] + " ms CPU for "
                                + benchSections[0] + " / " + benchSections[1] + " / " + benchSections[2] + " section meshes");
                        benchRound = 0;
                        next();
                    }
                }
                default -> {
                    result.add("unknown step " + s[0]);
                    next();
                }
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, prefix + "_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void next() {
        step++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void road(int x1, int z1, int x2, int z2, int kind) {
        ServerLevel level = player().level();
        BlockPos from = surface(level, x1, z1), to = surface(level, x2, z2);
        List<BlockPos> path = RoadBuilding.plan(level, from, to, Math.min(x1, x2) - 24, Math.min(z1, z2) - 24, Math.max(x1, x2) + 24, Math.max(z1, z2) + 24);
        if (path == null) {
            result.add("no route " + from + " -> " + to);
            return;
        }
        RoadBuilding.Road r = new RoadBuilding.Road(UUID.randomUUID(), UUID.randomUUID(), from, to, kind, List.of(), 0, true, List.of());
        RoadBuilding.append(r, path);
        for (int i = 0; i < r.cells.size(); i++) RoadBuilding.buildCell(level, r, i);
        result.add("road " + kind + ": " + r.cells.size() + " cells, " + r.cell(0) + " -> " + r.cell(r.cells.size() - 1));
    }

    private static void origin(String biome) {
        ServerLevel level = player().level();
        if (biome.equals("trail")) {
            trail(level);
            return;
        }
        if (biome.equals("flat")) {
            flat(level);
            return;
        }
        var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME, net.minecraft.resources.Identifier.parse(biome));
        int best = Integer.MAX_VALUE;
        for (int x = -3008; x <= 3008; x += 64) {
            for (int z = -3008; z <= 3008; z += 64) {
                int d = x * x + z * z;
                if (d >= best) continue;
                boolean all = true;
                for (int i = -1; i <= 1 && all; i++)
                    for (int j = -1; j <= 1 && all; j++) all = level.getUncachedNoiseBiome((x + 40 * i) >> 2, 16, (z + 40 * j) >> 2).is(key);
                if (all) {
                    best = d;
                    ox = x;
                    oz = z;
                }
            }
        }
        result.add("origin " + biome + ": " + (best == Integer.MAX_VALUE ? "not found" : ox + " " + oz));
    }

    /** The trodden path block with the most trail round it (9 x 9) near the closest trail-connected structure. */
    private static void trail(ServerLevel level) {
        var tag = net.minecraft.tags.TagKey.create(Registries.STRUCTURE, net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "trail_connected"));
        BlockPos s = level.findNearestMapStructure(tag, new BlockPos(ox, 64, oz), 100, false); // from the last origin
        if (s == null) {
            result.add("origin trail: no structure");
            return;
        }
        int r = 112, n = 2 * r + 1;
        int[][] trail = new int[n][n];
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos g = surface(level, s.getX() + dx, s.getZ() + dz);
                net.minecraft.world.level.block.state.BlockState b = level.getBlockState(g);
                trail[dx + r][dz + r] = level.getBlockState(g.above()).is(net.minecraft.world.level.block.Blocks.SNOW) ? -9
                        : b.getBlock() instanceof com.mechanical.TrampledPathBlock ? 2
                        : b.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT) || b.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                        || b.is(net.minecraft.world.level.block.Blocks.PACKED_MUD) ? 3 : 0;
            }
        }
        int best = -1;
        for (int x = 4; x < n - 4; x++) {
            for (int z = 4; z < n - 4; z++) {
                if (trail[x][z] <= 0 || Math.abs(x - r) < 24 && Math.abs(z - r) < 24) continue; // away from the structure itself
                int score = 0;
                for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) score += trail[x + i][z + j];
                if (score > best) {
                    best = score;
                    ox = s.getX() + x - r;
                    oz = s.getZ() + z - r;
                }
            }
        }
        result.add("origin trail near " + s + ": " + (best < 0 ? "none" : ox + " " + oz + " score " + best));
    }

    /** Nearest dry, flat 56 x 56 (from the noise, no chunks generated) to the last origin, in plains, meadow or forest. */
    private static void flat(ServerLevel level) {
        var gen = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int bx = ox, bz = oz;
        for (int ring = 0; ring <= 40; ring++) {
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
                    int cx = bx + i * 48, cz = bz + j * 48;
                    var b = level.getUncachedNoiseBiome(cx >> 2, 16, cz >> 2);
                    if (!(b.is(net.minecraft.world.level.biome.Biomes.PLAINS) || b.is(net.minecraft.world.level.biome.Biomes.MEADOW)
                            || b.is(net.minecraft.world.level.biome.Biomes.FOREST))) continue;
                    int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                    for (int dx = -28; dx <= 28; dx += 7) {
                        for (int dz = -28; dz <= 28; dz += 7) {
                            int h = gen.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                            min = Math.min(min, h);
                            max = Math.max(max, h);
                        }
                    }
                    if (min > level.getSeaLevel() + 1 && max - min <= 2) {
                        ox = cx;
                        oz = cz;
                        result.add("origin flat: " + ox + " " + oz + " heights " + min + ".." + max + " " + b.getRegisteredName());
                        return;
                    }
                }
            }
        }
        result.add("origin flat: none");
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
    }

    private BlendShots() {}
}
