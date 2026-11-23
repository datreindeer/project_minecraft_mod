package com.mechanical.client.wildlife.cold;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Pika: an egg-shaped ball of fur with no tail to see, a round head with big rounded white-rimmed ears, a blunt snout with
 * whiskers, short legs. Built at twice its size (ColdGeometry.pika, from scripts/gen_cold_models.py, texture 64x64
 * textures/entity/wildlife/pika.png) and drawn at half by ColdClient, so it keeps its detail. Scurries in quick hops,
 * sits up on its haunches nibbling when it grazes, flattens into a ball asleep; limp as a carcass, hung by the hind legs.
 */
public class PikaModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "pika"), "main");

    private final ModelPart body, head, rightEar, leftEar;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public PikaModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        rightEar = head.getChild("right_ear");
        leftEar = head.getChild("left_ear");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.3F;
            rightEar.xRot = leftEar.xRot = 0.8F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.zRot = 0.2F;
            rightEar.zRot -= 0.4F;
            leftEar.zRot += 0.4F;
            return;
        }
        if (state.dozing) { // flattened into a ball, ears laid back, a slow breath
            body.y += 1.5F + Mth.sin(t * 0.1F) * 0.2F;
            head.y += 2.0F;
            head.xRot = 0.2F;
            rightEar.xRot = leftEar.xRot = -0.6F;
            for (ModelPart leg : legs) leg.y -= 1.0F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float phase = state.walkAnimationPos * 2.0F; // quick little hops
        float swing = Mth.cos(phase) * 0.9F * speed;
        rightFrontLeg.xRot = leftFrontLeg.xRot = swing;
        rightHindLeg.xRot = leftHindLeg.xRot = -swing;
        float bob = Math.abs(Mth.sin(phase)) * 1.2F * speed;
        body.y -= bob;
        head.y -= bob;
        body.xRot = Mth.sin(phase) * 0.08F * speed;
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -20.0F, 20.0F) * Mth.DEG_TO_RAD;
        float flick = Mth.sin(t * 0.09F) > 0.92F ? Mth.sin(t * 1.5F) * 0.25F : 0.0F;
        rightEar.xRot += flick;

        // nibbling: up on its haunches, head and forepaws raised, the jaws working
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            body.xRot = -0.55F * graze;
            body.y += 0.5F * graze;
            head.y -= 2.5F * graze;
            head.z += 1.5F * graze;
            head.xRot += graze * (Mth.sin(t * 1.6F) * 0.1F - 0.1F);
            rightFrontLeg.y -= 3.0F * graze;
            leftFrontLeg.y -= 3.0F * graze;
            rightFrontLeg.z += 1.5F * graze;
            leftFrontLeg.z += 1.5F * graze;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -0.9F * graze;
        }
    }
}
