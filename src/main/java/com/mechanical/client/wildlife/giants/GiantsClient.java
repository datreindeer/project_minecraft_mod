package com.mechanical.client.wildlife.giants;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.giants.GiantsFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers for the Ice Age giants (GiantsFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class GiantsClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // model, baby scale, shadow, model scale (the mammoth is built at half size)
        event.registerEntityRenderer(GiantsFauna.WOOLLY_MAMMOTH.get(), c -> new GiantRenderer<>(c, new MammothModel(c.bakeLayer(MammothModel.LAYER)), 0.42F, 2.0F, 2.0F));
        event.registerEntityRenderer(GiantsFauna.WOOLLY_RHINO.get(), c -> new GiantRenderer<>(c, new RhinoModel(c.bakeLayer(RhinoModel.LAYER)), 0.5F, 1.1F, 1.0F));
        event.registerEntityRenderer(GiantsFauna.STEPPE_BISON.get(), c -> new GiantRenderer<>(c, new BisonModel(c.bakeLayer(BisonModel.LAYER)), 0.5F, 1.0F, 1.0F));
        event.registerEntityRenderer(GiantsFauna.MUSK_OX.get(), c -> new GiantRenderer<>(c, new MuskOxModel(c.bakeLayer(MuskOxModel.LAYER)), 0.5F, 0.9F, 1.0F));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MammothModel.LAYER, MammothModel::createBodyLayer);
        event.registerLayerDefinition(RhinoModel.LAYER, RhinoModel::createBodyLayer);
        event.registerLayerDefinition(BisonModel.LAYER, BisonModel::createBodyLayer);
        event.registerLayerDefinition(MuskOxModel.LAYER, MuskOxModel::createBodyLayer);
    }

    private GiantsClient() {}
}
