package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.CarcassHauling;
import com.mechanical.equipment.Equipment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * No sprinting with a carcass on your shoulders, one dragged behind you or a loaded travois (only the client's sprint
 * check reads this). Accessory slots ({@link Equipment}): they drop on death after the inventory, and a worn cloak wears
 * down with the armour.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "isMobilityRestricted", at = @At("HEAD"), cancellable = true)
    private void mechanical$hauling(CallbackInfoReturnable<Boolean> cir) {
        if (CarcassHauling.blocksSprint((Player) (Object) this)) cir.setReturnValue(true);
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void mechanical$dropAccessories(ServerLevel level, CallbackInfo ci) {
        Equipment.dropOnDeath((Player) (Object) this, level);
    }

    @Inject(method = "hurtArmor", at = @At("TAIL"))
    private void mechanical$hurtCloak(DamageSource source, float damage, CallbackInfo ci) {
        Equipment.hurtCloak((Player) (Object) this, source, damage);
    }
}
