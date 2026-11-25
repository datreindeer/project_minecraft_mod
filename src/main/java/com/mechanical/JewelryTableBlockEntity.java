package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What lies on a {@link JewelryTableBlock}. Steel Casting Table: slot 0 the Gold, Silver or Electrum Ingots waiting (one metal at a
 * time), slot 1 the cast pieces; {@link #mode} the mould (0: two Ring Blanks, 1: a Chain); with a Clay Kiln at the metal's
 * melting heat (gold 1100°, silver lower) or more beside it one ingot
 * melts and sets every {@link Jewelry#CAST_TICKS}. Jeweler's Bench: slot 0 the blank, slot 1 the cut gem; a chisel on it
 * sets the stone ({@link Jewelry#make}).
 */
public class JewelryTableBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int mode, progress;

    public JewelryTableBlockEntity(BlockPos pos, BlockState state) {
        super(Jewelry.TABLE_ENTITY.get(), pos, state);
    }

    public boolean casting() {
        return getBlockState().getBlock() instanceof JewelryTableBlock b && b.casting;
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public int mode() {
        return mode;
    }

    /** The ingot on the table decides the metal it casts: Gold (also while empty), Silver or Electrum. */
    private Molten metal() {
        ItemStack in = items.get(0);
        return in.is(IronOres.SILVER_INGOT.get()) ? Molten.SILVER : in.is(IronOres.ELECTRUM_INGOT.get()) ? Molten.ELECTRUM : Molten.GOLD;
    }

    /** What one ingot casts into in the current mould: two Ring Blanks or a Chain, of the metal on the table. */
    public ItemStack castResult() {
        Molten metal = metal();
        if (metal == Molten.SILVER) return mode == 0 ? new ItemStack(Jewelry.SILVER_RING_BLANK.get(), 2) : new ItemStack(Jewelry.SILVER_CHAIN.get());
        if (metal == Molten.ELECTRUM) return mode == 0 ? new ItemStack(Jewelry.ELECTRUM_RING_BLANK.get(), 2) : new ItemStack(Jewelry.ELECTRUM_CHAIN.get());
        return mode == 0 ? new ItemStack(Jewelry.GOLD_RING_BLANK.get(), 2) : new ItemStack(Jewelry.GOLD_CHAIN.get());
    }

    /** The kiln heat the ingots on the table need to melt: their metal's melting point. */
    public int castTemp() {
        return metal().melt;
    }

    boolean accepts(ItemStack stack) {
        if (casting()) return stack.is(Items.GOLD_INGOT) || stack.is(IronOres.SILVER_INGOT.get()) || stack.is(IronOres.ELECTRUM_INGOT.get());
        return Jewelry.jewelFor(stack) != null || Jewelry.gemOf(stack) != null || stack.getItem() instanceof BronzeTools.ChiselItem;
    }

    /** A Clay Kiln (either half) beside it at gold's melting heat or more: the casting multiblock is whole and hot. */
    public static boolean hotKilnBeside(ServerLevel level, BlockPos pos) {
        return hotKilnBeside(level, pos, IronOres.GOLD_TEMP);
    }

    /** As above for a given heat (silver melts lower than gold). */
    public static boolean hotKilnBeside(ServerLevel level, BlockPos pos, int temp) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = pos.relative(d);
            for (BlockPos k : new BlockPos[] {p, p.below()})
                if (level.getBlockEntity(k) instanceof KilnBlockEntity kiln && kiln.temperature() >= temp) return true;
        }
        return false;
    }

    void serverTick(ServerLevel level, BlockPos pos) {
        ItemStack in = items.get(0), out = items.get(1), result = castResult();
        boolean room = out.isEmpty() || ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize();
        if (in.isEmpty() || !room || !hotKilnBeside(level, pos, castTemp())) {
            if (progress > 0) progress = Math.max(0, progress - 2); // the melt sets back in the crucible
            return;
        }
        if (level.getGameTime() % 10 == 0) level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 2, 0.2, 0, 0.2, 0.01);
        if (++progress < Jewelry.CAST_TICKS) return;
        progress = 0;
        in.shrink(1);
        if (out.isEmpty()) items.set(1, result);
        else out.grow(result.getCount());
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.4F);
        for (ServerPlayer p : level.getPlayers(p -> p.blockPosition().closerThan(pos, 8))) FirePit.award(p, "iron_age/gold_casting");
        sync();
    }

    void use(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        if (casting()) {
            ItemStack in = items.get(0);
            if (!in.isEmpty() && !ItemStack.isSameItemSameComponents(in, stack)) { // one metal at a time
                player.sendOverlayMessage(Component.translatable("message.mechanical.casting.other_metal", in.getHoverName()));
                return;
            }
            if (in.getCount() >= Jewelry.TABLE_MAX_INGOTS) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.casting.full", Jewelry.TABLE_MAX_INGOTS));
                return;
            }
            if (in.isEmpty()) items.set(0, stack.consumeAndReturn(1, player));
            else {
                in.grow(1);
                stack.consume(1, player);
            }
            if (!hotKilnBeside(level, worldPosition, castTemp())) player.sendOverlayMessage(Component.translatable("message.mechanical.casting.needs_kiln", castTemp()));
            level.playSound(null, worldPosition, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
            sync();
            return;
        }
        Item piece = Jewelry.jewelFor(items.get(0));
        Jewelry.Jewel jewel = Jewelry.gemOf(items.get(1));
        if (stack.getItem() instanceof BronzeTools.ChiselItem) {
            if (piece == null || jewel == null) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.jewelers_bench.needs"));
                return;
            }
            player.getInventory().placeItemBackInInventory(Jewelry.make(piece, jewel), Prediction.SERVER_ONLY);
            items.set(0, ItemStack.EMPTY);
            items.set(1, ItemStack.EMPTY);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3F, 1.8F);
            if (player instanceof ServerPlayer sp) {
                FirePit.award(sp, "iron_age/jeweler");
                if (jewel.flawless()) FirePit.award(sp, "iron_age/flawless_jewel");
            }
            sync();
            return;
        }
        int slot = Jewelry.jewelFor(stack) != null ? 0 : 1;
        if (!items.get(slot).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.jewelers_bench.occupied"));
            return;
        }
        items.set(slot, stack.consumeAndReturn(1, player));
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.2F);
        sync();
    }

    /** Casting table: take what is cast, sneak to take the ingots back, else switch the mould. Bench: take back the gem, then the blank. */
    void useEmptyHand(ServerLevel level, Player player) {
        if (casting()) {
            if (!items.get(1).isEmpty()) give(player, 1);
            else if (player.isShiftKeyDown() && !items.get(0).isEmpty()) give(player, 0);
            else {
                mode = (mode + 1) % 2;
                progress = 0;
                player.sendOverlayMessage(Component.translatable("message.mechanical.casting.mode", castResult().getHoverName(), castResult().getCount()));
                sync();
            }
            return;
        }
        if (!items.get(1).isEmpty()) give(player, 1);
        else if (!items.get(0).isEmpty()) give(player, 0);
        else player.sendOverlayMessage(Component.translatable("message.mechanical.jewelers_bench.needs"));
    }

    private void give(Player player, int slot) {
        player.getInventory().placeItemBackInInventory(items.get(slot), Prediction.SERVER_ONLY);
        items.set(slot, ItemStack.EMPTY);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) Containers.dropContents(level, pos, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
        output.putInt("mode", mode);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        mode = input.getIntOr("mode", 0);
        progress = input.getIntOr("progress", 0);
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
