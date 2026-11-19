package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.worldgen.ModBiomes;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;

/** Keeps the built-in (vanilla-only) bootstrap of the overworld preset free of our biomes; see {@link ModBiomes#vanillaOnly}. */
@Mixin(MultiNoiseBiomeSourceParameterLists.class)
public abstract class MultiNoiseBiomeSourceParameterListsMixin {
    @Inject(method = "bootstrap", at = @At("HEAD"))
    private static void mechanical$vanillaOnly(BootstrapContext<MultiNoiseBiomeSourceParameterList> context, CallbackInfo ci) {
        ModBiomes.vanillaOnly = true;
    }

    @Inject(method = "bootstrap", at = @At("RETURN"))
    private static void mechanical$done(BootstrapContext<MultiNoiseBiomeSourceParameterList> context, CallbackInfo ci) {
        ModBiomes.vanillaOnly = false;
    }
}
