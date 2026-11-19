package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.Watercraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

/** Log Raft (wobbles on the water) and Dugout Canoe (water kept out of the hull; paddles shown only when the rider holds a Paddle). */
public class WatercraftRenderer extends AbstractBoatRenderer {
    public static class State extends BoatRenderState {
        boolean paddles = true;
    }

    private final EntityModel<BoatRenderState> model;
    private final Model.Simple waterPatch;
    private final float wobble;

    private WatercraftRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, ModelLayerLocation waterPatch, float wobble) {
        super(context, Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/watercraft/" + layer.model().getPath() + ".png"));
        this.model = new WatercraftModel(context.bakeLayer(layer));
        this.waterPatch = waterPatch == null ? null : new Model.Simple(context.bakeLayer(waterPatch), t -> RenderTypes.waterMask());
        this.wobble = wobble;
    }

    public static WatercraftRenderer raft(EntityRendererProvider.Context context) {
        return new WatercraftRenderer(context, WatercraftModel.RAFT, null, 2.5F);
    }

    public static WatercraftRenderer canoe(EntityRendererProvider.Context context) {
        return new WatercraftRenderer(context, WatercraftModel.CANOE, WatercraftModel.CANOE_WATER_PATCH, 0.0F);
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((State) state).paddles = !(entity instanceof Watercraft.DugoutCanoe canoe) || canoe.riderHasPaddle();
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (wobble > 0 && !state.isUnderWater) {
            // loose logs rock on every ripple
            float t = state.ageInTicks;
            poseStack.rotateDegrees(Axis.XP, Mth.sin(t * 0.13F) * wobble);
            poseStack.rotateDegrees(Axis.ZP, Mth.cos(t * 0.11F) * wobble);
        }
        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        if (waterPatch != null && !state.isUnderWater)
            collector.submitModel(waterPatch, Unit.INSTANCE, poseStack, texture, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
