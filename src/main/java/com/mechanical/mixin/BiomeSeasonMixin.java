package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.Seasons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

/**
 * The season shifts every biome's temperature (Seasons.offset). Added after vanilla's per-position cache, so cached
 * values never go stale. Rain or snow, snow cover, freezing water: vanilla reads them all from here.
 */
@Mixin(Biome.class)
public abstract class BiomeSeasonMixin {
    @Shadow
    public abstract float getBaseTemperature();

    @Inject(method = "getTemperature(Lnet/minecraft/core/BlockPos;I)F", at = @At("RETURN"), cancellable = true)
    private void mechanical$season(BlockPos pos, int seaLevel, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValueF() + Seasons.offset(getBaseTemperature()));
    }
}
