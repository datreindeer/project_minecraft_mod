package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Carcasses of four-legged animals (cow, pig, sheep, goat ...): legs sag towards the ground, or hang from a Hanging Rack. */
@Mixin(QuadrupedModel.class)
public abstract class QuadrupedModelMixin {
    @Shadow @Final protected ModelPart rightHindLeg;
    @Shadow @Final protected ModelPart leftHindLeg;
    @Shadow @Final protected ModelPart rightFrontLeg;
    @Shadow @Final protected ModelPart leftFrontLeg;
    @Shadow @Final protected ModelPart head;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)V", at = @At("TAIL"))
    private void mechanical$sagLegs(LivingEntityRenderState state, CallbackInfo ci) {
        // hung by the hind legs: the head already hangs in line with the body
        com.mechanical.client.LivestockPose.head(state, head); // eating at the trough or tired (D6)
        if (CarcassRenderer.isHung(state)) CarcassRenderer.hangLegs(new ModelPart[] {rightFrontLeg, leftFrontLeg}, new ModelPart[] {rightHindLeg, leftHindLeg});
        if (!CarcassRenderer.isCarcass(state)) return;
        for (ModelPart leg : new ModelPart[] {rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg}) leg.zRot = CarcassRenderer.LIMB_SAG;
    }
}
