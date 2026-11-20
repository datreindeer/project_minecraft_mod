package com.mechanical.wildlife.mountain;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Alpine marmot: a fat ground squirrel living in colonies (3-6) on mountain meadows, each with its burrow (the spot it
 * was born, saved). One always keeps watch: a player in sight within 14 blocks (5 sneaking) or a predator within 16 makes
 * it sit up on its haunches, and a second later it whistles - a shrill alarm heard far off ({@link #whistle}) that makes
 * every animal within 24 blocks bolt (AnimalFear panic; flocks take off) and sends the whole colony running for their
 * burrows. Down the burrow a marmot is out of reach (hidden: not drawn, can't be hit) until nothing has threatened it for
 * 10 s. Marmots also spend the night in the burrow. Small enough for a Snare. Fat for tallow, lamps and salve.
 */
public class Marmot extends WildAnimal {
    private static final Identifier TEXTURE = Ibex.tex("marmot"), BABY_TEXTURE = Ibex.tex("marmot_pup");
    /** Sitting up, watching (synced for the model through isCharging). */
    private static final EntityDataAccessor<Boolean> ALERT = SynchedEntityData.defineId(Marmot.class, EntityDataSerializers.BOOLEAN);
    /** Down its burrow (synced: the renderer draws nothing). */
    private static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.defineId(Marmot.class, EntityDataSerializers.BOOLEAN);
    static final double WHISTLE_RANGE = 24, WATCH_RANGE = 14, WATCH_RANGE_SNEAKING = 5, PREDATOR_RANGE = 16, BURROW_REACH = 24;
    private static final int ALARM_TICKS = 300, WHISTLE_COOLDOWN = 200, SAFE_TICKS = 200;

    private BlockPos home;
    private long alarmedUntil;
    private int whistleCooldown;

    public Marmot(EntityType<? extends Marmot> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.27);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ALERT, false);
        builder.define(HIDDEN, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // priority 0 and before AnimalFear's flee goal (added on join): a marmot runs for its burrow, not just away
        goalSelector.addGoal(0, new BurrowGoal());
        goalSelector.addGoal(2, new SentinelGoal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        home = blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (home != null) output.putLong("burrow", home.asLong());
        output.putBoolean("hidden", isHidden());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        home = input.getLong("burrow").map(BlockPos::of).orElse(null);
        setHidden(input.getBooleanOr("hidden", false));
    }

    public boolean isHidden() {
        return entityData.get(HIDDEN);
    }

    private void setHidden(boolean hidden) {
        entityData.set(HIDDEN, hidden);
        setInvisible(hidden);
    }

    public BlockPos burrow() {
        return home;
    }

    @Override
    public boolean isPickable() {
        return !isHidden() && super.isPickable();
    }

    @Override
    public boolean isCharging() {
        return entityData.get(ALERT);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (home == null) home = blockPosition();
        if (whistleCooldown > 0) whistleCooldown--;
    }

    boolean alarmed() {
        return alarmedUntil > level().getGameTime();
    }

    /** Heard (or gave) the alarm: makes for the burrow. */
    void alarm() {
        alarmedUntil = level().getGameTime() + ALARM_TICKS;
        entityData.set(ALERT, false);
    }

    /**
     * The alarm whistle: everything that hears it within {@link #WHISTLE_RANGE} takes fright - other marmots dive for
     * their burrows, flocks take off, other animals panic away from {@code from}; predators on the hunt don't care.
     */
    void whistle(Vec3 from) {
        whistleCooldown = WHISTLE_COOLDOWN;
        level().playSound(null, getX(), getEyeY(), getZ(), MountainFauna.MARMOT_WHISTLE, SoundSource.NEUTRAL, 3.0F, 0.95F + random.nextFloat() * 0.1F);
        for (Animal a : level().getEntitiesOfClass(Animal.class, getBoundingBox().inflate(WHISTLE_RANGE), a -> a != this)) {
            if (a instanceof Marmot m) {
                m.alarm();
                m.whistleCooldown = WHISTLE_COOLDOWN; // one whistle for the whole colony
            } else if (a instanceof FlockBird bird) bird.scare(from);
            else AnimalFear.panic(a, from);
        }
        alarm();
    }

    /** A player in sight (closer when he sneaks) or a predator nearby, or null. */
    Vec3 threat(double scale) {
        for (Player p : level().players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            double range = (p.isShiftKeyDown() ? WATCH_RANGE_SNEAKING : WATCH_RANGE) * scale;
            if (distanceTo(p) < range && hasLineOfSight(p)) return p.position();
        }
        LivingEntity hunter = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PREDATOR_RANGE * scale),
                e -> e instanceof Predator || e instanceof Fox || e instanceof Wolf w && !w.isTame())
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        return hunter != null ? hunter.position() : null;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && whistleCooldown == 0 && isAlive()) whistle(source.getEntity() != null ? source.getEntity().position() : position());
        return hurt;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.35F;
    }

    @Override
    public double fearMultiplier() {
        return 1.3;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isHidden() ? null : SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isHidden()) playSound(SoundEvents.RABBIT_JUMP, 0.08F, 0.8F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    /** Watching: a threat makes it sit up for a second, then whistle (unless the colony just did). */
    class SentinelGoal extends Goal {
        private int sitting;
        private Vec3 seen;

        SentinelGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isHidden() || alarmed() || isDozing() || whistleCooldown > 0 || random.nextInt(4) != 0) return false;
            seen = threat(1.0);
            return seen != null;
        }

        @Override
        public void start() {
            sitting = 25;
            getNavigation().stop();
            entityData.set(ALERT, true);
        }

        @Override
        public boolean canContinueToUse() {
            return sitting > 0 && !alarmed();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(seen.x, seen.y + 1, seen.z);
            if (--sitting == 0 && whistleCooldown == 0) whistle(seen);
        }

        @Override
        public void stop() {
            entityData.set(ALERT, false);
        }
    }

    /**
     * Alarmed (or night): runs to its burrow and goes down it. Hidden it waits until nothing has threatened it for
     * {@link #SAFE_TICKS} (and, at night, until morning), then comes up. A burrow out of reach (moved, cut off): it just
     * flees like any animal.
     */
    class BurrowGoal extends Goal {
        private int safe;

        BurrowGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean wantsDown() {
            return alarmed() || level().isDarkOutside() || AnimalFear.isPanicking(Marmot.this);
        }

        @Override
        public boolean canUse() {
            if (isHidden()) return true;
            if (home == null || !wantsDown() || isLeashed() || home.distToCenterSqr(position()) > BURROW_REACH * BURROW_REACH) return false;
            return getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.8) || closeToHome();
        }

        private boolean closeToHome() {
            return home.distToCenterSqr(getX(), home.getY() + 0.5, getZ()) < 2.5;
        }

        @Override
        public void start() {
            safe = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return isHidden() || wantsDown() && (!getNavigation().isDone() || closeToHome());
        }

        @Override
        public void tick() {
            if (!isHidden()) {
                if (closeToHome()) {
                    getNavigation().stop();
                    setHidden(true);
                    playSound(SoundEvents.ROOTED_DIRT_BREAK, 0.6F, 1.2F);
                } else if ((tickCount + getId()) % 20 == 0) {
                    getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.8);
                }
                return;
            }
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            if ((tickCount + getId()) % 20 != 0) return;
            safe = threat(0.8) != null || level().isDarkOutside() ? 0 : safe + 20;
            if (safe >= SAFE_TICKS) {
                setHidden(false);
                alarmedUntil = 0;
                playSound(SoundEvents.ROOTED_DIRT_STEP, 0.5F, 1.3F);
            }
        }
    }
}
