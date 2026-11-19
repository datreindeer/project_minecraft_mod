package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.CarcassRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Dead eyes don't glow: a phantom carcass is drawn without its glowing eyes layer. */
@Mixin(EyesLayer.class)
public abstract class EyesLayerMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void mechanical$noGlowWhenDead(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, EntityRenderState state,
            float yRot, float xRot, CallbackInfo ci) {
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) ci.cancel();
    }
}
