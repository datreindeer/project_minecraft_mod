package com.mechanical.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mechanical.Smithing;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/** The crossbow loads Iron Bolts (#mechanical:bolts) besides arrows, from the inventory and a worn quiver alike. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @ModifyReturnValue(method = "getAllSupportedProjectiles()Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> mechanical$bolts(Predicate<ItemStack> original) {
        return original.or(s -> s.is(Smithing.BOLTS));
    }
}
