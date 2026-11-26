package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * First person, vanilla asks for Items.CROSSBOW to draw one hand while a crossbow charges or is held charged: the Steel
 * Crossbow (Smithing) is a CrossbowItem too, so it gets the same hands.
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
    @WrapOperation(method = {"evaluateWhichHandsToRender", "isChargedCrossbow"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z")) // ItemStack.is(T): erased
    private static boolean mechanical$anyCrossbow(ItemStack stack, Object item, Operation<Boolean> original) {
        return original.call(stack, item) || item == Items.CROSSBOW && stack.getItem() instanceof CrossbowItem;
    }
}
