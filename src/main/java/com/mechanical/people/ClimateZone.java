package com.mechanical.people;

import com.mechanical.Mechanical;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The climate a tribe lives in, picked from the biome at its settlement's centre (or where a band was born). It decides
 * what the people wear (their skin set, client side) and what their villages, camps and settled camps are built of:
 * <ul>
 * <li>{@link #COLD}: snowy and frozen biomes (base temperature under 0.15, or tagged snowy). Fur hoods and coats; turf
 * houses on stone footings and mammoth-bone huts, spruce palisades.</li>
 * <li>{@link #TEMPERATE}: everything else. Leather and woven clothes; oak or spruce, bark roofs.</li>
 * <li>{@link #WARM}: desert, savanna and badlands (or hot and rainless). Light wraps and head cloths; mud-brick and
 * packed-mud houses with flat roofs, hide shades, acacia palisades.</li>
 * <li>{@link #JUNGLE}: jungle, bamboo jungle, swamps and mangroves. Loincloths, feathers and paint; stilt houses of
 * jungle wood under leaf roofs, bamboo palisades.</li>
 * </ul>
 */
public enum ClimateZone {
    COLD, TEMPERATE, WARM, JUNGLE;

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ClimateZone byId(int i) {
        ClimateZone[] all = values();
        return i < 0 || i >= all.length ? TEMPERATE : all[i];
    }

    public static ClimateZone of(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_JUNGLE) || biome.is(net.neoforged.neoforge.common.Tags.Biomes.IS_JUNGLE)
                || biome.is(net.neoforged.neoforge.common.Tags.Biomes.IS_SWAMP)) return JUNGLE;
        if (biome.is(BiomeTags.IS_SAVANNA) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(net.neoforged.neoforge.common.Tags.Biomes.IS_DESERT)) return WARM;
        float t = biome.value().getBaseTemperature();
        if (t < 0.15F || biome.is(net.neoforged.neoforge.common.Tags.Biomes.IS_SNOWY)) return COLD;
        if (t >= 1.5F && !biome.value().hasPrecipitation()) return WARM;
        return TEMPERATE;
    }

    /** The wood the tribe builds with; temperate tribes choose oak or spruce ({@code spruce}) by temperament. */
    public String wood(boolean spruce) {
        return switch (this) {
            case COLD -> "spruce";
            case TEMPERATE -> spruce ? "spruce" : "oak";
            case WARM -> "acacia";
            case JUNGLE -> "jungle";
        };
    }

    public BlockState log(boolean spruce) {
        return vanilla(wood(spruce) + "_log");
    }

    public BlockState stripped(boolean spruce) {
        return vanilla("stripped_" + wood(spruce) + "_log");
    }

    public BlockState fence(boolean spruce) {
        return vanilla(wood(spruce) + "_fence");
    }

    /** The posts of a palisade: logs, bamboo bundles in the jungle. */
    public BlockState post(boolean spruce) {
        return this == JUNGLE ? Blocks.BAMBOO_BLOCK.defaultBlockState() : log(spruce);
    }

    /** The sharpened top of each palisade post. */
    public BlockState stake(boolean spruce) {
        return switch (this) {
            case COLD -> People.SHARPENED_SPRUCE_LOG.get().defaultBlockState();
            case TEMPERATE -> (spruce ? People.SHARPENED_SPRUCE_LOG : People.SHARPENED_OAK_LOG).get().defaultBlockState();
            case WARM -> People.SHARPENED_ACACIA_LOG.get().defaultBlockState();
            case JUNGLE -> People.SHARPENED_BAMBOO_BLOCK.get().defaultBlockState();
        };
    }

    public BlockState barkRoof(boolean spruce) {
        return Mechanical.BARK_ROOFS.get(wood(spruce)).get().defaultBlockState();
    }

    public BlockState stump(boolean spruce) {
        return Mechanical.STUMPS.get(wood(spruce)).get().defaultBlockState();
    }

    /** Laid by the food store in place of the temperate hay bale: ice to keep meat, jars, a heap of melons. */
    public BlockState store() {
        return switch (this) {
            case COLD -> Blocks.PACKED_ICE.defaultBlockState();
            case TEMPERATE -> Blocks.HAY_BLOCK.defaultBlockState();
            case WARM -> Blocks.DECORATED_POT.defaultBlockState();
            case JUNGLE -> Blocks.MELON.defaultBlockState();
        };
    }

    private static BlockState vanilla(String name) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name));
        return b.defaultBlockState();
    }
}
