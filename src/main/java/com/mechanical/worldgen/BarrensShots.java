package com.mechanical.worldgen;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like ElkShots): with a file {@code barrensshots.flag} in the game directory (line 1: output folder, line 2:
 * the world to open, then one view per line: {@code name x y z yaw pitch}, where y may be {@code g+1.7}, that far above
 * the ground at x z), the client opens the world and screenshots each view (barrens_<name>.png), then quits. Does
 * nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BarrensShots {
    private record View(String name, double x, String y, double z, float yaw, float pitch) {}

    private static int stage = -1, ticks, view;
    private static File out;
    private static String world;
    private static final List<View> views = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "barrensshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                world = lines.get(1).trim();
                for (String l : lines.subList(2, lines.size())) {
                    String[] v = l.trim().split("\\s+");
                    if (v.length == 6) views.add(new View(v[0], Double.parseDouble(v[1]), v[2], Double.parseDouble(v[3]), Float.parseFloat(v[4]), Float.parseFloat(v[5])));
                }
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld(world, () -> {});
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
                if (ticks < 100) return;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.getWindow().setWindowed(1600, 900);
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                server.execute(() -> {
                    cmd("gamemode spectator @a");
                    cmd("time set 6000");
                    cmd("weather clear");
                    cmd("gamerule advance_time false");
                    cmd("gamerule advance_weather false");
                    go(views.get(0));
                });
                stage = 2;
                ticks = 0;
                return;
            }
            // stage 2: wait for the chunks round the view, shoot, move on
            if (ticks < (view == 0 ? 400 : 200)) return;
            shot(mc, views.get(view).name());
            view++;
            ticks = 0;
            if (view < views.size()) {
                View v = views.get(view);
                server.execute(() -> go(v));
            } else {
                stage = -2;
                mc.stop();
            }
        } catch (Exception e) {
            Mechanical.LOGGER.error("BarrensShots", e);
            stage = -2;
            mc.stop();
        }
    }

    private static void go(View v) {
        ServerPlayer p = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
        ServerLevel level = p.level();
        double y;
        if (v.y().startsWith("g")) {
            level.getChunk((int) Math.floor(v.x()) >> 4, (int) Math.floor(v.z()) >> 4);
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(v.x()), (int) Math.floor(v.z())) + Double.parseDouble(v.y().substring(1));
        } else y = Double.parseDouble(v.y());
        p.teleportTo(level, v.x(), y, v.z(), java.util.Set.of(), v.yaw(), v.pitch(), true);
    }

    private static void cmd(String c) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "barrens_" + name + ".png"));
            } catch (Exception e) {
                Mechanical.LOGGER.error("BarrensShots " + name, e);
            }
        });
    }

    private BarrensShots() {}
}
