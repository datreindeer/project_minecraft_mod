package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Farming;
import com.mechanical.Herbs;
import com.mechanical.WildGrainBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * A natural stand of wild grain (scripts/gen_wildgrain.py): an irregular patch 6-15 blocks across, a lobed, stretched
 * outline roughed up by noise, dense in the core and thinning to a ragged edge, with gaps where the grass and a few field
 * flowers and herbs of its land come through. Wild Rice stands in the shallow water at the edge of a marsh and on the wet
 * bank beside it. The scattered single plants ({@link #scatter}) are placed one per try by their own placement rules.
 * Every plant gets the stage the calendar wants today (WildGrainBlock.stageOn).
 */
final class WildGrainFeature extends StoneAgeFeature {
    static final List<WildGrainFeature> ALL = Farming.WILD_GRAINS.keySet().stream()
            .flatMap(k -> java.util.stream.Stream.of(new WildGrainFeature(k, false), new WildGrainFeature(k, true))).toList();
    /** Fewest plants that make a stand (Wild Rice: a fringe along the shore) else nothing is placed; how far the ground may rise or fall across it. */
    static final int MIN_PLANTS = 14, MIN_RICE = 8, MAX_DY = 2;

    final String kind;
    final boolean scatter;
    private final MapCodec<WildGrainFeature> codec;

    private WildGrainFeature(String kind, boolean scatter) {
        this.kind = kind;
        this.scatter = scatter;
        this.codec = MapCodec.unit(this);
    }

    String id() {
        return "wild_" + kind + (scatter ? "_scatter" : "_stand");
    }

    @Override
    public MapCodec<WildGrainFeature> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        WildGrainBlock grain = Farming.WILD_GRAINS.get(kind).get();
        if (scatter) return plant(level, grain, origin);
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!Decor.noStructure(level, c)) return false;
        boolean rice = grain instanceof WildGrainBlock.Rice;
        SimplexNoise noise = new SimplexNoise(random);
        double r = 3.5 + random.nextDouble() * 4, stretch = 1 + random.nextDouble() * 0.6, turn = random.nextDouble() * Math.PI;
        double a1 = random.nextDouble() * Mth.TWO_PI, a2 = random.nextDouble() * Mth.TWO_PI;
        double cos = Math.cos(turn), sin = Math.sin(turn);
        if (!rice && !gentle(level, c, r)) return false;
        int reach = Mth.ceil(r * stretch * 1.45) + 1;
        List<BlockPos> plants = new ArrayList<>(), extras = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double u = (dx * cos + dz * sin) / stretch, v = -dx * sin + dz * cos;
                double edge = r * (1 + 0.22 * Math.sin(3 * Math.atan2(v, u) + a1) + 0.12 * Math.sin(5 * Math.atan2(v, u) + a2))
                        + 1.6 * noise.get(dx * 0.18, dz * 0.18);
                double t = Math.sqrt(u * u + v * v) / Math.max(1, edge);
                if (t > 1.3) continue;
                // dense core, thinning out past the middle, broken by patches where grass and flowers win
                double density = (t < 0.6 ? 0.95 : 0.95 * (1 - (t - 0.6) / 0.7)) * (0.8 + 0.2 * noise.get(dx * 0.42 + 40, dz * 0.42));
                BlockPos p = surface(level, c.getX() + dx, c.getZ() + dz);
                if (Math.abs(p.getY() - c.getY()) > MAX_DY || covered(level, p)) continue;
                if (random.nextDouble() < density) plants.add(p);
                else if (t < 1.05) extras.add(p);
            }
        }
        plants.removeIf(p -> !fits(level, grain, p));
        if (plants.size() < (rice ? MIN_RICE : MIN_PLANTS)) return false;
        for (BlockPos p : plants) plant(level, grain, p);
        for (BlockPos p : extras) extra(level, random, p);
        Decor.logPlaced(id() + "_n" + plants.size(), c);
        return true;
    }

    /** Open, gently rolling ground round c: most of 12 spots on a ring within a block of its height and under open sky (wild grass stands on
     *  slopes too, but a stand cut up by half steps and cliffs doesn't read as one). */
    private static boolean gentle(WorldGenLevel level, BlockPos c, double r) {
        int flat = 0;
        for (int i = 0; i < 12; i++) {
            double a = i * Mth.TWO_PI / 12;
            BlockPos p = surface(level, c.getX() + Mth.floor(Math.cos(a) * r * 0.75), c.getZ() + Mth.floor(Math.sin(a) * r * 0.75));
            if (Math.abs(p.getY() - c.getY()) <= 1 && level.getFluidState(p).isEmpty() && !covered(level, p)) flat++;
        }
        return flat >= 10;
    }

    /** Under a tree: something more than a tall plant stands over the ground here (a grass of the open land doesn't grow in shade). */
    private static boolean covered(WorldGenLevel level, BlockPos p) {
        var type = level.getChunk(p.getX() >> 4, p.getZ() >> 4) instanceof net.minecraft.world.level.chunk.LevelChunk
                ? net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE : net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG;
        return level.getHeight(type, p.getX(), p.getZ()) > p.getY() + 2;
    }

    /** Room for the plant here: open ground (or, for rice, a block of still water) and soil it takes; Wild Rice also near water. */
    private static boolean fits(WorldGenLevel level, WildGrainBlock grain, BlockPos p) {
        BlockState here = level.getBlockState(p);
        boolean water = level.getFluidState(p).is(FluidTags.WATER);
        if (!(here.isAir() || water && here.is(Blocks.WATER) || here.canBeReplaced() && level.getFluidState(p).isEmpty()) || here.is(grain)) return false;
        if (here.getBlock() instanceof DoublePlantBlock && here.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return false;
        if (!grain.fluidOk(level, p) || !grain.defaultBlockState().canSurvive(level, p)) return false;
        if (!(grain instanceof WildGrainBlock.Rice) || water) return true;
        for (Direction d : Direction.Plane.HORIZONTAL) { // rice on the bank: water within two blocks, level or a step down
            for (int i = 1; i <= 2; i++) {
                for (int dy = 0; dy >= -1; dy--) if (level.getFluidState(p.relative(d, i).above(dy)).is(FluidTags.WATER)) return true;
            }
        }
        return false;
    }

    /** Sets the plant at today's stage; a tall grass it replaces goes, both halves. */
    private static boolean plant(WorldGenLevel level, WildGrainBlock grain, BlockPos p) {
        BlockState here = level.getBlockState(p);
        if (here.getBlock() instanceof DoublePlantBlock) set(level, p.above(), AIR);
        set(level, p, grain.placed(level, p, grain.stateAt(level.getLevel(), p)));
        return true;
    }

    /** Between the grain: grass mostly, now and then a field flower or herb of the stand's land (only on open dry ground). */
    private void extra(WorldGenLevel level, RandomSource random, BlockPos p) {
        if (!level.getBlockState(p).isAir() || !level.getFluidState(p).isEmpty()) return;
        int roll = random.nextInt(100);
        Block b;
        if (kind.equals("rice")) b = roll < 6 ? Herbs.HERBS.get("water_mint").get() : roll < 45 ? Blocks.SHORT_GRASS : null;
        else if (roll < 6) b = kind.equals("oats") || kind.equals("einkorn") ? Blocks.CORNFLOWER : Blocks.POPPY;
        else if (roll < 10) b = kind.equals("barley") ? Blocks.POPPY : Blocks.OXEYE_DAISY;
        else if (roll < 13) b = Herbs.HERBS.get(kind.equals("barley") || kind.equals("emmer") ? "yarrow" : "chamomile").get();
        else b = roll < 60 ? Blocks.SHORT_GRASS : null;
        if (b != null && b.defaultBlockState().canSurvive(level, p)) set(level, p, b.defaultBlockState());
    }
}
