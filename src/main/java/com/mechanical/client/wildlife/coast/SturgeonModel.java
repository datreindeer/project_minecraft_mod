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
 * Sturgeon: a long armoured body with a ridge of bony scutes down the back, a wedge head running out into a long
 * pointed snout with four barbels underneath, a narrow tail stock and the shark-like tail whose upper lobe sweeps up and
 * back. Slow, deep tail beats; the pectoral fins held out like wings. Rendered at 0.85x. Parts, boxes and skins
 * (sturgeon.png, 64x64, and the skinned one): {@link CoastGeometry#sturgeon()} from scripts/gen_owarm_coast_models.py.
 */
public class SturgeonModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "sturgeon"), "main");
    private static final float FIN_Z = -0.2F; // pectorals held out, slightly down (matches the generator)

    private final ModelPart head, tail, tailTip, rightFin, leftFin;

    public SturgeonModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("tail");
        tailTip = tail.getChild("tail_tip");
        rightFin = body.getChild("right_fin");
        leftFin = body.getChild("left_fin");
    }

    public static LayerDefinition createBodyLayer() {
        return CoastGeometry.sturgeon();
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isHung(state)) {
            // hung by the tail: the fins fall back along the body
            rightFin.zRot = 1.2F;
            leftFin.zRot = -1.2F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            rightFin.zRot = leftFin.zRot = CarcassRenderer.LIMB_SAG;
            return;
        }
        float t = state.ageInTicks;
        float beat = state.isInWater ? 0.25F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.7F : 1.2F;
        float rate = state.isInWater ? 0.18F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.3F : 1.2F;
        tail.yRot = -Mth.sin(t * rate) * 0.2F * beat;
        tailTip.yRot = -Mth.sin(t * rate - 1.0F) * 0.35F * beat;
        head.yRot = Mth.sin(t * rate) * 0.04F * beat;
        rightFin.zRot = FIN_Z + Mth.sin(t * 0.12F) * 0.08F;
        leftFin.zRot = -FIN_Z - Mth.sin(t * 0.12F) * 0.08F;
    }
}
