package com.mechanical;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import java.util.function.Predicate;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Furnace layout: input top, fuel bottom, flame between, arrow to the output. Also the Bloomery's (same slots and data). */
public class KilnMenu extends AbstractContainerMenu {
    private static final int INV_START = 3, INV_END = 39;
    private final Container kiln;
    private final ContainerData data;
    private final Predicate<ItemStack> input, fuel;

    public KilnMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(8));
    }

    public KilnMenu(int containerId, Inventory inventory, Container kiln, ContainerData data) {
        this(Mechanical.KILN_MENU.get(), containerId, inventory, kiln, data, KilnBlockEntity::canBake, s -> KilnBlockEntity.burnTicks(s) > 0);
    }

    /** The Bloomery's screen: Raw Iron or a cold bloom in, Charcoal as fuel, the bloom out. */
    public static KilnMenu bloomery(int containerId, Inventory inventory) {
        return bloomery(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(8));
    }

    public static KilnMenu bloomery(int containerId, Inventory inventory, Container bloomery, ContainerData data) {
        return new KilnMenu(Ironworking.BLOOMERY_MENU.get(), containerId, inventory, bloomery, data, BloomeryBlockEntity::canSmelt,
                s -> BloomeryBlockEntity.burnTicks(s) > 0);
    }

    private KilnMenu(MenuType<?> type, int containerId, Inventory inventory, Container kiln, ContainerData data, Predicate<ItemStack> input, Predicate<ItemStack> fuel) {
        super(type, containerId);
        this.kiln = kiln;
        this.data = data;
        this.input = input;
        this.fuel = fuel;
        addSlot(new Slot(kiln, KilnBlockEntity.INPUT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return input.test(stack);
            }
        });
        addSlot(new Slot(kiln, KilnBlockEntity.FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return fuel.test(stack);
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

    /** Kiln temperature in degrees. */
    public int temperature() {
        return data.get(4);
    }

    /** Overheating strain, 0..1. */
    public float strain() {
        return data.get(5) / (float) KilnBlockEntity.STRAIN_MAX;
    }

    /** The Bloomery's screen (hotter thermometer scale, its own marks). */
    public boolean isBloomery() {
        return getType() == Ironworking.BLOOMERY_MENU.get();
    }

    public boolean cracked() {
        return data.get(6) != 0;
    }

    /** Degrees the current bake (or the first firing) needs; 0 when nothing is baking. */
    public int needed() {
        return data.get(7);
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
            boolean moved = (input.test(stack) && moveItemStackTo(stack, KilnBlockEntity.INPUT, KilnBlockEntity.INPUT + 1, false))
                    || (fuel.test(stack) && moveItemStackTo(stack, KilnBlockEntity.FUEL, KilnBlockEntity.FUEL + 1, false));
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
