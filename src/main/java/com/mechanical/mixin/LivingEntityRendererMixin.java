package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mechanical.client.CarcassRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Skinned carcasses render with the flesh version of the animal's texture (textures/entity/skinned/...), aged ones with the
 * dried version (textures/entity/skinned_aged/...) or, lacking one, a dull brown tint.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Redirect(method = "getRenderType", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"))
    private Identifier mechanical$skinnedTexture(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState state) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Identifier texture = ((LivingEntityRenderer) renderer).getTextureLocation(state);
        return CarcassRenderer.carcassTexture(state, texture);
    }

    @ModifyArg(method = "submit", index = 6, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int mechanical$agedTint(Model<?> model, Object state, PoseStack poseStack, RenderType renderType, int light, int overlay, int tint, UvMapping uv, int outline) {
        if (!(state instanceof LivingEntityRenderState living) || !CarcassRenderer.isAged(living) && !CarcassRenderer.isTurning(living)) return tint;
        Identifier texture = ((LivingEntityRenderer) (Object) this).getTextureLocation(living);
        return ARGB.multiply(tint, CarcassRenderer.carcassTint(living, texture));
    }
}
