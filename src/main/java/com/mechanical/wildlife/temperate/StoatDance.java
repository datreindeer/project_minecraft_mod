package com.mechanical.wildlife.temperate;

import java.util.EnumSet;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.Hare;
import com.mechanical.wildlife.coldhunters.Ermine;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * The stoat (the mod's Ermine in its brown coat, now also in the plains and meadows) hunts hares: before it goes in it
 * dances, leaping, twisting and rolling a few blocks from the hare ({@link DanceGoal}, {@link #DANCE_TICKS}). The hare
 * stands and stares at it, spellbound, and the stoat closes in while it does. A white (winter) ermine in the snow hunts
 * lemmings as before.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class StoatDance {
    static final int DANCE_TICKS = 60;
    static final double DANCE_RANGE = 7, SPELL_RANGE = 10;

    private StoatDance() {}

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Ermine stoat)) return;
        stoat.goalSelector.addGoal(0, new DanceGoal(stoat));
        stoat.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(stoat, Hare.class, true,
                (target, level) -> !stoat.isBaby() && !stoat.isWhite() && stoat.getRandom().nextInt(4) == 0));
    }

    /** True while the stoat is dancing for this hare: the hare stands spellbound. */
    public static boolean spellbound(Hare hare) {
        return !hare.level().getEntitiesOfClass(Ermine.class, hare.getBoundingBox().inflate(SPELL_RANGE),
                s -> s.getTarget() == hare && DanceGoal.dancing(s)).isEmpty();
    }

    /** With a hare as its target and the hare a few blocks off: dances for {@link #DANCE_TICKS}, once per hunt, then attacks. */
    static class DanceGoal extends Goal {
        private static final java.util.Set<Ermine> DANCING = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
        private final Ermine stoat;
        private LivingEntity danced;
        private int ticks;

        DanceGoal(Ermine stoat) {
            this.stoat = stoat;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        static boolean dancing(Ermine stoat) {
            return DANCING.contains(stoat);
        }

        @Override
        public boolean canUse() {
            LivingEntity t = stoat.getTarget();
            return t instanceof Hare && t != danced && t.isAlive() && stoat.distanceTo(t) < DANCE_RANGE;
        }

        @Override
        public void start() {
            ticks = 0;
            danced = stoat.getTarget();
            DANCING.add(stoat);
            stoat.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < DANCE_TICKS && stoat.getTarget() == danced && danced.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            stoat.getLookControl().setLookAt(danced);
            if (stoat.onGround() && stoat.getRandom().nextInt(6) == 0) {
                // a leap and a twist on the spot, edging a little closer
                Vec3 to = danced.position().subtract(stoat.position()).multiply(1, 0, 1).normalize().scale(0.08);
                stoat.setDeltaMovement(to.x + (stoat.getRandom().nextDouble() - 0.5) * 0.2, 0.38, to.z + (stoat.getRandom().nextDouble() - 0.5) * 0.2);
                stoat.setYRot(stoat.getYRot() + 120 + stoat.getRandom().nextInt(120));
            }
            if (danced instanceof Hare hare) {
                hare.getNavigation().stop();
                hare.getLookControl().setLookAt(stoat);
            }
        }

        @Override
        public void stop() {
            DANCING.remove(stoat);
        }
    }
}
