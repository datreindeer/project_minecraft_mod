package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What lies on a griddle or skillet, or is shut in a barrel or press ({@link CookwareBlock}): single items, one per
 * slot. When they make a dish (Kitchen.match) it cooks: pans while the campfire under them burns (embers don't do), the
 * barrel and press with time alone, the mortar and sieve by hand (each empty-hand click one of {@code Kind.clicks}). Done, the items are replaced by the dish; on a pan it burns after
 * {@link Kitchen#PAN_BURN} more ticks over the fire.
 */
public class CookwareBlockEntity extends BlockEntity {
    private final List<ItemStack> items = new ArrayList<>();
    private boolean done;
    private int progress;

    public CookwareBlockEntity(BlockPos pos, BlockState state) {
        super(Kitchen.COOKWARE_ENTITY.get(), pos, state);
    }

    public CookwareBlock.Kind kind() {
        return ((CookwareBlock) getBlockState().getBlock()).kind;
    }

    /** What is in it now (the dish once done), for the renderer. */
    public List<ItemStack> items() {
        return items;
    }

    public boolean done() {
        return done;
    }

    public Kitchen.Match match() {
        return done ? null : Kitchen.match(kind().tool, kind().tier, items);
    }

    /** Time a dish takes in this cookware. */
    int cookTime() {
        return switch (kind().tool) {
            case PAN -> Kitchen.PAN_TIME;
            case BARREL -> Kitchen.BARREL_TIME;
            default -> Kitchen.PRESS_TIME;
        };
    }

    private boolean fireLit(ServerLevel level) {
        return TripodBlockEntity.fireRate(level.getBlockState(worldPosition.below())) >= 1;
    }

    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (items.isEmpty()) return;
        CookwareBlock.Kind kind = kind();
        if (kind.clicks > 0) return; // worked by hand (work)
        boolean heat = !kind.fire || fireLit(level);
        if (!done) {
            Kitchen.Match m = match();
            if (m == null || !heat) return;
            if (++progress >= cookTime()) {
                finish(level, m);
                level.playSound(null, pos, kind.fire ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.4F, kind.fire ? 1.6F : 0.8F);
            } else if (kind.fire && level.getRandom().nextInt(20) == 0) {
                level.sendParticles(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 1, 0.2, 0.02, 0.2, 0.01);
                level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.3F, 1.8F);
            }
            if (progress % 20 == 0) setChanged();
        } else if (kind.fire && heat && !items.getFirst().is(Mechanical.BURNT_FOOD.get()) && ++progress >= Kitchen.PAN_BURN) {
            int n = items.stream().mapToInt(ItemStack::getCount).sum();
            items.clear();
            items.add(new ItemStack(Mechanical.BURNT_FOOD.get(), n));
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 12, 0.2, 0.05, 0.2, 0.02);
            sync();
        }
    }

    private void finish(ServerLevel level, Kitchen.Match m) {
        items.clear();
        items.addAll(Kitchen.results(m));
        done = true;
        progress = 0;
        sync();
    }

    /** One turn of the pestle or shake of the sieve: the pestle moves a quarter round the mortar; the last one finishes. */
    void work(ServerLevel level, Kitchen.Match m) {
        CookwareBlock.Kind kind = kind();
        BlockState state = getBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            level.setBlock(worldPosition, state.setValue(HorizontalDirectionalBlock.FACING, state.getValue(HorizontalDirectionalBlock.FACING).getClockWise()), Block.UPDATE_ALL);
        double x = worldPosition.getX() + 0.5, z = worldPosition.getZ() + 0.5;
        ItemStack dust = items.get(level.getRandom().nextInt(items.size()));
        if (kind == CookwareBlock.Kind.MORTAR) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, dust.getItem()), x, worldPosition.getY() + 0.4, z, 4, 0.08, 0.02, 0.08, 0.03);
            level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, 1.5F + level.getRandom().nextFloat() * 0.2F);
        } else {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, dust.getItem()), x, worldPosition.getY() + 0.15, z, 6, 0.25, 0.02, 0.25, 0.0);
            level.playSound(null, worldPosition, SoundEvents.SAND_STEP, SoundSource.BLOCKS, 0.6F, 1.3F);
        }
        if (++progress >= kind.clicks) {
            finish(level, m);
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 0.7F);
        } else setChanged();
    }

    /** Puts one of the held item in (its bucket back for milk), if it can be part of a dish here and there is room. */
    void add(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        CookwareBlock.Kind kind = kind();
        if (done) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.cookware.take_first"));
            return;
        }
        if (!Kitchen.isIngredient(kind.tool, kind.tier, stack)) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.cookware.cant_go_in", stack.getHoverName()));
            return;
        }
        if (items.size() >= kind.capacity) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.cookware.full", kind.capacity));
            return;
        }
        if (Preserving.tooFarGone(stack, level, player)) return;
        ItemStack rest = stack.is(Items.MILK_BUCKET) ? new ItemStack(Items.BUCKET)
                : stack.is(BronzeTools.BRONZE_MILK_BUCKET.get()) ? stack.transmuteCopy(BronzeTools.BRONZE_BUCKET.get(), 1) : ItemStack.EMPTY;
        items.add(stack.consumeAndReturn(1, player));
        if (!rest.isEmpty() && !player.hasInfiniteMaterials()) {
            if (stack.isEmpty()) player.setItemInHand(hand, rest);
            else player.getInventory().placeItemBackInInventory(rest, Prediction.SERVER_ONLY);
        }
        progress = 0; // whatever went in last needs the full time
        level.playSound(null, worldPosition, kind.fire ? SoundEvents.CAMPFIRE_CRACKLE : SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.2F);
        sync();
    }

    /** Empty hand: the dish when done; while it works the time left (sneak: the raw things back); else the raw things. */
    void takeOut(ServerLevel level, Player player) {
        if (items.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.cookware.empty"));
            return;
        }
        Kitchen.Match m = match();
        if (!done && m != null && !player.isSecondaryUseActive() && kind().clicks > 0) {
            work(level, m);
            return;
        }
        if (!done && m != null && !player.isSecondaryUseActive()) {
            boolean heat = !kind().fire || fireLit(level);
            int seconds = (cookTime() - progress) / 20;
            player.sendOverlayMessage(heat ? Component.translatable("message.mechanical.cookware.working", m.dish().result().get().getName(new ItemStack(m.dish().result().get())),
                    seconds / 60, String.format("%02d", seconds % 60)) : Component.translatable("message.mechanical.cookware.no_fire"));
            return;
        }
        for (ItemStack s : items) {
            if (done) com.mechanical.skills.Skills.cooked(player, s);

            player.getInventory().placeItemBackInInventory(s, Prediction.SERVER_ONLY);
        }
        items.clear();
        done = false;
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.0F);
        sync();
    }

    private void sync() {
        setChanged();
        if (level == null) return;
        BlockState state = getBlockState(), now = state;
        boolean working = !done && !items.isEmpty() && match() != null;
        if (state.hasProperty(CookwareBlock.OPEN)) now = now.setValue(CookwareBlock.OPEN, !working);
        if (state.hasProperty(CookwareBlock.PRESSING)) now = now.setValue(CookwareBlock.PRESSING, working || done);
        if (now != state) level.setBlock(worldPosition, now, Block.UPDATE_ALL);
        level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
    }

    /** Broken: what was in it spills. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) for (ItemStack s : items) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, s);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.CODEC.listOf(), List.copyOf(items));
        output.putBoolean("done", done);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        input.read("items", ItemStack.CODEC.listOf()).ifPresent(items::addAll);
        done = input.getBooleanOr("done", false);
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
