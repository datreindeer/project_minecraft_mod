package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.client.CaveDarkness;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with {@code caveshots.flag} in the game directory (first line: output folder) the client opens the world
 * "CaveShots", sets Brightness to max, builds a closed stone room deep underground with a torch, lava, glow lichen and an
 * amethyst cluster, and takes caves_*.png: the room with the effect off (vanilla) and on, halfway through the fade, with a
 * held torch, with night vision, then the surface at midnight with the effect on and off; writes caves_result.txt and quits.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class CaveShots {
    private static int stage = -1, ticks, x, z, top;
    private static File out;
    private static final List<String> result = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "caveshots.flag");
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
                    mc.createWorldOpenFlows().openWorld("CaveShots", () -> {});
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
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    mc.options.gamma().set(1.0);
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); // no hotbar in the pictures
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        ServerPlayer p = player();
                        x = (int) Math.floor(p.getX());
                        z = (int) Math.floor(p.getZ());
                        cmd("gamemode creative @a");
                        cmd("time set noon");
                        cmd("weather clear");
                        cmd("difficulty peaceful"); // no spiders in the dark room
                        top = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                        look(x + 0.5, top, z + 0.5, 180, 10);
                    });
                    next();
                }
                case 2 -> {
                    if (ticks < 40) return;
                    server.execute(() -> {
                        // a closed room, y -40..-34 inside, far below the surface: no sky light at all
                        cmd(fill(-10, -41, -10, 10, -33, 10, "stone hollow"));
                        cmd(set(-6, -40, -6, "torch"));
                        cmd(fill(4, -41, -7, 6, -41, -6, "lava"));
                        cmd(fill(9, -39, 2, 9, -37, 4, "glow_lichen[east=true]")); // a patch on the near east wall, away from the rest
                        cmd(set(0, -37, -9, "glow_lichen[north=true]"));
                        cmd(set(1, -37, -9, "glow_lichen[north=true]"));
                        cmd(set(3, -40, -8, "amethyst_cluster[facing=up]"));
                        look(x + 0.5, -40, z + 7.5, 180, 12);
                    });
                    CaveDarkness.harnessOff = true;
                    next();
                }
                case 3 -> {
                    if (ticks < 80) return;
                    shot(mc, "cave_vanilla_gamma_max");
                    for (int[] c : new int[][] {{6, -41, -6}, {9, -38, 3}, {0, -37, -9}, {3, -40, -8}})
                        result.add("block " + c[0] + " " + c[1] + " " + c[2] + ": " + mc.level.getBlockState(new net.minecraft.core.BlockPos(x + c[0], c[1], z + c[2])));
                    CaveDarkness.harnessOff = false;
                    next();
                }
                case 4 -> {
                    if (ticks == 20) shot(mc, "cave_fade_half");
                    if (ticks < 60) return;
                    shot(mc, "cave_dark_gamma_max");
                    server.execute(() -> player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH)));
                    next();
                }
                case 5 -> {
                    if (ticks < 20) return;
                    shot(mc, "cave_held_torch");
                    server.execute(() -> {
                        player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        player().addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2000, 0, false, false));
                    });
                    next();
                }
                case 6 -> {
                    if (ticks < 20) return;
                    shot(mc, "cave_night_vision");
                    server.execute(() -> {
                        player().removeEffect(MobEffects.NIGHT_VISION);
                        look(x + 4.5, -40, z + 3.5, -90, 5); // facing the lichen patch on the east wall
                    });
                    next();
                }
                case 7 -> {
                    if (ticks < 20) return;
                    result.add("block light by the lichen: " + mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new net.minecraft.core.BlockPos(x + 8, -38, z + 3)));
                    shot(mc, "cave_glow_lichen");
                    server.execute(() -> {
                        cmd("time set midnight");
                        look(x + 0.5, top, z + 0.5, 180, 10);
                    });
                    next();
                }
                case 8 -> {
                    if (ticks < 80) return;
                    result.add("surface: cave strength " + CaveDarkness.strength());
                    shot(mc, "surface_night");
                    CaveDarkness.harnessOff = true;
                    next();
                }
                case 9 -> {
                    if (ticks < 5) return;
                    shot(mc, "surface_night_off");
                    CaveDarkness.harnessOff = false;
                    next();
                }
                case 10 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "caves_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "caves_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void next() {
        stage++;
        ticks = 0;
        result.add("stage " + stage + ": cave strength " + CaveDarkness.strength());
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void look(double px, double py, double pz, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), px, py, pz, java.util.Set.of(), yaw, pitch, true);
    }

    private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String what) {
        return "fill " + (x + x1) + " " + y1 + " " + (z + z1) + " " + (x + x2) + " " + y2 + " " + (z + z2) + " " + what;
    }

    private static String set(int dx, int y, int dz, String what) {
        return "setblock " + (x + dx) + " " + y + " " + (z + dz) + " " + what;
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "caves_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private CaveShots() {}
}
