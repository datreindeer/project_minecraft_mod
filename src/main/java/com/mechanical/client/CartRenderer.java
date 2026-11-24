package com.mechanical.client;

import com.mechanical.CartEntity;
import com.mechanical.Mechanical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Draws the {@link CartModel} turned to the cart's yaw (shafts forward), wheels rolling, the whole cart tipped about the axle
 * to its {@link CartEntity#pitch}: shafts up to the puller, or down on the ground when parked. Loaded carcasses draw themselves;
 * a mounted chest is the Chest item model standing on the bed; a caravan's cart has a canvas tilt over it, dyed for its kind (TiltModel), and its load under it (CargoRender).
 */
public class CartRenderer extends EntityRenderer<CartEntity, CartRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/cart.png");
    /** Bed floor top (CartModel floor box), blocks. */
    private static final float FLOOR = 11 / 16.0F;
    private final CartModel model;
    private final TiltModel canopy;
    private final ItemModelResolver items;
    private ItemStack chest;

    public static class State extends EntityRenderState {
        float yaw, spin, pitch, roll, lift;
        boolean canopy;
        Identifier tilt;
        final ItemStackRenderState chest = new ItemStackRenderState();
        final CargoRender.State cargo = new CargoRender.State();
    }

    public CartRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new CartModel(context.bakeLayer(CartModel.LAYER));
        canopy = new TiltModel(context.bakeLayer(TiltModel.CART));
        shadowRadius = 0.8F;
        items = context.getItemModelResolver();
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
        state.pitch = cart.pitch(partialTicks);
        state.roll = cart.roll(partialTicks);
        state.lift = (float) (cart.groundY(partialTicks) - state.y);
        state.canopy = cart.isCaravan() || !cart.canopy().isEmpty();
        state.tilt = CargoRender.tilt(cart.canopy(), false);
        if (cart.hasChest()) {
            if (chest == null) chest = new ItemStack(Items.CHEST);
            items.updateForNonLiving(state.chest, chest, ItemDisplayContext.NONE, cart);
        } else state.chest.clear();
        CargoRender.extract(cart, state.cargo, items);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // model -z (the shafts) to the facing direction; model y points down
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // here the front is -z and y is up; resting on its two wheels (rolled about the ground line under them), then
        // tipped about the axle, the wheels stay on the ground (CartEntity#onBed)
        poseStack.translate(0.0F, state.lift, 0.0F);
        poseStack.rotate(Axis.ZP.rotationDegrees(state.roll));
        poseStack.translate(0.0F, CartModel.AXLE_HEIGHT, CartModel.AXLE_BACK);
        poseStack.rotate(Axis.XP.rotationDegrees(state.pitch));
        poseStack.translate(0.0F, -CartModel.AXLE_HEIGHT, -CartModel.AXLE_BACK);
        if (!state.chest.isEmpty()) {
            // the item model is a block centred on the origin: lift it onto the floor
            poseStack.pushPose();
            poseStack.translate(0.0F, FLOOR + 0.5F, 0.1F);
            state.chest.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        // a caravan's load under the tilt: 2 across, 3 along the bed (20 x 28 pixels inside the boards)
        CargoRender.submit(state.cargo, 2, 3, 0.56F, FLOOR, 0.5F, poseStack, collector, state.lightCoords, state.outlineColor);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, state.spin, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (state.canopy) collector.submitModel(canopy, 0.0F, poseStack, state.tilt, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
