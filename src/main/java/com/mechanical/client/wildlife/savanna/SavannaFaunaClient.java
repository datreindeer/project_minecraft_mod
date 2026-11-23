package com.mechanical.client.wildlife.savanna;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.mountain.client.CaveHyenaModel;
import com.mechanical.wildlife.savanna.SavannaFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers of the savanna fauna (wildlife/savanna/SavannaFauna). The spotted hyena wears the cave hyena's model. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SavannaFaunaClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // adult model, baby model, extra baby scale, shadow
        event.registerEntityRenderer(SavannaFauna.SPOTTED_HYENA.get(), c -> {
            CaveHyenaModel model = new CaveHyenaModel(c.bakeLayer(CaveHyenaModel.LAYER));
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
            OstrichModel model = new OstrichModel(c.bakeLayer(OstrichModel.LAYER));
            return new SavannaAnimalRenderer<>(c, model, model, 0.4F, 0.6F);
        });
        event.registerEntityRenderer(SavannaFauna.AARDVARK.get(), c -> {
            AardvarkModel model = new AardvarkModel(c.bakeLayer(AardvarkModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.5F);
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CheetahModel.LAYER, CheetahModel::createBodyLayer);
        event.registerLayerDefinition(GazelleModel.LAYER, GazelleModel::createBodyLayer);
        event.registerLayerDefinition(OstrichModel.LAYER, OstrichModel::createBodyLayer);
        event.registerLayerDefinition(AardvarkModel.LAYER, AardvarkModel::createBodyLayer);
    }

    private SavannaFaunaClient() {}
}
