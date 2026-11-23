package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Deadwood;
import com.mechanical.Foraging;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.temperate.GrubLogBlock;
import com.mechanical.wildlife.temperate.TemperateFauna;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Small things on the forest floor (docs/terrain-plan.md phase 3), placed by scripts/gen_nature_decor.py: fallen logs,
 * snags and stumps, uprooted trees with their root plate and pit, and clusters of small mossy stones. Wood comes from the
 * nearest standing tree, so every forest drops its own kind (the mod's woods and vanilla's alike).
 * <p>
 * A fallen log is a row of ordinary log blocks (axis x or z, never y) lying on natural ground or sunk a block into it,
 * with only moss carpet, a mushroom or snow on top: a horizontal {@code #minecraft:logs} block over
 * {@code #minecraft:substrate_overworld} (or over another such log) is how later code can find them.
 */
final class ForestDecor extends StoneAgeFeature {
    enum Kind { FALLEN_LOG, SNAG, ROOT_PLATE, MOSSY_STONES }

    static final ForestDecor FALLEN_LOG = new ForestDecor(Kind.FALLEN_LOG), SNAG = new ForestDecor(Kind.SNAG),
            ROOT_PLATE = new ForestDecor(Kind.ROOT_PLATE), MOSSY_STONES = new ForestDecor(Kind.MOSSY_STONES);
    private static final Block[] STONES = {Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE,
            Blocks.STONE, Blocks.STONE, Blocks.ANDESITE, Blocks.ANDESITE, Blocks.TUFF};

    /** A tree species: its log, the log without bark, and its leaves (persistent; null when no crown was found). */
    record Wood(Block log, Block stripped, BlockState leaves) {}

    private final Kind kind;
    private final MapCodec<ForestDecor> codec;

    private ForestDecor(Kind kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<ForestDecor> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        BlockState ground = level.getBlockState(c.below());
        boolean soil = ground.is(BlockTags.SUBSTRATE_OVERWORLD) || kind == Kind.MOSSY_STONES && natural(ground);
        if (!soil || !level.getFluidState(c).isEmpty() || !level.getBlockState(c).canBeReplaced() || !Decor.noStructure(level, c)) return false;
        boolean done = switch (kind) {
            case FALLEN_LOG -> fallenLog(level, random, c);
            case SNAG -> snag(level, random, c);
            case ROOT_PLATE -> rootPlate(level, random, c);
            case MOSSY_STONES -> mossyStones(level, random, c);
        };
        if (done) Decor.logPlaced(kind.name().toLowerCase(), c);
        return done;
    }

    // ------------------------------------------------------------------ the four kinds

    private static boolean fallenLog(WorldGenLevel level, RandomSource random, BlockPos c) {
        Wood w = woodNear(level, c, 10);
        if (w == null) return false;
        Direction dir = randomSide(random);
        int len = 3 + random.nextInt(4) + (random.nextInt(3) == 0 ? random.nextInt(4) : 0);
        boolean sunk = random.nextInt(4) == 0; // half buried: a block down, flush with flat ground, out where the ground dips
        List<BlockPos> cells = logCells(level, sunk ? c.below() : c, dir, len, false);
        if (cells.size() < 3) return false;
        lay(level, cells, random.nextInt(5) == 0 ? w.stripped() : w.log(), dir.getAxis());
        dressLog(level, random, cells, dir.getAxis(), sunk ? 0.55F : 0.3F);
        if (!sunk && random.nextBoolean()) stump(level, random, c.relative(dir, -2), c.getY(), w.log());
        return true;
    }

    /**
     * A dead standing tree, a snag, 4-9 high: its own wood's bark only at the foot (one block, two on a tall one), above it
     * silver-grey Deadwood with now and then a Grub Log (bored by beetles, a woodpecker's hole), a jagged Broken Deadwood
     * top, one or two short dead branch stubs, sometimes Shelf Fungus. The trunk stands straight: a block stepping aside
     * (tried as a lean) reads as a 7 from a distance. At the foot: flakes of its bark, twigs, now and then the snapped-off
     * top lying in the grass. One in four is only a stump.
     */
    private static boolean snag(WorldGenLevel level, RandomSource random, BlockPos c) {
        Wood w = woodNear(level, c, 10);
        if (w == null) return false;
        if (random.nextInt(4) == 0) {
            if (!stump(level, random, c, c.getY(), w.log())) return false;
            foot(level, random, c, w, List.of(c));
            return true;
        }
        int want = 4 + random.nextInt(6);
        List<BlockPos> trunk = new ArrayList<>();
        for (int y = 0; y < want; y++) {
            BlockPos q = c.above(y);
            if (!level.getBlockState(q).canBeReplaced() || !level.getFluidState(q).isEmpty()) break;
            trunk.add(q);
        }
        if (trunk.size() < 4) return false;
        int top = trunk.size() - 1, bark = top >= 6 && random.nextBoolean() ? 2 : 1;
        BlockState grub = TemperateFauna.GRUB_LOG.get().defaultBlockState().setValue(GrubLogBlock.GRUBS, random.nextInt(GrubLogBlock.MAX_GRUBS + 1));
        int grubAt = top >= 4 && random.nextInt(3) > 0 ? bark + 1 + random.nextInt(top - bark - 1) : -1;
        for (int i = 0; i < top; i++) {
            set(level, trunk.get(i), i < bark ? pillar(w.log(), Direction.Axis.Y) : i == grubAt ? grub : pillar(Deadwood.DEADWOOD.get(), Direction.Axis.Y));
        }
        set(level, trunk.get(top), Deadwood.BROKEN_DEADWOOD.get().defaultBlockState());
        for (int i = 1 + (random.nextInt(3) == 0 ? 1 : 0), tries = 0; i > 0 && tries < 8; tries++) // branch stubs on the upper half
            if (branch(level, random, trunk.get(Math.max(bark, top / 2) + random.nextInt(Math.max(1, top - Math.max(bark, top / 2)))), randomSide(random))) i--;
        if (random.nextInt(3) == 0) // shelf fungus, low on the trunk where it stays damp
            for (int i = 1 + random.nextInt(3); i > 0; i--) {
                Direction d = randomSide(random);
                BlockPos f = trunk.get(random.nextInt(Math.min(top, 4))).relative(d);
                if (level.getBlockState(f).isAir()) set(level, f, Deadwood.SHELF_FUNGUS.get().defaultBlockState().setValue(Deadwood.ShelfFungus.FACING, d));
            }
        foot(level, random, c, w, trunk);
        if (random.nextInt(3) == 0) { // the snapped-off top lies in the grass nearby
            Direction d = randomSide(random);
            List<BlockPos> cells = logCells(level, c.relative(d, 2), d, 2 + random.nextInt(2), false);
            if (cells.size() >= 2) {
                lay(level, cells, Deadwood.DEADWOOD.get(), d.getAxis());
                dressLog(level, random, cells, d.getAxis(), 0.35F);
            }
        }
        return true;
    }

    /** A dead branch stub sticking out of the trunk at t towards d (a block, or a crooked two: out and on, or out and up). */
    private static boolean branch(WorldGenLevel level, RandomSource random, BlockPos t, Direction d) {
        BlockPos q = t.relative(d);
        if (!level.getBlockState(q).isAir() || !level.getBlockState(t).is(Deadwood.DEADWOOD.get())) return false;
        for (Direction n : Direction.values()) // never two stubs side by side
            if (level.getBlockState(q.relative(n)).is(Deadwood.DEADWOOD_BRANCH.get())) return false;
        BlockState stub = Deadwood.DEADWOOD_BRANCH.get().defaultBlockState().setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d.getOpposite()), true);
        Direction on = random.nextInt(3) > 0 ? null : random.nextBoolean() ? d : Direction.UP;
        if (on != null && level.getBlockState(q.relative(on)).isAir()) {
            set(level, q, stub.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(on), true));
            set(level, q.relative(on), Deadwood.DEADWOOD_BRANCH.get().defaultBlockState().setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(on.getOpposite()), true));
        } else set(level, q, stub);
        return true;
    }

    /** Round a dead tree's foot: flakes of its bark (when the wood has Fallen Bark), a twig or two, ferns and moss. */
    private static void foot(WorldGenLevel level, RandomSource random, BlockPos c, Wood w, List<BlockPos> trunk) {
        String path = BuiltInRegistries.BLOCK.getKey(w.log()).getPath();
        var bark = Deadwood.FALLEN_BARKS.get(path.substring(0, path.length() - "_log".length()));
        for (int i = 0; i < 6; i++) {
            BlockPos p = surface(level, c.getX() + random.nextInt(5) - 2, c.getZ() + random.nextInt(5) - 2);
            if (trunk.contains(p) || !level.getBlockState(p).isAir() || !level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) continue;
            if (bark != null && i < 4) set(level, p, bark.get().defaultBlockState());
            else set(level, p, Mechanical.TWIGS.get().defaultBlockState());
        }
        groundCover(level, random, c, 2, 3);
    }

    /**
     * An uprooted tree: the root plate standing on end, the trunk's butt in its middle (lifted a block off the ground for
     * two blocks, then lying on it) and behind the plate the crater it tore out of the ground.
     */
    private static boolean rootPlate(WorldGenLevel level, RandomSource random, BlockPos c) {
        Wood w = woodNear(level, c, 10);
        Direction dir = randomSide(random);
        if (w == null || !flat(level, c, 2, 1) || !flat(level, c.relative(dir.getOpposite(), 2), 2, 1)) return false;
        List<BlockPos> trunk = logCells(level, c.relative(dir, 3), dir, 3 + random.nextInt(4), false);
        BlockPos butt = c.above();
        if (trunk.size() < 2 || !level.getBlockState(butt.relative(dir)).isAir() || !level.getBlockState(butt.relative(dir, 2)).isAir()) return false;
        plate(level, random, c, dir, 2.2 + random.nextDouble() * 0.8);
        List<BlockPos> lifted = List.of(butt, butt.relative(dir), butt.relative(dir, 2));
        lay(level, lifted, w.log(), dir.getAxis());
        lay(level, trunk, w.log(), dir.getAxis());
        pit(level, random, c, dir);
        if (w.leaves() != null && random.nextBoolean()) crown(level, random, trunk.getLast().relative(dir), w.leaves());
        dressLog(level, random, trunk, dir.getAxis(), 0.15F);
        Decor.logPlaced("root_plate_dir_" + Decor.viewAngle(dir), butt);
        return true;
    }

    /** Two to five small stones, half sunk, moss on their tops, ferns and moss carpet between them. */
    private static boolean mossyStones(WorldGenLevel level, RandomSource random, BlockPos c) {
        if (!flat(level, c, 1, 2)) return false;
        int stones = 2 + random.nextInt(4);
        for (int i = 0; i < stones; i++) {
            BlockPos at = i == 0 ? c : surface(level, c.getX() + random.nextInt(7) - 3, c.getZ() + random.nextInt(7) - 3);
            if (!level.getFluidState(at).isEmpty() || !natural(level.getBlockState(at.below())) || !level.getBlockState(at).canBeReplaced()) continue;
            double r = i == 0 ? 1.1 + random.nextDouble() * 0.9 : 0.5 + random.nextDouble() * 0.8;
            List<BlockPos> placed;
            if (r < 0.9) { // a single stone
                set(level, at, STONES[random.nextInt(STONES.length)].defaultBlockState());
                placed = new ArrayList<>(List.of(at));
            } else { // a rounded stone sitting in the ground, a block or two showing
                placed = Decor.blob(level, random, r > 1.6 ? at : at.below(), r, r * 0.8, r * (0.8 + random.nextDouble() * 0.4),
                        rr -> STONES[rr.nextInt(STONES.length)].defaultBlockState());
            }
            for (BlockPos p : Decor.tops(level, placed)) {
                if (random.nextInt(3) == 0) set(level, p, Blocks.MOSS_BLOCK.defaultBlockState());
                else if (random.nextInt(3) == 0) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
            }
        }
        groundCover(level, random, c, 4, 8);
        return true;
    }

    // ------------------------------------------------------------------ shared pieces (WaterDecor uses them too)

    /** The wood of the nearest standing tree within r of c (a trunk at least 2 logs tall on natural ground), or null. */
    static Wood woodNear(WorldGenLevel level, BlockPos c, int r) {
        for (int d = 0; d <= r; d++) { // ring by ring, nearest first
            for (int dx = -d; dx <= d; dx++) {
                for (int dz = -d; dz <= d; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
                    for (int dy = -3; dy <= 3; dy++) {
                        BlockPos p = c.offset(dx, dy, dz);
                        BlockState s = level.getBlockState(p);
                        if (!s.is(BlockTags.LOGS) || !level.getBlockState(p.above()).is(s.getBlock())
                                || !level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) continue;
                        Wood w = wood(level, s.getBlock(), p);
                        if (w != null) return w;
                    }
                }
            }
        }
        return null;
    }

    private static Wood wood(WorldGenLevel level, Block log, BlockPos base) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(log);
        String path = id.getPath();
        if (!path.endsWith("_log") || path.startsWith("stripped_") || path.startsWith("sharpened_") || path.contains("grub")
                || path.contains("ancient") || path.contains("rune")) return null;
        Block stripped = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), "stripped_" + path)).orElse(log);
        BlockPos top = base;
        for (int i = 0; i < 40 && level.getBlockState(top.above()).is(log); i++) top = top.above();
        BlockState leaves = null;
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-2, -1, -2), top.offset(2, 2, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.LEAVES)) {
                leaves = s.trySetValue(BlockStateProperties.PERSISTENT, true);
                break;
            }
        }
        return new Wood(log, stripped, leaves);
    }

    static BlockState pillar(Block log, Direction.Axis axis) {
        return log.defaultBlockState().trySetValue(RotatedPillarBlock.AXIS, axis);
    }

    /**
     * Cells of a log lying from start along dir at start's height. It rests on the ground, sinks into natural ground that
     * rises a block, and may span a dip of one block for up to two cells (the first cell too if hangFirst). Stops at
     * anything else (water, trees, cliffs); a hanging end is cut off, so both ends rest.
     */
    static List<BlockPos> logCells(WorldGenLevel level, BlockPos start, Direction dir, int len, boolean hangFirst) {
        List<BlockPos> cells = new ArrayList<>();
        int hanging = 0;
        for (int i = 0; i < len; i++) {
            BlockPos p = start.relative(dir, i);
            int g = surface(level, p.getX(), p.getZ()).getY(), y = p.getY();
            BlockState s = level.getBlockState(p);
            boolean fits;
            if (g == y + 1) fits = natural(s);
            else if (g == y) {
                fits = s.canBeReplaced();
                hanging = 0;
            } else if (g == y - 1) fits = (i > 0 || hangFirst) && ++hanging <= 2 && s.canBeReplaced();
            else fits = false;
            if (!fits || !s.getFluidState().isEmpty() || g <= y && !level.getFluidState(p.below()).isEmpty()) break;
            cells.add(p);
        }
        while (!cells.isEmpty() && surface(level, cells.getLast().getX(), cells.getLast().getZ()).getY() < cells.getLast().getY()) cells.removeLast();
        return cells;
    }

    /** Lays the log along cells; a plant left standing on top of it is cleared. */
    static void lay(WorldGenLevel level, List<BlockPos> cells, Block log, Direction.Axis axis) {
        for (BlockPos p : cells) {
            set(level, p, pillar(log, axis));
            BlockState up = level.getBlockState(p.above());
            if (!up.isAir() && up.canBeReplaced() && up.getFluidState().isEmpty()) set(level, p.above(), AIR);
        }
    }

    /** Moss carpet and mushrooms on the log, ferns, moss and the odd wild mushroom along its sides. */
    static void dressLog(WorldGenLevel level, RandomSource random, List<BlockPos> cells, Direction.Axis axis, float moss) {
        Direction side = axis == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        for (BlockPos p : cells) {
            if (level.getBlockState(p.above()).isAir()) {
                float r = random.nextFloat();
                if (r < moss) set(level, p.above(), Blocks.MOSS_CARPET.defaultBlockState());
                else if (r < moss + 0.07F) set(level, p.above(), (random.nextInt(3) == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState());
            }
            for (Direction d : new Direction[] {side, side.getOpposite()}) {
                if (random.nextInt(5) > 0) continue;
                BlockPos b = surface(level, p.relative(d).getX(), p.relative(d).getZ());
                if (Math.abs(b.getY() - p.getY()) <= 1) plant(level, random, b);
            }
        }
    }

    /** A fern, moss carpet or wild mushroom on bare soil at p. */
    static void plant(WorldGenLevel level, RandomSource random, BlockPos p) {
        if (!level.getBlockState(p).isAir() || !level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) return;
        BlockState s = switch (random.nextInt(7)) {
            case 0, 1, 2 -> Blocks.FERN.defaultBlockState();
            case 3, 4 -> Blocks.MOSS_CARPET.defaultBlockState();
            case 5 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
            default -> Foraging.MUSHROOM_BLOCKS.get(random.nextBoolean() ? "porcini" : "chanterelle").get().defaultBlockState();
        };
        if (s.is(Blocks.MOSS_CARPET) || s.canSurvive(level, p)) set(level, p, s);
    }

    static void groundCover(WorldGenLevel level, RandomSource random, BlockPos c, int r, int tries) {
        for (int i = 0; i < tries; i++)
            plant(level, random, surface(level, c.getX() + random.nextInt(2 * r + 1) - r, c.getZ() + random.nextInt(2 * r + 1) - r));
    }

    /** A stump of 1-2 logs at p if the ground there is natural and at height y; moss or a mushroom on it. */
    static boolean stump(WorldGenLevel level, RandomSource random, BlockPos p, int y, Block log) {
        BlockPos at = surface(level, p.getX(), p.getZ());
        if (at.getY() != y || !level.getBlockState(at).isAir() || !level.getBlockState(at.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) return false;
        int h = random.nextInt(3) == 0 ? 2 : 1;
        for (int i = 0; i < h; i++) set(level, at.above(i), pillar(log, Direction.Axis.Y));
        if (random.nextBoolean()) set(level, at.above(h), (random.nextBoolean() ? Blocks.MOSS_CARPET : Blocks.BROWN_MUSHROOM).defaultBlockState());
        return true;
    }

    /**
     * The root plate, standing on end across dir (the way the trunk fell) with its middle a block above c: a ragged oval
     * about 5 wide and 4-5 high of Rooted Dirt, Dirt and Coarse Dirt with a few stones in it, two thick in the middle,
     * its top tipped back a block over the trunk. Root ends (Mangrove Roots) poke out at the rim and from the back, and
     * Hanging Roots hang under it.
     */
    static void plate(WorldGenLevel level, RandomSource random, BlockPos c, Direction dir, double radius) {
        Direction across = dir.getClockWise(), back = dir.getOpposite();
        BlockPos mid = c.above();
        double rh = radius + 0.3, rv = radius;
        List<BlockPos> soil = new ArrayList<>(), rim = new ArrayList<>();
        List<Direction> out = new ArrayList<>();
        for (int a = -(int) Math.ceil(rh); a <= rh; a++) {
            for (int b = -(int) Math.ceil(rv); b <= rv; b++) {
                double d = (a / rh) * (a / rh) + (b / rv) * (b / rv);
                if (d > 1 + (random.nextDouble() - 0.5) * 0.45) continue;
                BlockPos p = mid.relative(across, a).above(b).relative(dir, b >= 2 ? 1 : 0);
                if (soil(level, random, p)) soil.add(p);
                if (d < 0.3 && soil(level, random, p.relative(back))) soil.add(p.relative(back));
                if (d > 0.55) {
                    rim.add(p);
                    out.add(Math.abs(a / rh) >= Math.abs(b / rv) ? (a > 0 ? across : across.getOpposite()) : Direction.UP);
                }
            }
        }
        BlockState roots = Blocks.MANGROVE_ROOTS.defaultBlockState();
        for (int i = 0; i < rim.size(); i++) {
            BlockPos q = rim.get(i).relative(out.get(i));
            if (random.nextBoolean() && level.getBlockState(q).isAir()) set(level, q, roots);
        }
        for (BlockPos p : soil) {
            if (random.nextInt(4) == 0 && level.getBlockState(p.relative(back)).isAir()) set(level, p.relative(back), roots);
            if (level.getBlockState(p.below()).isAir() && random.nextInt(5) < 3) set(level, p.below(), Blocks.HANGING_ROOTS.defaultBlockState());
        }
    }

    /** One block of the plate if the spot is free: mostly dirt of three kinds, now and then a stone it tore up. */
    private static boolean soil(WorldGenLevel level, RandomSource random, BlockPos p) {
        if (!level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty()) return false;
        set(level, p, (switch (random.nextInt(12)) {
            case 0, 1, 2 -> Blocks.DIRT;
            case 3, 4 -> Blocks.COARSE_DIRT;
            case 5 -> random.nextBoolean() ? Blocks.COBBLESTONE : Blocks.ANDESITE;
            default -> Blocks.ROOTED_DIRT;
        }).defaultBlockState());
        return true;
    }

    /**
     * The crater behind the plate (c is the plate's foot): an oval about 3 long and 5 wide, a block deep, two in the
     * middle, floored with Coarse Dirt and Rooted Dirt; sometimes rain water stands in the deep part.
     */
    private static void pit(WorldGenLevel level, RandomSource random, BlockPos c, Direction dir) {
        Direction across = dir.getClockWise();
        List<BlockPos> deep = new ArrayList<>();
        for (int k = 1; k <= 3; k++) {
            for (int a = -2; a <= 2; a++) {
                double d = (k - 1.8) * (k - 1.8) / 2.0 + a * a / 6.5;
                if (d > 1 + (random.nextDouble() - 0.5) * 0.3) continue;
                BlockPos top = c.relative(dir, -k).relative(across, a);
                BlockPos g = surface(level, top.getX(), top.getZ()).below();
                if (g.getY() != c.getY() - 1 || !level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD)) continue;
                BlockState up = level.getBlockState(g.above());
                if (!up.isAir() && up.canBeReplaced()) set(level, g.above(), AIR);
                set(level, g, AIR);
                boolean middle = d < 0.4 && natural(level.getBlockState(g.below()));
                if (middle) {
                    set(level, g.below(), AIR);
                    deep.add(g.below());
                }
                set(level, middle ? g.below(2) : g.below(), (random.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT).defaultBlockState());
            }
        }
        // a pool only if every side of the deep cells is solid (or another deep cell), so it can't run off
        if (random.nextInt(3) > 0 || deep.isEmpty()) return;
        for (BlockPos p : deep) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!deep.contains(n) && (level.getBlockState(n).canBeReplaced() || !level.getFluidState(n).isEmpty())) return;
            }
        }
        for (BlockPos p : deep) set(level, p.below(), Blocks.MUD.defaultBlockState());
        for (BlockPos p : deep) set(level, p, Blocks.WATER.defaultBlockState());
    }

    /** The fallen crown: a low, flattened heap of (persistent) leaves around tip. */
    static void crown(WorldGenLevel level, RandomSource random, BlockPos tip, BlockState leaves) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    double d = (dx * dx + dz * dz) / 5.5 + (dy - 0.5) * (dy - 0.5) / 2.2;
                    if (d > 1 + (random.nextDouble() - 0.5) * 0.5) continue;
                    BlockPos p = tip.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.canBeReplaced() && s.getFluidState().isEmpty()) set(level, p, leaves);
                }
            }
        }
    }
}
