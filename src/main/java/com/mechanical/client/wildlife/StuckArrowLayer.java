package com.mechanical.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Arrows stuck in a wild animal (see ArrowWounds), like vanilla's arrow layer for players but for any model: each arrow
 * sits in a random cube of the model (fixed per animal), following that part's animation.
 */
public class StuckArrowLayer extends RenderLayer<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
    private final ArrowModel arrow;
    private final ArrowRenderState arrowState = new ArrowRenderState();

    public StuckArrowLayer(RenderLayerParent<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> parent, EntityRendererProvider.Context context) {
        super(parent);
        this.arrow = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, WildAnimalRenderState state, float yRot, float xRot) {
        if (state.arrowCount <= 0) return;
        int[] cubes = {0};
        getParentModel().root().visit(new PoseStack(), (pose, path, index, cube) -> cubes[0]++);
        if (cubes[0] == 0) return;
        RandomSource random = RandomSource.createThreadLocalInstance(state.entityId);
        for (int i = 0; i < state.arrowCount; i++) {
            int pick = random.nextInt(cubes[0]);
            float mx = random.nextFloat(), my = random.nextFloat(), mz = random.nextFloat();
            int[] n = {0};
            getParentModel().root().visit(poseStack, (pose, path, index, cube) -> {
                if (n[0]++ != pick) return;
                PoseStack at = new PoseStack();
                at.last().set(pose);
                at.translate(Mth.lerp(mx, cube.minX, cube.maxX) / 16.0F, Mth.lerp(my, cube.minY, cube.maxY) / 16.0F,
                        Mth.lerp(mz, cube.minZ, cube.maxZ) / 16.0F);
                // point the shaft out of the body, away from the cube's centre
                float dx = -(mx * 2 - 1), dy = -(my * 2 - 1), dz = -(mz * 2 - 1);
                at.rotateDegrees(Axis.YP, (float) Math.toDegrees(Math.atan2(dx, dz)) - 90.0F);
                at.rotateDegrees(Axis.ZP, (float) Math.toDegrees(Math.atan2(dy, Mth.sqrt(dx * dx + dz * dz))));
                collector.submitModel(arrow, arrowState, at, TippableArrowRenderer.NORMAL_ARROW_LOCATION, lightCoords,
                        OverlayTexture.NO_OVERLAY, state.outlineColor);
            });
        }
    }
}
