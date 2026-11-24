package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (wagon tilt, v0.195.0): with {@code tiltshots.flag} in the game directory (first line: output folder) the
 * client makes a flat world "TiltShots" with a hillside road of grass and path slabs (half a block up per block along x)
 * and a side slope (half a block per two blocks across z), parks wagons and carts on them (a team, three logs, a caravan
 * tilt with its load, a cart with a chest), then gallops a wagon up the road, shooting tilt_*.png from the side and
 * writing tilt_result.txt (each bed's pitch, roll and how far its wheels are off the ground).
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TiltShots {
    private static final String WORLD = "TiltShots";
    private static int stage = -1, ticks, step;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>(); // {wait ticks, Runnable}
    private static ArmorStand camera;
    private static int ground;
    private static WagonEntity teamWagon, logWagon, caravanWagon, driveWagon;
    private static CartEntity horseCart, chestCart;
    private static AbstractHorse driveLead;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "tiltshots.flag");
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
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
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
            Files.write(new File(out, "tilt_result.txt").toPath(), result);
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

    private static String f(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
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
                image.writeToFile(new File(out, "tilt_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
        result.add("shot " + name);
    }

    private static Vec3 at(double x, double y, double z) {
        return new Vec3(x, ground + y, z);
    }

    /** Half-steps of ground at a column: the hill road along x, the side slope across z further on. */
    private static int halves(int x, int z) {
        if (z >= 30) return Math.max(0, Math.min(8, (z - 33) / 2));
        return Math.max(0, Math.min(12, x - 4));
    }

    private static void column(ServerLevel l, int x, int z, boolean road) {
        int n = halves(x, z);
        BlockState full = road ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        for (int k = 0; k < n / 2; k++) l.setBlock(new BlockPos(x, ground + k, z), k == n / 2 - 1 && n % 2 == 0 ? full : Blocks.DIRT.defaultBlockState(), 2);
        if (n % 2 == 1) {
            if (n > 1) l.setBlock(new BlockPos(x, ground + n / 2 - 1, z), Blocks.DIRT.defaultBlockState(), 2);
            l.setBlock(new BlockPos(x, ground + n / 2, z), (road ? EarthBlocks.DIRT_PATH_SLAB : EarthBlocks.GRASS_SLAB).get().defaultBlockState(), 2);
        }
    }

    /** Ground surface height (blocks above the flat ground) at a point. */
    private static double surface(double x, double z) {
        return halves((int) Math.floor(x), (int) Math.floor(z)) * 0.5;
    }

    private static AbstractHorse horse(ServerLevel l, Vec3 at, float yaw) {
        AbstractHorse h = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(l, EntitySpawnReason.EVENT);
        h.setPos(at);
        h.setYRot(yaw);
        h.setYBodyRot(yaw);
        h.setYHeadRot(yaw);
        h.setTamed(true);
        h.setPersistenceRequired();
        l.addFreshEntity(h);
        return h;
    }

    private static WagonEntity wagon(ServerLevel l, double x, double z, float yaw, boolean team) {
        WagonEntity w = Carting.WAGON.get().create(l, EntitySpawnReason.EVENT);
        Vec3 p = at(x, surface(x, z) + 1.2, z);
        w.setPos(p);
        w.setYRot(yaw);
        l.addFreshEntity(w);
        if (team) {
            Vec3 ahead = Vec3.directionFromRotation(0, yaw).scale(WagonEntity.LEAD), side = Vec3.directionFromRotation(0, yaw + 90).scale(WagonEntity.PAIR / 2);
            Vec3 a = p.add(ahead).subtract(side), b = p.add(ahead).add(side);
            AbstractHorse lead = horse(l, at(a.x, surface(a.x, a.z) + 0.2, a.z), yaw), partner = horse(l, at(b.x, surface(b.x, b.z) + 0.2, b.z), yaw);
            lead.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Carting.HIDE_SADDLE_BLANKET.get()));
            w.setHarness(lead);
            w.setPartner(partner);
            if (driveLead == null && x < 4) driveLead = lead;
        }
        return w;
    }

    /** On this client: the bed's tilt and how far each wheel's bottom is above (+) or in (-) the ground. */
    private static void report(String name, TravoisEntity server) {
        if (!(mc().level.getEntity(server.getId()) instanceof TravoisEntity t)) {
            result.add(name + ": not on the client");
            return;
        }
        double along = t instanceof WagonEntity ? WagonEntity.FRONT_AXLE : 0, side = t instanceof WagonEntity ? WagonEntity.WHEEL_SIDE : CartEntity.WHEEL_SIDE;
        double pa = t instanceof CartEntity ? -CartEntity.AXLE_BACK : 0, pu = t instanceof CartEntity ? CartEntity.AXLE_HEIGHT : 0;
        StringBuilder wheels = new StringBuilder();
        for (int a = t instanceof WagonEntity ? -1 : 0; a <= (t instanceof WagonEntity ? 1 : 0); a += 2) // wagon: rear, front; cart: its axle
            for (int sd = -1; sd <= 1; sd += 2) {
                double al = t instanceof WagonEntity ? a * along : -CartEntity.AXLE_BACK;
                wheels.append(" ").append(f(t.onBed(al, sd * side, 0, pa, pu).y - groundAt(t, al, sd * side)));
            }
        result.add(name + ": pitch " + f(t.pitch) + " roll " + f(t.roll) + " entity y " + f(t.getY() - ground) + " rests at " + f(t.groundY - ground)
                + "; wheel bottoms above ground:" + wheels);
    }

    /** Where a wheel's bottom would rest right now (as TravoisEntity#groundUnder, without its look-ahead). */
    private static double groundAt(TravoisEntity t, double along, double side) {
        double r = t instanceof WagonEntity ? WagonEntity.WHEEL_RADIUS : CartEntity.WHEEL_RADIUS, reach = r * 0.6, curve = r - Math.sqrt(r * r - reach * reach), best = -999;
        Vec3 dir = Vec3.directionFromRotation(0, t.getYRot());
        Vec3 hub = t.position().add(dir.scale(along)).add(Vec3.directionFromRotation(0, t.getYRot() + 90).scale(side));
        for (int k = -1; k <= 1; k++) {
            Vec3 p = hub.add(dir.scale(k * reach));
            best = Math.max(best, t.level().clip(new net.minecraft.world.level.ClipContext(p.add(0, 1, 0), p.add(0, -1, 0), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, t)).getLocation().y - (k == 0 ? 0 : curve));
        }
        return best;
    }

    private static void script() {
        then(1, () -> server(() -> {
            for (String c : List.of("time set noon", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                    "gamerule spawn_mobs false", "difficulty peaceful"))
                cmd(c);
            ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            ServerLevel l = level();
            for (int x = -6; x <= 30; x++) for (int z = -12; z <= 46; z++) column(l, x, z, z >= -1 && z <= 1 || z >= 30 && x >= 8 && x <= 10);
            player().teleportTo(l, 8, ground + 8, -10, java.util.Set.of(), 0, 20, true);
        }));
        then(40, () -> server(() -> {
            ServerLevel l = level();
            teamWagon = wagon(l, 9.5, 0.5, -90, true); // up the road, the team ahead on the slope
            logWagon = wagon(l, 14.5, 10.5, 90, false); // down the slope, three logs
            caravanWagon = wagon(l, 9.5, 37.5, -90, false); // across the side slope, a caravan's tilt and load
            caravanWagon.setCanopy("woodsman");
            caravanWagon.setCargo(List.of("minecraft:oak_log", "minecraft:barrel", "minecraft:spruce_log", "minecraft:barrel", "minecraft:iron_axe", "minecraft:oak_log", "minecraft:hay_block", "minecraft:barrel"));
            driveWagon = wagon(l, -3.5, -5.5, -90, true);
            for (int k = 0; k < 3; k++) {
                FelledLogEntity log = FelledLogEntity.spawnStraight(l, at(14.5, 10, 14.5 + k * 2), 0, 3, Blocks.SPRUCE_LOG.defaultBlockState());
                log.startRiding(logWagon, true, true);
            }
            horseCart = Carting.CART.get().create(l, EntitySpawnReason.EVENT);
            horseCart.setPos(at(10.5, surface(10.5, -9.5) + 1, -9.5));
            horseCart.setYRot(-90);
            l.addFreshEntity(horseCart);
            Vec3 h = horseCart.position().add(Vec3.directionFromRotation(0, -90).scale(CartEntity.LEAD_HORSE));
            horseCart.setHarness(horse(l, at(h.x, surface(h.x, h.z) + 0.2, h.z), -90));
            chestCart = Carting.CART.get().create(l, EntitySpawnReason.EVENT);
            chestCart.setPos(at(16.5, surface(16.5, 38.5) + 1, 38.5));
            chestCart.setYRot(-90);
            chestCart.setCanopy("hunting");
            l.addFreshEntity(chestCart);
        }));
        then(80, () -> {
            report("team wagon (uphill)", teamWagon);
            report("log wagon (downhill)", logWagon);
            report("caravan wagon (side slope)", caravanWagon);
            report("horse cart (uphill)", horseCart);
            report("canopy cart (side slope)", chestCart);
            view(at(9.5, 4.5, -9), at(10.5, 3.2, 0.5));
        });
        then(30, () -> shot("team_side"));
        then(2, () -> view(at(4, 4.5, -4), at(10, 3.2, 0.5)));
        then(30, () -> shot("team_three_quarter"));
        then(2, () -> view(at(10, 5.5, -14), at(11, 3.8, -9.5)));
        then(30, () -> shot("cart_side"));
        then(2, () -> view(at(12.5, 6.2, -2.5), at(12, 4.6, 0.5)));
        then(30, () -> shot("team_pole"));
        then(2, () -> view(at(14.5, 8.5, 4), at(14.5, 6, 10.5)));
        then(30, () -> shot("logs_side"));
        then(2, () -> view(at(19, 9.5, 6), at(14.5, 6, 10.5)));
        then(30, () -> shot("logs_three_quarter"));
        then(2, () -> view(at(1, 2.5, 37.5), at(9.5, 1.5, 37.5)));
        then(30, () -> shot("caravan_front"));
        then(2, () -> view(at(9.5, 3.5, 30), at(9.5, 2, 37.5)));
        then(30, () -> shot("caravan_side"));
        then(2, () -> view(at(2, 4, 34), at(9.5, 2.2, 37.5)));
        then(30, () -> shot("caravan_three_quarter"));
        then(2, () -> view(at(22.5, 2.5, 38.5), at(16.5, 1.5, 38.5)));
        then(30, () -> shot("cart_back_slope"));

        // ---- drive: gallop the team up the road (from x -3.5, z -5.5 along +x: the road at z 0 is the slope; this lane rises too)
        then(5, () -> server(() -> {
            player().teleportTo(level(), driveLead.getX(), driveLead.getY(), driveLead.getZ() - 1.5, java.util.Set.of(), -90, 10, true);
        }));
        then(20, () -> server(() -> player().startRiding(driveLead)));
        then(20, () -> {
            result.add("riding: " + (mc().player.getVehicle() instanceof AbstractHorse) + "; team " + (driveWagon.harness() == driveLead) + " " + (driveWagon.partner() != null));
            mc().setCameraEntity(mc().player);
            mc().player.setYRot(-90);
            mc().options.keyUp.setDown(true);
        });
        for (int k = 1; k <= 8; k++) {
            int kk = k;
            then(5, () -> {
                if (mc().level.getEntity(driveWagon.getId()) instanceof WagonEntity w) {
                    view(w.position().add(0, 2.5, -8), w.position().add(0, 0.8, 0));
                    shot("drive_" + kk);
                    report("drive " + kk + " at x " + f(w.getX()), driveWagon);
                }
            });
        }
        then(5, () -> mc().options.keyUp.setDown(false));
        then(30, () -> {
            if (mc().level.getEntity(driveWagon.getId()) instanceof WagonEntity w) {
                view(w.position().add(-1, 3, -7), w.position().add(1.5, 1, 0));
                report("drive stopped at x " + f(w.getX()), driveWagon);
            }
        });
        then(20, () -> shot("drive_stopped"));
    }

    private TiltShots() {}
}
