package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.PloughEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws the {@link PloughModel} turned to the plough's yaw (beam forward), the wheel rolling, the whole plough tilted
 * about the wheel's axle: share dug in when it is down, lifted clear when it is up ({@link PloughEntity#pitch}).
 */
public class PloughRenderer extends EntityRenderer<PloughEntity, PloughRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/mouldboard_plough.png");
    private final PloughModel model;

    public static class State extends EntityRenderState {
        float yaw, spin, pitch;
    }

    public PloughRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new PloughModel(context.bakeLayer(PloughModel.LAYER));
        shadowRadius = 0.6F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PloughEntity plough, State state, float partialTicks) {
        super.extractRenderState(plough, state, partialTicks);
        state.yaw = plough.getViewYRot(partialTicks);
        state.spin = Mth.lerp(partialTicks, plough.wheelSpinO, plough.wheelSpin);
        state.pitch = plough.pitch(partialTicks);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // model -z (the beam's front) to the facing direction; here the front is -z and y is up
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yaw));
        float axleY = -PloughModel.AXLE_Y / 16.0F, axleZ = PloughModel.AXLE_Z / 16.0F;
        poseStack.translate(0.0F, axleY, axleZ);
        poseStack.rotate(Axis.XP.rotationDegrees(state.pitch));
        poseStack.translate(0.0F, -axleY, -axleZ);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, state.spin, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
