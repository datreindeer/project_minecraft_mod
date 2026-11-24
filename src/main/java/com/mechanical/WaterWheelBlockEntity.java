package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The turning Water Wheel (see {@link WaterPower}). Every second it measures the water round its paddles
 * ({@link #measure}), turns at {@link WaterPower#DEG_PER_POWER} degrees a tick per point of power, and hands that turn
 * down its axle. Drawn by client/WaterPowerRenderers.
 */
public class WaterWheelBlockEntity extends RotorBlockEntity {
    /** Offsets (across, up) in the wheel's plane: the 24 cells round the hub its spokes and paddles sweep. */
    public static final List<int[]> SWEPT = new ArrayList<>();
    static {
        for (int a = -2; a <= 2; a++) for (int b = -2; b <= 2; b++) if (a != 0 || b != 0) SWEPT.add(new int[] {a, b});
    }
    /** A cell counts when the water's flow is at least this much round the wheel (not straight at or away from the axle). */
    static final double TANGENTIAL = 0.3;

    /** Server: the first thing standing in the wheel's way at the last measure, or null. */
    @Nullable BlockPos blocked;

    public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(WaterPower.WATER_WHEEL_ENTITY.get(), pos, state);
    }

    public Direction.Axis axis() {
        return getBlockState().getValue(WaterWheelBlock.AXIS);
    }

    /** The world position of a cell in the wheel's plane. */
    public static BlockPos cell(BlockPos hub, Direction.Axis axis, int across, int up) {
        return axis == Direction.Axis.X ? hub.offset(0, up, across) : hub.offset(across, up, 0);
    }

    /**
     * Signed power from the water round the paddles (positive turns the wheel about its +axis), or 0 with {@link #blocked}
     * set when something solid stands in the swept cells. The 4 inner cells (next to the hub) hold only spokes and don't count; the other 20 do.
     */
    public float measure(Level level) {
        blocked = null;
        BlockPos hub = getBlockPos();
        Direction.Axis axis = axis();
        Vec3 a = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).getUnitVec3i());
        float sum = 0;
        for (int[] c : SWEPT) {
            BlockPos p = cell(hub, axis, c[0], c[1]);
            BlockState state = level.getBlockState(p);
            if (!state.canBeReplaced()) {
                blocked = p;
                return 0;
            }
            int d2 = c[0] * c[0] + c[1] * c[1];
            if (d2 < 2) continue; // inner cells: spokes only
            FluidState fluid = level.getFluidState(p);
            if (!fluid.is(FluidTags.WATER)) continue;
            boolean falling = fluid.getValue(FlowingFluid.FALLING);
            Vec3 flow = fluid.getFlow(level, p);
            Vec3 f = falling ? new Vec3(0, -1, 0) : new Vec3(flow.x, 0, flow.z);
            if (f.lengthSqr() < 1.0E-4) continue; // still water
            Vec3 r = Vec3.atLowerCornerOf(p.subtract(hub));
            double t = r.cross(f).dot(a) / (r.length() * f.length());
            if (Math.abs(t) >= TANGENTIAL) sum += Math.signum(t) * (falling ? 2 : 1);
        }
        return sum;
    }

    /**
     * Every second: the wheel that comes first (by position) on its axle line drives the line. It adds up every wheel's
     * water (signed by which way each turns on the line), caps it, turns at that speed and hands the turn to every part;
     * the machines on the line share the power ({@link WaterPower#share}).
     */
    static void serverTick(ServerLevel level, BlockPos pos, WaterWheelBlockEntity wheel) {
        long now = level.getGameTime();
        if (now % 20 != 3) return;
        wheel.body(level);
        List<WaterPower.Part> line = WaterPower.line(level, wheel);
        for (WaterPower.Part p : line)
            if (p.rotor() instanceof WaterWheelBlockEntity other && other.getBlockPos().asLong() < pos.asLong()) return; // that wheel drives the line
        float sum = 0;
        boolean jammed = false, upright = false;
        int turning = 0;
        for (WaterPower.Part p : line) {
            if (p.rotor() instanceof WaterWheelBlockEntity w) {
                float own = w.measure(level);
                sum += p.sign() * own;
                jammed |= w.blocked != null;
                if (Math.abs(own) >= 1) turning++;
            } else upright |= p.rotor().getBlockState().is(WaterPower.WOODEN_SHAFT.get()) && p.rotor().getBlockState().getValue(ShaftBlock.AXIS) == Direction.Axis.Y;
        }
        int power = jammed ? 0 : Math.min(WaterPower.MAX_POWER, Math.round(Math.abs(sum)));
        float speed = Math.signum(sum) * power * WaterPower.DEG_PER_POWER;
        if (speed != wheel.speed || power != wheel.power) {
            wheel.base = wheel.angle(now);
            wheel.t0 = now;
            wheel.speed = speed;
            wheel.power = power;
            wheel.sync();
        }
        int machines = (int) line.stream().filter(p -> p.rotor().getBlockState().getBlock() instanceof WaterPower.Driven).count();
        for (WaterPower.Part p : line)
            if (p.rotor() != wheel) p.rotor().feed(wheel.speed * p.sign(), wheel.base * p.sign(), wheel.t0,
                    p.rotor().getBlockState().getBlock() instanceof WaterPower.Driven ? WaterPower.share(wheel.power, machines) : wheel.power);
        if (power > 0) Smelting.award(level, pos, WaterPower.WHEEL_ADVANCEMENT, 12);
        if (power >= WaterPower.MAX_POWER) Smelting.award(level, pos, WaterPower.FULL_POWER_ADVANCEMENT, 12);
        if (power > 0 && turning >= 2) Smelting.award(level, pos, WaterPower.TWIN_WHEELS_ADVANCEMENT, 12);
        if (power > 0 && upright) Smelting.award(level, pos, WaterPower.POWER_CLIMBS_ADVANCEMENT, 12);
    }

    /** Puts back the wheel's solid body ({@link WaterWheelHitbox}) when it is missing, e.g. after the chunk loaded. */
    void body(ServerLevel level) {
        List<WaterWheelHitbox> boxes = level.getEntities(WaterPower.WHEEL_HITBOX.get(), new AABB(getBlockPos()), e -> e.blockPosition().equals(getBlockPos()));
        if (boxes.size() == 2) return;
        boxes.forEach(Entity::discard);
        for (boolean tall : new boolean[] {false, true}) level.addFreshEntity(WaterWheelHitbox.at(level, getBlockPos(), axis(), tall));
    }
}
