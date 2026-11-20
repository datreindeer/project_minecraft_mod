package com.mechanical.client.wildlife.forestbirds;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.forestbirds.ForestBirdsFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers of the forest birds (ForestBirdsFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ForestBirdsClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // model: layer, how far the neck reaches forward in flight, wing-beat amplitude, upright sitting tilt; then the shadow
        event.registerEntityRenderer(ForestBirdsFauna.CAPERCAILLIE.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.CAPERCAILLIE), 0.4F, 0.9F, 0.0F), 0.4F));
        event.registerEntityRenderer(ForestBirdsFauna.BUSTARD.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.GREAT_BUSTARD), 1.2F, 0.7F, 0.0F), 0.45F));
        event.registerEntityRenderer(ForestBirdsFauna.OWL.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.OWL), 0.0F, 0.75F, 0.9F), 0.3F));
        event.registerEntityRenderer(ForestBirdsFauna.RAVEN.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.RAVEN), 0.5F, 0.8F, 0.25F), 0.3F));
        event.registerEntityRenderer(ForestBirdsFauna.GOLDEN_EAGLE.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.GOLDEN_EAGLE), 0.4F, 0.55F, 0.45F), 0.45F));
        event.registerEntityRenderer(ForestBirdsFauna.VULTURE.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.VULTURE), 0.7F, 0.45F, 0.5F), 0.5F));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ForestBirdModel.CAPERCAILLIE, ForestBirdModel::createCapercaillieLayer);
        event.registerLayerDefinition(ForestBirdModel.GREAT_BUSTARD, ForestBirdModel::createGreatBustardLayer);
        event.registerLayerDefinition(ForestBirdModel.OWL, ForestBirdModel::createOwlLayer);
        event.registerLayerDefinition(ForestBirdModel.RAVEN, ForestBirdModel::createRavenLayer);
        event.registerLayerDefinition(ForestBirdModel.GOLDEN_EAGLE, ForestBirdModel::createGoldenEagleLayer);
        event.registerLayerDefinition(ForestBirdModel.VULTURE, ForestBirdModel::createVultureLayer);
    }

    private ForestBirdsClient() {}
}
