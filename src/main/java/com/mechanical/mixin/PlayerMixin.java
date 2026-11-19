package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.CarcassHauling;

import net.minecraft.world.entity.player.Player;

/** No sprinting with a carcass on your shoulders, one dragged behind you or a loaded travois (only the client's sprint check reads this). */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "isMobilityRestricted", at = @At("HEAD"), cancellable = true)
    private void mechanical$hauling(CallbackInfoReturnable<Boolean> cir) {
        if (CarcassHauling.blocksSprint((Player) (Object) this)) cir.setReturnValue(true);
    }
}
