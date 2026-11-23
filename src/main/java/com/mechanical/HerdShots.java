package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.Wildlife;

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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link ColdShots}): with a file {@code herdshots.flag} in the game directory (first line: an output
 * folder; optional second line: comma-separated subject names to shoot only those), the client opens the world
 * "HerdShots", builds a grass platform at y 200 and photographs the big herbivores and their young (moose, deer, reindeer,
 * aurochs, mouflon, boar, wild and tame horse): old_herd_&lt;name&gt;.png overview, then at eye height _front/_side/_q,
 * grazing (_graze), asleep at night (_lying) and as a skinned carcass (_carcass); writes herd_result.txt and quits.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HerdShots {
    /** name, type, baby, sex flag saved-data key (null: none), overview distance, close-up distance. */
    private record Subject(String name, Supplier<? extends EntityType<?>> type, boolean baby, String flag, double distance, double close) {}

    private static final String[] VIEWS = {"front", "side", "q", "graze", "lying", "carcass"};

    private static final List<Subject> SUBJECTS = List.of(
            new Subject("moose_bull", Wildlife.MOOSE, false, "bull", 7.0, 4.2),
            new Subject("moose_cow", Wildlife.MOOSE, false, null, 6.5, 3.8),
            new Subject("moose_calf", Wildlife.MOOSE, true, null, 4.0, 2.2),
            new Subject("giant_deer", Wildlife.GIANT_DEER, false, null, 8.5, 5.5),
            new Subject("red_deer_stag", Wildlife.RED_DEER, false, "stag", 5.5, 3.2),
            new Subject("red_deer_hind", Wildlife.RED_DEER, false, null, 5.0, 2.8),
            new Subject("red_deer_calf", Wildlife.RED_DEER, true, null, 3.2, 1.8),
            new Subject("roe_deer", Wildlife.ROE_DEER, false, null, 3.8, 2.2),
            new Subject("roe_deer_fawn", Wildlife.ROE_DEER, true, null, 2.6, 1.4),
            new Subject("reindeer_bull", Wildlife.REINDEER, false, "bull", 5.0, 3.0),
            new Subject("reindeer_cow", Wildlife.REINDEER, false, null, 4.8, 2.8),
            new Subject("reindeer_calf", Wildlife.REINDEER, true, null, 3.2, 1.8),
            new Subject("aurochs_bull", Wildlife.AUROCHS, false, "bull", 6.5, 3.8),
            new Subject("aurochs_cow", Wildlife.AUROCHS, false, null, 6.0, 3.5),
            new Subject("aurochs_calf", Wildlife.AUROCHS, true, null, 3.6, 2.0),
            new Subject("mouflon_ram", Wildlife.MOUFLON, false, "ram", 3.8, 2.2),
            new Subject("mouflon_ewe", Wildlife.MOUFLON, false, null, 3.6, 2.0),
            new Subject("mouflon_lamb", Wildlife.MOUFLON, true, null, 2.6, 1.4),
            new Subject("wild_boar", Wildlife.WILD_BOAR, false, null, 4.0, 2.2),
            new Subject("piglet", Wildlife.WILD_BOAR, true, null, 2.4, 1.2),
            new Subject("wild_horse", Wildlife.WILD_HORSE, false, null, 5.5, 3.2),
            new Subject("wild_horse_foal", Wildlife.WILD_HORSE, true, null, 3.6, 2.0),
            new Subject("tame_horse", Wildlife.TAME_HORSE, false, null, 5.5, 3.2),
            new Subject("tame_horse_saddled", Wildlife.TAME_HORSE, false, "saddle", 5.5, 3.2),
            new Subject("tame_horse_armor", Wildlife.TAME_HORSE, false, "armor", 5.5, 3.2));

    private static int stage = -1, ticks, index, view;
    private static File out;
    private static List<String> only = List.of();
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static Entity current;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "herdshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                if (lines.size() > 1 && !lines.get(1).isBlank()) only = List.of(lines.get(1).trim().split("\\s*,\\s*"));
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("HerdShots", () -> {});
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
                        p.teleportTo(p.level(), p.getX() + 600, 205, p.getZ(), java.util.Set.of(), 0, 20, true);
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
                case 2 -> { // place the next subject
                    if (ticks < 40) return;
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
                    if (ticks < 110) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name());
                    result.add(s.name() + ": " + current + " bb " + current.getBbWidth() + "x" + current.getBbHeight()
                            + (current instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse ? " saddled " + horse.isSaddled() : ""));
                    view = 0;
                    server.execute(() -> closeUp(s, VIEWS[0]));
                    stage = 6;
                    ticks = 0;
                }
                case 6 -> {
                    String v = VIEWS[view];
                    if (v.equals("graze") && current instanceof WildAnimal a) server.execute(() -> setData(a, "GRAZE", WildAnimal.GRAZE_TICKS / 2));
                    if (ticks < (v.equals("lying") ? 40 : 20)) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name() + "_" + v);
                    ticks = 0;
                    if (++view < VIEWS.length) {
                        String nv = VIEWS[view];
                        server.execute(() -> closeUp(s, nv));
                        return;
                    }
                    server.execute(() -> cmd("time set 6000"));
                    index++;
                    stage = 2;
                }
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "herd_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "herd_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    /** Sets one of WildAnimal's private synced fields (GRAZE / DOZING), as its goals would. */
    @SuppressWarnings("unchecked")
    private static <V> void setData(WildAnimal a, String field, V value) {
        try {
            var f = WildAnimal.class.getDeclaredField(field);
            f.setAccessible(true);
            a.getEntityData().set((EntityDataAccessor<V>) f.get(null), value);
        } catch (ReflectiveOperationException e) {
            result.add("setData " + field + ": " + e);
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()) + 600, z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 201, z + 0.5);
        cmd("gamemode creative @a");
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        cmd("gamerule doDaylightCycle false");
        cmd("time set 6000");
        cmd("weather clear");
        cmd("difficulty peaceful");
        cmd("effect give @a night_vision infinite 0 true");
        cmd("fill " + (x - 14) + " 200 " + (z - 14) + " " + (x + 14) + " 200 " + (z + 14) + " grass_block");
        cmd("fill " + (x - 14) + " 201 " + (z - 14) + " " + (x + 14) + " 216 " + (z + 14) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static void place(Subject s) {
        ServerLevel level = player().level();
        if (current != null) current.discard();
        Entity e = s.type().get().create(level, EntitySpawnReason.COMMAND);
        if (e == null) return;
        if (e instanceof net.minecraft.world.entity.LivingEntity living && "saddle".equals(s.flag())) {
            living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new net.minecraft.world.item.ItemStack(Carting.HIDE_SADDLE_BLANKET.get()));
        } else if (e instanceof net.minecraft.world.entity.LivingEntity living && "armor".equals(s.flag())) {
            living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HORSE_ARMOR));
        } else if (s.flag() != null) { // the sex is only read from saved data
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putBoolean(s.flag(), true);
            e.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
        }
        float yaw = 225;
        e.snapTo(base.x, base.y, base.z, yaw, 0);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setPersistenceRequired();
        }
        if (s.baby() && e instanceof AgeableMob a) a.setBaby(true);
        level.addFreshEntity(e);
        current = e;
        Vec3 mid = base.add(0, e.getBbHeight() * 0.5, 0);
        Vec3 eye = mid.add(new Vec3(-0.55, 0.35, -0.75).normalize().scale(s.distance()));
        aim(eye, mid);
    }

    private static void closeUp(Subject s, String v) {
        if (current == null) return;
        if (v.equals("lying")) {
            cmd("time set 18000");
            if (current instanceof WildAnimal a) setData(a, "DOZING", true);
            v = "q";
        }
        if (v.equals("graze")) v = "q";
        if (v.equals("carcass")) {
            cmd("time set 6000");
            if (current instanceof net.minecraft.world.entity.LivingEntity living) {
                CarcassEntity body = CarcassEntity.of(current.level(), living);
                body.setSkinned();
                current.discard();
                current.level().addFreshEntity(body);
                current = body;
            }
            Vec3 mid = base.add(0, 0.3, 0);
            aim(mid.add(new Vec3(-0.6, 0.55, -0.6).normalize().scale(s.close() * 1.1)), mid);
            return;
        }
        double yaw = Math.toRadians(225);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), left = new Vec3(fwd.z, 0, -fwd.x);
        Vec3 dir = switch (v) {
            case "front" -> fwd;
            case "side" -> left;
            default -> fwd.add(left).normalize();
        };
        double eyeY = base.y + Math.max(current.getEyeHeight(), 0.15);
        Vec3 look = new Vec3(base.x, eyeY - current.getBbHeight() * 0.25, base.z);
        aim(new Vec3(base.x, eyeY, base.z).add(dir.scale(s.close())), look);
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
                image.writeToFile(new File(out, "old_herd_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private HerdShots() {}
}
