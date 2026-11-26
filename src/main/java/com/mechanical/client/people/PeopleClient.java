package com.mechanical.client.people;

import com.mechanical.Mechanical;
import com.mechanical.people.People;
import com.mechanical.people.TribeDialog;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class PeopleClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(People.TRIBESPERSON.get(), TribespersonRenderer::new);
        event.registerEntityRenderer(People.MINER.get(), TribespersonRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TribeCapeModel.ELDER, TribeCapeModel::createElder);
        event.registerLayerDefinition(TribeCapeModel.TRADER, TribeCapeModel::createTrader);
        event.registerLayerDefinition(TribeCapeModel.OAK, TribeCapeModel::createOak);
    }

    /** Tribespeople are drawn by the player renderers (their state is an AvatarRenderState): the cape layer goes on those. */
    @SubscribeEvent
    static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) renderer.addLayer(new TribeCapeLayer((net.minecraft.client.renderer.entity.player.AvatarRenderer<?>) renderer, event.getEntityModels()));
        }
    }

    /** An envoy speaks: show the talk screen (payload handlers run on the main thread). */
    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(TribeDialog.Open.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new TribeDialogScreen(payload)));
        event.register(com.mechanical.people.CaravanTrade.Open.TYPE, (payload, context) -> {
            if (Minecraft.getInstance().gui.screen() instanceof CaravanTradeScreen s && s.entity() == payload.entity()) s.update(payload);
            else Minecraft.getInstance().gui.setScreen(new CaravanTradeScreen(payload));
        });
        event.register(com.mechanical.people.OakQuests.Open.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new OakQuestScreen(payload)));
    }

    private PeopleClient() {}
}
