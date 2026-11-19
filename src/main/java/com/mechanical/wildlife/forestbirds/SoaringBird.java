package com.mechanical.wildlife.forestbirds;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A big bird that rides the air in wide circles on still wings (eagle, vulture): each bird of a group wheels round the
 * same spot at its own radius and height, drifting to a new spot now and then. {@link #focus()} pins the circle over
 * something (a carcass for the vulture). The renderer keeps the wings gliding (ForestBirdRenderer).
 */
public abstract class SoaringBird extends FlockBird {
    protected Vec3 circleCenter;
    private double angle;
    private int drift;

    protected SoaringBird(EntityType<? extends SoaringBird> type, Level level, Kind kind) {
        super(type, level, kind);
    }

    /** What to circle over right now, or null to wander around home. */
    protected Vec3 focus() {
        return null;
    }

    protected double radius() {
        return 9 + getId() % 7;
    }

    @Override
    protected Vec3 pickTarget(Vec3 from) {
        Vec3 f = from == null ? focus() : null;
        circleCenter = f != null ? f : super.pickTarget(from);
        angle = Math.atan2(getZ() - circleCenter.z, getX() - circleCenter.x);
        drift = 300 + random.nextInt(500);
        return circleCenter;
    }

    @Override
    protected Vec3 follow(FlockBird lead) {
        if (lead instanceof SoaringBird s && s.circleCenter != null) circleCenter = s.circleCenter;
        return cruise(); // each circles the group's spot instead of trailing the leader
    }

    @Override
    protected Vec3 cruise() {
        Vec3 f = focus();
        if (circleCenter == null || f != null && f.distanceToSqr(circleCenter) > 9 || f == null && --drift <= 0) pickTarget(null);
        double r = radius();
        angle += speed() / r * (getId() % 2 == 0 ? 1 : -1);
        double alt = kind().minAlt() + getId() % (kind().maxAlt() - kind().minAlt() + 1);
        Vec3 point = new Vec3(circleCenter.x + Math.cos(angle) * r, surfaceY(circleCenter.x, circleCenter.z) + alt, circleCenter.z + Math.sin(angle) * r);
        return keepAltitude(seek(point, speed()), 4);
    }
}
