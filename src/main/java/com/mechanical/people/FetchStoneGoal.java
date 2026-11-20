package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.RockBlock;
import com.mechanical.people.Neighbours.Settlement;

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
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * Gatherers and elders of a settled camp bring in stone while it is short: they pick up loose stones near camp (only
 * with mobGriefing; the stones are gone then) or knock a few pieces off bare rock (the rock stays). Into the stockpile.
 */
class FetchStoneGoal extends Goal {
    private static final int RANGE = 16, WORK = 60, WANT = 16;
    private final Tribesperson mob;
    private BlockPos target;
    private int ticks, work;

    FetchStoneGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if ((mob.tickCount + mob.getId()) % 40 != 0 || mob.home() == null || mob.errand != null || mob.getTradingPlayer() != null
                || mob.getTarget() != null || mob.level().isDarkOutside() || mob.getRandom().nextInt(4) != 0) return false;
        TribeRole role = mob.role();
        if (role != TribeRole.GATHERER && role != TribeRole.ELDER && role != TribeRole.FISHER) return false;
        Settlement s = Settlements.of(mob);
        if (s == null || s.stock[Settlement.STONE] >= Math.max(WANT, Settlements.cost(s.plan, s.step)[Settlement.STONE])) return false;
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos h = mob.home();
        for (int i = 0; i < 60; i++) {
            BlockPos p = h.offset(mob.getRandom().nextInt(RANGE * 2 + 1) - RANGE, mob.getRandom().nextInt(9) - 4, mob.getRandom().nextInt(RANGE * 2 + 1) - RANGE);
            if (p.closerThan(h, 3)) continue; // not the camp's own fire ring
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof RockBlock && s.built.stream().noneMatch(b -> b == p.asLong())
                    || st.is(BlockTags.BASE_STONE_OVERWORLD) && level.getBlockState(p.above()).isAir()) {
                target = p.immutable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        ticks = work = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && ticks < 600 && mob.getTarget() == null && !mob.level().getBlockState(target).isAir();
    }

    @Override
    public void tick() {
        ticks++;
        Vec3 c = Vec3.atCenterOf(target);
        mob.getLookControl().setLookAt(c);
        double dx = mob.getX() - c.x, dz = mob.getZ() - c.z;
        if (dx * dx + dz * dz > 2.3 * 2.3 || Math.abs(mob.getY() - target.getY()) > 2) {
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(c.x, target.getY() + 1, c.z, 0.7);
            return;
        }
        mob.getNavigation().stop();
        if (work == 0) mob.setWorking(true);
        ServerLevel level = (ServerLevel) mob.level();
        BlockState st = level.getBlockState(target);
        if (++work % 10 == 0) {
            mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
            level.playSound(null, target, st.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.8F, 0.9F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), c.x, c.y + 0.3, c.z, 5, 0.2, 0.2, 0.2, 0.05);
        }
        if (work < WORK) return;
        int got = 1;
        if (st.getBlock() instanceof RockBlock && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            got = st.getValue(RockBlock.SIZE);
            level.removeBlock(target, false);
        }
        Settlements.deliver(mob, Settlement.STONE, got);
        target = null;
    }

    @Override
    public void stop() {
        mob.setWorking(false);
        target = null;
    }
}
