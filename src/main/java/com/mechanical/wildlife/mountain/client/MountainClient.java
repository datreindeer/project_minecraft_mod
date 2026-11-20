package com.mechanical.wildlife.mountain.client;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.BearCubModel;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.mountain.CaveBear;
import com.mechanical.wildlife.mountain.Marmot;
import com.mechanical.wildlife.mountain.MountainFauna;
import com.mojang.blaze3d.vertex.PoseStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers for the mountain fauna (MountainFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class MountainClient {
    /** The cave bear model is the brown bear's build, drawn this much bigger. */
    private static final float CAVE_BEAR_SCALE = 1.2F;

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // adult model, baby model, extra baby scale, shadow; the young are the adult model scaled down
        event.registerEntityRenderer(MountainFauna.IBEX.get(), c -> {
            IbexModel model = new IbexModel(c.bakeLayer(IbexModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.6F);
        });
        event.registerEntityRenderer(MountainFauna.CHAMOIS.get(), c -> {
            ChamoisModel model = new ChamoisModel(c.bakeLayer(ChamoisModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.5F);
        });
        event.registerEntityRenderer(MountainFauna.SAIGA.get(), c -> {
            SaigaModel model = new SaigaModel(c.bakeLayer(SaigaModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.55F);
        });
        // down its burrow a marmot is invisible: no shadow either
        event.registerEntityRenderer(MountainFauna.MARMOT.get(), c -> {
            MarmotModel model = new MarmotModel(c.bakeLayer(MarmotModel.LAYER));
            return new WildAnimalRenderer<Marmot>(c, model, model, 0.6F, 0.3F) {
                @Override
                protected float getShadowRadius(WildAnimalRenderState state) {
                    return state.isInvisible ? 0.0F : super.getShadowRadius(state);
                }
            };
        });
        event.registerEntityRenderer(MountainFauna.CAVE_BEAR.get(), c -> new WildAnimalRenderer<CaveBear>(c,
                new CaveBearModel(c.bakeLayer(CaveBearModel.LAYER)), new BearCubModel(c.bakeLayer(BearCubModel.LAYER)), 1.0F / CAVE_BEAR_SCALE, 1.3F) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(CAVE_BEAR_SCALE, CAVE_BEAR_SCALE, CAVE_BEAR_SCALE); // cubs: babyScale cancels it, so they match brown bear cubs
            }
        });
        event.registerEntityRenderer(MountainFauna.CAVE_HYENA.get(), c -> {
            CaveHyenaModel model = new CaveHyenaModel(c.bakeLayer(CaveHyenaModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.6F);
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(IbexModel.LAYER, IbexModel::createBodyLayer);
        event.registerLayerDefinition(ChamoisModel.LAYER, ChamoisModel::createBodyLayer);
        event.registerLayerDefinition(SaigaModel.LAYER, SaigaModel::createBodyLayer);
        event.registerLayerDefinition(MarmotModel.LAYER, MarmotModel::createBodyLayer);
        event.registerLayerDefinition(CaveBearModel.LAYER, CaveBearModel::createBodyLayer);
        event.registerLayerDefinition(CaveHyenaModel.LAYER, CaveHyenaModel::createBodyLayer);
    }

    private MountainClient() {}
}
