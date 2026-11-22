package com.mechanical.client.wildlife.cold;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.BearCubModel;
import com.mechanical.client.wildlife.BrownBearModel;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.client.wildlife.warm.LeopardModel;
import com.mechanical.client.wildlife.warm.WarmAnimalRenderer;
import com.mechanical.wildlife.cold.ColdFauna;
import com.mechanical.wildlife.cold.PolarBear;
import com.mechanical.wildlife.cold.SiberianTiger;
import com.mechanical.client.wildlife.coldhunters.LemmingModel;
import com.mechanical.client.wildlife.forestbirds.ForestBirdModel;
import com.mechanical.client.wildlife.forestbirds.ForestBirdRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renderers for the cold fauna (ColdFauna). The polar bear is the brown bear's model a little bigger with its own
 * texture (cubs stay cub-sized); the snow leopard and (a third bigger) the Siberian tiger are the leopard's model; the
 * snowy owl, black grouse and lammergeier the eagle-owl's, capercaillie's (smaller) and golden eagle's; the pika the
 * lemming's - each with its own texture. The arctic fox has its own model.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ColdClient {
    private static final float POLAR_BEAR_SCALE = 1.1F, TIGER_SCALE = 1.35F, GROUSE_SCALE = 0.75F;

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ColdFauna.POLAR_BEAR.get(), c -> new WildAnimalRenderer<PolarBear>(c,
                new BrownBearModel(c.bakeLayer(BrownBearModel.LAYER)), new BearCubModel(c.bakeLayer(BearCubModel.LAYER)), 1.0F / POLAR_BEAR_SCALE, 1.1F) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(POLAR_BEAR_SCALE, POLAR_BEAR_SCALE, POLAR_BEAR_SCALE);
            }
        });
        event.registerEntityRenderer(ColdFauna.ARCTIC_FOX.get(), c -> {
            ArcticFoxModel model = new ArcticFoxModel(c.bakeLayer(ArcticFoxModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.35F);
        });
        event.registerEntityRenderer(ColdFauna.SNOW_LEOPARD.get(), c -> {
            LeopardModel model = new LeopardModel(c.bakeLayer(LeopardModel.LAYER));
            return new WarmAnimalRenderer<>(c, model, model, 0.6F, 0.7F);
        });
        event.registerEntityRenderer(ColdFauna.SIBERIAN_TIGER.get(), c -> {
            LeopardModel model = new LeopardModel(c.bakeLayer(LeopardModel.LAYER));
            return new WarmAnimalRenderer<SiberianTiger>(c, model, model, 0.6F, 1.0F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(TIGER_SCALE, TIGER_SCALE, TIGER_SCALE);
                }
            };
        });
        // birds on the forest birds' models (layer, flight neck reach, wing beat, sitting tilt), each with its own texture
        event.registerEntityRenderer(ColdFauna.SNOWY_OWL.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.OWL), 0.0F, 0.75F, 0.9F), 0.3F));
        event.registerEntityRenderer(ColdFauna.BLACK_GROUSE.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.CAPERCAILLIE), 0.4F, 0.9F, 0.0F), 0.3F) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(GROUSE_SCALE, GROUSE_SCALE, GROUSE_SCALE);
            }
        });
        event.registerEntityRenderer(ColdFauna.LAMMERGEIER.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.GOLDEN_EAGLE), 0.4F, 0.55F, 0.45F), 0.5F));
        event.registerEntityRenderer(ColdFauna.PIKA.get(), c -> {
            LemmingModel model = new LemmingModel(c.bakeLayer(LemmingModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.2F);
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ArcticFoxModel.LAYER, ArcticFoxModel::createBodyLayer);
    }

    private ColdClient() {}
}
