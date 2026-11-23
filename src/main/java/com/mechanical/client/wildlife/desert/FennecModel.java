package com.mechanical.client.wildlife.desert;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Fennec Fox (wild and tamed): a small slim body on thin legs, a small head with a pointed muzzle, enormous upright ears
 * and a bushy, black-tipped tail carried low. Trots on diagonal pairs; the ears swivel on their own; asleep (or a tamed
 * one sitting) it curls up with the tail round its nose; a carcass lies limp. Geometry and the
 * 64x64 skin (textures/entity/wildlife/fennec_fox.png) come from scripts/gen_desert_models.py (DesertGeometry).
 */
public class FennecModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "fennec_fox"), "main");
    private static final float EAR_Z = 0.3F, TAIL_X = -0.9F;
    private final ModelPart body, head, rightEar, leftEar, tail, tailTip;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;

    public FennecModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        rightEar = head.getChild("right_ear");
        leftEar = head.getChild("left_ear");
        tail = body.getChild("tail");
        tailTip = tail.getChild("tail_tip");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return DesertGeometry.fennec();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);
        ModelPart[] front = {rightFrontLeg, leftFrontLeg}, hind = {rightHindLeg, leftHindLeg};
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            tail.xRot = -0.1F;
            tailTip.xRot = 0.0F;
            rightEar.zRot = -1.0F;
            leftEar.zRot = 1.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg}) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            rightEar.zRot = -1.1F;
            leftEar.zRot = 1.1F;
            tail.xRot = 0.0F;
            tail.yRot = 0.4F;
            return;
        }
        if (state.dozing) {
            // curled up: body down, legs tucked, head on the flank, the bushy tail round the nose, ears laid back
            float breathe = Mth.sin(t * 0.08F) * 0.12F;
            body.y += 4.0F + breathe;
            head.yRot = 1.1F;
            head.xRot = 0.3F;
            head.y += 0.8F;
            rightEar.xRot = leftEar.xRot = 0.9F;
            tail.xRot = -0.1F;
            tail.yRot = 1.4F;
            tailTip.yRot = 1.3F;
            tailTip.xRot = 0.0F;
            for (ModelPart leg : front) {
                leg.y = 22.5F;
                leg.xRot = -1.5F;
            }
            for (ModelPart leg : hind) {
                leg.y = 22.0F;
                leg.xRot = -1.5F;
                leg.z += 1.0F;
            }
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 1.0F;
        rightFrontLeg.xRot = leftHindLeg.xRot = Mth.cos(phase) * 1.1F * speed;
        leftFrontLeg.xRot = rightHindLeg.xRot = -Mth.cos(phase) * 1.1F * speed;
        body.y -= Mth.abs(Mth.sin(phase)) * 0.4F * speed;
        head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // the big ears turn on their own to every sound
        float l = Mth.sin(t * 0.05F + seed) > 0.9F ? Mth.sin(t * 0.7F) * 0.4F : 0.0F;
        float r = Mth.sin(t * 0.043F + seed + 2.0F) > 0.9F ? Mth.sin(t * 0.8F) * 0.4F : 0.0F;
        leftEar.yRot = l;
        rightEar.yRot = r;
        leftEar.xRot = rightEar.xRot = -0.1F + speed * 0.3F;
        tail.yRot = Mth.sin(t * 0.07F) * 0.2F + Mth.cos(phase) * 0.15F * speed;
        tail.xRot = TAIL_X + speed * 0.6F; // hangs low, held out behind at a run
        tailTip.yRot = Mth.sin(t * 0.07F - 0.8F) * 0.15F;
        // nosing about (grazing = sniffing the sand)
        head.xRot += state.grazeAmount * 0.8F;
        head.y += state.grazeAmount * 1.0F;
    }
}
