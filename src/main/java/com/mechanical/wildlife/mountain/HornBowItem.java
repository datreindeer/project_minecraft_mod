package com.mechanical.wildlife.mountain;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;

/**
 * Composite Horn Bow (Fletching Table: Ibex Horn over a Sinew Bow): stiffer limbs throw the arrow faster - arrow damage
 * goes with speed, so about a fifth more damage and range than a plain bow, whatever the arrow.
 */
public class HornBowItem extends BowItem {
    public static final float SPEED_FACTOR = 1.2F;

    public HornBowItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power, float uncertainty, float angle, LivingEntity target) {
        super.shootProjectile(shooter, projectile, index, power * SPEED_FACTOR, uncertainty * 0.8F, angle, target);
    }
}
