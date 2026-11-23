package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mouflon, ram and ewe in one model: a compact short-coated sheep on slim legs (back ~16 px), short tail. The ram shows
 * the heavy horns curling back, down and forward around the ear; the ewe shows two short spikes instead.
 * Geometry and skins (mouflon_{ram,ewe}.png): HerdGeometry.mouflon from scripts/gen_herd_models.py; the ram has the pale
 * saddle patch, dark ruff and flank line, both have white muzzle, belly, rump and socks.
 */
public class MouflonModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "mouflon"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float NECK_X = 0.55F;
    private static final float HEAD_X = -0.1F;
    private static final float GRAZE_NECK_X = 2.3F;
    private static final float GRAZE_HEAD_X = -0.9F;
    private static final float EAR_Z = 0.3F;
    private static final float LIE_DROP = 8.0F, LIE_LEG_Y = 22.8F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart ramHorns;
    private final ModelPart eweHorns;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public MouflonModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.ramHorns = this.head.getChild("ram_horns");
        this.eweHorns = this.head.getChild("ewe_horns");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.tail = this.body.getChild("tail");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.legs = new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return HerdGeometry.mouflon();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.ramHorns.visible = state.showsHorns;
        this.eweHorns.visible = !state.showsHorns;

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
            for (ModelPart leg : this.legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.xRot = 1.0F;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F;
            this.head.xRot = 0.2F;
            this.head.yRot = state.yRot * DEG * 0.5F;
            this.leftEar.zRot = 0.9F;
            this.rightEar.zRot = -0.9F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            // chin resting low, head turned a little to one side
            this.neck.xRot = 1.25F;
            this.neck.yRot = 0.4F;
            this.head.xRot = -0.55F;
            this.head.yRot = 0.25F;
            this.head.zRot = 0.15F;
            this.leftEar.zRot = 0.4F;
            this.rightEar.zRot = -0.4F;
            return;
        }

        // Nimble walk on diagonal pairs; bounding at a run (hind legs push together, body pitches).
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float run = Mth.clamp((speed - 0.45F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.7F;
        float amp = Math.min(0.95F, speed * 1.2F);
        float front = Mth.cos(phase) * amp;
        float hind = Mth.cos(phase + Mth.PI * (1.0F - run * 0.75F)) * amp;
        this.leftFrontLeg.xRot = front;
        this.rightFrontLeg.xRot = -front * (1.0F - run) + front * run * 0.85F;
        this.rightHindLeg.xRot = hind;
        this.leftHindLeg.xRot = -hind * (1.0F - run) + hind * run * 0.85F;
        this.leftFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.leftFrontLeg.xRot) * 1.2F;
        this.rightFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.rightFrontLeg.xRot) * 1.2F;
        this.leftHindLeg.getChild("lower").xRot = Math.max(0.0F, this.leftHindLeg.xRot) * 0.5F;
        this.rightHindLeg.getChild("lower").xRot = Math.max(0.0F, this.rightHindLeg.xRot) * 0.5F;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.25F * speed + 0.7F * run);
        this.body.xRot = Mth.sin(phase) * 0.08F * run;

        float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * DEG;
        float pitch = state.xRot * DEG;
        float graze = state.grazeAmount;
        float neckX = NECK_X - run * 0.15F + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed;
        float headX = HEAD_X + pitch * 0.7F;
        if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.7F) * 0.06F); // nibbling
            this.neck.y += graze * 1.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.4F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.6F;

        // Ears: quick flicks, back when running.
        float leftFlick = pulse(age * 0.055F + seed);
        float rightFlick = pulse(age * 0.047F + seed + 2.0F);
        this.leftEar.zRot = -EAR_Z + leftFlick * Mth.sin(age * 1.8F) * 0.4F;
        this.rightEar.zRot = EAR_Z - rightFlick * Mth.sin(age * 2.0F) * 0.4F;
        this.leftEar.yRot = 0.3F + run * 0.5F;
        this.rightEar.yRot = -0.3F - run * 0.5F;

        // Short tail: wags now and then, flicked up when fleeing.
        float wag = pulse(age * 0.07F + seed * 1.3F);
        this.tail.zRot = wag * Mth.sin(age * 1.5F) * 0.5F;
        this.tail.xRot = 0.3F + run * 1.0F;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    private static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }
}
