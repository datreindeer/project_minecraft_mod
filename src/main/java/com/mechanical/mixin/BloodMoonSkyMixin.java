package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.client.BloodMoonClient;
import com.mechanical.client.SmokeClient;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;

/** Adds the blood moon's sky, fog, light and moon layers, and the smoke fog indoors, on top of the client level's own. */
@Mixin(ClientLevel.class)
public abstract class BloodMoonSkyMixin {
    @Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
    private void mechanical$bloodMoon(EnvironmentAttributeSystem.Builder builder, CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
        BloodMoonClient.addLayers(builder);
        SmokeClient.addLayers(builder);
        com.mechanical.client.SanityClient.addLayers(builder);
    }
}
