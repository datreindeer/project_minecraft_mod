package com.mechanical;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Furnace layout: input top, fuel bottom, flame between, arrow to the output. */
public class KilnMenu extends AbstractContainerMenu {
    private static final int INV_START = 3, INV_END = 39;
    private final Container kiln;
    private final ContainerData data;

    public KilnMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    public KilnMenu(int containerId, Inventory inventory, Container kiln, ContainerData data) {
        super(Mechanical.KILN_MENU.get(), containerId);
        this.kiln = kiln;
        this.data = data;
        addSlot(new Slot(kiln, KilnBlockEntity.INPUT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KilnBlockEntity.recipeFor(stack).isPresent();
            }
        });
        addSlot(new Slot(kiln, KilnBlockEntity.FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KilnBlockEntity.burnTicks(stack) > 0;
            }
        });
        addSlot(new Slot(kiln, KilnBlockEntity.OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    /** Baking progress, 0..1. */
    public float progress() {
        int total = data.get(1);
        return total == 0 ? 0 : (float) data.get(0) / total;
    }

    /** Fuel left in the current item, 0..1; 0 when the fire is out. */
    public float burn() {
        int total = data.get(3);
        return total == 0 ? 0 : (float) data.get(2) / total;
    }

    public boolean lit() {
        return data.get(3) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            // recipe input first (logs are both), then fuel
            boolean moved = (KilnBlockEntity.recipeFor(stack).isPresent() && moveItemStackTo(stack, KilnBlockEntity.INPUT, KilnBlockEntity.INPUT + 1, false))
                    || (KilnBlockEntity.burnTicks(stack) > 0 && moveItemStackTo(stack, KilnBlockEntity.FUEL, KilnBlockEntity.FUEL + 1, false));
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return kiln.stillValid(player);
    }
}
