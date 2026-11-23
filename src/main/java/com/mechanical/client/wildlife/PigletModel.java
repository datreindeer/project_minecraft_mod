package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Wild boar piglet ("humbug"), a round body on stubby legs, a big head
 * with a short snout and disc nose, small ears, a thin tail. No tusks, no crest.
 * Geometry and skin: HerdGeometry.piglet and the textures from scripts/gen_herd_models.py, built at TWICE baby size
 * (drawn at half by the renderer's baby scale), so the pixel offsets below are doubled.
 */
public class PigletModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "piglet"), "main");

    private static final float HEAD_X_ROT = 0.1F;
    private static final float EAR_X_ROT = -0.25F;
    private static final float EAR_Z_ROT = 0.6F;
    private static final float TAIL_X_ROT = 0.3F;
    /** Little legs scurry: quicker than the adult boar's 0.9. */
    private static final float GAIT = 1.3F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart tail;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public PigletModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.snout = this.head.getChild("snout");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.tail = this.body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return HerdGeometry.piglet();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.25F; // in line with the body, snout down
            leftEar.xRot = rightEar.xRot = 1.0F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg}) leg.zRot = CarcassRenderer.LIMB_SAG;
            // the carcass renderer puts a head yaw in yRot that points the head at the ground
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += 0.2F;
            this.rightEar.xRot += 0.5F;
            this.leftEar.xRot += 0.5F;
            this.tail.xRot = 1.0F;
            return;
        }

        if (state.dozing) {
            // lying on its belly: body (bottom 19) dropped onto y = 24, legs tucked under it (front back, hind
            // forward) with the pivots lowered so they touch the ground; head level, chin on the ground
            this.body.y += 5.0F;
            for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg}) leg.y += 3.0F;
            this.rightFrontLeg.xRot = 1.4F;
            this.leftFrontLeg.xRot = 1.4F;
            this.rightHindLeg.xRot = -1.4F;
            this.leftHindLeg.xRot = -1.4F;
            this.head.xRot = 0.0F;
            this.head.y += 1.0F;
            this.rightEar.xRot += 0.3F;
            this.leftEar.xRot += 0.3F;
            return;
        }

        float age = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float graze = state.grazeAmount;

        // Scurrying trot: diagonal pairs, quick short strides; the stubby legs can swing wide without looking odd.
        float swing = Mth.cos(pos * GAIT) * speed * (0.6F + 0.25F * speed);
        this.rightFrontLeg.xRot = swing;
        this.leftHindLeg.xRot = swing;
        this.leftFrontLeg.xRot = -swing;
        this.rightHindLeg.xRot = -swing;
        // bouncy: the body bobs and rocks a little every step
        float step = Mth.abs(Mth.sin(pos * GAIT));
        this.body.y -= step * 1.0F * speed;
        this.body.zRot = Mth.sin(pos * GAIT) * 0.05F * speed;
        this.head.xRot += Mth.sin(pos * GAIT * 2.0F) * 0.08F * speed;

        // Head look: a piglet's neck is short but supple.
        this.head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * (1.0F - graze);
        this.head.xRot += Mth.clamp(state.xRot, -25.0F, 25.0F) * 0.6F * Mth.DEG_TO_RAD * (1.0F - graze);
        // idle: curious sniffing, ear flicks; ears flap a little on the move
        this.snout.y -= Mth.abs(Mth.sin(age * 0.5F)) * 0.3F * (1.0F - speed) * (Mth.sin(age * 0.05F) > 0.3F ? 1.0F : 0.0F);
        float flickR = Mth.sin(age * 0.13F) > 0.92F ? Mth.sin(age * 1.9F) * 0.3F : 0.0F;
        float flickL = Mth.sin(age * 0.13F + 2.0F) > 0.92F ? Mth.sin(age * 1.9F) * 0.3F : 0.0F;
        float flap = Mth.sin(pos * GAIT * 2.0F) * 0.15F * speed;
        this.rightEar.zRot -= flickR + flap;
        this.leftEar.zRot += flickL + flap;

        // Rooting: snout down in the earth, snuffling fast and nosing forward and back.
        if (graze > 0.0F) {
            float dig = Mth.sin(age * 0.7F);
            this.head.xRot = Mth.lerp(graze, this.head.xRot, 0.75F + dig * 0.1F);
            this.head.y += graze * 1.0F;
            this.head.z -= graze * (0.6F + Mth.cos(age * 0.7F) * 0.6F);
            this.snout.xRot -= graze * (0.1F + Mth.sin(age * 1.6F) * 0.1F);
            this.snout.yRot = graze * Mth.sin(age * 1.1F) * 0.12F;
        }

        // Tail: never still, a quick wiggle, faster when trotting.
        this.tail.zRot = Mth.sin(age * 0.6F) * 0.3F + Mth.sin(pos * GAIT) * 0.4F * speed;
        this.tail.xRot = TAIL_X_ROT + step * 0.2F * speed;
    }
}
