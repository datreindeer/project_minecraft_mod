package com.mechanical.worldgen;

import static net.minecraft.world.level.biome.Climate.Parameter.span;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * The mod's overworld biomes (data: scripts/gen_biomes.py, scripts/gen_world_biomes.py; placed by
 * mixin/OverworldBiomeBuilderMixin). Their ground blocks are material rules written by gen_world_biomes.py.
 */
public final class ModBiomes {
    public static final ResourceKey<Biome> DECIDUOUS_FOREST = key("deciduous_forest");
    public static final ResourceKey<Biome> HAZEL_MEADOW = key("hazel_meadow");
    public static final ResourceKey<Biome> WILLOW_FEN = key("willow_fen");
    public static final ResourceKey<Biome> PINE_HEATH = key("pine_heath");
    public static final ResourceKey<Biome> CHALK_DOWNS = key("chalk_downs");
    public static final ResourceKey<Biome> LIMESTONE_CAVERNS = key("limestone_caverns");

    /** A cave biome's share of the climate space at cave depth (vanilla's underground biomes use depth 0.2..0.9). */
    public record CaveSlot(ResourceKey<Biome> biome, Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness, Climate.Parameter depth, float offset) {}

    /**
     * One row per cave biome. Keep clear of vanilla: Dripstone Caves continentalness 0.8..1, Lush Caves humidity 0.7..1,
     * Sulfur Caves erosion 0.45..1 with weirdness -1.1..-0.85, Deep Dark depth 1.1 (the bottom). Overlapping rows tie.
     */
    public static final List<CaveSlot> CAVES = List.of(
            // temperate, mid inland, mid depth
            new CaveSlot(LIMESTONE_CAVERNS, span(-0.15F, 0.2F), span(-0.1F, 0.1F), span(0.03F, 0.55F), span(-0.375F, 0.45F), span(-1.0F, 1.0F),
                    span(0.3F, 0.8F), 0.35F));

    /**
     * True while vanilla's built-in registries are bootstrapped (dev-time command validation, datagen): those only know
     * vanilla biomes, so the overworld preset must stay vanilla there. Real worlds resolve it against the datapack biomes.
     */
    public static boolean vanillaOnly;

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("mechanical", name));
    }

    private ModBiomes() {}
}
