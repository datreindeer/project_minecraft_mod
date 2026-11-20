package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.Blood;
import com.mechanical.CarcassEntity;
import com.mechanical.HangingRackBlock;
import com.mechanical.Mechanical;
import com.mechanical.people.Neighbours.Settlement;

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
 * (they take the meat and hide) and the bones stay behind, as when a player butchers. A hunter of a settled camp with a
 * free Hanging Rack drags the body home and hangs it instead; once it has aged there it is butchered at the rack. A
 * settled camp's hides go to its stockpile (Settlements).
 */
class ButcherCarcassGoal extends Goal {
    private static final int WORK_TICKS = 120, KILL_MEMORY = 1200;
    private final Tribesperson mob;
    private CarcassEntity carcass;
    private BlockPos rack;
    private int ticks, work;

    ButcherCarcassGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null || (mob.tickCount + mob.getId()) % 10 != 0) return false;
        rack = null;
        Settlement camp = mob.home() != null ? Settlements.of(mob) : null;
        BlockPos campRack = camp != null ? camp.rack.filter(r -> mob.level().isLoaded(r) && mob.level().getBlockState(r).getBlock() instanceof HangingRackBlock).orElse(null) : null;
        if (mob.killPos == null) {
            // an aged body on the camp's own rack: time to cut it up
            if (campRack == null || mob.role() != TribeRole.HUNTER || mob.level().isDarkOutside() || (mob.tickCount + mob.getId()) % 200 != 0) return false;
            BlockPos r = campRack;
            carcass = mob.level().getEntitiesOfClass(CarcassEntity.class, new AABB(r).inflate(1, 3, 1), c -> r.equals(c.hungAt()) && c.aged()).stream().findFirst().orElse(null);
            return carcass != null;
        }
        if (mob.level().getGameTime() - mob.killTime > KILL_MEMORY) {
            mob.killPos = null;
            return false;
        }
        carcass = mob.level().getEntitiesOfClass(CarcassEntity.class, new AABB(mob.killPos, mob.killPos).inflate(4), c -> c.hungAt() == null).stream().findFirst().orElse(null);
        if (carcass != null && campRack != null && !carcass.isHuge() && !HangingRackBlock.occupied((ServerLevel) mob.level(), campRack)
                && carcass.distanceToSqr(Vec3.atCenterOf(campRack)) < 48 * 48) rack = campRack;
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
        return carcass.isAlive() && ticks < (rack != null ? 1600 : 600) && work <= WORK_TICKS && mob.getTarget() == null;
    }

    @Override
    public void tick() {
        ticks++;
        Vec3 c = carcass.getBoundingBox().getCenter();
        if (rack != null && work < 0) {
            drag();
            return;
        }
        mob.getLookControl().setLookAt(c);
        if (mob.distanceToSqr(c) > 2.0 * 2.0) {
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(carcass, 0.8);
            return;
        }
        if (rack != null) {
            work = -1; // got hold of it: off home with it
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
        boolean hung = carcass.hungAt() != null;
        BlockPos pos = carcass.blockPosition();
        BlockState pile = Mechanical.BONE_PILE.get().defaultBlockState();
        if (!hung && level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos)) level.setBlock(pos, pile, Block.UPDATE_ALL);
        carcass.discard();
        Settlements.deliver(mob, Settlement.HIDES, Settlements.HIDES_PER_KILL);
        work++;
    }

    /** Hauls the body behind them to the rack and hangs it there. */
    private void drag() {
        ServerLevel level = (ServerLevel) mob.level();
        Vec3 back = mob.position().subtract(mob.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2));
        carcass.setPos(back.x, mob.getY(), back.z);
        carcass.setDeltaMovement(Vec3.ZERO);
        if (mob.distanceToSqr(Vec3.atBottomCenterOf(rack)) > 2.5 * 2.5) {
            ErrandGoal.walk(mob, rack, 0.6);
            return;
        }
        BlockState state = level.getBlockState(rack);
        if (state.getBlock() instanceof HangingRackBlock && !HangingRackBlock.occupied(level, rack)) {
            carcass.hang(rack, state.getValue(HangingRackBlock.FACING));
            level.playSound(null, rack, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        work = WORK_TICKS + 1; // done
    }

    @Override
    public void stop() {
        mob.setWorking(false);
        carcass = null;
        rack = null;
    }
}
