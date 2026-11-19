package com.mechanical.people;

import java.util.EnumSet;
import java.util.List;

import com.mechanical.Campfires;
import com.mechanical.Mechanical;
import com.mechanical.RockBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * A group without a campfire (theirs was taken, or a hunting band that has roamed long enough) gathers and makes a new
 * one: after a while the member with the lowest id picks a flat, dry spot near the old fire (or where the band is), the
 * whole group walks there, and once most have arrived (or after a minute) a ring of stones and a burning campfire go
 * down. That fire becomes everyone's home. Bands never settle in the middle of a hunt.
 */
class SetUpCampGoal extends Goal {
    static final int FIRE_LOST_DELAY = 2400, BAND_DELAY = 24000, RETRY = 1200, GATHER_TIMEOUT = 1200;
    private final Tribesperson mob;
    private int walking;

    SetUpCampGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /** Called every few seconds: notices a missing fire and, when the time has come, lets the group pick a new spot. */
    static void check(Tribesperson mob, ServerLevel level, long now) {
        BlockPos home = mob.home();
        boolean homeless = home == null || level.isLoaded(home) && !level.getBlockState(home).is(Blocks.CAMPFIRE);
        if (!homeless) {
            mob.homelessSince = -1;
            return;
        }
        if (mob.homelessSince < 0) mob.homelessSince = now;
        for (Tribesperson t : mob.groupNearby(64)) { // the others already made a new fire: join them
            BlockPos theirs = t.home();
            if (theirs != null && !theirs.equals(home) && level.isLoaded(theirs) && level.getBlockState(theirs).is(Blocks.CAMPFIRE)) {
                mob.settleAt(theirs);
                return;
            }
        }
        if (mob.newCamp != null || now - mob.homelessSince < (home == null ? BAND_DELAY : FIRE_LOST_DELAY)) return;
        List<Tribesperson> group = mob.groupNearby(48);
        if (group.stream().anyMatch(t -> t.getId() < mob.getId() && t.newCamp == null)) return; // someone else decides
        boolean hunting = group.stream().anyMatch(t -> t.getTarget() != null || t.killPos != null && now - t.killTime < 1200);
        if (hunting || level.isRainingAt(mob.blockPosition()) && home == null) return;
        BlockPos spot = findSpot(level, home != null ? home : mob.blockPosition());
        if (spot == null) {
            mob.homelessSince = now - (home == null ? BAND_DELAY : FIRE_LOST_DELAY) + RETRY;
            return;
        }
        for (Tribesperson t : group) t.newCamp = spot;
        mob.newCamp = spot;
    }

    /** The nearest flat, dry, open spot to {@code near} (that spot first), within 16 blocks; null if none is loaded and fit. */
    static BlockPos findSpot(ServerLevel level, BlockPos near) {
        for (int r = 0; r <= 16; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // ring by ring
                    BlockPos p = fit(level, near.getX() + dx, near.getZ() + dz);
                    if (p != null && Math.abs(p.getY() - near.getY()) <= 6) return p;
                }
            }
        }
        return null;
    }

    /** Ground at x,z if the 5x5 around it is level within a block, natural, dry and free (no trunks or leaves overhead). */
    private static BlockPos fit(ServerLevel level, int x, int z) {
        if (!level.isLoaded(new BlockPos(x - 3, 0, z - 3)) || !level.isLoaded(new BlockPos(x + 3, 0, z + 3))) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos top = new BlockPos(x + dx, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz), z + dz);
                BlockState ground = level.getBlockState(top.below());
                if (Math.abs(top.getY() - y) > 1 || !level.getFluidState(top.below()).isEmpty() || ground.is(BlockTags.LEAVES)
                        || !(ground.is(BlockTags.SUBSTRATE_OVERWORLD) || ground.is(BlockTags.SAND) || ground.is(Blocks.GRAVEL)
                        || ground.is(BlockTags.BASE_STONE_OVERWORLD))) return null;
            }
        }
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean canUse() {
        return mob.newCamp != null && mob.getTarget() == null && mob.getTradingPlayer() == null;
    }

    @Override
    public void start() {
        walking = 0;
        moveTo();
    }

    private void moveTo() {
        BlockPos c = mob.newCamp;
        double a = mob.getRandom().nextDouble() * Math.PI * 2;
        mob.getNavigation().moveTo(c.getX() + 0.5 + Math.cos(a) * 2.5, c.getY(), c.getZ() + 0.5 + Math.sin(a) * 2.5, 0.8);
    }

    @Override
    public boolean canContinueToUse() {
        return mob.newCamp != null && mob.getTarget() == null;
    }

    @Override
    public void tick() {
        BlockPos c = mob.newCamp;
        walking++;
        double d = mob.distanceToSqr(Vec3.atBottomCenterOf(c));
        if (d > 4 * 4) {
            if (mob.getNavigation().isDone() && walking % 20 == 0) moveTo();
            if (walking > GATHER_TIMEOUT * 3) mob.newCamp = null; // stuck: give up, try again later
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(Vec3.atCenterOf(c));
        List<Tribesperson> group = mob.groupNearby(48);
        long here = group.stream().filter(t -> t.distanceToSqr(Vec3.atBottomCenterOf(c)) < 5 * 5).count();
        if (here * 3 >= group.size() * 2 || walking > GATHER_TIMEOUT) build((ServerLevel) mob.level(), c, group);
    }

    private void build(ServerLevel level, BlockPos c, List<Tribesperson> group) {
        if (!level.getBlockState(c).canBeReplaced()) {
            mob.newCamp = null; // something was put there meanwhile
            return;
        }
        BlockState fire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        if (fire.hasProperty(Campfires.EMBERS)) fire = fire.setValue(Campfires.EMBERS, false);
        level.setBlock(c, fire, Block.UPDATE_ALL);
        // a loose ring of stones around the fire
        int[][] ring = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 1}, {-1, 2}, {-2, -1}, {1, -2}};
        for (int[] o : ring) {
            BlockPos p = c.offset(o[0], 0, o[1]);
            if (level.getRandom().nextInt(4) == 0 || !level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty()
                    || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) continue;
            level.setBlock(p, Mechanical.LOOSE_STONE.get().defaultBlockState().setValue(RockBlock.SIZE, 1 + level.getRandom().nextInt(3)), Block.UPDATE_ALL);
        }
        level.playSound(null, c, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
        for (Tribesperson t : group) t.settleAt(c);
        mob.settleAt(c);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
