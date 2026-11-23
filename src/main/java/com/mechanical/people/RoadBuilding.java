package com.mechanical.people;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import com.mechanical.Coins;
import com.mechanical.EarthBlocks;
import com.mechanical.Mechanical;
import com.mechanical.TrampledPathBlock;
import com.mechanical.TrampledPaths;
import com.mechanical.worldgen.ModBiomes;
import com.mechanical.worldgen.SmoothGround;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Roads a settled NPC tribe builds for a player (docs/living-world-plan.md A5-A9).
 * <ul>
 * <li>A5: at Friendly or better their leader or trader (TribeDialog) hands out a {@link RoadMarkerItem}; set down at most
 * {@link #MAX_LEN} blocks from their camp it becomes a {@link Road}. Up to {@link #FREE_LEN} blocks it is free, longer costs
 * Silver Coins ({@link #price}). Two of their folk walk out (RoadBuildGoal) and lay a winding earth path 2-3 wide (see buildCell)
 * cell by cell. The route is planned a section at a time ({@link #plan}, A* over the ground round the frontier, slopes and
 * water cost more, anyone's build, trees and unloaded ground are off limits), so only blocks that are natural ground,
 * plants or water are ever dug, filled or replaced. Water is crossed on a plank causeway only when there is no way round.</li>
 * <li>A6: at {@link #TRADE_REP} or better a finished path can be widened into a trade road, same way: a worn track 3-4 wide in the local ground, with wheel ruts.</li>
 * <li>A7: the road never steps more than 1 block from cell to cell (cut and fill), and every 1-block step gets a slab of the
 * road's own ground (Dirt Path Slab, Sand Slab, ...).</li>
 * <li>A8/A9: the chunks round each walker (builders and, on a finished trade road, its traders) and round the frontier are
 * force-loaded ({@link #WINDOW}), a moving window, not the whole road. A tribe with a finished trade road keeps its camp
 * (3x3 chunks) loaded ({@link #BASE}).</li>
 * </ul>
 * Roads are saved (SavedData mechanical:roads); a finished road stays on record for the upgrade and A8.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class RoadBuilding extends SavedData {
    public static final int MAX_LEN = 1000, FREE_LEN = 500, BLOCKS_PER_SILVER = 50, TRADE_REP = 60;
    public static final int PATH = 0, TRADE = 1;
    /** How far from the frontier one section is planned (it fits in the frontier's 3x3 chunks), builders per road, camp edge. */
    static final int PLAN_R = 16, BUILDERS = 2, START_OUT = Settlements.PALISADE_R + 3, HOME_R = 24, LOST_CHECKS = 30;
    static final TicketController WINDOW = new TicketController(Identifier.fromNamespaceAndPath(Mechanical.MODID, "road_window"), RoadBuilding::validateWindow);
    static final TicketController BASE = new TicketController(Identifier.fromNamespaceAndPath(Mechanical.MODID, "road_base"), RoadBuilding::validateBase);

    /** One road: from the camp at {@link #base} to the marker at {@link #end} (the end also owns its window tickets). */
    public static final class Road {
        public final UUID tribe, owner;
        public final BlockPos base, end;
        public final int kind;
        /** Centre cells in order, y = the road's surface; {@link #built} of them are laid. */
        public final List<Long> cells;
        public int built;
        /** The plan reaches the end. */
        public boolean planned;
        /** Builders, and traders out on a finished trade road: the force-loaded window follows them. */
        public final List<UUID> walkers;
        /** Not saved: chunks this road keeps loaded (refilled from NeoForge's saved tickets), walkers not found lately. */
        final Set<Long> forced = new HashSet<>();
        final Map<UUID, Integer> lost = new HashMap<>();
        final Map<UUID, Long> lastChunk = new HashMap<>();

        Road(UUID tribe, UUID owner, BlockPos base, BlockPos end, int kind, List<Long> cells, int built, boolean planned, List<UUID> walkers) {
            this.tribe = tribe;
            this.owner = owner;
            this.base = base;
            this.end = end;
            this.kind = kind;
            this.cells = new ArrayList<>(cells);
            this.built = built;
            this.planned = planned;
            this.walkers = new ArrayList<>(walkers);
        }

        public boolean done() {
            return planned && built >= cells.size();
        }

        public BlockPos cell(int i) {
            return BlockPos.of(cells.get(i));
        }

        /** Where the road leaves the camp: {@link #START_OUT} blocks from its centre toward the end. */
        BlockPos start() {
            double dx = end.getX() - base.getX(), dz = end.getZ() - base.getZ(), d = Math.max(1, Math.sqrt(dx * dx + dz * dz));
            double out = Math.min(START_OUT, d / 2);
            return new BlockPos((int) Math.floor(base.getX() + 0.5 + dx / d * out), base.getY(), (int) Math.floor(base.getZ() + 0.5 + dz / d * out));
        }

        /** The next cell to lay, or the start before any is planned. */
        BlockPos frontier() {
            return cells.isEmpty() ? start() : cell(Math.min(built, cells.size() - 1));
        }

        static final Codec<Road> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("tribe").forGetter(r -> r.tribe),
                UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(r -> r.owner),
                BlockPos.CODEC.fieldOf("base").forGetter(r -> r.base),
                BlockPos.CODEC.fieldOf("end").forGetter(r -> r.end),
                Codec.INT.fieldOf("kind").forGetter(r -> r.kind),
                Codec.LONG.listOf().fieldOf("cells").forGetter(r -> r.cells),
                Codec.INT.fieldOf("built").forGetter(r -> r.built),
                Codec.BOOL.fieldOf("planned").forGetter(r -> r.planned),
                UUIDUtil.STRING_CODEC.listOf().fieldOf("walkers").forGetter(r -> r.walkers)).apply(i, Road::new));
    }

    private static final Codec<RoadBuilding> CODEC = Road.CODEC.listOf().fieldOf("roads").codec().xmap(RoadBuilding::new, d -> d.roads);
    private static final SavedDataType<RoadBuilding> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "roads"),
            () -> new RoadBuilding(List.of()), CODEC);

    public final List<Road> roads;
    /** Camps kept loaded for a trade road (A8). Not saved: refilled from the tickets on load. */
    private final Set<BlockPos> bases = new HashSet<>();

    private RoadBuilding(List<Road> roads) {
        this.roads = new ArrayList<>(roads);
    }

    /** Always the overworld's (tribes live there). */
    public static RoadBuilding get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The road this tribe is building now, or null. */
    public Road building(UUID tribe) {
        for (Road r : roads) if (r.tribe.equals(tribe) && !r.done()) return r;
        return null;
    }

    /** The road this tribesperson walks for (builder or trader), or null. */
    Road roadOf(UUID walker) {
        for (Road r : roads) if (r.walkers.contains(walker)) return r;
        return null;
    }

    public Road add(Road r) {
        roads.add(r);
        setDirty();
        return r;
    }

    /** Drops a road and lets go of its chunks (tests; a road whose route is blocked). */
    public void remove(ServerLevel level, Road r) {
        roads.remove(r);
        for (long c : r.forced) WINDOW.forceChunk(level, r.end, ChunkPos.getX(c), ChunkPos.getZ(c), false, false);
        r.forced.clear();
        setDirty();
        updateBases(level);
    }

    // ---------------------------------------------------------------- price (A5/A6, decided 2026-11-22)

    /**
     * Silver Coins a road this long costs: free up to {@link #FREE_LEN} blocks, then 1 Silver per started
     * {@link #BLOCKS_PER_SILVER} blocks (10 at the most), twice that for a trade road; -1 past {@link #MAX_LEN}.
     */
    public static int price(double length, int kind) {
        if (length > MAX_LEN) return -1;
        if (length <= FREE_LEN) return 0;
        return (int) Math.ceil((length - FREE_LEN) / BLOCKS_PER_SILVER) * (kind == TRADE ? 2 : 1);
    }

    static double length(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Takes {@code silver} Silver Coins from the player, a Gold Coin counting 10 (change back in silver). False if short. */
    public static boolean pay(Player player, int silver) {
        if (silver <= 0 || player.isCreative()) return true;
        Inventory inv = player.getInventory();
        int s = inv.countItem(Coins.SILVER_COIN.get()), g = inv.countItem(Coins.GOLD_COIN.get());
        if (s + g * Coins.GOLD_IN_SILVER < silver) return false;
        int fromSilver = Math.min(s, silver), rest = silver - fromSilver, gold = (rest + Coins.GOLD_IN_SILVER - 1) / Coins.GOLD_IN_SILVER;
        take(inv, Coins.SILVER_COIN.get(), fromSilver);
        take(inv, Coins.GOLD_COIN.get(), gold);
        int change = gold * Coins.GOLD_IN_SILVER - rest;
        if (change > 0) player.getInventory().placeItemBackInInventory(new ItemStack(Coins.SILVER_COIN.get(), change), net.minecraft.util.Prediction.SERVER_ONLY);
        return true;
    }

    private static void take(Inventory inv, Item item, int count) {
        for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (!st.is(item)) continue;
            int n = Math.min(count, st.getCount());
            st.shrink(n);
            count -= n;
        }
    }

    // ---------------------------------------------------------------- talking (TribeDialog)

    /** Road buttons for this tribe's envoy: a road at Friendly (none under way), the trade road at TRADE_REP on a finished path of the player's. */
    static int dialogButtons(ServerPlayer player, Tribe tribe, ServerLevel level) {
        if (Neighbours.get(level).settlement(tribe.id) == null) return 0;
        RoadBuilding data = get(level.getServer());
        if (data.building(tribe.id) != null) return 0;
        int rep = tribe.reputation(player), bits = 0;
        if (rep >= Tribe.FRIENDLY) bits |= TribeDialog.ROAD;
        if (rep >= TRADE_REP && data.pathOf(level.getServer(), tribe.id, player) != null) bits |= TribeDialog.UPGRADE;
        return bits;
    }

    /** A finished path of this tribe's built for the player or someone of their player tribe. */
    Road pathOf(MinecraftServer server, UUID tribe, Player player) {
        var clans = com.mechanical.clan.Clans.get(server);
        var clan = clans.clanOf(player.getUUID());
        for (Road r : roads)
            if (r.tribe.equals(tribe) && r.kind == PATH && r.done() && (r.owner.equals(player.getUUID()) || clan != null && clans.clanOf(r.owner) == clan)) return r;
        return null;
    }

    /** "Build us a road": the player gets a Road Marker for this tribe. */
    static void askRoad(ServerPlayer player, Tribesperson envoy) {
        Tribe tribe = envoy.tribe();
        ServerLevel level = (ServerLevel) envoy.level();
        Neighbours.Settlement s = Neighbours.get(level).settlement(tribe.id);
        if (s == null || get(level.getServer()).building(tribe.id) != null) return;
        player.getInventory().placeItemBackInInventory(RoadMarkerItem.of(tribe, s.centre), net.minecraft.util.Prediction.SERVER_ONLY);
        envoy.say(player, Component.translatable("chat.mechanical.road.marker", MAX_LEN, FREE_LEN, BLOCKS_PER_SILVER));
    }

    /** "Make it a trade road": paid now (price by the road's length, doubled), then they rebuild it as a wide, worn track. */
    static void askUpgrade(ServerPlayer player, Tribesperson envoy) {
        Tribe tribe = envoy.tribe();
        ServerLevel level = (ServerLevel) envoy.level();
        RoadBuilding data = get(level.getServer());
        Road path = data.pathOf(level.getServer(), tribe.id, player);
        if (path == null || data.building(tribe.id) != null || tribe.reputation(player) < TRADE_REP) return;
        int cost = price(length(path.base, path.end), TRADE);
        if (!pay(player, cost)) {
            envoy.say(player, Component.translatable("chat.mechanical.road.need_silver", cost));
            return;
        }
        data.roads.remove(path);
        Road trade = data.add(new Road(path.tribe, path.owner, path.base, path.end, TRADE, path.cells, 0, true, List.of()));
        trade.forced.addAll(path.forced); // same end, same ticket owner: the new road lets go of what the old one held
        envoy.say(player, Component.translatable("chat.mechanical.road.upgrade"));
        TribeVisits.award(player, "tribe_trade_road");
    }

    // ---------------------------------------------------------------- the route (A* round the frontier)

    /** A column the road may use: its top block's y, whether that top is water, and an extra cost that makes routes meander ({@link #plan}). */
    record Col(int y, boolean water, double wander) {}

    /** Ground a road may be laid on, dug or filled: earth, sand, gravel, stone, snow, peat, loess, trails, ground slabs. Anything else is someone's. */
    static boolean natural(BlockState s) {
        return SmoothGround.ground(s) || s.getBlock() instanceof TrampledPathBlock || s.is(Blocks.DIRT_PATH) || s.is(Blocks.COARSE_DIRT) || groundStep(s);
    }

    /** Plants, snow layers, leaves, air: cleared off the road freely. */
    static boolean clearable(BlockState s) {
        return s.isAir() || s.is(BlockTags.LEAVES) || s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    /** The column at x,z, or null if a road may not go there (a build, a tree, lava, ice, or not loaded). */
    static Col column(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
        BlockState s = level.getBlockState(p);
        while (s.is(Blocks.SNOW) && p.getY() > level.getMinY()) s = level.getBlockState(p = p.below());
        if (level.getFluidState(p).is(FluidTags.WATER)) return new Col(p.getY(), true, 0);
        return natural(s) ? new Col(p.getY(), false, 0) : null;
    }

    /** Step cost between two columns: flat 1, a 1-block step a little more, anything steeper much more (it means digging); water 12; plus the meander cost. */
    static double cost(Col a, Col b) {
        int dh = Math.abs(a.y - b.y);
        return 1 + (dh <= 1 ? dh * 0.5 : 2 + (dh - 1) * 4) + (b.water ? 12 : 0) + b.wander;
    }

    /**
     * A* from {@code from} toward {@code to} over the columns in {@code box} (x/z only), 4-connected: the path to {@code to}
     * if it lies in the box and can be reached, else to the reachable column closest to it. Columns as BlockPos (y =
     * ground), from first; just {@code from} if nothing better is reachable, null if {@code from} itself is unusable.
     */
    public static List<BlockPos> plan(ServerLevel level, BlockPos from, BlockPos to, int minX, int minZ, int maxX, int maxZ) {
        double len = Math.max(1, Math.sqrt(from.distSqr(new BlockPos(to.getX(), from.getY(), to.getZ())))), dx = (to.getX() - from.getX()) / len,
                dz = (to.getZ() - from.getZ()) / len;
        Map<Long, Col> cols = new HashMap<>();
        java.util.function.LongFunction<Col> col = k -> cols.computeIfAbsent(k, kk -> {
            int x = BlockPos.getX(kk), z = BlockPos.getZ(kk);
            if (x < minX || x > maxX || z < minZ || z > maxZ) return null;
            Col c = column(level, x, z);
            return c == null ? null : new Col(c.y, c.water, meander(x, z, to, dx, dz));
        });
        long start = BlockPos.asLong(from.getX(), 0, from.getZ()), goal = BlockPos.asLong(to.getX(), 0, to.getZ());
        if (col.apply(start) == null) return null;
        Map<Long, Double> g = new HashMap<>();
        Map<Long, Long> came = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        g.put(start, 0.0);
        open.add(new Node(h(start, goal), start));
        long best = start;
        double bestH = h(start, goal);
        Set<Long> closed = new HashSet<>();
        while (!open.isEmpty()) {
            long cur = open.poll().pos;
            if (!closed.add(cur)) continue;
            double hc = h(cur, goal);
            if (hc < bestH) {
                bestH = hc;
                best = cur;
            }
            if (cur == goal) break;
            Col c = col.apply(cur);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                long n = BlockPos.asLong(BlockPos.getX(cur) + d.getStepX(), 0, BlockPos.getZ(cur) + d.getStepZ());
                Col nc = col.apply(n);
                if (nc == null || closed.contains(n)) continue;
                double ng = g.get(cur) + cost(c, nc);
                if (ng < g.getOrDefault(n, Double.MAX_VALUE)) {
                    g.put(n, ng);
                    came.put(n, cur);
                    open.add(new Node(ng + h(n, goal), n));
                }
            }
        }
        List<BlockPos> path = new ArrayList<>();
        for (Long k = best; k != null; k = came.get(k)) path.add(new BlockPos(BlockPos.getX(k), cols.get(k).y, BlockPos.getZ(k)));
        java.util.Collections.reverse(path);
        return path;
    }

    private record Node(double f, long pos) {}

    /**
     * The meander cost of a column: how far it lies off a gently winding line toward {@code to} (up to {@link #MEANDER}
     * blocks either side of the straight line, coming back to it near the end). Keyed to the distance from the end, so
     * sections planned one after another follow the same bends. Even on flat land a road bends like a trodden track.
     */
    static double meander(int x, int z, BlockPos to, double dx, double dz) {
        double rx = x - to.getX(), rz = z - to.getZ(), along = -(rx * dx + rz * dz), side = rx * -dz + rz * dx;
        double want = (smooth((int) Math.floor(along), 0, 11, 16) - 0.5) * 2.6 * MEANDER * Math.min(1, along / 12);
        return Math.abs(side - want) * 0.35;
    }

    static final int MEANDER = 4;

    private static double h(long a, long b) {
        return Math.abs(BlockPos.getX(a) - BlockPos.getX(b)) + Math.abs(BlockPos.getZ(a) - BlockPos.getZ(b));
    }

    /**
     * Appends planned columns to the road, each surface at most 1 block above or below the one before (A7: the road cuts
     * into rises and is filled up over dips, so a step is never a cliff).
     */
    static void append(Road r, List<BlockPos> path) {
        for (BlockPos p : path) {
            int y = p.getY();
            if (!r.cells.isEmpty()) {
                BlockPos last = r.cell(r.cells.size() - 1);
                if (last.getX() == p.getX() && last.getZ() == p.getZ()) continue;
                y = Math.clamp(y, last.getY() - 1, last.getY() + 1);
            }
            r.cells.add(BlockPos.asLong(p.getX(), y, p.getZ()));
        }
    }

    /** Plans the next section from the frontier (once its ground is loaded). False if the way is blocked. */
    boolean planNext(ServerLevel level, Road r) {
        BlockPos from = r.cells.isEmpty() ? r.start() : r.cell(r.cells.size() - 1);
        if (!level.hasChunksAt(from.offset(-PLAN_R, 0, -PLAN_R), from.offset(PLAN_R, 0, PLAN_R))) return true; // wait for the window
        List<BlockPos> path = plan(level, from, r.end, from.getX() - PLAN_R, from.getZ() - PLAN_R, from.getX() + PLAN_R, from.getZ() + PLAN_R);
        if (path == null || path.size() < 2) {
            if (path != null && reached(path.getFirst(), r.end)) r.planned = true;
            return r.planned;
        }
        append(r, path);
        if (reached(r.cell(r.cells.size() - 1), r.end)) r.planned = true;
        setDirty();
        return true;
    }

    private static boolean reached(BlockPos p, BlockPos end) {
        return Math.abs(p.getX() - end.getX()) + Math.abs(p.getZ() - end.getZ()) <= 1;
    }

    // ---------------------------------------------------------------- laying a cell

    private static Direction dir(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        return Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /**
     * Lays cell {@code i}. Path: 2 lanes; trade road: 3. Where a smooth noise says so, one more lane on one side (laid only
     * where the ground is level with the road already), so the width wobbles and the edges are ragged. The middle is worn
     * most, patches come in blobs, the outer lanes fray into the grass round it (Worn Grass, the odd tuft or flower), and a
     * trade road has stretches of two wheel tracks with grass between. A cell the road steps up onto gets a slab of its own ground (a half
     * step each side), and the bank just outside the road is softened to a half step (a slab of its ground).
     */
    public static void buildCell(ServerLevel level, Road r, int i) {
        BlockPos c = r.cell(i);
        BlockPos prev = i > 0 ? r.cell(i - 1) : null, next = i + 1 < r.cells.size() ? r.cell(i + 1) : null;
        Direction along = prev != null ? dir(prev, c) : next != null ? dir(c, next) : Direction.NORTH;
        boolean step = prev != null && prev.getY() == c.getY() - 1 || next != null && next.getY() == c.getY() - 1;
        Palette pal = palette(level, c);
        Direction side = along.getClockWise();
        boolean trade = r.kind == TRADE;
        int lo = trade ? -1 : 0, hi = 1;
        double wobble = smooth(c.getX(), c.getZ(), 1, 7);
        boolean ruts = pal.dashes.length > 0 || pal.cold || trade && smooth(c.getX(), c.getZ(), 2, 8) > 0.5; // where the tracks are what shows, always
        int left = lo, right = hi; // the lanes actually laid
        for (int lane = lo - 1; lane <= hi + 1; lane++) {
            boolean extra = lane < lo || lane > hi;
            if (lane < lo && wobble > 0.35 || lane > hi && wobble < 0.65) continue;
            int wear = extra ? 2 : lane == 0 ? 0 : 1;
            if (lay(level, r, c.relative(side, lane), step, pal, wear, ruts, side) && extra) {
                left = Math.min(left, lane);
                right = Math.max(right, lane);
            }
        }
        soften(level, c.relative(side, left - 1), side.getOpposite(), c.getY());
        soften(level, c.relative(side, right + 1), side, c.getY());
        trimTall(level, c.relative(side, left - 1));
        trimTall(level, c.relative(side, right + 1));
    }

    /** No tall grass or large fern right beside the track (it would hide it): short grass or a fern instead. */
    private static void trimTall(ServerLevel level, BlockPos at) {
        if (!level.hasChunkAt(at)) return;
        BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        BlockState s = level.getBlockState(p);
        if (!s.is(Blocks.TALL_GRASS) && !s.is(Blocks.LARGE_FERN)) return;
        set(level, p.above(), Blocks.AIR.defaultBlockState());
        set(level, p, (s.is(Blocks.TALL_GRASS) ? Blocks.SHORT_GRASS : Blocks.FERN).defaultBlockState());
    }

    /** Ground slabs and stairs (EarthBlocks, laid by roads and by worldgen.SmoothGround on 1-block steps): natural ground too. */
    static boolean groundStep(BlockState s) {
        return s.is(EarthBlocks.DIRT_PATH_SLAB) || s.is(EarthBlocks.DIRT_SLAB) || s.is(EarthBlocks.GRASS_SLAB) || s.is(EarthBlocks.COARSE_DIRT_SLAB)
                || s.is(EarthBlocks.PODZOL_SLAB) || s.is(EarthBlocks.SAND_SLAB) || s.is(EarthBlocks.RED_SAND_SLAB) || s.is(EarthBlocks.GRAVEL_SLAB)
                || s.is(EarthBlocks.MUD_SLAB) || s.is(EarthBlocks.PEAT_SLAB) || s.is(EarthBlocks.LOESS_SLAB) || s.is(EarthBlocks.MOSS_SLAB)
                || s.is(EarthBlocks.DIRT_STAIRS) || s.is(EarthBlocks.GRASS_STAIRS) || s.is(EarthBlocks.COARSE_DIRT_STAIRS)
                || s.is(EarthBlocks.SAND_STAIRS) || s.is(EarthBlocks.RED_SAND_STAIRS) || s.getBlock() == SmoothGround.slab(chalk());
    }

    /** What one of their roads is made of (an upgrade may replace it); the stone and spruce of roads built before v0.179 too. */
    static boolean roadBlock(BlockState s) {
        return natural(s) || s.is(Blocks.PACKED_MUD) || s.is(Blocks.OAK_PLANKS) || s.is(Blocks.SPRUCE_PLANKS) || s.is(Blocks.OAK_LOG) || s.is(Blocks.SPRUCE_LOG)
                || s.is(Blocks.COBBLESTONE) || s.is(Blocks.STONE_BRICKS) || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE_STAIRS)
                || s.is(Blocks.MOSSY_COBBLESTONE_SLAB) || s.is(Blocks.STONE_BRICK_STAIRS) || s.is(Blocks.STONE_BRICK_SLAB);
    }

    /** Earth that may show through at a road's edge: natural, a full block, not rock (no stone on the road), not water. */
    private static boolean soil(BlockState s, ServerLevel level, BlockPos p) {
        return natural(s) && !groundStep(s) && !s.is(BlockTags.BASE_STONE_OVERWORLD) && !s.is(Blocks.SANDSTONE) && !s.is(Blocks.RED_SANDSTONE)
                && level.getFluidState(p).isEmpty() && s.isCollisionShapeFullBlock(level, p);
    }

    /**
     * One lane column: dig down or fill up to the road's height, then the surface and 2 blocks of headroom. Never touches a
     * build. {@code wear} 0 is the worn middle, 1 an outer lane, 2 the ragged extra lane (laid only where the ground is level
     * with the road already). Over water: planks (path) or a causeway of logs laid across (trade road). True if laid.
     */
    private static boolean lay(ServerLevel level, Road r, BlockPos at, boolean step, Palette pal, int wear, boolean ruts, Direction side) {
        int y = at.getY(), x = at.getX(), z = at.getZ();
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
        boolean snowed = false;
        while (level.getBlockState(top).is(Blocks.SNOW) && top.getY() > y) {
            top = top.below();
            snowed = true;
        }
        BlockState local = level.getBlockState(top);
        boolean localWet = level.getFluidState(top).is(FluidTags.WATER);
        if (wear == 2 && (top.getY() != y || localWet || !natural(local))) return false;
        boolean upgrade = r.kind == TRADE;
        java.util.function.Predicate<BlockState> ours = s -> upgrade ? roadBlock(s) : natural(s);
        // check the whole column first: anything someone built, and the lane is left as it is
        for (int yy = Math.max(top.getY(), y + 2); yy >= Math.min(top.getY(), y); yy--) {
            BlockState s = level.getBlockState(new BlockPos(x, yy, z));
            if (!clearable(s) && !ours.test(s) && !s.getFluidState().is(FluidTags.WATER)) return false;
        }
        boolean localOk = soil(local, level, top);
        for (int yy = top.getY(); yy > y; yy--) set(level, new BlockPos(x, yy, z), Blocks.AIR.defaultBlockState());
        for (int yy = top.getY() + 1; yy < y; yy++) set(level, new BlockPos(x, yy, z), pal.fill);
        BlockPos surfacePos = new BlockPos(x, y, z);
        BlockState surface;
        boolean grassy = false;
        if (level.getFluidState(surfacePos).is(FluidTags.WATER)) {
            surface = upgrade ? pal.log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, side.getAxis()) : pal.planks.defaultBlockState();
        } else {
            BlockState pick = pick(pal, x, z, wear, ruts, localOk ? local : null);
            grassy = pick.is(Blocks.GRASS_BLOCK) || pick.is(TrampledPaths.WORN_GRASS);
            surface = step ? slab(pick) : pick;
        }
        set(level, surfacePos, surface);
        for (int k = 1; k <= 2; k++) {
            BlockPos p = surfacePos.above(k);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && (clearable(s) || ours.test(s))) set(level, p, Blocks.AIR.defaultBlockState());
        }
        if (surface.getBlock() instanceof TrampledPathBlock) set(level, surfacePos, TrampledPathBlock.connect(surface, level, surfacePos));
        if (!step && !surface.is(pal.planks) && !surface.is(pal.log)) {
            double h = hash01(x, z, 9);
            BlockState cover = null;
            // snowy land: trodden snow, a thin layer over the track, the earth showing through in broken wheel tracks
            // (a footpath: down its middle) and a few worn patches
            boolean bare = wear == 1 && ruts && hash01(x, z, 14) < 0.55 || wear == 0 && !ruts && hash01(x, z, 14) < 0.4 || smooth(x, z, 13, 4) > 0.72;
            if (pal.cold) cover = bare ? null : Blocks.SNOW.defaultBlockState();
            else if (grassy) cover = h < 0.03 ? Blocks.DANDELION.defaultBlockState() : h < 0.05 ? Blocks.POPPY.defaultBlockState()
                    : h < 0.32 ? Blocks.SHORT_GRASS.defaultBlockState() : null;
            if (cover != null && cover.canSurvive(level, surfacePos.above())) set(level, surfacePos.above(), cover);
        }
        return true;
    }

    /**
     * The block for one lane column, all by smooth noise so the track is mottled, never striped. Outer lanes fray into
     * {@code local} (the earth that was there, or null; grass frays as Worn Grass or grass), the extra lane mostly. On a
     * trade road's rutted stretches the middle is a hump of the land's own ground (grass) between two bare wheel tracks.
     * Elsewhere grass creeps into the track in blobs, and the bare earth is mottled from the palette with a few specks.
     */
    private static BlockState pick(Palette p, int x, int z, int wear, boolean ruts, BlockState local) {
        boolean grass = local != null && local.is(Blocks.GRASS_BLOCK);
        BlockState worn = TrampledPaths.WORN_GRASS.get().defaultBlockState().setValue(TrampledPathBlock.PERSISTENT, true);
        double fray = smooth(x, z, 7, 3) * 0.75 + hash01(x, z, 8) * 0.25, roll = hash01(x, z, 3);
        if (local != null && (wear == 1 && grass && fray > 0.56 || wear == 2 && fray > 0.3)) return grass && fray < (wear == 2 ? 0.6 : 0.78) ? worn : local;
        if (ruts && wear == 0 && local != null) return grass && roll < 0.65 ? worn : local; // the hump between two wheel tracks
        if (grass && smooth(x, z, 12, 4) > 0.66) return roll < 0.6 ? worn : local;
        if (ruts && wear == 1 && p.dashes.length > 0 && hash01(x, z, 14) < 0.5) return p.dashes[(int) (hash01(x, z, 15) * p.dashes.length)];
        if (hash01(x, z, 4) < p.speckRate) return p.specks[(int) (hash01(x, z, 16) * p.specks.length)];
        double v = Math.clamp((smooth(x, z, 6, 3.5) - 0.5) * 2.8 + (hash01(x, z, 10) - 0.5) * 0.45 + 0.5, 0, 0.999);
        return p.earth[(int) (v * p.earth.length)];
    }

    /**
     * The column just outside the road: a bank 1-3 blocks above it is cut down to a slab of its own ground a half step above
     * the road, and the next column {@code out} down to a full block above the road, so a cut climbs in half steps instead
     * of standing as a wall; where the road stands a block above the land, a slab on the land makes a half step down.
     * Natural ground only.
     */
    private static void soften(ServerLevel level, BlockPos at, Direction out, int y) {
        if (cutDown(level, at, y, true)) cutDown(level, at.relative(out), y, false);
    }

    /** Lowers a bank column 1-3 blocks above road height {@code y}: to a slab at y+1, or a full block of its top at y+1. True if it was a bank. */
    private static boolean cutDown(ServerLevel level, BlockPos at, int y, boolean slabbed) {
        int x = at.getX(), z = at.getZ();
        if (!level.hasChunkAt(at)) return false;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
        if (level.getBlockState(top).is(Blocks.SNOW) || level.getBlockState(top.above()).is(Blocks.SNOW)) return false; // snow stays as it lies
        BlockState t = level.getBlockState(top);
        if (!soil(t, level, top) || !clearable(level.getBlockState(top.above())) || !level.getFluidState(top.above()).isEmpty()) return false;
        int h = top.getY() - y;
        BlockState slab = slab(t);
        if (slabbed && !slab.is(SmoothGround.slab(t.getBlock()))) return false; // no slab of its own: leave it
        if (h >= (slabbed ? 1 : 2) && h <= 3) {
            for (int yy = top.getY() - 1; yy > y + 1; yy--) if (!natural(level.getBlockState(new BlockPos(x, yy, z)))) return false;
            for (int yy = top.getY() + 1; yy > y + 1; yy--) set(level, new BlockPos(x, yy, z), Blocks.AIR.defaultBlockState());
            set(level, new BlockPos(x, y + 1, z), slabbed ? slab : t);
            return h >= 2;
        }
        if (slabbed && h == -1) set(level, top.above(), slab);
        return false;
    }

    /** A step's surface: the slab of that ground (the mod's ground slabs, chalk_slab, ...), Coarse Dirt Slab when it has none. */
    private static BlockState slab(BlockState of) {
        Block s = SmoothGround.slab(of.getBlock());
        if (!groundStep(s.defaultBlockState())) s = EarthBlocks.COARSE_DIRT_SLAB.get(); // no rock slabs: roads are earth
        return s.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    /**
     * A road's ground in one place, so it blends with the land round it: {@code earth}, the full-height earth blocks the
     * track is mottled with by smooth noise (no Dirt Path: a lower block among full ones gets dark shading at every edge),
     * {@code specks} scattered singly at {@code speckRate}, {@code dashes} for broken wheel tracks (or none), {@code fill} under it, the local wood for crossings, and whether snow lies.
     */
    record Palette(BlockState[] earth, BlockState[] specks, double speckRate, BlockState[] dashes, BlockState fill, Block planks, Block log, boolean cold) {}

    private static final int PLAIN = 0, RED = 1, SAND = 2, WET = 3, PODZOL = 4, SNOW = 5;
    private static final ResourceKey<Biome> PEAT_BOG = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mechanical.MODID, "peat_bog"));

    private static Block chalk() {
        return com.mechanical.StoneFamilies.block("chalk_block");
    }

    /**
     * The palette for cell {@code c}, from the ground 4 blocks out on each side (the most common kind wins over grass, ties
     * too) and, where that is grass all round, the biome: sand on sand (deserts, beaches), red sand on red sand and
     * terracotta (badlands), mud and packed mud in swamps and bogs, gravel in snow, podzol in taiga, else packed mud, rooted
     * dirt and coarse dirt. Loess (Mammoth Steppe), Chalk Block (Chalk Downs) and Peat (Peat Bog)
     * join in where they are the land's own ground.
     */
    static Palette palette(ServerLevel level, BlockPos c) {
        int[] votes = new int[6]; // the ground 4 out on each side: plain earth or grass, red sand, sand, mud, podzol, snow
        Block extra = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = c.relative(d, 4);
            if (!level.hasChunkAt(p)) continue;
            p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p).below();
            BlockState s = level.getBlockState(p);
            boolean snowed = s.is(Blocks.SNOW);
            if (snowed) s = level.getBlockState(p.below());
            votes[snowed || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW) ? SNOW : s.is(Blocks.RED_SAND) || s.is(BlockTags.TERRACOTTA) ? RED
                    : s.is(BlockTags.SAND) || s.is(Blocks.SANDSTONE) ? SAND : s.is(Blocks.MUD) || s.is(Blocks.MUDDY_MANGROVE_ROOTS) || s.is(EarthBlocks.PEAT) ? WET
                    : s.is(Blocks.PODZOL) ? PODZOL : PLAIN]++;
            if (s.is(EarthBlocks.PEAT) || s.is(EarthBlocks.LOESS) || s.is(chalk())) extra = s.getBlock();
        }
        int ground = PLAIN, best = 0;
        for (int k = RED; k <= SNOW; k++)
            if (votes[k] > best && votes[k] >= votes[PLAIN]) {
                ground = k;
                best = votes[k];
            }
        Holder<Biome> b = level.getBiome(c);
        if (b.is(ModBiomes.MAMMOTH_STEPPE)) extra = EarthBlocks.LOESS.get();
        else if (b.is(ModBiomes.CHALK_DOWNS)) extra = chalk(); // white: only as specks
        else if (b.is(PEAT_BOG)) extra = EarthBlocks.PEAT.get();
        // grass all round: the biome tells a swamp, snowy land or taiga (not sand: grassy ground is no desert track)
        if (ground == PLAIN) ground = b.is(Tags.Biomes.IS_SWAMP) || b.is(PEAT_BOG) ? WET : b.value().coldEnoughToSnow(c, level.getSeaLevel()) ? SNOW
                : b.is(BiomeTags.IS_TAIGA) ? PODZOL : PLAIN;
        BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState(), gravel = Blocks.GRAVEL.defaultBlockState(), packed = Blocks.PACKED_MUD.defaultBlockState(),
                rooted = Blocks.ROOTED_DIRT.defaultBlockState();
        BlockState fill = Blocks.DIRT.defaultBlockState();
        BlockState[] earth, specks = {gravel}, dashes = {};
        double speckRate = 0.025;
        boolean conifer = false;
        if (ground == RED) {
            BlockState rs = Blocks.RED_SAND.defaultBlockState();
            earth = new BlockState[] {rs};
            specks = new BlockState[] {coarse, coarse, gravel, packed};
            speckRate = 0.14;
            dashes = new BlockState[] {coarse, coarse, packed};
            fill = rs;
        } else if (ground == SAND) {
            // compacted sand: scattered coarse dirt, gravel and a little packed mud, darker in the two broken wheel tracks
            BlockState sd = Blocks.SAND.defaultBlockState();
            earth = new BlockState[] {sd};
            specks = new BlockState[] {coarse, coarse, gravel, packed};
            speckRate = 0.14;
            dashes = new BlockState[] {coarse, coarse, gravel, packed};
            fill = sd;
        } else if (ground == WET) {
            earth = new BlockState[] {packed, rooted, coarse, packed, rooted}; // no mud: it is near black
            specks = new BlockState[] {rooted};
        } else if (ground == SNOW) {
            earth = new BlockState[] {rooted, coarse, coarse, coarse, rooted};
            specks = new BlockState[] {coarse};
            conifer = true;
        } else if (ground == PODZOL) {
            earth = new BlockState[] {Blocks.PODZOL.defaultBlockState(), coarse, rooted, coarse, packed};
            conifer = true;
        } else {
            earth = new BlockState[] {packed, rooted, coarse, coarse, rooted};
        }
        if (extra == chalk()) {
            specks = new BlockState[] {gravel, extra.defaultBlockState()};
            speckRate = 0.04;
        } else if (extra != null) earth = java.util.stream.Stream.concat(java.util.Arrays.stream(earth), java.util.stream.Stream.of(extra.defaultBlockState()))
                .toArray(BlockState[]::new);
        return new Palette(earth, specks, speckRate, dashes, fill, conifer ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS, conifer ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG, ground == SNOW);
    }

    /** 0..1 from a position. */
    private static double hash01(int x, int z, int salt) {
        return (mix(x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL + salt * 0x632BE59BD9B4E019L) >>> 11) * 0x1.0p-53;
    }

    /** Value noise 0..1, smooth over about {@code scale} blocks: the road's width, runs of ruts, patches and frayed edges. */
    static double smooth(int x, int z, int salt, double scale) {
        double fx = x / scale, fz = z / scale;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = hash01(ix, iz, salt) + (hash01(ix + 1, iz, salt) - hash01(ix, iz, salt)) * tx;
        double b = hash01(ix, iz + 1, salt) + (hash01(ix + 1, iz + 1, salt) - hash01(ix, iz + 1, salt)) * tx;
        return a + (b - a) * tz;
    }

    private static void set(ServerLevel level, BlockPos p, BlockState s) {
        if (level.getBlockState(p) != s) level.setBlock(p, s, Block.UPDATE_ALL);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    // ---------------------------------------------------------------- every second: plans, builders, loaded chunks

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) return;
        RoadBuilding data = get(level.getServer());
        if (data.roads.isEmpty() && data.bases.isEmpty()) return;
        for (Road r : List.copyOf(data.roads)) data.tick(level, r);
        data.updateBases(level);
    }

    void tick(ServerLevel level, Road r) {
        if (!r.done() && r.built >= r.cells.size() && !planNext(level, r)) {
            blocked(level, r);
            return;
        }
        if (!r.done()) findBuilders(level, r);
        else if (r.kind == TRADE) findTraders(level, r);
        for (UUID id : List.copyOf(r.walkers)) {
            Entity e = level.getEntity(id);
            boolean gone = e == null ? r.lost.merge(id, 1, Integer::sum) > LOST_CHECKS : !e.isAlive();
            if (e != null) {
                r.lost.remove(id);
                r.lastChunk.put(id, ChunkPos.pack(e.blockPosition()));
            }
            // a finished road lets its walkers go once they are home (traders when their errand is over)
            boolean home = e instanceof Tribesperson t && r.done() && t.errand == null && t.home() != null && t.blockPosition().closerThan(t.home(), HOME_R);
            if (gone || home) {
                r.walkers.remove(id);
                r.lost.remove(id);
                r.lastChunk.remove(id);
                setDirty();
            }
        }
        updateWindow(level, r);
    }

    /** No way on from the frontier: the road ends where it got to, and the owner hears why. */
    private void blocked(ServerLevel level, Road r) {
        r.planned = true;
        setDirty();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(r.owner);
        if (owner != null) owner.sendSystemMessage(Component.translatable("message.mechanical.road.blocked", Tribe.of(r.tribe).name()));
    }

    /** Up to {@link #BUILDERS} of the tribe's working folk near their camp or the frontier. */
    private void findBuilders(ServerLevel level, Road r) {
        if (r.walkers.size() >= BUILDERS) return;
        for (BlockPos around : List.of(r.base, r.frontier())) {
            for (Tribesperson t : level.getEntitiesOfClass(Tribesperson.class, new AABB(around).inflate(48))) {
                if (r.walkers.size() >= BUILDERS) return;
                if (!r.tribe.equals(t.group()) || !t.isAlive() || r.walkers.contains(t.getUUID()) || t.errand != null || t.visit != null
                        || t.role() == TribeRole.ELDER || t.role().travels() || roadOf(t.getUUID()) != null) continue;
                r.walkers.add(t.getUUID());
                setDirty();
            }
        }
    }

    /** A9: traders setting out from a camp with a trade road are followed by the window until they are back. */
    private void findTraders(ServerLevel level, Road r) {
        if (!level.hasChunkAt(r.base)) return;
        for (Tribesperson t : level.getEntitiesOfClass(Tribesperson.class, new AABB(r.base).inflate(Settlements.PALISADE_R + 16))) {
            if (r.tribe.equals(t.group()) && t.errand != null && !r.walkers.contains(t.getUUID()) && roadOf(t.getUUID()) == null) {
                r.walkers.add(t.getUUID());
                setDirty();
            }
        }
    }

    /** The chunks this road wants loaded now: 3x3 round each walker, and round the frontier while building. */
    static Set<Long> window(Road r) {
        Set<Long> want = new HashSet<>();
        List<Long> centres = new ArrayList<>();
        for (UUID id : r.walkers) {
            Long c = r.lastChunk.get(id);
            if (c != null) centres.add(c);
        }
        if (!r.done()) centres.add(ChunkPos.pack(r.frontier()));
        if (!r.done() && r.walkers.size() < BUILDERS) centres.add(ChunkPos.pack(r.base)); // so new builders can be found at their camp
        for (long c : centres)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) want.add(ChunkPos.pack(ChunkPos.getX(c) + dx, ChunkPos.getZ(c) + dz));
        return want;
    }

    void updateWindow(ServerLevel level, Road r) {
        Set<Long> want = window(r);
        for (long c : List.copyOf(r.forced)) {
            if (want.contains(c)) continue;
            WINDOW.forceChunk(level, r.end, ChunkPos.getX(c), ChunkPos.getZ(c), false, false);
            r.forced.remove(c);
        }
        for (long c : want) if (r.forced.add(c)) WINDOW.forceChunk(level, r.end, ChunkPos.getX(c), ChunkPos.getZ(c), true, false);
    }

    /** A8: the camp of a tribe with a finished trade road stays loaded (3x3 chunks), so its traders set out with nobody there. */
    void updateBases(ServerLevel level) {
        Set<BlockPos> want = new HashSet<>();
        for (Road r : roads) if (r.kind == TRADE && r.done()) want.add(r.base);
        for (BlockPos b : List.copyOf(bases)) {
            if (want.contains(b)) continue;
            forceBase(level, b, false);
            bases.remove(b);
        }
        for (BlockPos b : want) if (bases.add(b)) forceBase(level, b, true);
    }

    private static void forceBase(ServerLevel level, BlockPos base, boolean add) {
        ChunkPos c = ChunkPos.containing(base);
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) BASE.forceChunk(level, base, c.x() + x, c.z() + z, add, false);
    }

    /** On load: keep each road's window tickets (and note them), drop those of roads that are gone. */
    static void validateWindow(ServerLevel level, TicketHelper tickets) {
        RoadBuilding data = get(level.getServer());
        tickets.getBlockTickets().forEach((pos, set) -> {
            Road road = null;
            for (Road r : data.roads) if (r.end.equals(pos)) road = r;
            if (road == null || level.dimension() != Level.OVERWORLD) tickets.removeAllTickets(pos);
            else road.forced.addAll(set.normal());
        });
    }

    static void validateBase(ServerLevel level, TicketHelper tickets) {
        RoadBuilding data = get(level.getServer());
        tickets.getBlockTickets().forEach((pos, set) -> {
            if (level.dimension() == Level.OVERWORLD && data.roads.stream().anyMatch(r -> r.kind == TRADE && r.done() && r.base.equals(pos))) data.bases.add(pos);
            else tickets.removeAllTickets(pos);
        });
    }

    @SubscribeEvent
    static void registerTickets(RegisterTicketControllersEvent event) {
        event.register(WINDOW);
        event.register(BASE);
    }
}
