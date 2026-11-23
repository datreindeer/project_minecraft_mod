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
 * no AI) at 1280x720 through a 50 degree lens from three sides at its own eye height (spectator, so the camera can sit
 * low): desert_NAME.png three-quarter, _side and _front; plus the rattlesnake rattling (the player in survival near it),
 * the condor in flight and a sand viper buried in sand; writes desert_result.txt and quits. Does nothing without the flag.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DesertShots {
    /**
     * name, summon command arguments (entity + NBT), camera distance, camera height (about the animal's eye), height of
     * the body's middle (aimed at), and a special: "rattle" (the player in survival nearby so it rattles; camera at the
     * player's own eye height), "fly" (held in the air with its flight flag on, wings spread).
     */
    private static final String[][] SUBJECTS = {
            {"desert_scorpion", "mechanical:desert_scorpion {NoAI:1b}", "1.6", "0.4", "0.15", ""},
            {"fennec_fox", "mechanical:fennec_fox {NoAI:1b}", "1.5", "0.55", "0.35", ""},
            {"wild_camel", "mechanical:wild_camel {NoAI:1b}", "5.5", "2.2", "1.4", ""},
            {"wild_camel_calf", "mechanical:wild_camel {NoAI:1b,Age:-24000}", "3.0", "1.3", "0.8", ""},
            {"tame_camel_packed", "mechanical:tame_camel {NoAI:1b,Tame:1b,ChestedHorse:1b}", "5.5", "2.2", "1.4", ""},
            {"sand_viper", "mechanical:sand_viper {NoAI:1b}", "1.6", "0.35", "0.1", ""},
            {"rattlesnake", "mechanical:rattlesnake {NoAI:1b}", "2.0", "0.4", "0.1", ""},
            {"rattlesnake_rattling", "mechanical:rattlesnake {NoAI:1b}", "2.4", "1.62", "0.2", "rattle"},
            {"puma", "mechanical:puma {NoAI:1b}", "3.0", "0.9", "0.6", ""},
            {"bighorn_ram", "mechanical:bighorn_sheep {NoAI:1b,male:1b}", "2.6", "1.15", "0.8", ""},
            {"bighorn_ewe", "mechanical:bighorn_sheep {NoAI:1b,male:0b}", "2.6", "1.1", "0.75", ""},
            {"bighorn_lamb", "mechanical:bighorn_sheep {NoAI:1b,Age:-24000}", "1.8", "0.7", "0.45", ""},
            {"condor", "mechanical:condor {NoAI:1b}", "3.0", "1.3", "0.8", ""},
            {"condor_flying", "mechanical:condor {NoAI:1b,NoGravity:1b}", "4.5", "1.4", "2.9", "fly"},
    };
    /** The three views: yaw of the camera (the animal faces west, yaw 90), its offset from the animal per block of distance. */
    private static final String[] VIEWS = {"", "_side", "_front"};
    private static final double[][] VIEW_AT = {{-0.71, 0.71, -135}, {0, 1, 180}, {-1, 0, -90}};
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
                    mc.getWindow().setWindowed(1280, 720);
                    mc.options.fov().set(50); // a narrower lens: the animal fills the frame
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        ServerPlayer p = player();
                        x = (int) Math.floor(p.getX());
                        z = (int) Math.floor(p.getZ());
                        cmd("gamemode spectator @a"); // the camera may sit low, the feet in the platform
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
                case 2 -> { // one subject at a time, three views each: three-quarter, side-on, head-on
                    // the last subject dies into a carcass (and a puff): clear twice, a few ticks apart, before the next comes
                    String[] s = SUBJECTS[subject];
                    if (ticks == 1) server.execute(DesertShots::clear);
                    if (ticks == 5) server.execute(DesertShots::clear);
                    if (ticks == 30) server.execute(() -> {
                        boolean fly = s[5].equals("fly");
                        double y = Y + (fly ? 2.5 : 0);
                        cmd("gamemode " + (s[5].equals("rattle") ? "survival" : "spectator") + " @a");
                        cmd("summon " + s[1].replace(" {", " " + (x + 0.5) + " " + y + " " + (z + 0.5) + " {"));
                        cmd("tp @e[type=!player,x=" + x + ",y=" + Y + ",z=" + z + ",distance=..5] " + (x + 0.5) + " " + y + " " + (z + 0.5) + " 90 0");
                        if (fly) setFlying();
                    });
                    for (int v = 0; v < VIEWS.length; v++) {
                        int at = 40 + v * 20;
                        if (ticks == at) {
                            double d = Double.parseDouble(s[2]), eye = Double.parseDouble(s[3]), centre = Double.parseDouble(s[4]);
                            double[] view = VIEW_AT[v];
                            float pitch = (float) Math.toDegrees(Math.atan2(eye - centre, d));
                            boolean rattle = s[5].equals("rattle");
                            server.execute(() -> look(0.5 + view[0] * d, rattle ? Y : Y + eye - 1.62, 0.5 + view[1] * d, (float) view[2], pitch));
                        }
                        if (ticks == at + 15) {
                            String name = s[0] + VIEWS[v];
                            shot(mc, name);
                            result.add("shot " + name);
                        }
                    }
                    if (ticks < 40 + VIEWS.length * 20) return;
                    if (++subject < SUBJECTS.length) ticks = 0;
                    else {
                        server.execute(() -> cmd("gamemode spectator @a"));
                        next();
                    }
                }
                case 3 -> { // a sand viper on real sand, AI on: it buries itself
                    if (ticks == 1) server.execute(() -> {
                        clear();
                        cmd(fill(-3, Y - 2, -3, 3, Y - 2, 3, "sandstone")); // under the sand, so it doesn't fall
                        cmd(fill(-3, Y - 1, -3, 3, Y - 1, 3, "sand"));
                        cmd("summon mechanical:sand_viper " + (x + 0.5) + " " + Y + " " + (z + 0.5) + " {Rotation:[90f,0f]}");
                        look(0.5 - 1.0, Y - 1.2, 0.5 + 1.0, -135, 10);
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

    /** The condor held in flight for the picture: its synced flight flag set (NoAI, no gravity keep it there). */
    private static void setFlying() {
        try {
            var field = com.mechanical.wildlife.FlockBird.class.getDeclaredField("FLYING");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var key = (net.minecraft.network.syncher.EntityDataAccessor<Boolean>) field.get(null);
            for (var e : player().level().getEntitiesOfClass(com.mechanical.wildlife.FlockBird.class, player().getBoundingBox().inflate(12))) e.getEntityData().set(key, true);
        } catch (ReflectiveOperationException e) {
            result.add("fly: " + e);
        }
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
