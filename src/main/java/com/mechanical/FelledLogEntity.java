package com.mechanical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A felled tree's logs lying on the ground as one body (docs/living-world-plan.md F1). {@link FallingTreeEntity} lands as
 * one of these; its blocks are kept as cells on a local grid (+z runs from the root end, the foot of the trunk, towards
 * the crown) turned by the entity's yaw. Cell 0 is always the root end, the rest in order of distance from it.
 * <ul>
 * <li>Gravity: it rests on its lowest layer (on solid blocks or water, not leaves), falls when nothing holds it and slides
 * off a ledge when its middle hangs over it. It sleeps when it lies still: no work per tick but a support check every 2 s.</li>
 * <li>Cutting: hold right-click with an axe on a block (all four hewing stages at once, same drops as a Fallen Log), or a
 * saw ({@link SawItem}, a Fallen Log's sawing). A middle block splits it in two, an end block shortens it. A knife scores
 * a block into a Dugout Blank where it lies.</li>
 * <li>Solid block by block for walking (EntityGetterMixin: {@link #collisionShape}); not for who lifts or pulls it. A
 * block being hewn or sawn shows the crack overlay (synced CRACK).</li>
 * <li>Lifting (up to {@link #HAND_MAX} blocks): an empty-hand right-click on an end lifts it; one lifter drags the other
 * end along, two carry it between them. Speed falls with {@link #liftLoad}; above 1 it only heaves along now and then.
 * Restrictions while lifting: {@link LogLifting}; poses: client/LogLiftClient.</li>
 * <li>Dragging (longer logs only): right-click near the root end with a Lead (or riding a horse: the horse pulls). The first pull draws the
 * logs into one straight log behind the root; then it trails after its pullers. Each player pulls {@link #PER_PULLER}
 * blocks, a horse {@link #HORSE} times that; too few and it doesn't move ("Too heavy"). Pullers are held at rope length.</li>
 * <li>Wagon: a straight log up to {@link #WAGON_MAX} blocks rides on an empty Wagon ({@link WagonEntity}).</li>
 * </ul>
 */
public class FelledLogEntity extends Entity implements IEntityWithComplexSpawn {
    /** Blocks one player can drag; a horse pulls this many times as much. */
    public static final int PER_PULLER = 8;
    public static final double HORSE = 3;
    public static final int MAX_PULLERS = 10;
    /** The log is pulled when its root end is farther than ROPE from the pullers; they are held within TETHER; past SNAP the lead breaks. */
    static final double ROPE = 2.5, TETHER = ROPE + 1.5, SNAP = 10;
    /** Blocks a tick at full strength; a load at the pullers' limit goes at 40 % of it. */
    static final double MAX_STEP = 0.3;
    /** Logs up to this many blocks are lifted by hand (an end each); longer ones need Leads, which only they take. */
    public static final int HAND_MAX = 12;
    /** Weight one player lifts at full pace: a lifted end carries half the log, the end still on the ground drags a quarter. */
    public static final double LIFT = 4;
    /** A lifted end is held this far from the player; the player stays within HOLD + LIFT_SLACK of it. */
    static final double HOLD = 0.4, LIFT_SLACK = 1.2;
    /** Blocks a tick carried light. */
    static final double CARRY_STEP = 0.25;
    /** How high the underside of a lifted end is held: at the hands, about hip height. */
    public static final float LIFT_HEIGHT = 0.45F; // its top stays under the eyes, so in first person it lies low in view
    /** Longest log the Wagon takes (its bed is 3 blocks: one hangs over the open back). */
    public static final int WAGON_MAX = 4;
    static final Identifier HAUL_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/haul_log"),
            WAGON_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/log_wagon");
    /** Entity ids of the pullers, comma separated (client: ropes and holding back its own player or horse). */
    private static final EntityDataAccessor<String> PULLERS = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.STRING);
    /** The block being hewn or sawn and how far (index * 16 + crack stage 0-9), -1 if none (client: the crack overlay). */
    private static final EntityDataAccessor<Integer> CRACK = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.INT);
    /** Each block's hewing stage and saw strokes as chars ('0' + stage * 8 + sawn): they change after spawning (client: the models). */
    private static final EntityDataAccessor<String> STAGES = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.STRING);
    /** Entity ids of the players lifting the root end and the crown end, -1 for none. */
    private static final EntityDataAccessor<Integer> LIFT_ROOT = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.INT),
            LIFT_TAIL = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.INT);
    /** Who lifts which log, per side (lifting restrictions, poses). */
    private static final Map<Player, FelledLogEntity> LIFTING_SERVER = new java.util.WeakHashMap<>(), LIFTING_CLIENT = new java.util.WeakHashMap<>();
    /** Ticks without a chop or stroke before the crack fades. */
    private static final int CRACK_FADE = 100;

    private final List<BlockPos> cells = new ArrayList<>();
    private final List<BlockState> states = new ArrayList<>();
    /** Middle of the cells on the local grid: the entity's position. */
    private double cx, cz;
    private boolean awake = true;
    private int restTicks;
    private double fallSpeed;
    /** Leads tied on when it was saved; dropped on load (the pullers aren't saved). */
    private int leadsToReturn;
    /** Server: who pulls it. */
    private final List<Entity> pullers = new ArrayList<>();
    /** Server: saw strokes so far per block. */
    private final Map<Integer, Integer> sawn = new HashMap<>();
    /** The lifters as set here (root, crown end), so a lookup by id isn't needed. */
    private final Entity[] lifterRefs = new Entity[2];
    /** Server: game time of the last chop or stroke (the crack fades {@link #CRACK_FADE} after it). */
    private long lastCrack;
    /** {@link #collisionShape} and the bounding box it was made for. */
    private net.minecraft.world.phys.shapes.VoxelShape shape;
    private AABB shapeFor;
    private int shapeLift;
    /** Client: puller ids from {@link #PULLERS}. */
    private int[] pullerIds = new int[0];

    public FelledLogEntity(EntityType<? extends FelledLogEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    // ---- shape

    public int size() {
        return cells.size();
    }

    public List<BlockPos> cells() {
        return cells;
    }

    public List<BlockState> states() {
        return states;
    }

    public double centreX() {
        return cx;
    }

    public double centreZ() {
        return cz;
    }

    private void setShape(List<BlockPos> newCells, List<BlockState> newStates) {
        cells.clear();
        states.clear();
        cells.addAll(newCells);
        for (BlockState st : newStates) states.add(lying(st));
        if (!level().isClientSide()) syncStages();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos c : cells) {
            minX = Math.min(minX, c.getX());
            maxX = Math.max(maxX, c.getX());
            minZ = Math.min(minZ, c.getZ());
            maxZ = Math.max(maxZ, c.getZ());
        }
        cx = (minX + maxX) / 2.0;
        cz = (minZ + maxZ) / 2.0;
        setBoundingBox(makeBoundingBox());
    }

    /** A log block as the mod's Fallen Log of its wood (lying along local z, unhewn), which shows hewing and sawing; other blocks as they are. */
    static BlockState lying(BlockState s) {
        if (s.getBlock() instanceof FallenLogBlock) return s;
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "");
        var fallen = Mechanical.FALLEN_LOGS.get(path);
        return fallen == null ? s : fallen.get().defaultBlockState();
    }

    private void syncStages() {
        StringBuilder b = new StringBuilder();
        for (BlockState st : states)
            b.append((char) ('0' + (st.getBlock() instanceof FallenLogBlock ? st.getValue(FallenLogBlock.STAGE) * 8 + st.getValue(FallenLogBlock.SAWN) : 0)));
        entityData.set(STAGES, b.toString());
    }

    /** Client: the synced stages onto the blocks. */
    private void applyStages() {
        String b = entityData.get(STAGES);
        if (b.length() != states.size()) return;
        for (int i = 0; i < states.size(); i++) {
            if (!(states.get(i).getBlock() instanceof FallenLogBlock)) continue;
            int v = b.charAt(i) - '0';
            states.set(i, states.get(i).setValue(FallenLogBlock.STAGE, Math.min(FallenLogBlock.LAST_STAGE, v / 8)).setValue(FallenLogBlock.SAWN, v % 8));
        }
    }

    /** Server: block {@code i} becomes {@code state} (a hewing stage, a saw stroke). */
    private void setState(int i, BlockState state) {
        states.set(i, state);
        syncStages();
    }

    /** Is it one straight row from the root, as the first pull leaves it? */
    public boolean straight() {
        for (int i = 0; i < cells.size(); i++) if (!cells.get(i).equals(cells.get(0).offset(0, 0, i)) || cells.get(i).getY() != 0) return false;
        return true;
    }

    private static Vec3 local(BlockPos c, double cx, double cz) {
        return new Vec3(c.getX() - cx, c.getY(), c.getZ() - cz);
    }

    /** Bottom centre of block {@code i} in the world, were the entity at {@code pos} turned to {@code yaw}. */
    private Vec3 world(int i, Vec3 pos, float yaw) {
        return pos.add(local(cells.get(i), cx, cz).yRot(-yaw * Mth.DEG_TO_RAD));
    }

    /** Block {@code i}'s bottom middle on the log's own grid (x across, z along from the middle; y its layer). */
    public Vec3 localCell(int i) {
        return local(cells.get(i), cx, cz);
    }

    /** A world point on the log's own grid (only x and z turned), and back. */
    public Vec3 toLocal(Vec3 world) {
        return world.subtract(position()).yRot(getYRot() * Mth.DEG_TO_RAD);
    }

    public Vec3 toWorld(Vec3 local) {
        return position().add(local.yRot(-getYRot() * Mth.DEG_TO_RAD));
    }

    public Vec3 world(int i) {
        return world(i, position(), getYRot());
    }

    /** The world block block {@code i} is in. */
    public BlockPos cellPos(int i) {
        return BlockPos.containing(world(i).add(0, 0.5, 0));
    }

    /** The block at world position {@code pos}, or -1. */
    public int indexAt(BlockPos pos) {
        for (int i = 0; i < cells.size(); i++) if (cellPos(i).equals(pos)) return i;
        return -1;
    }

    /** Where the root end is: its pullers' ropes are tied there. */
    public Vec3 rootPoint(float partialTicks) {
        float yaw = getViewYRot(partialTicks);
        return getPosition(partialTicks).add(local(cells.get(0), cx, cz).add(0, 0.5, -0.5).yRot(-yaw * Mth.DEG_TO_RAD));
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        if (cells == null || cells.isEmpty()) return super.makeBoundingBox(pos);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < cells.size(); i++) {
            Vec3 w = world(i, pos, getYRot());
            minX = Math.min(minX, w.x);
            maxX = Math.max(maxX, w.x);
            minY = Math.min(minY, w.y);
            maxY = Math.max(maxY, w.y);
            minZ = Math.min(minZ, w.z);
            maxZ = Math.max(maxZ, w.z);
        }
        // half a block turned to the yaw: 0.5 on the grid, up to 0.71 at 45°
        double r = getYRot() * Mth.DEG_TO_RAD, m = onGrid() ? 0.5 : 0.5 * (Math.abs(Math.sin(r)) + Math.abs(Math.cos(r)));
        return new AABB(minX - m, minY, minZ - m, maxX + m, maxY + 1, maxZ + m);
    }

    @Override
    public void setYRot(float yRot) {
        // the server keeps only the angle clients get (a byte: 1/256 turn), so both sides collide alike
        if (level() != null && !level().isClientSide()) yRot = Mth.unpackDegrees(Mth.packDegrees(yRot));
        super.setYRot(yRot);
        if (cells != null) setBoundingBox(makeBoundingBox());
    }

    /** The block {@code player} looks at, or -1: their view ray against each block's box on the log's own grid. */
    public int pick(Player player) {
        float r = getYRot() * Mth.DEG_TO_RAD;
        Vec3 eye = player.getEyePosition();
        Vec3 a = eye.subtract(position()).yRot(r), b = eye.add(player.getViewVector(1.0F).scale(player.entityInteractionRange() + 1)).subtract(position()).yRot(r);
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < cells.size(); i++) {
            Vec3 l = local(cells.get(i), cx, cz);
            double up = liftOffset(i);
            var hit = new AABB(l.x - 0.5, l.y + up, l.z - 0.5, l.x + 0.5, l.y + 1 + up, l.z + 0.5).clip(a, b);
            if (hit.isPresent() && hit.get().distanceToSqr(a) < bestD) {
                bestD = hit.get().distanceToSqr(a);
                best = i;
            }
        }
        return best;
    }

    /** The wood of block {@code i} ("birch"), or null if the mod has no fallen log of it. */
    private String wood(int i) {
        if (states.get(i).getBlock() instanceof FallenLogBlock f) return f.wood();
        String path = BuiltInRegistries.BLOCK.getKey(states.get(i).getBlock()).getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "");
        return Mechanical.FALLEN_LOGS.containsKey(path) ? path : null;
    }

    // ---- making them

    /**
     * Server: the logs of a felled tree come down here ({@link FallingTreeEntity}). {@code offsets} are from the cut log at
     * {@code base} (offset 0 must be among them); turned over towards {@code direction}, height becomes length and the root
     * end lies just past the stump. Logs that land in the same place lie on top of each other.
     */
    public static void land(ServerLevel level, BlockPos base, Direction direction, List<BlockPos> offsets, List<BlockState> logStates) {
        float yaw = direction.toYRot();
        Vec3 side = new Vec3(1, 0, 0).yRot(-yaw * Mth.DEG_TO_RAD);
        Integer[] order = new Integer[offsets.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        // root first, then up the trunk
        java.util.Arrays.sort(order, Comparator.comparingInt((Integer i) -> offsets.get(i).getY()).thenComparingInt(i -> Math.abs(offsets.get(i).getX()) + Math.abs(offsets.get(i).getZ())));
        List<BlockPos> cells = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        java.util.Set<BlockPos> taken = new java.util.HashSet<>();
        for (int i : order) {
            BlockPos o = offsets.get(i);
            BlockPos c = new BlockPos((int) Math.round(o.getX() * side.x + o.getZ() * side.z), 0, o.getY() + 1);
            while (!taken.add(c)) c = c.above();
            cells.add(c);
            states.add(logStates.get(i));
        }
        Vec3 origin = Vec3.atBottomCenterOf(base);
        for (List<Integer> part : components(cells)) {
            FelledLogEntity log = new FelledLogEntity(Mechanical.FELLED_LOG.get(), level);
            log.setShape(part.stream().map(cells::get).toList(), part.stream().map(states::get).toList());
            log.setYRot(yaw);
            Vec3 pos = origin.add(new Vec3(log.cx, 0, log.cz).yRot(-yaw * Mth.DEG_TO_RAD));
            // high enough that no block is in the ground; it settles from there
            int y = level.getMinY();
            for (int i = 0; i < log.size(); i++) {
                Vec3 w = log.world(i, pos, yaw);
                y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(w.x), Mth.floor(w.z)) - log.cells.get(i).getY());
            }
            log.setPos(pos.x, y, pos.z);
            for (int i = 0; i < log.size(); i++) crush(level, log.cellPos(i));
            level.addFreshEntity(log);
        }
    }

    /** Server: a straight log of {@code n} blocks of {@code state}, its root block standing on {@code root} (bottom centre), lying along {@code yaw} (tests). */
    static FelledLogEntity spawnStraight(ServerLevel level, Vec3 root, float yaw, int n, BlockState state) {
        FelledLogEntity log = new FelledLogEntity(Mechanical.FELLED_LOG.get(), level);
        List<BlockPos> line = new ArrayList<>();
        for (int i = 0; i < n; i++) line.add(new BlockPos(0, 0, i));
        log.setShape(line, java.util.Collections.nCopies(n, state));
        log.setYRot(yaw);
        log.setPos(root.add(Vec3.directionFromRotation(0, yaw).scale((n - 1) / 2.0)));
        level.addFreshEntity(log);
        return log;
    }

    /** Flowers, saplings and bushes where a log comes down are crushed (they drop what they drop). */
    private static void crush(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.VegetationBlock) level.destroyBlock(pos, true);
    }

    /** Groups of touching cells (corners count), each in breadth-first order from its first cell in list order. */
    static List<List<Integer>> components(List<BlockPos> cells) {
        Map<BlockPos, Integer> index = new HashMap<>();
        for (int i = 0; i < cells.size(); i++) index.put(cells.get(i), i);
        boolean[] seen = new boolean[cells.size()];
        List<List<Integer>> out = new ArrayList<>();
        for (int start = 0; start < cells.size(); start++) {
            if (seen[start]) continue;
            List<Integer> part = new ArrayList<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(start));
            seen[start] = true;
            while (!queue.isEmpty()) {
                int i = queue.poll();
                part.add(i);
                for (BlockPos n : BlockPos.betweenClosed(cells.get(i).offset(-1, -1, -1), cells.get(i).offset(1, 1, 1))) {
                    Integer j = index.get(n);
                    if (j != null && !seen[j]) {
                        seen[j] = true;
                        queue.add(j);
                    }
                }
            }
            out.add(part);
        }
        return out;
    }

    /**
     * Server: this log becomes the pieces of {@code newCells} (on the same grid, so nothing moves): one log per touching
     * group. The piece with the root end keeps the pullers if {@code keepPullers}; otherwise their leads drop.
     */
    private void rebuild(ServerLevel level, List<BlockPos> newCells, List<BlockState> newStates, boolean keepPullers) {
        rebuild(level, newCells, newStates, keepPullers, components(newCells));
    }

    /** As above, in the given groups of indices into {@code newCells} (a saw cut parts blocks that still touch). */
    private void rebuild(ServerLevel level, List<BlockPos> newCells, List<BlockState> newStates, boolean keepPullers, List<List<Integer>> parts) {
        putDownAll(); // cut: whoever lifted it lets go
        List<Entity> carried = new ArrayList<>(pullers);
        if (!keepPullers) for (Entity p : carried) untie(level, p, false);
        pullers.clear();
        discard();
        boolean first = true;
        for (List<Integer> part : parts) {
            int minY = part.stream().mapToInt(i -> newCells.get(i).getY()).min().orElse(0);
            FelledLogEntity log = new FelledLogEntity(Mechanical.FELLED_LOG.get(), level);
            log.setYRot(getYRot());
            log.setShape(part.stream().map(i -> newCells.get(i).below(minY)).toList(), part.stream().map(newStates::get).toList());
            log.setPos(position().add(new Vec3(log.cx - cx, minY, log.cz - cz).yRot(-getYRot() * Mth.DEG_TO_RAD)));
            if (first && keepPullers) {
                log.pullers.addAll(carried);
                log.syncPullers();
            }
            level.addFreshEntity(log);
            first = false;
        }
    }

    /** Server: block {@code i} is cut out (its drops are the caller's). */
    public void removeBlock(ServerLevel level, int i) {
        List<BlockPos> c = new ArrayList<>(cells);
        List<BlockState> s = new ArrayList<>(states);
        c.remove(i);
        s.remove(i);
        rebuild(level, c, s, i != 0);
    }

    /** Server: takes out the log block at {@code pos} with no drops (woodcutters, beavers). False if no log has a block there. */
    public static boolean removeAt(ServerLevel level, BlockPos pos) {
        for (FelledLogEntity log : level.getEntitiesOfClass(FelledLogEntity.class, new AABB(pos).inflate(1), e -> !e.isPassenger())) {
            int i = log.indexAt(pos);
            if (i >= 0) {
                log.removeBlock(level, i);
                return true;
            }
        }
        return false;
    }

    /**
     * Server: the logs drawn into one straight log from the root end, lying away from it along {@code yaw}: a new entity,
     * which takes over the pullers. Null (nothing changes) if {@code check} and it doesn't fit there.
     */
    FelledLogEntity straighten(ServerLevel level, float yaw, boolean check) {
        List<BlockPos> line = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) line.add(new BlockPos(0, 0, i));
        FelledLogEntity log = new FelledLogEntity(Mechanical.FELLED_LOG.get(), level);
        log.setShape(line, states);
        log.setYRot(yaw);
        Vec3 root = world(0);
        Vec3 pos = root.add(Vec3.directionFromRotation(0, yaw).scale(log.cz));
        boolean fits = !check;
        for (int up = 0; up <= 1 && !fits; up++) {
            if (log.fits(level, pos.add(0, up, 0), yaw)) {
                pos = pos.add(0, up, 0);
                fits = true;
            }
        }
        if (!fits) return null;
        log.setPos(pos);
        log.pullers.addAll(pullers);
        log.syncPullers();
        pullers.clear();
        discard();
        level.addFreshEntity(log);
        return log;
    }

    // ---- physics

    /** Blocks it can't pass through (leaves give way). */
    private static boolean blocks(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.is(BlockTags.LEAVES) && !s.getCollisionShape(level, pos).isEmpty();
    }

    /** What it can lie on: solid ground, or water (logs float). */
    private static boolean holds(Level level, BlockPos pos) {
        return blocks(level, pos) || level.getFluidState(pos).is(FluidTags.WATER);
    }

    /** Would it be clear of the ground at {@code pos} turned to {@code yaw}? */
    boolean fits(Level level, Vec3 pos, float yaw) {
        for (int i = 0; i < cells.size(); i++) if (blocks(level, BlockPos.containing(world(i, pos, yaw).add(0, 0.5, 0)))) return false;
        return true;
    }

    private void wake() {
        awake = true;
        restTicks = 0;
    }

    /** Server: does anything under its lowest layer hold it up? */
    private boolean supported() {
        int y = Mth.floor(getY() + 0.01) - 1;
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getY() != 0) continue;
            Vec3 w = world(i);
            if (holds(level(), BlockPos.containing(w.x, y, w.z))) return true;
        }
        return false;
    }

    /** Server: one step of falling or sliding off a ledge; true if it moved. */
    private boolean settle(ServerLevel level) {
        int baseY = Mth.floor(getY() + 0.01);
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE, comX = 0, comZ = 0;
        boolean any = false;
        for (int i = 0; i < cells.size(); i++) {
            BlockPos c = cells.get(i);
            comX += c.getX();
            comZ += c.getZ();
            if (c.getY() != 0) continue;
            Vec3 w = world(i);
            if (!holds(level, BlockPos.containing(w.x, baseY - 1, w.z))) continue;
            any = true;
            minX = Math.min(minX, c.getX());
            maxX = Math.max(maxX, c.getX());
            minZ = Math.min(minZ, c.getZ());
            maxZ = Math.max(maxZ, c.getZ());
        }
        if (!any) {
            fallSpeed = Math.min(fallSpeed + 0.08, 1.0);
            setPos(getX(), getY() - fallSpeed, getZ());
            return true;
        }
        boolean moved = false;
        if (fallSpeed > 0 || getY() != baseY) {
            if (fallSpeed > 0.4) playSound(SoundEvents.WOOD_FALL, 1.0F, 0.6F);
            fallSpeed = 0;
            setPos(getX(), baseY, getZ());
            moved = true;
        }
        // its middle hangs past what holds it: it slides that way, off the ledge
        comX /= cells.size();
        comZ /= cells.size();
        double sx = comX < minX - 0.5 ? -1 : comX > maxX + 0.5 ? 1 : 0, sz = comZ < minZ - 0.5 ? -1 : comZ > maxZ + 0.5 ? 1 : 0;
        if (sx == 0 && sz == 0) return moved;
        Vec3 step = new Vec3(sx, 0, sz).normalize().scale(0.2).yRot(-getYRot() * Mth.DEG_TO_RAD);
        if (!fits(level, position().add(step), getYRot())) return moved;
        setPos(position().add(step));
        return true;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            holdBackPullers();
            return;
        }
        if (isPassenger()) return; // on a wagon: it places us
        ServerLevel level = (ServerLevel) level();
        if (cells.isEmpty()) {
            discard();
            return;
        }
        if (!awake) {
            // asleep: only a look at what holds it now and then (a block dug out from under it)
            if ((level.getGameTime() + getId()) % 40 == 0 && !supported()) wake();
            return;
        }
        super.tick();
        if (entityData.get(CRACK) >= 0 && level.getGameTime() - lastCrack > CRACK_FADE) entityData.set(CRACK, -1);
        if (leadsToReturn > 0) {
            spawnAtLocation(level, new ItemStack(Items.LEAD, leadsToReturn));
            leadsToReturn = 0;
        }
        checkPullers(level);
        checkLifters();
        boolean moved = !pullers.isEmpty() && drag(level);
        if (lifterCount() > 0 && !isRemoved()) moved |= carry(level);
        if (isRemoved()) return; // straightened into a new log
        moved |= settle(level);
        if (moved || !pullers.isEmpty() || lifterCount() > 0 || entityData.get(CRACK) >= 0) restTicks = 0;
        else if (++restTicks > 20) awake = false;
    }

    /** Is it lined up with the block grid? */
    public boolean onGrid() {
        return Math.abs(Mth.wrapDegrees(getYRot() - Math.round(getYRot() / 90.0F) * 90.0F)) < 0.01F;
    }

    // ---- pullers

    private static double strength(Entity puller) {
        return puller instanceof AbstractHorse ? HORSE : 1;
    }

    /** The player behind a puller: itself, or a horse's rider. */
    private static Player playerOf(Entity puller) {
        return puller instanceof Player p ? p : puller.getControllingPassenger() instanceof Player rider ? rider : null;
    }

    public boolean isPulled() {
        return level().isClientSide() ? pullerIds.length > 0 : !pullers.isEmpty();
    }

    /** Is {@code player} (or the horse they ride) pulling it? Both sides. */
    public boolean pulledBy(Player player) {
        if (level().isClientSide()) return isPullerClient(player);
        return pullers.contains(player) || player.getVehicle() != null && pullers.contains(player.getVehicle());
    }

    /** The log {@code player} (or their horse) pulls, or null. Both sides. */
    public static FelledLogEntity pulledBy(Player player, double range) {
        for (FelledLogEntity log : player.level().getEntitiesOfClass(FelledLogEntity.class, player.getBoundingBox().inflate(range), l -> l.pulledBy(player)))
            return log;
        return null;
    }

    private void syncPullers() {
        StringBuilder s = new StringBuilder();
        for (Entity p : pullers) s.append(s.isEmpty() ? "" : ",").append(p.getId());
        entityData.set(PULLERS, s.toString());
    }

    /** Server: the lead comes off {@code puller}: back to its player (or dropped there if {@code broke} or nobody). */
    private void untie(ServerLevel level, Entity puller, boolean broke) {
        Player player = playerOf(puller);
        ItemStack lead = new ItemStack(Items.LEAD);
        if (!broke && player != null) {
            if (!player.hasInfiniteMaterials()) player.getInventory().placeItemBackInInventory(lead, net.minecraft.util.Prediction.SERVER_ONLY);
        } else {
            puller.spawnAtLocation(level, lead);
        }
        level.playSound(null, puller.getX(), puller.getY(), puller.getZ(), broke ? SoundEvents.LEAD_BREAK : SoundEvents.LEAD_UNTIED, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    /** Server: lets go of everyone (their leads back to them). */
    void untieAll(ServerLevel level) {
        putDownAll();
        for (Entity p : pullers) untie(level, p, false);
        pullers.clear();
        syncPullers();
    }

    /** Server: pullers gone or too far lose their lead. */
    private void checkPullers(ServerLevel level) {
        if (pullers.isEmpty()) return;
        Vec3 root = world(0);
        boolean changed = pullers.removeIf(p -> {
            if (p.isAlive() && p.level() == level && p.position().distanceTo(root) <= SNAP) return false;
            if (p.isAlive()) untie(level, p, true);
            else spawnAtLocation(level, new ItemStack(Items.LEAD));
            return true;
        });
        if (changed) syncPullers();
    }

    /** Server: one step of being pulled; true if it moved. */
    private boolean drag(ServerLevel level) {
        double force = 0;
        Vec3 pull = Vec3.ZERO;
        for (Entity p : pullers) {
            force += strength(p);
            pull = pull.add(p.position());
        }
        pull = pull.scale(1.0 / pullers.size());
        double capacity = force * PER_PULLER;
        if (capacity < size()) {
            if (tickCount % 40 == 0) {
                for (Entity p : pullers) {
                    Player player = playerOf(p);
                    if (player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.too_heavy"));
                }
            }
            return false;
        }
        Vec3 root = world(0);
        Vec3 off = new Vec3(pull.x - root.x, 0, pull.z - root.z);
        double d = off.length();
        if (d <= ROPE) return false;
        if (!straight()) {
            // the first pull draws the logs into one straight log lying away from the pullers
            Vec3 away = off.reverse();
            return straighten(level, (float) (Mth.atan2(-away.x, away.z) * Mth.RAD_TO_DEG), true) != null;
        }
        double speed = MAX_STEP * (1 - 0.6 * size() / capacity);
        Vec3 newRoot = root.add(off.scale(Math.min(d - ROPE, speed) / d));
        // the tail trails behind like a towed cart
        Vec3 tail = world(size() - 1);
        Vec3 f = new Vec3(tail.x - newRoot.x, 0, tail.z - newRoot.z);
        f = f.lengthSqr() < 1.0E-4 ? off.reverse().normalize() : f.normalize();
        float yaw = (float) (Mth.atan2(-f.x, f.z) * Mth.RAD_TO_DEG);
        Vec3 centre = new Vec3(newRoot.x, getY(), newRoot.z).add(f.scale(cz - cells.get(0).getZ()));
        for (int up = 0; up <= 1; up++) {
            Vec3 at = centre.add(0, up, 0);
            if (!fits(level, at, yaw)) continue;
            setYRot(yaw);
            setPos(at);
            if (tickCount % 20 == 0) {
                for (Entity p : pullers) if (playerOf(p) instanceof ServerPlayer sp) Kettle.award(sp, HAUL_ADVANCEMENT);
                playSound(SoundEvents.WOOD_STEP, 0.6F, 0.6F);
            }
            return true;
        }
        return false;
    }

    /** Client: its own player (or the horse they ride) can't walk off further than the rope reaches. */
    private void holdBackPullers() {
        if (cells.isEmpty()) return;
        for (boolean root : new boolean[] {true, false}) {
            Entity mover = lifter(root);
            if (mover == null || !mover.isLocalInstanceAuthoritative()) continue;
            if (mover instanceof Player p && LIFTING_CLIENT.get(p) != this) LIFTING_CLIENT.put(p, this); // synced before the player was known
            tether(mover, endPoint(root), HOLD + LIFT_SLACK);
            mover.setSprinting(false);
        }
        if (pullerIds.length == 0) return;
        Vec3 root = world(0);
        for (int id : pullerIds) {
            Entity mover = level().getEntity(id);
            if (mover != null && mover.isLocalInstanceAuthoritative()) tether(mover, root, TETHER);
        }
    }

    /** Client: holds {@code mover} within {@code reach} of {@code grip} (horizontally): no further out, no speed outwards. */
    private static void tether(Entity mover, Vec3 grip, double reach) {
        Vec3 off = mover.position().subtract(grip).multiply(1, 0, 1);
        double d = off.length();
        if (d <= reach) return;
        Vec3 n = off.scale(1 / d);
        double limit = Math.max(reach, new Vec3(mover.xo - grip.x, 0, mover.zo - grip.z).length());
        if (d > limit) mover.setPos(grip.x + n.x * limit, mover.getY(), grip.z + n.z * limit);
        Vec3 v = mover.getDeltaMovement();
        double out = v.x * n.x + v.z * n.z;
        if (out > 0) mover.setDeltaMovement(v.subtract(n.x * out, 0, n.z * out));
    }

    /** Client: the entities whose ropes run to the root end. */
    public int[] pullerIds() {
        return pullerIds;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (level().isClientSide() && (LIFT_ROOT.equals(accessor) || LIFT_TAIL.equals(accessor))) {
            LIFTING_CLIENT.values().removeIf(l -> l == this);
            for (EntityDataAccessor<Integer> a : List.of(LIFT_ROOT, LIFT_TAIL))
                if (level().getEntity(entityData.get(a)) instanceof Player p) LIFTING_CLIENT.put(p, this);
        }
        if (STAGES.equals(accessor) && level().isClientSide()) applyStages();
        if (PULLERS.equals(accessor)) {
            String s = entityData.get(PULLERS);
            pullerIds = s.isEmpty() ? new int[0] : java.util.Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();
        }
    }

    // ---- lifting by hand

    private static Map<Player, FelledLogEntity> lifting(Level level) {
        return level.isClientSide() ? LIFTING_CLIENT : LIFTING_SERVER;
    }

    /** The log {@code player} lifts an end of, or null. Both sides. */
    public static FelledLogEntity liftedBy(Player player) {
        FelledLogEntity log = lifting(player.level()).get(player);
        return log != null && log.isAlive() && log.lifts(player) ? log : null;
    }

    /** Is {@code player} lifting the root end (true), the crown end (false), or neither (null)? */
    public Boolean liftEnd(Entity player) {
        if (entityData.get(LIFT_ROOT) == player.getId()) return true;
        if (entityData.get(LIFT_TAIL) == player.getId()) return false;
        return null;
    }

    public boolean lifts(Entity player) {
        return liftEnd(player) != null;
    }

    /** Players lifting it (0, 1 or 2). */
    public int lifterCount() {
        return (rootLifted() ? 1 : 0) + (tailLifted() ? 1 : 0);
    }

    public boolean rootLifted() {
        return entityData.get(LIFT_ROOT) >= 0;
    }

    public boolean tailLifted() {
        return entityData.get(LIFT_TAIL) >= 0;
    }

    /**
     * How far block {@code i} is raised by the lifters: both ends up, {@link #LIFT_HEIGHT}; one end up, in proportion to
     * its middle's distance from the end on the ground (the log tilts). Collision, clicks and the render all use it.
     */
    public double liftOffset(int i) {
        boolean r = rootLifted(), t = tailLifted();
        if (r && t) return LIFT_HEIGHT;
        if (!r && !t) return 0;
        double along = i + 0.5; // from the root end (lifted logs are straight)
        return LIFT_HEIGHT * (r ? size() - along : along) / size();
    }

    /** Weight over lifting strength: above 1 the lifters strain (it only jerks along). Both sides. */
    public double liftLoad() {
        int n = lifterCount();
        if (n == 0) return 0;
        double weight = n == 2 ? size() : size() * 0.75; // one end on the ground: half lifted, a quarter dragged
        return weight / (n * LIFT);
    }

    /** The middle of the root end's face (true) or the crown end's, on the ground. Straight logs. */
    public Vec3 endPoint(boolean root) {
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        return position().add(f.scale((root ? -1 : 1) * size() / 2.0));
    }

    private Entity lifter(boolean root) {
        int id = entityData.get(root ? LIFT_ROOT : LIFT_TAIL);
        Entity ref = lifterRefs[root ? 0 : 1];
        return id < 0 ? null : ref != null && ref.getId() == id ? ref : level().getEntity(id);
    }

    private void setLifter(boolean root, Player player) {
        if (lifter(root) instanceof Player old) lifting(level()).remove(old);
        lifterRefs[root ? 0 : 1] = player;
        entityData.set(root ? LIFT_ROOT : LIFT_TAIL, player == null ? -1 : player.getId());
        if (player != null) lifting(level()).put(player, this);
        wake();
    }

    /** Server: {@code player} grabs the end nearest block {@code i}; the log is straightened first. */
    void lift(ServerLevel level, Player player, int i) {
        if (size() > HAND_MAX) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.too_big_to_lift"));
            return;
        }
        if (liftedBy(player) != null || !pullers.isEmpty()) return;
        BlockPos c = cells.get(i);
        int toRoot = chebyshev(c, cells.get(0)), toTail = chebyshev(c, cells.get(size() - 1));
        if (Math.min(toRoot, toTail) > 1) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.grab_end"));
            return;
        }
        boolean root = toRoot < toTail || toRoot == toTail && player.distanceToSqr(world(0)) <= player.distanceToSqr(world(size() - 1));
        if (entityData.get(root ? LIFT_ROOT : LIFT_TAIL) >= 0) root = !root; // the other end, if it is free and near
        if (entityData.get(root ? LIFT_ROOT : LIFT_TAIL) >= 0 || chebyshev(c, cells.get(root ? 0 : size() - 1)) > 1) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.end_taken"));
            return;
        }
        FelledLogEntity log = this;
        if (!straight()) {
            log = straighten(level, getYRot(), true);
            if (log == null) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.no_room"));
                return;
            }
        }
        log.setLifter(root, player);
        log.playSound(SoundEvents.WOOD_HIT, 1.0F, 0.7F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.lifted"));
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }

    /** Server: {@code player} puts their end down. */
    void putDown(Player player) {
        Boolean root = liftEnd(player);
        if (root == null) return;
        setLifter(root, null);
        lifting(level()).remove(player);
        playSound(SoundEvents.WOOD_FALL, 0.8F, 0.8F);
    }

    /** Server: both ends down. */
    void putDownAll() {
        for (boolean root : new boolean[] {true, false}) {
            if (lifter(root) instanceof Player p) putDown(p);
            else if (entityData.get(root ? LIFT_ROOT : LIFT_TAIL) >= 0) setLifter(root, null);
        }
    }

    /** Server: lifters gone, far off, riding or sneaking put their end down. */
    private void checkLifters() {
        for (boolean root : new boolean[] {true, false}) {
            if (entityData.get(root ? LIFT_ROOT : LIFT_TAIL) < 0) continue;
            Entity p = lifter(root);
            if (!(p instanceof Player player)) {
                setLifter(root, null);
            } else if (!p.isAlive() || p.isShiftKeyDown() || p.isPassenger() || p.position().distanceTo(endPoint(root)) > HOLD + LIFT_SLACK + 3) {
                putDown(player);
            }
        }
    }

    /**
     * Server: one step of being carried. Each lifted end goes towards its lifter (held {@link #HOLD} from them); an end on
     * the ground trails. Heavier is slower; past the lifters' strength it only heaves along now and then.
     */
    private boolean carry(ServerLevel level) {
        double load = liftLoad();
        double speed = CARRY_STEP * (load <= 1 ? 1 - 0.4 * load : 0.2 / (load * load)); // overloaded: a slow, even crawl
        Entity rl = rootLifted() ? lifter(true) : null, tl = tailLifted() ? lifter(false) : null;
        Vec3 root = endPoint(true), tail = endPoint(false);
        double len = size();
        Vec3 centre, f;
        if (rl != null && tl != null) {
            Vec3 tr = holdPoint(rl, root), tt = holdPoint(tl, tail);
            f = flat(tt.subtract(tr));
            if (f.lengthSqr() < 1.0E-4) return false;
            f = f.normalize();
            Vec3 want = tr.add(tt).scale(0.5);
            centre = position().add(clamp(flat(want.subtract(position())), speed));
        } else if (rl != null) {
            Vec3 newRoot = root.add(clamp(flat(holdPoint(rl, root).subtract(root)), speed));
            f = flat(tail.subtract(newRoot));
            if (f.lengthSqr() < 1.0E-4) return false;
            f = f.normalize();
            centre = newRoot.add(f.scale(len / 2));
        } else if (tl != null) {
            Vec3 newTail = tail.add(clamp(flat(holdPoint(tl, tail).subtract(tail)), speed));
            Vec3 back = flat(root.subtract(newTail));
            if (back.lengthSqr() < 1.0E-4) return false;
            back = back.normalize();
            f = back.reverse();
            centre = newTail.add(back.scale(len / 2));
        } else {
            return false;
        }
        centre = new Vec3(centre.x, getY(), centre.z);
        float yaw = (float) (Mth.atan2(-f.x, f.z) * Mth.RAD_TO_DEG);
        if (centre.distanceToSqr(position()) < 1.0E-5 && Math.abs(Mth.wrapDegrees(yaw - getYRot())) < 0.5F) return false;
        for (int up = 0; up <= 1; up++) {
            Vec3 at = centre.add(0, up, 0);
            if (!fits(level, at, yaw)) continue;
            setYRot(yaw);
            setPos(at);
            if (tickCount % 20 == 0) {
                for (Entity e : new Entity[] {rl, tl}) if (e instanceof ServerPlayer sp) Kettle.award(sp, HAUL_ADVANCEMENT);
                if (rl == null || tl == null) playSound(SoundEvents.WOOD_STEP, 0.5F, 0.6F); // the end on the ground scrapes along
            }
            return true;
        }
        return false;
    }

    /** Where a lifter wants the end now at {@code end}: {@link #HOLD} from them, towards it. */
    private static Vec3 holdPoint(Entity lifter, Vec3 end) {
        Vec3 d = flat(end.subtract(lifter.position()));
        return lifter.position().add(d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize().scale(HOLD));
    }

    private static Vec3 flat(Vec3 v) {
        return new Vec3(v.x, 0, v.z);
    }

    private static Vec3 clamp(Vec3 v, double max) {
        double l = v.length();
        return l > max ? v.scale(max / l) : v;
    }

    /** Does {@code e} handle this log (lifts or pulls it)? It doesn't bump into it then. Both sides. */
    public boolean handledBy(Entity e) {
        if (lifts(e)) return true;
        for (int id : pullerIds) if (id == e.getId()) return true;
        return !level().isClientSide() && pullers.contains(e);
    }

    /** The log {@code player} lifts or pulls (or their horse pulls), or null. Both sides: the Wagon loads it. */
    public static FelledLogEntity handled(Player player) {
        FelledLogEntity lifted = liftedBy(player);
        return lifted != null ? lifted : pulledBy(player, SNAP);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        lifting(level()).values().removeIf(l -> l == this);
        super.remove(reason);
    }

    // ---- interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || isPassenger()) return InteractionResult.PASS;
        int i = pick(player);
        if (i < 0) return InteractionResult.PASS;
        ItemStack held = player.getMainHandItem();
        boolean server = level() instanceof ServerLevel;
        if (held.is(Items.LEAD)) {
            if (server) tie((ServerLevel) level(), player, i, held);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && pulledBy(player)) {
            if (server) {
                Entity p = pullers.contains(player) ? player : player.getVehicle();
                pullers.remove(p);
                syncPullers();
                untie((ServerLevel) level(), p, false);
                player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.untied"));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && lifts(player)) {
            if (server) putDown(player);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            if (server) lift((ServerLevel) level(), player, i);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof SawItem) {
            SawItem.startSawingLog(player, hand, this, i);
            return InteractionResult.CONSUME;
        }
        if (held.is(ItemTags.AXES)) {
            if (server) chop((ServerLevel) level(), player, hand, i, held);
            return InteractionResult.CONSUME;
        }
        if (held.is(Watercraft.KNIVES)) {
            if (server) scoreDugout((ServerLevel) level(), player, hand, i, held);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private boolean isPullerClient(Player player) {
        for (int id : pullerIds) if (id == player.getId() || player.getVehicle() != null && id == player.getVehicle().getId()) return true;
        return false;
    }

    /** Server: a lead tied to the root end; riding a horse, the horse pulls. */
    void tie(ServerLevel level, Player player, int i, ItemStack lead) {
        if (size() <= HAND_MAX) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.lift_by_hand"));
            return;
        }
        BlockPos c = cells.get(i), root = cells.get(0);
        if (Math.max(Math.abs(c.getX() - root.getX()), Math.max(Math.abs(c.getY() - root.getY()), Math.abs(c.getZ() - root.getZ()))) > 1) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.root_end"));
            return;
        }
        Entity puller = player.getVehicle() instanceof AbstractHorse horse ? horse : player;
        if (pullers.contains(puller) || pullers.size() >= MAX_PULLERS) return;
        pullers.add(puller);
        syncPullers();
        lead.consume(1, player);
        wake();
        playSound(SoundEvents.LEAD_TIED, 1.0F, 0.8F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.felled_log.tied"));
    }

    /** Axe chops for the notch through a block, per chop a stage of a Fallen Log takes ({@link TreeChopping#chopsNeeded}). */
    static final int NOTCH_STAGES = 3;

    /**
     * Server: one axe chop at block {@code i} (held right- or left-click). Bark first: a block still in its bark is barked
     * (the chops of a Fallen Log's first stage, 1-2 Bark). Then, like the saw, a notch into the middle of the block, deeper
     * with each chop (the Fallen Log's kerf), until the block is cut through: it comes out as a hewn-through log (a Rough
     * Log at the axe's success chance, else Sticks) and the log parts there.
     */
    private void chop(ServerLevel level, Player player, InteractionHand hand, int i, ItemStack axe) {
        BlockState state = states.get(i);
        BlockPos at = cellPos(i);
        int perStage = TreeChopping.chopsNeeded(axe);
        if (!(state.getBlock() instanceof FallenLogBlock fallen)) { // no Fallen Log of this wood: all at once
            int needed = perStage * (FallenLogBlock.LAST_STAGE + 1);
            if (!TreeChopping.chop(level, player, hand, at, state, axe, needed)) {
                crack(i, TreeChopping.progress(level, at) * 10 / needed);
                return;
            }
            crack(-1, 0);
            Block.dropResources(state, level, at, null, player, axe);
            removeBlock(level, i);
            return;
        }
        if (state.getValue(FallenLogBlock.STAGE) == 0) { // the bark
            if (!TreeChopping.chop(level, player, hand, at, state, axe, perStage)) return;
            TreeChopping.hewDrops(level, at, fallen.wood(), 0, axe, player);
            setState(i, state.setValue(FallenLogBlock.STAGE, 1).setValue(FallenLogBlock.SAWN, 0));
            return;
        }
        int needed = perStage * NOTCH_STAGES;
        if (!TreeChopping.chop(level, player, hand, at, state, axe, needed)) {
            int deep = Mth.clamp(TreeChopping.progress(level, at) * FallenLogBlock.SAW_STROKES / needed, 1, FallenLogBlock.SAW_STROKES - 1);
            setState(i, state.setValue(FallenLogBlock.SAWN, deep));
            wake();
            return;
        }
        TreeChopping.hewDrops(level, at, fallen.wood(), FallenLogBlock.LAST_STAGE, axe, player);
        level.playSound(null, at, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F);
        removeBlock(level, i);
    }

    /** Server: one saw stroke ({@link SawItem}) across block {@code i}. */
    void sawStroke(ServerLevel level, Player player, InteractionHand hand, int i, ItemStack saw, boolean push) {
        sawBlock(level, player, hand, i, saw, push);
    }

    /** Server: one saw stroke across block {@code i} itself: the kerf one stroke deeper (a Fallen Log shows it); the last cuts it up as a sawn Fallen Log. */
    private void sawBlock(ServerLevel level, Player player, InteractionHand hand, int i, ItemStack saw, boolean push) {
        BlockPos at = cellPos(i);
        Vec3 w = world(i);
        BlockState state = states.get(i);
        level.playSound(null, w.x, w.y + 1, w.z, SoundEvents.AXE_SCRAPE.value(), SoundSource.BLOCKS, 0.6F, (push ? 1.35F : 1.1F) + random.nextFloat() * 0.1F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), w.x, w.y + 1, w.z, 3, 0.2, 0.02, 0.2, 0.02);
        int strokes = sawn.merge(i, 1, Integer::sum);
        if (strokes < FallenLogBlock.SAW_STROKES) {
            if (state.getBlock() instanceof FallenLogBlock) setState(i, state.setValue(FallenLogBlock.SAWN, strokes));
            else crack(i, strokes * 10 / FallenLogBlock.SAW_STROKES);
            wake();
            return;
        }
        sawn.remove(i);
        String wood = wood(i);
        if (wood != null) TreeChopping.sawDrops(level, at, wood, state.getBlock() instanceof FallenLogBlock ? state.getValue(FallenLogBlock.STAGE) : 0, player);
        else Block.dropResources(state, level, at, null, player, saw);
        level.playSound(null, at, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.1F);
        saw.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        player.stopUsingItem();
        removeBlock(level, i);
    }

    /** Server: a knife scores block {@code i} into a Dugout Blank lying where it is. */
    private void scoreDugout(ServerLevel level, Player player, InteractionHand hand, int i, ItemStack knife) {
        BlockPos at = cellPos(i);
        if (!level.getBlockState(at).canBeReplaced()) return;
        Direction.Axis axis = Direction.getApproximateNearest(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD)).getAxis();
        if (axis.isVertical()) axis = Direction.Axis.Z;
        level.setBlock(at, Watercraft.DUGOUT_BLANK.get().defaultBlockState().setValue(DugoutBlankBlock.AXIS, axis), Block.UPDATE_ALL);
        level.playSound(null, at, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.7F, 1.4F);
        knife.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        removeBlock(level, i);
    }

    /** Each block's box when the log lies on the block grid (vanilla collision); empty off it (LogCollision handles that). */
    public net.minecraft.world.phys.shapes.VoxelShape collisionShape() {
        int liftKey = (rootLifted() ? 1 : 0) + (tailLifted() ? 2 : 0);
        if (shape == null || shapeFor != getBoundingBox() || shapeLift != liftKey) {
            shapeLift = liftKey;
            net.minecraft.world.phys.shapes.VoxelShape out = net.minecraft.world.phys.shapes.Shapes.empty();
            // on the grid one box per block, exactly the block; turned off it vanilla gets nothing (it only knows upright
            // boxes): LogCollision clips movement against the turned blocks instead
            if (onGrid()) {
                for (int i = 0; i < cells.size(); i++) {
                    Vec3 w = world(i).add(0, liftOffset(i), 0);
                    out = net.minecraft.world.phys.shapes.Shapes.joinUnoptimized(out, net.minecraft.world.phys.shapes.Shapes.create(w.x - 0.5, w.y, w.z - 0.5, w.x + 0.5, w.y + 1, w.z + 0.5),
                            net.minecraft.world.phys.shapes.BooleanOp.OR);
                }
            }
            shape = out.optimize();
            shapeFor = getBoundingBox();
        }
        return shape;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        // on a wagon it would block the wagon's own team and riders; whoever lifts or pulls it doesn't bump into it
        return !isPassenger() && (other == null || !handledBy(other));
    }

    /** The crack overlay: block {@code i} at stage {@code stage} (0-9), or none. Server. */
    void crack(int i, int stage) {
        entityData.set(CRACK, i < 0 ? -1 : i * 16 + Math.min(9, stage));
        lastCrack = level().getGameTime();
        wake(); // so the crack can fade
    }

    /** Client: the cracked block's index, or -1. */
    public int crackedBlock() {
        int c = entityData.get(CRACK);
        return c < 0 ? -1 : c / 16;
    }

    /** Client: the crack stage 0-9. */
    public int crackStage() {
        return Math.max(0, entityData.get(CRACK)) % 16;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return vehicle instanceof TravoisEntity bed && bed.logSlots() > 0;
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        wake();
    }

    // ---- sync and save

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PULLERS, "");
        builder.define(CRACK, -1);
        builder.define(STAGES, "");
        builder.define(LIFT_ROOT, -1);
        builder.define(LIFT_TAIL, -1);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            buf.writeBlockPos(cells.get(i));
            buf.writeVarInt(Block.getId(states.get(i)));
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<BlockPos> c = new ArrayList<>();
        List<BlockState> s = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            c.add(buf.readBlockPos());
            s.add(Block.stateById(buf.readVarInt()));
        }
        setShape(c, s);
        applyStages();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        int[] flat = input.getIntArray("cells").orElse(new int[0]);
        List<BlockState> s = input.read("states", BlockState.CODEC.listOf()).orElse(List.of());
        List<BlockPos> c = new ArrayList<>();
        for (int i = 0; i + 2 < flat.length; i += 3) c.add(new BlockPos(flat[i], flat[i + 1], flat[i + 2]));
        if (c.size() == s.size()) setShape(c, s);
        leadsToReturn = input.getIntOr("leads", 0);
        wake();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        int[] flat = new int[cells.size() * 3];
        for (int i = 0; i < cells.size(); i++) {
            flat[i * 3] = cells.get(i).getX();
            flat[i * 3 + 1] = cells.get(i).getY();
            flat[i * 3 + 2] = cells.get(i).getZ();
        }
        output.putIntArray("cells", flat);
        output.store("states", BlockState.CODEC.listOf(), states);
        output.putInt("leads", pullers.size() + leadsToReturn);
    }
}
