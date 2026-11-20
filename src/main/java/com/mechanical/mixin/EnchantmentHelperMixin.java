package com.mechanical.mixin;

import java.util.List;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mechanical.equipment.Equipment;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * A worn cloak's enchantments work as if it were a chest piece ({@link Equipment#forEachCloakEnchantment}): it joins the
 * equipment every protection/thorns-style effect iterates over, and the items Mending can pick to repair.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "runIterationOnEquipment", at = @At("TAIL"))
    private static void mechanical$cloak(LivingEntity owner, EnchantmentHelper.EnchantmentInSlotVisitor method, CallbackInfo ci) {
        Equipment.forEachCloakEnchantment(owner, method);
    }

    @ModifyArg(method = "getRandomItemWith", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getRandomSafe(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
    private static List<EnchantedItemInUse> mechanical$cloakToo(List<EnchantedItemInUse> items, @Local(argsOnly = true) DataComponentType<?> componentType,
            @Local(argsOnly = true) LivingEntity source, @Local(argsOnly = true) Predicate<ItemStack> predicate) {
        Equipment.cloakWith(source, componentType, predicate).ifPresent(items::add);
        return items;
    }
}
