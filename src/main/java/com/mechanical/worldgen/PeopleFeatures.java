package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Worldgen for Stone Age people: inhabited camps, roaming hunting bands and the Miner under ground. Placement data: scripts/gen_people.py. */
public final class PeopleFeatures {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, Mechanical.MODID);

    static {
        FEATURE_TYPES.register("tribe_camp", () -> TribeCamp.CODEC);
        FEATURE_TYPES.register("hunting_band", () -> HuntingBand.CODEC);
        FEATURE_TYPES.register("miner_camp", () -> MinerCamp.CODEC);
    }

    /** True if a village or tribe village (structure start reference) covers this chunk or one next to it: people don't camp in villages. */
    static boolean nearVillage(WorldGenLevel level, BlockPos pos) {
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ChunkPos c = ChunkPos.containing(pos);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (var structure : level.getChunk(c.x() + dx, c.z() + dz).getAllReferences().keySet()) {
                    var holder = structures.wrapAsHolder(structure);
                    if (holder.is(StructureTags.VILLAGE) || holder.is(TribeVillageStructure.ID)) return true;
                }
            }
        }
        return false;
    }

    /** The real ground at x,z: under leaves, trunks, grass and snow (the worldgen heightmap stops on treetops). */
    static BlockPos ground(WorldGenLevel level, int x, int z) {
        BlockPos p = StoneAgeFeature.surface(level, x, z);
        for (int i = 0; i < 40; i++) {
            BlockState below = level.getBlockState(p.below());
            boolean clearable = below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS) || below.canBeReplaced() && below.getFluidState().isEmpty();
            if (!clearable) break;
            p = p.below();
        }
        return p;
    }

    private PeopleFeatures() {}
}
