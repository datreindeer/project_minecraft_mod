package com.mechanical.worldgen;

import java.util.UUID;

import com.mechanical.people.People;
import com.mechanical.people.TribeRole;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Two or three hunters roaming the woods together: one group, the first is the leader the others follow. No blocks. */
final class HuntingBand extends StoneAgeFeature {
    static final MapCodec<HuntingBand> CODEC = MapCodec.unit(HuntingBand::new);

    @Override
    public MapCodec<HuntingBand> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = PeopleFeatures.ground(level, origin.getX(), origin.getZ());
        if (!room(level, c) || !natural(level.getBlockState(c.below())) || PeopleFeatures.nearVillage(level, c)) return false;
        UUID group = new UUID(random.nextLong(), random.nextLong());
        com.mechanical.people.ClimateZone zone = com.mechanical.people.ClimateZone.of(level.getBiome(c)); // dressed for where they roam
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            BlockPos p = i == 0 ? c : PeopleFeatures.ground(level, c.getX() + random.nextInt(5) - 2, c.getZ() + random.nextInt(5) - 2);
            if (!room(level, p)) p = c;
            boolean leader = i == 0;
            spawn(level, random, People.TRIBESPERSON.get(), p, t -> t.setup(TribeRole.HUNTER, null, group, leader, zone));
        }
        return true;
    }

    /** Space to stand: no trunk, no water. */
    private static boolean room(WorldGenLevel level, BlockPos p) {
        return level.getFluidState(p).isEmpty() && level.getBlockState(p).canBeReplaced() && level.getBlockState(p.above()).canBeReplaced()
                && level.getFluidState(p.above()).isEmpty();
    }
}
