package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mechanical.Campfires;
import com.mechanical.DryingRackBlock;
import com.mechanical.Mechanical;
import com.mechanical.people.People;
import com.mechanical.people.TribeRole;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * An inhabited Stone Age camp: a burning campfire in a cleared spot, two or three hide tents facing it, a drying rack,
 * a log to sit on, and three to five people (an elder plus gatherers, woodcutters and, by water, fishers) who all call
 * this campfire home and share one group.
 */
final class TribeCamp extends StoneAgeFeature {
    static final MapCodec<TribeCamp> CODEC = MapCodec.unit(TribeCamp::new);
    private static final int FLAT_R = 5, CLEAR_R = 7, TENT_DIST = 5;

    @Override
    public MapCodec<TribeCamp> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = PeopleFeatures.ground(level, origin.getX(), origin.getZ());
        if (!groundFlat(level, c) || PeopleFeatures.nearVillage(level, c)) return false;
        boolean taiga = level.getBiome(c).is(BiomeTags.IS_TAIGA);
        clear(level, c);

        BlockState fire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        if (fire.hasProperty(Campfires.EMBERS)) fire = fire.setValue(Campfires.EMBERS, false);
        set(level, c, fire);

        int tents = 2 + random.nextInt(2);
        double base = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < tents; i++) {
            double a = base + i * Math.PI * 2 / tents + (random.nextDouble() - 0.5) * 0.5;
            BlockPos t = c.offset((int) Math.round(Math.cos(a) * TENT_DIST), 0, (int) Math.round(Math.sin(a) * TENT_DIST));
            tent(level, random, t, Direction.getApproximateNearest((double) (c.getX() - t.getX()), 0.0, (double) (c.getZ() - t.getZ())), taiga);
        }
        // between the first two tents: the drying rack; opposite: a log seat
        double between = base + Math.PI / tents;
        BlockPos rack = c.offset((int) Math.round(Math.cos(between) * 3), 0, (int) Math.round(Math.sin(between) * 3));
        onGround(level, rack, Mechanical.DRYING_RACK.get().defaultBlockState()
                .setValue(DryingRackBlock.FACING, randomSide(random)).setValue(DryingRackBlock.HIDE, 1 + random.nextInt(2)));
        BlockPos seat = c.offset((int) Math.round(-Math.cos(between) * 2), 0, (int) Math.round(-Math.sin(between) * 2));
        Direction.Axis axis = Math.abs(seat.getX() - c.getX()) >= Math.abs(seat.getZ() - c.getZ()) ? Direction.Axis.Z : Direction.Axis.X;
        set(level, seat, (taiga ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));

        // the people: an elder and a mix of the camp trades (fishers only by water)
        List<TribeRole> pool = new ArrayList<>(List.of(TribeRole.GATHERER, TribeRole.GATHERER, TribeRole.WOODCUTTER));
        if (waterNearby(level, c)) pool.addAll(List.of(TribeRole.FISHER, TribeRole.FISHER, TribeRole.FISHER));
        List<TribeRole> roles = new ArrayList<>(List.of(TribeRole.ELDER));
        int count = 3 + random.nextInt(3);
        while (roles.size() < count) roles.add(pool.get(random.nextInt(pool.size())));
        UUID group = new UUID(random.nextLong(), random.nextLong());
        for (int i = 0; i < roles.size(); i++) {
            double a = base + Math.PI / tents + i * Math.PI * 2 / roles.size() + 0.4;
            BlockPos p = c.offset((int) Math.round(Math.cos(a) * 3), 0, (int) Math.round(Math.sin(a) * 3));
            if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) p = c.offset(i % 2 == 0 ? 2 : -2, 0, i < 2 ? 2 : -2);
            TribeRole role = roles.get(i);
            spawn(level, random, People.TRIBESPERSON.get(), p, t -> t.setup(role, c, group, false));
        }
        return true;
    }

    /** Ground within FLAT_R varies at most one block, and is dry, natural soil. */
    private static boolean groundFlat(WorldGenLevel level, BlockPos c) {
        for (int dx = -FLAT_R; dx <= FLAT_R; dx++) {
            for (int dz = -FLAT_R; dz <= FLAT_R; dz++) {
                BlockPos g = PeopleFeatures.ground(level, c.getX() + dx, c.getZ() + dz);
                if (Math.abs(g.getY() - c.getY()) > 1 || !natural(level.getBlockState(g.below())) || !level.getFluidState(g).isEmpty()) return false;
            }
        }
        return true;
    }

    /** Clears trees (whole columns of logs and leaves) and plants from the camp, and levels it to the fire's height. */
    private static void clear(WorldGenLevel level, BlockPos c) {
        BlockState floor = level.getBlockState(c.below());
        for (int dx = -CLEAR_R; dx <= CLEAR_R; dx++) {
            for (int dz = -CLEAR_R; dz <= CLEAR_R; dz++) {
                if (dx * dx + dz * dz > CLEAR_R * CLEAR_R + 2) continue;
                BlockPos p = c.offset(dx, 0, dz);
                for (int y = 0; y < 28; y++) {
                    BlockPos q = p.above(y);
                    BlockState s = level.getBlockState(q);
                    if (s.isAir() || !s.getFluidState().isEmpty()) continue;
                    if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced() || y < 4) set(level, q, AIR);
                }
                if (dx * dx + dz * dz <= (FLAT_R + 1) * (FLAT_R + 1)) foundation(level, p.below(), floor);
            }
        }
    }

    private static boolean waterNearby(WorldGenLevel level, BlockPos c) {
        int water = 0;
        for (int dx = -14; dx <= 14; dx += 2) {
            for (int dz = -14; dz <= 14; dz += 2) {
                if (level.getFluidState(surface(level, c.getX() + dx, c.getZ() + dz)).is(FluidTags.WATER)) water++;
            }
        }
        return water >= 6;
    }

    /** A small A-frame hide tent on a 3x3 footprint, the door (open front) facing {@code front}. */
    private static void tent(WorldGenLevel level, RandomSource random, BlockPos t, Direction front, boolean taiga) {
        DyeColor hide = random.nextInt(3) == 0 ? DyeColor.WHITE : DyeColor.BROWN;
        BlockState wall = Blocks.WOOL.pick(hide).defaultBlockState();
        BlockState slope = Blocks.CARPET.pick(hide == DyeColor.WHITE ? DyeColor.BROWN : DyeColor.LIGHT_GRAY).defaultBlockState();
        BlockState pole = (taiga ? Blocks.SPRUCE_FENCE : Blocks.OAK_FENCE).defaultBlockState();
        Direction side = front.getClockWise();
        BlockState ground = level.getBlockState(t.below());
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos p = t.relative(front, a).relative(side, b);
                foundation(level, p.below(), ground.isAir() ? Blocks.DIRT.defaultBlockState() : ground);
                for (int y = 0; y < 4; y++) set(level, p.above(y), AIR);
                if (b != 0) {
                    set(level, p, wall);
                    set(level, p.above(), slope);
                } else {
                    if (a == -1) set(level, p, wall);
                    else if (a == 0) set(level, p, Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState()); // a hide on the floor
                    set(level, p.above(), wall);
                    if (a != 0) set(level, p.above(2), pole);
                }
            }
        }
    }
}
