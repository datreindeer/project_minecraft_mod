package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.temperate.SoundersAndColonies;
import com.mechanical.wildlife.temperate.TemperateFauna;

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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like {@link LogShots}): with a file {@code temperateshots.flag} in the game directory (its first line: an
 * output folder; an optional second line: a comma list of subject names to shoot, else all), the client opens the world
 * "TemperateShots", builds a grass platform at y 200 and photographs each temperate animal (wildlife.temperate)
 * standing still, the camera at its eye height: three-quarter (temperate_&lt;name&gt;.png), side (_side) and front
 * (_front), then writes temperate_result.txt and quits. The eel is shot in a pool and on land. Does nothing without the
 * flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TemperateShots {
    /** name, type, baby, camera distance, extra setup on the spawned entity (may be null), in water. */
    private record Subject(String name, Supplier<? extends EntityType<?>> type, boolean baby, double distance, Consumer<Entity> setup, boolean water) {}

    private static final List<Subject> SUBJECTS = List.of(
            new Subject("wisent", TemperateFauna.WISENT, false, 4.2, e -> bull(e), false),
            new Subject("wisent_cow", TemperateFauna.WISENT, false, 4.0, null, false),
            new Subject("wisent_calf", TemperateFauna.WISENT, true, 2.6, null, false),
            new Subject("pond_turtle", TemperateFauna.POND_TURTLE, false, 1.1, null, false),
            new Subject("water_vole", TemperateFauna.WATER_VOLE, false, 1.0, null, false),
            new Subject("stag_beetle", TemperateFauna.STAG_BEETLE, false, 1.0, null, false),
            new Subject("woodpecker", TemperateFauna.WOODPECKER, false, 1.0, null, false),
            new Subject("tawny_owl", TemperateFauna.TAWNY_OWL, false, 1.5, null, false),
            new Subject("skylark", TemperateFauna.SKYLARK, false, 0.9, null, false),
            new Subject("lapwing", TemperateFauna.LAPWING, false, 1.0, null, false),
            new Subject("bittern", TemperateFauna.BITTERN, false, 2.0, null, false),
            new Subject("sea_eagle", TemperateFauna.SEA_EAGLE, false, 2.4, null, false),
            new Subject("eel", TemperateFauna.EEL, false, 1.5, null, true),
            new Subject("eel_land", TemperateFauna.EEL, false, 1.4, null, false),
            new Subject("red_fox_with_egg", () -> EntityTypes.FOX, false, 2.2,
                    e -> ((Mob) e).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.EGG)), false),
            new Subject("stoat", com.mechanical.wildlife.coldhunters.ColdhuntersFauna.ERMINE, false, 1.5, null, false),
            new Subject("boar_tusker", Wildlife.WILD_BOAR, false, 3.5, e -> SoundersAndColonies.makeTusker((com.mechanical.wildlife.WildBoar) e), false));

    private static int stage = -1, ticks, index, view;
    /** Camera angles from the animal's heading (degrees) and their file suffixes: three-quarter, side, front. */
    private static final int[] VIEW_ANGLE = {50, 90, 0};
    private static final String[] VIEW_NAME = {"", "_side", "_front"};
    private static java.util.Set<String> only;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;
    private static Entity current;

    private static void bull(Entity e) {
        // the bull flag is private: set it through the save data
        net.minecraft.world.level.storage.TagValueOutput o = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(net.minecraft.util.ProblemReporter.DISCARDING);
        e.saveWithoutId(o);
        net.minecraft.nbt.CompoundTag tag = o.buildResult();
        tag.putBoolean("bull", true);
        e.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, e.registryAccess(), tag));
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "temperateshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                if (lines.size() > 1 && !lines.get(1).isBlank()) only = java.util.Set.of(lines.get(1).trim().split("\s*,\s*"));
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("TemperateShots", () -> {});
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
                        p.teleportTo(p.level(), p.getX() + 700, 205, p.getZ(), java.util.Set.of(), 0, 20, true); // load the chunks there first
                    });
                    stage = 5;
                    ticks = 0;
                }
                case 2 -> { // place the next subject
                    if (ticks < 40) return;
                    while (index < SUBJECTS.size() && only != null && !only.contains(SUBJECTS.get(index).name())) index++;
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
                case 3 -> { // let the chunk render, then shoot each view
                    if (ticks < (view == 0 ? 100 : 25)) return;
                    Subject s = SUBJECTS.get(index);
                    shot(mc, s.name() + VIEW_NAME[view]);
                    if (view == 0) result.add(s.name() + ": " + current);
                    ticks = 0;
                    if (++view < VIEW_ANGLE.length) {
                        int v = view;
                        server.execute(() -> aim(s, v));
                        return;
                    }
                    view = 0;
                    index++;
                    stage = 2;
                }
                case 5 -> { // build once the chunks over there are loaded
                    if (ticks < 80) return;
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.teleportTo(p.level(), p.getX() - 700, p.getY(), p.getZ(), java.util.Set.of(), 0, 20, true);
                        setup();
                    });
                    stage = 2;
                    ticks = 0;
                }
                case 4 -> {
                    if (ticks < 30) return;
                    Files.write(new File(out, "temperate_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "temperate_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    /** A 25x25 grass platform at y 200, 700 blocks east of the spawn, clear sky above, noon. */
    private static void setup() {
        ServerPlayer p = player();
        int x = (int) Math.floor(p.getX()) + 700, z = (int) Math.floor(p.getZ());
        base = new Vec3(x + 0.5, 201, z + 0.5);
        cmd("gamemode creative @a");
        p.getAbilities().flying = true; // hangs where it is put, as the camera
        p.onUpdateAbilities();
        cmd("gamerule doDaylightCycle false");
        cmd("time set noon");
        cmd("weather clear");
        cmd("difficulty peaceful");
        cmd("fill " + (x - 12) + " 199 " + (z - 12) + " " + (x + 12) + " 199 " + (z + 12) + " dirt");
        cmd("fill " + (x - 12) + " 200 " + (z - 12) + " " + (x + 12) + " 200 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 12) + " 201 " + (z - 12) + " " + (x + 12) + " 215 " + (z + 12) + " air");
        p.teleportTo(p.level(), base.x, base.y + 3, base.z - 6, java.util.Set.of(), 0, 20, true);
    }

    private static void place(Subject s) {
        ServerLevel level = player().level();
        if (current != null) current.discard();
        int bx = (int) Math.floor(base.x), bz = (int) Math.floor(base.z);
        // a pool for the eel, grass again for the rest
        cmd("fill " + (bx - 2) + " 200 " + (bz - 2) + " " + (bx + 2) + " 200 " + (bz + 2) + (s.water() ? " water" : " grass_block"));
        Entity e = s.type().get().create(level, EntitySpawnReason.COMMAND);
        if (e == null) return;
        float yaw = 225; // head to the north-east: from the camera (south-west of it) a three-quarter view of its left side
        e.snapTo(base.x, base.y - (s.water() ? 0.6 : 0), base.z, yaw, 0);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setPersistenceRequired();
        }
        if (s.baby() && e instanceof AgeableMob a) a.setBaby(true);
        if (!s.baby() && e instanceof AgeableMob a) a.setBaby(false);
        if (s.setup() != null) s.setup().accept(e);
        level.addFreshEntity(e);
        current = e;
        aim(s, 0);
    }

    /** The camera {@code distance} from the animal at its eye height, VIEW_ANGLE[view] degrees off its heading. */
    private static void aim(Subject s, int view) {
        ServerLevel level = player().level();
        Entity e = current;
        Vec3 mid = e.position().add(0, Math.max(e.getEyeHeight() * 0.9, e.getBbHeight() * 0.45), 0);
        double a = Math.toRadians(e.getYRot() + VIEW_ANGLE[view]);
        Vec3 eye = mid.add(new Vec3(-Math.sin(a), 0.12, Math.cos(a)).normalize().scale(s.distance()));
        Vec3 d = mid.subtract(eye);
        float camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float camPitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        ServerPlayer p = player();
        p.teleportTo(level, eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), camYaw, camPitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "temperate_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private TemperateShots() {}
}
