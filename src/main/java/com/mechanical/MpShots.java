package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (multiplayer check, v0.195.0): with {@code mpshots.flag} in the game directory (line 1: output folder, line 2:
 * server address) the client joins a dedicated server started with {@code mptest.flag} (people.MpTest) and plays the recent
 * features through with real clicks and keys: hitching and galloping a wagon with two horses, dragging a felled log, a
 * water wheel's body and rendering, the caravan trade screen, the oak keeper's quest screen, cooking in a kettle and a clay
 * pot, and a kettle catching blood under a hanging rack. It writes what its own world shows next to the server's answers to
 * {@code /mptest where} (mp_result.txt), shoots mp_*.png and quits.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class MpShots {
    private static int stage = -1, ticks, step;
    private static File out;
    private static String address;
    /** Line 3 of the flag, if any: only these scenes (wagon,log,wheel,caravan,quest,kitchen). */
    private static java.util.Set<String> only = java.util.Set.of();
    private static final List<String> result = new ArrayList<>();
    private static final List<Object[]> STEPS = new ArrayList<>(); // {wait ticks, Runnable}
    private static Runnable monitor;
    private static ArmorStand camera;
    private static double ground;
    private static WagonEntity wagon;
    private static AbstractHorse lead, partner;
    private static FelledLogEntity log;
    private static double maxPair, maxWagon;
    private static int samples;

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent event) {
        if (stage < 0) return;
        String s = event.getMessage().getString();
        if (s.startsWith("[mp]") || s.contains("rror")) result.add("  server: " + s);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "mpshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                address = lines.get(1).trim();
                if (lines.size() > 2 && !lines.get(2).isBlank()) only = java.util.Set.of(lines.get(2).trim().split(","));
                script();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (mc.gui.screen() instanceof TitleScreen title) {
                    mc.options.renderDistance().set(8);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    mc.options.pauseOnLostFocus = false;
                    net.minecraft.client.gui.screens.ConnectScreen.startConnecting(title, mc, ServerAddress.parseString(address),
                            new ServerData("mptest", address, ServerData.Type.OTHER), false, null);
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getConnection() == null) {
                if (stage == 1 && ++ticks > 1200) {
                    result.add("never joined " + address);
                    finish(mc);
                }
                return;
            }
            ticks++;
            if (stage == 1) {
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks < 100) return;
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                ground = mc.player.getY();
                result.add("joined " + address + " as " + mc.player.getName().getString() + ", ground y " + f(ground));
                stage = 2;
                ticks = 0;
                step = 0;
                return;
            }
            if (monitor != null) monitor.run();
            if (camera != null) camera.setOldPosAndRot();
            if (step >= STEPS.size()) {
                if (ticks < 20) return;
                finish(mc);
                return;
            }
            Object[] s = STEPS.get(step);
            if (ticks < (int) s[0]) return;
            ((Runnable) s[1]).run();
            step++;
            ticks = 0;
        } catch (Throwable e) {
            result.add("error: " + e);
            for (StackTraceElement el : e.getStackTrace()) if (el.getClassName().startsWith("com.mechanical")) result.add("  at " + el);
            finish(mc);
        }
    }

    private static void finish(Minecraft mc) {
        try {
            Files.write(new File(out, "mp_result.txt").toPath(), result);
        } catch (Exception ignored) {}
        stage = -2;
        mc.stop();
    }

    private static String scene = "";

    private static void then(int wait, Runnable r) {
        if (!only.isEmpty() && !scene.isEmpty() && !only.contains(scene)) return;
        STEPS.add(new Object[] {wait, r});
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static void cmd(String c) {
        mc().player.connection.sendCommand(c);
    }

    private static void hold(String item) {
        cmd("item replace entity @s weapon.mainhand with " + item);
    }

    private static void tp(double x, double z, float yaw) {
        cmd(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f %.1f 15", x, ground, z, yaw));
    }

    private static String f(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }

    private static String fmt(Vec3 v) {
        return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", v.x, v.y, v.z);
    }

    private static void note(String s) {
        result.add(s);
    }

    private static void view(Vec3 at, Vec3 target) {
        Minecraft mc = mc();
        if (camera == null) {
            camera = new ArmorStand(EntityTypes.ARMOR_STAND, mc.level);
            camera.setInvisible(true);
        }
        Vec3 d = target.subtract(at);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()));
        camera.setPos(at.x, at.y - camera.getEyeHeight(), at.z);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.setYHeadRot(yaw);
        camera.setOldPosAndRot();
        mc.setCameraEntity(camera);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static void own(CameraType type) {
        mc().setCameraEntity(mc().player);
        mc().options.setCameraType(type);
    }

    private static void shot(String name) {
        Screenshot.takeScreenshot(mc().gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "mp_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
        result.add("shot " + name);
    }

    private static <T extends Entity> T nearest(Class<T> type, double range, java.util.function.Predicate<T> filter) {
        var p = mc().player;
        return mc().level.getEntitiesOfClass(type, p.getBoundingBox().inflate(range), filter::test).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    private static void interact(Entity e) {
        if (e == null) {
            note("  (nothing to click)");
            return;
        }
        var r = mc().gameMode.interact(mc().player, e, new EntityHitResult(e), InteractionHand.MAIN_HAND);
        note("  clicked " + e.getType().toShortString() + " " + e.getId() + ": " + r);
    }

    private static void useOn(BlockPos pos, Direction face) {
        var r = mc().gameMode.useItemOn(mc().player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5)), face, pos, false));
        note("  used " + mc().player.getMainHandItem().getItem() + " on " + mc().level.getBlockState(pos).getBlock() + " " + pos.toShortString() + ": " + r);
    }

    private static void clientEntities() {
        var p = mc().player;
        for (Entity e : mc().level.getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(24),
                e -> e instanceof TravoisEntity || e instanceof FelledLogEntity || e instanceof AbstractHorse || e instanceof WaterWheelHitbox))
            note(String.format(Locale.ROOT, "  client: %s %d %.3f %.3f %.3f %.1f%s", e.getType().toShortString(), e.getId(), e.getX(), e.getY(), e.getZ(), e.getYRot(),
                    e.getVehicle() != null ? " on " + e.getVehicle().getId() : ""));
    }

    /** Both sides' view of the vehicles and logs near the player. */
    private static void compare(String when) {
        note("compare (" + when + "):");
        clientEntities();
        cmd("mptest where");
    }

    private static void script() {
        then(1, () -> {
            for (String c : List.of("gamemode survival", "time set noon", "gamerule advance_time false", "gamerule advance_weather false", "weather clear",
                    "gamerule spawn_mobs false", "difficulty peaceful", "clear @s"))
                cmd(c);
        });

        scene = "wagon";
        // ---- A: a wagon and two horses, hitched by hand (lead the second, ride the first, click the wagon), galloped and turned
        then(10, () -> tp(0.5, 0.5, -90));
        then(10, () -> cmd("mptest wagon"));
        then(30, () -> {
            wagon = nearest(WagonEntity.class, 12, w -> true);
            lead = nearest(AbstractHorse.class, 12, h -> !h.getItemBySlot(EquipmentSlot.SADDLE).isEmpty());
            partner = nearest(AbstractHorse.class, 12, h -> h != lead);
            note("wagon scene: wagon " + (wagon != null) + ", saddled horse " + (lead != null) + ", second horse " + (partner != null));
            hold("minecraft:lead");
            tp(lead.getX(), lead.getZ() - 1.8, 0); // within reach of both horses and, once up, the wagon
        });
        then(10, () -> interact(partner));
        then(10, () -> {
            note("  second horse led by me (client): " + (partner.getLeashHolder() == mc().player));
            hold("air");
        });
        then(5, () -> interact(lead));
        then(10, () -> {
            note("  riding (client): " + (mc().player.getVehicle() == lead));
            interact(wagon);
        });
        then(10, () -> {
            note("  team (client): lead " + (wagon.harness() == lead) + ", second " + (wagon.partner() == partner));
            own(CameraType.THIRD_PERSON_FRONT);
            mc().player.setYRot(-90);
            mc().player.setXRot(28);
            mc().options.keyUp.setDown(true);
            mc().options.keySprint.setDown(true);
            maxPair = maxWagon = 0;
            samples = 0;
            monitor = MpShots::measureTeam;
        });
        then(60, () -> {
            note("  gallop: speed " + f(lead.getDeltaMovement().horizontalDistance() * 20) + " blocks/s");
            shot("wagon_gallop");
        });
        for (int k = 1; k <= 40; k++) {
            int kk = k;
            then(1, () -> mc().player.setYRot(-90 - kk * 2.25F));
            if (k == 20) then(0, () -> shot("wagon_turn"));
        }
        then(20, () -> {
            mc().options.keyUp.setDown(false);
            mc().options.keySprint.setDown(false);
        });
        then(30, () -> {
            monitor = null;
            note("  team on my client over " + samples + " ticks: second horse off its place max " + f(maxPair) + ", wagon off the rigid pole's place max " + f(maxWagon));
            compare("wagon stopped, rider on");
            Vec3 c = wagon.position();
            view(c.add(5, 3.5, 6), c.add(1.5, 0.6, 0));
        });
        then(20, () -> shot("wagon_stopped"));
        then(5, () -> mc().options.keyShift.setDown(true));
        then(5, () -> mc().options.keyShift.setDown(false));
        then(100, () -> {
            note("  dismounted (client): " + (mc().player.getVehicle() == null));
            compare("5 s after getting off");
        });

        scene = "log";
        // ---- B: a felled log lifted and dragged
        then(20, () -> tp(0.5, 40.5, -90));
        then(10, () -> cmd("mptest log"));
        then(20, () -> {
            log = nearest(FelledLogEntity.class, 8, l -> !l.isPassenger());
            note("log scene: log " + (log != null) + (log != null ? " size " + log.size() + " at " + fmt(log.position()) : ""));
            hold("air");
        });
        then(5, () -> interact(log));
        then(10, () -> {
            note("  lifted (client): " + (FelledLogEntity.liftedBy(mc().player) != null));
            own(CameraType.THIRD_PERSON_BACK);
            mc().player.setYRot(90);
            mc().options.keyUp.setDown(true);
        });
        then(30, () -> shot("log_drag"));
        then(50, () -> mc().options.keyUp.setDown(false));
        then(20, () -> {
            note("  hands to the held end (client): " + f(mc().player.position().subtract(log.endPoint(true)).horizontalDistance()) + " / other end "
                    + f(mc().player.position().subtract(log.endPoint(false)).horizontalDistance()));
            compare("log held after dragging");
        });
        then(5, () -> mc().options.keyShift.setDown(true));
        then(8, () -> mc().options.keyShift.setDown(false));
        then(30, () -> {
            note("  put down (client): lifted " + (FelledLogEntity.liftedBy(mc().player) != null));
            compare("log put down");
        });

        scene = "wheel";
        // ---- C: a water wheel: its body entities, rendering, walking into it
        then(10, () -> tp(40.5, 0.5, 0));
        then(10, () -> cmd("mptest wheel"));
        then(80, () -> {
            var boxes = mc().level.getEntitiesOfClass(WaterWheelHitbox.class, mc().player.getBoundingBox().inflate(12));
            note("wheel scene: body entities on my client " + boxes.size());
            for (var b : boxes) note("  body bbox " + b.getBoundingBox());
            view(new Vec3(48, ground + 4, 3.5), new Vec3(40.5, ground + 3, 7));
        });
        then(10, () -> shot("wheel_a"));
        then(10, () -> shot("wheel_b"));
        then(5, () -> {
            own(CameraType.FIRST_PERSON);
            mc().player.setYRot(0);
            mc().player.setXRot(0);
            mc().options.keyUp.setDown(true);
        });
        then(40, () -> {
            mc().options.keyUp.setDown(false);
            note("  walked into the wheel: my z " + f(mc().player.getZ()) + " (the wide arm's edge at 4.0, so about 3.7)");
            cmd("data get entity @s Pos");
        });

        scene = "caravan";
        // ---- D: the caravan trade screen
        then(10, () -> tp(80.5, 0.5, 0));
        then(10, () -> cmd("mptest caravan"));
        then(60, () -> interact(nearest(com.mechanical.people.Tribesperson.class, 8, t -> t.getDisplayName().getString().contains("Leader"))));
        then(20, () -> {
            note("caravan: screen " + (mc().gui.screen() == null ? "none" : mc().gui.screen().getClass().getSimpleName()));
            if (mc().gui.hud.isHidden()) mc().gui.hud.toggle();
        });
        then(10, () -> shot("caravan_dialog"));
        then(2, () -> { // the dialog's Trade button, clicked where it is drawn
            for (var c : mc().gui.screen() == null ? List.<net.minecraft.client.gui.components.events.GuiEventListener>of() : mc().gui.screen().children())
                if (c instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().toLowerCase(Locale.ROOT).contains("trade")) {
                    var click = new net.minecraft.client.input.MouseButtonEvent(b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0,
                            new net.minecraft.client.input.MouseButtonInfo(0, 0));
                    Object before = mc().gui.screen();
                    b.onPress(click);
                    boolean took = before != mc().gui.screen();
                    note("  clicked " + b.getMessage().getString() + " (taken " + took + ", active " + b.isActive() + "); screen now "
                            + (mc().gui.screen() == null ? "none" : mc().gui.screen().getClass().getSimpleName()));
                    break;
                }
        });
        then(20, () -> note("  then screen " + (mc().gui.screen() == null ? "none" : mc().gui.screen().getClass().getSimpleName())));
        then(10, () -> shot("caravan_trade"));
        then(2, () -> {
            mc().gui.setScreen(null);
            if (!mc().gui.hud.isHidden()) mc().gui.hud.toggle();
        });

        scene = "quest";
        // ---- E: the oak keeper's quest screen
        then(10, () -> tp(120.5, 0.5, 0));
        then(10, () -> cmd("mptest quest"));
        then(40, () -> interact(nearest(com.mechanical.people.Tribesperson.class, 5, t -> true)));
        then(20, () -> {
            note("quest: screen " + (mc().gui.screen() == null ? "none" : mc().gui.screen().getClass().getSimpleName()));
            if (mc().gui.hud.isHidden()) mc().gui.hud.toggle();
        });
        then(10, () -> shot("quest"));
        then(2, () -> {
            mc().gui.setScreen(null);
            if (!mc().gui.hud.isHidden()) mc().gui.hud.toggle();
        });

        scene = "kitchen";
        // ---- F: kettle on a tripod, clay pot, blood kettle under a rack
        then(10, () -> tp(160.5, 40.5, 0));
        then(10, () -> cmd("mptest kitchen"));
        BlockPos base = BlockPos.containing(160.5, 0, 40.5);
        then(20, () -> hold("mechanical:tripod"));
        then(5, () -> useOn(at(base.offset(-1, 0, 3)), Direction.UP));
        then(5, () -> hold("mechanical:bronze_kettle"));
        then(5, () -> useOn(at(base.offset(-1, 1, 3)), Direction.NORTH));
        then(5, () -> hold("minecraft:water_bucket"));
        then(5, () -> useOn(at(base.offset(-1, 1, 3)), Direction.NORTH));
        for (String food : List.of("minecraft:beef", "minecraft:carrot", "minecraft:potato")) {
            then(5, () -> hold(food));
            then(5, () -> useOn(at(base.offset(-1, 1, 3)), Direction.NORTH));
        }
        then(5, () -> hold("mechanical:clay_cooking_pot"));
        then(5, () -> useOn(at(base.offset(1, 0, 3)), Direction.UP));
        then(5, () -> hold("minecraft:water_bucket"));
        then(5, () -> useOn(at(base.offset(1, 1, 3)), Direction.NORTH));
        for (String food : List.of("minecraft:carrot", "minecraft:potato")) {
            then(5, () -> hold(food));
            then(5, () -> useOn(at(base.offset(1, 1, 3)), Direction.NORTH));
        }
        then(5, () -> hold("mechanical:bronze_kettle"));
        then(5, () -> useOn(at(base.offset(4, 0, 3)), Direction.NORTH));
        then(5, () -> cmd("mptest bleed"));
        then(40, () -> {
            note("kitchen: tripod " + mc().level.getBlockState(at(base.offset(-1, 1, 3))) + ", pot " + mc().level.getBlockState(at(base.offset(1, 1, 3)))
                    + ", rack " + mc().level.getBlockState(at(base.offset(4, 0, 3))));
            if (mc().level.getBlockEntity(at(base.offset(-1, 1, 3))) instanceof TripodBlockEntity t)
                note("  tripod on my client: " + t.saveWithoutMetadata(mc().level.registryAccess()));
            if (mc().level.getBlockEntity(at(base.offset(1, 1, 3))) instanceof TripodBlockEntity t)
                note("  pot on my client: " + t.saveWithoutMetadata(mc().level.registryAccess()));
            cmd("mptest where");
            view(new Vec3(161.5, ground + 2.6, 41.2), new Vec3(161.5, ground + 0.8, 43.5));
        });
        then(20, () -> shot("kitchen_pots"));
        then(2, () -> view(new Vec3(162.5, ground + 2.2, 41.5), new Vec3(164.5, ground + 0.6, 43.5)));
        then(20, () -> shot("blood_kettle"));
        then(2, () -> hold("minecraft:bucket"));
        then(5, () -> useOn(at(base.offset(4, 0, 3)), Direction.NORTH));
        then(10, () -> note("  bucket on the blood kettle: in hand now " + mc().player.getMainHandItem().getItem() + ", rack " + mc().level.getBlockState(at(base.offset(4, 0, 3)))));
        then(5, () -> cmd("tick sprint 6000"));
        then(200, () -> {
            if (mc().level.getBlockEntity(at(base.offset(-1, 1, 3))) instanceof TripodBlockEntity t)
                note("  tripod after 6000 ticks, my client: " + t.saveWithoutMetadata(mc().level.registryAccess()));
            if (mc().level.getBlockEntity(at(base.offset(1, 1, 3))) instanceof TripodBlockEntity t)
                note("  pot after 6000 ticks, my client: " + t.saveWithoutMetadata(mc().level.registryAccess()));
            cmd("mptest where");
        });
        then(20, () -> shot("kitchen_done"));
        then(2, () -> hold("minecraft:bowl"));
        then(5, () -> useOn(at(base.offset(-1, 1, 3)), Direction.NORTH));
        then(10, () -> {
            note("  bowl in the kettle: in hand now " + mc().player.getMainHandItem().getItem());
            hold("minecraft:bowl");
        });
        then(5, () -> useOn(at(base.offset(1, 1, 3)), Direction.NORTH));
        then(10, () -> note("  bowl in the clay pot: in hand now " + mc().player.getMainHandItem().getItem()));
        then(20, () -> shot("kitchen_after_bowls"));
    }

    private static BlockPos at(BlockPos p) {
        return p.atY((int) Math.floor(ground) + p.getY());
    }

    /** On my client each tick: how far the second horse and the wagon are from where the rigid team puts them (as BugShots). */
    private static void measureTeam() {
        Vec3 side = partner.position().subtract(lead.position()).multiply(1, 0, 1);
        Vec3 want = Vec3.directionFromRotation(0, lead.getYRot() + (side.dot(Vec3.directionFromRotation(0, lead.getYRot() + 90)) > 0 ? 90 : -90)).scale(WagonEntity.PAIR);
        maxPair = Math.max(maxPair, side.subtract(want).horizontalDistance());
        Vec3 mid = lead.position().add(want.scale(0.5));
        Vec3 front = mid.subtract(Vec3.directionFromRotation(0, lead.getYRot()).scale(WagonEntity.POLE));
        Vec3 rigid = front.subtract(Vec3.directionFromRotation(0, wagon.getYRot()).scale(WagonEntity.FRONT_AXLE));
        maxWagon = Math.max(maxWagon, rigid.subtract(wagon.position()).horizontalDistance());
        samples++;
    }

    private MpShots() {}
}
