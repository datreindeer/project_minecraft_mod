package com.mechanical.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.datafixers.util.Pair;

import static com.mechanical.worldgen.ModBiomes.*;
import com.mechanical.worldgen.ModBiomes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

/**
 * Puts the mod's biomes into the overworld preset by giving them a share of some vanilla climate slots: the slot's
 * humidity (or temperature) range is cut in two and one half goes to our biome. Only the biome map changes; terrain
 * comes from the same noises as before. Data: scripts/gen_biomes.py.
 * <ul>
 * <li>Forest, temperate, humidity 2 (-0.1..0.1): the wetter half (0..0.1) becomes Deciduous Forest.</li>
 * <li>Plains, cool and temperate, humidity 1 (-0.35..-0.1): the wetter part (-0.2..-0.1, next to the forests) becomes Hazel Meadow.</li>
 * <li>Plains, cool, humidity 0 (driest): becomes Pine Heath (cool dappled forest, its weird-side variant, stays).</li>
 * <li>Swamp (cool + temperate): the cool half, and the humid part (humidity above 0.1) of the temperate half, becomes
 * Willow Fen; Swamp keeps the drier temperate part.</li>
 * </ul>
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    @Shadow @Final private Climate.Parameter[] temperatures;
    @Shadow @Final private Climate.Parameter[] humidities;

    @Shadow
    private void addSurfaceBiome(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter temperature,
            Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness,
            float offset, ResourceKey<Biome> biome) {}

    // the split halves never match these conditions again, so the re-entrant calls just add the point
    @Inject(method = "addSurfaceBiome", at = @At("HEAD"), cancellable = true)
    private void mechanical$shareSlot(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter temperature,
            Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness,
            float offset, ResourceKey<Biome> biome, CallbackInfo ci) {
        if (ModBiomes.vanillaOnly) return;
        if (biome == Biomes.FOREST && temperature.equals(temperatures[2]) && humidity.equals(humidities[2])) {
            addSurfaceBiome(biomes, temperature, Climate.Parameter.span(-0.1F, 0.0F), continentalness, erosion, weirdness, offset, biome);
            addSurfaceBiome(biomes, temperature, Climate.Parameter.span(0.0F, 0.1F), continentalness, erosion, weirdness, offset, DECIDUOUS_FOREST);
        } else if (biome == Biomes.PLAINS && (temperature.equals(temperatures[1]) || temperature.equals(temperatures[2]))
                && humidity.equals(humidities[1])) {
            addSurfaceBiome(biomes, temperature, Climate.Parameter.span(-0.35F, -0.2F), continentalness, erosion, weirdness, offset, biome);
            addSurfaceBiome(biomes, temperature, Climate.Parameter.span(-0.2F, -0.1F), continentalness, erosion, weirdness, offset, HAZEL_MEADOW);
        } else if (biome == Biomes.PLAINS && temperature.equals(temperatures[1]) && humidity.equals(humidities[0])) {
            addSurfaceBiome(biomes, temperature, humidity, continentalness, erosion, weirdness, offset, PINE_HEATH);
        } else if (biome == Biomes.SWAMP && temperature.equals(Climate.Parameter.span(temperatures[1], temperatures[2]))) {
            addSurfaceBiome(biomes, temperatures[1], humidity, continentalness, erosion, weirdness, offset, WILLOW_FEN);
            addSurfaceBiome(biomes, temperatures[2], Climate.Parameter.span(-1.0F, 0.1F), continentalness, erosion, weirdness, offset, biome);
            addSurfaceBiome(biomes, temperatures[2], Climate.Parameter.span(0.1F, 1.0F), continentalness, erosion, weirdness, offset, WILLOW_FEN);
        } else {
            return;
        }
        ci.cancel();
    }
}
