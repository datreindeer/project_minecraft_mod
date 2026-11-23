package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mechanical.Calendar;
import com.mechanical.Kettle;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Fire and regrowth (terrain-plan phase 6). In a dry summer (summer, no rain) dry lightning now and then strikes a tree or
 * a meadow 48-110 blocks from a player, out of their sight and away from players' land, and lights it with vanilla fire
 * (which only spreads within 128 blocks of a player and dies in rain). The fire is watched ({@link Watch}): once it is out,
 * trunks that burnt stand again as Charred Log and the burnt ground turns to Burnt Soil and Wood Ash. Where nobody is, a
 * forest chunk can burn too on a dry summer day ({@link #burnField}, rare).
 * <p>
 * A burnt chunk then grows back over a few seasons ({@link #regrow}): ash and Burnt Soil turn to Coarse Dirt, Fireweed comes
 * up through it first, Coarse Dirt grasses over, then grass, flowers and saplings (birch first, the pioneer, or the wood of
 * the nearest living tree) and the saplings grow into a young forest (ForestCreep grows saplings everywhere). The charred
 * trunks crumble a block at a time down to stumps. When no burnt ground is left and young trees stand, the chunk has
 * regrown: advancement From the Ashes for standing in it. Never in the Scorched Barrens, which stay burnt.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Wildfire {
    /** Dry lightning: one in this many seconds of a dry summer day per player. */
    static final int LIGHTNING_ONE_IN = 2400;
    /** Where nobody is: chance a forest chunk burns on a dry summer day; radius of such a fire. */
    static final float UNSEEN_FIRE = 1 / 20000F;
    static final int UNSEEN_RADIUS = 7;
    /** Watched round the strike, and for how long at most (10 minutes). */
    static final int WATCH_RADIUS = 28, WATCH_TICKS = 12000;
    /** Daily chances in a burnt chunk: ash or Burnt Soil to Coarse Dirt, Fireweed on Burnt Soil, Coarse Dirt to grass, something on bare grass, a charred trunk losing its top. */
    static final float ASH_TO_DIRT = 1 / 30F, FIREWEED = 1 / 60F, DIRT_TO_GRASS = 1 / 40F, PLANT = 1 / 50F, CRUMBLE = 1 / 40F;
    static final Identifier FROM_THE_ASHES = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/from_the_ashes");
    private static final ResourceKey<Biome> BARRENS = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
            Identifier.fromNamespaceAndPath(Mechanical.MODID, "scorched_barrens"));
    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET};

    /** A fire burning after dry lightning: the logs that stood round it, and the columns fire was seen in. */
    private record Watch(BlockPos centre, long start, java.util.Map<BlockPos, Direction.Axis> logs, Set<Long> burnt, int[] quiet) {}

    private static final List<Watch> WATCHES = new ArrayList<>();

    private Wildfire() {}

    static boolean dry(ServerLevel level) {
        return Calendar.season(level.getOverworldClockTime()) == 1 && !level.isRaining();
    }

    // ---------------------------------------------------------------- dry lightning and the fire it lights

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || NatureClock.paused) return;
        long now = level.getGameTime();
        if (now % 20 == 0 && dry(level) && level.getGameRules().get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER) != 0)
            for (ServerPlayer p : level.players())
                if (!p.isSpectator() && level.getRandom().nextInt(LIGHTNING_ONE_IN) == 0) strikeNear(level, p, false);
        if (now % 40 == 0) for (Iterator<Watch> it = WATCHES.iterator(); it.hasNext(); ) if (watch(level, it.next(), now)) it.remove();
    }

    /** Dry lightning near the player (demo: in front of them, 30 blocks off, any land): a fire starts and is watched. */
    static boolean strikeNear(ServerLevel level, ServerPlayer player, boolean demo) {
        RandomSource r = level.getRandom();
        for (int tries = 0; tries < 24; tries++) {
            double angle = demo ? Math.toRadians(player.getYRot() + 90) + (r.nextDouble() - 0.5) * 0.4 : r.nextDouble() * Math.PI * 2;
            double dist = demo ? 24 + r.nextInt(12) : 48 + r.nextInt(62);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist), z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            BlockState hit = level.getBlockState(at.below());
            if (!(hit.getBlock() instanceof LeavesBlock || hit.is(BlockTags.LOGS) || hit.is(Blocks.GRASS_BLOCK))) continue;
            var biome = level.getBiome(at);
            if (biome.is(BARRENS) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.value().getBaseTemperature() < 0.3F) continue;
            if (!demo && (NatureClock.seen(level, at) || !free(level, at, 2))) continue;
            LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt == null) return false;
            bolt.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            level.addFreshEntity(bolt);
            // the bolt lights its own block; a dry crown catches round it too
            for (int i = 0; i < 4; i++) {
                BlockPos f = at.offset(r.nextInt(3) - 1, -r.nextInt(3), r.nextInt(3) - 1);
                if (level.getBlockState(f).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, f)) level.setBlockAndUpdate(f, Blocks.FIRE.defaultBlockState());
            }
            WATCHES.add(new Watch(at, level.getGameTime(), logsAround(level, at, WATCH_RADIUS), new HashSet<>(), new int[1]));
            return true;
        }
        return false;
    }

    /** Every chunk within r chunks of at is loaded and free for nature. */
    static boolean free(ServerLevel level, BlockPos at, int r) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                BlockPos p = at.offset(dx * 16, 0, dz * 16);
                if (!NatureClock.free(level, p)) return false;
            }
        return true;
    }

    /** The upright logs and logs lying on the ground round c (what can stand again as Charred Log). */
    private static java.util.Map<BlockPos, Direction.Axis> logsAround(ServerLevel level, BlockPos c, int r) {
        java.util.Map<BlockPos, Direction.Axis> logs = new java.util.HashMap<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int x = c.getX() + dx, z = c.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), ground = groundY(level, x, z);
                for (int y = ground + 1; y < top; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.is(BlockTags.LOGS) && s.hasProperty(RotatedPillarBlock.AXIS) && (s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y || y == ground + 1))
                        logs.put(p, s.getValue(RotatedPillarBlock.AXIS));
                }
            }
        return logs;
    }

    /** The natural ground of a column: below logs, leaves and plants. */
    static int groundY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        while (p.getY() > level.getMinY() && (level.getBlockState(p).is(BlockTags.LOGS) || level.getBlockState(p).is(ScorchedBarrens.CHARRED_LOG.get())
                || !level.getBlockState(p).isSolid())) p.move(Direction.DOWN);
        return p.getY();
    }

    /** Looks for fire round the strike; true when the watch is over (put out and scorched, or nothing caught). */
    private static boolean watch(ServerLevel level, Watch w, long now) {
        if (level.getChunkSource().getChunkNow(w.centre.getX() >> 4, w.centre.getZ() >> 4) == null) return true;
        boolean burning = false;
        int r = WATCH_RADIUS;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int x = w.centre.getX() + dx, z = w.centre.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1, ground = groundY(level, x, z);
                for (int y = ground + 1; y <= top; y++)
                    if (level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.FIRE)) {
                        burning = true;
                        w.burnt.add(BlockPos.asLong(x, ground, z));
                        break;
                    }
            }
        w.quiet[0] = burning ? 0 : w.quiet[0] + 1;
        if (now - w.start < WATCH_TICKS && (burning || w.quiet[0] < 4)) return false;
        if (w.burnt.isEmpty()) return true;
        // out: the trunks that burnt stand again charred, the ground where fire was turns to Burnt Soil and ash
        w.logs.entrySet().stream().sorted(java.util.Comparator.comparingInt(e -> e.getKey().getY())).forEach(e -> { // bottom up, so each rests on the last
            BlockPos p = e.getKey();
            BlockState s = level.getBlockState(p), below = level.getBlockState(p.below());
            if ((s.isAir() || s.is(BlockTags.FIRE)) && (below.isSolid() || below.is(ScorchedBarrens.CHARRED_LOG.get()))) level.setBlockAndUpdate(p, charred(e.getValue()));
        });
        RandomSource rr = level.getRandom();
        Set<Long> chunks = new HashSet<>();
        for (long packed : w.burnt) {
            BlockPos g = BlockPos.of(packed);
            scorchGround(level, g, rr.nextFloat() < 0.75F, rr);
            chunks.add(net.minecraft.world.level.ChunkPos.pack(g.getX() >> 4, g.getZ() >> 4));
        }
        long today = NatureClock.today(level);
        for (long c : chunks) markBurnt(level, net.minecraft.world.level.ChunkPos.getX(c), net.minecraft.world.level.ChunkPos.getZ(c), today);
        Mechanical.LOGGER.info("Wildfire at {} burnt out: {} columns", w.centre, w.burnt.size());
        return true;
    }

    /** SmoothGround's grassy half steps a fire burns, and what they grow back through. */
    static final Set<String> BURNABLE_STEPS = Set.of("grass_slab", "grass_stairs", "dirt_slab", "dirt_stairs", "podzol_slab", "moss_slab");

    /** A burnt half step and what it turns into next: ash and burnt soil to coarse dirt, coarse dirt to grass. */
    static final java.util.Map<String, String> BURNT_STEPS = java.util.Map.of("burnt_soil_slab", "coarse_dirt_slab", "wood_ash_slab", "coarse_dirt_slab",
            "coarse_dirt_slab", "grass_slab", "coarse_dirt_stairs", "grass_stairs");

    private static String key(BlockState s) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    }

    /** The mod block of that name with the same shape (slab type, stair facing, half). */
    static BlockState swap(BlockState s, String path) {
        Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, path));
        return b == Blocks.AIR ? s : b.withPropertiesOf(s);
    }

    private static BlockState charred(Direction.Axis axis) {
        return ScorchedBarrens.CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    static void markBurnt(ServerLevel level, int cx, int cz, long day) {
        LevelChunk c = level.getChunkSource().getChunkNow(cx, cz);
        if (c == null) return;
        NatureClock.Data d = NatureClock.data(c);
        d.burnDay = Math.max(1, day);
        d.regrown = false;
        c.markUnsaved();
    }

    /** The ground at g (the top natural block) after a fire: plants and snow on it gone, Burnt Soil or Wood Ash (inner), or partly at the edge. */
    static void scorchGround(ServerLevel level, BlockPos g, boolean inner, RandomSource r) {
        BlockState s = level.getBlockState(g);
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        if (BURNABLE_STEPS.contains(path)) { // the half steps of SmoothGround burn too
            BlockState up = level.getBlockState(g.above());
            if (!up.isAir() && up.canBeReplaced()) level.setBlock(g.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (inner || r.nextFloat() < 0.5F) level.setBlock(g, swap(s, path.endsWith("_slab") ? (r.nextFloat() < 0.6F ? "burnt_soil_slab" : "wood_ash_slab") : "coarse_dirt_stairs"), Block.UPDATE_ALL);
            return;
        }
        if (!(s.is(BlockTags.SUBSTRATE_OVERWORLD) || s.is(Blocks.COARSE_DIRT))) return;
        BlockState up = level.getBlockState(g.above());
        if (!up.isAir() && (up.canBeReplaced() || up.is(BlockTags.SAPLINGS) || up.is(BlockTags.FLOWERS) || up.is(Blocks.SNOW) || up.is(BlockTags.FIRE)))
            level.setBlock(g.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        float f = r.nextFloat();
        BlockState to = inner ? (f < 0.55F ? ScorchedBarrens.BURNT_SOIL.get() : f < 0.85F ? ScorchedBarrens.WOOD_ASH.get() : Blocks.COARSE_DIRT).defaultBlockState()
                : (f < 0.35F ? ScorchedBarrens.BURNT_SOIL.get() : f < 0.6F ? Blocks.COARSE_DIRT : s.getBlock()).defaultBlockState();
        if (to.getBlock() != s.getBlock()) level.setBlock(g, to, Block.UPDATE_ALL);
    }

    /**
     * The aftermath of a fire round c, radius r, a ragged round patch: leaves gone, trunks and lying logs Charred Log, branches
     * gone, plants and snow gone, the ground Burnt Soil and Wood Ash (an edge only partly). Chunks are marked burnt.
     */
    static int burnField(ServerLevel level, BlockPos c, int r, RandomSource rand, NatureClock.Day d) {
        int changed = 0;
        Set<Long> chunks = new HashSet<>();
        double phase = rand.nextDouble() * 6.3;
        for (int dx = -r - 2; dx <= r + 2; dx++)
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                double edge = r * (0.85 + 0.25 * Math.sin(Math.atan2(dz, dx) * 3 + phase)) + rand.nextDouble() * 1.5;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > edge) continue;
                int x = c.getX() + dx, z = c.getZ() + dz;
                BlockPos col = new BlockPos(x, 0, z);
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null || d != null && !d.can(col.atY(groundY(level, x, z)))) continue;
                int ground = groundY(level, x, z), top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                for (int y = top; y > ground; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof LeavesBlock || s.is(Blocks.VINE) || s.is(com.mechanical.Deadwood.DEADWOOD_BRANCH.get())
                            || s.is(com.mechanical.Deadwood.SHELF_FUNGUS.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    else if (s.is(com.mechanical.Deadwood.BROKEN_DEADWOOD.get()) || s.getBlock() instanceof com.mechanical.wildlife.temperate.GrubLogBlock)
                        level.setBlock(p, charred(Direction.Axis.Y), Block.UPDATE_CLIENTS);
                    else if ((s.is(BlockTags.LOGS) || s.is(com.mechanical.Deadwood.DEADWOOD.get())) && s.hasProperty(RotatedPillarBlock.AXIS)) {
                        Direction.Axis axis = s.getValue(RotatedPillarBlock.AXIS);
                        boolean stands = axis == Direction.Axis.Y || y == ground + 1;
                        level.setBlock(p, stands ? charred(axis) : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else continue;
                    changed++;
                }
                scorchGround(level, new BlockPos(x, ground, z), dist < edge - 1.5, rand);
                changed++;
                chunks.add(net.minecraft.world.level.ChunkPos.pack(x >> 4, z >> 4));
            }
        long day = d != null ? d.day : NatureClock.today(level);
        for (long ch : chunks) markBurnt(level, net.minecraft.world.level.ChunkPos.getX(ch), net.minecraft.world.level.ChunkPos.getZ(ch), day);
        return changed;
    }

    // ---------------------------------------------------------------- the nature clock: unseen fires and regrowth

    static void day(NatureClock.Day d) {
        if (d.runs("fire")) unseenFire(d, d.random(3));
        if (d.runs("regrowth") && NatureClock.data(d.chunk).burnDay > 0) regrow(d, d.random(4));
    }

    /** A forest chunk catches fire on a dry summer day while nobody is there (rare). */
    private static void unseenFire(NatureClock.Day d, RandomSource r) {
        if (d.limit != null || d.season() != 1 || r.nextFloat() >= UNSEEN_FIRE || dayRains(d)) return;
        BlockPos c = d.top(d.pos.getMiddleBlockX(), d.pos.getMiddleBlockZ());
        var biome = d.level.getBiome(c);
        if (!biome.is(BiomeTags.IS_FOREST) || biome.is(BARRENS)) return;
        d.changes += burnField(d.level, new BlockPos(c.getX(), 0, c.getZ()), UNSEEN_RADIUS, r, d);
    }

    /** Did it rain on this day (past days have no weather saved: a roll by day, a third of days). */
    static boolean dayRains(NatureClock.Day d) {
        return RandomSource.create(d.level.getSeed() ^ d.day * 0x5DEECE66DL).nextInt(3) == 0;
    }

    /** One day of a burnt chunk growing back. */
    static void regrow(NatureClock.Day d, RandomSource r) {
        ServerLevel level = d.level;
        if (level.getBiome(d.top(d.pos.getMiddleBlockX(), d.pos.getMiddleBlockZ())).is(BARRENS)) return;
        int burnt = 0, trees = 0;
        int x0 = d.pos.getMinBlockX(), z0 = d.pos.getMinBlockZ();
        for (int i = 0; i < 256; i++) {
            int x = x0 + (i & 15), z = z0 + (i >> 4);
            BlockPos top = d.top(x, z);
            BlockState s = level.getBlockState(top);
            if (s.is(ScorchedBarrens.CHARRED_LOG.get()) && s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y) { // a charred trunk crumbles from the top
                if (level.getBlockState(top.below()).is(ScorchedBarrens.CHARRED_LOG.get()) && r.nextFloat() < CRUMBLE && d.can(top))
                    d.set(top, Blocks.AIR.defaultBlockState());
                continue;
            }
            if (StormFelling.upright(s) && level.getBlockState(top.below()).is(BlockTags.LOGS)) trees++;
            BlockPos g = top;
            BlockState up = level.getBlockState(g.above());
            boolean bare = up.isAir();
            if (s.is(ScorchedBarrens.WOOD_ASH.get()) || s.is(ScorchedBarrens.BURNT_SOIL.get())) {
                burnt++;
                if (!d.can(g)) continue;
                float f = r.nextFloat();
                if (f < ASH_TO_DIRT) d.set(g, Blocks.COARSE_DIRT.defaultBlockState());
                else if (f < ASH_TO_DIRT + FIREWEED && bare && s.is(ScorchedBarrens.BURNT_SOIL.get())) fireweed(d, g.above());
            } else if (s.getBlock() instanceof com.mechanical.DirtSlabBlock || s.is(BlockTags.STAIRS) && BURNT_STEPS.containsKey(key(s))) {
                String next = BURNT_STEPS.get(key(s));
                if (next == null) continue;
                burnt++;
                if (d.can(g) && r.nextFloat() < (next.startsWith("grass") ? DIRT_TO_GRASS : ASH_TO_DIRT)) d.set(g, swap(s, next));
            } else if (s.is(Blocks.COARSE_DIRT)) {
                burnt++;
                if (d.can(g) && r.nextFloat() < DIRT_TO_GRASS) d.set(g, Blocks.GRASS_BLOCK.defaultBlockState());
            } else if (s.is(Blocks.GRASS_BLOCK) && bare && d.can(g.above()) && r.nextFloat() < PLANT) {
                float f = r.nextFloat();
                if (f < 0.55F) d.set(g.above(), Blocks.SHORT_GRASS.defaultBlockState());
                else if (f < 0.7F) fireweed(d, g.above());
                else if (f < 0.85F) d.set(g.above(), FLOWERS[r.nextInt(FLOWERS.length)].defaultBlockState());
                else if (!treeNear(level, g.above(), 3)) d.set(g.above(), pioneer(level, g.above(), r).defaultBlockState());
            }
        }
        NatureClock.Data data = NatureClock.data(d.chunk);
        if (burnt == 0 && trees >= 2 && !data.regrown && d.limit == null) {
            data.regrown = true;
            d.chunk.markUnsaved();
        }
    }

    private static void fireweed(NatureClock.Day d, BlockPos p) {
        BlockState f = ScorchedBarrens.FIREWEED.get().defaultBlockState();
        if (d.level.getBlockState(p).isAir() && d.level.getBlockState(p.above()).isAir() && d.can(p.above()) && f.canSurvive(d.level, p)) {
            DoublePlantBlock.placeAt(d.level, f, p, Block.UPDATE_ALL);
            d.changes += 2;
        }
    }

    /** A sapling, log or grown tree within r (saplings keep their distance). */
    static boolean treeNear(ServerLevel level, BlockPos p, int r) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-r, -1, -r), p.offset(r, 1, r))) {
            BlockState s = level.getBlockState(q);
            if (s.is(BlockTags.SAPLINGS) || s.is(BlockTags.LOGS)) return true;
        }
        return false;
    }

    /** Birch mostly (the first tree back after a fire), else the wood of the nearest living tree within 12. */
    private static Block pioneer(ServerLevel level, BlockPos p, RandomSource r) {
        if (r.nextFloat() < 0.6F) return Blocks.BIRCH_SAPLING;
        for (int d = 4; d <= 12; d++)
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos q = p.relative(dir, d);
                BlockPos t = new BlockPos(q.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, q.getX(), q.getZ()) - 1, q.getZ());
                BlockState s = level.getBlockState(t);
                if (StormFelling.upright(s)) return NatureRot.sapling(s);
            }
        return Blocks.BIRCH_SAPLING;
    }

    /** From the Ashes: standing in a chunk that grew back after a fire. */
    static void checkRegrown(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            LevelChunk c = level.getChunkSource().getChunkNow(p.getBlockX() >> 4, p.getBlockZ() >> 4);
            if (c != null && NatureClock.data(c).regrown) Kettle.award(p, FROM_THE_ASHES);
        }
    }
}
