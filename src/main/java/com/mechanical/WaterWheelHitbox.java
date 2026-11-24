package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The solid body of a Water Wheel (see {@link WaterWheelBlock}). The wheel's 24 cells must stay free for the water that
 * turns it, so no blocks fill them: instead two of these invisible entities, a cross (5 wide 3 high, and 3 wide 5 high,
 * as thick as the wheel), are solid to walk against and stand on, and stop blocks being placed inside the wheel. Water
 * runs through them. Not saved: the hub puts them back when it loads ({@link WaterWheelBlockEntity#serverTick}), and
 * they go when the hub is gone.
 */
public class WaterWheelHitbox extends Entity {
    /** Bit 0: the wheel's axis is Z (else X). Bit 1: the tall arm of the cross (else the wide one). */
    private static final EntityDataAccessor<Byte> SHAPE = SynchedEntityData.defineId(WaterWheelHitbox.class, EntityDataSerializers.BYTE);
    /** Half the wheel's thickness along its axle (its rims stand 2..14 px). */
    private static final double HALF_THICK = 6 / 16.0;

    public WaterWheelHitbox(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        blocksBuilding = true;
    }

    static WaterWheelHitbox at(Level level, BlockPos hub, Direction.Axis axis, boolean tall) {
        WaterWheelHitbox box = new WaterWheelHitbox(WaterPower.WHEEL_HITBOX.get(), level);
        box.entityData.set(SHAPE, (byte) ((axis == Direction.Axis.Z ? 1 : 0) | (tall ? 2 : 0)));
        box.setPos(Vec3.atCenterOf(hub));
        return box;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(SHAPE, (byte) 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (SHAPE.equals(accessor)) setBoundingBox(makeBoundingBox());
    }

    /** The position is the hub's centre. */
    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        int s = entityData.get(SHAPE);
        double across = (s & 2) != 0 ? 1.5 : 2.5, up = (s & 2) != 0 ? 2.5 : 1.5;
        return (s & 1) != 0 ? new AABB(pos.x - across, pos.y - up, pos.z - HALF_THICK, pos.x + across, pos.y + up, pos.z + HALF_THICK)
                : new AABB(pos.x - HALF_THICK, pos.y - up, pos.z - across, pos.x + HALF_THICK, pos.y + up, pos.z + across);
    }

    /** Server: gone once its hub is. */
    @Override
    public void tick() {
        if (level() instanceof ServerLevel && tickCount % 20 == 0) {
            BlockState hub = level().getBlockState(blockPosition());
            int s = entityData.get(SHAPE);
            if (!hub.is(WaterPower.WATER_WHEEL.get()) || (hub.getValue(WaterWheelBlock.AXIS) == Direction.Axis.Z) != ((s & 1) != 0)) discard();
        }
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
