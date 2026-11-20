package com.mechanical.client.wildlife.waterfowl;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.waterfowl.WaterfowlFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers of the waterfowl (WaterfowlFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class WaterfowlClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WaterfowlFauna.MALLARD.get(),
                c -> new WaterfowlRenderer(c, new WaterfowlBirdModel(c.bakeLayer(WaterfowlBirdModel.MALLARD), 0.5F, 0.9F), 0.25F, 1.1F));
        event.registerEntityRenderer(WaterfowlFauna.MUTE_SWAN.get(),
                c -> new WaterfowlRenderer(c, new WaterfowlBirdModel(c.bakeLayer(WaterfowlBirdModel.MUTE_SWAN), 1.45F, 0.6F), 0.55F, 0.75F));
        event.registerEntityRenderer(WaterfowlFauna.GREY_HERON.get(),
                c -> new WaterfowlRenderer(c, new WaderModel(c.bakeLayer(WaderModel.GREY_HERON), false, 0.75F), 0.35F, 0.6F));
        event.registerEntityRenderer(WaterfowlFauna.COMMON_CRANE.get(),
                c -> new WaterfowlRenderer(c, new WaderModel(c.bakeLayer(WaderModel.COMMON_CRANE), true, 0.65F), 0.4F, 0.65F));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WaterfowlBirdModel.MALLARD, WaterfowlBirdModel::createMallardLayer);
        event.registerLayerDefinition(WaterfowlBirdModel.MUTE_SWAN, WaterfowlBirdModel::createMuteSwanLayer);
        event.registerLayerDefinition(WaderModel.GREY_HERON, WaderModel::createGreyHeronLayer);
        event.registerLayerDefinition(WaderModel.COMMON_CRANE, WaderModel::createCommonCraneLayer);
    }

    private WaterfowlClient() {}
}
