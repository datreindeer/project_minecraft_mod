package com.mechanical;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The flint pickaxe doubles as a hammerstone: rocks are knapped into stone tool heads with it in the crafting grid,
 * and it stays there, losing one durability per use (like the knife).
 */
public class FlintPickaxeItem extends Item {
    public FlintPickaxeItem(Properties properties) {
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
