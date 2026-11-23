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
    enum Kind { OUTCROP, BONES, ANTHILL, LONE_TREE, FARM_WALLS, FIRE_RING }

    static final OpenDecor OUTCROP = new OpenDecor(Kind.OUTCROP), BONES = new OpenDecor(Kind.BONES), ANTHILL = new OpenDecor(Kind.ANTHILL),
            LONE_TREE = new OpenDecor(Kind.LONE_TREE), FARM_WALLS = new OpenDecor(Kind.FARM_WALLS), FIRE_RING = new OpenDecor(Kind.FIRE_RING);
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
        };
        if (at != null) Decor.logPlaced(kind.name().toLowerCase(), at);
        return at != null;
    }

    // ------------------------------------------------------------------ the kinds

    /**
     * A rock outcrop (häll): bare bedrock worn smooth by the ice, flush with the turf in a ragged patch 5-11 across, a low
     * hump of a block in the middle now and then, grass and moss in its cracks, gravel at the edge. The rock is whatever
     * lies under the ground there (Stone, Granite, Gneiss ...), Granite where none is near.
     */
    private static BlockPos outcrop(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD) || !flat(level, c, 2, 1)) return null;
        BlockState found = MountainDecor.rockUnder(level, c);
        BlockState rock = found != null && found.is(BlockTags.BASE_STONE_OVERWORLD) ? found
                : (random.nextBoolean() ? Blocks.GRANITE : Blocks.STONE).defaultBlockState();
        double rx = 2.5 + random.nextDouble() * 3, rz = 2 + random.nextDouble() * 2.5, ang = random.nextDouble() * Math.PI;
        double cos = Math.cos(ang), sin = Math.sin(ang);
        // a crack or two across: lines where the turf stays
        double crack = random.nextDouble() * Math.PI, cc = Math.cos(crack), cs = Math.sin(crack), off = random.nextDouble() * 2 - 1;
        int r = (int) Math.ceil(rx) + 1, built = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double u = dx * cos + dz * sin, v = -dx * sin + dz * cos;
                double e = sq(u / rx) + sq(v / rz) + (random.nextDouble() - 0.5) * 0.35;
                if (e > 1.15) continue;
                BlockPos g = top(level, c.getX() + dx, c.getZ() + dz).below();
                if (Math.abs(g.getY() + 1 - c.getY()) > 1 || !dryNatural(level, g) || !level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD)) continue;
                if (Math.abs(dx * cc + dz * cs - off) < 0.5 && random.nextInt(3) > 0) continue; // the crack
                clear(level, g.above());
                if (e > 0.9) { // the ragged rim
                    if (random.nextInt(3) == 0) set(level, g, (random.nextBoolean() ? Blocks.GRAVEL : Blocks.COARSE_DIRT).defaultBlockState());
                    continue;
                }
                set(level, g, random.nextInt(7) == 0 ? weather(random, rock) : rock);
                if (e < 0.2 && random.nextInt(3) > 0 && level.getBlockState(g.above()).canBeReplaced()) set(level, g.above(), rock);
                else if (random.nextInt(9) == 0) set(level, g.above(), Blocks.MOSS_CARPET.defaultBlockState());
                built++;
            }
        }
        return built >= 8 ? c : null;
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
     * An anthill (myrstack): a smooth, steep dome of spruce needles over soil, 3-5 across and 1.5-2.5 high, built in half
     * blocks (Podzol and Podzol Slab, Coarse Dirt Slab low on its flanks) so it rounds off, its surface strewn with Leaf
     * Litter, more litter round its foot.
     */
    private static BlockPos anthill(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!level.getBlockState(c.below()).is(BlockTags.SUBSTRATE_OVERWORLD) || !flat(level, c, 2, 1)) return null;
        boolean big = random.nextInt(3) == 0;
        double r = big ? 2.6 : 1.8, height = big ? 2.5 : 1.5; // radius and height in blocks
        double ox = (random.nextDouble() - 0.5) * 0.6, oz = (random.nextDouble() - 0.5) * 0.6; // the peak a little off centre
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = Math.sqrt(sq(dx - ox) + sq(dz - oz));
                BlockPos t = top(level, c.getX() + dx, c.getZ() + dz);
                if (Math.abs(t.getY() - c.getY()) > 1 || !dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced()) continue;
                double h = height * (1 - Math.pow(d / r, 1.6)) + (c.getY() - t.getY()); // a steep cone with a round top, in blocks
                int halves = (int) Math.round(h * 2);
                if (halves <= 0) {
                    if (d < r + 1.8 && random.nextInt(3) > 0 && level.getBlockState(t).isAir()) litter(level, random, t);
                    continue;
                }
                clear(level, t);
                int y = 0;
                for (; halves >= 2; halves -= 2, y++) set(level, t.above(y), halves == 2 && random.nextInt(4) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PODZOL.defaultBlockState());
                if (halves == 1) set(level, t.above(y), (y == 0 && random.nextInt(3) == 0 ? EarthBlocks.COARSE_DIRT_SLAB : EarthBlocks.PODZOL_SLAB).get()
                        .defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                else if (random.nextInt(3) > 0) litter(level, random, t.above(y)); // needles on the flat of a full step
            }
        }
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
     * The tumbled walls of an abandoned farm: a rough rectangle 10-16 by 8-13 of low dry-stone wall following the lie of
     * the land, a course of big stones (Fieldstone, Mossy Cobblestone, now and then Cobblestone) with Dry Stone Wall on top
     * in stretches, long runs fallen to a scatter of stones in the grass, stones tumbled out beside it, a gateway gap,
     * the corner of a house foundation inside and nettles (they follow people). Plains only, rare.
     */
    private static BlockPos farmWalls(WorldGenLevel level, RandomSource random, BlockPos c) {
        int w = 10 + random.nextInt(7), d = 8 + random.nextInt(6);
        BlockPos o = c.offset(-w / 2, 0, -d / 2);
        for (int x = 0; x <= w; x += 2)
            for (int z = 0; z <= d; z += 2) {
                BlockPos t = top(level, o.getX() + x, o.getZ() + z);
                if (Math.abs(t.getY() - c.getY()) > 2 || !dryNatural(level, t.below()) || !level.getBlockState(t).canBeReplaced()) return null;
            }
        List<BlockPos> ring = new ArrayList<>();
        for (int x = 0; x < w; x++) ring.add(o.offset(x, 0, 0));
        for (int z = 0; z < d; z++) ring.add(o.offset(w, 0, z));
        for (int x = w; x > 0; x--) ring.add(o.offset(x, 0, d));
        for (int z = d; z > 0; z--) ring.add(o.offset(0, 0, z));
        BlockState wall = EarthBlocks.DRY_STONE_WALL.get().defaultBlockState();
        List<BlockPos> walls = new ArrayList<>();
        int gate = random.nextInt(w - 4) + 2; // a two-wide gateway in the first side
        boolean standing = true, capped = false;
        for (int i = 0; i < ring.size(); i++) {
            if (i > 3 && random.nextInt(standing ? 9 : 4) == 0) standing = !standing; // standing runs of about 9, fallen of 4
            if (random.nextInt(5) == 0) capped = !capped;
            BlockPos t = top(level, ring.get(i).getX(), ring.get(i).getZ());
            if (!level.getBlockState(t).canBeReplaced() || !dryNatural(level, t.below())) continue;
            Direction out = i < w ? Direction.NORTH : i < w + d ? Direction.EAST : i < 2 * w + d ? Direction.SOUTH : Direction.WEST;
            if (i == gate || i == gate + 1) continue;
            clear(level, t);
            if (!standing) { // fallen: a stone sunk in the turf or a loose stone, now and then nothing
                int k = random.nextInt(4);
                if (k == 0) set(level, t.below(), fieldstone(random));
                else if (k == 1) rock(level, random, t, Mechanical.LOOSE_STONE.get());
                else if (k == 2) tumble(level, random, t, out);
                continue;
            }
            set(level, t, fieldstone(random));
            if (capped && random.nextInt(4) > 0 && level.getBlockState(t.above()).canBeReplaced()) {
                set(level, t.above(), wall);
                walls.add(t.above());
            } else if (random.nextInt(6) == 0) set(level, t.above(), Blocks.MOSS_CARPET.defaultBlockState());
            if (random.nextInt(5) == 0) tumble(level, random, t, random.nextBoolean() ? out : out.getOpposite());
        }
        // the corner of a house: two short foundation courses inside one corner, its floor gone to coarse earth
        BlockPos h = o.offset(2, 0, 2);
        for (int i = 0; i < 5; i++)
            for (BlockPos p : new BlockPos[] {h.offset(i, 0, 0), h.offset(0, 0, i)}) {
                BlockPos t = top(level, p.getX(), p.getZ());
                if (random.nextInt(4) > 0 && level.getBlockState(t).canBeReplaced() && dryNatural(level, t.below())) {
                    clear(level, t);
                    set(level, t, fieldstone(random));
                }
            }
        for (int i = 0; i < 6; i++) {
            BlockPos g = top(level, h.getX() + 1 + random.nextInt(3), h.getZ() + 1 + random.nextInt(3)).below();
            if (dryNatural(level, g) && level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD)) set(level, g, Blocks.COARSE_DIRT.defaultBlockState());
        }
        for (BlockPos p : walls) set(level, p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p));
        for (int i = 0; i < 10; i++) {
            BlockPos p = top(level, o.getX() + 1 + random.nextInt(w - 1), o.getZ() + 1 + random.nextInt(d - 1));
            BlockState nettle = Spinning.STINGING_NETTLE.get().defaultBlockState();
            if (level.getBlockState(p).isAir() && nettle.canSurvive(level, p)) set(level, p, nettle);
        }
        return c;
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

    /** A weathered patch on bare rock: moss in a hollow, a frost-cracked cobble, a vein of another stone, grit. */
    private static BlockState weather(RandomSource random, BlockState rock) {
        return switch (random.nextInt(4)) {
            case 0 -> Blocks.MOSS_BLOCK.defaultBlockState();
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            case 2 -> rock.is(Blocks.ANDESITE) ? Blocks.STONE.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
            default -> Blocks.GRAVEL.defaultBlockState();
        };
    }

    private static BlockState mound(RandomSource random) {
        return (switch (random.nextInt(5)) {
            case 0, 1 -> Blocks.PODZOL;
            case 2 -> Blocks.ROOTED_DIRT;
            default -> Blocks.COARSE_DIRT;
        }).defaultBlockState();
    }
}
