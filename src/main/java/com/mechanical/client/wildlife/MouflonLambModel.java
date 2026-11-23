package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mouflon lamb, built at baby size: small woolly body on thin legs, a round hornless head with sideways ears.
 * Back ~10 px.
 * Geometry and skin: HerdGeometry.mouflonLamb and the textures from scripts/gen_herd_models.py, built at TWICE baby size
 * (drawn at half by the renderer's baby scale), so the pixel offsets below are doubled.
 */
public class MouflonLambModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "mouflon_lamb"), "main");

    private static final float NECK_X = 0.5F;
    private static final float HEAD_X = -0.2F;
    private static final float GRAZE_NECK_X = 2.2F;
    private static final float GRAZE_HEAD_X = -0.9F;
    private static final float LIE_DROP = 11.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;

    public MouflonLambModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.tail = this.body.getChild("tail");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return HerdGeometry.mouflonLamb();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        float pitch = state.xRot * Mth.DEG_TO_RAD;
        float age = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // neck straight down in line with the body (it rises from the chest when standing)
            head.xRot = -1.25F; // head hanging limp below it, nose a little towards the belly
            leftEar.yRot = 1.0F; // sideways ears swung down
            rightEar.yRot = -1.0F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            this.leftFrontLeg.zRot = this.rightFrontLeg.zRot = this.leftHindLeg.zRot = this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F;
            this.head.yRot = yaw * 0.5F;
            return;
        }

        if (state.dozing) {
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 1.3F;
            this.neck.yRot = 0.5F;
            this.head.xRot = -0.6F;
            this.head.yRot = 0.3F;
            this.head.zRot = 0.15F;
            this.leftEar.zRot = 0.5F;
            this.rightEar.zRot = -0.5F;
            return;
        }

        // springy little steps; a running lamb bounces stiff-legged ("pronking")
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.8F;
        float swing = Mth.cos(phase) * Math.min(0.9F, speed * 1.3F) * (1.0F - run * 0.6F);
        this.leftFrontLeg.xRot = swing;
        this.rightHindLeg.xRot = swing;
        this.rightFrontLeg.xRot = -swing;
        this.leftHindLeg.xRot = -swing;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.5F * speed + 2.4F * run);
        for (ModelPart leg : new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg}) {
            leg.y -= Mth.abs(Mth.sin(phase)) * 2.4F * run; // the whole lamb leaves the ground
        }

        float graze = state.grazeAmount;
        float look = 1.0F - graze;
        this.neck.xRot = Mth.lerp(graze, NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.4F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.7F, GRAZE_HEAD_X + Mth.sin(age * 0.7F) * 0.06F);
        this.head.yRot = yaw * 0.6F * look;
        this.neck.y += graze * 2.0F;

        float tl = (age + 9.0F) % 47.0F;
        if (tl < 6.0F) this.leftEar.zRot -= Mth.sin(tl / 6.0F * Mth.PI) * 0.5F;
        float tr = age % 59.0F;
        if (tr < 6.0F) this.rightEar.zRot += Mth.sin(tr / 6.0F * Mth.PI) * 0.5F;

        float tt = (age + 13.0F) % 30.0F;
        this.tail.zRot = tt < 8.0F ? Mth.sin(tt * 2.2F) * 0.6F * Mth.sin(tt / 8.0F * Mth.PI) : 0.0F;
        this.tail.xRot += run * 0.8F;
    }
}
