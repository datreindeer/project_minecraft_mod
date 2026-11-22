package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link LogShots}): with a file {@code coldshots.flag} in the game directory (its first line: an output
 * folder), the client opens the world "ColdShots", builds a snow platform at y 200 and photographs each cold animal
 * (wildlife.cold) standing still in a three-quarter view (the pika beside its hay pile): cold_&lt;name&gt;.png, then writes
 * cold_result.txt and quits.
 * The arctic fox is shot twice, on a winter day and a summer day of the calendar (its coat follows it). Does nothing
 * without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ColdShots {
    /** name, type, baby, camera distance, day of the year (-1: leave the calendar). */
    private record Subject(String name, Supplier<? extends EntityType<?>> type, boolean baby, double distance, int dayOfYear) {}

    private static final List<Subject> SUBJECTS = List.of(
            new Subject("polar_bear", com.mechanical.wildlife.cold.ColdFauna.POLAR_BEAR, false, 5.5, -1),
            new Subject("polar_bear_cub", com.mechanical.wildlife.cold.ColdFauna.POLAR_BEAR, true, 3.0, -1),
            new Subject("arctic_fox_winter", com.mechanical.wildlife.cold.ColdFauna.ARCTIC_FOX, false, 1.9, 300),
            new Subject("arctic_fox_summer", com.mechanical.wildlife.cold.ColdFauna.ARCTIC_FOX, false, 1.9, 120),
            new Subject("snow_leopard", com.mechanical.wildlife.cold.ColdFauna.SNOW_LEOPARD, false, 4.0, -1),
            new Subject("siberian_tiger", com.mechanical.wildlife.cold.ColdFauna.SIBERIAN_TIGER, false, 5.5, -1),
            new Subject("snowy_owl", com.mechanical.wildlife.cold.ColdFauna.SNOWY_OWL, false, 2.2, -1),
            new Subject("black_grouse", com.mechanical.wildlife.cold.ColdFauna.BLACK_GROUSE, false, 2.0, -1),
            new Subject("lammergeier", com.mechanical.wildlife.cold.ColdFauna.LAMMERGEIER, false, 3.2, -1),
            new Subject("pika", com.mechanical.wildlife.cold.ColdFauna.PIKA, false, 1.6, 150));

    private static int stage = -1, ticks, index;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static Entity current;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "coldshots.flag");
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
                    mc.createWorldOpenFlows().openWorld("ColdShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> {
                    mc.options.pauseOnLostFocus = false;
                    if (mc.player.isDeadOrDying()) mc.player.respawn();
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() + 600, 205, p.getZ(), java.util.Set.of(), 0, 20, true); // load the chunks there first
                    });
                    stage = 5;
                    ticks = 0;
                }
                case 2 -> { // place the next subject
                    if (ticks < 40) return;
                    if (index >= SUBJECTS.size()) {
                        stage = 4;
                        ticks = 0;
                        return;
                    }
                    Subject s = SUBJECTS.get(index);
                    server.execute(() -> place(server, s));
                    stage = 3;
                    ticks = 0;
                }
                case 3 -> { // let the coat settle and the chunk render, then shoot
                    if (ticks < 130) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name());
                    result.add(s.name() + ": " + current + (current instanceof com.mechanical.wildlife.cold.ArcticFox f ? " white=" + f.isWhite() : ""));
                    index++;
                    stage = 2;
                    ticks = 0;
                }
                case 5 -> { // build once the chunks over there are loaded
                    if (ticks < 80) return;
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() - 600, p.getY(), p.getZ(), java.util.Set.of(), 0, 20, true);
                        setup();
                    });
                    stage = 2;
                    ticks = 0;
                }
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "cold_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "cold_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    /** A 25x25 snow platform at y 200, 600 blocks east of the spawn (away from what the world holds), clear sky above. */
    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()) + 600, z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 201, z + 0.5);
        cmd("gamemode creative @a");
        p.getAbilities().flying = true; // hangs where it is put, as the camera
        p.onUpdateAbilities();
        cmd("gamerule doDaylightCycle false");
        cmd("weather clear");
        cmd("difficulty peaceful");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 200 " + (z + 12) + " snow_block");
        cmd("fill " + (x - 12) + " 201 " + (z - 12) + " " + (x + 12) + " 215 " + (z + 12) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static void place(MinecraftServer server, Subject s) {
        ServerLevel level = player().level();
        if (current != null) current.discard();
        // the pika beside its hay pile; nothing there for the others
        net.minecraft.core.BlockPos pile = net.minecraft.core.BlockPos.containing(base).east();
        level.setBlockAndUpdate(pile, s.name().equals("pika")
                ? com.mechanical.wildlife.cold.ColdFauna.PIKA_HAY_PILE.get().defaultBlockState().setValue(com.mechanical.wildlife.GuanoPileBlock.LAYERS, 3)
                : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        if (s.dayOfYear() >= 0) Calendar.overworldClock(server).ifPresent(c ->
                server.clockManager().setTotalTicks(c, Calendar.clockOn(level.getOverworldClockTime(), s.dayOfYear()) / Calendar.CLOCK_DAY * Calendar.CLOCK_DAY + 6000));
        Entity e = s.type().get().create(level, EntitySpawnReason.COMMAND);
        if (e == null) return;
        float yaw = 225; // head to the north-east: from the camera (north-west of it) a three-quarter view of its left side
        e.snapTo(base.x, base.y, base.z, yaw, 0);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setPersistenceRequired();
        }
        if (s.baby() && e instanceof AgeableMob a) a.setBaby(true);
        level.addFreshEntity(e);
        current = e;
        // camera to the south-east of it, a little above, looking at its middle
        Vec3 mid = base.add(0, e.getBbHeight() * 0.5, 0);
        Vec3 eye = mid.add(new Vec3(-0.55, 0.35, -0.75).normalize().scale(s.distance()));
        Vec3 d = mid.subtract(eye);
        float camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float camPitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        ServerPlayer p = player();
        p.teleportTo(level, eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), camYaw, camPitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "cold_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private ColdShots() {}
}
