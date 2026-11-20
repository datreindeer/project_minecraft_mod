package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mechanical.equipment.Quiver;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Arrows lying on the ground go into a worn quiver first when picked up ({@link Quiver#pickUp}). */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean mechanical$quiverFirst(Inventory inventory, ItemStack stack, Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return Quiver.pickUp(player, stack, s -> original.call(inventory, s));
    }
}
