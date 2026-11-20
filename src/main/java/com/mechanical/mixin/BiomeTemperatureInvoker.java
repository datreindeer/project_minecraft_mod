package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

/** Lets {@link com.mechanical.Thaw} read a biome's temperature at a spot, season included (Biome.getTemperature is private). */
@Mixin(Biome.class)
public interface BiomeTemperatureInvoker {
    @Invoker("getTemperature")
    float mechanical$temperature(BlockPos pos, int seaLevel);
}
