package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link ColdShots}): with a file {@code predshots.flag} in the game directory (its first line: an output
 * folder), the client opens the world "PredShots", builds a grass platform at y 200 and photographs each of the older
 * predators and Ice Age giants (brown bear, wolf, cave lion, mammoth, woolly rhino, steppe bison, musk ox, and their young):
 * old_pred_&lt;name&gt;.png three-quarter with its hitbox drawn and a villager beside it for scale, then at its eye height
 * from the front, the side and three-quarter (_front/_side/_q), attacking (_attack), asleep (_sleep) and as a skinned
 * carcass (_carcass); then writes old_pred_result.txt and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class PredShots {
    /** name, type, baby, camera distance, close-up distance. */
    private record Subject(String name, Supplier<? extends EntityType<?>> type, boolean baby, double distance, double close) {}

    private static final String[] VIEWS = {"front", "face", "side", "q", "attack", "sleep", "carcass"};

    private static List<Subject> SUBJECTS = List.of(
            new Subject("brown_bear", com.mechanical.wildlife.Wildlife.BROWN_BEAR, false, 5.0, 2.8),
            new Subject("brown_bear_cub", com.mechanical.wildlife.Wildlife.BROWN_BEAR, true, 2.6, 1.8),
            new Subject("grey_wolf", com.mechanical.wildlife.Wildlife.GREY_WOLF, false, 3.0, 2.2),
            new Subject("cave_lion", com.mechanical.wildlife.Wildlife.CAVE_LION, false, 4.2, 2.4),
            new Subject("woolly_mammoth", com.mechanical.wildlife.giants.GiantsFauna.WOOLLY_MAMMOTH, false, 10.0, 6.0),
            new Subject("woolly_mammoth_calf", com.mechanical.wildlife.giants.GiantsFauna.WOOLLY_MAMMOTH, true, 5.0, 2.8),
            new Subject("woolly_rhino", com.mechanical.wildlife.giants.GiantsFauna.WOOLLY_RHINO, false, 5.5, 3.8),
            new Subject("woolly_rhino_calf", com.mechanical.wildlife.giants.GiantsFauna.WOOLLY_RHINO, true, 3.0, 1.7),
            new Subject("steppe_bison", com.mechanical.wildlife.giants.GiantsFauna.STEPPE_BISON, false, 5.5, 3.4),
            new Subject("steppe_bison_bull", com.mechanical.wildlife.giants.GiantsFauna.STEPPE_BISON, false, 5.5, 3.4),
            new Subject("steppe_bison_calf", com.mechanical.wildlife.giants.GiantsFauna.STEPPE_BISON, true, 3.0, 1.7),
            new Subject("musk_ox", com.mechanical.wildlife.giants.GiantsFauna.MUSK_OX, false, 5.0, 2.8),
            new Subject("musk_ox_calf", com.mechanical.wildlife.giants.GiantsFauna.MUSK_OX, true, 2.8, 1.6),
            // share the brown bear's skin layout (recoloured) and the cub's model: checked so they stay right
            new Subject("cave_bear", com.mechanical.wildlife.mountain.MountainFauna.CAVE_BEAR, false, 5.5, 3.2),
            new Subject("cave_bear_cub", com.mechanical.wildlife.mountain.MountainFauna.CAVE_BEAR, true, 2.6, 1.8));

    private static int stage = -1, ticks, index, view;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static Entity current, villager;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "predshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                if (lines.size() > 1 && !lines.get(1).isBlank()) {
                    List<String> only = List.of(lines.get(1).trim().split(","));
                    SUBJECTS = SUBJECTS.stream().filter(s -> only.contains(s.name())).toList();
                }
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("PredShots", () -> {});
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
                case 2 -> { // place the next subject
                    if (ticks < 40) return;
                    if (index >= SUBJECTS.size()) {
                        stage = 4;
                        ticks = 0;
                        return;
                    }
                    Subject s = SUBJECTS.get(index);
                    mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.ALWAYS_ON);
                    server.execute(() -> place(s));
                    stage = 3;
                    ticks = 0;
                }
                case 3 -> { // let the chunk render, then shoot
                    if (ticks < 100) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name());
                    result.add(s.name() + ": " + current + " bb " + current.getBbWidth() + " x " + current.getBbHeight());
                    view = 0;
                    server.execute(() -> closeUp(s, VIEWS[0]));
                    stage = 6;
                    ticks = 0;
                }
                case 6 -> { // the close-ups, one view at a time
                    if (ticks == 1) mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.NEVER);
                    // asleep: set on the client copy every tick (the server's wake check clears it at noon)
                    if (current != null && mc.level != null && mc.level.getEntity(current.getId()) instanceof com.mechanical.wildlife.WildAnimal a)
                        setDozing(a, VIEWS[view].equals("sleep"));
                    if (ticks < 25) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name() + "_" + VIEWS[view]);
                    ticks = 2;
                    if (++view < VIEWS.length) {
                        String v = VIEWS[view];
                        server.execute(() -> closeUp(s, v));
                        return;
                    }
                    index++;
                    stage = 2;
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
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "old_pred_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "old_pred_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    /** A 31x31 grass platform at y 200, 600 blocks east of the spawn, clear sky above, noon forever. */
    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()) + 600, z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 201, z + 0.5);
        cmd("gamemode creative @a");
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        cmd("time set noon");
        cmd("gamerule advance_time false");
        cmd("gamerule spawn_mobs false");
        cmd("weather clear");
        cmd("difficulty peaceful");
        cmd("fill " + (x - 15) + " 200 " + (z - 15) + " " + (x + 15) + " 200 " + (z + 15) + " grass_block");
        cmd("fill " + (x - 15) + " 201 " + (z - 15) + " " + (x + 15) + " 215 " + (z + 15) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static void freeze(Entity e, float yaw) {
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setPersistenceRequired();
        }
    }

    private static void place(Subject s) {
        ServerLevel level = player().level();
        if (current != null) current.discard();
        if (villager != null) villager.discard();
        Entity e = s.type().get().create(level, EntitySpawnReason.COMMAND);
        if (e == null) return;
        float yaw = 225; // head to the north-east: from the camera (north-west of it) a three-quarter view of its left side
        e.snapTo(base.x, base.y, base.z, yaw, 0);
        freeze(e, yaw);
        if (s.name().endsWith("_bull")) { // the bull flag is only read from saved data
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putBoolean("bull", true);
            e.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
            e.snapTo(base.x, base.y, base.z, yaw, 0);
            freeze(e, yaw);
        }
        if (s.baby() && e instanceof AgeableMob a) a.setBaby(true);
        level.addFreshEntity(e);
        current = e;
        // a villager (about player size) beside it, for scale
        villager = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        if (villager != null) {
            double off = e.getBbWidth() * 0.5 + 1.4;
            villager.snapTo(base.x - off * 1.2, base.y, base.z + off * 0.3, yaw, 0); // beside its hindquarters, clear of the head
            freeze(villager, yaw);
            level.addFreshEntity(villager);
        }
        Vec3 mid = base.add(0, e.getBbHeight() * 0.5, 0);
        Vec3 eye = mid.add(new Vec3(-0.55, 0.35, -0.75).normalize().scale(s.distance() * 0.75));
        aim(eye, mid);
    }

    private static void aim(Vec3 eye, Vec3 at) {
        Vec3 d = at.subtract(eye);
        float camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float camPitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        ServerPlayer p = player();
        p.teleportTo(p.level(), eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), camYaw, camPitch, true);
    }

    /** The camera at the subject's eye height, {@code close} blocks away: in front, at its left side, or three-quarter front-left. */
    private static void closeUp(Subject s, String v) {
        if (current == null) return;
        if (villager != null) {
            villager.discard();
            villager = null;
        }
        if (current instanceof Mob m) m.setAggressive(v.equals("attack"));
        if (v.equals("carcass")) { // skinned, as after the first butchering pass (the carcass texture)
            if (current instanceof net.minecraft.world.entity.LivingEntity living) {
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
            case "front" -> fwd;
            case "side" -> left;
            default -> fwd.add(left).normalize();
        };
        if (v.equals("face")) { // straight at the head, from just in front of it, level with the eyes
            double eyeY = base.y + current.getBbHeight() * 0.95;
            Vec3 head = new Vec3(base.x, eyeY, base.z).add(fwd.scale(current.getBbWidth() * 0.6));
            aim(head.add(fwd.scale(s.close() * 0.6)), head);
            return;
        }
        double scale = v.equals("carcass") || v.equals("sleep") ? 1.0 : 0.85;
        double eyeY = base.y + Math.max(current.getEyeHeight(), 0.15) * (v.equals("carcass") || v.equals("sleep") ? 0.8 : 1.0);
        Vec3 look = new Vec3(base.x, eyeY - current.getBbHeight() * 0.25, base.z);
        aim(new Vec3(base.x, eyeY, base.z).add(dir.scale(s.close() * scale)), look);
    }

    /** WildAnimal's dozing flag is private (only its sleep goal sets it): dev-only reflection. */
    private static void setDozing(Entity e, boolean on) {
        if (!(e instanceof com.mechanical.wildlife.WildAnimal)) return;
        try {
            var f = com.mechanical.wildlife.WildAnimal.class.getDeclaredField("DOZING");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            var key = (net.minecraft.network.syncher.EntityDataAccessor<Boolean>) f.get(null);
            e.getEntityData().set(key, on);
        } catch (ReflectiveOperationException ex) {
            result.add("dozing: " + ex);
        }
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "old_pred_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private PredShots() {}
}
