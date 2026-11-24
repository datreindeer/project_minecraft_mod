package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Something turned by a Water Wheel (see {@link WaterPower}): the wheel itself, a Wooden Shaft, a Gearbox or the Saw
 * Bench's crank. Its turn is a pure function of the synced {@code speed}, {@code base} and {@code t0}
 * ({@link #angle}), so every part on one line shows the wheel's own phase and nothing drifts or needs a sync per tick.
 * {@code power} is the wheel's power (0..16) that reached it; it lapses when the wheel stops feeding it.
 */
public class RotorBlockEntity extends BlockEntity {
    /** Degrees per tick, signed: positive turns about the part's +axis. */
    protected float speed;
    /** The angle at game time {@link #t0}. */
    protected float base;
    protected long t0;
    protected int power;
    /** Game time of the last feed (not synced). */
    protected long fedAt = Long.MIN_VALUE / 2;

    public RotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public RotorBlockEntity(BlockPos pos, BlockState state) {
        this(WaterPower.ROTOR_ENTITY.get(), pos, state);
    }

    public float angle(float time) {
        return (base + speed * (time - t0)) % 360.0F;
    }

    public float speed() {
        return speed;
    }

    public int power() {
        return power;
    }

    /** Takes the turn a wheel hands down the line; syncs only when it changed. */
    public void feed(float speed, float base, long t0, int power) {
        fedAt = level.getGameTime();
        if (speed == this.speed && base == this.base && t0 == this.t0 && power == this.power) return;
        this.speed = speed;
        this.base = base;
        this.t0 = t0;
        this.power = power;
        sync();
    }

    /** Server, every second: stops (where it stands) once no wheel has fed it for a while. */
    public void lapse() {
        long now = level.getGameTime();
        if (fedAt == Long.MIN_VALUE / 2) fedAt = now; // just loaded: give the wheel time to feed it again
        if ((speed != 0 || power != 0) && now - fedAt > WaterPower.LAPSE_TICKS) {
            base = angle(now);
            t0 = now;
            speed = 0;
            power = 0;
            sync();
        }
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("speed", speed);
        output.putFloat("base", base);
        output.putLong("t0", t0);
        output.putInt("power", power);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        speed = input.getFloatOr("speed", 0);
        base = input.getFloatOr("base", 0);
        t0 = input.getLongOr("t0", 0);
        power = input.getIntOr("power", 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
