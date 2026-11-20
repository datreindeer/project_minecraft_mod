package com.mechanical.wildlife.river;

import com.mechanical.NightDangers;
import com.mechanical.Sanity;
import com.mechanical.Smoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Mosquito Swarm: a hovering cloud of gnats over swamp water on warm evenings and nights (Willow Fen, swamps). It finds
 * the nearest player and dances round their head: every few seconds a bite (half a heart now and then) and the itching
 * wears on the nerves (sanity drains a little, which eats into daylight's recovery). Smoke and fire drive it off: a
 * burning campfire or fire pit within {@link #FIRE_RANGE} of it or its victim, or a smoky room, and it scatters; a hit
 * or a flame kills it. No carcass, no drops. It breaks up at sunrise.
 */
public class MosquitoSwarm extends AmbientCreature {
    static final double CHASE_RANGE = 12, FIRE_RANGE = 6;
    static final int BITE_TICKS = 60;
    private Vec3 wander;
    private int smoked;

    public MosquitoSwarm(EntityType<? extends MosquitoSwarm> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0).add(Attributes.FLYING_SPEED, 0.4);
    }

    /** Spawn rule: warm biome, dusk or night, open air over or by water. Ambient spawning never runs at world generation. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos, EntitySpawnReason reason, RandomSource random) {
        if (!(level instanceof net.minecraft.world.level.ServerLevelAccessor server) || !level.getBlockState(pos).isAir()) return false;
        if (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND || reason == EntitySpawnReason.SPAWNER) return true;
        long time = server.getLevel().getOverworldClockTime() % 24000;
        boolean dusk = time >= 11500 && time <= 14500, night = time > 14500 && time < 22500;
        if (!dusk && !(night && random.nextInt(3) == 0)) return false;
        return level.getBiome(pos).value().getBaseTemperature() >= 0.5F && level.canSeeSky(pos) && RiverFauna.nearWater(level, pos.below(), 4, false);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(1.0, 0.8, 1.0));
        // the cloud itself: specks drifting off it now and then (the model draws the swarm)
        if (level().isClientSide() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.MYCELIUM, getX() + (random.nextDouble() - 0.5) * 0.8, getY() + random.nextDouble() * 0.7,
                    getZ() + (random.nextDouble() - 0.5) * 0.8, 0, 0, 0);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 10 == 0) {
            // daylight breaks it up; smoke and fire drive it off
            long time = level.getOverworldClockTime() % 24000;
            if (time > 23000 || time < 11000 && random.nextInt(20) == 0) {
                disperse(level);
                return;
            }
            Player victim = level.getNearestPlayer(this, CHASE_RANGE);
            if (smoky(level, blockPosition()) || victim != null && distanceTo(victim) < 3 && smoky(level, victim.blockPosition())) smoked += 10;
            else smoked = Math.max(0, smoked - 5);
            if (smoked >= 40) {
                disperse(level);
                return;
            }
        }
        Player victim = smoked > 0 ? null : level.getNearestPlayer(this, CHASE_RANGE);
        if (victim != null && (victim.isCreative() || victim.isSpectator())) victim = null;
        Vec3 goal;
        if (victim != null) {
            // circling round the head
            float a = (tickCount + getId() * 7) * 0.15F;
            goal = victim.getEyePosition().add(Mth.cos(a) * 0.6, Mth.sin(a * 1.7F) * 0.25 - 0.2, Mth.sin(a) * 0.6);
            if (tickCount % BITE_TICKS == 0 && distanceToSqr(victim) < 4) bite(level, victim);
        } else {
            if (wander == null || random.nextInt(40) == 0 || position().distanceToSqr(wander) < 1) {
                wander = position().add(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            }
            goal = wander;
        }
        // smoked: away from the fire
        if (smoked > 0) {
            BlockPos fire = NightDangers.nearestFire(level, position(), FIRE_RANGE * 2);
            if (fire != null) goal = position().add(position().subtract(Vec3.atCenterOf(fire)).normalize().scale(4)).add(0, 1, 0);
        }
        Vec3 d = goal.subtract(position());
        Vec3 m = getDeltaMovement();
        double speed = victim != null ? 0.35 : 0.15;
        setDeltaMovement(m.add((Math.signum(d.x) * speed - m.x) * 0.1, (Math.signum(d.y) * speed - m.y) * 0.1, (Math.signum(d.z) * speed - m.z) * 0.1));
        setYRot((float) (Mth.atan2(m.z, m.x) * Mth.RAD_TO_DEG) - 90.0F);
    }

    static boolean smoky(ServerLevel level, BlockPos pos) {
        return NightDangers.nearFire(level, Vec3.atCenterOf(pos), FIRE_RANGE) || Smoke.smokeAt(level, pos) > 0.1F;
    }

    private void bite(ServerLevel level, Player victim) {
        Sanity.set(victim, Sanity.of(victim) - 1.0F);
        if (random.nextInt(3) == 0) victim.hurtServer(level, damageSources().mobAttack(this), 1.0F);
    }

    private void disperse(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        discard();
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
    public boolean canBreatheUnderwater() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 30;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    public float getVoicePitch() {
        return 2.0F; // the thin whine
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }
}
