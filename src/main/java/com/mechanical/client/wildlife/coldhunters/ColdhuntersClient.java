package com.mechanical.client.wildlife.coldhunters;

import com.google.common.reflect.TypeToken;
import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.coldhunters.ColdhuntersFauna;
import com.mechanical.wildlife.coldhunters.PineMarten;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Renderers and model layers for the cold hunters (ColdhuntersFauna). Young are the adult model scaled down. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ColdhuntersClient {
    /** Pine marten clinging to a trunk: 1 climbing up (head up), 2 coming down head first. */
    static final ContextKey<Integer> CLIMB = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "pine_marten_climb"));

    /** Ermine, lemming and pine marten are built at twice their size (HuntGeometry) and drawn at half, for finer detail. */
    private static final float HALF = 0.5F;

    private static <T extends WildAnimal> WildAnimalRenderer<T> half(net.minecraft.client.renderer.entity.EntityRendererProvider.Context c,
            net.minecraft.client.model.EntityModel<WildAnimalRenderState> model, float babyScale, float shadow) {
        return new WildAnimalRenderer<T>(c, model, model, babyScale, shadow) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack pose) {
                super.scale(state, pose);
                pose.scale(HALF, HALF, HALF);
            }
        };
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ColdhuntersFauna.WOLVERINE.get(), c -> {
            WolverineModel model = new WolverineModel(c.bakeLayer(WolverineModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.6F);
        });
        event.registerEntityRenderer(ColdhuntersFauna.LYNX.get(), c -> {
            LynxModel model = new LynxModel(c.bakeLayer(LynxModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.6F);
        });
        event.registerEntityRenderer(ColdhuntersFauna.ERMINE.get(), c -> {
            ErmineModel model = new ErmineModel(c.bakeLayer(ErmineModel.LAYER));
            return half(c, model, 0.6F, 0.2F);
        });
        event.registerEntityRenderer(ColdhuntersFauna.LEMMING.get(), c -> {
            LemmingModel model = new LemmingModel(c.bakeLayer(LemmingModel.LAYER));
            return half(c, model, 0.6F, 0.2F);
        });
        event.registerEntityRenderer(ColdhuntersFauna.PINE_MARTEN.get(), c -> {
            PineMartenModel model = new PineMartenModel(c.bakeLayer(PineMartenModel.LAYER));
            return new WildAnimalRenderer<PineMarten>(c, model, model, 0.6F, 0.25F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack pose) {
                    super.scale(state, pose);
                    pose.scale(HALF, HALF, HALF); // built at twice its size
                }

                @Override
                protected void setupRotations(WildAnimalRenderState state, PoseStack pose, float bodyRot, float scale) {
                    super.setupRotations(state, pose, bodyRot, scale);
                    Integer climb = state.getRenderData(CLIMB);
                    if (climb == null || climb == 0) return;
                    // stood on end about the body's middle: belly to the bark, head up (or down, coming down head first)
                    float mid = state.boundingBoxHeight / 2 / scale;
                    pose.translate(0.0F, mid, 0.0F);
                    pose.rotateDegrees(Axis.XP, climb == 2 ? 90.0F : -90.0F);
                    pose.translate(0.0F, -mid, 0.0F);
                }
            };
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WolverineModel.LAYER, WolverineModel::createBodyLayer);
        event.registerLayerDefinition(LynxModel.LAYER, LynxModel::createBodyLayer);
        event.registerLayerDefinition(ErmineModel.LAYER, ErmineModel::createBodyLayer);
        event.registerLayerDefinition(LemmingModel.LAYER, LemmingModel::createBodyLayer);
        event.registerLayerDefinition(PineMartenModel.LAYER, PineMartenModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof PineMarten marten && marten.isClimbing()) state.setRenderData(CLIMB, marten.isDescending() ? 2 : 1);
        });
    }

    private ColdhuntersClient() {}
}
