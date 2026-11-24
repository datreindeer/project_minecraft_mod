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

        /** The same, knowing how hot it was tapped (a trough carries that on, for how far it runs). */
        default void fill(ServerLevel level, BlockPos at, Molten m, int mb, int heat) {
            fill(level, at, m, mb);
        }

        /**
         * Where the stream lands now, in the sink block's own coordinates (0..1): the surface of the metal in the cavity
         * being filled, {@code poured} of {@code need} mB in. The stream is drawn down to it.
         */
        default net.minecraft.world.phys.Vec3 target(ServerLevel level, BlockPos at, int poured, int need) {
            return new net.minecraft.world.phys.Vec3(0.5, 0.2, 0.5);
        }

        /** Shows the cast filling while the stream runs ({@code poured} of {@code need} mB in so far). */
        default void progress(ServerLevel level, BlockPos at, Molten m, int poured, int need) {}

        /** The stream stopped before the cast was whole (its metal went back to the pool). */
        default void abort(ServerLevel level, BlockPos at) {}

        /** Why it refuses a melt (null: it says nothing). */
        default @Nullable Component refuses(ServerLevel level, BlockPos at, Molten m) {
            return null;
        }
    }

    /** The sinks the spout knows, asked in turn (the casting tier adds its tables, basins, flasks and molds). */
    public static final java.util.List<Sink> SINKS = new java.util.ArrayList<>(java.util.List.of(new ClayMoldSink(), new TroughFlow.Sink()));

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
        public net.minecraft.world.phys.Vec3 target(ServerLevel level, BlockPos at, int poured, int need) {
            return new net.minecraft.world.phys.Vec3(0.5, 3.05 / 16, 0.5); // the cavity's floor, the middle of the mold
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
    /** Synced: where the stream lands, relative to this block's corner (the sink is the block below). */
    private float tx = 0.5F, ty = -0.8F, tz = 0.5F;
    private @Nullable Sink lastSink;
    private int warnIn;
    private long lastRun;
    private static final int HOLD_TICKS = 8;

    public PouringSpoutBlockEntity(BlockPos pos, BlockState state) {
        super(HighFurnace.POURING_SPOUT_ENTITY.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(PouringSpoutBlock.FACING);
    }

    public @Nullable Molten stream() {
        return stream;
    }

    /** Where the stream lands, relative to this block's corner. */
    public net.minecraft.world.phys.Vec3 target() {
        return new net.minecraft.world.phys.Vec3(tx, ty, tz);
    }

    /** The lip the stream leaves from, relative to this block's corner. */
    public net.minecraft.world.phys.Vec3 lip() {
        Direction d = facing();
        return new net.minecraft.world.phys.Vec3(0.5 + d.getStepX() * 0.09, 0.36, 0.5 + d.getStepZ() * 0.09);
    }

    /** The furnace whose Taphole this spout sits on, formed, or null. */
    @Nullable HighFurnaceBlockEntity furnace(ServerLevel level) {
        BlockPos tap = worldPosition.relative(facing().getOpposite());
        if (!level.getBlockState(tap).is(HighFurnace.TAPHOLE.get())) return null;
        HighFurnaceBlockEntity f = HighFurnaceBlockEntity.find(level, tap);
        return f != null && f.formed() && f.shape().tapholes().stream().anyMatch(w -> w.pos().equals(tap)) ? f : null;
    }

    /** Where the spout draws from: a furnace on its taphole, or the trough it sits at the end of. */
    private interface Source {
        @Nullable Molten molten();

        int available();

        int drain(Molten m, int mb);

        void giveBack(Molten m, int mb);

        int heat();

        /** A furnace starts a cast only when it holds all of it; a trough feeds a cast as it refills. */
        default boolean whole() {
            return true;
        }
    }

    private @Nullable Source source(ServerLevel level) {
        if (!getBlockState().getValue(PouringSpoutBlock.OPEN)) return null;
        HighFurnaceBlockEntity f = furnace(level);
        if (f != null) return new Source() {
            public @Nullable Molten molten() {
                Molten m = f.pouring();
                return m != null && f.temperature() >= m.melt ? m : null;
            }

            public int available() {
                Molten m = molten();
                return m == null ? 0 : f.pool().get(m);
            }

            public int drain(Molten m, int mb) {
                f.setChanged();
                return f.pool().drain(m, mb);
            }

            public void giveBack(Molten m, int mb) {
                f.pool().add(m, mb);
            }

            public int heat() {
                return Math.round(f.temperature());
            }
        };
        if (level.getBlockEntity(worldPosition.relative(facing().getOpposite())) instanceof TroughBlockEntity t) return new Source() {
            public @Nullable Molten molten() {
                return t.solid(level.getGameTime()) ? null : t.molten();
            }

            public int available() {
                return molten() == null ? 0 : t.amount();
            }

            public int drain(Molten m, int mb) {
                t.markOut(facing(), level.getGameTime());
                return t.take(mb, level.getGameTime());
            }

            public void giveBack(Molten m, int mb) {
                t.add(m, mb, t.dist(), t.heat(), level.getGameTime());
            }

            public int heat() {
                return t.heat();
            }

            public boolean whole() {
                return false;
            }
        };
        return null;
    }

    private @Nullable Source source;

    void serverTick(ServerLevel level) {
        Molten running = null;
        Source src = source(level);
        Molten m = src == null ? null : src.molten();
        BlockPos at = worldPosition.below();
        if (bufferMolten != null && (m != bufferMolten || src == null)) giveBack();
        source = src;
        if (m != null && src.available() > 0) {
            Sink sink = null;
            int need = 0;
            for (Sink s : SINKS) {
                need = s.need(level, at, m);
                if (need > 0) {
                    sink = s;
                    break;
                }
            }
            if (sink != null && bufferMolten == null && src.available() < need && src.whole() && !(sink instanceof TroughFlow.Sink)) sink = null; // a cast starts only when it is all there
            if (sink == null) {
                giveBack();
                if (--warnIn <= 0) {
                    warnIn = 40;
                    for (Sink s : SINKS) {
                        Component why = s.refuses(level, at, m);
                        if (why != null) for (ServerPlayer p : level.players()) if (p.blockPosition().closerThan(worldPosition, 8)) p.sendOverlayMessage(why);
                    }
                }
            } else {
                int take = src.drain(m, Math.min(RATE, need - buffer));
                buffer += take;
                bufferMolten = m;
                lastSink = sink;
                running = take > 0 ? m : null;
                sink.progress(level, at, m, buffer, need);
                net.minecraft.world.phys.Vec3 t = sink.target(level, at, buffer, need);
                if (sink instanceof TroughFlow.Sink) t = new net.minecraft.world.phys.Vec3(lip().x, t.y, lip().z); // a trough: it falls straight down off the lip
                float nx = (float) t.x, ny = (float) t.y - 1, nz = (float) t.z;
                if (nx != tx || ny != ty || nz != tz) {
                    tx = nx;
                    ty = ny;
                    tz = nz;
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                }
                if (buffer >= need) {
                    sink.fill(level, at, m, buffer, src.heat());
                    buffer = 0;
                    bufferMolten = null;
                    level.playSound(null, at, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.3F);
                    Smelting.award(level, worldPosition, HighFurnace.POUR_ADVANCEMENT, 16);
                }
            }
        } else giveBack();
        // the stream stays drawn through the ticks a trough below has no room for a moment (it drains every few ticks)
        if (running != null) lastRun = level.getGameTime();
        else if (stream != null && level.getGameTime() - lastRun < HOLD_TICKS && src != null && src.molten() == stream) running = stream;
        if (running != stream) {
            stream = running;
            if (getBlockState().getValue(PouringSpoutBlock.POURING) != (running != null))
                level.setBlock(worldPosition, getBlockState().setValue(PouringSpoutBlock.POURING, running != null), Block.UPDATE_CLIENTS);
            BlockPos tap = worldPosition.relative(facing().getOpposite());
            BlockState ts = level.getBlockState(tap);
            if (ts.is(HighFurnace.TAPHOLE.get()) && ts.getValue(HighFurnaceWallBlock.Facing.OPEN) != (running != null)) // the plug out while it pours
                level.setBlock(tap, ts.setValue(HighFurnaceWallBlock.Facing.OPEN, running != null), Block.UPDATE_CLIENTS);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (running != null) level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        if (running != null && level.getGameTime() % 30 == 0) level.playSound(null, at, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    /** What the stream poured into a cast that is not whole runs back into the pool (or is lost with the furnace). */
    private void giveBack() {
        if (buffer > 0 && lastSink != null && level instanceof ServerLevel server) lastSink.abort(server, worldPosition.below());
        if (buffer > 0 && bufferMolten != null && source != null) source.giveBack(bufferMolten, buffer);
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
        double x = worldPosition.getX() + tx, z = worldPosition.getZ() + tz, y = worldPosition.getY() + ty + 0.02; // where it lands
        if (r.nextInt(4) == 0) { // a drop falling beside the stream
            double f = r.nextDouble();
            net.minecraft.world.phys.Vec3 lip = lip();
            level.addParticle(ParticleTypes.FALLING_LAVA, worldPosition.getX() + lip.x + (x - worldPosition.getX() - lip.x) * f + (r.nextDouble() - 0.5) * 0.08,
                    worldPosition.getY() + lip.y + (y - worldPosition.getY() - lip.y) * f, worldPosition.getZ() + lip.z + (z - worldPosition.getZ() - lip.z) * f + (r.nextDouble() - 0.5) * 0.08, 0, 0, 0);
        }
        if (r.nextInt(3) == 0) // tiny sparks off the surface
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 0.02, z, (r.nextDouble() - 0.5) * 0.12, 0.05 + r.nextDouble() * 0.08, (r.nextDouble() - 0.5) * 0.12);
        if (r.nextInt(6) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0.015, 0);

    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer = input.getIntOr("buffer", 0);
        bufferMolten = Molten.byId(input.getStringOr("buffer_molten", ""));
        stream = Molten.byId(input.getStringOr("stream", ""));
        tx = input.getFloatOr("tx", 0.5F);
        ty = input.getFloatOr("ty", -0.8F);
        tz = input.getFloatOr("tz", 0.5F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("buffer", buffer);
        if (bufferMolten != null) output.putString("buffer_molten", bufferMolten.id);
        if (stream != null) output.putString("stream", stream.id);
        output.putFloat("tx", tx);
        output.putFloat("ty", ty);
        output.putFloat("tz", tz);
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
