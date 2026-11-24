package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * Dev only: with {@code watershots.flag} in the game directory the client makes a fresh flat world "WaterShots" and builds
 * a mill: an overshot Water Wheel fed by a stone race, its axle through a shaft and a Gearbox into a Saw Bench on a plank
 * floor, the Log Flume line fed from a stone basin, three logs in it. Then it shoots water_*.png. The flag file's first
 * line is the output folder; every further line is a view "name x y z tx ty tz" (relative to the hub's column at ground
 * level), so views can be changed without a rebuild; with none, a default set is shot. Writes water_result.txt and quits.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class WaterShots {
    private static final String WORLD = "WaterShots";
    private static int stage = -1, ticks, step;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>();
    private static final List<String> views = new ArrayList<>();
    private static ArmorStand camera;
    private static int ground;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "watershots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                for (String l : lines.subList(1, lines.size())) if (!l.isBlank()) views.add(l.trim());
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
                    mc.getWindow().setWindowed(1600, 900);
                    mc.options.renderDistance().set(8);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(4321L, false, false),
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
            Files.write(new File(out, "water_result.txt").toPath(), result);
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
                image.writeToFile(new File(out, "water_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private static Vec3 at(double x, double y, double z) {
        return new Vec3(x, ground + y, z);
    }

    private static void set(int x, int y, int z, BlockState s) {
        level().setBlockAndUpdate(new BlockPos(x, ground + y, z), s);
    }

    private static void build() {
        for (String c : List.of("time set 5000", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                "gamerule spawn_mobs false", "difficulty peaceful"))
            cmd(c);
        ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState(), mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                planks = Blocks.SPRUCE_PLANKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        // the tailrace: a pit under the wheel, a channel off to the west, stone sides
        for (int x = -12; x <= 1; x++) for (int z = -3; z <= 3; z++) {
            boolean channel = Math.abs(z) <= 2 && x <= 0;
            set(x, -1, z, channel ? air : (x + z) % 3 == 0 ? mossy : stone);
            set(x, -2, z, stone);
        }
        // the race: a stone aqueduct along x 0 from the south, its water falling past the wheel's +z side (z 1, 2)
        for (int z = 3; z <= 11; z++) {
            set(0, 5, z, (z % 4 == 0) ? mossy : stone);
            set(-1, 6, z, stone);
            set(1, 6, z, stone);
            set(0, 6, z, Blocks.WATER.defaultBlockState());
            if (z % 4 == 3) for (int y = -1; y <= 4; y++) set(0, y, z, stone);
        }
        set(0, 6, 12, stone);
        set(0, 6, 2, Blocks.WATER.defaultBlockState());
        // the wheel, axis X, hub at y 3: its plane x 0, z -2..2, y 1..5
        set(0, 3, 0, WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.X));
        // mill floor and the line: shaft, gearbox, the saw at x 3
        for (int x = 2; x <= 4; x++) for (int z = -5; z <= 4; z++) set(x, 2, z, planks);
        for (int[] p : new int[][] {{2, -5}, {4, -5}, {2, 4}, {4, 4}, {4, 0}})
            for (int y = -1; y <= 1; y++) set(p[0], y, p[1], Blocks.SPRUCE_LOG.defaultBlockState());
        set(1, 3, 0, WaterPower.GEARBOX.get().defaultBlockState());
        set(2, 3, 0, WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X));
        // power climbing to a loft: upright shafts off the gearbox, a gearbox up there and a shaft along the loft
        for (int y = 4; y <= 6; y++) set(1, y, 0, WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Y));
        set(1, 7, 0, WaterPower.GEARBOX.get().defaultBlockState());
        for (int z = 1; z <= 2; z++) set(1, 7, z, WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        // a second wheel on the same axle, upstream (it turns with the line)
        for (int x = -2; x <= -1; x++) set(x, 3, 0, WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X));
        set(-3, 3, 0, WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.X));
        BlockState flume = Sawmill.LOG_FLUME.get().defaultBlockState().setValue(LogFlumeBlock.AXIS, Direction.Axis.Z);
        for (int z = -3; z <= 3; z++)
            set(3, 3, z, z == 0 ? Sawmill.SAW_BENCH.get().defaultBlockState().setValue(SawBenchBlock.FACING, Direction.NORTH) : flume);
        // the basin feeding the flume's in end
        for (int[] p : new int[][] {{2, -4}, {4, -4}, {3, -5}}) set(p[0], 3, p[1], stone);
        set(3, 3, -4, Blocks.WATER.defaultBlockState());
        player().teleportTo(level(), 8.5, ground + 30, 14.5, java.util.Set.of(), 180, 20, true);
    }

    private static void stock() {
        if (!(level().getBlockEntity(new BlockPos(3, ground + 3, 0)) instanceof SawBenchBlockEntity saw)) {
            result.add("no saw");
            return;
        }
        saw.setBladeForTest(Sawmill.BLADE_USES);
        saw.layForTest(new ItemStack(Items.OAK_LOG), -0.6F);
        saw.layForTest(new ItemStack(Items.STRIPPED_BIRCH_LOG), -1.6F);
        saw.layForTest(new ItemStack(Items.SPRUCE_LOG), -2.6F);
        saw.setChanged();
        level().sendBlockUpdated(saw.getBlockPos(), saw.getBlockState(), saw.getBlockState(), 3);
        result.add("saw problem " + saw.problem() + " power " + saw.power());
    }

    private static final String[] DEFAULT = {
            "overview -9 9 -11 1.5 3 0",
            "wheel_face -8 3.5 0.5 0 3 0",
            "wheel_oblique -5 6 -6 0 3 0",
            "wheel_close -3 4.5 -2.5 0 3.2 0.5",
            "saw 6.5 5.5 -2 3.5 4 0",
            "saw_close 5.5 4.6 -1.2 3.5 4 0.3",
            "saw_crank 1.6 4.4 -1.6 2.6 3.6 0.4",
            "gearbox 0.5 5.2 -2.2 1.8 3.5 0",
            "flume 5 5 -6 3.5 3.5 -2"};

    private static void script() {
        then(1, () -> server(WaterShots::build));
        then(100, () -> server(() -> {
            if (level().getBlockEntity(new BlockPos(0, ground + 3, 0)) instanceof WaterWheelBlockEntity w)
                result.add("wheel power " + w.power() + " sum " + w.measure(level()) + " blocked " + w.blocked + " bodies "
                        + level().getEntities(WaterPower.WHEEL_HITBOX.get(), new net.minecraft.world.phys.AABB(w.getBlockPos()).inflate(3), e -> true).size());
            for (BlockPos q : new BlockPos[] {new BlockPos(-3, ground + 3, 0), new BlockPos(1, ground + 5, 0), new BlockPos(1, ground + 7, 2)})
                if (level().getBlockEntity(q) instanceof RotorBlockEntity r) result.add(q + " power " + r.power() + " speed " + r.speed());
        }));
        List<String> list = views.isEmpty() ? List.of(DEFAULT) : views;
        for (String v : list) {
            String[] p = v.split("\\s+");
            if (p[0].startsWith("saw") || p[0].startsWith("flume")) then(1, () -> server(() -> {
                if (level().getBlockEntity(new BlockPos(3, ground + 3, 0)) instanceof SawBenchBlockEntity saw && saw.logs().isEmpty()) stock();
            }));
            if (p[0].startsWith("stand")) then(1, () -> server(() -> { // the player dropped onto the wheel: it holds them
                ServerPlayer pl = player();
                pl.getAbilities().flying = false;
                pl.onUpdateAbilities();
                pl.teleportTo(level(), 0.5, ground + 7.5, 0.3, java.util.Set.of(), 90, 10, true);
            }));
            if (p[0].startsWith("stand")) then(40, () -> server(() -> result.add("player stands at y " + (player().getY() - ground) + " on ground " + player().onGround())));
            then(10, () -> view(at(Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])),
                    at(Double.parseDouble(p[4]), Double.parseDouble(p[5]), Double.parseDouble(p[6]))));
            then(25, () -> shot(p[0]));
            if (p[0].startsWith("saw") || p[0].startsWith("wheel")) for (int i = 1; i <= 2; i++) {
                int n = i;
                then(4, () -> shot(p[0] + "_m" + n)); // motion: a few ticks on
            }
        }
        then(5, () -> server(() -> {
            if (level().getBlockEntity(new BlockPos(3, ground + 3, 0)) instanceof SawBenchBlockEntity saw)
                result.add("end: saw logs " + saw.logs().size() + " blade " + saw.blade() + " problem " + saw.problem());
        }));
    }

    private WaterShots() {}
}
