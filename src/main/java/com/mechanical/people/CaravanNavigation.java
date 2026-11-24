package com.mechanical.people;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * A tribesperson's ground navigation. While its Trader Caravan is on the road (Caravans.ARRIVING, LEAVING) every step off
 * path ground costs more ({@link #OFF_ROAD}), so the column keeps to a road or trail where one runs its way, even round a
 * bend: trodden trails and worn grass (worldgen Trails, TrampledPaths), dirt paths, gravel, packed mud, coarse dirt, and
 * the lanes of any road a tribe built (RoadBuilding). Off the paths the extra cost rises and falls in wide soft patches
 * ({@link #ROUGH}), so across open land it wanders a little along the easier ground instead of drawing a ruler line.
 * Everyone else plans paths as usual.
 */
class CaravanNavigation extends GroundPathNavigation {
    /** The extra cost of a block off path ground, plus up to {@link #ROUGH} more by the lie of the land (vanilla: 1 per block walked). */
    static final float OFF_ROAD = 0.9F, ROUGH = 0.8F;
    /** Built road cells this far round the caravan count, and this many blocks to each side of a road's centre line. */
    static final int ROAD_SEARCH = 96, ROAD_HALF = 2;

    CaravanNavigation(Mob mob, Level level) {
        super(mob, level);
        setMaxVisitedNodesMultiplier(4.0F); // the cost field makes the search spread wider before it reaches the target
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        nodeEvaluator = new WalkNodeEvaluator() {
            private LongSet roadCells;

            @Override
            public void prepare(PathNavigationRegion region, Mob entity) {
                super.prepare(region, entity);
                roadCells = entity instanceof Tribesperson t && t.caravanPhase != Caravans.STOPPED && entity.level() instanceof ServerLevel level
                        ? roadColumns(level, entity.blockPosition()) : null;
            }

            @Override
            public void done() {
                super.done();
                roadCells = null;
            }

            @Override
            protected Node findAcceptedNode(int x, int y, int z, int jumpSize, double nodeHeight, Direction travelDirection, PathType blockPathTypeCurrent) {
                Node n = super.findAcceptedNode(x, y, z, jumpSize, nodeHeight, travelDirection, blockPathTypeCurrent);
                if (roadCells != null && n != null && n.costMalus >= 0 && !onRoad(n.x, n.y, n.z))
                    n.costMalus = Math.max(n.costMalus, OFF_ROAD + ROUGH * (float) RoadBuilding.smooth(n.x, n.z, 21, 9));
                return n;
            }

            private boolean onRoad(int x, int y, int z) {
                if (roadCells.contains(BlockPos.asLong(x, 0, z))) return true;
                BlockPos below = new BlockPos(x, y - 1, z);
                return Caravans.pathGround(currentContext.getBlockState(below)) || Caravans.pathGround(currentContext.getBlockState(below.above()));
            }
        };
        return new PathFinder(nodeEvaluator, maxVisitedNodes);
    }

    /** The (x, z) columns (y 0) of every built road lane within {@link #ROAD_SEARCH} of {@code at}. */
    static LongSet roadColumns(ServerLevel level, BlockPos at) {
        LongSet out = new LongOpenHashSet();
        for (RoadBuilding.Road r : RoadBuilding.get(level.getServer()).roads)
            for (int i = 0; i < r.built && i < r.cells.size(); i++) {
                BlockPos c = r.cell(i);
                if (Math.abs(c.getX() - at.getX()) > ROAD_SEARCH || Math.abs(c.getZ() - at.getZ()) > ROAD_SEARCH) continue;
                for (int dx = -ROAD_HALF; dx <= ROAD_HALF; dx++)
                    for (int dz = -ROAD_HALF; dz <= ROAD_HALF; dz++) out.add(BlockPos.asLong(c.getX() + dx, 0, c.getZ() + dz));
            }
        return out;
    }
}
