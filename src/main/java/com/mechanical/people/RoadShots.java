package com.mechanical.people;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mechanical.Mechanical;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like com.mechanical.LogShots): with a file {@code roadshots.flag} in the game directory (its first line: an
 * output folder), the client opens the world "RoadShots", builds a platform at y 200 with a 3-block rise and a plank wall,
 * plans and lays a path and a trade road across it (RoadBuilding) and takes screenshots (road_*.png); then it quits.
 * Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class RoadShots {
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static int bx, bz;

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
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("RoadShots", () -> {});
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
                    server.execute(RoadShots::setup);
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    next();
                }
                case 2 -> { // overview from the south-west: the wall, the rise, both roads climbing it
                    if (ticks < 80) return;
                    server.execute(() -> look(bx - 12.5, 211, bz + 15.5, -140, 30));
                    next();
                }
                case 3 -> {
                    if (ticks < 40) return;
                    shot(mc, "overview");
                    server.execute(() -> look(bx + 3.5, 204.5, bz + 11.5, 180, 22)); // side on to the steps
                    next();
                }
                case 4 -> {
                    if (ticks < 30) return;
                    shot(mc, "steps_side");
                    server.execute(() -> look(bx - 4.5, 202.6, bz + 5.5, -90, 12)); // standing on the trade road, looking up the steps
                    next();
                }
                case 5 -> {
                    if (ticks < 30) return;
                    shot(mc, "trade_road_steps");
                    server.execute(() -> look(bx - 10.5, 201.6, bz - 1.5, -95, 10)); // on the path, at the wall
                    next();
                }
                case 6 -> {
                    if (ticks < 30) return;
                    shot(mc, "path_round_wall");
                    next();
                }
                case 7 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "road_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
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

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void look(double x, double y, double z, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), x, y, z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** Server: grass at y 199 (x -22..22, z -12..12 round the player), a 3-block rise east of x+2, a plank wall; then both roads. */
    private static void setup() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        bx = (int) Math.floor(p.getX());
        bz = (int) Math.floor(p.getZ());
        cmd("gamemode spectator @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("gamerule advance_time false");
        cmd("fill " + (bx - 22) + " 190 " + (bz - 12) + " " + (bx + 22) + " 198 " + (bz + 12) + " dirt");
        cmd("fill " + (bx - 22) + " 199 " + (bz - 12) + " " + (bx + 22) + " 199 " + (bz + 12) + " grass_block");
        cmd("fill " + (bx - 22) + " 200 " + (bz - 12) + " " + (bx + 22) + " 215 " + (bz + 12) + " air");
        cmd("fill " + (bx + 2) + " 199 " + (bz - 12) + " " + (bx + 22) + " 201 " + (bz + 12) + " dirt");
        cmd("fill " + (bx + 2) + " 202 " + (bz - 12) + " " + (bx + 22) + " 202 " + (bz + 12) + " grass_block");
        cmd("fill " + (bx - 8) + " 200 " + (bz - 6) + " " + (bx - 8) + " 201 " + (bz + 1) + " oak_planks"); // someone's wall across the path's line
        cmd("kill @e[type=!player,distance=..60]");
        road(level, -2, RoadBuilding.PATH);
        road(level, 5, RoadBuilding.TRADE);
    }

    private static void road(ServerLevel level, int dz, int kind) {
        BlockPos from = new BlockPos(bx - 18, 199, bz + dz), to = new BlockPos(bx + 18, 202, bz + dz);
        List<BlockPos> path = RoadBuilding.plan(level, from, to, bx - 22, bz - 12, bx + 22, bz + 12);
        if (path == null) {
            result.add("no route for kind " + kind);
            return;
        }
        RoadBuilding.Road r = new RoadBuilding.Road(UUID.randomUUID(), UUID.randomUUID(), from, to, kind, List.of(), 0, true, List.of());
        RoadBuilding.append(r, path);
        for (int i = 0; i < r.cells.size(); i++) RoadBuilding.buildCell(level, r, i);
        result.add("kind " + kind + ": " + r.cells.size() + " cells, from " + r.cell(0) + " to " + r.cell(r.cells.size() - 1));
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
