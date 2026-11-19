package com.mechanical.client;

import com.mechanical.CartEntity;
import com.mechanical.Mechanical;
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

/** Draws the {@link CartModel} turned to the cart's yaw (shafts forward), wheels rolling. Loaded carcasses draw themselves. */
public class CartRenderer extends EntityRenderer<CartEntity, CartRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/cart.png");
    private final CartModel model;

    public static class State extends EntityRenderState {
        float yaw, spin;
    }

    public CartRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new CartModel(context.bakeLayer(CartModel.LAYER));
        shadowRadius = 0.8F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CartEntity cart, State state, float partialTicks) {
        super.extractRenderState(cart, state, partialTicks);
        state.yaw = cart.getViewYRot(partialTicks);
        state.spin = Mth.lerp(partialTicks, cart.wheelSpinO, cart.wheelSpin);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // model -z (the shafts) to the facing direction; model y points down
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, state.spin, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
