package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The melt in a Refractory Trough (or a Trough Gate): one melt, up to {@link #CAPACITY} mB, when it last moved, how many
 * troughs it has come from the tap ({@link #dist}) and how hot it was when tapped ({@link #heat}). Metal that has not
 * moved for {@link #SET_TICKS} has set; metal that ran further than its reach froze where it stopped ({@link #frozen}).
 * A trough also keeps the part of a cast it is pouring into a mold beside it ({@link #buffer}). Moved only by
 * {@link TroughFlow}; drawn by client/TroughRenderer.
 */
public class TroughBlockEntity extends BlockEntity {
    public static final int CAPACITY = 200, SET_TICKS = 1200, RUNNING_TICKS = 12;

    @Nullable Molten molten;
    int mb, dist, heat;
    long movedAt;
    boolean frozen;
    /** Metal on its way into the mold beside it (on side {@link #bufferSide}); it goes back into the trough if the mold goes. */
    int buffer;
    @Nullable Direction bufferSide;
    @Nullable Molten bufferMolten;

    public TroughBlockEntity(BlockPos pos, BlockState state) {
        super(HighFurnace.TROUGH_ENTITY.get(), pos, state);
    }

    public @Nullable Molten molten() {
        return mb > 0 ? molten : null;
    }

    public int amount() {
        return mb;
    }

    public int dist() {
        return dist;
    }

    public int heat() {
        return heat;
    }

    public boolean solid(long now) {
        return mb > 0 && (frozen || now - movedAt >= SET_TICKS);
    }

    public boolean running(long now) {
        return mb > 0 && !frozen && now - movedAt < RUNNING_TICKS;
    }

    /** How cool it has got, 0 molten .. 1 set (for the cooling colours). */
    public float cooled(long now) {
        return mb == 0 ? 0 : frozen ? 1 : Math.clamp((now - movedAt) / (float) SET_TICKS, 0, 1);
    }

    /** Does it take {@code m} now (empty, or the same melt still runny)? */
    boolean takes(Molten m, long now) {
        return mb == 0 || molten == m && !solid(now);
    }

    int room() {
        return CAPACITY - mb;
    }

    /** Adds metal arriving from {@code fromDist} troughs out, tapped at {@code fromHeat}°. */
    void add(Molten m, int amount, int fromDist, int fromHeat, long now) {
        if (amount <= 0) return;
        if (mb == 0) {
            dist = fromDist;
            heat = fromHeat;
            frozen = false;
        } else {
            dist = Math.min(dist, fromDist);
            heat = Math.max(heat, fromHeat);
        }
        molten = m;
        mb += amount;
        movedAt = now;
        sync();
    }

    /** Takes out up to {@code amount}; returns how much. */
    int take(int amount, long now) {
        int out = Math.min(amount, mb);
        if (out <= 0) return 0;
        mb -= out;
        if (mb == 0) {
            molten = null;
            frozen = false;
        }
        movedAt = now;
        sync();
        return out;
    }

    void freeze() {
        if (mb > 0 && !frozen) {
            frozen = true;
            sync();
        }
    }

    void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // broken with runny metal in it: it spills and is lost (set metal is knocked out with a pickaxe first)
        if (mb + buffer > 0 && level instanceof ServerLevel server) {
            server.sendParticles(solid(server.getGameTime()) ? ParticleTypes.SMOKE : ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    4, 0.2, 0.05, 0.2, 0.0);
            server.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (molten != null && mb > 0) {
            output.putString("molten", molten.id);
            output.putInt("mb", mb);
            output.putInt("dist", dist);
            output.putInt("heat", heat);
            output.putLong("moved_at", movedAt);
            output.putBoolean("frozen", frozen);
        }
        if (buffer > 0 && bufferSide != null && bufferMolten != null) {
            output.putInt("buffer", buffer);
            output.putString("buffer_side", bufferSide.getSerializedName());
            output.putString("buffer_molten", bufferMolten.id);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        molten = Molten.byId(input.getStringOr("molten", ""));
        mb = molten == null ? 0 : input.getIntOr("mb", 0);
        dist = input.getIntOr("dist", 0);
        heat = input.getIntOr("heat", 0);
        movedAt = input.getLongOr("moved_at", 0);
        frozen = input.getBooleanOr("frozen", false);
        buffer = input.getIntOr("buffer", 0);
        bufferSide = Direction.byName(input.getStringOr("buffer_side", ""));
        bufferMolten = Molten.byId(input.getStringOr("buffer_molten", ""));
        if (bufferSide == null || bufferMolten == null) buffer = 0;
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
