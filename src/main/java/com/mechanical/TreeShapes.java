package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tree shapes vanilla has no placer for, used by the tree features in scripts/gen_trees.py:
 * {@code mechanical:multi_stem_trunk_placer} (hazel: several thin stems from one stool) and
 * {@code mechanical:weeping_foliage_placer} (willow: a dome of leaves with curtains hanging from its rim).
 */
public final class TreeShapes {
    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACERS = DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, Mechanical.MODID);
    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS = DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, Mechanical.MODID);
    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<MultiStemTrunkPlacer>> MULTI_STEM = TRUNK_PLACERS.register(
            "multi_stem_trunk_placer", () -> new TrunkPlacerType<>(MultiStemTrunkPlacer.CODEC));
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<WeepingFoliagePlacer>> WEEPING = FOLIAGE_PLACERS.register(
            "weeping_foliage_placer", () -> new FoliagePlacerType<>(WeepingFoliagePlacer.CODEC));
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<PalmFoliagePlacer>> PALM = FOLIAGE_PLACERS.register(
            "palm_foliage_placer", () -> new FoliagePlacerType<>(PalmFoliagePlacer.CODEC));

    public static void init(IEventBus modEventBus) {
        TRUNK_PLACERS.register(modEventBus);
        FOLIAGE_PLACERS.register(modEventBus);
    }

    /** A main stem plus 2-4 side stems that sprout beside it just above the ground and lean outwards; each gets its own crown. */
    public static class MultiStemTrunkPlacer extends TrunkPlacer {
        public static final MapCodec<MultiStemTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i).apply(i, MultiStemTrunkPlacer::new));

        public MultiStemTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
            super(baseHeight, heightRandA, heightRandB);
        }

        @Override
        protected TrunkPlacerType<?> type() {
            return MULTI_STEM.get();
        }

        @Override
        public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random,
                                                                int treeHeight, BlockPos origin, TreeFeature tree) {
            placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), tree);
            List<FoliagePlacer.FoliageAttachment> crowns = new ArrayList<>();
            for (int y = 0; y < treeHeight; y++) placeLog(level, trunkSetter, random, origin.above(y), tree);
            crowns.add(new FoliagePlacer.FoliageAttachment(origin.above(treeHeight), 0, false));
            List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            net.minecraft.util.Util.shuffle(sides, random);
            int stems = 2 + random.nextInt(3);
            for (Direction side : sides.subList(0, Math.min(stems, 4))) {
                int height = treeHeight - 1 - random.nextInt(2);
                int lean = 2 + random.nextInt(Math.max(1, height - 2));  // where the stem steps one further out
                BlockPos pos = origin.above(1).relative(side);
                if (!placeLog(level, trunkSetter, random, pos, tree)) continue;
                for (int y = 2; y < height; y++) {
                    pos = y == lean ? pos.above().relative(random.nextBoolean() ? side : side.getClockWise()) : pos.above();
                    if (!placeLog(level, trunkSetter, random, pos, tree)) break;
                }
                crowns.add(new FoliagePlacer.FoliageAttachment(pos.above(), 0, false));
            }
            return crowns;
        }
    }

    /**
     * A low dome (radius, height like blob leaves) whose rim sheds curtains of leaves up to {@code hang_length} blocks down.
     * Curtains stop where they would get too far from the log to stay alive (leaf distance 7), like vanilla's hanging leaves;
     * measured from the attachment, which is a log on fancy trunks (shortcut: on a straight trunk the log is one lower, so
     * the lowest curtain block may decay; add a log-aware check if a straight-trunk willow is ever wanted).
     */
    public static class WeepingFoliagePlacer extends FoliagePlacer {
        public static final MapCodec<WeepingFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(i -> foliagePlacerParts(i)
                .and(Codec.intRange(0, 16).fieldOf("height").forGetter(p -> p.height))
                .and(Codec.intRange(0, 8).fieldOf("hang_length").forGetter(p -> p.hangLength))
                .apply(i, WeepingFoliagePlacer::new));
        private final int height, hangLength;

        public WeepingFoliagePlacer(IntProvider radius, IntProvider offset, int height, int hangLength) {
            super(radius, offset);
            this.height = height;
            this.hangLength = hangLength;
        }

        @Override
        protected FoliagePlacerType<?> type() {
            return WEEPING.get();
        }

        @Override
        protected void createFoliage(WorldGenLevel level, FoliageSetter foliageSetter, RandomSource random, TreeFeature tree, int treeHeight,
                                     FoliageAttachment attachment, int foliageHeight, int leafRadius, int offset) {
            BlockPos origin = attachment.pos();
            int radius = leafRadius + attachment.radiusOffsetXZ();
            int bottom = offset - foliageHeight;
            for (int yo = offset; yo >= bottom; yo--) {
                placeLeavesRow(level, foliageSetter, random, tree, origin, yo == offset ? radius - 1 : radius, yo, attachment.doubleTrunk());
            }
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (inside(Math.abs(dx), Math.abs(dz), radius - 1) || !foliageSetter.isSet(pos.setWithOffset(origin, dx, bottom, dz)) || random.nextInt(4) == 0) continue;
                    int length = 1 + random.nextInt(Math.max(1, hangLength));
                    for (int k = 1; k <= length; k++) {
                        pos.move(Direction.DOWN);
                        if (pos.distManhattan(origin) >= 7 || !tryPlaceLeaf(level, foliageSetter, random, tree, pos)) break;
                    }
                }
            }
        }

        private static boolean inside(int dx, int dz, int radius) {
            return (dx + 0.5F) * (dx + 0.5F) + (dz + 0.5F) * (dz + 0.5F) <= radius * radius;
        }

        @Override
        public int foliageHeight(RandomSource random, int treeHeight, TreeFeature tree) {
            return height;
        }

        @Override
        protected boolean shouldSkipLocation(RandomSource random, int dx, int y, int dz, int currentRadius, boolean doubleTrunk) {
            return !inside(dx, dz, currentRadius);
        }
    }

    /**
     * Date palm crown: a tuft on the trunk top and eight fronds (radius long, the diagonals one shorter) running out level
     * and drooping at their ends, so the crown is a star of arching leaves rather than a ball.
     */
    public static class PalmFoliagePlacer extends FoliagePlacer {
        public static final MapCodec<PalmFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(i -> foliagePlacerParts(i).apply(i, PalmFoliagePlacer::new));

        public PalmFoliagePlacer(IntProvider radius, IntProvider offset) {
            super(radius, offset);
        }

        @Override
        protected FoliagePlacerType<?> type() {
            return PALM.get();
        }

        @Override
        protected void createFoliage(WorldGenLevel level, FoliageSetter foliageSetter, RandomSource random, TreeFeature tree, int treeHeight,
                                     FoliageAttachment attachment, int foliageHeight, int leafRadius, int offset) {
            BlockPos top = attachment.pos().above(offset);
            int radius = leafRadius + attachment.radiusOffsetXZ();
            tryPlaceLeaf(level, foliageSetter, random, tree, top);
            tryPlaceLeaf(level, foliageSetter, random, tree, top.above());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    boolean diagonal = dx != 0 && dz != 0;
                    int length = diagonal ? radius - 1 : radius - random.nextInt(2) + 1, y = 0;
                    for (int k = 1; k <= length; k++) {
                        // leaves only keep alive through faces: diagonals step like stairs, a drooping tip hangs from the block above it
                        if (diagonal) tryPlaceLeaf(level, foliageSetter, random, tree, top.offset(dx * k, y, dz * (k - 1)));
                        boolean droop = k > 1 && k >= length - 1;
                        if (droop) tryPlaceLeaf(level, foliageSetter, random, tree, top.offset(dx * k, y, dz * k));
                        if (droop) y--;
                        tryPlaceLeaf(level, foliageSetter, random, tree, top.offset(dx * k, y, dz * k));
                    }
                }
            }
        }

        @Override
        public int foliageHeight(RandomSource random, int treeHeight, TreeFeature tree) {
            return 2;
        }

        @Override
        protected boolean shouldSkipLocation(RandomSource random, int dx, int y, int dz, int currentRadius, boolean doubleTrunk) {
            return false;
        }
    }

    private TreeShapes() {}
}
