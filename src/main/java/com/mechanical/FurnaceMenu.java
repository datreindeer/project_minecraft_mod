package com.mechanical;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** The Smelting Furnace's status screen: no slots (everything goes in down the shaft), only the synced numbers. */
public class FurnaceMenu extends AbstractContainerMenu {
    private final FurnaceTapBlockEntity tap;
    private final ContainerData data;

    public FurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(15));
    }

    public FurnaceMenu(int containerId, Inventory inventory, FurnaceTapBlockEntity tap, ContainerData data) {
        super(Smelting.FURNACE_MENU.get(), containerId);
        this.tap = tap;
        this.data = data;
        addDataSlots(data);
    }

    public int temperature() {
        return data.get(0);
    }

    public boolean lit() {
        return data.get(1) != 0;
    }

    /** Fuel left in the burning piece, 0..1. */
    public float burn() {
        return data.get(3) == 0 ? 0 : (float) data.get(2) / data.get(3);
    }

    public int fuel() {
        return data.get(4);
    }

    public boolean open() {
        return data.get(5) != 0;
    }

    /** Degrees the unmelted charge needs; 0 when everything is molten. */
    public int meltTemp() {
        return data.get(6);
    }

    /** The furnace's contents as numbers, rebuilt from the synced data (the screen reuses FurnaceMelt's rules). */
    public FurnaceMelt melt() {
        FurnaceMelt m = new FurnaceMelt();
        for (int i = 0; i < FurnaceMelt.METALS.length; i++) {
            m.setPool(FurnaceMelt.METALS[i], data.get(7 + i));
            m.charge[i] = data.get(11 + i);
        }
        return m;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return tap == null || (!tap.isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(tap.getBlockPos())) <= 64);
    }
}
