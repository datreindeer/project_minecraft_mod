package com.mechanical.worldgen;

import com.mechanical.EarthBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Bog Oak: 1-3 black oak trunks, each 3-6 logs long, lying flat and buried in the ground (peat, mud, dirt, clay) of bogs and
 * cold wet woods. Only blocks of natural ground are replaced, so a trunk never sticks out into air or water. Placement
 * (y 55-62, biomes #mechanical:has_peat): scripts/gen_earth.py.
 */
final class BogOakLogs extends StoneAgeFeature {
    static final MapCodec<BogOakLogs> CODEC = MapCodec.unit(BogOakLogs::new);

    @Override
    public MapCodec<BogOakLogs> codec() {
        return CODEC;
    }

    private static boolean buried(BlockState s) {
        return natural(s) || s.is(EarthBlocks.PEAT.get()) || s.is(Blocks.CLAY);
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        boolean any = false;
        for (int t = 1 + random.nextInt(3); t > 0; t--) {
            Direction along = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
            BlockPos start = origin.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            BlockState log = EarthBlocks.BOG_OAK_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
            for (int i = 3 + random.nextInt(4), n = 0; n < i; n++) {
                BlockPos at = start.relative(along, n);
                if (!buried(level.getBlockState(at))) continue;
                set(level, at, log);
                any = true;
            }
        }
        return any;
    }
}
