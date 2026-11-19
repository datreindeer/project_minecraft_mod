package com.mechanical.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A lone flat standing slab, one block thick, with carved signs on its face and a trodden patch in front. */
final class RuneStone extends StoneAgeFeature {
    static final MapCodec<RuneStone> CODEC = MapCodec.unit(RuneStone::new);

    @Override
    public MapCodec<RuneStone> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 2, 1)) return false;
        boolean tuff = random.nextInt(3) == 0;
        BlockState body = (tuff ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
        BlockState carved = (tuff ? Blocks.CHISELED_TUFF : Blocks.CHISELED_STONE_BRICKS).defaultBlockState();
        Direction face = randomSide(random);
        int width = 1 + random.nextInt(2), height = 3 + random.nextInt(2);
        Decor.scuff(level, random, c.relative(face, 2), 1.6);
        for (int b = 0; b < width; b++) {
            foundation(level, Decor.at(c, face, 0, -1, b), body);
            for (int y = 0; y < height; y++) {
                // signs on the middle rows, moss creeping up from the ground
                boolean sign = y >= 1 && y < height - 1 && random.nextInt(3) > 0;
                BlockState s = sign ? carved : y == 0 && random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : body;
                set(level, Decor.at(c, face, 0, y, b), s);
            }
            // a rough, uneven top edge
            if (random.nextBoolean()) set(level, Decor.at(c, face, 0, height, b),
                    (tuff ? Blocks.TUFF_SLAB : Blocks.STONE_SLAB).defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        Decor.rocks(level, random, c, 2 + random.nextInt(3), 2, 3.5, Decor.looseFor(body));
        for (int i = 0; i < 3; i++) Decor.flower(level, random, surface(level, c.getX() + random.nextInt(5) - 2, c.getZ() + random.nextInt(5) - 2));
        return true;
    }
}
