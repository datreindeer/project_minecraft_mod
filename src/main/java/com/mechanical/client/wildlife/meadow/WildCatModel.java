package com.mechanical.client.wildlife.meadow;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Wild cat: a stocky body, broad head with wide-set rounded ears and a short muzzle, slim forelegs, deeper hind legs and
 * the thick blunt tail in two pieces, carried low. Walks with the cat's pacing gait and a gently swinging tail; stalking
 * it creeps with the belly near the ground, head low and level, tail straight out; it springs with legs stretched; asleep
 * it lies curled with the tail round its feet. The kitten is this model scaled down with its own texture.
 * Texture: textures/entity/wildlife/wild_cat.png, scripts/gen_small_models.py (with the geometry, SmallGeometry).
 */
public class WildCatModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wild_cat"), "main");

    private final ModelPart body, head, rightEar, leftEar, tail, tailTip;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;

    public WildCatModel(ModelPart root) {
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
        return com.mechanical.client.wildlife.SmallGeometry.wildCat(); // scripts/gen_small_models.py
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        ModelPart[] front = {rightFrontLeg, leftFrontLeg}, hind = {rightHindLeg, leftHindLeg};

        if (CarcassRenderer.isHung(state)) {
            // head down from the hook: legs hang, head and tail in line with the body, ears limp
            CarcassRenderer.hangLegs(front, hind);
            tail.xRot = 0.1F;
            tailTip.xRot = 0.0F;
            rightEar.zRot = -0.6F;
            leftEar.zRot = 0.6F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg}) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            tail.xRot = -0.3F;
            tail.yRot = 0.4F;
            return;
        }
        if (state.dozing) {
            // curled up: body down on the ground, legs tucked, head turned onto the flank, tail round the front
            float breathe = Mth.sin(t * 0.08F) * 0.15F;
            body.y += 6.0F + breathe;
            head.yRot = 1.1F;
            head.xRot = 0.3F;
            head.y += 1.0F;
            tail.xRot = -0.1F;
            tail.yRot = 1.3F;
            tailTip.yRot = 1.2F;
            tailTip.xRot = 0.0F;
            for (ModelPart leg : front) {
                leg.y = 22.0F;
                leg.xRot = -1.5F;
            }
            for (ModelPart leg : hind) {
                leg.y = 21.5F;
                leg.xRot = -1.5F;
                leg.z += 1.0F;
            }
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.9F;
        // pacing: both legs of a side together
        rightFrontLeg.xRot = rightHindLeg.xRot = Mth.cos(phase) * 1.0F * speed;
        leftFrontLeg.xRot = leftHindLeg.xRot = -Mth.cos(phase) * 1.0F * speed;
        head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        tail.yRot = Mth.sin(t * 0.08F) * 0.25F + Mth.cos(phase) * 0.15F * speed;
        tailTip.yRot = Mth.sin(t * 0.08F - 0.8F) * 0.2F;
        // ears twitch now and then
        float twitch = Mth.sin(t * 0.05F) > 0.97F ? 0.25F : 0.0F;
        rightEar.zRot -= twitch;

        if (state.leaping) {
            // the spring: forelegs reaching out ahead, hind legs pushed back, tail out straight
            rightFrontLeg.xRot = leftFrontLeg.xRot = -1.3F;
            rightHindLeg.xRot = leftHindLeg.xRot = 1.1F;
            tail.xRot = -0.2F;
            return;
        }
        if (state.crouching) {
            // stalking: belly near the ground, legs bent under, head low and level, tail out behind and twitching at the tip
            body.y += 3.5F;
            for (ModelPart leg : front) {
                leg.y += 3.0F;
                leg.xRot = leg.xRot * 0.4F - 0.6F;
            }
            for (ModelPart leg : hind) {
                leg.y += 3.5F;
                leg.xRot = leg.xRot * 0.4F + 0.7F;
            }
            head.xRot = 0.15F;
            tail.xRot = -0.25F;
            tail.yRot = 0.0F;
            tailTip.yRot = Mth.sin(t * 0.6F) * 0.35F;
            rightEar.zRot = -0.5F;
            leftEar.zRot = 0.5F;
            return;
        }
        // sniffing the ground (grazing = nosing about)
        head.xRot += state.grazeAmount * 0.7F;
        head.y += state.grazeAmount * 1.0F;
    }
}
