package com.mechanical.worldgen;

import com.mechanical.BronzeOres;
import com.mechanical.Mechanical;
import com.mechanical.RockBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Surface finds over ore veins: probes a few columns of the feature's own chunk (never outside it), digging down up to
 * {@link #DEPTH} blocks for copper or tin ore; a hit leaves 1-3 loose finds on the ground nearby (Malachite, sometimes
 * Native Copper, over copper; Cassiterite over tin). Runs in the vegetal step, after the ores are placed.
 */
public final class OreIndicators implements Feature {
    public static final MapCodec<OreIndicators> CODEC = MapCodec.unit(OreIndicators::new);
    static final int COLUMNS = 3, DEPTH = 64;

    @Override
    public MapCodec<OreIndicators> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        boolean placed = false;
        for (int i = 0; i < COLUMNS; i++) {
            int x = cx + random.nextInt(16), z = cz + random.nextInt(16);
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Block find = findBelow(level, random, x, top - 1, z);
            if (find == null) continue;
            for (int n = 1 + random.nextInt(3); n > 0; n--) {
                int px = Math.clamp(x + random.nextInt(5) - 2, cx, cx + 15), pz = Math.clamp(z + random.nextInt(5) - 2, cz, cz + 15);
                placed |= drop(level, random, new BlockPos(px, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz), pz), find);
            }
        }
        return placed;
    }

    /** The find for the first copper or tin ore in the column below, or null. */
    private static Block findBelow(WorldGenLevel level, RandomSource random, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        for (int bottom = Math.max(level.getMinY(), y - DEPTH); pos.getY() > bottom; pos.move(Direction.DOWN)) {
            BlockState s = level.getBlockState(pos);
            if (s.is(BlockTags.COPPER_ORES))
                return random.nextFloat() < 0.15F ? BronzeOres.LOOSE_NATIVE_COPPER.get() : BronzeOres.LOOSE_MALACHITE.get();
            if (s.is(Mechanical.TIN_ORE.get())) return BronzeOres.LOOSE_CASSITERITE.get();
        }
        return null;
    }

    private static boolean drop(WorldGenLevel level, RandomSource random, BlockPos pos, Block find) {
        BlockState here = level.getBlockState(pos);
        // air or a short plant; not water, and not half of a tall plant (the other half would float)
        if (!here.isAir() && !(here.canBeReplaced() && here.getFluidState().isEmpty() && !(here.getBlock() instanceof DoublePlantBlock))) return false;
        BlockState state = find.defaultBlockState().setValue(RockBlock.SIZE, random.nextInt(4) == 0 ? 2 : 1);
        if (!state.canSurvive(level, pos)) return false;
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }
}
