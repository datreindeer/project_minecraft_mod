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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with {@code flintshots.flag} in the game directory (first line: output folder) the client makes a flat world
 * "FlintShots", gives the player a Flint and Steel (flint in the empty other hand) and an unlit campfire, and strikes it with
 * real uses: a picture every tick through a strike in third person (front three-quarter, then side) and in first person,
 * plus one at rest; also checks the fire caught, the wear, and that vanilla's ground fire still works. Writes
 * flint_result.txt and quits. Nothing happens without the flag file. Pattern: BugShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FlintShots {
    private static final String WORLD = "FlintShots";
    private static int stage = -1, ticks, step;
    private static File out;
    private static ArmorStand camera;
    private static Vec3 body;
    private static BlockPos fire;
    private static int ground;
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>(); // {wait ticks, Runnable}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "flintshots.flag");
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
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.SURVIVAL, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(78L, false, false),
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
                Files.write(new File(out, "flint_result.txt").toPath(), result);
                stage = -2;
                mc.stop();
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
            try {
                Files.write(new File(out, "flint_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void then(int wait, Runnable r) {
        STEPS.add(new Object[] {wait, r});
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static void server(Runnable r) {
        mc().getSingleplayerServer().execute(r);
    }

    private static void script() {
        then(1, () -> server(() -> {
            var server = mc().getSingleplayerServer();
            for (String c : List.of("time set noon", "gamerule advance_time false", "weather clear", "gamerule spawn_mobs false")) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
            }
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            ground = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            p.teleportTo(p.level(), 0.5, ground, 0.5, java.util.Set.of(), 0, 30, true);
            fire = new BlockPos(0, ground, 2);
            unlit();
            p.getInventory().setItem(0, new ItemStack(Mechanical.FLINT_AND_STEEL.get()));
            p.getInventory().setSelectedSlot(0);
            body = new Vec3(0.5, ground + 1.0, 0.5);
        }));
        // third person, front three-quarter: at rest, then every tick of a strike
        then(40, () -> view(body.add(1.5, 0.45, 1.9), body.add(0, 0.15, 0.2)));
        then(10, () -> shot("tp_rest"));
        then(2, FlintShots::strike);
        for (int t = 1; t <= FlintAndSteelItem.STRIKE_TICKS; t++) {
            int tt = t;
            then(1, () -> shot("tp_" + tt));
        }
        then(10, () -> {
            result.add("campfire lit by one strike: " + mc().getSingleplayerServer().overworld().getBlockState(fire).getValue(CampfireBlock.LIT));
            result.add("wear after one strike: " + mc().player.getMainHandItem().getDamageValue() + "/" + mc().player.getMainHandItem().getMaxDamage());
            server(FlintShots::unlit);
            view(body.add(1.9, 0.35, 0.35), body.add(0, 0.15, 0.35)); // from the flint hand's side
        });
        then(15, FlintShots::strike);
        for (int t = 1; t <= FlintAndSteelItem.STRIKE_TICKS; t++) {
            int tt = t;
            then(1, () -> shot("side_" + tt));
        }
        // first person, hand visible
        then(15, () -> {
            server(FlintShots::unlit);
            mc().setCameraEntity(mc().player);
            mc().options.setCameraType(CameraType.FIRST_PERSON);
            mc().player.setXRot(30);
            if (mc().gui.hud.isHidden()) mc().gui.hud.toggle();
        });
        then(25, () -> shot("fp_rest"));
        then(2, FlintShots::strike);
        for (int t = 1; t <= FlintAndSteelItem.STRIKE_TICKS; t++) {
            int tt = t;
            then(1, () -> shot("fp_" + tt));
        }
        // vanilla's part: fire on the ground
        then(10, () -> {
            BlockPos at = new BlockPos(2, ground - 1, 0);
            mc().gameMode.useItemOn(mc().player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false));
        });
        then(10, () -> result.add("fire set on the ground (vanilla behaviour): " + mc().getSingleplayerServer().overworld().getBlockState(new BlockPos(2, ground, 0)).is(Blocks.FIRE)));
    }

    private static void unlit() {
        mc().getSingleplayerServer().overworld().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
    }

    /** A real use on the campfire, and the swing it starts (Minecraft#startUseItem's, with the item's interact animation). */
    private static void strike() {
        Minecraft mc = mc();
        Vec3 c = Vec3.atCenterOf(fire);
        var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(c.add(0, 0.2, 0), Direction.UP, fire, false));
        FlintShots.result.add("use: " + result);
        mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getInteractAnimation(), false);
    }

    private static void shot(String name) {
        Screenshot.takeScreenshot(mc().gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "flint_" + name + ".png"));
            } catch (Exception ignored) {}
        });
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

    private FlintShots() {}
}
