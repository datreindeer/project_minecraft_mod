package com.mechanical.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mechanical.Smithing;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/**
 * The crossbow loads only Iron Bolts (#mechanical:bolts): no arrows, no fireworks, from the inventory, a worn quiver and the
 * off hand alike. Mobs with crossbows are unaffected (vanilla hands them an arrow when they hold nothing it loads).
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @ModifyReturnValue(method = "getAllSupportedProjectiles()Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> mechanical$bolts(Predicate<ItemStack> original) {
        return s -> s.is(Smithing.BOLTS);
    }

    @ModifyReturnValue(method = "getSupportedHeldProjectiles()Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> mechanical$heldBolts(Predicate<ItemStack> original) {
        return s -> s.is(Smithing.BOLTS);
    }
}
