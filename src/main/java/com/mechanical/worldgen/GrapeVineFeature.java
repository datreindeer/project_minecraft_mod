package com.mechanical.worldgen;

import com.mechanical.Calendar;
import com.mechanical.GrapeVineBlock;
import com.mechanical.Vineyard;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Wild grape vines (scripts/gen_flora.py): near the spot, one or two vines climb a tree trunk or a rock face from the
 * ground up 2-5 blocks, at the stage the season wants (bare in winter), as wild grapes climb the trees of warm woods.
 */
final class GrapeVineFeature extends StoneAgeFeature {
    static final MapCodec<GrapeVineFeature> CODEC = MapCodec.unit(GrapeVineFeature::new);

    @Override
    public MapCodec<GrapeVineFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        GrapeVineBlock vine = Vineyard.GRAPE_VINE.get();
        int season = Calendar.season(level.getLevel().getOverworldClockTime());
        BlockState base = vine.defaultBlockState().setValue(GrapeVineBlock.AGE, season == 3 ? 0 : new int[] {1, 2, 3}[season])
                .setValue(GrapeVineBlock.LEAFLESS, season == 3);
        int columns = 0;
        for (int tries = 0; tries < 32 && columns < 1 + random.nextInt(2); tries++) {
            int x = origin.getX() + random.nextInt(13) - 6, z = origin.getZ() + random.nextInt(13) - 6;
            BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(random)) {
                if (!level.getBlockState(ground).isAir() || !climbable(level, ground.relative(d))) continue;
                int height = 2 + random.nextInt(4);
                BlockPos p = ground;
                for (int i = 0; i < height && level.getBlockState(p).isAir() && climbable(level, p.relative(d)); i++, p = p.above()) {
                    set(level, p, base.setValue(VineBlock.getPropertyForFace(d), true));
                }
                columns++;
                break;
            }
        }
        return columns > 0;
    }

    /** A trunk or bare rock with a full face toward the vine. */
    private static boolean climbable(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return (s.is(BlockTags.LOGS) || s.is(BlockTags.BASE_STONE_OVERWORLD)) && s.isCollisionShapeFullBlock(level, p);
    }
}
