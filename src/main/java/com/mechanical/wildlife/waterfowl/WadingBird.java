package com.mechanical.wildlife.waterfowl;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * A long-legged flock bird (heron, crane) that wades instead of swimming: it stands and walks in shallow water (one block
 * deep over a firm bottom), comes down there or on the shore rather than out on open water, and takes off again if it
 * finds itself out of its depth.
 */
public abstract class WadingBird extends FlockBird {
    /** Deeper than this (fluid height over its feet) and it is out of its depth. */
    private static final double MAX_DEPTH = 1.1;

    protected WadingBird(EntityType<? extends WadingBird> type, Level level, Kind kind) {
        super(type, level, kind);
        setPathfindingMalus(PathType.WATER, 0.0F); // walks into the shallows like onto land
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // no floating up in the water (it stands on the bottom); strolls may lead into the shallows
        goalSelector.removeAllGoals(g -> g instanceof FloatGoal || g instanceof WaterAvoidingRandomStrollGoal);
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.6, 160));
    }

    /** A player in a Plume Headdress can come this close before wading birds notice him (unless they already panic). */
    static final double PLUMED_NOTICE = 4;

    @Override
    protected Vec3 threat() {
        Vec3 threat = super.threat();
        if (threat == null || com.mechanical.AnimalFear.isPanicking(this)) return threat;
        net.minecraft.world.entity.player.Player p = level().getNearestPlayer(this, 32);
        return p != null && WaterfowlFauna.wearsPlumes(p) && distanceTo(p) > PLUMED_NOTICE ? null : threat;
    }

    /** Wading, not floating: stands on the bottom, and flies off when the water gets too deep. */
    @Override
    protected void swim() {
        if (getFluidHeight(FluidTags.WATER) > MAX_DEPTH) takeOff(false, null);
    }

    /** Is the column at x, z one block of water over a solid bottom? Returns the standing spot (on the bottom), else null. */
    protected Vec3 shallowAt(int x, int z) {
        BlockPos col = BlockPos.containing(x, getY(), z);
        if (!level().hasChunkAt(col)) return null;
        BlockPos top = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);
        BlockState water = level().getBlockState(top.below());
        if (!water.getFluidState().is(FluidTags.WATER) || !level().getBlockState(top.below(2)).isSolid()) return null;
        return new Vec3(x + 0.5, top.getY() - 1, z + 0.5);
    }

    /** Shallow water first, then dry ground (or tree tops for perching kinds), never deep water. */
    @Override
    protected Vec3 findLandingSpot(Vec3 around, int radius) {
        for (int i = 0; i < 12; i++) {
            Vec3 spot = shallowAt(Mth.floor(around.x) + random.nextInt(radius * 2 + 1) - radius, Mth.floor(around.z) + random.nextInt(radius * 2 + 1) - radius);
            if (spot != null) return spot.add(0, 1, 0); // aims at the surface; it settles onto the bottom
        }
        for (int i = 0; i < 12; i++) {
            int x = Mth.floor(around.x) + random.nextInt(radius * 2 + 1) - radius;
            int z = Mth.floor(around.z) + random.nextInt(radius * 2 + 1) - radius;
            BlockPos col = BlockPos.containing(x, getY(), z);
            if (!level().hasChunkAt(col)) continue;
            BlockPos top = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);
            BlockState below = level().getBlockState(top.below());
            if (below.getFluidState().isEmpty() && canLandOn(below, false)) return new Vec3(x + 0.5, top.getY(), z + 0.5);
        }
        return null;
    }
}
