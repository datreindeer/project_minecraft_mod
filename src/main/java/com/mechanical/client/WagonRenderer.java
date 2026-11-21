package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.WagonEntity;
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
 * Draws the {@link WagonModel} turned to the wagon's yaw (pole forward), wheels rolling, the front axle and pole swung
 * towards the horses ({@link WagonEntity#steer}). The modules stand on the bed as item models scaled to their place: a
 * Chest, a Hay Bale, an oak slab for a seat. Loaded carcasses and seated players draw themselves.
 */
public class WagonRenderer extends EntityRenderer<WagonEntity, WagonRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wagon.png");
    /** Modules fill a place (0.75 deep) without touching the next. */
    private static final float MODULE_SCALE = 0.72F;
    private final WagonModel model;
    private final ItemModelResolver items;
    /** Chest, hay, seat by module id; made on first use (never in a static initializer: components aren't bound yet). */
    private ItemStack[] moduleStacks;

    public static class State extends EntityRenderState {
        float yaw, spin, steer;
        final ItemStackRenderState[] modules = new ItemStackRenderState[WagonEntity.SLOTS];

        State() {
            for (int i = 0; i < modules.length; i++) modules[i] = new ItemStackRenderState();
        }
    }

    public WagonRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new WagonModel(context.bakeLayer(WagonModel.LAYER));
        shadowRadius = 1.1F;
        items = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WagonEntity wagon, State state, float partialTicks) {
        super.extractRenderState(wagon, state, partialTicks);
        state.yaw = wagon.getViewYRot(partialTicks);
        state.spin = Mth.lerp(partialTicks, wagon.wheelSpinO, wagon.wheelSpin);
        state.steer = Mth.lerp(partialTicks, wagon.steerO, wagon.steer);
        if (moduleStacks == null)
            moduleStacks = new ItemStack[] {ItemStack.EMPTY, new ItemStack(Items.CHEST), new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.OAK_SLAB)};
        for (int i = 0; i < WagonEntity.SLOTS; i++) {
            int module = wagon.module(i);
            if (module == WagonEntity.EMPTY) state.modules[i].clear();
            else items.updateForNonLiving(state.modules[i], moduleStacks[module], ItemDisplayContext.NONE, wagon);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // model -z (the pole) to the facing direction; here the front is -z and y is up
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yaw));
        for (int i = 0; i < WagonEntity.SLOTS; i++) {
            if (state.modules[i].isEmpty()) continue;
            // the item model is a block centred on the origin: stand it on the floor in its place
            poseStack.pushPose();
            poseStack.translate(0.0F, (float) WagonEntity.BED + MODULE_SCALE / 2, (float) -WagonEntity.slotAlong(i));
            poseStack.scale(MODULE_SCALE, MODULE_SCALE, MODULE_SCALE);
            state.modules[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // model x and y both flipped (a half turn about z): + yRot still swings the pole to the wagon's right, as + steer means
        collector.submitModel(model, new WagonModel.Pose(state.spin, state.steer * Mth.DEG_TO_RAD), poseStack, TEXTURE,
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
