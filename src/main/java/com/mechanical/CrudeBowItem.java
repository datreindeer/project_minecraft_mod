package com.mechanical;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mechanical.tools.ToolParts;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Stone age bow from the Tool Station: works like a bow, but its arrows hit softer (whatever the arrow) (and it wears out fast).
 * Its string, grip and stave change the arrow's speed and the draw time ({@link ToolParts.Ranged}).
 */
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

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        int duration = getUseDuration(stack, entity);
        return super.releaseUsing(stack, level, entity, duration - ToolParts.ranged(stack).onVanillaCurve(duration - remainingTime, MAX_DRAW_DURATION));
    }

    @Override
    protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> projectiles, float power,
                         float uncertainty, boolean crit, @Nullable LivingEntity target) {
        super.shoot(level, shooter, hand, weapon, projectiles, power * ToolParts.ranged(weapon).power(), uncertainty, crit, target);
    }
}
