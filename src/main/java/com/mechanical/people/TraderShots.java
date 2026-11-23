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
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
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
                plan();
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
        for (String kind : new String[] {"woodsman", "mining", "jewelry", "hunting", "herbs", "foraging"})
            caravan(kind, false, kind.equals("jewelry") ? "sand" : "grass_block");
        caravan("mining", true, "grass_block");
        caravan("hunting", true, "grass_block");
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
        add(1, () -> lookAtCaravan(6, 1.8), null);
        add(30, null, name + "_near");
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
