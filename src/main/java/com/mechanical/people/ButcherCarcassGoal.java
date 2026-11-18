package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.Blood;
import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * After a kill, the hunter goes to the carcass, kneels and butchers it (swings, blood); when done the carcass is gone
 * (they take the meat and hide) and the bones stay behind, as when a player butchers.
 */
class ButcherCarcassGoal extends Goal {
    private static final int WORK_TICKS = 120, KILL_MEMORY = 1200;
    private final Tribesperson mob;
    private CarcassEntity carcass;
    private int ticks, work;

    ButcherCarcassGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.killPos == null || mob.getTarget() != null || mob.tickCount % 10 != 0) return false;
        if (mob.level().getGameTime() - mob.killTime > KILL_MEMORY) {
            mob.killPos = null;
            return false;
        }
        carcass = mob.level().getEntitiesOfClass(CarcassEntity.class, new AABB(mob.killPos, mob.killPos).inflate(4)).stream().findFirst().orElse(null);
        return carcass != null;
    }

    @Override
    public void start() {
        ticks = work = 0;
        mob.killPos = null;
        mob.getNavigation().moveTo(carcass, 0.8);
    }

    @Override
    public boolean canContinueToUse() {
        return carcass.isAlive() && ticks < 600 && work <= WORK_TICKS && mob.getTarget() == null;
    }

    @Override
    public void tick() {
        ticks++;
        Vec3 c = carcass.getBoundingBox().getCenter();
        mob.getLookControl().setLookAt(c);
        if (mob.distanceToSqr(c) > 2.0 * 2.0) {
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(carcass, 0.8);
            return;
        }
        mob.getNavigation().stop();
        if (work == 0) mob.setWorking(true);
        ServerLevel level = (ServerLevel) mob.level();
        if (++work % 8 == 0) {
            mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
            Blood.spray(level, c.x, c.y, c.z, 4);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.6F, 0.7F + mob.getRandom().nextFloat() * 0.3F);
        }
        if (work % 40 == 0) Blood.splatter(level, carcass.blockPosition(), 1, 2);
        if (work < WORK_TICKS) return;
        BlockPos pos = carcass.blockPosition();
        BlockState pile = Mechanical.BONE_PILE.get().defaultBlockState();
        if (level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos)) level.setBlock(pos, pile, Block.UPDATE_ALL);
        carcass.discard();
        work++;
    }

    @Override
    public void stop() {
        mob.setWorking(false);
        carcass = null;
    }
}
