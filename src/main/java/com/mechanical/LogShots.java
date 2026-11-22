package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with a file {@code logshots.flag} in the game directory (its first line: an output folder), the client opens
 * the world "LogShots", builds a platform at y 200 with felled logs, a wagon and a cart, and takes screenshots
 * (logs_*.png) of the logs, a lifted log in third and first person and a crack, then right-clicks the wagon and the cart
 * through the real client path while lifting and writes what happened to logs_result.txt; then it quits. Does nothing
 * without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class LogShots {
    /** Draw each felled log's collision boxes (FelledLogRenderer) while shots are taken. */
    public static boolean showShapes;
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static FelledLogEntity logZ, logX, logW;
    private static Entity wagon, cart;
    private static Vec3 base;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "logshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
                showShapes = true;
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("LogShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> {
                    if (ticks < 100) return;
                    server.execute(LogShots::setup);
                    next();
                }
                case 2 -> { // the layout from above, collision boxes drawn
                    if (ticks < 60) return;
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> look(base.add(2.5, 5, -2.5), 0, 55));
                    next();
                }
                case 3 -> {
                    if (ticks < 30) return;
                    shot(mc, "layout");
                    server.execute(() -> look(base.add(-0.5, 0, 2.5), -90, 30)); // beside the z log's west face
                    next();
                }
                case 4 -> {
                    if (ticks < 15) return;
                    double before = mc.player.getX();
                    mc.player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.8, 0, 0)); // client movement, into the log
                    result.add("walk into the z log: x " + before + " -> " + mc.player.getX() + " (west face at " + base.x + ", expected " + (base.x - 0.3) + ")");
                    FelledLogEntity cz = (FelledLogEntity) mc.level.getEntity(logZ.getId());
                    result.add("client z log at " + cz.position() + " yaw " + cz.getYRot() + ", server " + logZ.position() + " yaw " + logZ.getYRot());
                    // lift the z log's root end, standing north of it
                    server.execute(() -> {
                        look(logZ.endPoint(true).add(0, 0, -0.5), 180, 20);
                        logZ.lift((ServerLevel) logZ.level(), player(), 0);
                    });
                    next();
                }
                case 5 -> { // walk north 3 blocks with the log trailing (real client movement)
                    if (ticks > 5 && ticks <= 25) mc.player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, 0, -0.15));
                    if (ticks < 45) return;
                    result.add("lifting (client) " + (FelledLogEntity.liftedBy(mc.player) != null) + ", log root end at " + logZ.endPoint(true) + ", player at " + mc.player.position());
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    mc.player.setYRot(90); // camera east of the player, looking west at the log's side
                    mc.player.setXRot(15);
                    next();
                }
                case 6 -> {
                    if (ticks < 10) return;
                    shot(mc, "lift_third");
                    mc.player.setYRot(-90);
                    next();
                }
                case 7 -> {
                    if (ticks < 10) return;
                    shot(mc, "lift_third_other_side");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    mc.player.setYRot(180); // walking away, log behind
                    mc.player.setXRot(30);
                    next();
                }
                case 8 -> {
                    if (ticks < 10) return;
                    shot(mc, "lift_first_ahead");
                    mc.player.setYRot(0); // facing the log
                    mc.player.setXRot(35);
                    next();
                }
                case 9 -> {
                    if (ticks < 10) return;
                    shot(mc, "lift_first_facing");
                    // the cart, still lifting: must do nothing
                    Entity c = mc.level.getEntity(cart.getId());
                    mc.gameMode.interact(mc.player, c, new EntityHitResult(c), InteractionHand.MAIN_HAND);
                    next();
                }
                case 10 -> {
                    if (ticks < 10) return;
                    result.add("after right-clicking the cart while lifting: cart puller " + ((CartEntity) cart).puller() + ", still lifting " + (FelledLogEntity.liftedBy(player()) != null));
                    server.execute(() -> {
                        logZ.putDown(player());
                        look(logW.endPoint(true).add(0, 0, -0.5), 180, 20);
                        logW.lift((ServerLevel) logW.level(), player(), 0);
                    });
                    next();
                }
                case 11 -> {
                    if (ticks < 10) return;
                    Entity w = mc.level.getEntity(wagon.getId());
                    for (int k = 0; k < 3; k++) mc.gameMode.interact(mc.player, w, new EntityHitResult(w), InteractionHand.MAIN_HAND); // a held click repeats
                    next();
                }
                case 12 -> {
                    if (ticks < 10) return;
                    WagonEntity wg = (WagonEntity) wagon;
                    result.add("after right-clicking the wagon while lifting a 4-block log: harness " + wg.harness() + ", log on bed " + (wg.carriedLog() != null)
                            + ", lifting " + (FelledLogEntity.liftedBy(player()) != null) + ", player harnessed to " + TravoisEntity.harnessedTo(player()));
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    mc.player.setYRot(160);
                    next();
                }
                case 13 -> {
                    if (ticks < 15) return;
                    shot(mc, "wagon");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        logX.crack(1, 6);
                        look(logX.world(1).add(0, 0, -1.8), 0, 45);
                    });
                    next();
                }
                case 14 -> {
                    if (ticks < 20) return;
                    FelledLogEntity cx = (FelledLogEntity) mc.level.getEntity(logX.getId());
                    result.add("crack on the client: block " + cx.crackedBlock() + " stage " + cx.crackStage());
                    shot(mc, "crack");
                    next();
                }
                case 15 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "logs_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "logs_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void look(Vec3 at, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), at.x, at.y, at.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** Server: platform at y 199, logs along z and x, reference stone blocks at their ends, a wagon and a cart. */
    private static void setup() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x, 200, z);
        cmd("gamemode survival @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("fill " + (x - 10) + " 199 " + (z - 10) + " " + (x + 10) + " 199 " + (z + 12) + " stone");
        cmd("fill " + (x - 10) + " 200 " + (z - 10) + " " + (x + 10) + " 206 " + (z + 12) + " air");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20000, 0, false, false));
        var oak = Blocks.OAK_LOG.defaultBlockState();
        // z log: root block at (x, 200, z+1) .. crown (x, 200, z+5); stone right after its crown end at z+6
        logZ = FelledLogEntity.spawnStraight(level, new Vec3(x + 0.5, 200, z + 1.5), 0, 5, oak);
        level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x, 200, z + 6), Blocks.STONE.defaultBlockState());
        // x log: root block at (x+3, 200, z+1) .. (x+6, 200, z+1); stone at x+7
        logX = FelledLogEntity.spawnStraight(level, new Vec3(x + 3.5, 200, z + 1.5), -90, 4, oak);
        level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x + 7, 200, z + 1), Blocks.STONE.defaultBlockState());
        // a 4-block log to load: root at (x-5, z+1), lying south
        logW = FelledLogEntity.spawnStraight(level, new Vec3(x - 4.5, 200, z + 1.5), 0, 4, oak);
        wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        wagon.setPos(x - 4.5, 200, z - 2.0);
        level.addFreshEntity(wagon);
        cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
        cart.setPos(x + 1.5, 200, z - 3.5);
        level.addFreshEntity(cart);
        look(base.add(0.5, 0, -2.5), 0, 30);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "logs_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private LogShots() {}
}
