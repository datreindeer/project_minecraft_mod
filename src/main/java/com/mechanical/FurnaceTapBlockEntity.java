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
 * an empty middle block (the chamber) with a {@link FurnaceChuteBlock} above it, the tap being the middle of the bottom
 * front wall; checked every second and on neighbour changes. Formed, it marks the side and back walls' bottom middles
 * as tuyeres and lights the chute ({@link #decorate}).
 * <ul>
 * <li>Intake: item entities in the chute's opening. Ore and ingots go into the {@link FurnaceMelt} charge (up to its
 * capacity, and only what mixes), Charcoal / Coal into the fuel (up to {@link #FUEL_MAX}); anything else is thrown back
 * off the chute.</li>
 * <li>Heat: the kiln's model ({@link KilnBlockEntity#heatStep}, charcoal's 800° natural, 1300° with bellows,
 * {@link KilnBlockEntity#PUMP}° per pump). Lit with a Fire Drill; burns its fuel while lit; goes out when it runs out.</li>
 * <li>Melting: one quarter unit per {@link #QUARTER_TICKS} at the melting point, faster when hotter ({@link KilnBlockEntity#speed}).</li>
 * <li>Tapping: open, hot enough, and with a settled melt, the tap fills an empty Clay Mold right in front of the spout
 * after a moment, or pours into the Clay Channel there and the metal runs through the network ({@link ChannelFlow}).
 * The flow keeps running (downhill, into molds) with the tap closed too.</li>
 * </ul>
 */
public class FurnaceTapBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FUEL_MAX = 16, QUARTER_TICKS = 6;
    /** A pour straight into a mold in front of the spout takes this long plus {@link #TICKS_PER_UNIT} per unit of the mold. */
    public static final int POUR_TICKS = 10, TICKS_PER_UNIT = 5;

    final FurnaceMelt melt = new FurnaceMelt();
    private int fuel, burn, burnTotal, maxTemp = 800, bellowsTemp = 1300;
    private float temp = Float.NaN, ambient = 20, meltProgress;
    // not saved: worked out again after loading
    private boolean formed;
    private @Nullable Component problem = Component.translatable("message.mechanical.furnace.checking");
    private int checkIn, stepIn, pourLeft, syncIn, pouringFor;
    private boolean dirty;

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

    /** The bottom of the furnace's middle column (a brick), right behind the tap; the chamber is above it, the chute on top. */
    public BlockPos shaft() {
        return worldPosition.relative(facing().getOpposite());
    }

    public BlockPos chute() {
        return shaft().above(2);
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

    /** Checks the structure: 24 Furnace Bricks, this tap and the chute around the empty chamber. */
    public void check(ServerLevel level) {
        BlockPos c = shaft();
        int missing = 0;
        BlockPos firstMissing = null, blocked = null;
        boolean bottomOpen = false, chute = false;
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (p.equals(worldPosition)) continue;
                    BlockState s = level.getBlockState(p);
                    if (dx == 0 && dz == 0 && dy == 1) {
                        if (!s.isAir()) blocked = p.immutable();
                    } else if (dx == 0 && dz == 0 && dy == 2) {
                        chute = s.is(Smelting.FURNACE_CHUTE.get());
                    } else if (!s.is(Smelting.FURNACE_BRICKS.get())) {
                        if (dx == 0 && dz == 0) bottomOpen = s.isAir();
                        if (missing++ == 0) firstMissing = p.immutable();
                    }
                }
            }
        }
        boolean was = formed;
        BlockPos top = chute();
        if (bottomOpen && !chute && level.getBlockState(top).isAir()) // built before 0.58: the shaft was open top to bottom
            problem = Component.translatable("message.mechanical.furnace.old_shaft", c.getX(), c.getY(), c.getZ(), top.getX(), top.getY(), top.getZ());
        else if (missing > 0) problem = Component.translatable("message.mechanical.furnace.missing", missing, firstMissing.getX(), firstMissing.getY(), firstMissing.getZ());
        else if (!chute) problem = Component.translatable("message.mechanical.furnace.no_chute", top.getX(), top.getY(), top.getZ());
        else if (blocked != null) problem = Component.translatable("message.mechanical.furnace.blocked", blocked.getX(), blocked.getY(), blocked.getZ());
        else problem = null;
        formed = problem == null;
        if (formed && !was) {
            level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
            Smelting.award(level, worldPosition, Smelting.FORMED_ADVANCEMENT, 12);
        }
        decorate(level);
    }

    /**
     * Shows the formed furnace: the middle of the bottom row of the side and back walls as tuyeres (glowing while lit),
     * the chute's glow. Only touches bricks that aren't another furnace's tuyere; client update only, no neighbour updates.
     */
    void decorate(ServerLevel level) {
        Direction f = facing();
        FurnaceBricksBlock.Tuyere want = !formed ? FurnaceBricksBlock.Tuyere.NONE : lit() ? FurnaceBricksBlock.Tuyere.LIT : FurnaceBricksBlock.Tuyere.COLD;
        for (Direction d : new Direction[] {f.getClockWise(), f.getCounterClockWise(), f.getOpposite()}) {
            BlockPos p = shaft().relative(d);
            BlockState s = level.getBlockState(p);
            if (!s.is(Smelting.FURNACE_BRICKS.get())) continue;
            if (s.getValue(FurnaceBricksBlock.TUYERE) != FurnaceBricksBlock.Tuyere.NONE && s.getValue(FurnaceBricksBlock.FACING) != d) continue;
            BlockState t = s.setValue(FurnaceBricksBlock.TUYERE, want).setValue(FurnaceBricksBlock.FACING, d);
            if (t != s) level.setBlock(p, t, Block.UPDATE_CLIENTS);
        }
        BlockState s = level.getBlockState(chute());
        if (s.is(Smelting.FURNACE_CHUTE.get()) && s.getValue(FurnaceChuteBlock.LIT) != (formed && lit()))
            level.setBlock(chute(), s.setValue(FurnaceChuteBlock.LIT, formed && lit()), Block.UPDATE_CLIENTS);
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
    private long heldUntil;

    public boolean pump() {
        return pump(false);
    }

    /** One pump; an Iron Bellows ({@code iron}) pumps twice as hard and holds the heat ({@link KilnBlockEntity#IRON_HOLD_TICKS}). */
    public boolean pump(boolean iron) {
        if (!formed || !lit()) return false;
        if (iron && level != null) heldUntil = level.getGameTime() + KilnBlockEntity.IRON_HOLD_TICKS;
        float max = Math.min(bellowsTemp, KilnBlockEntity.CEILING);
        if (temperature() < max) temp = Math.min(max, temperature() + KilnBlockEntity.PUMP * (iron ? KilnBlockEntity.IRON_PUMP_FACTOR : 1));
        setChanged();
        return true;
    }

    private void setLit(boolean lit) {
        if (level == null || lit() == lit) return;
        level.setBlock(worldPosition, getBlockState().setValue(FurnaceTapBlock.LIT, lit), Block.UPDATE_ALL);
        if (level instanceof ServerLevel server) decorate(server);
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
        BlockPos top = chute().above();
        temp = Math.min(KilnBlockEntity.heatStep(temperature(), lit, maxTemp, ambient, level.isRainingAt(top), level.getGameTime() < heldUntil),
                KilnBlockEntity.CEILING);

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

    /** Takes what fell through the chute into the chamber, or lies in the chute (or on its rim). */
    private void intake(ServerLevel level) {
        BlockPos c = chute();
        AABB box = new AABB(c.getX(), c.getY() - 1, c.getZ(), c.getX() + 1, c.getY() + 1.25, c.getZ() + 1);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!e.isAlive()) continue;
            ItemStack stack = e.getItem().copy();
            boolean metal = FurnaceMelt.quarters(stack) != null;
            int taken = take(stack);
            if (taken > 0) {
                stack.shrink(taken);
                level.playSound(null, c, metal ? SoundEvents.LAVA_POP : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.02);
            }
            if (stack.isEmpty()) {
                e.discard();
                continue;
            }
            // doesn't fit / doesn't melt / doesn't mix: thrown back off the chute (from above the intake box)
            e.setItem(stack);
            e.setPos(c.getX() + 0.5, c.getY() + 1.6, c.getZ() + 0.5);
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            e.setDeltaMovement(Math.cos(angle) * 0.25, 0.4, Math.sin(angle) * 0.25);
            level.playSound(null, c.above(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
        }
    }

    /** Puts what it can of {@code stack} into the charge or the fuel; returns how many items it took. */
    int take(ItemStack stack) {
        FurnaceMelt.Amount a = FurnaceMelt.quarters(stack);
        int taken = 0;
        if (a != null) {
            taken = melt.fits(a, stack.getCount());
            melt.add(a, taken);
        } else if (isFuel(stack)) {
            taken = Math.min(stack.getCount(), FUEL_MAX - fuel);
            fuel += taken;
        }
        if (taken > 0) dirty = true;
        return taken;
    }

    /** The metal the tap lets out now, or null (closed, broken, too cold, still settling, nothing castable). */
    @Nullable Metal flowing(BlockState state) {
        Metal metal = melt.alloy();
        boolean flows = formed && state.getValue(FurnaceTapBlock.OPEN) && metal != null && !melt.settling() && temperature() >= metal.melt;
        return flows ? metal : null;
    }

    /** A mold right in front of the spout fills after a moment; a channel there gets the metal and runs it on ({@link ChannelFlow}). */
    private void pour(ServerLevel level, BlockState state) {
        Metal metal = flowing(state);
        BlockPos spout = worldPosition.relative(facing());
        BlockState front = level.getBlockState(spout);
        if (front.getBlock() instanceof ClayMoldBlock mold) {
            if (metal != null && accepts(front, metal, melt.pourable())) {
                if (pourLeft <= 0) {
                    pourLeft = POUR_TICKS + mold.shape.units * TICKS_PER_UNIT;
                    level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.7F, 1.1F);
                }
                pouringFor = 2;
                if (--pourLeft <= 0 && melt.pour(mold.shape.units * 4)) {
                    dirty = true;
                    level.setBlock(spout, front.setValue(ClayMoldBlock.FILL, ClayMoldBlock.Fill.MOLTEN).setValue(ClayMoldBlock.METAL, metal), Block.UPDATE_ALL);
                    level.scheduleTick(spout, mold, ClayMoldBlock.SET_TICKS);
                    level.playSound(null, spout, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
                    level.sendParticles(ParticleTypes.LAVA, spout.getX() + 0.5, spout.getY() + 0.25, spout.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.0);
                }
            } else {
                pourLeft = 0;
            }
        } else if (--stepIn <= 0) {
            stepIn = ChannelFlow.STEP_TICKS;
            BlockPos first = ChannelFlow.across(level, worldPosition, ChannelFlow.port(worldPosition, facing(), worldPosition.getY()));
            if (first != null && level.getBlockState(first).getBlock() instanceof ClayChannelBlock && ChannelFlow.step(level, first, melt, metal)) {
                dirty = true;
                pouringFor = ChannelFlow.STEP_TICKS * 2;
            }
        }
        if (pouringFor > 0) pouringFor--;
        setPouring(pouringFor > 0);
    }

    static boolean accepts(BlockState s, Metal metal, int quarters) {
        return s.getBlock() instanceof ClayMoldBlock mold && s.getValue(ClayMoldBlock.FILL) == ClayMoldBlock.Fill.EMPTY
                && metal.casts(mold.shape) && mold.shape.units * 4 <= quarters;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // the fuel falls out; molten metal and unmelted ore in a broken tap are lost
        if (level != null && fuel > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.CHARCOAL, fuel));
        if (level instanceof ServerLevel server) {
            formed = false;
            decorate(server); // plain bricks again
        }
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
