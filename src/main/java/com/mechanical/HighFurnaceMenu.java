package com.mechanical;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * The High Furnace's screen (client/HighFurnaceScreen): the synced numbers, and on the client the Front's own block
 * entity (synced every half second) for the melting places and the pool's layers. A click on a layer chooses what the
 * tapholes pour ({@link #clickMenuButton}: the layer's {@link Molten} ordinal, {@link #BOTTOM} for the bottom layer).
 */
public class HighFurnaceMenu extends AbstractContainerMenu {
    public static final int BOTTOM = 99;
    private final HighFurnaceBlockEntity furnace;
    private final ContainerData data;
    private final net.minecraft.core.BlockPos pos;
    private final net.minecraft.world.level.Level level;

    static HighFurnaceMenu client(int containerId, Inventory inventory, net.minecraft.network.RegistryFriendlyByteBuf buf) {
        return new HighFurnaceMenu(containerId, inventory, null, new SimpleContainerData(22), buf.readBlockPos());
    }

    public HighFurnaceMenu(int containerId, Inventory inventory, HighFurnaceBlockEntity furnace, ContainerData data, net.minecraft.core.BlockPos pos) {
        super(HighFurnace.HIGH_FURNACE_MENU.get(), containerId);
        this.furnace = furnace;
        this.data = data;
        this.pos = pos;
        this.level = inventory.player.level();
        addDataSlots(data);
    }

    /** The Front's block entity (the client's copy on the client), or null. */
    public HighFurnaceBlockEntity furnace() {
        return furnace != null ? furnace : level.getBlockEntity(pos) instanceof HighFurnaceBlockEntity f ? f : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (furnace == null) return false;
        if (id == BOTTOM) furnace.select(null);
        else if (id >= 0 && id < Molten.values().length) furnace.select(Molten.values()[id]);
        else return false;
        return true;
    }

    public int temperature() { return data.get(0); }
    public int target() { return data.get(1); }
    public boolean lit() { return data.get(2) != 0; }
    public float effectiveBlast() { return data.get(3) / 10F; }
    public int volume() { return data.get(4); }
    public int fireboxes() { return data.get(5); }
    public int dryingSeconds() { return data.get(6); }
    public int pool() { return data.get(7); }
    public int capacity() { return data.get(8); }
    public int fuel(int i) { return data.get(9 + i); }
    public int blast(int i) { return data.get(13 + i); }
    public boolean burning(int i) { return data.get(17 + i) != 0; }
    public int totalBlast() { return data.get(21); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return furnace == null || (!furnace.isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(furnace.getBlockPos())) <= 64);
    }
}
