package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link ColdShots}): with a file {@code vanillashots.flag} in the game directory (first line: an output
 * folder; optional second line: comma-separated subject names to shoot only those) the client opens the world
 * "VanillaShots", builds a grass platform at y 200 and photographs each vanilla mob the player still meets, beside the
 * mod animal it should stand up to (the cow by the aurochs, the pig by the wild boar ...): vanilla_&lt;name&gt;.png, then
 * alone at its eye height from the front, the side and three-quarter (_front/_side/_q) and, for livestock, as a skinned
 * carcass (_carcass); writes vanilla_result.txt and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class VanillaShots {
    /** name, entity id, NBT, baby, overview distance, close-up distance, mod animal beside it (or null), carcass view. */
    private record Subject(String name, String id, String nbt, boolean baby, double distance, double close, String compare, boolean carcass) {}

    private static Subject s(String name, String id, String nbt, double distance, double close, String compare) {
        return new Subject(name, id, nbt, false, distance, close, compare, false);
    }

    private static Subject live(String name, String id, String nbt, boolean baby, double distance, double close, String compare) {
        return new Subject(name, id, nbt, baby, distance, close, compare, true);
    }

    private static final List<Subject> SUBJECTS = List.of(
            live("cow", "cow", "{variant:\"minecraft:temperate\"}", false, 5.5, 2.6, "mechanical:aurochs"),
            live("cow_warm", "cow", "{variant:\"minecraft:warm\"}", false, 4.5, 2.6, null),
            live("cow_cold", "cow", "{variant:\"minecraft:cold\"}", false, 4.5, 2.6, null),
            live("cow_calf", "cow", "{variant:\"minecraft:temperate\"}", true, 2.6, 1.6, null),
            live("pig", "pig", "{variant:\"minecraft:temperate\"}", false, 4.0, 2.0, "mechanical:wild_boar"),
            live("pig_warm", "pig", "{variant:\"minecraft:warm\"}", false, 3.2, 2.0, null),
            live("pig_cold", "pig", "{variant:\"minecraft:cold\"}", false, 3.2, 2.0, null),
            live("piglet", "pig", "{variant:\"minecraft:temperate\"}", true, 2.0, 1.2, null),
            live("sheep", "sheep", "{Color:0b}", false, 4.5, 2.2, "mechanical:mouflon"),
            live("sheep_brown", "sheep", "{Color:12b}", false, 3.5, 2.2, null),
            live("sheep_sheared", "sheep", "{Color:0b,Sheared:1b}", false, 3.5, 2.2, null),
            live("lamb", "sheep", "{Color:0b}", true, 2.2, 1.3, null),
            live("chicken", "chicken", "{variant:\"minecraft:temperate\"}", false, 2.6, 1.2, "mechanical:jungle_fowl"),
            live("chicken_warm", "chicken", "{variant:\"minecraft:warm\"}", false, 2.0, 1.2, null),
            live("chicken_cold", "chicken", "{variant:\"minecraft:cold\"}", false, 2.0, 1.2, null),
            live("chick", "chicken", "{variant:\"minecraft:temperate\"}", true, 1.4, 0.8, null),
            s("wolf", "wolf", "{variant:\"minecraft:pale\"}", 4.0, 2.0, "mechanical:grey_wolf"),
            s("wolf_woods", "wolf", "{variant:\"minecraft:woods\"}", 3.0, 2.0, null),
            s("cat", "cat", "{variant:\"minecraft:tabby\"}", 2.6, 1.2, "mechanical:wild_cat"),
            s("rabbit", "rabbit", "{}", 2.4, 1.0, "mechanical:hare"),
            s("fox", "fox", "{}", 2.6, 1.4, null),
            s("spider", "spider", "{}", 3.5, 2.0, null),
            s("cave_spider", "cave_spider", "{}", 2.2, 1.2, null),
            s("bat", "bat", "{}", 1.6, 0.9, null),
            s("villager", "villager", "{}", 3.0, 1.8, null),
            s("iron_golem", "iron_golem", "{}", 5.0, 3.0, null),
            s("witch", "witch", "{}", 3.0, 1.8, null),
            s("enderman", "enderman", "{}", 4.5, 2.6, null),
            s("slime", "slime", "{Size:1}", 2.6, 1.5, null),
            s("husk", "husk", "{}", 3.0, 1.8, null),
            s("drowned", "drowned", "{}", 3.0, 1.8, null),
            s("stray", "stray", "{}", 3.0, 1.8, null),
            s("panda", "panda", "{}", 4.0, 2.2, null),
            s("parrot", "parrot", "{}", 1.6, 0.9, null),
            s("ocelot", "ocelot", "{}", 2.6, 1.3, null),
            s("turtle", "turtle", "{}", 2.6, 1.4, null),
            s("frog", "frog", "{}", 1.6, 0.8, null),
            s("armadillo", "armadillo", "{}", 1.8, 1.0, null),
            s("camel", "camel", "{}", 6.0, 3.5, null),
            s("goat", "goat", "{}", 3.0, 1.8, null),
            s("bee", "bee", "{}", 1.4, 0.8, null),
            s("squid", "squid", "{}", 3.0, 1.6, null),
            s("dolphin", "dolphin", "{}", 3.5, 2.0, null),
            s("salmon", "salmon", "{}", 1.6, 0.9, null),
            s("cod", "cod", "{}", 1.6, 0.9, null),
            s("mooshroom", "mooshroom", "{}", 4.5, 2.6, null),
            s("piglin", "piglin", "{}", 3.0, 1.8, null),
            s("hoglin", "hoglin", "{}", 4.5, 2.6, null),
            s("ghast", "ghast", "{}", 12.0, 7.0, null),
            s("blaze", "blaze", "{}", 3.5, 2.0, null),
            s("strider", "strider", "{}", 3.0, 1.8, null));

    /** The close-ups after the overview: from the front, the left side and three-quarter front, the camera at eye height. */
    private static final String[] VIEWS = {"front", "side", "q", "carcass"};

    private static int stage = -1, ticks, index, view;
    private static File out;
    private static List<String> only = List.of();
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static Entity current, beside;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "vanillashots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                if (lines.size() > 1 && !lines.get(1).isBlank()) only = Arrays.stream(lines.get(1).split(",")).map(String::trim).toList();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("VanillaShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> {
                    mc.options.pauseOnLostFocus = false;
                    if (mc.player.isDeadOrDying()) mc.player.respawn();
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() + 600, 205, p.getZ(), java.util.Set.of(), 0, 20, true); // load the chunks there first
                    });
                    stage = 5;
                    ticks = 0;
                }
                case 5 -> { // build once the chunks over there are loaded
                    if (ticks < 80) return;
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() - 600, p.getY(), p.getZ(), java.util.Set.of(), 0, 20, true);
                        setup();
                    });
                    stage = 2;
                    ticks = 0;
                }
                case 2 -> { // place the next subject
                    if (ticks < 30) return;
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
                case 3 -> { // let it settle and the chunk render, then shoot
                    if (ticks < 90) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name());
                    result.add(s.name() + ": " + current);
                    view = 0;
                    server.execute(() -> closeUp(s, VIEWS[0]));
                    stage = 6;
                    ticks = 0;
                }
                case 6 -> { // the close-ups, one view at a time
                    if (ticks < 20) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name() + "_" + VIEWS[view]);
                    ticks = 0;
                    if (++view < (s.carcass() ? VIEWS.length : VIEWS.length - 1)) {
                        String v = VIEWS[view];
                        server.execute(() -> closeUp(s, v));
                        return;
                    }
                    index++;
                    stage = 2;
                }
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "vanilla_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "vanilla_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    /** A 25x25 grass platform at y 200, 600 blocks east of the spawn, clear sky above, noon, no spawns. */
    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()) + 600, z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 201, z + 0.5);
        cmd("gamemode creative @a");
        p.getAbilities().flying = true; // hangs where it is put, as the camera
        p.onUpdateAbilities();
        cmd("gamerule advance_time false");
        cmd("gamerule doDaylightCycle false");
        cmd("gamerule spawn_mobs false");
        cmd("time set noon");
        cmd("weather clear");
        cmd("difficulty easy"); // peaceful would remove the monsters
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 200 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 201 " + (z - 12) + " " + (x + 12) + " 215 " + (z + 12) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static Entity spawn(ServerLevel level, String id, String nbt, boolean baby, Vec3 at, float yaw) throws Exception {
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id.contains(":") ? id : "minecraft:" + id));
        Entity e = type.create(level, EntitySpawnReason.COMMAND);
        if (e == null) return null;
        if (nbt != null && !nbt.equals("{}"))
            e.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), TagParser.parseCompoundFully(nbt)));
        e.snapTo(at.x, at.y, at.z, yaw, 0);
        e.setNoGravity(id.equals("bat") || id.equals("ghast") || id.equals("bee") || id.equals("blaze") || id.equals("parrot"));
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setPersistenceRequired();
            m.setOnGround(true); // no AI, no physics: else a chicken thinks it is falling and flaps
        }
        if (baby && e instanceof AgeableMob a) a.setBaby(true);
        level.addFreshEntity(e);
        return e;
    }

    private static void place(Subject s) {
        ServerLevel level = player().level();
        if (current != null) current.discard();
        if (beside != null) beside.discard();
        beside = null;
        float yaw = 225; // head to the north-east: from the camera (north-west of it) a three-quarter view of its left side
        Vec3 view = new Vec3(-0.55, 0, -0.75).normalize(), across = new Vec3(0.75, 0, -0.55).normalize();
        try {
            current = spawn(level, s.id(), s.nbt(), s.baby(), base, yaw);
            if (s.compare() != null) beside = spawn(level, s.compare(), "{}", false, base.add(across.scale(s.distance() * 0.45)), yaw);
        } catch (Exception ex) {
            result.add(s.name() + " failed: " + ex);
            return;
        }
        if (current == null) return;
        Vec3 mid = base.add(0, current.getBbHeight() * 0.5, 0);
        if (beside != null) mid = mid.add(across.scale(s.distance() * 0.22));
        Vec3 eye = mid.add(view.add(0, 0.47, 0).normalize().scale(s.distance() * 0.7));
        aim(eye, mid);
    }

    /** The camera at the subject's eye height, {@code close} blocks away: in front, at its left side, or three-quarter front-left. */
    private static void closeUp(Subject s, String v) {
        if (current == null) return;
        if (beside != null) {
            beside.discard();
            beside = null;
        }
        if (v.equals("carcass")) { // skinned, as after the first butchering pass (the carcass texture)
            if (current instanceof LivingEntity living) {
                CarcassEntity body = CarcassEntity.of(current.level(), living);
                body.setSkinned();
                current.discard();
                current.level().addFreshEntity(body);
                current = body;
            }
            v = "q";
        }
        double yaw = Math.toRadians(225);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), left = new Vec3(fwd.z, 0, -fwd.x);
        Vec3 dir = switch (v) {
            case "front" -> fwd;
            case "side" -> left;
            default -> fwd.add(left).normalize();
        };
        double eyeY = base.y + Math.max(current.getEyeHeight(), 0.15);
        Vec3 look = new Vec3(base.x, eyeY - current.getBbHeight() * 0.15, base.z);
        aim(new Vec3(base.x, eyeY, base.z).add(dir.scale(s.close())), look);
    }

    private static void aim(Vec3 eye, Vec3 at) {
        Vec3 d = at.subtract(eye);
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
                image.writeToFile(new File(out, "vanilla_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private VanillaShots() {}
}
