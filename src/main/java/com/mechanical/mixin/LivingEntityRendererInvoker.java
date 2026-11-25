package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** The transforms a living model is drawn with, to find where a dragged carcass's legs end up (CarcassRenderer's drag ropes). */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {
    @Invoker("setupRotations")
    void mechanical$setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale);

    @Invoker("scale")
    void mechanical$scale(LivingEntityRenderState state, PoseStack poseStack);
}
