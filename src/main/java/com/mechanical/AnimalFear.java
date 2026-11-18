package com.mechanical;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Wild animals keep away from players and flee in panic when hurt. Panic spreads: an animal that sees a panicking or
 * bleeding animal panics too. Sneaking lets you get much closer.
 * Only animals using the goal system (cow, pig, sheep, chicken, rabbit, horse, llama, fox, wolf ...); "brain" animals
 * (goat, camel, frog, axolotl, armadillo, sniffer) don't get this yet.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class AnimalFear {
    /** How close a player can get before being noticed: sprinting, walking, sneaking (blocks). Beyond SNEAK it needs line of sight. */
    private static final double NOTICE_SPRINT = 16, NOTICE_WALK = 10, NOTICE_SNEAK = 4;
    private static final double PANIC_RANGE = 32, CONTAGION_RANGE = 12;
    private static final double FLEE_SPEED = 1.5, PANIC_SPEED = 2.0;
    private static final int PANIC_TICKS = 200;

    /** Server-side: game time until which each animal panics, and what it runs from. */
    private record Panic(long until, Vec3 from) {}
    private static final Map<LivingEntity, Panic> PANIC = new WeakHashMap<>();

    static boolean isWild(LivingEntity entity) {
        if (entity instanceof com.mechanical.wildlife.BrownBear) return false; // bears never flee or panic
        if (!(entity instanceof Animal animal)) return false;
        if (animal instanceof TamableAnimal tame && tame.isTame()) return false;
        if (animal instanceof AbstractHorse horse && horse.isTamed()) return false;
        return !animal.isLeashed();
    }

    public static boolean isPanicking(LivingEntity entity) {
        Panic p = PANIC.get(entity);
        return p != null && p.until > entity.level().getGameTime();
    }

    public static void panic(LivingEntity entity, Vec3 from) {
        if (isWild(entity)) PANIC.put(entity, new Panic(entity.level().getGameTime() + PANIC_TICKS, from));
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Animal animal) {
            animal.goalSelector.addGoal(0, new FleeGoal(animal));
        }
    }

    @SubscribeEvent
    static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        var attacker = event.getSource().getEntity();
        if (attacker != null) {
            panic(entity, attacker.position());
            return;
        }
        // bleeding, falling ...: keep running from what scared it, or else from the nearest player
        Panic old = PANIC.get(entity);
        Player near = entity.level().getNearestPlayer(entity, PANIC_RANGE);
        panic(entity, old != null ? old.from() : near != null ? near.position() : entity.position());
    }

    private static double noticeRange(Player player) {
        return player.isCrouching() ? NOTICE_SNEAK : player.isSprinting() ? NOTICE_SPRINT : NOTICE_WALK;
    }

    /** Runs away from noticed players, faster and further when panicking; spreads panic to animals that see it. */
    static class FleeGoal extends Goal {
        private final Animal mob;
        private Vec3 threat;
        private boolean panicking;

        FleeGoal(Animal mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private Player noticedPlayer() {
            Player nearest = mob.level().getNearestPlayer(mob, NOTICE_SPRINT * 1.5);
            if (nearest == null || nearest.isCreative() || nearest.isSpectator()) return null;
            double dist = mob.distanceTo(nearest);
            double skittish = mob instanceof com.mechanical.wildlife.WildAnimal wild ? wild.fearMultiplier() : 1.0;
            if (dist > noticeRange(nearest) * skittish) return null;
            return dist <= NOTICE_SNEAK || mob.hasLineOfSight(nearest) ? nearest : null;
        }

        private Vec3 findThreat() {
            if (!isWild(mob)) return null;
            if (mob.getTarget() != null) {
                // a hurt, bleeding animal gives up the fight and runs for its life
                if (!Blood.isBleeding(mob)) return null;
                mob.setTarget(null);
            }
            panicking = isPanicking(mob);
            if (panicking) {
                Player near = mob.level().getNearestPlayer(mob, PANIC_RANGE);
                return near != null && !near.isCreative() && !near.isSpectator() ? near.position() : PANIC.get(mob).from();
            }
            Player noticed = noticedPlayer();
            return noticed != null ? noticed.position() : null;
        }

        @Override
        public boolean canUse() {
            // asleep: only waking up (WildAnimal.wakeIfDisturbed) makes it run
            if (mob instanceof com.mechanical.wildlife.WildAnimal wild && wild.isDozing()) return false;
            threat = findThreat();
            return threat != null && flee();
        }

        private boolean flee() {
            int range = panicking ? 20 : 12;
            Vec3 target = DefaultRandomPos.getPosAway(mob, range, 7, threat);
            if (target == null) target = DefaultRandomPos.getPosAway(mob, range / 2, 4, threat);
            if (target == null) {
                // cornered or odd terrain: just head straight away from the threat
                Vec3 away = mob.position().subtract(threat).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) return false;
                target = mob.position().add(away.normalize().scale(range / 2.0));
            }
            return mob.getNavigation().moveTo(target.x, target.y, target.z, panicking ? PANIC_SPEED : FLEE_SPEED);
        }

        /** The path leads towards the threat, not away (the player moved round, or the path bends back). */
        private boolean headingTowards(Vec3 t) {
            var goal = mob.getNavigation().getTargetPos();
            if (goal == null) return false;
            Vec3 toGoal = Vec3.atBottomCenterOf(goal).subtract(mob.position()).multiply(1, 0, 1);
            Vec3 toThreat = t.subtract(mob.position()).multiply(1, 0, 1);
            return toGoal.dot(toThreat) > 0 || Vec3.atBottomCenterOf(goal).distanceToSqr(t) < mob.position().distanceToSqr(t);
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.getNavigation().isDone();
        }

        @Override
        public void tick() {
            if (mob.tickCount % 10 != 0) return;
            // keep running while the threat is close, and spread the panic to animals that can see us
            Vec3 t = findThreat();
            if (t != null && (mob.getNavigation().isDone() || headingTowards(t))) {
                threat = t;
                flee();
            }
            if (panicking || Blood.isBleeding(mob)) {
                for (Animal other : mob.level().getEntitiesOfClass(Animal.class, mob.getBoundingBox().inflate(CONTAGION_RANGE),
                        o -> o != mob && !isPanicking(o) && o.hasLineOfSight(mob))) {
                    panic(other, threat != null ? threat : mob.position());
                }
            }
        }
    }

    private AnimalFear() {}
}
