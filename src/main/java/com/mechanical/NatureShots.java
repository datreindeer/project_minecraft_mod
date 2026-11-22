package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with a file {@code natureshots.flag} in the game directory (its first line: an output folder), the client opens
 * the world "NatureShots", builds a scene at y 200 (a stream with a Beaver Dam and a beaver, a joined Feeding Trough with
 * animals eating and a tired cow, a churned pen floor) and takes screenshots nature_*.png, then quits. Does nothing without
 * the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class NatureShots {
    private static int stage = -1, ticks;
    private static File out;
    private static Vec3 base;
    private static final List<String> result = new ArrayList<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "natureshots.flag");
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
                    mc.createWorldOpenFlows().openWorld("NatureShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> {
                    mc.options.pauseOnLostFocus = false;
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    server.execute(NatureShots::setup);
                    mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); // F1
                    mc.gui.toastManager().clear();
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    next();
                }
                case 2 -> { // the beaver on the bank by its dam
                    if (ticks == 60) server.execute(() -> look(base.add(3.5, 0.2, 6.6), 20, 28));
                    if (ticks == 90) shot(mc, "beaver");
                    if (ticks < 95) return;
                    next();
                }
                case 3 -> { // the dam across the stream, water held back
                    if (ticks == 1) server.execute(() -> look(base.add(4.5, 1.4, 11.0), 90, 22)); // downstream, looking up at the dam and the pond behind
                    if (ticks == 30) shot(mc, "dam");
                    if (ticks < 35) return;
                    next();
                }
                case 4 -> { // the trough with animals at it
                    if (ticks == 1) server.execute(() -> look(base.add(0.6, 1.4, 2.5), 90, 28)); // from the east along the trough
                    if (ticks == 30) shot(mc, "trough");
                    if (ticks < 35) return;
                    next();
                }
                case 5 -> { // a tired cow, head hanging, on a churned pen floor
                    if (ticks == 1) server.execute(() -> look(base.add(-9.5, 0.4, -1.2), 0, 16)); // beside the tired cow
                    if (ticks == 30) shot(mc, "tired_pen");
                    if (ticks < 35) return;
                    next();
                }
                case 6 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "nature_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "nature_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void look(Vec3 at, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), at.x, at.y, at.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static BlockPos at(double dx, double dy, double dz) {
        return BlockPos.containing(base.add(dx, dy, dz));
    }

    private static void set(double dx, double dy, double dz, Block block) {
        player().level().setBlockAndUpdate(at(dx, dy, dz), block.defaultBlockState());
    }

    /** A still animal (no AI) at base + d facing {@code yaw}. */
    private static <T extends Mob> T spawn(EntityType<T> type, double dx, double dz, float yaw) {
        ServerLevel level = player().level();
        T mob = type.create(level, EntitySpawnReason.COMMAND);
        mob.snapTo(base.x + dx, base.y, base.z + dz, yaw, 0);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }

    private static void setup() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x, 200, z);
        cmd("gamemode spectator @a");
        cmd("time set noon");
        cmd("weather clear");
        cmd("gamerule doDaylightCycle false");
        cmd("kill @e[type=!minecraft:player]");
        cmd("fill " + (x - 16) + " 195 " + (z - 10) + " " + (x + 16) + " 198 " + (z + 22) + " dirt");
        cmd("fill " + (x - 16) + " 199 " + (z - 10) + " " + (x + 16) + " 199 " + (z + 22) + " grass_block");
        cmd("fill " + (x - 16) + " 200 " + (z - 10) + " " + (x + 16) + " 215 " + (z + 22) + " air");
        // a stream 2 wide running east-west at z+10..11, 2 deep; a dam of two courses across it at x, the water behind higher
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = 10; dz <= 11; dz++) {
                set(dx, -2, dz, Blocks.WATER);
                set(dx, -1, dz, Blocks.WATER);
            }
        }
        for (int dz = 10; dz <= 11; dz++) {
            set(0, -1, dz, com.mechanical.wildlife.river.RiverFauna.BEAVER_DAM.get());
            set(0, 0, dz, com.mechanical.wildlife.river.RiverFauna.BEAVER_DAM.get());
            for (int dx = -14; dx < 0; dx++) set(dx, 0, dz, Blocks.WATER); // the pond behind the dam, one block up
        }
        for (int dx = -15; dx < 0; dx++) for (int side : new int[] {9, 12}) set(dx, 0, side, Blocks.GRASS_BLOCK); // raised banks hold the pond
        for (int dz = 10; dz <= 11; dz++) set(-15, 0, dz, Blocks.GRASS_BLOCK);
        // a few young trees by the water, one gnawed down already (a stump)
        set(5, 0, 7, Blocks.OAK_LOG);
        set(-6, 0, 7, Blocks.BIRCH_SAPLING);
        spawn(com.mechanical.wildlife.river.RiverFauna.BEAVER.get(), 3.5, 9.0, 110);

        // a joined trough of two with water and wheat, a cow and a sheep eating, a horse drinking from the end
        for (int dx = -4; dx <= -3; dx++) set(dx, 0, 2, Husbandry.FEEDING_TROUGH.get());
        FeedingTroughBlock.fillWater(level, at(-4, 0, 2));
        FeedingTroughBlock.addFood(level, at(-3, 0, 2), new ItemStack(Items.WHEAT, 32));
        long eatForever = level.getGameTime() + 1_000_000;
        Animal cow = spawn(EntityTypes.COW, -3.5, 0.9, 0);
        Animal sheep = spawn(EntityTypes.SHEEP, -2.5, 3.9, 180);
        AbstractHorse horse = spawn(EntityTypes.HORSE, -5.9, 2.5, -90);
        horse.setTamed(true);
        for (Animal a : new Animal[] {cow, sheep, horse}) a.setData(Husbandry.NEEDS, new Husbandry.Needs(eatForever, eatForever, eatForever));

        // a crowded pen floor churned in stages, and a cow there out of food and water
        for (int dx = -12; dx <= -7; dx++) {
            for (int dz = -2; dz <= 3; dz++) {
                int k = Math.floorMod(dx * 7 + dz * 13, 5);
                set(dx, -1, dz, k == 0 ? Blocks.GRASS_BLOCK : k == 1 ? Blocks.DIRT : k == 2 ? Blocks.COARSE_DIRT : k == 3 ? Blocks.MUD : Blocks.PACKED_MUD);
            }
        }
        Animal tired = spawn(EntityTypes.COW, -9.5, 2.5, 90);
        long now = level.getGameTime();
        tired.setData(Husbandry.NEEDS, new Husbandry.Needs(now - 1, now - 1, 0));
        result.add("setup at " + base + ", tired " + Husbandry.tired(tired));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "nature_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private NatureShots() {}
}
