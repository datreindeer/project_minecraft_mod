package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.HaulingClient;

import net.minecraft.client.MouseHandler;

/** Dragging a carcass the view stays on it: re-aimed every frame after the mouse turned it, before the camera is placed ({@link HaulingClient#lockView}). */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "handleAccumulatedMovement", at = @At("RETURN"))
    private void mechanical$lockHaulingView(CallbackInfo ci) {
        HaulingClient.lockView();
    }
}
