package com.mechanical.client.wildlife.savanna;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.savanna.SavannaFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers of the savanna fauna (wildlife/savanna/SavannaFauna). Models and skins: scripts/gen_savanna_models.py. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SavannaFaunaClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // adult model, baby model, extra baby scale, shadow
        event.registerEntityRenderer(SavannaFauna.SPOTTED_HYENA.get(), c -> {
            SpottedHyenaModel model = new SpottedHyenaModel(c.bakeLayer(SpottedHyenaModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.6F);
        });
        event.registerEntityRenderer(SavannaFauna.CHEETAH.get(), c -> {
            CheetahModel model = new CheetahModel(c.bakeLayer(CheetahModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.55F);
        });
        event.registerEntityRenderer(SavannaFauna.GAZELLE.get(), c -> {
            GazelleModel model = new GazelleModel(c.bakeLayer(GazelleModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.45F);
        });
        event.registerEntityRenderer(SavannaFauna.OSTRICH.get(), c -> {
            return new SavannaAnimalRenderer<>(c, new OstrichModel(c.bakeLayer(OstrichModel.LAYER)),
                    new OstrichModel(c.bakeLayer(OstrichModel.CHICK_LAYER)), 0.42F, 0.6F);
        });
        event.registerEntityRenderer(SavannaFauna.AARDVARK.get(), c -> {
            AardvarkModel model = new AardvarkModel(c.bakeLayer(AardvarkModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.5F);
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SpottedHyenaModel.LAYER, SpottedHyenaModel::createBodyLayer);
        event.registerLayerDefinition(CheetahModel.LAYER, CheetahModel::createBodyLayer);
        event.registerLayerDefinition(GazelleModel.LAYER, GazelleModel::createBodyLayer);
        event.registerLayerDefinition(OstrichModel.LAYER, OstrichModel::createBodyLayer);
        event.registerLayerDefinition(OstrichModel.CHICK_LAYER, OstrichModel::createChickLayer);
        event.registerLayerDefinition(AardvarkModel.LAYER, AardvarkModel::createBodyLayer);
    }

    private SavannaFaunaClient() {}
}
