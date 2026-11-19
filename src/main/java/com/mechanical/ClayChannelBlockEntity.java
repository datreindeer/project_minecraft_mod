package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Metal running through a Clay Channel, for the client to draw (client/MoltenRenderer): which metal, the side it comes
 * in from and the side it leaves by, until a game time. The furnace tap sets it for every pour; it holds no metal itself.
 */
public class ClayChannelBlockEntity extends BlockEntity {
    private @Nullable Metal metal;
    private Direction from = Direction.NORTH, to = Direction.SOUTH;
    private long until;

    public ClayChannelBlockEntity(BlockPos pos, BlockState state) {
        super(Smelting.CLAY_CHANNEL_ENTITY.get(), pos, state);
    }

    /** The metal running through at this game time, or null. */
    public @Nullable Metal metal(long now) {
        return now < until ? metal : null;
    }

    public Direction from() {
        return from;
    }

    public Direction to() {
        return to;
    }

    void flow(Metal metal, Direction from, Direction to, long until) {
        this.metal = metal;
        this.from = from;
        this.to = to;
        this.until = until;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (metal != null) output.putString("metal", metal.getSerializedName());
        output.putInt("from", from.get3DDataValue());
        output.putInt("to", to.get3DDataValue());
        output.putLong("until", until);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        metal = input.getString("metal").map(Metal::byId).orElse(null);
        from = Direction.from3DDataValue(input.getIntOr("from", 2));
        to = Direction.from3DDataValue(input.getIntOr("to", 3));
        until = input.getLongOr("until", 0);
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
