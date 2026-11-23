package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (terrain plan phase 2): with {@code smoothshots.flag} in the game directory the client opens a saved world and
 * photographs fixed views. Flag lines: output folder, world folder name, file prefix, then one view per line:
 * {@code name x y z yaw pitch} (camera position). Spectator at noon, clear weather; writes PREFIX_result.txt and quits.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SmoothShots {
    private static int stage = -1, ticks, view;
    private static File out;
    private static String world, prefix;
    private static final List<String[]> VIEWS = new ArrayList<>();
    private static final List<String> result = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "smoothshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                world = lines.get(1).trim();
                prefix = lines.get(2).trim();
                for (String l : lines.subList(3, lines.size())) if (!l.isBlank()) VIEWS.add(l.trim().split("\\s+"));
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
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    cmd("gamemode spectator @a");
                    cmd("time set noon");
                    cmd("gamerule advance_time false");
                    cmd("gamerule spawn_mobs false");
                    cmd("weather clear");
                });
                stage = 2;
                ticks = 0;
                return;
            }
            if (view >= VIEWS.size()) {
                if (ticks < 20) return;
                Files.write(new File(out, prefix + "_result.txt").toPath(), result);
                stage = -2;
                mc.stop();
                return;
            }
            String[] v = VIEWS.get(view);
            if (ticks == 1) server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(p.level(), Double.parseDouble(v[1]), Double.parseDouble(v[2]), Double.parseDouble(v[3]), java.util.Set.of(),
                        Float.parseFloat(v[4]), Float.parseFloat(v[5]), true);
            });
            if (ticks < 160) return; // chunks load and mesh
            String name = prefix + "_" + v[0];
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                try (image) {
                    image.writeToFile(new File(out, name + ".png"));
                } catch (Exception e) {
                    result.add("screenshot " + name + " failed: " + e);
                }
            });
            result.add("shot " + name);
            view++;
            ticks = 0;
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, prefix + "_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void cmd(String c) {
        ServerPlayer p = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private SmoothShots() {}
}
