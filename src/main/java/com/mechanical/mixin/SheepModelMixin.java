package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.renderer.entity.state.SheepRenderState;

/** Sheep set their head after QuadrupedModel (for vanilla grazing): the livestock pose again on top (D6). */
@Mixin(SheepModel.class)
public abstract class SheepModelMixin extends QuadrupedModel<SheepRenderState> {
    private SheepModelMixin() {
        super(null);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/SheepRenderState;)V", at = @At("TAIL"))
    private void mechanical$livestockPose(SheepRenderState state, CallbackInfo ci) {
        com.mechanical.client.LivestockPose.head(state, head);
    }
}
