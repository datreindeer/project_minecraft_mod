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
    /** The pole's reach ahead of the front axle to the yoke, blocks (WagonModel). */
    private static final float POLE_RUN = 2.4F;
    private final WagonModel model;
    private final TiltModel canopy;
    private final ItemModelResolver items;
    /** Chest, hay, seat by module id; made on first use (never in a static initializer: components aren't bound yet). */
    private ItemStack[] moduleStacks;

    public static class State extends EntityRenderState {
        float yaw, spin, steer, pitch, roll, lift, pole;
        boolean open; // a log on the bed: no back board
        final ItemStackRenderState[] modules = new ItemStackRenderState[WagonEntity.SLOTS];
        final CargoRender.State cargo = new CargoRender.State();
        Identifier tilt; // a caravan's wagon: its dyed tilt, else null

        State() {
            for (int i = 0; i < modules.length; i++) modules[i] = new ItemStackRenderState();
        }
    }

    public WagonRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new WagonModel(context.bakeLayer(WagonModel.LAYER));
        canopy = new TiltModel(context.bakeLayer(TiltModel.WAGON));
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
        state.pitch = wagon.pitch(partialTicks);
        state.roll = wagon.roll(partialTicks);
        double ground = wagon.groundY(partialTicks);
        state.lift = (float) (ground - state.y);
        // the pole keeps to the horses' collars: its tilt in the world follows how far above the front axle they stand
        float pitch = state.pitch * Mth.DEG_TO_RAD;
        double horses = wagon.harness() == null ? Double.NaN : wagon.partner() == null ? wagon.harness().getPosition(partialTicks).y
                : (wagon.harness().getPosition(partialTicks).y + wagon.partner().getPosition(partialTicks).y) / 2;
        double rise = Double.isNaN(horses) ? 0 : horses - ground - Math.sin(pitch) * WagonEntity.FRONT_AXLE;
        state.pole = Mth.clamp((float) Math.atan2(rise, POLE_RUN), -0.6F, 0.6F) - pitch;
        state.open = wagon.carriedLog() != null;
        if (moduleStacks == null)
            moduleStacks = new ItemStack[] {ItemStack.EMPTY, new ItemStack(Items.CHEST), new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.OAK_SLAB)};
        for (int i = 0; i < WagonEntity.SLOTS; i++) {
            int module = wagon.module(i);
            if (module == WagonEntity.EMPTY) state.modules[i].clear();
            else items.updateForNonLiving(state.modules[i], moduleStacks[module], ItemDisplayContext.NONE, wagon);
        }
        CargoRender.extract(wagon, state.cargo, items);
        state.tilt = wagon.canopy().isEmpty() ? null : CargoRender.tilt(wagon.canopy(), true);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // model -z (the pole) to the facing direction; here the front is -z and y is up
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // resting on its four wheels: rolled about the ground line under the middle, pitched about the middle (WagonEntity#onBed)
        poseStack.translate(0.0F, state.lift, 0.0F);
        poseStack.rotate(Axis.ZP.rotationDegrees(state.roll));
        poseStack.rotate(Axis.XP.rotationDegrees(state.pitch));
        for (int i = 0; i < WagonEntity.SLOTS; i++) {
            if (state.modules[i].isEmpty()) continue;
            // the item model is a block centred on the origin: stand it on the floor in its place
            poseStack.pushPose();
            poseStack.translate(0.0F, (float) WagonEntity.BED + MODULE_SCALE / 2, (float) -WagonEntity.slotAlong(i));
            poseStack.scale(MODULE_SCALE, MODULE_SCALE, MODULE_SCALE);
            state.modules[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        // a caravan's load: 2 across, 4 along the bed (24 x 48 pixels inside the boards)
        CargoRender.submit(state.cargo, 2, 4, 0.68F, (float) WagonEntity.BED, 0.62F, poseStack, collector, state.lightCoords, state.outlineColor);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // model x and y both flipped (a half turn about z): + yRot still swings the pole to the wagon's right, as + steer means
        collector.submitModel(model, new WagonModel.Pose(state.spin, state.steer * Mth.DEG_TO_RAD, state.pole, state.open), poseStack, TEXTURE,
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (state.tilt != null) collector.submitModel(canopy, 0.0F, poseStack, state.tilt, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
