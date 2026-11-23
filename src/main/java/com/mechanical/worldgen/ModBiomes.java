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
    public static final ResourceKey<Biome> SALT_CAVES = key("salt_caves");
    public static final ResourceKey<Biome> OCHRE_CAVES = key("ochre_caves");
    public static final ResourceKey<Biome> SOAPSTONE_GROTTOS = key("soapstone_grottos");
    public static final ResourceKey<Biome> GLOWWORM_CAVES = key("glowworm_caves");
    public static final ResourceKey<Biome> ICE_CAVES = key("ice_caves");
    public static final ResourceKey<Biome> PEAT_HOLLOWS = key("peat_hollows");
    public static final ResourceKey<Biome> BONE_CAVES = key("bone_caves");
    public static final ResourceKey<Biome> GALENA_SEAMS = key("galena_seams");
    public static final ResourceKey<Biome> MAGMA_VENTS = key("magma_vents");
    public static final ResourceKey<Biome> CRYSTAL_GEODES = key("crystal_geodes");
    public static final ResourceKey<Biome> FLOODED_CAVES = key("flooded_caves");
    public static final ResourceKey<Biome> MAMMOTH_STEPPE = key("mammoth_steppe");
    /** Burnt forest beyond 2500 blocks (ScorchedBarrens); cut from cool Forest, which it stays closer in (MultiNoiseBiomeSourceMixin). */
    public static final ResourceKey<Biome> SCORCHED_BARRENS = key("scorched_barrens");
    public static final ResourceKey<Biome> SCORCHED_BARRENS_SOURCE = Biomes.FOREST;

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
    /** The warm part of the temperate band, next to the warm band: no snowy mountains here (the buffer between cold and warm). */
    private static final Climate.Parameter WARM_EDGE = span(0.05F, 0.2F);

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
            new SurfaceShare(Biomes.DESERT, key("gypsum_flats"), null, span(-1.0F, -0.35F), null, span(0.05F, 1.0F), null),
            // the frozen band's driest snowy plains (humidity band 0): cold, dry grassland (docs/biomes.md); Ice Spikes keep their slot
            new SurfaceShare(Biomes.SNOWY_PLAINS, MAMMOTH_STEPPE, T0, span(-1.0F, -0.35F), null, null, null),
            // the warm end of cool forest, in the middle of its humidity band, so forest lies round it on every climate side
            // (plains start at humidity -0.1). Two bands of the large-scale noises cross in blobs; ScorchedBarrens.stays turns
            // back every part with anything but living forest within 24 blocks. ~0.2 % of samples beyond 2600 stay barrens,
            // Mushroom Fields ~0.05 % (ElkwightWorldTest logs both). Only beyond 2500 blocks
            new SurfaceShare(SCORCHED_BARRENS_SOURCE, SCORCHED_BARRENS, span(-0.21F, -0.15F), span(-0.035F, 0.035F), null, null, null),
            // no snow next to warm land: vanilla gives temperate mountains snowy slopes and peaks right up to the warm band,
            // where savanna and jungle begin. In the warm part of the temperate band they are snowless instead
            new SurfaceShare(Biomes.SNOWY_SLOPES, Biomes.MEADOW, WARM_EDGE, null, null, null, null),
            new SurfaceShare(Biomes.GROVE, Biomes.TAIGA, WARM_EDGE, null, null, null, null),
            new SurfaceShare(Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, WARM_EDGE, null, null, null, null),
            new SurfaceShare(Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS, WARM_EDGE, null, null, null, null));

    /** A cave biome's share of the climate space at cave depth (vanilla's underground biomes use depth 0.2..0.9). */
    public record CaveSlot(ResourceKey<Biome> biome, Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness, Climate.Parameter depth, float offset) {}

    /**
     * One row per cave biome (docs/caves.md; slot table in docs/world-materials-plan.md "Fas 5+8"). Keep clear of vanilla:
     * Dripstone Caves continentalness 0.8..1, Lush Caves humidity 0.7..1, Sulfur Caves erosion 0.45..1 with weirdness
     * -1.1..-0.85, Deep Dark depth 1.1 with erosion -1..-0.375. Overlapping rows tie.
     * <p>Biome choice is the nearest point, and the surface biomes sit at depth 0 and 1, so a row wins where its distance
     * (offset included) is below min(depth, 1 - depth). The offset is each row's rarity knob: it sets how far the row
     * reaches past its own box and how thick its depth band is. Measured in a real world, depth is about
     * 0.5 + (30 - y) / 128 on land, with wide spread (less under the sea, more under hills); measured peaks: 0.3..0.45 at
     * y ~20..45, 0.5..0.75 at y ~-20..10, 0.8..0.92 and the bottom at y ~-40;
     * around depth 1 (y ~-30) the surface biomes always win, and below 1.06 (y < -40) the bottom rows share with the Deep Dark.
     */
    private static final Climate.Parameter ALL = span(-1.0F, 1.0F), LAND = span(-0.11F, 0.45F), INLAND = span(0.03F, 0.45F),
            NOT_LUSH = span(-1.0F, 0.35F), NOT_DEEP_DARK = span(-0.3F, 1.0F);
    private static final Climate.Parameter SHALLOW = span(0.3F, 0.45F), MID = span(0.5F, 0.75F), DEEP = span(0.8F, 0.92F), BOTTOM = span(1.06F, 1.5F);
    /** Every row but the coast's: equal offsets let the boxes alone split the space; 0.1 still beats the surface's depth-1 points down to depth ~0.9. */
    static final float OFFSET = 0.1F;

    public static final List<CaveSlot> CAVES = List.of(
            // shallow: under cold, cool-wet, temperate-humid, dry temperate and hot dry land
            new CaveSlot(ICE_CAVES, span(-1.0F, -0.5F), span(-1.0F, 0.3F), LAND, ALL, ALL, SHALLOW, OFFSET + 0.02F),
            new CaveSlot(PEAT_HOLLOWS, span(-0.45F, -0.15F), span(0.1F, 0.35F), LAND, ALL, ALL, SHALLOW, OFFSET),
            new CaveSlot(GLOWWORM_CAVES, span(-0.1F, 0.5F), span(0.1F, 0.35F), INLAND, ALL, ALL, SHALLOW, OFFSET),
            new CaveSlot(BONE_CAVES, span(-0.4F, 0.15F), span(-0.6F, 0.0F), INLAND, span(-0.2F, 0.6F), ALL, SHALLOW, OFFSET + 0.02F),
            new CaveSlot(OCHRE_CAVES, span(0.3F, 1.0F), span(-1.0F, -0.1F), INLAND, ALL, ALL, SHALLOW, OFFSET),
            // under coasts and the sea: no other row is near in the sea's climate, so a small box and a high offset keep it
            // to a band (points far from the box in any one dimension go to the surface biome)
            new CaveSlot(FLOODED_CAVES, span(-0.45F, 0.55F), span(-0.35F, 0.3F), span(-0.4F, -0.11F), span(-0.375F, 0.45F), span(-0.3F, 0.3F),
                    span(0.35F, 0.55F), 0.3F),
            // mid depth
            new CaveSlot(LIMESTONE_CAVERNS, span(-0.1F, 0.3F), span(-0.2F, 0.2F), INLAND, ALL, ALL, MID, OFFSET + 0.03F),
            new CaveSlot(SALT_CAVES, span(0.35F, 1.0F), span(-1.0F, -0.3F), LAND, ALL, ALL, MID, OFFSET),
            new CaveSlot(SOAPSTONE_GROTTOS, span(-1.0F, -0.3F), span(-1.0F, 0.3F), span(0.0F, 0.45F), span(-1.0F, 0.0F), ALL, MID, OFFSET),
            // deep, above the surface biomes' depth-1 points; Magma Vents and Crystal Geodes also at the bottom beside the Deep Dark
            new CaveSlot(GALENA_SEAMS, span(-0.45F, 0.1F), span(-0.35F, 0.3F), span(-0.11F, 0.45F), span(-0.3F, 0.45F), span(-0.45F, 0.45F), DEEP, OFFSET),
            new CaveSlot(MAGMA_VENTS, span(0.35F, 1.0F), span(-0.6F, 0.3F), span(-0.11F, 0.45F), NOT_DEEP_DARK, ALL, DEEP, OFFSET),
            new CaveSlot(MAGMA_VENTS, span(0.35F, 1.0F), span(-0.6F, 0.3F), span(-0.11F, 0.45F), NOT_DEEP_DARK, ALL, BOTTOM, OFFSET),
            new CaveSlot(CRYSTAL_GEODES, span(-0.2F, 0.35F), NOT_LUSH, span(0.03F, 0.45F), NOT_DEEP_DARK, span(0.6F, 1.0F), DEEP, OFFSET + 0.02F),
            new CaveSlot(CRYSTAL_GEODES, span(-0.2F, 0.35F), NOT_LUSH, span(0.03F, 0.45F), NOT_DEEP_DARK, span(0.6F, 1.0F), BOTTOM, OFFSET + 0.02F));

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
