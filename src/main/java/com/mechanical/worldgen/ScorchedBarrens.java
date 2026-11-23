package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mechanical.AncientOak;
import com.mechanical.EarthBlocks;
import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Scorched Barrens (docs/elkwight-plan.md phase 1): a burnt forest only beyond {@link #MIN_DISTANCE} blocks from 0,0 and
 * always ringed by living forest (mixin.MultiNoiseBiomeSourceMixin turns it back into its source biome elsewhere, see
 * {@link #stays}). Its blocks, and the charred trees.
 * Data: scripts/gen_world_biomes.py (biome, ground, features), scripts/gen_elkwight.py (blocks, ruin).
 */
public final class ScorchedBarrens {
    /** Neither the biome nor the Elkwight ruins come closer to 0,0 than this. */
    public static final int MIN_DISTANCE = 2500;

    public static boolean farEnough(int x, int z) {
        return (long) x * x + (long) z * z >= (long) MIN_DISTANCE * MIN_DISTANCE;
    }

    /** Burnt trunks: standing, fallen and stumps. Drops charcoal (loot table). */
    public static final DeferredBlock<RotatedPillarBlock> CHARRED_LOG = Mechanical.BLOCKS.registerBlock("charred_log", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(1.5F).sound(SoundType.WOOD));
    /** Grey wood ash lying over the burnt ground; falls like sand. */
    public static final DeferredBlock<ColoredFallingBlock> WOOD_ASH = Mechanical.BLOCKS.registerBlock("wood_ash",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFF615E5A), p), p -> p.mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.SAND));
    /** The forest floor after the fire: dark earth with charcoal and ash in it. Plants take root in it again. */
    public static final DeferredBlock<Block> BURNT_SOIL = Mechanical.BLOCKS.registerBlock("burnt_soil", Block::new,
            p -> p.mapColor(MapColor.TERRACOTTA_BLACK).strength(0.5F).sound(SoundType.ROOTED_DIRT));
    /** The thin parts of burnt trees (8 and 4 px, joining like chorus plants): limbs, tapering tops, splinters, root flares. */
    public static final DeferredBlock<AncientOak.Branch> CHARRED_BRANCH = Mechanical.BLOCKS.registerBlock("charred_branch", p -> new AncientOak.Branch(8, p),
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<AncientOak.Branch> CHARRED_TWIG = Mechanical.BLOCKS.registerBlock("charred_twig", p -> new AncientOak.Branch(4, p),
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.WOOD).noOcclusion());
    /** The first flower after a forest fire: tall magenta spikes coming up through the ash. */
    public static final DeferredBlock<TallFlowerBlock> FIREWEED = Mechanical.BLOCKS.registerBlock("fireweed", TallFlowerBlock::new,
            p -> p.mapColor(MapColor.COLOR_MAGENTA).noCollision().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava().pushReaction(PushReaction.POPPED));
    /** Blackstone carved with faintly glowing green runes, in the Elkwight ruins. */
    public static final DeferredBlock<RuneBlock> RUNE_BLACKSTONE = Mechanical.BLOCKS.registerBlock("rune_blackstone", RuneBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE).lightLevel(s -> 4));

    /**
     * Biome check in quart coordinates (4 blocks): a few blocks of margin, since the biome zoom can pick a neighbouring
     * quart for a block, so no block within MIN_DISTANCE ever reads as Scorched Barrens.
     */
    public static boolean biomeAllowed(int quartX, int quartZ) {
        long x = quartX * 4L + 2, z = quartZ * 4L + 2, r = MIN_DISTANCE + 12;
        return x * x + z * z >= r * r;
    }

    /** Quarts (24 blocks) of living forest round every Scorched Barrens quart: it never touches plains, water, desert... */
    public static final int RING = 6;
    /** What counts as living forest round the barrens (and the barrens itself, so its inside is fine). */
    private static final Set<ResourceKey<Biome>> LIVING = Set.of(ModBiomes.SCORCHED_BARRENS, ModBiomes.DECIDUOUS_FOREST, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.DAPPLED_FOREST, Biomes.TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);

    /**
     * Whether a raw Scorched Barrens column stays barrens (MultiNoiseBiomeSourceMixin; else it is Forest): far enough out,
     * and every raw surface biome within RING quarts is living forest or barrens. So the barrens that stays always has a
     * ring of at least RING quarts of forest round it. The grid must reach RING quarts past (quartX, quartZ).
     */
    public static boolean stays(SurfaceGrid grid, int quartX, int quartZ) {
        return biomeAllowed(quartX, quartZ) && grid.all(quartX, quartZ, RING, ScorchedBarrens::living);
    }

    public static boolean living(Holder<Biome> h) {
        return h instanceof Holder.Reference<Biome> r && LIVING.contains(r.key());
    }

    /**
     * The resolver MultiNoiseBiomeSourceMixin returns: raw barrens quarts that may not stay become {@code source}. Only
     * they pay for the neighbourhood check, with one surface grid per chunk (the chunk and RING quarts round it) and one
     * decision per column. sizeX 0: a resolver for anywhere (may be shared between threads), so no cache.
     */
    public static BiomeResolver resolver(BiomeResolver raw, MultiNoiseBiomeSource biomes, Climate.Sampler sampler, Holder<Biome> barrens,
            Holder<Biome> source, int minX, int minZ, int sizeX, int sizeZ) {
        return new BiomeResolver() {
            private SurfaceGrid grid;
            private final byte[] stays = new byte[sizeX * sizeZ]; // per column: 0 not yet decided, 1 stays, 2 forest

            @Override
            public Holder<Biome> getNoiseBiome(int x, int y, int z) {
                Holder<Biome> h = raw.getNoiseBiome(x, y, z);
                if (h != barrens) return h;
                if (!biomeAllowed(x, z)) return source;
                if (x < minX || z < minZ || x >= minX + sizeX || z >= minZ + sizeZ)
                    return stays(SurfaceGrid.around(biomes, sampler, x, z, RING), x, z) ? h : source;
                int i = x - minX + (z - minZ) * sizeX;
                if (stays[i] == 0) {
                    if (grid == null) grid = new SurfaceGrid(biomes, sampler, minX - RING, minZ - RING, sizeX + 2 * RING, sizeZ + 2 * RING, 1);
                    stays[i] = (byte) (stays(grid, x, z) ? 1 : 2);
                }
                return stays[i] == 1 ? h : source;
            }
        };
    }

    /** Every quart within radius blocks of (x, z) is Scorched Barrens after the rules (the Elkwight ruin sits well inside). */
    public static boolean deepInside(MultiNoiseBiomeSource source, Climate.Sampler sampler, int x, int z, int radius) {
        int qx = QuartPos.fromBlock(x), qz = QuartPos.fromBlock(z), r = (radius + 3) / 4;
        SurfaceGrid g = SurfaceGrid.around(source, sampler, qx, qz, r + RING);
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                if (dx * dx + dz * dz <= r * r && !(g.is(qx + dx, qz + dz, b -> b.is(ModBiomes.SCORCHED_BARRENS)) && stays(g, qx + dx, qz + dz)))
                    return false;
        return true;
    }

    public static void init() {}

    private ScorchedBarrens() {}

    /** Now and then a green mote drifts off the runes. */
    public static final class RuneBlock extends Block {
        public RuneBlock(Properties p) {
            super(p);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(8) != 0) return;
            Direction d = Direction.getRandom(random);
            if (!level.isEmptyBlock(pos.relative(d))) return;
            level.addParticle(new DustParticleOptions(0x6BFF8A, 0.5F + random.nextFloat() * 0.3F),
                    pos.getX() + 0.5 + d.getStepX() * 0.6, pos.getY() + 0.5 + d.getStepY() * 0.6, pos.getZ() + 0.5 + d.getStepZ() * 0.6,
                    d.getStepX() * 0.01, 0.02, d.getStepZ() * 0.01);
        }
    }

    /**
     * One burnt tree, in many shapes so a burnt forest reads as one: stumps (some splintered, some with root flares), snags
     * of every height with snapped or tapering tops and stubs of branches, leaning snags, now and then a big old tree that
     * kept its charred limbs, and trunks lying flat on their own (some half buried in ash), sometimes with the stump they
     * broke from a little way off. Thin parts are Charred Branch (8 px) and Charred Twig (4 px).
     */
    static final class CharredTree extends StoneAgeFeature {
        static final MapCodec<CharredTree> CODEC = MapCodec.unit(CharredTree::new);

        @Override
        public MapCodec<CharredTree> codec() {
            return CODEC;
        }

        @Override
        public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
            BlockPos c = ground(level, origin);
            return c != null && grow(level, random, c, random.nextInt(100));
        }

        /** The free block on sturdy dry ground at origin's column (8 up, 16 down from it), or null. */
        static BlockPos ground(WorldGenLevel level, BlockPos origin) {
            // from the placement's height to the ground (the _WG heightmaps are stale or absent outside early worldgen)
            BlockPos c = origin;
            for (int i = 0; i < 8 && !level.getBlockState(c).canBeReplaced(); i++) c = c.above();
            for (int i = 0; i < 16 && level.getBlockState(c.below()).canBeReplaced(); i++) c = c.below();
            // on any sturdy ground: Wood Ash, Burnt Soil, Blackstone and Basalt are not in the substrate tags
            boolean ok = level.getBlockState(c.below()).isFaceSturdy(level, c.below(), Direction.UP) && level.getFluidState(c).isEmpty()
                    && level.getBlockState(c).canBeReplaced();
            return ok ? c : null;
        }

        /** A tree of the kind roll (0-99) picks at c (a free block on ground). Falls back to a stump, so it always places. */
        static boolean grow(WorldGenLevel level, RandomSource random, BlockPos c, int roll) {
            if (roll < 28) return stump(level, random, c);
            if (roll < 48) return snag(level, random, c, 3 + random.nextInt(4), null);
            if (roll < 61) return snag(level, random, c, 7 + random.nextInt(6), null);
            if (roll < 69) return snag(level, random, c, 6 + random.nextInt(5), randomSide(random));
            if (roll < 76) return bigTree(level, random, c) || snag(level, random, c, 8 + random.nextInt(4), null);
            return fallen(level, random, c) || stump(level, random, c);
        }

        private static BlockState log(Direction.Axis axis) {
            return CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        }

        private static boolean free(WorldGenLevel level, BlockPos p) {
            BlockState s = level.getBlockState(p);
            return s.canBeReplaced() && s.getFluidState().isEmpty();
        }

        private static boolean sturdy(WorldGenLevel level, BlockPos p) {
            return level.getBlockState(p).isFaceSturdy(level, p, Direction.UP);
        }

        /**
         * A limb of Charred Branch from the block at from, one step per direction in steps, the last twigs cells of it
         * Charred Twig; each piece joins the one before (from itself counts when it is a trunk). Stops where it is blocked.
         */
        static int limb(WorldGenLevel level, BlockPos from, List<Direction> steps, int twigs) {
            List<BlockPos> cells = new ArrayList<>();
            BlockPos p = from;
            for (Direction d : steps) {
                p = p.relative(d);
                if (!free(level, p)) break;
                cells.add(p);
            }
            for (int i = 0; i < cells.size(); i++) {
                Block b = i >= cells.size() - twigs ? CHARRED_TWIG.get() : CHARRED_BRANCH.get();
                BlockState s = b.defaultBlockState().setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(steps.get(i).getOpposite()), true);
                if (i + 1 < cells.size()) s = s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(steps.get(i + 1)), true);
                set(level, cells.get(i), s);
            }
            return cells.size();
        }

        /** A short stub of a burnt branch out of a trunk block: straight out, out and up, or out twice. */
        private static void stub(WorldGenLevel level, RandomSource random, BlockPos trunk, Direction d) {
            // straight out, mostly; out twice now and then (no out-and-up arms: they read as cactus)
            List<Direction> steps = random.nextInt(4) == 0 ? List.of(d, d) : List.of(d);
            limb(level, trunk, steps, random.nextInt(3) == 0 ? 0 : 1);
        }

        /** The snapped top on the trunk block top: flat, tapering (a branch then a twig), or splintered (twigs). */
        private static void snappedTop(WorldGenLevel level, RandomSource random, BlockPos top) {
            switch (random.nextInt(7)) {
                case 0, 1, 2 -> {} // broken off square
                case 3, 4 -> limb(level, top, random.nextBoolean() ? List.of(Direction.UP, Direction.UP) : List.of(Direction.UP), 1);
                default -> { // splinters: a twig or two standing up off the jagged break
                    limb(level, top, List.of(Direction.UP), 1);
                    if (random.nextBoolean()) limb(level, top.above(), List.of(Direction.UP), 1);
                }
            }
        }

        /** Root flares: Charred Branch running out along the ground from a trunk's foot. */
        private static void roots(WorldGenLevel level, RandomSource random, BlockPos foot, int n) {
            for (int i = 0; i < n; i++) {
                Direction d = randomSide(random);
                BlockPos r = foot.relative(d);
                if (free(level, r) && sturdy(level, r.below())) limb(level, foot, List.of(d), random.nextInt(2));
            }
        }

        /** 1-2 blocks, often splintered on top, sometimes with root flares. */
        private static boolean stump(WorldGenLevel level, RandomSource random, BlockPos c) {
            int h = random.nextInt(3) == 0 ? 2 : 1;
            for (int y = 0; y < h; y++) if (free(level, c.above(y))) set(level, c.above(y), log(Direction.Axis.Y));
            if (!level.getBlockState(c).is(CHARRED_LOG.get())) return false;
            BlockPos top = c.above(h - 1);
            if (level.getBlockState(top).is(CHARRED_LOG.get()) && random.nextInt(5) < 3) limb(level, top, List.of(Direction.UP), 1);
            if (random.nextInt(4) == 0) roots(level, random, c, 1 + random.nextInt(2));
            return true;
        }

        /** A dead standing trunk h high with a snapped top and stubs of branches; leaning towards lean when it is set. */
        private static boolean snag(WorldGenLevel level, RandomSource random, BlockPos c, int h, Direction lean) {
            int every = 3 + random.nextInt(2);
            BlockPos p = c, last = null;
            List<BlockPos> trunk = new ArrayList<>();
            for (int y = 0; y < h; y++) {
                if (lean != null && y >= 2 && (y - 2) % every == every - 1) {
                    // the lean: one more block beside this one at the same height, the trunk carries on from there
                    if (!free(level, p.relative(lean))) break;
                    set(level, p, log(lean.getAxis()));
                    p = p.relative(lean);
                }
                if (!free(level, p)) break;
                set(level, p, log(Direction.Axis.Y));
                trunk.add(last = p);
                p = p.above();
            }
            if (last == null) return false;
            int got = trunk.size();
            if (got >= 4) // stubs of branches on the upper two thirds, below the top
                for (int i = random.nextInt(got >= 7 ? 3 : 2); i > 0; i--)
                    stub(level, random, trunk.get(got / 3 + random.nextInt(got - 1 - got / 3)), randomSide(random));
            snappedTop(level, random, last);
            if (random.nextInt(3) == 0) roots(level, random, c, 1 + random.nextInt(2));
            return true;
        }

        /**
         * A big old tree: a 2 x 2 butt 2-3 high burnt through unevenly, one trunk on up to 10-13, three to five charred limbs
         * reaching out and a little up from the upper half, a forked top and root flares round the foot.
         */
        private static boolean bigTree(WorldGenLevel level, RandomSource random, BlockPos c) {
            Direction a = randomSide(random), b = a.getClockWise();
            BlockPos[] butt = {c, c.relative(a), c.relative(b), c.relative(a).relative(b)};
            int base = 2 + random.nextInt(2), h = 10 + random.nextInt(4);
            for (BlockPos q : butt) if (!free(level, q) || !sturdy(level, q.below()) || !free(level, q.above(base))) return false;
            for (BlockPos q : butt) for (int y = 0; y < base; y++) set(level, q.above(y), log(Direction.Axis.Y));
            BlockPos top = c;
            for (int y = base; y < h && free(level, c.above(y)); y++) set(level, top = c.above(y), log(Direction.Axis.Y));
            // the butt burnt through unevenly: one corner a block higher, splintered
            BlockPos corner = butt[1 + random.nextInt(3)].above(base);
            if (free(level, corner)) {
                set(level, corner, log(Direction.Axis.Y));
                limb(level, corner, List.of(Direction.UP), 1);
            }
            int th = top.getY() - c.getY();
            for (int i = 3 + random.nextInt(3); i > 0; i--) {
                Direction d = randomSide(random);
                BlockPos from = c.above(th / 2 + random.nextInt(Math.max(1, th / 2 - 1)));
                // reaching out and a little up: a step up only between two steps out, so it slants instead of bending upright
                List<Direction> steps = random.nextBoolean() ? List.of(d, d, Direction.UP, d) : List.of(d, Direction.UP, d, d);
                limb(level, from, steps.subList(0, 2 + random.nextInt(3)), 1);
            }
            // the forked top: a leader carries on up, a second fork bends out
            limb(level, top, List.of(Direction.UP, Direction.UP), 1);
            Direction f = randomSide(random);
            limb(level, top.below(), List.of(f, Direction.UP, f), 1);
            for (BlockPos q : butt) if (random.nextInt(3) == 0) roots(level, random, q, 1);
            return true;
        }

        /**
         * A trunk lying flat, 4-9 long, on its own: it stops before it would touch another charred log and spans no dip
         * wider than a block. Broken branches stick up from it, the crown end tapers; half buried ones have Wood Ash drifted
         * against both sides. Sometimes the stump it broke from stands a block or two off its foot.
         */
        private static boolean fallen(WorldGenLevel level, RandomSource random, BlockPos c) {
            Direction d = randomSide(random), side = d.getClockWise();
            int len = 4 + random.nextInt(4) + (random.nextInt(3) == 0 ? random.nextInt(3) : 0);
            List<BlockPos> cells = new ArrayList<>();
            int gap = 0;
            for (int i = 0; i < len; i++) {
                BlockPos p = c.relative(d, i);
                if (!free(level, p) || touchesLog(level, p)) break;
                if (sturdy(level, p.below())) gap = 0;
                else if (++gap > 1 || i == 0) break;
                cells.add(p);
            }
            while (!cells.isEmpty() && !sturdy(level, cells.getLast().below())) cells.removeLast();
            if (cells.size() < 3) return false;
            for (BlockPos p : cells) set(level, p, log(d.getAxis()));
            boolean buried = random.nextInt(3) == 0;
            BlockState drift = EarthBlocks.WOOD_ASH_SLAB.get().defaultBlockState();
            for (BlockPos p : cells) {
                if (!buried) continue;
                for (Direction s : new Direction[] {side, side.getOpposite()}) {
                    BlockPos q = p.relative(s);
                    if (random.nextInt(4) > 0 && level.getBlockState(q).isAir() && sturdy(level, q.below())) set(level, q, drift);
                }
            }
            // broken branches standing up off the trunk, the crown end tapering on
            for (int i = random.nextInt(3); i > 0; i--) {
                BlockPos p = cells.get(1 + random.nextInt(cells.size() - 1));
                limb(level, p, random.nextBoolean() ? List.of(Direction.UP) : List.of(random.nextBoolean() ? side : side.getOpposite()), 1);
            }
            BlockPos crown = cells.getLast();
            if (random.nextBoolean() && sturdy(level, crown.relative(d).below())) limb(level, crown, List.of(d, d), 1);
            if (random.nextInt(5) < 2) { // the stump it broke from
                BlockPos s = ground(level, c.relative(d.getOpposite(), 2 + random.nextInt(2)));
                if (s != null && Math.abs(s.getY() - c.getY()) <= 1 && !touchesLog(level, s)) stump(level, random, s);
            }
            return true;
        }

        /** Another charred log next to p on any side: fallen trunks never weld onto stumps or snags. */
        private static boolean touchesLog(WorldGenLevel level, BlockPos p) {
            for (Direction s : Direction.values()) if (level.getBlockState(p.relative(s)).is(CHARRED_LOG.get())) return true;
            return false;
        }
    }
}
