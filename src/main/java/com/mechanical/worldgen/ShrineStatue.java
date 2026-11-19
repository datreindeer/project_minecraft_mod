package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * An old stone figure on a raised plinth in the middle of a small worn plaza of flagstones, gravel and trodden dirt.
 * Worn steps lead up to it; offerings (a pot, candles, bones) lie at its feet and wild flowers grow at the edge.
 * Sometimes the head has fallen off and lies in the grass.
 */
final class ShrineStatue extends StoneAgeFeature {
    static final MapCodec<ShrineStatue> CODEC = MapCodec.unit(ShrineStatue::new);

    @Override
    public MapCodec<ShrineStatue> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 4, 1)) return false;
        levelGround(level, c, 2, 6);
        Decor.scuff(level, random, c, 4.5);
        // flagstones in the middle
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 6 || random.nextInt(6) == 0) continue;
                set(level, c.offset(dx, -1, dz), switch (random.nextInt(4)) {
                    case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                    case 1 -> Blocks.ANDESITE.defaultBlockState();
                    default -> Blocks.COBBLESTONE.defaultBlockState();
                });
            }
        }
        Direction face = randomSide(random);
        BlockState stone = (random.nextBoolean() ? Blocks.STONE : Blocks.ANDESITE).defaultBlockState();
        // plinth: 3x3, one block high
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 1; b++) set(level, Decor.at(c, face, a, 0, b), Decor.weathered(random, Blocks.COBBLESTONE.defaultBlockState()));
        // worn steps up the front, some of them broken down to slabs
        for (int b = -1; b <= 1; b++) {
            BlockPos step = Decor.at(c, face, 2, 0, b);
            set(level, step, random.nextInt(3) == 0
                    ? Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                    : (random.nextInt(3) == 0 ? Blocks.MOSSY_COBBLESTONE_STAIRS : Blocks.COBBLESTONE_STAIRS).defaultBlockState()
                            .setValue(StairBlock.FACING, face.getOpposite()));
        }
        // the figure: robed body, shoulders, arms hanging, head
        int robe = 2 + random.nextInt(2);
        for (int y = 1; y <= robe; y++) set(level, c.above(y), Decor.weathered(random, stone));
        int sh = robe + 1;
        for (int b = -1; b <= 1; b++) set(level, Decor.at(c, face, 0, sh, b), Decor.weathered(random, stone));
        BlockState arm = (stone.is(Blocks.ANDESITE) ? Blocks.ANDESITE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState();
        set(level, Decor.at(c, face, 0, sh - 1, -1), arm);
        set(level, Decor.at(c, face, 0, sh - 1, 1), arm);
        BlockState head = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        if (random.nextInt(4) == 0) {
            // fallen: the head lies in the grass beside the plinth
            BlockPos fallen = surface(level, Decor.at(c, face, 0, 0, 3).getX(), Decor.at(c, face, 0, 0, 3).getZ());
            if (level.getFluidState(fallen).isEmpty()) set(level, fallen, head);
        } else {
            set(level, c.above(sh + 1), head);
        }
        // offerings at its feet on the plinth, and on the plaza before it
        pot(level, random, Decor.at(c, face, 1, 1, random.nextBoolean() ? -1 : 1), "shrine_statue");
        for (int b : new int[] {-1, 1}) {
            BlockPos at = Decor.at(c, face, 1, 1, b);
            if (level.getBlockState(at).isAir() && random.nextBoolean())
                set(level, at, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + random.nextInt(3)));
        }
        if (random.nextBoolean()) onGround(level, Decor.at(c, face, 3, 0, random.nextInt(3) - 1), Mechanical.BONE_PILE.get().defaultBlockState());
        // wild flowers and loose stones round the edge
        for (int i = 0; i < 10; i++) {
            double ang = random.nextDouble() * Math.PI * 2, d = 3.5 + random.nextDouble() * 2;
            Decor.flower(level, random, surface(level, c.getX() + (int) Math.round(Math.cos(ang) * d), c.getZ() + (int) Math.round(Math.sin(ang) * d)));
        }
        Decor.rocks(level, random, c, 3, 4, 6, Mechanical.LOOSE_STONE.get());
        return true;
    }
}
