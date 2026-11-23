package com.mechanical.people;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.mechanical.Mechanical;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like com.mechanical.TerrainShots): with {@code roadshots.flag} in the game directory the client builds tribe
 * roads (RoadBuilding) on real terrain and photographs them. Line 1: output folder; line 2: seed (plus {@code fresh} to
 * recreate the world "RoadShots"); then one site per line: {@code name biome} (e.g. {@code plains minecraft:plains}). At
 * each site, the flattest spot near the closest such biome gets a path and, 8 blocks beside it, a trade road, 36 blocks
 * long; shots {@code road_<name>_eye.png} (standing on the trade road), {@code _high} (15 blocks up) and {@code _path}.
 * Writes road_result.txt and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class RoadShots {
    private static final String WORLD = "RoadShots";
    private static int stage = -1, ticks, site, shotNo;
    private static File out;
    private static long seed;
    private static final List<String[]> sites = new ArrayList<>();
    private static final List<String> result = new ArrayList<>();
    /** Camera spots of the current site: x, y, z, yaw, pitch. */
    private static final List<double[]> cams = new ArrayList<>();
    private static volatile boolean built, found;
    private static boolean laying;
    private static int bx, bz, range;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "roadshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                String[] s = lines.get(1).trim().split("\\s+");
                seed = Long.parseLong(s[0]);
                if (List.of(s).contains("fresh")) {
                    File dir = new File(mc.gameDirectory, "saves/" + WORLD);
                    if (dir.exists()) try (var walk = Files.walk(dir.toPath())) {
                        walk.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
                    }
                }
                for (String l : lines.subList(2, lines.size())) if (!l.isBlank()) sites.add(l.trim().split("\\s+"));
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.options.renderDistance().set(12);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").exists()) mc.createWorldOpenFlows().openWorld(WORLD, () -> {});
                    else mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.SPECTATOR, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(seed, true, false),
                            reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(),
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
            if (stage == 2) { // build the next site
                if (site >= sites.size()) {
                    Files.write(new File(out, "road_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                    return;
                }
                if (ticks == 1) {
                    built = false;
                    found = false;
                    String[] s = sites.get(site);
                    server.execute(() -> {
                        try {
                            build(s);
                        } catch (Exception e) {
                            result.add(s[0] + ": error " + e);
                        }
                        built = true;
                    });
                }
                if (!built) return;
                if (found && !laying) { // lay the roads once the chunks' snow catch-up (Thaw) is done
                    laying = true;
                    ticks = 0;
                    return;
                }
                if (laying && ticks == 300) {
                    built = false;
                    String[] s = sites.get(site);
                    server.execute(() -> {
                        try {
                            layRoads(s);
                        } catch (Exception e) {
                            result.add(s[0] + ": error " + e);
                        }
                        built = true;
                    });
                }
                if (laying && (ticks <= 300 || !built)) return;
                laying = false;
                if (cams.isEmpty()) site++;
                else {
                    stage = 3;
                    shotNo = 0;
                }
                ticks = 0;
                return;
            }
            if (stage == 3) {
                if (shotNo >= cams.size()) {
                    cams.clear();
                    site++;
                    stage = 2;
                    ticks = 0;
                    return;
                }
                double[] c = cams.get(shotNo);
                if (ticks == 1) server.execute(() -> {
                    ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                    p.teleportTo(p.level(), c[0], c[1], c[2], java.util.Set.of(), (float) c[3], (float) c[4], true);
                });
                if (ticks < 200 || ticks < 1200 && !mc.levelRenderer.hasRenderedAllSections()) return;
                if (ticks < 1400 && ticks % 100 != 0) return;
                shot(mc, sites.get(site)[0] + "_" + (shotNo == 0 ? "eye" : shotNo == 1 ? "high" : shotNo == 2 ? "path" : "close"));
                shotNo++;
                ticks = 0;
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "road_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    /** Server: find the biome and the flattest spot near it, load it and go there. */
    private static void build(String[] s) {
        ServerPlayer p = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
        ServerLevel level = p.level();
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(s[1]));
        var hit = level.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 64, 0), 6400, 32, 64);
        if (hit == null) {
            result.add(s[0] + ": no " + s[1]);
            return;
        }
        BlockPos f = hit.getFirst();
        int bestX = f.getX(), bestZ = f.getZ(), bestRange = Integer.MAX_VALUE;
        for (int dx = -128; dx <= 128; dx += 16)
            for (int dz = -128; dz <= 128; dz += 16) {
                int cx = f.getX() + dx, cz = f.getZ() + dz;
                level.getChunk(cx >> 4, cz >> 4);
                if (!level.getBiome(new BlockPos(cx, level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz), cz)).is(key)) continue;
                int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, bad = 0;
                for (int x = -18; x <= 18; x += 3)
                    for (int z = -6; z <= 6; z += 3) {
                        level.getChunk((cx + x) >> 4, (cz + z) >> 4);
                        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(cx + x, 0, cz + z)).below();
                        if (!level.getFluidState(g).isEmpty() || !RoadBuilding.natural(level.getBlockState(g))) bad++;
                        lo = Math.min(lo, g.getY());
                        hi = Math.max(hi, g.getY());
                    }
                int range = hi - lo + bad * 3;
                if (range < bestRange) {
                    bestRange = range;
                    bestX = cx;
                    bestZ = cz;
                }
            }
        for (int x = bestX - 40; x <= bestX + 40; x += 16)
            for (int z = bestZ - 40; z <= bestZ + 40; z += 16) level.getChunk(x >> 4, z >> 4);
        p.teleportTo(level, bestX + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, bestX, bestZ) + 20, bestZ + 0.5, java.util.Set.of(), 0, 90, true);
        bx = bestX;
        bz = bestZ;
        range = bestRange;
        found = true;
    }

    /** Server, a while after {@link #build}: lay both roads and note the camera spots. */
    private static void layRoads(String[] s) {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0).level();
        int bestX = bx, bestZ = bz, bestRange = range;
        cmd("kill @e[type=!player,x=" + bestX + ",y=100,z=" + bestZ + ",distance=..90]");
        BlockPos pathEnd = road(level, s[0], bestX, bestZ - 4, RoadBuilding.PATH);
        BlockPos tradeStart = road(level, s[0], bestX, bestZ + 4, RoadBuilding.TRADE);
        if (tradeStart == null) return;
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bestX, bestZ + 4);
        cams.add(new double[] {tradeStart.getX() + 0.5, open(level, tradeStart.getX(), tradeStart.getY() + 1.1, tradeStart.getZ()), tradeStart.getZ() + 0.5, -90, 12}); // eye, looking east along it
        cams.add(new double[] {bestX - 10.5, Math.max(gy + 15, level.getHeight(Heightmap.Types.MOTION_BLOCKING, bestX - 11, bestZ + 16) + 3), bestZ + 16.5, -140, 40}); // 15 up, from the south-west
        if (pathEnd != null) cams.add(new double[] {pathEnd.getX() + 0.5, open(level, pathEnd.getX(), pathEnd.getY() + 1.1, pathEnd.getZ()), pathEnd.getZ() + 0.5, 90, 12}); // on the path, looking west
        int cy = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bestX - 8, bestZ + 4), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bestX - 12, bestZ + 4));
        cams.add(new double[] {bestX - 11.5, open(level, bestX - 12, cy + 2.2, bestZ + 4), bestZ + 4.5, -90, 38}); // close up, looking down the trade road
        result.add(s[0] + ": " + s[1] + " at " + bestX + " " + bestZ + " (height range + bad " + bestRange + ")");
    }

    /** Plans and lays a road from x-18 to x+18 along row z; its first cell for a TRADE road, its last for a path (null if no route). */
    private static BlockPos road(ServerLevel level, String name, int x, int z, int kind) {
        BlockPos from = new BlockPos(x - 18, 0, z), to = new BlockPos(x + 18, 0, z);
        List<BlockPos> path = RoadBuilding.plan(level, from, to, x - 22, z - 10, x + 22, z + 10);
        if (path == null || path.size() < 2) {
            result.add(name + ": no route for kind " + kind);
            return null;
        }
        RoadBuilding.Road r = new RoadBuilding.Road(UUID.randomUUID(), UUID.randomUUID(), path.getFirst(), path.getLast(), kind, List.of(), 0, true, List.of());
        RoadBuilding.append(r, path);
        for (int i = 0; i < r.cells.size(); i++) RoadBuilding.buildCell(level, r, i);
        result.add(name + " kind " + kind + ": " + r.cells.size() + " cells, from " + r.cell(0) + " to " + r.cell(r.cells.size() - 1));
        return kind == RoadBuilding.TRADE ? r.cell(0) : r.cell(r.cells.size() - 1);
    }

    /** The lowest camera height from {@code y} up where the camera and the block below its eye are not inside a block (a log, leaves). */
    private static double open(ServerLevel level, int x, double y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, (int) Math.floor(y), z);
        for (int i = 0; i < 12; i++) {
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) break;
            p.move(0, 1, 0);
            y += 1;
        }
        return y;
    }

    private static void cmd(String c) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "road_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private RoadShots() {}
}
