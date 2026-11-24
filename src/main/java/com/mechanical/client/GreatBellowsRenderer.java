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
 * The Great Bellows' moving parts ({@link GreatBellowsBlockEntity}): the shaft running across the block in line with the
 * incoming axle, turning with it, and its cam pressing the top board down once a turn; between the strokes the board
 * rises again on its hinge at the bellows' mouth and the folded leather opens under it. All from the rotor's angle, so
 * it works in step with the wheel. Part models: scripts/gen_high_furnace.py (hinges and axles at their model centre).
 */
public class GreatBellowsRenderer implements BlockEntityRenderer<GreatBellowsBlockEntity, GreatBellowsRenderer.State> {
    /** The board's widest opening, degrees (it just clears the shaft); leather folds between the boards. */
    private static final float OPEN = 12.5F, PLEATS = 5;

    public static class State extends BlockEntityRenderState {
        ItemStackRenderState lid, pleat, pleatIn, shaft;
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
            state.pleatIn = WaterPowerRenderers.part(items, "great_bellows_pleat_in", level, seed);
            state.shaft = WaterPowerRenderers.part(items, "great_bellows_shaft", level, seed);
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
        float open = OPEN * (0.5F + 0.5F * Mth.cos(state.axle * Mth.DEG_TO_RAD)); // the cam down: pressed shut
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.rotateDegrees(Axis.YP, state.yaw);
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, 90);
        pose.rotateDegrees(Axis.ZP, state.axle);
        state.shaft.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        for (int k = 1; k <= PLEATS; k++) {
            pose.pushPose();
            at(pose, 2, 2.5F + k * 0.2F, 2.5F);
            pose.rotateDegrees(Axis.XP, -open * k / (PLEATS + 1));
            (k % 2 == 1 ? state.pleat : state.pleatIn).submit(pose, collector, light, overlay, 0);
            pose.popPose();
        }
        pose.pushPose();
        at(pose, 2, 3.6F, 2.5F);
        pose.rotateDegrees(Axis.XP, -open);
        state.lid.submit(pose, collector, light, overlay, 0);
        pose.popPose();
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
