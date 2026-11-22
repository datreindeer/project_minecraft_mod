package com.mechanical;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Both hands on a felled log's end ({@link FelledLogEntity#liftedBy}): no item use, attacks or block breaking; only the
 * log itself (put it down) and a Wagon (load it) take the click. Sprinting: CarcassHauling#blocksSprint.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class LogLifting {
    private static boolean lifting(Player player) {
        return FelledLogEntity.liftedBy(player) != null;
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!lifting(event.getEntity())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!lifting(event.getEntity())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof FelledLogEntity || event.getTarget() instanceof WagonEntity || !lifting(event.getEntity())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (lifting(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (lifting(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && lifting(player)) event.setCanceled(true);
    }

    private LogLifting() {}
}
