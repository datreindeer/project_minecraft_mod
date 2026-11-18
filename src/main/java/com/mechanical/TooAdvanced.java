package com.mechanical;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Blocks from later ages found in villages (furnace, anvil, smithing table ...) can't be used yet: right-clicking does
 * nothing. Their loot tables are overridden too, so breaking them gives only rocks or sticks. The list is the
 * {@code mechanical:too_advanced} block tag, so a later age can take blocks off it.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TooAdvanced {
    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Mechanical.MODID, "too_advanced"));

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(TAG) || event.getEntity().isCreative()) return;
        // sneaking with a block still places it against this one
        if (event.getEntity().isSecondaryUseActive() && !event.getItemStack().isEmpty()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!event.getLevel().isClientSide()) event.getEntity().sendOverlayMessage(Component.translatable("message.mechanical.too_advanced"));
    }

    private TooAdvanced() {}
}
