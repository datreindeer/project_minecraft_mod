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
 * spirits are seen) and photographs each folklore being in turn (folklore_&lt;name&gt;.png); the Dune Wraith and Frost
 * Giant last, in a winter thunderstorm. Then it quits. Does nothing without the flag file. Same pattern as JungleShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FolkShots {
    private static int stage = -1, ticks, index;
    private static File out;
    private static Vec3 base;
    private static Mob current;
    private static final List<String> result = new ArrayList<>();

    /** name, type (null: the Mossy Troll Stone blocks), camera distance, camera height, storm. */
    private record Shot(String name, Supplier<? extends EntityType<? extends Mob>> type, double dist, double height, boolean storm) {}

    private static final List<Shot> SHOTS = List.of(
            new Shot("huldra", Beings.HULDRA::get, 3.2, 1.4, false),
            new Shot("huldra_back", Beings.HULDRA::get, -3.2, 1.4, false),
            new Shot("will_o_wisp", Beings.WILL_O_WISP::get, 2.2, 1.2, false),
            new Shot("jungle_spirit_mask", Beings.JUNGLE_SPIRIT_MASK::get, 1.8, 0.7, false),
            new Shot("tomte", Beings.TOMTE::get, 1.8, 0.8, false),
            new Shot("nacken", Beings.NACKEN::get, 3.2, 1.4, false),
            new Shot("bog_body", Beings.BOG_BODY::get, 3.2, 1.4, false),
            new Shot("mossy_troll_stone", null, 3.0, 1.6, false),
            new Shot("dune_wraith", Beings.DUNE_WRAITH::get, 3.2, 1.4, true),
            new Shot("frost_giant", Beings.FROST_GIANT::get, 7.5, 3.5, true));

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
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
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
            if (stage == 2) {
                if (index >= SHOTS.size()) {
                    if (ticks < 20) return;
                    Files.write(new File(out, "folklore_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                    return;
                }
                Shot s = SHOTS.get(index);
                if (ticks == 5 && s.storm() && (index == 0 || !SHOTS.get(index - 1).storm())) server.execute(() -> {
                    cmd("calendar set season winter");
                    cmd("time set midnight");
                    cmd("weather thunder");
                });
                if (ticks == 40) server.execute(() -> place(s));
                if (ticks == 75) shot(mc, s.name());
                if (ticks == 80) {
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

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** The being at the platform's centre facing south, still; the camera off its front left (negative dist: from behind). */
    private static void place(Shot s) {
        ServerLevel level = player().level();
        double height = 2.0;
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
            height = mob.getBbHeight();
        }
        Vec3 target = (current != null ? current.position() : base).add(0, height * 0.5, 0);
        double d = Math.abs(s.dist()), side = Math.signum(s.dist());
        Vec3 cam = base.add(-d * 0.8 * side, s.height(), d * 0.6 * side);
        Vec3 v = target.subtract(cam);
        float yaw = (float) (Math.atan2(-v.x, v.z) * 180 / Math.PI);
        float pitch = (float) (-Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * 180 / Math.PI);
        ServerPlayer p = player();
        p.teleportTo(level, cam.x, cam.y - p.getEyeHeight(), cam.z, java.util.Set.of(), yaw, pitch, true);
        result.add(s.name() + " at " + (current != null ? current.position() : base) + " camera " + cam + " thunder " + level.isThundering());
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
