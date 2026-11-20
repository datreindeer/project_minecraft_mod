package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.equipment.Quiver;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

/** Arrows shot from a worn quiver come out of the quiver ({@link Quiver#afterUseAmmo}). */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
    @Inject(method = "useAmmo", at = @At("RETURN"))
    private static void mechanical$fromQuiver(ItemStack weapon, ItemStack projectile, LivingEntity holder, boolean forceInfinite,
            CallbackInfoReturnable<ItemStack> cir) {
        Quiver.afterUseAmmo(projectile);
    }
}
