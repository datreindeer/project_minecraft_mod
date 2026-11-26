package com.mechanical;

import java.io.File;
import java.nio.file.Files;

import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.Equipment;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with a file {@code magmashots.flag} in the game directory (its first line: an output folder), the client opens
 * the world "MagmaShots" and takes shots of the metal blocks, the Magma Block under a Baking Stone and a Clay Kiln, the
 * electrum casting table and the items in the inventory; then quits. Pattern: P8Shots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class MagmaShots {
    private static int stage = -1, ticks;
    private static File out;
    private static final int Y = 200;
    private static int x, z;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (stage == -1) {
            File flag = new File(mc.gameDirectory, "magmashots.flag");
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
                mc.createWorldOpenFlows().openWorld("MagmaShots", () -> {});
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
                server.execute(MagmaShots::setup);
                next();
            }
            case 2 -> { // the three metals side by side
                if (ticks < 100) return;
                shot(mc, "metals");
                server.execute(() -> look(x + 1.5, Y + 0.6, z - 0.4, 0, 33));
                next();
            }
            case 3 -> {
                if (ticks < 40) return;
                shot(mc, "metals_close");
                server.execute(() -> look(x + 8.0, Y + 0.8, z - 0.3, 0, 30));
                next();
            }
            case 4 -> { // baking stone and kiln on magma
                if (ticks < 60) return;
                shot(mc, "magma_stations");
                server.execute(() -> look(x + 12.5, Y + 1.0, z + 0.2, 0, 35));
                next();
            }
            case 5 -> {
                if (ticks < 60) return;
                shot(mc, "electrum_casting");
                mc.gui.hud.toggle();
                server.execute(() -> player().setGameMode(net.minecraft.world.level.GameType.SURVIVAL));
                next();
            }
            case 6 -> {
                if (ticks < 20) return;
                mc.gui.setScreen(new InventoryScreen(mc.player));
                next();
            }
            case 7 -> {
                if (ticks < 20) return;
                shot(mc, "inventory");
                next();
            }
            case 8 -> {
                if (ticks < 20) return;
                stage = -2;
                mc.stop();
            }
            default -> {}
        }
    }

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static void look(double px, double py, double pz, float yaw, float pitch) {
        player().teleportTo(player().level(), px, py, pz, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void frame(ServerLevel level, BlockPos on, Item item) {
        ItemFrame f = new ItemFrame(level, on.north(), Direction.NORTH);
        f.setItem(new ItemStack(item), false);
        level.addFreshEntity(f);
    }

    private static void setup() {
        ServerPlayer p = player();
        ServerLevel level = p.level();
        x = (int) Math.floor(p.getX());
        z = (int) Math.floor(p.getZ());
        cmd("gamemode creative @a");
        cmd("time set 6000");
        cmd("weather clear");
        cmd("difficulty peaceful");
        cmd("fill " + (x - 8) + " 199 " + (z - 6) + " " + (x + 20) + " 199 " + (z + 12) + " smooth_stone");
        cmd("fill " + (x - 8) + " 200 " + (z - 6) + " " + (x + 20) + " 208 " + (z + 12) + " air");
        cmd("kill @e[type=!player]");
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        // the metals: silver, electrum, gold blocks with their pieces framed above
        BlockPos row = new BlockPos(x, Y, z + 3);
        level.setBlockAndUpdate(row, HighFurnace.SILVER_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(row.east(1), HighFurnace.ELECTRUM_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(row.east(2), Blocks.GOLD_BLOCK.defaultBlockState());
        for (int i = 0; i < 3; i++) level.setBlockAndUpdate(row.east(i).above(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
        frame(level, row.above(), IronOres.SILVER_INGOT.get());
        frame(level, row.east(1).above(), IronOres.ELECTRUM_INGOT.get());
        frame(level, row.east(2).above(), Items.GOLD_INGOT);
        // a Baking Stone with dough, and a campfire under a kiln, both on magma
        BlockPos stone = new BlockPos(x + 7, Y + 1, z + 2);
        level.setBlockAndUpdate(stone.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(stone, Farming.BAKING_STONE.get().defaultBlockState().setValue(BakingStoneBlock.CONTENTS, BakingStoneBlock.Contents.BREAD));
        BlockPos kiln = new BlockPos(x + 9, Y + 1, z + 2);
        level.setBlockAndUpdate(kiln.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
        BlockState k = Mechanical.KILN.get().defaultBlockState().setValue(KilnBlock.FACING, Direction.NORTH);
        level.setBlock(kiln, k.setValue(KilnBlock.HALF, DoubleBlockHalf.LOWER), 2);
        level.setBlock(kiln.above(), k.setValue(KilnBlock.HALF, DoubleBlockHalf.UPPER), 2);
        if (level.getBlockEntity(kiln) instanceof KilnBlockEntity be) {
            be.setTemperatureForTest(Magma.HEAT);
            be.setItem(KilnBlockEntity.INPUT, new ItemStack(Items.CLAY_BALL, 32));
        }
        // electrum on the casting table and the bench
        BlockPos table = new BlockPos(x + 12, Y, z + 3);
        level.setBlockAndUpdate(table, Jewelry.STEEL_CASTING_TABLE.get().defaultBlockState());
        level.setBlockAndUpdate(table.west(), Jewelry.JEWELERS_BENCH.get().defaultBlockState());
        if (level.getBlockEntity(table) instanceof JewelryTableBlockEntity t) {
            t.items().set(0, new ItemStack(IronOres.ELECTRUM_INGOT.get(), 3));
            t.items().set(1, new ItemStack(Jewelry.ELECTRUM_RING_BLANK.get(), 2));
            t.setChanged();
            level.sendBlockUpdated(table, t.getBlockState(), t.getBlockState(), 3);
        }
        if (level.getBlockEntity(table.west()) instanceof JewelryTableBlockEntity t) {
            t.items().set(0, new ItemStack(Jewelry.ELECTRUM_CHAIN.get()));
            t.items().set(1, new ItemStack(Items.EMERALD));
            level.sendBlockUpdated(table.west(), t.getBlockState(), t.getBlockState(), 3);
        }
        // the inventory: the three metals' jewellery next to each other
        Item[][] rows = {{IronOres.SILVER_INGOT.get(), IronOres.SILVER_NUGGET.get(), HighFurnace.SILVER_BLOCK_ITEM.get(), Jewelry.SILVER_RING_BLANK.get(), Jewelry.SILVER_CHAIN.get()},
                {IronOres.ELECTRUM_INGOT.get(), IronOres.ELECTRUM_NUGGET.get(), HighFurnace.ELECTRUM_BLOCK_ITEM.get(), Jewelry.ELECTRUM_RING_BLANK.get(), Jewelry.ELECTRUM_CHAIN.get()},
                {Items.GOLD_INGOT, Items.GOLD_NUGGET, Items.GOLD_BLOCK, Jewelry.GOLD_RING_BLANK.get(), Jewelry.GOLD_CHAIN.get()}};
        p.getInventory().clearContent();
        for (int r = 0; r < 3; r++)
            for (int i = 0; i < 5; i++) p.getInventory().setItem(9 + r * 9 + i, new ItemStack(rows[r][i]));
        Item[][] pieces = {{Jewelry.SILVER_RING.get(), Jewelry.SILVER_NECKLACE.get()}, {Jewelry.ELECTRUM_RING.get(), Jewelry.ELECTRUM_NECKLACE.get()},
                {Jewelry.GOLD_RING.get(), Jewelry.GOLD_NECKLACE.get()}};
        for (int r = 0; r < 3; r++)
            for (int i = 0; i < 2; i++) p.getInventory().setItem(9 + r * 9 + 6 + i, Jewelry.make(pieces[r][i], new Jewelry.Jewel("ruby", false)));
        p.getInventory().setItem(0, new ItemStack(Magma.CRUSHED_MAGMA.get(), 12));
        p.getInventory().setItem(1, new ItemStack(Items.MAGMA_BLOCK, 4));
        p.getInventory().setItem(2, new ItemStack(HighFurnace.CRUSHED_QUARTZITE.get(), 4));
        Equipment.of(p).set(Accessory.RING_1, Jewelry.make(Jewelry.ELECTRUM_RING.get(), new Jewelry.Jewel("sapphire", true)));
        look(x + 1.5, Y + 0.8, z - 1.6, 0, 18);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, name + ".png"));
            } catch (Exception ex) {
                Mechanical.LOGGER.error("screenshot {} failed", name, ex);
            }
        });
    }

    private MagmaShots() {}
}
