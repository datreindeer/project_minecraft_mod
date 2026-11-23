package com.mechanical.client.wildlife.jungle;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.jungle.JungleFauna;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with a file {@code jungleshots.flag} in the game directory (its first line: an output folder; an optional
 * second line: the shot names to take, comma-separated), the client opens the world "JungleShots", builds a grass
 * platform at y 200 and photographs each jungle creature in turn: jungle_<name>.png from the front left quarter, then
 * close-ups at its eye height (jungle_<name>_front/_side/_q.png) and, for the big ones, its carcass
 * (jungle_<name>_carcass.png); then quits. Does nothing without the flag file. Same pattern as NatureShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class JungleShots {
    private static int stage = -1, ticks, index;
    private static File out;
    private static Vec3 base;
    private static Mob current;
    private static CarcassEntity carcass;
    private static Set<String> only;
    private static final List<String> result = new ArrayList<>();

    /**
     * name, type, overview camera distance and height, close-up distance, UUID parity (picks the variant: the dart frog's
     * colour, the proboscis monkey's and fiddler crab's sex; -1 any), whether to shoot its carcass.
     */
    private record Shot(String name, java.util.function.Supplier<? extends EntityType<? extends Mob>> type, double dist, double height,
            double close, int parity, boolean carcass) {}

    private static final List<Shot> SHOTS = List.of(
            new Shot("tiger", JungleFauna.TIGER::get, 4.2, 1.4, 3.0, -1, true),
            new Shot("python", JungleFauna.PYTHON::get, 3.6, 1.6, 2.4, -1, true),
            new Shot("orangutan", JungleFauna.ORANGUTAN::get, 3.6, 1.2, 2.6, -1, true),
            new Shot("monitor_lizard", JungleFauna.MONITOR_LIZARD::get, 3.0, 1.2, 2.0, -1, true),
            new Shot("poison_dart_frog", JungleFauna.POISON_DART_FROG::get, 0.9, 0.5, 0.7, 0, false),
            new Shot("poison_dart_frog_yellow", JungleFauna.POISON_DART_FROG::get, 0.9, 0.5, 0.7, 1, false),
            new Shot("saltwater_crocodile", JungleFauna.SALTWATER_CROCODILE::get, 5.2, 2.0, 3.6, -1, true),
            new Shot("mudskipper", JungleFauna.MUDSKIPPER::get, 1.0, 0.5, 0.8, -1, false),
            new Shot("fiddler_crab", JungleFauna.FIDDLER_CRAB::get, 1.0, 0.55, 0.8, 0, false),
            new Shot("fiddler_crab_female", JungleFauna.FIDDLER_CRAB::get, 1.0, 0.55, 0.8, 1, false),
            new Shot("proboscis_monkey", JungleFauna.PROBOSCIS_MONKEY::get, 2.8, 1.0, 2.2, 0, true),
            new Shot("proboscis_monkey_female", JungleFauna.PROBOSCIS_MONKEY::get, 2.8, 1.0, 2.2, 1, false));
    /** Close-up camera directions round the creature (it faces +z): head-on, its right flank, the front right quarter. */
    private static final String[] VIEWS = {"front", "side", "q"};

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "jungleshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                if (lines.size() > 1 && !lines.get(1).isBlank()) only = Set.of(lines.get(1).trim().split("\\s*,\\s*"));
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("JungleShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            if (stage == 1) {
                mc.options.pauseOnLostFocus = false;
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks < 100) return;
                server.execute(JungleShots::setup);
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); // F1
                mc.gui.toastManager().clear();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                stage = 2;
                ticks = 0;
                return;
            }
            if (stage == 2) {
                if (index >= SHOTS.size()) {
                    if (ticks < 20) return;
                    Files.write(new File(out, "jungle_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                    return;
                }
                Shot s = SHOTS.get(index);
                if (only != null && !only.contains(s.name())) {
                    index++;
                    ticks = 0;
                    return;
                }
                if (ticks == 20) server.execute(() -> place(s));
                if (ticks == 50) shot(mc, s.name());
                for (int v = 0; v < VIEWS.length; v++) { // each close-up: camera moved, photographed 8 ticks later
                    int at = 55 + v * 12, view = v;
                    if (ticks == at) server.execute(() -> view(s, view));
                    if (ticks == at + 8) shot(mc, s.name() + "_" + VIEWS[view]);
                }
                int end = 55 + VIEWS.length * 12;
                if (s.carcass()) {
                    if (ticks == end) server.execute(() -> toCarcass(s));
                    if (ticks == end + 20) shot(mc, s.name() + "_carcass");
                    end += 25;
                }
                if (ticks == end) {
                    server.execute(() -> {
                        if (current != null) current.discard();
                        if (carcass != null) carcass.discard();
                        carcass = null;
                    });
                    index++;
                    ticks = 0;
                }
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "jungle_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** The creature at the platform's centre, facing south (+z), still; the camera off its front left quarter. */
    private static void place(Shot s) {
        ServerLevel level = player().level();
        Mob mob = s.type().get().create(level, EntitySpawnReason.COMMAND);
        if (s.parity() >= 0) mob.setUUID(new UUID(level.getRandom().nextLong(), 2L * level.getRandom().nextInt(1 << 20) + s.parity()));
        mob.snapTo(base.x, base.y, base.z, 0, 0);
        mob.setYHeadRot(0);
        mob.setYBodyRot(0);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        current = mob;
        lookAt(base.add(-s.dist() * 0.8, s.height(), s.dist() * 0.6), base.add(0, mob.getBbHeight() * 0.5, 0));
        result.add(s.name() + " size " + mob.getBbWidth() + " x " + mob.getBbHeight() + " eye " + mob.getEyeHeight());
    }

    /** Close-up {@code v} with the camera at the creature's eye height. */
    private static void view(Shot s, int v) {
        double eye = Math.max(current.getEyeHeight(), 0.15) + 0.03, c = s.close();
        Vec3 cam = switch (v) {
            case 0 -> base.add(0, eye, c);
            case 1 -> base.add(-c, eye, 0);
            default -> base.add(-c * 0.7, eye, c * 0.7);
        };
        lookAt(cam, base.add(0, Math.min(eye, current.getBbHeight() * 0.5), 0));
    }

    /** The creature as a carcass lying where it stood, seen from a little above. */
    private static void toCarcass(Shot s) {
        ServerLevel level = player().level();
        carcass = CarcassEntity.of(level, current);
        current.discard();
        level.addFreshEntity(carcass);
        double c = s.close();
        lookAt(base.add(-c * 0.6, c * 0.6, c * 0.7), base.add(0, 0.2, 0));
    }

    private static void lookAt(Vec3 cam, Vec3 target) {
        Vec3 d = target.subtract(cam);
        float yaw = (float) (Math.atan2(-d.x, d.z) * 180 / Math.PI);
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        ServerPlayer p = player();
        p.teleportTo(p.level(), cam.x, cam.y - p.getEyeHeight(), cam.z, Set.of(), yaw, pitch, true);
    }

    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 200, z + 0.5);
        cmd("gamemode spectator @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("gamerule doDaylightCycle false");
        cmd("gamerule doMobSpawning false");
        cmd("kill @e[type=!minecraft:player]");
        cmd("fill " + (x - 12) + " 197 " + (z - 12) + " " + (x + 12) + " 198 " + (z + 12) + " dirt");
        cmd("fill " + (x - 12) + " 199 " + (z - 12) + " " + (x + 12) + " 199 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 212 " + (z + 12) + " air");
        result.add("setup at " + base);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "jungle_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private JungleShots() {}
}
