package com.mechanical.people;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mechanical.Coins;
import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like TribeAiShots): with a file {@code tradershots.flag} in the game directory (its first line: an output folder),
 * the client opens the world "TraderShots", builds a platform at y 200 far from everything and takes screenshots
 * (trader_*.png): every caravan kind's group with its cart (camels on sand for the jewelry caravan), a big wagon caravan,
 * a line-up of each kind's leader, handler and guard, close-ups, and the trade screen; then it quits. Nothing without the flag.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TraderShots {
    private record Step(int delay, Runnable server, String shot) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static final List<String> result = new ArrayList<>();
    private static int stage = -1, ticks, step;
    private static File out;
    private static Vec3 base;
    private static Tribesperson leader;
    private static boolean polish;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "tradershots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                polish = lines.size() > 1 && lines.get(1).trim().equals("polish");
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("TraderShots", () -> {});
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
                if (ticks == 1) mc.getWindow().setWindowed(1600, 900);
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                if (mc.player.isDeadOrDying()) mc.player.respawn();
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks < 100) return;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    cmd("gamemode creative @a");
                    cmd("time set 4000");
                    cmd("weather clear");
                    cmd("gamerule advance_time false");
                    cmd("gamerule advance_weather false");
                    look(player().position().add(4000, 0, 0).with(net.minecraft.core.Direction.Axis.Y, 206), 0, 30);
                });
                if (polish) planPolish();
                else plan();
                stage = 2;
                ticks = 0;
                return;
            }
            if (step >= STEPS.size()) {
                Files.write(new File(out, "trader_result.txt").toPath(), result);
                stage = -2;
                mc.stop();
                return;
            }
            Step s = STEPS.get(step);
            if (ticks < s.delay) return;
            if (s.shot != null) shot(mc, s.shot);
            if (s.server != null) server.execute(() -> {
                try {
                    s.server.run();
                } catch (Exception e) {
                    result.add("step " + step + " error: " + e);
                }
            });
            step++;
            ticks = 0;
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "trader_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    /** The shoot: each step waits, then shoots what is on screen, then runs its server part (setting up the next shot). */
    private static void plan() {
        add(160, () -> {
            base = new Vec3(Math.floor(player().getX()) + 0.5, 200, Math.floor(player().getZ()) + 0.5);
            lineUp();
        }, null);
        add(60, () -> look(base.add(8.6, 2.2, -9.5), 0, 8), null);
        add(30, null, "lineup");
        add(1, () -> look(base.add(3.1, 1.7, -4.6), 0, 6), null);
        add(30, null, "lineup_close_a");
        add(1, () -> look(base.add(14.1, 1.7, -4.6), 0, 6), null);
        add(30, null, "lineup_close_b");
        add(1, () -> look(base.add(8.6, 2.6, 6.5), 180, 14), null);
        add(30, null, "lineup_back");
        for (String kind : new String[] {"woodsman", "mining", "jewelry", "hunting", "herbs", "foraging"})
            caravan(kind, false, kind.equals("jewelry") ? "sand" : "grass_block");
        caravan("mining", true, "grass_block");
        caravan("hunting", true, "grass_block");
        column("mining", true);
        column("herbs", false);
        // the trade screen
        add(1, () -> {
            clear("grass_block");
            leader = Caravans.spawn(player().level(), net.minecraft.core.BlockPos.containing(base), Identifier.fromNamespaceAndPath(Mechanical.MODID, "jewelry"), true);
            ServerPlayer p = player();
            cmd("gamemode survival @a");
            p.getInventory().clearContent();
            p.getInventory().add(new ItemStack(Coins.GOLD_COIN.get(), 2));
            p.getInventory().add(new ItemStack(Coins.SILVER_COIN.get(), 7));
            p.getInventory().add(new ItemStack(Coins.COPPER_COIN.get(), 14));
            p.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, 5));
            p.getInventory().add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, "ruby")), 2));
            p.getInventory().add(new ItemStack(Items.DIAMOND, 1));
        }, null);
        add(40, () -> {
            leader.setNoAi(true);
            look(leader.position().add(0, 0, -2.5), 0, 10);
        }, null);
        add(160, () -> CaravanTrade.open(player(), leader), null); // the advancement toasts fade first
        add(30, () -> {
            Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().gui.hud.isHidden()) Minecraft.getInstance().gui.hud.toggle(); });
        }, null);
        add(20, null, "trade_screen");
        add(1, () -> {
            CaravanTrade.trade(player(), leader, firstSale());
            CaravanTrade.open(player(), leader);
        }, null);
        add(20, null, "trade_screen_after");
        add(1, () -> {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(null));
            Minecraft.getInstance().execute(() -> { if (!Minecraft.getInstance().gui.hud.isHidden()) Minecraft.getInstance().gui.hud.toggle(); });
            cmd("gamemode creative @a");
        }, null);
    }

    /**
     * "polish" on the flag's second line: close-ups of the hunting, woodsman and foraging kinds' folk from the front, three
     * quarters and behind (hoods, back gear), then a caravan arriving along a bending trail, seen from above as it walks.
     */
    private static void planPolish() {
        String[] kinds = {"hunting", "woodsman", "foraging"};
        add(160, () -> base = new Vec3(Math.floor(player().getX()) + 0.5, 200, Math.floor(player().getZ()) + 0.5), null);
        for (String kind : kinds) {
            add(1, () -> lineUpKind(kind), null);
            double[][] views = {{0, 3.0}, {35, 2.4}, {180, 3.0}, {145, 2.2}};
            for (int v = 0; v < views.length; v++) {
                double[] view = views[v];
                add(v == 0 ? 40 : 1, () -> {
                    double a = Math.toRadians(view[0]);
                    Vec3 mid = base.add(0.95, 1.25, 0);
                    // the folk face north (-z): the camera round them from there, at head height (look() takes the feet)
                    Vec3 eye = mid.add(Math.sin(a) * view[1], 0.45, -Math.cos(a) * view[1]);
                    Vec3 d = mid.subtract(eye);
                    look(eye.subtract(0, 1.62, 0), (float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
                }, null);
                add(30, null, "polish_" + kind + "_" + (int) view[0]);
            }
        }
        // a trail bending out north of the straight line; the caravan sets out where it starts
        add(1, () -> {
            clear("grass_block");
            int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
            for (int i = 0; i <= 60; i++) {
                double t = i / 60.0;
                int px = x + (int) Math.round(18 * (1 - t)), pz = z - (int) Math.round(8 * Math.sin(Math.PI * t));
                cmd("fill " + px + " 199 " + pz + " " + (px + 1) + " 199 " + (pz + 1) + " mechanical:trodden_path");
            }
            var stop = net.minecraft.core.BlockPos.containing(base);
            leader = Caravans.arrive(player().level(), stop, stop.offset(18, 0, 0), Identifier.fromNamespaceAndPath(Mechanical.MODID, "hunting"), false, 18); // the cart behind stays on the platform
            look(base.add(11, 30, -4.5), 0, 89);
        }, null);
        for (int k = 0; k < 4; k++) {
            int n = k;
            add(n == 0 ? 60 : 160, () -> {
                Tribesperson handler = null;
                for (Tribesperson t : leader.groupNearby(48)) if (t.role() == TribeRole.HANDLER) handler = t;
                result.add("trail " + n + ": handler at " + (handler == null ? "none" : handler.blockPosition() + " phase " + handler.caravanPhase));
            }, "polish_trail_" + n);
        }
    }

    /** The kind's leader, handler and guard side by side at the platform's centre, facing north. */
    private static void lineUpKind(String kind) {
        clear("grass_block");
        ServerLevel level = player().level();
        TribeRole[] roles = {TribeRole.TRADER, TribeRole.HANDLER, TribeRole.GUARD};
        UUID tribe;
        var r = level.getRandom();
        do tribe = new UUID(r.nextLong(), r.nextLong()); while (Tribe.of(tribe).temperament != Tribe.Temperament.TRADERS);
        for (int j = 0; j < roles.length; j++) {
            Tribesperson t = People.TRIBESPERSON.get().create(level, EntitySpawnReason.EVENT);
            t.setPos(base.add(j * 0.95, 0, 0));
            level.addFreshEntity(t);
            t.setup(roles[j], null, tribe, j == 0);
            t.setCaravanKind(Identifier.fromNamespaceAndPath(Mechanical.MODID, kind));
            if (roles[j] == TribeRole.GUARD) Caravans.arm(t, 1, r);
            t.setNoAi(true);
            t.setYRot(180);
            t.yBodyRot = t.yHeadRot = 180;
        }
    }

    private static int firstSale() {
        for (int i = 0; i < leader.stock.deals.size(); i++) if (leader.stock.deals.get(i).sale()) return i;
        return 0;
    }

    /** A caravan of {@code kind} on a platform of {@code ground}: from the side, wide, and the leader up close. */
    private static void caravan(String kind, boolean big, String ground) {
        String name = kind + (big ? "_big" : "");
        add(1, () -> {
            clear(ground);
            leader = Caravans.spawn(player().level(), net.minecraft.core.BlockPos.containing(base), Identifier.fromNamespaceAndPath(Mechanical.MODID, kind), big);
            result.add(name + ": " + (leader == null ? "none" : leader.stock.deals.size() + " deals, purse " + leader.stock.purse));
        }, null);
        add(50, () -> lookAtCaravan(10, 4), null);
        add(30, null, name);
        add(1, () -> lookAtCaravan(5, 1.6), null);
        add(30, null, name + "_near");
        add(1, () -> lookAtBack(4.5), null);
        add(30, null, name + "_back");
        add(1, () -> {
            leader.setNoAi(true);
            Vec3 face = Vec3.directionFromRotation(0, leader.getYRot());
            look(leader.position().add(face.scale(2.4)).add(0, 1.3, 0), leader.getYRot() + 180, 8);
        }, null);
        add(30, null, name + "_leader");
        add(1, () -> { // from the side: the old man's stoop
            Vec3 side = Vec3.directionFromRotation(0, leader.yBodyRot + 90);
            look(leader.position().add(side.scale(2.6)).add(0, 1.0, 0), leader.yBodyRot - 90, 4);
        }, null);
        add(30, null, name + "_leader_side");
    }

    /** On the road: a caravan set out 20 blocks off walks in; shot from beside the column twice on the way. */
    private static void column(String kind, boolean big) {
        String name = "column_" + kind + (big ? "_big" : "");
        add(1, () -> {
            clear("grass_block");
            var stop = net.minecraft.core.BlockPos.containing(base);
            leader = Caravans.arrive(player().level(), stop, stop.offset(20, 0, 4), Identifier.fromNamespaceAndPath(Mechanical.MODID, kind), big, 20);
            look(base.add(14, 6, -14), 45, 20);
        }, null);
        for (int k = 0; k < 2; k++) {
            int n = k;
            add(n == 0 ? 120 : 140, () -> {
                Tribesperson handler = null;
                for (Tribesperson t : leader.groupNearby(48)) if (t.role() == TribeRole.HANDLER) handler = t;
                if (handler == null) return;
                Vec3 mid = handler.position().add(leader.position()).scale(0.5);
                Vec3 heading = handler.position().subtract(leader.position()).multiply(1, 0, 1).normalize();
                Vec3 eye = mid.add(new Vec3(-heading.z, 0, heading.x).scale(8)).add(heading.scale(3)).add(0, 2.5, 0);
                Vec3 d = mid.add(0, 0.8, 0).subtract(eye);
                look(eye, (float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
                result.add(name + " " + n + ": phase " + handler.caravanPhase + " handler at " + handler.blockPosition() + " leader at " + leader.blockPosition());
            }, null);
            add(20, null, name + "_" + n);
        }
    }

    /** Behind the cart or wagon, {@code dist} back, looking in at the open end of its tilt. */
    private static void lookAtBack(double dist) {
        List<TravoisEntity> beds = player().level().getEntitiesOfClass(TravoisEntity.class, new AABB(BlockPosOf(base)).inflate(16));
        if (beds.isEmpty()) return;
        TravoisEntity bed = beds.get(0);
        Vec3 back = Vec3.directionFromRotation(0, bed.getYRot()).scale(-1);
        if (bed instanceof com.mechanical.WagonEntity) dist += 1.5;
        Vec3 eye = bed.position().add(back.scale(dist)).add(0, 1.9, 0);
        look(eye, bed.getYRot(), 22);
    }

    /** Beside the caravan (square to its cart or wagon), {@code dist} out and {@code up} high, looking at its middle. */
    private static void lookAtCaravan(double dist, double up) {
        ServerLevel level = player().level();
        List<TravoisEntity> beds = level.getEntitiesOfClass(TravoisEntity.class, new AABB(BlockPosOf(base)).inflate(16));
        Vec3 mid = leader.position();
        float yaw = 0;
        if (!beds.isEmpty()) {
            TravoisEntity bed = beds.get(0);
            yaw = bed.getYRot();
            mid = bed.position().add(leader.position()).scale(0.5);
        }
        Vec3 side = Vec3.directionFromRotation(0, yaw + 90);
        if (!beds.isEmpty() && beds.get(0) instanceof com.mechanical.WagonEntity) dist *= 1.5; // a wagon is long
        Vec3 eye = mid.add(side.scale(dist)).add(0, up, 0);
        Vec3 d = mid.add(0, 0.8, 0).subtract(eye);
        float lookYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        look(eye, lookYaw, pitch);
    }

    private static net.minecraft.core.BlockPos BlockPosOf(Vec3 v) {
        return net.minecraft.core.BlockPos.containing(v);
    }

    /** Each kind's leader, handler and guard in a row facing north (towards the camera). */
    private static void lineUp() {
        clear("grass_block");
        ServerLevel level = player().level();
        String[] kinds = {"woodsman", "mining", "jewelry", "hunting", "herbs", "foraging"};
        TribeRole[] roles = {TribeRole.TRADER, TribeRole.HANDLER, TribeRole.GUARD};
        UUID tribe;
        var r = level.getRandom();
        do tribe = new UUID(r.nextLong(), r.nextLong()); while (Tribe.of(tribe).temperament != Tribe.Temperament.TRADERS);
        for (int k = 0; k < kinds.length; k++) {
            for (int j = 0; j < roles.length; j++) {
                Tribesperson t = People.TRIBESPERSON.get().create(level, EntitySpawnReason.EVENT);
                t.setPos(base.add(k * 3.4 + j * 0.95, 0, 0));
                level.addFreshEntity(t);
                t.setup(roles[j], null, tribe, j == 0);
                t.setCaravanKind(Identifier.fromNamespaceAndPath(Mechanical.MODID, kinds[k]));
                if (roles[j] == TribeRole.TRADER) t.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Coins.COIN_PURSE.get()));
                if (roles[j] == TribeRole.GUARD) Caravans.arm(t, 1, r);
                t.setNoAi(true);
                t.setYRot(180);
                t.yBodyRot = t.yHeadRot = 180;
            }
        }
    }

    private static void clear(String ground) {
        int x = (int) Math.floor(base.x), z = (int) Math.floor(base.z);
        cmd("execute at @p run kill @e[type=!player,distance=..80]");
        cmd("fill " + (x - 24) + " 199 " + (z - 24) + " " + (x + 24) + " 199 " + (z + 24) + " " + ground);
        cmd("fill " + (x - 24) + " 198 " + (z - 24) + " " + (x + 24) + " 198 " + (z + 24) + " stone");
        cmd("fill " + (x - 24) + " 200 " + (z - 24) + " " + (x + 24) + " 212 " + (z + 24) + " air");
    }

    private static void add(int wait, Runnable server, String shot) {
        STEPS.add(new Step(wait, server, shot));
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void look(Vec3 at, float yaw, float pitch) {
        ServerPlayer p = player();
        if (p.isCreative()) {
            p.getAbilities().flying = true; // hangs where it is put
            p.onUpdateAbilities();
        }
        p.teleportTo(p.level(), at.x, at.y, at.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "trader_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private TraderShots() {}
}
