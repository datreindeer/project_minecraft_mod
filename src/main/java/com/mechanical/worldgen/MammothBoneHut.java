package com.mechanical.worldgen;

import com.mechanical.BearRugBlock;
import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A dome of bone arches, the gaps stuffed with hides (wool) and packed snow, a fur floor and a pot of tusks inside. */
final class MammothBoneHut extends StoneAgeFeature {
    static final MapCodec<MammothBoneHut> CODEC = MapCodec.unit(MammothBoneHut::new);

    @Override
    public MapCodec<MammothBoneHut> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 3, 1)) return false;
        levelGround(level, c, 3, 5);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.8);
                    if (dist >= 3.5 || dist < 2.4) continue;
                    // bone arches run along the axes and diagonals; the rest is filler
                    boolean arch = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
                    set(level, c.offset(dx, dy, dz), arch ? Blocks.BONE_BLOCK.defaultBlockState() : filler(random));
                }
            }
        }
        Direction out = randomSide(random), p = out.getClockWise();
        for (int step = 2; step <= 3; step++) {
            set(level, c.relative(out, step), AIR);
            set(level, c.relative(out, step).above(), AIR);
        }
        set(level, c, Mechanical.BEAR_RUG.get().defaultBlockState().setValue(BearRugBlock.FACING, out));
        set(level, c.relative(p, 1), Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState());
        set(level, c.relative(p, -1), Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState());
        pot(level, random, c.relative(out, -2), "mammoth_bone_hut");
        if (random.nextBoolean()) pot(level, random, c.relative(out, -1).relative(p, -1), "mammoth_bone_hut"); // a pot holds one stack
        set(level, c.relative(out, -1).relative(p, 1), Mechanical.BONE_PILE.get().defaultBlockState());
        return true;
    }

    private static BlockState filler(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> Blocks.WOOL.pick(DyeColor.BROWN).defaultBlockState();
            case 1 -> Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState();
            default -> Blocks.SNOW_BLOCK.defaultBlockState();
        };
    }
}
