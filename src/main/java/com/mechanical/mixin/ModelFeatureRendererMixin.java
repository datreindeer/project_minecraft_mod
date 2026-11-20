package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

/**
 * Carcasses of vanilla animals whose models have no pose mixin of their own (fox, wolf, cat, llama, camel, frog, bat ...):
 * right after the model is posed, the legs sag or hang by their part names ({@link CarcassRenderer#poseVanillaCarcass}).
 */
@Mixin(ModelFeatureRenderer.class)
public abstract class ModelFeatureRendererMixin {
    @Inject(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void mechanical$carcassPose(ModelFeatureRenderer.Submit<?> submit, CallbackInfo ci) {
        CarcassRenderer.poseVanillaCarcass(submit.model(), submit.state());
    }
}
