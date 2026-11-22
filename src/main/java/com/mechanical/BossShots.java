package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.client.wildlife.ElkwightRenderer;
import com.mechanical.wildlife.elkwight.Elkwight;
import com.mechanical.wildlife.elkwight.ElkwightAxeEntity;
import com.mechanical.wildlife.elkwight.Elkwights;
import com.mechanical.wildlife.elkwight.AntlerCrownBlock;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Dev only (like LogShots): with a file {@code bossshots.flag} in the game directory (its first line: an output folder),
 * the client opens the world "BossShots", builds a platform at y 200 and takes screenshots (boss_*.png) of the Elkwight
 * from the front and side, in each attack's telegraph pose, his glowing eyes at night, his carcass, the heavy axe and the
 * two trophies; then it quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BossShots {
    private record Step(String shot, int delay, Runnable server) {}

    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static final List<Step> steps = new ArrayList<>();
    private static Vec3 base;
    private static Elkwight boss;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "bossshots.flag");
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
                    mc.createWorldOpenFlows().openWorld("BossShots", () -> {});
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
                if (mc.player.isDeadOrDying()) mc.player.respawn();
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks < 100) return;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(BossShots::setup);
                plan();
                stage = 2;
                ticks = 0;
                return;
            }
            int i = stage - 2;
            if (i >= steps.size()) {
                if (ticks < 20) return;
                Files.write(new File(out, "boss_result.txt").toPath(), result);
                stage = -2;
                mc.stop();
                return;
            }
            Step s = steps.get(i);
            if (ticks == 1) server.execute(s.server());
            if (ticks == 2) mc.gui.hud.getChat().clearMessages(false);
            if (ticks < s.delay()) return;
            if (s.shot() != null) shot(mc, s.shot());
            if ("boss_front".equals(s.shot()) && !mc.gui.hud.isHidden()) mc.gui.hud.toggle(); // the rest without the HUD
            stage++;
            ticks = 0;
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "boss_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void pose(int action, float progress) {
        boss.showAction(action);
        ElkwightRenderer.shotProgress = progress;
    }

    private static void plan() {
        steps.add(new Step(null, 80, () -> look(base.add(0.5, 2.4, -8.5), 0, 6))); // chunks round the platform load
        steps.add(new Step("boss_front", 30, () -> pose(Elkwight.IDLE, 0)));
        steps.add(new Step("boss_side", 30, () -> look(base.add(-9.0, 2.4, 0.5), -90, 6)));
        steps.add(new Step("boss_chop", 30, () -> {
            look(base.add(-6.0, 2.6, -6.0), -45, 8);
            pose(Elkwight.CHOP_WIND, 0.7F);
        }));
        steps.add(new Step("boss_spin", 30, () -> pose(Elkwight.SPIN_WIND, 0.55F)));
        steps.add(new Step("boss_sweep", 30, () -> pose(Elkwight.SWEEP_WIND, 0.5F)));
        steps.add(new Step("boss_throw", 30, () -> {
            look(base.add(-9.0, 2.4, 0.5), -90, 6);
            pose(Elkwight.THROW_WIND, 0.9F);
        }));
        steps.add(new Step("boss_wave", 30, () -> {
            look(base.add(-6.0, 2.6, -6.0), -45, 8);
            pose(Elkwight.WAVE_WIND, 0.95F);
        }));
        steps.add(new Step("boss_eat", 30, () -> pose(Elkwight.EAT, 0.5F)));
        steps.add(new Step("boss_night", 40, () -> {
            cmd("time set 18000");
            look(base.add(0.5, 3.0, -9.5), 0, 8);
            pose(Elkwight.IDLE, 0);
        }));
        steps.add(new Step(null, 10, () -> cmd("time set 6000")));
        steps.add(new Step("boss_carcass", 40, () -> {
            ServerLevel level = player().level();
            CarcassEntity body = CarcassEntity.of(level, boss);
            body.setPos(base.add(0, 0, 0));
            body.setYRot(90);
            boss.discard();
            level.addFreshEntity(body);
            ElkwightAxeEntity.dropAt(level, base.add(6.5, 0, 4.0), 30);
            look(base.add(-5.0, 4.5, -5.0), -45, 35);
        }));
        steps.add(new Step("boss_axe", 30, () -> look(base.add(6.5, 3.2, 1.0), 0, 50)));
        steps.add(new Step("boss_trophies", 40, () -> {
            ServerLevel level = player().level();
            BlockPos t = BlockPos.containing(base.add(8, 0, 0));
            for (int x = -3; x <= 3; x++) for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(t.offset(x, y, 3), Blocks.SPRUCE_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(t, Elkwights.AXE_TROPHY.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH));
            level.setBlockAndUpdate(t.offset(0, 3, 2), Elkwights.ANTLER_CROWN.get().defaultBlockState().setValue(AntlerCrownBlock.FACING, Direction.NORTH));
            look(Vec3.atBottomCenterOf(t).add(0, 1.8, -5.5), 0, 5);
        }));
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

    /** Server: a grass platform at y 199 far from spawn, the Elkwight in the middle, awake but held still. */
    private static void setup() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        base = new Vec3(Math.floor(p.getX()) + 4000.5, 200, Math.floor(p.getZ()) + 0.5);
        cmd("gamemode creative @a");
        cmd("time set 6000");
        cmd("weather clear");
        cmd("gamerule advance_time false");
        look(base.add(0.5, 2.4, -8.5), 0, 6);
        int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
        cmd("forceload add " + (x - 16) + " " + (z - 16) + " " + (x + 16) + " " + (z + 16));
        cmd("fill " + (x - 14) + " 199 " + (z - 14) + " " + (x + 14) + " 199 " + (z + 14) + " grass_block");
        cmd("fill " + (x - 14) + " 200 " + (z - 14) + " " + (x + 14) + " 212 " + (z + 14) + " air");
        boss = Elkwights.ELKWIGHT.get().create(level, EntitySpawnReason.COMMAND);
        boss.setPos(base);
        boss.setYRot(180);
        boss.setYHeadRot(180);
        boss.yBodyRot = 180;
        boss.setNoAi(true);
        level.addFreshEntity(boss);
        boss.wake();
        result.add("boss at " + base + " health " + boss.getHealth());
    }

    private static void shot(Minecraft mc, String name) {
        mc.gui.hud.getChat().clearMessages(false);
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
        result.add("shot " + name);
    }

    private BossShots() {}
}
