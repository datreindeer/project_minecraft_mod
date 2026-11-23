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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link OwarmShots}): with a file {@code decorshots.flag} in the game directory, the client opens the world
 * "DecorShots" (a copy of a dev server world) and photographs spots in it, then quits. Flag file: first line the output
 * folder, then one shot per line: {@code name x y z distance angle [height]}. The camera stands distance blocks from
 * x,y,z toward angle (degrees, 0 = south, 90 = west), at eye height over the ground there (or height blocks above y when
 * given), looking at the spot. A line {@code @ x y z command} goes to x,y,z and runs the command there (to place a feature
 * on a chosen spot before shooting it; several commands joined by " ; "); "@@ x y z command" runs only if nothing was placed since
 * the last shot (the next spot to try); "last" for x y z means the last decoration a command placed, and an angle "dN" is
 * the angle it logged to look from plus N (needs decorlog.flag). Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DecorShots {
    private record Shot(String name, double x, double y, double z, double distance, double angle, Double height, String cmd) {
        /** "last" coordinates (NaN) mean the last decoration a command placed; an angle over 1000 is 1000 + an offset from the angle it logged. */
        Shot resolve() {
            if (!Double.isNaN(x)) return this;
            var p = com.mechanical.worldgen.Decor.lastPlaced;
            if (p == null) return new Shot(name, 0, 100, 0, distance, angle, height, cmd);
            double a = angle >= 1000 ? (com.mechanical.worldgen.Decor.lastAngle >= 0 ? com.mechanical.worldgen.Decor.lastAngle : 0) + angle - 1000 : angle;
            return new Shot(name, p.getX(), p.getY(), p.getZ(), distance, a, height, cmd);
        }
    }

    private static double num(String v) {
        return v.equals("last") ? Double.NaN : v.startsWith("d") ? 1000 + Double.parseDouble(v.substring(1)) : Double.parseDouble(v);
    }

    private static int placedBefore = -1;

    private static final List<Shot> shots = new ArrayList<>();
    private static final List<String> result = new ArrayList<>();
    private static int stage = -1, ticks, index;
    private static File out;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "decorshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                for (String l : lines.subList(1, lines.size())) {
                    String[] p = l.trim().split("\\s+");
                    // "@ x y z command": go there, then run the command (e.g. place feature); "@@ ...": only if nothing was placed since
                    // the last shot (try the next spot); "@ last command": at the last placed decoration ("x y z" = "last")
                    if (p.length >= 3 && (p[0].equals("@") || p[0].equals("@@"))) {
                        boolean last = p[1].equals("last");
                        int at = last ? 2 : 4;
                        shots.add(new Shot(p[0], num(p[1]), last ? 0 : num(p[2]), last ? 0 : num(p[3]), 0, 0, null,
                                String.join(" ", java.util.Arrays.copyOfRange(p, at, p.length))));
                        continue;
                    }
                    if (p.length >= 4 && p[1].equals("last")) { // "name last distance angle [height]"
                        shots.add(new Shot(p[0], Double.NaN, 0, 0, num(p[2]), num(p[3]), p.length > 4 ? num(p[4]) : null, null));
                        continue;
                    }
                    if (p.length < 6) continue;
                    shots.add(new Shot(p[0], num(p[1]), num(p[2]), num(p[3]), num(p[4]), num(p[5]), p.length > 6 ? num(p[6]) : null, null));
                }
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("DecorShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> { // settle in
                    mc.options.pauseOnLostFocus = false;
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        cmd("gamemode spectator @a");
                        cmd("gamerule advance_time false");
                        cmd("time set 2000");
                        cmd("weather clear");
                    });
                    stage = 2;
                    ticks = 0;
                }
                case 2 -> { // fly to the next spot so its chunks load
                    if (index >= shots.size()) {
                        Files.write(new File(out, "decor_result.txt").toPath(), result);
                        stage = -2;
                        mc.stop();
                        return;
                    }
                    Shot s0 = shots.get(index);
                    if (s0.name().equals("@@") && placedBefore >= 0 && com.mechanical.worldgen.Decor.placedCount != placedBefore) {
                        index++; // placed already: skip the fallback spot
                        return;
                    }
                    if (!s0.name().startsWith("@")) placedBefore = -1;
                    else if (placedBefore < 0) {
                        placedBefore = com.mechanical.worldgen.Decor.placedCount;
                        com.mechanical.worldgen.Decor.lastAngle = -1;
                    }
                    Shot s = s0.resolve();
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), s.x(), s.y() + 30, s.z(), java.util.Set.of(), 0, 90, true);
                    });
                    stage = 3;
                    ticks = 0;
                }
                case 3 -> { // stand at the camera spot
                    if (ticks < 60) return;
                    Shot s = shots.get(index).resolve();
                    if (s.cmd() != null) { // a command line: run it where we are, then on to the next line
                        server.execute(() -> {
                            ServerPlayer pl = player();
                            for (String c : s.cmd().split(" ; ")) pl.level().getServer().getCommands().performPrefixedCommand(
                                    pl.level().getServer().createCommandSourceStack().withPosition(pl.position()), c); // the server's permission: the copied world may not allow commands
                        });
                        server.execute(() -> result.add("@ " + s.cmd() + " -> last placed " + com.mechanical.worldgen.Decor.lastPlaced + " angle " + com.mechanical.worldgen.Decor.lastAngle));
                        index++;
                        stage = 2;
                        ticks = 0;
                        return;
                    }
                    server.execute(() -> aim(s));
                    stage = 4;
                    ticks = 0;
                }
                case 4 -> {
                    if (ticks < 110) return;
                    shot(mc, shots.get(index).name());
                    index++;
                    stage = 2;
                    ticks = 0;
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "decor_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void aim(Shot s) {
        ServerPlayer p = player();
        var level = p.level();
        Vec3 look = new Vec3(s.x() + 0.5, s.y() + 0.5, s.z() + 0.5), eye = null;
        boolean overview = s.name().startsWith("overview");
        // a lying log is shot from the side: start from the angle across its axis
        double base = s.angle();
        for (int dy = 0; dy >= -1 && !s.name().contains("plate") && !s.name().contains("dam"); dy--) {
            var st = level.getBlockState(net.minecraft.core.BlockPos.containing(s.x(), s.y() + dy, s.z()));
            if (st.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) && st.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) != net.minecraft.core.Direction.Axis.Y) {
                base = (st.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.X ? 0 : 90) + (s.angle() % 2 == 0 ? 0 : 180) + 25;
                break;
            }
        }
        // at eye height over open ground, the first angle (from the given one round) and distance with a clear view
        for (int i = 0; i < 16 && eye == null; i++) {
            for (double dist : new double[] {s.distance(), s.distance() - 2, s.distance() + 2, s.distance() + 4}) {
                double a = Math.toRadians(base + 22.5 * i);
                double cx = s.x() + 0.5 - Math.sin(a) * dist, cz = s.z() + 0.5 + Math.cos(a) * dist;
                int ix = (int) Math.floor(cx), iz = (int) Math.floor(cz), ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
                // overviews stand in the open (no canopy over the camera) and a little higher
                if (overview && level.getHeight(Heightmap.Types.MOTION_BLOCKING, ix, iz) > ground) continue;
                double y = s.height() != null ? s.y() + s.height() : ground + (overview ? 4.5 : 2.3);
                Vec3 e = new Vec3(cx, y, cz);
                var hit = level.clip(new net.minecraft.world.level.ClipContext(e, look, net.minecraft.world.level.ClipContext.Block.VISUAL,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, p));
                if (level.getBlockState(net.minecraft.core.BlockPos.containing(e)).isAir() && hit.getLocation().distanceTo(look) < 1.6) {
                    eye = e;
                    break;
                }
                if (s.height() != null) break;
            }
        }
        if (eye == null) {
            double a = Math.toRadians(s.angle());
            eye = new Vec3(s.x() + 0.5 - Math.sin(a) * s.distance(), s.y() + (s.height() != null ? s.height() : 3), s.z() + 0.5 + Math.cos(a) * s.distance());
            result.add(s.name() + ": no clear view");
        }
        Vec3 d = look.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        p.teleportTo(level, eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), yaw, pitch, true);
        result.add(s.name() + ": eye " + eye + " yaw " + yaw + " pitch " + pitch);
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private DecorShots() {}
}
