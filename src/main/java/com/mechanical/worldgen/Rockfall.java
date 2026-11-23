package com.mechanical.worldgen;

import java.util.List;

import com.mechanical.RockBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rockfall (terrain-plan phase 7). In the thaw (the months Snowmelt and Thaw) and on rainy days of spring and autumn, a
 * block of rock high on a steep mountainside (30+ above the sea, a drop of 4+ within two blocks) works loose and tumbles
 * down the slope, always to the lowest neighbour, until the ground levels out. At the foot it lies as a rock (its own
 * stone, or Cobblestone where it shattered) with Gravel and loose rocks round it, so talus builds up below cliffs over the
 * years. Out of sight like the rest of the clock; a player near enough hears it come down the same day.
 */
final class Rockfall {
    /** Daily chance per mountain chunk in the thaw, and on a rainy spring or autumn day. */
    static final float THAW = 1 / 6F, RAIN = 1 / 20F;
    static final int ABOVE_SEA = 30, DROP = 4, MAX_STEPS = 48;

    private Rockfall() {}

    static void day(NatureClock.Day d) {
        if (!d.runs("rockfall")) return;
        RandomSource r = d.random(9);
        int month = d.month(), season = d.season();
        float chance = month == 11 || month == 0 ? THAW : (season == 0 || season == 2) && Wildfire.dayRains(d) ? RAIN : 0;
        if (chance == 0 || r.nextFloat() >= chance) return;
        for (int tries = 0; tries < 8; tries++) {
            BlockPos top = d.top(d.pos.getMinBlockX() + r.nextInt(16), d.pos.getMinBlockZ() + r.nextInt(16));
            if (top.getY() >= d.level.getSeaLevel() + ABOVE_SEA && loose(d, top) && fall(d, top, r)) return;
        }
    }

    static boolean rock(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE);
    }

    /** Bare rock on the brink: open above, the slope dropping 4+ within two blocks. */
    static boolean loose(NatureClock.Day d, BlockPos p) {
        if (!rock(d.level.getBlockState(p)) || !d.level.getBlockState(p.above()).canBeReplaced()) return false;
        for (Direction dir : Direction.Plane.HORIZONTAL)
            if (d.top(p.getX() + dir.getStepX() * 2, p.getZ() + dir.getStepZ() * 2).getY() <= p.getY() - DROP) return true;
        return false;
    }

    /** The rock at p comes down the slope and comes to rest at the foot with its talus; false if it can't go. */
    static boolean fall(NatureClock.Day d, BlockPos p, RandomSource r) {
        ServerLevel level = d.level;
        BlockState stone = level.getBlockState(p);
        if (!d.can(p)) return false;
        BlockPos at = p;
        int gentle = 0, sx = 0, sz = 0;
        for (int i = 0; i < MAX_STEPS; i++) { // always to the lowest neighbour; it stops where the slope eases off (two gentle steps)
            BlockPos next = null;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos n = d.top(at.getX() + dx, at.getZ() + dz);
                    if (level.getChunkSource().getChunkNow(n.getX() >> 4, n.getZ() >> 4) == null) continue;
                    if (next == null || n.getY() < next.getY()) next = n;
                }
            if (next == null || next.getY() >= at.getY()) break;
            gentle = at.getY() - next.getY() == 1 ? gentle + 1 : 0;
            if (gentle >= 2) break;
            sx = Integer.signum(next.getX() - at.getX());
            sz = Integer.signum(next.getZ() - at.getZ());
            at = next;
        }
        // it bounces on out over the level ground at the foot, further the higher it fell
        for (int i = 0, roll = Math.min(4, (p.getY() - at.getY()) / 4); i < roll && (sx != 0 || sz != 0); i++) {
            BlockPos next = d.top(at.getX() + sx, at.getZ() + sz);
            if (next.getY() > at.getY() || level.getChunkSource().getChunkNow(next.getX() >> 4, next.getZ() >> 4) == null) break;
            at = next;
        }
        if (at.equals(p) || p.getY() - at.getY() < 3) return false;
        BlockPos foot = at;
        if (!d.can(foot.above()) || !level.getFluidState(foot.above()).isEmpty()) return false;
        d.set(p, Blocks.AIR.defaultBlockState());
        if (level.getBlockState(p.above()).is(Blocks.SNOW)) d.set(p.above(), Blocks.AIR.defaultBlockState());
        // the rock itself, on the ground at the foot or beside a rock already lying there
        BlockPos rest = foot.above();
        if (!level.getBlockState(rest).canBeReplaced()) rest = d.top(rest.getX() + r.nextInt(3) - 1, rest.getZ() + r.nextInt(3) - 1).above();
        if (level.getBlockState(rest).canBeReplaced() && level.getFluidState(rest).isEmpty() && d.can(rest))
            d.set(rest, (r.nextInt(3) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : stone.is(BlockTags.BASE_STONE_OVERWORLD) ? stone : Blocks.COBBLESTONE.defaultBlockState()));
        // its splinters: gravel on the ground round the foot, loose rocks on it
        Block loose = Decor.looseFor(stone);
        for (int i = 0; i < 3 + r.nextInt(3); i++) {
            BlockPos g = d.top(foot.getX() + r.nextInt(5) - 2, foot.getZ() + r.nextInt(5) - 2);
            BlockState s = level.getBlockState(g), up = level.getBlockState(g.above());
            if (!d.can(g) || !level.getFluidState(g.above()).isEmpty() || Math.abs(g.getY() - foot.getY()) > 1) continue; // only the ground at the foot
            if (r.nextInt(3) == 0 && (s.is(BlockTags.SUBSTRATE_OVERWORLD) || s.is(Blocks.STONE))) d.set(g, Blocks.GRAVEL.defaultBlockState());
            else if ((up.isAir() || up.canBeReplaced() && up.getFluidState().isEmpty()) && s.isFaceSturdy(level, g, Direction.UP))
                d.set(g.above(), loose.defaultBlockState().setValue(RockBlock.SIZE, 1 + r.nextInt(3)));
        }
        if (d.limit == null && d.day == NatureClock.today(level)) // heard, not seen
            for (BlockPos s : List.of(p, foot)) level.playSound(null, s, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 3.0F, 0.6F);
        return true;
    }
}
