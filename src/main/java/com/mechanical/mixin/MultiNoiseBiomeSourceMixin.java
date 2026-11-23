package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.worldgen.ModBiomes;
import com.mechanical.worldgen.ScorchedBarrens;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

/**
 * Scorched Barrens only beyond {@link ScorchedBarrens#MIN_DISTANCE} from 0,0: closer in, every resolver (chunk biomes,
 * structures, /locate) gives its source biome ({@link ModBiomes#SCORCHED_BARRENS_SOURCE}) instead, so the map stays seamless.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @Unique private volatile Holder<Biome> mechanical$barrens, mechanical$source;
    @Unique private volatile boolean mechanical$looked;

    @Inject(method = "createResolver", at = @At("RETURN"), cancellable = true)
    private void mechanical$resolver(Climate.Sampler sampler, CallbackInfoReturnable<BiomeResolver> cir) {
        cir.setReturnValue(mechanical$near(cir.getReturnValue()));
    }

    @Inject(method = "createResolverForChunk", at = @At("RETURN"), cancellable = true)
    private void mechanical$chunkResolver(Climate.Sampler sampler, int minQuartX, int minQuartY, int minQuartZ, int quartSizeX, int quartSizeY,
            int quartSizeZ, CallbackInfoReturnable<BiomeResolver> cir) {
        cir.setReturnValue(mechanical$near(cir.getReturnValue()));
    }

    @Unique
    private BiomeResolver mechanical$near(BiomeResolver resolver) {
        if (!mechanical$looked) {
            for (Holder<Biome> h : ((BiomeSource) (Object) this).possibleBiomes()) {
                if (h.is(ModBiomes.SCORCHED_BARRENS)) mechanical$barrens = h;
                if (h.is(ModBiomes.SCORCHED_BARRENS_SOURCE)) mechanical$source = h;
            }
            mechanical$looked = true;
        }
        Holder<Biome> barrens = mechanical$barrens, source = mechanical$source;
        if (barrens == null || source == null) return resolver; // not the overworld preset
        return (x, y, z) -> {
            Holder<Biome> h = resolver.getNoiseBiome(x, y, z);
            return h == barrens && !ScorchedBarrens.biomeAllowed(x, z) ? source : h;
        };
    }
}
