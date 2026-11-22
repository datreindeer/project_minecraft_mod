package com.mechanical.people;

import java.util.EnumSet;
import java.util.List;

import com.mechanical.AncientOak;
import com.mechanical.Foraging;
import com.mechanical.WildBerryBushBlock;
import com.mechanical.WildMushroomBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The day of the Oak Folk (OakFolk), one goal for all four kinds. Gatherers walk to ripe berry bushes and mushrooms in
 * the clearing, kneel and pick them, and plant a new bush, mushroom or glowcap in a bare spot where few grow; worshippers
 * go to their own place facing the trunk, sit and pray; light-bearers walk slowly round the tree with their candles;
 * the keeper stands near the door, now and then raising the staff. Not while fighting, trading or hostile-fleeing.
 */
class OakFolkGoal extends Goal {
    static final int PLANT_ODDS = 3, CROWDED = 3;
    private final Tribesperson mob;
    private BlockPos target;
    private boolean planting;
    private int ticks;
    private double angle;

    OakFolkGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.role().oak() && mob.oakFoot != null && mob.getTarget() == null && mob.getTradingPlayer() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        target = null;
        ticks = 0;
        angle = Math.atan2(mob.getZ() - mob.oakFoot.getZ(), mob.getX() - mob.oakFoot.getX());
    }

    @Override
    public void stop() {
        if (mob.isWorking()) mob.setWorking(false);
        if (mob.role() != TribeRole.OAK_LIGHTBEARER) mob.setIdle(0);
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos foot = mob.oakFoot;
        switch (mob.role()) {
            case OAK_GATHERER -> gather(level, foot);
            case OAK_WORSHIPPER -> pray(level, foot);
            case OAK_LIGHTBEARER -> walkRound(level, foot);
            default -> keep(level, foot);
        }
    }

    // ---- gatherers ----

    private void gather(ServerLevel level, BlockPos foot) {
        if (target == null) {
            if (ticks % 40 != 0) return;
            mob.setIdle(0);
            target = findRipe(level, foot);
            planting = target == null && mob.getRandom().nextInt(PLANT_ODDS) == 0;
            if (planting) target = findBare(level, foot);
            return;
        }
        if (mob.distanceToSqr(Vec3.atBottomCenterOf(target)) > 2.2 * 2.2) {
            if (ticks % 20 == 0) mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.6);
            if (ticks > 600) target = null; // can't get there
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(Vec3.atCenterOf(target));
        if (!mob.isWorking()) {
            mob.setWorking(true);
            mob.setIdle(CampLife.TEND);
            ticks = 0;
            return;
        }
        if (ticks < 40) return;
        if (planting) plant(level, target);
        else pick(level, target);
        mob.setWorking(false);
        mob.setIdle(0);
        target = null;
        ticks = 0;
    }

    /** A ripe bush or a wild mushroom in the clearing, near this gatherer. */
    private BlockPos findRipe(ServerLevel level, BlockPos foot) {
        BlockPos at = mob.blockPosition();
        for (int i = 0; i < 48; i++) {
            BlockPos p = at.offset(mob.getRandom().nextInt(17) - 8, mob.getRandom().nextInt(5) - 2, mob.getRandom().nextInt(17) - 8);
            if (!inClearing(p, foot)) continue;
            BlockState s = level.getBlockState(p);
            if (WildBerryBushBlock.isRipe(s) || s.getBlock() instanceof WildMushroomBlock) return p;
        }
        return null;
    }

    /** Picks it: berries off the bush (it grows back), a mushroom taken whole. Into the basket, so nothing is dropped. */
    static void pick(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof WildBerryBushBlock bush && WildBerryBushBlock.isRipe(s)) bush.pick(level, p, s);
        else if (s.getBlock() instanceof WildMushroomBlock) level.removeBlock(p, false);
    }

    /** A bare grass or moss spot in the clearing with fewer than {@link #CROWDED} bushes, mushrooms or glowcaps round it. */
    private BlockPos findBare(ServerLevel level, BlockPos foot) {
        for (int i = 0; i < 32; i++) {
            double a = mob.getRandom().nextDouble() * Math.PI * 2, d = 10 + mob.getRandom().nextDouble() * (OakFolk.CLEARING_R - 12);
            BlockPos p = OakFolk.ground(level, BlockPos.containing(foot.getX() + 0.5 + Math.cos(a) * d, foot.getY(), foot.getZ() + 0.5 + Math.sin(a) * d));
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.DIRT) && crowd(level, p) < CROWDED) return p;
        }
        return null;
    }

    private static int crowd(ServerLevel level, BlockPos p) {
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-2, -1, -2), p.offset(2, 1, 2))) if (planted(level.getBlockState(q))) n++;
        return n;
    }

    private static boolean planted(BlockState s) {
        return s.getBlock() instanceof WildBerryBushBlock || s.getBlock() instanceof WildMushroomBlock || s.is(AncientOak.GLOWCAP.get());
    }

    /** Plants one of several kinds there: a berry bush (young), a mushroom, or a glowcap; whatever can grow on that spot. */
    static boolean plant(ServerLevel level, BlockPos p) {
        if (!level.getBlockState(p).isAir()) return false;
        List<Block> kinds = new java.util.ArrayList<>();
        for (String b : List.of("raspberry", "blueberry", "wild_strawberry", "elderberry")) kinds.add(Foraging.BUSH_BLOCKS.get(b).get());
        for (String m : List.of("chanterelle", "porcini", "morel", "puffball")) kinds.add(Foraging.MUSHROOM_BLOCKS.get(m).get());
        kinds.add(AncientOak.GLOWCAP.get());
        java.util.Collections.shuffle(kinds, new java.util.Random(level.getRandom().nextLong()));
        for (Block b : kinds) {
            BlockState s = b.defaultBlockState();
            if (s.hasProperty(WildBerryBushBlock.AGE)) s = s.setValue(WildBerryBushBlock.AGE, 0);
            if (s.canSurvive(level, p)) {
                level.setBlock(p, s, Block.UPDATE_ALL);
                return true;
            }
        }
        return false;
    }

    static boolean inClearing(BlockPos p, BlockPos foot) {
        double dx = p.getX() - foot.getX(), dz = p.getZ() - foot.getZ();
        return dx * dx + dz * dz < OakFolk.CLEARING_R * OakFolk.CLEARING_R;
    }

    // ---- worshippers ----

    private void pray(ServerLevel level, BlockPos foot) {
        if (target == null) {
            double a = (mob.getId() * 2.399) % (Math.PI * 2); // a place of their own round the trunk
            target = OakFolk.ground(level, BlockPos.containing(foot.getX() + 0.5 + Math.cos(a) * 11, foot.getY(), foot.getZ() + 0.5 + Math.sin(a) * 11));
        }
        Vec3 trunk = new Vec3(foot.getX() + 0.5, foot.getY() + 4, foot.getZ() + 0.5);
        if (mob.distanceToSqr(Vec3.atBottomCenterOf(target)) > 1.5 * 1.5 && ticks < 400) {
            mob.setIdle(0);
            if (ticks % 20 == 1) mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(trunk);
        mob.setYBodyRot((float) (Math.atan2(trunk.z - mob.getZ(), trunk.x - mob.getX()) * 180 / Math.PI) - 90);
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
        mob.setIdle(CampLife.PRAY | CampLife.SITTING);
    }

    // ---- light-bearers ----

    private void walkRound(ServerLevel level, BlockPos foot) {
        mob.setIdle(CampLife.CANDLES);
        if (mob.getNavigation().isInProgress() && ticks % 200 != 0) return;
        angle += 0.35 + mob.getRandom().nextDouble() * 0.2;
        double r = 15 + 3 * Math.sin(angle * 2 + mob.getId());
        BlockPos p = OakFolk.ground(level, BlockPos.containing(foot.getX() + 0.5 + Math.cos(angle) * r, foot.getY(), foot.getZ() + 0.5 + Math.sin(angle) * r));
        mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.4);
    }

    // ---- the keeper ----

    private void keep(ServerLevel level, BlockPos foot) {
        if (mob.idle() == CampLife.STAFF && ticks % 100 == 0) mob.setIdle(0);
        if (ticks % 400 == 0 && mob.getRandom().nextInt(3) == 0) mob.setIdle(CampLife.STAFF); // raises the staff to the tree
        if (ticks % 200 == 0 && !mob.getNavigation().isInProgress()) {
            double a = mob.getRandom().nextDouble() * Math.PI * 2, d = 10 + mob.getRandom().nextDouble() * 4;
            BlockPos p = OakFolk.ground(level, BlockPos.containing(foot.getX() + 0.5 + Math.cos(a) * d, foot.getY(), foot.getZ() + 0.5 + Math.sin(a) * d));
            mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.4);
        }
    }
}
