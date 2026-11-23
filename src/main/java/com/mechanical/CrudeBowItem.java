package com.mechanical;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Stone age bow from the Tool Station: works like a bow, but its arrows hit softer (whatever the arrow) (and it wears out fast). */
public class CrudeBowItem extends BowItem {
    public static final double DAMAGE_FACTOR = 0.7;

    public CrudeBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(SpecialArrowItem.baseDamage(projectileStack) * DAMAGE_FACTOR); // AbstractArrow has no getter
        return arrow;
    }
}
