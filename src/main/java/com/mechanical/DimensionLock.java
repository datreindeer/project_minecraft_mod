package com.mechanical;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The Nether and the End are closed for now (config {@code netherAndEndClosed} in mechanical-local.toml, ServerConfig):
 * nothing travels there (portals: mixin.PortalProcessorMixin, which also stops the Nether from building an exit portal;
 * anything else, commands included: {@link EntityTravelToDimensionEvent}), no Nether portal is lit outside the Nether and no
 * Eye of Ender goes into an End Portal Frame. The way home stays open: portals can still be lit and used in the Nether, the
 * End's exit portal works, and respawning is not travel.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class DimensionLock {
    public static boolean closed() {
        return ServerConfig.NETHER_AND_END_CLOSED.get();
    }

    /** May something in {@code from} not go to {@code to}? Only the way into the Nether and the End is shut. */
    public static boolean blocks(ResourceKey<Level> from, ResourceKey<Level> to) {
        return closed() && from != to && (to == Level.NETHER || to == Level.END);
    }

    public static void tell(Entity entity, String message) {
        if (entity instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.mechanical.dimension." + message));
    }

    // before the others (a Downed player's carrier doesn't put him down for a journey that never happens)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onTravel(EntityTravelToDimensionEvent event) {
        if (!blocks(event.getEntity().level().dimension(), event.getDimension())) return;
        event.setCanceled(true);
        tell(event.getEntity(), "closed");
    }

    @SubscribeEvent
    static void onPortalLit(BlockEvent.PortalSpawnEvent event) {
        if (closed() && event.getLevel() instanceof Level level && level.dimension() != Level.NETHER) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onEyeIntoFrame(PlayerInteractEvent.RightClickBlock event) {
        // server side only: the server's config decides (a client's prediction is corrected by the server)
        if (event.getLevel().isClientSide() || !closed() || !event.getItemStack().is(Items.ENDER_EYE)
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.END_PORTAL_FRAME)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        tell(event.getEntity(), "frame");
    }

    private DimensionLock() {}
}
