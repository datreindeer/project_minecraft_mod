package com.mechanical;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Storage Jar's 9 slots (vanilla 3x3 menu). While nobody has it open the lid is on and the jar is sealed: everything
 * perishable in it carries the time it was sealed ({@link Preserving#intoBasket}), and when the lid comes off again its
 * spoil time moves later so the sealed time counted only {@link Pottery#JAR_RATE}. Items a hopper takes out or puts in
 * while it is sealed are settled / stamped on the way, so nothing leaves with a stale stamp.
 */
public class StorageJarBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            long now = level.getGameTime();
            items.replaceAll(s -> Preserving.outOfBasket(s, now, sealedRate(s)));
            lid(level, pos, state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            long now = level.getGameTime();
            items.replaceAll(s -> Preserving.intoBasket(s, now));
            lid(level, pos, state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {}

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof DispenserMenu menu && menu.slots.get(0).container == StorageJarBlockEntity.this;
        }
    };

    public StorageJarBlockEntity(BlockPos pos, BlockState state) {
        this(Pottery.STORAGE_JAR_ENTITY.get(), pos, state);
    }

    /** For stores built the same way (the Root Cellar). */
    protected StorageJarBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Share of the normal rate this stack ages at while sealed. */
    protected float rate(ItemStack stack) {
        return Pottery.JAR_RATE;
    }

    /** {@link #rate} times the cold where the store stands (Preserving.coldRate). */
    private float sealedRate(ItemStack stack) {
        return rate(stack) * (level == null ? 1 : Preserving.coldRate(level, worldPosition));
    }

    protected void lidSound(Level level, BlockPos pos, boolean open) {
        level.playSound(null, pos, open ? SoundEvents.DECORATED_POT_INSERT : SoundEvents.DECORATED_POT_HIT, SoundSource.BLOCKS, 0.6F,
                open ? 1.3F : 0.9F + level.getRandom().nextFloat() * 0.1F);
    }

    private void lid(Level level, BlockPos pos, BlockState state, boolean open) {
        lidSound(level, pos, open);
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, open));
    }

    private boolean sealed() {
        return openers.getOpenerCount() == 0 && level != null;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = super.removeItem(slot, count);
        return sealed() ? Preserving.outOfBasket(out, level.getGameTime(), sealedRate(out)) : out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = super.removeItemNoUpdate(slot);
        return sealed() ? Preserving.outOfBasket(out, level.getGameTime(), sealedRate(out)) : out;
    }

    @Override
    public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        super.setItem(slot, sealed() ? Preserving.intoBasket(stack, level.getGameTime()) : stack, insideTransaction);
    }

    /** Broken: the food comes out settled, as if the lid had been lifted. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (sealed()) items.replaceAll(s -> Preserving.outOfBasket(s, level.getGameTime(), sealedRate(s)));
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator())
            openers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator()) openers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return openers.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
    }

    void recheckOpen() {
        if (!remove) openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DispenserMenu(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.mechanical.storage_jar");
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }
}
