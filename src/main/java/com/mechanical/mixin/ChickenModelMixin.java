package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;

/** Chicken carcasses: legs and wings sag towards the ground. */
@Mixin(ChickenModel.class)
public abstract class ChickenModelMixin {
    @Shadow @Final private ModelPart rightLeg;
    @Shadow @Final private ModelPart leftLeg;
    @Shadow @Final private ModelPart rightWing;
    @Shadow @Final private ModelPart leftWing;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/ChickenRenderState;)V", at = @At("TAIL"))
    private void mechanical$sagLimbs(ChickenRenderState state, CallbackInfo ci) {
        if (!CarcassRenderer.isCarcass(state)) return;
        for (ModelPart limb : new ModelPart[] {rightLeg, leftLeg, rightWing, leftWing}) limb.zRot = CarcassRenderer.LIMB_SAG;
    }
}
