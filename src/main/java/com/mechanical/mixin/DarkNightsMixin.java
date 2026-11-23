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
 * Underground (CaveDark, faded by client.CaveDarkness) the ambient floor goes entirely, day or night, and a fixed Brightness
 * replaces the option: unlit stays black, block light (lava, glow lichen, torches, DynamicLights) lights its surroundings. Night vision is separate in the shader and still works.
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class DarkNightsMixin {
    private static final float DAY_SKY = 1.0F, NIGHT_SKY = 0.24F, CUT = 0.9F;
    /** The Brightness used underground, whatever the option says (0 Moody .. 1 Bright). */
    private static final float CAVE_GAMMA = 0.65F;

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
    private void mechanical$darkNights(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (!state.needsUpdate || level == null || !level.dimensionType().hasSkyLight()) return;
        float night = Mth.clamp((DAY_SKY - state.skyFactor) / (DAY_SKY - NIGHT_SKY), 0.0F, 1.0F);
        if (night > 0) {
            // a blood moon lights the night a little (tinted red by BloodMoonClient's sky light colour)
            float cut = CUT * night * (1 - 0.45F * com.mechanical.client.BloodMoonClient.strength());
            state.brightness = Mth.lerp(night, state.brightness, 0.0F);
            state.skyFactor *= 1 - cut;
            // shared constant: replace it, never mutate
            state.ambientColor = new Vector3f(state.ambientColor).mul(1 - cut);
        }
        float cave = com.mechanical.client.CaveDarkness.strength();
        if (cave > 0) {
            // the darkness comes from taking the ambient floor away (a trace stays: the shader's gamma curve divides by
            // the brightest channel). Unlit stays black under any gamma (it maps 0 to 0), so a fixed gamma replaces the
            // player's: torches, lava and glow lichen light their surroundings the same for everyone
            state.ambientColor = new Vector3f(state.ambientColor).mul(1 - 0.99F * cave);
            state.brightness = Mth.lerp(cave, state.brightness, CAVE_GAMMA);
        }
    }
}
