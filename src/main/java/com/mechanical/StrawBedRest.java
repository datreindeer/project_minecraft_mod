package com.mechanical;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Straw Bed: the stone age can't sleep the night away, but lying down in straw is a rest. Right-click sets your respawn
 * point and resets the time since you last rested (so phantoms keep away); the night goes on.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class StrawBedRest {
    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.STRAW_BED)) return;
        if (event.getEntity().isSecondaryUseActive() && !event.getItemStack().isEmpty()) return; // sneak-place against it
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(player.level().dimension(), event.getPos(), player.getYRot(), player.getXRot()), false), false);
        Sanity.rest(player);
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        player.sendOverlayMessage(Component.translatable("message.mechanical.straw_bed_rest").withStyle(ChatFormatting.GRAY));
    }

    private StrawBedRest() {}
}
