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
import com.mechanical.wildlife.cold.Pika;
import com.mechanical.wildlife.cold.SiberianTiger;
import com.mechanical.wildlife.cold.SnowLeopard;
import com.mechanical.client.wildlife.forestbirds.ForestBirdModel;
import com.mechanical.client.wildlife.forestbirds.ForestBirdRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renderers for the cold fauna (ColdFauna). Every animal has its own model built by scripts/gen_cold_models.py
 * (ColdGeometry) with the skin painted for it; the animation is the shared one of its body plan: the polar bear and its
 * cub move like the brown bear's (BrownBearModel, BearCubModel), the arctic fox like the other canids (PredatorModel),
 * the snow leopard and the Siberian tiger like the leopard (LeopardModel), the birds like the forest birds
 * (ForestBirdModel); the pika has its own PikaModel, built at twice its size and drawn at half.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ColdClient {
    public static final ModelLayerLocation POLAR_BEAR = layer("polar_bear"), POLAR_BEAR_CUB = layer("polar_bear_cub"),
            SNOW_LEOPARD = layer("snow_leopard"), SIBERIAN_TIGER = layer("siberian_tiger"), SNOWY_OWL = layer("snowy_owl"),
            BLACK_GROUSE = layer("black_grouse"), LAMMERGEIER = layer("lammergeier");
    /** Drawn sizes: the snow leopard is a smaller, lower cat than the leopard model it moves like; the pika is built at 2x. */
    private static final float POLAR_BEAR_SCALE = 1.1F, SNOW_LEOPARD_SCALE = 0.75F, TIGER_SCALE = 1.15F, PIKA_SCALE = 0.5F;

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, name), "main");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ColdFauna.POLAR_BEAR.get(), c -> new WildAnimalRenderer<PolarBear>(c,
                new BrownBearModel(c.bakeLayer(POLAR_BEAR)), new BearCubModel(c.bakeLayer(POLAR_BEAR_CUB)), 1.0F / POLAR_BEAR_SCALE, 1.1F) {
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
            LeopardModel model = new LeopardModel(c.bakeLayer(SNOW_LEOPARD));
            return new WarmAnimalRenderer<SnowLeopard>(c, model, model, 0.6F, 0.55F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(SNOW_LEOPARD_SCALE, SNOW_LEOPARD_SCALE, SNOW_LEOPARD_SCALE);
                }
            };
        });
        event.registerEntityRenderer(ColdFauna.SIBERIAN_TIGER.get(), c -> {
            LeopardModel model = new LeopardModel(c.bakeLayer(SIBERIAN_TIGER));
            return new WarmAnimalRenderer<SiberianTiger>(c, model, model, 0.6F, 1.0F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(TIGER_SCALE, TIGER_SCALE, TIGER_SCALE);
                }
            };
        });
        // birds on the forest birds' animation (layer, flight neck reach, wing beat, sitting tilt)
        event.registerEntityRenderer(ColdFauna.SNOWY_OWL.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(SNOWY_OWL), 0.0F, 0.75F, 1.45F), 0.3F));
        event.registerEntityRenderer(ColdFauna.BLACK_GROUSE.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(BLACK_GROUSE), 0.4F, 0.9F, 0.1F), 0.25F));
        event.registerEntityRenderer(ColdFauna.LAMMERGEIER.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(LAMMERGEIER), 0.4F, 0.55F, 0.5F), 0.5F));
        event.registerEntityRenderer(ColdFauna.PIKA.get(), c -> {
            PikaModel model = new PikaModel(c.bakeLayer(PikaModel.LAYER));
            return new WildAnimalRenderer<Pika>(c, model, model, 0.6F, 0.2F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(PIKA_SCALE, PIKA_SCALE, PIKA_SCALE);
                }
            };
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(POLAR_BEAR, ColdGeometry::polarBear);
        event.registerLayerDefinition(POLAR_BEAR_CUB, ColdGeometry::polarBearCub);
        event.registerLayerDefinition(ArcticFoxModel.LAYER, ColdGeometry::arcticFox);
        event.registerLayerDefinition(SNOW_LEOPARD, ColdGeometry::snowLeopard);
        event.registerLayerDefinition(SIBERIAN_TIGER, ColdGeometry::siberianTiger);
        event.registerLayerDefinition(SNOWY_OWL, ColdGeometry::snowyOwl);
        event.registerLayerDefinition(BLACK_GROUSE, ColdGeometry::blackGrouse);
        event.registerLayerDefinition(LAMMERGEIER, ColdGeometry::lammergeier);
        event.registerLayerDefinition(PikaModel.LAYER, ColdGeometry::pika);
    }

    private ColdClient() {}
}
