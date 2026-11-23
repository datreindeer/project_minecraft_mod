package com.mechanical.client.folklore;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mechanical.folklore.Beings;
import com.mechanical.folklore.Faith;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with a file {@code folkshots.flag} in the game directory (its first line: an output folder), the client opens
 * the world "FolkShots", builds a grass platform at y 200 at night (night vision on, Faith 100, creative flying so the
 * spirits are seen) and photographs each folklore being at eye level from three quarters, the front, the side and the
 * back (folklore_&lt;name&gt;[_front|_side|_back].png); the Dune Wraith and Frost Giant last, in a winter thunderstorm. Then it quits. Does nothing without the flag file. Same pattern as JungleShots.
 * With "watcher" on the flag file's second line it photographs only the Mountain Watcher instead, by day: perched and
 * held in flight from all round (folklore_mountain_watcher_&lt;n&gt;.png), then free and circling, seen from the platform
 * (folklore_mountain_watcher_sky&lt;n&gt;.png).
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FolkShots {
    private static int stage = -1, ticks, index;
    private static File out;
    private static Vec3 base;
    private static Mob current;
    private static boolean watcher;
    /** The Watcher's views: angle round from its front, camera height over the platform, distance, bird height, flying (1). */
    private static final double[][] WATCHER_VIEWS = {{35, 1.6, 4.5, 0, 0}, {0, 1.3, 4.5, 0, 0}, {90, 1.3, 4.5, 0, 0}, {180, 1.6, 4.5, 0, 0},
            {35, 1.6, 2.6, 0, 0}, {35, 5.5, 7.5, 3, 1}, {90, 1.8, 8.0, 3.5, 1}, {0, 3.2, 7.0, 3, 1}, {160, 8.5, 6.0, 3, 1},
            {210, 0.2, 6.5, 4, 1}, {60, 3.6, 2.8, 3, 1}, {20, 6.2, 4.2, 3, 1}};
    private static final int SKY_SHOTS = 6;
    private static final List<String> result = new ArrayList<>();

    /** name, type (null: the Mossy Troll Stone blocks), camera distance, camera height (eye level), storm. */
    private record Shot(String name, Supplier<? extends EntityType<? extends Mob>> type, double dist, double height, boolean storm) {}

    /** Each being from three quarters (folklore_<name>.png), the front, the side and the back; the giant from further off too. */
    private static final String[] VIEWS = {"", "_front", "_side", "_back"};
    private static final double[] ANGLES = {35, 0, 90, 180};

    private static final List<Shot> SHOTS = List.of(
            new Shot("huldra", Beings.HULDRA::get, 2.0, 1.6, false),
            new Shot("will_o_wisp", Beings.WILL_O_WISP::get, 1.6, 1.3, false),
            new Shot("jungle_spirit_mask", Beings.JUNGLE_SPIRIT_MASK::get, 1.9, 1.65, false),
            new Shot("tomte", Beings.TOMTE::get, 1.3, 0.8, false),
            new Shot("nacken", Beings.NACKEN::get, 2.0, 1.6, false),
            new Shot("bog_body", Beings.BOG_BODY::get, 2.0, 1.5, false),
            new Shot("mossy_troll_stone", null, 3.0, 1.6, false),
            new Shot("dune_wraith", Beings.DUNE_WRAITH::get, 2.1, 1.6, true),
            new Shot("frost_giant", Beings.FROST_GIANT::get, 6.5, 3.4, true));

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "folkshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                watcher = lines.size() > 1 && lines.get(1).trim().equals("watcher");
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("FolkShots", () -> {});
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
                server.execute(FolkShots::setup);
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); // F1
                mc.gui.toastManager().clear();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                stage = 2;
                ticks = 0;
                return;
            }
            if (stage == 2 && watcher) {
                watcherTick(mc, server);
                return;
            }
            if (stage == 2) {
                if (index >= SHOTS.size()) {
                    if (ticks < 20) return;
                    Files.write(new File(out, "folklore_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                    return;
                }
                Shot s = SHOTS.get(index);
                int wait = s.storm() ? 140 : 0; // thunder builds up over a few seconds
                if (ticks == 5 && s.storm()) server.execute(() -> {
                    cmd("time set midnight");
                    cmd("weather thunder");
                });
                if (ticks == 5 && s.name().equals("frost_giant")) server.execute(() -> cmd("calendar set season winter"));
                // he walks back into the snow when the storm ends: keep it on the server, but stop the snowfall on screen
                if (s.name().equals("frost_giant") && mc.level != null) {
                    mc.level.setRainLevel(0);
                    mc.level.setThunderLevel(0);
                }
                if (ticks == 40 + wait) server.execute(() -> place(s));
                int views = s.type() == null ? 1 : VIEWS.length;
                for (int v = 0; v < views; v++) {
                    int view = v;
                    if (ticks == 50 + wait + v * 25) server.execute(() -> aim(s, ANGLES[view]));
                    if (ticks == 70 + wait + v * 25) shot(mc, s.name() + VIEWS[view]);
                }
                if (ticks == 75 + wait + views * 25) {
                    server.execute(() -> {
                        if (current != null) current.discard();
                        if (s.type() == null) cmd("fill " + (int) Math.floor(base.x) + " 200 " + (int) Math.floor(base.z) + " "
                                + (int) Math.floor(base.x) + " 201 " + (int) Math.floor(base.z) + " air");
                    });
                    index++;
                    ticks = 0;
                }
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "folklore_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void watcherTick(Minecraft mc, net.minecraft.server.MinecraftServer server) throws Exception {
        if (index == 0 && ticks == 5) server.execute(() -> {
            cmd("time set noon");
            cmd("weather clear");
            cmd("effect clear @a minecraft:night_vision");
            // Watchers left in the world by an earlier run (they cannot be killed)
            for (var e : player().level().getEntities(com.mechanical.folklore.Folklore.MOUNTAIN_WATCHER.get(), x -> true)) e.discard();
        });
        if (index < WATCHER_VIEWS.length) {
            double[] v = WATCHER_VIEWS[index];
            int n = index;
            if (ticks == 30) server.execute(() -> holdWatcher(v));
            if (ticks == 60) shot(mc, "mountain_watcher_" + n);
            if (ticks == 65) {
                index++;
                ticks = 0;
            }
            return;
        }
        int k = index - WATCHER_VIEWS.length;
        if (k == 0 && ticks == 1) server.execute(() -> {
            // free: high over the platform, flying, left to circle
            if (current != null) current.discard();
            Mob mob = com.mechanical.folklore.Folklore.MOUNTAIN_WATCHER.get().create(player().level(), EntitySpawnReason.COMMAND);
            mob.snapTo(base.x + 2, base.y + 14, base.z, 0, 0);
            mob.setPersistenceRequired();
            setFlying(mob);
            player().level().addFreshEntity(mob);
            current = mob;
        });
        if (k >= SKY_SHOTS) {
            if (ticks > 20) {
                Files.write(new File(out, "folklore_result.txt").toPath(), result);
                stage = -2;
                mc.stop();
            }
            return;
        }
        int wait = k == 0 ? 200 : 50;
        if (ticks > wait - 10 && ticks <= wait) server.execute(FolkShots::lookAtWatcher);
        if (ticks == wait + 2) shot(mc, "mountain_watcher_sky" + k);
        if (ticks == wait + 5) {
            index++;
            ticks = 0;
        }
    }

    /** The Watcher held still (no AI) at the given height, perched or in flight, the camera round it. */
    private static void holdWatcher(double[] v) {
        ServerLevel level = player().level();
        if (current == null) {
            Mob mob = com.mechanical.folklore.Folklore.MOUNTAIN_WATCHER.get().create(level, EntitySpawnReason.COMMAND);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            mob.snapTo(base.x, base.y, base.z, 0, 0);
            level.addFreshEntity(mob);
            current = mob;
        }
        if (v[4] > 0 && current instanceof com.mechanical.wildlife.FlockBird b && !b.isFlying()) setFlying(b);
        current.setNoGravity(v[4] > 0);
        current.teleportTo(base.x, base.y + v[3], base.z);
        current.setYRot(0);
        current.setYHeadRot(0);
        current.setYBodyRot(0);
        Vec3 at = current.position().add(0, current.getBbHeight() * 0.5, 0);
        double a = Math.toRadians(v[0]);
        Vec3 cam = new Vec3(base.x + Math.sin(a) * v[2], base.y + v[1], base.z + Math.cos(a) * v[2]);
        look(cam, at);
        result.add("watcher view " + java.util.Arrays.toString(v) + " flying " + ((com.mechanical.wildlife.FlockBird) current).isFlying());
    }

    private static void lookAtWatcher() {
        if (current == null) return;
        look(base.add(0, 1.6, 0), current.position().add(0, 0.6, 0));
        result.add("sky: watcher at " + current.position() + " flying " + ((com.mechanical.wildlife.FlockBird) current).isFlying());
    }

    /** FlockBird's synced FLYING flag (private: its takeOff also sets course and plays a sound). */
    @SuppressWarnings("unchecked")
    private static void setFlying(Mob bird) {
        try {
            var f = com.mechanical.wildlife.FlockBird.class.getDeclaredField("FLYING");
            f.setAccessible(true);
            bird.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Boolean>) f.get(null), true);
            bird.setNoGravity(true);
            var timer = com.mechanical.wildlife.FlockBird.class.getDeclaredField("flightTimer");
            timer.setAccessible(true);
            timer.setInt(bird, 100000); // stays up for the whole series
        } catch (ReflectiveOperationException e) {
            result.add("flying: " + e);
        }
    }

    private static void look(Vec3 cam, Vec3 at) {
        Vec3 v = at.subtract(cam);
        float yaw = (float) (Math.atan2(-v.x, v.z) * 180 / Math.PI);
        float pitch = (float) (-Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * 180 / Math.PI);
        ServerPlayer p = player();
        p.teleportTo(p.level(), cam.x, cam.y - p.getEyeHeight(), cam.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** The being at the platform's centre facing south (+z), still. */
    private static void place(Shot s) {
        ServerLevel level = player().level();
        current = null;
        if (s.type() == null) {
            int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
            cmd("fill " + x + " 200 " + z + " " + x + " 201 " + z + " mechanical:mossy_troll_stone");
        } else {
            Mob mob = s.type().get().create(level, EntitySpawnReason.COMMAND);
            double y = s.name().equals("will_o_wisp") || s.name().equals("jungle_spirit_mask") ? base.y + 1.0 : base.y;
            mob.snapTo(base.x, y, base.z, 0, 0);
            mob.setYHeadRot(0);
            mob.setYBodyRot(0);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
            current = mob;
        }
        aim(s, ANGLES[0]);
    }

    /** The camera {@code angle} degrees round from the being's front (towards its left), at eye level, looking at its middle. */
    private static void aim(Shot s, double angle) {
        ServerLevel level = player().level();
        double height = current != null ? current.getBbHeight() : 2.0;
        Vec3 at = current != null ? current.position() : base;
        Vec3 target = at.add(0, Math.min(height * 0.55, s.height() - (current != null ? at.y - base.y : 0)), 0);
        double a = Math.toRadians(angle);
        Vec3 cam = new Vec3(at.x + Math.sin(a) * s.dist(), base.y + s.height(), at.z + Math.cos(a) * s.dist());
        Vec3 v = target.subtract(cam);
        float yaw = (float) (Math.atan2(-v.x, v.z) * 180 / Math.PI);
        float pitch = (float) (-Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * 180 / Math.PI);
        ServerPlayer p = player();
        p.teleportTo(level, cam.x, cam.y - p.getEyeHeight(), cam.z, java.util.Set.of(), yaw, pitch, true);
        result.add(s.name() + " " + angle + " at " + at + " camera " + cam + " thunder " + level.isThundering());
    }

    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 200, z + 0.5);
        cmd("gamemode creative @a");
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        Faith.set(p, Faith.MAX);
        cmd("calendar set season autumn");
        cmd("time set midnight");
        cmd("weather clear");
        cmd("gamerule doDaylightCycle false");
        cmd("gamerule doWeatherCycle false");
        cmd("gamerule doMobSpawning false");
        cmd("effect give @a minecraft:night_vision infinite 0 true");
        cmd("kill @e[type=!minecraft:player]");
        cmd("fill " + (x - 12) + " 197 " + (z - 12) + " " + (x + 12) + " 198 " + (z + 12) + " dirt");
        cmd("fill " + (x - 12) + " 199 " + (z - 12) + " " + (x + 12) + " 199 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 212 " + (z + 12) + " air");
        result.add("setup at " + base);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "folklore_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private FolkShots() {}
}
