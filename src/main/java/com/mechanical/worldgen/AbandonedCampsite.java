package com.mechanical.worldgen;

import com.mechanical.DryingRackBlock;
import com.mechanical.FallenLogBlock;
import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A burnt-out campfire ringed with stones, a log to sit on, a drying rack, scraps of bone and a pot of leftovers. */
final class AbandonedCampsite extends StoneAgeFeature {
    static final MapCodec<AbandonedCampsite> CODEC = MapCodec.unit(AbandonedCampsite::new);

    @Override
    public MapCodec<AbandonedCampsite> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 3, 1)) return false;
        levelGround(level, c, 3, 3);
        Direction d = randomSide(random), p = d.getClockWise();
        set(level, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        for (Direction side : Direction.Plane.HORIZONTAL) if (random.nextInt(3) > 0) rock(level, random, c.relative(side), Mechanical.LOOSE_STONE.get());
        set(level, c.relative(d, 2).relative(p, -1), Mechanical.FALLEN_LOGS.get("oak").get().defaultBlockState().setValue(FallenLogBlock.AXIS, p.getAxis()));
        set(level, c.relative(d, 2), Mechanical.FALLEN_LOGS.get("oak").get().defaultBlockState().setValue(FallenLogBlock.AXIS, p.getAxis()));
        onGround(level, c.relative(p, 2).relative(d, -1), Mechanical.DRYING_RACK.get().defaultBlockState().setValue(DryingRackBlock.FACING, p.getOpposite()));
        onGround(level, c.relative(p, -2).relative(d, -1), Mechanical.BONE_PILE.get().defaultBlockState());
        pot(level, random, c.relative(d, -2).relative(p, 1), "abandoned_campsite");
        if (random.nextBoolean()) pot(level, random, c.relative(d, -2).relative(p, -1), "abandoned_campsite"); // a pot holds one stack
        return true;
    }
}
