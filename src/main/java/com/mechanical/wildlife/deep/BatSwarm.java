package com.mechanical.wildlife.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Bat Swarm: at dusk a cloud of bats pours out of a cave mouth ({@link DeepFauna#caveMouth}). For the first
 * {@link #LINGER} ticks it whirls round the mouth, rising out of it with a din of squeaks, so whoever sees it knows there
 * is a cave there; then it streams off over the land and is gone after {@link #LIFE} ticks. Harmless. Struck down, it
 * leaves a few Bat Wings and some Guano (loot table); no carcass. The bats aren't luminous: in the dark it's just a
 * flicker of shapes.
 */
public class BatSwarm extends AmbientCreature {
    static final int LINGER = 300, LIFE = 900;
    private BlockPos home;
    private float leaveYaw;

    public BatSwarm(EntityType<? extends BatSwarm> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true; // a cloud: it streams out through the mouth whatever the rock's shape
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.FLYING_SPEED, 0.5);
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
    }

    /** The cave mouth it came out of (where it first appeared if none was set). */
    public BlockPos home() {
        if (home == null) home = blockPosition();
        return home;
    }

    /** Still whirling over its cave mouth. */
    public boolean lingering() {
        return tickCount < LINGER;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount > LIFE) {
            discard();
            return;
        }
        Vec3 goal;
        if (lingering()) {
            // whirling round the mouth, slowly rising out of it
            float a = (tickCount + getId() * 13) * 0.12F;
            double r = 1.5 + Mth.sin(tickCount * 0.05F), rise = Math.min(4.0, tickCount / 60.0);
            goal = Vec3.atCenterOf(home()).add(Mth.cos(a) * r, rise + Mth.sin(a * 1.3F) * 0.5, Mth.sin(a) * r);
            if (tickCount == 1) leaveYaw = random.nextFloat() * 360;
        } else {
            // streaming away over the land and up
            goal = position().add(Vec3.directionFromRotation(-15, leaveYaw).scale(4));
        }
        Vec3 d = goal.subtract(position());
        Vec3 m = getDeltaMovement();
        double speed = lingering() ? 0.3 : 0.45;
        setDeltaMovement(m.add((Math.signum(d.x) * Math.min(speed, Math.abs(d.x)) - m.x) * 0.2,
                (Math.signum(d.y) * Math.min(speed, Math.abs(d.y)) - m.y) * 0.2, (Math.signum(d.z) * Math.min(speed, Math.abs(d.z)) - m.z) * 0.2));
        setYRot((float) (Mth.atan2(m.z, m.x) * Mth.RAD_TO_DEG) - 90.0F);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(1.0, 0.9, 1.0));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    protected void pushEntities() {}

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {}

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 8; // a din of squeaks
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }
}
