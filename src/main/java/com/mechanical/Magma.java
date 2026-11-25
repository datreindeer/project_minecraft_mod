package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * What the Magma Block is good for. It warms you against cold ({@link Temperature#target}, never past comfortable),
 * keeps snow and ice off the ground round it (Thaw, mixin.BiomeSeasonMixin), and lies under a Baking Stone, Clay Kiln or
 * Smelting Furnace as a heat that never runs out but is weaker than any fire there: {@link #HEAT}° (a kiln's wood fire is
 * 650°, coal 800°), no bellows, a Baking Stone bakes at half speed. Crushed on a Stone Anvil it gives Crushed Magma, which
 * lights a High Furnace's Fireboxes without fail and goes into Fire Clay and Silica Bricks (scripts/gen_magma.py).
 */
public final class Magma {
    /** Degrees a Magma Block holds a kiln or Smelting Furnace at: bricks and pottery, no hot melts. */
    public static final int HEAT = 600;
    /** Body warmth beside it (a campfire gives 10) and how far it reaches. */
    public static final float WARMTH = 8;
    public static final int RANGE = 5;
    /** Snow and ice don't stay within this many blocks to the side (and up to this far above the magma). */
    public static final int SNOW_RADIUS = 2;

    public static final DeferredItem<Item> CRUSHED_MAGMA = ITEMS.registerSimpleItem("crushed_magma");

    public static void init() {}

    /** Is a Magma Block right under {@code pos}? */
    public static boolean under(Level level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK);
    }

    /** The warmth of the nearest Magma Block to a body standing in {@code pos} (0: none in reach). They don't add up. */
    public static float warmth(Level level, BlockPos pos) {
        double best = RANGE * RANGE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RANGE, -2, -RANGE), pos.offset(RANGE, 3, RANGE))) {
            double d = p.distSqr(pos);
            if (d < best && level.hasChunkAt(p) && level.getBlockState(p).is(Blocks.MAGMA_BLOCK)) best = d;
        }
        return WARMTH * (float) (1 - Math.sqrt(best) / RANGE);
    }

    /** Is snow or ice at {@code pos} in a Magma Block's warmth? Never loads a chunk. */
    public static boolean meltsSnow(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SNOW_RADIUS, -SNOW_RADIUS, -SNOW_RADIUS), pos.offset(SNOW_RADIUS, 0, SNOW_RADIUS)))
            if (level.hasChunkAt(p) && level.getBlockState(p).is(Blocks.MAGMA_BLOCK)) return true;
        return false;
    }

    private Magma() {}
}
