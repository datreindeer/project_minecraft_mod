package com.mechanical.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mechanical.Kettle;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;

/**
 * The spring flood (terrain-plan phase 7). At the snowmelt (a few days late in the month Snowmelt, a different start each
 * year) rivers and creeks rise a block for 3-5 days: a water source is laid a block above every river surface cell and on
 * the flat banks (ground level with the water) up to 5 blocks out. Every block of water it lays is saved in the chunk
 * ({@link NatureClock.Data#flood}) and taken away exactly when the flood goes down; flowing water at the flood level may not
 * turn into new sources meanwhile ({@link #onSource}). Afterwards Clay and Gravel lie here and there on the banks, and now
 * and then a drift log. Not in oceans, on beaches or in swamps. Advancement High Water for wading into it.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class SpringFlood {
    /** Day of the year the flood can start from (Snowmelt is days 330-359), how many later at most, and how long it lasts. */
    static final int FROM = 335, SPREAD = 12, MIN_DAYS = 3, MORE_DAYS = 3;
    /** How far out over flat banks the water spreads; leftovers per bank cell; chance of a drift log per flood and chunk. */
    static final int REACH = 5;
    static final float SILT = 0.12F, DRIFT = 0.35F;
    static final Identifier HIGH_WATER = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/high_water");

    private SpringFlood() {}

    /** This year's flood: first day of the year and how many days. */
    static int[] window(ServerLevel level, long day) {
        long year = Math.floorDiv(day, com.mechanical.Calendar.YEAR_DAYS);
        RandomSource r = RandomSource.create(level.getSeed() ^ year * 0x2545F4914F6CDD1DL);
        return new int[] {FROM + r.nextInt(SPREAD), MIN_DAYS + r.nextInt(MORE_DAYS)};
    }

    static void day(NatureClock.Day d) {
        if (!d.runs("flood")) return;
        NatureClock.Data data = NatureClock.data(d.chunk);
        int[] w = window(d.level, d.day);
        int doy = d.dayOfYear();
        boolean in = doy >= w[0] && doy < w[0] + w[1];
        if (in && data.flood.isEmpty() && doy == w[0]) flood(d);
        else if (!in && !data.flood.isEmpty()) recede(d, d.random(10));
    }

    /** River water: a source with air above in a narrow stream, land within 3 blocks on both sides (6 in a river biome). */
    static boolean river(ServerLevel level, BlockPos w) {
        if (!level.getFluidState(w).is(FluidTags.WATER) || !level.getFluidState(w).isSource() || !level.getBlockState(w.above()).isAir()) return false;
        var biome = level.getBiome(w);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || biome.is(net.minecraft.world.level.biome.Biomes.SWAMP)
                || biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)) return false;
        int reach = biome.is(BiomeTags.IS_RIVER) ? 6 : 3; // a river may be wider than a creek, but a lake or an estuary never floods
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            boolean a = false, b = false;
            for (int i = 1; i <= reach; i++) {
                a |= !level.getFluidState(w.relative(Direction.get(Direction.AxisDirection.POSITIVE, axis), i)).is(FluidTags.WATER);
                b |= !level.getFluidState(w.relative(Direction.get(Direction.AxisDirection.NEGATIVE, axis), i)).is(FluidTags.WATER);
            }
            if (a && b) return true;
        }
        return false;
    }

    /** The water rises a block over the river and its flat banks in this chunk. */
    static int flood(NatureClock.Day d) {
        ServerLevel level = d.level;
        Map<Integer, List<BlockPos>> byLevel = new HashMap<>();
        int x0 = d.pos.getMinBlockX(), z0 = d.pos.getMinBlockZ();
        for (int i = 0; i < 256; i++) {
            int x = x0 + (i & 15), z = z0 + (i >> 4);
            BlockPos w = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
            if (river(level, w)) byLevel.computeIfAbsent(w.getY(), k -> new ArrayList<>()).add(w);
        }
        if (byLevel.isEmpty()) return 0;
        // the chunk's main water level (a creek stepping down a slope floods only at one level)
        int wy = byLevel.entrySet().stream().max(Map.Entry.comparingByValue((a, b) -> a.size() - b.size())).get().getKey();
        Set<BlockPos> cells = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> depth = new HashMap<>();
        for (BlockPos w : byLevel.get(wy)) {
            BlockPos c = w.above();
            if (d.can(c) && cells.add(c)) {
                queue.add(c);
                depth.put(c, 0);
            }
        }
        while (!queue.isEmpty()) { // out over the flat banks
            BlockPos c = queue.poll();
            int dd = depth.get(c);
            if (dd >= REACH) continue;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(dir);
                if (cells.contains(n) || level.getChunkSource().getChunkNow(n.getX() >> 4, n.getZ() >> 4) == null) continue; // banks may reach into the next chunk
                BlockState ground = level.getBlockState(n.below()), there = level.getBlockState(n);
                boolean bank = (ground.is(BlockTags.SUBSTRATE_OVERWORLD) || ground.is(BlockTags.SAND) || ground.is(Blocks.GRAVEL))
                        && (there.isAir() || there.canBeReplaced() && there.getFluidState().isEmpty());
                if (bank && d.can(n)) {
                    cells.add(n);
                    depth.put(n, dd + 1);
                    queue.add(n);
                }
            }
        }
        NatureClock.Data data = NatureClock.data(d.chunk);
        data.floodY = wy + 1;
        for (BlockPos c : cells) {
            d.set(c, Blocks.WATER.defaultBlockState());
            data.flood.add(c.asLong());
        }
        d.chunk.markUnsaved();
        return cells.size();
    }

    /** The flood goes down: every block of water it laid is taken away; clay, gravel and now and then a drift log are left. */
    static void recede(NatureClock.Day d, RandomSource r) {
        ServerLevel level = d.level;
        NatureClock.Data data = NatureClock.data(d.chunk);
        List<BlockPos> banks = new ArrayList<>();
        for (long packed : data.flood) {
            BlockPos c = BlockPos.of(packed);
            if (level.getFluidState(c).is(FluidTags.WATER)) d.set(c, Blocks.AIR.defaultBlockState());
            if (level.getFluidState(c.below()).isEmpty()) banks.add(c);
        }
        // water that flowed on at the flood level beside it drains too
        for (long packed : data.flood) {
            BlockPos c = BlockPos.of(packed);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(dir);
                if (level.getFluidState(n).is(FluidTags.WATER) && !level.getFluidState(n).isSource() && d.can(n)) d.set(n, Blocks.AIR.defaultBlockState());
            }
        }
        data.flood.clear();
        d.chunk.markUnsaved();
        for (BlockPos b : banks) { // silt
            BlockPos g = b.below();
            if (r.nextFloat() < SILT && d.can(g) && level.getBlockState(g).is(BlockTags.SUBSTRATE_OVERWORLD))
                d.set(g, (r.nextBoolean() ? Blocks.CLAY : Blocks.GRAVEL).defaultBlockState());
        }
        if (!banks.isEmpty() && r.nextFloat() < DRIFT) { // a log the water brought, left on the bank
            BlockPos b = banks.get(r.nextInt(banks.size()));
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            List<BlockPos> logs = ForestDecor.logCells(level, b, dir, 2 + r.nextInt(3), false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
            logs.removeIf(c -> !d.can(c));
            if (logs.size() >= 2) {
                ForestDecor.lay(level, logs, DRIFTWOOD[r.nextInt(DRIFTWOOD.length)], dir.getAxis());
                d.changes += logs.size();
            }
        }
    }

    private static final net.minecraft.world.level.block.Block[] DRIFTWOOD = {Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.STRIPPED_PALE_OAK_LOG};

    /** While a chunk is flooded, flowing water at its flood level never becomes a source (so nothing stays when it goes down). */
    @SubscribeEvent
    static void onSource(CreateFluidSourceEvent event) {
        BlockPos p = event.getPos();
        if (event.getLevel().dimension() != Level.OVERWORLD) return;
        for (int dx = -1; dx <= 1; dx++) // a flood's banks may reach into the next chunk
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk c = event.getLevel().getChunkSource().getChunkNow((p.getX() >> 4) + dx, (p.getZ() >> 4) + dz);
                if (c == null) continue;
                NatureClock.Data data = NatureClock.data(c);
                if (!data.flood.isEmpty() && p.getY() == data.floodY) {
                    event.setCanConvert(false);
                    return;
                }
            }
    }

    /** High Water: wading in a spring flood. */
    static void checkWading(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            LevelChunk c = level.getChunkSource().getChunkNow(p.getBlockX() >> 4, p.getBlockZ() >> 4);
            if (c == null) continue;
            NatureClock.Data data = NatureClock.data(c);
            if (!data.flood.isEmpty() && Math.abs(p.getBlockY() - data.floodY) <= 1 && p.isInWater()) Kettle.award(p, HIGH_WATER);
        }
    }
}
