package com.mechanical.client.wildlife.underground;

import com.google.common.reflect.TypeToken;
import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.underground.GiantCentipede;
import com.mechanical.wildlife.underground.Lindworm;
import com.mechanical.wildlife.underground.Troll;
import com.mechanical.wildlife.underground.Vittra;
import com.mechanical.wildlife.underground.UndergroundFauna;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Renderer and model layer for the underground fauna (UndergroundFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class UndergroundClient {
    /** GiantCentipede.cling(): 1 up a wall (head up, belly to the rock), 2 upside down on the ceiling. */
    static final ContextKey<Integer> CLING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "giant_centipede_cling"));
    /** Lindworm.pose(): lunging or coiled (absent while it just moves). */
    static final ContextKey<Integer> LINDWORM_POSE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "lindworm_pose"));

    /** The wildlife renderer with the whole model scaled (the folk models are built at human size). */
    private static <T extends WildAnimal> WildAnimalRenderer<T> scaled(net.minecraft.client.renderer.entity.EntityRendererProvider.Context c,
            net.minecraft.client.model.EntityModel<WildAnimalRenderState> model, float scale, float shadow) {
        return new WildAnimalRenderer<T>(c, model, model, 1.0F, shadow) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack pose) {
                super.scale(state, pose);
                pose.scale(scale, scale, scale);
            }
        };
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(UndergroundFauna.GIANT_CENTIPEDE.get(), c -> {
            GiantCentipedeModel model = new GiantCentipedeModel(c.bakeLayer(GiantCentipedeModel.LAYER));
            return new WildAnimalRenderer<GiantCentipede>(c, model, model, 1.0F, 0.4F) {
                @Override
                protected void setupRotations(WildAnimalRenderState state, PoseStack pose, float bodyRot, float scale) {
                    super.setupRotations(state, pose, bodyRot, scale);
                    Integer cling = state.getRenderData(CLING);
                    if (cling == null || cling == GiantCentipede.GROUND) return;
                    float mid = state.boundingBoxHeight / 2 / scale;
                    if (cling == GiantCentipede.CEILING) { // legs up on the rock, back to the floor
                        pose.translate(0.0F, mid, 0.0F);
                        pose.rotateDegrees(Axis.ZP, 180.0F);
                        pose.translate(0.0F, -mid, 0.0F);
                    } else { // stood on end, head up, tail at the feet
                        pose.translate(0.0F, mid + 0.45F, 0.0F);
                        pose.rotateDegrees(Axis.XP, -90.0F);
                        pose.translate(0.0F, -mid, 0.0F);
                    }
                }
            };
        });
    }

    @SubscribeEvent
    static void registerFolk(EntityRenderersEvent.RegisterRenderers event) {
        event.<Vittra>registerEntityRenderer(UndergroundFauna.VITTRA.get(), c -> scaled(c, new FolkModel(c.bakeLayer(FolkModel.VITTRA), 0.1F), 0.75F, 0.4F));
        event.<Troll>registerEntityRenderer(UndergroundFauna.TROLL.get(), c -> scaled(c, new FolkModel(c.bakeLayer(FolkModel.TROLL), 0.45F), 1.45F, 1.0F));
        event.<Lindworm>registerEntityRenderer(UndergroundFauna.LINDWORM.get(), c -> scaled(c, new LindwormModel(c.bakeLayer(LindwormModel.LAYER)), 1.3F, 1.2F));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GiantCentipedeModel.LAYER, GiantCentipedeModel::createBodyLayer);
        event.registerLayerDefinition(FolkModel.VITTRA, FolkModel::createVittraLayer);
        event.registerLayerDefinition(FolkModel.TROLL, FolkModel::createTrollLayer);
        event.registerLayerDefinition(LindwormModel.LAYER, LindwormModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof GiantCentipede c && c.cling() != GiantCentipede.GROUND) state.setRenderData(CLING, c.cling());
            if (animal instanceof Lindworm w && w.pose() != Lindworm.MOVING) state.setRenderData(LINDWORM_POSE, w.pose());
        });
    }

    private UndergroundClient() {}
}
