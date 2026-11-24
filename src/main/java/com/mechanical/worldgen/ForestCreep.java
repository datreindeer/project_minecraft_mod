package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Deadwood;
import com.mechanical.Husbandry;
import com.mechanical.Mechanical;
import com.mechanical.TreeFelling;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;

/**
 * The forest moves (terrain-plan phase 6). Saplings grow into trees on the clock too, so a chunk nobody visits still grows
 * its saplings ({@link #grow}). At a forest's edge a sapling of the edge trees' wood now and then comes up out in the open
 * meadow, 3-7 blocks from a trunk, so woods creep out slowly ({@link #creep}); where grazers (livestock, or wild deer,
 * bison and the like) feed nearby the meadow stays open, and deer browse saplings anyway (WildNature). In forests that
 * have snags, an old tree now and then dies and stands as a snag ({@link #die}, ForestDecor's snag of its own wood), and a
 * snag in time comes down and lies as a Deadwood fallen log ({@link #topple}), which rots like any other.
 */
final class ForestCreep {
    /** Daily chances: a sapling growing; a chunk trying one spot at a forest edge; an old tree dying in a snag forest; a snag falling. */
    static final float GROW = 1 / 40F, CREEP = 1 / 6F, DIE = 1 / 300F, TOPPLE = 1 / 90F;
    /** Distance from a trunk a creeping sapling comes up at, and how far grazers keep a meadow open. */
    static final int CREEP_NEAR = 3, CREEP_FAR = 7, GRAZE_REACH = 16;

    private ForestCreep() {}

    static void day(NatureClock.Day d) {
        if (d.runs("grow") || d.runs("creep") || d.runs("topple")) columns(d, d.random(5), d.random(6));
        if (d.runs("creep")) creep(d, d.random(7));
        if (d.runs("die")) die(d, d.random(8));
    }

    /** Saplings grow and snags fall: one pass over the chunk's columns. */
    private static void columns(NatureClock.Day d, RandomSource grow, RandomSource topple) {
        int x0 = d.pos.getMinBlockX(), z0 = d.pos.getMinBlockZ();
        for (int i = 0; i < 256; i++) {
            BlockPos top = d.top(x0 + (i & 15), z0 + (i >> 4));
            BlockState s = d.level.getBlockState(top), up = d.level.getBlockState(top.above());
            if (up.is(BlockTags.SAPLINGS) && (d.runs("grow") || d.runs("creep")) && grow.nextFloat() < GROW && d.can(top.above())) grow(d, top.above(), grow);
            else if (s.is(Deadwood.BROKEN_DEADWOOD.get()) && d.runs("topple") && topple.nextFloat() < TOPPLE) topple(d, top, topple);
        }
    }

    /** A sapling grows into its tree (two growth steps). */
    static boolean grow(NatureClock.Day d, BlockPos p, RandomSource r) {
        for (int i = 0; i < 4 && d.level.getBlockState(p).is(BlockTags.SAPLINGS); i++) {
            BlockState s = d.level.getBlockState(p);
            if (s.getBlock() instanceof SaplingBlock sapling) sapling.advanceTree(d.level, p, s, r);
            else if (s.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(d.level, p, s, net.minecraft.world.level.block.BonemealSource.INTERACTION)) b.performBonemeal(d.level, r, p, s, net.minecraft.world.level.block.BonemealSource.INTERACTION);
            else return false;
        }
        boolean grown = !d.level.getBlockState(p).is(BlockTags.SAPLINGS);
        if (grown) d.changes += 8;
        return grown;
    }

    /** One spot at a forest edge: open grass, a trunk 3-7 blocks away, none closer, no grazers: a sapling of that tree. */
    private static void creep(NatureClock.Day d, RandomSource r) {
        if (r.nextFloat() >= CREEP) return;
        ServerLevel level = d.level;
        int x = d.pos.getMinBlockX() + r.nextInt(16), z = d.pos.getMinBlockZ() + r.nextInt(16);
        BlockPos g = d.top(x, z);
        BlockState up = level.getBlockState(g.above());
        if (!level.getBlockState(g).is(Blocks.GRASS_BLOCK) || !(up.isAir() || up.is(Blocks.SHORT_GRASS)) || !d.can(g.above())
                || level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) != g.getY() + 1) return; // under a crown is forest already
        BlockState trunk = null;
        // the whole square: a trunk found early must not stop the search for one (or a sapling) too close
        for (int dx = -CREEP_FAR; dx <= CREEP_FAR; dx++)
            for (int dz = -CREEP_FAR; dz <= CREEP_FAR; dz++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                BlockPos t = d.top(x + dx, z + dz);
                BlockState s = level.getBlockState(t);
                if (!(s.is(BlockTags.LOGS) || s.is(BlockTags.SAPLINGS) || level.getBlockState(t.above()).is(BlockTags.SAPLINGS))) continue;
                if (dist < CREEP_NEAR) return; // something grows close already
                if (trunk == null && StormFelling.upright(s) && t.getY() - g.getY() >= 3) trunk = s;
            }
        if (trunk == null || grazed(level, g)) return;
        Block sapling = NatureRot.sapling(trunk);
        if (!sapling.defaultBlockState().canSurvive(level, g.above())) return;
        d.set(g.above(), sapling.defaultBlockState());
    }

    /** Livestock or wild grazers feed near p: the meadow stays open. */
    static boolean grazed(ServerLevel level, BlockPos p) {
        return !level.getEntities((Entity) null, new AABB(p).inflate(GRAZE_REACH),
                e -> Husbandry.grazer(e) || e instanceof WildAnimal w && !(w instanceof Predator)).isEmpty();
    }

    /** In a forest with snags, an old tree now and then dies: its crown is gone and its trunk stands as a snag. */
    private static void die(NatureClock.Day d, RandomSource r) {
        if (r.nextFloat() >= DIE || !snagForest(d)) return;
        for (int tries = 0; tries < 8; tries++) {
            BlockPos top = d.top(d.pos.getMinBlockX() + 2 + r.nextInt(12), d.pos.getMinBlockZ() + 2 + r.nextInt(12));
            BlockPos base = StormFelling.trunkBase(d.level, top);
            if (base != null && top.getY() - base.getY() >= 4 && kill(d, base, r)) return;
        }
    }

    /** The tree standing at base dies into a snag of its own wood; false if it isn't a lone ordinary tree. */
    static boolean kill(NatureClock.Day d, BlockPos base, RandomSource r) {
        ServerLevel level = d.level;
        TreeFelling.Tree tree = TreeFelling.find(level, base);
        if (tree == null || tree.logs().size() > 40 || !tree.logs().stream().allMatch(d::can) || !tree.leaves().stream().allMatch(d::can)) return false;
        Block log = level.getBlockState(base).getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(log);
        Block stripped = BuiltInRegistries.BLOCK.getValue(id.withPath("stripped_" + id.getPath()));
        for (BlockPos p : tree.leaves()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos p : tree.logs()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        d.changes += tree.logs().size() + tree.leaves().size() / 4;
        NatureRot.prime(level, base.getX() >> 4, base.getZ() >> 4, 1);
        ForestDecor.snag(level, r, base, new ForestDecor.Wood(log, stripped == Blocks.AIR ? log : stripped, null), false);
        return true;
    }

    private static boolean snagForest(NatureClock.Day d) {
        var registry = d.level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        Biome biome = d.level.getBiome(d.top(d.pos.getMiddleBlockX(), d.pos.getMiddleBlockZ())).value();
        for (String name : new String[] {"snag", "snag_old"}) {
            PlacedFeature f = registry.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
            if (f != null && biome.getGenerationSettings().hasFeature(f)) return true;
        }
        return false;
    }

    /** A snag (its Broken Deadwood top at top) comes down: the bark foot stays as a stump, the rest lies as a Deadwood log. */
    static boolean topple(NatureClock.Day d, BlockPos top, RandomSource r) {
        ServerLevel level = d.level;
        List<BlockPos> trunk = new ArrayList<>(List.of(top));
        BlockPos p = top.below();
        while (trunk.size() < 16 && (level.getBlockState(p).is(Deadwood.DEADWOOD.get()) || level.getBlockState(p).getBlock() instanceof com.mechanical.wildlife.temperate.GrubLogBlock)) {
            trunk.add(p);
            p = p.below();
        }
        if (trunk.size() < 3 || !trunk.stream().allMatch(d::can)) return false;
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        for (BlockPos q : trunk) {
            for (Direction s : Direction.values()) { // branch stubs and shelf fungus go with it
                BlockState n = level.getBlockState(q.relative(s));
                if ((n.is(Deadwood.DEADWOOD_BRANCH.get()) || n.is(Deadwood.SHELF_FUNGUS.get())) && d.can(q.relative(s))) {
                    level.setBlock(q.relative(s), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    if (level.getBlockState(q.relative(s).above()).is(Deadwood.DEADWOOD_BRANCH.get())) level.setBlock(q.relative(s).above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        d.changes += trunk.size();
        BlockPos foot = trunk.getLast().below();
        List<BlockPos> cells = ForestDecor.logCells(level, foot.relative(dir), dir, trunk.size() - 1, true, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        cells.removeIf(c -> !d.can(c));
        if (cells.size() >= 2) {
            ForestDecor.lay(level, cells, Deadwood.DEADWOOD.get(), dir.getAxis());
            d.changes += cells.size();
        }
        return true;
    }
}
