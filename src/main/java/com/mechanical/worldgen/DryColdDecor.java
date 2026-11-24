package com.mechanical.worldgen;

import static com.mechanical.worldgen.MountainDecor.clear;
import static com.mechanical.worldgen.MountainDecor.dryNatural;
import static com.mechanical.worldgen.MountainDecor.sq;
import static com.mechanical.worldgen.MountainDecor.top;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Deadwood;
import com.mechanical.EarthBlocks;
import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Small things in dry, warm land and on the cold plains (docs/terrain-plan.md phase 4), placed by
 * scripts/gen_land_decor.py. Dry: dead trees among dead bushes, skeletons half buried in the sand, small rock overhangs
 * and dried-out creek beds. Cold: blocks of ice lying on the snow, frost-split boulders and mammoth tusks sticking out of
 * the Loess of the Mammoth Steppe. Termite mounds have their own feature already. Ice is Packed or Blue Ice, which the
 * spring thaw (Thaw) leaves alone.
 */
final class DryColdDecor extends StoneAgeFeature {
    enum Kind { DEAD_TREE, SAND_SKELETON, OVERHANG, DRY_CREEK, ICE_BLOCKS, FROST_SPLIT, MAMMOTH_TUSKS }

    static final DryColdDecor DEAD_TREE = new DryColdDecor(Kind.DEAD_TREE), SAND_SKELETON = new DryColdDecor(Kind.SAND_SKELETON),
            OVERHANG = new DryColdDecor(Kind.OVERHANG), DRY_CREEK = new DryColdDecor(Kind.DRY_CREEK), ICE_BLOCKS = new DryColdDecor(Kind.ICE_BLOCKS),
            FROST_SPLIT = new DryColdDecor(Kind.FROST_SPLIT), MAMMOTH_TUSKS = new DryColdDecor(Kind.MAMMOTH_TUSKS);
    private static final Block[] SPLIT_STONES = {Blocks.GRANITE, Blocks.STONE, Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE};

    private final Kind kind;
    private final MapCodec<DryColdDecor> codec;

    private DryColdDecor(Kind kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<DryColdDecor> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!Decor.noStructure(level, c)) return false;
        // the creeks are laid over the whole chunk, wherever the origin fell
        if (kind != Kind.DRY_CREEK && (!dryNatural(level, c.below()) || !level.getBlockState(c).canBeReplaced())) return false;
        BlockPos at = switch (kind) {
            case DEAD_TREE -> deadTree(level, random, c);
            case SAND_SKELETON -> level.getBlockState(c.below()).is(BlockTags.SAND) ? OpenDecor.bones(level, random, c, true) : null;
            case OVERHANG -> overhang(level, random, c);
            case DRY_CREEK -> dryCreek(level, random, c);
            case ICE_BLOCKS -> iceBlocks(level, random, c);
            case FROST_SPLIT -> frostSplit(level, random, c);
            case MAMMOTH_TUSKS -> tusks(level, random, c);
        };
        if (at != null) Decor.logPlaced(kind.name().toLowerCase(), at);
        return at != null;
    }

    // ------------------------------------------------------------------ dry and warm

    /**
     * A dead tree, bleached silver-grey (Deadwood, no bark left): a trunk 3-6 high, sometimes leaning, its roots running
     * out over the ground; limbs that leave it thick (Deadwood) and thin out to Deadwood Branch and Deadwood Twig, climbing
     * as they reach out, forking now and then; a jagged broken top or a last thin leader. In the savanna the acacia's
     * shape: a short trunk forking into two or three leaders that spread into a flat crown. Coarse Dirt round its foot,
     * dead bushes and dry grass, fallen twigs and now and then a fallen limb.
     */
    private static BlockPos deadTree(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 1)) return null;
        boolean savanna = level.getBiome(c).is(net.minecraft.tags.BiomeTags.IS_SAVANNA);
        int h = savanna ? 2 + random.nextInt(2) : 3 + random.nextInt(4);
        for (int y = 0; y <= h + 4; y++) if (!level.getBlockState(c.above(y)).canBeReplaced()) return null;
        if (ForestDecor.woodNear(level, c, 4) != null) return null;
        Limbs t = new Limbs(level);
        // the trunk, leaning a block over on a tall one now and then
        Direction lean = !savanna && h >= 5 && random.nextInt(3) == 0 ? randomSide(random) : null;
        BlockPos p = c;
        for (int y = 0; y < h; y++) {
            if (lean != null && y == h / 2) {
                t.log(p, lean.getAxis());
                p = p.relative(lean);
            }
            t.log(p, Direction.Axis.Y);
            if (y < h - 1) p = p.above();
        }
        BlockPos top = p;
        Direction[] sides = shuffled(random);
        // the crown: two or three leaders forking out of the top (Deadwood, climbing out at a slant; flatter in the
        // savanna), each spreading into limbs that thin to twigs and fork; a low limb or two off the trunk
        int leaders = 2 + random.nextInt(2);
        for (int i = 0; i < leaders; i++) {
            Direction d = sides[i];
            List<Direction> lead = savanna ? List.of(d, Direction.UP, d) : random.nextBoolean() ? List.of(Direction.UP, d, Direction.UP) : List.of(d, Direction.UP, Direction.UP);
            BlockPos end = t.limb(top, lead, 1, 0);
            if (end == null) continue;
            for (Direction s2 : new Direction[] {d, d.getClockWise(), d.getCounterClockWise()})
                if (s2 == d || random.nextInt(3) > 0) t.branchOut(random, end, s2, 3 + random.nextInt(3), 0, savanna ? 5 : 3);
        }
        for (int i = 0, n = random.nextInt(3); i < n && h >= 3; i++)
            t.branchOut(random, t.trunkAt(1 + random.nextInt(h - 2)), sides[(leaders + i) % 4], 2 + random.nextInt(3), 0, 3);
        if (random.nextInt(3) == 0 && level.getBlockState(top.above()).isAir()) set(level, top.above(), Deadwood.BROKEN_DEADWOOD.get().defaultBlockState());
        // roots: thin ones running out over the ground and down into it
        for (Direction d : sides) {
            if (random.nextInt(4) == 0 || !level.getBlockState(c.relative(d)).canBeReplaced()) continue;
            BlockPos end = t.limb(c, random.nextBoolean() ? List.of(d) : List.of(d, d), 0, 0);
            if (end != null && natural(level.getBlockState(end.below()))) t.sink(end);
        }
        t.finish();
        // the ground: a ragged patch of coarse dirt, dry plants, twigs, now and then a fallen limb
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos g = top(level, c.getX() + dx, c.getZ() + dz).below();
                if (dx * dx + dz * dz > 10 || noise.get(dx * 0.5, dz * 0.5) < -0.1 || !dryNatural(level, g) || !level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD)
                        || !level.getBlockState(g.above()).canBeReplaced()) continue;
                set(level, g, Blocks.COARSE_DIRT.defaultBlockState());
            }
        dryPlants(level, random, c, 3, 7);
        for (int i = 0; i < 3; i++) {
            BlockPos q = top(level, c.getX() + random.nextInt(7) - 3, c.getZ() + random.nextInt(7) - 3);
            if (level.getBlockState(q).isAir()) onGround(level, q, Mechanical.TWIGS.get().defaultBlockState());
        }
        if (random.nextInt(3) == 0) { // a limb that came down, lying in the dust
            Direction d = randomSide(random);
            BlockPos g = top(level, c.relative(d, 3).relative(d.getClockWise(), random.nextInt(3) - 1));
            if (Math.abs(g.getY() - c.getY()) <= 1 && level.getBlockState(g.below()).isFaceSturdy(level, g.below(), Direction.UP)) {
                Limbs f = new Limbs(level);
                f.limb(g.relative(d.getCounterClockWise()), wander(random, d.getClockWise(), 3, 0), 0, 1);
                f.finish();
            }
        }
        return c;
    }

    /** Out toward d for n steps, a step up now and then (never twice running, never first), now and then one step aside. */
    private static List<Direction> wander(RandomSource random, Direction d, int n, double climb) {
        List<Direction> steps = new ArrayList<>();
        boolean aside = false;
        for (int i = 0; i < n; i++) {
            Direction last = steps.isEmpty() ? null : steps.get(steps.size() - 1);
            if (i > 0 && last != Direction.UP && random.nextDouble() < climb) steps.add(Direction.UP);
            else if (!aside && i > 1 && random.nextInt(5) == 0) {
                steps.add(random.nextBoolean() ? d.getClockWise() : d.getCounterClockWise());
                aside = true;
            } else steps.add(d);
        }
        return steps;
    }

    private static Direction[] shuffled(RandomSource random) {
        List<Direction> l = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        java.util.Collections.shuffle(l, new java.util.Random(random.nextLong()));
        return l.toArray(new Direction[0]);
    }

    /**
     * A dead tree's wood as it is laid: Deadwood logs and the joined-up thin pieces (Deadwood Branch, Deadwood Twig), each
     * thin piece remembering which neighbours it joins, so {@link #finish} can set its arms.
     */
    private static final class Limbs {
        private final WorldGenLevel level;
        private final List<BlockPos> trunk = new ArrayList<>();
        private final java.util.Map<BlockPos, Block> thin = new java.util.LinkedHashMap<>();
        private final java.util.Map<BlockPos, java.util.EnumSet<Direction>> joins = new java.util.HashMap<>();

        Limbs(WorldGenLevel level) {
            this.level = level;
        }

        void log(BlockPos p, Direction.Axis axis) {
            set(level, p, ForestDecor.pillar(Deadwood.DEADWOOD.get(), axis));
            if (axis == Direction.Axis.Y) trunk.add(p);
        }

        BlockPos trunkAt(int i) {
            return trunk.get(Math.min(i, trunk.size() - 1));
        }

        /** A limb from the block at from: logs Deadwood cells first, the last twigs Twig, Branch between. Its end, or null if none laid. */
        BlockPos limb(BlockPos from, List<Direction> steps, int logs, int twigs) {
            List<BlockPos> cells = new ArrayList<>();
            BlockPos p = from;
            for (Direction d : steps) {
                p = p.relative(d);
                BlockState s = level.getBlockState(p);
                if (!s.canBeReplaced() || !s.getFluidState().isEmpty() || thin.containsKey(p)) break;
                cells.add(p);
            }
            if (cells.isEmpty()) return null;
            BlockPos prev = from;
            for (int i = 0; i < cells.size(); i++) {
                BlockPos q = cells.get(i);
                Direction d = steps.get(i);
                if (i < logs) set(level, q, ForestDecor.pillar(Deadwood.DEADWOOD.get(), d.getAxis()));
                else {
                    thin.put(q, i >= cells.size() - twigs ? Deadwood.DEADWOOD_TWIG.get() : Deadwood.DEADWOOD_BRANCH.get());
                    var j = joins.computeIfAbsent(q, k -> java.util.EnumSet.noneOf(Direction.class));
                    if (thin.containsKey(prev) || level.getBlockState(prev).is(Deadwood.DEADWOOD.get())) j.add(d.getOpposite()); // a lying limb's butt end is free
                    if (thin.containsKey(prev)) joins.get(prev).add(d);
                }
                prev = q;
            }
            return prev;
        }

        /**
         * A limb out of the block at from toward d: n steps, low and wide (a step up only after two out, one in flat), the
         * last step a twig; each piece past the first may fork off a short twig to one side or up. logs: its first cells as Deadwood.
         */
        void branchOut(RandomSource random, BlockPos from, Direction d, int n, int logs, int flat) {
            List<Direction> steps = new ArrayList<>();
            int outs = 0;
            for (int i = 0; i < n; i++) {
                boolean up = outs >= 2 && random.nextInt(flat) == 0;
                steps.add(up ? Direction.UP : i == n - 1 && random.nextBoolean() ? (random.nextBoolean() ? d.getClockWise() : d.getCounterClockWise()) : d);
                outs = up ? 0 : outs + 1;
            }
            List<BlockPos> cells = new ArrayList<>();
            BlockPos p = from;
            for (Direction s : steps) cells.add(p = p.relative(s));
            if (limb(from, steps, logs, 1) == null) return;
            for (int i = Math.max(1, logs); i < cells.size() - 1; i++) {
                if (!thin.containsKey(cells.get(i)) || random.nextInt(5) > 1) continue;
                Direction f = random.nextInt(3) == 0 ? Direction.UP : random.nextBoolean() ? d.getClockWise() : d.getCounterClockWise();
                if (steps.get(i) == f || steps.get(i) == Direction.UP && f == Direction.UP) continue;
                limb(cells.get(i), random.nextBoolean() ? List.of(f) : List.of(f, f == Direction.UP ? d : Direction.UP), 0, 1);
            }
        }

        /** The thin piece at p goes on down into the ground (a root). */
        void sink(BlockPos p) {
            joins.get(p).add(Direction.DOWN);
        }

        void finish() {
            thin.forEach((p, b) -> {
                BlockState s = b.defaultBlockState();
                for (Direction d : joins.get(p)) s = s.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(d), true);
                set(level, p, s);
            });
        }
    }

    /**
     * A rock shelter (overhang): a hollow worn into the foot of a cliff by wind and frost, 5-9 wide, 2-3 deep and 3-4
     * high in its middle, its roof and back rounded in half blocks (the rock's own slabs and stairs, upside down under
     * the roof) and its lip left jutting over a dry floor of sand or grit. Inside: dead bushes at the mouth, sometimes old
     * bones or the ash of a fire someone made out of the wind. In the dry lands' cliffs (sandstone, terracotta, stone).
     */
    private static BlockPos overhang(WorldGenLevel level, RandomSource random, BlockPos c) {
        MountainDecor.Cliff k = MountainDecor.cliff(level, c, 5, 3, 6, true);
        if (k == null) return Decor.reject("overhang:no_cliff");
        Direction up = k.up(), side = up.getClockWise();
        BlockPos foot = k.foot();
        if (MountainDecor.sheer(level, foot, up) < 5) return Decor.reject("overhang:low");
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        double hw = 2.6 + random.nextDouble() * 1.8, hd = 2.2 + random.nextDouble() * 1.0, hh = 2.1 + random.nextDouble() * 0.8;
        // the hollow's centre: at the foot of the face, a little above the floor
        double cx = foot.getX() + 0.5 + up.getStepX() * 0.6, cz = foot.getZ() + 0.5 + up.getStepZ() * 0.6, cy = foot.getY() + 0.9;
        int uu = up.getStepX(), uw = up.getStepZ(), su = side.getStepX(), sw = side.getStepZ();
        Shaper.Density rock = (x, y, z) -> { // above 0 where rock stays
            double dx = x - cx, dz = z - cz, a = dx * uu + dz * uw, s = dx * su + dz * sw, t = y - cy;
            if (t < -0.9) return 1; // the floor stays
            double e = Math.sqrt(sq(s / hw) + sq(a / hd) + sq(Math.max(t, 0) / hh) + sq(Math.min(t, 0) / 1.2)) + 0.12 * noise.get(x * 0.5, y * 0.5, z * 0.5);
            return (e - 1) * 1.5;
        };
        int r = (int) Math.ceil(hw) + 1, carved = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        List<BlockPos> floor = new ArrayList<>();
        for (int s = -r; s <= r; s++)
            for (int a = 0; a <= (int) Math.ceil(hd) + 1; a++)
                for (int y = 0; y <= (int) Math.ceil(hh) + 2; y++) {
                    m.set(foot.getX() + uu * a + su * s, foot.getY() + y, foot.getZ() + uw * a + sw * s);
                    BlockState here = level.getBlockState(m);
                    if (!SmoothGround.rock(here) && !here.is(BlockTags.TERRACOTTA)) continue;
                    int bits = 0;
                    for (int o = 0; o < 8; o++)
                        if (rock.at(m.getX() + 0.25 + 0.5 * (o & 1), m.getY() + 0.25 + 0.5 * (o >> 2), m.getZ() + 0.25 + 0.5 * ((o >> 1) & 1)) > 0) bits |= 1 << o;
                    if (bits == 0xFF) continue;
                    BlockState st = Integer.bitCount(bits) <= 1 ? AIR : Shaper.shape(bits, here.getBlock(), here.is(BlockTags.TERRACOTTA) ? Blocks.RED_SANDSTONE : Blocks.STONE);
                    BlockPos p = m.immutable();
                    set(level, p, st == null ? AIR : st);
                    carved++;
                    if (y == 0 && (st == null || st.isAir())) floor.add(p);
                }
        if (carved < 12) return Decor.reject("overhang:small");
        // the floor: dry grit and sand, a dead bush or two at the mouth, old bones or a fire's ash now and then
        boolean red = k.rock().is(BlockTags.TERRACOTTA) || k.rock().is(Blocks.RED_SANDSTONE);
        for (BlockPos p : floor) {
            BlockPos g = p.below();
            if (natural(level.getBlockState(g))) set(level, g, (random.nextInt(3) == 0 ? Blocks.COARSE_DIRT : red ? Blocks.RED_SAND : Blocks.SAND).defaultBlockState());
        }
        if (!floor.isEmpty() && random.nextInt(3) == 0) {
            BlockPos f = floor.get(random.nextInt(floor.size()));
            set(level, f.below(), ScorchedBarrens.WOOD_ASH.get().defaultBlockState());
            if (random.nextBoolean()) set(level, f, ForestDecor.pillar(ScorchedBarrens.CHARRED_LOG.get(), side.getAxis()));
        } else if (!floor.isEmpty() && random.nextBoolean()) onGround(level, floor.get(random.nextInt(floor.size())), Mechanical.BONE_PILE.get().defaultBlockState());
        dryPlants(level, random, foot.relative(up.getOpposite()), 3, 5);
        Decor.logPlaced("overhang_dir_" + Decor.viewAngle(up.getOpposite()), foot.above(1));
        return foot;
    }

    /**
     * A dried-out creek bed (a wash): long, shallow beds winding for a hundred blocks and more across the dry land, laid
     * by every chunk for its own columns from one world-wide pattern (the creek runs along the zero line of a smooth
     * noise; a second, slower noise says where creeks are at all, so each is a long stretch that narrows to nothing at
     * both ends). The channel a block below the land and 1-3 wide, its banks sloping half a step down (Gravel Slab, now
     * and then the ground's own: Sand Slab, Grass Slab ...) so it lies soft in the land. Its bed gravel down the middle and
     * the ground's sand or dirt in drifts, cracked mud (Packed Mud) in the low places, a cobble here and there and pebbles
     * on top; dead bushes and dry grass along the banks, now and then a bleached branch stranded in it. Never water: only
     * in biomes without rain, on level ground, never within 9 blocks of water.
     */
    private static BlockPos dryCreek(WorldGenLevel level, RandomSource random, BlockPos c) {
        Creeks k = Creeks.of(level.getSeed());
        int x0 = c.getX() & ~15, z0 = c.getZ() & ~15;
        // the land before anything is dug (2 round the chunk, for the slope)
        int[][] h = new int[20][20];
        for (int i = 0; i < 20; i++) for (int j = 0; j < 20; j++) h[i][j] = top(level, x0 + i - 2, z0 + j - 2).getY();
        List<BlockPos> bed = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                int x = x0 + i, z = z0 + j;
                double in = k.inside(x + 0.5, z + 0.5); // > 0 in the channel, 0..-1 on its sloping bank, below -1 outside
                if (in < -2.2) continue;
                int y = h[i + 2][j + 2];
                BlockPos g = new BlockPos(x, y - 1, z);
                if (level.getBiome(g).value().hasPrecipitation() || !creekGround(level, g, 2)) continue;
                int slope = Math.max(Math.max(Math.abs(h[i][j + 2] - y), Math.abs(h[i + 4][j + 2] - y)), Math.max(Math.abs(h[i + 2][j] - y), Math.abs(h[i + 2][j + 4] - y)));
                if (slope > 2 || waterNear(level, m, x, y, z)) continue;
                BlockState ground = level.getBlockState(g);
                if (in < -1) { // beyond the bank: its dry tufts
                    if (random.nextInt(9) == 0) dryTuft(level, random, g.above());
                    continue;
                }
                clear(level, g.above());
                if (in > 0.55) { // the channel, a block down
                    set(level, g, AIR);
                    set(level, g.below(), creekBed(random, k.noise, g.below(), ground, in));
                    if (random.nextInt(5) == 0) rock(level, random, g, Mechanical.LOOSE_STONE.get()); // the pebbles the water rolled
                    bed.add(g);
                } else { // its sloping bank half a block down: gravel near the channel, now and then the ground's own
                    Block slab = SmoothGround.slab(ground.getBlock());
                    boolean gravel = in > 0.1 || slab == Blocks.AIR || k.noise.get(x * 0.5 + 9, z * 0.5) > 0;
                    set(level, g, (gravel ? EarthBlocks.GRAVEL_SLAB.get() : slab).defaultBlockState());
                }
            }
        if (bed.isEmpty()) return Decor.reject("dry_creek:none_here");
        if (random.nextInt(6) == 0) { // a bleached branch the last flood left
            BlockPos g = bed.get(random.nextInt(bed.size()));
            Direction d = randomSide(random);
            Limbs f = new Limbs(level);
            f.limb(g.relative(d.getOpposite()), List.of(d, d, d.getClockWise()), 0, 1);
            f.finish();
        }
        return bed.get(bed.size() / 2);
    }

    /** Water on or in the ground within 9 of a column (sampled round it). */
    private static boolean waterNear(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z) {
        for (int dx = -9; dx <= 9; dx += 3)
            for (int dz = -9; dz <= 9; dz += 3) {
                BlockPos t = top(level, x + dx, z + dz);
                if (Math.abs(t.getY() - y) <= 3 && (!level.getFluidState(t).isEmpty() || !level.getFluidState(t.below()).isEmpty())) return true;
            }
        return false;
    }

    /** A dead bush, dry grass or tall dry grass at p if it can stand there. */
    private static void dryTuft(WorldGenLevel level, RandomSource random, BlockPos p) {
        if (!level.getBlockState(p).isAir()) return;
        int k = random.nextInt(4);
        if (k == 0 && level.getBlockState(p.above()).isAir() && Blocks.TALL_DRY_GRASS.defaultBlockState().canSurvive(level, p))
            set(level, p, Blocks.TALL_DRY_GRASS.defaultBlockState());
        else {
            BlockState plant = (k == 1 ? Blocks.DEAD_BUSH : Blocks.SHORT_DRY_GRASS).defaultBlockState();
            if (plant.canSurvive(level, p)) set(level, p, plant);
        }
    }

    /**
     * The world's dry creeks: a creek runs along the zero line of a smooth noise (wiggled by a finer one), as wide as a
     * third noise says, and only where a slow fourth one is high enough, tapering to nothing at the edges of that.
     */
    static final class Creeks {
        private static volatile Creeks last;
        final long seed;
        final net.minecraft.world.level.levelgen.synth.SimplexNoise line, wiggle, mask, noise;

        private Creeks(long seed) {
            this.seed = seed;
            var r = new net.minecraft.world.level.levelgen.LegacyRandomSource(seed ^ 0x6372_6565_6b73L);
            line = new net.minecraft.world.level.levelgen.synth.SimplexNoise(r);
            wiggle = new net.minecraft.world.level.levelgen.synth.SimplexNoise(r);
            mask = new net.minecraft.world.level.levelgen.synth.SimplexNoise(r);
            noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(r);
        }

        static Creeks of(long seed) {
            Creeks k = last;
            if (k == null || k.seed != seed) last = k = new Creeks(seed);
            return k;
        }

        private double field(double x, double z) {
            return line.get(x / 90, z / 90) + 0.3 * wiggle.get(x / 26, z / 26);
        }

        /** How far inside the channel x,z lies, in blocks: > 0 in it, 0 to -1 on its bank, below that outside. */
        double inside(double x, double z) {
            double taper = Math.min(1, (mask.get(x / 170, z / 170) - 0.42) / 0.14);
            if (taper <= 0) return -9;
            double f = field(x, z), gx = field(x + 0.5, z) - field(x - 0.5, z), gz = field(x, z + 0.5) - field(x, z - 0.5);
            double dist = Math.abs(f) / Math.max(1e-4, Math.sqrt(gx * gx + gz * gz)); // blocks to the zero line
            double half = (0.9 + 0.6 * (noise.get(x / 40, z / 40) + 1)) * taper;
            return half - dist;
        }
    }

    /** Dry natural ground at g with natural ground under it, deep enough to dig a channel of the depth into. */
    private static boolean creekGround(WorldGenLevel level, BlockPos g, int depth) {
        for (int k = 0; k <= depth; k++) if (!dryNatural(level, g.below(k))) return false;
        return level.getBlockState(g.above()).canBeReplaced();
    }

    /** The bed: drifts of gravel and the ground's own sand or dirt, cracked mud where it lies deepest, a cobble now and then. */
    private static BlockState creekBed(RandomSource random, net.minecraft.world.level.levelgen.synth.SimplexNoise noise, BlockPos p, BlockState ground, double deep) {
        double v = noise.get(p.getX() * 0.25, p.getZ() * 0.25);
        if (random.nextInt(40) == 0) return Blocks.COBBLESTONE.defaultBlockState();
        if (deep > 1.1 && v < -0.25) return Blocks.PACKED_MUD.defaultBlockState(); // cracked mud where the last pools dried
        boolean red = ground.is(Blocks.RED_SAND) || ground.is(BlockTags.TERRACOTTA);
        if (red ? v > 0.35 : deep > 0.9 || v > 0.3) return Blocks.GRAVEL.defaultBlockState(); // the gravel runs down the middle (in streaks in red sand)
        if (ground.is(BlockTags.SAND) || ground.is(BlockTags.TERRACOTTA)) return (ground.is(Blocks.RED_SAND) || ground.is(BlockTags.TERRACOTTA) ? Blocks.RED_SAND : Blocks.SAND).defaultBlockState();
        return (random.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.GRAVEL).defaultBlockState();
    }

    // ------------------------------------------------------------------ cold

    /**
     * Ice blocks on the snowy plain: broken-off chunks of old ice (Packed Ice, Blue Ice at the heart) lying tilted every
     * which way, a big one and two to four smaller, half drowned in a snow drift that the wind has shaped round them:
     * deep against their windward side, a long tail tapering away in their lee, the snow's surface smooth in layers. Only
     * where snow never melts (Thaw.coldAllYear), so the drift stays.
     */
    private static BlockPos iceBlocks(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 3, 1)) return Decor.reject("ice_blocks:not_flat");
        if (!com.mechanical.Thaw.coldAllYear(level.getLevel(), level.getBiome(c).value(), c)) return Decor.reject("ice_blocks:thaws");
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        int pieces = 3 + random.nextInt(3);
        double[][] ice = new double[pieces][]; // centre x, y, z, half sizes, turn, tip
        for (int i = 0; i < pieces; i++) {
            double big = i == 0 ? 1 : 0.45 + random.nextDouble() * 0.35, a = random.nextDouble() * Math.PI * 2, d = i == 0 ? 0 : 2.2 + random.nextDouble() * 2.8;
            ice[i] = new double[] {c.getX() + 0.5 + Math.cos(a) * d, c.getY() + 0.3 * big, c.getZ() + 0.5 + Math.sin(a) * d,
                    (1.6 + random.nextDouble() * 0.9) * big, 0.75 + random.nextDouble() * 0.35 * big, (1.2 + random.nextDouble() * 0.6) * big,
                    random.nextDouble() * Math.PI, (random.nextDouble() - 0.5) * 1.5}; // slabs of ice, thick but flat, tipped every way
        }
        Shaper.Density chunks = (x, y, z) -> {
            double best = -9;
            for (double[] k : ice) {
                double dx = x - k[0], dy = y - k[1], dz = z - k[2], cs = Math.cos(k[6]), sn = Math.sin(k[6]), ct = Math.cos(k[7]), st = Math.sin(k[7]);
                double u = dx * cs + dz * sn, w = -dx * sn + dz * cs, v = dy * ct - u * st;
                u = u * ct + dy * st;
                double e = Math.max(Math.abs(u) / k[3], Math.max(Math.abs(v) / k[4], Math.abs(w) / k[5]));
                best = Math.max(best, (1 - e) * k[4] + 0.15 * noise.get(x * 0.6, y * 0.6, z * 0.6));
            }
            return best;
        };
        int r = 7;
        Shaper.Stone stone = p -> chunks.at(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > 0.9 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE;
        var cells = Shaper.build(level, c.getX() - r, c.getY() - 1, c.getZ() - r, c.getX() + r, c.getY() + 4, c.getZ() + r, chunks, stone, Blocks.PACKED_ICE);
        if (cells.size() < 10) return Decor.reject("ice_blocks:small");
        java.util.Set<BlockPos> solid = new java.util.HashSet<>();
        for (Shaper.Cell k : cells) solid.add(k.pos());
        // the drift: snow layers by how sheltered a spot is, deep on the windward side and in a tail on the lee
        double wind = random.nextDouble() * Math.PI * 2, wx = Math.cos(wind), wz = Math.sin(wind);
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                if (solid.contains(t.below()) || !level.getBlockState(t).canBeReplaced() || Math.abs(t.getY() - c.getY()) > 1) continue;
                double drift = 0;
                for (double[] k : ice) {
                    double ex = t.getX() + 0.5 - k[0], ez = t.getZ() + 0.5 - k[2], along = ex * wx + ez * wz, across = -ex * wz + ez * wx;
                    double reach = k[3] + (along > 0 ? 4.5 : 1.2); // a long tail downwind, a short bank upwind
                    double dd = Math.sqrt(sq(along / reach) + sq(across / (k[3] * 1.3)));
                    drift = Math.max(drift, (1 - dd) * (k[4] * 2.2));
                }
                drift += 0.4 * noise.get(t.getX() * 0.3, t.getZ() * 0.3);
                int layers = (int) Math.round(drift * 4);
                if (layers <= 0) continue;
                BlockState g = level.getBlockState(t.below());
                if (g.hasProperty(net.minecraft.world.level.block.SnowyBlock.SNOWY)) set(level, t.below(), g.setValue(net.minecraft.world.level.block.SnowyBlock.SNOWY, true));
                if (layers >= 8) {
                    set(level, t, Blocks.SNOW_BLOCK.defaultBlockState());
                    layers = Math.min(7, layers - 8);
                    if (layers > 0 && level.getBlockState(t.above()).canBeReplaced() && !solid.contains(t.above())) set(level, t.above(), Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, layers));
                } else set(level, t, Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, layers));
            }
        // snow on the ice's tops
        for (BlockPos p : Shaper.tops(level, cells))
            if (random.nextBoolean()) set(level, p.above(), Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 1 + random.nextInt(2)));
        return c;
    }

    /**
     * A boulder split by frost: one rounded block 6-8 across and 2.5-3 high, sunk in the ground to its widest so only its
     * dome shows and its sides round down into the turf (half blocks of the stone's slabs and stairs), cleft right through
     * by a single straight crack that opens upward like a V, the two halves leaning a little apart but still matching;
     * grit and drifted snow in the crack, patches of a second stone, flakes of the stone shed about it. The stone is the
     * rock under the ground, or Granite, Stone, Andesite or Diorite carried there by the ice.
     */
    private static BlockPos frostSplit(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 1)) return Decor.reject("frost_split:not_flat");
        BlockState found = MountainDecor.rockUnder(level, c);
        Block stone = (found != null && found.is(BlockTags.BASE_STONE_OVERWORLD) ? found : SPLIT_STONES[random.nextInt(SPLIT_STONES.length)].defaultBlockState()).getBlock();
        Block second = MountainDecor.secondStone(stone);
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        double r = 3.1 + random.nextDouble() * 0.9, ry = 2.8 + random.nextDouble() * 0.6, rz = r * (0.85 + random.nextDouble() * 0.2);
        double ox = c.getX() + 0.5, oz = c.getZ() + 0.5, oy = c.getY() - 0.2; // sunk to its widest: no straight sides
        boolean alongX = random.nextBoolean();
        double crack = (alongX ? oz : ox) + (random.nextDouble() - 0.5) * 1.2, lean = 0.12 + random.nextDouble() * 0.08;
        Shaper.Density split = (x, y, z) -> {
            double t = Math.max(0, y - c.getY()), u = (alongX ? z : x) - crack, sgn = Math.signum(u);
            // each half leans away from the crack: sample the whole stone sheared back toward it
            double su = u - sgn * t * lean, sx = alongX ? x : crack + su, sz = alongX ? crack + su : z;
            double e = Math.pow(Math.pow(Math.abs(sx - ox) / r, 2.4) + Math.pow(Math.abs(y - oy) / ry, 2.4) + Math.pow(Math.abs(sz - oz) / rz, 2.4), 1 / 2.4);
            double body = (1 - e) * ry + 0.1 * noise.get(x * 0.3, y * 0.3, z * 0.3);
            return Math.min(body, Math.abs(u) - 0.3 - 0.2 * t); // the cleft, opening upward
        };
        int ir = (int) Math.ceil(r) + 3;
        for (int dx = -ir; dx <= ir; dx++) for (int dz = -ir; dz <= ir; dz++) clear(level, top(level, c.getX() + dx, c.getZ() + dz));
        var cells = Shaper.build(level, c.getX() - ir, c.getY() - 2, c.getZ() - ir, c.getX() + ir, c.getY() + (int) Math.ceil(ry) + 2, c.getZ() + ir, split,
                p -> noise.get(p.getX() * 0.3, p.getY() * 0.3 + 17, p.getZ() * 0.3) > 0.45 ? second : stone, null);
        if (cells.stream().filter(k -> k.pos().getY() >= c.getY()).count() < 15) return Decor.reject("frost_split:small");
        // grit along the crack's floor, frost and snow drifted into it
        for (int t = -ir; t <= ir; t++) {
            int ck = (int) Math.floor(crack);
            BlockPos g = top(level, alongX ? c.getX() + t : ck, alongX ? ck : c.getZ() + t);
            if (Math.abs(g.getY() - c.getY()) > 1 || !level.getBlockState(g).canBeReplaced()) continue;
            if (dryNatural(level, g.below()) && !SmoothGround.rock(level.getBlockState(g.below()))) set(level, g.below(), Blocks.GRAVEL.defaultBlockState());
            if (MountainDecor.cold(level, g) && level.getBlockState(g).isAir())
                set(level, g, Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 3 + random.nextInt(4)));
        }
        // flakes and splinters shed about it
        for (int i = 0; i < 8; i++) {
            BlockPos t = top(level, c.getX() + random.nextInt(11) - 5, c.getZ() + random.nextInt(11) - 5);
            if (!dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced()) continue;
            if (random.nextInt(3) == 0) set(level, t.below(), Blocks.GRAVEL.defaultBlockState());
            else if (random.nextInt(3) == 0) { BlockState f = Shaper.shape(0x0F, stone, Blocks.STONE); clear(level, t); set(level, t, f); }
            else rock(level, random, t, Decor.looseFor(stone.defaultBlockState()));
        }
        return c;
    }


    /**
     * Mammoth tusks out of the Loess (Mammoth Steppe): a pair of curved Mammoth Tusks, each two blocks tall, rising out of
     * a patch of bare Loess and curving forward, the skull a lump of bone half buried between their roots, the ribs
     * (Rib Cage, sunk) showing behind it in a line; sometimes one tusk broken off. Bone Piles about.
     */
    private static BlockPos tusks(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 1)) return null;
        Direction face = randomSide(random), side = face.getClockWise(), back = face.getOpposite();
        // bare loess about it, the turf blown off
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 6 + random.nextInt(8) || random.nextInt(5) < 3) continue; // specks, not a patch
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                if (!dryNatural(level, t.below()) || !level.getBlockState(t.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) continue;
                clear(level, t);
                set(level, t.below(), (random.nextInt(5) == 0 ? Blocks.COARSE_DIRT : EarthBlocks.LOESS.get()).defaultBlockState());
            }
        BlockState tusk = Structures.MAMMOTH_TUSK.get().defaultBlockState().setValue(OldBonesBlock.FACING, face);
        boolean broken = random.nextInt(4) == 0;
        for (int sgn : new int[] {-1, 1}) {
            BlockPos root = top(level, c.relative(side, sgn));
            if (Math.abs(root.getY() - c.getY()) > 1 || !level.getBlockState(root).canBeReplaced()) continue;
            set(level, root, tusk.setValue(OldBonesBlock.LOW, true));
            if (!(broken && sgn == 1) && level.getBlockState(root.above()).canBeReplaced()) set(level, root.above(), tusk.setValue(OldBonesBlock.LOW, false));
        }
        // the skull between the roots, and behind it the spine and ribs, half buried
        set(level, c.below(), Blocks.BONE_BLOCK.defaultBlockState());
        set(level, c.relative(back).below(), Blocks.BONE_BLOCK.defaultBlockState());
        onGround(level, c.relative(back), Mechanical.BONE_PILE.get().defaultBlockState());
        BlockState ribs = Structures.RIB_CAGE.get().defaultBlockState().setValue(OldBonesBlock.FACING, face).setValue(OldBonesBlock.LOW, true);
        for (int i = 3; i <= 5; i++) {
            BlockPos p = top(level, c.relative(back, i));
            if (Math.abs(p.getY() - c.getY()) > 1 || !level.getBlockState(p).canBeReplaced() || random.nextInt(4) == 0) continue;
            clear(level, p);
            set(level, p.below(), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, face.getAxis()));
            set(level, p, ribs);
        }
        for (int i = 0; i < 3; i++) {
            BlockPos p = top(level, c.getX() + random.nextInt(9) - 4, c.getZ() + random.nextInt(9) - 4);
            if (level.getBlockState(p).isAir()) onGround(level, p, Mechanical.BONE_PILE.get().defaultBlockState());
        }
        return c;
    }

    /** Dead Bushes and dry grass on dry ground around c. */
    private static void dryPlants(WorldGenLevel level, RandomSource random, BlockPos c, int r, int tries) {
        for (int i = 0; i < tries; i++) {
            BlockPos p = top(level, c.getX() + random.nextInt(2 * r + 1) - r, c.getZ() + random.nextInt(2 * r + 1) - r);
            BlockState s = (random.nextInt(3) == 0 ? Blocks.SHORT_DRY_GRASS : Blocks.DEAD_BUSH).defaultBlockState();
            if (level.getBlockState(p).isAir() && s.canSurvive(level, p)) set(level, p, s);
        }
    }
}
