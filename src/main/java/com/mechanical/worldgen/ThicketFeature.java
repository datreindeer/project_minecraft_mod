package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Calendar;
import com.mechanical.Flora;
import com.mechanical.ThicketBushBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A berry thicket (scripts/gen_flora.py): 4-11 bushes of one kind grown out from the spot in a ragged clump, mostly grown
 * (joined up) with young ones round the edge, and the bushes in the middle (two or more neighbours) often two blocks
 * tall (not the low mats). Berries at the stage the season wants.
 */
final class ThicketFeature extends StoneAgeFeature {
    static final List<ThicketFeature> ALL = List.of("blackthorn", "sea_buckthorn", "jujube", "rosehip", "crowberry", "bearberry", "barberry", "caper",
            "red_currant", "black_currant", "white_currant", "gooseberry", "cranberry").stream().map(ThicketFeature::new).toList();

    final String kind;
    private final MapCodec<ThicketFeature> codec;

    private ThicketFeature(String kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<ThicketFeature> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        ThicketBushBlock bush = Flora.THICKETS.get(kind).get();
        int season = Calendar.season(level.getLevel().getOverworldClockTime());
        origin = ground(level, origin.getX(), origin.getZ());
        boolean bare = !bush.evergreen() && season == 3 && ThicketBushBlock.wintry(level.getLevel(), origin);
        BlockState grown = bush.defaultBlockState().setValue(ThicketBushBlock.AGE, bush.target(season)).setValue(ThicketBushBlock.LEAFLESS, bare);
        List<BlockPos> placed = new ArrayList<>();
        if (!fits(level, bush, origin)) return false;
        placed.add(origin);
        int want = 4 + random.nextInt(8);
        for (int tries = 0; placed.size() < want && tries < want * 6; tries++) {
            BlockPos from = placed.get(random.nextInt(placed.size())).relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            BlockPos p = ground(level, from.getX(), from.getZ());
            if (Math.abs(p.getY() - origin.getY()) > 2 || placed.contains(p) || !fits(level, bush, p)) continue;
            placed.add(p);
        }
        // the first ones are the old middle of the clump; the last third may be young
        for (int i = 0; i < placed.size(); i++) {
            boolean young = i > placed.size() * 2 / 3 && random.nextInt(2) == 0;
            set(level, placed.get(i), young ? bush.defaultBlockState().setValue(ThicketBushBlock.YOUNG, true).setValue(ThicketBushBlock.LEAFLESS, bare) : grown);
        }
        for (BlockPos p : placed) set(level, p, bush.withSides(level, p, level.getBlockState(p)));
        List<BlockPos> tops = new ArrayList<>();
        for (BlockPos p : placed) {
            BlockState s = level.getBlockState(p);
            if (!bush.low() && !s.getValue(ThicketBushBlock.YOUNG) && ThicketBushBlock.sides(s) >= 2 && random.nextInt(3) > 0 && level.getBlockState(p.above()).isAir()) {
                set(level, p.above(), grown.setValue(ThicketBushBlock.HALF, DoubleBlockHalf.UPPER));
                tops.add(p.above());
            }
        }
        for (BlockPos p : tops) set(level, p, bush.withSides(level, p, level.getBlockState(p)));
        return true;
    }

    /** Above the ground at x,z, under any leaves (works in worldgen and for /place feature). */
    private static BlockPos ground(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static boolean fits(WorldGenLevel level, ThicketBushBlock bush, BlockPos p) {
        BlockState here = level.getBlockState(p);
        return (here.isAir() || here.canBeReplaced() && here.getFluidState().isEmpty() && !here.is(bush)
                && !(here.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock))
                && bush.defaultBlockState().canSurvive(level, p);
    }
}
