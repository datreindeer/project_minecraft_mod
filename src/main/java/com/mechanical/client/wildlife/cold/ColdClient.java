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
import com.mojang.blaze3d.vertex.PoseStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renderers for the cold fauna (ColdFauna). The polar bear is the brown bear's model a little bigger with its own
 * texture (cubs stay cub-sized); the snow leopard is the leopard's model with its own texture; the arctic fox has its own.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ColdClient {
    private static final float POLAR_BEAR_SCALE = 1.1F;

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
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ArcticFoxModel.LAYER, ArcticFoxModel::createBodyLayer);
    }

    private ColdClient() {}
}
