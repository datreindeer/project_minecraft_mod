package com.mechanical.client.wildlife.deep;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.deep.BatSwarm;
import com.mechanical.wildlife.deep.DeepFauna;
import com.mechanical.wildlife.giants.GiantsFauna;
import com.mechanical.wildlife.mountain.MountainFauna;
import com.mechanical.worldgen.ModBiomes;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with a file {@code deepshots.flag} in the game directory (its first line: an output folder), the client opens
 * the world "DeepShots", builds a grass platform at y 200 and photographs each deep creature (deep_<name>.png; the olm and
 * the fish in a pool, the spider beside its web in a passage), then finds a Mammoth Steppe and photographs it
 * (steppe_mammoth_steppe.png), then quits. Does nothing without the flag file. Same pattern as JungleShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DeepShots {
    private static int stage = -1, ticks, index;
    private static File out;
    private static Vec3 base;
    private static final List<Mob> current = new ArrayList<>();
    private static final List<String> result = new ArrayList<>();
    private static Vec3 steppeCam;

    /** name, scene builder (server thread), camera distance, camera height. */
    private record Shot(String name, Runnable scene, double dist, double height) {}

    private static final List<Shot> SHOTS = List.of(
            new Shot("cave_salamander", () -> pool(DeepFauna.CAVE_SALAMANDER.get(), 1), 1.2, 0.5),
            new Shot("blind_cave_fish", () -> pool(DeepFauna.BLIND_CAVE_FISH.get(), 4), 1.6, 0.6),
            new Shot("bat_swarm", () -> spawn(DeepFauna.BAT_SWARM.get(), base.add(0, 1.0, 0)), 3.0, 1.6),
            new Shot("cavern_spider", () -> spawn(DeepFauna.CAVERN_SPIDER.get(), base), 2.4, 1.2),
            new Shot("cave_web", DeepShots::webScene, 3.4, 1.4));

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "deepshots.flag");
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
                    mc.createWorldOpenFlows().openWorld("DeepShots", () -> {});
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
                server.execute(DeepShots::setup);
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
                    stage = 3;
                    ticks = 0;
                    server.execute(DeepShots::goToSteppe);
                    return;
                }
                Shot s = SHOTS.get(index);
                if (ticks == 20) server.execute(() -> {
                    aimed = false;
                    s.scene().run();
                    if (!aimed) aim(s.dist(), s.height());
                });
                if (ticks == 60) shot(mc, "deep_" + s.name());
                if (ticks == 65) {
                    server.execute(DeepShots::clear);
                    index++;
                    ticks = 0;
                }
                return;
            }
            if (stage == 3) {
                // the steppe: wait for the chunks round the camera to load and render
                if (ticks == 150) server.execute(DeepShots::herd);
                if (ticks == 300) shot(mc, "steppe_mammoth_steppe");
                if (ticks == 320) {
                    Files.write(new File(out, "deep_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "deep_result.txt").toPath(), result);
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

    private static Mob spawn(EntityType<? extends Mob> type, Vec3 at) {
        ServerLevel level = player().level();
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        mob.snapTo(at.x, at.y, at.z, 0, 0);
        mob.setYHeadRot(0);
        mob.setYBodyRot(0);
        mob.setNoAi(true); // the swarm's bats still wheel (the model animates on age)
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        current.add(mob);
        return mob;
    }

    /** A 5x5 pool, 2 deep, sunk into the platform, with {@code n} of the creature in it. */
    private static void pool(EntityType<? extends Mob> type, int n) {
        int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
        cmd("fill " + (x - 2) + " 198 " + (z - 2) + " " + (x + 2) + " 199 " + (z + 2) + " water");
        cmd("fill " + (x - 2) + " 197 " + (z - 2) + " " + (x + 2) + " 197 " + (z + 2) + " stone");
        for (int i = 0; i < n; i++) spawn(type, new Vec3(base.x + (i % 2) * 0.7 - 0.35 * (n > 1 ? 1 : 0), 198.6, base.z + (i / 2) * 0.6 - 0.3));
        Vec3 cam = base.add(-0.9 - n * 0.15, 0.2, 0.9 + n * 0.15);
        look(cam, new Vec3(base.x, 198.7, base.z));
        aimed = true;
    }

    /** A narrow stone passage (one wide, two high, running away from the camera) webbed across, the spider at its mouth. */
    private static void webScene() {
        int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
        cmd("fill " + (x - 1) + " 200 " + (z - 6) + " " + (x - 1) + " 201 " + z + " stone");
        cmd("fill " + (x + 1) + " 200 " + (z - 6) + " " + (x + 1) + " 201 " + z + " stone");
        cmd("fill " + (x - 1) + " 202 " + (z - 6) + " " + (x + 1) + " 202 " + z + " stone");
        cmd("fill " + x + " 200 " + (z - 6) + " " + x + " 201 " + (z - 6) + " stone");
        ServerLevel level = player().level();
        int webs = com.mechanical.wildlife.deep.CavernSpider.weave(level, new BlockPos(x, 200, z - 3));
        result.add("webs " + webs);
        Mob spider = spawn(DeepFauna.CAVERN_SPIDER.get(), base.add(0.9, 0, 1.6));
        spider.snapTo(spider.getX(), spider.getY(), spider.getZ(), 150, 0);
        look(base.add(-1.2, 1.5, 4.2), base.add(0, 0.6, -2.5));
        aimed = true;
    }

    private static boolean aimed;

    /** Camera off the front left quarter of the platform's centre. */
    private static void aim(double dist, double height) {
        Vec3 target = base.add(0, 0.4, 0);
        Vec3 cam = base.add(-dist * 0.8, height, dist * 0.6);
        look(cam, target);
        result.add("camera " + cam);
    }

    private static void look(Vec3 cam, Vec3 target) {
        Vec3 d = target.subtract(cam);
        float yaw = (float) (Math.atan2(-d.x, d.z) * 180 / Math.PI);
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        ServerPlayer p = player();
        p.teleportTo(p.level(), cam.x, cam.y - p.getEyeHeight(), cam.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void clear() {
        current.forEach(Mob::discard);
        current.clear();
        int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
        cmd("fill " + (x - 12) + " 197 " + (z - 12) + " " + (x + 12) + " 198 " + (z + 12) + " dirt");
        cmd("fill " + (x - 12) + " 199 " + (z - 12) + " " + (x + 12) + " 199 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 212 " + (z + 12) + " air");
    }

    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 200, z + 0.5);
        steppeFrom = BlockPos.containing(base);
        cmd("gamemode spectator @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("gamerule doDaylightCycle false");
        cmd("gamerule doMobSpawning false");
        cmd("kill @e[type=!minecraft:player]");
        clear();
        result.add("setup at " + base);
    }

    /** The nearest Mammoth Steppe: the camera 14 blocks over its ground, a few of its animals in front. */
    private static void goToSteppe() {
        ServerLevel level = player().level();
        BlockPos from = new BlockPos(steppeFrom.getX(), 64, steppeFrom.getZ());
        var found = level.findClosestBiome3d(b -> b.is(ModBiomes.MAMMOTH_STEPPE), from, 8000, 48, 64);
        if (found != null && found.getFirst().closerThan(steppeFrom, 40)) found = null; // the platform sits on it: skip the shot
        if (found == null) {
            result.add("no mammoth steppe found");
            return;
        }
        BlockPos at = found.getFirst();
        level.getChunk(at.getX() >> 4, at.getZ() >> 4); // generate it
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        result.add("steppe at " + at + " ground " + y);
        Vec3 ground = new Vec3(at.getX() + 0.5, y, at.getZ() + 0.5);
        steppeAt = at;
        steppeCam = ground.add(-14, 8, 14);
        look(steppeCam, ground.add(6, 0, -6));
    }

    private static BlockPos steppeAt, steppeFrom;

    /** A few steppe animals in front of the camera, once the chunks are loaded. */
    private static void herd() {
        if (steppeAt == null) return;
        ServerLevel level = player().level();
        BlockPos at = steppeAt;
        EntityType<?>[] herd = {GiantsFauna.WOOLLY_MAMMOTH.get(), GiantsFauna.WOOLLY_MAMMOTH.get(), Wildlife.WILD_HORSE.get(),
                Wildlife.WILD_HORSE.get(), GiantsFauna.STEPPE_BISON.get(), MountainFauna.SAIGA.get()};
        for (int i = 0; i < herd.length; i++) {
            int ax = at.getX() + 2 + i * 3, az = at.getZ() - 2 - (i % 3) * 3;
            int ay = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ax, az);
            Mob mob = (Mob) herd[i].create(level, EntitySpawnReason.COMMAND);
            mob.snapTo(ax + 0.5, ay, az + 0.5, 200 + i * 25, 0);
            mob.setNoAi(true);
            level.addFreshEntity(mob);
        }
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

    private DeepShots() {}
}
