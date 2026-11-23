package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mechanical.wildlife.coast.CoastFauna;
import com.mechanical.wildlife.river.RiverFauna;
import com.mechanical.wildlife.warm.WarmFauna;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
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
 * Dev only (like {@link ColdShots}): with a file {@code owarmshots.flag} in the game directory (its first line: an output
 * folder), the client opens the world "OwarmShots", builds a sand platform with a deep pool at y 200 and photographs the
 * older warm, coast and river animals (wildlife.warm, wildlife.coast, wildlife.river): old_warm_&lt;name&gt;.png, then close
 * up at eye height from the front, the side and three-quarter (_front/_side/_q.png) and as a skinned carcass
 * (_carcass.png); water subjects (fish, swimming seals, walruses, beavers, otters) hang mid-pool and are shot from under
 * water. Then writes old_warm_result.txt and quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class OwarmShots {
    /** name, type, baby, overview distance, close-up distance, in the pool, extra setup on the new entity. */
    private record Subject(String name, Supplier<? extends EntityType<?>> type, boolean baby, double distance, double close,
                           boolean water, Consumer<Entity> setup) {}

    private static final String[] VIEWS = {"front", "side", "q", "carcass"};
    private static final String[] WATER_VIEWS = {"front", "side", "q"};

    private static Consumer<Entity> flag(String key) {
        return e -> {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putBoolean(key, true);
            e.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, e.level().registryAccess(), tag));
        };
    }

    /** The peacock's fan held half-way through a display (the counter is private and runs down by itself). */
    private static final Consumer<Entity> FAN = e -> {
        try {
            var f = com.mechanical.wildlife.warm.Peafowl.class.getDeclaredField("DISPLAY");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            var key = (net.minecraft.network.syncher.EntityDataAccessor<Integer>) f.get(null);
            e.setOnGround(true); // Peafowl.aiStep folds the fan when not on the ground
            e.getEntityData().set(key, com.mechanical.wildlife.warm.Peafowl.DISPLAY_TICKS / 2);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    };

    private static final List<Subject> ALL = List.of(
            new Subject("onager", WarmFauna.ONAGER, false, 4.5, 2.6, false, null),
            new Subject("onager_foal", WarmFauna.ONAGER, true, 3.0, 1.8, false, null),
            new Subject("tame_onager", WarmFauna.TAME_ONAGER, false, 4.5, 2.6, false, null),
            new Subject("water_buffalo", WarmFauna.WATER_BUFFALO, false, 5.5, 3.4, false, null),
            new Subject("water_buffalo_calf", WarmFauna.WATER_BUFFALO, true, 3.2, 2.0, false, null),
            new Subject("tapir", WarmFauna.TAPIR, false, 4.2, 2.4, false, null),
            new Subject("tapir_calf", WarmFauna.TAPIR, true, 2.6, 1.5, false, null),
            new Subject("tortoise", WarmFauna.TORTOISE, false, 2.0, 1.5, false, null),
            new Subject("desert_lizard", WarmFauna.DESERT_LIZARD, false, 1.6, 1.1, false, null),
            new Subject("leopard", WarmFauna.LEOPARD, false, 3.8, 2.2, false, null),
            new Subject("hornbill", WarmFauna.HORNBILL, false, 2.2, 1.2, false, null),
            new Subject("jungle_fowl", WarmFauna.JUNGLE_FOWL, false, 1.9, 1.1, false, flag("rooster")),
            new Subject("jungle_fowl_hen", WarmFauna.JUNGLE_FOWL, false, 1.9, 1.1, false, null),
            new Subject("peacock", WarmFauna.PEAFOWL, false, 2.6, 1.5, false, flag("peacock")),
            new Subject("peacock_fan", WarmFauna.PEAFOWL, false, 3.0, 1.9, false, flag("peacock").andThen(FAN)),
            new Subject("peahen", WarmFauna.PEAFOWL, false, 2.2, 1.3, false, null),
            new Subject("seal", CoastFauna.SEAL, false, 3.0, 1.8, false, null),
            new Subject("seal_swim", CoastFauna.SEAL, false, 3.0, 2.0, true, null),
            new Subject("walrus", CoastFauna.WALRUS, false, 5.0, 3.0, false, null),
            new Subject("walrus_swim", CoastFauna.WALRUS, false, 5.0, 3.2, true, null),
            new Subject("pike", CoastFauna.PIKE, false, 2.0, 1.3, true, null),
            new Subject("sturgeon", CoastFauna.STURGEON, false, 3.0, 2.0, true, null),
            new Subject("beaver", RiverFauna.BEAVER, false, 2.4, 1.3, false, null),
            new Subject("beaver_swim", RiverFauna.BEAVER, false, 2.4, 1.4, true, null),
            new Subject("otter", RiverFauna.OTTER, false, 2.4, 1.3, false, null),
            new Subject("otter_swim", RiverFauna.OTTER, false, 2.4, 1.4, true, null),
            new Subject("mosquito_swarm", RiverFauna.MOSQUITO_SWARM, false, 2.4, 1.6, false, null));

    private static List<Subject> subjects = ALL;
    private static int stage = -1, ticks, index, view;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base, pool;
    private static Entity current;

    private static String[] views(Subject s) {
        return s.water() || s.name().equals("mosquito_swarm") ? WATER_VIEWS : VIEWS;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "owarmshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                // optional second line: only these subjects (comma separated)
                if (lines.size() > 1 && !lines.get(1).isBlank()) {
                    List<String> only = List.of(lines.get(1).trim().split("\\s*,\\s*"));
                    subjects = ALL.stream().filter(s -> only.contains(s.name())).toList();
                }
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("OwarmShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            if ((stage == 3 || stage == 6) && current != null && index < subjects.size() && !subjects.get(index).water()) {
                // NoAI mobs never run their movement, so nothing sets onGround: without it birds pose as in flight
                Entity seen = mc.level.getEntity(current.getId());
                if (seen != null) seen.setOnGround(true);
            }
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
                case 5 -> {
                    if (ticks < 80) return;
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() - 600, p.getY(), p.getZ(), java.util.Set.of(), 0, 20, true);
                        setup();
                    });
                    stage = 2;
                    ticks = 0;
                }
                case 2 -> {
                    if (ticks < 40) return;
                    if (index >= subjects.size()) {
                        stage = 4;
                        ticks = 0;
                        return;
                    }
                    Subject s = subjects.get(index);
                    server.execute(() -> place(s));
                    stage = 3;
                    ticks = 0;
                }
                case 3 -> {
                    Subject s = subjects.get(index);
                    if (ticks == 110 && s.name().endsWith("_fan")) server.execute(() -> { if (current != null) FAN.accept(current); });
                    if (ticks < 130) return;
                    shot(mc, s.name());
                    result.add(s.name() + ": " + current);
                    view = 0;
                    server.execute(() -> closeUp(s, views(s)[0]));
                    stage = 6;
                    ticks = 0;
                }
                case 6 -> {
                    if (ticks < 22) return;
                    Subject s = subjects.get(index);
                    shot(mc, s.name() + "_" + views(s)[view]);
                    ticks = 0;
                    if (++view < views(s).length) {
                        String v = views(s)[view];
                        server.execute(() -> closeUp(s, v));
                        return;
                    }
                    index++;
                    stage = 2;
                }
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "old_warm_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "old_warm_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    /** A 25x25 sand platform at y 200, 600 blocks east of the spawn, with a 9x9 pool 5 deep north of the middle. */
    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()) + 600, z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 201, z + 0.5);
        pool = new Vec3(x + 0.5, 198.2, z + 9.5);
        cmd("gamemode creative @a");
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        cmd("gamerule doDaylightCycle false");
        cmd("time set noon");
        cmd("weather clear");
        cmd("difficulty peaceful");
        cmd("effect give @a conduit_power infinite 0 true"); // clear sight under water
        cmd("effect give @a night_vision infinite 0 true");
        cmd("fill " + (x - 12) + " 194 " + (z - 12) + " " + (x + 12) + " 200 " + (z + 22) + " sandstone");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 200 " + (z + 22) + " sand");
        cmd("fill " + (x - 6) + " 195 " + (z + 4) + " " + (x + 6) + " 200 " + (z + 16) + " water");
        cmd("fill " + (x - 12) + " 201 " + (z - 12) + " " + (x + 12) + " 215 " + (z + 22) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static Vec3 at(Subject s) {
        return s.water() ? pool : base;
    }

    private static void place(Subject s) {
        ServerLevel level = player().level();
        if (current != null) current.discard();
        Entity e = s.type().get().create(level, EntitySpawnReason.COMMAND);
        if (e == null) return;
        if (s.setup() != null) s.setup().accept(e);
        float yaw = 225;
        Vec3 c = at(s);
        double y = s.name().equals("mosquito_swarm") ? c.y + 0.6 : c.y;
        e.snapTo(c.x, y, c.z, yaw, 0);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setPersistenceRequired();
        }
        if (e instanceof AgeableMob a) a.setBaby(s.baby());
        e.setOnGround(!s.water());
        level.addFreshEntity(e);
        current = e;
        Vec3 mid = e.position().add(0, e.getBbHeight() * 0.5, 0);
        Vec3 dir = s.water() ? new Vec3(-0.6, 0.05, -0.8) : new Vec3(-0.55, 0.35, -0.75);
        aimAt(mid, mid.add(dir.normalize().scale(s.distance())));
    }

    private static void closeUp(Subject s, String v) {
        if (current == null) return;
        if (v.equals("carcass")) {
            if (current instanceof LivingEntity living) {
                CarcassEntity body = CarcassEntity.of(current.level(), living);
                body.setSkinned();
                current.discard();
                current.level().addFreshEntity(body);
                current = body;
            }
            v = "q";
        } else if (s.name().endsWith("_fan")) {
            FAN.accept(current);
        }
        double yaw = Math.toRadians(225);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), left = new Vec3(fwd.z, 0, -fwd.x);
        Vec3 dir = switch (v) {
            case "front" -> fwd;
            case "side" -> left;
            default -> fwd.add(left).normalize();
        };
        Vec3 c = current.position();
        double eyeY = c.y + Math.max(current.getEyeHeight(), 0.15);
        Vec3 look = new Vec3(c.x, eyeY - current.getBbHeight() * 0.15, c.z);
        aimAt(look, new Vec3(c.x, eyeY, c.z).add(dir.scale(s.close())));
    }

    private static void aimAt(Vec3 look, Vec3 eye) {
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
                image.writeToFile(new File(out, "old_warm_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private OwarmShots() {}
}
