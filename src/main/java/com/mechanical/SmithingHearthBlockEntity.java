package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Smithing Hearth's coals ({@link SmithingHearthBlock}): {@link #SLOTS} pieces of iron or steel lie in them. While it
 * burns ({@link #BURN_TICKS} a Charcoal, up to {@link #MAX_FUEL} in store) a cold bloom or ingot comes to forging heat in
 * {@link #HEAT_TICKS} and a hot one stays hot however long it lies there. Drawn by client/SmithingHearthRenderer.
 */
public class SmithingHearthBlockEntity extends BlockEntity {
    public static final int SLOTS = 4, BURN_TICKS = 1200, MAX_FUEL = 16, HEAT_TICKS = 200;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final int[] heating = new int[SLOTS];
    private int fuel, burn;

    public SmithingHearthBlockEntity(BlockPos pos, BlockState state) {
        super(Smithing.SMITHING_HEARTH_ENTITY.get(), pos, state);
    }

    public ItemStack item(int slot) {
        return items.get(slot);
    }

    public int fuel() {
        return fuel;
    }

    public boolean lit() {
        return getBlockState().getValue(SmithingHearthBlock.LIT);
    }

    /** Charcoal into the store; false when full. */
    boolean addFuel() {
        if (fuel >= MAX_FUEL) return false;
        fuel++;
        setChanged();
        return true;
    }

    /** Lights it if there is fuel; false otherwise. */
    boolean light(ServerLevel level) {
        if (lit() || fuel == 0 && burn == 0) return false;
        if (burn == 0) {
            fuel--;
            burn = BURN_TICKS;
        }
        level.setBlock(worldPosition, getBlockState().setValue(SmithingHearthBlock.LIT, true), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7F, 1.0F);
        setChanged();
        return true;
    }

    /** A bloom or ingot into a free spot in the coals; false when there is none. */
    boolean put(ItemStack one) {
        for (int i = 0; i < SLOTS; i++) {
            if (!items.get(i).isEmpty()) continue;
            items.set(i, one);
            heating[i] = 0;
            sync();
            return true;
        }
        return false;
    }

    /** The last piece laid in (empty when there is none). */
    ItemStack take() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (items.get(i).isEmpty()) continue;
            ItemStack out = items.get(i);
            items.set(i, ItemStack.EMPTY);
            heating[i] = 0;
            sync();
            return out;
        }
        return ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmithingHearthBlockEntity hearth) {
        if (level instanceof ServerLevel server && level.getGameTime() % 20 == 0) hearth.second(server);
    }

    /** One second of burning: fuel, heating the cold pieces, keeping the hot ones hot. */
    void second(ServerLevel server) {
        BlockState state = getBlockState();
        BlockPos pos = worldPosition;
        SmithingHearthBlockEntity hearth = this;
        if (!state.getValue(SmithingHearthBlock.LIT)) return;
        hearth.burn -= 20;
        if (hearth.burn <= 0) {
            if (hearth.fuel > 0) {
                hearth.fuel--;
                hearth.burn = BURN_TICKS;
            } else {
                hearth.burn = 0;
                server.setBlock(pos, state.setValue(SmithingHearthBlock.LIT, false), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
                hearth.setChanged();
                return;
            }
        }
        boolean changed = false;
        long now = server.getGameTime();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = hearth.items.get(i);
            if (s.isEmpty()) continue;
            if (Ironworking.hotItem(s)) { // kept at forging heat
                s.set(Casting.COOLS_AT.get(), now + Ironworking.HOT_SECONDS * 20L);
                s.setDamageValue(0);
                continue;
            }
            net.minecraft.world.item.Item hot = Smithing.hotOf(s);
            if (hot == null || (hearth.heating[i] += 20) < HEAT_TICKS) continue;
            hearth.items.set(i, Ironworking.heated(s, hot, now)); // keeps its blows
            hearth.heating[i] = 0;
            changed = true;
        }
        if (changed) hearth.sync();
        else hearth.setChanged();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (ItemStack s : items) Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), s);
        for (int i = 0; i < fuel; i++) Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), new ItemStack(net.minecraft.world.item.Items.CHARCOAL));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items);
        output.putIntArray("heating", heating);
        output.putInt("fuel", fuel);
        output.putInt("burn", burn);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < SLOTS; i++) items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        });
        input.getIntArray("heating").ifPresent(a -> System.arraycopy(a, 0, heating, 0, Math.min(a.length, SLOTS)));
        fuel = input.getIntOr("fuel", 0);
        burn = input.getIntOr("burn", 0);
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
