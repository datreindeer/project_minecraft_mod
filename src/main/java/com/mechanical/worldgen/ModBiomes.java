package com.mechanical.worldgen;

import static net.minecraft.world.level.biome.Climate.Parameter.span;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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

    /**
     * A surface biome's share of one vanilla biome's climate slots (phases 4 and 7): every slot of {@code source} that
     * overlaps all the given ranges (null = any) is cut, the overlap goes to {@code biome}, the rest stays with the source
     * (and can be cut again by a later row). Axes: temperature, humidity, continentalness, erosion, weirdness.
     */
    public record SurfaceShare(ResourceKey<Biome> source, ResourceKey<Biome> biome, Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness) {
        public Climate.Parameter[] ranges() {
            return new Climate.Parameter[] {temperature, humidity, continentalness, erosion, weirdness};
        }
    }

    // vanilla's temperature bands (OverworldBiomeBuilder.temperatures): 0 frozen .. 4 hot
    private static final Climate.Parameter T0 = span(-1.0F, -0.45F), T1 = span(-0.45F, -0.15F), T2 = span(-0.15F, 0.2F), T3 = span(0.2F, 0.55F);

    /** Surface biomes of the world-materials plan (phases 4 and 7); docs/world-materials-plan.md "Fas 4+7" has the table. */
    public static final List<SurfaceShare> SURFACE = List.of(
            // the wetter half of cool humid taiga, on flat land (erosion above 0.05): next to Willow Fen
            new SurfaceShare(Biomes.TAIGA, key("peat_bog"), T1, span(0.2F, 0.3F), null, span(0.05F, 1.0F), null),
            // cold taiga (frozen band, humid), on hilly land (erosion below 0.05)
            new SurfaceShare(Biomes.TAIGA, key("glacial_moraine"), T0, null, null, span(-1.0F, 0.05F), null),
            // cool windswept hills, negative weirdness half
            new SurfaceShare(Biomes.WINDSWEPT_HILLS, key("slate_highlands"), T1, null, null, null, span(-1.0F, 0.0F)),
            // temperate windswept forest, the drier humid band, positive weirdness half: small
            new SurfaceShare(Biomes.WINDSWEPT_FOREST, key("soapstone_valley"), T2, span(0.1F, 0.3F), null, null, span(0.0F, 1.0F)),
            // temperate meadow, humidity -0.1..0.3 (the drier temperate meadow and all cool meadow stay)
            new SurfaceShare(Biomes.MEADOW, key("limestone_karst"), T2, span(-0.1F, 0.3F), null, null, null),
            // warm savanna plateau, the wetter of its two humidity bands
            new SurfaceShare(Biomes.SAVANNA_PLATEAU, key("marble_gorge"), T3, span(-0.35F, -0.1F), null, null, null),
            // badlands, humidity band 1 (the driest and middle bands stay Badlands; Eroded and Wooded untouched)
            new SurfaceShare(Biomes.BADLANDS, key("ochre_badlands"), null, span(-0.35F, -0.1F), null, null, null),
            // stony shore in the humid bands
            new SurfaceShare(Biomes.STONY_SHORE, key("basalt_coast"), null, span(0.1F, 1.0F), null, null, null),
            // cool beaches, the drier part
            new SurfaceShare(Biomes.BEACH, key("shingle_beach"), T1, span(-1.0F, 0.1F), null, null, null),
            // temperate swamp (the part Willow Fen left) near the coast: near-inland continentalness
            new SurfaceShare(Biomes.SWAMP, key("reed_delta"), T2, null, span(-0.11F, 0.03F), null, null),
            // the driest desert band on flat land
            new SurfaceShare(Biomes.DESERT, key("gypsum_flats"), null, span(-1.0F, -0.35F), null, span(0.05F, 1.0F), null));

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
