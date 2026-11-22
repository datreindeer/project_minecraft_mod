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
import com.mechanical.Mechanical;
import com.mechanical.TrampledPathBlock;
import com.mechanical.TrampledPaths;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.StairBlock;
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
 * Silver Coins ({@link #price}). Two of their folk walk out (RoadBuildGoal) and lay a path 2 wide (Trodden Path, gravel)
 * cell by cell. The route is planned a section at a time ({@link #plan}, A* over the ground round the frontier, slopes and
 * water cost more, anyone's build, trees and unloaded ground are off limits), so only blocks that are natural ground,
 * plants or water are ever dug, filled or replaced. Water is crossed on a plank causeway only when there is no way round.</li>
 * <li>A6: at {@link #TRADE_REP} or better a finished path can be widened into a 3-wide stone trade road, same way.</li>
 * <li>A7: the road never steps more than 1 block from cell to cell (cut and fill), and every 1-block step gets a stair
 * (a slab on a 1-cell hump).</li>
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
     * {@link #BLOCKS_PER_SILVER} blocks (10 at the most), twice that for a stone trade road; -1 past {@link #MAX_LEN}.
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

    /** "Make it a trade road": paid now (price by the road's length, doubled), then they rebuild it 3 wide in stone. */
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

    /** A column the road may use: its top block's y, and whether that top is water. */
    record Col(int y, boolean water) {}

    /** Ground a road may be laid on, dug or filled: natural earth, sand, gravel, stone, snow, trails. Anything else is someone's. */
    static boolean natural(BlockState s) {
        return s.is(BlockTags.SUBSTRATE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.getBlock() instanceof TrampledPathBlock || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.CLAY) || s.is(Blocks.COARSE_DIRT);
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
        if (level.getFluidState(p).is(FluidTags.WATER)) return new Col(p.getY(), true);
        return natural(s) ? new Col(p.getY(), false) : null;
    }

    /** Step cost between two columns: flat 1, a 1-block step a little more, anything steeper much more (it means digging); water 12. */
    static double cost(Col a, Col b) {
        int dh = Math.abs(a.y - b.y);
        return 1 + (dh <= 1 ? dh * 0.5 : 2 + (dh - 1) * 4) + (b.water ? 12 : 0);
    }

    /**
     * A* from {@code from} toward {@code to} over the columns in {@code box} (x/z only), 4-connected: the path to {@code to}
     * if it lies in the box and can be reached, else to the reachable column closest to it. Columns as BlockPos (y =
     * ground), from first; just {@code from} if nothing better is reachable, null if {@code from} itself is unusable.
     */
    public static List<BlockPos> plan(ServerLevel level, BlockPos from, BlockPos to, int minX, int minZ, int maxX, int maxZ) {
        Map<Long, Col> cols = new HashMap<>();
        java.util.function.LongFunction<Col> col = k -> cols.computeIfAbsent(k, kk -> {
            int x = BlockPos.getX(kk), z = BlockPos.getZ(kk);
            return x < minX || x > maxX || z < minZ || z > maxZ ? null : column(level, x, z);
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

    /** Lays cell {@code i}: every lane at the cell's height, a stair where the road steps up onto it (a slab on a hump). */
    public static void buildCell(ServerLevel level, Road r, int i) {
        BlockPos c = r.cell(i);
        BlockPos prev = i > 0 ? r.cell(i - 1) : null, next = i + 1 < r.cells.size() ? r.cell(i + 1) : null;
        Direction along = prev != null ? dir(prev, c) : next != null ? dir(c, next) : Direction.NORTH;
        Direction upFrom = null; // the stair's facing: the way the road climbs onto this cell
        boolean hump = false;
        if (prev != null && prev.getY() == c.getY() - 1) upFrom = dir(prev, c);
        if (next != null && next.getY() == c.getY() - 1) {
            if (upFrom != null) hump = true;
            upFrom = dir(next, c);
        }
        int[] lanes = r.kind == TRADE ? new int[] {-1, 0, 1} : new int[] {0, 1};
        Direction side = along.getClockWise();
        for (int lane : lanes) {
            BlockPos at = c.relative(side, lane);
            BlockState surface = hump ? slab(r.kind) : upFrom != null ? stair(r.kind).setValue(StairBlock.FACING, upFrom) : null;
            lay(level, r, at, surface, lane == 0);
        }
    }

    private static BlockState stair(int kind) {
        return (kind == TRADE ? Blocks.STONE_BRICK_STAIRS : Blocks.MOSSY_COBBLESTONE_STAIRS).defaultBlockState();
    }

    private static BlockState slab(int kind) {
        return (kind == TRADE ? Blocks.STONE_BRICK_SLAB : Blocks.MOSSY_COBBLESTONE_SLAB).defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    /** What one of their roads is made of (an upgrade may replace it). */
    static boolean roadBlock(BlockState s) {
        return natural(s) || s.is(Blocks.SPRUCE_PLANKS) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.STONE_BRICKS) || s.is(Blocks.MOSSY_COBBLESTONE)
                || s.is(Blocks.MOSSY_COBBLESTONE_STAIRS) || s.is(Blocks.MOSSY_COBBLESTONE_SLAB) || s.is(Blocks.STONE_BRICK_STAIRS) || s.is(Blocks.STONE_BRICK_SLAB);
    }

    /** One lane column: dig down or fill up to the road's height, then the surface and 2 blocks of headroom. Never touches a build. */
    private static void lay(ServerLevel level, Road r, BlockPos at, BlockState step, boolean middle) {
        int y = at.getY(), x = at.getX(), z = at.getZ();
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
        while (level.getBlockState(top).is(Blocks.SNOW) && top.getY() > y) top = top.below();
        boolean upgrade = r.kind == TRADE;
        java.util.function.Predicate<BlockState> ours = s -> upgrade ? roadBlock(s) : natural(s);
        // check the whole column first: anything someone built, and the lane is left as it is
        for (int yy = Math.max(top.getY(), y + 2); yy >= Math.min(top.getY(), y); yy--) {
            BlockState s = level.getBlockState(new BlockPos(x, yy, z));
            if (!clearable(s) && !ours.test(s) && !s.getFluidState().is(FluidTags.WATER)) return;
        }
        for (int yy = top.getY(); yy > y; yy--) set(level, new BlockPos(x, yy, z), Blocks.AIR.defaultBlockState());
        BlockState fill = (upgrade ? Blocks.COBBLESTONE : Blocks.DIRT).defaultBlockState();
        for (int yy = top.getY() + 1; yy < y; yy++) set(level, new BlockPos(x, yy, z), fill);
        BlockPos surfacePos = new BlockPos(x, y, z);
        boolean wet = level.getFluidState(surfacePos).is(FluidTags.WATER);
        BlockState surface = step != null ? step : wet ? (upgrade ? Blocks.STONE_BRICKS : Blocks.SPRUCE_PLANKS).defaultBlockState() : surface(r.kind, x, z, middle);
        set(level, surfacePos, surface);
        for (int k = 1; k <= 2; k++) {
            BlockPos p = surfacePos.above(k);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && (clearable(s) || ours.test(s))) set(level, p, Blocks.AIR.defaultBlockState());
        }
        if (surface.getBlock() instanceof TrampledPathBlock) set(level, surfacePos, TrampledPathBlock.connect(surface, level, surfacePos));
    }

    /** Path: Trodden Path with gravel and coarse dirt patches. Trade road: stone bricks down the middle, cobblestone edges. */
    private static BlockState surface(int kind, int x, int z, boolean middle) {
        long hsh = mix(x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL);
        int roll = (int) Math.floorMod(hsh, 10L);
        if (kind == TRADE) return (middle && roll < 7 ? Blocks.STONE_BRICKS : roll < 8 ? Blocks.COBBLESTONE : roll < 9 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE_BRICKS).defaultBlockState();
        if (roll < 7) return TrampledPaths.TRODDEN_PATH.get().defaultBlockState().setValue(TrampledPathBlock.PERSISTENT, true);
        return (roll < 9 ? Blocks.GRAVEL : Blocks.COARSE_DIRT).defaultBlockState();
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
