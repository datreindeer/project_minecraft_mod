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
 * The Great Bellows' moving parts ({@link GreatBellowsBlockEntity}): the axle across the block in line with the wheel's,
 * a crank on its free end (the side no shaft comes in from), a long pitman rod from the crank pin up to a rocker beam on a
 * tall post, and from the beam's back arm a hanger down to the top board, which swings on its hinge at the nozzle end;
 * pleated leather opens between the boards. All from the rotor's angle, so it works in step with the wheel. The bellows
 * reaches back into the block behind. Part models: scripts/gen_high_furnace.py.
 */
public class GreatBellowsRenderer implements BlockEntityRenderer<GreatBellowsBlockEntity, GreatBellowsRenderer.State> {
    private static final float CRANK = 3, PIVOT_Y = 24, PIVOT_Z = 16, FRONT_ARM = 12, BACK_ARM = 14, HANG = 4;
    private static final float HINGE_Y = 11.5F, HINGE_Z = 2, BACK_Z = 28, PLEATS = 6;

    public static class State extends BlockEntityRenderState {
        ItemStackRenderState lid, pleat, pleatIn, shaft, crank, pitman, beam, post, pin;
        float yaw, axle;
        int side = 1;
    }

    private final ItemModelResolver items;

    public GreatBellowsRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** A shaft or gearbox meets the bellows from this side. */
    private static boolean driven(@Nullable Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
        if (level == null) return false;
        var s = level.getBlockState(pos.relative(side));
        return s.getBlock() instanceof com.mechanical.GearboxBlock || s.getBlock() instanceof com.mechanical.ShaftBlock && s.getValue(com.mechanical.ShaftBlock.AXIS) == side.getAxis();
    }

    @Override
    public void extractRenderState(GreatBellowsBlockEntity bellows, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bellows, state, partialTicks, camera, breakProgress);
        Level level = bellows.getLevel();
        int seed = (int) bellows.getBlockPos().asLong();
        var front = bellows.facing();
        // the model's +x (east when it faces north) is the nozzle's clockwise side; the crank goes where no shaft comes in
        state.side = driven(level, bellows.getBlockPos(), front.getClockWise()) ? -1 : 1;
        if (state.lid == null || state.post == null) {
            state.lid = WaterPowerRenderers.part(items, "great_bellows_lid", level, seed);
            state.pleat = WaterPowerRenderers.part(items, "great_bellows_pleat", level, seed);
            state.pleatIn = WaterPowerRenderers.part(items, "great_bellows_pleat_in", level, seed);
            state.shaft = WaterPowerRenderers.part(items, "great_bellows_shaft", level, seed);
            state.crank = WaterPowerRenderers.part(items, "great_bellows_crank", level, seed);
            state.pitman = WaterPowerRenderers.part(items, "great_bellows_pitman", level, seed);
            state.beam = WaterPowerRenderers.part(items, "great_bellows_beam", level, seed);
            state.pin = WaterPowerRenderers.part(items, "great_bellows_pin", level, seed);
        }
        state.post = WaterPowerRenderers.part(items, state.side > 0 ? "great_bellows_post_r" : "great_bellows_post_l", level, seed);
        state.yaw = -front.toYRot() + 180; // the model's nozzle points north
        state.axle = bellows.angle(WaterPowerRenderers.time(level, partialTicks));
    }

    /** Puts the pose at a point given in the block's pixels (0..16), the block's model frame. */
    private static void at(PoseStack pose, float x, float y, float z) {
        pose.translate((x - 8) / 16F, (y - 8) / 16F, (z - 8) / 16F);
    }

    /** Draws a 16 px rod part from (x, y0, z0) to (x, y1, z1), stretched to fit. */
    private static void rod(PoseStack pose, ItemStackRenderState part, SubmitNodeCollector c, int light, float x, float y0, float z0, float y1, float z1) {
        float dy = y1 - y0, dz = z1 - z0, len = (float) Math.sqrt(dy * dy + dz * dz);
        pose.pushPose();
        at(pose, x, y0, z0);
        pose.rotateDegrees(Axis.XP, (float) Math.toDegrees(Math.atan2(dz, dy)));
        pose.scale(1, len / 16, 1);
        part.submit(pose, c, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords, overlay = OverlayTexture.NO_OVERLAY;
        float th = state.axle * Mth.DEG_TO_RAD;
        float pinY = 8 + CRANK * Mth.cos(th), pinZ = 8 + CRANK * Mth.sin(th);
        float tilt = (float) Math.asin(Mth.clamp((pinY - 8) / FRONT_ARM, -1, 1)); // the beam rocks with the crank
        float frontY = PIVOT_Y + FRONT_ARM * Mth.sin(tilt), frontZ = PIVOT_Z - FRONT_ARM * Mth.cos(tilt);
        float backY = PIVOT_Y - BACK_ARM * Mth.sin(tilt), backZ = PIVOT_Z + BACK_ARM * Mth.cos(tilt);
        float boardY = backY - HANG;
        float open = (float) Math.toDegrees(Math.atan2(Math.max(0.3F, boardY - HINGE_Y - 1.5F), BACK_Z - HINGE_Z));
        float sx = 8 + state.side * 9.6F; // the crank side, just outside the bearing post (its sweep stays clear of the wall and the floor)
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.rotateDegrees(Axis.YP, state.yaw);
        pose.pushPose(); // the axle
        pose.rotateDegrees(Axis.YP, 90);
        pose.rotateDegrees(Axis.ZP, -state.axle);
        state.shaft.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        pose.pushPose(); // the crank on its end
        at(pose, sx, 8, 8);
        pose.rotateDegrees(Axis.YP, 90);
        pose.rotateDegrees(Axis.ZP, -state.axle);
        state.crank.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        pose.pushPose(); // the rocker post (authored at the block's side)
        state.post.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        pose.pushPose(); // the rocker beam on its pivot
        at(pose, sx, PIVOT_Y, PIVOT_Z);
        pose.rotateDegrees(Axis.XP, (float) -Math.toDegrees(tilt));
        state.beam.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        float rx = sx + state.side * 1.2F;
        rod(pose, state.pitman, collector, light, rx, pinY, pinZ, frontY, frontZ);   // crank pin up to the beam
        rod(pose, state.pitman, collector, light, rx, boardY, BACK_Z, backY, backZ); // the hanger down to the board
        for (float[] j : new float[][] {{pinY, pinZ}, {frontY, frontZ}, {backY, backZ}, {boardY, BACK_Z}}) { // pins and eyes at the joints
            pose.pushPose();
            at(pose, rx, j[0], j[1]);
            pose.rotateDegrees(Axis.YP, 90);
            state.pin.submit(pose, collector, light, overlay, 0);
            pose.popPose();
        }
        // the leather folds, then the top board, all on the hinge at the mouth
        for (int k = 1; k <= PLEATS; k++) {
            pose.pushPose();
            at(pose, 8, HINGE_Y + k * 0.03F, HINGE_Z);
            pose.rotateDegrees(Axis.XP, -open * k / (PLEATS + 1));
            pose.translate(6 / 16F, 0, 12 / 16F);
            pose.translate(-6 / 16F, 0, 0);
            (k % 2 == 1 ? state.pleat : state.pleatIn).submit(pose, collector, light, overlay, 0);
            pose.popPose();
        }
        pose.pushPose();
        at(pose, 8, HINGE_Y, HINGE_Z);
        pose.rotateDegrees(Axis.XP, -open);
        pose.translate(0, 0, 12 / 16F);
        state.lid.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        pose.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(GreatBellowsBlockEntity bellows) {
        return new net.minecraft.world.phys.AABB(bellows.getBlockPos()).inflate(1.2, 0, 1.2).expandTowards(0, 1.2, 0)
                .expandTowards(bellows.facing().getOpposite().getStepX(), 0, bellows.facing().getOpposite().getStepZ());
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.GREAT_BELLOWS_ENTITY.get(), GreatBellowsRenderer::new);
        }
    }
}
