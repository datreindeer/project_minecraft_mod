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
 * Dev only: with {@code watershots.flag} in the game directory the client makes a fresh flat world "KitchenShots" and builds
 * a mill: an overshot Water Wheel fed by a stone race, its axle through a shaft and a Gearbox into a Saw Bench on a plank
 * floor, the Log Flume line fed from a stone basin, three logs in it. Then it shoots water_*.png. The flag file's first
 * line is the output folder; every further line is a view "name x y z tx ty tz" (relative to the hub's column at ground
 * level), so views can be changed without a rebuild; with none, a default set is shot. Writes water_result.txt and quits.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class KitchenShots {
    private static final String WORLD = "KitchenShots";
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
                File flag = new File(mc.gameDirectory, "kitchenshots.flag");
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
            Files.write(new File(out, "kitchen_result.txt").toPath(), result);
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
                image.writeToFile(new File(out, "kitchen_" + name + ".png"));
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

    private static void fill(int x, int y, int z, net.minecraft.world.item.Item... items) {
        if (!(level().getBlockEntity(new BlockPos(x, ground + y, z)) instanceof CookwareBlockEntity c)) {
            result.add("no cookware at " + x + " " + y + " " + z);
            return;
        }
        for (var item : items) c.add(level(), player(), net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(item));
    }

    private static void cook(int x, int y, int z, int ticks) {
        if (level().getBlockEntity(new BlockPos(x, ground + y, z)) instanceof CookwareBlockEntity c)
            for (int i = 0; i < ticks; i++) c.serverTick(level(), c.getBlockPos(), c.getBlockState());
    }

    private static void frame(int x, int y, int z, net.minecraft.world.item.Item item) {
        var f = new net.minecraft.world.entity.decoration.ItemFrame(level(), new BlockPos(x, ground + y, z), Direction.NORTH);
        f.setItem(new ItemStack(item), false);
        level().addFreshEntity(f);
    }

    private static void build() {
        for (String c : List.of("time set 6000", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                "gamerule spawn_mobs false", "difficulty peaceful"))
            cmd(c);
        ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        BlockState floor = Blocks.SPRUCE_PLANKS.defaultBlockState(), fire = Blocks.CAMPFIRE.defaultBlockState(), wall = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = -7; x <= 7; x++) for (int z = -5; z <= 4; z++) set(x, -1, z, floor);
        for (int x = -7; x <= 7; x++) for (int y = 0; y <= 3; y++) set(x, y, 4, wall);
        // the steel pans over their fires
        set(0, 0, 0, fire);
        set(0, 1, 0, Kitchen.STEEL_PAN.get().defaultBlockState());
        set(2, 0, 0, fire);
        set(2, 1, 0, Kitchen.STEEL_PAN.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
        // the mortar on a table, the sieve on the floor, the skillet, a barrel
        set(-2, 0, 0, Blocks.SPRUCE_PLANKS.defaultBlockState());
        set(-2, 1, 0, Kitchen.MORTAR_AND_PESTLE.get().defaultBlockState());
        set(-4, 0, 0, Kitchen.SIEVE.get().defaultBlockState());
        set(4, 0, 0, fire);
        set(4, 1, 0, Kitchen.IRON_SKILLET.get().defaultBlockState());
        set(-5, 0, 2, Kitchen.FERMENTING_BARREL.get().defaultBlockState());
        set(-6, 0, 2, Kitchen.MORTAR_AND_PESTLE.get().defaultBlockState());
        set(6, 0, 2, Kitchen.SIEVE.get().defaultBlockState());
        player().teleportTo(level(), 0.5, ground + 30, -14.5, java.util.Set.of(), 0, 20, true);
    }

    private static void stock() {
        fill(0, 1, 0, Items.BEEF, Items.BEEF, Kitchen.KITCHEN_PEPPER.get());
        fill(2, 1, 0, Herbs.ITEMS_BY_NAME.get("rice").get(), Herbs.ITEMS_BY_NAME.get("rice").get(), Kitchen.GOLDEN_SPICE.get(),
                Herbs.ITEMS_BY_NAME.get("onion").get());
        cook(2, 1, 0, Kitchen.PAN_TIME + 2);
        fill(-2, 1, 0, Herbs.BLACK_PEPPER.get(), Herbs.ITEMS_BY_NAME.get("ginger").get(), Herbs.CINNAMON_BARK.get());
        fill(-4, 0, 0, Farming.FLOUR.get());
        fill(4, 1, 0, Items.MUTTON, Items.MUTTON, Herbs.HERB_ITEMS.get("thyme").get(), Herbs.HERB_ITEMS.get("rosemary_sprigs").get());
        fill(-6, 0, 2, Herbs.ITEMS_BY_NAME.get("saffron").get());
        fill(6, 0, 2, Trees.FRUITS.get("date_palm").get());
        var items = new java.util.ArrayList<net.minecraft.world.item.Item>(List.of(Kitchen.STEEL_KNIFE_SET.get(), Kitchen.KITCHEN_PEPPER.get(),
                Kitchen.SWEET_SPICE.get(), Kitchen.GOLDEN_SPICE.get(), Kitchen.PICKLING_SPICE.get(), Kitchen.HERB_SALT.get(), Kitchen.FINE_FLOUR.get(),
                Kitchen.SEA_BUCKTHORN_JUICE.get(), Kitchen.CIDER.get(), Kitchen.ALE.get(), Kitchen.SAFFRON_RICE.get(), Kitchen.PEPPERED_STEAK.get(),
                Kitchen.SPICED_PILAF.get(), Kitchen.PICKLED_VEGETABLES.get(), Kitchen.GINGERBREAD.get(), Kitchen.ROAST_WITH_HERBS.get()));
        for (String f : Kitchen.PIE_FRUITS) items.add(Kitchen.PIES.get(f).get());
        for (int i = 0; i < items.size(); i++) frame(-5 + i % 10, 2 - i / 10, 3, items.get(i));
        result.add("stocked");
    }

    private static final String[] DEFAULT = {
            "overview 0.5 4.5 -5.5 0.5 1 1.5",
            "pans 1.5 2.9 -1.6 1.5 1.1 0.5",
            "pan_steak 0.5 2.4 -0.9 0.5 1.1 0.5",
            "pan_rice 2.5 2.4 -0.9 2.5 1.1 0.5",
            "mortar -1.5 2.5 -1.1 -1.5 1.25 0.5",
            "mortar_side -0.4 1.9 -0.2 -1.5 1.3 0.5",
            "sieve -3.5 1.6 -1.0 -3.5 0.2 0.5",
            "skillet 4.5 2.4 -0.9 4.5 1.1 0.5",
            "frames 0 2.2 0.6 0 1.7 3.5",
            "frames_close -2.5 2.4 1.9 -2.5 2.2 3.5"};

    private static void script() {
        then(1, () -> server(KitchenShots::build));
        then(40, () -> server(KitchenShots::stock));
        List<String> list = views.isEmpty() ? List.of(DEFAULT) : views;
        for (String v : list) {
            String[] p = v.split("\\s+");
            if (p[0].startsWith("mortar")) then(1, () -> server(() -> {
                if (level().getBlockEntity(new BlockPos(-2, ground + 1, 0)) instanceof CookwareBlockEntity m && !m.done() && m.match() != null)
                    m.work(level(), m.match()); // turn the pestle a step between shots
            }));
            then(10, () -> view(at(Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])),
                    at(Double.parseDouble(p[4]), Double.parseDouble(p[5]), Double.parseDouble(p[6]))));
            then(25, () -> shot(p[0]));
        }
    }

    private KitchenShots() {}
}
