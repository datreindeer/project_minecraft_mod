package com.mechanical.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * The day at home for camp folk (docs/living-world-plan.md C): in the morning they gather by the fire to eat and stretch,
 * through the day this goal leaves them to their work goals (chopping, gathering, fishing, building), in the evening they
 * sit round the fire, and at night everyone but the warriors lays out a Straw Bed near it and sleeps till morning while the
 * warriors keep watch (CampLife). Not for raiders, visitors, travellers, caravans or the Miner.
 */
class DailyRhythmGoal extends Goal {
    private final Tribesperson mob;
    /** The day (overworld clock / day) the morning meal and stretch were done. */
    private long morningDone = -1;
    private int ticks, phase;

    DailyRhythmGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private long today() {
        return mob.level().getOverworldClockTime() / com.mechanical.Calendar.CLOCK_DAY;
    }

    private boolean free() {
        return mob.home() != null && !mob.isRaider() && mob.visit == null && mob.errand == null && mob.leaveAt < 0 && mob.newCamp == null
                && mob.role() != TribeRole.MINER && mob.getTarget() == null && mob.getTradingPlayer() == null && !mob.isInWater();
    }

    /** Read late: goals are made in the Mob constructor, before Tribesperson's fields are set. */
    private CampLife life() {
        return mob.life;
    }

    @Override
    public boolean canUse() {
        if (!free()) return false;
        int p = life().phase();
        return p == CampLife.EVENING || p == CampLife.NIGHT || p == CampLife.MORNING && morningDone != today();
    }

    @Override
    public boolean canContinueToUse() {
        return free() && life().phase() == phase && (phase != CampLife.MORNING || morningDone != today());
    }

    @Override
    public void start() {
        phase = life().phase();
        ticks = 0;
    }

    @Override
    public void tick() {
        ticks++;
        BlockPos fire = mob.home();
        Vec3 flame = Vec3.atCenterOf(fire);
        switch (phase) {
            case CampLife.MORNING -> {
                // up by the fire: a bite to eat, a long stretch, then off to work
                if (!life().standAt(life().seat(fire, CampLife.SEAT_R + 0.6), flame)) {
                    if (ticks > 600) morningDone = today(); // can't get there: skip it today
                    return;
                }
                if (life().action() == CampLife.NONE) {
                    if (ticks < 2000) {
                        life().start(CampLife.EAT, 120);
                        ticks = 2000; // eaten
                    } else if (ticks < 4000) {
                        life().start(CampLife.STRETCH, 60);
                        ticks = 4000;
                    } else morningDone = today();
                }
            }
            case CampLife.EVENING -> life().sit(life().seat(fire, CampLife.SEAT_R), flame);
            default -> {
                if (life().warrior()) life().guard(fire);
                else life().sleep(fire);
            }
        }
    }

    @Override
    public void stop() {
        life().reset();
        mob.getNavigation().stop();
    }
}
