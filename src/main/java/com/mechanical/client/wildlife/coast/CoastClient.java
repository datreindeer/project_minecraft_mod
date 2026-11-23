package com.mechanical.client.wildlife.coast;

import com.mechanical.Mechanical;
import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.coast.CoastFauna;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers for the coast fauna (Seal, Walrus, Pike, Sturgeon). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class CoastClient {
    /** CarcassRenderer lifts a lying body by this (its flank height guess, from the hitbox width); see {@link #carcassShift}. */
    private static float carcassLift(float width) {
        return Math.clamp(width * 0.42F, 0.1F, 0.38F);
    }

    /**
     * Lying on its side the model's +x points up (CarcassRenderer turns it 90 degrees about z), so moving along x by the
     * difference between the real half-thickness and CarcassRenderer's lift rests the flank on the ground.
     */
    private static float carcassShift(float width, float halfThickness) {
        return halfThickness - carcassLift(width);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // seal: half its 11 px wide body (flank bulges), at 0.85x; walrus: half its 18 px wide sagging body, at 1.5x
        event.registerEntityRenderer(CoastFauna.SEAL.get(), c -> {
            SealModel model = new SealModel(c.bakeLayer(SealModel.LAYER));
            return new ScaledRenderer<>(c, model, 0.55F, 0.6F, 0.85F, carcassShift(1.0F, 5.5F * 0.85F / 16));
        });
        event.registerEntityRenderer(CoastFauna.WALRUS.get(), c -> {
            WalrusModel model = new WalrusModel(c.bakeLayer(WalrusModel.LAYER));
            return new ScaledRenderer<>(c, model, 0.5F, 1.3F, 1.5F, carcassShift(2.0F, 9 * 1.5F / 16));
        });
        event.registerEntityRenderer(CoastFauna.PIKE.get(), c -> new FishRenderer<>(c, new PikeModel(c.bakeLayer(PikeModel.LAYER)),
                Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/pike.png"), 0.3F, 0.8F, 0.0F));
        event.registerEntityRenderer(CoastFauna.STURGEON.get(), c -> new FishRenderer<>(c, new SturgeonModel(c.bakeLayer(SturgeonModel.LAYER)),
                Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/sturgeon.png"), 0.6F, 0.85F, carcassShift(1.3F, 3 * 0.85F / 16)));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SealModel.LAYER, SealModel::createBodyLayer);
        event.registerLayerDefinition(WalrusModel.LAYER, WalrusModel::createBodyLayer);
        event.registerLayerDefinition(PikeModel.LAYER, PikeModel::createBodyLayer);
        event.registerLayerDefinition(SturgeonModel.LAYER, SturgeonModel::createBodyLayer);
    }

    /** The wildlife renderer with a model scale and the carcass resting height fixed for a body thicker or thinner than usual. */
    static class ScaledRenderer<T extends WildAnimal> extends WildAnimalRenderer<T> {
        private final float scale, carcassShift;

        ScaledRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> model, float babyScale, float shadow,
                       float scale, float carcassShift) {
            super(context, model, model, babyScale, shadow);
            this.scale = scale;
            this.carcassShift = carcassShift;
        }

        @Override
        protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
            super.scale(state, poseStack);
            poseStack.scale(scale, scale, scale);
        }

        @Override
        protected void setupRotations(WildAnimalRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            if (CarcassRenderer.isCarcass(state)) poseStack.translate(carcassShift, 0.0F, 0.0F);
        }
    }

    /** Fish (also the deep fauna's cave fish and olm): a fixed texture, the body swaying as it swims, flopping on its side out of the water (like vanilla fish). */
    public static class FishRenderer<T extends Mob> extends MobRenderer<T, LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {
        private final Identifier texture;
        private final float scale, carcassShift;

        public FishRenderer(EntityRendererProvider.Context context, EntityModel<LivingEntityRenderState> model, Identifier texture, float shadow,
                     float scale, float carcassShift) {
            super(context, model, shadow);
            this.texture = texture;
            this.scale = scale;
            this.carcassShift = carcassShift;
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return texture;
        }

        @Override
        protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
            super.scale(state, poseStack);
            poseStack.scale(scale, scale, scale);
        }

        @Override
        protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            if (CarcassRenderer.isCarcass(state)) {
                poseStack.translate(carcassShift, 0.0F, 0.0F);
                return;
            }
            if (CarcassRenderer.isHung(state)) return;
            boolean wet = state.isInWater;
            poseStack.rotateDegrees(Axis.YP, (wet ? 1.0F : 1.3F) * 4.3F * Mth.sin((wet ? 0.6F : 1.0F) * 0.6F * state.ageInTicks));
            if (!wet) {
                poseStack.translate(0.2F, 0.1F, 0.0F);
                poseStack.rotateDegrees(Axis.ZP, 90.0F);
            }
        }
    }

    private CoastClient() {}
}
