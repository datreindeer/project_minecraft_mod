package com.mechanical.client.wildlife.desert;

import com.google.common.reflect.TypeToken;
import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.StuckArrowLayer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.client.wildlife.forestbirds.ForestBirdModel;
import com.mechanical.client.wildlife.forestbirds.ForestBirdRenderer;
import com.mechanical.client.wildlife.meadow.SnakeModel;
import com.mechanical.client.wildlife.warm.LeopardModel;
import com.mechanical.client.wildlife.warm.TameOnagerRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.desert.DesertFauna;
import com.mechanical.wildlife.desert.Rattlesnake;
import com.mechanical.wildlife.desert.SandViper;
import com.mechanical.wildlife.desert.TameFennec;
import com.mechanical.wildlife.desert.TameCamel;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Renderers, model layers and render-state extras for the desert fauna (DesertFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DesertFaunaClient {
    /** Sand viper under the sand: drawn this far (blocks) down, so only the top of the coil and the head show. */
    private static final float BURIED_DEPTH = 0.16F;
    private static final ContextKey<Boolean> BURIED = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "sand_viper_buried"));
    /** The puma is the leopard's build, drawn a little bigger; the condor the vulture's, much bigger. */
    private static final float PUMA_SCALE = 1.12F, CONDOR_SCALE = 1.4F, CAMEL_CALF = 0.55F;

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DesertFauna.DESERT_SCORPION.get(), c -> {
            ScorpionModel model = new ScorpionModel(c.bakeLayer(ScorpionModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 1.0F, 0.35F);
        });
        event.registerEntityRenderer(DesertFauna.FENNEC_FOX.get(), c -> {
            FennecModel model = new FennecModel(c.bakeLayer(FennecModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.3F);
        });
        event.registerEntityRenderer(DesertFauna.TAME_FENNEC.get(), TameFennecRenderer::new);
        event.registerEntityRenderer(DesertFauna.WILD_CAMEL.get(), c -> {
            CamelModel model = new CamelModel(c.bakeLayer(CamelModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, CAMEL_CALF, 0.9F);
        });
        event.registerEntityRenderer(DesertFauna.TAME_CAMEL.get(), c -> {
            CamelModel model = new CamelModel(c.bakeLayer(CamelModel.LAYER));
            return new TameOnagerRenderer(c, model, model, 0.9F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    if (state.isBaby) poseStack.scale(CAMEL_CALF, CAMEL_CALF, CAMEL_CALF);
                }
            };
        });
        event.registerEntityRenderer(DesertFauna.SAND_VIPER.get(), c -> {
            SnakeModel model = new SnakeModel(c.bakeLayer(SnakeModel.LAYER));
            return new WildAnimalRenderer<SandViper>(c, model, model, 1.0F, 0.2F) {
                @Override
                protected void setupRotations(WildAnimalRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
                    super.setupRotations(state, poseStack, bodyRot, entityScale);
                    if (Boolean.TRUE.equals(state.getRenderData(BURIED))) poseStack.translate(0.0F, -BURIED_DEPTH, 0.0F);
                }

                @Override
                protected float getShadowRadius(WildAnimalRenderState state) {
                    return Boolean.TRUE.equals(state.getRenderData(BURIED)) ? 0.0F : super.getShadowRadius(state);
                }
            };
        });
        event.registerEntityRenderer(DesertFauna.RATTLESNAKE.get(), c -> {
            SnakeModel model = new SnakeModel(c.bakeLayer(SnakeModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 1.0F, 0.2F);
        });
        event.registerEntityRenderer(DesertFauna.PUMA.get(), c -> {
            LeopardModel model = new LeopardModel(c.bakeLayer(LeopardModel.LAYER));
            return new WildAnimalRenderer<WildAnimal>(c, model, model, 0.6F, 0.75F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(PUMA_SCALE, PUMA_SCALE, PUMA_SCALE);
                }
            };
        });
        event.registerEntityRenderer(DesertFauna.BIGHORN_SHEEP.get(), c -> {
            BighornModel model = new BighornModel(c.bakeLayer(BighornModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.6F);
        });
        event.registerEntityRenderer(DesertFauna.CONDOR.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.VULTURE), 0.7F, 0.45F, 0.5F), 0.7F) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(CONDOR_SCALE, CONDOR_SCALE, CONDOR_SCALE);
            }
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ScorpionModel.LAYER, ScorpionModel::createBodyLayer);
        event.registerLayerDefinition(FennecModel.LAYER, FennecModel::createBodyLayer);
        event.registerLayerDefinition(CamelModel.LAYER, CamelModel::createBodyLayer);
        event.registerLayerDefinition(BighornModel.LAYER, BighornModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof SandViper viper && viper.isBuried()) state.setRenderData(BURIED, true);
            else if (animal instanceof Rattlesnake snake && snake.isRattling()) state.setRenderData(SnakeModel.RATTLING, true);
        });
    }

    /** The tame fennec: the wild fennec's model and coat; sitting it curls up (the dozing pose). */
    static class TameFennecRenderer extends MobRenderer<TameFennec, WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
        TameFennecRenderer(EntityRendererProvider.Context context) {
            super(context, new FennecModel(context.bakeLayer(FennecModel.LAYER)), 0.3F);
            addLayer(new StuckArrowLayer(this, context));
        }

        @Override
        public WildAnimalRenderState createRenderState() {
            return new WildAnimalRenderState();
        }

        @Override
        public void extractRenderState(TameFennec fennec, WildAnimalRenderState state, float partialTicks) {
            super.extractRenderState(fennec, state, partialTicks);
            state.texture = fennec.texture();
            state.dozing = fennec.isInSittingPose();
            state.arrowCount = fennec.getArrowCount();
            state.entityId = fennec.getId();
        }

        @Override
        public Identifier getTextureLocation(WildAnimalRenderState state) {
            return state.texture;
        }
    }

    private DesertFaunaClient() {}
}
