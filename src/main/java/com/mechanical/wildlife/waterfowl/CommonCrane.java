package com.mechanical.wildlife.waterfowl;

import java.util.List;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Common crane: tall grey bird of fens, bogs and wet meadows, in flocks that forage over the ground (grazing) and wade
 * the shallows. Very wary. They travel high in long V-shaped skeins, trumpeting, and keep moving on: now and then the
 * flock leader takes them a long way off ({@link #MIGRATE}), all heading the same way. On the ground in daylight two
 * cranes sometimes break into their dance: wings half spread, bowing, leaping into the air, calling.
 */
public class CommonCrane extends WadingBird {
    public static final Kind KIND = new Kind("common_crane", 0.5, 16, 30, 700, 1400, 900, 2000, true, true, false, 1.3,
            () -> SoundEvents.GOAT_SCREAMING_AMBIENT, 1.35F);
    private static final EntityDataAccessor<Integer> DANCE = SynchedEntityData.defineId(CommonCrane.class, EntityDataSerializers.INT);
    static final int DANCE_TICKS = 120;
    /** Chance (1 in n) that a leader taking off leads the flock away to a new home, and how far (blocks). */
    static final int MIGRATE = 3, MIGRATE_MIN = 64, MIGRATE_MAX = 128;
    /** Heading of this flock's wandering (radians), so it keeps going one way rather than back and forth. */
    private final float heading;

    public CommonCrane(EntityType<? extends CommonCrane> type, Level level) {
        super(type, level, KIND);
        heading = random.nextFloat() * Mth.TWO_PI;
    }

    public boolean isDancing() {
        return entityData.get(DANCE) > 0;
    }

    /** 0..1, eased in and out over 10 ticks. */
    public float danceAmount(float partialTick) {
        int t = entityData.get(DANCE);
        if (t <= 0) return 0.0F;
        return Math.clamp((Math.min(t, DANCE_TICKS - t) + partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DANCE, 0);
    }

    @Override
    public void aiStep() {
        int t = entityData.get(DANCE);
        if (t > 0 && !level().isClientSide()) entityData.set(DANCE, isFlying() ? 0 : t - 1);
        super.aiStep();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (isDancing()) {
            getNavigation().stop();
            if (onGround() && random.nextInt(18) == 0) setDeltaMovement(getDeltaMovement().add(0, 0.42, 0)); // a leap
            if (random.nextInt(50) == 0) playAmbientSound();
            return;
        }
        if (isFlying() || isDozing() || !onGround() || level.isDarkOutside() || (tickCount + getId()) % 100 != 0 || random.nextInt(10) != 0) return;
        List<CommonCrane> partners = level.getEntitiesOfClass(CommonCrane.class, getBoundingBox().inflate(8, 2, 8),
                c -> c != this && c.onGround() && !c.isFlying() && !c.isDozing() && !c.isDancing());
        if (partners.isEmpty()) return;
        startDance(partners.get(0));
    }

    /** This crane and {@code partner} dance, facing each other. */
    public void startDance(CommonCrane partner) {
        for (CommonCrane c : new CommonCrane[] {this, partner}) {
            CommonCrane other = c == this ? partner : this;
            c.entityData.set(DANCE, DANCE_TICKS);
            c.getNavigation().stop();
            c.getLookControl().setLookAt(other);
            float yaw = (float) (Mth.atan2(other.getZ() - c.getZ(), other.getX() - c.getX()) * Mth.RAD_TO_DEG) - 90.0F;
            c.setYRot(yaw);
            c.yBodyRot = yaw;
        }
        playAmbientSound();
    }

    @Override
    protected void takeOff(boolean frightened, Vec3 from) {
        boolean wasFlying = isFlying();
        super.takeOff(frightened, from);
        // the leader of a calm flock sometimes leads it off to new grounds, the whole flock moving its home together
        if (!wasFlying && !frightened && leader == this && home != null && random.nextInt(MIGRATE) == 0) {
            double d = MIGRATE_MIN + random.nextInt(MIGRATE_MAX - MIGRATE_MIN + 1);
            double a = heading + (random.nextDouble() - 0.5) * 0.8;
            home = home.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            target = null;
            for (FlockBird b : flock) if (b instanceof CommonCrane c) c.home = home;
        }
    }
}
