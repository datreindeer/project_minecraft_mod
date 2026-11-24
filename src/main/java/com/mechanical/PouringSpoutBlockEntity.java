package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Pouring Spout's work (see {@link PouringSpoutBlock}). Open, on a formed High Furnace's Taphole, it lets out the
 * furnace's pouring layer ({@link HighFurnaceBlockEntity#pouring}) while the furnace is at least that melt's heat, at
 * {@link #RATE} mB a tick, into what stands right under its lip (a {@link Sink}). The stream fills the spout's buffer
 * until the cast is whole, then the cast takes it all; if the sink goes away mid-pour the buffer runs back into the
 * pool, so no metal is made or lost. A Clay Mold takes only melts up to {@link #CLAY_MAX}° ("Too hot for clay").
 * The client draws the stream (client/PouringSpoutRenderer) and its spatter.
 */
public class PouringSpoutBlockEntity extends BlockEntity {
    public static final int RATE = 40, CLAY_MAX = 1100;

    /** What stands under the lip and takes melt: how much one cast needs, and how to finish it. */
    public interface Sink {
        /** mB one cast of {@code m} needs here, or 0 if it takes none now. */
        int need(ServerLevel level, BlockPos at, Molten m);

        /** The cast is whole: {@code mb} of {@code m} went in. */
        void fill(ServerLevel level, BlockPos at, Molten m, int mb);

        /** Why it refuses a melt (null: it says nothing). */
        default @Nullable Component refuses(ServerLevel level, BlockPos at, Molten m) {
            return null;
        }
    }

    /** The sinks the spout knows, asked in turn (the casting tier adds its tables, basins, flasks and molds). */
    public static final java.util.List<Sink> SINKS = new java.util.ArrayList<>(java.util.List.of(new ClayMoldSink()));

    /** A Clay Mold: an empty one takes a whole cast of copper or bronze (its shape's units x 100 mB), never hotter than clay stands. */
    static final class ClayMoldSink implements Sink {
        @Override
        public int need(ServerLevel level, BlockPos at, Molten m) {
            BlockState s = level.getBlockState(at);
            if (!(s.getBlock() instanceof ClayMoldBlock mold) || s.getValue(ClayMoldBlock.FILL) != ClayMoldBlock.Fill.EMPTY) return 0;
            if (m.melt > CLAY_MAX || m.metal == null || !m.metal.casts(mold.shape)) return 0;
            return mold.shape.units * Molten.UNIT;
        }

        @Override
        public void fill(ServerLevel level, BlockPos at, Molten m, int mb) {
            BlockState s = level.getBlockState(at);
            if (!(s.getBlock() instanceof ClayMoldBlock mold) || m.metal == null) return;
            level.setBlock(at, s.setValue(ClayMoldBlock.FILL, ClayMoldBlock.Fill.MOLTEN).setValue(ClayMoldBlock.METAL, m.metal), Block.UPDATE_ALL);
            level.scheduleTick(at, mold, ClayMoldBlock.SET_TICKS);
        }

        @Override
        public @Nullable Component refuses(ServerLevel level, BlockPos at, Molten m) {
            return level.getBlockState(at).getBlock() instanceof ClayMoldBlock && m.melt > CLAY_MAX
                    ? Component.translatable("message.mechanical.pouring_spout.too_hot") : null;
        }
    }

    private int buffer;
    private @Nullable Molten bufferMolten;
    /** Synced: the melt running now (null: none) and how far it falls, in blocks. */
    private @Nullable Molten stream;
    private float fall = 0.65F;
    private int warnIn;

    public PouringSpoutBlockEntity(BlockPos pos, BlockState state) {
        super(HighFurnace.POURING_SPOUT_ENTITY.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(PouringSpoutBlock.FACING);
    }

    public @Nullable Molten stream() {
        return stream;
    }

    public float fall() {
        return fall;
    }

    /** The furnace whose Taphole this spout sits on, formed, or null. */
    @Nullable HighFurnaceBlockEntity furnace(ServerLevel level) {
        BlockPos tap = worldPosition.relative(facing().getOpposite());
        if (!level.getBlockState(tap).is(HighFurnace.TAPHOLE.get())) return null;
        HighFurnaceBlockEntity f = HighFurnaceBlockEntity.find(level, tap);
        return f != null && f.formed() && f.shape().tapholes().stream().anyMatch(w -> w.pos().equals(tap)) ? f : null;
    }

    void serverTick(ServerLevel level) {
        Molten running = null;
        HighFurnaceBlockEntity f = getBlockState().getValue(PouringSpoutBlock.OPEN) ? furnace(level) : null;
        Molten m = f == null ? null : f.pouring();
        BlockPos at = worldPosition.below();
        if (bufferMolten != null && (m != bufferMolten || f == null)) giveBack(f);
        if (m != null && f.temperature() >= m.melt && f.pool().get(m) > 0) {
            Sink sink = null;
            int need = 0;
            for (Sink s : SINKS) {
                need = s.need(level, at, m);
                if (need > 0) {
                    sink = s;
                    break;
                }
            }
            if (sink != null && bufferMolten == null && f.pool().get(m) < need) sink = null; // a cast starts only when the pool holds all of it
            if (sink == null) {
                giveBack(f);
                if (--warnIn <= 0) {
                    warnIn = 40;
                    for (Sink s : SINKS) {
                        Component why = s.refuses(level, at, m);
                        if (why != null) for (ServerPlayer p : level.players()) if (p.blockPosition().closerThan(worldPosition, 8)) p.sendOverlayMessage(why);
                    }
                }
            } else {
                int take = f.pool().drain(m, Math.min(RATE, need - buffer));
                buffer += take;
                bufferMolten = m;
                running = take > 0 ? m : null;
                if (buffer >= need) {
                    sink.fill(level, at, m, buffer);
                    buffer = 0;
                    bufferMolten = null;
                    level.playSound(null, at, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.3F);
                    Smelting.award(level, worldPosition, HighFurnace.POUR_ADVANCEMENT, 16);
                }
                f.setChanged();
            }
        } else giveBack(f);
        if (running != stream) {
            stream = running;
            if (getBlockState().getValue(PouringSpoutBlock.POURING) != (running != null))
                level.setBlock(worldPosition, getBlockState().setValue(PouringSpoutBlock.POURING, running != null), Block.UPDATE_CLIENTS);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (running != null) level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        if (running != null && level.getGameTime() % 30 == 0) level.playSound(null, at, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    /** What the stream poured into a cast that is not whole runs back into the pool (or is lost with the furnace). */
    private void giveBack(@Nullable HighFurnaceBlockEntity f) {
        if (buffer > 0 && bufferMolten != null && f != null) f.pool().add(bufferMolten, buffer);
        buffer = 0;
        bufferMolten = null;
    }

    /** Test: what is in the stream right now (mB not yet in a whole cast). */
    int bufferForTest() {
        return buffer;
    }

    void clientTick(Level level) {
        if (stream == null) return;
        RandomSource r = level.getRandom();
        Direction d = facing();
        double x = worldPosition.getX() + 0.5 + d.getStepX() * 0.3, z = worldPosition.getZ() + 0.5 + d.getStepZ() * 0.3, y = worldPosition.getY() - fall + 0.35;
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
        if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, (r.nextDouble() - 0.5) * 0.3, 0.15 + r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.3);
        if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.04, 0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer = input.getIntOr("buffer", 0);
        bufferMolten = Molten.byId(input.getStringOr("buffer_molten", ""));
        stream = Molten.byId(input.getStringOr("stream", ""));
        fall = input.getFloatOr("fall", 0.65F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("buffer", buffer);
        if (bufferMolten != null) output.putString("buffer_molten", bufferMolten.id);
        if (stream != null) output.putString("stream", stream.id);
        output.putFloat("fall", fall);
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
