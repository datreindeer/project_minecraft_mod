package com.mechanical.worldgen;

import com.mechanical.DryingRackBlock;
import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** On a bank right by the water: a cold campfire, fish drying racks, a pot of fishing gear and a short plank jetty. */
final class FishingCamp extends StoneAgeFeature {
    static final MapCodec<FishingCamp> CODEC = MapCodec.unit(FishingCamp::new);

    @Override
    public MapCodec<FishingCamp> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // a river or beach chunk is mostly water: look for a bank anywhere in the chunk, not just at the one random spot
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        for (int i = 0; i < 24; i++) {
            BlockPos c = i == 0 ? surface(level, origin.getX(), origin.getZ()) : surface(level, x0 + 2 + random.nextInt(12), z0 + 2 + random.nextInt(12));
            if (tryCamp(level, random, c)) return true;
        }
        return false;
    }

    private static boolean tryCamp(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 1)) return false;
        // open water 3-5 blocks away, its surface level with the bank or up to 2 below it
        Direction toWater = null;
        int shore = 0, waterY = 0;
        search:
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 3; k <= 5; k++) {
                BlockPos col = c.relative(d, k);
                for (int y = c.getY() - 1; y >= c.getY() - 3; y--) {
                    if (level.getFluidState(col.atY(y)).is(FluidTags.WATER) && level.getBlockState(col.atY(y + 1)).isAir()) {
                        toWater = d;
                        shore = k;
                        waterY = y;
                        break search;
                    }
                }
            }
        }
        if (toWater == null) return false;
        levelGround(level, c, 2, 3);
        Direction p = toWater.getClockWise();
        set(level, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        onGround(level, c.relative(p, 2), Mechanical.DRYING_RACK.get().defaultBlockState().setValue(DryingRackBlock.FACING, toWater));
        onGround(level, c.relative(p, -2), Mechanical.DRYING_RACK.get().defaultBlockState().setValue(DryingRackBlock.FACING, toWater));
        pot(level, random, c.relative(toWater, -2).relative(p, 1), "fishing_camp");
        if (random.nextBoolean()) onGround(level, c.relative(toWater, -2).relative(p, -1), Mechanical.BONE_PILE.get().defaultBlockState());
        // jetty: planks just above the water, held up by log piles driven to the bottom
        for (int k = shore; k < shore + 4; k++) {
            BlockPos deck = c.relative(toWater, k).atY(waterY + 1);
            if (!level.getFluidState(deck.below()).is(FluidTags.WATER) || !level.getBlockState(deck).isAir()) break;
            set(level, deck, Blocks.SPRUCE_PLANKS.defaultBlockState());
            if ((k - shore) % 2 == 1) {
                BlockPos pile = deck.below();
                for (int i = 0; i < 8 && !level.getBlockState(pile).isSolid(); i++, pile = pile.below()) set(level, pile, Blocks.SPRUCE_LOG.defaultBlockState());
            }
        }
        return true;
    }
}
