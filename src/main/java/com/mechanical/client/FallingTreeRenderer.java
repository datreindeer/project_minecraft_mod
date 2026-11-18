package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;

import com.mechanical.FallingTreeEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/** Draws the tree's blocks rotating around the stump, accelerating like a real fall. */
public class FallingTreeRenderer extends EntityRenderer<FallingTreeEntity, FallingTreeRenderer.State> {
    public static class State extends EntityRenderState {
        final List<MovingBlockRenderState> blocks = new ArrayList<>();
        final List<BlockPos> offsets = new ArrayList<>();
        Direction direction = Direction.NORTH;
        float angle;
    }

    public FallingTreeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FallingTreeEntity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        return true; // the hitbox is one block but the tree is huge; there are only ever a few of these
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FallingTreeEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float t = Mth.clamp((entity.tickCount + partialTicks) / FallingTreeEntity.FALL_TICKS, 0.0F, 1.0F);
        state.angle = t * t * Mth.HALF_PI;
        state.direction = entity.direction();
        state.offsets.clear();
        state.offsets.addAll(entity.offsets());
        state.blocks.clear();
        BlockPos base = entity.blockPosition();
        // The tree's old positions were solid a moment ago and are still being relit, so sampling light there gives
        // dark patches. Light the whole tree from one already-lit spot: beside the stump on the feller's side, or above it.
        BlockPos lightPos = base;
        if (entity.level() instanceof ClientLevel level) {
            BlockPos beside = base.relative(entity.direction().getOpposite()).above();
            BlockPos above = base.above(2);
            lightPos = level.getRawBrightness(beside, 0) >= level.getRawBrightness(above, 0) ? beside : above;
        }
        for (int i = 0; i < entity.offsets().size(); i++) {
            MovingBlockRenderState block = new MovingBlockRenderState();
            BlockPos pos = base.offset(entity.offsets().get(i));
            block.blockState = entity.states().get(i);
            block.blockPos = lightPos; // also where getBlockState reports the block, so no faces get culled against neighbours
            block.randomSeedPos = pos;
            if (entity.level() instanceof ClientLevel level) {
                block.biome = level.getBiome(pos);
                block.cardinalLighting = level.cardinalLighting();
                block.lightEngine = level.getLightEngine();
            }
            state.blocks.add(block);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // rotating about (dz, 0, -dx) tips +Y towards the fall direction
        poseStack.rotate(new Quaternionf().rotationAxis(state.angle, state.direction.getStepZ(), 0, -state.direction.getStepX()));
        for (int i = 0; i < state.blocks.size(); i++) {
            BlockPos o = state.offsets.get(i);
            poseStack.pushPose();
            poseStack.translate(o.getX() - 0.5, o.getY(), o.getZ() - 0.5);
            collector.submitMovingBlock(poseStack, state.blocks.get(i), state.outlineColor);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
