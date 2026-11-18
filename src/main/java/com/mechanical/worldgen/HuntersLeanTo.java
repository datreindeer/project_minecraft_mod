package com.mechanical.worldgen;

import com.mechanical.BearRugBlock;
import com.mechanical.DryingRackBlock;
import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Two spruce posts hold a ridge log; a hide roof (brown wool) slopes from it down to the ground at the back.
 * Under it a bear rug and a pot of hunting gear, a drying rack out front.
 */
final class HuntersLeanTo extends StoneAgeFeature {
    static final MapCodec<HuntersLeanTo> CODEC = MapCodec.unit(HuntersLeanTo::new);

    @Override
    public MapCodec<HuntersLeanTo> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 3, 1)) return false;
        levelGround(level, c, 3, 4);
        Direction front = randomSide(random), p = front.getClockWise();
        BlockState post = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState beam = post.setValue(RotatedPillarBlock.AXIS, p.getAxis());
        BlockState hide = Blocks.WOOL.pick(DyeColor.BROWN).defaultBlockState();
        for (int s = -2; s <= 2; s++) {
            BlockPos row = c.relative(p, s);
            if (Math.abs(s) == 2) {
                set(level, row.relative(front, 1), post);
                set(level, row.relative(front, 1).above(), post);
            }
            set(level, row.relative(front, 1).above(2), beam);
            set(level, row.above(2), hide);
            set(level, row.relative(front, -1).above(), hide);
            set(level, row.relative(front, -2), hide);
        }
        onGround(level, c.relative(p, -1), Mechanical.BEAR_RUG.get().defaultBlockState().setValue(BearRugBlock.FACING, p));
        pot(level, random, c.relative(p, 1).relative(front, -1), "hunters_lean_to");
        if (random.nextBoolean()) pot(level, random, c.relative(front, -1), "hunters_lean_to"); // a pot holds one stack
        onGround(level, c.relative(front, 3).relative(p, 2), Mechanical.DRYING_RACK.get().defaultBlockState().setValue(DryingRackBlock.FACING, front));
        if (random.nextBoolean()) onGround(level, c.relative(front, 3).relative(p, -1), Mechanical.BONE_PILE.get().defaultBlockState());
        return true;
    }
}
