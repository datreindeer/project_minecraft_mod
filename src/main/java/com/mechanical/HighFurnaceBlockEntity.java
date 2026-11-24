package com.mechanical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The High Furnace's controller, in its Front (see {@link HighFurnace}, docs/steel-furnace-plan.md sections 3-4).
 * <ul>
 * <li>Shape: {@link HighFurnaceShape#find} every {@link #CHECK_TICKS} ticks and on the tick after a wall block changes
 * ({@link #wallChanged}). Broken, it goes out; its contents stay here. A part in an unloaded chunk pauses it.</li>
 * <li>Fire: each Firebox keeps its own fuel ({@link #FUEL_MAX}) and burns one piece in {@link Fuel#ticks} at no blast,
 * {@code ticks / (1 + b / 8)} at blast b. A Great Bellows with its nozzle at a Firebox's door gives it blast b = the
 * power it gets from its axle ({@link GreatBellowsBlockEntity}; machines on one axle share it).</li>
 * <li>Heat: target = the fuel's natural heat + 50° per point of {@code B_eff = min(16, B * 20 / inside blocks)} (B the
 * sum of the blast at burning fireboxes), up to the fuel's ceiling; the worst fuel counts. Rises 6°/s to 800° and 4°/s
 * above, falls 2°/s to the target (1°/s out), times {@code sqrt(20 / inside blocks)}. Rain in the open top: -100°. A new
 * Front holds at most {@link #DRY_MAX}° for its first {@link #DRY_TICKS} lit ticks while the furnace dries out.</li>
 * <li>Looks: the bottom ring's joints, the Fireboxes' grates and the sight hatch glow in four steps ({@link HighFurnace#HEAT},
 * set only when a step changes); the client draws flames and sparks at the open top (HighFurnaceFrontBlock).</li>
 * </ul>
 * The pool ({@link MeltPool}, 400 mB per inside block) is kept and saved here; melting comes in phase 3.
 */
public class HighFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CHECK_TICKS = 40, FUEL_MAX = 32, MB_PER_BLOCK = 400, DRY_TICKS = 3600, DRY_MAX = 600;
    public static final int FULL_BLAST = 16, BLAST_HEAT = 50;
    /** Rise per second below 800° and above, fall per second lit and out (times {@link #sizeFactor}). */
    public static final float RISE_LOW = 6, RISE_HIGH = 4, FALL_LIT = 2, FALL_OUT = 1;
    public static final int WHITE_HEAT = 1450;

    /** What a Firebox burns: natural heat, ceiling with full blast, ticks per piece at no blast. Coke comes in phase 7. */
    public enum Fuel {
        CHARCOAL(800, 1600, 1200);

        public final int natural, ceiling, ticks;

        Fuel(int natural, int ceiling, int ticks) {
            this.natural = natural;
            this.ceiling = ceiling;
            this.ticks = ticks;
        }
    }

    public static @Nullable Fuel fuelOf(ItemStack stack) {
        return stack.is(Items.CHARCOAL) || stack.is(Items.COAL) ? Fuel.CHARCOAL : null;
    }

    /** One Firebox's fire: pieces waiting, what is left of the burning piece (ticks at no blast), lit or not. */
    static final class Fire {
        int fuel;
        float burn;
        boolean lit;
        Fuel kind = Fuel.CHARCOAL;
        /** Not saved: the blast its bellows gave at the last tick. */
        int blast;

        boolean burning() {
            return lit && (burn > 0 || fuel > 0);
        }
    }

    private final Map<Long, Fire> fires = new HashMap<>();
    final MeltPool pool = new MeltPool(0);
    /** The melting places, one per inside block, each holding one item, and how far each has come (ticks at the melting point). */
    private net.minecraft.core.NonNullList<ItemStack> slots = net.minecraft.core.NonNullList.withSize(0, ItemStack.EMPTY);
    private float[] progress = new float[0];
    /** The layer chosen in the screen for the tapholes to pour (a {@link Molten}), or null for the bottom layer. */
    private @Nullable Molten selected;
    private float temp = Float.NaN, ambient = 20;
    private int dried, totalBlast;
    // worked out again after loading
    private HighFurnaceShape.@Nullable Shape shape;
    private @Nullable Component problem = Component.translatable("message.mechanical.high_furnace.checking");
    private boolean paused, dirty;
    private int checkIn, syncIn, shownStep = -1;
    /** Wall blocks last decorated, so a broken furnace can put them back to their loose look. */
    private List<HighFurnaceShape.Wall> decorated = List.of();
    /** Client: the open top (from the synced shape) and whether it is drying out. */
    private @Nullable Top clientTop;
    private int @Nullable [] clientBox;
    private boolean clientDrying;

    /** Synced to the screen: 0 temperature, 1 target, 2 lit, 3 B_eff x10, 4 inside blocks, 5 fireboxes, 6 drying seconds left,
     * 7 pool mB, 8 capacity mB, then for up to 4 fireboxes: 9-12 fuel, 13-16 blast, 17-20 burning, 21 total blast. */
    final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            List<Fire> fs = fireList();
            return switch (id) {
                case 0 -> Math.round(temperature());
                case 1 -> Math.round(target());
                case 2 -> lit() ? 1 : 0;
                case 3 -> Math.round(effectiveBlast() * 10);
                case 4 -> volume();
                case 5 -> fs.size();
                case 6 -> lit() && dried < DRY_TICKS ? (DRY_TICKS - dried) / 20 : 0;
                case 7 -> pool.total();
                case 8 -> pool.capacity();
                case 21 -> totalBlast;
                default -> {
                    int i = (id - 9) % 4, kind = (id - 9) / 4;
                    if (i >= fs.size()) yield 0;
                    Fire f = fs.get(i);
                    yield kind == 0 ? f.fuel + (f.burn > 0 ? 1 : 0) : kind == 1 ? f.blast : f.burning() ? 1 : 0;
                }
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return 22;
        }
    };

    public HighFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HighFurnace.HIGH_FURNACE_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- shape

    public Direction facing() {
        return getBlockState().getValue(HighFurnaceFrontBlock.FACING);
    }

    public HighFurnaceShape.@Nullable Shape shape() {
        return shape;
    }

    public boolean formed() {
        return shape != null;
    }

    public @Nullable Component problem() {
        return problem;
    }

    public boolean paused() {
        return paused;
    }

    public int volume() {
        return shape == null ? 0 : shape.volume();
    }

    /** sqrt(20 / inside blocks): a big furnace heats and cools slower. */
    public float sizeFactor() {
        return shape == null ? 1 : (float) Math.sqrt(20.0 / shape.volume());
    }

    public boolean isFirebox(BlockPos p) {
        return shape != null && shape.fireboxes().stream().anyMatch(w -> w.pos().equals(p));
    }

    /** A furnace's wall changed at {@code pos}: the furnace it belongs to (if any) checks itself next tick. */
    static void wallChanged(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -HighFurnaceShape.MAX_LAYERS, -4), pos.offset(4, 0, 4)))
            if (level.getBlockEntity(p) instanceof HighFurnaceBlockEntity f && (f.shape == null ? p.distManhattan(pos) <= 8 : f.shape.isWall(pos) || f.shape.inside(pos)))
                f.checkIn = Math.min(f.checkIn, 1);
    }

    /** The formed furnace whose bottom ring holds {@code wall}, or null. */
    public static @Nullable HighFurnaceBlockEntity find(ServerLevel level, BlockPos wall) {
        for (BlockPos p : BlockPos.betweenClosed(wall.offset(-4, 0, -4), wall.offset(4, 0, 4)))
            if (level.getBlockEntity(p) instanceof HighFurnaceBlockEntity f) {
                if (f.shape == null && f.checkIn > 0) f.check(level);
                if (f.shape != null && f.shape.isWall(wall) || f.shape == null && p.distManhattan(wall) <= 6) return f;
            }
        return null;
    }

    public void check(ServerLevel level) {
        HighFurnaceShape.Result r = HighFurnaceShape.find(level, worldPosition, facing(), level::isLoaded);
        checkIn = CHECK_TICKS;
        paused = r.paused();
        if (paused) return;
        boolean was = shape != null;
        HighFurnaceShape.Shape old = shape;
        shape = r.shape();
        problem = r.problem();
        if (shape != null) {
            pool.setCapacity(shape.volume() * MB_PER_BLOCK);
            resizeSlots(level, shape.volume());
            fires.keySet().retainAll(shape.fireboxes().stream().map(w -> w.pos().asLong()).toList()); // a firebox taken out loses its fire (its fuel drops with it)
            for (HighFurnaceShape.Wall w : shape.fireboxes()) fires.computeIfAbsent(w.pos().asLong(), k -> new Fire());
            if (!was) {
                level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 0.6F);
                Smelting.award(level, worldPosition, HighFurnace.FORMED_ADVANCEMENT, 16);
            }
        } else for (Fire f : fires.values()) f.lit = false; // a broken furnace goes out; the fuel stays
        if (!java.util.Objects.equals(old, shape)) {
            shownStep = -1;
            sync();
        }
    }

    // ---------------------------------------------------------------- fire

    List<Fire> fireList() {
        List<Fire> out = new ArrayList<>();
        if (shape != null) for (HighFurnaceShape.Wall w : shape.fireboxes()) out.add(fires.computeIfAbsent(w.pos().asLong(), k -> new Fire()));
        return out;
    }

    public boolean lit() {
        for (Fire f : fires.values()) if (f.burning()) return true;
        return false;
    }

    public boolean drying() {
        return level != null && level.isClientSide() ? clientDrying : lit() && dried < DRY_TICKS;
    }

    /** Puts what fits of {@code stack} (Charcoal, Coal) into the Firebox at {@code pos}; returns how many it took. */
    public int addFuel(BlockPos pos, ItemStack stack) {
        Fire f = fires.computeIfAbsent(pos.asLong(), k -> new Fire());
        Fuel kind = fuelOf(stack);
        if (kind == null) return 0;
        int taken = Math.min(stack.getCount(), FUEL_MAX - f.fuel);
        f.fuel += taken;
        f.kind = kind;
        setChanged();
        return taken;
    }

    public boolean canLight() {
        return shape != null && fireList().stream().anyMatch(f -> f.burn > 0 || f.fuel > 0);
    }

    /** Fire Drill or Flint and Steel on a Firebox: every Firebox with fuel catches. */
    public void light() {
        for (Fire f : fireList()) if (f.burn > 0 || f.fuel > 0) f.lit = true;
        setChanged();
    }

    public Component describeFirebox(BlockPos pos) {
        Fire f = fires.getOrDefault(pos.asLong(), new Fire());
        return Component.translatable(f.burning() ? "message.mechanical.high_furnace.firebox_lit" : "message.mechanical.high_furnace.firebox",
                f.fuel, FUEL_MAX, f.blast, FULL_BLAST);
    }

    /** The blast a Firebox gets: the power of a Great Bellows right outside its door with the nozzle at it. */
    int blastAt(ServerLevel level, HighFurnaceShape.Wall w) {
        BlockPos at = w.pos().relative(w.out());
        BlockState s = level.getBlockState(at);
        if (!s.is(HighFurnace.GREAT_BELLOWS.get()) || s.getValue(GreatBellowsBlock.FACING) != w.out().getOpposite()) return 0;
        return level.getBlockEntity(at) instanceof GreatBellowsBlockEntity b ? Math.min(FULL_BLAST, b.power()) : 0;
    }

    public float effectiveBlast() {
        return shape == null ? 0 : Math.min(FULL_BLAST, totalBlast * 20F / shape.volume());
    }

    /** What the heat heads for now. */
    public float target() {
        if (shape == null || !lit()) return ambient;
        Fuel worst = null;
        for (Fire f : fireList()) if (f.burning() && (worst == null || f.kind.ceiling < worst.ceiling)) worst = f.kind;
        if (worst == null) return ambient;
        float t = Math.min(worst.ceiling, worst.natural + BLAST_HEAT * effectiveBlast());
        if (level != null && rainedOn()) t -= 100;
        if (dried < DRY_TICKS) t = Math.min(t, DRY_MAX);
        return t;
    }

    private boolean rainedOn() {
        return shape != null && level.isRainingAt(new BlockPos((shape.min().getX() + shape.max().getX()) / 2, shape.max().getY() + 1, (shape.min().getZ() + shape.max().getZ()) / 2));
    }

    public float temperature() {
        return Float.isNaN(temp) ? ambient : temp;
    }

    // ---------------------------------------------------------------- tick

    void serverTick(ServerLevel level) {
        if (Float.isNaN(temp) || level.getGameTime() % 20 == 0) {
            ambient = Temperature.ambient(level, worldPosition) * 1.5F;
            if (Float.isNaN(temp)) temp = ambient;
        }
        if (--checkIn <= 0) check(level);
        if (paused) return;
        List<Fire> fs = fireList();
        totalBlast = 0;
        if (shape != null) {
            for (int i = 0; i < fs.size(); i++) {
                Fire f = fs.get(i);
                f.blast = blastAt(level, shape.fireboxes().get(i));
                if (!f.burning()) continue;
                totalBlast += f.blast;
                if (f.blast >= FULL_BLAST) Smelting.award(level, worldPosition, HighFurnace.FULL_BLAST_ADVANCEMENT, 16);
                if (f.burn <= 0) { // the next piece
                    f.fuel--;
                    f.burn += f.kind.ticks;
                }
                f.burn -= 1 + f.blast / 8F;
                if (f.burn <= 0 && f.fuel == 0) {
                    f.burn = 0;
                    f.lit = false;
                }
            }
        }
        boolean lit = lit();
        if (lit) {
            NightDangers.markLit(level, worldPosition);
            if (dried < DRY_TICKS && ++dried == DRY_TICKS) sync();
        }
        float goal = target(), t = temperature(), k = sizeFactor() / 20;
        if (t < goal) temp = Math.min(goal, t + (t < 800 ? RISE_LOW : RISE_HIGH) * k);
        else temp = Math.max(goal, t - (lit ? FALL_LIT : FALL_OUT) * k);
        if (temp >= WHITE_HEAT) Smelting.award(level, worldPosition, HighFurnace.WHITE_HEAT_ADVANCEMENT, 16);
        decorate(level);
        if (shape != null) {
            intake(level);
            melt(level);
            if (pool.mix(temperature())) dirty = true;
        }
        setChanged();
        if (level.getGameTime() % 20 == 0) dirty = true; // the temperature for the client's flames
        if (dirty && --syncIn <= 0) {
            dirty = false;
            syncIn = 10;
            sync();
        }
    }

    // ---------------------------------------------------------------- melting (phase 3)

    public net.minecraft.core.NonNullList<ItemStack> slots() {
        return slots;
    }

    /** How far the item in place {@code i} has melted, 0..1. */
    public float progress(int i) {
        HighFurnaceMelting.Melt m = i < slots.size() ? HighFurnaceMelting.of(slots.get(i)) : null;
        return m == null || i >= progress.length ? 0 : Math.min(1, progress[i] / m.ticks());
    }

    public MeltPool pool() {
        return pool;
    }

    public @Nullable Molten selected() {
        return selected != null && pool.get(selected) > 0 ? selected : null;
    }

    /** The layer the tapholes pour: the chosen one while there is any of it, else the bottom layer. */
    public @Nullable Molten pouring() {
        Molten s = selected();
        return s != null ? s : pool.bottom();
    }

    /** The screen chose a layer (null: back to the bottom layer). */
    public void select(@Nullable Molten m) {
        selected = m;
        dirty = true;
        setChanged();
    }

    /** One melting place per inside block; places lost when the furnace shrinks give their items back out of the top. */
    private void resizeSlots(ServerLevel level, int n) {
        if (slots.size() == n) return;
        net.minecraft.core.NonNullList<ItemStack> next = net.minecraft.core.NonNullList.withSize(n, ItemStack.EMPTY);
        float[] p = new float[n];
        for (int i = 0; i < slots.size(); i++) {
            if (i < n) {
                next.set(i, slots.get(i));
                p[i] = i < progress.length ? progress[i] : 0;
            } else if (!slots.get(i).isEmpty()) spit(level, slots.get(i));
        }
        slots = next;
        progress = p;
    }

    /** Puts one item of {@code stack} in a free place; false if it does not melt here or every place is taken. */
    public boolean take(ItemStack stack) {
        if (HighFurnaceMelting.of(stack) == null) return false;
        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i).isEmpty()) {
                slots.set(i, stack.copyWithCount(1));
                progress[i] = 0;
                dirty = true;
                return true;
            }
        return false;
    }

    /** Items thrown in at the open top: what melts takes free places one by one; what never melts is thrown back out with a hiss. */
    private void intake(ServerLevel level) {
        BlockPos lo = shape.min(), hi = shape.max();
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(lo.getX(), lo.getY(), lo.getZ(), hi.getX() + 1, hi.getY() + 1.5, hi.getZ() + 1);
        for (net.minecraft.world.entity.item.ItemEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            if (!e.isAlive()) continue;
            ItemStack stack = e.getItem();
            if (HighFurnaceMelting.of(stack) == null) {
                ItemStack out = stack.copy();
                e.discard();
                spit(level, out);
                continue;
            }
            int taken = 0;
            while (taken < stack.getCount() && take(stack)) taken++;
            if (taken == 0) continue; // every place busy: it lies on the charge until one frees up
            level.playSound(null, e.blockPosition(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            if (taken >= stack.getCount()) e.discard();
            else e.setItem(stack.copyWithCount(stack.getCount() - taken));
        }
    }

    /** Throws an item back out of the top with a hiss. */
    private void spit(ServerLevel level, ItemStack stack) {
        if (shape == null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), stack);
            return;
        }
        Vec3 top = topForTest();
        net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(level, top.x, top.y + 0.6, top.z, stack);
        double a = level.getRandom().nextDouble() * Math.PI * 2;
        e.setDeltaMovement(Math.cos(a) * 0.3, 0.45, Math.sin(a) * 0.3);
        level.addFreshEntity(e);
        level.playSound(null, BlockPos.containing(top), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.5F);
    }

    /** Every place melts at once: 40 ticks per 100 mB at the melting point, faster when hotter; it waits while too cold or the pool is full. */
    private void melt(ServerLevel level) {
        float t = temperature();
        for (int i = 0; i < slots.size(); i++) {
            HighFurnaceMelting.Melt m = HighFurnaceMelting.of(slots.get(i));
            if (m == null || t < m.molten().melt || pool.room() < m.mb()) continue;
            progress[i] += KilnBlockEntity.speed(t, m.molten().melt);
            if (progress[i] < m.ticks()) continue;
            pool.add(m.molten(), m.mb());
            slots.set(i, ItemStack.EMPTY);
            progress[i] = 0;
            dirty = true;
        }
        if (dirty && pool.layers().size() >= 3) Smelting.award(level, worldPosition, HighFurnace.LAYER_CAKE_ADVANCEMENT, 16);
    }

    public static int step(float temp) {
        return temp < 600 ? 0 : temp < 1000 ? 1 : temp < 1300 ? 2 : 3;
    }

    /**
     * The glow: the bottom ring's casing and taphole joints by the heat step, each Firebox by its own fire, the hatch;
     * the special blocks turned to face out. Client updates only; touches blocks only when the step or a fire changed.
     */
    void decorate(ServerLevel level) {
        int s = shape == null ? 0 : step(temperature());
        List<Fire> fs = fireList();
        int key = s * 31 + (shape == null ? -7 : fs.stream().mapToInt(f -> f.burning() ? 1 : 0).reduce(0, (a, b) -> a * 2 + b));
        if (key == shownStep) return;
        shownStep = key;
        List<HighFurnaceShape.Wall> walls = new ArrayList<>();
        if (shape != null) {
            int layers = shape.max().getY() - shape.min().getY() + 1;
            java.util.Set<BlockPos> ring = new java.util.HashSet<>();
            for (HighFurnaceShape.Wall w : shape.ring()) ring.add(w.pos());
            for (int dy = 0; dy < layers; dy++)
                for (HighFurnaceShape.Wall w : shape.ring()) {
                    HighFurnaceShape.Wall at = new HighFurnaceShape.Wall(w.pos().above(dy), w.out());
                    walls.add(at);
                    BlockState st = level.getBlockState(at.pos());
                    int heat = dy == 0 || dy == 1 || dy == layers - 1 ? s : 0; // the hearth, the hot course and the glowing throat
                    if (st.is(HighFurnace.FIREBOX.get())) {
                        int i = shape.fireboxes().indexOf(w);
                        heat = i >= 0 && i < fs.size() && fs.get(i).burning() ? Math.max(1, s) : Math.max(0, s - 1);
                    }
                    if (st.is(HighFurnace.BANDED_FURNACE_CASING.get())) {
                        BlockState t = st.setValue(HighFurnace.HEAT, heat).setValue(HighFurnaceWallBlock.Casing.FACING, w.out())
                                .setValue(HighFurnaceWallBlock.Casing.PART, HighFurnaceWallBlock.Casing.part(dy, layers))
                                .setValue(HighFurnaceWallBlock.Casing.END_CW, !ring.contains(w.pos().relative(w.out().getClockWise())))
                                .setValue(HighFurnaceWallBlock.Casing.END_CCW, !ring.contains(w.pos().relative(w.out().getCounterClockWise())));
                        if (t != st) level.setBlock(at.pos(), t, Block.UPDATE_CLIENTS);
                    } else set(level, at, heat);
                }
        }
        for (HighFurnaceShape.Wall w : decorated) if (!walls.contains(w)) loose(level, w.pos()); // no longer part of it
        decorated = walls;
        BlockState me = getBlockState();
        if (me.getValue(HighFurnace.HEAT) != s) level.setBlock(worldPosition, me.setValue(HighFurnace.HEAT, s), Block.UPDATE_CLIENTS);
    }

    /** A wall block that is no longer part of a formed furnace: cold, and casing back to its loose look. */
    private static void loose(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        BlockState t = st.is(HighFurnace.BANDED_FURNACE_CASING.get()) ? st.getBlock().defaultBlockState()
                : st.hasProperty(HighFurnace.HEAT) && !st.is(HighFurnace.HIGH_FURNACE_FRONT.get()) ? st.setValue(HighFurnace.HEAT, 0) : st;
        if (t != st) level.setBlock(p, t, Block.UPDATE_CLIENTS);
    }

    private static void set(ServerLevel level, HighFurnaceShape.Wall w, int heat) {
        BlockState st = level.getBlockState(w.pos());
        if (!st.hasProperty(HighFurnace.HEAT) || st.is(HighFurnace.HIGH_FURNACE_FRONT.get())) return;
        BlockState t = st.setValue(HighFurnace.HEAT, heat);
        if (t.hasProperty(HighFurnaceWallBlock.Facing.FACING)) t = t.setValue(HighFurnaceWallBlock.Facing.FACING, w.out());
        if (t != st) level.setBlock(w.pos(), t, Block.UPDATE_CLIENTS);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ---------------------------------------------------------------- client: the open top

    /** The open top seen by the client: the inside's footprint at its top layer. */
    public record Top(double x0, double z0, double x1, double z1, double y) {
        public double x(RandomSource r) {
            return x0 + 0.15 + r.nextDouble() * (x1 - x0 - 0.3);
        }

        public double z(RandomSource r) {
            return z0 + 0.15 + r.nextDouble() * (z1 - z0 - 0.3);
        }

        public BlockPos centre() {
            return BlockPos.containing((x0 + x1) / 2, y, (z0 + z1) / 2);
        }

        /** Somewhere in the walls (for the steam while it dries out). */
        public BlockPos wallSpot(RandomSource r) {
            int ly = (int) y - 1 - r.nextInt(5);
            boolean xSide = r.nextBoolean();
            double x = xSide ? (r.nextBoolean() ? x0 - 1 : x1) : x0 + r.nextInt((int) (x1 - x0));
            double z = xSide ? z0 + r.nextInt((int) (z1 - z0)) : (r.nextBoolean() ? z0 - 1 : z1);
            return BlockPos.containing(x, ly, z);
        }
    }

    public @Nullable Top clientTop() {
        return clientTop;
    }

    /** Client: the inside's box from the synced shape (min x y z, max x y z), or null. */
    public int @Nullable [] clientBox() {
        return clientBox;
    }

    /**
     * Client, every tick: smoke always rises from a warm top; over 1200° flames lick up at the rim and the roar starts,
     * over 1300° sparks fly, over 1400° lava spits; a new furnace steams from its joints while it dries out.
     */
    void clientTick(net.minecraft.world.level.Level level) {
        Top top = clientTop;
        if (top == null) return;
        RandomSource random = level.getRandom();
        float t = temperature();
        double y = top.y();
        if (clientDrying && random.nextInt(3) == 0) {
            BlockPos p = top.wallSpot(random);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX() + random.nextDouble(), p.getY() + random.nextDouble(), p.getZ() + random.nextDouble(), 0, 0.03, 0);
        }
        if (t < 400) return;
        if (random.nextFloat() < t / 1600F)
            level.addParticle(t > 900 ? net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE : net.minecraft.core.particles.ParticleTypes.SMOKE,
                    top.x(random), y + 0.1, top.z(random), 0, 0.05 + random.nextDouble() * 0.03, 0);
        if (t > 1000) for (int i = 0; i < 1 + (int) ((t - 1000) / 200); i++)
            if (random.nextInt(2) == 0) level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, top.x(random), y - 0.6 + random.nextDouble() * 0.7, top.z(random),
                    (random.nextDouble() - 0.5) * 0.02, 0.05 + random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.02);
        if (t > 1200 && random.nextInt(3) == 0)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    top.x(random), y + 0.2 + random.nextDouble() * 0.4, top.z(random), 0, 0.12 + random.nextDouble() * 0.06, 0);
        if (t > 1300 && random.nextInt(2) == 0)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, top.x(random), y + 0.1, top.z(random),
                    (random.nextDouble() - 0.5) * 0.4, 0.35 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.4);
        if (t > 1400 && random.nextInt(6) == 0)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA, top.x(random), y - 0.2, top.z(random), 0, 0, 0);
        BlockPos c = top.centre();
        if (random.nextInt(40) == 0) level.playLocalSound(c, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.6F, false);
        if (t > 1000 && random.nextInt(50) == 0) level.playLocalSound(c, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.6F, 0.5F, false);
        if (t > 1300 && random.nextInt(120) == 0) level.playLocalSound(c, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.8F, 0.8F, false);
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // the fuel in the fireboxes falls out (the melt and the Furnace Salamander come with the pool in phase 6)
        if (level != null) {
            for (ItemStack it : slots) if (!it.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), it.copy());
            int fuel = 0;
            for (Fire f : fires.values()) fuel += f.fuel;
            if (fuel > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.CHARCOAL, fuel));
        }
        if (level instanceof ServerLevel server) {
            shape = null;
            shownStep = -1;
            decorate(server);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        temp = input.getFloatOr("temp", Float.NaN);
        dried = input.getIntOr("dried", 0);
        pool.load(input.getIntArray("pool").orElse(new int[0]));
        int n = input.getIntOr("slot_count", 0);
        slots = net.minecraft.core.NonNullList.withSize(n, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(input, slots);
        int[] pr = input.getIntArray("progress").orElse(new int[0]);
        progress = new float[n];
        for (int i = 0; i < n && i < pr.length; i++) progress[i] = pr[i] / 100F;
        selected = Molten.byId(input.getStringOr("selected", ""));
        pool.setCapacity(input.getIntOr("capacity", 0));
        fires.clear();
        int[] at = input.getIntArray("fire_pos").orElse(new int[0]);
        int[] fuel = input.getIntArray("fire_fuel").orElse(new int[0]), burn = input.getIntArray("fire_burn").orElse(new int[0]);
        for (int i = 0; 3 * i + 2 < at.length && i < fuel.length && i < burn.length; i++) {
            Fire f = new Fire();
            f.fuel = fuel[i] & 0xFFFF;
            f.lit = (fuel[i] >> 16) != 0;
            f.burn = burn[i];
            fires.put(new BlockPos(at[3 * i], at[3 * i + 1], at[3 * i + 2]).asLong(), f);
        }
        // the client: the open top and the drying from the synced shape
        int[] box = input.getIntArray("box").orElse(new int[0]);
        clientTop = box.length == 6 ? new Top(box[0], box[2], box[3] + 1, box[5] + 1, box[4] + 1) : null;
        clientBox = box.length == 6 ? box : null;
        clientDrying = input.getBooleanOr("drying", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!Float.isNaN(temp)) output.putFloat("temp", temp);
        output.putInt("dried", dried);
        output.putIntArray("pool", pool.save());
        output.putInt("slot_count", slots.size());
        net.minecraft.world.ContainerHelper.saveAllItems(output, slots);
        int[] pr = new int[progress.length];
        for (int i = 0; i < pr.length; i++) pr[i] = Math.round(progress[i] * 100);
        output.putIntArray("progress", pr);
        if (selected != null) output.putString("selected", selected.id);
        output.putInt("capacity", pool.capacity());
        int[] at = new int[fires.size() * 3];
        int[] fuel = new int[fires.size()], burn = new int[fires.size()];
        int i = 0;
        for (Map.Entry<Long, Fire> e : fires.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            at[3 * i] = p.getX();
            at[3 * i + 1] = p.getY();
            at[3 * i + 2] = p.getZ();
            fuel[i] = e.getValue().fuel | (e.getValue().lit ? 1 << 16 : 0);
            burn[i++] = Math.round(e.getValue().burn);
        }
        output.putIntArray("fire_pos", at);
        output.putIntArray("fire_fuel", fuel);
        output.putIntArray("fire_burn", burn);
        if (shape != null) output.putIntArray("box", new int[] {shape.min().getX(), shape.min().getY(), shape.min().getZ(),
                shape.max().getX(), shape.max().getY(), shape.max().getZ()});
        output.putBoolean("drying", drying());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mechanical.high_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new HighFurnaceMenu(containerId, inventory, this, data, worldPosition);
    }

    // ---------------------------------------------------------------- tests

    void setTemperatureForTest(float t) {
        temp = t;
    }

    void setDriedForTest() {
        dried = DRY_TICKS;
    }

    Vec3 topForTest() {
        return shape == null ? Vec3.ZERO : new Vec3((shape.min().getX() + shape.max().getX() + 1) / 2.0, shape.max().getY() + 1, (shape.min().getZ() + shape.max().getZ() + 1) / 2.0);
    }
}
