package com.mechanical.client.wildlife.river;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Otter: long and low, a sleek tube of a body with heavier hips, a broad flat head on a thick neck, small ears,
 * whiskers, short legs with webbed hind feet and a thick tail tapering in two pieces. Bounds along on land with the
 * back arching; swimming it undulates from the head down through the tail, legs folded back. Eating, it sits up on its
 * haunches with the fish in its mouth.
 * Geometry: {@link RiverGeometry#otter}; texture textures/entity/wildlife/otter.png (64x64) and its skinned carcass skin,
 * both generated with the boxes by scripts/gen_owarm_river_models.py.
 */
public class OtterModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "otter"), "main");
    private static final float TAIL_X = -0.25F, TIP_X = -0.1F;

    private final ModelPart body, tail, tip, head, fish;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public OtterModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        tail = body.getChild("tail");
        tip = tail.getChild("tail_tip");
        head = body.getChild("head");
        fish = head.getChild("fish");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return RiverGeometry.otter();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        fish.visible = RiverFaunaClient.flag(state, RiverFaunaClient.FISH);

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.15F;
            tail.xRot = 0.1F; // straight up past the gambrel, the stiff tail
            tip.xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            tail.xRot = 0.05F;
            tip.xRot = 0.0F;
            return;
        }
        if (state.dozing) {
            // curled up nose to tail
            body.y += 1.8F;
            for (ModelPart leg : legs) leg.y += 1.6F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -1.4F;
            rightHindLeg.xRot = leftHindLeg.xRot = 1.4F;
            head.yRot = 0.7F;
            head.xRot = 0.2F;
            tail.xRot = 0.0F;
            tail.yRot = 0.9F;
            tip.yRot = 0.8F;
            body.y += Mth.sin(t * 0.08F) * 0.08F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.4F);
        float phase = state.walkAnimationPos;
        if (RiverFaunaClient.flag(state, RiverFaunaClient.SWIMMING)) {
            // undulating: a wave running from the head back through the tail, legs folded along the body
            float w = t * 0.4F;
            head.yRot = Mth.sin(w) * 0.15F + Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
            head.xRot = -0.15F;
            body.yRot = Mth.sin(w - 0.8F) * 0.06F;
            tail.xRot = 0.0F;
            tail.yRot = Mth.sin(w - 1.6F) * 0.35F;
            tip.xRot = 0.0F;
            tip.yRot = Mth.sin(w - 2.4F) * 0.45F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = 1.3F;
            rightHindLeg.xRot = 1.2F + Mth.sin(w) * 0.3F;
            leftHindLeg.xRot = 1.2F - Mth.sin(w) * 0.3F;
            return;
        }

        if (RiverFaunaClient.flag(state, RiverFaunaClient.EATING)) {
            // sat up on its haunches, chewing the fish held in the forepaws
            body.xRot = -0.9F;
            body.y += 1.0F;
            body.z += 1.5F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -1.6F;
            rightFrontLeg.y -= 4.5F;
            leftFrontLeg.y -= 4.5F;
            rightFrontLeg.z += 1.5F;
            leftFrontLeg.z += 1.5F;
            head.xRot = 0.9F + Mth.sin(t * 1.4F) * 0.08F;
            tail.xRot = 0.9F; // flat on the ground behind
            tip.xRot = 0.0F;
            return;
        }

        // bounding: front and hind pairs together, the back arching as it gathers
        float bound = Mth.sin(phase * 0.9F);
        rightFrontLeg.xRot = leftFrontLeg.xRot = bound * 1.0F * speed;
        rightHindLeg.xRot = leftHindLeg.xRot = -bound * 1.0F * speed;
        body.xRot = Mth.cos(phase * 0.9F) * 0.08F * speed;
        body.y -= Math.max(0.0F, Mth.cos(phase * 0.9F)) * 0.8F * speed;
        head.yRot = Mth.clamp(state.yRot, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        tail.xRot = TAIL_X - bound * 0.15F * speed;
        tip.xRot = TIP_X;
        tail.yRot = Mth.sin(t * 0.15F) * 0.08F;

        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            // nose down, sniffing along the bank
            head.xRot = Mth.lerp(graze, head.xRot, 0.7F + Mth.sin(t * 1.5F) * 0.05F);
        }
    }
}
