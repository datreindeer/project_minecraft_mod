package com.mechanical.worldgen;

import static com.mechanical.worldgen.MountainDecor.clear;
import static com.mechanical.worldgen.MountainDecor.dryNatural;
import static com.mechanical.worldgen.MountainDecor.sq;
import static com.mechanical.worldgen.MountainDecor.top;

import java.util.ArrayList;
import java.util.List;

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
        if (!dryNatural(level, c.below()) || !level.getBlockState(c).canBeReplaced() || !Decor.noStructure(level, c)) return false;
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
     * A dead tree, dry and grey: a barkless trunk 3-6 high, its roots flaring out flush with the ground, two to four bare
     * crooked branches stepping out and up from its top half, ending in thin twigs (fence posts), the broken leader a
     * twig too. Coarse Dirt at its foot, Dead Bushes and dry grass round about, sometimes a fallen branch. Stripped Acacia
     * in the savanna, bleached Pale Oak or grey Oak elsewhere.
     */
    private static BlockPos deadTree(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 1, 1)) return null;
        boolean savanna = level.getBiome(c).is(net.minecraft.tags.BiomeTags.IS_SAVANNA);
        boolean pale = !savanna && random.nextInt(3) > 0;
        Block wood = savanna ? Blocks.STRIPPED_ACACIA_LOG : pale ? Blocks.STRIPPED_PALE_OAK_LOG : Blocks.STRIPPED_OAK_LOG;
        BlockState twig = (savanna ? Blocks.ACACIA_FENCE : pale ? Blocks.PALE_OAK_FENCE : Blocks.OAK_FENCE).defaultBlockState();
        int h = 3 + random.nextInt(4);
        for (int y = 0; y <= h + 3; y++) if (!level.getBlockState(c.above(y)).canBeReplaced()) return null;
        clear(level, c);
        set(level, c.below(), Blocks.COARSE_DIRT.defaultBlockState());
        for (int y = 0; y < h; y++) set(level, c.above(y), ForestDecor.pillar(wood, Direction.Axis.Y));
        List<BlockPos> twigs = new ArrayList<>();
        // roots flaring out, sunk flush with the ground
        for (Direction d : Direction.Plane.HORIZONTAL)
            if (random.nextInt(3) > 0 && natural(level.getBlockState(c.below().relative(d)))) set(level, c.below().relative(d), ForestDecor.pillar(wood, d.getAxis()));
        // the branches, from the top half of the trunk, each its own way
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        java.util.Collections.shuffle(sides, new java.util.Random(random.nextLong()));
        int branches = 2 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            Direction d = sides.get(i % 4);
            BlockPos p = c.above(h - 1 - (i == 0 ? 0 : random.nextInt(Math.max(1, h / 2))));
            // a thick stub of the branch now and then, then thin wood (fence posts joined up) stepping out and up
            if (random.nextInt(3) == 0 && level.getBlockState(p.relative(d)).canBeReplaced()) {
                p = p.relative(d);
                set(level, p, ForestDecor.pillar(wood, d.getAxis()));
            }
            int n = 3 + random.nextInt(3);
            Direction bend = random.nextBoolean() ? d.getClockWise() : d.getCounterClockWise();
            for (int step = 0; step < n; step++) {
                boolean up = step % 2 == 1 || step > 1 && random.nextInt(3) == 0; // out, up, out, up: a crooked rising twig
                BlockPos q = up ? p.above() : p.relative(step == n - 1 && random.nextInt(3) == 0 ? bend : d);
                if (!level.getBlockState(q).canBeReplaced()) break;
                set(level, q, twig);
                twigs.add(q);
                p = q;
            }
        }
        // the broken leader
        BlockPos lead = c.above(h);
        for (int i = 0, n = random.nextInt(3); i < n && level.getBlockState(lead.above(i)).canBeReplaced(); i++) {
            set(level, lead.above(i), twig);
            twigs.add(lead.above(i));
        }
        for (BlockPos t : twigs) set(level, t, Block.updateFromNeighbourShapes(level.getBlockState(t), level, t));
        if (random.nextInt(3) == 0) { // a branch that came down
            Direction d = randomSide(random);
            List<BlockPos> cells = ForestDecor.logCells(level, top(level, c.relative(d, 2).relative(d.getClockWise(), 1)), d.getClockWise(), 2, false);
            if (cells.size() == 2) ForestDecor.lay(level, cells, wood, d.getClockWise().getAxis());
        }
        for (int i = 0; i < 4; i++) {
            BlockPos g = top(level, c.getX() + random.nextInt(5) - 2, c.getZ() + random.nextInt(5) - 2).below();
            if (dryNatural(level, g) && level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD) && level.getBlockState(g.above()).canBeReplaced())
                set(level, g, Blocks.COARSE_DIRT.defaultBlockState());
        }
        dryPlants(level, random, c, 3, 8);
        return c;
    }

    /**
     * A small rock overhang: a shelf of the rock 2-3 blocks out from a wall, 3 blocks up, 3-5 wide, a shallow hollow
     * worn into the wall under it with a dry floor. In the dry lands' cliffs (sandstone, terracotta, stone).
     */
    private static BlockPos overhang(WorldGenLevel level, RandomSource random, BlockPos c) {
        MountainDecor.Cliff k = MountainDecor.cliff(level, c, 5, 3, 5);
        if (k == null) return null;
        Direction up = k.up(), out = up.getOpposite(), side = up.getClockWise();
        BlockPos foot = k.foot();
        if (MountainDecor.sheer(level, foot, up) < 3) return null;
        int y = 3, len = 2 + random.nextInt(2), half = 1 + random.nextInt(2);
        BlockState rock = k.rock();
        // room under its middle: open, level ground out to its edge
        for (int a = 0; a < len; a++) {
            BlockPos t = top(level, foot.relative(out, a));
            if (Math.abs(t.getY() - foot.getY()) > 1) return null;
            for (int dy = 0; dy <= y; dy++) if (!level.getBlockState(foot.relative(out, a).above(dy)).canBeReplaced()) return null;
        }
        int built = 0;
        for (int a = 0; a < len; a++)
            for (int s = -half; s <= half; s++) {
                boolean rim = a == len - 1 && (Math.abs(s) == half || random.nextInt(3) == 0);
                if (rim && a > 0) continue; // a ragged outer edge
                BlockPos p = foot.relative(out, a).relative(side, s).above(y);
                if (!level.getBlockState(p).canBeReplaced()) continue; // the wall juts out here already
                built++;
                set(level, p, rock);
                if (a < len - 1 && level.getBlockState(p.above()).canBeReplaced()) set(level, p.above(), rock); // thicker toward the wall
            }
        if (built < 3) return null;
        // the hollow under the shelf: a block worn into the wall, two high, a dry floor
        for (int s = -half + 1; s <= half - 1; s++) {
            BlockPos w = foot.relative(up).relative(side, s);
            if (!SmoothGround.rock(level.getBlockState(w)) || !SmoothGround.rock(level.getBlockState(w.above(2)))) continue;
            set(level, w, AIR);
            set(level, w.above(), AIR);
            if (natural(level.getBlockState(w.below()))) set(level, w.below(), Blocks.COARSE_DIRT.defaultBlockState());
        }
        dryPlants(level, random, foot, 2, 3);
        Decor.logPlaced("overhang_dir_" + Decor.viewAngle(out), foot.above(2));
        return foot;
    }

    /**
     * A dried-out creek bed: a shallow channel winding 14-28 blocks over the land, 2-3 wide, two blocks deep in its middle
     * and one at its edges (SmoothGround then rounds its banks with half steps), its middle floored with Gravel, cracked
     * mud (Packed Mud) and a few cobbles, its edges with Coarse Dirt and mud. Pebbles in it, dead bushes along its banks.
     * Its bed only ever steps a block at a time.
     */
    private static BlockPos dryCreek(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 1)) return null;
        double ang = random.nextDouble() * Math.PI * 2, x = c.getX() + 0.5, z = c.getZ() + 0.5;
        int len = 14 + random.nextInt(15);
        java.util.Map<Long, Integer> depth = new java.util.LinkedHashMap<>(); // column -> depth (1 edge, 2 middle)
        int lastY = c.getY() - 1, steps = 0;
        for (int i = 0; i < len; i++) {
            ang += (random.nextDouble() - 0.5) * 0.6;
            x += Math.cos(ang);
            z += Math.sin(ang);
            BlockPos mid = top(level, (int) Math.floor(x), (int) Math.floor(z)).below();
            if (Math.abs(mid.getY() - lastY) > 1 || !creekGround(level, mid, 2)) break;
            lastY = mid.getY();
            steps++;
            depth.merge(BlockPos.asLong(mid.getX(), 0, mid.getZ()), 2, Math::max);
            // the edges: one or both sides of the middle, across the way it runs
            double nx = -Math.sin(ang), nz = Math.cos(ang);
            for (int sgn : new int[] {-1, 1}) {
                if (sgn == 1 && random.nextInt(4) == 0) continue;
                BlockPos e = top(level, (int) Math.floor(x + nx * sgn), (int) Math.floor(z + nz * sgn)).below();
                if (Math.abs(e.getY() - mid.getY()) <= 1 && creekGround(level, e, 1)) depth.merge(BlockPos.asLong(e.getX(), 0, e.getZ()), 1, Math::max);
            }
        }
        if (steps < 10) return null;
        BlockPos at = null;
        for (var en : depth.entrySet()) {
            BlockPos g = top(level, BlockPos.getX(en.getKey()), BlockPos.getZ(en.getKey())).below();
            int dep = en.getValue();
            clear(level, g.above());
            for (int k = 0; k < dep; k++) set(level, g.below(k), AIR);
            BlockPos bed = g.below(dep);
            set(level, bed, creekBed(random, dep == 2, level.getBlockState(bed)));
            if (dep == 2 && random.nextInt(5) == 0) rock(level, random, bed.above(), Mechanical.LOOSE_STONE.get());
            if (dep == 2 && at == null && random.nextInt(4) == 0) at = bed.above();
        }
        for (int i = 0; i < depth.size() / 4; i++) {
            long k = depth.keySet().stream().skip(random.nextInt(depth.size())).findFirst().orElseThrow();
            dryPlants(level, random, top(level, BlockPos.getX(k), BlockPos.getZ(k)), 2, 1);
        }
        return at != null ? at : c;
    }

    /** Dry natural ground at g with natural ground under it, deep enough to dig a channel of the depth into. */
    private static boolean creekGround(WorldGenLevel level, BlockPos g, int depth) {
        for (int k = 0; k <= depth; k++) if (!dryNatural(level, g.below(k))) return false;
        return level.getBlockState(g.above()).canBeReplaced();
    }

    private static BlockState creekBed(RandomSource random, boolean middle, BlockState here) {
        int k = random.nextInt(20);
        if (middle) return (k < 8 ? Blocks.GRAVEL : k < 14 ? Blocks.PACKED_MUD : k < 16 ? Blocks.COARSE_DIRT : k < 17 ? Blocks.COBBLESTONE : here.getBlock()).defaultBlockState();
        return (k < 7 ? Blocks.COARSE_DIRT : k < 13 ? Blocks.PACKED_MUD : k < 16 ? Blocks.GRAVEL : here.getBlock()).defaultBlockState();
    }

    // ------------------------------------------------------------------ cold

    /** Blocks of ice lying on the snowy plain: a big chunk half sunk in the ground, a few smaller ones about it. */
    private static BlockPos iceBlocks(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 1)) return null;
        // a floe pushed up on edge: a sheet of ice one block thick, 3-5 long and 2-3 high with a ragged top, a second
        // smaller one leaning beside it now and then, half drowned in drift
        Direction along = randomSide(random), thick = along.getClockWise();
        int len = 3 + random.nextInt(3), rise = 1 + random.nextInt(2), width = random.nextInt(3) == 0 ? 2 : 1;
        List<BlockPos> ice = new ArrayList<>();
        for (int a = 0; a < len; a++)
            for (int w = 0; w < width; w++) {
                int edge = a == 0 || a == len - 1 ? 1 : 0;
                int h = rise - edge - random.nextInt(2) - (w > 0 ? 1 : 0);
                for (int y = -1; y <= h; y++) {
                    BlockPos p = top(level, c.relative(along, a).relative(thick, w)).above(y);
                    if (y > 0 && random.nextInt(7) == 0) break; // a broken edge
                    BlockState s = level.getBlockState(p);
                    if (!s.canBeReplaced() && !natural(s)) continue;
                    set(level, p, (random.nextInt(5) == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState());
                    ice.add(p);
                }
            }
        // broken pieces about it, and drifted snow against it all
        for (int i = 0; i < 4; i++) {
            BlockPos p = top(level, c.getX() + random.nextInt(9) - 4, c.getZ() + random.nextInt(9) - 4);
            if (dryNatural(level, p.below()) && level.getBlockState(p).canBeReplaced() && !ice.contains(p)) {
                set(level, p.below(), Blocks.PACKED_ICE.defaultBlockState());
                ice.add(p.below());
            }
        }
        for (BlockPos p : ice)
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos q = top(level, p.relative(d));
                BlockState snow = Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 2 + random.nextInt(4));
                if (q.getY() <= p.getY() + 1 && level.getBlockState(q).isAir() && snow.canSurvive(level, q)) set(level, q, snow);
            }
        return c;
    }

    /** A boulder split by frost: two halves with a crack between, gravel in the crack, splinters and grit about it. */
    private static BlockPos frostSplit(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 1)) return null;
        BlockState found = MountainDecor.rockUnder(level, c);
        BlockState stone = found != null && found.is(BlockTags.BASE_STONE_OVERWORLD) ? found : SPLIT_STONES[random.nextInt(SPLIT_STONES.length)].defaultBlockState();
        double r = 2.0 + random.nextDouble() * 0.9;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) clear(level, top(level, c.getX() + dx, c.getZ() + dz));
        // laid out first, then built: the half beyond the crack is shifted a block away from it
        boolean alongX = random.nextBoolean();
        Direction away = alongX ? Direction.EAST : Direction.SOUTH;
        double ry = r * (0.62 + random.nextDouble() * 0.15), rz = r * (0.75 + random.nextDouble() * 0.3);
        int ir = (int) Math.ceil(r) + 1;
        for (int dx = -ir; dx <= ir; dx++)
            for (int dy = -2; dy <= ir; dy++)
                for (int dz = -ir; dz <= ir; dz++) {
                    double e = sq(dx / r) + sq((dy + 0.6) / ry) + sq(dz / rz) + (random.nextDouble() - 0.5) * 0.18;
                    int across = alongX ? dx : dz;
                    if (e > 1 || across == 0) continue; // the crack
                    BlockPos p = c.offset(dx, dy, dz);
                    if (across > 0) p = p.relative(away);
                    BlockState s = level.getBlockState(p);
                    if (!s.canBeReplaced() && !natural(s)) continue;
                    set(level, p, random.nextInt(9) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : stone);
                }
        // grit in the crack
        for (int k = -ir; k <= ir; k++) {
            for (int a = 0; a <= 1; a++) {
                BlockPos g = top(level, c.relative(away, a).relative(away.getClockWise(), k)).below();
                if (dryNatural(level, g) && !SmoothGround.rock(level.getBlockState(g)) && random.nextInt(3) > 0) set(level, g, Blocks.GRAVEL.defaultBlockState());
            }
        }
        // splinters and grit
        for (int i = 0; i < 7; i++) {
            BlockPos t = top(level, c.getX() + random.nextInt(9) - 4, c.getZ() + random.nextInt(9) - 4);
            if (!dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced()) continue;
            if (random.nextInt(3) == 0) set(level, t.below(), Blocks.GRAVEL.defaultBlockState());
            else if (random.nextInt(3) == 0) set(level, t, stone);
            else rock(level, random, t, Decor.looseFor(stone));
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
                if (dx * dx + dz * dz > 5 + random.nextInt(9) || random.nextInt(5) < 2) continue;
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
