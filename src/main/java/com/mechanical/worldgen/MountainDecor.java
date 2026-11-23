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
import com.mechanical.wildlife.Wildlife;
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
     * A talus fan: scree shed by a rock wall, lying against its foot as a half cone that spreads as it runs out, its slope
     * smooth in half blocks (Gravel Slab, the rock's slabs and stairs). Gravel above all, the wall's rock, cobbles and a
     * second stone in patches through it, the coarse stuff rolled furthest: a few big stones half sunk at its toe. Moss
     * on the cobbles in mild land, loose stones about.
     */
    private static BlockPos talus(WorldGenLevel level, RandomSource random, BlockPos c) {
        Cliff k = cliff(level, c, 6, 6, 7);
        if (k == null || !dryNatural(level, k.foot().below()) || !openFoot(level, k)) return null;
        BlockPos b = k.foot();
        Direction out = k.up().getOpposite(), side = out.getClockWise();
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        double len = 5 + random.nextDouble() * 2 + Math.min(k.rise(), 24) / 6.0, h0 = 2.2 + random.nextDouble() + Math.min(k.rise(), 24) / 10.0;
        double skew = (random.nextDouble() - 0.5) * 0.5;
        Block rock = k.rock().getBlock(), second = secondStone(rock);
        int ox = b.getX(), oz = b.getZ(), ou = out.getStepX(), ow = out.getStepZ(), su = side.getStepX(), sw = side.getStepZ();
        int r = (int) Math.ceil(len) + 2;
        // the ground under the fan, before it is laid
        java.util.Map<Long, Integer> groundY = new java.util.HashMap<>();
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++) groundY.put(BlockPos.asLong(ox + x, 0, oz + z), top(level, ox + x, oz + z).getY());
        Shaper.Density fan = (x, y, z) -> {
            double dx = x - ox - 0.5, dz = z - oz - 0.5, a = dx * ou + dz * ow + 0.5, s = dx * su + dz * sw - skew * a;
            if (a < -0.5) return -1;
            double run = Math.max(0, 1 - a / len), half = 1.6 + a * 0.75 + 0.6 * noise.get(a * 0.2, 5);
            double lateral = 1 - sq(s / half);
            if (lateral <= 0) return -1;
            double t = h0 * Math.pow(run, 1.25) * Math.pow(lateral, 0.55) + 0.45 * noise.get(dx * 0.35, dz * 0.35) - 0.15;
            Integer g = groundY.get(BlockPos.asLong((int) Math.floor(x), 0, (int) Math.floor(z)));
            return g == null ? -1 : g + t - y;
        };
        Shaper.Stone stone = p -> {
            double v = noise.get(p.getX() * 0.3, p.getY() * 0.3 + 40, p.getZ() * 0.3);
            return v > 0.45 ? Blocks.COBBLESTONE : v > 0.25 ? rock : v < -0.6 ? second : Blocks.GRAVEL;
        };
        var cells = Shaper.build(level, ox - r, b.getY() - 2, oz - r, ox + r, b.getY() + (int) Math.ceil(h0) + 2, oz + r, fan, stone, null);
        if (cells.size() < 10) return null;
        boolean mild = !cold(level, b);
        for (BlockPos p : Shaper.tops(level, cells))
            if (mild && level.getBlockState(p).is(Blocks.COBBLESTONE) && random.nextInt(3) == 0) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
        // the big stones rolled to the toe, half sunk
        Block loose = Decor.looseFor(k.rock());
        for (int i = 0, n = 1 + random.nextInt(3); i < n; i++) {
            BlockPos at = top(level, b.relative(out, (int) len - 1 + random.nextInt(3)).relative(side, random.nextInt(5) - 2));
            if (!dryNatural(level, at.below())) continue;
            double rr = 0.9 + random.nextDouble() * 0.7;
            int bx = at.getX(), by = at.getY(), bz = at.getZ();
            Shaper.Density stoneBall = (x, y, z) -> 1 - Math.sqrt(sq(x - bx - 0.5) + sq((y - by + 0.15) * 1.3) + sq(z - bz - 0.5)) / rr + 0.15 * noise.get(x * 0.7, y * 0.7, z * 0.7);
            Shaper.build(level, bx - 2, by - 1, bz - 2, bx + 2, by + 2, bz + 2, stoneBall, p -> random.nextInt(4) == 0 ? second : rock, null);
        }
        Decor.rocks(level, random, b.relative(out, (int) (len / 2)), 5 + random.nextInt(4), 1, len, loose);
        Decor.logPlaced("talus_dir_" + Decor.viewAngle(out), top(level, b.relative(out, 2)));
        return top(level, b.relative(out, 2));
    }

    /**
     * A big block broken off a wall: a tilted, round-edged block 3-6 across and 2-4 high, sunk to a third of its height,
     * its faces smooth in half blocks (the rock's slabs and stairs) and its rock patched with a second stone and cracked
     * cobbles, moss on its top in mild land. The scar it left rolling out, torn turf and grit, and the splinters it shed.
     */
    private static BlockPos fallenRock(WorldGenLevel level, RandomSource random, BlockPos c) {
        Cliff k = cliff(level, c, 8, 8, 8);
        if (k == null || !openFoot(level, k)) return null;
        Direction out = k.up().getOpposite(), side = out.getClockWise();
        int dist = 3 + random.nextInt(4);
        BlockPos b = top(level, k.foot().relative(out, dist).relative(side, random.nextInt(5) - 2));
        if (!flat(level, b, 2, 1)) return null;
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        double hx = 1.6 + random.nextDouble() * 1.3, hy = 1.3 + random.nextDouble() * 0.9, hz = 1.4 + random.nextDouble() * 1.1;
        double yaw = random.nextDouble() * Math.PI, pitch = (random.nextDouble() - 0.5) * 0.6, roll = (random.nextDouble() - 0.5) * 0.6;
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch), cr = Math.cos(roll), sr = Math.sin(roll);
        double ox = b.getX() + 0.5, oz = b.getZ() + 0.5, oy = b.getY() + hy * 0.4;
        Shaper.Density block = (x, y, z) -> {
            double dx = x - ox, dy = y - oy, dz = z - oz;
            double u = dx * cy + dz * sy, w = -dx * sy + dz * cy; // turned, then tipped
            double v = dy * cp - u * sp;
            u = u * cp + dy * sp;
            double v2 = v * cr - w * sr;
            w = w * cr + v * sr;
            double e = Math.pow(Math.pow(Math.abs(u / hx), 4) + Math.pow(Math.abs(v2 / hy), 4) + Math.pow(Math.abs(w / hz), 4), 0.25);
            return (1 - e) * Math.min(hx, Math.min(hy, hz)) + 0.22 * noise.get(x * 0.5, y * 0.5, z * 0.5);
        };
        Block rock = k.rock().getBlock(), second = secondStone(rock);
        boolean cold = cold(level, b);
        Shaper.Stone stone = p -> {
            double v = noise.get(p.getX() * 0.25, p.getY() * 0.25 + 9, p.getZ() * 0.25);
            return v > 0.5 ? second : v < -0.62 ? (cold ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE) : rock;
        };
        int r = (int) Math.ceil(Math.max(hx, Math.max(hy, hz)) * 1.3) + 1;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) clear(level, top(level, b.getX() + x, b.getZ() + z));
        var cells = Shaper.build(level, b.getX() - r, b.getY() - 2, b.getZ() - r, b.getX() + r, b.getY() + r + 1, b.getZ() + r, block, stone, null);
        if (cells.size() < 12) return null;
        Shaper.seat(level, cells, stone, 3);
        if (!cold) for (BlockPos p : Shaper.tops(level, cells))
            if (noise.get(p.getX() * 0.4, p.getZ() * 0.4 + 3) > -0.1) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
        // the scar back toward the wall: torn turf and grit
        for (int a = 1; a < dist + 1; a++)
            for (int s = -1; s <= 1; s++) {
                BlockPos g = top(level, b.relative(k.up(), a + r - 1).relative(side, s)).below();
                if (random.nextInt(3) > 0 && dryNatural(level, g) && g.getY() <= b.getY() && level.getBlockState(g).is(net.minecraft.tags.BlockTags.SUBSTRATE_OVERWORLD))
                    set(level, g, (random.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.GRAVEL).defaultBlockState());
            }
        Decor.rocks(level, random, b, 4 + random.nextInt(4), r, r + 3, Decor.looseFor(k.rock()));
        Decor.logPlaced("fallen_rock_dir_" + Decor.viewAngle(out), b);
        return b;
    }

    /**
     * A natural arch: what is left of a rock fin worn through. A fin 13-20 across and 7-10 high, flat-topped with steep
     * flanks, bowed a little in plan, thin over the crown (about three deep) and thick at its feet where it flares into
     * the ground; through it an opening 7-10 wide and 5-7 high, rounder at the top than at its sides. Every surface is laid at half-block resolution
     * (Shaper: the rock's slabs and stairs), its outline and faces wavering with smooth noise, one leg heavier than the
     * other. Of the rock under the ground, in big patches of a second stone and moss on the tops in mild land; in the
     * badlands Red Sandstone with level bands of Terracotta through it as in the cliffs. Fallen pieces, loose stones and
     * grit at its feet. Rare. Advancement Under the Arch (stand beneath one).
     */
    private static BlockPos arch(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 3, 1)) return null;
        BlockState ground = level.getBlockState(c.below());
        boolean banded = ground.is(net.minecraft.tags.BlockTags.TERRACOTTA) || ground.is(Blocks.RED_SAND);
        BlockState found = banded ? Blocks.RED_SANDSTONE.defaultBlockState() : rockUnder(level, c);
        if (found == null) return null;
        Block rock = found.getBlock(), second = secondStone(rock);
        Direction along = random.nextBoolean() ? Direction.EAST : Direction.SOUTH, across = along.getClockWise();
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        double ai = 3.4 + random.nextDouble() * 1.4, hi = 4.6 + random.nextDouble() * 2; // the opening's half width and height
        double hc = hi + 2 + random.nextDouble() * 1.2, hs = hc + 0.4 + random.nextDouble() * 0.9; // the crown, the shoulders over the legs
        double heavy = random.nextBoolean() ? 1 : -1, wHeavy = 3.4 + random.nextDouble() * 1.2, wLight = 2.3 + random.nextDouble() * 0.9; // the legs
        double h = hs + 1, w = ai + wHeavy;
        double lean = (random.nextDouble() - 0.5) * 0.25, bow = 0.5 + random.nextDouble() * 0.6;
        int ox = c.getX(), oz = c.getZ(), oy = c.getY();
        int au = across.getStepX(), aw = across.getStepZ(), vu = along.getStepX(), vw = along.getStepZ();
        Shaper.Density fin = (x, y, z) -> {
            double dx = x - ox - 0.5, dz = z - oz - 0.5, u = dx * au + dz * aw, v = dx * vu + dz * vw, t = y - oy;
            double au2 = Math.abs(u), legW = u * heavy > 0 ? wHeavy : wLight;
            // the upper edge: sagging a little over the span, rounded shoulders, then the legs' flanks falling to the ground
            double top = au2 < ai ? hc + (hs - hc) * sq(au2 / ai)
                    : hs * Math.sqrt(Math.max(0, 1 - sq(Math.min(1, (au2 - ai) / legW)))) - (au2 - ai > legW ? 9 : 0);
            top += 0.75 * noise.get(u * 0.18, v * 0.25 + 7);
            // thin over the crown, thick in the legs and flared at the feet
            double depth = 1.2 + 0.9 * Math.min(1, au2 / ai) + 1.1 * Math.max(0, 1 - t / 3) + 0.45 * noise.get(u * 0.3, t * 0.3);
            // its faces bulge and hollow (no flat walls)
            double side = depth - Math.abs(v - bow * Math.cos(u / w * 1.6) - lean * t - 0.8 * noise.get(u * 0.17 + 20, t * 0.17));
            // the opening: a rounded-top hole through it, its sides near upright
            double hu = (u - 0.4 * heavy) / ai, ht = (t + 0.4) / hi;
            double hole = (Math.pow(Math.pow(Math.abs(hu), 2.8) + Math.pow(Math.max(ht, 0), 2), 1 / 2.4) - 1) * ai + 0.3 * noise.get(u * 0.4 + 3, t * 0.4);
            if (t < 0) hole = Math.abs(hu) < 1 ? -1 : hole; // the floor under the opening stays ground
            return Math.min(Math.min(top - t, side), hole) + 0.08 * noise.get(x * 0.3, y * 0.3, z * 0.3);
        };
        Shaper.Stone stone = p -> {
            if (banded) return band(p.getY());
            return noise.get(p.getX() * 0.13, p.getY() * 0.2, p.getZ() * 0.13) > 0.35 ? second : rock;
        };
        int r = (int) Math.ceil(w) + 3;
        var cells = Shaper.build(level, ox - r, oy - 3, oz - r, ox + r, oy + (int) Math.ceil(h) + 2, oz + r, fin, stone, banded ? Blocks.RED_SANDSTONE : null);
        if (cells.size() < 60) return null;
        Shaper.seat(level, cells, stone, 6);
        // a skirt of scree round the feet: half steps of sand or gravel against the rock
        Block grit = banded ? Blocks.RED_SAND : ground.is(Blocks.SAND) ? Blocks.SAND : Blocks.GRAVEL;
        BlockState skirt = SmoothGround.slab(grit).defaultBlockState();
        for (Shaper.Cell k : cells) {
            if (k.pos().getY() != oy || !k.full()) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = k.pos().relative(d);
                if (random.nextInt(3) > 0 && level.getBlockState(n).canBeReplaced() && dryNatural(level, n.below()) && skirt.getBlock() != Blocks.AIR) {
                    clear(level, n);
                    set(level, n, skirt);
                }
            }
        }
        // the way through: nothing growing or lying in it
        for (int a = -(int) ai; a <= (int) ai; a++)
            for (int b = -3; b <= 3; b++) clear(level, top(level, c.relative(across, a).relative(along, b)));
        boolean cold = cold(level, c);
        // moss and a little grass on the mild tops, never in the badlands
        if (!banded && !cold) for (BlockPos p : Shaper.tops(level, cells))
            if (noise.get(p.getX() * 0.3, p.getZ() * 0.3) > 0.1 && level.getBlockState(p.above()).isAir()) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
        // at its feet: fallen pieces half sunk, loose stones and grit
        for (int sgn : new int[] {-1, 1}) {
            BlockPos foot = c.relative(across, sgn * (int) Math.round((ai + w) / 2));
            for (int i = 0; i < 10; i++) {
                double an = random.nextDouble() * Math.PI * 2, dist = 3 + random.nextDouble() * 3.5;
                BlockPos t = top(level, foot.getX() + (int) Math.round(Math.cos(an) * dist), foot.getZ() + (int) Math.round(Math.sin(an) * dist));
                if (!dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced() || Math.abs(t.getY() - oy) > 2) continue;
                switch (random.nextInt(4)) {
                    case 0 -> set(level, t.below(), stone.at(t).defaultBlockState()); // a piece sunk flush
                    case 1 -> { // a flat fallen slab
                        BlockState slab = Shaper.shape(0x0F, stone.at(t), banded ? Blocks.RED_SANDSTONE : null);
                        if (slab != null) { clear(level, t); set(level, t, slab); }
                    }
                    case 2 -> rock(level, random, t, banded ? Mechanical.LOOSE_STONE.get() : Decor.looseFor(found));
                    default -> { if (!SmoothGround.rock(level.getBlockState(t.below()))) set(level, t.below(), (banded ? Blocks.RED_SAND : Blocks.GRAVEL).defaultBlockState()); }
                }
            }
        }
        Decor.logPlaced("rock_arch_dir_" + Decor.viewAngle(along), c);
        return c;
    }

    /** The stone that comes in patches through a rock: Andesite in Stone, Tuff in Andesite, Stone in the rest. */
    static Block secondStone(Block rock) {
        if (rock == Blocks.STONE) return Blocks.ANDESITE;
        if (rock == Blocks.ANDESITE) return Blocks.TUFF;
        if (rock == Blocks.SANDSTONE) return Blocks.SMOOTH_SANDSTONE;
        if (rock == Blocks.RED_SANDSTONE) return Blocks.SMOOTH_RED_SANDSTONE;
        if (rock == Blocks.GRANITE || rock == Blocks.DIORITE) return Blocks.STONE;
        return rock;
    }

    /** The badlands arch's layers, bottom to top: Red Sandstone with level bands of Terracotta 1-2 thick, the same all round at one height. */
    private static final java.util.function.Supplier<Block[]> BANDS = () -> {
        var t = Blocks.DYED_TERRACOTTA;
        Block rs = Blocks.RED_SANDSTONE;
        return new Block[] {rs, rs, rs, t.orange(), t.orange(), rs, rs, Blocks.TERRACOTTA, rs, rs, rs, t.orange(), rs, rs, Blocks.SMOOTH_RED_SANDSTONE, Blocks.TERRACOTTA};
    };

    private static Block band(int y) {
        Block[] bands = BANDS.get();
        return bands[Math.floorMod(y, bands.length)];
    }

    /**
     * A bird cliff: a sheer face at least 6 high where rock doves nest, seen from afar by the white it is streaked with.
     * Ledges of the rock's slab jut from the face, a nest (Rock Dove, sometimes eggs) or a heap of guano on each, and down
     * the face under every ledge a long white run of droppings (Calcite, flecked with Diorite), widest at the ledge and
     * thinning and breaking up as it goes down; more guano and a few feathers' worth of bone meal white at the foot, and
     * the doves themselves about the cliff.
     */
    private static BlockPos birdCliff(WorldGenLevel level, RandomSource random, BlockPos c) {
        Cliff k = cliff(level, c, 6, 3, 9);
        if (k == null) return null;
        Direction up = k.up(), side = up.getClockWise();
        BlockPos foot = k.foot();
        int sheer = sheer(level, foot, up);
        if (sheer < 6) return null;
        BlockState guano = VanillaGame.GUANO_PILE.get().defaultBlockState();
        BlockState ledge = Shaper.shape(0x0F, k.rock().getBlock(), Blocks.STONE);
        int ledges = 0;
        for (int i = 0; i < 24 && ledges < 7; i++) {
            BlockPos front = foot.relative(side, random.nextInt(9) - 4).above(3 + random.nextInt(sheer - 3));
            BlockPos wall = front.relative(up);
            if (!level.getBlockState(front).isAir() || !level.getBlockState(front.above()).isAir() || !SmoothGround.rock(level.getBlockState(wall))
                    || level.getBlockState(front.relative(side)).getBlock() instanceof net.minecraft.world.level.block.SlabBlock
                    || level.getBlockState(front.relative(side.getOpposite())).getBlock() instanceof net.minecraft.world.level.block.SlabBlock) continue;
            set(level, front, ledge);
            BlockPos on = front.above();
            if (random.nextInt(3) > 0)
                set(level, on, Birds.BIRD_NEST.get().defaultBlockState().setValue(BirdNestBlock.BIRD, BirdNestBlock.Bird.ROCK_DOVE).setValue(BirdNestBlock.EGGS, random.nextInt(3)));
            else set(level, on, guano.setValue(GuanoPileBlock.LAYERS, 1 + random.nextInt(2)));
            // the white run under it: the face below the ledge, two wide at first, breaking up lower down
            int len = 4 + random.nextInt(Math.max(1, front.getY() - foot.getY() - 2));
            for (int j = 0; j < len; j++) {
                BlockPos f = wall.below(j + 1);
                if (random.nextInt(8) < 7 - j / 3) streak(level, random, f);
                if (j < 3 && random.nextBoolean()) streak(level, random, f.relative(random.nextBoolean() ? side : side.getOpposite()));
            }
            streak(level, random, wall);
            ledges++;
        }
        if (ledges < 3) return null;
        for (int s = -5; s <= 5; s++)
            for (int a = 0; a <= 2; a++) {
                BlockPos g = top(level, foot.relative(side, s).relative(up.getOpposite(), a));
                if (random.nextInt(2 + a) == 0 && Math.abs(g.getY() - foot.getY()) <= 1 && level.getBlockState(g).isAir())
                    onGround(level, g, guano.setValue(GuanoPileBlock.LAYERS, 1 + random.nextInt(a == 0 ? 3 : 1)));
            }
        for (int i = 0, n = 2 + random.nextInt(3); i < n; i++)
            spawn(level, random, Wildlife.ROCK_DOVE.get(), foot.relative(up.getOpposite(), 2).relative(side, random.nextInt(5) - 2).above(1 + random.nextInt(3)), m -> {});
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
     * A snow patch that outlasts the summer, high up in a hollow or on a shelf where the drifts lie deepest: a ragged
     * patch 7-15 across of Snow Blocks laid flush in the ground (Thaw never melts Snow Blocks), so it follows the lie of
     * the land instead of standing on it, fraying at its rim into scattered snow and wet, dark earth (Mud, Gravel) where
     * it melts back. Where snow never melts (Thaw.coldAllYear) it swells in its middle with snow layers. From y 150 where
     * snow never melts, from y 200 in the mountains (y 240 in warm ones such as Stony Peaks). Not on ground that is snow.
     */
    private static BlockPos snowPatch(WorldGenLevel level, RandomSource random, BlockPos c) {
        BlockState ground = level.getBlockState(c.below());
        if (c.getY() < SNOW_FROM || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.POWDER_SNOW) || !natural(ground) || !flat(level, c, 2, 2)) return null;
        boolean frozen = Thaw.coldAllYear(level.getLevel(), level.getBiome(c).value(), c);
        if (!frozen && (c.getY() < SNOW_HIGH || c.getY() < SNOW_HIGH + 40 && level.getBiome(c).value().getBaseTemperature() > 0.5F)) return null;
        // a hollow or a level shelf: the ground around lies no lower than here on the whole
        int sum = 0, n = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) for (int k = 4; k <= 6; k += 2) { sum += top(level, c.relative(d, k)).getY() - c.getY(); n++; }
        if (sum < -n / 2) return null;
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        double rx = 3.5 + random.nextDouble() * 3.5, rz = 2.8 + random.nextDouble() * 2.2, ang = random.nextDouble() * Math.PI;
        double cos = Math.cos(ang), sin = Math.sin(ang);
        int r = (int) Math.ceil(rx) + 3, built = 0;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                double u = dx * cos + dz * sin, v = -dx * sin + dz * cos;
                double e = 1 - Math.sqrt(sq(u / rx) + sq(v / rz)) + 0.45 * noise.get((c.getX() + dx) * 0.22, (c.getZ() + dz) * 0.22)
                        + 0.15 * noise.get((c.getX() + dx) * 0.7, (c.getZ() + dz) * 0.7);
                if (e < -0.3) continue;
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                BlockPos g = t.below();
                BlockState gs = level.getBlockState(g);
                if (Math.abs(t.getY() - c.getY()) > 4 || !natural(gs) || !level.getFluidState(t).isEmpty() || !level.getBlockState(t).canBeReplaced()) continue;
                if (e > 0) { // the patch, flush with the ground
                    clear(level, t);
                    set(level, g, Blocks.SNOW_BLOCK.defaultBlockState());
                    if (frozen && e > 0.35) set(level, t, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Math.min(6, 1 + (int) ((e - 0.35) * 9))));
                    built++;
                } else if (random.nextInt(3) == 0) { // the melting rim: a scrap of snow, wet dark earth, grit
                    if (e > -0.12 && random.nextBoolean()) set(level, g, Blocks.SNOW_BLOCK.defaultBlockState());
                    else if (gs.is(net.minecraft.tags.BlockTags.SUBSTRATE_OVERWORLD)) set(level, g, (random.nextBoolean() ? Blocks.MUD : Blocks.COARSE_DIRT).defaultBlockState());
                    else if (random.nextBoolean()) set(level, g, Blocks.GRAVEL.defaultBlockState());
                }
            }
        return built >= 20 ? c : null;
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

    /** The ground out from a wall's foot is open and gentle (a block a step at most over 5) and bare of snow: room for a fan or a fallen block to lie in sight. */
    static boolean openFoot(WorldGenLevel level, Cliff k) {
        Direction out = k.up().getOpposite();
        BlockPos prev = k.foot();
        for (int a = 1; a <= 5; a++) {
            BlockPos t = top(level, k.foot().relative(out, a));
            if (Math.abs(t.getY() - prev.getY()) > 1 || t.getY() > k.foot().getY() + 1 || !level.getFluidState(t).isEmpty() || level.getBlockState(t).is(Blocks.SNOW)) return false;
            prev = t;
        }
        return !level.getBlockState(k.foot()).is(Blocks.SNOW);
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

    /** A block of white droppings on a rock face: Calcite, now and then Diorite, if it is rock. */
    private static void streak(WorldGenLevel level, RandomSource random, BlockPos p) {
        if (SmoothGround.rock(level.getBlockState(p))) set(level, p, (random.nextInt(4) == 0 ? Blocks.DIORITE : Blocks.CALCITE).defaultBlockState());
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
