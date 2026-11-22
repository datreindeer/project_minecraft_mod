package com.mechanical.wildlife.jungle;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Saltwater Crocodile: an armoured giant of the mangrove channels. It lies still in the water with only eyes and nostrils
 * above the surface ({@link #isLurking}), and when something comes within {@link #LUNGE_RANGE} blocks of it - drinking at
 * the edge, wading, swimming - it lunges, seizes it in its jaws ({@link Grip}) and drags it under in a death roll
 * ({@link #isRolling}): 3 damage a second, under water, until someone else strikes the crocodile loose (one blow) or the
 * victim tears free (6 blows of its own); it lets go after 20 s. On land it is slow and goes back to the water. Gives the
 * Raw Crocodile Hide (fat-tanned into Crocodile Hide: armour) and meat; a tough hide that needs a tusk-grade knife.
 */
public class Crocodile extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/saltwater_crocodile.png");
    private static final EntityDataAccessor<Boolean> ROLLING = SynchedEntityData.defineId(Crocodile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LURKING = SynchedEntityData.defineId(Crocodile.class, EntityDataSerializers.BOOLEAN);
    static final double LUNGE_RANGE = 5.0, BITE_RANGE = 1.9;
    private static final int WATER_RANGE = 16, COOLDOWN = 400;
    private final Grip grip = new Grip(this, 3.0F, 6, 400);
    private int cooldown;

    public Crocodile(EntityType<? extends Crocodile> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ARMOR, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLLING, false);
        builder.define(LURKING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // it keeps its own depth: lurking at the surface, sinking with its prey
        goalSelector.removeAllGoals(g -> g instanceof FloatGoal || g.getClass().getSimpleName().equals("FollowHerdGoal"));
        goalSelector.addGoal(2, new BackToWaterGoal());
    }

    /** Death-rolling a victim (synced for the model). */
    public boolean isRolling() {
        return entityData.get(ROLLING);
    }

    /** Lying still in the water, eyes above the surface (synced for the model). */
    public boolean isLurking() {
        return entityData.get(LURKING);
    }

    public LivingEntity victim() {
        return grip.victim();
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 0.0;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; // holds its breath for an hour
    }

    boolean isPrey(LivingEntity e) {
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e instanceof WildAnimal && !(e instanceof Crocodile) && !(e instanceof Predator) && e.isAlive()
                && e.getBbWidth() < 2.0F;
    }

    /** Seizes {@code target} in its jaws and starts the roll. */
    public void seize(LivingEntity target) {
        grip.seize(target);
        entityData.set(ROLLING, true);
        entityData.set(LURKING, false);
        getNavigation().stop();
        playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 0.6F);
    }

    private void letGo(Vec3 from) {
        grip.release();
        entityData.set(ROLLING, false);
        cooldown = COOLDOWN;
        if (from != null) AnimalFear.panic(this, from);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (cooldown > 0) cooldown--;
        if (isRolling()) {
            if (!grip.tick(level)) {
                letGo(null);
                return;
            }
            // the victim is held at the jaws, and goes down with the crocodile
            Vec3 jaws = position().add(Vec3.directionFromRotation(0, getYRot()).scale(getBbWidth() * 0.7));
            grip.moveAnchor(new Vec3(jaws.x, getY(), jaws.z));
            if (isInWater()) setDeltaMovement(getDeltaMovement().multiply(0.6, 1, 0.6).add(0, -0.04, 0));
            return;
        }
        boolean still = isInWater() && getTarget() == null && !AnimalFear.isPanicking(this) && getNavigation().isDone();
        if (still != isLurking()) entityData.set(LURKING, still);
        if (isInWater()) {
            // float with only the eyes out
            setDeltaMovement(getDeltaMovement().add(0, isEyeInFluid(FluidTags.WATER) ? 0.025 : -0.01, 0));
            if (still) setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
        }
        if (cooldown == 0 && !isNoAi() && (tickCount + getId()) % 4 == 0) hunt(level);
    }

    /** From the water (or its edge): lunges at prey within LUNGE_RANGE, seizes what is within BITE_RANGE. */
    void hunt(ServerLevel level) {
        if (!isInWater() && !nearWater(2)) return;
        AABB box = getBoundingBox().inflate(LUNGE_RANGE, 2, LUNGE_RANGE);
        LivingEntity best = null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::isPrey)) {
            if (best == null || distanceToSqr(e) < distanceToSqr(best)) best = e;
        }
        if (best == null) return;
        if (distanceTo(best) <= BITE_RANGE + best.getBbWidth() / 2) {
            seize(best);
        } else if (onGround() || isInWater()) {
            Vec3 d = best.position().subtract(position()).multiply(1, 0, 1).normalize();
            setDeltaMovement(d.x * 0.9, 0.25, d.z * 0.9); // the explosive lunge out of the water
            setYRot((float) (net.minecraft.util.Mth.atan2(d.z, d.x) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F);
            yBodyRot = getYRot();
            playSound(SoundEvents.GENERIC_SPLASH, 1.0F, 0.7F);
        }
    }

    private boolean nearWater(int r) {
        BlockPos me = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -1, -r), me.offset(r, 0, r))) {
            if (level().getFluidState(p).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRolling() && grip.onHolderHurt(source)) letGo(source.getEntity() == null ? null : source.getEntity().position());
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isLurking() ? null : SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.TURTLE_SHAMBLE, 0.4F, 0.6F);
    }

    /** On land and not fleeing: back to water deep enough to lie in (2 blocks) within WATER_RANGE. */
    class BackToWaterGoal extends Goal {
        private BlockPos water;

        BackToWaterGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isInWater() || isRolling() || getTarget() != null || (tickCount + getId()) % 40 != 0 || random.nextInt(3) != 0) return false;
            BlockPos me = blockPosition();
            water = null;
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-WATER_RANGE, -4, -WATER_RANGE), me.offset(WATER_RANGE, 2, WATER_RANGE))) {
                if (level().getFluidState(p).is(FluidTags.WATER) && level().getFluidState(p.below()).is(FluidTags.WATER)
                        && (water == null || p.distSqr(me) < water.distSqr(me))) water = p.immutable();
            }
            return water != null && getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return !isInWater() && !getNavigation().isDone() && !isRolling();
        }
    }
}
