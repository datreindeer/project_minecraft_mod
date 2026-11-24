package com.mechanical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * How metal moves through Clay Channels. Everything that joins (channels, Clay Molds, a Furnace Tap's spout) has PORTS:
 * the middle of a block edge at a height (a flat channel: its four sides at its floor; a slope: its low side at its
 * floor and its high side one block up; a mold: its four sides; a tap: its spout). Two things join when they share a
 * port, so a slope links a channel one block down with one one block up, like rails on a hill.
 * <p>Every {@link #STEP_TICKS} the tap runs {@link #step}: it pours up to {@link #POUR_RATE} quarters into the channel
 * at its spout; then every channel passes up to {@link #RATE} quarters of what it held at the start of the step
 * downhill first (a lower channel, or a slope), then along level channels away from the tap, so the metal runs one
 * channel per step and fills the network from its low and far ends back; then one empty mold beside a channel with
 * metal fills, if the metal joined to it (plus what the open tap can still pour) is enough for the whole cast.
 * <p>Conservation: every change is a move of whole quarters between two holders (furnace pool, channel, mold, crucible)
 * that subtracts exactly what it adds; only slag (copper tapped with tin in it) and breaking a channel with metal in
 * it destroy metal, and nothing creates it.
 */
public final class ChannelFlow {
    public static final int STEP_TICKS = 5, POUR_RATE = 4, RATE = 4, MAX_CHANNELS = 64;
    private static final int CAP = ClayChannelBlockEntity.CAPACITY;

    /** The middle of the edge on side {@code d} of block {@code p}, at height {@code h} (x and z in half blocks). */
    public static BlockPos port(BlockPos p, Direction d, int h) {
        return new BlockPos(2 * p.getX() + 1 + d.getStepX(), h, 2 * p.getZ() + 1 + d.getStepZ());
    }

    /**
     * Every port of the thing at {@code p}: the one source of truth for what joins what, asked by the Clay Channels, the
     * Refractory Troughs ({@link TroughFlow}) and the casts' open sides ({@link #connect}). Casts that stand lower than a
     * trough (a Foundry Basin, a Molding Flask, a Pig Mold, and a Sand Runner dug in the floor) have their ports one block
     * up: a trough standing on the floor beside one joins it and pours over its rim.
     */
    public static List<BlockPos> ports(BlockPos p, BlockState s) {
        List<BlockPos> out = new ArrayList<>(4);
        Block b = s.getBlock();
        if (b instanceof RefractoryTroughBlock.Gate) { // a gate: its two ends (shut, TroughFlow passes nothing on through it)
            for (Direction.AxisDirection a : Direction.AxisDirection.values())
                    out.add(port(p, Direction.fromAxisAndDirection(s.getValue(RefractoryTroughBlock.Gate.AXIS), a), p.getY()));
        } else if (b instanceof RefractoryTroughBlock.SandRunner || b instanceof PigMoldBlock || b instanceof FoundryBasinBlock || b instanceof MoldingFlaskBlock) {
            for (Direction d : Direction.Plane.HORIZONTAL) out.add(port(p, d, p.getY() + 1));
        } else if (b instanceof ClayChannelBlock) {
            Direction up = s.getValue(ClayChannelBlock.SLOPE).up;
            if (up != null) {
                out.add(port(p, up.getOpposite(), p.getY()));
                out.add(port(p, up, p.getY() + 1));
            } else {
                for (Direction d : Direction.Plane.HORIZONTAL) out.add(port(p, d, p.getY()));
            }
        } else if (b instanceof ClayMoldBlock || b instanceof IngotMoldBlock) { // the iron molds sit beside troughs at their level
            for (Direction d : Direction.Plane.HORIZONTAL) out.add(port(p, d, p.getY()));
        } else if (b instanceof PouringSpoutBlock) { // a spout at a trough's end: its back
            out.add(port(p, s.getValue(PouringSpoutBlock.FACING).getOpposite(), p.getY()));
        } else if (b instanceof FurnaceTapBlock) {
            out.add(port(p, s.getValue(FurnaceTapBlock.FACING), p.getY()));
        } else if (s.is(HighFurnace.TAPHOLE.get())) { // the High Furnace's taphole: its front, at the hole's lip
            out.add(port(p, s.getValue(HighFurnaceWallBlock.Facing.FACING), p.getY()));
        }
        return out;
    }

    /** The port of {@code p} on side {@code d}, or null. */
    public static @Nullable BlockPos portOn(BlockPos p, BlockState s, Direction d) {
        for (BlockPos port : ports(p, s)) if (port.getX() == 2 * p.getX() + 1 + d.getStepX() && port.getZ() == 2 * p.getZ() + 1 + d.getStepZ()) return port;
        return null;
    }

    /**
     * Whether two things sharing a port pass metal, so they join: always through a channel (casts never join each other),
     * a steel-age cast or a Pouring Spout only with a Refractory Trough, a bronze Furnace Tap only with a Clay Channel.
     */
    public static boolean joins(Block a, Block b) {
        return (a instanceof ClayChannelBlock || b instanceof ClayChannelBlock) && takes(a, b) && takes(b, a);
    }

    private static boolean takes(Block a, Block other) {
        if (a instanceof IngotMoldBlock || a instanceof FoundryBasinBlock || a instanceof MoldingFlaskBlock || a instanceof PouringSpoutBlock)
            return other instanceof RefractoryTroughBlock;
        if (a instanceof FurnaceTapBlock) return other instanceof ClayChannelBlock && !(other instanceof RefractoryTroughBlock);
        return true;
    }

    /** A steel-age cast with open sides (the iron molds, Pig Mold, Foundry Basin, Molding Flask). */
    public static boolean isCast(BlockState s) {
        return s.getBlock() instanceof IngotMoldBlock || s.getBlock() instanceof FoundryBasinBlock || s.getBlock() instanceof MoldingFlaskBlock;
    }

    /** A cast's open sides: true where a trough joins it (its rim is notched there, or a chute hangs over it). */
    public static BlockState connect(LevelReader level, BlockPos pos, BlockState s) {
        for (var e : ClayChannelBlock.SIDES.entrySet()) {
            BlockPos port = portOn(pos, s, e.getKey());
            s = s.setValue(e.getValue(), port != null && across(level, pos, s, port) != null);
        }
        return s;
    }

    /** A cast placed or changed: its own open sides, and the troughs diagonally above it that may now join it. */
    static void castPlaced(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        BlockState t = connect(level, pos, state);
        if (t != state) level.setBlock(pos, t, Block.UPDATE_ALL);
        ClayChannelBlock.refreshAround(level, pos);
    }

    /** The open sides a cast's block state carries (all shut). */
    static BlockState shut(BlockState s) {
        for (var p : ClayChannelBlock.SIDES.values()) s = s.setValue(p, false);
        return s;
    }

    /** The shape something that joins takes here now (a channel's arms and slope, a cast's open sides), or {@code s} itself. */
    static BlockState reshape(LevelReader level, BlockPos pos, BlockState s) {
        if (s.getBlock() instanceof ClayChannelBlock channel) return channel.stateAt(level, pos);
        return isCast(s) ? connect(level, pos, s) : s;
    }

    /** Height for the flow, in half blocks: a mold sits below the channels beside it (it fills first), a slope between its two levels. */
    static int elevation(BlockPos p, BlockState s) {
        if (s.getBlock() instanceof ClayMoldBlock || isCast(s)) return 2 * p.getY() - 1;
        if (s.getBlock() instanceof ClayChannelBlock && s.getValue(ClayChannelBlock.SLOPE).up != null) return 2 * p.getY() + 1;
        return 2 * p.getY();
    }

    /** What shares {@code port} with {@code self} on the other side of its edge (at the port's height, or a slope one below), or null. */
    public static @Nullable BlockPos across(BlockGetter level, BlockPos self, BlockPos port) {
        return across(level, self, level.getBlockState(self), port);
    }

    /** The same, for {@code self} being {@code selfState} (a block being placed is not in the level yet); only what {@link #joins} it. */
    public static @Nullable BlockPos across(BlockGetter level, BlockPos self, BlockState selfState, BlockPos port) {
        int dx = port.getX() - (2 * self.getX() + 1), dz = port.getZ() - (2 * self.getZ() + 1);
        if (dx == 0 && dz == 0) return null;
        for (int y = port.getY(); y >= port.getY() - 1; y--) {
            BlockPos q = new BlockPos(self.getX() + dx, y, self.getZ() + dz);
            if (level instanceof LevelReader reader && !reader.hasChunkAt(q)) return null;
            BlockState qs = level.getBlockState(q);
            if (ports(q, qs).contains(port) && joins(selfState.getBlock(), qs.getBlock())) return q;
        }
        return null;
    }

    /** A connected channel network, channels in breadth-first order from where it was explored. */
    static final class Net {
        final List<BlockPos> nodes = new ArrayList<>();
        final List<ClayChannelBlockEntity> channels = new ArrayList<>();
        final List<List<Integer>> adj = new ArrayList<>();
        final List<List<BlockPos>> molds = new ArrayList<>();
        final List<Integer> elev = new ArrayList<>(), depth = new ArrayList<>();

        int size() {
            return nodes.size();
        }
    }

    static Net explore(ServerLevel level, BlockPos start) {
        Net net = new Net();
        Map<BlockPos, Integer> index = new HashMap<>();
        if (!(level.getBlockEntity(start) instanceof ClayChannelBlockEntity first)) return net;
        add(net, index, start, first, level.getBlockState(start), 0);
        for (int i = 0; i < net.size(); i++) {
            BlockPos p = net.nodes.get(i);
            for (BlockPos port : ports(p, level.getBlockState(p))) {
                BlockPos q = across(level, p, port);
                if (q == null) continue;
                BlockState s = level.getBlockState(q);
                if (s.getBlock() instanceof ClayMoldBlock) {
                    if (!net.molds.get(i).contains(q)) net.molds.get(i).add(q);
                    continue;
                }
                Integer j = index.get(q);
                if (j == null) {
                    if (net.size() >= MAX_CHANNELS || !(level.getBlockEntity(q) instanceof ClayChannelBlockEntity c)) continue;
                    j = add(net, index, q, c, s, net.depth.get(i) + 1);
                }
                if (!net.adj.get(i).contains(j)) net.adj.get(i).add(j);
                if (!net.adj.get(j).contains(i)) net.adj.get(j).add(i);
            }
        }
        return net;
    }

    private static int add(Net net, Map<BlockPos, Integer> index, BlockPos p, ClayChannelBlockEntity c, BlockState s, int depth) {
        int i = net.size();
        index.put(p.immutable(), i);
        net.nodes.add(p.immutable());
        net.channels.add(c);
        net.adj.add(new ArrayList<>());
        net.molds.add(new ArrayList<>());
        net.elev.add(elevation(p, s));
        net.depth.add(depth);
        return i;
    }

    /**
     * One step of flow through the network starting at {@code first} (the channel at a tap's spout): the furnace pours
     * {@code pour} (null: it doesn't), the metal runs, maybe a mold fills. True if any metal moved out of the furnace.
     */
    static boolean step(ServerLevel level, BlockPos first, FurnaceMelt melt, @Nullable Metal pour) {
        long now = level.getGameTime();
        Net net = explore(level, first);
        int n = net.size();
        if (n == 0) return false;
        Metal[] metal = new Metal[n];
        int[] held = new int[n], in = new int[n], out = new int[n];
        boolean[] solid = new boolean[n];
        for (int i = 0; i < n; i++) {
            ClayChannelBlockEntity c = net.channels.get(i);
            metal[i] = c.metal();
            held[i] = c.amount();
            solid[i] = c.solid(now);
        }
        boolean fromFurnace = false;
        if (pour != null && !solid[0] && (held[0] == 0 || metal[0] == pour)) {
            int q = Math.min(Math.min(POUR_RATE, melt.pourable()), CAP - held[0]);
            if (q > 0 && melt.pour(q)) {
                in[0] = q;
                metal[0] = pour;
                fromFurnace = true;
            }
        }
        for (int i = 0; i < n; i++) {
            if (solid[i] || held[i] == 0) continue;
            int give = Math.min(RATE, held[i]);
            for (int pass = 0; pass < 2 && give > 0; pass++) {
                for (int j : net.adj.get(i)) {
                    boolean way = pass == 0 ? net.elev.get(j) < net.elev.get(i) : net.elev.get(j).equals(net.elev.get(i)) && net.depth.get(j) > net.depth.get(i);
                    if (!way || solid[j] || (held[j] + in[j] > 0 && metal[j] != metal[i])) continue;
                    int t = Math.min(give, CAP - held[j] - in[j]);
                    if (t <= 0) continue;
                    metal[j] = metal[i];
                    in[j] += t;
                    out[i] += t;
                    give -= t;
                    if (give == 0) break;
                }
            }
        }
        int[] now2 = new int[n];
        for (int i = 0; i < n; i++) now2[i] = held[i] + in[i] - out[i];
        fromFurnace |= fillMold(level, net, metal, now2, solid, melt, pour);
        for (int i = 0; i < n; i++) net.channels.get(i).set(metal[i], now2[i], now);
        return fromFurnace;
    }

    /** Fills the first empty mold beside a channel with metal, from the metal joined to it (nearest first), then the furnace. True if the furnace gave some. */
    private static boolean fillMold(ServerLevel level, Net net, Metal[] metal, int[] amount, boolean[] solid, FurnaceMelt melt, @Nullable Metal pour) {
        for (int i = 0; i < net.size(); i++) {
            if (amount[i] == 0 || solid[i]) continue;
            for (BlockPos mp : net.molds.get(i)) {
                BlockState ms = level.getBlockState(mp);
                Metal m = metal[i];
                if (!(ms.getBlock() instanceof ClayMoldBlock mold) || ms.getValue(ClayMoldBlock.FILL) != ClayMoldBlock.Fill.EMPTY || !m.casts(mold.shape)) continue;
                int need = mold.shape.units * 4;
                // the liquid metal joined to this channel, nearest first
                List<Integer> order = new ArrayList<>();
                boolean[] seen = new boolean[net.size()];
                ArrayDeque<Integer> queue = new ArrayDeque<>();
                queue.add(i);
                seen[i] = true;
                int have = 0;
                while (!queue.isEmpty()) {
                    int k = queue.poll();
                    order.add(k);
                    have += amount[k];
                    for (int j : net.adj.get(k)) {
                        if (seen[j] || solid[j] || amount[j] == 0 || metal[j] != m) continue;
                        seen[j] = true;
                        queue.add(j);
                    }
                }
                int furnace = pour == m && seen[0] ? melt.pourable() : 0;
                if (have + furnace < need) continue;
                int left = need;
                for (int k : order) {
                    int t = Math.min(left, amount[k]);
                    amount[k] -= t;
                    left -= t;
                    if (left == 0) break;
                }
                if (left > 0 && !melt.pour(left)) throw new IllegalStateException("furnace short of metal it reported"); // pourable() said it had it
                level.setBlock(mp, ms.setValue(ClayMoldBlock.FILL, ClayMoldBlock.Fill.MOLTEN).setValue(ClayMoldBlock.METAL, m), Block.UPDATE_ALL);
                level.scheduleTick(mp, mold, ClayMoldBlock.SET_TICKS);
                level.playSound(null, mp, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
                level.sendParticles(ParticleTypes.LAVA, mp.getX() + 0.5, mp.getY() + 0.25, mp.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.0);
                Smelting.award(level, mp, Smelting.TAPPED_ADVANCEMENT, 16);
                return left > 0;
            }
        }
        return false;
    }

    /**
     * An empty Crucible used on a channel: scoops whole units (up to a crucible's 10) of the metal nearest the channel
     * out of its network, molten metal into a Crucible of Molten Metal, set metal into a Crucible of Solid Metal (melt it
     * again in a kiln). Empty when there isn't a whole unit.
     */
    static ItemStack scoop(ServerLevel level, BlockPos clicked) {
        long now = level.getGameTime();
        Net net = explore(level, clicked);
        Metal m = null;
        boolean solid = false;
        for (ClayChannelBlockEntity c : net.channels) {
            if (c.metal() != null) {
                m = c.metal();
                solid = c.solid(now);
                break;
            }
        }
        if (m == null) return ItemStack.EMPTY;
        int total = 0;
        for (ClayChannelBlockEntity c : net.channels) if (c.metal() == m && c.solid(now) == solid) total += c.amount();
        int units = Math.min(CrucibleContents.CAPACITY, total / 4);
        if (units == 0) return ItemStack.EMPTY;
        int left = units * 4;
        for (ClayChannelBlockEntity c : net.channels) {
            if (c.metal() != m || c.solid(now) != solid) continue;
            int t = Math.min(left, c.amount());
            c.set(m, c.amount() - t, solid ? now - ClayChannelBlockEntity.SET_TICKS : now); // what is left of set metal stays set
            left -= t;
            if (left == 0) break;
        }
        CrucibleContents contents = CrucibleContents.EMPTY.add(m, units);
        if (!solid) return CrucibleItem.molten(contents, now);
        ItemStack out = new ItemStack(Casting.SOLID_CRUCIBLE.get());
        out.set(Casting.CRUCIBLE_CONTENTS.get(), contents);
        return out;
    }

    private ChannelFlow() {}
}
