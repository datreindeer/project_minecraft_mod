package com.mechanical.equipment;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The container behind the {@link AccessorySlot}s of a player's inventory menu. It looks the player's
 * {@link EquipmentInventory} up on every call instead of holding it, because the menu is built in the Player constructor,
 * before the saved attachment is read (which replaces the default one), and a client's copy is replaced by syncs.
 */
final class EquipmentView implements Container {
    private final Player player;

    EquipmentView(Player player) {
        this.player = player;
    }

    private EquipmentInventory inventory() {
        return Equipment.of(player);
    }

    @Override
    public int getContainerSize() {
        return Accessory.COUNT;
    }

    @Override
    public boolean isEmpty() {
        return inventory().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory().getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return inventory().removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory().removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory().setItem(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        inventory().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        inventory().clearContent();
    }
}
