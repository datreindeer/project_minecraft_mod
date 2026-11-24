package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How melt runs through Refractory Troughs (docs/steel-furnace-plan.md section 6, "Rännor"). The troughs join the way
 * Clay Channels do ({@link ChannelFlow#ports}, {@link ChannelFlow#across}: block-edge ports, slopes like rails, molds
 * beside them), but each trough moves its own metal every {@link #STEP_TICKS}, in mB, any melt:
 * <ol>
 * <li>a mold (or flask, table, basin ...) beside it takes a whole cast through the trough's buffer, as under a spout;</li>
 * <li>downhill first, shared evenly between every lower trough (a T or a fork fills its branches together);</li>
 * <li>then level troughs with less in them get half the difference (it spreads evenly along a level run).</li>
 * </ol>
 * Every move takes exactly what it gives. Melt cools as it runs: it reaches {@link #reach} troughs from the tap
 * (8 + one per 25° the tap was over its melting point); the trough past that freezes at once. A Clay Channel joined
 * to a trough carrying melt hotter than {@link PouringSpoutBlockEntity#CLAY_MAX}° cracks and breaks, and the metal that
 * ran into it is lost. A closed Trough Gate has no ports, so nothing passes it.
 */
public final class TroughFlow {
    public static final int STEP_TICKS = 5, RATE = 200, BASE_REACH = 8, DEGREES_PER_TROUGH = 25;

    public static int reach(int tapHeat, Molten m) {
        return BASE_REACH + Math.max(0, tapHeat - m.melt) / DEGREES_PER_TROUGH;
    }

    static void tick(ServerLevel level, BlockPos pos, TroughBlockEntity t) {
        long now = level.getGameTime();
        if (now % STEP_TICKS != 0 || t.mb == 0 || t.solid(now)) {
            if (t.buffer > 0 && t.solid(now)) returnBuffer(t, now);
            return;
        }
        Molten m = t.molten;
        BlockState state = level.getBlockState(pos);
        // 1. a cast beside it
        pourIntoSink(level, pos, t, m, now);
        if (t.mb == 0) return;
        // 2./3. on through the joined troughs
        int give = Math.min(RATE, t.mb), elev = ChannelFlow.elevation(pos, state);
        List<TroughBlockEntity> down = new ArrayList<>(), flat = new ArrayList<>();
        for (BlockPos port : ChannelFlow.ports(pos, state)) {
            BlockPos q = ChannelFlow.across(level, pos, port);
            if (q == null) continue;
            BlockState qs = level.getBlockState(q);
            if (level.getBlockEntity(q) instanceof TroughBlockEntity o) {
                if (!o.takes(m, now) || o.room() <= 0) continue;
                int e = ChannelFlow.elevation(q, qs);
                if (e < elev) down.add(o);
                else if (e == elev && o.mb < t.mb) flat.add(o);
            } else if (qs.getBlock() instanceof ClayChannelBlock && !(qs.getBlock() instanceof RefractoryTroughBlock) && m.melt > PouringSpoutBlockEntity.CLAY_MAX) {
                crack(level, q, t, now);
                if (t.mb == 0) return;
            }
        }
        if (!down.isEmpty()) {
            int each = give / down.size(), extra = give % down.size(), start = (int) ((now / STEP_TICKS) % down.size());
            for (int i = 0; i < down.size(); i++) {
                TroughBlockEntity o = down.get((start + i) % down.size());
                int n = Math.min(each + (i < extra ? 1 : 0), o.room());
                move(level, t, o, m, n, now);
            }
            give = Math.min(give, t.mb);
        }
        for (TroughBlockEntity o : flat) {
            int n = Math.min(Math.min((t.mb - o.mb) / 2, give / Math.max(1, flat.size())), o.room());
            move(level, t, o, m, n, now);
        }
    }

    /** Moves {@code n} mB from one trough to the next one out; past the melt's reach it freezes there. */
    private static void move(ServerLevel level, TroughBlockEntity from, TroughBlockEntity to, Molten m, int n, long now) {
        if (n <= 0) return;
        int got = from.take(n, now);
        to.add(m, got, from.dist + 1, from.heat, now);
        if (to.dist > reach(to.heat, m)) to.freeze();
    }

    /** Hot iron in clay: the channel glows, cracks and breaks; what ran into it is lost. */
    private static void crack(ServerLevel level, BlockPos q, TroughBlockEntity t, long now) {
        t.take(Math.min(RATE / 2, t.mb), now);
        level.sendParticles(ParticleTypes.LAVA, q.getX() + 0.5, q.getY() + 0.2, q.getZ() + 0.5, 6, 0.25, 0.05, 0.25, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, q.getX() + 0.5, q.getY() + 0.3, q.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.02);
        level.playSound(null, q, SoundEvents.DECORATED_POT_SHATTER, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, q, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.9F);
        level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
    }

    /** Fills a mold beside the trough through the trough's buffer (a whole cast at a time, as under a spout). */
    private static void pourIntoSink(ServerLevel level, BlockPos pos, TroughBlockEntity t, Molten m, long now) {
        if (t.bufferSide != null) {
            BlockPos q = pos.relative(t.bufferSide);
            PouringSpoutBlockEntity.Sink sink = sinkAt(level, q, t.bufferMolten);
            int need = sink == null ? 0 : sink.need(level, q, t.bufferMolten);
            if (need == 0 || t.bufferMolten != m) {
                if (sink != null) sink.abort(level, q);
                returnBuffer(t, now);
                return;
            }
            int n = t.take(Math.min(RATE / 5, need - t.buffer), now);
            t.buffer += n;
            sink.progress(level, q, m, t.buffer, need);
            if (t.buffer >= need) {
                sink.fill(level, q, m, t.buffer, t.heat);
                t.buffer = 0;
                t.bufferSide = null;
                t.bufferMolten = null;
                level.playSound(null, q, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
            }
            t.sync();
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos q = pos.relative(d);
            PouringSpoutBlockEntity.Sink sink = sinkAt(level, q, m);
            if (sink == null) continue;
            t.bufferSide = d;
            t.bufferMolten = m;
            return;
        }
    }

    /** A sink beside a trough that takes {@code m} now (never another trough: they are joined, not poured into). */
    private static PouringSpoutBlockEntity.Sink sinkAt(ServerLevel level, BlockPos q, Molten m) {
        if (m == null || level.getBlockEntity(q) instanceof TroughBlockEntity) return null;
        for (PouringSpoutBlockEntity.Sink s : PouringSpoutBlockEntity.SINKS) if (s.need(level, q, m) > 0) return s;
        return null;
    }

    private static void returnBuffer(TroughBlockEntity t, long now) {
        if (t.buffer > 0 && t.bufferMolten != null) {
            t.molten = t.bufferMolten;
            t.mb += t.buffer;
            t.movedAt = now;
        }
        t.buffer = 0;
        t.bufferSide = null;
        t.bufferMolten = null;
        t.sync();
    }

    /** As a Pouring Spout's sink: a trough under the lip takes what it has room for, a little each tick. */
    static final class Sink implements PouringSpoutBlockEntity.Sink {
        @Override
        public int need(ServerLevel level, BlockPos at, Molten m) {
            return level.getBlockEntity(at) instanceof TroughBlockEntity t && t.takes(m, level.getGameTime()) ? Math.min(PouringSpoutBlockEntity.RATE, t.room()) : 0;
        }

        @Override
        public void fill(ServerLevel level, BlockPos at, Molten m, int mb) {
            fill(level, at, m, mb, m.melt);
        }

        @Override
        public void fill(ServerLevel level, BlockPos at, Molten m, int mb, int heat) {
            if (level.getBlockEntity(at) instanceof TroughBlockEntity t) t.add(m, mb, 0, heat, level.getGameTime());
        }

        @Override
        public net.minecraft.world.phys.Vec3 target(ServerLevel level, BlockPos at, int poured, int need) {
            int in = level.getBlockEntity(at) instanceof TroughBlockEntity t ? t.mb : 0;
            return new net.minecraft.world.phys.Vec3(0.5, (2 + 3F * in / TroughBlockEntity.CAPACITY) / 16, 0.5);
        }
    }

    private TroughFlow() {}
}
