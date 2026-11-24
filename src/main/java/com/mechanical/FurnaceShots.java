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
 * Dev only (like WaterShots): with {@code furnaceshots.flag} in the game directory the client makes a flat world and
 * builds two High Furnaces: the smallest (inside 2x2x5 at x 0..1, z 0..1) with its Great Bellows on a real overshot
 * Water Wheel (via gearboxes and an upright shaft), and the biggest (3x3x8 at x 10..12, z 0..2) with three bellows
 * driven from code. The flag's first line is the output folder; then one step per line: a view "name x y z tx ty tz"
 * (relative to the ground at 0,0) shoots fs_name.png; "!light" stocks and lights every Firebox; "!heat T" sets both
 * furnaces to T° (dried out); "!time N"; "!gamma G" (0..1, for night views); "!gui" opens the small furnace's screen and shoots it; "!wait N" waits N ticks.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FurnaceShots {
    private static final String WORLD = "FurnaceShots";
    private static int stage = -1, ticks, step, ground;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>();
    private static ArmorStand camera;
    private static boolean feeding;
    /** The flow demonstrations: troughs fed steel from above (each "!demo x y z" adds one) until "!demostop". */
    private static final List<BlockPos> demoAt = new ArrayList<>();
    private static final BlockPos SMALL_FRONT = new BlockPos(0, 0, -1), BIG_FRONT = new BlockPos(10, 0, -1);

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "furnaceshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                File dir = new File(mc.gameDirectory, "saves/" + WORLD);
                if (dir.exists()) try (var walk = Files.walk(dir.toPath())) {
                    walk.sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
                }
                script(lines.subList(1, lines.size()));
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
            if (!demoAt.isEmpty()) server(() -> {
                for (BlockPos q : demoAt)
                    if (level().getBlockEntity(rel(q.getX(), q.getY(), q.getZ())) instanceof TroughBlockEntity t && t.room() > 0)
                        t.add(Molten.STEEL, Math.min(40, t.room()), 0, 1560, level().getGameTime(), null);
            });
            if (feeding) server(() -> {
                for (BlockPos q : new BlockPos[] {new BlockPos(14, 0, 1), new BlockPos(8, 0, 1), new BlockPos(12, 0, 4), new BlockPos(9, 0, -12)})
                    if (level().getBlockEntity(rel(q.getX(), q.getY(), q.getZ())) instanceof GreatBellowsBlockEntity b) b.feed(16 * WaterPower.DEG_PER_POWER, 0, 0, 16);
            });
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
            Files.write(new File(out, "fs_result.txt").toPath(), result);
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
                image.writeToFile(new File(out, "fs_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private static BlockPos rel(int x, int y, int z) {
        return new BlockPos(x, ground + y, z);
    }

    private static void set(int x, int y, int z, BlockState s) {
        level().setBlockAndUpdate(rel(x, y, z), s);
    }

    /** A furnace: inside x0.., z0.. (wide x deep), floor at y -1, walls 0..layers-1, the Front at x0 in the north wall. */
    private static void furnace(int x0, int z0, int wide, int deep, int layers) {
        for (int x = x0; x < x0 + wide; x++) for (int z = z0; z < z0 + deep; z++) set(x, -1, z, HighFurnace.SILICA_BRICKS.get().defaultBlockState());
        BlockState c = HighFurnace.BANDED_FURNACE_CASING.get().defaultBlockState();
        for (int y = 0; y < layers; y++) {
            for (int x = x0; x < x0 + wide; x++) {
                set(x, y, z0 - 1, c);
                set(x, y, z0 + deep, c);
            }
            for (int z = z0; z < z0 + deep; z++) {
                set(x0 - 1, y, z, c);
                set(x0 + wide, y, z, c);
            }
        }
        // the corners are left open (they are not part of the furnace): the corner straps show there
        set(x0, 0, z0 - 1, HighFurnace.HIGH_FURNACE_FRONT.get().defaultBlockState().setValue(HighFurnaceFrontBlock.FACING, Direction.NORTH));
    }

    private static void build() {
        Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0).teleportTo(level(), -30.5, 80, -30.5, java.util.Set.of(), 0, 0, true);
        for (String c : List.of("time set 5000", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                "gamerule spawn_mobs false", "difficulty peaceful"))
            cmd(c);
        ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState(), cobble = Blocks.COBBLESTONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        // a cobbled yard under both furnaces
        for (int x = -4; x <= 17; x++) for (int z = -6; z <= 7; z++) set(x, -1, z, (x * 7 + z * 3) % 5 == 0 ? Blocks.GRAVEL.defaultBlockState() : cobble);
        furnace(0, 0, 2, 2, 5);
        set(2, 0, 0, HighFurnace.FIREBOX.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, Direction.EAST));
        furnace(10, 0, 3, 3, 8);
        // one firebox and bellows per side, not a row of furniture: east, west and the back wall
        set(13, 0, 1, HighFurnace.FIREBOX.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, Direction.EAST));
        set(14, 0, 1, HighFurnace.GREAT_BELLOWS.get().defaultBlockState().setValue(GreatBellowsBlock.FACING, Direction.WEST));
        set(9, 0, 1, HighFurnace.FIREBOX.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, Direction.WEST));
        set(8, 0, 1, HighFurnace.GREAT_BELLOWS.get().defaultBlockState().setValue(GreatBellowsBlock.FACING, Direction.EAST));
        set(12, 0, 3, HighFurnace.FIREBOX.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, Direction.SOUTH));
        set(12, 0, 4, HighFurnace.GREAT_BELLOWS.get().defaultBlockState().setValue(GreatBellowsBlock.FACING, Direction.NORTH));
        // the big furnace's casting floor, north of it clear of everything: a taphole and spout pouring into a trough line in a
        // pit, a T to two molds, a shut gate holding the metal back from the third mold
        set(12, 0, -1, HighFurnace.TAPHOLE.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, Direction.NORTH));
        set(12, 0, -2, HighFurnace.POURING_SPOUT.get().defaultBlockState().setValue(PouringSpoutBlock.FACING, Direction.NORTH).setValue(PouringSpoutBlock.OPEN, true));
        for (int x = 9; x <= 15; x++) for (int z = -10; z <= -2; z++) set(x, -1, z, Blocks.AIR.defaultBlockState());
        set(12, -1, -2, Blocks.AIR.defaultBlockState());
        for (int[] q : new int[][] {{12, -2}, {12, -3}, {12, -4}, {11, -5}, {12, -5}, {13, -5}, {12, -7}})
            set(q[0], -1, q[1], HighFurnace.REFRACTORY_TROUGH.get().defaultBlockState());
        set(12, -1, -6, HighFurnace.TROUGH_GATE.get().defaultBlockState().setValue(RefractoryTroughBlock.Gate.AXIS, Direction.Axis.Z));
        set(10, -1, -5, HighFurnace.FOUNDRY_BASIN.get().defaultBlockState());
        set(14, -1, -5, HighFurnace.IRON_PLATE_MOLD.get().defaultBlockState());
        set(12, -1, -8, HighFurnace.POURING_SPOUT.get().defaultBlockState().setValue(PouringSpoutBlock.FACING, Direction.NORTH).setValue(PouringSpoutBlock.OPEN, true)
                .setValue(PouringSpoutBlock.SHORT, true));
        for (int x = 11; x <= 13; x++) for (int z = -10; z <= -8; z++) for (int y = -3; y <= -2; y++) set(x, y, z, Blocks.AIR.defaultBlockState());
        set(12, -3, -9, HighFurnace.FOUNDRY_TABLE.get().defaultBlockState());
        set(12, -3, -10, Blocks.STONE_BRICKS.defaultBlockState());
        set(12, -3, -8, HighFurnace.FOUNDRY_TABLE.get().defaultBlockState());
        set(12, -2, -8, HighFurnace.MOLDING_FLASK.get().defaultBlockState().setValue(MoldingFlaskBlock.STAGE, MoldingFlaskBlock.Stage.READY));
        if (level().getBlockEntity(rel(12, -2, -8)) instanceof MoldingFlaskBlock.Entity fe) {
            fe.shape = MoldingFlaskBlock.FlaskShape.PICKAXE_HEAD;
            fe.setChanged();
        }
        // the flow demonstration east of the furnaces: a level run with a T branching north to a closed end, then a step
        // down a slope to a lower run (dug into the yard) that is longer than steel's reach
        for (int x = 19; x <= 40; x++) for (int z = -8; z <= -2; z++) set(x, -1, z, x >= 28 && z == -4 ? Blocks.AIR.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
        for (int x = 20; x <= 27; x++) set(x, 0, -4, HighFurnace.REFRACTORY_TROUGH.get().defaultBlockState());
        for (int z = -6; z <= -5; z++) set(24, 0, z, HighFurnace.REFRACTORY_TROUGH.get().defaultBlockState());
        for (int x = 28; x <= 39; x++) set(x, -1, -4, HighFurnace.REFRACTORY_TROUGH.get().defaultBlockState());
        for (int x = 27; x <= 31; x++) set(x, -1, -3, Blocks.AIR.defaultBlockState()); // open the step's trench on its south side
        // a straight gate run: fed at x 20, 3 troughs, a shut gate, 2 troughs, an Iron Ingot Mold
        for (int x = 20; x <= 25; x++) set(x, 0, -8, x == 23 ? HighFurnace.TROUGH_GATE.get().defaultBlockState().setValue(RefractoryTroughBlock.Gate.AXIS, Direction.Axis.X)
                : HighFurnace.REFRACTORY_TROUGH.get().defaultBlockState());
        set(26, 0, -8, HighFurnace.IRON_INGOT_MOLD.get().defaultBlockState());
        for (int x = 19; x <= 40; x++) for (int y = -1; y <= 0; y++) for (int z = -8; z <= -3; z++) {
            BlockPos q = rel(x, y, z);
            if (level().getBlockState(q).getBlock() instanceof ClayChannelBlock c) level().setBlockAndUpdate(q, c.stateAt(level(), q));
        }
        // the step join in the open: a raised run on stone pillars (y 1), a slope down (x 33), a lower run on the ground (y 0)
        for (int x = 30; x <= 32; x++) {
            set(x, 0, -11, Blocks.STONE_BRICKS.defaultBlockState());
            set(x, 1, -11, HighFurnace.REFRACTORY_TROUGH.get().defaultBlockState());
        }
        for (int x = 33; x <= 37; x++) set(x, 0, -11, HighFurnace.REFRACTORY_TROUGH.get().defaultBlockState());
        for (int x = 30; x <= 37; x++) for (int y = 0; y <= 1; y++) {
            BlockPos q = rel(x, y, -11);
            if (level().getBlockState(q).getBlock() instanceof ClayChannelBlock c) level().setBlockAndUpdate(q, c.stateAt(level(), q));
        }
        // the finery hearth with its own bellows, a coke oven, a hot blast stove by the small furnace's bellows, a sight glass
        set(8, 0, -12, HighFurnace.FINERY_HEARTH.get().defaultBlockState());
        set(9, 0, -12, HighFurnace.GREAT_BELLOWS.get().defaultBlockState().setValue(GreatBellowsBlock.FACING, Direction.WEST));
        set(5, 0, -12, HighFurnace.COKE_OVEN.get().defaultBlockState().setValue(CokeOvenBlock.FACING, Direction.SOUTH));
        set(3, 0, -1, HighFurnace.HOT_BLAST_STOVE.get().defaultBlockState());
        set(3, 1, -1, HighFurnace.HOT_BLAST_STOVE.get().defaultBlockState());
        set(13, 3, 1, HighFurnace.SIGHT_GLASS.get().defaultBlockState());
        set(13, 4, 1, HighFurnace.SIGHT_GLASS.get().defaultBlockState());
        // the patternmaker's corner: a Pattern Bench, Casting Sand, a rammed flask, the three iron molds
        set(5, 0, -7, HighFurnace.PATTERN_BENCH.get().defaultBlockState().setValue(PatternBenchBlock.FACING, Direction.SOUTH));
        set(4, 0, -7, HighFurnace.CASTING_SAND.get().defaultBlockState());
        set(6, 0, -7, HighFurnace.MOLDING_FLASK.get().defaultBlockState().setValue(MoldingFlaskBlock.STAGE, MoldingFlaskBlock.Stage.SAND));
        set(4, 0, -6, HighFurnace.IRON_INGOT_MOLD.get().defaultBlockState());
        set(5, 0, -6, HighFurnace.IRON_PLATE_MOLD.get().defaultBlockState());
        set(6, 0, -6, HighFurnace.IRON_NUGGET_MOLD.get().defaultBlockState());
        // the troughs take their shape from their neighbours: shape them all again now everything stands
        for (int x = 9; x <= 15; x++) for (int z = -10; z <= -2; z++) {
            BlockPos q = rel(x, -1, z);
            if (level().getBlockState(q).getBlock() instanceof ClayChannelBlock c) level().setBlockAndUpdate(q, c.stateAt(level(), q));
        }
        // a taphole in the small furnace's back wall with a spout pouring into an iron ingot mold in a pit
        set(1, 0, 2, HighFurnace.TAPHOLE.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, Direction.SOUTH));
        set(1, 0, 3, HighFurnace.POURING_SPOUT.get().defaultBlockState().setValue(PouringSpoutBlock.FACING, Direction.SOUTH).setValue(PouringSpoutBlock.OPEN, true));
        for (int x = -1; x <= 3; x++) for (int z = 3; z <= 6; z++) set(x, -1, z, HighFurnace.CASTING_SAND.get().defaultBlockState());
        for (int z = 3; z <= 5; z++) set(1, -1, z, HighFurnace.SAND_RUNNER.get().defaultBlockState());
        for (int[] q : new int[][] {{0, 4}, {2, 4}, {0, 5}, {2, 5}}) set(q[0], -1, q[1], HighFurnace.PIG_MOLD.get().defaultBlockState());
        for (int z = 3; z <= 5; z++) {
            BlockPos q = rel(1, -1, z);
            level().setBlockAndUpdate(q, HighFurnace.SAND_RUNNER.get().stateAt(level(), q));
        }
        // the small furnace's bellows on a real wheel: an overshot wheel (axis z, hub 3,3,6), its axle north through a gearbox,
        // down an upright shaft to a gearbox at the bellows' height and on into the bellows' side
        set(3, 0, 0, HighFurnace.GREAT_BELLOWS.get().defaultBlockState().setValue(GreatBellowsBlock.FACING, Direction.WEST));
        BlockState sz = WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Z),
                sy = WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Y);
        set(3, 0, 1, sz);
        set(3, 0, 2, WaterPower.GEARBOX.get().defaultBlockState());
        set(3, 1, 2, sy);
        set(3, 2, 2, sy);
        set(3, 3, 2, WaterPower.GEARBOX.get().defaultBlockState());
        for (int z = 3; z <= 5; z++) set(3, 3, z, sz);
        set(3, 3, 6, WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.Z));
        // the race: a stone trough along x at y 6 (floor y 5), its water falling past the wheel's +x side (x 5)
        for (int x = 6; x <= 14; x++) {
            set(x, 5, 6, x % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : stone);
            set(x, 6, 5, stone);
            set(x, 6, 7, stone);
            set(x, 6, 6, Blocks.WATER.defaultBlockState());
            if (x % 4 == 2) for (int y = 0; y <= 4; y++) set(x, y, 6, stone);
        }
        set(15, 6, 6, stone);
        set(5, 6, 6, Blocks.WATER.defaultBlockState());
        // the tailrace: a pit under the wheel's falling side, running off south
        for (int x = 4; x <= 6; x++) for (int z = 4; z <= 13; z++) {
            set(x, -1, z, x == 5 || z > 5 && z < 9 ? air : stone);
            set(x, -2, z, stone);
        }
    }

    private static void light() {
        for (BlockPos front : new BlockPos[] {SMALL_FRONT, BIG_FRONT})
            if (level().getBlockEntity(rel(front.getX(), front.getY(), front.getZ())) instanceof HighFurnaceBlockEntity f) {
                f.check(level());
                result.add(front + " formed " + f.formed() + " problem " + (f.problem() == null ? "-" : f.problem().getString()));
                if (f.shape() != null) for (HighFurnaceShape.Wall w : f.shape().fireboxes()) f.addFuel(w.pos(), new ItemStack(Items.CHARCOAL, 32));
                f.light();
                f.setDriedForTest();
            }
    }

    private static void heat(float t) {
        for (BlockPos front : new BlockPos[] {SMALL_FRONT, BIG_FRONT})
            if (level().getBlockEntity(rel(front.getX(), front.getY(), front.getZ())) instanceof HighFurnaceBlockEntity f) {
                f.setTemperatureForTest(t);
                result.add(front + " heat " + t + " target " + f.target() + " blast " + f.effectiveBlast());
            }
    }

    /** Layers in both pools and a few things still melting (for the screen and the view down the top). */
    private static void fill() {
        for (BlockPos front : new BlockPos[] {SMALL_FRONT, BIG_FRONT})
            if (level().getBlockEntity(rel(front.getX(), front.getY(), front.getZ())) instanceof HighFurnaceBlockEntity f) {
                f.pool().add(Molten.GOLD, 1600);
                f.pool().add(Molten.COPPER, 1400);
                f.pool().add(Molten.BRONZE, front == BIG_FRONT ? 9000 : 2600);
                f.pool().add(Molten.LEAD, 400);
                for (int i = 0; i < 6; i++) f.take(new ItemStack(BronzeOres.CRUSHED_COPPER_ORE.get()));
                f.take(new ItemStack(Items.RAW_GOLD));
                f.take(new ItemStack(IronOres.RAW_GALENA.get()));
                f.take(new ItemStack(Items.COPPER_INGOT));
                f.setChanged();
            }
    }

    private static Vec3 at(double x, double y, double z) {
        return new Vec3(x, ground + y, z);
    }

    private static void script(List<String> lines) {
        then(1, () -> server(FurnaceShots::build));
        then(60, () -> server(() -> {
            if (level().getBlockEntity(rel(3, 3, 6)) instanceof WaterWheelBlockEntity w) result.add("wheel power " + w.power());
            if (level().getBlockEntity(rel(3, 0, 0)) instanceof GreatBellowsBlockEntity b) result.add("small bellows power " + b.power());
        }));
        then(1, () -> feeding = true);
        for (String l : lines) {
            String[] p = l.trim().split("\\s+");
            if (p.length == 0 || p[0].isEmpty()) continue;
            switch (p[0]) {
                case "!light" -> then(2, () -> server(FurnaceShots::light));
                case "!heat" -> then(2, () -> server(() -> heat(Float.parseFloat(p[1]))));
                case "!fill" -> then(2, () -> server(FurnaceShots::fill));
                case "!demo" -> then(1, () -> demoAt.add(p.length > 3 ? new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])) : new BlockPos(20, 0, -4)));
                case "!clear" -> then(1, () -> server(() -> {
                    for (int x = 19; x <= 40; x++) for (int y = -1; y <= 0; y++) for (int z = -8; z <= -3; z++)
                        if (level().getBlockEntity(rel(x, y, z)) instanceof TroughBlockEntity t) t.take(t.amount(), level().getGameTime());
                }));
                case "!demostop" -> then(1, demoAt::clear);
                case "!clay" -> then(1, () -> server(() -> { // a Clay Channel joined to a trough (it cracks by hot iron)
                    BlockPos q = rel(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    level().setBlockAndUpdate(q, Smelting.CLAY_CHANNEL.get().defaultBlockState());
                    level().setBlockAndUpdate(q, Smelting.CLAY_CHANNEL.get().stateAt(level(), q));
                }));
                // night views: brighter gamma so the dark yard still reads around the glowing metal
                case "!gamma" -> then(1, () -> Minecraft.getInstance().options.gamma().set(Double.parseDouble(p[1])));
                case "!extras" -> then(2, () -> server(() -> {
                    if (level().getBlockEntity(rel(8, 0, -12)) instanceof FineryHearthBlock.Entity e) {
                        e.fuel = 16;
                        e.pig = 3;
                        e.setForTest(1400);
                        level().setBlockAndUpdate(rel(8, 0, -12), level().getBlockState(rel(8, 0, -12)).setValue(FineryHearthBlock.LIT, true));
                    }
                    if (level().getBlockEntity(rel(5, 0, -12)) instanceof CokeOvenBlock.Entity o) {
                        o.coal = CokeOvenBlock.BATCH;
                        o.start(level());
                    }
                    if (level().getBlockEntity(rel(10, -1, -5)) instanceof FoundryBasinBlock.Entity b) {
                        b.loam = FoundryBasinBlock.Loam.BELL;
                        b.sync();
                    }
                }));
                case "!gate" -> then(2, () -> server(() -> {
                    BlockPos g = rel(12, -1, -6);
                    level().setBlockAndUpdate(g, level().getBlockState(g).setValue(RefractoryTroughBlock.Gate.OPEN, p.length < 2)); // "!gate shut" shuts it
                }));
                case "!pig" -> then(2, () -> server(() -> {
                    if (level().getBlockEntity(rel(SMALL_FRONT.getX(), SMALL_FRONT.getY(), SMALL_FRONT.getZ())) instanceof HighFurnaceBlockEntity f) {
                        f.pool().add(Molten.PIG_IRON, Integer.parseInt(p[1]));
                        f.select(Molten.PIG_IRON);
                    }
                }));
                case "!gold", "!melt" -> then(2, () -> server(() -> {
                    if (level().getBlockEntity(rel(BIG_FRONT.getX(), BIG_FRONT.getY(), BIG_FRONT.getZ())) instanceof HighFurnaceBlockEntity f) {
                        Molten m = p[0].equals("!gold") ? Molten.GOLD : Molten.byId(p[2]);
                        f.pool().add(m, Integer.parseInt(p[1]));
                        f.select(m);
                    }
                }));
                case "!shot" -> then(1, () -> shot(p[1]));
                case "!ladle" -> then(2, () -> server(() -> { // two Steel Ladles in frames on a brick post at (2, 0, -9): empty and full
                    set(2, 0, -9, Blocks.BRICKS.defaultBlockState());
                    set(2, 1, -9, Blocks.BRICKS.defaultBlockState());
                    for (int y = 0; y <= 1; y++) {
                        var frame = new net.minecraft.world.entity.decoration.ItemFrame(level(), rel(2, y, -10), Direction.NORTH);
                        ItemStack ladle = new ItemStack(HighFurnace.STEEL_LADLE.get());
                        if (y == 1) SteelLadleItem.set(ladle, Molten.STEEL, 800);
                        frame.setItem(ladle);
                        level().addFreshEntity(frame);
                    }
                }));
                // the player out of frame but within 32 blocks, so block cracks, particles and sounds reach the client
                case "!player" -> then(2, () -> server(() -> {
                    var pl = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    pl.getAbilities().flying = true;
                    pl.onUpdateAbilities();
                    pl.teleportTo(level(), Double.parseDouble(p[1]), ground + Double.parseDouble(p[2]), Double.parseDouble(p[3]), java.util.Set.of(), 0, 0, true);
                }));
                case "!probe" -> then(1, () -> server(() -> { // the casting pit's end: the trough, the short spout and the flask
                    StringBuilder b = new StringBuilder("probe " + p[1] + ":");
                    if (level().getBlockEntity(rel(12, -1, -7)) instanceof TroughBlockEntity t) b.append(" trough ").append(t.amount());
                    if (level().getBlockEntity(rel(12, -1, -8)) instanceof PouringSpoutBlockEntity sp) b.append(" spout ").append(sp.stream()).append(" buffer ").append(sp.bufferForTest());
                    if (level().getBlockEntity(rel(12, -2, -8)) instanceof MoldingFlaskBlock.Entity f)
                        b.append(" flask ").append(level().getBlockState(f.getBlockPos()).getValue(MoldingFlaskBlock.STAGE)).append(" filled ").append(f.filled());
                    result.add(b.toString());
                }));
                case "!time" -> then(2, () -> cmd("time set " + p[1]));
                case "!wait" -> then(Integer.parseInt(p[1]), () -> {});
                case "!gui" -> {
                    then(2, () -> server(() -> {
                        var pl = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
                        pl.teleportTo(level(), 10.5, ground, -3.5, java.util.Set.of(), 0, 20, true);
                        if (level().getBlockEntity(rel(10, 0, -1)) instanceof HighFurnaceBlockEntity f) pl.openMenu(f, buf -> buf.writeBlockPos(f.getBlockPos()));
                    }));
                    then(20, () -> shot("gui"));
                    then(2, () -> Minecraft.getInstance().player.closeContainer());
                }
                default -> {
                    then(8, () -> view(at(Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])),
                            at(Double.parseDouble(p[4]), Double.parseDouble(p[5]), Double.parseDouble(p[6]))));
                    then(20, () -> shot(p[0]));
                }
            }
        }
    }

    private FurnaceShots() {}
}
