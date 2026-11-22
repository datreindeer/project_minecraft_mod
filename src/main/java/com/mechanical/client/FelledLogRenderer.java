package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.FelledLogEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Draws a {@link FelledLogEntity}: each log's own block model lying along the body (axis turned to its length), and taut ropes from the root end to its pullers. */
public class FelledLogRenderer extends EntityRenderer<FelledLogEntity, FelledLogRenderer.State> {
    public static class State extends EntityRenderState {
        final List<MovingBlockRenderState> blocks = new ArrayList<>();
        final List<Vec3> offsets = new ArrayList<>();
        float yaw;
    }

    public FelledLogRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FelledLogEntity log, State state, float partialTicks) {
        super.extractRenderState(log, state, partialTicks);
        state.yaw = log.getViewYRot(partialTicks);
        state.offsets.clear();
        int n = log.size();
        while (state.blocks.size() < n) state.blocks.add(new MovingBlockRenderState());
        while (state.blocks.size() > n) state.blocks.removeLast();
        ClientLevel level = log.level() instanceof ClientLevel l ? l : null;
        for (int i = 0; i < n; i++) {
            BlockPos c = log.cells().get(i);
            state.offsets.add(new Vec3(c.getX() - log.centreX() - 0.5, c.getY(), c.getZ() - log.centreZ() - 0.5));
            BlockState block = log.states().get(i);
            // lying along the body: the grain runs along local z
            if (block.hasProperty(BlockStateProperties.AXIS)) block = block.setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.Z);
            MovingBlockRenderState r = state.blocks.get(i);
            BlockPos pos = log.cellPos(i);
            r.blockState = block;
            r.blockPos = pos; // nothing else is reported there, so no faces are culled against the world
            r.randomSeedPos = pos;
            if (level != null) {
                r.biome = level.getBiome(pos);
                r.cardinalLighting = level.cardinalLighting();
                r.lightEngine = level.getLightEngine();
            }
        }
        int[] ids = log.pullerIds();
        if (ids.length == 0 || n == 0 || level == null) {
            state.leashStates = null;
            return;
        }
        Vec3 start = log.rootPoint(partialTicks), at = log.getPosition(partialTicks);
        BlockPos startPos = BlockPos.containing(start);
        state.leashStates = new ArrayList<>(ids.length);
        for (int id : ids) {
            Entity puller = level.getEntity(id);
            if (puller == null) continue;
            EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
            leash.start = start;
            leash.offset = start.subtract(at);
            leash.end = puller.getRopeHoldPosition(partialTicks);
            BlockPos endPos = BlockPos.containing(puller.getEyePosition(partialTicks));
            leash.startBlockLight = level.getBrightness(LightLayer.BLOCK, startPos);
            leash.endBlockLight = level.getBrightness(LightLayer.BLOCK, endPos);
            leash.startSkyLight = level.getBrightness(LightLayer.SKY, startPos);
            leash.endSkyLight = level.getBrightness(LightLayer.SKY, endPos);
            leash.slack = false;
            state.leashStates.add(leash);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(-state.yaw));
        for (int i = 0; i < state.blocks.size(); i++) {
            Vec3 o = state.offsets.get(i);
            poseStack.pushPose();
            poseStack.translate(o.x, o.y, o.z);
            collector.submitMovingBlock(poseStack, state.blocks.get(i), state.outlineColor);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
