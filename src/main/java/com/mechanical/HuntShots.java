package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link ColdShots}): with a file {@code huntshots.flag} in the game directory (its first line: an output
 * folder), the client opens the world "HuntShots", builds a platform at y 200 and photographs the older "hunt" creatures
 * (wildlife.coldhunters, the Mushroom Man, the underground Vittra, Troll, Giant Centipede and Lindworm): hunt_&lt;name&gt;.png
 * in three-quarter view, then close up at eye height from the front, the side and three-quarter (_front/_side/_q) and,
 * for the animals, as a skinned carcass (_carcass). Then the tribespeople: a row of the four skins of each role and
 * climate, dyed as a tribe's (hunt_npc_&lt;climate&gt;_&lt;role&gt;.png, _back, and _close of the first two). Writes
 * hunt_result.txt and quits. An optional second line in the flag file limits the run to names starting with one of its
 * comma-separated prefixes (rows are named npc_&lt;climate&gt;_&lt;role&gt;). Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HuntShots {
    /** name, type, extra NBT (or null), baby, overview distance, close-up distance, carcass shot. */
    private record Subject(String name, Supplier<? extends EntityType<?>> type, CompoundTag nbt, boolean baby, double distance, double close, boolean carcass) {}

    /** A row of tribespeople: role, climate, the skins to show. */
    private record Row(String name, Supplier<? extends EntityType<?>> type, int role, int climate, int[] skins) {}

    private static final String[] VIEWS = {"front", "side", "q", "carcass"};
    private static final String[] ROW_VIEWS = {"front", "back", "close"};

    private static final List<Subject> SUBJECTS = List.of(
            new Subject("wolverine", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.WOLVERINE, null, false, 3.0, 1.9, true),
            new Subject("lynx", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.LYNX, null, false, 3.2, 2.0, true),
            new Subject("lynx_kitten", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.LYNX, null, true, 2.0, 1.2, false),
            new Subject("ermine", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.ERMINE, null, false, 1.5, 0.9, true),
            new Subject("ermine_winter", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.ERMINE, flag("white"), false, 1.5, 0.9, false),
            new Subject("pine_marten", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.PINE_MARTEN, null, false, 1.9, 1.1, true),
            new Subject("lemming", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.LEMMING, null, false, 1.2, 0.7, true),
            new Subject("mushroom_man", com.mechanical.wildlife.night.NightFauna.MUSHROOM_MAN, null, false, 3.4, 2.0, false),
            new Subject("vittra", com.mechanical.wildlife.underground.UndergroundFauna.VITTRA, null, false, 3.4, 1.8, true),
            new Subject("troll", com.mechanical.wildlife.underground.UndergroundFauna.TROLL, null, false, 6.0, 3.2, true),
            new Subject("giant_centipede", com.mechanical.wildlife.underground.UndergroundFauna.GIANT_CENTIPEDE, null, false, 2.6, 1.5, true),
            new Subject("lindworm", com.mechanical.wildlife.underground.UndergroundFauna.LINDWORM, null, false, 6.0, 3.5, true));

    private static final List<Row> ROWS = rows();
    private static final List<Subject> subjects = new ArrayList<>(SUBJECTS);
    private static final List<Row> rows = new ArrayList<>(ROWS);

    private static List<Row> rows() {
        List<Row> out = new ArrayList<>();
        String[] climates = {"cold", "temperate", "warm", "jungle"};
        String[] roles = {"hunter", "gatherer", "fisher", "woodcutter", "elder"};
        int[] all = {0, 1, 2, 3};
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < roles.length; r++)
                out.add(new Row(climates[c] + "_" + roles[r], com.mechanical.people.People.TRIBESPERSON, r, c, all));
        out.add(new Row("trader", com.mechanical.people.People.TRIBESPERSON, 5, 1, all));
        out.add(new Row("guard", com.mechanical.people.People.TRIBESPERSON, 6, 1, all));
        out.add(new Row("miner", com.mechanical.people.People.MINER, 7, 1, new int[] {0, 2}));
        out.add(new Row("oak", com.mechanical.people.People.TRIBESPERSON, -1, 1, new int[] {8, 9, 10, 11}));
        return out;
    }

    private static CompoundTag flag(String key) {
        CompoundTag t = new CompoundTag();
        t.putBoolean(key, true);
        return t;
    }

    private static int stage = -1, ticks, index, view;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static Entity current;
    private static final List<Entity> crowd = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "huntshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                // an optional second line: only the subjects and rows whose names start with one of these (comma separated)
                if (lines.size() > 1 && !lines.get(1).isBlank()) {
                    List<String> only = List.of(lines.get(1).trim().split(","));
                    subjects.removeIf(s -> only.stream().noneMatch(s.name()::startsWith));
                    rows.removeIf(r -> only.stream().noneMatch(("npc_" + r.name())::startsWith));
                }
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("HuntShots", () -> {});
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
                case 2 -> { // the next creature, or the rows of people once they are done
                    if (ticks < 30) return;
                    if (index >= subjects.size()) {
                        index = 0;
                        stage = 7;
                        ticks = 0;
                        return;
                    }
                    Subject s = subjects.get(index);
                    server.execute(() -> place(s));
                    stage = 3;
                    ticks = 0;
                }
                case 3 -> {
                    if (ticks < 100) return;
                    Subject s = subjects.get(index);
                    shot(mc, s.name());
                    result.add(s.name() + ": " + current);
                    view = 0;
                    server.execute(() -> closeUp(s, VIEWS[0]));
                    stage = 6;
                    ticks = 0;
                }
                case 6 -> {
                    if (ticks < 20) return;
                    Subject s = subjects.get(index);
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
                case 7 -> { // a row of people
                    if (ticks < 30) return;
                    if (index >= rows.size()) {
                        stage = 4;
                        ticks = 0;
                        return;
                    }
                    Row r = rows.get(index);
                    view = 0;
                    server.execute(() -> placeRow(r, ROW_VIEWS[0]));
                    stage = 8;
                    ticks = 0;
                }
                case 8 -> {
                    if (ticks < (view == 0 ? 80 : 20)) return;
                    Row r = rows.get(index);
                    shot(mc, "npc_" + r.name() + (view == 0 ? "" : "_" + ROW_VIEWS[view]));
                    ticks = 0;
                    if (++view < ROW_VIEWS.length) {
                        String v = ROW_VIEWS[view];
                        server.execute(() -> rowCamera(r, v));
                        return;
                    }
                    result.add("npc " + r.name() + ": " + crowd.size());
                    index++;
                    stage = 7;
                }
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "hunt_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "hunt_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
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
        cmd("gamerule doMobSpawning false");
        cmd("time set 6000");
        cmd("weather clear");
        cmd("difficulty easy"); // peaceful would remove the Mushroom Man
        cmd("fill " + (x - 14) + " 200 " + (z - 14) + " " + (x + 14) + " 200 " + (z + 14) + " grass_block");
        cmd("fill " + (x - 14) + " 201 " + (z - 14) + " " + (x + 14) + " 215 " + (z + 14) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static void clear() {
        // a troll caught by the sun leaves two blocks of Troll Stone; sweep the stage clean
        cmd("fill " + (int) Math.floor(base.x - 3) + " 201 " + (int) Math.floor(base.z - 3) + " " + (int) Math.floor(base.x + 3) + " 207 " + (int) Math.floor(base.z + 3) + " air");
        if (current != null) current.discard();
        current = null;
        crowd.forEach(Entity::discard);
        crowd.clear();
    }

    private static Entity spawn(Supplier<? extends EntityType<?>> type, CompoundTag nbt, Vec3 at, float yaw) {
        ServerLevel level = player().level();
        Entity e = type.get().create(level, EntitySpawnReason.COMMAND);
        if (e == null) return null;
        if (nbt != null) e.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), nbt));
        e.snapTo(at.x, at.y, at.z, yaw, 0);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setPersistenceRequired();
        }
        level.addFreshEntity(e);
        return e;
    }

    private static void place(Subject s) {
        clear();
        float yaw = 225;
        // the troll turns to stone under open sky by day: one block high over its head keeps the sun off (light 14, not 15)
        if (s.name().equals("troll")) cmd("setblock " + (int) Math.floor(base.x) + " 206 " + (int) Math.floor(base.z) + " stone");
        Entity e = spawn(s.type(), s.nbt(), base, yaw);
        if (e == null) return;
        if (s.baby() && e instanceof AgeableMob a) a.setBaby(true);
        current = e;
        Vec3 mid = base.add(0, e.getBbHeight() * 0.5, 0);
        look(mid.add(new Vec3(-0.55, 0.35, -0.75).normalize().scale(s.distance())), mid);
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
        }
        double yaw = Math.toRadians(225);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), left = new Vec3(fwd.z, 0, -fwd.x);
        Vec3 dir = switch (v) {
            case "front" -> fwd;
            case "side" -> left;
            default -> fwd.add(left).normalize();
        };
        double eyeY = base.y + Math.max(current.getEyeHeight(), 0.15);
        Vec3 at = new Vec3(base.x, eyeY - current.getBbHeight() * 0.15, base.z);
        look(new Vec3(base.x, eyeY, base.z).add(dir.scale(s.close())), at);
    }

    /** The row stands along x facing north (towards -z); the camera in front of it. */
    private static void placeRow(Row r, String v) {
        clear();
        ServerLevel level = player().level();
        int n = r.skins().length;
        for (int i = 0; i < n; i++) {
            CompoundTag t = new CompoundTag();
            int skin = r.skins()[i];
            int role = r.role() >= 0 ? r.role() : skin; // the Oak Folk: one of each of the four Oak roles, skin 0..3 in turn
            t.putInt("role", role);
            t.putInt("skin", r.role() >= 0 ? skin : i);
            t.putInt("climate", r.climate());
            t.putIntArray("group", UUIDUtil.uuidToIntArray(new UUID(0x5eedL, 7L + r.climate())));
            Vec3 at = base.add((i - (n - 1) / 2.0) * 1.1, 0, 0);
            Entity e = spawn(r.type(), t, at, 180);
            if (e != null) crowd.add(e);
        }
        result.add("row " + r.name() + " in " + level.dimension());
        rowCamera(r, v);
    }

    private static void rowCamera(Row r, String v) {
        double eye = 1.45;
        int n = r.skins().length;
        Vec3 mid = base.add(0, eye - 0.25, 0);
        switch (v) {
            case "back" -> look(base.add(0, eye, 4.3), mid);
            case "close" -> {
                Vec3 m2 = base.add((-(n - 1) / 2.0 + 0.55) * 1.1, 1.25, 0);
                look(m2.add(0, 0.2, -2.0), m2);
            }
            default -> look(base.add(0, eye, -4.3), mid);
        }
    }

    private static void look(Vec3 eye, Vec3 at) {
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
                image.writeToFile(new File(out, "hunt_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private HuntShots() {}
}
