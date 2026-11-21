package com.mechanical;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Yew Longbow (Bronze Age, Tool Station: Sinew + Leather + Yew Stave): the heavy bow. Its arrows fly {@link #SPEED_FACTOR}
 * times as fast as a plain bow's (arrow damage goes with speed, so more damage and range), but a full draw takes
 * {@link #DRAW_TICKS} ticks instead of vanilla's 20. The Horn Bow stays the quick one.
 */
public class YewLongbowItem extends BowItem {
    public static final float SPEED_FACTOR = 1.3F;
    public static final int DRAW_TICKS = 30;

    public YewLongbowItem(Properties properties) {
        super(properties);
    }

    /** Vanilla's power curve stretched over {@link #DRAW_TICKS}. */
    public static float powerForTime(int timeHeld) {
        return getPowerForTime(timeHeld * MAX_DRAW_DURATION / DRAW_TICKS);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        // vanilla's getPowerForTime is static, so hand super a held time scaled down to its 20-tick curve
        int duration = getUseDuration(stack, entity);
        int held = duration - remainingTime;
        return super.releaseUsing(stack, level, entity, duration - held * MAX_DRAW_DURATION / DRAW_TICKS);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power, float uncertainty, float angle, LivingEntity target) {
        super.shootProjectile(shooter, projectile, index, power * SPEED_FACTOR, uncertainty, angle, target);
    }
}
