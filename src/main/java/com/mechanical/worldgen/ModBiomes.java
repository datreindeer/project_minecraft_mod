package com.mechanical.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** The mod's overworld biomes (data: scripts/gen_biomes.py; placed by mixin/OverworldBiomeBuilderMixin). */
public final class ModBiomes {
    public static final ResourceKey<Biome> DECIDUOUS_FOREST = key("deciduous_forest");
    public static final ResourceKey<Biome> HAZEL_MEADOW = key("hazel_meadow");
    public static final ResourceKey<Biome> WILLOW_FEN = key("willow_fen");
    public static final ResourceKey<Biome> PINE_HEATH = key("pine_heath");

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
