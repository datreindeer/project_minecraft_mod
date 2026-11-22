package com.mechanical.worldgen;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.client.RuinFogClient;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like com.mechanical.LogShots): with a file {@code elkshots.flag} in the game directory (line 1: output folder,
 * line 2: "x z" in a Scorched Barrens, line 3: "x z" of an Elkwight ruin, optional line 4: the world, default "ElkShots"), the client opens that world and takes
 * screenshots (elk_*.png) of the biome and of the ruin from outside, inside in its fog, and inside once its boss is marked
 * dead; writes elk_result.txt and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ElkShots {
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static int bx, bz, rx, rz;
    private static BlockPos centre;
    private static String world;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "elkshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                var lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                String[] b = lines.get(1).trim().split("\\s+"), r = lines.get(2).trim().split("\\s+");
                bx = Integer.parseInt(b[0]);
                bz = Integer.parseInt(b[1]);
                rx = Integer.parseInt(r[0]);
                rz = Integer.parseInt(r[1]);
                world = lines.size() > 3 ? lines.get(3).trim() : "ElkShots";
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
            switch (stage) {
                case 1 -> {
                    mc.options.pauseOnLostFocus = false;
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        cmd("gamemode spectator @a");
                        cmd("time set 5000");
                        cmd("weather clear");
                        cmd("gamerule advance_time false");
                        cmd("gamerule advance_weather false");
                        look(bx + 0.5, ground(bx, bz) + 28, bz + 0.5, 30, 22);
                    });
                    next();
                }
                case 2 -> {
                    if (ticks < 300) return; // chunks round about generate and arrive
                    shot(mc, "biome_overview");
                    server.execute(() -> {
                        result.add("biome at " + bx + ", " + bz + ": " + player().level().getBiome(new BlockPos(bx, 80, bz)).getRegisteredName());
                        look(bx + 0.5, ground(bx, bz) + 1.7, bz + 0.5, 200, 5);
                    });
                    next();
                }
                case 3 -> {
                    if (ticks < 120) return;
                    shot(mc, "biome_ground");
                    server.execute(() -> look(rx - 40.5, ground(rx, rz) + 22, rz + 0.5, -90, 22)); // west of the ruin, outside its bounds
                    next();
                }
                case 4 -> {
                    if (ticks < 300) return;
                    server.execute(() -> {
                        ServerLevel level = player().level();
                        var ruin = ElkwightRuins.ruinAt(level, new BlockPos(rx, ground(rx, rz), rz));
                        result.add("ruin at " + rx + ", " + rz + ": " + ruin.map(BoundingBox::toString).orElse("none"));
                        BoundingBox box = ruin.orElse(null);
                        centre = box == null ? new BlockPos(rx, ground(rx, rz), rz) : new BlockPos(box.getCenter().getX(), box.minY() + ElkwightRuinStructure.DOWN, box.getCenter().getZ());
                    });
                    shot(mc, "ruin_outside");
                    next();
                }
                case 5 -> {
                    if (ticks < 5) return;
                    server.execute(() -> look(centre.getX() + 0.5, centre.getY() + 40, centre.getZ() + 0.5, 0, 90));
                    next();
                }
                case 6 -> {
                    if (ticks < 100) return;
                    shot(mc, "ruin_above");
                    server.execute(() -> look(centre.getX() - 13.5, centre.getY() + 3.5, centre.getZ() - 4.5, -70, 8));
                    next();
                }
                case 7 -> {
                    if (ticks < 80) return;
                    RuinFogClient.snap();
                    next();
                }
                case 8 -> {
                    if (ticks < 5) return;
                    shot(mc, "ruin_fog");
                    server.execute(() -> {
                        ServerLevel level = player().level();
                        result.add("state in the arena (1 = fog): " + ElkwightRuins.state(level, player().blockPosition()));
                        ElkwightRuins.ruinAt(level, centre).ifPresent(r -> ElkwightRuins.markDead(level, r));
                    });
                    next();
                }
                case 9 -> {
                    if (ticks < 60) return;
                    RuinFogClient.snap();
                    next();
                }
                case 10 -> {
                    if (ticks < 5) return;
                    shot(mc, "ruin_cleared");
                    server.execute(() -> result.add("state after the boss died (2 = cleared): " + ElkwightRuins.state(player().level(), player().blockPosition())));
                    next();
                }
                case 11 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "elk_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "elk_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    /** Server: loads the chunk and gives the ground height. */
    private static int ground(int x, int z) {
        ServerLevel level = player().level();
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static void look(double x, double y, double z, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), x, y, z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "elk_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private ElkShots() {}
}
