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

/** Draws the {@link TravoisModel} turned to the travois' yaw (its poles point the way it faces). Loaded carcasses draw themselves. */
public class TravoisRenderer extends EntityRenderer<TravoisEntity, TravoisRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/travois.png");
    private final Model.Simple model;

    public static class State extends EntityRenderState {
        float yaw;
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
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // model -z (the front) to the facing direction; model y points down
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, Unit.INSTANCE, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
