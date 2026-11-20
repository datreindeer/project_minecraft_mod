package com.mechanical.wildlife.waterfowl;

import java.util.Comparator;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Grey heron: a lone fisher of river banks, swamps and lake shores. It wades into the shallows and stands there,
 * motionless for minutes, leaning out over the water; when a fish comes within reach it spears it with a lightning thrust
 * of the neck (the fish is gone). Now and then it takes something too small to see. Off the water it walks the bank
 * slowly, looking for a good spot; it flies with slow, deep beats, neck folded back and legs trailing, and may perch in
 * trees. Wary: it flies off croaking when a player comes near.
 */
public class GreyHeron extends WadingBird {
    public static final Kind KIND = new Kind("grey_heron", 0.35, 6, 16, 200, 500, 1400, 3000, false, true, true, 1.0,
            () -> SoundEvents.FOX_SCREECH, 0.55F);
    private static final EntityDataAccessor<Integer> STRIKE = SynchedEntityData.defineId(GreyHeron.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HUNTING = SynchedEntityData.defineId(GreyHeron.class, EntityDataSerializers.BOOLEAN);
    static final int STRIKE_TICKS = 10;
    /** Reach of its thrust (blocks, horizontally), and how far it looks for fish. */
    static final double REACH = 2.2, WATCH = 5;
    private AbstractFish quarry;
    private int catches;

    public GreyHeron(EntityType<? extends GreyHeron> type, Level level) {
        super(type, level, KIND);
    }

    public boolean isHunting() {
        return entityData.get(HUNTING);
    }

    /** Fish speared since it spawned (not saved; for the self-check and curiosity). */
    public int catches() {
        return catches;
    }

    /** 0..1..0 over the thrust. */
    public float strikeAmount(float partialTick) {
        int t = entityData.get(STRIKE);
        if (t <= 0) return 0.0F;
        float age = STRIKE_TICKS - t + partialTick;
        return age < 3 ? age / 3.0F : Math.max(0.0F, (STRIKE_TICKS - age) / (STRIKE_TICKS - 3.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STRIKE, 0);
        builder.define(HUNTING, false);
    }

    @Override
    public void aiStep() {
        int t = entityData.get(STRIKE);
        if (t > 0 && !level().isClientSide()) entityData.set(STRIKE, t - 1);
        super.aiStep();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        boolean hunting = !isFlying() && !isDozing() && isInWater() && !level.isDarkOutside();
        if (hunting != isHunting()) entityData.set(HUNTING, hunting);
        if (!hunting) {
            quarry = null;
            // on the bank by day: now and then walks down into the shallows nearby
            if (!isFlying() && !isDozing() && onGround() && !level.isDarkOutside() && (tickCount + getId()) % 100 == 0 && random.nextInt(3) == 0) wadeIn();
            return;
        }
        getNavigation().stop(); // stands stock still while it fishes
        if (entityData.get(STRIKE) == STRIKE_TICKS - 3) spear(level); // the bill reaches the water
        if (entityData.get(STRIKE) > 0) return;
        if ((tickCount + getId()) % 10 == 0) {
            if (quarry == null || !quarry.isAlive() || quarry.distanceToSqr(this) > WATCH * WATCH) {
                quarry = level.getEntitiesOfClass(AbstractFish.class, getBoundingBox().inflate(WATCH, 2, WATCH), f -> f.isAlive() && level.getFluidState(f.blockPosition()).is(net.minecraft.tags.FluidTags.WATER))
                        .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            }
            if (quarry != null) {
                getLookControl().setLookAt(quarry, 30.0F, 90.0F);
                double dx = quarry.getX() - getX(), dz = quarry.getZ() - getZ();
                if (dx * dx + dz * dz < REACH * REACH && random.nextInt(3) == 0) strike();
            } else if (random.nextInt(60) == 0) {
                strike(); // something small in the shallows (a minnow, a frog)
            }
        }
        // a slow step or two along the shallows now and then
        if ((tickCount + getId()) % 400 == 0 && quarry == null && random.nextInt(2) == 0) wadeIn();
    }

    private void strike() {
        entityData.set(STRIKE, STRIKE_TICKS);
    }

    private void spear(ServerLevel level) {
        Vec3 at = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.2));
        level.sendParticles(ParticleTypes.SPLASH, at.x, getY() + 0.9, at.z, 10, 0.2, 0.05, 0.2, 0.1);
        playSound(SoundEvents.GENERIC_SPLASH, 0.35F, 1.6F);
        if (quarry != null && quarry.isAlive() && quarry.distanceToSqr(this) < (REACH + 0.8) * (REACH + 0.8)) {
            quarry.discard(); // swallowed whole
            quarry = null;
            catches++;
            heal(1.0F);
        }
    }

    /** Walks to a shallow spot within 8 blocks (or stays put if there is none). */
    private void wadeIn() {
        for (int i = 0; i < 10; i++) {
            Vec3 spot = shallowAt(getBlockX() + random.nextInt(17) - 8, getBlockZ() + random.nextInt(17) - 8);
            if (spot != null) {
                getNavigation().moveTo(spot.x, spot.y, spot.z, 0.7);
                return;
            }
        }
    }
}
