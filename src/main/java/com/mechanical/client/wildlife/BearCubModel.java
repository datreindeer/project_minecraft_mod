package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Brown bear cub, built at baby size (rendered at scale 1): a round, fluffy 7x6x10 body, a big head with a short muzzle
 * and large round ears, chubby short legs, a stub tail. No hump yet.
 * Geometry and skin: scripts/gen_pred_models.py (PredGeometry.bearCub, textures/entity/wildlife/brown_bear_cub.png; the
 * cave bear cub shares this layer with a recoloured skin, the polar bear cub has its own geometry).
 */
public class BearCubModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "bear_cub"), "main");

    private static final float GAIT = 0.9F;

    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart[] legs;

    public BearCubModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.head = root.getChild("head");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return PredGeometry.bearCub();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.25F; // in line with the body, muzzle down
            leftEar.xRot = rightEar.xRot = 1.0F;
            tail.xRot = 2.6F; // the stub sticks back (+z): folds down over the rump
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot = 0.3F;
            return;
        }

        float age = state.ageInTicks;
        if (state.dozing) {
            // curled on its belly: body (bottom y 21) dropped onto y = 24, forelegs forward under the chin,
            // hind legs back; leg pivots 1.5 px (half a leg) above the ground
            this.body.y += 3.0F + Mth.sin(age * 0.09F) * 0.12F;
            for (ModelPart leg : legs) leg.y = 22.5F;
            this.rightFrontLeg.xRot = -Mth.HALF_PI;
            this.leftFrontLeg.xRot = -Mth.HALF_PI;
            this.rightHindLeg.xRot = Mth.HALF_PI;
            this.leftHindLeg.xRot = Mth.HALF_PI;
            // chin resting on the forepaws (top at y 21)
            this.head.y += 3.0F;
            this.head.xRot = 0.1F;
            this.head.yRot = -0.2F;
            this.rightEar.zRot -= 0.2F;
            this.leftEar.zRot += 0.2F;
            return;
        }

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float graze = state.grazeAmount;

        // bouncy little amble, the whole cub bobs
        float swing = Mth.cos(pos * GAIT) * speed * 1.1F;
        this.rightFrontLeg.xRot = swing;
        this.leftHindLeg.xRot = swing;
        this.leftFrontLeg.xRot = -swing;
        this.rightHindLeg.xRot = -swing;
        float bob = Mth.abs(Mth.sin(pos * GAIT)) * 0.7F * speed;
        this.body.y -= bob;
        this.head.y -= bob;
        this.body.zRot = Mth.sin(pos * GAIT) * 0.06F * speed;

        // curious: looks around a lot, head tilted
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * (1.0F - graze);
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * (1.0F - graze);
        this.head.zRot = Mth.sin(age * 0.05F) * 0.12F;
        float flick = Mth.sin(age * 0.13F) > 0.9F ? Mth.sin(age * 1.5F) * 0.25F : 0.0F;
        this.rightEar.zRot -= flick;
        this.leftEar.zRot += flick * 0.6F;

        if (graze > 0.0F) {
            this.head.xRot = Mth.lerp(graze, this.head.xRot, 0.9F);
            this.head.y += graze * 1.5F;
        }

        this.tail.zRot = Mth.sin(age * 0.25F) * 0.15F;
    }
}
