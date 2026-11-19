package com.mechanical;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Straw Bed: the stone age can't sleep the night away, but lying down in straw is a rest. Right-click sets your respawn
 * point and resets the time since you last rested (so phantoms keep away); the night goes on.
 * Right-click one holding a Wolf Pelt to spread it over the bed: a Furred Straw Bed, which rests better (more sanity, a
 * little healing, and warmth for a while afterwards). Broken, it gives the pelt back.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class StrawBedRest {
    static final float FURRED_HEAL = 4;

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean furred = state.is(Mechanical.FURRED_STRAW_BED.get());
        if (!furred && !state.is(Blocks.STRAW_BED)) return;
        ItemStack held = event.getItemStack();
        if (event.getEntity().isSecondaryUseActive() && !held.isEmpty()) return; // sneak-place against it
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!furred && held.is(Wildlife.WOLF_PELT.get())) {
            spreadPelt(level, pos, state);
            held.consume(1, player);
            return;
        }
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(player.level().dimension(), pos, player.getYRot(), player.getXRot()), false), false);
        if (Sanity.rest(player, furred) && furred) player.heal(FURRED_HEAL);
        if (furred) Temperature.warmFromBedroll(player);
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        player.sendOverlayMessage(Component.translatable(furred ? "message.mechanical.furred_straw_bed_rest" : "message.mechanical.straw_bed_rest")
                .withStyle(ChatFormatting.GRAY));
    }

    /** Swaps both halves for the furred bed, keeping facing and part (no shape updates, so neither half breaks the other). */
    static void spreadPelt(Level level, BlockPos pos, BlockState state) {
        Block fur = Mechanical.FURRED_STRAW_BED.get();
        BlockPos other = pos.relative(AbstractBedBlock.getConnectedDirection(state));
        BlockState otherState = level.getBlockState(other);
        level.setBlock(pos, fur.withPropertiesOf(state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (otherState.is(Blocks.STRAW_BED)) level.setBlock(other, fur.withPropertiesOf(otherState), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private StrawBedRest() {}
}
