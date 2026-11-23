package com.mechanical.client.wildlife.temperate;

import com.mechanical.Mechanical;
import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.BirdModel;
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
 * Renderers and model layers for the temperate fauna (TemperateFauna). Most borrow a related animal's model with their own
 * texture: the wisent the steppe bison's, the pond turtle the tortoise's (smaller), the water vole the field vole's
 * (bigger), the tawny owl the eagle-owl's (smaller, its ear tufts left blank in the texture), the sea eagle the golden
 * eagle's (bigger), the bittern the heron's (squatter), and the small birds (woodpecker, skylark, lapwing) the rock
 * dove's at small sizes. The stag beetle and the eel have their own models.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TemperateClient {
    public static final ModelLayerLocation WOODPECKER = layer("woodpecker"), SKYLARK = layer("skylark"), LAPWING = layer("lapwing");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, name), "main");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TemperateFauna.WISENT.get(), c -> new GiantRenderer<>(c, new BisonModel(c.bakeLayer(BisonModel.LAYER)), 0.5F, 1.0F, 0.95F));
        event.registerEntityRenderer(TemperateFauna.POND_TURTLE.get(), c -> {
            TortoiseModel model = new TortoiseModel(c.bakeLayer(TortoiseModel.LAYER));
            return new WarmAnimalRenderer<PondTurtle>(c, model, model, 0.5F, 0.3F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(0.62F, 0.55F, 0.62F); // flatter and smaller than the tortoise
                }
            };
        });
        event.registerEntityRenderer(TemperateFauna.WATER_VOLE.get(), c -> {
            FieldVoleModel model = new FieldVoleModel(c.bakeLayer(FieldVoleModel.LAYER));
            return new WildAnimalRenderer<WaterVole>(c, model, model, 0.6F, 0.25F) {
                @Override
                protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(1.45F, 1.45F, 1.45F);
                }
            };
        });
        event.registerEntityRenderer(TemperateFauna.STAG_BEETLE.get(), c -> {
            StagBeetleModel model = new StagBeetleModel(c.bakeLayer(StagBeetleModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 1.0F, 0.2F);
        });
        event.registerEntityRenderer(TemperateFauna.TAWNY_OWL.get(), c -> scaledForest(c, new ForestBirdModel(c.bakeLayer(ForestBirdModel.OWL), 0.0F, 0.75F, 0.9F), 0.25F, 0.85F));
        event.registerEntityRenderer(TemperateFauna.SEA_EAGLE.get(), c -> scaledForest(c, new ForestBirdModel(c.bakeLayer(ForestBirdModel.GOLDEN_EAGLE), 0.4F, 0.5F, 0.45F), 0.5F, 1.15F));
        event.registerEntityRenderer(TemperateFauna.BITTERN.get(), c -> new WaterfowlRenderer(c, new WaderModel(c.bakeLayer(WaderModel.GREY_HERON), false, 0.75F), 0.3F, 0.65F) {
            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(0.85F, 0.72F, 0.85F); // a squat little heron
            }
        });
        event.registerEntityRenderer(TemperateFauna.WOODPECKER.get(), c -> scaledBird(c, WOODPECKER, 0.82F));
        event.registerEntityRenderer(TemperateFauna.SKYLARK.get(), c -> scaledBird(c, SKYLARK, 0.7F));
        event.registerEntityRenderer(TemperateFauna.LAPWING.get(), c -> scaledBird(c, LAPWING, 0.85F));
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

    private static BirdRenderer scaledBird(EntityRendererProvider.Context c, ModelLayerLocation layer, float size) {
        return new BirdRenderer(c, layer, 0.4F, 0.9F, 0.2F) {
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
        event.registerLayerDefinition(WOODPECKER, BirdModel::createRockDoveLayer);
        event.registerLayerDefinition(SKYLARK, BirdModel::createRockDoveLayer);
        event.registerLayerDefinition(LAPWING, BirdModel::createRockDoveLayer);
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
