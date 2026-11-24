package com.mechanical;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** The High Furnace's screen (client/HighFurnaceScreen): no slots yet (phase 3 adds the melting places), only the synced numbers. */
public class HighFurnaceMenu extends AbstractContainerMenu {
    private final HighFurnaceBlockEntity furnace;
    private final ContainerData data;

    public HighFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(22));
    }

    public HighFurnaceMenu(int containerId, Inventory inventory, HighFurnaceBlockEntity furnace, ContainerData data) {
        super(HighFurnace.HIGH_FURNACE_MENU.get(), containerId);
        this.furnace = furnace;
        this.data = data;
        addDataSlots(data);
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
