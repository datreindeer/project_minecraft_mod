package com.mechanical.client.wildlife.jungle;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.meadow.SnakeModel;
import com.mechanical.client.wildlife.warm.DesertLizardModel;
import com.mechanical.client.wildlife.warm.LeopardModel;
import com.mechanical.wildlife.jungle.JungleFauna;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renderers and model layers of the jungle fauna (wildlife/jungle/JungleFauna). The tiger, python and monitor borrow the
 * leopard's, snake's and desert lizard's models (layers registered by their own packages) at a bigger scale with their own
 * textures; the rest have their own models.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class JungleFaunaClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // model, whole-model scale, extra baby scale, shadow
        event.registerEntityRenderer(JungleFauna.TIGER.get(), c -> new JungleRenderer<>(c, new LeopardModel(c.bakeLayer(LeopardModel.LAYER)), 1.3F, 0.6F, 0.7F));
        event.registerEntityRenderer(JungleFauna.PYTHON.get(), c -> new JungleRenderer<>(c, new SnakeModel(c.bakeLayer(SnakeModel.LAYER)), 2.2F, 1.0F, 0.2F));
        event.registerEntityRenderer(JungleFauna.MONITOR_LIZARD.get(), c -> new JungleRenderer<>(c, new DesertLizardModel(c.bakeLayer(DesertLizardModel.LAYER)), 2.2F, 0.5F, 0.25F));
        event.registerEntityRenderer(JungleFauna.ORANGUTAN.get(), c -> new JungleRenderer<>(c, new PrimateModel(c.bakeLayer(PrimateModel.LAYER), false), 1.0F, 0.5F, 0.6F));
        event.registerEntityRenderer(JungleFauna.PROBOSCIS_MONKEY.get(), c -> new JungleRenderer<>(c, new PrimateModel(c.bakeLayer(PrimateModel.LAYER), true), 0.7F, 0.6F, 0.6F));
        event.registerEntityRenderer(JungleFauna.SALTWATER_CROCODILE.get(), c -> new JungleRenderer<>(c, new CrocodileModel(c.bakeLayer(CrocodileModel.LAYER)), 1.25F, 0.5F, 0.9F));
        event.registerEntityRenderer(JungleFauna.POISON_DART_FROG.get(), c -> new JungleRenderer<>(c,
                new SmallJungleModels.DartFrogModel(c.bakeLayer(SmallJungleModels.DartFrogModel.LAYER)), 1.0F, 1.0F, 0.15F));
        event.registerEntityRenderer(JungleFauna.MUDSKIPPER.get(), c -> new JungleRenderer<>(c,
                new SmallJungleModels.MudskipperModel(c.bakeLayer(SmallJungleModels.MudskipperModel.LAYER)), 1.2F, 1.0F, 0.15F));
        event.registerEntityRenderer(JungleFauna.FIDDLER_CRAB.get(), c -> new JungleRenderer<>(c,
                new SmallJungleModels.FiddlerCrabModel(c.bakeLayer(SmallJungleModels.FiddlerCrabModel.LAYER)), 1.2F, 1.0F, 0.2F));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PrimateModel.LAYER, PrimateModel::createBodyLayer);
        event.registerLayerDefinition(CrocodileModel.LAYER, CrocodileModel::createBodyLayer);
        event.registerLayerDefinition(SmallJungleModels.DartFrogModel.LAYER, SmallJungleModels.DartFrogModel::createBodyLayer);
        event.registerLayerDefinition(SmallJungleModels.MudskipperModel.LAYER, SmallJungleModels.MudskipperModel::createBodyLayer);
        event.registerLayerDefinition(SmallJungleModels.FiddlerCrabModel.LAYER, SmallJungleModels.FiddlerCrabModel::createBodyLayer);
    }

    private JungleFaunaClient() {}
}
