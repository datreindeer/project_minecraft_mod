package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;
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
 * A short cave dug into a hillside (or, on flat ground, a hollow mound of mossy rubble) with old bones, a pot of a past
 * victim's gear and a brown bear living in it.
 */
final class BearDen extends StoneAgeFeature {
    static final MapCodec<BearDen> CODEC = MapCodec.unit(BearDen::new);

    @Override
    public MapCodec<BearDen> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!level.getFluidState(c).isEmpty() || !natural(level.getBlockState(c.below()))) return false;
        Direction into = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos far = c.relative(d, 4);
            if (surface(level, far.getX(), far.getZ()).getY() >= c.getY() + 4
                    && surface(level, c.relative(d, 2).getX(), c.relative(d, 2).getZ()).getY() >= c.getY() + 2) {
                into = d;
                break;
            }
        }
        BlockPos den;
        if (into != null) {
            den = c.relative(into, 4);
            if (!dryAround(level, den)) return false;
            dig(level, random, c, into);
        } else {
            if (!flat(level, c, 3, 1)) return false;
            into = randomSide(random);
            den = c;
            mound(level, random, c, into);
        }
        Direction p = into.getClockWise();
        onGround(level, den.relative(p, 1).relative(into, 1), Mechanical.BONE_PILE.get().defaultBlockState());
        onGround(level, den.relative(p, -1).relative(into, -1), Mechanical.BONE_PILE.get().defaultBlockState());
        if (random.nextBoolean()) set(level, den.relative(p, -1).relative(into, 1),
                Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, p.getAxis()));
        pot(level, random, den.relative(p, 1).relative(into, -1).relative(into, random.nextInt(2) * 2), "bear_den");
        // shortcut: the bear only dozes at night (WildAnimal wakes it in daylight), so it is spawned awake
        spawn(level, random, Wildlife.BROWN_BEAR.get(), den, bear -> {});
        return true;
    }

    /** No water near the den (a cave flooding into a river looks bad). */
    private static boolean dryAround(WorldGenLevel level, BlockPos den) {
        for (BlockPos q : BlockPos.betweenClosed(den.offset(-3, -1, -3), den.offset(3, 3, 3)))
            if (!level.getFluidState(q).isEmpty()) return false;
        return true;
    }

    /** Tunnel from the slope's foot into the hill, widening into a 5x5 chamber; holes to the sky are patched with rubble. */
    private static void dig(WorldGenLevel level, RandomSource random, BlockPos c, Direction into) {
        Direction p = into.getClockWise();
        for (int step = 0; step <= 6; step++) {
            int w = step < 2 ? 1 : 2, h = step < 2 ? 2 : 3;
            for (int s = -w; s <= w; s++) {
                BlockPos col = c.relative(into, step).relative(p, s);
                set(level, col.below(), random.nextInt(3) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
                for (int y = 0; y < h; y++) set(level, col.above(y), AIR);
                if (step >= 2 && !level.getBlockState(col.above(h)).isSolid()) set(level, col.above(h), rubble(random));
            }
        }
    }

    /** Hollow half-dome of rubble, radius 3, with an opening facing out. */
    private static void mound(WorldGenLevel level, RandomSource random, BlockPos c, Direction out) {
        levelGround(level, c, 3, 5);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + dy * dy * 1.6);
                    if (dist >= 3.5) continue;
                    BlockPos q = c.offset(dx, dy, dz);
                    set(level, q, dist >= 2.4 ? rubble(random) : AIR);
                }
            }
        }
        for (int step = 2; step <= 3; step++) {
            set(level, c.relative(out, step), AIR);
            set(level, c.relative(out, step).above(), AIR);
        }
    }

    private static BlockState rubble(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }
}
