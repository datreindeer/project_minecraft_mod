package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mechanical.BirdNestBlock;
import com.mechanical.Mechanical;
import com.mechanical.Birds;
import com.mechanical.Thaw;
import com.mechanical.wildlife.GuanoPileBlock;
import com.mechanical.wildlife.VanillaGame;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.Fluids;

/**
 * Small things in the mountains (docs/terrain-plan.md phase 4), placed by scripts/gen_land_decor.py: talus fans at the
 * foot of rock walls, big fallen blocks, rare natural arches, bird cliffs, small waterfalls out of valley walls and snow
 * patches that last the summer high up. Everything is built of the rock it stands by (the wall's own stone).
 */
final class MountainDecor extends StoneAgeFeature {
    enum Kind { TALUS, FALLEN_ROCK, ROCK_ARCH, BIRD_CLIFF, WATERFALL, SNOW_PATCH }

    static final MountainDecor TALUS = new MountainDecor(Kind.TALUS), FALLEN_ROCK = new MountainDecor(Kind.FALLEN_ROCK),
            ROCK_ARCH = new MountainDecor(Kind.ROCK_ARCH), BIRD_CLIFF = new MountainDecor(Kind.BIRD_CLIFF),
            WATERFALL = new MountainDecor(Kind.WATERFALL), SNOW_PATCH = new MountainDecor(Kind.SNOW_PATCH);
    /** Snow patches from SNOW_FROM where snow never melts (Thaw.coldAllYear), from SNOW_HIGH in cool mountains. */
    static final int SNOW_FROM = 150, SNOW_HIGH = 200;

    /** A rock wall seen from its foot: the last ground before the wall, the way up, how high it rises, its rock. */
    record Cliff(BlockPos foot, Direction up, int rise, BlockState rock) {}

    private final Kind kind;
    private final MapCodec<MountainDecor> codec;

    private MountainDecor(Kind kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<MountainDecor> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!level.getFluidState(c).isEmpty() || !Decor.noStructure(level, c)) return false;
        BlockPos at = switch (kind) {
            case TALUS -> talus(level, random, c);
            case FALLEN_ROCK -> fallenRock(level, random, c);
            case ROCK_ARCH -> arch(level, random, c);
            case BIRD_CLIFF -> birdCliff(level, random, c);
            case WATERFALL -> waterfall(level, random, c);
            case SNOW_PATCH -> snowPatch(level, random, c);
        };
        if (at != null) Decor.logPlaced(kind.name().toLowerCase(), at);
        return at != null;
    }

    // ------------------------------------------------------------------ the kinds (each returns where it was built, or null)

    /**
     * A talus fan: a half cone of Gravel, Cobblestone and the wall's rock against the foot of a rock wall, thickest at the
     * wall (2-4 blocks) and thinning out over 4-10 blocks to gravel and loose stones in the turf.
     */
    private static BlockPos talus(WorldGenLevel level, RandomSource random, BlockPos c) {
        Cliff k = cliff(level, c, 6, 6, 7);
        if (k == null || !dryNatural(level, k.foot().below())) return null;
        BlockPos b = k.foot();
        Direction out = k.up().getOpposite(), side = out.getClockWise();
        int len = 4 + random.nextInt(4) + Math.min(k.rise() / 8, 3);
        double h = 1.5 + random.nextDouble() * 1.2 + Math.min(k.rise(), 24) / 16.0;
        Block loose = Decor.looseFor(k.rock());
        int built = 0;
        for (int a = 0; a <= len + 1; a++) {
            for (int s = -len - 1; s <= len + 1; s++) {
                double r = Math.sqrt(a * a + s * s * 1.2) + (random.nextDouble() - 0.5) * 1.4;
                if (r > len + 1.2) continue;
                BlockPos g = top(level, b.relative(out, a).relative(side, s)).below();
                int dy = g.getY() + 1 - b.getY();
                if (dy > 1 || dy < -1 - a / 2 || !dryNatural(level, g)) continue;
                double t = h * (1 - r / len) + dy * 0.5; // a dip in the ground fills up a little more
                clear(level, g.above());
                if (t < 0.6) { // the thin rim: gravel and loose stones in the turf
                    if (random.nextInt(3) == 0) set(level, g, Blocks.GRAVEL.defaultBlockState());
                    else if (random.nextInt(5) == 0) rock(level, random, g.above(), loose);
                    continue;
                }
                set(level, g, scree(random, k.rock()));
                int n = (int) Math.round(t - 0.6), y = 1;
                for (; y <= n && level.getBlockState(g.above(y)).canBeReplaced(); y++) set(level, g.above(y), scree(random, k.rock()));
                if (random.nextInt(6) == 0) rock(level, random, g.above(y), loose);
                built++;
            }
        }
        if (built < 5) return null;
        Decor.logPlaced("talus_dir_" + Decor.viewAngle(out), top(level, b.relative(out, 2))); // for the shots: the side to look from
        return top(level, b.relative(out, 2));
    }

    /** A big angular block broken off a wall: 3-5 wide, 2-3.5 high, corners knocked off, sunk a block, its top tipped (slabs). */
    private static BlockPos fallenRock(WorldGenLevel level, RandomSource random, BlockPos c) {
        Cliff k = cliff(level, c, 8, 8, 8);
        if (k == null) return null;
        Direction out = k.up().getOpposite(), side = out.getClockWise();
        BlockPos b = top(level, k.foot().relative(out, 2 + random.nextInt(4)).relative(side, random.nextInt(5) - 2));
        if (!flat(level, b, 1, 1)) return null;
        int wx = 3 + random.nextInt(3), wz = 3 + random.nextInt(2);
        if (random.nextBoolean()) { int t = wx; wx = wz; wz = t; }
        // a tipped block: its top a tilted plane (rounded with the rock's slab), 2-3.5 above the ground, sunk a block
        double base = 2.3 + random.nextDouble() * 1.2, sx = (random.nextDouble() - 0.5) * 0.7, sz = (random.nextDouble() - 0.5) * 0.7;
        BlockState half = slabOf(k.rock());
        boolean cold = cold(level, b);
        for (int x = -1; x <= wx; x++) for (int z = -1; z <= wz; z++) clear(level, top(level, b.getX() + x - wx / 2, b.getZ() + z - wz / 2));
        List<BlockPos> placed = new ArrayList<>();
        for (int x = 0; x < wx; x++) {
            for (int z = 0; z < wz; z++) {
                boolean corner = (x == 0 || x == wx - 1) && (z == 0 || z == wz - 1);
                if (corner && random.nextInt(5) < 3) continue; // corners knocked off, whole
                double h = base + sx * (x - wx / 2.0) + sz * (z - wz / 2.0) - (corner ? 0.5 : 0);
                int full = (int) Math.floor(h);
                double f = h - full;
                if (f >= 0.75) full++;
                BlockPos colBase = b.offset(x - wx / 2, 0, z - wz / 2);
                for (int y = -1; y < full; y++) {
                    BlockPos p = colBase.above(y);
                    BlockState s = level.getBlockState(p);
                    if (!s.canBeReplaced() && !natural(s) || !s.getFluidState().isEmpty()) continue;
                    set(level, p, y == -1 || random.nextInt(12) > 0 ? k.rock() : !cold ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                    placed.add(p);
                }
                if (f >= 0.25 && f < 0.75 && half != null && level.getBlockState(colBase.above(full)).canBeReplaced())
                    set(level, colBase.above(full), half.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
        }
        if (!cold) for (BlockPos p : Decor.tops(level, placed)) if (random.nextInt(5) == 0) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
        // the splinters it shed when it hit
        for (int i = 0; i < 6; i++) {
            BlockPos g = top(level, b.getX() + random.nextInt(9) - 4, b.getZ() + random.nextInt(9) - 4).below();
            if (dryNatural(level, g) && level.getBlockState(g.above()).canBeReplaced()) set(level, g, Blocks.GRAVEL.defaultBlockState());
        }
        Decor.rocks(level, random, b, 3 + random.nextInt(4), 2.5, 5, Decor.looseFor(k.rock()));
        Decor.logPlaced("fallen_rock_dir_" + Decor.viewAngle(out), b);
        return b;
    }

    /**
     * A natural arch: what is left of a rock fin worn through. A low, wide span (5-7 across and 3-4 high inside) with a
     * thick crown, thinning toward the middle of its depth, its legs widening and deepening as they go down into the
     * ground. Its top and its underside are rounded in half blocks (the rock's slab), the surface wavy (smooth noise, not
     * single blocks). Of the rock under the ground (Stone, Slate, Sandstone ...); in the badlands banded Terracotta, the
     * bands running level all round as in the cliffs, Red Sandstone slabs for the half steps. Weathered rubble at its feet:
     * fallen pieces, slabs, loose stones and gravel. The way through runs east-west or north-south. Rare. Advancement Under
     * the Arch (stand beneath one).
     */
    private static BlockPos arch(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 3, 2)) return null;
        BlockState ground = level.getBlockState(c.below());
        boolean banded = ground.is(net.minecraft.tags.BlockTags.TERRACOTTA) || ground.is(Blocks.RED_SAND);
        BlockState rock = banded ? null : rockUnder(level, c);
        if (rock == null && !banded) return null;
        BlockState half = banded ? Blocks.RED_SANDSTONE_SLAB.defaultBlockState() : slabOf(rock);
        Direction along = random.nextBoolean() ? Direction.EAST : Direction.SOUTH, across = along.getClockWise();
        double ai = 2.3 + random.nextDouble() * 1.0, hi = 2.8 + random.nextDouble() * 0.8; // the opening's half width and height
        double ho = hi + 1.7 + random.nextDouble() * 0.6, ao = ai + 2.3 + random.nextDouble() * 0.8;
        double[] ph = new double[6];
        for (int k = 0; k < ph.length; k++) ph[k] = random.nextDouble() * 6.3;
        int ia = (int) Math.ceil(ao) + 1;
        boolean cold = cold(level, c);
        List<BlockPos> tops = new ArrayList<>();
        int built = 0;
        for (int a = -ia; a <= ia; a++) {
            double dep = 1.25 + 1.4 * Math.min(1, Math.abs(a) / ao); // thin over the crown, deep at the feet
            for (int b = -3; b <= 3; b++) {
                double q = sq(a / ao) + 0.75 * sq(b / dep);
                if (q >= 1) continue;
                double top = ho * Math.sqrt(1 - q) + 0.22 * (Math.sin(a * 0.8 + ph[0]) + Math.sin(b * 1.3 + ph[1]) + Math.sin((a - b) * 0.6 + ph[2]));
                boolean leg = Math.abs(a) >= ai;
                double low = leg ? Double.NEGATIVE_INFINITY
                        : hi * Math.sqrt(1 - sq(a / ai)) + 0.15 * (Math.sin(a * 1.1 + ph[3]) + Math.sin(b * 0.9 + ph[4]));
                BlockPos col = c.relative(across, a).relative(along, b);
                int groundY = top(level, col).getY() - c.getY();
                if (!leg && Math.abs(b) <= 1) for (int y = 0; y < low; y++) clear(level, col.above(y)); // the way through
                if (top - low < 0.9) continue;
                // the cells: full where wholly inside, a slab where the surface cuts a cell near its middle
                int from, to;
                BlockState first = null, last = null;
                if (leg) from = Math.min(0, groundY);
                else {
                    from = (int) Math.floor(low);
                    double f = low - from;
                    if (f >= 0.75) from++;
                    else if (f >= 0.25) first = half != null ? half.setValue(SlabBlock.TYPE, SlabType.TOP) : null;
                    if (f >= 0.25 && f < 0.75 && half == null) from++;
                }
                to = (int) Math.floor(top);
                double g = top - to;
                if (g >= 0.25 && g < 0.75 && half != null) last = half.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                else if (g < 0.25) to--;
                for (int y = from; y <= to; y++) {
                    BlockPos p = col.above(y);
                    BlockState here = level.getBlockState(p);
                    if (!here.canBeReplaced() && !natural(here)) continue;
                    BlockState st = banded ? band(p.getY()) : rock;
                    if (y == from && first != null && y != to) st = first;
                    if (y == to && last != null && y != from) st = last;
                    set(level, p, st);
                    built++;
                    if (y == to && last == null) tops.add(p);
                }
            }
        }
        if (built < 30) return null;
        // weathered rubble at the feet: fallen pieces half sunk, slabs, loose stones and grit
        for (int sgn : new int[] {-1, 1}) {
            BlockPos foot = c.relative(across, sgn * (int) Math.round((ai + ao) / 2));
            for (int i = 0; i < 9; i++) {
                double an = random.nextDouble() * Math.PI * 2, dist = 2.5 + random.nextDouble() * 3;
                BlockPos t = top(level, foot.getX() + (int) Math.round(Math.cos(an) * dist), foot.getZ() + (int) Math.round(Math.sin(an) * dist));
                if (!dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced()) continue;
                BlockState st = banded ? band(t.getY()) : rock;
                switch (random.nextInt(5)) {
                    case 0 -> set(level, t.below(), st); // a piece sunk flush
                    case 1 -> { clear(level, t); set(level, t, st); } // a fallen block
                    case 2 -> { if (half != null) { clear(level, t); set(level, t, half.setValue(SlabBlock.TYPE, SlabType.BOTTOM)); } }
                    case 3 -> rock(level, random, t, banded ? Mechanical.LOOSE_STONE.get() : Decor.looseFor(rock));
                    default -> { if (!SmoothGround.rock(level.getBlockState(t.below()))) set(level, t.below(), Blocks.GRAVEL.defaultBlockState()); }
                }
            }
        }
        // keep the way through clear of anything strewn there
        for (int a = -(int) ai; a <= (int) ai; a++)
            for (int b = -2; b <= 2; b++)
                for (int y = 0; y < 3; y++) {
                    BlockPos p = c.relative(across, a).relative(along, b).above(y);
                    BlockState here = level.getBlockState(p);
                    if (here.getBlock() instanceof com.mechanical.RockBlock || here.getBlock() instanceof SlabBlock && Math.abs(b) <= 1 && y == 0) set(level, p, AIR);
                }
        Decor.logPlaced("rock_arch_dir_" + Decor.viewAngle(along), c);
        if (!cold && !banded) for (BlockPos p : tops) if (random.nextInt(4) == 0 && level.getBlockState(p.above()).isAir()) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
        return c;
    }

    /** The slab of a rock (stone_slab for stone, sandstone_slab ...), by name; null if it has none. */
    static BlockState slabOf(BlockState rock) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(rock.getBlock());
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_slab"))
                .filter(b -> b instanceof SlabBlock).map(Block::defaultBlockState).orElse(null);
    }

    /** The badlands' terracotta bands, bottom to top: runs of colour 1-3 thick, the same all round at one height. */
    private static final java.util.function.Supplier<Block[]> BANDS = () -> {
        var t = Blocks.DYED_TERRACOTTA;
        return new Block[] {Blocks.TERRACOTTA, Blocks.TERRACOTTA, t.orange(), t.orange(), t.orange(), t.yellow(), Blocks.TERRACOTTA, t.red(), t.red(),
                t.white(), Blocks.TERRACOTTA, Blocks.TERRACOTTA, t.lightGray(), t.orange(), t.orange(), t.brown()};
    };

    private static BlockState band(int y) {
        Block[] bands = BANDS.get();
        return bands[Math.floorMod(y, bands.length)].defaultBlockState();
    }

    /**
     * A bird cliff: a sheer face at least 6 high with ledges here and there, a nest (Rock Dove) or a heap of guano on each,
     * white streaks (Calcite) running down the rock beneath them and guano at the foot.
     */
    private static BlockPos birdCliff(WorldGenLevel level, RandomSource random, BlockPos c) {
        Cliff k = cliff(level, c, 6, 3, 10);
        if (k == null) return null;
        Direction up = k.up(), side = up.getClockWise();
        BlockPos foot = k.foot();
        int sheer = sheer(level, foot, up);
        if (sheer < 6) return null;
        int ledges = 0;
        BlockState guano = VanillaGame.GUANO_PILE.get().defaultBlockState();
        for (int i = 0; i < 14 && ledges < 6; i++) {
            BlockPos front = foot.relative(side, random.nextInt(7) - 3).above(3 + random.nextInt(sheer - 3));
            BlockPos wall = front.relative(up);
            if (!level.getBlockState(front).isAir() || !level.getBlockState(front.below()).isAir() || !level.getBlockState(front.above()).isAir()
                    || !SmoothGround.rock(level.getBlockState(wall))) continue;
            set(level, front, k.rock());
            BlockPos on = front.above();
            if (random.nextInt(3) > 0)
                set(level, on, Birds.BIRD_NEST.get().defaultBlockState().setValue(BirdNestBlock.BIRD, BirdNestBlock.Bird.ROCK_DOVE).setValue(BirdNestBlock.EGGS, random.nextInt(3)));
            else set(level, on, guano.setValue(GuanoPileBlock.LAYERS, 1 + random.nextInt(2)));
            streak(level, random, wall.below(), 2 + random.nextInt(4));
            streak(level, random, front.below().relative(side, random.nextBoolean() ? 1 : -1).relative(up), 1 + random.nextInt(2));
            ledges++;
        }
        if (ledges < 2) return null;
        for (int s = -4; s <= 4; s++) {
            BlockPos g = top(level, foot.relative(side, s));
            if (random.nextInt(3) == 0 && Math.abs(g.getY() - foot.getY()) <= 1 && level.getBlockState(g).isAir()) onGround(level, g, guano);
        }
        Decor.logPlaced("bird_cliff_dir_" + Decor.viewAngle(up.getOpposite()), foot.above(sheer / 2));
        return foot.above(sheer / 2);
    }

    /**
     * A small waterfall out of a valley wall: one or two water sources in a notch of a sheer wall (rock or earth) 3+ blocks up, the water
     * falling straight down the face into a plunge pool sunk in the ground below. Only where every pool cell is closed in
     * by solid ground and the notch by solid blocks on all sides, so the water can't spread. Moss around the notch.
     */
    private static BlockPos waterfall(WorldGenLevel level, RandomSource random, BlockPos c) {
        Cliff k = cliff(level, c, 6, 3, 7);
        if (k == null || !dryNatural(level, k.foot().below())) return null;
        Direction up = k.up(), side = up.getClockWise();
        BlockPos foot = k.foot();
        int width = random.nextInt(3) == 0 ? 2 : 1;
        int sheer = Integer.MAX_VALUE;
        for (int s = 0; s < width; s++) sheer = Math.min(sheer, sheer(level, foot.relative(side, s), up));
        if (sheer < 4) return null;
        int h = 3 + random.nextInt(sheer - 3);
        List<BlockPos> sources = new ArrayList<>();
        for (int s = 0; s < width; s++) sources.add(foot.relative(side, s).relative(up).above(h));
        for (BlockPos src : sources) {
            BlockState wall = level.getBlockState(src);
            if (!natural(wall) || wall.getBlock() instanceof net.minecraft.world.level.block.FallingBlock || !solid(level, src.below()) || !solid(level, src.relative(up))) return null;
            for (Direction d : new Direction[] {side, side.getOpposite()})
                if (!sources.contains(src.relative(d)) && !solid(level, src.relative(d))) return null;
        }
        // the pool: under the fall, and a block out from the wall if that is closed in too
        Set<BlockPos> pool = new LinkedHashSet<>();
        for (int s = 0; s < width; s++) pool.add(foot.relative(side, s).below());
        Set<BlockPos> wide = new LinkedHashSet<>(pool);
        for (BlockPos p : pool) wide.add(p.relative(up.getOpposite()));
        if (closedIn(level, wide)) pool = wide;
        else if (!closedIn(level, pool)) return null;
        for (BlockPos p : pool) {
            clear(level, p.above());
            if (natural(level.getBlockState(p.below()))) set(level, p.below(), Blocks.GRAVEL.defaultBlockState());
            set(level, p, Blocks.WATER.defaultBlockState());
        }
        BlockState falling = Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock();
        for (BlockPos src : sources) {
            for (Direction d : new Direction[] {side, side.getOpposite(), Direction.UP, Direction.DOWN}) {
                BlockPos m = src.relative(d);
                if (!sources.contains(m) && random.nextInt(3) > 0 && solid(level, m) && natural(level.getBlockState(m)))
                    set(level, m, Blocks.MOSS_BLOCK.defaultBlockState());
            }
            set(level, src, Blocks.WATER.defaultBlockState());
            level.scheduleTick(src, Fluids.WATER, 0);
            // the fall itself, laid at once: the pool under it is then never the top of its column, so it never freezes over
            for (BlockPos f = src.relative(up.getOpposite()); f.getY() >= foot.getY(); f = f.below()) {
                clear(level, f);
                set(level, f, falling);
                level.scheduleTick(f, Fluids.FLOWING_WATER, 0);
            }
        }
        // a few mossy stones and ferns by the pool
        for (int i = 0; i < 5; i++) {
            BlockPos g = top(level, foot.getX() + random.nextInt(7) - 3, foot.getZ() + random.nextInt(7) - 3);
            if (level.getFluidState(g.below()).isEmpty()) {
                if (random.nextBoolean()) rock(level, random, g, Decor.looseFor(k.rock()));
                else ForestDecor.plant(level, random, g);
            }
        }
        Decor.logPlaced("waterfall_dir_" + Decor.viewAngle(up.getOpposite()), foot.above(2));
        return foot;
    }

    /**
     * A snow patch that outlasts the summer, high up: an oval of Snow Blocks 4-12 across (Thaw never melts Snow Blocks),
     * fraying out to thinner and thinner snow layers at its rim. From y 150 where snow never melts (Thaw.coldAllYear),
     * and from y 200 in the mountains (y 240 in warm ones such as Stony Peaks), there without the rim. Not on ground that is snow.
     */
    private static BlockPos snowPatch(WorldGenLevel level, RandomSource random, BlockPos c) {
        BlockState ground = level.getBlockState(c.below());
        if (c.getY() < SNOW_FROM || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.POWDER_SNOW) || !natural(ground) || !flat(level, c, 1, 2)) return null;
        // snow layers last only where it never thaws; higher up the Snow Block core alone outlasts summer
        boolean frozen = Thaw.coldAllYear(level.getLevel(), level.getBiome(c).value(), c);
        if (!frozen && (c.getY() < SNOW_HIGH || c.getY() < SNOW_HIGH + 40 && level.getBiome(c).value().getBaseTemperature() > 0.5F)) return null;
        double rx = 2.2 + random.nextDouble() * 3.8, rz = 1.6 + random.nextDouble() * 2.2, ang = random.nextDouble() * Math.PI;
        double cos = Math.cos(ang), sin = Math.sin(ang);
        int r = (int) Math.ceil(rx) + 1, built = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double u = dx * cos + dz * sin, v = -dx * sin + dz * cos;
                double e = sq(u / rx) + sq(v / rz) + (random.nextDouble() - 0.5) * 0.3;
                if (e > 1) continue;
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                BlockState g = level.getBlockState(t.below());
                if (Math.abs(t.getY() - c.getY()) > 3 || !natural(g) || !level.getFluidState(t).isEmpty() || !level.getBlockState(t).canBeReplaced()) continue;
                clear(level, t);
                if (e < 0.45) set(level, t, Blocks.SNOW_BLOCK.defaultBlockState());
                else if (!frozen) continue;
                else {
                    if (g.hasProperty(SnowyBlock.SNOWY)) set(level, t.below(), g.setValue(SnowyBlock.SNOWY, true));
                    set(level, t, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Math.max(1, Math.min(7, (int) Math.round(9 * (1 - e))))));
                }
                built++;
            }
        }
        return built >= 6 ? c : null;
    }

    // ------------------------------------------------------------------ helpers (DryColdDecor uses some)

    /**
     * The highest rock wall near c: walking out from c in each direction over ground that stays within a block of level
     * (at most search columns), the first step of 2+ blocks up is the wall, its foot the last column before it. The wall
     * counts if within reach columns of the foot the ground rises at least minRise and its face is mostly rock (base
     * stone or sandstone). Null if there is none.
     */
    static Cliff cliff(WorldGenLevel level, BlockPos c, int search, int reach, int minRise) {
        Cliff best = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos foot = c;
            for (int k = 0; k < search; k++) {
                BlockPos next = top(level, foot.relative(d));
                int step = next.getY() - foot.getY();
                if (step >= 2) {
                    int rise = 0;
                    for (int j = 1; j <= reach; j++) rise = Math.max(rise, top(level, foot.relative(d, j)).getY() - foot.getY());
                    BlockState rock = null;
                    int rocks = 0, faces = 0;
                    for (int y = 0; y <= Math.min(rise, 6); y++) {
                        BlockState s = level.getBlockState(foot.relative(d).above(y));
                        if (!s.isSolid()) break;
                        faces++;
                        if (SmoothGround.rock(s)) {
                            rocks++;
                            if (rock == null) rock = s;
                        }
                    }
                    if (rocks * 2 < faces) rock = null; // mostly earth: not a rock wall
                    if (rise >= minRise && rock != null && (best == null || rise > best.rise())) best = new Cliff(foot, d, rise, rock);
                    break;
                }
                if (Math.abs(step) > 1 || !level.getFluidState(next).isEmpty()) break;
                foot = next;
            }
        }
        return best;
    }

    /** How many blocks up from foot the wall (the column toward up) is solid with open air (or a small plant) in front of it. */
    static int sheer(WorldGenLevel level, BlockPos foot, Direction up) {
        int h = 0;
        while (h < 40 && level.getBlockState(foot.above(h)).canBeReplaced() && level.getFluidState(foot.above(h)).isEmpty()
                && solid(level, foot.relative(up).above(h))) h++;
        return h;
    }

    /** Every horizontal side of each cell is solid or another cell, and every cell has solid ground under it. */
    static boolean closedIn(WorldGenLevel level, Set<BlockPos> cells) {
        for (BlockPos p : cells) {
            if (!solid(level, p.below()) || !natural(level.getBlockState(p))) return false;
            for (Direction d : Direction.Plane.HORIZONTAL) if (!cells.contains(p.relative(d)) && !solid(level, p.relative(d))) return false;
        }
        return true;
    }

    /** A white guano streak down a rock face: n blocks of Calcite down from p, while it is rock. */
    private static void streak(WorldGenLevel level, RandomSource random, BlockPos p, int n) {
        for (int i = 0; i < n && SmoothGround.rock(level.getBlockState(p.below(i))); i++)
            set(level, p.below(i), (random.nextInt(4) == 0 ? Blocks.DIORITE : Blocks.CALCITE).defaultBlockState());
    }

    static boolean solid(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isSolid() && s.getFluidState().isEmpty() && !s.canBeReplaced();
    }

    /** Natural ground with no water on or in it. */
    static boolean dryNatural(WorldGenLevel level, BlockPos g) {
        return natural(level.getBlockState(g)) && level.getFluidState(g).isEmpty() && level.getFluidState(g.above()).isEmpty();
    }

    /** The rock under the ground at c (base stone or sandstone within 10 blocks down), or null. */
    static BlockState rockUnder(WorldGenLevel level, BlockPos c) {
        for (int d = 1; d <= 10; d++) {
            BlockState s = level.getBlockState(c.below(d));
            if (SmoothGround.rock(s)) return s;
            if (!natural(s)) return null;
        }
        return null;
    }

    /** One block of scree: gravel, cobble or the wall's rock. */
    static BlockState scree(RandomSource random, BlockState rock) {
        int i = random.nextInt(20);
        return i < 7 ? Blocks.GRAVEL.defaultBlockState() : i < 11 ? Blocks.COBBLESTONE.defaultBlockState()
                : i == 11 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : rock;
    }

    /** Clears a small plant (both halves of a tall one) or thin snow at p. */
    static void clear(WorldGenLevel level, BlockPos p) {
        for (int i = 0; i < 2; i++) {
            BlockState s = level.getBlockState(p.above(i));
            if (s.isAir() || !s.canBeReplaced() || !s.getFluidState().isEmpty()) return;
            set(level, p.above(i), AIR);
        }
    }

    static boolean cold(WorldGenLevel level, BlockPos p) {
        return level.getBiome(p).value().getBaseTemperature() < 0.15F;
    }

    static BlockPos top(WorldGenLevel level, BlockPos p) {
        return surface(level, p.getX(), p.getZ());
    }

    static BlockPos top(WorldGenLevel level, int x, int z) {
        return surface(level, x, z);
    }

    static double sq(double v) {
        return v * v;
    }
}
