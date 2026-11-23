package com.mechanical.client.wildlife.jungle;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.jungle.JungleFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renderers and model layers of the jungle fauna (wildlife/jungle/JungleFauna). Every creature has its own model; the
 * geometry of all of them is generated with their skins by scripts/gen_jungle_models.py (JungleGeometry).
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class JungleFaunaClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // model, whole-model scale, extra baby scale, shadow; the little ones are built at twice their size
        event.registerEntityRenderer(JungleFauna.TIGER.get(), c -> new JungleRenderer<>(c, new TigerModel(c.bakeLayer(TigerModel.LAYER)), 1.0F, 0.6F, 0.9F));
        event.registerEntityRenderer(JungleFauna.PYTHON.get(), c -> new JungleRenderer<>(c, new PythonModel(c.bakeLayer(PythonModel.LAYER)), 1.0F, 1.0F, 0.4F));
        event.registerEntityRenderer(JungleFauna.MONITOR_LIZARD.get(), c -> new JungleRenderer<>(c, new MonitorModel(c.bakeLayer(MonitorModel.LAYER)), 1.0F, 0.5F, 0.45F));
        event.registerEntityRenderer(JungleFauna.ORANGUTAN.get(), c -> new JungleRenderer<>(c,
                new PrimateModel(c.bakeLayer(PrimateModel.ORANGUTAN), 1.1F, 8.0F), 1.0F, 0.5F, 0.6F));
        event.registerEntityRenderer(JungleFauna.PROBOSCIS_MONKEY.get(), c -> new JungleRenderer<>(c,
                new PrimateModel(c.bakeLayer(PrimateModel.PROBOSCIS), 1.2F, 7.0F), 0.8F, 0.6F, 0.5F));
        event.registerEntityRenderer(JungleFauna.SALTWATER_CROCODILE.get(), c -> new JungleRenderer<>(c, new CrocodileModel(c.bakeLayer(CrocodileModel.LAYER)), 1.0F, 0.5F, 1.1F));
        event.registerEntityRenderer(JungleFauna.POISON_DART_FROG.get(), c -> new JungleRenderer<>(c,
                new SmallJungleModels.DartFrogModel(c.bakeLayer(SmallJungleModels.DartFrogModel.LAYER)), 0.5F, 1.0F, 0.3F));
        event.registerEntityRenderer(JungleFauna.MUDSKIPPER.get(), c -> new JungleRenderer<>(c,
                new SmallJungleModels.MudskipperModel(c.bakeLayer(SmallJungleModels.MudskipperModel.LAYER)), 0.5F, 1.0F, 0.3F));
        event.registerEntityRenderer(JungleFauna.FIDDLER_CRAB.get(), c -> new JungleRenderer<>(c,
                new SmallJungleModels.FiddlerCrabModel(c.bakeLayer(SmallJungleModels.FiddlerCrabModel.LAYER)), 0.5F, 1.0F, 0.4F));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TigerModel.LAYER, TigerModel::createBodyLayer);
        event.registerLayerDefinition(PythonModel.LAYER, PythonModel::createBodyLayer);
        event.registerLayerDefinition(MonitorModel.LAYER, MonitorModel::createBodyLayer);
        event.registerLayerDefinition(PrimateModel.ORANGUTAN, JungleGeometry::orangutan);
        event.registerLayerDefinition(PrimateModel.PROBOSCIS, JungleGeometry::proboscis);
        event.registerLayerDefinition(CrocodileModel.LAYER, CrocodileModel::createBodyLayer);
        event.registerLayerDefinition(SmallJungleModels.DartFrogModel.LAYER, SmallJungleModels.DartFrogModel::createBodyLayer);
        event.registerLayerDefinition(SmallJungleModels.MudskipperModel.LAYER, SmallJungleModels.MudskipperModel::createBodyLayer);
        event.registerLayerDefinition(SmallJungleModels.FiddlerCrabModel.LAYER, SmallJungleModels.FiddlerCrabModel::createBodyLayer);
    }

    private JungleFaunaClient() {}
}
