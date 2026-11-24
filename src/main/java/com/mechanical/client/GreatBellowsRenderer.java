package com.mechanical.client;

import com.mechanical.GreatBellowsBlockEntity;
import com.mechanical.HighFurnace;
import com.mechanical.Mechanical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The Great Bellows' moving parts ({@link GreatBellowsBlockEntity}): the spur gears on the incoming axle (both sides),
 * the cam shaft above turning the other way, and the two bellows' top boards hinged at the nozzle end, lifted and let
 * fall in turn by the cams, with their leather pleats opening between the boards. All from the rotor's angle, so it
 * moves in step with the wheel. Part models: scripts/gen_high_furnace.py (hinges and axles at their model centre).
 */
public class GreatBellowsRenderer implements BlockEntityRenderer<GreatBellowsBlockEntity, GreatBellowsRenderer.State> {
    /** The boards' widest opening, degrees; the gear ratio from the axle to the cam shaft. */
    private static final float OPEN = 17, RATIO = 1.3F, PLEATS = 3;
    private static final float[] BELLOWS_X = {1.5F, 8.5F};

    public static class State extends BlockEntityRenderState {
        ItemStackRenderState lid, pleat, cam, gear;
        float yaw, axle;
    }

    private final ItemModelResolver items;

    public GreatBellowsRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GreatBellowsBlockEntity bellows, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bellows, state, partialTicks, camera, breakProgress);
        Level level = bellows.getLevel();
        int seed = (int) bellows.getBlockPos().asLong();
        if (state.lid == null) {
            state.lid = WaterPowerRenderers.part(items, "great_bellows_lid", level, seed);
            state.pleat = WaterPowerRenderers.part(items, "great_bellows_pleat", level, seed);
            state.cam = WaterPowerRenderers.part(items, "great_bellows_cam", level, seed);
            state.gear = WaterPowerRenderers.part(items, "great_bellows_gear", level, seed);
        }
        state.yaw = -bellows.facing().toYRot() + 180; // the model's nozzle points north
        state.axle = bellows.angle(WaterPowerRenderers.time(level, partialTicks));
    }

    /** Puts the pose at a point given in the block's pixels (0..16), the block's model frame. */
    private static void at(PoseStack pose, float x, float y, float z) {
        pose.translate((x - 8) / 16F, (y - 8) / 16F, (z - 8) / 16F);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords, overlay = OverlayTexture.NO_OVERLAY;
        float cam = -state.axle * RATIO;
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.rotateDegrees(Axis.YP, state.yaw);
        // the spur gears on the axle's ends, outside the side bearings
        for (float x : new float[] {-0.1F, 16.1F}) {
            pose.pushPose();
            at(pose, x, 8, 8);
            pose.rotateDegrees(Axis.YP, 90);
            pose.rotateDegrees(Axis.ZP, state.axle);
            state.gear.submit(pose, collector, light, overlay, 0);
            pose.popPose();
        }
        // the cam shaft across the back posts
        pose.pushPose();
        at(pose, 8, 13, 12.5F);
        pose.rotateDegrees(Axis.YP, 90);
        pose.rotateDegrees(Axis.ZP, cam);
        state.cam.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        // the two bellows: lids open in turn (half a turn apart), pleats spread between bottom and lid
        for (int i = 0; i < 2; i++) {
            float open = OPEN * (0.5F + 0.5F * Mth.sin((cam + i * 180) * Mth.DEG_TO_RAD));
            float x0 = BELLOWS_X[i];
            for (int k = 1; k <= PLEATS; k++) {
                pose.pushPose();
                at(pose, x0, 3.5F + k * 0.15F, 2.6F);
                pose.rotateDegrees(Axis.XP, -open * k / (PLEATS + 1));
                state.pleat.submit(pose, collector, light, overlay, 0);
                pose.popPose();
            }
            pose.pushPose();
            at(pose, x0, 4.6F, 2);
            pose.rotateDegrees(Axis.XP, -open);
            state.lid.submit(pose, collector, light, overlay, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.GREAT_BELLOWS_ENTITY.get(), GreatBellowsRenderer::new);
        }
    }
}
