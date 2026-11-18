package com.mechanical;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The Bark Basket's 3x3 inventory. Only fruit, berries and hides/leather fit (tag mechanical:basket_storable). The contents
 * live on the basket item itself (container component) and are written back on every change. While open, the basket's
 * own inventory slot is locked so it can't be moved or dropped.
 */
public class BarkBasketMenu extends AbstractContainerMenu {
    public static final int SIZE = 9;
    public static final TagKey<Item> STORABLE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "basket_storable"));
    private static final int INV_START = SIZE, INV_END = SIZE + 36;

    /** Inventory index of the open basket (40 = off hand), -1 on the client. */
    private final int lockedSlot;
    private final ItemStack basketStack;

    /** Client side. */
    public BarkBasketMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE), -1, ItemStack.EMPTY);
    }

    /** Server side: {@code stack} is the basket being held, at inventory index {@code slot}. */
    public static BarkBasketMenu open(int containerId, Inventory inventory, int slot, ItemStack stack) {
        SimpleContainer contents = new SimpleContainer(SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
            }
        };
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        for (int i = 0; i < SIZE; i++) contents.setItem(i, items.get(i));
        return new BarkBasketMenu(containerId, inventory, contents, slot, stack);
    }

    private BarkBasketMenu(int containerId, Inventory inventory, Container basket, int lockedSlot, ItemStack basketStack) {
        super(Mechanical.BARK_BASKET_MENU.get(), containerId);
        this.lockedSlot = lockedSlot;
        this.basketStack = basketStack;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(basket, col + row * 3, 62 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(STORABLE);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col, 8 + col * 18, 142));
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != lockedSlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return index != lockedSlot;
            }
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (!stack.is(STORABLE) || !moveItemStackTo(stack, 0, SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        // client: always; server: the player still holds this exact basket
        return lockedSlot < 0 || player.getInventory().getItem(lockedSlot) == basketStack;
    }
}
