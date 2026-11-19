package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.FallenLogBlock;
import com.mechanical.StumpBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Woodcutters walk to a tree trunk, stump or fallen log near camp and chop at it for a while (swings, wood sounds and
 * chips). A standing tree is felled for real (it falls and lies as fallen logs), a fallen log is hewn up and taken; then
 * they carry the wood home to feed the fire ({@link Tribesperson#carryingWood}).
 */
class ChopWoodGoal extends Goal {
    private static final int RANGE = 14, SWINGS = 14;
    private final Tribesperson mob;
    private BlockPos target;
    private int ticks, swings;

    ChopWoodGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static boolean choppable(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof StumpBlock || s.getBlock() instanceof FallenLogBlock) return true;
        // the foot of a trunk, standing on the ground
        return s.is(BlockTags.LOGS) && level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD);
    }

    @Override
    public boolean canUse() {
        if (!mob.canWork(TribeRole.WOODCUTTER) || mob.carryingWood || mob.getRandom().nextInt(80) != 0) return false;
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos c = mob.workCenter();
        for (int i = 0; i < 80; i++) {
            BlockPos p = c.offset(mob.getRandom().nextInt(RANGE * 2 + 1) - RANGE, mob.getRandom().nextInt(7) - 3, mob.getRandom().nextInt(RANGE * 2 + 1) - RANGE);
            if (choppable(level, p)) {
                target = p.immutable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        ticks = swings = 0;
        mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.7);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < 500 && swings < SWINGS && mob.getTarget() == null && choppable((ServerLevel) mob.level(), target);
    }

    @Override
    public void tick() {
        ticks++;
        Vec3 c = Vec3.atCenterOf(target);
        mob.getLookControl().setLookAt(c);
        double dx = mob.getX() - c.x, dz = mob.getZ() - c.z;
        if (dx * dx + dz * dz > 2.3 * 2.3 || Math.abs(mob.getY() - target.getY()) > 1.5) {
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(c.x, target.getY(), c.z, 0.7);
            return;
        }
        mob.getNavigation().stop();
        if (ticks % 14 != 0) return;
        ServerLevel level = (ServerLevel) mob.level();
        BlockState s = level.getBlockState(target);
        mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        var sound = s.getSoundType();
        level.playSound(null, target, sound.getHitSound(), SoundSource.NEUTRAL, 1.0F, sound.getPitch() * 0.8F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.05);
        if (++swings >= SWINGS) {
            if (s.is(BlockTags.LOGS)) {
                // a real tree: it comes down like when a player fells it, falling away from the woodcutter
                var tree = com.mechanical.TreeFelling.find(level, target);
                if (tree != null) {
                    net.minecraft.core.Direction away = net.minecraft.core.Direction.getApproximateNearest(c.x - mob.getX(), 0, c.z - mob.getZ());
                    com.mechanical.TreeFelling.fell(level, target, tree, away, mob, mob.getMainHandItem());
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
    }
}
