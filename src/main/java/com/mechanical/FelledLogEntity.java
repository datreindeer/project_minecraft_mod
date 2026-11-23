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
    public static final double LIFT = 2.5;
    /** Movement speed lost under a log: a lifter or puller moves at 1 / (1 + BURDEN * load) of their speed. */
    static final double BURDEN = 0.8;
    private static final Identifier WEIGHT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "log_weight");
    /** A lifted end is held this far from the player; the player stays within HOLD + LIFT_SLACK of it (drawn in if further). */
    static final double HOLD = 0.4, LIFT_SLACK = 0.25;
    /** Blocks a tick a lifter standing off the end is drawn in to it. */
    static final double DRAW_IN = 0.12;
    /** A dragged log this long (blocks) or shorter swings round freely; longer ones resist being swung sideways (share 3 / length). */
    static final double NIMBLE = 3;
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
    /** Lying on a slope: how much higher each next block from the root end lies (blocks per block; straight logs). */
    private static final EntityDataAccessor<Float> TILT = SynchedEntityData.defineId(FelledLogEntity.class, EntityDataSerializers.FLOAT);
    /**
     * Heavy, rough bark: it starts sliding down along its length only steeper than SLIDE_TILT (per block, ~31 degrees) and
     * when its supports lie more than STEP_RISE apart in height (a log over a one-block step stays put), and once sliding keeps on
     * while steeper than KINETIC_TILT. Never steeper than MAX_TILT.
     */
    static final double SLIDE_TILT = 0.6, KINETIC_TILT = 0.45, STEP_RISE = 1.05, MAX_TILT = 0.75;
    /** Sliding speed (blocks per tick): it gathers SLIDE_ACCEL a tick up to SLIDE_SPEED per unit of tilt past KINETIC_TILT, within MIN..MAX. */
    static final double SLIDE_ACCEL = 0.004, SLIDE_SPEED = 0.3, MIN_SLIDE = 0.02, MAX_SLIDE = 0.1;
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
    private double fallSpeed, slideSpeed;
    /** Leads tied on when it was saved; dropped on load (the pullers aren't saved). */
    private int leadsToReturn;
    /** Server: who pulls it. */
    private final List<Entity> pullers = new ArrayList<>();
    /** Server: how far each block is cut through, in saw strokes (an axe's chop counts a share): saw and axe deepen the same cut. */
    private final Map<Integer, Double> cut = new HashMap<>();
    /** Each block's cut: its plane and opening, as {@link #facing} codes it (towards whoever started it). Synced in STAGES. */
    private final List<Integer> rolls = new ArrayList<>();
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
        rolls.clear();
        for (int i = 0; i < cells.size(); i++) rolls.add(0);
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
        for (int i = 0; i < states.size(); i++) {
            BlockState st = states.get(i);
            int v = st.getBlock() instanceof FallenLogBlock ? st.getValue(FallenLogBlock.STAGE) * 8 + st.getValue(FallenLogBlock.SAWN) : 0;
            b.append((char) ('0' + rolls.get(i) * 32 + v));
        }
        entityData.set(STAGES, b.toString());
    }

    /** Block {@code i}'s cut: plane * 8 + opening ({@link #facing}). Both sides. */
    public int roll(int i) {
        return i < rolls.size() ? rolls.get(i) : 0;
    }

    /** These blocks' cuts open as in {@code from} (a piece of a log keeps its blocks' cuts). */
    private void setRolls(List<Integer> from) {
        for (int i = 0; i < rolls.size() && i < from.size(); i++) rolls.set(i, from.get(i));
        if (!level().isClientSide()) syncStages();
    }

    /** Client: the synced stages onto the blocks. */
    private void applyStages() {
        String b = entityData.get(STAGES);
        if (b.length() != states.size()) return;
        for (int i = 0; i < states.size(); i++) {
            int v = b.charAt(i) - '0';
            rolls.set(i, v / 32);
            v %= 32;
            if (!(states.get(i).getBlock() instanceof FallenLogBlock)) continue;
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
            double up = entityData == null ? 0 : liftOffset(i);
            minX = Math.min(minX, w.x);
            maxX = Math.max(maxX, w.x);
            minY = Math.min(minY, w.y + up);
            maxY = Math.max(maxY, w.y + up);
            minZ = Math.min(minZ, w.z);
            maxZ = Math.max(maxZ, w.z);
        }
        // half a block turned to the yaw: 0.5 on the grid, up to 0.71 at 45°
        double r = getYRot() * Mth.DEG_TO_RAD, m = onGrid() ? 0.5 : 0.5 * (Math.abs(Math.sin(r)) + Math.abs(Math.cos(r)));
        return new AABB(minX - m, minY, minZ - m, maxX + m, maxY + 1, maxZ + m);
    }

    /** Client: this client's player lifts an end: it carries it here too and ignores the server's (older) places for it. */
    @Override
    protected boolean isLocalClientAuthoritative() {
        for (boolean root : new boolean[] {true, false}) if (lifter(root) instanceof Player p && p.isLocalPlayer()) return true;
        return false;
    }

    /** Others' clients: moved smoothly between the server's places (every 2 ticks). */
    @Override
    protected net.minecraft.world.entity.InterpolationHandler createInterpolationHandler() {
        return net.minecraft.world.entity.LinearInterpolationHandler.create(this, 2);
    }

    @Override
    public void setYRot(float yRot) {
        // only the angle clients get (a byte: 1/256 turn), so both sides collide alike, and a lifter's client carrying it
        // turns it in the same steps as the server
        if (level() != null) yRot = Mth.unpackDegrees(Mth.packDegrees(yRot));
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
     * {@code base}; the tree tips over towards {@code direction}: height becomes distance along the fall line, the root
     * end just past the stump. Blocks hold together only end to end along their grain (a log's top to the next one's
     * bottom), never side to side or corner to corner: the trunk is one log, each branch across the fall line its own,
     * a branch along the fall line swings down to lie along it. A block landing where another already lies goes one
     * block to the side, or splinters into Sticks.
     */
    public static void land(ServerLevel level, BlockPos base, Direction direction, List<BlockPos> offsets, List<BlockState> logStates) {
        float yaw = direction.toYRot();
        Direction.Axis sideAxis = direction.getClockWise().getAxis();
        Vec3 side = new Vec3(1, 0, 0).yRot(-yaw * Mth.DEG_TO_RAD), origin = Vec3.atBottomCenterOf(base);
        Integer[] order = new Integer[offsets.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        // the trunk first (upright logs), then by height
        java.util.Arrays.sort(order, Comparator.comparingInt((Integer i) -> grainAxis(logStates.get(i)) == Direction.Axis.Y ? 0 : 1)
                .thenComparingInt(i -> offsets.get(i).getY()));
        Map<BlockPos, BlockState> landed = new HashMap<>();
        Map<BlockPos, Boolean> across = new HashMap<>(); // grain across the fall line (else along it)
        for (int i : order) {
            BlockPos o = offsets.get(i);
            Direction.Axis grain = grainAxis(logStates.get(i));
            int x = (int) Math.round(o.getX() * side.x + o.getZ() * side.z), z = o.getY() + 1;
            boolean isAcross = grain == sideAxis;
            if (grain.isHorizontal() && !isAcross) z += o.getX() * direction.getStepX() + o.getZ() * direction.getStepZ();
            BlockPos c = new BlockPos(x, 0, z);
            if (landed.containsKey(c)) c = !landed.containsKey(c.east()) ? c.east() : !landed.containsKey(c.west()) ? c.west() : null;
            if (c == null) {
                Vec3 w = origin.add(new Vec3(x, 0, z).yRot(-yaw * Mth.DEG_TO_RAD));
                Block.popResource(level, BlockPos.containing(w.add(0, 1, 0)), new ItemStack(Items.STICK, 2));
                continue;
            }
            landed.put(c, logStates.get(i));
            across.put(c, isAcross);
        }
        // runs end to end along the grain: along the fall line (local z) or across it (local x)
        java.util.Set<BlockPos> done = new java.util.HashSet<>();
        for (BlockPos start : landed.keySet()) {
            if (done.contains(start)) continue;
            boolean isAcross = across.get(start);
            BlockPos step = isAcross ? new BlockPos(1, 0, 0) : new BlockPos(0, 0, 1);
            BlockPos first = start;
            while (landed.containsKey(first.subtract(step)) && across.get(first.subtract(step)) == isAcross) first = first.subtract(step);
            List<BlockState> run = new ArrayList<>();
            for (BlockPos c = first; landed.containsKey(c) && across.get(c) == isAcross; c = c.offset(step)) {
                run.add(landed.get(c));
                done.add(c);
            }
            Vec3 root = origin.add(new Vec3(first.getX(), 0, first.getZ()).yRot(-yaw * Mth.DEG_TO_RAD));
            dropLine(level, root, isAcross ? yaw - 90 : yaw, run);
        }
    }

    /** The way a log block's grain runs (upright if it has none). */
    private static Direction.Axis grainAxis(BlockState state) {
        return state.hasProperty(BlockStateProperties.AXIS) ? state.getValue(BlockStateProperties.AXIS) : Direction.Axis.Y;
    }

    /** Server: a straight log of {@code run}, its root block's bottom middle at {@code root} (x, z), lying along {@code yaw}, set down on the ground there. */
    private static void dropLine(ServerLevel level, Vec3 root, float yaw, List<BlockState> run) {
        FelledLogEntity log = new FelledLogEntity(Mechanical.FELLED_LOG.get(), level);
        List<BlockPos> line = new ArrayList<>();
        for (int i = 0; i < run.size(); i++) line.add(new BlockPos(0, 0, i));
        log.setShape(line, run);
        log.setYRot(yaw);
        Vec3 pos = root.add(Vec3.directionFromRotation(0, yaw).scale((run.size() - 1) / 2.0));
        int y = level.getMinY(); // high enough that no block is in the ground; it settles from there
        for (int i = 0; i < log.size(); i++) {
            Vec3 w = log.world(i, pos, log.getYRot());
            y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(w.x), Mth.floor(w.z)));
        }
        int lift = 0; // on top of a log already lying there
        while (lift < 4 && !log.fits(level, new Vec3(pos.x, y + lift, pos.z), log.getYRot())) lift++;
        log.setPos(pos.x, y + lift, pos.z);
        for (int i = 0; i < log.size(); i++) crush(level, log.cellPos(i));
        level.addFreshEntity(log);
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

    /** Groups of cells joined end to end along the grain (local z): side by side or corner to corner never holds. Breadth-first from the first cell in list order. */
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
                for (BlockPos n : new BlockPos[] {cells.get(i).north(), cells.get(i).south()}) {
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
    private void rebuild(ServerLevel level, List<BlockPos> newCells, List<BlockState> newStates, List<Integer> newRolls, boolean keepPullers) {
        List<List<Integer>> parts = components(newCells);
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
            log.setRolls(part.stream().map(newRolls::get).toList());
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
        List<Integer> r = new ArrayList<>(rolls);
        c.remove(i);
        s.remove(i);
        r.remove(i);
        rebuild(level, c, s, r, i != 0);
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
        log.setRolls(rolls);
        log.setYRot(yaw);
        log.ignore = this;
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

    /** What it can lie on besides blocks: water (logs float). */
    private static boolean water(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    /** Another log it may overlap while it is being replaced by this one (straighten). */
    private FelledLogEntity ignore;

    // ---- the real block shapes (slabs, stairs, paths, snow layers...) along the log's whole footprint

    /** The log's sides are drawn in this much when tested against blocks: it may touch them, not overlap them. */
    private static final double SKIN = 0.02;

    /**
     * A row of blocks on the log's own grid as one strip: local x in x0 ± 0.5, local z (along the log) z0..z1; its bottom in
     * the world at local z is y + c0 + c1 * z (tilted on a slope, or by one lifted end).
     */
    record Strip(double x0, double z0, double z1, double c0, double c1) {
        double bottom(double y, double z) {
            return y + c0 + c1 * z;
        }
    }

    /** The log as strips with tilt {@code k}: a straight log is one (raised or tilted by its lifters), any other one per block. */
    List<Strip> strips(double k) {
        int n = size();
        if (!straight()) {
            List<Strip> out = new ArrayList<>();
            for (BlockPos c : cells) out.add(new Strip(c.getX() - cx, c.getZ() - cz - 0.5, c.getZ() - cz + 0.5, c.getY(), 0));
            return out;
        }
        boolean r = rootLifted(), t = tailLifted();
        if (r && t) return List.of(new Strip(0, -n / 2.0, n / 2.0, LIFT_HEIGHT, 0));
        if (r || t) return List.of(new Strip(0, -n / 2.0, n / 2.0, LIFT_HEIGHT / 2, (r ? -1 : 1) * LIFT_HEIGHT / n)); // as liftOffset
        return List.of(new Strip(0, -n / 2.0, n / 2.0, 0, k));
    }

    /** The world blocks' collision boxes (leaves excluded; water as a box to its surface if {@code withWater}) near strip {@code s} between two heights. */
    private static List<AABB> boxes(Level level, Strip s, Vec3 pos, float yaw, double yLo, double yHi, boolean withWater) {
        double r = -yaw * Mth.DEG_TO_RAD, cos = Math.cos(r), sin = Math.sin(r);
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int k = 0; k < 4; k++) { // the strip's corners in the world (as Vec3#yRot(-yaw))
            double lx = s.x0 + (k % 2 == 0 ? -0.5 : 0.5), lz = k < 2 ? s.z0 : s.z1;
            double wx = pos.x + lx * cos + lz * sin, wz = pos.z + lz * cos - lx * sin;
            minX = Math.min(minX, wx);
            maxX = Math.max(maxX, wx);
            minZ = Math.min(minZ, wz);
            maxZ = Math.max(maxZ, wz);
        }
        List<AABB> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(Mth.floor(minX), Mth.floor(yLo), Mth.floor(minZ), Mth.floor(maxX), Mth.floor(yHi), Mth.floor(maxZ))) {
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && !st.is(BlockTags.LEAVES)) for (AABB b : st.getCollisionShape(level, p).toAabbs()) out.add(b.move(p));
            if (withWater && water(level, p) && !water(level, p.above()))
                out.add(new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + level.getFluidState(p).getHeight(level, p), p.getZ() + 1));
        }
        return out;
    }

    /**
     * The stretch of local z over which world box {@code b}'s footprint overlaps strip {@code s} of a log at {@code pos}
     * turned to {@code yaw}, its sides drawn in by {@link #SKIN}; null if they don't overlap. The box's footprint is turned
     * onto the log's grid and clipped to the strip.
     */
    static double[] span(Strip s, Vec3 pos, float yaw, AABB b) {
        double r = yaw * Mth.DEG_TO_RAD, cos = Math.cos(r), sin = Math.sin(r);
        double[] xs = new double[12], zs = new double[12];
        double[][] corners = {{b.minX, b.minZ}, {b.maxX, b.minZ}, {b.maxX, b.maxZ}, {b.minX, b.maxZ}};
        for (int k = 0; k < 4; k++) { // as toLocal: Vec3#yRot(yaw)
            double x = corners[k][0] - pos.x, z = corners[k][1] - pos.z;
            xs[k] = x * cos + z * sin;
            zs[k] = z * cos - x * sin;
        }
        int n = clip(xs, zs, 4, true, s.x0 - 0.5 + SKIN, true);
        n = clip(xs, zs, n, true, s.x0 + 0.5 - SKIN, false);
        n = clip(xs, zs, n, false, s.z0 + SKIN, true);
        n = clip(xs, zs, n, false, s.z1 - SKIN, false);
        if (n < 3) return null;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            lo = Math.min(lo, zs[k]);
            hi = Math.max(hi, zs[k]);
        }
        return new double[] {lo, hi};
    }

    /** The convex polygon (xs, zs; n corners) cut to x (or z) above (or below) {@code bound}; returns its new corner count. */
    private static int clip(double[] xs, double[] zs, int n, boolean onX, double bound, boolean above) {
        double[] ox = java.util.Arrays.copyOf(xs, n), oz = java.util.Arrays.copyOf(zs, n);
        int m = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double vi = onX ? ox[i] : oz[i], vj = onX ? ox[j] : oz[j];
            boolean inI = above ? vi >= bound : vi <= bound, inJ = above ? vj >= bound : vj <= bound;
            if (inI) {
                xs[m] = ox[i];
                zs[m++] = oz[i];
            }
            if (inI != inJ) {
                double f = (bound - vi) / (vj - vi);
                xs[m] = ox[i] + f * (ox[j] - ox[i]);
                zs[m++] = oz[i] + f * (oz[j] - oz[i]);
            }
        }
        return m;
    }

    /** Would it be clear of every block shape and of other felled logs at {@code pos} turned to {@code yaw}? */
    boolean fits(Level level, Vec3 pos, float yaw) {
        return fits(level, pos, yaw, tilt());
    }

    /** As {@link #fits(Level, Vec3, float)}, lying with tilt {@code k}. */
    boolean fits(Level level, Vec3 pos, float yaw, double k) {
        for (Strip s : strips(k)) {
            double b0 = s.bottom(pos.y, s.z0), b1 = s.bottom(pos.y, s.z1);
            for (AABB b : boxes(level, s, pos, yaw, Math.min(b0, b1), Math.max(b0, b1) + 1, false)) {
                double[] sp = span(s, pos, yaw, b);
                if (sp == null) continue;
                double lo = s.bottom(pos.y, sp[0]), hi = s.bottom(pos.y, sp[1]);
                if (b.maxY > Math.min(lo, hi) + SKIN && b.minY < Math.max(lo, hi) + 1 - SKIN) return false;
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            Vec3 w = world(i, pos, yaw).add(0, 0.5 + (straight() && lifterCount() == 0 ? k * localCell(i).z : liftOffset(i)), 0);
            // the middle and near the four corners of the block, on its own grid
            for (int c = 0; c < 5; c++) {
                Vec3 off = c == 0 ? Vec3.ZERO : new Vec3(c % 2 == 0 ? 0.35 : -0.35, 0, c < 3 ? 0.35 : -0.35).yRot(-yaw * Mth.DEG_TO_RAD);
                if (inOtherLog(level, w.add(off), this)) return false;
            }
        }
        return true;
    }

    /**
     * What would hold strip {@code s} up (lying at height {@code y} with its own tilt): the tops of the block shapes, water
     * and other felled logs under its footprint, from {@code above} over its bottom down to {@code depth} under it, as
     * (local z, top) points: the two ends of each one's stretch under it. A full block's top a whole block up is not under
     * it but a wall beside it (it never climbs walls); a slab's top half a block up is, so a log sunk into one rises onto it.
     */
    private List<double[]> supports(Level level, Strip s, double y, double above, double depth) {
        List<double[]> out = new ArrayList<>();
        double b0 = s.bottom(y, s.z0), b1 = s.bottom(y, s.z1);
        Vec3 pos = new Vec3(getX(), y, getZ());
        for (AABB b : boxes(level, s, pos, getYRot(), Math.min(b0, b1) - depth - 1, Math.max(b0, b1) + above, true)) {
            double[] sp = span(s, pos, getYRot(), b);
            if (sp == null) continue;
            for (double z : sp) {
                double bottom = s.bottom(y, z);
                if (b.maxY <= bottom + above && b.maxY > bottom - depth) out.add(new double[] {z, b.maxY});
            }
        }
        // other logs: their blocks' tops, sampled along the strip and across it
        double step = 0.5;
        for (double z = s.z0 + 0.05; z <= s.z1 - 0.05 + 1.0E-6; z += step) {
            for (double x : new double[] {s.x0 - 0.35, s.x0, s.x0 + 0.35}) {
                double bottom = s.bottom(y, z);
                double top = otherLogTop(level, toWorld(new Vec3(x, 0, z)), bottom - depth, bottom + above);
                if (top > -Double.MAX_VALUE) out.add(new double[] {z, top});
            }
        }
        return out;
    }

    /** The highest top of another felled log's block over the world point (x, z) between {@code lo} and {@code hi}, or -MAX_VALUE. */
    private double otherLogTop(Level level, Vec3 p, double lo, double hi) {
        double best = -Double.MAX_VALUE;
        for (FelledLogEntity o : level.getEntitiesOfClass(FelledLogEntity.class, new AABB(p.x, lo - 1, p.z, p.x, hi + 1, p.z).inflate(0.05),
                l -> l != this && l != ignore && !l.isPassenger() && l.isAlive())) {
            Vec3 l = o.toLocal(p);
            for (int i = 0; i < o.size(); i++) {
                Vec3 c = o.localCell(i);
                double top = o.getY() + c.y + o.liftOffset(i) + 1;
                if (Math.abs(l.x - c.x) < 0.5 && Math.abs(l.z - c.z) < 0.5 && top > lo && top <= hi) best = Math.max(best, top);
            }
        }
        return best;
    }

    /** Is the world point {@code p} inside a block of a felled log other than {@code except} (and not one riding a bed)? */
    static boolean inOtherLog(Level level, Vec3 p, FelledLogEntity except) {
        for (FelledLogEntity o : level.getEntitiesOfClass(FelledLogEntity.class, new AABB(p, p).inflate(0.05),
                l -> l != except && (except == null || l != except.ignore) && !l.isPassenger() && l.isAlive())) {
            Vec3 l = o.toLocal(p);
            for (int i = 0; i < o.size(); i++) {
                Vec3 c = o.localCell(i);
                double bottom = o.getY() + c.y + o.liftOffset(i);
                if (Math.abs(l.x - c.x) < 0.5 && Math.abs(l.z - c.z) < 0.5 && p.y > bottom && p.y < bottom + 1) return true;
            }
        }
        return false;
    }

    private void wake() {
        awake = true;
        restTicks = 0;
    }

    /** Server: does anything under its lowest layer hold it up? */
    private boolean supported() {
        if (straight()) {
            Strip s = strips(tilt()).get(0);
            for (double[] p : supports(level(), s, getY(), 0.3, 0.05)) if (p[1] > s.bottom(getY(), p[0]) - 0.05) return true;
            return false;
        }
        int y = Mth.floor(getY() + 0.01) - 1;
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getY() != 0) continue;
            Vec3 w = world(i);
            BlockPos under = BlockPos.containing(w.x, y, w.z);
            if (blocks(level(), under) || water(level(), under) || inOtherLog(level(), new Vec3(w.x, y + 0.5, w.z), this)) return true;
        }
        return false;
    }

    /** Blocks it can't pass through (leaves give way): the old block-centre test, for logs that never were straightened. */
    private static boolean blocks(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.is(BlockTags.LEAVES) && !s.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean holds(Level level, BlockPos pos) {
        return blocks(level, pos) || water(level, pos);
    }

    /** One step of falling or sliding off a ledge; true if it moved. Server, and the client of its lifter (carry). */
    private boolean settle(Level level) {
        if (straight()) return lifterCount() == 0 && !isPulled() ? settleOnSlope(level) : settleHandled(level);
        if (tilt() != 0) entityData.set(TILT, 0.0F);
        int baseY = Mth.floor(getY() + 0.01);
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE, comX = 0, comZ = 0;
        boolean any = false;
        for (int i = 0; i < cells.size(); i++) {
            BlockPos c = cells.get(i);
            comX += c.getX();
            comZ += c.getZ();
            if (c.getY() != 0) continue;
            Vec3 w = world(i);
            if (!holds(level, BlockPos.containing(w.x, baseY - 1, w.z)) && !inOtherLog(level, new Vec3(w.x, baseY - 0.5, w.z), this)) continue; // ground, water or a log under it (legacy shapes only)
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

    /** How far below a block the ground may be and still hold it up: deeper, the block is over a gap (a log bridges it). */
    static final double SUPPORT_DEPTH = 1.5;
    /** How far above its bottom a block's top still counts as under it: a slab it sank into, not a full block (a wall). */
    static final double SUPPORT_ABOVE = 0.99;

    /**
     * A straight log someone lifts or pulls: held at their tilt (or level), it rests on the highest thing under its footprint
     * (stepping up onto slabs and paths, down off them), or falls.
     */
    private boolean settleHandled(Level level) {
        if (tilt() != 0) entityData.set(TILT, 0.0F);
        Strip s = strips(0).get(0);
        double rest = -Double.MAX_VALUE;
        for (double[] p : supports(level, s, getY(), SUPPORT_ABOVE, SUPPORT_DEPTH)) rest = Math.max(rest, p[1] - s.bottom(0, p[0]));
        if (rest == -Double.MAX_VALUE) { // nothing under it
            fallSpeed = Math.min(fallSpeed + 0.08, 1.0);
            setPos(getX(), getY() - fallSpeed, getZ());
            return true;
        }
        if (Math.abs(rest - getY()) < 1.0E-4) {
            fallSpeed = 0;
            return false;
        }
        if (rest < getY()) {
            fallSpeed = Math.min(fallSpeed + 0.08, 1.0);
            setPos(getX(), Math.max(rest, getY() - fallSpeed), getZ());
        } else if (fits(level, new Vec3(getX(), rest, getZ()), getYRot())) {
            fallSpeed = 0;
            setPos(getX(), rest, getZ());
        } else return false;
        return true;
    }

    /**
     * Server, a straight log: it rests like a stiff pole on what lies under its blocks (ground, water, other logs within
     * {@link #SUPPORT_DEPTH}; deeper is a gap it bridges): along the lowest line over those supports that has support on
     * both sides of its middle, so it tilts along a slope or over a bump but never hangs in the air. With support on one
     * side of its middle only, it slides off level that way, over the edge, and falls. Steeper than SLIDE_TILT (and more than
     * a step from end to end) it slides slowly down along its length until the ground stops it. True if it moved.
     */
    private boolean settleOnSlope(Level level) {
        int n = size();
        double k0 = tilt();
        // (t along it from the middle, top) under its whole footprint: the real shapes, so it lies on slabs, not in them
        List<double[]> supports = supports(level, strips(k0).get(0), getY(), SUPPORT_ABOVE, SUPPORT_DEPTH);
        if (supports.isEmpty()) { // nothing under it: it falls, level
            fallSpeed = Math.min(fallSpeed + 0.08, 1.0);
            setPos(getX(), getY() - fallSpeed, getZ());
            if (k0 != 0) entityData.set(TILT, 0.0F);
            return true;
        }
        boolean left = false, right = false;
        for (double[] p : supports) {
            left |= p[0] < -0.02; // something under it on that side of its middle (exactly at the middle is not enough)
            right |= p[0] > 0.02;
        }
        if (!left || !right) { // its middle is past its supports: it slides off level that way and drops
            if (k0 != 0) entityData.set(TILT, 0.0F);
            Vec3 off = Vec3.directionFromRotation(0, getYRot()).scale(left ? 0.2 : -0.2); // towards the unsupported end
            double top = -Double.MAX_VALUE;
            for (double[] p : supports) top = Math.max(top, p[1]);
            Vec3 at = new Vec3(getX(), Math.max(top, getY() - 0.2), getZ()).add(off);
            if (fits(level, at, getYRot(), 0)) {
                setPos(at);
                return true;
            }
            if (top > getY() && fits(level, new Vec3(getX(), top, getZ()), getYRot(), 0)) setPos(getX(), top, getZ());
            return false;
        }
        // the lowest line over the supports with one on each side of the middle (the bridge of their lower hull)
        double bestK = 0, bestMid = Double.MAX_VALUE;
        for (double[] l : supports) {
            if (l[0] >= -0.02) continue;
            for (double[] r : supports) {
                if (r[0] <= 0.02) continue;
                double k = n == 1 || r[0] - l[0] < 1.0E-6 ? 0 : (r[1] - l[1]) / (r[0] - l[0]); // a single block lies level
                k = Mth.clamp(k, -MAX_TILT, MAX_TILT);
                double mid = -Double.MAX_VALUE;
                for (double[] p : supports) mid = Math.max(mid, p[1] - k * p[0]);
                if (mid < bestMid - 1.0E-6) {
                    bestMid = mid;
                    bestK = k;
                }
            }
        }
        double k = bestK, mid = bestMid;
        boolean moved = false;
        if (Math.abs(k - k0) > 1.0E-4 && !fits(level, new Vec3(getX(), Math.max(mid, getY() - fallSpeed), getZ()), getYRot(), k)) k = k0; // it can't turn that way (something over it)
        if (mid < getY() - 0.01) { // coming down onto it
            fallSpeed = Math.min(fallSpeed + 0.08, 1.0);
            double y = Math.max(mid, getY() - fallSpeed);
            if (y <= mid + 1.0E-6) {
                if (fallSpeed > 0.4 && !level.isClientSide()) playSound(SoundEvents.WOOD_FALL, 1.0F, 0.6F);
                fallSpeed = 0;
            }
            setPos(getX(), y, getZ());
            moved = true;
        } else if (mid > getY() + 1.0E-4 && fits(level, new Vec3(getX(), mid, getZ()), getYRot(), k)) { // sunk into a slab: up onto it
            setPos(getX(), mid, getZ());
            fallSpeed = 0;
            moved = true;
        }
        if (mid >= getY() - 0.01) fallSpeed = 0; // resting on it (a drop that ended within the margin left the fall running, and no slide)
        if (Math.abs(k - k0) > 1.0E-4) {
            entityData.set(TILT, (float) k);
            setBoundingBox(makeBoundingBox());
            moved = true;
        }
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (double[] p : supports) {
            lo = Math.min(lo, p[0]);
            hi = Math.max(hi, p[0]);
        }
        boolean slides = fallSpeed == 0 && (slideSpeed > 0 ? Math.abs(k) > KINETIC_TILT : Math.abs(k) > SLIDE_TILT && Math.abs(k) * (hi - lo) > STEP_RISE);
        if (!slides) slideSpeed = 0;
        else { // too steep: it slides towards its lower end, slowly gathering speed
            slideSpeed = Math.min(slideSpeed + SLIDE_ACCEL, Mth.clamp(SLIDE_SPEED * (Math.abs(k) - KINETIC_TILT), MIN_SLIDE, MAX_SLIDE));
            Vec3 down = Vec3.directionFromRotation(0, getYRot()).scale(k > 0 ? -1 : 1);
            Vec3 at = position().add(down.scale(slideSpeed)).add(0, -slideSpeed * Math.abs(k), 0);
            if (fits(level, at, getYRot())) {
                setPos(at);
                if (tickCount % 10 == 0 && !level.isClientSide()) playSound(SoundEvents.WOOD_STEP, 0.7F, 0.5F);
                moved = true;
            } else slideSpeed = 0;
        }
        return moved;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            if (!isPassenger() && !cells.isEmpty() && lifterCount() > 0 && isLocalInstanceAuthoritative()) {
                // this client's player lifts it: carried here as on the server, so the end stays at their hands
                Vec3 server = getPositionCodec().getBase();
                if (server.distanceToSqr(position()) > 4) snapTo(server.x, server.y, server.z, getYRot(), getXRot());
                carry(level());
                settle(level());
            }
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
        if ((lifterCount() > 0 || !pullers.isEmpty()) && tilt() != 0) entityData.set(TILT, 0.0F); // handled, it is held level
        boolean moved = !pullers.isEmpty() && drag(level);
        if (lifterCount() > 0 && !isRemoved()) moved |= carry(level);
        if (isRemoved()) return; // straightened into a new log
        burdenAll();
        moved |= settle(level);
        if (moved || !pullers.isEmpty() || lifterCount() > 0 || entityData.get(CRACK) >= 0) restTicks = 0;
        else if (++restTicks > 20) awake = false;
    }

    /** Is it lined up with the block grid? */
    public boolean onGrid() {
        return Math.abs(Mth.wrapDegrees(getYRot() - Math.round(getYRot() / 90.0F) * 90.0F)) < 0.01F;
    }

    /** Server: each lifter and puller slowed by the load they bear. */
    private void burdenAll() {
        double lift = liftLoad();
        for (boolean root : new boolean[] {true, false}) {
            Entity e = entityData.get(root ? LIFT_ROOT : LIFT_TAIL) >= 0 ? lifter(root) : null;
            if (e != null) burden(e, 1 / (1 + BURDEN * lift));
        }
        if (!pullers.isEmpty()) {
            double force = 0;
            for (Entity p : pullers) force += strength(p);
            double load = size() / (force * PER_PULLER);
            for (Entity p : pullers) burden(p, 1 / (1 + BURDEN * load));
        }
    }

    /** {@code e} moves at {@code factor} of its speed (1: freed of the log). */
    static void burden(Entity e, double factor) {
        if (!(e instanceof net.minecraft.world.entity.LivingEntity living)) return;
        var speed = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (factor >= 0.999) speed.removeModifier(WEIGHT);
        else speed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(WEIGHT, factor - 1,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
        burden(puller, 1);
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
        for (double up : new double[] {0, 0.5, 1}) {
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
            tether(mover, endPoint(root), HOLD + LIFT_SLACK, DRAW_IN); // hands at the end: drawn in if they stand off it
            keepOut(mover, root);
            mover.setSprinting(false);
        }
        if (pullerIds.length == 0) return;
        Vec3 root = world(0);
        for (int id : pullerIds) {
            Entity mover = level().getEntity(id);
            if (mover != null && mover.isLocalInstanceAuthoritative()) tether(mover, root, TETHER, 0);
        }
    }

    /**
     * Client: holds {@code mover} within {@code reach} of {@code grip} (horizontally): no further out than it was, no
     * speed outwards; standing further off, it is drawn in by {@code drawIn} a tick.
     */
    private static void tether(Entity mover, Vec3 grip, double reach, double drawIn) {
        Vec3 off = mover.position().subtract(grip).multiply(1, 0, 1);
        double d = off.length();
        if (d <= reach) return;
        Vec3 n = off.scale(1 / d);
        double limit = Math.max(reach, Math.min(d, new Vec3(mover.xo - grip.x, 0, mover.zo - grip.z).length()) - drawIn);
        if (d > limit) mover.setPos(grip.x + n.x * limit, mover.getY(), grip.z + n.z * limit);
        Vec3 v = mover.getDeltaMovement();
        double out = v.x * n.x + v.z * n.z;
        if (out > 0) mover.setDeltaMovement(v.subtract(n.x * out, 0, n.z * out));
    }

    /**
     * Client: a lifter walking into the log they hold stays at its end ({@link #HOLD} off the end face, in line with it):
     * they push it along as fast as it goes, never stepping into it. Beside the end (holding it from the side) is free.
     */
    private void keepOut(Entity mover, boolean root) {
        Vec3 l = toLocal(mover.position());
        double s = root ? -1 : 1, face = s * size() / 2.0, reach = 0.5 + mover.getBbWidth() / 2;
        double out = (l.z - face) * s; // past the end face: positive
        // a little inside HOLD, so the hold point lies past the end face and the push goes on (carry)
        double keep = HOLD - 0.12;
        if (out >= keep || Math.abs(l.x) >= reach) return;
        Vec3 w = toWorld(new Vec3(l.x, 0, face + s * keep));
        mover.setPos(w.x, mover.getY(), w.z);
        Vec3 v = mover.getDeltaMovement(), n = Vec3.directionFromRotation(0, getYRot()).scale(s); // outwards along the log
        double in = v.x * n.x + v.z * n.z;
        if (in < 0) mover.setDeltaMovement(v.subtract(n.x * in, 0, n.z * in));
    }

    /** Client: the entities whose ropes run to the root end. */
    public int[] pullerIds() {
        return pullerIds;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if ((TILT.equals(accessor) || LIFT_ROOT.equals(accessor) || LIFT_TAIL.equals(accessor)) && cells != null && !cells.isEmpty()) setBoundingBox(makeBoundingBox());
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
        if (!r && !t) return tiltOffset(i);
        double along = i + 0.5; // from the root end (lifted logs are straight)
        return LIFT_HEIGHT * (r ? size() - along : along) / size();
    }

    /** Block {@code i}'s rise on a slope from the log's middle (its position's height). */
    public double tiltOffset(int i) {
        double k = tilt();
        return k == 0 ? 0 : k * (cells.get(i).getZ() - cells.get(0).getZ() + 0.5 - size() / 2.0);
    }

    /** How much higher each next block lies on a slope (0: level). Both sides. */
    public float tilt() {
        return entityData.get(TILT);
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
        burden(player, 1);
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
            } else if (!p.isAlive() || p.isShiftKeyDown() || p.isPassenger() || p.position().distanceTo(endPoint(root)) > HOLD + LIFT_SLACK + 4) {
                putDown(player);
            }
        }
    }

    /**
     * One step of being carried (server, and the client of a lifter). Each lifted end goes to its lifter's hands
     * ({@link #HOLD} from them); an end on the ground trails behind, along the path the held end took. Heavier is slower,
     * past the lifters' strength it only heaves along; and a long log resists being swung sideways (shares
     * {@link #NIMBLE} / length of its pace across itself), so it turns slowly about the held end.
     */
    private boolean carry(Level level) {
        double load = liftLoad();
        double speed = CARRY_STEP * (load <= 1 ? 1 - 0.4 * load : 0.2 / (load * load)); // overloaded: a slow, even crawl
        double across = speed * Math.min(1, NIMBLE / size());
        Entity rl = rootLifted() ? lifter(true) : null, tl = tailLifted() ? lifter(false) : null;
        Vec3 root = endPoint(true), tail = endPoint(false), axis = Vec3.directionFromRotation(0, getYRot()); // root to crown
        double len = size();
        Vec3 centre, f;
        if (rl != null && tl != null) {
            Vec3 tr = holdPoint(rl, root), tt = holdPoint(tl, tail);
            Vec3 want = flat(tt.subtract(tr));
            if (want.lengthSqr() < 1.0E-4) return false;
            // the turn the two pairs of hands ask for, at most as far as an end may swing across this tick
            float wantYaw = (float) (Mth.atan2(-want.x, want.z) * Mth.RAD_TO_DEG), maxTurn = (float) Math.toDegrees(across / (len / 2));
            f = Vec3.directionFromRotation(0, getYRot() + Mth.clamp(Mth.wrapDegrees(wantYaw - getYRot()), -maxTurn, maxTurn));
            centre = position().add(clamp(flat(tr.add(tt).scale(0.5).subtract(position())), speed));
        } else if (rl != null || tl != null) {
            Entity lifter = rl != null ? rl : tl;
            Vec3 end = rl != null ? root : tail, other = rl != null ? tail : root, out = rl != null ? axis : axis.reverse(); // from the held end along the log
            Vec3 step = flat(holdPoint(lifter, end).subtract(end));
            double along = step.dot(out);
            Vec3 side = step.subtract(out.scale(along));
            Vec3 l = toLocal(lifter.position());
            if (along > 0 && Math.abs(l.x) < 0.5 + lifter.getBbWidth() / 2) side = Vec3.ZERO; // pushed end-on (keepOut): it goes along its length
            Vec3 newEnd = end.add(out.scale(Mth.clamp(along, -speed, speed))).add(clamp(side, across));
            Vec3 back = flat(other.subtract(newEnd)); // the far end trails after it
            if (back.lengthSqr() < 1.0E-4) return false;
            back = back.normalize();
            f = rl != null ? back : back.reverse();
            centre = newEnd.add(back.scale(len / 2));
        } else {
            return false;
        }
        centre = new Vec3(centre.x, getY(), centre.z);
        float yaw = (float) (Mth.atan2(-f.x, f.z) * Mth.RAD_TO_DEG);
        if (centre.distanceToSqr(position()) < 1.0E-6 && Math.abs(Mth.wrapDegrees(yaw - getYRot())) < 0.05F) return false;
        for (double up : new double[] {0, 0.5, 1}) { // up onto a slab or a step
            Vec3 at = centre.add(0, up, 0);
            if (!fits(level, at, yaw)) continue;
            setYRot(yaw);
            setPos(at);
            if (tickCount % 20 == 0 && !level.isClientSide()) {
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
        if (!level().isClientSide()) {
            for (Entity p : pullers) burden(p, 1);
            for (Entity e : lifterRefs) if (e != null && lifts(e)) burden(e, 1);
        }
        lifting(level()).values().removeIf(l -> l == this);
        super.remove(reason);
    }

    // ---- interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (isPassenger()) {
            // on a cart or wagon: sneak + right-click with an empty hand rolls this log off behind it; any other click is the
            // bed's (a log in your hands goes on next to this one)
            if (!(getVehicle() instanceof TravoisEntity bed)) return InteractionResult.PASS;
            if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && handled(player) == null) return bed.unloadLogUse(player, this);
            return bed.interact(player, hand, location.add(position()).subtract(bed.position()));
        }
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

    /** Axe chops to cut through a block, per chop a stage of a Fallen Log takes ({@link TreeChopping#chopsNeeded}). */
    static final int NOTCH_STAGES = 3;

    /**
     * Server: one axe chop at block {@code i} (held right- or left-click). Bark first: a block still in its bark is barked
     * (the chops of a Fallen Log's first stage, 1-2 Bark). Then, like the saw and into the same cut, a notch into the
     * middle of the block from the side the player stands at, deeper with each chop, until the block is cut through: it
     * comes out as a hewn-through log (a Rough Log at the axe's success chance, else Sticks) and the log parts there.
     */
    private void chop(ServerLevel level, Player player, InteractionHand hand, int i, ItemStack axe) {
        BlockState state = states.get(i);
        BlockPos at = cellPos(i);
        int perStage = TreeChopping.chopsNeeded(axe);
        if (state.getBlock() instanceof FallenLogBlock fallen && state.getValue(FallenLogBlock.STAGE) == 0 && state.getValue(FallenLogBlock.SAWN) == 0) {
            if (!TreeChopping.chop(level, player, hand, at, state, axe, perStage)) return; // the bark
            TreeChopping.hewDrops(level, at, fallen.wood(), 0, axe, player);
            setState(i, state.setValue(FallenLogBlock.STAGE, 1));
            return;
        }
        if (!TreeChopping.swing(level, player, hand, at, state)) return;
        int needed = state.getBlock() instanceof FallenLogBlock ? perStage * NOTCH_STAGES : perStage * (FallenLogBlock.LAST_STAGE + 1);
        if (!deepen(i, player, FallenLogBlock.SAW_STROKES / (double) needed)) return;
        axe.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        if (state.getBlock() instanceof FallenLogBlock fallen) TreeChopping.hewDrops(level, at, fallen.wood(), FallenLogBlock.LAST_STAGE, axe, player);
        else Block.dropResources(state, level, at, null, player, axe);
        level.playSound(null, at, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F);
        removeBlock(level, i);
    }

    /**
     * Server: the cut into block {@code i} deepens by {@code strokes} saw strokes' worth; true once it is through. A fresh
     * cut opens on the side of the block facing {@code player} (from the top if they look down on it). Shown by the
     * Fallen Log's kerf (other blocks: cracks).
     */
    private boolean deepen(int i, Player player, double strokes) {
        BlockState state = states.get(i);
        boolean fallen = state.getBlock() instanceof FallenLogBlock;
        double c = cut.getOrDefault(i, fallen ? (double) state.getValue(FallenLogBlock.SAWN) : 0);
        if (c == 0) rolls.set(i, facing(player, i));
        c += strokes;
        if (c >= FallenLogBlock.SAW_STROKES - 1.0E-6) {
            cut.remove(i);
            crack(-1, 0);
            return true;
        }
        cut.put(i, c);
        if (fallen) setState(i, state.setValue(FallenLogBlock.SAWN, Mth.clamp((int) c, 1, FallenLogBlock.SAW_STROKES - 1)));
        else crack(i, (int) (c * 10 / FallenLogBlock.SAW_STROKES));
        wake();
        return false;
    }

    /**
     * The cut a player starts into block {@code i} (client/LogMesh): its plane holds the way they face, across the log or
     * along it, and it opens from the top if they look down on the block, else from the face towards them. Code: plane
     * (0 across, 1 along) * 8 + opening (0 up, 1 +x, 2 -x, 3 +z, 4 -z, on the log's grid).
     */
    private int facing(Player player, int i) {
        Vec3 look = player.getViewVector(1.0F).yRot(getYRot() * Mth.DEG_TO_RAD);
        boolean along = Math.abs(look.z) > Math.abs(look.x);
        Vec3 eye = toLocal(player.getEyePosition()), c = localCell(i);
        double dx = eye.x - c.x, dz = eye.z - c.z, dy = player.getEyeY() - (getY() + c.y + liftOffset(i) + 0.5);
        if (dy > 0.3 && look.y < -0.45) return (along ? 8 : 0); // looking down on it: from the top
        if (along) return 8 + (dz > 0 ? 3 : 4);
        return dx > 0 ? 1 : 2;
    }

    /** Server: one saw stroke ({@link SawItem}) across block {@code i}: the cut one stroke deeper; through, it comes out as a sawn Fallen Log (Bark if it still had it, Rough Logs). */
    void sawStroke(ServerLevel level, Player player, InteractionHand hand, int i, ItemStack saw, boolean push) {
        BlockPos at = cellPos(i);
        Vec3 w = world(i);
        BlockState state = states.get(i);
        level.playSound(null, w.x, w.y + 1, w.z, SoundEvents.AXE_SCRAPE.value(), SoundSource.BLOCKS, 0.6F, (push ? 1.35F : 1.1F) + random.nextFloat() * 0.1F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), w.x, w.y + 1, w.z, 3, 0.2, 0.02, 0.2, 0.02);
        if (!deepen(i, player, 1)) return;
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
        builder.define(TILT, 0.0F);
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
        int[] r = input.getIntArray("rolls").orElse(new int[0]);
        if (r.length == cells.size()) setRolls(java.util.Arrays.stream(r).boxed().toList());
        leadsToReturn = input.getIntOr("leads", 0);
        entityData.set(TILT, input.getFloatOr("tilt", 0.0F));
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
        output.putIntArray("rolls", rolls.stream().mapToInt(Integer::intValue).toArray());
        output.putInt("leads", pullers.size() + leadsToReturn);
        output.putFloat("tilt", tilt());
    }
}
