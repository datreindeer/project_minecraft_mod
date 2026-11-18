package com.mechanical.wildlife;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Base for the mod's wild animals (forest wildlife). They don't breed, roam in loose herds, graze now and then, and
 * use the shared fear/panic behaviour from {@link com.mechanical.AnimalFear} (added to every animal on join).
 * Subclasses set attributes, sounds, texture and any special behaviour.
 */
public abstract class WildAnimal extends Animal {
    /** Grazing: counts down while the head is lowered; synced for the head-down animation. */
    private static final EntityDataAccessor<Integer> GRAZE = SynchedEntityData.defineId(WildAnimal.class, EntityDataSerializers.INT);
    public static final int GRAZE_TICKS = 60;
    /** Asleep at night, lying down (not vanilla bed sleeping, hence "dozing"). Synced for the lying pose. */
    private static final EntityDataAccessor<Boolean> DOZING = SynchedEntityData.defineId(WildAnimal.class, EntityDataSerializers.BOOLEAN);
    /** A sleeping animal wakes when a player comes this close (sneaking: much closer), then bolts. */
    private static final double WAKE_RANGE = 7, WAKE_RANGE_SNEAKING = 2.5;

    protected WildAnimal(EntityType<? extends WildAnimal> type, Level level) {
        super(type, level);
    }

    /** Texture for the renderer (common-side so variants like male/female can pick one). */
    public abstract Identifier texture();

    /** Chance that a herd member after the first spawns as a baby. */
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        return super.finalizeSpawn(level, difficulty, reason, groupData != null ? groupData : new AgeableMob.AgeableMobGroupData(babyChance()));
    }

    /** How skittish: multiplies the distance at which players are noticed (1 = normal). */
    public double fearMultiplier() {
        return 1.0;
    }

    /** Antlers / tusks shown, for models that have them. */
    public boolean showsHorns() {
        return false;
    }

    /** Head lowered for a charge (boar, stag), for models. */
    public boolean isCharging() {
        return getTarget() != null;
    }

    public boolean isDozing() {
        return entityData.get(DOZING);
    }

    /** 0..1: how far the head is lowered for grazing. */
    public float grazeAmount(float partialTick) {
        int t = entityData.get(GRAZE);
        if (t <= 0) return 0;
        // ease in and out over the first/last 10 ticks
        float edge = Math.min(t, GRAZE_TICKS - t) + (t < GRAZE_TICKS / 2 ? -partialTick : partialTick);
        return Math.clamp(edge / 10.0F, 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GRAZE, 0);
        builder.define(DOZING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new DozeGoal(this));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new FollowHerdGoal(this));
        goalSelector.addGoal(6, new GrazeGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        int t = entityData.get(GRAZE);
        if (t > 0) entityData.set(GRAZE, t - 1);
        if (isDozing() && !level().isClientSide()) wakeIfDisturbed();
        super.aiStep();
    }

    private void wakeIfDisturbed() {
        if (hurtTime > 0 || !level().isDarkOutside() || isInWater()) {
            entityData.set(DOZING, false);
            return;
        }
        Player near = level().getNearestPlayer(this, WAKE_RANGE);
        if (near != null && !near.isSpectator() && !near.isCreative()
                && (!near.isShiftKeyDown() || distanceTo(near) < WAKE_RANGE_SNEAKING)) {
            entityData.set(DOZING, false);
            com.mechanical.AnimalFear.panic(this, near.position());
        }
    }

    // ---- wild: no breeding ----

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    /** Keeps loosely together: walks back towards the nearest of its kind when it strays too far. */
    static class FollowHerdGoal extends Goal {
        private static final double STRAY = 10, SEARCH = 24;
        private final WildAnimal mob;
        private WildAnimal other;

        FollowHerdGoal(WildAnimal mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getRandom().nextInt(40) != 0) return false;
            List<? extends WildAnimal> herd = mob.level().getEntitiesOfClass(mob.getClass(), mob.getBoundingBox().inflate(SEARCH), o -> o != mob);
            other = herd.stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
            return other != null && mob.distanceTo(other) > STRAY;
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(other, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return other.isAlive() && mob.distanceTo(other) > STRAY / 2 && !mob.getNavigation().isDone();
        }
    }

    /** At night: lies down and sleeps until morning, or until woken (see {@link #wakeIfDisturbed}). */
    static class DozeGoal extends Goal {
        private final WildAnimal mob;

        DozeGoal(WildAnimal mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return mob.level().isDarkOutside() && mob.onGround() && !mob.isInWater() && mob.getTarget() == null
                    && !com.mechanical.AnimalFear.isPanicking(mob) && mob.getRandom().nextInt(100) == 0
                    && mob.level().getNearestPlayer(mob, WAKE_RANGE * 2) == null;
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
            mob.entityData.set(DOZING, true);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.isDozing();
        }

        @Override
        public void tick() {
            mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            mob.entityData.set(DOZING, false);
        }
    }

    /** Stands still with the head down for a few seconds. */
    static class GrazeGoal extends Goal {
        private final WildAnimal mob;

        GrazeGoal(WildAnimal mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return mob.onGround() && mob.getRandom().nextInt(200) == 0;
        }

        @Override
        public void start() {
            mob.entityData.set(GRAZE, GRAZE_TICKS);
            mob.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return mob.entityData.get(GRAZE) > 0;
        }
    }
}
