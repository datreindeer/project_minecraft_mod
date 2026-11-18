package com.mechanical;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** The fletching table: three inputs (tip / shaft / fletching) and a result, see {@link FletchingRecipes}. */
public class FletchingMenu extends AbstractContainerMenu {
    private static final int RESULT = 3, INV_START = 4, INV_END = 40;

    private final ContainerLevelAccess access;
    private final ResultContainer result = new ResultContainer();
    private final SimpleContainer inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    public FletchingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public FletchingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(Mechanical.FLETCHING_MENU.get(), containerId);
        this.access = access;
        addSlot(new Slot(inputs, 0, 8, 48));
        addSlot(new Slot(inputs, 1, 26, 48));
        addSlot(new Slot(inputs, 2, 44, 48));
        addSlot(new Slot(result, 0, 98, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                for (int i = 0; i < 3; i++) inputs.removeItem(i, 1);
                super.onTake(player, stack);
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == inputs) {
            result.setItem(0, FletchingRecipes.find(inputs.getItem(0), inputs.getItem(1), inputs.getItem(2))
                    .map(FletchingRecipes.Recipe::assemble).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == RESULT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            slot.onTake(player, copy);
            return copy;
        }
        if (index < RESULT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, RESULT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inputs));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.FLETCHING_TABLE);
    }
}
