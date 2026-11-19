package com.mechanical.wildlife;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Herring gull: wheels in wide circles over the water (each gull of a group round the same spot, at its own radius and
 * height), crying, and dives at the surface where it sees fish. Rests on beaches or floats on the water. Scared of
 * players like every bird; one arrow brings it down.
 */
public class Seagull extends FlockBird {
    private Vec3 circleCenter;
    private double angle;
    private Vec3 dive;
    private int diveCooldown = 100;

    public Seagull(EntityType<? extends Seagull> type, Level level) {
        super(type, level, SEAGULL);
    }

    private double radius() {
        return 6 + getId() % 7;
    }

    @Override
    protected Vec3 pickTarget(Vec3 from) {
        if (from != null || circleCenter == null || random.nextInt(4) == 0) circleCenter = findWater(from);
        angle = Math.atan2(getZ() - circleCenter.z, getX() - circleCenter.x);
        return circleCenter;
    }

    /** Open water near home (away from a threat if scared); home itself if there is none. */
    private Vec3 findWater(Vec3 from) {
        Vec3 base = from != null ? position().add(position().subtract(from).multiply(1, 0, 1).normalize().scale(24)) : home;
        for (int i = 0; i < 16; i++) {
            int x = Mth.floor(base.x) + random.nextInt(33) - 16, z = Mth.floor(base.z) + random.nextInt(33) - 16;
            BlockPos col = BlockPos.containing(x, getY(), z);
            if (!level().hasChunkAt(col)) continue;
            BlockPos top = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);
            if (level().getFluidState(top.below()).is(FluidTags.WATER)) return new Vec3(x + 0.5, top.getY(), z + 0.5);
        }
        return base;
    }

    @Override
    protected Vec3 follow(FlockBird lead) {
        if (lead instanceof Seagull gull && gull.circleCenter != null) circleCenter = gull.circleCenter;
        return cruise(); // gulls each wheel round the group's spot instead of trailing the leader
    }

    @Override
    protected Vec3 cruise() {
        if (circleCenter == null) pickTarget(null);
        if (dive != null) return diveTick();
        if (--diveCooldown <= 0 && random.nextInt(30) == 0) dive = findFish();
        double r = radius();
        angle += speed() / r * (getId() % 2 == 0 ? 1 : 0.9);
        double alt = kind.minAlt() + getId() % (kind.maxAlt() - kind.minAlt() + 1);
        Vec3 point = new Vec3(circleCenter.x + Math.cos(angle) * r, surfaceY(circleCenter.x, circleCenter.z) + alt, circleCenter.z + Math.sin(angle) * r);
        return keepAltitude(seek(point, speed()), 3);
    }

    /** A fish under the water within sight below; aims at the surface above it. */
    private Vec3 findFish() {
        diveCooldown = 40;
        return level().getEntitiesOfClass(AbstractFish.class, getBoundingBox().inflate(12, 18, 12), f -> f.isInWater() && f.getY() < getY())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr))
                .map(f -> new Vec3(f.getX(), surfaceY(f.getX(), f.getZ()), f.getZ())).orElse(null);
    }

    private Vec3 diveTick() {
        if (getY() < dive.y + 0.6 || isInWater()) {
            // hits the water, then climbs away again
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SPLASH, getX(), dive.y + 0.1, getZ(), 16, 0.3, 0.05, 0.3, 0.1);
                level.sendParticles(ParticleTypes.BUBBLE, getX(), dive.y - 0.3, getZ(), 6, 0.2, 0.1, 0.2, 0.05);
            }
            playSound(SoundEvents.GENERIC_SPLASH, 0.4F, 1.5F);
            setDeltaMovement(getDeltaMovement().x, 0.45, getDeltaMovement().z);
            dive = null;
            diveCooldown = 160 + random.nextInt(240);
            return getDeltaMovement();
        }
        return seek(dive, speed() * 1.8);
    }

    @Override
    protected void land() {
        dive = null;
        super.land();
    }
}
