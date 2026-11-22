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
 * Dev only: with {@code desertshots.flag} in the game directory (first line: output folder) the client opens the world
 * "DesertShots", builds a sandstone platform high in the sky at noon and photographs each desert animal on it (frozen,
 * no AI) as desert_*.png, plus a sand viper buried in sand; writes desert_result.txt and quits. Does nothing without
 * the flag file. (After SavannaShots.)
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DesertShots {
    /** name, summon command arguments (entity + NBT), camera distance, height of the body's middle above the ground. */
    private static final String[][] SUBJECTS = {
            {"desert_scorpion", "mechanical:desert_scorpion {NoAI:1b}", "2.0", "0.25"},
            {"fennec_fox", "mechanical:fennec_fox {NoAI:1b}", "2.4", "0.35"},
            {"wild_camel", "mechanical:wild_camel {NoAI:1b}", "6.5", "1.2"},
            {"wild_camel_calf", "mechanical:wild_camel {NoAI:1b,Age:-24000}", "4.0", "0.7"},
            {"tame_camel_packed", "mechanical:tame_camel {NoAI:1b,Tame:1b,ChestedHorse:1b}", "6.5", "1.2"},
            {"sand_viper", "mechanical:sand_viper {NoAI:1b}", "2.0", "0.15"},
            {"rattlesnake", "mechanical:rattlesnake {NoAI:1b}", "2.2", "0.15"},
            {"puma", "mechanical:puma {NoAI:1b}", "3.8", "0.6"},
            {"bighorn_ram", "mechanical:bighorn_sheep {NoAI:1b,male:1b}", "3.8", "0.8"},
            {"bighorn_ewe", "mechanical:bighorn_sheep {NoAI:1b,male:0b}", "3.8", "0.8"},
            {"bighorn_lamb", "mechanical:bighorn_sheep {NoAI:1b,Age:-24000}", "2.6", "0.5"},
            {"condor", "mechanical:condor {NoAI:1b}", "4.2", "0.7"},
    };
    private static int stage = -1, ticks, x, z, subject;
    private static final int Y = 200;
    private static File out;
    private static final List<String> result = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "desertshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("DesertShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> {
                    mc.options.pauseOnLostFocus = false;
                    if (mc.player.isDeadOrDying()) mc.player.respawn();
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); // no hotbar in the pictures
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        ServerPlayer p = player();
                        x = (int) Math.floor(p.getX());
                        z = (int) Math.floor(p.getZ());
                        cmd("gamemode creative @a");
                        cmd("time set noon");
                        cmd("gamerule advance_time false");
                        cmd("gamerule spawn_mobs false");
                        cmd("weather clear");
                        cmd(fill(-12, Y - 1, -12, 12, Y - 1, 12, "smooth_sandstone"));
                        cmd(fill(-12, Y, -12, 12, Y + 8, 12, "air"));
                        look(0.5, Y, -4.0, 0, 15);
                    });
                    next();
                }
                case 2 -> { // one subject at a time: clear the platform, summon, frame it side-on from the south-west
                    // the last subject dies into a carcass (and a puff): clear twice, a few ticks apart, before the next comes
                    if (ticks == 1) server.execute(DesertShots::clear);
                    if (ticks == 5) server.execute(DesertShots::clear);
                    if (ticks == 30) {
                        String[] s = SUBJECTS[subject];
                        double d = Double.parseDouble(s[2]), centre = Double.parseDouble(s[3]);
                        double dist = Math.hypot(d * 0.35, d);
                        float pitch = (float) Math.toDegrees(Math.atan2(1.62 - centre, dist)); // eyes down to the body's middle
                        server.execute(() -> {
                            cmd("summon " + s[1].replace(" {", " " + (x + 0.5) + " " + Y + " " + (z + 0.5) + " {"));
                            cmd("tp @e[type=!player,x=" + x + ",y=" + Y + ",z=" + z + ",distance=..3] " + (x + 0.5) + " " + Y + " " + (z + 0.5) + " 90 0");
                            look(0.5 - d * 0.35, Y, 0.5 + d, 199.3F, pitch); // from the south-south-west, facing its flank
                        });
                    }
                    if (ticks < 70) return;
                    shot(mc, SUBJECTS[subject][0]);
                    result.add("shot " + SUBJECTS[subject][0]);
                    if (++subject < SUBJECTS.length) ticks = 0;
                    else next();
                }
                case 3 -> { // a sand viper on real sand, AI on: it buries itself
                    if (ticks == 1) server.execute(() -> {
                        clear();
                        cmd(fill(-3, Y - 2, -3, 3, Y - 2, 3, "sandstone")); // under the sand, so it doesn't fall
                        cmd(fill(-3, Y - 1, -3, 3, Y - 1, 3, "sand"));
                        cmd("summon mechanical:sand_viper " + (x + 0.5) + " " + Y + " " + (z + 0.5) + " {Rotation:[90f,0f]}");
                        look(0.5 - 0.7, Y, 0.5 + 2.2, 199.3F, 30);
                    });
                    if (ticks < 600) return;
                    next();
                }
                case 4 -> {
                    if (ticks < 5) return;
                    shot(mc, "sand_viper_buried");
                    next();
                }
                case 5 -> {
                    if (ticks < 5) return;
                    next();
                }
                case 6 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "desert_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "desert_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    /** Everything but the player off the platform, into the void below the world (carcasses don't die of /kill). */
    private static void clear() {
        cmd("tp @e[type=!player,x=" + x + ",y=" + Y + ",z=" + z + ",distance=..30] " + x + " -200 " + z);
    }

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void look(double dx, double py, double dz, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), x + dx, py, z + dz, java.util.Set.of(), yaw, pitch, true);
    }

    private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String what) {
        return "fill " + (x + x1) + " " + y1 + " " + (z + z1) + " " + (x + x2) + " " + y2 + " " + (z + z2) + " " + what;
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "desert_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private DesertShots() {}
}
