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
            case ROOT_CELLAR -> rootCellar(level, random, c, randomSide(random));
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
     * An anthill (myrstack): a wood ants' mound of needles and twigs, a smooth dome (Anthill: a small one a block and a
     * quarter across, or a big one two blocks high and two and a half across), in a ragged skirt of the needles the ants
     * drag in (Leaf Litter on Podzol) with twigs about it. In the coniferous forest, a few on open ground near it.
     */
    private static BlockPos anthill(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD) || !flat(level, c, 1, 0)) return null;
        boolean big = random.nextInt(3) == 0;
        if (big && !level.getBlockState(c.above()).canBeReplaced()) return null;
        BlockState hill = Structures.ANTHILL.get().defaultBlockState();
        // the mound's skirt: the ground under and around it worn to podzol, its plants gone (the big dome overhangs a block round)
        int r = big ? 2 : 1;
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        for (int dx = -r - 2; dx <= r + 2; dx++)
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                if (Math.abs(t.getY() - c.getY()) > 1 || !dryNatural(level, t.below())) continue;
                double d = Math.sqrt(dx * dx + dz * dz) + 0.6 * noise.get(dx * 0.6, dz * 0.6);
                if (d <= r + 0.5) {
                    for (int y = 0; y < 2; y++) if (!level.getBlockState(t.above(y)).isAir() && level.getBlockState(t.above(y)).canBeReplaced()) set(level, t.above(y), AIR);
                    if (level.getBlockState(t.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) set(level, t.below(), Blocks.PODZOL.defaultBlockState());
                } else if (d <= r + 2.2 && random.nextInt(5) > 0) { // the ring of loose needles the ants drag in
                    clear(level, t);
                    if (random.nextInt(4) == 0) onGround(level, t, Mechanical.TWIGS.get().defaultBlockState());
                    else litter(level, random, t);
                }
            }
        if (big) {
            set(level, c, hill.setValue(AnthillBlock.PART, AnthillBlock.Part.BASE));
            set(level, c.above(), hill.setValue(AnthillBlock.PART, AnthillBlock.Part.TOP));
        } else set(level, c, hill);
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
     * corner of a house inside (its foundation course and a heap of hearth stones) with nettles, which follow people,
     * and now and then the farm's root cellar dug in nearby. Plains only, rare.
     */
    private static BlockPos farmWalls(WorldGenLevel level, RandomSource random, BlockPos c) {
        int w = 11 + random.nextInt(7), d = 9 + random.nextInt(6);
        int blocked = 0; // a bush or a lone tree in the field is fine, a slope or a pond is not
        for (int x = -w / 2; x <= w / 2; x += 2)
            for (int z = -d / 2; z <= d / 2; z += 2) {
                BlockPos t = top(level, c.getX() + x, c.getZ() + z);
                if (!level.getFluidState(t).isEmpty()) return null;
                if (!dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced()) blocked++; // a tree's crown or a bush
                else if (Math.abs(t.getY() - c.getY()) > 2) return null;
            }
        if (blocked > 4) return null;
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
        // the root cellar the farm kept its turnips in, in the yard, its door toward the house
        if (random.nextInt(3) == 0 && Math.min(w, d) >= 12) {
            int dx = hx - c.getX(), dz = hz - c.getZ();
            Direction toHouse = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            rootCellar(level, random, top(level, c.relative(toHouse.getOpposite(), 1)), toHouse);
        }
        return c;
    }

    /**
     * A root cellar (jordkällare): an old store room dug two blocks into the ground and heaped over with earth, a grassy
     * mound 9-12 across standing 3-4 above the land, its turf smooth in half blocks (Grass Slab, Grass Stairs), so the
     * room is covered all over. In front a face of dry-laid Fieldstone with a low doorway, a big stone across its top and
     * an old Spruce Door; two steps down to it between little wing walls. Inside, a room three wide and four deep, walled
     * and vaulted in Fieldstone (its ceiling's edges in upside-down stairs), a floor of trodden earth, a shelf of planks at
     * the back, pots (one holding what was stored there), cobwebs in the corners. Nettles at the door. Rare, on plains and
     * meadows, and now and then by an abandoned farm's walls.
     */
    static BlockPos rootCellar(WorldGenLevel level, RandomSource random, BlockPos c, Direction face) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD) || !flat(level, c, 4, 1) || ForestDecor.woodNear(level, c, 3) != null) return null;
        Direction back = face.getOpposite(), right = face.getClockWise();
        var noise = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        int g = c.getY(), y0 = g - 2; // the land's first free block, the cellar's floor two below it (its vault flush with the land)
        // the room: s -1..1 across, a 1..4 back from the face (a 0), floor y0, two high
        BlockPos o = c; // the face's middle at the doorway
        java.util.function.BiFunction<Integer, Integer, BlockPos> at = (a, s) -> o.relative(back, a).relative(right, s);
        // 1. the mound: a lopsided dome over the room, highest a little behind its middle, cut off at the face
        double mx = o.getX() + 0.5 + back.getStepX() * 2.6, mz = o.getZ() + 0.5 + back.getStepZ() * 2.6;
        double ra = 5.2 + random.nextDouble(), rs = 4.4 + random.nextDouble() * 0.8, hm = 3.4 + random.nextDouble() * 0.6;
        int bx = back.getStepX(), bz = back.getStepZ(), rx = right.getStepX(), rz = right.getStepZ();
        Shaper.Density mound = (x, y, z) -> {
            double dx = x - mx, dz = z - mz, a = dx * bx + dz * bz, s = dx * rx + dz * rz;
            if (a < -2.6) return -1; // the face
            double e = Math.sqrt(sq(a / ra) + sq(s / rs)) + 0.1 * noise.get(x * 0.4, z * 0.4);
            return e >= 1 ? -1 : g - 0.4 + hm * Math.pow(1 - e * e, 0.8) - y;
        };
        Shaper.Stone earth = p -> mound.at(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5) > 0 ? Blocks.DIRT : Blocks.GRASS_BLOCK;
        int r = 9;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) clear(level, top(level, o.getX() + dx, o.getZ() + dz));
        var turf = Shaper.build(level, o.getX() - r, g - 1, o.getZ() - r, o.getX() + r, g + 4, o.getZ() + r, mound, earth, null);
        if (turf.size() < 20) return null;
        // 2. the room and its walls: Fieldstone round a hollow two high, vaulted
        BlockState stone = EarthBlocks.FIELDSTONE.get().defaultBlockState();
        for (int a = 0; a <= 5; a++)
            for (int s = -2; s <= 2; s++)
                for (int y = y0 - 1; y <= y0 + 2; y++) {
                    BlockPos p = at.apply(a, s).atY(y);
                    boolean inside = a >= 1 && a <= 4 && Math.abs(s) <= 1 && y >= y0 && y <= y0 + 1;
                    boolean shell = !inside && (a <= 5 && Math.abs(s) <= 2);
                    if (inside) set(level, p, AIR);
                    else if (shell && (y > y0 - 1 || a > 0)) set(level, p, random.nextInt(6) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : stone);
                }
        // the vault: upside-down stairs along the ceiling's two long edges
        for (int a = 1; a <= 4; a++)
            for (int s : new int[] {-1, 1}) {
                Direction toWall = s < 0 ? right.getOpposite() : right;
                set(level, at.apply(a, s).atY(y0 + 1), EarthBlocks.FIELDSTONE_STAIRS.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, toWall).setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
            }
        // the floor of trodden earth; the face: a big stone over the doorway, the door
        for (int a = 1; a <= 4; a++) for (int s = -1; s <= 1; s++) set(level, at.apply(a, s).atY(y0 - 1), (random.nextInt(3) == 0 ? Blocks.COARSE_DIRT : Blocks.PACKED_MUD).defaultBlockState());
        set(level, at.apply(0, 0).atY(y0 + 2), Blocks.STONE.defaultBlockState()); // the lintel
        set(level, at.apply(0, -1).atY(y0 + 2), stone);
        set(level, at.apply(0, 1).atY(y0 + 2), stone);
        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, back)
                .setValue(net.minecraft.world.level.block.DoorBlock.HINGE, random.nextBoolean() ? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT : net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT);
        set(level, at.apply(0, 0).atY(y0), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        set(level, at.apply(0, 0).atY(y0 + 1), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        // the face goes on up where the mound is cut off behind it, a slab on top; the entry: two steps down between wing walls
        for (int s = -2; s <= 2; s++) {
            int y = y0 + 3;
            for (; y <= g + 3 && !level.getBlockState(at.apply(1, s).atY(y)).canBeReplaced(); y++) set(level, at.apply(0, s).atY(y), stone);
            if (level.getBlockState(at.apply(0, s).atY(y)).canBeReplaced()) set(level, at.apply(0, s).atY(y), EarthBlocks.FIELDSTONE_SLAB.get().defaultBlockState());
        }
        for (int a = -1; a >= -2; a--)
            for (int s : new int[] {-1, 1}) {
                set(level, at.apply(a, s).atY(y0), stone);
                set(level, at.apply(a, s).atY(y0 + 1), stone);
                if (a == -1 && random.nextBoolean()) set(level, at.apply(a, s).atY(g), EarthBlocks.FIELDSTONE_SLAB.get().defaultBlockState());
            }
        BlockPos pit = at.apply(-1, 0).atY(y0);
        for (int y = 0; y <= 2; y++) set(level, pit.above(y), AIR);
        set(level, pit.below(), Blocks.PACKED_MUD.defaultBlockState());
        BlockState stair = EarthBlocks.FIELDSTONE_STAIRS.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, face);
        set(level, at.apply(-2, 0).atY(y0), stair);
        set(level, at.apply(-2, 0).atY(y0 + 1), AIR);
        clear(level, at.apply(-2, 0).atY(g));
        set(level, at.apply(-3, 0).atY(y0 + 1), stair);
        clear(level, at.apply(-3, 0).atY(g));
        // 3. inside: a shelf at the back, pots, cobwebs
        for (int s = -1; s <= 1; s++)
            set(level, at.apply(4, s).atY(y0), Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        pot(level, random, at.apply(3, random.nextBoolean() ? -1 : 1).atY(y0), "root_cellar");
        if (random.nextBoolean()) set(level, at.apply(4, random.nextInt(3) - 1).atY(y0 + 1), Blocks.DECORATED_POT.defaultBlockState());
        for (int s : new int[] {-1, 1}) if (random.nextBoolean()) set(level, at.apply(random.nextBoolean() ? 1 : 4, s).atY(y0 + 1), Blocks.COBWEB.defaultBlockState());
        // the turf: grass and ferns on the mound, nettles by the door
        for (Shaper.Cell k : turf) {
            BlockPos p = k.pos().above();
            if (!level.getBlockState(p).isAir() || random.nextInt(3) > 0) continue;
            BlockState plant = (random.nextInt(5) == 0 ? Blocks.FERN : Blocks.SHORT_GRASS).defaultBlockState();
            if (plant.canSurvive(level, p)) set(level, p, plant);
        }
        BlockState nettle = Spinning.STINGING_NETTLE.get().defaultBlockState();
        for (int i = 0; i < 6; i++) {
            BlockPos p = top(level, at.apply(-1 - random.nextInt(3), random.nextInt(7) - 3));
            if (level.getBlockState(p).isAir() && nettle.canSurvive(level, p)) set(level, p, nettle);
        }
        Decor.logPlaced("root_cellar_dir_" + Decor.viewAngle(face), at.apply(-1, 0).atY(g));
        return at.apply(-1, 0).atY(g);
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
