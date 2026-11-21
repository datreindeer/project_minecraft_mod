package com.mechanical.worldgen;

import com.mechanical.Mechanical;
import com.mechanical.people.ClimateZone;
import com.mechanical.people.Miners;
import com.mechanical.people.People;
import com.mechanical.people.TribeRole;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * The Miner (people.Miners) on a cave floor below y {@link Miners#TOP}: usually at a small camp (a lit campfire, a Straw
 * Bed and a chest of chests/miner_camp), one time in {@link #LONE_ODDS} wandering alone. Placement: scripts/gen_people.py.
 */
final class MinerCamp extends StoneAgeFeature {
    static final MapCodec<MinerCamp> CODEC = MapCodec.unit(MinerCamp::new);
    static final int LONE_ODDS = 4;

    @Override
    public MapCodec<MinerCamp> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = floor(level, origin);
        if (c == null || c.getY() > Miners.TOP) return false;
        Direction side = randomSide(random);
        BlockPos fire = null;
        if (random.nextInt(LONE_ODDS) != 0) {
            fire = c;
            set(level, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            bed(level, c.relative(side, 2).relative(side.getClockWise()), side.getClockWise());
            BlockPos chest = c.relative(side.getOpposite(), 2);
            if (standable(level, chest)) {
                set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, side));
                RandomizableContainer.setBlockEntityLootTable(level, random, chest,
                        ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/miner_camp")));
            }
        }
        BlockPos at = fire == null ? c : c.relative(side.getCounterClockWise(), 2);
        if (!standable(level, at)) at = c.relative(side);
        BlockPos home = fire;
        spawn(level, random, People.MINER.get(), standable(level, at) ? at : c,
                t -> t.setup(TribeRole.MINER, home, Miners.tribe(random), true, ClimateZone.TEMPERATE));
        return true;
    }

    /** A Straw Bed: foot at p, head toward {@code facing}; only where both halves fit. */
    private static void bed(WorldGenLevel level, BlockPos p, Direction facing) {
        if (!standable(level, p) || !standable(level, p.relative(facing))) return;
        var bed = Blocks.STRAW_BED.defaultBlockState().setValue(AbstractBedBlock.FACING, facing);
        set(level, p, bed.setValue(AbstractBedBlock.PART, BedPart.FOOT));
        set(level, p.relative(facing), bed.setValue(AbstractBedBlock.PART, BedPart.HEAD));
    }

    /** Dry air with room to stand over a sturdy floor. */
    private static boolean standable(WorldGenLevel level, BlockPos p) {
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    /** The cave floor nearest above or below origin (within 16): standable, out of the sky, its 3x3 round it floor too. */
    private static BlockPos floor(WorldGenLevel level, BlockPos origin) {
        for (int i = 0; i < 32; i++) {
            BlockPos p = origin.above(i % 2 == 0 ? i / 2 : -(i / 2 + 1));
            if (!standable(level, p) || level.canSeeSky(p)) continue;
            boolean room = true;
            for (Direction d : Direction.Plane.HORIZONTAL) room &= standable(level, p.relative(d));
            if (room) return p;
        }
        return null;
    }
}
