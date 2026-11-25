package com.mechanical;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * The Furnace Salamander (docs/steel-furnace-plan.md section 3): the heavy lump of frozen metal and slag left when a High
 * Furnace's Front is broken with melt in it. It carries half of every layer ({@link MeltPool#save} form, under "melt";
 * one from before 0.208.0 has the old numbers under "pool") and melts back into a High Furnace at 1200° or more, as far as
 * there is room.
 */
public final class FurnaceSalamander {
    public static ItemStack of(MeltPool half) {
        ItemStack s = new ItemStack(HighFurnace.FURNACE_SALAMANDER.get());
        CompoundTag t = new CompoundTag();
        t.put("melt", half.save());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    /** What it carries, into {@code into}. */
    public static void load(ItemStack s, MeltPool into) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        CompoundTag t = d == null ? new CompoundTag() : d.copyTag();
        if (t.contains("melt")) into.load(t.getCompoundOrEmpty("melt"));
        else into.loadLegacy(t.getIntArray("pool").orElse(new int[0]));
    }

    private FurnaceSalamander() {}
}
