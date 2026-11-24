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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
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
 * Dev only (wagon cells, v0.202.0): with {@code wagonshots.flag} in the game directory (first line: output folder) the
 * client makes a flat world "WagonShots", parks wagons with 2, 4 and 6 chests, 4 chests with hay and a sheep, and an empty bed
 * with four cows, shoots wagon_*.png from behind and from above, and opens the combined chest screen of three of them.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class WagonShots {
    private static final String WORLD = "WagonShots";
    private static int stage = -1, ticks, step, ground;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>();
    private static ArmorStand camera;
    private static final WagonEntity[] wagons = new WagonEntity[6];
    private static final double[] X = {0, 8, 16, 24, 32, 40};

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "wagonshots.flag");
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
                    mc.options.renderDistance().set(8);
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
            if (stage == 1) {
                mc.options.pauseOnLostFocus = false;
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks < 80) return;
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                stage = 2;
                ticks = 0;
                step = 0;
                return;
            }
            if (camera != null) camera.setOldPosAndRot();
            if (step >= STEPS.size()) {
                if (ticks < 20) return;
                finish(mc);
                return;
            }
            Object[] s = STEPS.get(step);
            if (ticks < (int) s[0]) return;
            ((Runnable) s[1]).run();
            step++;
            ticks = 0;
        } catch (Throwable e) {
            result.add("error: " + e);
            for (StackTraceElement el : e.getStackTrace()) if (el.getClassName().startsWith("com.mechanical")) result.add("  at " + el);
            finish(mc);
        }
    }

    private static void finish(Minecraft mc) {
        try {
            Files.write(new File(out, "wagon_result.txt").toPath(), result);
        } catch (Exception ignored) {}
        stage = -2;
        mc.stop();
    }

    private static void then(int wait, Runnable r) {
        STEPS.add(new Object[] {wait, r});
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static ServerLevel level() {
        return mc().getSingleplayerServer().overworld();
    }

    private static ServerPlayer player() {
        return mc().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void server(Runnable r) {
        mc().getSingleplayerServer().execute(r);
    }

    private static void cmd(String c) {
        var server = mc().getSingleplayerServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void view(Vec3 at, Vec3 target) {
        Minecraft mc = mc();
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
        Screenshot.takeScreenshot(mc().gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "wagon_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
        result.add("shot " + name);
    }

    private static Vec3 at(double x, double y, double z) {
        return new Vec3(x, ground + y, z);
    }

    private static void put(WagonEntity w, Item item, int times) {
        ServerPlayer p = player();
        for (int i = 0; i < times; i++) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            w.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    /** Led by the player's lead and loaded the way a player does: a right-click with an empty hand. */
    private static void load(WagonEntity w, net.minecraft.world.entity.Mob animal) {
        player().teleportTo(level(), w.getX() + 3, w.getY(), w.getZ(), java.util.Set.of(), 0, 0, true);
        animal.setPos(w.position().add(1.8, 0, 0));
        animal.setLeashedTo(player(), true);
        w.interact(player(), InteractionHand.MAIN_HAND, Vec3.ZERO);
    }

    private static void fill(WagonEntity w) {
        for (int c = 0; c < WagonEntity.SLOTS; c++) if (w.module(c) == WagonEntity.CHEST) for (int i = 0; i < 27; i += 2) w.chest(c).setItem(i, new ItemStack(i % 4 == 0 ? Items.COBBLESTONE : Items.IRON_INGOT, 1 + (i + c * 7) % 60));
    }

    private static void script() {
        then(1, () -> server(() -> {
            for (String c : List.of("time set noon", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                    "gamerule spawn_mobs false", "difficulty peaceful"))
                cmd(c);
            ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            player().teleportTo(level(), 16, ground + 8, -14, java.util.Set.of(), 0, 20, true);
        }));
        then(40, () -> server(() -> {
            ServerLevel l = level();
            for (int i = 0; i < wagons.length; i++) {
                wagons[i] = Carting.WAGON.get().create(l, EntitySpawnReason.EVENT);
                wagons[i].setPos(at(X[i], 0.3, 0));
                wagons[i].setYRot(0); // facing +z
                l.addFreshEntity(wagons[i]);
            }
            put(wagons[0], Items.CHEST, 1);
            put(wagons[1], Items.CHEST, 2);
            put(wagons[2], Items.CHEST, 3);
            put(wagons[3], Items.CHEST, 4);
            put(wagons[3], Items.HAY_BLOCK, 1);
            put(wagons[3], Items.OAK_SLAB, 1);
            put(wagons[4], Items.CHEST, 6);
            put(wagons[5], Items.CHEST, 4);
            put(wagons[5], Items.HAY_BLOCK, 1);
            var sheep = EntityTypes.SHEEP.create(l, EntitySpawnReason.EVENT);
            sheep.setPos(wagons[5].position());
            l.addFreshEntity(sheep);
            load(wagons[5], sheep);
            for (int i = 0; i < 5; i++) fill(wagons[i]);
            CartEntity cart = Carting.CART.get().create(l, EntitySpawnReason.EVENT);
            cart.setPos(at(48, 0.3, 0));
            cart.setYRot(0);
            l.addFreshEntity(cart);
        }));
        then(60, () -> {});
        String[] names = {"1chest", "2chests", "3chests", "4chests_hay_seat", "6chests", "4chests_sheep_hay"};
        for (int i = 0; i < names.length; i++) {
            double cx = X[i];
            String n = names[i];
            then(2, () -> view(at(cx - 3.6, 3.6, -4.8), at(cx, 1.5, 0)));
            then(30, () -> shot(n + "_3q"));
            then(2, () -> view(at(cx + 0.01, 6.5, -2.2), at(cx, 1.2, 0)));
            then(30, () -> shot(n + "_top"));
            then(2, () -> view(at(cx - 1.4, 2.3, -3.2), at(cx, 1.3, 0)));
            then(30, () -> shot(n + "_close"));
        }
        // the hint box: the player's own view, the HUD on, aimed at the double chest of the 2-chest wagon and at a cart
        for (double[] aim : new double[][] {{X[1], 15}, {X[4], 15}, {48, 15}}) {
            then(2, () -> {
                mc().setCameraEntity(mc().player);
                if (mc().gui.hud.isHidden()) mc().gui.hud.toggle();
                server(() -> player().teleportTo(level(), aim[0], ground, -2.4, java.util.Set.of(), 0, (float) aim[1], true));
            });
            then(40, () -> shot("hint_" + (int) aim[0]));
        }
        then(2, () -> {
            if (!mc().gui.hud.isHidden()) mc().gui.hud.toggle();
        });
        for (int i : new int[] {0, 1, 4}) {
            int k = i;
            then(2, () -> server(() -> {
                player().teleportTo(level(), X[k] + 2.5, ground + 1, 0, java.util.Set.of(), 0, 0, true);
                player().openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> wagons[k].chestMenu(id, inv, 0), wagons[k].getDisplayName()));
            }));
            then(40, () -> shot("gui_" + names[k]));
            then(2, () -> server(() -> player().closeContainer()));
            then(10, () -> {});
        }
    }
}
