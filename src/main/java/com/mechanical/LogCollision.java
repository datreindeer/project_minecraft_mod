package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Collision with felled logs ({@link FelledLogEntity}) for every entity that moves (mixin on Entity#collide). Logs on the
 * block grid collide through vanilla (their block boxes, EntityGetterMixin). A log turned off the grid can't be told to
 * vanilla (it only knows upright boxes), so movement is clipped here instead: the log as turned boxes (one per straight
 * log, else one per block), the entity as an upright cylinder of half its width. It stops against a log's side and
 * slides along it, lands on and stands on its top, bumps its head on its underside, and steps up onto it if it steps
 * that high (horses). A mob walking into any log jumps onto it, so animals and monsters cross felled trees.
 */
public final class LogCollision {
    private static final double EPS = 1.0E-7;

    /** A turned box: middle and half sizes on the log's own grid (x across, z along), and its bottom and top in the world. */
    private record Box(FelledLogEntity log, double x, double z, double halfX, double halfZ, double bottom, double top) {}

    /** Entity#collide's result {@code move} (vanilla's, for the movement {@code wanted}), also clipped against turned logs. */
    public static Vec3 collide(Entity entity, Vec3 wanted, Vec3 move) {
        if (entity instanceof FelledLogEntity || entity.noPhysics || wanted.lengthSqr() < EPS) return move;
        AABB box = entity.getBoundingBox();
        List<FelledLogEntity> logs = entity.level().getEntitiesOfClass(FelledLogEntity.class, box.expandTowards(wanted).inflate(1.0),
                l -> l.canBeCollidedWith(entity));
        if (logs.isEmpty()) return move;
        List<Box> boxes = new ArrayList<>();
        for (FelledLogEntity log : logs) if (!log.onGrid()) addBoxes(log, boxes);
        Vec3 out = boxes.isEmpty() ? move : clip(entity, box, move, boxes);
        if (entity instanceof Mob mob) jumpOver(mob, box, wanted, out, logs);
        return out;
    }

    private static void addBoxes(FelledLogEntity log, List<Box> out) {
        int n = log.size();
        if (log.straight() && log.lifterCount() == 0 && log.tilt() == 0) { // one long box: no seams to snag on along its sides
            Vec3 a = log.localCell(0), b = log.localCell(n - 1);
            out.add(new Box(log, a.x, (a.z + b.z) / 2, 0.5, n / 2.0, log.getY(), log.getY() + 1));
            return;
        }
        for (int i = 0; i < n; i++) {
            Vec3 c = log.localCell(i);
            double bottom = log.getY() + c.y + log.liftOffset(i);
            out.add(new Box(log, c.x, c.z, 0.5, 0.5, bottom, bottom + 1));
        }
    }

    /** {@code move} clipped against the turned boxes: up and down first, then sideways at the new height. */
    static Vec3 clip(Entity entity, AABB box, Vec3 move, List<Box> boxes) {
        double r = entity.getBbWidth() / 2, h = box.getYsize(), y0 = box.minY;
        double cx = (box.minX + box.maxX) / 2, cz = (box.minZ + box.maxZ) / 2;
        double mx = move.x, my = move.y, mz = move.z;
        for (Box b : boxes) {
            if (penetration(b, cx, cz, r) <= EPS && penetration(b, cx + mx, cz + mz, r) <= EPS) continue; // not over or under it
            if (my < 0 && y0 >= b.top - 1.0E-3 && y0 + my < b.top) my = b.top - y0; // lands on it
            else if (my > 0 && y0 + h <= b.bottom + 1.0E-3 && y0 + h + my > b.bottom) my = b.bottom - (y0 + h); // head against it
        }
        // a low enough side is stepped up onto (as vanilla steps up blocks), from the ground only
        float step = entity.maxUpStep();
        if (entity.onGround() && my <= 0 && step > 0) {
            for (Box b : boxes) {
                double rise = b.top - y0;
                if (rise > EPS && rise <= step + EPS && penetration(b, cx + mx, cz + mz, r) > EPS) my = Math.max(my, rise);
            }
        }
        double ny0 = y0 + my, ny1 = ny0 + h;
        for (int pass = 0; pass < 3; pass++) {
            boolean pushed = false;
            for (Box b : boxes) {
                if (ny0 >= b.top - 1.0E-3 || ny1 <= b.bottom + 1.0E-3) continue; // above or below it
                double before = Math.max(0, penetration(b, cx, cz, r)), after = penetration(b, cx + mx, cz + mz, r);
                if (after <= before + EPS) continue; // not deeper in (already overlapping: free to move out)
                Vec3 l = b.log.toLocal(new Vec3(cx + mx, 0, cz + mz));
                double[] nrm = normal(b, l.x, l.z);
                Vec3 w = b.log.toWorld(new Vec3(l.x + nrm[0] * (after - before), 0, l.z + nrm[1] * (after - before)));
                mx = w.x - cx;
                mz = w.z - cz;
                pushed = true;
            }
            if (!pushed) break;
        }
        return new Vec3(mx, my, mz);
    }

    /** How far a cylinder of radius {@code r} at world (x, z) reaches into the box's footprint (negative: that far apart). */
    private static double penetration(Box b, double x, double z, double r) {
        Vec3 l = b.log.toLocal(new Vec3(x, 0, z));
        double dx = Math.abs(l.x - b.x) - b.halfX, dz = Math.abs(l.z - b.z) - b.halfZ;
        double outside = Math.sqrt(Math.max(dx, 0) * Math.max(dx, 0) + Math.max(dz, 0) * Math.max(dz, 0));
        return r - (outside + Math.min(Math.max(dx, dz), 0));
    }

    /** The way out of the box's footprint from local (x, z): from its nearest point, or its nearest side if inside. */
    private static double[] normal(Box b, double x, double z) {
        double ox = x - b.x, oz = z - b.z;
        double dx = Math.abs(ox) - b.halfX, dz = Math.abs(oz) - b.halfZ;
        if (dx > 0 || dz > 0) {
            double nx = Math.signum(ox) * Math.max(dx, 0), nz = Math.signum(oz) * Math.max(dz, 0), len = Math.sqrt(nx * nx + nz * nz);
            return new double[] {nx / len, nz / len};
        }
        return dx > dz ? new double[] {ox >= 0 ? 1 : -1, 0} : new double[] {0, oz >= 0 ? 1 : -1};
    }

    /** A mob on the ground held up by a log ahead that it can't step onto jumps (a log's top is 1 block up: a jump clears it). */
    private static void jumpOver(Mob mob, AABB box, Vec3 wanted, Vec3 out, List<FelledLogEntity> logs) {
        if (!mob.onGround() || mob.isInWater()) return;
        double want = wanted.horizontalDistanceSqr(), got = out.horizontalDistanceSqr();
        if (want < 1.0E-6 || got > want * 0.81) return; // not held up
        AABB ahead = box.move(wanted.x * 3, 0, wanted.z * 3).inflate(0.05);
        for (FelledLogEntity log : logs) {
            double top = log.getY() + 1 - mob.getY();
            if (top > mob.maxUpStep() && top <= 1.3 && log.getBoundingBox().intersects(ahead)) {
                mob.getJumpControl().jump();
                return;
            }
        }
    }

    private LogCollision() {}
}
