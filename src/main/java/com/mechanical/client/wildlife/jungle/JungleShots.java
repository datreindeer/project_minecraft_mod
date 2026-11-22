package com.mechanical.client.wildlife.jungle;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.jungle.JungleFauna;

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
 * Dev only: with a file {@code jungleshots.flag} in the game directory (its first line: an output folder), the client
 * opens the world "JungleShots", builds a grass platform at y 200 and photographs each jungle creature in turn
 * (jungle_<name>.png), then quits. Does nothing without the flag file. Same pattern as NatureShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class JungleShots {
    private static int stage = -1, ticks, index;
    private static File out;
    private static Vec3 base;
    private static Mob current;
    private static final List<String> result = new ArrayList<>();

    /** name, type, camera distance, camera height. */
    private record Shot(String name, java.util.function.Supplier<? extends EntityType<? extends Mob>> type, double dist, double height) {}

    private static final List<Shot> SHOTS = List.of(
            new Shot("tiger", JungleFauna.TIGER::get, 4.2, 1.4),
            new Shot("python", JungleFauna.PYTHON::get, 3.6, 1.6),
            new Shot("orangutan", JungleFauna.ORANGUTAN::get, 3.6, 1.2),
            new Shot("monitor_lizard", JungleFauna.MONITOR_LIZARD::get, 3.0, 1.2),
            new Shot("poison_dart_frog", JungleFauna.POISON_DART_FROG::get, 0.9, 0.5),
            new Shot("saltwater_crocodile", JungleFauna.SALTWATER_CROCODILE::get, 5.2, 2.0),
            new Shot("mudskipper", JungleFauna.MUDSKIPPER::get, 1.0, 0.5),
            new Shot("fiddler_crab", JungleFauna.FIDDLER_CRAB::get, 1.0, 0.55),
            new Shot("proboscis_monkey", JungleFauna.PROBOSCIS_MONKEY::get, 2.8, 1.0));

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "jungleshots.flag");
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
                    mc.createWorldOpenFlows().openWorld("JungleShots", () -> {});
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
                server.execute(JungleShots::setup);
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); // F1
                mc.gui.toastManager().clear();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                stage = 2;
                ticks = 0;
                return;
            }
            if (stage == 2) {
                if (index >= SHOTS.size()) {
                    if (ticks < 20) return;
                    Files.write(new File(out, "jungle_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                    return;
                }
                Shot s = SHOTS.get(index);
                if (ticks == 20) server.execute(() -> place(s));
                if (ticks == 50) shot(mc, s.name());
                if (ticks == 55) {
                    server.execute(() -> {
                        if (current != null) current.discard();
                    });
                    index++;
                    ticks = 0;
                }
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "jungle_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** The creature at the platform's centre, side-on, still; the camera off its front left quarter. */
    private static void place(Shot s) {
        ServerLevel level = player().level();
        Mob mob = s.type().get().create(level, EntitySpawnReason.COMMAND);
        mob.snapTo(base.x, base.y, base.z, 0, 0);
        mob.setYHeadRot(0);
        mob.setYBodyRot(0);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        current = mob;
        Vec3 target = base.add(0, mob.getBbHeight() * 0.5, 0);
        Vec3 cam = base.add(-s.dist() * 0.8, s.height(), s.dist() * 0.6); // facing south (+z): seen from the front left
        Vec3 d = target.subtract(cam);
        float yaw = (float) (Math.atan2(-d.x, d.z) * 180 / Math.PI);
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        ServerPlayer p = player();
        p.teleportTo(level, cam.x, cam.y - p.getEyeHeight(), cam.z, java.util.Set.of(), yaw, pitch, true);
        result.add(s.name() + " at " + mob.position() + " camera " + cam);
    }

    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 200, z + 0.5);
        cmd("gamemode spectator @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("gamerule doDaylightCycle false");
        cmd("gamerule doMobSpawning false");
        cmd("kill @e[type=!minecraft:player]");
        cmd("fill " + (x - 12) + " 197 " + (z - 12) + " " + (x + 12) + " 198 " + (z + 12) + " dirt");
        cmd("fill " + (x - 12) + " 199 " + (z - 12) + " " + (x + 12) + " 199 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 212 " + (z + 12) + " air");
        result.add("setup at " + base);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "jungle_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private JungleShots() {}
}
