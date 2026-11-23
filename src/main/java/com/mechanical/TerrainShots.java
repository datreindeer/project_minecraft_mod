package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
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
 * Dev only: with {@code terrainshots.flag} in the game directory the client photographs natural terrain. Line 1: output
 * folder; line 2: seed, plus {@code fresh} to delete and recreate the world and {@code world=Name} for another world than
 * "TerrainShots" (created with that seed if missing); line 3: file prefix; then one camera spot per line:
 * {@code name camX camZ targetX targetZ height [pitch]}, the camera {@code height} blocks above the ground (tree tops
 * count) at camX/camZ, or at y N for {@code =N}, looking at the ground at the target (or at the given pitch). Noon, clear
 * sky, no clouds or HUD, render distance 24; waits for the terrain to render, writes {@code <prefix>_<name>.png} and
 * {@code <prefix>_result.txt} and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TerrainShots {
    private static String world = "TerrainShots";
    private static int stage = -1, ticks, spot;
    private static File out;
    private static String prefix;
    private static long seed;
    private static final List<String[]> spots = new ArrayList<>();
    private static final List<String> result = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "terrainshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                String[] s = lines.get(1).trim().split("\\s+");
                seed = Long.parseLong(s[0]);
                for (String t : s) if (t.startsWith("world=")) world = t.substring(6);
                if (List.of(s).contains("fresh")) {
                    File dir = new File(mc.gameDirectory, "saves/" + world);
                    if (dir.exists()) try (var walk = Files.walk(dir.toPath())) {
                        walk.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
                    }
                }
                prefix = lines.get(2).trim();
                for (String l : lines.subList(3, lines.size())) if (!l.isBlank()) spots.add(l.trim().split("\\s+"));
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                // a fresh game directory opens on the accessibility onboarding screen first
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.options.renderDistance().set(24);
                    mc.options.simulationDistance().set(5);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    if (new File(mc.gameDirectory, "saves/" + world + "/level.dat").exists()) mc.createWorldOpenFlows().openWorld(world, () -> {});
                    else mc.createWorldOpenFlows().createFreshLevel(world,
                            new LevelSettings(world, GameType.SPECTATOR, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
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
            if (stage == 2) {
                if (spot >= spots.size()) {
                    Files.write(new File(out, prefix + "_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                    return;
                }
                String[] s = spots.get(spot);
                if (ticks == 1) server.execute(() -> {
                    ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                    ServerLevel level = p.level();
                    int cx = Integer.parseInt(s[1]), cz = Integer.parseInt(s[2]), tx = Integer.parseInt(s[3]), tz = Integer.parseInt(s[4]);
                    double cy = s[5].startsWith("=") ? Double.parseDouble(s[5].substring(1)) : ground(level, cx, cz, Heightmap.Types.MOTION_BLOCKING) + Double.parseDouble(s[5]),
                            ty = ground(level, tx, tz, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
                    double dx = tx - cx, dz = tz - cz, d = Math.sqrt(dx * dx + dz * dz);
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float pitch = s.length > 6 ? Float.parseFloat(s[6]) : (float) Math.toDegrees(Math.atan2(cy + 1.62 - ty, d));
                    p.teleportTo(level, cx + 0.5, cy, cz + 0.5, java.util.Set.of(), yaw, pitch, true);
                    result.add(s[0] + ": camera " + cx + " " + (int) cy + " " + cz + ", ground at target y " + (int) ty + ", yaw " + (int) yaw + " pitch "
                            + (int) pitch);
                });
                // wait for the chunks round the camera to generate and render
                if (ticks < 400 || ticks < 1600 && !mc.levelRenderer.hasRenderedAllSections()) return;
                if (ticks < 1800 && ticks % 200 != 0) return;
                shot(mc, s[0]);
                spot++;
                ticks = 0;
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

    /** The top of the given heightmap at x, z (generating the chunk): the camera stands on tree tops, the target is the ground. */
    private static int ground(ServerLevel level, int x, int z, Heightmap.Types type) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(type, x, z);
    }

    private static void cmd(String c) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, prefix + "_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private TerrainShots() {}
}
