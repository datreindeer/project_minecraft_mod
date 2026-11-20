package com.mechanical.wildlife;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.BloodMoon;
import com.mechanical.Mechanical;
import com.mechanical.NightDangers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Night predators (grey wolf, cave lion): sleep by day and keep away from players then; hunt at night (or any time on a
 * blood moon, or when starving). Afraid of fire: they keep clear of burning campfires/kilns (NightDangers' fire registry),
 * won't go for someone sitting by one, and won't come close to someone holding a torch (item tag
 * {@code mechanical:scares_predators}) - unless it is a blood moon or they are starving.
 */
public abstract class Predator extends WildAnimal {
    public static final TagKey<Item> SCARES_PREDATORS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "scares_predators"));
    /** Keeps this far from a burning fire, and leaves alone anyone within FIRE_SAFE of one. */
    static final double FIRE_AVOID = 12, FIRE_SAFE = 8;
    /** Stays at least this far from someone holding a torch. */
    static final double TORCH_KEEP_OFF = 4.5;
    /** Ticks since the last kill after which it is starving and fire no longer keeps it off (two days). */
    private static final int STARVING = 48000;
    /** Tag on predators spawned by the night spawner (Predators): they may despawn when far from players. */
    public static final String NIGHT_TAG = "mechanical.night_predator";

    /** Crouched, stalking (lion; wolves creeping up). Synced for the model. */
    private static final EntityDataAccessor<Boolean> CROUCHING = SynchedEntityData.defineId(Predator.class, EntityDataSerializers.BOOLEAN);
    /** Counts down while howling / roaring; synced for the head-up animation. */
    private static final EntityDataAccessor<Integer> CALLING = SynchedEntityData.defineId(Predator.class, EntityDataSerializers.INT);
    /** In the air, leaping at prey. Synced for the model. */
    private static final EntityDataAccessor<Boolean> LEAPING = SynchedEntityData.defineId(Predator.class, EntityDataSerializers.BOOLEAN);

    private int hunger;
    private long retreatUntil;

    protected Predator(EntityType<? extends Predator> type, Level level) {
        super(type, level);
    }

    /** Animals (or people) it goes after at night. */
    protected abstract boolean isPrey(LivingEntity e);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CROUCHING, false);
        builder.define(CALLING, 0);
        builder.define(LEAPING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new AvoidFireGoal(this));
        goalSelector.addGoal(3, new CarcassScentGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        hunger++;
        int c = entityData.get(CALLING);
        if (c > 0) entityData.set(CALLING, c - 1);
        if (isLeaping() && onGround() && getDeltaMovement().y <= 0) entityData.set(LEAPING, false);
    }

    // ---- state ----

    public boolean isCrouching() {
        return entityData.get(CROUCHING);
    }

    protected void setCrouching(boolean crouching) {
        entityData.set(CROUCHING, crouching);
    }

    public boolean isLeaping() {
        return entityData.get(LEAPING);
    }

    protected void setLeaping(boolean leaping) {
        entityData.set(LEAPING, leaping);
    }

    /** 0..1: head thrown back for a howl / roar. */
    public float callAmount() {
        int t = entityData.get(CALLING);
        return t <= 0 ? 0 : Math.min(1.0F, Math.min(t, 50 - t) / 8.0F);
    }

    protected void startCalling(int ticks) {
        entityData.set(CALLING, ticks);
    }

    protected boolean isCalling() {
        return entityData.get(CALLING) > 0;
    }

    public boolean starving() {
        return hunger > STARVING;
    }

    /** Blood moon or starving: fire no longer keeps it off. */
    protected boolean fearless() {
        return BloodMoon.isActive(level()) || starving();
    }

    /** Night (or a blood moon, or hunger): time to hunt. */
    protected boolean huntingTime() {
        return level().isDarkOutside() || BloodMoon.isActive(level()) || starving();
    }

    protected boolean retreating() {
        return retreatUntil > level().getGameTime();
    }

    /** Breaks off and runs (badly hurt, pack halved, scared off). */
    protected void retreat(Vec3 from, int ticks) {
        retreatUntil = level().getGameTime() + ticks;
        setTarget(null);
        setCrouching(false);
        AnimalFear.panic(this, from);
    }

    /** By day, or while retreating, it behaves like other wild animals and keeps away from players (AnimalFear). */
    public boolean fearsPlayers() {
        return retreating() || !huntingTime();
    }

    @Override
    protected boolean sleepTime() {
        return level().isBrightOutside() && !BloodMoon.isActive(level()) && !starving();
    }

    @Override
    public double fearMultiplier() {
        return 1.3;
    }

    /** {@code e} holds a torch or similar (main or off hand). */
    static boolean holdsFire(LivingEntity e) {
        return e.getMainHandItem().is(SCARES_PREDATORS) || e.getOffhandItem().is(SCARES_PREDATORS);
    }

    protected boolean torchKeepsOff(LivingEntity e) {
        return !fearless() && holdsFire(e);
    }

    /**
     * If {@code t} shelters by a fire: prowls round that fire just beyond its reach (FIRE_AVOID + 4), on its own side of
     * it (so the path never crosses the light), and returns true. Call every tick; repaths every second.
     */
    protected boolean prowlRoundFire(LivingEntity t, double speed) {
        if (fearless()) return false;
        BlockPos fire = NightDangers.nearestFire(level(), t.position(), FIRE_SAFE);
        if (fire == null) return false;
        if (tickCount % 20 == 0) {
            Vec3 c = Vec3.atCenterOf(fire), off = position().subtract(c);
            double a = Math.atan2(off.z, off.x) + (getId() % 2 == 0 ? 0.4 : -0.4);
            double r = (FIRE_AVOID + 4) * NightDangers.reach(level(), fire); // a Fire Pit's reach is bigger
            getNavigation().moveTo(c.x + Math.cos(a) * r, getY(), c.z + Math.sin(a) * r, speed);
        }
        return true;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        hunger = 0;
        return super.killedEntity(level, entity, source);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return entityTags().contains(NIGHT_TAG);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("hunger", hunger);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        hunger = input.getIntOr("hunger", random.nextInt(STARVING / 2));
    }

    /** Valid quarry right now. */
    protected boolean canHunt(LivingEntity e) {
        if (e == null || !e.isAlive() || e == this) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return isPrey(e);
    }

    /**
     * The smell of blood: at hunting time a carcass within {@link #RANGE} blocks draws it. One being carried, dragged or
     * pulled on a travois makes the one hauling it its quarry (the hunting goals take over, fire fear included). One lying
     * unattended or hung on a rack it goes to and eats (10 s), leaving the bones - unless a burning fire is within
     * FIRE_SAFE of the carcass (blood moon and hunger excepted).
     */
    static class CarcassScentGoal extends Goal {
        static final double RANGE = 40;
        private static final int FEED_TICKS = 200;
        private final Predator mob;
        private com.mechanical.CarcassEntity carcass;
        private int feeding;

        CarcassScentGoal(Predator mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        /** The player carrying, dragging or pulling it on a travois, or null. */
        static LivingEntity keeper(com.mechanical.CarcassEntity c) {
            Player p = c.hauler();
            if (p != null) return p;
            return c.getVehicle() instanceof com.mechanical.TravoisEntity t ? t.harness() : null;
        }

        private boolean guarded(com.mechanical.CarcassEntity c) {
            return !mob.fearless() && NightDangers.nearFire(mob.level(), c.position(), FIRE_SAFE);
        }

        @Override
        public boolean canUse() {
            if (mob.getRandom().nextInt(20) != 0 || !mob.huntingTime() || mob.retreating() || mob.isDozing() || mob.getTarget() != null) return false;
            carcass = null;
            double best = Double.MAX_VALUE;
            for (com.mechanical.CarcassEntity c : mob.level().getEntitiesOfClass(com.mechanical.CarcassEntity.class, mob.getBoundingBox().inflate(RANGE))) {
                if (c.animal() == mob.getType()) continue; // not its own kind
                LivingEntity keeper = keeper(c);
                if (keeper != null) {
                    if (mob.canHunt(keeper)) {
                        mob.setTarget(keeper);
                        return false;
                    }
                    continue;
                }
                double d = mob.distanceToSqr(c);
                if (d < best && !guarded(c)) {
                    best = d;
                    carcass = c;
                }
            }
            return carcass != null && mob.getNavigation().moveTo(carcass, 1.0);
        }

        @Override
        public void start() {
            feeding = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return carcass.isAlive() && keeper(carcass) == null && mob.getTarget() == null && !mob.retreating() && mob.huntingTime() && !guarded(carcass);
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 c = carcass.getBoundingBox().getCenter();
            mob.getLookControl().setLookAt(c);
            double reach = 1.2 + (mob.getBbWidth() + carcass.getBbWidth()) / 2;
            if (mob.distanceToSqr(c.x, carcass.getY(), c.z) > reach * reach) {
                if (mob.tickCount % 20 == 0) mob.getNavigation().moveTo(carcass, 1.0);
                return;
            }
            mob.getNavigation().stop();
            ServerLevel level = (ServerLevel) mob.level();
            if (++feeding % 10 == 0) {
                com.mechanical.Blood.spray(level, c.x, c.y, c.z, 3);
                mob.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), 0.8F, 0.6F);
            }
            if (feeding < FEED_TICKS) return;
            // eaten down to the bones
            com.mechanical.Blood.splatter(level, carcass.blockPosition(), 1, 4);
            BlockPos pos = carcass.blockPosition();
            var pile = Mechanical.BONE_PILE.get().defaultBlockState();
            if (carcass.hungAt() == null && level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos))
                level.setBlock(pos, pile, net.minecraft.world.level.block.Block.UPDATE_ALL);
            carcass.discard();
            mob.hunger = 0;
        }
    }

    /** Walks out of a burning fire's reach (except on a blood moon or when starving) and drops a target sheltering by it. */
    static class AvoidFireGoal extends Goal {
        private final Predator mob;

        AvoidFireGoal(Predator mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // canUse is only polled every other tick (which ones depends on the entity id), so roll rather than test tickCount
            if (mob.getRandom().nextInt(5) != 0 || mob.fearless()) return false;
            BlockPos fire = NightDangers.nearestFire(mob.level(), mob.position(), FIRE_AVOID);
            if (fire == null) return false;
            Vec3 away = DefaultRandomPos.getPosAway(mob, (int) (FIRE_AVOID * NightDangers.reach(mob.level(), fire)) + 6, 7, Vec3.atCenterOf(fire));
            return away != null && mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.getNavigation().isDone();
        }
    }
}
