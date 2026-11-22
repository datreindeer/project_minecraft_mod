package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.Equipment;
import com.mechanical.equipment.PackMenu;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with a file {@code p8shots.flag} in the game directory (its first line: an output folder), the client opens
 * the world "P8Shots", builds a big Fire Pit (grown with the Steel Hearth Ring, lit, a roast and food on it, the big tripod
 * and kettle), a Steel Casting Table by a Clay Kiln and a Jeweler's Bench with work on them, and takes p8_*.png shots of
 * them and of the satchel-with-tool-loops and Steel-Frame Pack screens (B); writes p8_result.txt and quits. Nothing
 * happens without the flag file. Pattern: LogShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class P8Shots {
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static Vec3 base;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "p8shots.flag");
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
                    mc.createWorldOpenFlows().openWorld("P8Shots", () -> {});
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
                    if (mc.player.isDeadOrDying()) mc.player.respawn();
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                    server.execute(P8Shots::setup);
                    next();
                }
                case 2 -> { // the big pit from the side, a little above
                    if (ticks < 120) return; // smoke builds up
                    shot(mc, "big_fire_pit");
                    server.execute(() -> look(base.add(1.5, 4.5, -2.0), 0, 62));
                    next();
                }
                case 3 -> {
                    if (ticks < 30) return;
                    shot(mc, "big_fire_pit_top");
                    server.execute(() -> look(base.add(12.5, 1.6, -0.6), 0, 40));
                    next();
                }
                case 4 -> {
                    if (ticks < 30) return;
                    shot(mc, "jewelers_bench");
                    if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                    server.execute(() -> {
                        ServerPlayer p = player();
                        ItemStack satchel = new ItemStack(Mechanical.LEATHER_SATCHEL.get());
                        satchel.set(Satchels.LOOPED.get(), Unit.INSTANCE);
                        Equipment.of(p).set(Accessory.BACK, satchel);
                        PackMenu.openWorn(p);
                        var menu = p.containerMenu;
                        menu.getSlot(0).set(new ItemStack(Items.BREAD, 12));
                        menu.getSlot(1).set(new ItemStack(Items.TORCH, 32));
                        menu.getSlot(4).set(new ItemStack(Items.ARROW, 20));
                        menu.getSlot(9).set(new ItemStack(Smithing.STEEL_SAW.get()));
                        menu.getSlot(10).set(new ItemStack(Smithing.STEEL_KNIFE.get()));
                        menu.getSlot(11).set(new ItemStack(Smithing.STEEL_SMITHING_HAMMER.get()));
                        result.add("satchel menu: " + menu.slots.size() + " slots");
                    });
                    next();
                }
                case 5 -> {
                    if (ticks < 20) return;
                    result.add("screen: " + mc.gui.screen());
                    shot(mc, "satchel_gui");
                    server.execute(() -> {
                        ServerPlayer p = player();
                        p.closeContainer();
                        ItemStack pack = new ItemStack(Satchels.STEEL_FRAME_PACK.get());
                        Equipment.of(p).set(Accessory.BACK, pack);
                        Equipment.of(p).set(Accessory.HIP, new ItemStack(Mechanical.QUIVER.get()));
                        PackMenu.openWorn(p);
                        var menu = p.containerMenu;
                        for (int i = 0; i < 25; i += 3) menu.getSlot(i).set(new ItemStack(i % 2 == 0 ? Items.COBBLESTONE : Items.OAK_LOG, 64));
                        menu.getSlot(25).set(new ItemStack(Smithing.STEEL_CHISEL.get()));
                        result.add("pack menu: " + menu.slots.size() + " slots");
                    });
                    next();
                }
                case 6 -> {
                    if (ticks < 20) return;
                    shot(mc, "pack_gui");
                    next();
                }
                case 7 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "p8_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> {}
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "p8_result.txt").toPath(), result);
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
        p.level().setBlockAndUpdate(BlockPos.containing(at).below(), Blocks.BARRIER.defaultBlockState()); // something to stand on up there
        p.teleportTo(p.level(), at.x, Math.floor(at.y), at.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void setup() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        p.setHealth(p.getMaxHealth());
        p.fallDistance = 0;
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        base = new Vec3(x, 200, z);
        cmd("gamemode creative @a");
        cmd("time set 13000");
        cmd("weather clear");
        cmd("gamerule doDaylightCycle false");
        cmd("kill @e[type=!player]"); // felled logs and carts left in the borrowed world
        p.getAbilities().flying = true; // the camera stays where it is put
        p.onUpdateAbilities();
        cmd("fill " + (x - 10) + " 199 " + (z - 10) + " " + (x + 20) + " 199 " + (z + 12) + " grass_block");
        cmd("fill " + (x - 10) + " 200 " + (z - 10) + " " + (x + 20) + " 212 " + (z + 12) + " air");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20000, 0, false, false));
        // a small pit at (x, z)..(x+1, z+1), grown with the Steel Hearth Ring: the player stands north-west, so it grows south-east
        BlockPos c = new BlockPos(x, 200, z);
        BlockState pit = FirePit.FIRE_PIT.get().defaultBlockState();
        for (int part = 0; part < 4; part++) level.setBlockAndUpdate(c.offset(part & 1, 0, part >> 1), pit.setValue(FirePitBlock.PART, part));
        p.teleportTo(level, x - 3, 200, z - 3, java.util.Set.of(), 0, 0, true);
        result.add("grow: " + FirePitBlock.upgrade(level, c, p));
        FirePitBlockEntity e = FirePit.entity(level, c, level.getBlockState(c));
        result.add("big: " + FirePit.isBig(level.getBlockState(c)) + " entity " + (e == null ? null : e.getBlockPos()));
        if (e != null) {
            for (int i = 0; i < 20; i++) e.feed(level, p, new ItemStack(Mechanical.FIREWOOD.get("oak").get()));
            e.light();
            ItemStack food = new ItemStack(Items.MUTTON, 4);
            for (int i = 0; i < 3; i++) e.placeFood(level, p, food);
            ItemStack fish = new ItemStack(Items.COD, 3);
            for (int i = 0; i < 3; i++) e.placeFood(level, p, fish);
            var cow = EntityTypes.COW.create(level, EntitySpawnReason.LOAD);
            cow.setPos(x + 6, 200, z + 6);
            CarcassEntity carcass = CarcassEntity.of(level, cow);
            level.addFreshEntity(carcass);
            carcass.setSkinned();
            result.add("roast hung: " + e.hangCarcass(level, p, carcass));
        }
        // the big tripod with its kettle over the middle (x+1, z+1)
        BlockPos mid = e == null ? c.offset(1, 0, 1) : e.getBlockPos().offset(1, 0, 1);
        level.setBlockAndUpdate(mid.above(), Kettle.TRIPOD.get().defaultBlockState().setValue(TripodBlock.PIT, TripodBlock.BIG_PIT).setValue(TripodBlock.KETTLE, true));
        // gold: a Clay Kiln on a campfire with the Steel Casting Table beside it, the Jeweler's Bench next along
        BlockPos k = new BlockPos(x + 11, 200, z + 2);
        BlockState kiln = Mechanical.KILN.get().defaultBlockState();
        level.setBlockAndUpdate(k, kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.LOWER).setValue(KilnBlock.LIT, true));
        level.setBlockAndUpdate(k.above(), kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.UPPER).setValue(KilnBlock.LIT, true));
        level.setBlockAndUpdate(k.east(), Jewelry.STEEL_CASTING_TABLE.get().defaultBlockState());
        if (level.getBlockEntity(k.east()) instanceof JewelryTableBlockEntity t) {
            t.items().set(0, new ItemStack(Items.GOLD_INGOT, 3));
            t.items().set(1, new ItemStack(Jewelry.GOLD_RING_BLANK.get(), 2));
            t.setChanged();
        }
        level.setBlockAndUpdate(k.east(2), Jewelry.JEWELERS_BENCH.get().defaultBlockState());
        if (level.getBlockEntity(k.east(2)) instanceof JewelryTableBlockEntity b) {
            b.items().set(0, new ItemStack(Jewelry.GOLD_CHAIN.get()));
            b.items().set(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, "flawless_sapphire"))));
            b.setChanged();
        }
        // finished pieces on display in item frames would need entities; a ruby ring and a garnet necklace go in the hotbar instead
        p.getInventory().setItem(0, Jewelry.make(Jewelry.GOLD_RING.get(), new Jewelry.Jewel("ruby", true)));
        p.getInventory().setItem(1, Jewelry.make(Jewelry.GOLD_NECKLACE.get(), new Jewelry.Jewel("garnet", false)));
        p.getInventory().setItem(2, Jewelry.make(Jewelry.GOLD_RING.get(), new Jewelry.Jewel("sapphire", false)));
        look(base.add(-2.6, 1.8, -2.6), -45, 28);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "p8_" + name + ".png"));
            } catch (Exception ex) {
                result.add("screenshot " + name + " failed: " + ex);
            }
        });
    }

    private P8Shots() {}
}
