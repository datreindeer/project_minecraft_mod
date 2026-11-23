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
 * Dev only: with {@code savannashots.flag} in the game directory (first line: output folder) the client opens the world
 * "SavannaShots", builds a grass platform high in the sky at noon and photographs each savanna animal on it (frozen, no
 * AI) as savanna_*.png, plus a termite mound spire and a cave lion lying in the shade of acacia leaves; writes
 * savanna_result.txt and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SavannaShots {
    /** name, summon command arguments (entity + NBT), camera distance, height of the body's middle above the ground. */
    private static final String[][] SUBJECTS = {
            {"spotted_hyena", "mechanical:spotted_hyena {NoAI:1b}", "2.6", "0.6"},
            {"cheetah", "mechanical:cheetah {NoAI:1b}", "2.5", "0.6"},
            {"gazelle", "mechanical:gazelle {NoAI:1b}", "2.5", "0.7"},
            {"gazelle_fawn", "mechanical:gazelle {NoAI:1b,Age:-24000}", "1.8", "0.4"},
            {"ostrich_male", "mechanical:ostrich {NoAI:1b,male:1b}", "3.8", "1.1"},
            {"ostrich_female", "mechanical:ostrich {NoAI:1b,male:0b}", "3.8", "1.1"},
            {"ostrich_chick", "mechanical:ostrich {NoAI:1b,Age:-24000}", "1.8", "0.4"},
            {"aardvark", "mechanical:aardvark {NoAI:1b}", "2.2", "0.4"},
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
                File flag = new File(mc.gameDirectory, "savannashots.flag");
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
                    mc.createWorldOpenFlows().openWorld("SavannaShots", () -> {});
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
                        cmd(fill(-12, Y - 1, -12, 12, Y - 1, 12, "grass_block"));
                        cmd(fill(-12, Y, -12, 12, Y + 8, 12, "air"));
                        look(0.5, Y, -4.0, 0, 15);
                    });
                    next();
                }
                case 2 -> { // one subject at a time: clear the platform, summon, frame it side-on from the south-west
                    if (ticks == 1) {
                        String[] s = SUBJECTS[subject];
                        double d = Double.parseDouble(s[2]), centre = Double.parseDouble(s[3]);
                        double dist = Math.hypot(d * 0.35, d);
                        float pitch = (float) Math.toDegrees(Math.atan2(1.62 - centre, dist)); // eyes down to the body's middle
                        server.execute(() -> {
                            clear();
                            cmd("summon " + s[1].replace(" {", " " + (x + 0.5) + " " + Y + " " + (z + 0.5) + " {"));
                            cmd("tp @e[type=!player,x=" + x + ",y=" + Y + ",z=" + z + ",distance=..3] " + (x + 0.5) + " " + Y + " " + (z + 0.5) + " 90 0");
                            look(0.5 - d * 0.35, Y, 0.5 + d, 199.3F, pitch); // from the south-south-west, facing its flank
                        });
                    }
                    if (ticks < 40) return;
                    shot(mc, SUBJECTS[subject][0]);
                    result.add("shot " + SUBJECTS[subject][0]);
                    if (++subject < SUBJECTS.length) ticks = 0;
                    else next();
                }
                case 3 -> { // a termite mound spire, and a lion asleep in the shade of an acacia crown
                    if (ticks == 1) server.execute(() -> {
                        clear();
                        cmd(fill(-2, Y, 0, -2, Y + 2, 0, "mechanical:termite_mound"));
                        cmd(fill(1, Y, 0, 1, Y + 3, 0, "acacia_log"));
                        cmd(fill(-1, Y + 4, -2, 4, Y + 4, 2, "acacia_leaves[persistent=true]"));
                        cmd("summon mechanical:aardvark " + (x - 1.2) + " " + Y + " " + (z + 1.2) + " {NoAI:1b}");
                        cmd("summon minecraft:item " + (x - 2.5) + " " + Y + " " + (z + 1.5) + " {Item:{id:\"mechanical:termites\",count:1}}");
                        look(-0.5, Y + 1.6, 6.5, 170, 15);
                    });
                    if (ticks < 40) return;
                    shot(mc, "termite_mound");
                    next();
                }
                case 4 -> {
                    if (ticks == 1) server.execute(() -> {
                        clear();
                        cmd(fill(-2, Y, 0, -2, Y + 2, 0, "air"));
                        // a lion out in the noon sun, the camera far off (no player within 14 blocks): it walks into the shade
                        cmd("summon mechanical:cave_lion " + (x + 10.5) + " " + Y + " " + (z + 0.5));
                        look(2.5, Y, 20.5, 180, 5);
                    });
                    if (ticks < 500) return;
                    server.execute(() -> {
                        var lions = player().level().getEntitiesOfClass(com.mechanical.wildlife.CaveLion.class,
                                new net.minecraft.world.phys.AABB(x - 20, Y - 2, z - 20, x + 20, Y + 8, z + 20));
                        for (var l : lions) result.add("lion at " + (l.getX() - x) + ", " + (l.getZ() - z) + " shaded " + l.shaded(l.blockPosition()) + " dozing " + l.isDozing());
                        look(2.5, Y, 8.0, 180, 10);
                    });
                    next();
                }
                case 5 -> {
                    if (ticks < 15) return;
                    shot(mc, "lion_in_shade");
                    next();
                }
                case 6 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "savanna_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "savanna_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    /** Removes every animal, carcass and item on the platform (discarded: no death, no carcass left behind). */
    private static void clear() {
        player().level().getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(x - 30, Y - 4, z - 30, x + 30, Y + 20, z + 30),
                e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
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
                image.writeToFile(new File(out, "savanna_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private SavannaShots() {}
}
