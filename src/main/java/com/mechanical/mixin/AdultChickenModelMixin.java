package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;

/** A chicken hung by the feet: the head (which stands up from the body) flops down towards the ground. */
@Mixin(AdultChickenModel.class)
public abstract class AdultChickenModelMixin {
    @Shadow @Final private ModelPart head;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/ChickenRenderState;)V", at = @At("TAIL"))
    private void mechanical$hangHead(ChickenRenderState state, CallbackInfo ci) {
        // the head box points up (-y); +pi/2 swings it to -z, which is world down when hung
        if (CarcassRenderer.isHung(state)) head.xRot = 1.4F;
    }
}
