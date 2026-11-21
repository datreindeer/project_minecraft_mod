package com.mechanical;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Tinder: right-click an unlit campfire, Fire Pit or clay kiln with Leaf Litter, Bark or Mushroom Flesh to lay tinder in it. The next Fire Drill
 * attempt then catches much more easily; the tinder is used up when the fire catches.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Tinder {
    public static final float CHANCE_WITHOUT = 0.33F, CHANCE_WITH = 0.75F;

    /** On the campfire / kiln (lower half) block entity. */
    public static final Supplier<AttachmentType<Boolean>> TINDER = Mechanical.ATTACHMENTS.register("tinder", () -> AttachmentType
            .builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("value"))
            .build());

    public static void init() {}

    public static boolean isTinder(ItemStack stack) {
        return stack.is(Items.LEAF_LITTER) || stack.is(Mechanical.BARK_TAG) || stack.is(com.mechanical.wildlife.night.NightFauna.MUSHROOM_FLESH.get());
    }

    /** The block entity that holds the tinder for an unlit campfire or kiln at {@code pos}, or null. */
    public static BlockEntity holder(Level level, BlockPos pos, BlockState state) {
        if (CampfireBlock.canLight(state)) return level.getBlockEntity(pos);
        if (FirePit.canLight(state)) return FirePit.entity(level, pos, state);
        if (state.getBlock() instanceof KilnBlock && !state.getValue(KilnBlock.LIT)) return level.getBlockEntity(KilnBlock.lowerPos(state, pos));
        if (state.getBlock() instanceof FurnaceTapBlock && !state.getValue(FurnaceTapBlock.LIT)) return level.getBlockEntity(pos);
        if (state.getBlock() instanceof BloomeryHearthBlock && !state.getValue(BloomeryHearthBlock.LIT)) return level.getBlockEntity(pos);
        return null;
    }

    public static boolean has(BlockEntity holder) {
        return holder != null && holder.getData(TINDER);
    }

    public static void set(BlockEntity holder, boolean tinder) {
        holder.setData(TINDER, tinder);
        holder.setChanged();
    }

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!isTinder(stack)) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity holder = holder(level, pos, level.getBlockState(pos));
        if (holder == null) return;
        event.setCanceled(true); // don't place the leaf litter
        if (has(holder)) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level instanceof ServerLevel server) {
            set(holder, true);
            stack.consume(1, event.getEntity());
            server.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
            server.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.0);
        }
    }

    private Tinder() {}
}
