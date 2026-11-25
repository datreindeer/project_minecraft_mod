package com.mechanical.wildlife;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Base for the mod's wild animals (forest wildlife). Wild ones don't breed, roam in loose herds, graze now and then, and
 * use the shared fear/panic behaviour from {@link com.mechanical.AnimalFear} (added to every animal on join).
 * Subclasses set attributes, sounds, texture and any special behaviour.
 * <p>The species in {@link Taming} can be tamed: crouch with their favourite food (they don't shy from that,
 * {@link #calmAround}) and feed them by hand until they are tame; once they half trust you, you may stand. A tamed one
 * no longer flees or turns on people, follows its food, and is livestock ({@link com.mechanical.Husbandry}): it needs food
 * and water and breeds by itself when content. Hurting a wild one costs all its trust.
 */
public abstract class WildAnimal extends Animal {
    /** Grazing: counts down while the head is lowered; synced for the head-down animation. */
    private static final EntityDataAccessor<Integer> GRAZE = SynchedEntityData.defineId(WildAnimal.class, EntityDataSerializers.INT);
    public static final int GRAZE_TICKS = 60;
    /** Asleep at night, lying down (not vanilla bed sleeping, hence "dozing"). Synced for the lying pose. */
    private static final EntityDataAccessor<Boolean> DOZING = SynchedEntityData.defineId(WildAnimal.class, EntityDataSerializers.BOOLEAN);
    /** A sleeping animal wakes when a player comes this close (sneaking: much closer), then bolts. */
    private static final double WAKE_RANGE = 7, WAKE_RANGE_SNEAKING = 2.5;
    /** Sluggish in winter (WinterAnimals): slower, and strolls a third as often. */
    private static final Identifier WINTER_ID = Identifier.fromNamespaceAndPath("mechanical", "winter_sluggish");
    private static final AttributeModifier WINTER_SLOW = new AttributeModifier(WINTER_ID, -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    /**
     * How a species deals with water. {@code slowDown} is the per-tick water drag in place of vanilla's 0.8 (which leaves
     * land animals crawling along at a seventh of their walking pace).
     */
    public enum WaterHabit {
        /** Water animals: path through water like over land, swim at a good pace, own swimming animation. */
        SWIMMER(0.95F),
        /** Keep out of water (path around it), but when they must go in they swim properly and make for the shore. */
        AVOIDS(0.91F),
        /** Keep out of water and swim poorly: paddle slowly to the nearest shore. */
        POOR(0.86F),
        /** Handles water itself (birds, snakes): vanilla. */
        OWN(0.8F);

        final float slowDown;

        WaterHabit(float slowDown) {
            this.slowDown = slowDown;
        }

        /** Shares the generic quadruped swimming pose (head up, body low, legs paddling) and makes for the shore. */
        public boolean landAnimal() {
            return this == AVOIDS || this == POOR;
        }
    }

    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(WildAnimal.class, EntityDataSerializers.BOOLEAN);
    private int tameTrust, tameCooldown;

    protected WildAnimal(EntityType<? extends WildAnimal> type, Level level) {
        super(type, level);
        if (waterHabit() == WaterHabit.SWIMMER) {
            setPathfindingMalus(PathType.WATER, 0.0F);
            setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
        } // the others keep vanilla's water malus (8 per block): they go round water unless there's no other way
    }

    /** Called from the constructor: return a constant. Most large land animals keep the default. */
    public WaterHabit waterHabit() {
        return WaterHabit.AVOIDS;
    }

    @Override
    protected float getWaterSlowDown() {
        return waterHabit().slowDown;
    }

    /** Swimming (for models): in water and not standing on the bottom. */
    public boolean isSwimmingNow() {
        return isInWater() && !onGround();
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

    /** Fraction of max health below which it bleeds (Blood). */
    public float bleedThreshold() {
        return 0.5F;
    }

    /** True: no blood spray on hits (and no arrow-wound bleeding) until it is below {@link #bleedThreshold()}. */
    public boolean bloodOnlyWhenBleeding() {
        return false;
    }

    /** Antlers / tusks shown, for models that have them. */
    public boolean showsHorns() {
        return false;
    }

    /** Head lowered for a charge (boar, stag), for models. */
    public boolean isCharging() {
        // the target only exists server-side; MeleeAttackGoal's synced aggressive flag tells the client
        return getTarget() != null || isAggressive();
    }

    public boolean isDozing() {
        return entityData.get(DOZING);
    }

    /** Lowers the head as for grazing (digging, rooting), unless it is already down. */
    protected void startGrazing() {
        if (entityData.get(GRAZE) <= 0) entityData.set(GRAZE, GRAZE_TICKS);
    }

    /** Wakes it up (birds taking off with their flock). */
    protected void wake() {
        entityData.set(DOZING, false);
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
        builder.define(TAMED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (waterHabit().landAnimal()) goalSelector.addGoal(1, new ShoreGoal(this));
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.BreedGoal(this, 1.0));
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.TemptGoal(this, 1.1, this::isFood, false) {
            @Override
            public boolean canUse() {
                return isTamed() && super.canUse();
            }
        });
        goalSelector.addGoal(3, new DozeGoal(this));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new FollowHerdGoal(this));
        goalSelector.addGoal(6, new GrazeGoal(this));
        // water animals now and then stroll into the water too
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8, waterHabit() == WaterHabit.SWIMMER ? 0.25F : 0.001F) {
            @Override
            public boolean canUse() {
                return (!com.mechanical.WinterAnimals.winter(mob.level()) || mob.getRandom().nextInt(3) == 0) && super.canUse();
            }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        int t = entityData.get(GRAZE);
        if (t > 0) entityData.set(GRAZE, t - 1);
        if (tameCooldown > 0) tameCooldown--;
        if (isDozing() && !level().isClientSide()) wakeIfDisturbed();
        if (!level().isClientSide() && (tickCount + getId()) % 100 == 0) winterPace();
        super.aiStep();
        if (!level().isClientSide() && isInWater()) hauloutOntoBank();
    }

    /**
     * Swimming against a bank on its way somewhere: haul out onto it (swimming alone won't climb out of a ditch, and a
     * one-block bank is already too high for the float-jumps of most animals).
     */
    private void hauloutOntoBank() {
        if (!horizontalCollision || zza <= 0.0F) return; // zza: the move control steered for its wanted position this tick
        Vec3 step = new Vec3(getMoveControl().getWantedX() - getX(), 0.0, getMoveControl().getWantedZ() - getZ());
        if (step.lengthSqr() < 1.0E-4) return;
        step = step.normalize().scale(getBbWidth() / 2 + 0.5);
        double x = getX() + step.x, z = getZ() + step.z;
        for (int y = getBlockY() + 1; y <= getBlockY() + 2; y++) {
            BlockPos at = BlockPos.containing(x, y, z);
            if (level().getFluidState(at).isEmpty() && level().getBlockState(at).getCollisionShape(level(), at).isEmpty()
                    && level().getBlockState(at.below()).isFaceSturdy(level(), at.below(), net.minecraft.core.Direction.UP)
                    && level().noCollision(this, getBoundingBox().move(x - getX(), y - getY(), z - getZ()))) {
                setPos(x, y, z);
                return;
            }
        }
    }

    private void winterPace() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        boolean winter = com.mechanical.WinterAnimals.winter(level());
        if (speed == null || winter == speed.hasModifier(WINTER_ID)) return;
        if (winter) speed.addTransientModifier(WINTER_SLOW);
        else speed.removeModifier(WINTER_ID);
    }

    private void wakeIfDisturbed() {
        if (hurtTime > 0 || !sleepTime() || isInWater()) {
            entityData.set(DOZING, false);
            return;
        }
        Player near = isTamed() ? null : level().getNearestPlayer(this, WAKE_RANGE);
        if (near != null && !near.isSpectator() && !near.isCreative() && !calmAround(near)
                && (!near.isShiftKeyDown() || distanceTo(near) < WAKE_RANGE_SNEAKING)) {
            entityData.set(DOZING, false);
            onWoken(near);
        }
    }

    /** When it sleeps: at night; predators (Predator) sleep by day instead. */
    protected boolean sleepTime() {
        return level().isDarkOutside();
    }

    /** Woken by a player: bolts. Predators override this (the brown bear attacks). */
    protected void onWoken(Player near) {
        com.mechanical.AnimalFear.panic(this, near.position());
    }

    // ---- taming; only the tamed breed (and nobody in winter, see WinterAnimals) ----

    public boolean isTamed() {
        return entityData.get(TAMED);
    }

    /** Feedings so far. */
    public int tameTrust() {
        return tameTrust;
    }

    private boolean holdsFavourite(Player player) {
        Taming.Diet diet = Taming.diet(getType());
        return diet != null && (player.getMainHandItem().is(diet.food()) || player.getOffhandItem().is(diet.food()));
    }

    /** Not shy of this player: crouching with its favourite food, or half won over and he isn't running. */
    public boolean calmAround(Player player) {
        Taming.Diet diet = Taming.diet(getType());
        return diet != null && (player.isCrouching() && holdsFavourite(player) || tameTrust * 2 >= diet.feeds() && !player.isSprinting());
    }

    /** The one choke point for turning on people: never when tamed, nor on a calm player who hasn't hurt it. */
    @Override
    public void setTarget(LivingEntity target) {
        if (target instanceof Player p && (isTamed() || p != getLastHurtByMob() && calmAround(p))) return;
        super.setTarget(target);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Taming.Diet diet = Taming.diet(getType());
        // never down to vanilla's feeding for these: a tamed one's meals are Husbandry's business, and none breeds by hand
        if (diet == null || isTamed() || !stack.is(diet.food())) return super.mobInteract(player, hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        tameFeed(player, stack, diet);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Server: one hand-feeding of a wild one. Standing up before it half trusts you, it bolts. */
    public void tameFeed(Player player, ItemStack stack, Taming.Diet diet) {
        if (!player.isCrouching() && tameTrust * 2 < diet.feeds()) {
            com.mechanical.AnimalFear.panic(this, player.position());
            player.sendOverlayMessage(Component.translatable("message.mechanical.taming.shies", getDisplayName()));
            return;
        }
        if (tameCooldown > 0) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.taming.chewing", getDisplayName()));
            return;
        }
        stack.consume(1, player);
        com.mechanical.Blood.stanch(this, (ServerLevel) level());
        tameCooldown = Taming.FEED_COOLDOWN;
        tameTrust++;
        wake();
        setTarget(null);
        getNavigation().stop();
        startGrazing();
        playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 0.9F);
        boolean done = tameTrust >= diet.feeds();
        ((ServerLevel) level()).sendParticles(done ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER,
                getX(), getY() + getBbHeight() + 0.2, getZ(), done ? 7 : 4, getBbWidth() / 2, 0.2, getBbWidth() / 2, 0);
        if (!done) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.taming.trust", getDisplayName(), tameTrust, diet.feeds()));
            return;
        }
        setTamed();
        // the "tame a wild animal" advancements listen for this
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) net.minecraft.advancements.triggers.CriteriaTriggers.TAME_ANIMAL.trigger(sp, this);
        player.sendOverlayMessage(Component.translatable("message.mechanical.taming.tamed", getDisplayName()));
    }

    /** Tame from now on: it stays for good and fights no one. */
    public void setTamed() {
        entityData.set(TAMED, true);
        setPersistenceRequired();
        setTarget(null);
        setAggressive(false);
    }

    /** Head down to eat or drink (tamed ones at the trough or on the grass). */
    public void showEating() {
        wake();
        startGrazing();
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) {
        if (source.getEntity() instanceof Player && !isTamed()) tameTrust = 0;
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (isTamed()) output.putBoolean("tamed", true);
        if (tameTrust > 0) output.putInt("tame_trust", tameTrust);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(TAMED, input.getBooleanOr("tamed", false));
        tameTrust = input.getIntOr("tame_trust", 0);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        Taming.Diet diet = Taming.diet(getType());
        return diet != null && stack.is(diet.food());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        if (!isTamed() || !(getType().create(level, EntitySpawnReason.BREEDING) instanceof WildAnimal young)) return null;
        // rolls the sex, for the species that have two looks
        young.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.BREEDING, null);
        young.setTamed();
        return young;
    }

    @Override
    public boolean canMate(Animal partner) {
        return partner != this && partner.getType() == getType() && partner instanceof WildAnimal other && isTamed() && other.isTamed()
                && isInLove() && other.isInLove();
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
            List<? extends WildAnimal> herd = mob.level().getEntitiesOfClass(mob.getClass(), mob.getBoundingBox().inflate(SEARCH), o -> o != mob && o.isTamed() == mob.isTamed());
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

    /**
     * Land animals in the water with nowhere to go (a flight ended there, a herd stroll gave out): swim to the nearest
     * dry ground instead of bobbing in place. Fleeing (AnimalFear, priority 0) or a hunt takes precedence.
     */
    static class ShoreGoal extends Goal {
        private static final int RANGE = 16, MAX_TICKS = 400;
        private final WildAnimal mob;
        private BlockPos shore;
        private int ticks;

        ShoreGoal(WildAnimal mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!mob.isInWater() || (mob.tickCount + mob.getId()) % 20 != 0 || !mob.getNavigation().isDone() || mob.getTarget() != null) return false;
            shore = null;
            for (BlockPos p : BlockPos.withinBoxByManhattanDistance(mob.blockPosition(), RANGE, 4, RANGE)) {
                if (WalkNodeEvaluator.getPathTypeStatic(mob, p) == PathType.WALKABLE) {
                    shore = p.immutable();
                    break;
                }
            }
            return shore != null;
        }

        private double speed() {
            return mob.waterHabit() == WaterHabit.POOR ? 1.0 : 1.3;
        }

        @Override
        public void start() {
            ticks = MAX_TICKS;
            mob.getNavigation().moveTo(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5, speed());
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && mob.isInWater() && mob.getTarget() == null;
        }

        @Override
        public void tick() {
            // no path (odd terrain): steer straight for it
            if (mob.getNavigation().isDone()) mob.getMoveControl().setWantedPosition(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5, speed());
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
            return mob.sleepTime() && mob.onGround() && !mob.isInWater() && mob.getTarget() == null
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
            WildNature.grazed(mob);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.entityData.get(GRAZE) > 0;
        }
    }
}
