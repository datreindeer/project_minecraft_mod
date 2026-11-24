package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.Equipment;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with {@code packshots.flag} in the game directory (first line: output folder) the client makes a flat world
 * "PackShots", puts the Steel-Frame Pack on the player (bare, then over an iron chestplate) and shoots pack_*.png from
 * behind, the side and the front; then quits. Nothing happens without the flag file. Pattern: BugShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class PackShots {
    private static final String WORLD = "PackShots";
    private static int stage = -1, ticks, step;
    private static File out;
    private static ArmorStand camera;
    private static Vec3 body;
    private static boolean armored, aimed;
    private static final String[] NAMES = {"back", "side", "front", "armored_back", "armored_side"};

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "packshots.flag");
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
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(77L, false, false),
                            reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new TitleScreen());
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
                if (ticks < 80) return;
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                server.execute(() -> {
                    for (String c : List.of("time set noon", "gamerule advance_time false", "weather clear")) {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
                    }
                    ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                    int ground = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
                    p.teleportTo(p.level(), 0.5, ground, 0.5, java.util.Set.of(), 0, 0, true);
                    Equipment.of(p).set(Accessory.BACK, new ItemStack(Satchels.STEEL_FRAME_PACK.get()));
                    body = new Vec3(0.5, ground + 1.0, 0.5);
                });
                stage = 2;
                ticks = 0;
                return;
            }
            if (ticks < 40) return;
            if (step == 3 && !armored) { // now over chest armour
                armored = true;
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                server.execute(() -> p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)));
                ticks = 0;
                return;
            }
            if (step >= NAMES.length) {
                mc.stop();
                return;
            }
            if (!aimed) {
                aim(step);
                aimed = true;
                ticks = 0;
                return;
            }
            shotFor(mc, step);
            aimed = false;
            step++;
            ticks = 0;
        } catch (Throwable e) {
            e.printStackTrace();
            mc.stop();
        }
    }

    private static void aim(int i) {
        double[][] dir = {{0, -1.9}, {-1.9, 0}, {0, 1.9}, {0, -1.9}, {-1.9, 0}};
        view(body.add(dir[i][0], 0.5, dir[i][1]), body.add(0, 0.3, 0));
    }

    private static void shotFor(Minecraft mc, int i) {
        // views: 0 back, 1 side, 2 front, then armored: 3 back, 4 side
        double[][] dir = {{0, -1.9}, {-1.9, 0}, {0, 1.9}, {0, -1.9}, {-1.9, 0}};
        int idx = i;
        view(body.add(dir[idx][0], 0.6, dir[idx][1]), body.add(0, 0.2, 0));
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "pack_" + NAMES[idx] + ".png"));
            } catch (Exception ignored) {}
        });
    }

    private static void view(Vec3 at, Vec3 target) {
        Minecraft mc = Minecraft.getInstance();
        if (camera == null) {
            camera = new ArmorStand(EntityTypes.ARMOR_STAND, mc.level);
            camera.setInvisible(true);
        }
        Vec3 d = target.subtract(at);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()));
        camera.setPos(at.x, at.y - camera.getEyeHeight(), at.z);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.setYHeadRot(yaw);
        camera.setOldPosAndRot();
        mc.setCameraEntity(camera);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private PackShots() {}
}
