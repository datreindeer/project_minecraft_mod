package com.mechanical.client.clan;

import com.mechanical.Mechanical;
import com.mechanical.clan.ClanNetwork;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** G opens the tribe screen: ask the server, which answers with a view ({@link ClanScreen}). The roster feeds the Tab list ({@link TribeTabList}). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ClanClient {
    public static final KeyMapping OPEN = new KeyMapping("key.mechanical.tribe", com.mojang.blaze3d.platform.InputConstants.KEY_G, KeyMapping.Category.MULTIPLAYER);

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(ClanNetwork.View.TYPE, (view, context) -> {
            Minecraft mc = Minecraft.getInstance();
            if (view.open()) mc.gui.setScreen(new ClanScreen(view));
            else if (mc.gui.screen() instanceof ClanScreen screen) screen.update(view);
        });
        event.register(ClanNetwork.Roster.TYPE, (roster, context) -> TribeTabList.roster = roster);
        event.register(com.mechanical.clan.Lands.Entered.TYPE, (land, context) -> LandTitle.show(land));
    }

    @SubscribeEvent
    static void registerHud(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerBelow(net.neoforged.neoforge.client.gui.VanillaGuiLayers.TITLE, LandTitle.ID, LandTitle::render);
    }

    /** A new server may not send a roster: until it does, Tab shows vanilla's list. */
    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TribeTabList.roster = null;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) ClientPacketDistributor.sendToServer(new ClanNetwork.Request());
        }
    }

    private ClanClient() {}
}
