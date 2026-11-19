package com.mechanical.worldgen;

import static com.mechanical.worldgen.Decor.at;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A brown bear's den: a cave dug into a hillside, or on flat ground a hollow dug under the root plate of a fallen tree.
 * Inside a bed of moss and leaves, old bones and a pot of a past victim's gear; outside a trodden path, a claw-scratched
 * tree and the remains of a meal. The way out is at least 3 wide and 3 high everywhere (the bear is 1.3 x 1.4), with
 * floor steps of at most one block.
 */
final class BearDen extends StoneAgeFeature {
    static final MapCodec<BearDen> CODEC = MapCodec.unit(BearDen::new);
    /** Local grid of carved columns: a and b from -GRID to GRID. */
    private static final int GRID = 16;

    private record Wood(Block log, Block stripped, Block leaves, ResourceKey<Feature> tree, boolean taiga) {}

    @Override
    public MapCodec<BearDen> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!level.getFluidState(c).isEmpty() || !natural(level.getBlockState(c.below()))) return false;
        Wood wood = woodFor(level, c, random);
        Direction into = hillSide(level, random, c);
        BlockPos den, pathStart;
        if (into != null) {
            if (!dry(level, at(c, into, -3, -2, -6), at(c, into, 14, 6, 7))) return false;
            den = hillside(level, random, c, into);
            pathStart = at(c, into, -1, 0, 0);
        } else {
            if (!flat(level, c, 3, 1)) return false;
            into = randomSide(random);
            if (!dry(level, at(c, into, -6, -6, -5), at(c, into, 11, 2, 6))) return false;
            den = rootPlate(level, random, c, into, wood);
            pathStart = at(c, into, -5, 0, 0);
        }
        furnish(level, random, den, into);
        outside(level, generator, random, pathStart, into, wood);
        // shortcut: the bear only dozes at night (WildAnimal wakes it in daylight), so it is spawned awake
        spawn(level, random, Wildlife.BROWN_BEAR.get(), den, bear -> {});
        return true;
    }

    private static Wood woodFor(WorldGenLevel level, BlockPos c, RandomSource random) {
        var biome = level.getBiome(c);
        if (biome.is(BiomeTags.IS_TAIGA))
            return new Wood(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_LEAVES,
                    random.nextBoolean() ? TreeFeatures.SPRUCE : TreeFeatures.PINE, true);
        if (biome.is(Biomes.DARK_FOREST))
            return new Wood(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, TreeFeatures.DARK_OAK, false);
        if (random.nextInt(4) == 0)
            return new Wood(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_LEAVES, TreeFeatures.BIRCH, false);
        return new Wood(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_LEAVES, TreeFeatures.OAK, false);
    }

    /** A direction in which the ground rises steeply enough to dig a cave into, or null on flat ground. */
    private static Direction hillSide(WorldGenLevel level, RandomSource random, BlockPos c) {
        Direction d = randomSide(random);
        for (int i = 0; i < 4; i++, d = d.getClockWise()) {
            int y = c.getY();
            if (height(level, at(c, d, 2, 0, 0)) >= y + 2 && height(level, at(c, d, 4, 0, 0)) >= y + 4 && height(level, at(c, d, 7, 0, 0)) >= y + 5
                    && height(level, at(c, d, 7, 0, -3)) >= y + 3 && height(level, at(c, d, 7, 0, 3)) >= y + 3
                    // and a way down in front that a bear can walk, not a cliff ledge
                    && height(level, at(c, d, -2, 0, 0)) >= y - 1 && height(level, at(c, d, -4, 0, 0)) >= y - 2) return d;
        }
        return null;
    }

    /** Height of the ground at p's column; leaves of trees from neighbouring chunks don't count as a hill. */
    private static int height(WorldGenLevel level, BlockPos p) {
        BlockPos top = surface(level, p.getX(), p.getZ());
        return natural(level.getBlockState(top.below())) ? top.getY() : Integer.MIN_VALUE;
    }

    private static boolean dry(WorldGenLevel level, BlockPos from, BlockPos to) {
        for (BlockPos q : BlockPos.betweenClosed(from, to)) if (!level.getFluidState(q).isEmpty()) return false;
        return true;
    }

    /** Wall jitter per 30-degree sector, so the chamber is a rough oval instead of a clean ellipse. */
    private static double[] jitter(RandomSource random) {
        double[] j = new double[12];
        for (int i = 0; i < j.length; i++) j[i] = (random.nextDouble() - 0.5) * 0.4;
        return j;
    }

    /** Distance from the oval's centre relative to its jittered edge: below 1 is inside. */
    private static double oval(double da, double db, double[] jitter) {
        int sector = (int) Math.floor((Math.atan2(db, da) + Math.PI) / (Math.PI * 2) * jitter.length) % jitter.length;
        return Math.sqrt(da * da + db * db) / (1 + jitter[sector]);
    }

    /**
     * Cave in the slope: a 3-5 wide mouth framed by mossy boulders, a short tunnel and a domed oval chamber. Returns
     * the chamber centre at floor level.
     */
    private static BlockPos hillside(WorldGenLevel level, RandomSource random, BlockPos c, Direction into) {
        int cA = 6 + random.nextInt(2);
        double ra = 3.5 + random.nextDouble(), rb = 3.2 + random.nextDouble();
        double[] wall = jitter(random);
        double phase = random.nextDouble() * 6;
        int[][] h = new int[GRID * 2 + 1][GRID * 2 + 1];
        for (int a = 0; a <= cA + 6; a++) {
            // the tunnel is 3 wide everywhere, 5 where it bulges
            double half = 1.2 + 0.9 * Math.max(0, Math.sin(a * 0.9 + phase));
            for (int b = -6; b <= 6; b++) {
                int hc = 0;
                if (a <= cA && Math.abs(b) <= half) hc = Math.abs(b) <= 1 ? 3 + (b == 0 && random.nextInt(3) == 0 ? 1 : 0) : 2;
                double t = oval((a - cA) / ra, b / rb, wall);
                if (t < 1) hc = Math.max(hc, (int) Math.round(2 + 2.6 * Math.sqrt(1 - t * t)));
                h[a + GRID][b + GRID] = hc;
            }
        }
        for (int a = 0; a <= cA + 6; a++) {
            for (int b = -6; b <= 6; b++) {
                int hc = h[a + GRID][b + GRID];
                if (hc == 0) continue;
                foundation(level, at(c, into, a, -1, b), caveFloor(random));
                for (int y = 0; y < hc; y++) set(level, at(c, into, a, y, b), AIR);
                BlockPos roof = at(c, into, a, hc, b);
                if (a <= 1) {
                    // mossy stone lintel over the mouth
                    if (!level.getBlockState(roof).isSolid() || random.nextBoolean()) set(level, roof, mossyStone(random));
                } else if (!level.getBlockState(roof).isSolid()) {
                    set(level, roof, earth(level, roof));
                } else if (random.nextInt(3) == 0) {
                    set(level, roof, Blocks.ROOTED_DIRT.defaultBlockState());
                    if (hc >= 3 && random.nextBoolean()) set(level, roof.below(), Blocks.HANGING_ROOTS.defaultBlockState());
                }
                // close the walls where the hill is too thin
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    int na = a + (side == into ? 1 : side == into.getOpposite() ? -1 : 0);
                    int nb = b + (side == into.getClockWise() ? 1 : side == into.getCounterClockWise() ? -1 : 0);
                    if (na < 0) continue;
                    int nh = h[na + GRID][nb + GRID];
                    for (int y = nh; y <= hc; y++) {
                        BlockPos q = at(c, into, na, y, nb);
                        if (!level.getBlockState(q).isSolid()) set(level, q, a <= 1 ? mossyStone(random) : earth(level, q));
                    }
                }
            }
        }
        // boulders flanking the mouth, then make sure nothing of them blocks the way out
        int mouth = 1;
        while (mouth < 5 && h[GRID][GRID + mouth + 1] > 0) mouth++;
        for (int s : new int[] {-1, 1}) {
            double r = 1.3 + random.nextDouble() * 0.6;
            var placed = Decor.blob(level, random, at(c, into, -1 - random.nextInt(2), 0, s * (mouth + 2)), r, r * 0.9, r, BearDen::mossyStone);
            for (BlockPos top : Decor.tops(level, placed)) {
                if (random.nextBoolean()) set(level, top, Blocks.MOSS_BLOCK.defaultBlockState());
                if (random.nextInt(4) == 0) set(level, top.above(), Blocks.MOSS_CARPET.defaultBlockState());
            }
        }
        for (int a = -3; a <= 0; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int y = 0; y < 3; y++) set(level, at(c, into, a, y, b), AIR);
                if (a < 0) foundation(level, at(c, into, a, -1, b), pathFloor(random));
            }
        }
        return at(c, into, cA, 0, 0);
    }

    /**
     * A tree blown over with its root plate standing on edge; the bear dug a pit in front of the plate and a hollow under
     * the ground behind it, beneath the fallen trunk. Returns the chamber centre at floor level.
     */
    private static BlockPos rootPlate(WorldGenLevel level, RandomSource random, BlockPos c, Direction into, Wood wood) {
        double cA = 4 + random.nextDouble(), ra = 3.3 + random.nextDouble() * 0.8, rb = 3.2 + random.nextDouble() * 0.8;
        double[] wall = jitter(random);
        // the pit: a ramp of one-block steps from the ground down to the den floor 4 below
        for (int a = -5; a <= -1; a++) {
            int floor = -(a + 5);
            for (int b = -2; b <= 2; b++) {
                int y0 = Math.abs(b) <= 1 ? floor : a <= -2 && random.nextInt(3) > 0 ? floor + 1 : 99;
                if (y0 > 0) continue;
                foundation(level, at(c, into, a, y0 - 1, b), Math.abs(b) <= 1 ? pathFloor(random) : caveFloor(random));
                for (int y = y0; y <= 2; y++) set(level, at(c, into, a, y, b), AIR);
            }
        }
        // the hollow: an oval room 3 high in the middle, 2 at the rough edges, reached through a 3-wide mouth
        for (int a = 0; a <= 10; a++) {
            for (int b = -5; b <= 5; b++) {
                double t = oval((a - cA) / ra, b / rb, wall);
                boolean corridor = a <= cA && Math.abs(b) <= 1;
                if (!corridor && t >= 1) continue;
                int top = corridor || t < 0.75 ? -2 : -3;
                foundation(level, at(c, into, a, -5, b), caveFloor(random));
                for (int y = -4; y <= top; y++) set(level, at(c, into, a, y, b), AIR);
                BlockPos roof = at(c, into, a, top + 1, b);
                if (!level.getBlockState(roof).isSolid()) set(level, roof, earth(level, roof));
                else if (a >= 1 && random.nextInt(3) == 0) {
                    set(level, roof, Blocks.ROOTED_DIRT.defaultBlockState());
                    if (random.nextBoolean()) set(level, roof.below(), Blocks.HANGING_ROOTS.defaultBlockState());
                }
            }
        }
        // the root plate: a ragged disc of rooted earth on edge, its upper half leaning out over the pit
        double pr = 2.8 + random.nextDouble() * 0.9, py = 2.8 + random.nextDouble() * 0.6;
        for (int b = -4; b <= 4; b++) {
            for (int y = -1; y <= 5; y++) {
                double d = (b / pr) * (b / pr) + ((y - 1.5) / py) * ((y - 1.5) / py);
                if (d > 1 + (random.nextDouble() - 0.5) * 0.4 && !(y == -1 && Math.abs(b) <= 1)) continue;
                int a = y >= 3 ? -1 : 0;
                set(level, at(c, into, a, y, b), plate(random));
                if (a == -1 && y == 3 && random.nextInt(3) > 0) set(level, at(c, into, -1, 2, b), Blocks.HANGING_ROOTS.defaultBlockState());
            }
            if (Math.abs(b) <= 1 && random.nextInt(3) == 0) set(level, at(c, into, 0, -2, b), Blocks.HANGING_ROOTS.defaultBlockState());
        }
        // the trunk lying on the ground from the plate's centre, its crown withering at the far end
        BlockState log = wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, into.getAxis());
        int length = 6 + random.nextInt(4);
        // it rises out of the plate's centre and comes down onto the ground
        for (int a = 0; a <= length; a++) set(level, at(c, into, a, a < 3 ? 1 : 0, 0), log);
        BlockState branch = wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, into.getClockWise().getAxis());
        set(level, at(c, into, length - 2, 0, random.nextBoolean() ? 1 : -1), branch);
        BlockState leaves = wood.leaves().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        Decor.blob(level, random, at(c, into, length + 1, 1, 0), 1.8, 1.3, 1.8, r -> leaves);
        set(level, at(c, into, length + 1, 0, 0), log);
        // dug-out earth heaped beside the pit
        for (int s : new int[] {-1, 1}) {
            for (int i = 0; i < 4; i++) {
                BlockPos q = at(c, into, -2 - random.nextInt(3), 0, s * (3 + random.nextInt(2)));
                BlockPos p = surface(level, q.getX(), q.getZ());
                if (natural(level.getBlockState(p.below())))
                    set(level, p, (random.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.DIRT).defaultBlockState());
            }
        }
        return at(c, into, (int) Math.round(cA), -4, 0);
    }

    /** The bed at the back, bones lying about and the pot in a side nook. */
    private static void furnish(WorldGenLevel level, RandomSource random, BlockPos den, Direction into) {
        Direction side = into.getClockWise();
        BlockPos bed = at(den, into, 1, 0, random.nextInt(3) - 1);
        for (int da = -1; da <= 2; da++) {
            for (int db = -2; db <= 2; db++) {
                if (da * da + db * db > 4 + random.nextInt(2)) continue;
                BlockPos p = at(bed, into, da, 0, db);
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.below()).isSolid()) continue;
                if (random.nextBoolean()) set(level, p.below(), Blocks.MOSS_BLOCK.defaultBlockState());
                BlockState s = switch (random.nextInt(5)) {
                    case 0, 1 -> Blocks.MOSS_CARPET.defaultBlockState();
                    case 2, 3 -> Blocks.LEAF_LITTER.defaultBlockState().setValue(LeafLitterBlock.FACING, randomSide(random))
                            .setValue(BlockStateProperties.SEGMENT_AMOUNT, 2 + random.nextInt(3));
                    default -> Blocks.FERN.defaultBlockState();
                };
                if (s.canSurvive(level, p)) set(level, p, s);
            }
        }
        BlockState bonePile = Mechanical.BONE_PILE.get().defaultBlockState();
        for (int i = 0; i < 3; i++) {
            BlockPos p = at(den, into, -1 - random.nextInt(3), 0, random.nextInt(5) - 2);
            if (level.getBlockState(p).isAir()) onGround(level, p, bonePile);
        }
        int s = random.nextBoolean() ? 1 : -1;
        BlockPos bone = at(den, into, -1 + random.nextInt(2), 0, 2 * s);
        if (level.getBlockState(bone).isAir() && level.getBlockState(bone.below()).isSolid())
            set(level, bone, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, (random.nextBoolean() ? into : side).getAxis()));
        for (int a : new int[] {2, 1, 0, 3}) {
            BlockPos p = at(den, into, a, 0, -2 * s);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolid()) {
                pot(level, random, p, "bear_den");
                break;
            }
        }
    }

    /** Signs of the bear outside: a trodden path, a tree scratched by its claws and bones from an old meal. */
    private static void outside(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos start, Direction into, Wood wood) {
        double phase = random.nextDouble() * 6;
        for (int k = 0; k <= 8; k++) {
            int wobble = (int) Math.round(Math.sin(k * 0.6 + phase) * 1.2);
            for (int b = wobble - 1; b <= wobble + 1; b++) {
                if (k > 4 && random.nextInt(3) == 0) continue;
                BlockPos q = at(start, into, -k, 0, b);
                BlockPos ground = surface(level, q.getX(), q.getZ()).below();
                if (!natural(level.getBlockState(ground)) || !level.getFluidState(ground.above()).isEmpty()) continue;
                set(level, ground, k <= 2 ? pathFloor(random) : trodden(random, wood));
                BlockState above = level.getBlockState(ground.above());
                if (!above.isAir() && !above.canSurvive(level, ground.above())) set(level, ground.above(), AIR);
            }
        }
        int s = random.nextBoolean() ? 1 : -1;
        scratchedTree(level, generator, random, at(start, into, -2 - random.nextInt(3), 0, s * (3 + random.nextInt(2))), wood);
        // remains of a meal on the other side of the path
        BlockPos meal = at(start, into, -3 - random.nextInt(3), 0, -s * (2 + random.nextInt(2)));
        BlockPos m = surface(level, meal.getX(), meal.getZ());
        if (level.getFluidState(m).isEmpty() && natural(level.getBlockState(m.below()))) {
            set(level, m.below(), Blocks.COARSE_DIRT.defaultBlockState());
            set(level, m, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, into.getClockWise().getAxis()));
            for (int i = 0; i < 3; i++) {
                BlockPos p = surface(level, m.getX() + random.nextInt(5) - 2, m.getZ() + random.nextInt(5) - 2);
                if (level.getBlockState(p).isAir()) onGround(level, p, Mechanical.BONE_PILE.get().defaultBlockState());
            }
        }
    }

    /** Grows a tree of the biome (or, if there is no room, leaves a dead snag) with a band of bark clawed off. */
    private static void scratchedTree(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos near, Wood wood) {
        BlockPos p = surface(level, near.getX(), near.getZ());
        if (!level.getFluidState(p).isEmpty() || !natural(level.getBlockState(p.below()))) return;
        boolean grown = level.registryAccess().lookupOrThrow(Registries.FEATURE).get(wood.tree())
                .map(t -> t.value().place(level, generator, random, p)).orElse(false);
        if (!grown || !level.getBlockState(p).is(wood.log())) {
            int height = 3 + random.nextInt(3);
            for (int y = 0; y < height; y++) set(level, p.above(y), wood.log().defaultBlockState());
        }
        // dark oak trunks are 2x2
        for (BlockPos col : new BlockPos[] {p, p.east(), p.south(), p.south().east()}) {
            if (!level.getBlockState(col).is(wood.log())) continue;
            for (int y = 1; y <= 2; y++) if (level.getBlockState(col.above(y)).is(wood.log())) set(level, col.above(y), wood.stripped().defaultBlockState());
        }
    }

    private static BlockState caveFloor(RandomSource random) {
        return switch (random.nextInt(8)) {
            case 0, 1, 2, 3 -> Blocks.COARSE_DIRT.defaultBlockState();
            case 4, 5 -> Blocks.DIRT.defaultBlockState();
            case 6 -> Blocks.GRAVEL.defaultBlockState();
            default -> Blocks.ROOTED_DIRT.defaultBlockState();
        };
    }

    /** Trodden floor where the way out is open to the sky: nothing a tree could grow on and block it. */
    private static BlockState pathFloor(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0, 1 -> Blocks.DIRT_PATH.defaultBlockState();
            case 2 -> Blocks.GRAVEL.defaultBlockState();
            default -> Blocks.PACKED_MUD.defaultBlockState();
        };
    }

    /** Earth to fill a gap in the hill: grass on top where it sees the sky, dirt underneath. */
    private static BlockState earth(WorldGenLevel level, BlockPos p) {
        return level.getBlockState(p.above()).isAir() ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
    }

    private static BlockState mossyStone(RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0, 1 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 2 -> Blocks.ANDESITE.defaultBlockState();
            case 3 -> Blocks.MOSS_BLOCK.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    private static BlockState plate(RandomSource random) {
        return switch (random.nextInt(10)) {
            case 0, 1, 2, 3, 4 -> Blocks.ROOTED_DIRT.defaultBlockState();
            case 5, 6 -> Blocks.DIRT.defaultBlockState();
            case 7 -> Blocks.COARSE_DIRT.defaultBlockState();
            case 8 -> Blocks.MUD.defaultBlockState();
            default -> Blocks.COBBLESTONE.defaultBlockState();
        };
    }

    private static BlockState trodden(RandomSource random, Wood wood) {
        return switch (random.nextInt(4)) {
            case 0 -> wood.taiga() ? Blocks.PODZOL.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState();
            case 1 -> Blocks.DIRT_PATH.defaultBlockState();
            default -> Blocks.COARSE_DIRT.defaultBlockState();
        };
    }
}
