package com.mechanical.client;

import com.mechanical.Glider;
import com.mechanical.Mechanical;

import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Glider Cloak motion for the local player (see {@link Glider}). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class GliderClient {
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof LocalPlayer player
                && Glider.canGlide(player, player.input.keyPresses.jump(), player.input.keyPresses.shift())) Glider.glide(player);
    }

    private GliderClient() {}
}
