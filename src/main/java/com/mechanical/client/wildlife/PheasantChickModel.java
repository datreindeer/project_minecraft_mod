package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

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
 * Pheasant chick, built at baby size (rendered at scale 1, about 6 px tall): a round downy ball with a big head, a tiny
 * beak, stubby wing nubs, thin legs and a little tail tuft. Buff down with dark back stripes and an eye stripe.
 * Scurries with quick steps and a head bob, pecks when grazing, tilts its head while peeping, and flutters its wing
 * nubs frantically when airborne. Texture: textures/entity/wildlife/pheasant_chick.png, painted by
 * scripts/gen_small_models.py (with the geometry, SmallGeometry).
 */
public class PheasantChickModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "pheasant_chick"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public PheasantChickModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return SmallGeometry.pheasantChick(); // scripts/gen_small_models.py
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[0], new ModelPart[] {leftLeg, rightLeg});
            head.xRot = 1.0F; // flops down
            leftWing.xRot = rightWing.xRot = 3.0F; // wings fall open, hanging from the shoulders towards the ground
            leftWing.zRot = 0.6F;
            rightWing.zRot = -0.6F;
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart limb : new ModelPart[] {this.rightLeg, this.leftLeg, this.rightWing, this.leftWing}) {
                limb.zRot = CarcassRenderer.LIMB_SAG;
            }
            this.head.xRot += 0.6F; // neck limp
            return;
        }

        if (state.dozing) {
            // hunkered down: body (bottom 22.5) dropped onto y = 24, legs folded forward and pivots lowered so they sit
            // hidden inside the body, head sunk into the front of the body
            this.body.y += 1.5F;
            this.leftLeg.xRot = -1.5F;
            this.rightLeg.xRot = -1.5F;
            this.leftLeg.y += 1.3F;
            this.rightLeg.y += 1.3F;
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.y += 1.0F;
            this.head.z += 0.5F;
            return;
        }

        float t = state.ageInTicks;
        if (state.flying) {
            // a chick can only flutter: nubs beating at double the adult rate, body upright, head up, legs dangling
            float beat = Mth.sin(state.flap * 2.0F);
            float bob = 0.4F * Mth.cos(state.flap * 2.0F);
            this.leftWing.zRot = -0.9F - 0.7F * beat;
            this.rightWing.zRot = 0.9F + 0.7F * beat;
            this.body.xRot -= 0.25F;
            this.body.y += bob;
            this.head.xRot -= 0.15F;
            this.tail.xRot += 0.3F;
            this.leftLeg.xRot = 0.35F + 0.15F * beat;
            this.rightLeg.xRot = 0.35F - 0.15F * beat;
            this.leftLeg.y += bob;
            this.rightLeg.y += bob;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 1.0F; // short legs: quicker steps than the adult's 0.6662
        float graze = state.grazeAmount;
        float still = (1.0F - speed) * (1.0F - graze);

        // scurrying: fast alternating legs, a little hop per step, the head bobs forward and back
        this.leftLeg.xRot = Mth.cos(phase) * 1.1F * speed;
        this.rightLeg.xRot = Mth.cos(phase + Mth.PI) * 1.1F * speed;
        this.body.y -= Math.abs(Mth.sin(phase)) * 0.4F * speed;
        this.body.zRot = Mth.sin(phase) * 0.08F * speed;
        this.head.z += Mth.sin(phase * 2.0F) * 0.5F * speed;
        this.head.xRot += Mth.sin(phase * 2.0F) * 0.08F * speed;
        this.leftWing.zRot = -0.15F * speed;
        this.rightWing.zRot = 0.15F * speed;
        this.tail.xRot += Mth.cos(phase * 2.0F) * 0.15F * speed;

        // pecking: tips forward, quick jabs at the ground
        float peck = Math.max(0.0F, Mth.sin(t * 0.7F)) * 0.45F;
        this.body.xRot += 0.3F * graze;
        this.head.y += 0.5F * graze;
        this.head.xRot += (1.0F + peck) * graze;

        // idle peeping: the head cocks to one side now and then, chin-up peeps, the odd nub shuffle
        float s = Mth.sin(t * 0.05F);
        float cock = Math.signum(s) * Math.max(0.0F, Math.abs(s) * 3.0F - 2.0F);
        float peep = Math.max(0.0F, Mth.sin(t * 0.31F) * 4.0F - 3.0F);
        this.head.zRot += 0.35F * cock * still;
        this.head.xRot -= 0.2F * peep * still;
        float shuffle = Math.max(0.0F, Mth.sin(t * 0.09F) * 8.0F - 7.0F);
        this.leftWing.zRot -= 0.3F * shuffle * still;
        this.rightWing.zRot += 0.3F * shuffle * still;
        this.tail.yRot += 0.15F * Mth.sin(t * 0.13F);
    }
}
