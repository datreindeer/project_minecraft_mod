package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.GearboxBlock;
import com.mechanical.Mechanical;
import com.mechanical.SawBenchBlockEntity;
import com.mechanical.Sawmill;
import com.mechanical.ShaftBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The Saw Bench's moving parts ({@link SawBenchBlockEntity}): the flywheel on the side the shaft comes in, turning
 * {@link SawBenchBlockEntity#CRANK_RATIO} times the wheel's speed; the pitman rod from its crank pin up to the sash; the
 * sash with the blade riding up and down with the crank; and the logs floating in the in-side flume. The log at the blade
 * is drawn in two pieces: what is still whole upstream of the blade, and what has come through downstream (boards from a
 * barked-off log, a hewn log from one with bark). Part models: scripts/gen_waterpower.py.
 */
public class SawBenchRenderer implements BlockEntityRenderer<SawBenchBlockEntity, SawBenchRenderer.State> {
    /** Crank radius and the flywheel's distance from the bench's centre line, in pixels. */
    private static final float CRANK = 3, WHEEL_X = 9.25F, PITMAN_X = 10.85F, PITMAN = 17.75F;
    /** A log's thickness, and its centre above the block centre (floating in the 10 px deep water). */
    private static final float LOG = 0.5F, LOG_Y = 1 / 16F;

    public static class Piece {
        final ItemStackRenderState model = new ItemStackRenderState();
        float from, to; // along the line, blocks from the blade
        boolean boards;
    }

    public static class State extends BlockEntityRenderState {
        ItemStackRenderState sash, flywheel, pitman;
        float yaw, crank, bob;
        int side = 1;
        final List<Piece> pieces = new ArrayList<>();
    }

    private final ItemModelResolver items;

    public SawBenchRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** A shaft or gearbox meets the bench from this side. */
    private static boolean driven(Level level, BlockPos pos, Direction side) {
        BlockState s = level.getBlockState(pos.relative(side));
        return s.getBlock() instanceof GearboxBlock || s.getBlock() instanceof ShaftBlock && s.getValue(ShaftBlock.AXIS) == side.getAxis();
    }

    @Override
    public void extractRenderState(SawBenchBlockEntity saw, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(saw, state, partialTicks, camera, breakProgress);
        Level level = saw.getLevel();
        int seed = (int) saw.getBlockPos().asLong();
        if (state.sash == null) {
            state.sash = WaterPowerRenderers.part(items, "saw_sash", level, seed);
            state.flywheel = WaterPowerRenderers.part(items, "saw_flywheel", level, seed);
            state.pitman = WaterPowerRenderers.part(items, "saw_pitman", level, seed);
        }
        Direction front = saw.front();
        state.yaw = 180 - front.toYRot();
        state.side = level != null && !driven(level, saw.getBlockPos(), front.getClockWise()) && driven(level, saw.getBlockPos(), front.getCounterClockWise()) ? -1 : 1;
        float time = WaterPowerRenderers.time(level, partialTicks);
        state.crank = saw.angle(time) * SawBenchBlockEntity.CRANK_RATIO % 360;
        state.bob = Mth.sin(time * 0.12F) * 0.012F;
        state.pieces.clear();
        boolean moving = saw.problem().flows();
        for (SawBenchBlockEntity.Log log : saw.logs()) {
            Sawmill.Wood wood = Sawmill.woodOf(log.item);
            if (wood == null) continue;
            // the client moves logs a tick at a time; slide between ticks while they float
            float s = log.s + (moving && log.s < -SawBenchBlockEntity.HALF ? SawBenchBlockEntity.FEED * partialTicks : 0);
            float back = s - SawBenchBlockEntity.HALF, ahead = s + SawBenchBlockEntity.HALF;
            ItemStack whole = log.item.getItem() instanceof com.mechanical.RoughLogItem ? new ItemStack(Sawmill.strippedLog(wood.wood())) : log.item;
            piece(state, whole, back, Math.min(ahead, 0), false, level, seed);
            if (ahead > 0) piece(state, wood.bark() ? new ItemStack(Sawmill.strippedLog(wood.wood())) : new ItemStack(Sawmill.planks(wood.wood())),
                    0, ahead, !wood.bark(), level, seed);
        }
    }

    private void piece(State state, ItemStack stack, float from, float to, boolean boards, @Nullable Level level, int seed) {
        if (to - from < 0.01F) return;
        Piece p = new Piece();
        items.updateForTopItem(p.model, stack, ItemDisplayContext.NONE, level, null, seed);
        p.from = from;
        p.to = to;
        p.boards = boards;
        state.pieces.add(p);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords, overlay = OverlayTexture.NO_OVERLAY;
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.rotateDegrees(Axis.YP, state.yaw); // local -Z: the in side, +Z: downstream
        float th = state.crank * Mth.DEG_TO_RAD;
        float pinY = CRANK * Mth.cos(th), pinZ = state.side * CRANK * Mth.sin(th); // the pin turns about the crank's own axis
        // flywheel with its crank pin, on the driven side
        pose.pushPose();
        pose.translate(state.side * WHEEL_X / 16F, 0, 0);
        pose.rotateDegrees(Axis.YP, state.side * 90);
        pose.rotateDegrees(Axis.ZP, state.crank);
        state.flywheel.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        // the sash rides with the pin's height (a long pitman)
        pose.pushPose();
        pose.translate(0, pinY / 16F, 0);
        state.sash.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        // pitman from the pin up to the sash's crossbar
        pose.pushPose();
        pose.translate(state.side * PITMAN_X / 16F, pinY / 16F, pinZ / 16F);
        pose.rotateDegrees(Axis.XP, (float) Math.toDegrees(Math.atan2(-pinZ, PITMAN)));
        state.pitman.submit(pose, collector, light, overlay, 0);
        pose.popPose();
        // logs
        for (Piece p : state.pieces) {
            float len = p.to - p.from, mid = (p.from + p.to) / 2;
            if (p.boards) { // three boards stacked as they come off the blade
                for (int b = 0; b < 3; b++) {
                    pose.pushPose();
                    pose.translate(0, LOG_Y + state.bob - 0.16F + b * 0.16F, mid - b * 0.04F);
                    pose.scale(LOG * (0.95F - b * 0.08F), 0.1F, Math.max(0.01F, len - b * 0.08F));
                    p.model.submit(pose, collector, light, overlay, 0);
                    pose.popPose();
                }
                continue;
            }
            pose.pushPose();
            pose.translate(0, LOG_Y + state.bob, mid);
            pose.scale(LOG, LOG, len);
            pose.rotateDegrees(Axis.XP, 90);
            p.model.submit(pose, collector, light, overlay, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SawBenchBlockEntity saw) {
        return new AABB(saw.getBlockPos()).inflate(Sawmill.FLUMES + 1, 1, Sawmill.FLUMES + 1).expandTowards(0, 1, 0);
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Sawmill.SAW_BENCH_ENTITY.get(), SawBenchRenderer::new);
        }

        /** The water face in a flume or saw bench is tinted like the biome's water. */
        @SubscribeEvent
        static void tints(RegisterColorHandlersEvent.BlockTintSources event) {
            event.register(List.of(BlockTintSources.water()), Sawmill.LOG_FLUME.get(), Sawmill.SAW_BENCH.get());
        }
    }
}
