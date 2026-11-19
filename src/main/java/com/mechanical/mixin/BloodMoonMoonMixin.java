package com.mechanical.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mechanical.client.BloodMoonClient;

import net.minecraft.client.renderer.SkyRenderer;

/** Draws the moon blood red during a blood moon. */
@Mixin(SkyRenderer.class)
public abstract class BloodMoonMoonMixin {
    @ModifyArg(method = "renderMoon", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private Vector4f mechanical$redMoon(Vector4f colour) {
        return BloodMoonClient.moonTint(colour);
    }
}
