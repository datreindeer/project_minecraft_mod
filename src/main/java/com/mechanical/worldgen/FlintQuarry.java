package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A shallow pit dug through gravel into stone, littered with flint nodules, with a pot of half-made tools. */
final class FlintQuarry extends StoneAgeFeature {
    static final MapCodec<FlintQuarry> CODEC = MapCodec.unit(FlintQuarry::new);

    @Override
    public MapCodec<FlintQuarry> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 4, 2)) return false;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-5, -3, -5), c.offset(5, 0, 5)))
            if (!level.getFluidState(q).isEmpty()) return false; // a pit next to water would flood
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 4.6) continue;
                BlockPos top = surface(level, c.getX() + dx, c.getZ() + dz);
                if (d > 3.6) {
                    set(level, top.below(), Blocks.GRAVEL.defaultBlockState()); // spoil heaped around the rim
                    continue;
                }
                int floor = c.getY() - (d < 2 ? 2 : 1);
                for (int y = floor; y < Math.max(top.getY(), c.getY()) + 1; y++) set(level, top.atY(y), AIR);
                foundation(level, top.atY(floor - 1), pitFloor(random));
            }
        }
        for (int i = 0; i < 10; i++) {
            BlockPos at = surface(level, c.getX() + random.nextInt(7) - 3, c.getZ() + random.nextInt(7) - 3);
            rock(level, random, at, i < 7 ? Mechanical.LOOSE_FLINT.get() : Mechanical.LOOSE_STONE.get());
        }
        BlockPos potAt = surface(level, c.getX() + 1, c.getZ() + 1);
        if (level.getBlockState(potAt).isAir()) pot(level, random, potAt, "flint_quarry");
        return true;
    }

    private static BlockState pitFloor(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0, 1 -> Blocks.GRAVEL.defaultBlockState();
            case 2 -> Blocks.ANDESITE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }
}
