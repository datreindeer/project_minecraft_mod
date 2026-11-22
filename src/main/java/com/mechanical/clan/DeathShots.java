package com.mechanical.clan;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.client.DownedClient;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only, like LogShots: with {@code deathshots.flag} in the game directory (first line: the output folder) the client
 * opens the world "DeathShots", goes down on a platform at y 200 (Downed#goDown directly: singleplayer has no tribe
 * mate), takes shots of the downed screen early and late, in third person and while holding R, dies, and takes shots of
 * the wake-up at world spawn and then in a bed (death_*.png, death_result.txt); then it quits. Nothing without the flag.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DeathShots {
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static BlockPos base;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "deathshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("DeathShots", () -> {});
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
                    mc.options.pauseOnLostFocus = false;
                    if (mc.player.isDeadOrDying()) mc.player.respawn();
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    server.execute(DeathShots::setup);
                    next();
                }
                case 2 -> {
                    if (ticks < 40) return;
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> Downed.goDown(player(), player().damageSources().generic()));
                    next();
                }
                case 3 -> {
                    if (ticks < 30) return;
                    mc.player.setXRot(-10);
                    result.add("downed: kind " + Downed.kind(mc.player) + ", pose " + mc.player.getPose() + ", eye " + mc.player.getEyeHeight());
                    shot(mc, "downed_early");
                    server.execute(() -> {
                        Downed.DOWNED.get(player().getUUID()).ticks = 1000;
                        Downed.sync(player());
                    });
                    next();
                }
                case 4 -> {
                    if (ticks < 20) return;
                    shot(mc, "downed_late");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    mc.player.setXRot(45);
                    next();
                }
                case 5 -> {
                    if (ticks < 10) return;
                    shot(mc, "downed_third");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    mc.player.setXRot(-10);
                    DownedClient.KEY.setDown(true); // hold R: the give-up bar
                    next();
                }
                case 6 -> {
                    if (ticks < 15) return;
                    shot(mc, "downed_give_up");
                    DownedClient.KEY.setDown(false);
                    next();
                }
                case 7 -> {
                    if (ticks < 10) return;
                    server.execute(() -> Downed.finish(player(), null));
                    next();
                }
                case 8 -> {
                    if (ticks < 30) return;
                    result.add("after finish: dead " + mc.player.isDeadOrDying() + ", screen " + mc.gui.screen());
                    mc.player.respawn();
                    mc.gui.setScreen(null);
                    next();
                }
                case 9 -> { // waking at world spawn: third person, eyes opening, standing up, then first person
                    if (ticks == 2) result.add("respawned: kind " + Downed.kind(mc.player) + ", camera " + mc.options.getCameraType());
                    if (ticks == 8 || ticks == 22 || ticks == 34 || ticks == 48 || ticks == 62 || ticks == 74) shot(mc, "wake_" + ticks);
                    if (ticks == 95) {
                        shot(mc, "wake_up");
                        result.add("woken: kind " + Downed.kind(mc.player) + ", camera " + mc.options.getCameraType() + ", pose " + mc.player.getPose()
                                + ", spirit " + Downed.SPIRIT.containsKey(mc.player.getUUID()));
                        server.execute(DeathShots::bed);
                        next();
                    }
                }
                case 10 -> {
                    if (ticks < 20) return;
                    server.execute(() -> player().kill(player().level()));
                    next();
                }
                case 11 -> {
                    if (ticks < 30) return;
                    mc.player.respawn();
                    mc.gui.setScreen(null);
                    next();
                }
                case 12 -> { // waking in the bed
                    if (ticks == 2) result.add("bed respawn: kind " + Downed.kind(mc.player) + " at " + mc.player.blockPosition());
                    if (ticks == 10 || ticks == 34 || ticks == 62) shot(mc, "bed_wake_" + ticks);
                    if (ticks == 95) {
                        result.add("bed woken: kind " + Downed.kind(mc.player) + ", spirit " + Downed.SPIRIT.containsKey(mc.player.getUUID())
                                + ", at " + mc.player.blockPosition());
                        next();
                    }
                }
                case 13 -> {
                    if (ticks < 10) return;
                    Files.write(new File(out, "death_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "death_result.txt").toPath(), result);
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

    private static void cmd(String c) {
        var server = player().level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** Server: a grass platform at y 200, world spawn on it, a sword in hand (hidden while down). */
    private static void setup() {
        ServerPlayer p = player();
        p.setHealth(p.getMaxHealth());
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new BlockPos(x, 200, z);
        cmd("gamemode survival @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("fill " + (x - 10) + " 199 " + (z - 10) + " " + (x + 10) + " 199 " + (z + 10) + " grass_block");
        cmd("fill " + (x - 10) + " 200 " + (z - 10) + " " + (x + 10) + " 206 " + (z + 10) + " air");
        cmd("fill " + (x + 3) + " 200 " + (z + 3) + " " + (x + 3) + " 202 " + (z + 3) + " oak_log");
        cmd("setworldspawn " + x + " 200 " + z);
        cmd("gamerule respawn_radius 0");
        cmd("gamerule spawn_radius 0");
        p.getInventory().setItem(p.getInventory().getSelectedSlot(), new ItemStack(Items.IRON_SWORD));
        p.teleportTo(p.level(), x + 0.5, 200, z + 0.5, java.util.Set.of(), 0, 10, true);
    }

    /** Server: a bed on the platform and the player's respawn point in it. */
    private static void bed() {
        ServerPlayer p = player();
        int x = base.getX() - 3, z = base.getZ();
        cmd("setblock " + x + " 200 " + z + " red_bed[facing=south,part=foot]");
        cmd("setblock " + x + " 200 " + (z + 1) + " red_bed[facing=south,part=head]");
        p.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(p.level().dimension(), new BlockPos(x, 200, z + 1), 0, 0), true), false);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "death_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private DeathShots() {}
}
