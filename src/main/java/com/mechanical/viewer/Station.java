package com.mechanical.viewer;

import java.util.Locale;

import com.mechanical.HighFurnace;
import com.mechanical.Ironworking;
import com.mechanical.Mechanical;
import com.mechanical.MoldBench;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Where a recipe is made. */
public enum Station {
    CRAFTING_SHAPED, CRAFTING_SHAPELESS, STONECUTTING, CAMPFIRE, SMOKING, SMELTING, TOOL_STATION, FLETCHING, ARMOR_STATION, LEATHERWORKING,
    MOLD_BENCH, PATTERN_BENCH, KILN, BLOOMERY;

    public ItemStack icon() {
        return new ItemStack(item());
    }

    private Item item() {
        return switch (this) {
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> Items.CRAFTING_TABLE;
            case STONECUTTING -> Items.STONECUTTER;
            case CAMPFIRE -> Items.CAMPFIRE;
            case SMOKING -> Items.SMOKER;
            case SMELTING -> Items.FURNACE;
            case TOOL_STATION -> Mechanical.TOOL_STATION_ITEM.get();
            case FLETCHING -> Items.FLETCHING_TABLE;
            case ARMOR_STATION -> Mechanical.ARMOR_STATION_ITEM.get();
            case LEATHERWORKING -> Mechanical.LEATHERWORKING_BENCH_ITEM.get();
            case MOLD_BENCH -> MoldBench.MOLD_BENCH_ITEM.get();
            case PATTERN_BENCH -> HighFurnace.PATTERN_BENCH_ITEM.get();
            case KILN -> Mechanical.KILN_ITEM.get();
            case BLOOMERY -> Ironworking.BLOOMERY_HEARTH_ITEM.get();
        };
    }

    public String langKey() {
        return "viewer.mechanical.station." + name().toLowerCase(Locale.ROOT);
    }
}
