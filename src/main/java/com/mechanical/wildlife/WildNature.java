package com.mechanical.wildlife;

import com.mechanical.Husbandry;
import com.mechanical.WildBerryBushBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Living-world plan A10, what wild animals do to the land when they lower their heads ({@link WildAnimal}'s graze, a few
 * times a minute at most, so it costs nothing): deer and elk browse saplings and bushes (a sapling is eaten, a berry bush
 * stripped back), wild boar root up the turf to bare dirt, other big grazers now and then crop grass down to dirt (it grows
 * back). Birds sow berry bushes ({@link #sowBerries}). Everywhere, clan zones too, unless mobGriefing is off.
 */
public final class WildNature {
    static final int BROWSE_ONE_IN = 3, ROOT_ONE_IN = 6, BARE_ONE_IN = 20, BROWSE_REACH = 2;
    /** How far from its bush a bird drops the seed. */
    static final int SOW_MIN = 4, SOW_MAX = 12;

    public static boolean deer(WildAnimal mob) {
        return mob instanceof RoeDeer || mob instanceof RedDeer || mob instanceof Moose || mob instanceof Reindeer || mob instanceof GiantDeer;
    }

    /** Server: {@code mob} just lowered its head to graze. */
    static void grazed(WildAnimal mob) {
        if (!(mob.level() instanceof ServerLevel level) || mob.isBaby() || !level.getGameRules().get(GameRules.MOB_GRIEFING)) return;
        RandomSource r = mob.getRandom();
        BlockPos on = mob.getOnPos();
        if (deer(mob)) {
            if (r.nextInt(BROWSE_ONE_IN) == 0) browse(level, mob.blockPosition());
        } else if (mob instanceof WildBoar) {
            if (r.nextInt(ROOT_ONE_IN) == 0) root(level, on);
        } else if (!(mob instanceof Predator) && !(mob instanceof BrownBear) && mob.getBbWidth() >= 0.8F && r.nextInt(BARE_ONE_IN) == 0
                && level.getBlockState(on).is(Blocks.GRASS_BLOCK)) {
            Husbandry.bare(level, on);
        }
    }

    /** Eats the first sapling or bush within reach: a sapling or plain bush is gone, a berry bush is stripped to bare twigs. */
    public static boolean browse(ServerLevel level, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-BROWSE_REACH, -1, -BROWSE_REACH), at.offset(BROWSE_REACH, 1, BROWSE_REACH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.SAPLINGS) || s.is(Blocks.BUSH) || s.is(Blocks.SWEET_BERRY_BUSH)) {
                level.destroyBlock(p, false);
                return true;
            }
            if (s.getBlock() instanceof WildBerryBushBlock && s.getValue(WildBerryBushBlock.AGE) > 0) {
                level.levelEvent(2001, p, Block.getId(s));
                level.setBlockAndUpdate(p, s.setValue(WildBerryBushBlock.AGE, 0));
                return true;
            }
        }
        return false;
    }

    /** A boar's snout roots up the turf: grass to bare dirt (grass grows back over it), the plants on it torn up. */
    public static boolean root(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!s.is(Blocks.GRASS_BLOCK)) return false;
        level.levelEvent(2001, pos, Block.getId(s));
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        if (level.getBlockState(pos.above()).is(BlockTags.REPLACEABLE)) level.destroyBlock(pos.above(), false); // grass and flowers torn up
        return true;
    }

    /**
     * A bird ate berries from the bush at {@code from} and drops a seed some way off: a young bush of the same kind on open
     * ground there, if nothing grows within 2 blocks of the spot. Called rarely from the bush's random tick.
     */
    public static boolean sowBerries(ServerLevel level, BlockPos from, BlockState bush, RandomSource r) {
        if (level.getEntitiesOfClass(FlockBird.class, new net.minecraft.world.phys.AABB(from).inflate(24)).isEmpty()) return false;
        int dist = SOW_MIN + r.nextInt(SOW_MAX - SOW_MIN + 1);
        double angle = r.nextDouble() * Math.PI * 2;
        int x = from.getX() + (int) Math.round(Math.cos(angle) * dist), z = from.getZ() + (int) Math.round(Math.sin(angle) * dist);
        if (!level.hasChunkAt(x, z)) return false;
        BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        BlockState young = bush.getBlock().defaultBlockState().setValue(WildBerryBushBlock.AGE, 0);
        if (!level.getBlockState(spot).isAir() || !young.canSurvive(level, spot)) return false;
        for (BlockPos p : BlockPos.betweenClosed(spot.offset(-2, -1, -2), spot.offset(2, 1, 2))) {
            if (level.getBlockState(p).getBlock() instanceof WildBerryBushBlock) return false;
        }
        level.setBlockAndUpdate(spot, young);
        return true;
    }

    private WildNature() {}
}
