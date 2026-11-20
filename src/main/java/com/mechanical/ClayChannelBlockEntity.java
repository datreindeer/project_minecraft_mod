package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The metal standing in one Clay Channel: one metal, up to {@link #CAPACITY} quarter units, and when it last moved.
 * Metal that hasn't moved in or out for {@link #SET_TICKS} has set solid (worked out from the time, nothing ticks).
 * Moved only by {@link ChannelFlow}; drawn by client/MoltenRenderer.
 */
public class ClayChannelBlockEntity extends BlockEntity {
    /** One crucible unit per channel. */
    public static final int CAPACITY = 4;
    /** 60 s, as long as a Crucible of Molten Metal stays molten. */
    public static final int SET_TICKS = 1200;
    /** Metal that moved this recently is drawn running. */
    public static final int RUNNING_TICKS = 12;

    private @Nullable Metal metal;
    private int amount;
    private long movedAt;

    public ClayChannelBlockEntity(BlockPos pos, BlockState state) {
        super(Smelting.CLAY_CHANNEL_ENTITY.get(), pos, state);
    }

    /** The metal in it, or null when empty. */
    public @Nullable Metal metal() {
        return amount > 0 ? metal : null;
    }

    /** Quarter units of metal in it. */
    public int amount() {
        return amount;
    }

    public boolean solid(long now) {
        return amount > 0 && now - movedAt >= SET_TICKS;
    }

    public boolean running(long now) {
        return amount > 0 && now - movedAt < RUNNING_TICKS;
    }

    /** Sets the contents (only {@link ChannelFlow} calls this, always moving the difference from or to somewhere else). */
    void set(@Nullable Metal metal, int amount, long now) {
        if (amount == this.amount && (amount == 0 || metal == this.metal)) return;
        this.metal = amount > 0 ? metal : null;
        this.amount = amount;
        this.movedAt = now;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // broken with metal in it: it spills and is lost (scoop it out with a Crucible first)
        if (amount > 0 && level instanceof ServerLevel server) {
            server.sendParticles(solid(server.getGameTime()) ? ParticleTypes.SMOKE : ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    4, 0.2, 0.05, 0.2, 0.0);
            server.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (metal != null && amount > 0) {
            output.putString("metal", metal.getSerializedName());
            output.putInt("amount", amount);
            output.putLong("moved_at", movedAt);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        metal = input.getString("metal").map(Metal::byId).orElse(null);
        amount = metal == null ? 0 : input.getIntOr("amount", 0);
        movedAt = input.getLongOr("moved_at", 0);
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
