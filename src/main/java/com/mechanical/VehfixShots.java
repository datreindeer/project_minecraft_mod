package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
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
 * Dev only (like {@link WagonShots}): with a file {@code vehfixshots.flag} in the game directory (its first line: an
 * output folder), the client makes a flat world, drags a pig, a cow and a red deer, rides a horse up to a wagon (the
 * attach hint), hitches it and swings the rider round, takes screenshots (vehfix_*.png) and quits.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class VehfixShots {
    private static final String WORLD = "VehfixShots";
    private static int stage = -1, ticks, step, ground;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>();
    private static ArmorStand camera;
    private static CarcassEntity body;
    private static AbstractHorse horse;
    private static WagonEntity wagon;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "vehfixshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
                out.mkdirs();
                File dir = new File(mc.gameDirectory, "saves/" + WORLD);
                if (dir.exists()) try (var walk = Files.walk(dir.toPath())) {
                    walk.sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
                }
                script();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.options.renderDistance().set(6);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(1234L, false, false),
                            reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new TitleScreen());
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            mc.options.pauseOnLostFocus = false;
            if (stage == 1) {
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks < 80) return;
                stage = 2;
                ticks = 0;
            }
            if (camera != null) camera.setOldPosAndRot();
            if (step >= STEPS.size()) {
                if (ticks >= 20) finish(mc);
                return;
            }
            Object[] s = STEPS.get(step);
            if (ticks < (int) s[0]) return;
            ((Runnable) s[1]).run();
            step++;
            ticks = 0;
        } catch (Throwable e) {
            result.add("error: " + e);
            finish(mc);
        }
    }

    private static void script() {
        then(1, () -> server(() -> {
            for (String c : List.of("time set noon", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                    "gamerule spawn_mobs false", "kill @e[type=!player]"))
                cmd(c);
            ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            player().teleportTo(level(), 0.5, ground, 0.5, java.util.Set.of(), 0, 20, true);
        }));
        List<EntityType<?>> animals = List.of(EntityTypes.PIG, EntityTypes.COW, com.mechanical.wildlife.Wildlife.RED_DEER.get());
        String[] names = {"pig", "cow", "red_deer"};
        for (int i = 0; i < 3; i++) {
            EntityType<?> type = animals.get(i);
            String name = names[i];
            double x = i * 12;
            then(10, () -> server(() -> {
                player().teleportTo(level(), x + 0.5, ground, 1.5, java.util.Set.of(), 180, 35, true);
                body = carcass(type, new Vec3(x + 0.5, ground, 0.5));
                CarcassHauling.pickUpOrLetGo(player(), body);
            }));
            for (int k = 1; k <= 16; k++) { // walks on, the body coming after
                double z = 1.5 + k * 0.2;
                then(2, () -> server(() -> player().teleportTo(level(), x + 0.5, ground, z, java.util.Set.of(), 180, 35, true)));
            }
            then(20, () -> view(new Vec3(x + 4.0, ground + 2.4, 3.6), new Vec3(x + 0.5, ground + 0.6, 3.4)));
            then(15, () -> shot("drag_" + name));
            then(10, () -> server(() -> CarcassHauling.letGo(body, player(), null)));
        }
        then(10, () -> server(() -> {
            ServerPlayer p = player();
            wagon = Carting.WAGON.get().create(level(), EntitySpawnReason.EVENT);
            wagon.setPos(new Vec3(0.5, ground, 20.5));
            wagon.setYRot(0);
            level().addFreshEntity(wagon);
            horse = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level(), EntitySpawnReason.EVENT);
            horse.setPos(new Vec3(2.7, ground, 23.3));
            horse.setYRot(0);
            horse.setTamed(true);
            level().addFreshEntity(horse);
            p.setGameMode(GameType.SURVIVAL);
            p.teleportTo(level(), 2.7, ground, 23.3, java.util.Set.of(), 140, 25, true);
            p.startRiding(horse, true, true);
        }));
        then(5, () -> {
            camera = null;
            Minecraft mc = Minecraft.getInstance();
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.setYRot(140);
            mc.player.setXRot(25);
        });
        then(30, () -> shot("saddle_attach_hint"));
        then(10, () -> server(() -> {
            TravoisEntity.grabKey(player());
            result.add("hitched " + (wagon.harness() == horse));
            AbstractHorse second = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level(), EntitySpawnReason.EVENT);
            second.setPos(horse.position().add(-WagonEntity.PAIR, 0, 0));
            second.setTamed(true);
            level().addFreshEntity(second);
            wagon.setPartner(second);
        }));
        then(5, () -> { // the rider swings round: the horse comes after at the wagon's pace
            Minecraft.getInstance().player.setYRot(-90);
        });
        then(20, () -> result.add("after 20 ticks the horse's yaw (client) " + Minecraft.getInstance().player.getVehicle().getYRot() + " (rider -90, started at 0)"));
        then(2, () -> {
            Vec3 at = Minecraft.getInstance().player.getVehicle().position();
            view(at.add(4.5, 5.0, 4.5), at.add(0, 0.5, 0));
        });
        then(10, () -> shot("hitched_turning"));
    }

    private static CarcassEntity carcass(EntityType<?> type, Vec3 at) {
        var dead = (LivingEntity) type.create(level(), EntitySpawnReason.EVENT);
        dead.setPos(at);
        CarcassEntity c = CarcassEntity.of(level(), dead);
        level().addFreshEntity(c);
        return c;
    }

    private static void finish(Minecraft mc) {
        try {
            Files.write(new File(out, "vehfix_result.txt").toPath(), result);
        } catch (Exception ignored) {}
        stage = -2;
        mc.stop();
    }

    private static void then(int wait, Runnable r) {
        STEPS.add(new Object[] {wait, r});
    }

    private static ServerLevel level() {
        return Minecraft.getInstance().getSingleplayerServer().overworld();
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void server(Runnable r) {
        Minecraft.getInstance().getSingleplayerServer().execute(r);
    }

    private static void cmd(String c) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
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

    private static void shot(String name) {
        Screenshot.takeScreenshot(Minecraft.getInstance().gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "vehfix_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
        result.add("shot " + name);
    }

    private VehfixShots() {}
}
