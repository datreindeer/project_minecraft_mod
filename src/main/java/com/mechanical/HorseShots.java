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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link LogShots}): with a file {@code horseshots.flag} in the game directory (its first line: an output
 * folder), the client opens the world "HorseShots", builds a platform at y 200 with a wagon and its team (you riding the
 * lead horse), a pack horse with a body on each side and an exhausted horse, and takes screenshots (horse_*.png) of the
 * stamina HUD and the horses; then it quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HorseShots {
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static AbstractHorse lead, pack, tired;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "horseshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("HorseShots", () -> {});
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
                    server.execute(HorseShots::setup);
                    next();
                }
                case 2 -> { // riding the lead horse of the team: the HUD shows both bars
                    if (ticks < 60) return;
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    mc.player.setYRot(150);
                    mc.player.setXRot(20);
                    next();
                }
                case 3 -> {
                    if (ticks < 20) return;
                    shot(mc, "hud_team");
                    result.add("riding " + mc.player.getVehicle());
                    server.execute(() -> {
                        player().stopRiding();
                        look(pack.position().add(-3.2, 0.6, 0.0), -90, 15); // west of the pack horse, looking east at its left side
                    });
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    next();
                }
                case 4 -> {
                    if (ticks < 30) return;
                    shot(mc, "side_carcasses_left");
                    server.execute(() -> look(pack.position().add(3.2, 0.6, 0.0), 90, 15)); // east: its right side
                    next();
                }
                case 5 -> {
                    if (ticks < 30) return;
                    shot(mc, "side_carcasses_right");
                    server.execute(() -> look(tired.position().add(-2.5, 1.0, -2.5), -45, 25));
                    next();
                }
                case 6 -> {
                    if (ticks < 30) return;
                    shot(mc, "exhausted");
                    result.add("tired horse exhausted (client) " + HorseWork.exhausted((AbstractHorse) mc.level.getEntity(tired.getId())));
                    next();
                }
                case 7 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "horse_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "horse_result.txt").toPath(), result);
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

    private static AbstractHorse horse(ServerLevel level, Vec3 at, float yaw) {
        AbstractHorse h = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.EVENT);
        h.setPos(at);
        h.setYRot(yaw);
        h.setYBodyRot(yaw);
        h.setYHeadRot(yaw);
        h.setTamed(true);
        h.setNoAi(true); // stands for the picture
        level.addFreshEntity(h);
        return h;
    }

    private static void body(ServerLevel level, EntityType<?> type, AbstractHorse horse, int side) {
        var animal = (net.minecraft.world.entity.LivingEntity) type.create(level, EntitySpawnReason.EVENT);
        animal.setPos(horse.position());
        CarcassEntity c = CarcassEntity.of(level, animal);
        level.addFreshEntity(c);
        c.slingOn(horse, side);
    }

    /** Server: platform at y 199; a wagon facing south with its team (lead 70 % stamina, partner tired), you on the lead horse; a pack horse; an exhausted horse. */
    private static void setup() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        p.setHealth(p.getMaxHealth());
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 200, z + 0.5);
        cmd("gamemode survival @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("fill " + (x - 12) + " 199 " + (z - 12) + " " + (x + 12) + " 199 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 210 " + (z + 12) + " air");
        cmd("kill @e[type=!player,distance=..40]");
        WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        wagon.setPos(base);
        wagon.setYRot(0);
        level.addFreshEntity(wagon);
        lead = horse(level, base.add(-WagonEntity.PAIR / 2, 0, WagonEntity.LEAD), 0);
        lead.setNoAi(false);
        AbstractHorse partner = horse(level, base.add(WagonEntity.PAIR / 2, 0, WagonEntity.LEAD), 0);
        wagon.setHarness(lead);
        wagon.setPartner(partner);
        HorseWork.setStamina(lead, 70);
        HorseWork.setStamina(partner, 20);
        p.startRiding(lead, true, true);
        pack = horse(level, base.add(6, 0, 4), 0);
        body(level, EntityTypes.PIG, pack, 0);
        body(level, EntityTypes.RABBIT, pack, 1);
        tired = horse(level, base.add(-6, 0, 4), 30);
        HorseWork.setStamina(tired, 0);
        HorseWork.exhaust(tired);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "horse_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private HorseShots() {}
}
