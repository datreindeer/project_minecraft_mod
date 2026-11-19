package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The ore lying on a Stone Anvil and how many times it has been struck. The item is drawn by client/AnvilRenderer. */
public class StoneAnvilBlockEntity extends BlockEntity {
    public static final int HITS = 4;
    /** Height of the anvil's top face, in blocks (the renderer lays the item there). */
    public static final float TOP = 5 / 16.0F;

    private ItemStack item = ItemStack.EMPTY;
    private int hits;

    public StoneAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(BronzeOres.STONE_ANVIL_ENTITY.get(), pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public void place(ServerLevel level, ItemStack one) {
        item = one;
        hits = 0;
        level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
        sync();
    }

    public ItemStack take() {
        ItemStack out = item;
        item = ItemStack.EMPTY;
        hits = 0;
        sync();
        return out;
    }

    /** One blow of the hammerstone: wears the tool, and the last of {@link #HITS} blows crushes the ore. */
    public void strike(ServerLevel level, Player player, ItemStack tool, InteractionHand hand) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + TOP + 0.05, z = worldPosition.getZ() + 0.5;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item.getItem()), x, y, z, 6, 0.12, 0.05, 0.12, 0.08);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()), x, y, z, 8, 0.2, 0.02, 0.2, 0.0);
        level.playSound(null, worldPosition, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0F, 0.7F + level.getRandom().nextFloat() * 0.3F);
        tool.hurtAndBreak(1, player, hand);
        if (++hits < HITS) {
            setChanged();
            return;
        }
        ItemStack out = BronzeOres.crushed(item);
        item = ItemStack.EMPTY;
        hits = 0;
        level.playSound(null, worldPosition, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 4, 0.1, 0.05, 0.1, 0.01);
        Block.popResource(level, worldPosition.above(), out);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!item.isEmpty()) output.store("item", ItemStack.CODEC, item);
        output.putInt("hits", hits);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        item = input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        hits = input.getIntOr("hits", 0);
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
