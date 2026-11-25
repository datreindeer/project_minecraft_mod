package com.mechanical.mixin;

import com.mechanical.DimensionLock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A closed Nether or End ({@link DimensionLock}) is stopped before the portal looks for its destination: vanilla builds
 * the Nether's exit portal (and the End platform) there, before the travel event is fired.
 */
@Mixin(PortalProcessor.class)
public class PortalProcessorMixin {
    @Shadow
    private Portal portal;

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void mechanical$closed(ServerLevel level, Entity entity, CallbackInfoReturnable<TeleportTransition> cir) {
        // a Nether Portal leads into the Nether from anywhere else, an End Portal into the End
        boolean shut = portal instanceof NetherPortalBlock && DimensionLock.blocks(level.dimension(), Level.NETHER)
                || portal instanceof EndPortalBlock && DimensionLock.blocks(level.dimension(), Level.END);
        if (!shut) return;
        DimensionLock.tell(entity, "closed");
        cir.setReturnValue(null);
    }
}
