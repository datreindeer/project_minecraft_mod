package com.mechanical.skills;

import java.util.Locale;

import com.mechanical.Mechanical;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public enum Skill {
    SMITHING("iron_smithing_hammer"), MINING("steel_pickaxe"), FARMING("bronze_hoe"), FISHING("bone_hook_fishing_rod"),
    SLAUGHTER("iron_knife"), WOODCUTTING("steel_axe"), GOLD_SMITHING("gold_ring"), COOKING("iron_skillet"),
    HUNTING("bronze_spear"), MASONRY("iron_chisel"), LEATHERWORKING("leather_satchel"), FLETCHING("bronze_arrow");

    private final String icon;

    Skill(String icon) {
        this.icon = icon;
    }

    /** Saved under this key, so never rename. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String langKey() {
        return "skill." + Mechanical.MODID + "." + key();
    }

    public ItemStack icon() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, icon)));
    }
}
