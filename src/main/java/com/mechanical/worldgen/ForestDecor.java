package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Foraging;
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

    /** A dead standing tree (bark mostly gone, stubby branches, bored holes), or now and then just a stump. */
    private static boolean snag(WorldGenLevel level, RandomSource random, BlockPos c) {
        Wood w = woodNear(level, c, 10);
        if (w == null) return false;
        if (random.nextInt(3) == 0) {
            if (!stump(level, random, c, c.getY(), w.log())) return false;
            groundCover(level, random, c, 2, 3);
            return true;
        }
        int h = 0, want = 4 + random.nextInt(5);
        while (h < want && level.getBlockState(c.above(h)).isAir()) h++;
        if (h < 4) return false;
        for (int y = 0; y < h; y++)
            set(level, c.above(y), pillar(y == 0 || random.nextInt(5) == 0 ? w.log() : w.stripped(), Direction.Axis.Y));
        // woodpecker and beetle holes: a bored, grub-eaten block or two up the trunk
        for (int i = random.nextInt(3); i > 0; i--)
            set(level, c.above(2 + random.nextInt(h - 2)), TemperateFauna.GRUB_LOG.get().defaultBlockState().setValue(GrubLogBlock.GRUBS, random.nextInt(2)));
        for (int i = 1 + random.nextInt(3); i > 0; i--) {
            Direction d = randomSide(random);
            BlockPos p = c.above(2 + random.nextInt(h - 2)).relative(d);
            for (int k = random.nextInt(3) == 0 ? 2 : 1; k > 0 && level.getBlockState(p).isAir(); k--, p = p.relative(d))
                set(level, p, pillar(w.stripped(), d.getAxis()));
        }
        groundCover(level, random, c, 2, 4);
        return true;
    }

    /** An uprooted tree: the trunk on the ground, the root plate standing on end at its foot and the pit it tore out. */
    private static boolean rootPlate(WorldGenLevel level, RandomSource random, BlockPos c) {
        Wood w = woodNear(level, c, 10);
        if (w == null || !flat(level, c, 2, 1)) return false;
        Direction dir = randomSide(random);
        List<BlockPos> trunk = logCells(level, c.relative(dir), dir, 5 + random.nextInt(4), false);
        if (trunk.size() < 4) return false;
        lay(level, trunk, w.log(), dir.getAxis());
        plate(level, random, c, dir, 1.8 + random.nextDouble());
        pit(level, random, c.relative(dir.getOpposite()), dir);
        if (w.leaves() != null && random.nextBoolean()) crown(level, random, trunk.getLast().relative(dir), w.leaves());
        dressLog(level, random, trunk, dir.getAxis(), 0.15F);
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
     * The root plate: a ragged disc of Rooted Dirt, Dirt and Coarse Dirt standing on end across dir (the way the trunk
     * fell), its middle a block above c, Hanging Roots under every overhang.
     */
    static void plate(WorldGenLevel level, RandomSource random, BlockPos c, Direction dir, double radius) {
        Direction across = dir.getClockWise();
        BlockPos mid = c.above();
        int ir = (int) Math.ceil(radius);
        List<BlockPos> placed = new ArrayList<>();
        for (int a = -ir; a <= ir; a++) {
            for (int b = -ir; b <= ir; b++) {
                if (a * a + b * b > radius * radius + (random.nextDouble() - 0.5) * 1.5) continue;
                BlockPos p = mid.relative(across, a).above(b);
                if (!level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty()) continue;
                set(level, p, (switch (random.nextInt(6)) {
                    case 0, 1 -> Blocks.DIRT;
                    case 2 -> Blocks.COARSE_DIRT;
                    default -> Blocks.ROOTED_DIRT;
                }).defaultBlockState());
                placed.add(p);
            }
        }
        for (BlockPos p : placed) if (level.getBlockState(p.below()).isAir() && random.nextInt(5) < 3) set(level, p.below(), Blocks.HANGING_ROOTS.defaultBlockState());
    }

    /** The hole the roots tore out, behind the plate: a block deep, two in the middle, sometimes holding a little rain water. */
    private static void pit(WorldGenLevel level, RandomSource random, BlockPos back, Direction dir) {
        Direction across = dir.getClockWise();
        List<BlockPos> deep = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            for (int a = -1; a <= 1; a++) {
                BlockPos top = back.relative(dir, -k).relative(across, a);
                BlockPos g = surface(level, top.getX(), top.getZ()).below();
                if (g.getY() != back.getY() - 1 || !level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD)) continue;
                BlockState up = level.getBlockState(g.above());
                if (!up.isAir() && up.canBeReplaced()) set(level, g.above(), AIR);
                set(level, g, AIR);
                boolean middle = a == 0 && natural(level.getBlockState(g.below()));
                if (middle) {
                    set(level, g.below(), AIR);
                    deep.add(g.below());
                }
                set(level, middle ? g.below(2) : g.below(), (random.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT).defaultBlockState());
            }
        }
        // a pool only if every side of the deep cells is solid (or the other deep cell), so it can't run off
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
