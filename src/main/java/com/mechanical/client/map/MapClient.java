package com.mechanical.client.map;

import com.mechanical.Mechanical;
import com.mechanical.map.MapNetwork;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** M opens the world map ({@link MapScreen}), which then asks the server for what is in view. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class MapClient {
    public static final KeyMapping OPEN = new KeyMapping("key.mechanical.world_map", KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD,
            InputConstants.KEY_M, KeyMapping.Category.MISC);

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(MapNetwork.People.TYPE, (people, context) -> {
            if (Minecraft.getInstance().gui.screen() instanceof MapScreen map) map.people(people);
        });
        event.register(MapNetwork.Features.TYPE, (features, context) -> {
            if (Minecraft.getInstance().gui.screen() instanceof MapScreen map) map.features(features.json());
        });
        event.register(MapNetwork.Tile.TYPE, (tile, context) -> {
            if (Minecraft.getInstance().gui.screen() instanceof MapScreen map) map.tile(tile);
        });
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new MapScreen());
        }
    }

    private MapClient() {}
}
