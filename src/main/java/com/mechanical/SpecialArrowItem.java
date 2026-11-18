package com.mechanical;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Arrows from the fletching table: a normal arrow with a different base damage (vanilla 2.0), optionally burning
 * (sets what it hits on fire). The arrow remembers its item, so picking it up gives this arrow back.
 */
public class SpecialArrowItem extends ArrowItem {
    private final double damage;
    private final boolean fire;

    public SpecialArrowItem(Properties properties, double damage, boolean fire) {
        super(properties);
        this.damage = damage;
        this.fire = fire;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity owner, ItemStack weapon) {
        AbstractArrow arrow = super.createArrow(level, stack, owner, weapon);
        arrow.setBaseDamage(damage);
        if (fire) arrow.igniteForSeconds(100);
        return arrow;
    }
}
