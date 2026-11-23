package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.forestbirds.ForestBirdsFauna;
import com.mechanical.wildlife.meadow.MeadowFauna;
import com.mechanical.wildlife.waterfowl.WaterfowlFauna;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link ColdShots}): with a file {@code smallshots.flag} in the game directory (its first line: an output
 * folder), the client opens the world "SmallShots", builds a grass platform at y 200 and photographs the older small
 * mammals and birds (the "small" review package): small_&lt;name&gt;.png (three-quarter overview), then close up at eye height
 * from the front, the side and three-quarter, mid-stride (_walk), birds also held in flight (_fly_q/_fly_side/_fly_front),
 * and last as a skinned carcass (_carcass). Writes small_result.txt and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SmallShots {
    /** flags: synced data to set, "NAME=value" comma-separated (booleans 1/0, ints); bird: also flight views; swim: in a pool. */
    private record Subject(String name, Supplier<? extends EntityType<?>> type, boolean baby, double distance, double close,
                           String flags, boolean bird, boolean swim) {}

    private static final String[] MAMMAL_VIEWS = {"front", "side", "q", "walk", "carcass"};
    private static final String[] BIRD_VIEWS = {"front", "side", "q", "walk", "fly_q", "fly_side", "fly_front", "carcass"};
    private static final String[] POSE_VIEWS = {"front", "side", "q"};

    private static final List<Subject> SUBJECTS = List.of(
            m("badger", Wildlife.BADGER, false, 2.2, 1.3, ""),
            m("hare", Wildlife.HARE, false, 2.0, 1.2, ""),
            m("hare_leveret", Wildlife.HARE, true, 1.4, 0.8, ""),
            m("desert_hare", Wildlife.DESERT_HARE, false, 2.0, 1.2, ""),
            m("hedgehog", Wildlife.HEDGEHOG, false, 1.4, 0.8, ""),
            pose("hedgehog_curled", Wildlife.HEDGEHOG, 1.4, 0.8, "CURLED=1"),
            m("squirrel", Wildlife.SQUIRREL, false, 1.4, 0.8, ""),
            m("field_vole", MeadowFauna.FIELD_VOLE, false, 1.0, 0.55, ""),
            m("mole", MeadowFauna.MOLE, false, 1.1, 0.6, ""),
            m("wild_cat", MeadowFauna.WILD_CAT, false, 2.2, 1.3, ""),
            m("wild_cat_kitten", MeadowFauna.WILD_CAT, true, 1.4, 0.8, ""),
            b("pheasant", Wildlife.PHEASANT, false, 2.2, 1.3, ""),
            m("pheasant_chick", Wildlife.PHEASANT, true, 1.0, 0.55, ""),
            b("crow", Wildlife.CROW, false, 1.6, 1.0, ""),
            b("goose", Wildlife.GOOSE, false, 2.2, 1.4, ""),
            b("rock_dove", Wildlife.ROCK_DOVE, false, 1.4, 0.85, ""),
            b("ptarmigan", Wildlife.PTARMIGAN, false, 1.5, 0.9, ""),
            b("ptarmigan_winter", Wildlife.PTARMIGAN, false, 1.5, 0.9, "WHITE=1"),
            b("seagull", Wildlife.SEAGULL, false, 1.8, 1.1, ""),
            b("capercaillie", ForestBirdsFauna.CAPERCAILLIE, false, 2.6, 1.6, ""),
            pose("capercaillie_display", ForestBirdsFauna.CAPERCAILLIE, 2.6, 1.6, "DISPLAY=100000"),
            b("capercaillie_hen", ForestBirdsFauna.CAPERCAILLIE, false, 2.2, 1.3, "HEN=1"),
            b("great_bustard", ForestBirdsFauna.BUSTARD, false, 3.0, 1.9, ""),
            b("owl", ForestBirdsFauna.OWL, false, 1.8, 1.1, ""),
            b("raven", ForestBirdsFauna.RAVEN, false, 1.9, 1.15, ""),
            b("golden_eagle", ForestBirdsFauna.GOLDEN_EAGLE, false, 2.6, 1.6, ""),
            b("vulture", ForestBirdsFauna.VULTURE, false, 2.8, 1.7, ""),
            b("mallard_hen", WaterfowlFauna.MALLARD, false, 1.8, 1.1, ""),
            b("mallard_drake", WaterfowlFauna.MALLARD, false, 1.8, 1.1, "DRAKE=1"),
            new Subject("mallard_swim", WaterfowlFauna.MALLARD, false, 1.8, 1.1, "DRAKE=1", false, true),
            b("mute_swan", WaterfowlFauna.MUTE_SWAN, false, 3.0, 1.9, ""),
            new Subject("mute_swan_swim", WaterfowlFauna.MUTE_SWAN, false, 3.0, 1.9, "", false, true),
            b("grey_heron", WaterfowlFauna.GREY_HERON, false, 3.2, 2.0, ""),
            b("common_crane", WaterfowlFauna.COMMON_CRANE, false, 3.4, 2.1, ""));

    private static Subject m(String n, Supplier<? extends EntityType<?>> t, boolean baby, double d, double c, String f) {
        return new Subject(n, t, baby, d, c, f, false, false);
    }

    private static Subject b(String n, Supplier<? extends EntityType<?>> t, boolean baby, double d, double c, String f) {
        return new Subject(n, t, baby, d, c, f, true, false);
    }

    /** A special pose: only the overview and the three close-ups. */
    private static Subject pose(String n, Supplier<? extends EntityType<?>> t, double d, double c, String f) {
        return new Subject(n, t, false, d, c, f + ",POSE=1", false, false);
    }

    private static String[] views(Subject s) {
        if (s.flags().contains("POSE=1") || s.swim()) return POSE_VIEWS;
        return s.bird() ? BIRD_VIEWS : MAMMAL_VIEWS;
    }

    private static int stage = -1, ticks, index, view;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static Entity current;
    private static boolean walking, airborne;
    /** Only these subjects (names, comma-separated, second line of the flag file); all when absent. */
    private static List<String> only = List.of();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "smallshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                if (lines.size() > 1 && !lines.get(1).isBlank()) only = List.of(lines.get(1).trim().split(","));
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("SmallShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            if (current != null && mc.level != null && mc.level.getEntity(current.getId()) instanceof LivingEntity l) {
                l.setOnGround(!airborne); // the client copy of a NoAI mob never learns it stands (the pheasant flies by that)
            }
            if (walking && current != null && mc.level != null) {
                // mid-stride, frozen: the client copy's walk cycle held at a fixed phase and speed
                Entity c = mc.level.getEntity(current.getId());
                if (c instanceof LivingEntity l) {
                    l.walkAnimation.stop();
                    l.walkAnimation.update(2.4F, 1.0F, 1.0F);
                    l.walkAnimation.setSpeed(0.9F);
                }
            }
            MinecraftServer server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> {
                    mc.options.pauseOnLostFocus = false;
                    if (mc.player.isDeadOrDying()) mc.player.respawn();
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                    mc.getWindow().setWindowed(1280, 720);
                    mc.options.fov().set(50);
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() + 700, 205, p.getZ(), java.util.Set.of(), 0, 20, true);
                    });
                    stage = 5;
                    ticks = 0;
                }
                case 5 -> {
                    if (ticks < 80) return;
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() - 700, p.getY(), p.getZ(), java.util.Set.of(), 0, 20, true);
                        setup();
                    });
                    stage = 2;
                    ticks = 0;
                }
                case 2 -> { // place the next subject
                    if (ticks < 20) return;
                    while (index < SUBJECTS.size() && !only.isEmpty() && !only.contains(SUBJECTS.get(index).name())) index++;
                    if (index >= SUBJECTS.size()) {
                        stage = 4;
                        ticks = 0;
                        return;
                    }
                    Subject s = SUBJECTS.get(index);
                    server.execute(() -> place(s));
                    stage = 3;
                    ticks = 0;
                }
                case 3 -> {
                    if (ticks < 60) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name());
                    result.add(s.name() + ": " + current);
                    view = 0;
                    server.execute(() -> closeUp(s, views(s)[0]));
                    stage = 6;
                    ticks = 0;
                }
                case 6 -> {
                    if (ticks < 20) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name() + "_" + views(s)[view]);
                    ticks = 0;
                    if (++view < views(s).length) {
                        String v = views(s)[view];
                        server.execute(() -> closeUp(s, v));
                        return;
                    }
                    walking = false;
                    index++;
                    stage = 2;
                }
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "small_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "small_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    /** A 25x25 grass platform at y 200, 700 blocks east of the spawn, clear sky above; a 5x5 pool 1 deep beside the middle. */
    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()) + 700, z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 201, z + 0.5);
        cmd("gamemode spectator @a"); // the camera can sit close without pushing the animal
        cmd("gamerule advance_time false");
        cmd("gamerule spawn_mobs false");
        cmd("time set noon");
        cmd("weather clear");
        cmd("difficulty peaceful");
        cmd("fill " + (x - 12) + " 199 " + (z - 12) + " " + (x + 12) + " 199 " + (z + 12) + " stone");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 200 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 201 " + (z - 12) + " " + (x + 12) + " 215 " + (z + 12) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static void place(Subject s) {
        airborne = walking = false;
        ServerLevel level = player().level();
        if (current != null) current.discard();
        int bx = (int) Math.floor(base.x), bz = (int) Math.floor(base.z);
        // the pool under the swimmers, grass again for the others
        cmd("fill " + (bx - 2) + " 200 " + (bz - 2) + " " + (bx + 2) + " 200 " + (bz + 2) + (s.swim() ? " water" : " grass_block"));
        Entity e = s.type().get().create(level, EntitySpawnReason.COMMAND);
        if (e == null) return;
        float yaw = 225;
        e.snapTo(base.x, base.y - (s.swim() ? 0.25 : 0), base.z, yaw, 0);
        if (e instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setYHeadRot(yaw);
            mob.yBodyRot = yaw;
            mob.setPersistenceRequired();
        }
        if (s.baby() && e instanceof AgeableMob a) a.setBaby(true);
        level.addFreshEntity(e);
        for (String f : s.flags().split(",")) {
            if (f.isBlank() || f.startsWith("POSE")) continue;
            String[] kv = f.split("=");
            setData(e, kv[0], Integer.parseInt(kv[1]));
        }
        current = e;
        Vec3 mid = base.add(0, e.getBbHeight() * 0.5, 0);
        Vec3 eye = mid.add(new Vec3(-0.55, 0.35, -0.75).normalize().scale(s.distance()));
        aim(eye, mid);
    }

    /** Sets a synced Boolean or Integer data field (a static EntityDataAccessor named {@code field} up the class chain). */
    @SuppressWarnings("unchecked")
    private static void setData(Entity e, String field, int value) {
        for (Class<?> c = e.getClass(); c != null; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(field);
                f.setAccessible(true);
                Object key = f.get(null);
                Object now = e.getEntityData().get((EntityDataAccessor<Object>) key);
                e.getEntityData().set((EntityDataAccessor<Object>) key, now instanceof Boolean ? (Object) (value != 0) : (Object) value);
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException ex) {
                result.add("data " + field + ": " + ex);
                return;
            }
        }
        result.add("no data field " + field + " on " + e.getClass().getSimpleName());
    }

    /** The camera at the subject's eye height, {@code close} blocks away: in front, at its left side, or three-quarter front-left. */
    private static void closeUp(Subject s, String v) {
        if (current == null) return;
        double lift = 0;
        walking = v.equals("walk");
        airborne = v.startsWith("fly");
        if (airborne) {
            if (v.equals("fly_q")) { // up into the air, wings going
                current.setNoGravity(true);
                if (current instanceof com.mechanical.wildlife.FlockBird) setData(current, "FLYING", 1);
                current.teleportTo(base.x, base.y + 1.2, base.z);
            }
            lift = 1.2;
        }
        if (v.equals("carcass")) {
            if (current instanceof com.mechanical.wildlife.FlockBird) setData(current, "FLYING", 0);
            current.teleportTo(base.x, base.y, base.z);
            if (current instanceof LivingEntity living) {
                CarcassEntity body = CarcassEntity.of(current.level(), living);
                body.setSkinned();
                current.discard();
                current.level().addFreshEntity(body);
                current = body;
            }
        }
        double yaw = Math.toRadians(225);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), left = new Vec3(fwd.z, 0, -fwd.x);
        Vec3 dir = switch (v) {
            case "front", "fly_front" -> fwd;
            case "side" -> left;
            case "fly_side" -> left.add(0, -0.45, 0).normalize(); // a little from below, as seen from the ground
            default -> fwd.add(left).normalize();
        };
        double scale = v.startsWith("fly") ? 2.2 : v.equals("carcass") ? 1.8 : 1.25;
        double eyeY = base.y + lift + Math.max(current.getEyeHeight(), 0.15);
        Vec3 look = new Vec3(base.x, eyeY - current.getBbHeight() * 0.15, base.z);
        Vec3 eye = new Vec3(base.x, eyeY, base.z).add(dir.scale(s.close() * scale));
        if (v.equals("carcass")) eye = eye.add(0, s.close() * 0.4, 0);
        aim(eye, look);
    }

    private static void aim(Vec3 eye, Vec3 look) {
        Vec3 d = look.subtract(eye);
        float camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float camPitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        ServerPlayer p = player();
        p.teleportTo(p.level(), eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), camYaw, camPitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "small_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private SmallShots() {}
}
