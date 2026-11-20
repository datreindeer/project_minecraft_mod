package com.mechanical.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.clan.Clans;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;

/** The locator bar shows a player only to members of the same tribe (spectators still see everyone). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWaypointMixin {
    @Inject(method = "makeWaypointConnectionWith", at = @At("HEAD"), cancellable = true)
    private void mechanical$tribeOnly(ServerPlayer receiver, CallbackInfoReturnable<Optional<WaypointTransmitter.Connection>> cir) {
        if ((Object) this instanceof ServerPlayer source && !receiver.isSpectator() && !Clans.sameTribe(source, receiver)) cir.setReturnValue(Optional.empty());
    }
}
