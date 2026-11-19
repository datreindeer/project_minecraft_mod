package com.mechanical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Smelting Furnace's controller, in its {@link FurnaceTapBlock}. The furnace is the 3x3x3 of Furnace Bricks around
 * a 1x1 open shaft, the tap being the middle of the bottom front wall; checked every second and on neighbour changes.
 * <ul>
 * <li>Intake: item entities in the shaft. Ore and ingots go into the {@link FurnaceMelt} charge (up to its capacity, and
 * only what mixes), Charcoal / Coal into the fuel (up to {@link #FUEL_MAX}); anything else is thrown back out the top.</li>
 * <li>Heat: the kiln's model ({@link KilnBlockEntity#heatStep}, charcoal's 800° natural, 1300° with bellows,
 * {@link KilnBlockEntity#PUMP}° per pump). Lit with a Fire Drill; burns its fuel while lit; goes out when it runs out.</li>
 * <li>Melting: one quarter unit per {@link #QUARTER_TICKS} at the melting point, faster when hotter ({@link KilnBlockEntity#speed}).</li>
 * <li>Tapping: open, hot enough, and with a settled melt, the tap finds the nearest empty Clay Mold that casts the metal
 * (directly in front, or along connected Clay Channels), lets the metal run for a moment and fills it. The metal only
 * leaves the furnace when the mold is filled; with nowhere to go it simply waits.</li>
 * </ul>
 */
public class FurnaceTapBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FUEL_MAX = 16, QUARTER_TICKS = 6, MAX_CHANNELS = 64;
    /** A pour takes this long plus {@link #TICKS_PER_UNIT} per unit of the mold, plus 2 per channel the metal runs along. */
    public static final int POUR_TICKS = 10, TICKS_PER_UNIT = 5;

    final FurnaceMelt melt = new FurnaceMelt();
    private int fuel, burn, burnTotal, maxTemp = 800, bellowsTemp = 1300;
    private float temp = Float.NaN, ambient = 20, meltProgress;
    // not saved: worked out again after loading
    private boolean formed;
    private @Nullable Component problem = Component.translatable("message.mechanical.furnace.checking");
    private int checkIn, searchIn, pourLeft, syncIn;
    private boolean dirty;
    private @Nullable Route route;

    record Route(BlockPos mold, List<BlockPos> path) {}

    /** Synced to the screen: 0 temperature, 1 lit, 2 burn left, 3 burn total, 4 fuel items, 5 open, 6 melting point needed,
     * 7-10 pool quarters, 11-14 charge quarters (copper, tin, bronze, poor bronze). */
    final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> Math.round(temperature());
                case 1 -> lit() ? 1 : 0;
                case 2 -> burn;
                case 3 -> burnTotal;
                case 4 -> fuel;
                case 5 -> getBlockState().getValue(FurnaceTapBlock.OPEN) ? 1 : 0;
                case 6 -> melt.meltTemp();
                default -> id < 11 ? melt.pool(FurnaceMelt.METALS[id - 7]) : melt.charge[id - 11];
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return 15;
        }
    };

    public FurnaceTapBlockEntity(BlockPos pos, BlockState state) {
        super(Smelting.FURNACE_TAP_ENTITY.get(), pos, state);
    }

    public FurnaceMelt melt() {
        return melt;
    }

    public float temperature() {
        return Float.isNaN(temp) ? ambient : temp;
    }

    private boolean lit() {
        return getBlockState().getValue(FurnaceTapBlock.LIT);
    }

    public Direction facing() {
        return getBlockState().getValue(FurnaceTapBlock.FACING);
    }

    /** The bottom of the shaft, right behind the tap. */
    public BlockPos shaft() {
        return worldPosition.relative(facing().getOpposite());
    }

    /** Null when the furnace is complete, else what is wrong. */
    public @Nullable Component problem() {
        return problem;
    }

    public void recheck() {
        checkIn = 0;
    }

    /** Is this position one of the furnace's walls (the tap included)? */
    public boolean isWall(BlockPos p) {
        BlockPos c = shaft();
        int dx = p.getX() - c.getX(), dy = p.getY() - c.getY(), dz = p.getZ() - c.getZ();
        return Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && dy >= 0 && dy <= 2 && (dx != 0 || dz != 0);
    }

    /** The formed furnace whose wall is at {@code wall} (for bellows), or null. */
    public static @Nullable FurnaceTapBlockEntity find(ServerLevel level, BlockPos wall) {
        for (BlockPos p : BlockPos.betweenClosed(wall.offset(-2, -2, -2), wall.offset(2, 0, 2)))
            if (level.getBlockEntity(p) instanceof FurnaceTapBlockEntity tap && tap.isWall(wall)) return tap;
        return null;
    }

    /** Checks the structure: 23 Furnace Bricks and this tap around an open shaft three blocks deep. */
    public void check(ServerLevel level) {
        BlockPos c = shaft();
        int missing = 0;
        BlockPos firstMissing = null, blocked = null;
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (p.equals(worldPosition)) continue;
                    BlockState s = level.getBlockState(p);
                    if (dx == 0 && dz == 0) {
                        if (!s.isAir() && blocked == null) blocked = p.immutable();
                    } else if (!s.is(Smelting.FURNACE_BRICKS.get())) {
                        if (missing++ == 0) firstMissing = p.immutable();
                    }
                }
            }
        }
        boolean was = formed;
        if (missing > 0) problem = Component.translatable("message.mechanical.furnace.missing", missing, firstMissing.getX(), firstMissing.getY(), firstMissing.getZ());
        else if (blocked != null) problem = Component.translatable("message.mechanical.furnace.blocked", blocked.getX(), blocked.getY(), blocked.getZ());
        else problem = null;
        formed = problem == null;
        if (formed && !was) {
            level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
            Smelting.award(level, worldPosition, Smelting.FORMED_ADVANCEMENT, 12);
        }
    }

    public boolean canLight() {
        return formed && (burn > 0 || fuel > 0);
    }

    /** Fire Drill: lights it, finishing partly burnt fuel first. */
    public boolean tryLight() {
        if (!canLight()) return false;
        if (burn <= 0) takeFuel();
        setLit(true);
        setChanged();
        return true;
    }

    private void takeFuel() {
        var f = KilnBlockEntity.fuelFor(new ItemStack(Items.CHARCOAL)).orElseThrow();
        burn = burnTotal = f.ticks();
        maxTemp = f.maxTemp();
        bellowsTemp = f.bellowsTemp();
        fuel--;
    }

    /** Only the hot fuels (Charcoal, Coal) burn in it. */
    static boolean isFuel(ItemStack stack) {
        return KilnBlockEntity.fuelFor(stack).map(f -> f.maxTemp() >= 800).orElse(false);
    }

    /** One pump of a bellows on a wall: +{@link KilnBlockEntity#PUMP}° up to the fuel's bellows max. False if it isn't lit. */
    public boolean pump() {
        if (!formed || !lit()) return false;
        float max = Math.min(bellowsTemp, KilnBlockEntity.CEILING);
        if (temperature() < max) temp = Math.min(max, temperature() + KilnBlockEntity.PUMP);
        setChanged();
        return true;
    }

    private void setLit(boolean lit) {
        if (level != null && lit() != lit) level.setBlock(worldPosition, getBlockState().setValue(FurnaceTapBlock.LIT, lit), Block.UPDATE_ALL);
    }

    private void setPouring(boolean pouring) {
        if (level != null && getBlockState().getValue(FurnaceTapBlock.POURING) != pouring)
            level.setBlock(worldPosition, getBlockState().setValue(FurnaceTapBlock.POURING, pouring), Block.UPDATE_ALL);
    }

    void serverTick(ServerLevel level, BlockState state) {
        if (Float.isNaN(temp) || level.getGameTime() % 20 == 0) {
            ambient = Temperature.ambient(level, worldPosition) * 1.5F;
            if (Float.isNaN(temp)) temp = ambient;
        }
        if (checkIn-- <= 0) {
            check(level);
            checkIn = 20;
        }
        if (!formed && lit()) setLit(false); // a broken furnace goes out (the fuel left stays)
        if (formed) intake(level);
        boolean lit = lit();
        if (lit) {
            NightDangers.markLit(level, worldPosition);
            if (burn <= 0) {
                if (fuel > 0) takeFuel();
                else {
                    burnTotal = 0;
                    setLit(false);
                    lit = false;
                }
            }
            if (burn > 0) burn = Math.max(0, burn - Temperature.fireDrain(level, worldPosition, level.getRandom()));
        }
        BlockPos top = shaft().above(3);
        temp = Math.min(KilnBlockEntity.heatStep(temperature(), lit, maxTemp, ambient, level.isRainingAt(top)), KilnBlockEntity.CEILING);

        int need = melt.meltTemp();
        if (formed && need > 0 && temp >= need) {
            meltProgress += KilnBlockEntity.speed(temp, need);
            while (meltProgress >= QUARTER_TICKS && melt.chargeTotal() > 0) {
                meltProgress -= QUARTER_TICKS;
                melt.meltQuarter();
                dirty = true;
            }
        } else if (need == 0) {
            meltProgress = 0;
        }
        pour(level, state);
        setChanged();
        if (dirty && --syncIn <= 0) {
            dirty = false;
            syncIn = 10;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Takes what falls down the shaft. */
    private void intake(ServerLevel level) {
        BlockPos c = shaft();
        AABB box = new AABB(c.getX(), c.getY(), c.getZ(), c.getX() + 1, c.getY() + 3, c.getZ() + 1);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!e.isAlive()) continue;
            ItemStack stack = e.getItem().copy();
            int taken = 0;
            FurnaceMelt.Amount a = FurnaceMelt.quarters(stack);
            if (a != null) {
                taken = melt.fits(a, stack.getCount());
                melt.add(a, taken);
            } else if (isFuel(stack)) {
                taken = Math.min(stack.getCount(), FUEL_MAX - fuel);
                fuel += taken;
            }
            if (taken > 0) {
                stack.shrink(taken);
                dirty = true;
                level.playSound(null, c, a != null ? SoundEvents.LAVA_POP : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, c.getX() + 0.5, c.getY() + 3.0, c.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.02);
            }
            if (stack.isEmpty()) {
                e.discard();
                continue;
            }
            // doesn't fit / doesn't melt / doesn't mix: thrown back out of the top
            e.setItem(stack);
            e.setPos(c.getX() + 0.5, c.getY() + 3.2, c.getZ() + 0.5);
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            e.setDeltaMovement(Math.cos(angle) * 0.25, 0.4, Math.sin(angle) * 0.25);
            level.playSound(null, c.above(3), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
        }
    }

    private void pour(ServerLevel level, BlockState state) {
        Metal metal = melt.alloy();
        boolean flows = formed && state.getValue(FurnaceTapBlock.OPEN) && metal != null && !melt.settling() && temperature() >= metal.melt;
        if (!flows) {
            route = null;
            setPouring(false);
            return;
        }
        if (route == null) {
            if (searchIn-- > 0) return;
            searchIn = 10;
            route = findRoute(level, metal, melt.pourable());
            if (route == null) {
                setPouring(false);
                return;
            }
            pourLeft = POUR_TICKS + ((ClayMoldBlock) level.getBlockState(route.mold()).getBlock()).shape.units * TICKS_PER_UNIT + route.path().size() * 2;
            markFlow(level, route, metal, level.getGameTime() + pourLeft + 15);
            setPouring(true);
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.7F, 1.1F);
        }
        if (--pourLeft > 0) return;
        Route done = route;
        route = null;
        searchIn = 0;
        for (BlockPos p : done.path()) if (!(level.getBlockState(p).getBlock() instanceof ClayChannelBlock)) return; // the channel was broken: nothing lost
        BlockState ms = level.getBlockState(done.mold());
        if (!accepts(ms, metal, melt.pourable())) return;
        ClayMoldBlock mold = (ClayMoldBlock) ms.getBlock();
        if (!melt.pour(mold.shape.units * 4)) return;
        dirty = true;
        level.setBlock(done.mold(), ms.setValue(ClayMoldBlock.FILL, ClayMoldBlock.Fill.MOLTEN).setValue(ClayMoldBlock.METAL, metal), Block.UPDATE_ALL);
        level.scheduleTick(done.mold(), mold, ClayMoldBlock.SET_TICKS);
        level.playSound(null, done.mold(), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
        level.sendParticles(ParticleTypes.LAVA, done.mold().getX() + 0.5, done.mold().getY() + 0.25, done.mold().getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.0);
        if (!done.path().isEmpty()) Smelting.award(level, worldPosition, Smelting.TAPPED_ADVANCEMENT, 16);
    }

    static boolean accepts(BlockState s, Metal metal, int quarters) {
        return s.getBlock() instanceof ClayMoldBlock mold && s.getValue(ClayMoldBlock.FILL) == ClayMoldBlock.Fill.EMPTY
                && metal.casts(mold.shape) && mold.shape.units * 4 <= quarters;
    }

    /** The nearest mold that takes a cast of this metal: right in front of the spout, or along the channels from there. */
    @Nullable Route findRoute(ServerLevel level, Metal metal, int quarters) {
        BlockPos start = worldPosition.relative(facing());
        BlockState first = level.getBlockState(start);
        if (accepts(first, metal, quarters)) return new Route(start, List.of());
        if (!(first.getBlock() instanceof ClayChannelBlock)) return null;
        Map<BlockPos, BlockPos> from = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        from.put(start, worldPosition);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos c = queue.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(d);
                if (from.containsKey(n) || n.equals(worldPosition) || !level.isLoaded(n)) continue;
                BlockState s = level.getBlockState(n);
                if (s.getBlock() instanceof ClayChannelBlock) {
                    if (from.size() >= MAX_CHANNELS) continue;
                    from.put(n, c);
                    queue.add(n);
                } else if (accepts(s, metal, quarters)) {
                    List<BlockPos> path = new ArrayList<>();
                    for (BlockPos p = c; !p.equals(worldPosition); p = from.get(p)) path.addFirst(p);
                    return new Route(n, path);
                }
            }
        }
        return null;
    }

    /** Lights up the channels on the route: each one shows metal running from where it came in to where it goes out. */
    private void markFlow(ServerLevel level, Route route, Metal metal, long until) {
        List<BlockPos> path = route.path();
        for (int i = 0; i < path.size(); i++) {
            BlockPos p = path.get(i);
            BlockPos prev = i == 0 ? worldPosition : path.get(i - 1), next = i == path.size() - 1 ? route.mold() : path.get(i + 1);
            if (level.getBlockEntity(p) instanceof ClayChannelBlockEntity channel)
                channel.flow(metal, Direction.getNearest(prev.subtract(p), Direction.NORTH), Direction.getNearest(next.subtract(p), Direction.NORTH), until);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // the fuel falls out; molten metal and unmelted ore in a broken tap are lost
        if (level != null && fuel > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.CHARCOAL, fuel));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        melt.load(input.getIntArray("melt").orElse(new int[0]));
        fuel = input.getIntOr("fuel", 0);
        burn = input.getIntOr("burn", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        maxTemp = input.getIntOr("max_temp", 800);
        bellowsTemp = input.getIntOr("bellows_temp", 1300);
        temp = input.getFloatOr("temp", Float.NaN);
        meltProgress = input.getFloatOr("melt_progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("melt", melt.save());
        output.putInt("fuel", fuel);
        output.putInt("burn", burn);
        output.putInt("burn_total", burnTotal);
        output.putInt("max_temp", maxTemp);
        output.putInt("bellows_temp", bellowsTemp);
        if (!Float.isNaN(temp)) output.putFloat("temp", temp);
        output.putFloat("melt_progress", meltProgress);
    }

    // the client draws the molten pool in the shaft (client/MoltenRenderer)
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
        return Component.translatable("container.mechanical.smelting_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FurnaceMenu(containerId, inventory, this, data);
    }
}
