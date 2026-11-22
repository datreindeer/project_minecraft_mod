package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.FallenLogBlock;
import com.mechanical.StumpBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Woodcutters walk out to a natural tree, stump or fallen log beyond the camp (through the gate of a walled one) and chop at it for a while (swings, wood sounds and
 * chips). A standing tree is felled for real (it falls and lies as a felled log, FelledLogEntity), a fallen log or a block of
 * a felled one is hewn up and taken; then
 * they carry the wood home to feed the fire ({@link Tribesperson#carryingWood}).
 */
class ChopWoodGoal extends Goal {
    private static final int RANGE = 14, SWINGS = 14;
    private final Tribesperson mob;
    private BlockPos target;
    /** A felled tree being hewn up ({@code target} is its last block), or null. */
    private com.mechanical.FelledLogEntity log;
    private int ticks, swings;

    ChopWoodGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * What a woodcutter may chop at: a stump or fallen log, or the foot of a natural tree (see {@link #naturalTrunk}),
     * and only outside the camp: never the village's own walls, houses, racks or woodpile.
     */
    static boolean choppable(Tribesperson mob, ServerLevel level, BlockPos p) {
        if (!mob.outsideCamp(p)) return false;
        BlockState s = level.getBlockState(p);
        return s.getBlock() instanceof StumpBlock || s.getBlock() instanceof FallenLogBlock || naturalTrunk(level, p);
    }

    /** The foot of a tree that grew: a column of one kind of log standing on soil, with natural (not placed) leaves round its top. */
    static boolean naturalTrunk(Level level, BlockPos foot) {
        BlockState s = level.getBlockState(foot);
        if (!s.is(BlockTags.LOGS) || !level.getBlockState(foot.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) return false;
        BlockPos.MutableBlockPos top = foot.mutable();
        for (int h = 0; h < 40 && level.getBlockState(top.above()).is(s.getBlock()); h++) top.move(Direction.UP);
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-1, 0, -1), top.offset(1, 1, 1))) {
            BlockState n = level.getBlockState(p);
            if (n.getBlock() instanceof LeavesBlock && !n.getValue(LeavesBlock.PERSISTENT)) return true;
        }
        return false;
    }

    @Override
    public boolean canUse() {
        if (!mob.canWork(TribeRole.WOODCUTTER) || mob.carryingWood || mob.getRandom().nextInt(80) != 0) return false;
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos c = mob.workCenter();
        int camp = mob.campRadius(), r = camp + RANGE;
        // felled trees lying about are hewn up first, from the crown end; never one someone is dragging or that lies in the camp
        log = null;
        var logs = level.getEntitiesOfClass(com.mechanical.FelledLogEntity.class, new net.minecraft.world.phys.AABB(c).inflate(r, 12, r),
                l -> !l.isPassenger() && !l.isPulled() && l.size() > 0 && mob.outsideCamp(l.blockPosition()));
        if (!logs.isEmpty()) {
            log = logs.get(mob.getRandom().nextInt(logs.size()));
            target = log.cellPos(log.size() - 1);
            return true;
        }
        for (int i = 0; i < 60; i++) {
            int dx = mob.getRandom().nextInt(r * 2 + 1) - r, dz = mob.getRandom().nextInt(r * 2 + 1) - r;
            if (dx * dx + dz * dz <= camp * camp) continue;
            BlockPos col = c.offset(dx, 0, dz);
            if (!level.isLoaded(col)) continue;
            // the top of the column (a tree's top log, a stump, a fallen log or bare ground), then down the trunk to its foot
            BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col).below();
            for (int h = 0; h < 40 && level.getBlockState(p.below()).is(BlockTags.LOGS); h++) p = p.below();
            if (Math.abs(p.getY() - c.getY()) > 12) continue;
            // a settled camp (Neighbours) fells standing trees only with mobGriefing
            if (choppable(mob, level, p) && (!level.getBlockState(p).is(BlockTags.LOGS) || Settlements.of(mob) == null
                    || level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING))) {
                target = p.immutable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        ticks = swings = 0;
        mob.walkTo(target, 0.7);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < 900 && swings < SWINGS && mob.getTarget() == null
                && (log != null ? log.isAlive() && !log.isPulled() && log.indexAt(target) >= 0 : choppable(mob, (ServerLevel) mob.level(), target));
    }

    @Override
    public void tick() {
        ticks++;
        Vec3 c = Vec3.atCenterOf(target);
        mob.getLookControl().setLookAt(c);
        double dx = mob.getX() - c.x, dz = mob.getZ() - c.z;
        if (dx * dx + dz * dz > 2.3 * 2.3 || Math.abs(mob.getY() - target.getY()) > 1.5) {
            if (mob.getNavigation().isDone()) mob.walkTo(target, 0.7);
            return;
        }
        mob.getNavigation().stop();
        if (ticks % 14 != 0) return;
        ServerLevel level = (ServerLevel) mob.level();
        BlockState s = log != null ? log.states().get(Math.max(0, log.indexAt(target))) : level.getBlockState(target);
        mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        var sound = s.getSoundType();
        level.playSound(null, target, sound.getHitSound(), SoundSource.NEUTRAL, 1.0F, sound.getPitch() * 0.8F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.05);
        if (++swings >= SWINGS) {
            if (log != null) {
                com.mechanical.FelledLogEntity.removeAt(level, target); // hewn up and carried off
            } else if (s.is(BlockTags.LOGS)) {
                // a real tree: it comes down like when a player fells it, falling away from the woodcutter
                var tree = com.mechanical.TreeFelling.find(level, target);
                if (tree != null && tree.logs().stream().allMatch(mob::outsideCamp)) { // never a tree grown into the camp's wall
                    net.minecraft.core.Direction away = net.minecraft.core.Direction.getApproximateNearest(c.x - mob.getX(), 0, c.z - mob.getZ());
                    com.mechanical.TreeFelling.fell(level, target, tree, away, mob, mob.getMainHandItem());
                    PlantGoal.replant(level, target, s); // a new tree for the one taken
                    level.playSound(null, target, net.minecraft.sounds.SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.2F, 0.6F);
                }
            } else if (s.getBlock() instanceof FallenLogBlock) {
                level.removeBlock(target, false); // hewn up and carried off
            }
            mob.carryingWood = true;
            mob.wantsHome = true;
        }
    }

    @Override
    public void stop() {
        target = null;
        log = null;
    }
}
