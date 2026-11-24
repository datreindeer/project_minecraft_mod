package com.mechanical.worldgen;

import static com.mechanical.worldgen.MountainDecor.clear;
import static com.mechanical.worldgen.MountainDecor.dryNatural;
import static com.mechanical.worldgen.MountainDecor.sq;
import static com.mechanical.worldgen.MountainDecor.top;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.EarthBlocks;
import com.mechanical.Mechanical;
import com.mechanical.Spinning;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Small things on open ground (docs/terrain-plan.md phase 4), placed by scripts/gen_land_decor.py: bare rock outcrops
 * flush with the turf, the bones of a long-dead animal, anthills, a lone old tree on the plain, the tumbled dry-stone
 * walls of an abandoned farm and an old fire ring. Boulders, cairns, standing stones, campsites and junipers already
 * have their own features (Boulder, Cairn, StandingStones, AbandonedCampsite, gen_earth's juniper).
 */
final class OpenDecor extends StoneAgeFeature {
    enum Kind { OUTCROP, BONES, ANTHILL, LONE_TREE, FARM_WALLS, FIRE_RING, ROOT_CELLAR }

    static final OpenDecor OUTCROP = new OpenDecor(Kind.OUTCROP), BONES = new OpenDecor(Kind.BONES), ANTHILL = new OpenDecor(Kind.ANTHILL),
            LONE_TREE = new OpenDecor(Kind.LONE_TREE), FARM_WALLS = new OpenDecor(Kind.FARM_WALLS), FIRE_RING = new OpenDecor(Kind.FIRE_RING),
            ROOT_CELLAR = new OpenDecor(Kind.ROOT_CELLAR);
    private static final String[] TREES = {"mechanical:linden", "mechanical:linden", "mechanical:ash", "minecraft:fancy_oak", "minecraft:fancy_oak"};

    private final Kind kind;
    private final MapCodec<OpenDecor> codec;

    private OpenDecor(Kind kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<OpenDecor> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!dryNatural(level, c.below()) || !level.getBlockState(c).canBeReplaced() || !Decor.noStructure(level, c)) return false;
        BlockPos at = switch (kind) {
            case OUTCROP -> outcrop(level, random, c);
            case BONES -> bones(level, random, c, false);
            case ANTHILL -> anthill(level, random, c);
            case LONE_TREE -> loneTree(level, generator, random, c);
            case FARM_WALLS -> farmWalls(level, random, c);
            case FIRE_RING -> fireRing(level, random, c);
            case ROOT_CELLAR -> rootCellar(level, random, c);
        };
        if (at != null) Decor.logPlaced(kind.name().toLowerCase(), at);
        return at != null;
    }

    // ------------------------------------------------------------------ the kinds

    /**
     * A rock outcrop (häll): a low whaleback of bedrock worn smooth by the ice, 6-13 long and up to 1-2.5 high, gentle on
     * the side the ice came from and steeper on its lee, its back rounded in half blocks (the rock's slabs and stairs) and
     * sinking into the turf at its edges. The rock is what lies under the ground there (Stone, Granite, Gneiss ...), Granite
     * where none is near, with veins of a second stone; moss and lichen on it, grass and flowers crowding its foot.
     */
    private static BlockPos outcrop(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD) || !flat(level, c, 3, 1)) return null;
        BlockState found = MountainDecor.rockUnder(level, c);
        Block rock = (found != null && found.is(BlockTags.BASE_STONE_OVERWORLD) ? found : (random.nextBoolean() ? Blocks.GRANITE : Blocks.STONE).defaultBlockState()).getBlock();
        Block second = MountainDecor.secondStone(rock);
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        double rx = 3 + random.nextDouble() * 3.5, rz = 2 + random.nextDouble() * 2, ang = random.nextDouble() * Math.PI, hmax = 1.1 + random.nextDouble() * 1.4;
        double cos = Math.cos(ang), sin = Math.sin(ang);
        int ox = c.getX(), oz = c.getZ(), r = (int) Math.ceil(rx) + 2;
        java.util.Map<Long, Integer> groundY = new java.util.HashMap<>();
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++) groundY.put(BlockPos.asLong(ox + x, 0, oz + z), top(level, ox + x, oz + z).getY());
        Shaper.Density back = (x, y, z) -> {
            double dx = x - ox - 0.5, dz = z - oz - 0.5, u = dx * cos + dz * sin, v = -dx * sin + dz * cos;
            double e = Math.sqrt(sq(u / rx) + sq(v / rz)) + 0.22 * noise.get(x * 0.3, z * 0.3);
            if (e >= 1) return -1;
            double lee = 1 + 0.45 * Math.max(-0.6, Math.min(0.6, u / rx)); // steeper toward +u
            double t = hmax * Math.pow(1 - e, 0.65 * lee) + 0.2 * noise.get(x * 0.6, z * 0.6) - 0.35;
            Integer g = groundY.get(BlockPos.asLong((int) Math.floor(x), 0, (int) Math.floor(z)));
            return g == null ? -1 : g + t - y;
        };
        Shaper.Stone stone = p -> Math.abs(noise.get(p.getX() * 0.18 + 30, p.getZ() * 0.18)) < 0.08 ? second : rock;
        var cells = Shaper.build(level, ox - r, c.getY() - 2, oz - r, ox + r, c.getY() + 3, oz + r, back, stone, null);
        if (cells.stream().filter(k -> k.pos().getY() >= c.getY()).count() < 8) return null;
        // moss and lichen in patches on its back; grass and flowers at its foot
        for (BlockPos p : Shaper.tops(level, cells))
            if (noise.get(p.getX() * 0.35 + 7, p.getZ() * 0.35) > 0.35) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
        for (int i = 0; i < 14; i++) {
            BlockPos p = top(level, ox + random.nextInt(2 * r + 1) - r, oz + random.nextInt(2 * r + 1) - r);
            BlockState plant = (random.nextInt(4) == 0 ? Blocks.FERN : Blocks.SHORT_GRASS).defaultBlockState();
            if (level.getBlockState(p).isAir() && plant.canSurvive(level, p)) set(level, p, plant);
        }
        for (int i = 0; i < 3; i++) Decor.flower(level, random, top(level, ox + random.nextInt(2 * r + 1) - r, oz + random.nextInt(2 * r + 1) - r));
        return c;
    }

    /**
     * The bones of a long-dead animal: a backbone of Bone Blocks sunk flush in the ground, a Rib Cage over it (thin ribs
     * arching over the spine, some gone), a skull at the head end, a leg bone lying apart and Bone Piles about. buried:
     * half sunk in the sand, only the rib tips and the skull showing (the desert skeleton). Advancement Old Bones.
     */
    static BlockPos bones(WorldGenLevel level, RandomSource random, BlockPos c, boolean buried) {
        Direction dir = randomSide(random), side = dir.getClockWise();
        int len = 3 + random.nextInt(2);
        for (int i = -1; i <= len; i++)
            for (int s = -1; s <= 1; s++) {
                BlockPos t = top(level, c.relative(dir, i).relative(side, s));
                if (t.getY() != c.getY() || !dryNatural(level, t.below())) return null;
            }
        BlockState spine = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, dir.getAxis());
        BlockState ribs = Structures.RIB_CAGE.get().defaultBlockState().setValue(OldBonesBlock.FACING, dir).setValue(OldBonesBlock.LOW, buried);
        for (int i = 0; i < len; i++) {
            BlockPos at = c.relative(dir, i);
            if (i > 0 && i < len - 1 || random.nextBoolean()) set(level, at.below(buried ? 2 : 1), spine); // the tail end often gone
            clear(level, at);
            // the ribs: the chest (not the neck or the tail end); one gone now and then
            if (i > 0 && i < len - 1 && random.nextInt(5) > 0) set(level, at, ribs);
            else if (!buried && random.nextInt(3) == 0) onGround(level, at, Mechanical.BONE_PILE.get().defaultBlockState());
        }
        // the skull at the head end, turned a little, a leg bone and small bones lying apart
        BlockPos head = c.relative(dir, len);
        clear(level, head);
        int facing = Math.floorMod(Math.round(dir.toYRot() / 22.5F) + 8 + random.nextInt(5) - 2, 16);
        set(level, head, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, facing));
        BlockPos leg = top(level, c.relative(dir, random.nextInt(len)).relative(side, random.nextBoolean() ? 2 : -2));
        if (leg.getY() == c.getY() && level.getBlockState(leg).canBeReplaced() && dryNatural(level, leg.below())) {
            clear(level, leg);
            set(level, leg.below(), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, random.nextBoolean() ? dir.getAxis() : side.getAxis()));
        }
        for (int i = 0; i < 2; i++) {
            BlockPos p = top(level, c.getX() + random.nextInt(7) - 3, c.getZ() + random.nextInt(7) - 3);
            if (level.getBlockState(p).isAir()) onGround(level, p, Mechanical.BONE_PILE.get().defaultBlockState());
        }
        if (!buried) for (int i = 0; i < 3; i++) Decor.flower(level, random, top(level, c.getX() + random.nextInt(7) - 3, c.getZ() + random.nextInt(7) - 3));
        return c;
    }

    /**
     * An anthill (myrstack): a wood ants' mound, low and a little lopsided, 3-5 blocks across and up to a block and a half
     * high, heaped of needles and crumbs: its flanks in half blocks of Podzol and Coarse Dirt (their slabs and stairs,
     * Shaper) so it swells softly out of the forest floor, and only on its crown the Anthill itself, a small rounded cap of
     * needles. Round it a ragged skirt of the needles the ants drag in (Leaf Litter on Podzol) and twigs. In the
     * coniferous forest, a few on open ground near it.
     */
    private static BlockPos anthill(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) return Decor.reject("anthill:ground");
        if (!flat(level, c, 2, 1)) return Decor.reject("anthill:not_flat");
        for (int y = 0; y < 3; y++) if (!level.getBlockState(c.above(y)).canBeReplaced()) return Decor.reject("anthill:no_room");
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        boolean big = random.nextInt(3) > 0;
        double rad = big ? 2.1 + random.nextDouble() * 0.6 : 1.5 + random.nextDouble() * 0.4, hmax = big ? 1.5 + random.nextDouble() * 0.3 : 1.25;
        // a little lopsided: the crown off the middle toward the sun, the flank there steeper
        double ox = c.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, oz = c.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
        double sx = 1 + (random.nextDouble() - 0.5) * 0.35, sz = 1 + (random.nextDouble() - 0.5) * 0.35;
        int g = c.getY();
        Shaper.Density mound = (x, y, z) -> {
            double e = Math.sqrt(sq((x - ox) / (rad * sx)) + sq((z - oz) / (rad * sz))) + 0.2 * noise.get(x * 0.6, z * 0.6);
            // a blunt cone, not a dome: the flanks climb steadily to a rounded crown
            return e >= 1 ? -1 : g - 0.1 + hmax * Math.pow(1 - e, 1.15) * (1 + 0.25 * e) - y;
        };
        // podzol (the needle heap) over most of it, coarse dirt where the ants' digging shows through low down
        Shaper.Stone stuff = p -> p.getY() < g + 1 && noise.get(p.getX() * 0.6 + 5, p.getZ() * 0.6) > 0.35 ? Blocks.COARSE_DIRT : Blocks.PODZOL;
        int r = (int) Math.ceil(rad) + 1;
        for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) clear(level, top(level, c.getX() + dx, c.getZ() + dz));
        var cells = Shaper.build(level, c.getX() - r, g, c.getZ() - r, c.getX() + r, g + 2, c.getZ() + r, mound, stuff, Blocks.COARSE_DIRT);
        if (cells.size() < 4) return Decor.reject("anthill:small");
        // the crown: the cap on the highest full block of the middle (in place of a half block lying on it)
        BlockPos crown = c;
        while (crown.getY() < g + 3 && !level.getBlockState(crown).canBeReplaced() && level.getBlockState(crown).isCollisionShapeFullBlock(level, crown)) crown = crown.above();
        set(level, crown, Structures.ANTHILL.get().defaultBlockState());
        // the skirt: the ground round it worn to podzol, loose needles and twigs the ants drag in
        for (int dx = -r - 2; dx <= r + 2; dx++)
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                if (Math.abs(t.getY() - g) > 1 || !dryNatural(level, t.below())) continue;
                double d = Math.sqrt(sq(c.getX() + dx + 0.5 - ox) + sq(c.getZ() + dz + 0.5 - oz)) - rad + 0.6 * noise.get(dx * 0.6, dz * 0.6);
                if (d > 2.2 || d < -0.2) continue;
                if (level.getBlockState(t.below()).is(BlockTags.SUBSTRATE_OVERWORLD) && random.nextInt(3) > 0) set(level, t.below(), Blocks.PODZOL.defaultBlockState());
                if (random.nextInt(5) == 0) continue;
                if (random.nextInt(4) == 0) onGround(level, t, Mechanical.TWIGS.get().defaultBlockState());
                else litter(level, random, t);
            }
        for (BlockPos p : Shaper.tops(level, cells)) if (!p.equals(crown.below()) && random.nextInt(3) == 0) litter(level, random, p.above());
        return c;
    }

    /** A little Leaf Litter (spruce needles and leaves) at p, a random part of the block covered. */
    private static void litter(WorldGenLevel level, RandomSource random, BlockPos p) {
        BlockState s = Blocks.LEAF_LITTER.defaultBlockState();
        s = s.trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SEGMENT_AMOUNT, 1 + random.nextInt(4))
                .trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, randomSide(random));
        if (level.getBlockState(p).isAir() && s.canSurvive(level, p)) set(level, p, s);
    }

    /**
     * A lone old tree on the open plain (a vårdträd): a big Linden, Ash or Oak where no other tree stands within 14 blocks,
     * flowers and a stone at its foot.
     */
    private static BlockPos loneTree(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos c) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD) || !flat(level, c, 3, 1) || ForestDecor.woodNear(level, c, 14) != null) return null;
        var tree = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Identifier.parse(TREES[random.nextInt(TREES.length)]));
        clear(level, c);
        if (tree == null || !tree.place(level, generator, random, c)) return null;
        for (int i = 0; i < 6; i++) Decor.flower(level, random, top(level, c.getX() + random.nextInt(9) - 4, c.getZ() + random.nextInt(9) - 4));
        if (random.nextBoolean()) Decor.rocks(level, random, c, 2, 2, 3, Mechanical.LOOSE_STONE.get());
        return c;
    }

    /**
     * The tumbled walls of an abandoned farm: a field 11-17 by 9-14 enclosed by low dry-stone walls that run as people
     * laid them, not quite straight (each side bows a little, its corners not square), following the lie of the land. A
     * course of big stones (Fieldstone, Mossy Cobblestone) with Dry Stone Wall on top in the stretches still standing,
     * Fieldstone Slabs where the top course has slid off, long runs fallen to stones in the grass with some tumbled out
     * beside them; moss on the stones, grass and ferns crowding their feet. A gateway between two taller stones, the
     * corner of a house inside (its foundation course and a heap of hearth stones) with nettles, which follow people.
     * Only on a level, dry stretch of the plains with no water near; rare.
     */
    private static BlockPos farmWalls(WorldGenLevel level, RandomSource random, BlockPos c) {
        int w = 11 + random.nextInt(7), d = 9 + random.nextInt(6);
        int blocked = 0; // a bush or a lone tree in the field is fine, a slope or a pond is not
        for (int x = -w / 2 - 3; x <= w / 2 + 3; x += 2)
            for (int z = -d / 2 - 3; z <= d / 2 + 3; z += 2) {
                BlockPos t = top(level, c.getX() + x, c.getZ() + z);
                if (!level.getFluidState(t).isEmpty() || !level.getFluidState(t.below()).isEmpty()) return Decor.reject("farm_walls:water");
                if (Math.abs(x) > w / 2 || Math.abs(z) > d / 2) continue; // round the field: only no water
                if (!dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced()) blocked++; // a tree's crown or a bush
                else if (Math.abs(t.getY() - c.getY()) > 1) return Decor.reject("farm_walls:not_level");
            }
        if (blocked > 4) return Decor.reject("farm_walls:wooded");
        // the corners, each pushed a little off square, and the sides between them bowing
        double[][] k = new double[4][];
        int[][] sq = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int i = 0; i < 4; i++) k[i] = new double[] {c.getX() + sq[i][0] * w / 2.0 + (random.nextDouble() - 0.5) * 2.4, c.getZ() + sq[i][1] * d / 2.0 + (random.nextDouble() - 0.5) * 2.4};
        List<BlockPos> ring = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int[] sideStart = new int[4];
        for (int i = 0; i < 4; i++) {
            sideStart[i] = ring.size();
            double[] a = k[i], b = k[(i + 1) % 4];
            double bow = (random.nextDouble() - 0.5) * 2.2, len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            double nx = -(b[1] - a[1]) / len, nz = (b[0] - a[0]) / len;
            int lx = Integer.MIN_VALUE, lz = 0;
            for (double t = 0; t < 1; t += 0.25 / len) {
                double off = bow * 4 * t * (1 - t);
                int x = (int) Math.floor(a[0] + (b[0] - a[0]) * t + nx * off), z = (int) Math.floor(a[1] + (b[1] - a[1]) * t + nz * off);
                if (lx != Integer.MIN_VALUE && x != lx && z != lz && seen.add(BlockPos.asLong(x, 0, lz))) ring.add(new BlockPos(x, 0, lz)); // no diagonal gaps
                if (seen.add(BlockPos.asLong(x, 0, z))) ring.add(new BlockPos(x, 0, z));
                lx = x;
                lz = z;
            }
        }
        BlockState wall = EarthBlocks.DRY_STONE_WALL.get().defaultBlockState();
        List<BlockPos> walls = new ArrayList<>();
        int gate = sideStart[0] + 2 + random.nextInt(Math.max(1, sideStart[1] - sideStart[0] - 5));
        boolean standing = true;
        for (int i = 0; i < ring.size(); i++) {
            if (i > 3 && random.nextInt(standing ? 10 : 5) == 0) standing = !standing; // standing runs of about 10, fallen of 5
            BlockPos t = top(level, ring.get(i).getX(), ring.get(i).getZ());
            if (!level.getBlockState(t).canBeReplaced() || !dryNatural(level, t.below()) || Math.abs(t.getY() - c.getY()) > 3) continue;
            int side = i < sideStart[1] ? 0 : i < sideStart[2] ? 1 : i < sideStart[3] ? 2 : 3;
            Direction out = side == 0 ? Direction.NORTH : side == 1 ? Direction.EAST : side == 2 ? Direction.SOUTH : Direction.WEST;
            clear(level, t);
            if (i == gate || i == gate + 1) continue;
            if (i == gate - 1 || i == gate + 2) { // the gateposts: two stones on end
                set(level, t, fieldstone(random));
                set(level, t.above(), EarthBlocks.FIELDSTONE.get().defaultBlockState());
                continue;
            }
            if (!standing) { // fallen: a stone sunk in the turf, a slab of it, a loose stone, now and then nothing
                switch (random.nextInt(5)) {
                    case 0 -> set(level, t.below(), fieldstone(random));
                    case 1 -> set(level, t, EarthBlocks.FIELDSTONE_SLAB.get().defaultBlockState());
                    case 2 -> rock(level, random, t, Mechanical.LOOSE_STONE.get());
                    case 3 -> tumble(level, random, t, random.nextBoolean() ? out : out.getOpposite());
                    default -> {}
                }
                continue;
            }
            set(level, t, fieldstone(random));
            int top = random.nextInt(6);
            if (top < 3 && level.getBlockState(t.above()).canBeReplaced()) {
                set(level, t.above(), wall);
                walls.add(t.above());
            } else if (top == 3) set(level, t.above(), EarthBlocks.FIELDSTONE_SLAB.get().defaultBlockState());
            else if (top == 4) set(level, t.above(), Blocks.MOSS_CARPET.defaultBlockState());
            if (random.nextInt(6) == 0) tumble(level, random, t, random.nextBoolean() ? out : out.getOpposite());
        }
        for (BlockPos p : walls) set(level, p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p));
        // grass and ferns crowding the walls' feet
        for (BlockPos q : ring) {
            if (random.nextInt(3) > 0) continue;
            BlockPos p = top(level, q.getX() + random.nextInt(3) - 1, q.getZ() + random.nextInt(3) - 1);
            BlockState plant = (random.nextInt(3) == 0 ? Blocks.FERN : Blocks.TALL_GRASS).defaultBlockState();
            if (level.getBlockState(p).isAir() && plant.canSurvive(level, p)) {
                if (plant.is(Blocks.TALL_GRASS)) {
                    if (level.getBlockState(p.above()).isAir()) net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, plant, p, Block.UPDATE_CLIENTS);
                } else set(level, p, plant);
            }
        }
        // the corner of a house: its foundation course along two sides of one corner, the hearth a heap of stones
        double[] hc = k[random.nextInt(4)];
        int hx = (int) Math.floor((hc[0] * 3 + c.getX()) / 4), hz = (int) Math.floor((hc[1] * 3 + c.getZ()) / 4);
        int sx = c.getX() > hx ? 1 : -1, sz = c.getZ() > hz ? 1 : -1;
        for (int i = 0; i < 5; i++)
            for (BlockPos p : new BlockPos[] {new BlockPos(hx + i * sx, 0, hz), new BlockPos(hx, 0, hz + i * sz)}) {
                BlockPos t = top(level, p.getX(), p.getZ());
                if (random.nextInt(4) > 0 && level.getBlockState(t).canBeReplaced() && dryNatural(level, t.below())) {
                    clear(level, t);
                    set(level, t.below(), fieldstone(random));
                    if (random.nextInt(3) == 0) set(level, t, EarthBlocks.FIELDSTONE_SLAB.get().defaultBlockState());
                }
            }
        BlockPos hearth = top(level, hx + 2 * sx, hz + 2 * sz);
        if (level.getBlockState(hearth).canBeReplaced() && dryNatural(level, hearth.below())) {
            clear(level, hearth);
            set(level, hearth, Blocks.COBBLESTONE.defaultBlockState());
            set(level, hearth.relative(sx > 0 ? Direction.EAST : Direction.WEST), EarthBlocks.FIELDSTONE_SLAB.get().defaultBlockState());
            set(level, hearth.below(), ScorchedBarrens.WOOD_ASH.get().defaultBlockState());
        }
        BlockState nettle = Spinning.STINGING_NETTLE.get().defaultBlockState();
        for (int i = 0; i < 12; i++) {
            BlockPos p = top(level, hx + sx * random.nextInt(5), hz + sz * random.nextInt(5));
            if (level.getBlockState(p).isAir() && nettle.canSurvive(level, p)) set(level, p, nettle);
        }
        return c;
    }

    /**
     * A root cellar (jordkällare): an old store room dug into the foot of a hillside, only its stone face showing. The
     * face of dry-laid Fieldstone with a low doorway, a big stone across its top and an old Spruce Door, set into the
     * slope with turf running on over it; the hill over the room rounded out with earth where it is too thin (a mound
     * smooth in half blocks, Grass Slab and Grass Stairs, that runs into the hillside), and in front a little yard cut
     * into the slope, held on both sides by wing walls that step down with the banks of earth behind them. Inside, a room
     * three wide and four deep, walled and vaulted in Fieldstone (its ceiling's edges in upside-down stairs), a floor of
     * trodden earth, a shelf of planks at the back, pots (one holding what was stored there), cobwebs in the corners.
     * Nettles at the door. Only where a hillside rises behind level ground; rare, on plains, meadows and downs.
     */
    static BlockPos rootCellar(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) return Decor.reject("root_cellar:ground");
        if (ForestDecor.woodNear(level, c, 4) != null) return Decor.reject("root_cellar:wood");
        int g = c.getY();
        // the hillside: which way the land rises behind c, steadily but not as a cliff, with level ground in front
        Direction face = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (hillside(level, c, d)) {
                face = d;
                break;
            }
        }
        if (face == null) return Decor.reject("root_cellar:no_slope");
        Direction back = face.getOpposite(), right = face.getClockWise();
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        BlockPos o = c; // the face's middle at the doorway
        java.util.function.BiFunction<Integer, Integer, BlockPos> at = (a, s) -> o.relative(back, a).relative(right, s);
        int bx = back.getStepX(), bz = back.getStepZ(), rx = right.getStepX(), rz = right.getStepZ();
        // 1. the earth: a broad round mound over the room (its peak over the room's middle) and banks flanking the yard,
        // stepping down toward the front; laid only above the land, so where the hill is higher it stays as it is
        double ra = 4.4 + random.nextDouble() * 0.6, rs = 3.9 + random.nextDouble() * 0.5, hm = 3.7 + random.nextDouble() * 0.5;
        int r = 8;
        java.util.Map<Long, Integer> groundY = new java.util.HashMap<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) groundY.put(BlockPos.asLong(o.getX() + dx, 0, o.getZ() + dz), top(level, o.getX() + dx, o.getZ() + dz).getY());
        Shaper.Density earthTop = (x, y, z) -> {
            Integer gy = groundY.get(BlockPos.asLong((int) Math.floor(x), 0, (int) Math.floor(z)));
            if (gy == null || y < gy) return -1;
            double ax = (x - o.getX() - 0.5) * bx + (z - o.getZ() - 0.5) * bz, sx = (x - o.getX() - 0.5) * rx + (z - o.getZ() - 0.5) * rz;
            double n = 0.12 * noise.get(x * 0.35, z * 0.35), top = -9;
            if (ax >= -0.5) { // the mound
                double e = Math.sqrt(sq((ax - 2.5) / ra) + sq(sx / rs)) + n;
                if (e < 1) top = g + hm * Math.sqrt(1 - e * e);
            }
            double side = Math.abs(sx);
            if (ax > -3 && ax < 0.5 && side > 1.5 && side < 5) // the banks beside the yard, low toward the front and outward
                top = Math.max(top, g + (2.9 + ax * 0.95) * Math.min(1, (5 - side) / 2.5) + n);
            return top - y;
        };
        Shaper.Stone earth = p -> earthTop.at(p.getX() + 0.5, p.getY() + 1.25, p.getZ() + 0.5) > 0 ? Blocks.DIRT : Blocks.GRASS_BLOCK;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) clear(level, top(level, o.getX() + dx, o.getZ() + dz));
        var turf = Shaper.build(level, o.getX() - r, g - 1, o.getZ() - r, o.getX() + r, g + 6, o.getZ() + r, earthTop, earth, null);
        // 2. the yard in front, cut level into the slope
        for (int a = -3; a <= -1; a++)
            for (int s = -1; s <= 1; s++) {
                BlockPos p = at.apply(a, s).atY(g);
                for (int y = 0; y <= 4; y++) if (!level.getBlockState(p.above(y)).isAir()) set(level, p.above(y), AIR);
                BlockState under = level.getBlockState(p.below());
                if (!under.is(BlockTags.SUBSTRATE_OVERWORLD) || under.is(Blocks.DIRT)) set(level, p.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        // 3. the room and its walls: Fieldstone round a hollow two high, vaulted
        BlockState stone = EarthBlocks.FIELDSTONE.get().defaultBlockState();
        for (int a = 0; a <= 5; a++)
            for (int s = -2; s <= 2; s++)
                for (int y = g - 1; y <= g + 2; y++) {
                    BlockPos p = at.apply(a, s).atY(y);
                    boolean inside = a >= 1 && a <= 4 && Math.abs(s) <= 1 && y >= g && y <= g + 1;
                    if (inside) set(level, p, AIR);
                    else if (a > 0 || Math.abs(s) <= 1) set(level, p, random.nextInt(6) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : stone);
                }
        for (int a = 1; a <= 4; a++)
            for (int s : new int[] {-1, 1}) {
                Direction toWall = s < 0 ? right.getOpposite() : right;
                set(level, at.apply(a, s).atY(g + 1), EarthBlocks.FIELDSTONE_STAIRS.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, toWall).setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
            }
        for (int a = 1; a <= 4; a++) for (int s = -1; s <= 1; s++) set(level, at.apply(a, s).atY(g - 1), (random.nextInt(3) == 0 ? Blocks.COARSE_DIRT : Blocks.PACKED_MUD).defaultBlockState());
        // the face: a big stone over the doorway, the door
        set(level, at.apply(0, 0).atY(g + 2), Blocks.STONE.defaultBlockState()); // the lintel
        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, back)
                .setValue(net.minecraft.world.level.block.DoorBlock.HINGE, random.nextBoolean() ? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT : net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT);
        set(level, at.apply(0, 0).atY(g), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        set(level, at.apply(0, 0).atY(g + 1), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        // over the lintel the turf runs on: a grass slab lip where the mound left the top of the face bare
        for (int s = -1; s <= 1; s++) {
            BlockPos p = at.apply(0, s).atY(g + 3);
            if (level.getBlockState(p).canBeReplaced()) set(level, p, EarthBlocks.GRASS_SLAB.get().defaultBlockState());
        }
        // the wing walls: along both sides of the yard, stepping down with the banks behind them
        for (int a = 0; a >= -2; a--)
            for (int s : new int[] {-2, 2}) {
                int hw = a == 0 ? 3 : a == -1 ? 2 : 1;
                for (int y = g - 1; y < g + hw; y++) set(level, at.apply(a, s).atY(y), random.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : stone);
                BlockPos cap = at.apply(a, s).atY(g + hw);
                if (level.getBlockState(cap).canBeReplaced() || level.getBlockState(cap).is(EarthBlocks.GRASS_SLAB.get()))
                    set(level, cap, (random.nextInt(3) == 0 ? EarthBlocks.GRASS_SLAB.get() : EarthBlocks.FIELDSTONE_SLAB.get()).defaultBlockState());
            }
        // 4. inside: a shelf at the back, pots, cobwebs
        for (int s = -1; s <= 1; s++)
            set(level, at.apply(4, s).atY(g), Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        pot(level, random, at.apply(3, random.nextBoolean() ? -1 : 1).atY(g), "root_cellar");
        if (random.nextBoolean()) set(level, at.apply(4, random.nextInt(3) - 1).atY(g + 1), Blocks.DECORATED_POT.defaultBlockState());
        for (int s : new int[] {-1, 1}) if (random.nextBoolean()) set(level, at.apply(random.nextBoolean() ? 1 : 4, s).atY(g + 1), Blocks.COBWEB.defaultBlockState());
        // the turf: grass and ferns on the mound, nettles by the door
        for (Shaper.Cell k : turf) {
            BlockPos p = k.pos().above();
            if (!level.getBlockState(p).isAir() || random.nextInt(3) > 0) continue;
            BlockState plant = (random.nextInt(5) == 0 ? Blocks.FERN : Blocks.SHORT_GRASS).defaultBlockState();
            if (plant.canSurvive(level, p)) set(level, p, plant);
        }
        BlockState nettle = Spinning.STINGING_NETTLE.get().defaultBlockState();
        for (int i = 0; i < 6; i++) {
            BlockPos p = top(level, at.apply(-1 - random.nextInt(3), random.nextInt(5) - 2));
            if (level.getBlockState(p).isAir() && nettle.canSurvive(level, p)) set(level, p, nettle);
        }
        Decor.logPlaced("root_cellar_dir_" + Decor.viewAngle(face), at.apply(-1, 0).atY(g));
        return at.apply(-1, 0).atY(g);
    }

    /** Behind c (away from face) the land rises steadily, 1-3 by 2 out, 2 by 4, 3-6 by 6, all across 3; in front it lies level; all dry ground. */
    private static boolean hillside(WorldGenLevel level, BlockPos c, Direction face) {
        Direction back = face.getOpposite(), right = face.getClockWise();
        for (int s = -1; s <= 1; s++) {
            int[] h = new int[10];
            for (int a = -3; a <= 6; a++) {
                BlockPos t = top(level, c.relative(back, a).relative(right, s));
                if (!dryNatural(level, t.below())) return false;
                h[a + 3] = t.getY() - c.getY();
            }
            if (h[5] < 1 || h[7] < 2 || h[9] < 3 || h[9] > 6 || h[4] > 2 || h[5] > 3) return false; // a slope, not a wall
            for (int a = 0; a < 3; a++) if (Math.abs(h[a]) > 1) return false;
        }
        return true;
    }

    /** One stone of a dry-stone wall: mostly Fieldstone, some mossy, a few plain cobbles. */
    private static BlockState fieldstone(RandomSource random) {
        int k = random.nextInt(10);
        return (k < 6 ? EarthBlocks.FIELDSTONE.get() : k < 9 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState();
    }

    /** A stone tumbled a block or two out from the wall at t, toward out: half sunk in the turf, or a loose stone. */
    private static void tumble(WorldGenLevel level, RandomSource random, BlockPos t, Direction out) {
        BlockPos f = top(level, t.relative(out, 1 + random.nextInt(2)));
        if (Math.abs(f.getY() - t.getY()) > 1 || !dryNatural(level, f.below()) || !level.getBlockState(f).canBeReplaced()) return;
        if (random.nextBoolean()) set(level, f.below(), fieldstone(random));
        else rock(level, random, f, Mechanical.LOOSE_STONE.get());
    }

    /** An old fire ring: a ring of stones sunk in the turf around a bed of Wood Ash and charred wood, grass creeping back. */
    private static BlockPos fireRing(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 2, 0)) return null;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 2.3) continue;
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                if (t.getY() != c.getY()) continue;
                clear(level, t);
                if (d < 1.2) { // the hearth: ash and a charred brand sunk in it, grass coming back
                    if (d < 0.5 && random.nextInt(3) > 0) set(level, t.below(), ForestDecor.pillar(ScorchedBarrens.CHARRED_LOG.get(), random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z));
                    else if (random.nextInt(4) > 0) set(level, t.below(), (random.nextInt(3) == 0 ? Blocks.COARSE_DIRT : ScorchedBarrens.WOOD_ASH.get()).defaultBlockState());
                    if (random.nextInt(4) == 0) set(level, t, Blocks.SHORT_GRASS.defaultBlockState());
                } else if (random.nextInt(6) > 0) { // the ring: stones sunk in the turf, loose ones on some
                    set(level, t.below(), (random.nextInt(3) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState());
                    if (random.nextBoolean()) rock(level, random, t, Mechanical.LOOSE_STONE.get());
                }
            }
        }
        // a seat: an old log a little way off
        Direction s = randomSide(random);
        List<BlockPos> seat = ForestDecor.logCells(level, top(level, c.relative(s, 3).relative(s.getClockWise(), -1)), s.getClockWise(), 3, false);
        if (seat.size() >= 2 && random.nextBoolean()) {
            ForestDecor.lay(level, seat, Blocks.OAK_LOG, s.getClockWise().getAxis());
            ForestDecor.dressLog(level, random, seat, s.getClockWise().getAxis(), 0.3F);
        }
        return c;
    }
}
