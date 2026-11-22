package com.mechanical.people;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mechanical.Mechanical;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like LogShots): with a file {@code tribeaishots.flag} in the game directory (its first line: an output folder),
 * the client opens the world "TribeAiShots", builds a grass platform at y 200 and takes screenshots (tribeai_*.png): a
 * visiting band sitting round a fire, a row of people standing and a row seated in each idle action, and a camp at night
 * (beds laid out round the fire, the hunter on watch); then it quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TribeAiShots {
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static final List<Tribesperson> spawned = new ArrayList<>();
    private static Neighbours.Visit visit;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "tribeaishots.flag");
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
                    mc.createWorldOpenFlows().openWorld("TribeAiShots", () -> {});
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
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        cmd("gamemode creative @a");
                        look(player().position().add(3000, 0, 0).with(net.minecraft.core.Direction.Axis.Y, 202), 0, 30); // away from the world's tribes
                    });
                    stage = 20;
                    ticks = 0;
                }
                case 20 -> { // chunks load round the new spot
                    if (ticks < 120) return;
                    server.execute(TribeAiShots::visitScene);
                    stage = 2;
                    ticks = 0;
                }
                case 2 -> { // the band walks in and sits down
                    if (ticks < 400) return;
                    server.execute(() -> {
                        for (Tribesperson t : spawned) result.add("visitor " + t.tribeName() + " " + t.role() + " idle " + t.idle() + " at " + t.position());
                        look(base.add(-5.5, 3.2, -5.5), -45, 28);
                    });
                    next();
                }
                case 3 -> {
                    if (ticks < 20) return;
                    shot(mc, "visit_sitting");
                    server.execute(() -> look(base.add(0.5, 1.5, -4.5), 0, 12));
                    next();
                }
                case 4 -> {
                    if (ticks < 20) return;
                    shot(mc, "visit_sitting_close");
                    server.execute(TribeAiShots::idleScene);
                    next();
                }
                case 5 -> {
                    if (ticks < 40) return;
                    server.execute(() -> look(base.add(3.5, 1.2, -5.0), 0, 8));
                    next();
                }
                case 6 -> {
                    if (ticks < 20) return;
                    shot(mc, "idle_standing");
                    for (Tribesperson t : spawned) {
                        var c = mc.level.getEntity(t.getId());
                        result.add("client idle " + (c instanceof Tribesperson ct ? ct.idle() : "missing") + " server " + t.idle());
                    }
                    server.execute(() -> look(base.add(3.5, 1.0, 1.0), 0, 18));
                    next();
                }
                case 7 -> {
                    if (ticks < 20) return;
                    shot(mc, "idle_seated");
                    server.execute(TribeAiShots::nightScene);
                    next();
                }
                case 8 -> { // they lay out their beds and lie down
                    if (ticks < 500) return;
                    server.execute(() -> {
                        for (Tribesperson t : spawned) result.add("night " + t.tribeName() + " " + t.role() + " sleeping " + t.isSleeping() + " bed " + t.life.bed + " at " + t.position());
                        look(base.add(-6.5, 4.5, -6.5), -45, 32);
                    });
                    next();
                }
                case 9 -> {
                    if (ticks < 20) return;
                    shot(mc, "night_camp");
                    server.execute(() -> look(base.add(0.5, 1.6, -5.5), 0, 15));
                    next();
                }
                case 10 -> {
                    if (ticks < 20) return;
                    shot(mc, "night_camp_close");
                    next();
                }
                case 11 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "tribeai_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "tribeai_result.txt").toPath(), result);
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

    private static void look(Vec3 at, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), at.x, at.y, at.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static Tribesperson person(ServerLevel level, TribeRole role, Vec3 at, BlockPos home, UUID group, boolean leader) {
        Tribesperson t = People.TRIBESPERSON.get().create(level, EntitySpawnReason.EVENT);
        t.setPos(at);
        level.addFreshEntity(t);
        t.setup(role, home, group, leader);
        spawned.add(t);
        return t;
    }

    /** A clean platform: grass at y 199, air above, nothing else about. */
    private static void clear() {
        int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
        cmd("fill " + (x - 14) + " 199 " + (z - 14) + " " + (x + 14) + " 199 " + (z + 14) + " grass_block");
        cmd("fill " + (x - 14) + " 200 " + (z - 14) + " " + (x + 14) + " 210 " + (z + 14) + " air");
        spawned.forEach(Tribesperson::discard);
        spawned.clear();
        cmd("kill @e[type=!player,distance=..40]");
    }

    private static void visitScene() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        base = new Vec3(Math.floor(p.getX()) + 0.5, 200, Math.floor(p.getZ()) + 0.5);
        cmd("gamemode creative @a");
        cmd("time set 3000");
        cmd("weather clear");
        cmd("gamerule advance_time false");
        clear();
        BlockPos fire = BlockPos.containing(base);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        UUID band = UUID.randomUUID();
        long day = level.getOverworldClockTime() / com.mechanical.Calendar.CLOCK_DAY + 1;
        visit = new Neighbours.Visit(UUID.randomUUID(), band, fire, fire, fire, Neighbours.Visit.STAYING, level.getGameTime(), day, true, Optional.empty(), 0);
        Neighbours.get(level).visits.clear(); // a dev world: no other bands about
        Neighbours.get(level).visits.add(visit);
        TribeRole[] roles = {TribeRole.HUNTER, TribeRole.GATHERER, TribeRole.ELDER, TribeRole.WOODCUTTER, TribeRole.GATHERER, TribeRole.HUNTER};
        for (int i = 0; i < roles.length; i++) {
            double a = i * Math.PI * 2 / roles.length;
            Tribesperson t = person(level, roles[i], base.add(Math.cos(a) * 8, 0, Math.sin(a) * 8), null, band, i == 0);
            t.visit = visit.id;
            t.life.forcedPhase = CampLife.DAY;
        }
        look(base.add(0, 6, -10), 0, 30);
    }

    /** Two rows facing north (toward the camera): standing in each idle action, and seated in each. */
    private static void idleScene() {
        ServerLevel level = player().level();
        Neighbours.get(level).visits.remove(visit);
        clear();
        int[] standing = {CampLife.STRETCH, CampLife.CHECK_WEAPON, CampLife.RUMMAGE, CampLife.EAT, CampLife.TALK, CampLife.WARM_HANDS};
        int[] seated = {CampLife.WHITTLE, CampLife.EAT, CampLife.TALK, CampLife.RUMMAGE, CampLife.WARM_HANDS, CampLife.CHECK_WEAPON};
        TribeRole[] roles = {TribeRole.GATHERER, TribeRole.HUNTER, TribeRole.GATHERER, TribeRole.WOODCUTTER, TribeRole.ELDER, TribeRole.FISHER};
        UUID group = UUID.randomUUID();
        for (int i = 0; i < 6; i++) {
            pose(person(level, roles[i], base.add(i * 1.4, 0, -1), null, group, false), standing[i]);
            pose(person(level, roles[(i + 3) % 6], base.add(i * 1.4, 0, 4), null, group, false), seated[i] | CampLife.SITTING);
        }
        result.add("idle rows, standing " + java.util.Arrays.toString(standing) + ", seated " + java.util.Arrays.toString(seated));
    }

    private static void pose(Tribesperson t, int idle) {
        t.setNoAi(true); // keeps the pose: CampLife only runs with AI on
        t.setYRot(180);
        t.yBodyRot = t.yHeadRot = 180;
        t.setIdle(idle);
    }

    private static void nightScene() {
        ServerLevel level = player().level();
        clear();
        cmd("time set 18000");
        cmd("effect give @a night_vision 600 0 true");
        BlockPos fire = BlockPos.containing(base);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        UUID camp = UUID.randomUUID();
        TribeRole[] roles = {TribeRole.GATHERER, TribeRole.ELDER, TribeRole.WOODCUTTER, TribeRole.FISHER, TribeRole.HUNTER};
        for (int i = 0; i < roles.length; i++) {
            double a = i * Math.PI * 2 / roles.length;
            person(level, roles[i], base.add(Math.cos(a) * 3, 0, Math.sin(a) * 3), fire, camp, i == 0).life.forcedPhase = CampLife.NIGHT;
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "tribeai_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private TribeAiShots() {}
}
