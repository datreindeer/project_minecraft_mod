package com.mechanical;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/** A tool that stays in the crafting grid, losing 1 durability per craft (the Drop Spindle). */
public class GridToolItem extends Item {
    public GridToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        if (!(instance instanceof ItemStack stack)) return null;
        ItemStack used = stack.copyWithCount(1);
        used.setDamageValue(used.getDamageValue() + 1);
        return used.getDamageValue() >= used.getMaxDamage() ? null : ItemStackTemplate.fromNonEmptyStack(used);
    }
}
