package com.mechanical;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Stone age bow from the Tool Station: works like a bow, but its arrows hit softer (and it wears out fast). */
public class CrudeBowItem extends BowItem {
    private static final double DAMAGE_FACTOR = 0.7;

    public CrudeBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(2.0 * DAMAGE_FACTOR); // vanilla base 2.0 (no getter)
        return arrow;
    }
}
