package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EquineRenderState;

/** A tired horse hangs its head (D6, LivestockPose). */
@Mixin(AbstractEquineModel.class)
public abstract class AbstractEquineModelMixin {
    @Shadow @Final protected ModelPart headParts;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/EquineRenderState;)V", at = @At("TAIL"))
    private void mechanical$tiredHead(EquineRenderState state, CallbackInfo ci) {
        com.mechanical.client.LivestockPose.equineHead(state, headParts);
    }
}
