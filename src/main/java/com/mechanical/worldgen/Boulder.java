package com.mechanical.worldgen;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * A big rounded boulder (sometimes two or three together), sunk into the ground with loose rocks scattered around.
 * Three kinds: glacial erratics of foreign stone on open land, mossy boulders in forests, sandstone in deserts.
 */
final class Boulder extends StoneAgeFeature {
    enum Kind { ERRATIC, MOSSY, SANDSTONE }

    static final Boulder ERRATIC = new Boulder(Kind.ERRATIC), MOSSY = new Boulder(Kind.MOSSY), SANDSTONE = new Boulder(Kind.SANDSTONE);
    // erratic stone sets: main block, then the odd fleck mixed in
    private static final Block[][] ERRATICS = {
            {Blocks.GRANITE, Blocks.STONE}, {Blocks.DIORITE, Blocks.CALCITE}, {Blocks.ANDESITE, Blocks.STONE},
            {Blocks.STONE, Blocks.COBBLESTONE}, {Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE}, {Blocks.TUFF, Blocks.ANDESITE},
            {Blocks.CALCITE, Blocks.DIORITE}, {Blocks.GRANITE, Blocks.DIORITE}};

    private final Kind kind;
    private final MapCodec<Boulder> codec;

    private Boulder(Kind kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<Boulder> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 2, 2)) return false;
        Block[] set = switch (kind) {
            case ERRATIC -> ERRATICS[random.nextInt(ERRATICS.length)];
            case MOSSY -> new Block[] {Blocks.MOSSY_COBBLESTONE, random.nextBoolean() ? Blocks.STONE : Blocks.ANDESITE};
            case SANDSTONE -> level.getBlockState(c.below()).is(Blocks.RED_SAND)
                    ? new Block[] {Blocks.RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE}
                    : new Block[] {Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE};
        };
        int pieces = random.nextInt(4) == 0 ? 2 + random.nextInt(2) : 1;
        double big = kind == Kind.ERRATIC ? 3.5 : 2.6;
        for (int i = 0; i < pieces; i++) {
            BlockPos at = i == 0 ? c : surface(level, c.getX() + random.nextInt(9) - 4, c.getZ() + random.nextInt(9) - 4);
            if (!level.getFluidState(at).isEmpty()) continue;
            double scale = i == 0 ? 1 : 0.6;
            double rx = (1.4 + random.nextDouble() * (big - 1.4)) * scale, rz = (1.4 + random.nextDouble() * (big - 1.4)) * scale;
            double ry = Math.max(1, Math.min(rx, rz) * (0.6 + random.nextDouble() * 0.4));
            // a third or more of it is buried, so it sits in the ground instead of on it
            BlockPos center = at.below(1 + (int) (ry * 0.35));
            List<BlockPos> placed = Decor.blob(level, random, center, rx, ry, rz,
                    r -> (r.nextInt(6) == 0 ? set[1] : set[0]).defaultBlockState());
            dress(level, random, Decor.tops(level, placed));
            Decor.rocks(level, random, at, 2 + random.nextInt(4), rx + 1, rx + 3, Decor.looseFor(set[0].defaultBlockState()));
        }
        if (kind == Kind.MOSSY) {
            for (int i = 0; i < 6; i++) {
                BlockPos p = surface(level, c.getX() + random.nextInt(9) - 4, c.getZ() + random.nextInt(9) - 4);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD))
                    set(level, p, (random.nextBoolean() ? Blocks.MOSS_CARPET : Blocks.FERN).defaultBlockState());
            }
        }
        return true;
    }

    /** Moss on forest boulders, a moss tuft now and then on erratics; desert stones stay bare. */
    private void dress(WorldGenLevel level, RandomSource random, List<BlockPos> tops) {
        for (BlockPos p : tops) {
            switch (kind) {
                case MOSSY -> {
                    if (random.nextInt(3) > 0) set(level, p, Blocks.MOSS_BLOCK.defaultBlockState());
                    if (random.nextInt(4) == 0) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
                }
                case ERRATIC -> {
                    if (random.nextInt(7) == 0) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
                }
                case SANDSTONE -> { }
            }
        }
    }
}
