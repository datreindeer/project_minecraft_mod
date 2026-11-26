package com.mechanical.mixin;

import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mechanical.Smithing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Crossbows load only bolts (#mechanical:bolts; the Steel Crossbow alone takes Steel Bolts, Smithing#loads): no arrows, no
 * fireworks, from the inventory, a worn quiver and the off hand alike. Mobs with crossbows are unaffected (vanilla hands
 * them an arrow when they hold nothing it loads). The Steel Crossbow and a crossbow's parts stretch the charge time and
 * speed the bolt (Smithing#chargeFactor / #powerFactor; 1 for a stack never built at the Tool Station, so pillagers' too).
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @ModifyReturnValue(method = "getAllSupportedProjectiles()Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> mechanical$bolts(Predicate<ItemStack> original) {
        Item self = (Item) (Object) this;
        return s -> Smithing.loads(self, s);
    }

    @ModifyReturnValue(method = "getSupportedHeldProjectiles()Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> mechanical$heldBolts(Predicate<ItemStack> original) {
        Item self = (Item) (Object) this;
        return s -> Smithing.loads(self, s);
    }

    /** Static, so the item model's pull, the first-person hold and the mob AI all see the same charge time. */
    @ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
    private static int mechanical$charge(int original, ItemStack crossbow, LivingEntity user) {
        float factor = Smithing.chargeFactor(crossbow);
        return factor == 1 ? original : Math.max(1, Math.round(original * factor));
    }

    @WrapOperation(method = "performShooting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;shoot(Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/world/entity/LivingEntity;)V"))
    private void mechanical$power(CrossbowItem self, ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> projectiles,
                                  float power, float uncertainty, boolean crit, @Nullable LivingEntity target, Operation<Void> original) {
        original.call(self, level, shooter, hand, weapon, projectiles, power * Smithing.powerFactor(weapon), uncertainty, crit, target);
    }
}
