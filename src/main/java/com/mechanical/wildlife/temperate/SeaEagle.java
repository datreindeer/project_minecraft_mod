package com.mechanical.wildlife.temperate;

import java.util.Comparator;

import com.mechanical.wildlife.forestbirds.SoaringBird;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * White-tailed sea eagle: the great eagle of coasts, big rivers and lakes, broad-winged with a pale head and a white
 * tail. Soars in wide circles (SoaringBird) and watches the water: a fish swimming near the surface ({@link #SPOT_RANGE},
 * at most {@link #MAX_DEPTH} under) it swoops on and snatches out in its talons (the fish is gone, with a splash), then
 * rises again. No carcass of its own: feathers, Eagle Feathers and Talons (loot table).
 */
public class SeaEagle extends SoaringBird {
    public static final Kind KIND = new Kind("sea_eagle", 0.48, 14, 26, 900, 1800, 300, 900, false, false, true, 1.4,
            () -> SoundEvents.FOX_SCREECH, 1.5F);
    static final double SPOT_RANGE = 32, STRIKE = 1.6, MAX_DEPTH = 2.5;
    /** Ticks after a catch before it hunts again. */
    static final int FED_TICKS = 1200;
    private AbstractFish prey;
    private int fed;

    public SeaEagle(EntityType<? extends SeaEagle> type, Level level) {
        super(type, level, KIND);
    }

    /** A fish in the water within reach of a dive: near the surface, below the eagle. */
    boolean catchable(AbstractFish f) {
        if (!f.isAlive() || !f.isInWater()) return false;
        int surface = level().getHeight(Heightmap.Types.MOTION_BLOCKING, f.getBlockX(), f.getBlockZ());
        return surface - f.getY() <= MAX_DEPTH && f.getY() < getY();
    }

    @Override
    protected Vec3 cruise() {
        if (fed > 0) {
            fed--;
            return super.cruise();
        }
        if (prey != null && (!catchable(prey) || distanceToSqr(prey) > SPOT_RANGE * SPOT_RANGE * 2.25)) prey = null;
        if (prey == null && (tickCount + getId()) % 40 == 0) {
            prey = level().getEntitiesOfClass(AbstractFish.class, getBoundingBox().inflate(SPOT_RANGE, 30, SPOT_RANGE), this::catchable)
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        if (prey == null) return super.cruise();
        Vec3 at = prey.position();
        if (distanceToSqr(at) < STRIKE * STRIKE) {
            snatch((ServerLevel) level(), prey);
            prey = null;
            return new Vec3(getDeltaMovement().x, 0.5, getDeltaMovement().z);
        }
        double dist = Math.sqrt(horizontalDistSqr(at));
        Vec3 aim = dist > 6 ? new Vec3(at.x, Math.max(at.y + 4, getY() - 0.3), at.z) : at;
        return seek(aim, speed() * (dist > 6 ? 1.2 : 1.7));
    }

    /** Takes the fish out of the water: gone with a splash. */
    void snatch(ServerLevel level, AbstractFish fish) {
        level.sendParticles(ParticleTypes.SPLASH, fish.getX(), fish.getY() + 0.5, fish.getZ(), 20, 0.4, 0.1, 0.4, 0.1);
        playSound(SoundEvents.GENERIC_SPLASH, 1.0F, 0.9F);
        fish.discard();
        fed = FED_TICKS;
    }

    public boolean isFed() {
        return fed > 0;
    }
}
