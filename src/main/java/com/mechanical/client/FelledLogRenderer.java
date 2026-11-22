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
        /** The block being hewn or sawn (-1: none) and its crack stage. */
        int cracked = -1, crackStage;
        /** The seam being sawn (behind block kerf, -1: none) and how deep the cut is (0..1). */
        int kerf = -1;
        float kerfDepth;
        /** Lifted by hand: both ends up (raised evenly), or one end (the root end if rootUp) tilting the log about the other. */
        boolean bothUp, oneUp, rootUp;
        int length;
        /** Dev (LogShots): the collision boxes, relative to the entity. */
        net.minecraft.world.phys.shapes.VoxelShape debugShape;
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
            if (block.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) block = block.setValue(BlockStateProperties.HORIZONTAL_AXIS, net.minecraft.core.Direction.Axis.Z);
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
        state.debugShape = com.mechanical.LogShots.showShapes ? log.collisionShape().move(-state.x, -state.y, -state.z) : null;
        state.bothUp = log.rootLifted() && log.tailLifted();
        state.oneUp = log.rootLifted() != log.tailLifted();
        state.rootUp = log.rootLifted();
        state.length = n;
        state.cracked = log.crackedBlock() < n ? log.crackedBlock() : -1;
        state.crackStage = log.crackStage();
        state.kerf = log.kerfSeam() + 1 < n ? log.kerfSeam() : -1;
        state.kerfDepth = log.kerfDepth();
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

    private static final net.minecraft.resources.Identifier KERF = net.minecraft.resources.Identifier.fromNamespaceAndPath(com.mechanical.Mechanical.MODID, "textures/block/saw_kerf.png");

    /** A kerf 1/8 wide at z = 1 (the seam), a hair proud of the faces: across the top, and from the top down {@code depth} on both sides. */
    private static void kerf(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, float depth, int light) {
        float z0 = 1 - 0.0625F, z1 = 1 + 0.0625F, e = 0.003F, y0 = 1 - depth;
        quad(pose, buffer, light, 0, 1, 0, 0, 1 + e, z0, 0, 1 + e, z1, 1, 1 + e, z1, 1, 1 + e, z0); // up
        quad(pose, buffer, light, -1, 0, 0, -e, y0, z0, -e, y0, z1, -e, 1, z1, -e, 1, z0); // west
        quad(pose, buffer, light, 1, 0, 0, 1 + e, y0, z1, 1 + e, y0, z0, 1 + e, 1, z0, 1 + e, 1, z1); // east
    }

    private static void quad(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, float nx, float ny, float nz, float... v) {
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int side = 0; side < 2; side++) // both windings: whichever way it faces, culling keeps one
            for (int j = 0; j < 4; j++) {
                int k = side == 0 ? j : 3 - j;
                buffer.addVertex(pose, v[k * 3], v[k * 3 + 1], v[k * 3 + 2]).setColor(-1).setUv(uv[k][0], uv[k][1])
                        .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
            }
    }

    /** The destroy-stage texture on all six faces of the unit block (the crack overlay). */
    private static void crackCube(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer) {
        float[][] faces = { // four corners each, counter-clockwise seen from outside
            {0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1}, {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0}, // down, up
            {0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0}, {0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1}, // north, south
            {0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0}, {1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1}}; // west, east
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (float[] f : faces)
            for (int k = 0; k < 4; k++)
                buffer.addVertex(pose, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(-1).setUv(uv[k][0], uv[k][1]).setLight(0xF000F0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.debugShape != null)
            collector.submitShapeOutline(poseStack, state.debugShape, net.minecraft.client.renderer.rendertype.RenderTypes.lines(), 0xFFFF0000, 3.0F, false);
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(-state.yaw));
        // one straight log: raised evenly, or tilted about the bottom edge of the end on the ground so the lifted end's
        // underside comes up to LIFT_HEIGHT (the collision boxes step along the same line, FelledLogEntity#liftOffset)
        float h = FelledLogEntity.LIFT_HEIGHT;
        if (state.bothUp) {
            poseStack.translate(0, h, 0);
        } else if (state.oneUp && state.length > 0) {
            float pivot = (state.rootUp ? 1 : -1) * state.length / 2.0F; // the root end is at local -z
            poseStack.translate(0, 0, pivot);
            poseStack.rotate(Axis.XP.rotation((float) Math.asin(Math.min(1.0F, h / state.length)) * (state.rootUp ? 1 : -1)));
            poseStack.translate(0, 0, -pivot);
        }
        for (int i = 0; i < state.blocks.size(); i++) {
            Vec3 o = state.offsets.get(i);
            poseStack.pushPose();
            poseStack.translate(o.x, o.y, o.z);
            collector.submitMovingBlock(poseStack, state.blocks.get(i), state.outlineColor);
            if (i == state.cracked) {
                // a hair larger than the block: drawn in the same pass as the log, at equal depth it would lose to it
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(1.004F, 1.004F, 1.004F);
                poseStack.translate(-0.5, -0.5, -0.5);
                // vanilla's crumbling type multiplies the colour below by twice the texture (grey leaves it, the cracks darken);
                // twice over, the cracks read on dark bark while the rest still keeps its colour
                var crumbling = net.minecraft.client.resources.model.ModelBakery.DESTROY_TYPES.get(state.crackStage);
                collector.submitCustomGeometry(poseStack, crumbling, FelledLogRenderer::crackCube);
                collector.submitCustomGeometry(poseStack, crumbling, FelledLogRenderer::crackCube);
            }
            if (i == state.kerf) {
                // the saw cut across the seam at the far face of this block: a dark line over the top and down both sides
                float depth = Math.max(0.08F, state.kerfDepth);
                collector.submitCustomGeometry(poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(KERF),
                        (pose, buffer) -> kerf(pose, buffer, depth, state.lightCoords));
            }
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
