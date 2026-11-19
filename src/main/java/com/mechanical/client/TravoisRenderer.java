package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * Draws the {@link TravoisModel} turned to the travois' yaw (its poles point the way it faces) and tipped about the rear pole
 * ends to its {@link TravoisEntity#pitch}: up to the puller's hip, or front down on the ground when let go. Loaded carcasses
 * draw themselves (tilted the same way, see CarcassRenderer).
 */
public class TravoisRenderer extends EntityRenderer<TravoisEntity, TravoisRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/travois.png");
    private final Model.Simple model;

    public static class State extends EntityRenderState {
        float yaw, pitch;
    }

    public TravoisRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new Model.Simple(context.bakeLayer(TravoisModel.LAYER), RenderTypes::entityCutout);
        shadowRadius = 0.6F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TravoisEntity travois, State state, float partialTicks) {
        super.extractRenderState(travois, state, partialTicks);
        state.yaw = travois.getViewYRot(partialTicks);
        state.pitch = travois.pitch(partialTicks);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // model -z (the front) to the facing direction; model y points down
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // here the front is -z and y is up; tip about the rear ends, which stay on the ground
        poseStack.translate(0.0F, TravoisModel.PIVOT_Y, TravoisModel.PIVOT_Z);
        poseStack.rotate(Axis.XP.rotationDegrees(state.pitch - TravoisModel.POLE_ANGLE));
        poseStack.translate(0.0F, -TravoisModel.PIVOT_Y, -TravoisModel.PIVOT_Z);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, Unit.INSTANCE, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
