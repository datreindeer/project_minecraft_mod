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
        List<EntityType<?>> animals = List.of(EntityTypes.PIG, EntityTypes.COW, com.mechanical.wildlife.Wildlife.RED_DEER.get(),
                com.mechanical.wildlife.giants.GiantsFauna.WOOLLY_MAMMOTH.get());
        String[] names = {"pig", "cow", "red_deer", "mammoth"};
        for (int i = 0; i < animals.size(); i++) {
            EntityType<?> type = animals.get(i);
            String name = names[i];
            double x = i * 16 + 0.5;
            boolean huge = i == 3;
            // facing the body and walking backwards (yaw 180: it lies towards -z), then a second player joins
            then(10, () -> server(() -> {
                player().teleportTo(level(), x, ground, 1.5, java.util.Set.of(), 180, 20, true);
                body = carcass(type, new Vec3(x, ground, huge ? -1.5 : 0.5));
                CarcassHauling.pickUpOrLetGo(player(), body);
                if (huge) for (int n = 0; n < 3; n++) mate(new Vec3(x - 2.4 + n * 1.6, ground, 2.6 + (n % 2) * 0.6), 180);
            }));
            for (int k = 1; k <= 16; k++) { // walks on, the body coming after
                double z = 1.5 + k * 0.2;
                then(2, () -> server(() -> {
                    player().teleportTo(level(), x, ground, z, java.util.Set.of(), 180, 20, true);
                    walkMates(0.2);
                }));
            }
            then(30, () -> side(huge ? 8.0 : 4.2));
            then(15, () -> shot("drag_" + name + "_side"));
            then(5, () -> { // close on where the rope is tied
                Vec3 grip = ((CarcassEntity) Minecraft.getInstance().level.getEntity(body.getId())).gripPoint(0, 1.0F).add(0, 0.45, 0.2);
                view(grip.add(huge ? -3.5 : -1.7, 0.7, huge ? 1.2 : 0.6), grip);
            });
            then(15, () -> shot("drag_" + name + "_close"));
            if (i == 0) { // his own view, looking down at it
                then(5, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    camera = null;
                    mc.setCameraEntity(mc.player);
                    mc.player.setYRot(180);
                    mc.player.setXRot(55);
                });
                then(15, () -> shot("drag_pig_first_person"));
            }
            if (!huge) {
                then(5, () -> server(() -> mate(player().position().add(1.2, 0, 0.3), 180)));
                then(30, () -> side(4.2));
                then(15, () -> shot("drag_" + name + "_two_side"));
                then(5, () -> view(body().add(-3.2, 3.0, 3.6), body().add(0, 0.5, 0.8)));
                then(15, () -> shot("drag_" + name + "_two_high"));
                then(5, () -> server(VehfixShots::dismiss));
                // walking off with it behind him
                for (int k = 1; k <= 12; k++) then(2, () -> server(() -> player().teleportTo(level(), x, ground, player().getZ() + 0.2, java.util.Set.of(), 0, 20, true)));
                then(40, () -> side(4.2));
                then(15, () -> shot("drag_" + name + "_behind_side"));
            } else {
                then(5, () -> view(body().add(-6.0, 5.0, 6.5), body().add(0, 0.8, 1.0)));
                then(15, () -> shot("drag_" + name + "_high"));
            }
            then(10, () -> server(() -> {
                CarcassHauling.letGo(body, player(), null);
                dismiss();
                body.discard();
            }));
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
        then(80, () -> {}); // swung all the way round
        // dead from the side and from above: the pole runs along the horses to the yoke
        then(2, () -> {
            var h = Minecraft.getInstance().player.getVehicle();
            Vec3 ahead = Vec3.directionFromRotation(0, h.getYRot()), right = Vec3.directionFromRotation(0, h.getYRot() + 90);
            Vec3 mid = h.position().subtract(ahead.scale(1.4));
            view(mid.add(right.scale(6.0)).add(0, 1.5, 0), mid.add(0, 0.9, 0));
        });
        then(15, () -> shot("hitched_side"));
        then(2, () -> {
            var h = Minecraft.getInstance().player.getVehicle();
            Vec3 ahead = Vec3.directionFromRotation(0, h.getYRot()), right = Vec3.directionFromRotation(0, h.getYRot() + 90);
            Vec3 mid = h.position().subtract(ahead.scale(1.2)).add(right.scale(0.7));
            view(mid.add(0, 6.0, 0).subtract(ahead.scale(0.6)), mid.add(0, 0.5, 0));
        });
        then(15, () -> shot("hitched_above"));
        // a straight team: from the side, then with the near horse hidden to see the pole run along the far one to the yoke
        Vec3 w = new Vec3(30.5, 0, 20.5);
        List<AbstractHorse> pair = new ArrayList<>();
        then(2, () -> server(() -> {
            WagonEntity straight = Carting.WAGON.get().create(level(), EntitySpawnReason.EVENT);
            straight.setPos(w.add(0, ground, 0));
            straight.setYRot(0);
            level().addFreshEntity(straight);
            for (int i = 0; i < 2; i++) {
                AbstractHorse h = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level(), EntitySpawnReason.EVENT);
                h.setPos(w.add((i * 2 - 1) * WagonEntity.PAIR / 2, ground, WagonEntity.LEAD));
                h.setYRot(0);
                h.setTamed(true);
                h.setNoAi(true);
                level().addFreshEntity(h);
                pair.add(h);
            }
            straight.setHarness(pair.get(0));
            straight.setPartner(pair.get(1));
        }));
        then(40, () -> view(new Vec3(36.5, ground + 1.8, 22.0), new Vec3(30.5, ground + 0.9, 22.0)));
        then(15, () -> shot("pole_side"));
        then(2, () -> server(() -> pair.get(1).setInvisible(true)));
        then(20, () -> shot("pole_side_near_horse_hidden"));
    }

    /** The other pullers: a server-side stand-in holds the body, the client is given a player with its id to draw. */
    private static final List<net.neoforged.neoforge.common.util.FakePlayer> mates = new ArrayList<>();

    /** Server. */
    private static void mate(Vec3 at, float yaw) {
        var m = new net.neoforged.neoforge.common.util.FakePlayer(level(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Mate" + mates.size()));
        m.snapTo(at.x, at.y, at.z, yaw, 0);
        body.addHauler(m);
        mates.add(m);
        int id = m.getId();
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            var r = new net.minecraft.client.player.RemotePlayer(mc.level, m.getGameProfile());
            r.setId(id);
            place(r, at, yaw);
            mc.level.addEntity(r);
        });
    }

    /** Client. */
    private static void place(net.minecraft.world.entity.LivingEntity r, Vec3 at, float yaw) {
        r.snapTo(at.x, at.y, at.z, yaw, 0);
        r.setYBodyRot(yaw);
        r.setYHeadRot(yaw);
        r.yBodyRotO = r.yHeadRotO = yaw;
        r.setOldPosAndRot();
    }

    /** Server: the others walk on {@code dz}. */
    private static void walkMates(double dz) {
        for (var m : mates) {
            m.setPos(m.getX(), m.getY(), m.getZ() + dz);
            Vec3 at = m.position();
            float yaw = m.getYRot();
            int id = m.getId();
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.level.getEntity(id) instanceof net.minecraft.world.entity.LivingEntity r) place(r, at, yaw);
            });
        }
    }

    /** Server. */
    private static void dismiss() {
        for (var m : mates) {
            CarcassHauling.letGo(body, m, null);
            int id = m.getId();
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.level.removeEntity(id, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED));
        }
        mates.clear();
    }

    private static Vec3 body() {
        return Minecraft.getInstance().level.getEntity(body.getId()).position();
    }

    /** From the side (+x), level with the grip. */
    private static void side(double away) {
        Vec3 mid = body().add(0, 0.7, 0.8);
        view(mid.add(-away, 0.5, 0), mid); // the legs are on its -x side
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
