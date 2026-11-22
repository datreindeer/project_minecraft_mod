package com.mechanical.people;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Unarmed people (and anyone near a swelling Mushroom Man) keep away from monsters: they run toward the camp fire (a visiting
 * band's: the player's fire it came to) or an armed
 * member of their group if that leads away from the threat, else just away. With nowhere to run they are cornered and
 * may hit back (see {@link Tribesperson#isCornered()}). Anyone badly hurt runs from a Giant Deer too ({@link #dreads}).
 */
class FleeThreatGoal extends Goal {
    private static final double RANGE = 8, BURST_RANGE = 6;
    private final Tribesperson mob;
    private LivingEntity threat;

    FleeThreatGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /** A hostile monster, any non-player mob that is after this person, or (for visitors) another tribe's raider. */
    static boolean isThreat(Tribesperson mob, LivingEntity e) {
        if (!e.isAlive() || e instanceof Player) return false;
        if (e instanceof Tribesperson t) return mob.visit != null && t.raid != null && !t.inGroup(mob); // visitors run from raiders at the camp
        return e instanceof Enemy && !(e instanceof NeutralMob) || MonstersHuntPeople.huntsPeople(e) || e instanceof Mob m && m.getTarget() == mob;
    }

    /** Even hunters break off from a fighting Giant Deer once it has hurt them badly (below half health). */
    static boolean dreads(Tribesperson mob, LivingEntity e) {
        return e instanceof com.mechanical.wildlife.GiantDeer deer && deer.isAlive() && !deer.isBroken() && mob.getHealth() < mob.getMaxHealth() * 0.5F;
    }

    private static boolean hissing(LivingEntity e) {
        return e instanceof com.mechanical.wildlife.night.MushroomMan m && m.getSwellDir() > 0;
    }

    @Override
    public boolean canUse() {
        if (mob.getTradingPlayer() != null) return false;
        boolean armed = mob.isArmed();
        List<LivingEntity> near = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(RANGE),
                e -> isThreat(mob, e) && (!armed || hissing(e) && e.distanceToSqr(mob) < BURST_RANGE * BURST_RANGE) || dreads(mob, e));
        threat = near.stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
        if (threat == null) return false;
        Vec3 to = refuge();
        if (to == null) {
            mob.cornered(threat);
            return false;
        }
        mob.getNavigation().moveTo(to.x, to.y, to.z, 1.25);
        return true;
    }

    /** The fire or an armed group member, if that is farther from the threat than we are; else a random spot away. */
    private Vec3 refuge() {
        double now = mob.distanceToSqr(threat);
        BlockPos hearth = mob.home() != null ? mob.home() : mob.visitFire(); // visitors run to the player's fire
        if (hearth != null) {
            Vec3 fire = Vec3.atBottomCenterOf(hearth).add(1.5, 0, 1.5);
            if (fire.distanceToSqr(threat.position()) > now + 9 && fire.distanceToSqr(mob.position()) > 4) return fire;
        }
        for (Tribesperson t : mob.groupNearby(16)) {
            if (t != mob && t.isArmed() && t.distanceToSqr(threat) > now + 9) return t.position();
        }
        return DefaultRandomPos.getPosAway(mob, 12, 6, threat.position());
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone() && threat.isAlive() && threat.distanceToSqr(mob) < 14 * 14;
    }

    @Override
    public void start() {
        if (mob.getTarget() == threat) mob.setTarget(null);
    }

    @Override
    public void stop() {
        threat = null;
    }
}
