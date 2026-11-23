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
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (bug pass v0.185.0): with {@code bugshots.flag} in the game directory (first line: output folder) the client
 * makes a fresh flat world "BugShots" and plays the bugs through with real keys: a felled log dropped on slabs and one
 * pushed into a wall, a log dragged round a corner by its end, a wagon galloped with its pair and turned, the rider getting
 * off (the team must stand), and two logs loaded by real clicks. It measures as it goes (hands to log end, second horse
 * and wagon to where the rigid team puts them, how far the left team moves), writes bugs_result.txt, shoots bugs_*.png
 * and quits. Views come from a camera stand of its own, so the player can act while being watched.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BugShots {
    private static final String WORLD = "BugShots";
    private static int stage = -1, ticks, step;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>(); // {wait ticks, Runnable}
    private static Runnable monitor;
    private static ArmorStand camera;
    private static int ground;
    private static FelledLogEntity slabLog, wallLog, dragLog, load1, load2;
    private static WagonEntity wagon;
    private static AbstractHorse lead, partner;
    private static double maxGap, minGap, maxPair, maxWagon, sumGap;
    private static int samples;
    private static Vec3 leadAt, partnerAt, wagonAt;
    private static double jitter;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "bugshots.flag");
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
                    mc.options.renderDistance().set(10);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.SURVIVAL, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
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
            if (monitor != null) monitor.run();
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
            Files.write(new File(out, "bugs_result.txt").toPath(), result);
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

    private static String fmt(Vec3 v) {
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", v.x, v.y, v.z);
    }

    private static String f(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }

    /** The view from {@code at} looking at {@code target} (a camera stand of our own; the player stays where it is). */
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

    /** The player's own view (first person or a third person one). */
    private static void own(CameraType type) {
        mc().setCameraEntity(mc().player);
        mc().options.setCameraType(type);
    }

    private static void shot(String name) {
        Screenshot.takeScreenshot(mc().gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "bugs_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
        result.add("shot " + name);
    }

    private static Vec3 at(double x, double y, double z) {
        return new Vec3(x, ground + y, z);
    }

    private static FelledLogEntity client(FelledLogEntity log) {
        return (FelledLogEntity) mc().level.getEntity(log.getId());
    }

    private static void keys(boolean up, boolean sprint) {
        mc().options.keyUp.setDown(up);
        mc().options.keySprint.setDown(sprint);
    }

    private static void script() {
        // ---- the world: noon, clear, peaceful; the ground height of the flat world
        then(1, () -> server(() -> {
            for (String c : List.of("time set noon", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                    "gamerule spawn_mobs false", "difficulty peaceful", "gamemode survival @a"))
                cmd(c);
            ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            ServerLevel l = level();
            // A: grass slabs (x 0..6, z 0..3) and a dirt wall two high (x 12, z -4..10)
            BlockState slab = EarthBlocks.GRASS_SLAB.get().defaultBlockState();
            for (int x = 0; x <= 6; x++) for (int z = 0; z <= 3; z++) l.setBlockAndUpdate(new BlockPos(x, ground, z), slab);
            for (int z = -4; z <= 10; z++) for (int y = 0; y <= 1; y++) l.setBlockAndUpdate(new BlockPos(12, ground + y, z), Blocks.DIRT.defaultBlockState());
            BlockState oak = Blocks.OAK_LOG.defaultBlockState(), birch = Blocks.BIRCH_LOG.defaultBlockState();
            slabLog = FelledLogEntity.spawnStraight(l, at(1.6, 2.5, 0.9), -62, 4, birch); // turned across the slabs, one end off them
            wallLog = FelledLogEntity.spawnStraight(l, at(7.5, 0, 6.5), -60, 4, oak); // crown towards the wall, slanting
            dragLog = FelledLogEntity.spawnStraight(l, at(0.5, 0, 40.5), 90, 4, oak); // crown towards -x
            player().teleportTo(l, 2.5, ground, 12.5, java.util.Set.of(), 180, 20, true);
        }));
        then(60, () -> {
            result.add("ground " + ground);
            result.add("slab log: y " + f(slabLog.getY()) + " (slab top " + (ground + 0.5) + ") tilt " + f(slabLog.tilt()) + " yaw " + f(slabLog.getYRot()));
            view(at(-1.5, 2.6, 4.8), at(3, 0.6, 1.0));
        });
        then(40, () -> shot("log_on_slabs"));
        then(5, () -> view(at(6.5, 1.0, -0.2), at(2.5, 0.4, 1.5)));
        then(40, () -> shot("log_on_slabs_low"));

        // ---- B1: a log lifted and pushed into the wall at a slant
        then(5, () -> server(() -> {
            Vec3 behind = wallLog.endPoint(true).subtract(Vec3.directionFromRotation(0, wallLog.getYRot()).scale(0.6));
            player().teleportTo(level(), behind.x, ground, behind.z, java.util.Set.of(), wallLog.getYRot(), 25, true);
        }));
        then(10, () -> server(() -> wallLog.lift(level(), player(), 0)));
        then(10, () -> {
            result.add("wall log lifted (client): " + (FelledLogEntity.liftedBy(mc().player) != null));
            own(CameraType.FIRST_PERSON);
            keys(true, false);
            minGap = 9;
            monitor = () -> {
                FelledLogEntity c = client(wallLog);
                if (c != null && c.lifterCount() > 0) minGap = Math.min(minGap, mc().player.position().subtract(c.endPoint(true)).horizontalDistance());
            };
        });
        then(90, () -> {
            keys(false, false);
            FelledLogEntity c = client(wallLog);
            result.add("pushed at the wall: client tip " + fmt(c.endPoint(false)) + " server tip " + fmt(wallLog.endPoint(false)) + " (wall face x 12)"
                    + " fits(server) " + wallLog.fits(level(), wallLog.position(), wallLog.getYRot()));
            result.add("  hands to end (client) " + f(mc().player.position().subtract(c.endPoint(true)).horizontalDistance()));
            view(at(7.0, 3.0, 12.5), c.endPoint(false).add(0, 0.5, 0));
        });
        then(30, () -> {
            shot("log_wall_held");
            result.add("  pushing it (walking into it): hands to end min " + f(minGap));
            monitor = null;
            own(CameraType.THIRD_PERSON_BACK);
        });
        then(10, () -> shot("log_wall_push_third"));
        then(2, () -> own(CameraType.FIRST_PERSON));
        then(10, () -> shot("log_wall_push_first"));
        then(2, () -> mc().options.keyShift.setDown(true));
        then(8, () -> mc().options.keyShift.setDown(false));
        then(40, () -> {
            result.add("put down at the wall: tip " + fmt(wallLog.endPoint(false)) + " y " + f(wallLog.getY()) + " tilt " + f(wallLog.tilt())
                    + " fits " + wallLog.fits(level(), wallLog.position(), wallLog.getYRot()));
            view(at(9.5, 2.4, 10.5), wallLog.endPoint(false).add(0, 0.4, 0));
        });
        then(30, () -> shot("log_wall_down"));

        // ---- B2: drag a log round a corner by its end: west, then a right turn to the north
        then(5, () -> server(() -> {
            Vec3 root = dragLog.endPoint(true);
            player().teleportTo(level(), root.x + 0.5, ground, root.z, java.util.Set.of(), -90, 15, true); // facing east, the log behind
        }));
        then(10, () -> server(() -> dragLog.lift(level(), player(), 0)));
        then(10, () -> {
            own(CameraType.THIRD_PERSON_BACK);
            mc().player.setYRot(-90);
            keys(true, false);
            maxGap = sumGap = 0;
            minGap = 9;
            samples = 0;
            monitor = () -> {
                FelledLogEntity c = client(dragLog);
                if (c == null || FelledLogEntity.liftedBy(mc().player) == null) return;
                double gap = mc().player.position().subtract(c.endPoint(true)).horizontalDistance();
                if (gap > maxGap + 0.2 || gap > 1.2 && samples % 10 == 0) result.add("  gap " + f(gap) + " at tick " + samples + " player " + fmt(mc().player.position()) + " end " + fmt(c.endPoint(true)));
                maxGap = Math.max(maxGap, gap);
                minGap = Math.min(minGap, gap);
                sumGap += gap;
                samples++;
            };
        });
        then(120, () -> result.add("drag east: player " + fmt(mc().player.position()) + " log yaw " + f(client(dragLog).getYRot())));
        for (int k = 1; k <= 30; k++) { // the turn: 90 degrees over 3 s, walking on
            int kk = k;
            then(2, () -> mc().player.setYRot(-90 - kk * 3));
            if (k == 15) then(0, () -> {
                view(mc().player.position().add(3, 7, 4), mc().player.position().add(1.5, 0, -1));
                shot("drag_turn");
                own(CameraType.THIRD_PERSON_BACK);
            });
        }
        then(120, () -> {
            keys(false, false);
            FelledLogEntity c = client(dragLog);
            result.add("drag north: player " + fmt(mc().player.position()) + " log yaw " + f(c.getYRot()) + " root end " + fmt(c.endPoint(true)));
            result.add("  hands to held end while dragging: min " + f(minGap) + " max " + f(maxGap) + " mean " + f(sumGap / Math.max(1, samples)) + " (" + samples + " ticks; held at "
                    + FelledLogEntity.HOLD + ", slack " + FelledLogEntity.LIFT_SLACK + ")");
            monitor = null;
            view(mc().player.position().add(4, 6, 3), c.position());
        });
        then(30, () -> shot("drag_after_corner"));
        then(5, () -> own(CameraType.THIRD_PERSON_BACK));
        then(20, () -> shot("drag_hands"));
        then(2, () -> own(CameraType.THIRD_PERSON_FRONT));
        then(20, () -> shot("drag_hands_front"));
        then(2, () -> { // turn round to face the log: in first person its end lies low at the hands
            own(CameraType.FIRST_PERSON);
            mc().player.setYRot(mc().player.getYRot() + 180);
            mc().player.setXRot(35);
        });
        then(20, () -> {
            shot("drag_first_person");
            result.add("  facing it: hands to end " + f(mc().player.position().subtract(client(dragLog).endPoint(true)).horizontalDistance()));
        });
        then(2, () -> mc().options.keyShift.setDown(true));
        then(8, () -> mc().options.keyShift.setDown(false));

        // ---- C: the wagon galloping with its pair, then a turn
        then(10, () -> server(() -> {
            ServerLevel l = level();
            Vec3 w = at(0.5, 0, 100.5);
            wagon = Carting.WAGON.get().create(l, EntitySpawnReason.EVENT);
            wagon.setPos(w);
            wagon.setYRot(-90); // facing +x
            l.addFreshEntity(wagon);
            Vec3 ahead = Vec3.directionFromRotation(0, -90).scale(WagonEntity.LEAD), left = Vec3.directionFromRotation(0, -180);
            lead = horse(l, w.add(ahead).subtract(left.scale(WagonEntity.PAIR / 2)));
            partner = horse(l, w.add(ahead).add(left.scale(WagonEntity.PAIR / 2)));
            lead.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Carting.HIDE_SADDLE_BLANKET.get()));
            player().teleportTo(l, lead.getX(), lead.getY(), lead.getZ() - 1.5, java.util.Set.of(), -90, 10, true);
            wagon.setHarness(lead);
            wagon.setPartner(partner);
        }));
        then(20, () -> server(() -> player().startRiding(lead)));
        then(20, () -> {
            result.add("riding: " + (mc().player.getVehicle() instanceof AbstractHorse) + "; team " + (wagon.harness() == lead) + " " + (wagon.partner() == partner));
            own(CameraType.THIRD_PERSON_FRONT);
            mc().player.setYRot(-90);
            mc().player.setXRot(28);
            keys(true, true);
            maxPair = maxWagon = 0;
            samples = 0;
            monitor = BugShots::measureTeam;
        });
        then(80, () -> {
            Vec3 v = mc().player.getVehicle().getDeltaMovement();
            result.add("gallop: speed " + f(v.horizontalDistance() * 20) + " blocks/s");
            shot("wagon_gallop_front");
            own(CameraType.THIRD_PERSON_BACK);
            mc().player.setXRot(40);
        });
        then(15, () -> shot("wagon_gallop_back"));
        then(20, () -> own(CameraType.THIRD_PERSON_FRONT));
        for (int k = 1; k <= 40; k++) { // a 90 degree turn to the left at a gallop
            int kk = k;
            then(1, () -> mc().player.setYRot(-90 - kk * 2.25F));
            if (k == 20) then(0, () -> shot("wagon_turn"));
        }
        then(30, () -> {
            shot("wagon_after_turn");
            result.add("team while galloping and turning, " + samples + " ticks on the rider's client: second horse off its place max " + f(maxPair)
                    + ", wagon off the rigid pole's place max " + f(maxWagon));
            keys(false, false);
        });
        // ---- D: get off; the team must stand
        then(40, () -> {
            monitor = null;
            mc().options.keyShift.setDown(true);
        });
        then(5, () -> mc().options.keyShift.setDown(false));
        then(20, () -> {
            result.add("dismounted: " + (mc().player.getVehicle() == null));
            leadAt = lead.position();
            partnerAt = partner.position();
            wagonAt = wagon.position();
            maxPair = 0;
            jitter = 0;
            monitor = () -> {
                if (!(mc().level.getEntity(partner.getId()) instanceof AbstractHorse b) || !(mc().level.getEntity(lead.getId()) instanceof AbstractHorse a)) return;
                jitter = Math.max(jitter, Math.abs(net.minecraft.util.Mth.wrapDegrees(b.yBodyRot - b.yBodyRotO)) + Math.abs(net.minecraft.util.Mth.wrapDegrees(b.yHeadRot - b.yHeadRotO))
                        + b.position().subtract(new Vec3(b.xo, b.yo, b.zo)).length() * 50);
            };
        });
        then(100, () -> {
            result.add("standing (player beside them, 5 s): second horse's largest change per tick (degrees of body + head turn + 50 x blocks moved): " + f(jitter));
            own(CameraType.THIRD_PERSON_FRONT);
        });
        then(10, () -> shot("wagon_dismounted_near"));
        then(5, () -> {
            monitor = null;
            server(() -> player().teleportTo(level(), wagonAt.x - 6, ground, wagonAt.z + 6, java.util.Set.of(), 0, 0, true));
        });
        then(300, () -> {
            result.add("20 s after getting off: lead moved " + f(lead.position().distanceTo(leadAt)) + ", second " + f(partner.position().distanceTo(partnerAt))
                    + ", wagon " + f(wagon.position().distanceTo(wagonAt)) + "; still hitched " + (wagon.harness() == lead && wagon.partner() == partner));
            Vec3 c = wagon.position();
            view(c.add(5, 3.5, 6), c.add(1.5, 0.6, 0));
        });
        then(30, () -> shot("wagon_left_standing"));

        // ---- E: two logs loaded by real clicks: the second by clicking the first one on the bed
        then(5, () -> server(() -> {
            Vec3 c = wagon.position();
            load1 = FelledLogEntity.spawnStraight(level(), c.add(-1, 0, 3), 0, 3, Blocks.SPRUCE_LOG.defaultBlockState());
            load2 = FelledLogEntity.spawnStraight(level(), c.add(1, 0, 3), 0, 3, Blocks.SPRUCE_LOG.defaultBlockState());
            player().teleportTo(level(), c.x - 1, c.y, c.z + 2.4, java.util.Set.of(), 180, 20, true);
        }));
        then(15, () -> server(() -> load1.lift(level(), player(), 0)));
        then(10, () -> {
            var w = mc().level.getEntity(wagon.getId());
            mc().gameMode.interact(mc().player, w, new EntityHitResult(w), InteractionHand.MAIN_HAND);
        });
        then(10, () -> server(() -> {
            Vec3 c = wagon.position();
            player().teleportTo(level(), c.x + 1, c.y, c.z + 2.4, java.util.Set.of(), 180, 20, true);
        }));
        then(10, () -> server(() -> load2.lift(level(), player(), 0)));
        then(10, () -> {
            var onBed = mc().level.getEntity(load1.getId());
            mc().gameMode.interact(mc().player, onBed, new EntityHitResult(onBed), InteractionHand.MAIN_HAND);
        });
        then(15, () -> {
            result.add("logs on the wagon after clicking the first loaded log with the second: " + wagon.carriedLogs().size());
            Vec3 c = wagon.position();
            view(c.add(-3.5, 3, 4.5), c.add(0, 0.8, 0));
        });
        then(30, () -> shot("wagon_two_logs"));
    }

    private static AbstractHorse horse(ServerLevel l, Vec3 at) {
        AbstractHorse h = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(l, EntitySpawnReason.EVENT);
        h.setPos(at);
        h.setYRot(-90);
        h.setYBodyRot(-90);
        h.setTamed(true);
        h.setPersistenceRequired();
        l.addFreshEntity(h);
        return h;
    }

    /** On the rider's client each tick: how far the second horse and the wagon are from where the rigid team puts them. */
    private static void measureTeam() {
        var level = mc().level;
        if (!(level.getEntity(wagon.getId()) instanceof WagonEntity w) || !(level.getEntity(lead.getId()) instanceof AbstractHorse a)
                || !(level.getEntity(partner.getId()) instanceof AbstractHorse b)) return;
        Vec3 side = b.position().subtract(a.position()).multiply(1, 0, 1);
        Vec3 want = Vec3.directionFromRotation(0, a.getYRot() + (side.dot(Vec3.directionFromRotation(0, a.getYRot() + 90)) > 0 ? 90 : -90)).scale(WagonEntity.PAIR);
        maxPair = Math.max(maxPair, side.subtract(want).horizontalDistance());
        Vec3 mid = a.position().add(want.scale(0.5));
        Vec3 front = mid.subtract(Vec3.directionFromRotation(0, a.getYRot()).scale(WagonEntity.POLE));
        Vec3 rigid = front.subtract(Vec3.directionFromRotation(0, w.getYRot()).scale(WagonEntity.FRONT_AXLE));
        maxWagon = Math.max(maxWagon, rigid.subtract(w.position()).horizontalDistance());
        samples++;
    }

    private BugShots() {}
}
