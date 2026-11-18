package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Skinned carcasses render with the flesh version of the animal's texture (textures/entity/skinned/...). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Redirect(method = "getRenderType", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"))
    private Identifier mechanical$skinnedTexture(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState state) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Identifier texture = ((LivingEntityRenderer) renderer).getTextureLocation(state);
        return CarcassRenderer.isSkinned(state) ? CarcassRenderer.skinnedTexture(texture) : texture;
    }
}
