package com.mechanical.client.wildlife.temperate;

import com.mechanical.Mechanical;
import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.BirdRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.client.wildlife.forestbirds.ForestBirdModel;
import com.mechanical.client.wildlife.forestbirds.ForestBirdRenderer;
import com.mechanical.client.wildlife.giants.BisonModel;
import com.mechanical.client.wildlife.giants.GiantRenderer;
import com.mechanical.client.wildlife.meadow.FieldVoleModel;
import com.mechanical.client.wildlife.warm.TortoiseModel;
import com.mechanical.client.wildlife.warm.WarmAnimalRenderer;
import com.mechanical.client.wildlife.waterfowl.WaderModel;
import com.mechanical.client.wildlife.waterfowl.WaterfowlRenderer;
import com.mechanical.wildlife.temperate.Eel;
import com.mechanical.wildlife.temperate.PondTurtle;
import com.mechanical.wildlife.temperate.TemperateFauna;
import com.mechanical.wildlife.temperate.WaterVole;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renderers and model layers for the temperate fauna (TemperateFauna). Every animal has its own geometry and skin
 * ({@link TemperateGeometry}, scripts/gen_temperate_models.py), built on the part names of a related animal's model class
 * so that class animates it: the wisent on BisonModel, the pond turtle on TortoiseModel, the water vole on FieldVoleModel,
 * the tawny owl and the sea eagle on ForestBirdModel, the bittern on WaderModel, the small birds (woodpecker, skylark,
 * lapwing) on BirdModel. The stag beetle and the eel have their own model classes.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TemperateClient {
    public static final ModelLayerLocation WOODPECKER = layer("woodpecker"), SKYLARK = layer("skylark"), LAPWING = layer("lapwing"),
            WISENT = layer("wisent"), POND_TURTLE = layer("pond_turtle"), WATER_VOLE = layer("water_vole"), TAWNY_OWL = layer("tawny_owl"),
            SEA_EAGLE = layer("sea_eagle"), BITTERN = layer("bittern");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, name), "main");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TemperateFauna.WISENT.get(), c -> new GiantRenderer<>(c, new BisonModel(c.bakeLayer(WISENT)), 0.5F, 1.1F, 1.0F));
        event.registerEntityRenderer(TemperateFauna.POND_TURTLE.get(), c -> {
            TortoiseModel model = new TortoiseModel(c.bakeLayer(POND_TURTLE));
            return new WarmAnimalRenderer<PondTurtle>(c, model, model, 0.5F, 0.3F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(0.7F, 0.7F, 0.7F);
                }
            };
        });
        event.registerEntityRenderer(TemperateFauna.WATER_VOLE.get(), c -> {
            FieldVoleModel model = new FieldVoleModel(c.bakeLayer(WATER_VOLE));
            return new WildAnimalRenderer<WaterVole>(c, model, model, 0.6F, 0.25F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(0.75F, 0.75F, 0.75F);
                }
            };
        });
        event.registerEntityRenderer(TemperateFauna.STAG_BEETLE.get(), c -> {
            StagBeetleModel model = new StagBeetleModel(c.bakeLayer(StagBeetleModel.LAYER));
            return new WildAnimalRenderer<com.mechanical.wildlife.temperate.StagBeetle>(c, model, model, 1.0F, 0.2F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(0.7F, 0.7F, 0.7F);
                }
            };
        });
        event.registerEntityRenderer(TemperateFauna.TAWNY_OWL.get(), c -> scaledForest(c, new ForestBirdModel(c.bakeLayer(TAWNY_OWL), 0.0F, 0.75F, 1.1F), 0.25F, 0.85F));
        event.registerEntityRenderer(TemperateFauna.SEA_EAGLE.get(), c -> scaledForest(c, new ForestBirdModel(c.bakeLayer(SEA_EAGLE), 0.4F, 0.5F, 0.45F), 0.5F, 1.05F));
        event.registerEntityRenderer(TemperateFauna.BITTERN.get(), c -> new WaterfowlRenderer(c, new WaderModel(c.bakeLayer(BITTERN), false, 0.75F), 0.3F, 0.65F) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(0.8F, 0.8F, 0.8F);
            }
        });
        event.registerEntityRenderer(TemperateFauna.WOODPECKER.get(), c -> new BirdRenderer(c, WOODPECKER, 0.4F, 0.9F, 0.2F));
        event.registerEntityRenderer(TemperateFauna.SKYLARK.get(), c -> new BirdRenderer(c, SKYLARK, 0.4F, 0.9F, 0.2F));
        event.registerEntityRenderer(TemperateFauna.LAPWING.get(), c -> new BirdRenderer(c, LAPWING, 0.4F, 0.9F, 0.2F));
        event.registerEntityRenderer(TemperateFauna.EEL.get(), EelRenderer::new);
    }

    private static ForestBirdRenderer scaledForest(EntityRendererProvider.Context c, ForestBirdModel model, float shadow, float size) {
        return new ForestBirdRenderer(c, model, shadow) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(size, size, size);
            }
        };
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StagBeetleModel.LAYER, StagBeetleModel::createBodyLayer);
        event.registerLayerDefinition(EelModel.LAYER, EelModel::createBodyLayer);
        event.registerLayerDefinition(WOODPECKER, TemperateGeometry::woodpecker);
        event.registerLayerDefinition(SKYLARK, TemperateGeometry::skylark);
        event.registerLayerDefinition(LAPWING, TemperateGeometry::lapwing);
        event.registerLayerDefinition(WISENT, TemperateGeometry::wisent);
        event.registerLayerDefinition(POND_TURTLE, TemperateGeometry::pondTurtle);
        event.registerLayerDefinition(WATER_VOLE, TemperateGeometry::waterVole);
        event.registerLayerDefinition(TAWNY_OWL, TemperateGeometry::tawnyOwl);
        event.registerLayerDefinition(SEA_EAGLE, TemperateGeometry::seaEagle);
        event.registerLayerDefinition(BITTERN, TemperateGeometry::bittern);
    }

    /** The eel: its texture, swaying as it swims, writhing on its side out of the water (like vanilla fish). */
    static class EelRenderer extends MobRenderer<Eel, LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/temperate/eel.png");

        EelRenderer(EntityRendererProvider.Context context) {
            super(context, new EelModel(context.bakeLayer(EelModel.LAYER)), 0.2F);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return TEXTURE;
        }

        @Override
        protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
            if (!state.isInWater) {
                poseStack.translate(0.1F, 0.1F, 0.0F);
                poseStack.rotateDegrees(Axis.ZP, 90.0F);
            } else {
                poseStack.rotateDegrees(Axis.YP, 3.0F * Mth.sin(0.4F * state.ageInTicks));
            }
        }
    }

    private TemperateClient() {}
}
