package com.mechanical;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * The Furnace Salamander (docs/steel-furnace-plan.md section 3): the heavy lump of frozen metal and slag left when a High
 * Furnace's Front is broken with melt in it. It carries half of every layer ({@link MeltPool#save} form) and melts back
 * into a High Furnace at 1200° or more, as far as there is room.
 */
public final class FurnaceSalamander {
    public static ItemStack of(int[] pool) {
        ItemStack s = new ItemStack(HighFurnace.FURNACE_SALAMANDER.get());
        CompoundTag t = new CompoundTag();
        t.putIntArray("pool", pool);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    public static int[] pool(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? new int[0] : d.copyTag().getIntArray("pool").orElse(new int[0]);
    }

    private FurnaceSalamander() {}
}
