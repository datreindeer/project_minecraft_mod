package com.mechanical.client.wildlife.coast;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Pike: long torpedo body, a flat duck-bill snout on a long head, the dorsal and anal fins set far back near the forked
 * tail (the pike's mark), small pectoral fins. Hangs nearly still with just the fins fanning; the tail beats when it
 * swims and lashes when it darts. Rendered at 0.8x. Parts, boxes and pike.png (64x64): {@link CoastGeometry#pike()} from
 * scripts/gen_owarm_coast_models.py.
 */
public class PikeModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "pike"), "main");
    private static final float FIN_Z = -0.4F; // pectorals droop (matches the generator)

    private final ModelPart tail, caudal, rightFin, leftFin;

    public PikeModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        tail = body.getChild("tail");
        caudal = tail.getChild("caudal");
        rightFin = body.getChild("right_fin");
        leftFin = body.getChild("left_fin");
    }

    public static LayerDefinition createBodyLayer() {
        return CoastGeometry.pike();
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return; // limp
        float t = state.ageInTicks;
        float beat = state.isInWater ? 0.15F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.8F : 1.4F;
        float rate = state.isInWater ? 0.35F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.6F : 1.7F;
        tail.yRot = -Mth.sin(t * rate) * 0.35F * beat;
        caudal.yRot = -Mth.sin(t * rate - 0.9F) * 0.4F * beat;
        // the pectorals keep fanning even when it hangs still
        rightFin.zRot = FIN_Z + Mth.sin(t * 0.4F) * 0.3F;
        leftFin.zRot = -FIN_Z - Mth.sin(t * 0.4F) * 0.3F;
    }
}
