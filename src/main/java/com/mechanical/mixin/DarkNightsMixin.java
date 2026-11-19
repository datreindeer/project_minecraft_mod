package com.mechanical.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.Mth;

/**
 * Darker nights: at night the Brightness option is ignored and sky light and ambient light are cut to a tenth, so
 * unlit places are close to black. Block light (torches, fires) is untouched. Storms count as partly night.
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class DarkNightsMixin {
    private static final float DAY_SKY = 1.0F, NIGHT_SKY = 0.24F, CUT = 0.9F;

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
    private void mechanical$darkNights(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (!state.needsUpdate || level == null || !level.dimensionType().hasSkyLight()) return;
        float night = Mth.clamp((DAY_SKY - state.skyFactor) / (DAY_SKY - NIGHT_SKY), 0.0F, 1.0F);
        if (night <= 0) return;
        // a blood moon lights the night a little (tinted red by BloodMoonClient's sky light colour)
        float cut = CUT * night * (1 - 0.45F * com.mechanical.client.BloodMoonClient.strength());
        state.brightness = Mth.lerp(night, state.brightness, 0.0F);
        state.skyFactor *= 1 - cut;
        // shared constant: replace it, never mutate
        state.ambientColor = new Vector3f(state.ambientColor).mul(1 - cut);
    }
}
