package com.mechanical.equipment;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The inventory inside a bag item (its container component) as a live {@link Container} for a menu. Changes are written
 * to the item at once; if the item's contents change from outside while the menu is open (arrows picked up into a worn
 * quiver), the next access reloads them, so neither side overwrites the other.
 */
final class ItemContents implements Container {
    private final ItemStack bag;
    private final NonNullList<ItemStack> items;
    private final Predicate<ItemStack> allowed;
    private @Nullable ItemContainerContents loaded;

    ItemContents(ItemStack bag, int size, Predicate<ItemStack> allowed) {
        this.bag = bag;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.allowed = allowed;
    }

    private NonNullList<ItemStack> items() {
        ItemContainerContents now = bag.get(DataComponents.CONTAINER);
        if (now != loaded) {
            for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
            if (now != null) now.copyInto(items);
            loaded = now;
        }
        return items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items()) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(items(), slot, count);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = ContainerHelper.takeItem(items(), slot);
        setChanged();
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items().set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return allowed.test(stack);
    }

    @Override
    public void setChanged() {
        ItemContainerContents contents = ItemContainerContents.fromItems(items);
        bag.set(DataComponents.CONTAINER, contents);
        loaded = bag.get(DataComponents.CONTAINER);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items().clear();
        setChanged();
    }
}
