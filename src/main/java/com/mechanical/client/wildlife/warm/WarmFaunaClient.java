package com.mechanical.client.wildlife.warm;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.BirdRenderer;
import com.mechanical.client.wildlife.PheasantChickModel;
import com.mechanical.wildlife.warm.WarmFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers of the warm-country fauna (wildlife/warm/WarmFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class WarmFaunaClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // adult model, baby model (built at baby size), extra baby scale, shadow
        event.registerEntityRenderer(WarmFauna.ONAGER.get(), c -> new WarmAnimalRenderer<>(c,
                new OnagerModel(c.bakeLayer(OnagerModel.LAYER)), new OnagerFoalModel(c.bakeLayer(OnagerFoalModel.LAYER)), 1.0F, 0.7F));
        event.registerEntityRenderer(WarmFauna.TAME_ONAGER.get(), TameOnagerRenderer::new);
        event.registerEntityRenderer(WarmFauna.WATER_BUFFALO.get(), c -> new WarmAnimalRenderer<>(c,
                new WaterBuffaloModel(c.bakeLayer(WaterBuffaloModel.LAYER)), new WaterBuffaloCalfModel(c.bakeLayer(WaterBuffaloCalfModel.LAYER)), 1.0F, 1.1F));
        event.registerEntityRenderer(WarmFauna.TAPIR.get(), c -> new WarmAnimalRenderer<>(c,
                new TapirModel(c.bakeLayer(TapirModel.LAYER)), new TapirCalfModel(c.bakeLayer(TapirCalfModel.LAYER)), 1.0F, 0.8F));
        // small ones: the young are the adult scaled down
        event.registerEntityRenderer(WarmFauna.TORTOISE.get(), c -> {
            TortoiseModel model = new TortoiseModel(c.bakeLayer(TortoiseModel.LAYER));
            return new WarmAnimalRenderer<>(c, model, model, 0.45F, 0.5F);
        });
        event.registerEntityRenderer(WarmFauna.DESERT_LIZARD.get(), c -> {
            DesertLizardModel model = new DesertLizardModel(c.bakeLayer(DesertLizardModel.LAYER));
            return new WarmAnimalRenderer<>(c, model, model, 0.5F, 0.25F);
        });
        event.registerEntityRenderer(WarmFauna.LEOPARD.get(), c -> {
            LeopardModel model = new LeopardModel(c.bakeLayer(LeopardModel.LAYER));
            return new WarmAnimalRenderer<>(c, model, model, 0.6F, 0.7F);
        });
        // birds: hornbill flocks (layer, flight neck reach, wing beat, shadow); fowl and peafowl like the pheasant
        event.registerEntityRenderer(WarmFauna.HORNBILL.get(), c -> new BirdRenderer(c, HornbillLayer.LAYER, 0.5F, 0.7F, 0.35F));
        event.registerEntityRenderer(WarmFauna.JUNGLE_FOWL.get(), c -> new WarmAnimalRenderer<>(c,
                new JungleFowlModel(c.bakeLayer(JungleFowlModel.LAYER)), new PheasantChickModel(c.bakeLayer(PheasantChickModel.LAYER)), 1.0F, 0.3F));
        event.registerEntityRenderer(WarmFauna.PEAFOWL.get(), c -> {
            PeafowlModel model = new PeafowlModel(c.bakeLayer(PeafowlModel.LAYER));
            return new WarmAnimalRenderer<>(c, model, model, 1.0F, 0.4F);
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OnagerModel.LAYER, OnagerModel::createBodyLayer);
        event.registerLayerDefinition(OnagerFoalModel.LAYER, OnagerFoalModel::createBodyLayer);
        event.registerLayerDefinition(WaterBuffaloModel.LAYER, WaterBuffaloModel::createBodyLayer);
        event.registerLayerDefinition(WaterBuffaloCalfModel.LAYER, WaterBuffaloCalfModel::createBodyLayer);
        event.registerLayerDefinition(TapirModel.LAYER, TapirModel::createBodyLayer);
        event.registerLayerDefinition(TapirCalfModel.LAYER, TapirCalfModel::createBodyLayer);
        event.registerLayerDefinition(TortoiseModel.LAYER, TortoiseModel::createBodyLayer);
        event.registerLayerDefinition(DesertLizardModel.LAYER, DesertLizardModel::createBodyLayer);
        event.registerLayerDefinition(LeopardModel.LAYER, LeopardModel::createBodyLayer);
        event.registerLayerDefinition(HornbillLayer.LAYER, HornbillLayer::create);
        event.registerLayerDefinition(JungleFowlModel.LAYER, JungleFowlModel::createBodyLayer);
        event.registerLayerDefinition(PeafowlModel.LAYER, PeafowlModel::createBodyLayer);
    }

    private WarmFaunaClient() {}
}
