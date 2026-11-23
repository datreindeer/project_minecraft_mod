package com.mechanical.worldgen;

import com.mechanical.Thaw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Snow drifts (terrain-plan phase 7). On a windy day of snow in winter (one in three winter days, the wind mostly from the
 * west), snow blown over open ground piles up in the lee of whatever stands up out of it: boulders and stones, tree trunks,
 * stumps and fallen logs. Behind each the drift is deepest right at the foot (two layers for each block it stands up) and
 * thins out over a few blocks downwind, a layer more each windy day until it reaches that shape. Only where it is cold
 * enough to snow. The spring thaw (Thaw) melts the drifts like any snow, the deep ones last.
 */
final class SnowDrift {
    /** Snow layers per block an obstacle stands up, how far downwind a drift reaches at most, and the deepest it gets. */
    static final int PER_BLOCK = 2, LENGTH = 5, DEEPEST = 7;

    private SnowDrift() {}

    static void day(NatureClock.Day d) {
        if (!d.runs("drift") || d.season() != 3) return;
        RandomSource r = d.random(11);
        if (r.nextInt(3) != 0) return; // a windy day of snow
        BlockPos c = d.top(d.pos.getMiddleBlockX(), d.pos.getMiddleBlockZ());
        if (Thaw.temperature(d.level, d.level.getBiome(c).value(), c) >= Thaw.SNOW_BELOW) return;
        drift(d, wind(r));
    }

    /** Mostly from the west, so drifts lie east of things; now and then from the north or the south-west. */
    static Direction wind(RandomSource r) {
        int i = r.nextInt(6);
        return i < 3 ? Direction.EAST : i == 3 ? Direction.SOUTH : i == 4 ? Direction.NORTH : Direction.EAST.getClockWise();
    }

    /** One windy day: every obstacle in the chunk gets its drift a layer deeper downwind ({@code lee}: the way the wind blows). */
    static int drift(NatureClock.Day d, Direction lee) {
        ServerLevel level = d.level;
        int before = d.changes;
        int x0 = d.pos.getMinBlockX(), z0 = d.pos.getMinBlockZ();
        for (int i = 0; i < 256 && d.changes - before < 40; i++) {
            BlockPos top = d.top(x0 + (i & 15), z0 + (i >> 4));
            BlockState s = level.getBlockState(top);
            if (s.is(Blocks.SNOW)) s = level.getBlockState(top = top.below()); // a cap of snow on a stone
            if (!obstacle(s)) continue;
            BlockPos after = d.top(top.getX() + lee.getStepX(), top.getZ() + lee.getStepZ());
            BlockPos ground = groundOf(level, after);
            int high = top.getY() - ground.getY();
            if (high < 1) continue;
            int deepest = Math.min(DEEPEST, PER_BLOCK * Math.min(high, 3));
            for (int k = 1; k <= LENGTH; k++) {
                int want = deepest - (k - 1) * deepest / LENGTH - (k > 1 ? 1 : 0);
                if (want < 1) break;
                BlockPos col = top.relative(lee, k);
                BlockPos g = groundOf(level, d.top(col.getX(), col.getZ()));
                if (Math.abs(g.getY() - ground.getY()) > 1) break; // the ground drops or rises away: the drift ends
                pile(d, g.above(), want);
                if (want > 2) // the drift fans out a little behind it, lower at the sides
                    for (Direction side : new Direction[] {lee.getClockWise(), lee.getCounterClockWise()}) {
                        BlockPos sg = groundOf(level, d.top(col.getX() + side.getStepX(), col.getZ() + side.getStepZ()));
                        if (Math.abs(sg.getY() - g.getY()) <= 1 && !obstacle(level.getBlockState(sg))) pile(d, sg.above(), want - 2);
                    }
            }
        }
        return d.changes - before;
    }

    /** Stands up out of the snow and stops the wind: rock, a log or trunk, a stump. */
    static boolean obstacle(BlockState s) {
        return Rockfall.rock(s) || s.is(BlockTags.LOGS) || NatureRot.wood(s) || s.getBlock() instanceof com.mechanical.StumpBlock;
    }

    /** The ground under any snow lying at the top of a column. */
    private static BlockPos groundOf(ServerLevel level, BlockPos top) {
        BlockPos p = top;
        while (level.getBlockState(p).is(Blocks.SNOW) || level.getBlockState(p).isAir()) p = p.below();
        return p;
    }

    /** One more layer at p, up to want. */
    private static void pile(NatureClock.Day d, BlockPos p, int want) {
        BlockState s = d.level.getBlockState(p);
        if (!d.can(p)) return;
        if (s.is(Blocks.SNOW)) {
            int layers = s.getValue(SnowLayerBlock.LAYERS);
            if (layers < want) d.set(p, s.setValue(SnowLayerBlock.LAYERS, layers + 1));
        } else if ((s.isAir() || s.canBeReplaced() && s.getFluidState().isEmpty()) && Blocks.SNOW.defaultBlockState().canSurvive(d.level, p)) { // grass and flowers are snowed under
            d.set(p, Blocks.SNOW.defaultBlockState());
        }
    }
}
