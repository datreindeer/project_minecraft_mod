package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mechanical.CampAge;
import com.mechanical.EarthBlocks;
import com.mechanical.Thaw;
import com.mechanical.TrampledPathBlock;
import com.mechanical.clan.Clans;
import com.mechanical.people.RoadBuilding;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Smooth ground (docs/terrain-plan.md phase 2): every natural 1-block step gets a half step. Runs once per overworld chunk
 * last in top_layer_modification (after trails and the snow) and on old chunks through {@link Retrogen}.
 * <ul>
 * <li>A column exactly 1 lower than a neighbour (4 sides, or only a corner) gets, on top, a bottom slab of the higher
 * neighbour's ground (Grass Slab, Sand Slab, Stone Slab ... see {@link #variant}). Steps of 2 or more stay cliffs. The grass (or podzol) under a slab
 * becomes dirt, so each half step shows one grass fringe (the slab's), not two.</li>
 * <li>Where the higher ground is rock and the edge runs straight (the columns on both sides along the edge are at the same
 * height), a stair of that rock faces the step instead, with inner and outer corner shapes where two edges meet. The shape
 * comes from the heights alone, so it matches across chunk borders.</li>
 * <li>Where snow lies all year (Thaw.coldAllYear: it would not melt even at midsummer) or the higher ground is snow, snow
 * layers instead: 5 on a straight step (half a block of footing, so mobs walk up), 3 under a corner, 6 in a nook. Seasonal
 * snow gets the slab (a Grass Slab snowy), which outlasts the thaw.</li>
 * <li>Never under water, on or beside a footpath, inside a structure's box, under a tree or any block that isn't a plant
 * (plants, tall ones too, move aside or go); the ground scan looks through plants, logs, leaves and earlier half steps.</li>
 * </ul>
 * Each chunk writes only its own columns and reads 2 beyond.
 */
final class SmoothGround extends StoneAgeFeature {
    static final MapCodec<SmoothGround> CODEC = MapCodec.unit(SmoothGround::new);
    private static final int NONE = Integer.MIN_VALUE, R = 2, SCAN = 48;
    /** Snow layers on a straight step, under an outer corner, in an inner corner. 5 layers stand 4/8 high: half a block of footing. */
    static final int SNOW_STEP = 5, SNOW_CORNER = 3, SNOW_NOOK = 6;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final Heightmap.Types[] SURFACES = {Heightmap.Types.WORLD_SURFACE, Heightmap.Types.WORLD_SURFACE_WG};
    /** Ground block -> its slab / stairs (or AIR: none), found by name once. */
    private static final Map<Block, Block> SLABS = new ConcurrentHashMap<>(), STAIRS = new ConcurrentHashMap<>();
    /** Dev only: the environment variable MECHANICAL_NOSMOOTH turns the pass off, for before-pictures of the same seed. */
    private static final boolean OFF = System.getenv("MECHANICAL_NOSMOOTH") != null;

    @Override
    public MapCodec<SmoothGround> codec() {
        return SmoothGround.CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (OFF) return false;
        ChunkPos c = ChunkPos.containing(origin);
        return smooth(level, c.getMinBlockX(), c.getMinBlockZ(), 16) > 0;
    }

    /**
     * One column's ground: the top natural block's y (NONE if none or wet), the block, whether snow lies on it and never melts
     * there (Thaw.coldAllYear), whether it is a path.
     */
    private record Column(int y, BlockState ground, boolean snow, boolean cold, boolean path) {}

    /**
     * Smooths the size x size square at x0,z0 (reading {@link #R} beyond it). Returns how many half steps were laid.
     */
    static int smooth(WorldGenLevel level, int x0, int z0, int size) {
        int w = size + 2 * R;
        Column[] cols = new Column[w * w];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < cols.length; i++) cols[i] = column(level, x0 - R + i % w, z0 - R + i / w, p);
        List<BoundingBox> boxes = structureBoxes(level, x0, z0, size);
        if (boxes == null) return 0;
        int laid = 0;
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                BlockState found = halfStep(cols, w, dx + R, dz + R);
                if (found == null) continue;
                BlockState state = found;
                Column col = cols[dx + R + (dz + R) * w];
                int x = x0 + dx, z = z0 + dz, y = col.y + 1;
                if (boxes.stream().anyMatch(b -> b.isInside(x, y, z))) continue;
                BlockPos at = new BlockPos(x, y, z);
                if (!free(level, at)) continue;
                if (state.is(Blocks.SNOW) && !state.canSurvive(level, at)) continue;
                movePlant(level, at);
                BlockState ground = col.ground;
                // under snow layers the grass stays (snowy); under a slab it turns to dirt, else its fringe shows twice above the step below
                if (ground.hasProperty(SnowyBlock.SNOWY) && state.is(Blocks.SNOW)) set(level, at.below(), ground.setValue(SnowyBlock.SNOWY, true));
                else if (ground.getBlock() instanceof SpreadingSnowyBlock || ground.is(Blocks.PODZOL)) set(level, at.below(), Blocks.DIRT.defaultBlockState());
                if (col.snow && state.hasProperty(SnowyBlock.SNOWY)) state = state.setValue(SnowyBlock.SNOWY, true);
                set(level, at, state);
                laid++;
            }
        }
        return laid;
    }

    /** Spots a displaced plant may move to, nearest first: within 2 blocks round it, from 1 below to 2 above. */
    private static final List<BlockPos> NEARBY = BlockPos.betweenClosedStream(-2, -1, -2, 2, 2, 2).map(BlockPos::immutable)
            .filter(o -> o.getX() != 0 || o.getZ() != 0)
            .sorted(java.util.Comparator.comparingInt(o -> o.getX() * o.getX() + o.getZ() * o.getZ() + Math.abs(o.getY() - 1))).toList();

    /**
     * A small plant where the half step goes moves to the nearest free full ground top within 2 blocks (it can't sit on a
     * slab), so slopes keep their tufts; with no room, or when tall, it goes (a plant left standing would leave a hole in the slope).
     */
    private static void movePlant(WorldGenLevel level, BlockPos at) {
        BlockState plant = level.getBlockState(at);
        if (plant.isAir() || plant.is(Blocks.SNOW)) return;
        if (plant.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) { // a tall plant goes, both halves
            set(level, at.above(), Blocks.AIR.defaultBlockState());
            return;
        }
        for (BlockPos o : NEARBY) {
            BlockPos to = at.offset(o);
            if (level.getBlockState(to).isAir() && level.getFluidState(to).isEmpty() && plant.canSurvive(level, to)) {
                set(level, to, plant);
                return;
            }
        }
    }

    /** The half step for column (cx, cz) of the grid, or null. */
    private static BlockState halfStep(Column[] cols, int w, int cx, int cz) {
        Column c = cols[cx + cz * w];
        if (c.y == NONE) return null;
        int h = c.y;
        Direction up1 = null, up2 = null;
        int ups = 0;
        for (Direction d : SIDES) {
            Column n = at(cols, w, cx + d.getStepX(), cz + d.getStepZ());
            if (n.path) return null; // beside a footpath: leave it (trails make their own steps)
            if (n.y == h + 1) {
                ups++;
                if (up1 == null) up1 = d;
                else if (up2 == null) up2 = d;
            }
        }
        Column high;
        Direction corner1 = null, corner2 = null;
        if (ups > 0) {
            high = at(cols, w, cx + up1.getStepX(), cz + up1.getStepZ());
        } else {
            high = null;
            for (int i = 0; i < 4 && high == null; i++) {
                Direction a = SIDES[i], b = SIDES[(i + 1) % 4];
                Column n = at(cols, w, cx + a.getStepX() + b.getStepX(), cz + a.getStepZ() + b.getStepZ());
                if (n.y == h + 1) {
                    high = n;
                    corner1 = a;
                    corner2 = b;
                }
            }
            if (high == null) return null;
        }
        boolean opposite = ups == 2 && up1.getOpposite() == up2;
        // snow steps only where snow lies all year; seasonal snow would melt off the step, so it gets a (snowy) slab
        if (high.snow && high.cold || c.snow && c.cold || high.ground.is(Blocks.SNOW_BLOCK) || high.ground.is(Blocks.POWDER_SNOW)) {
            int layers = ups == 0 ? SNOW_CORNER : ups == 1 ? SNOW_STEP : SNOW_NOOK;
            return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
        }
        Block stair = rock(high.ground) ? variant(high.ground.getBlock(), STAIRS, "stairs") : Blocks.AIR;
        if (stair != Blocks.AIR && !opposite && ups <= 2) {
            BlockState s = stair.defaultBlockState().setValue(StairBlock.HALF, Half.BOTTOM);
            if (ups == 1) {
                // straight: the edge goes on along both sides at this height
                Direction side = up1.getClockWise();
                if (height(cols, w, cx + side.getStepX(), cz + side.getStepZ()) == h && height(cols, w, cx - side.getStepX(), cz - side.getStepZ()) == h)
                    return s.setValue(StairBlock.FACING, up1);
            } else if (ups == 2) {
                // inner corner: both walls go on (vanilla's rule: the stair in front faces up2)
                if (height(cols, w, cx - up1.getStepX(), cz - up1.getStepZ()) == h && height(cols, w, cx - up2.getStepX(), cz - up2.getStepZ()) == h)
                    return s.setValue(StairBlock.FACING, up1).setValue(StairBlock.SHAPE, up2 == up1.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT);
            } else if (height(cols, w, cx + corner1.getStepX(), cz + corner1.getStepZ()) == h && height(cols, w, cx + corner2.getStepX(), cz + corner2.getStepZ()) == h) {
                // outer corner: the stair behind faces corner2
                return s.setValue(StairBlock.FACING, corner1).setValue(StairBlock.SHAPE, corner2 == corner1.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT);
            }
        }
        Block slab = variant(high.ground.getBlock(), SLABS, "slab");
        return slab == Blocks.AIR ? null : slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    private static Column at(Column[] cols, int w, int x, int z) {
        return cols[x + z * w];
    }

    private static int height(Column[] cols, int w, int x, int z) {
        return x < 0 || z < 0 || x >= w || z >= w ? NONE : cols[x + z * w].y;
    }

    /** Scans down from the surface through air, plants, snow, leaves, logs and bottom slabs/stairs to the ground. */
    private static Column column(WorldGenLevel level, int x, int z, BlockPos.MutableBlockPos p) {
        // a live chunk keeps WORLD_SURFACE, a chunk being generated keeps the _WG one (its neighbours may lack the other)
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
        int top = level.getMinY();
        for (Heightmap.Types type : SURFACES) if (chunk.hasPrimedHeightmap(type)) top = Math.max(top, chunk.getHeight(type, x & 15, z & 15));
        boolean snow = false;
        for (int y = top; y > top - SCAN; y--) {
            BlockState s = level.getBlockState(p.set(x, y, z));
            if (s.is(Blocks.SNOW)) {
                snow = true;
                continue;
            }
            if (seeThrough(s)) {
                snow = false;
                continue;
            }
            if (path(s)) return new Column(NONE, s, false, false, true);
            if (!ground(s) || !level.getFluidState(p.set(x, y + 1, z)).isEmpty()) return new Column(NONE, s, false, false, false);
            boolean cold = snow && Thaw.coldAllYear(level.getLevel(), level.getBiome(p).value(), p.set(x, y + 1, z));
            return new Column(y, s, snow, cold, false);
        }
        return new Column(NONE, Blocks.AIR.defaultBlockState(), false, false, false);
    }

    private static boolean seeThrough(BlockState s) {
        if (s.isAir() || !s.isSolid() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)) return true;
        if (s.getBlock() instanceof SlabBlock) return s.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
        return s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM;
    }

    private static boolean path(BlockState s) {
        return s.getBlock() instanceof TrampledPathBlock || s.is(Blocks.DIRT_PATH) || s.is(EarthBlocks.DIRT_PATH_SLAB);
    }

    /** Natural ground a half step may be made of or laid on. */
    static boolean ground(BlockState s) {
        return s.is(BlockTags.SUBSTRATE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || rock(s) || s.is(Blocks.SNOW_BLOCK)
                || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.CLAY) || s.is(EarthBlocks.PEAT) || s.is(EarthBlocks.LOESS)
                || s.is(ScorchedBarrens.WOOD_ASH);
    }

    static boolean rock(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE);
    }

    /** Air, a small plant or thin snow there and nothing resting on it from above (the top half of a tall plant), no water. */
    private static boolean free(WorldGenLevel level, BlockPos at) {
        BlockState s = level.getBlockState(at);
        if (!level.getFluidState(at).isEmpty()) return false;
        if (s.isAir() || s.is(Blocks.SNOW)) return true;
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) return false;
        return plant(s);
    }

    /** A small plant the slab may replace: grass, ferns, flowers (tall ones too), saplings, mushrooms. Not cane, cactus or bamboo. */
    private static boolean plant(BlockState s) {
        return !s.hasBlockEntity() && (s.canBeReplaced() || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.BROWN_MUSHROOM) || s.is(Blocks.RED_MUSHROOM));
    }

    /**
     * The slab (kind "slab") or stairs of a ground block: the mod's ground slabs first (mechanical:sand_slab), then the
     * block's own family (minecraft:stone_slab, granite_stairs), without a _block/_stone suffix (chalk_block -> chalk_slab),
     * then the cobbled one (slate -> cobbled_slate_slab, deepslate -> cobbled_deepslate_slab). AIR when there is none.
     */
    static Block variant(Block base, Map<Block, Block> cache, String kind) {
        return cache.computeIfAbsent(base, b -> {
            if (b == Blocks.GRASS_BLOCK) return kind.equals("slab") ? EarthBlocks.GRASS_SLAB.get() : EarthBlocks.GRASS_STAIRS.get();
            if (b == Blocks.ROOTED_DIRT) return kind.equals("slab") ? EarthBlocks.DIRT_SLAB.get() : EarthBlocks.DIRT_STAIRS.get();
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            String path = id.getPath(), bare = path.replaceAll("_(block|stone)$", "");
            for (Identifier c : List.of(Identifier.fromNamespaceAndPath("mechanical", path + "_" + kind), id.withPath(path + "_" + kind),
                    id.withPath(bare + "_" + kind), id.withPath("cobbled_" + bare + "_" + kind))) {
                Optional<Block> found = BuiltInRegistries.BLOCK.getOptional(c);
                if (found.isPresent()) return found.get();
            }
            return Blocks.AIR;
        });
    }

    /** Boxes of the structures reaching this square or within 2 of it; null when a structure start can't be read (leave it all). */
    private static List<BoundingBox> structureBoxes(WorldGenLevel level, int x0, int z0, int size) {
        List<BoundingBox> boxes = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int cx = (x0 - R) >> 4; cx <= (x0 + size + R - 1) >> 4; cx++) {
            for (int cz = (z0 - R) >> 4; cz <= (z0 + size + R - 1) >> 4; cz++) {
                // a WorldGenRegion throws (not null) for a chunk outside it: ask first
                ChunkAccess chunk = level.hasChunk(cx, cz) ? level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false) : null;
                if (chunk == null) continue;
                for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> e : chunk.getAllReferences().entrySet()) {
                    for (long ref : e.getValue()) {
                        if (!seen.add(ref * 31 + e.getKey().hashCode())) continue;
                        ChunkAccess start = level.hasChunk(ChunkPos.getX(ref), ChunkPos.getZ(ref))
                                ? level.getChunk(ChunkPos.getX(ref), ChunkPos.getZ(ref), ChunkStatus.STRUCTURE_STARTS, false) : null;
                        if (start == null) return null;
                        StructureStart s = start.getStartForStructure(e.getKey());
                        if (s != null && s.isValid()) boxes.add(s.getBoundingBox().inflatedBy(2));
                    }
                }
            }
        }
        return boxes;
    }

    /** Retrogen leaves chunks near a camp (CampAge), a tribe's Fire Pit or a tribe road alone. */
    static boolean protectedChunk(ServerLevel level, ChunkPos pos) {
        BlockPos centre = new BlockPos(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ());
        int r = CampAge.RADIUS + 16;
        for (CampAge.Camp camp : CampAge.get(level).camps())
            if (Math.abs(camp.centre.getX() - centre.getX()) <= r && Math.abs(camp.centre.getZ() - centre.getZ()) <= r) return true;
        if (Clans.pitNear(level, centre, r)) return true;
        for (RoadBuilding.Road road : RoadBuilding.get(level.getServer()).roads)
            for (long cell : road.cells)
                if (Math.abs(BlockPos.getX(cell) - centre.getX()) <= 24 && Math.abs(BlockPos.getZ(cell) - centre.getZ()) <= 24) return true;
        return false;
    }
}
