package com.mechanical.worldgen;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

import static com.mechanical.worldgen.ScorchedBarrens.BURNT_SOIL;
import static com.mechanical.worldgen.ScorchedBarrens.CHARRED_LOG;
import static com.mechanical.worldgen.ScorchedBarrens.WOOD_ASH;

/**
 * The Scorched Barrens' floor, its trees and its edge, one pass per chunk (placed by scripts/gen_world_biomes.py). Every
 * column knows how far it is from the other side of the border (barrens or living forest), up to {@link #EDGE} blocks.
 * <ul>
 * <li>INSIDE (listed in the barrens, vegetal decoration): at the rim singed grass with dry grass and dead leaves, scorched
 * trees that are still alive (bark on, a charred foot, a thin crown of dead leaves); further in burnt soil, half-burnt
 * trees; then the burnt forest: Wood Ash lying in the hollows and bare Burnt Soil on the rises, burnt trees
 * ({@link ScorchedBarrens.CharredTree}) in a density that comes and goes, with clearings; here and there regrowth:
 * Fireweed, ferns, the odd birch sapling.</li>
 * <li>OUTSIDE (listed in the living forests round it, after their trees): within a few blocks of the barrens the feet of
 * the trees are charred, their crowns thinned, and the ground singed.</li>
 * </ul>
 */
final class BurntForest extends StoneAgeFeature {
    static final BurntForest INSIDE = new BurntForest(true), OUTSIDE = new BurntForest(false);
    /** How far (blocks) the edge reaches: the barrens' rim band inside, the singed band outside is narrower. */
    static final int EDGE = 16;

    private final boolean inside;
    private final MapCodec<BurntForest> codec;

    private BurntForest(boolean inside) {
        this.inside = inside;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<BurntForest> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        if (!ScorchedBarrens.farEnough(Math.abs(cx + 8) + 48, Math.abs(cz + 8) + 48)) return false; // no barrens this near 0,0
        int y = Math.max(level.getSeaLevel(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + 8, cz + 8));
        // the forest pass only where a barrens is near: a coarse look first (barrens are always far wider than 16 blocks)
        if (!inside && !nearBarrens(level, cx, cz, y)) return false;
        float[] dist = distances(level, cx, cz, y);
        if (dist == null) return false;
        long seed = level.getSeed();
        if (inside) inside(level, random, cx, cz, dist, seed);
        else outside(level, random, cx, cz, dist, seed);
        return true;
    }

    // ------------------------------------------------------------------ the distance field

    /** The biome at a quart (4 blocks) of the chunk or its 8 neighbours: the only chunks a feature may read. */
    private static boolean barrens(WorldGenLevel level, int qx, int qy, int qz) {
        return level.getNoiseBiome(qx, qy, qz).is(ModBiomes.SCORCHED_BARRENS);
    }

    private static boolean nearBarrens(WorldGenLevel level, int cx, int cz, int y) {
        int q0x = (cx >> 2) - 4, q0z = (cz >> 2) - 4;
        for (int i = 0; i < 12; i += 2)
            for (int j = 0; j < 12; j += 2)
                if (barrens(level, q0x + i, y >> 2, q0z + j)) return true;
        return false;
    }

    /**
     * Per column of the chunk (x + 16 z): blocks to the nearest quart on the other side of the border, EDGE when none is
     * that close, -1 for a column on the other side itself. Null when this pass has no column of its own side in the chunk.
     */
    private float[] distances(WorldGenLevel level, int cx, int cz, int y) {
        int n = 12, q0x = (cx >> 2) - 4, q0z = (cz >> 2) - 4; // the chunk and 16 blocks round it, in quarts
        boolean[] mine = new boolean[n * n];
        boolean any = false;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                mine[i + j * n] = barrens(level, q0x + i, y >> 2, q0z + j) == inside;
                if (i >= 4 && i < 8 && j >= 4 && j < 8) any |= mine[i + j * n];
            }
        if (!any) return null;
        float[] d = new float[256];
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                if (!mine[4 + (x >> 2) + (4 + (z >> 2)) * n]) {
                    d[x + 16 * z] = -1;
                    continue;
                }
                int best = EDGE * EDGE;
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < n; j++) {
                        if (mine[i + j * n]) continue;
                        // to the near edge of that quart, not its middle
                        int dx = Math.max(0, Math.abs(i * 4 + 2 - 16 - x) - 2), dz = Math.max(0, Math.abs(j * 4 + 2 - 16 - z) - 2);
                        best = Math.min(best, dx * dx + dz * dz);
                    }
                d[x + 16 * z] = (float) Math.sqrt(best);
            }
        return d;
    }

    // ------------------------------------------------------------------ inside the barrens

    private static void inside(WorldGenLevel level, RandomSource random, int cx, int cz, float[] dist, long seed) {
        int[] h = new int[20 * 20]; // terrain heights of the chunk and 2 round it, for hollows and rises
        for (int i = 0; i < 20; i++)
            for (int j = 0; j < 20; j++) h[i + 20 * j] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx - 2 + i, cz - 2 + j) - 1;
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState(), soil = BURNT_SOIL.get().defaultBlockState(), ash = WOOD_ASH.get().defaultBlockState();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                float d = dist[x + 16 * z];
                if (d < 0) continue;
                BlockPos g = ground(level, cx + x, cz + z);
                if (g == null || !barrensGround(level.getBlockState(g))) continue;
                // the edge wanders: blocks of rim in and out along the border
                float e = d + (float) (noise(seed + 11, (cx + x) / 7.0, (cz + z) / 7.0) - 0.5) * 7;
                float r = random.nextFloat();
                BlockState top = null;
                if (e < 3) top = r < 0.74F ? grass : r < 0.8F ? Blocks.PODZOL.defaultBlockState() : soil; // no orange coarse dirt: it clashed
                else if (e < 9) {
                    float pg = 0.65F * (9 - e) / 6;
                    top = r < pg ? grass : r < pg + 0.55F ? soil : null;
                } else if (e < 14) {
                    if (r < 0.04F) top = grass;
                    else if (r < 0.5F * (14 - e) / 5 && level.getBlockState(g).is(WOOD_ASH.get())) top = soil;
                } else if (!level.getBlockState(g).is(Blocks.GRAVEL)) {
                    // ash settles in the hollows, the wind and rain bare the burnt soil on the rises
                    int i = x + 2, j = z + 2, here = h[i + 20 * j];
                    float around = (h[i - 2 + 20 * j] + h[i + 2 + 20 * j] + h[i + 20 * (j - 2)] + h[i + 20 * (j + 2)]) / 4F;
                    if (here < around - 0.6F && r < 0.85F) top = ash;
                    else if (here > around + 0.6F && r < 0.8F) top = soil;
                }
                if (top != null) set(level, g, top);
                if (top == grass || e < 9 && top == soil) singedPlant(level, random, g.above(), e, top == grass);
            }
        trees(level, random, cx, cz, dist, seed);
        regrowth(level, random, cx, cz, dist);
    }

    private static boolean barrensGround(BlockState s) {
        return s.is(WOOD_ASH.get()) || s.is(BURNT_SOIL.get()) || s.is(Blocks.GRAVEL) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT);
    }

    /** Dry grass, a tuft of green at the very rim, dead leaves, a fern: the singed undergrowth of the edge. */
    private static void singedPlant(WorldGenLevel level, RandomSource random, BlockPos p, float e, boolean grass) {
        if (!level.getBlockState(p).isAir()) return;
        float r = random.nextFloat() * (grass ? 1 : 2.5F);
        BlockState s = null;
        if (r < 0.30F) s = Blocks.SHORT_DRY_GRASS.defaultBlockState();
        else if (r < 0.37F) s = Blocks.TALL_DRY_GRASS.defaultBlockState();
        else if (r < 0.46F && e < 3) s = Blocks.SHORT_GRASS.defaultBlockState();
        else if (r < 0.49F && e < 5) s = Blocks.FERN.defaultBlockState();
        else if (r < 0.57F)
            s = Blocks.LEAF_LITTER.defaultBlockState().trySetValue(BlockStateProperties.SEGMENT_AMOUNT, 1 + random.nextInt(4))
                    .trySetValue(BlockStateProperties.HORIZONTAL_FACING, randomSide(random));
        if (s != null && s.canSurvive(level, p)) set(level, p, s);
    }

    /**
     * The trees: scorched but alive at the rim, half burnt further in, then the burnt forest. How many comes and goes
     * with a slow noise (thick stands, thin ones, clearings), and no trunk stands right against another.
     */
    private static void trees(WorldGenLevel level, RandomSource random, int cx, int cz, float[] dist, long seed) {
        double stand = noise(seed + 21, (cx + 8) / 48.0, (cz + 8) / 48.0);
        int count = Math.round((float) (Math.pow(stand, 1.4) * 11)) + random.nextInt(2);
        for (int tries = count * 3, placed = 0; tries > 0 && placed < count; tries--) {
            int x = random.nextInt(16), z = random.nextInt(16);
            float d = dist[x + 16 * z];
            if (d < 0 || noise(seed + 23, (cx + x) / 9.0, (cz + z) / 9.0) < 0.28) continue; // a clearing
            BlockPos g = ground(level, cx + x, cz + z);
            if (g == null || !barrensGround(level.getBlockState(g)) && !level.getBlockState(g).is(Blocks.BLACKSTONE)) continue;
            BlockPos c = ScorchedBarrens.CharredTree.ground(level, g.above());
            if (c == null || crowded(level, c)) continue;
            float e = d + (float) (noise(seed + 11, (cx + x) / 7.0, (cz + z) / 7.0) - 0.5) * 7;
            boolean ok;
            if (e < 3) ok = random.nextBoolean() && scorchedTree(level, random, c, 0.1F);
            else if (e < 8) ok = scorchedTree(level, random, c, 0.15F + (e - 3) / 10);
            else if (e < 14) ok = random.nextInt(3) > 0 ? scorchedTree(level, random, c, 0.6F + (e - 8) / 15)
                    : ScorchedBarrens.CharredTree.grow(level, random, c, random.nextInt(60));
            else ok = ScorchedBarrens.CharredTree.grow(level, random, c, random.nextInt(100));
            if (ok) placed++;
        }
    }

    /** Another trunk within a block of c (so no two weld together). */
    private static boolean crowded(WorldGenLevel level, BlockPos c) {
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 2, 1))) {
            BlockState s = level.getBlockState(p);
            if (s.is(CHARRED_LOG.get()) || s.is(BlockTags.LOGS) || s.getBlock() instanceof PipeBlock) return true;
        }
        return false;
    }

    /**
     * A tree the fire passed: the species of the forest round about, its foot charred (higher the deeper in), the crown
     * thin and dead-leaved (the barrens' foliage colour browns oak leaves), none at all once burn is high; charred stubs.
     * burn 0..1.
     */
    static boolean scorchedTree(WorldGenLevel level, RandomSource random, BlockPos c, float burn) {
        ForestDecor.Wood w = ForestDecor.woodNear(level, c, 14);
        Block log = w != null ? w.log() : random.nextBoolean() ? Blocks.OAK_LOG : Blocks.BIRCH_LOG;
        BlockState leaves = w != null && w.leaves() != null ? w.leaves()
                : (log == Blocks.BIRCH_LOG ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int h = 5 + random.nextInt(4) - (burn > 0.6F ? random.nextInt(3) : 0);
        int charred = Math.min(h, 1 + Math.round(burn * (h - 1)) + random.nextInt(2));
        BlockPos top = null;
        for (int y = 0; y < h && level.getBlockState(c.above(y)).canBeReplaced(); y++)
            set(level, top = c.above(y), y < charred ? CHARRED_LOG.get().defaultBlockState() : ForestDecor.pillar(log, Direction.Axis.Y));
        if (top == null || top.getY() - c.getY() < 3) return top != null;
        float keep = burn > 0.75F ? 0 : 0.6F * (1 - burn / 0.75F) + 0.08F;
        if (keep > 0)
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-2, -2, -2), top.offset(2, 1, 2))) {
                int dx = p.getX() - top.getX(), dy = p.getY() - top.getY(), dz = p.getZ() - top.getZ();
                if (dx * dx + dz * dz + dy * dy * 1.6 > 6.5 || random.nextFloat() > keep || !level.getBlockState(p).isAir()) continue;
                set(level, p.immutable(), leaves);
            }
        if (burn > 0.3F) // dead limbs, burnt black
            for (int i = random.nextInt(3); i > 0; i--)
                ScorchedBarrens.CharredTree.limb(level, top.below(1 + random.nextInt(2)), List.of(randomSide(random)), 1);
        else if (random.nextInt(3) == 0) ScorchedBarrens.CharredTree.limb(level, top, List.of(Direction.UP), 1);
        return true;
    }

    /** Fireweed in clumps, ferns in the shelter of a trunk, a birch sapling now and then, a tuft of dry grass. */
    private static void regrowth(WorldGenLevel level, RandomSource random, int cx, int cz, float[] dist) {
        if (random.nextInt(6) == 0) {
            int x = 3 + random.nextInt(10), z = 3 + random.nextInt(10);
            if (dist[x + 16 * z] >= 9) for (int i = 2 + random.nextInt(4); i > 0; i--)
                fireweed(level, ground(level, cx + x + random.nextInt(7) - 3, cz + z + random.nextInt(7) - 3));
        }
        if (random.nextInt(4) == 0) {
            int x = 2 + random.nextInt(12), z = 2 + random.nextInt(12);
            if (dist[x + 16 * z] >= 9) for (int i = 2 + random.nextInt(4); i > 0; i--)
                rooted(level, random, ground(level, cx + x + random.nextInt(5) - 2, cz + z + random.nextInt(5) - 2), Blocks.FERN.defaultBlockState(), true);
        }
        if (random.nextInt(14) == 0) {
            int x = random.nextInt(16), z = random.nextInt(16);
            if (dist[x + 16 * z] >= 9) rooted(level, random, ground(level, cx + x, cz + z), Blocks.BIRCH_SAPLING.defaultBlockState(), false);
        }
        for (int i = random.nextInt(3) == 0 ? 1 + random.nextInt(3) : 0; i > 0; i--) {
            int x = random.nextInt(16), z = random.nextInt(16);
            if (dist[x + 16 * z] >= 9) rooted(level, random, ground(level, cx + x, cz + z), Blocks.SHORT_DRY_GRASS.defaultBlockState(), false);
        }
    }

    /** A plant on barrens ground at g (the ground block), which turns to Burnt Soil under it. near: only beside a trunk. */
    private static void rooted(WorldGenLevel level, RandomSource random, BlockPos g, BlockState plant, boolean near) {
        if (g == null || !barrensGround(level.getBlockState(g)) || !level.getBlockState(g.above()).isAir()) return;
        if (near) {
            boolean trunk = false;
            for (Direction d : Direction.Plane.HORIZONTAL) trunk |= level.getBlockState(g.above().relative(d)).is(CHARRED_LOG.get());
            if (!trunk && random.nextInt(3) > 0) return;
        }
        BlockState was = level.getBlockState(g);
        set(level, g, BURNT_SOIL.get().defaultBlockState());
        if (plant.canSurvive(level, g.above())) set(level, g.above(), plant);
        else set(level, g, was);
    }

    private static void fireweed(WorldGenLevel level, BlockPos g) {
        if (g == null || !barrensGround(level.getBlockState(g)) || !level.getBlockState(g.above()).isAir() || !level.getBlockState(g.above(2)).isAir()) return;
        set(level, g, BURNT_SOIL.get().defaultBlockState());
        BlockState f = ScorchedBarrens.FIREWEED.get().defaultBlockState();
        if (f.canSurvive(level, g.above())) DoublePlantBlock.placeAt(level, f, g.above(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ the forest round it

    private static void outside(WorldGenLevel level, RandomSource random, int cx, int cz, float[] dist, long seed) {
        final float reach = 9;
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                float d = dist[x + 16 * z];
                if (d < 0) continue;
                float e = d + (float) (noise(seed + 11, (cx + x) / 7.0, (cz + z) / 7.0) - 0.5) * 5;
                if (e >= reach) continue;
                float f = 1 - Math.max(0, e) / reach; // 1 at the border, 0 at reach
                // crowns thinned by the heat: leaves gone here and there, more of them near the border
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx + x, cz + z);
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(cx + x, top, cz + z);
                BlockPos g = null;
                for (int i = 0; i < 40; i++, p.move(Direction.DOWN)) {
                    BlockState s = level.getBlockState(p);
                    if (s.is(BlockTags.LEAVES)) {
                        if (random.nextFloat() < 0.4F * f) set(level, p.immutable(), AIR);
                    } else if (s.is(BlockTags.LOGS)) {
                        // a trunk's foot standing on the ground: charred a block or two up
                        if (level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD) && random.nextFloat() < f * 1.3F) {
                            int k = 1 + (random.nextFloat() < f ? 1 : 0);
                            for (int y = 0; y < k && level.getBlockState(p.above(y)).is(BlockTags.LOGS); y++) set(level, p.above(y), CHARRED_LOG.get().defaultBlockState());
                            break;
                        }
                    } else if (!s.isAir() && !s.canBeReplaced()) {
                        g = p.immutable();
                        break;
                    }
                }
                if (g == null || !level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD) || !level.getFluidState(g.above()).isEmpty()) continue;
                // singed ground: dry grass instead of green, patches of burnt soil and coarse dirt nearest the border
                float r = random.nextFloat();
                BlockState above = level.getBlockState(g.above());
                if (r < 0.22F * f * f) {
                    set(level, g, (random.nextInt(3) == 0 ? Blocks.PODZOL : BURNT_SOIL.get()).defaultBlockState());
                    if (!above.isAir() && !above.canSurvive(level, g.above())) set(level, g.above(), AIR);
                } else if ((above.isAir() || above.is(Blocks.SHORT_GRASS) || above.is(Blocks.TALL_GRASS)) && r < 0.22F * f * f + 0.45F * f
                        && level.getBlockState(g).is(Blocks.GRASS_BLOCK)) {
                    if (above.is(Blocks.TALL_GRASS)) set(level, g.above(2), AIR);
                    set(level, g.above(), (random.nextInt(4) == 0 ? Blocks.TALL_DRY_GRASS : Blocks.SHORT_DRY_GRASS).defaultBlockState());
                }
            }
    }

    // ------------------------------------------------------------------ helpers

    /** The ground block (the first solid one below plants, leaves and trunks) of a column, null under water. */
    static BlockPos ground(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 20, z);
        for (int i = 0; i < 48; i++, p.move(Direction.DOWN)) {
            BlockState s = level.getBlockState(p);
            if (!s.getFluidState().isEmpty()) return null;
            if (s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(CHARRED_LOG.get()) || s.getBlock() instanceof PipeBlock
                    || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)) continue;
            return p.immutable();
        }
        return null;
    }

    /** Smooth value noise in 0..1, one lattice cell per unit. */
    static double noise(long seed, double x, double z) {
        int ix = Mth.floor(x), iz = Mth.floor(z);
        double fx = x - ix, fz = z - iz, sx = fx * fx * (3 - 2 * fx), sz = fz * fz * (3 - 2 * fz);
        return Mth.lerp(sz, Mth.lerp(sx, hash(seed, ix, iz), hash(seed, ix + 1, iz)), Mth.lerp(sx, hash(seed, ix, iz + 1), hash(seed, ix + 1, iz + 1)));
    }

    private static double hash(long seed, int x, int z) {
        long n = seed ^ x * 341873128712L ^ z * 132897987541L;
        n = (n ^ (n >>> 33)) * 0xff51afd7ed558ccdL;
        n = (n ^ (n >>> 33)) * 0xc4ceb9fe1a85ec53L;
        n ^= n >>> 33;
        return (n & 0xFFFFFF) / (double) 0x1000000;
    }
}
