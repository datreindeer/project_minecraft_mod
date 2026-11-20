package com.mechanical.wildlife.mountain.client;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.LyingPose;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Shared animation for the mountain group's horned grazers (IbexModel, ChamoisModel, SaigaModel), after MouflonModel.
 * Parts: body (with tail and neck; the neck has the head; the head has left_ear, right_ear, horns and small_horns - one of
 * the two horn sets shows, by {@code showsHorns}) and four legs, each with a "lower" child. Poses: walk on diagonal
 * pairs, bounding gallop at speed, grazing, ears flicking, butting (the ibex rut: head low, horns forward, rearing a
 * little), asleep lying down (LyingPose), limp as a carcass, and head down on the Hanging Rack.
 */
public abstract class BovidModel extends EntityModel<WildAnimalRenderState> {
    private static final float DEG = Mth.DEG_TO_RAD;
    protected final ModelPart body, neck, head, horns, smallHorns, leftEar, rightEar, tail;
    protected final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;
    private final ModelPart[] legs;
    /** Neck and head resting pitch, how far the body sinks lying down (px) and where the folded legs pivot. */
    private final float neckX, headX, lieDrop, lieLegY, earZ;

    protected BovidModel(ModelPart root, float neckX, float headX, float lieDrop, float lieLegY, float earZ) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.horns = head.getChild("horns");
        this.smallHorns = head.getChild("small_horns");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.tail = body.getChild("tail");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.legs = new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg};
        this.neckX = neckX;
        this.headX = headX;
        this.lieDrop = lieDrop;
        this.lieLegY = lieLegY;
        this.earZ = earZ;
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        horns.visible = state.showsHorns;
        smallHorns.visible = !state.showsHorns;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // straight down in line with the body
            head.xRot = -1.25F; // hanging limp below it
            leftEar.yRot = 1.0F;
            rightEar.yRot = -1.0F;
            tail.xRot = -1.9F; // flops over the back
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            neck.xRot = 1.0F;
            neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F;
            head.xRot = 0.2F;
            head.yRot = state.yRot * DEG * 0.5F;
            leftEar.zRot = 0.9F;
            rightEar.zRot = -0.9F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);
        if (state.dozing) {
            LyingPose.apply(body, lieDrop, lieLegY, age, new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.2F;
            neck.yRot = 0.45F;
            head.xRot = -0.5F;
            head.yRot = 0.25F;
            head.zRot = 0.15F;
            leftEar.zRot = 0.4F;
            rightEar.zRot = -0.4F;
            return;
        }

        // nimble walk on diagonal pairs; bounding at a run (hind legs push together, the body pitches)
        float speed = Math.min(state.walkAnimationSpeed, 1.3F);
        float run = Mth.clamp((speed - 0.45F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.7F;
        float amp = Math.min(1.0F, speed * 1.2F);
        float front = Mth.cos(phase) * amp;
        float hind = Mth.cos(phase + Mth.PI * (1.0F - run * 0.75F)) * amp;
        leftFrontLeg.xRot = front;
        rightFrontLeg.xRot = -front * (1.0F - run) + front * run * 0.85F;
        rightHindLeg.xRot = hind;
        leftHindLeg.xRot = -hind * (1.0F - run) + hind * run * 0.85F;
        for (ModelPart leg : new ModelPart[] {leftFrontLeg, rightFrontLeg}) leg.getChild("lower").xRot = Math.max(0.0F, -leg.xRot) * 1.2F;
        for (ModelPart leg : new ModelPart[] {leftHindLeg, rightHindLeg}) leg.getChild("lower").xRot = Math.max(0.0F, leg.xRot) * 0.5F;
        body.y -= Mth.abs(Mth.sin(phase)) * (0.25F * speed + 0.8F * run);
        body.xRot = Mth.sin(phase) * 0.09F * run;

        float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * DEG;
        float pitch = state.xRot * DEG;
        float graze = state.grazeAmount;
        float nx = neckX - run * 0.15F + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed;
        float hx = headX + pitch * 0.7F;
        if (graze > 0.0F) {
            nx = Mth.lerp(graze, nx, neckX + 1.75F);
            hx = Mth.lerp(graze, hx, headX - 0.8F + Mth.sin(age * 0.7F) * 0.06F); // nibbling
            neck.y += graze * 1.5F;
            yaw *= 1.0F - graze;
        }
        if (state.charging) {
            // butting: neck stretched forward and low, forehead down so the horns point at the rival; a little rear
            float rear = Math.max(0.0F, Mth.sin(age * 0.18F + seed)) * 0.25F;
            nx = neckX + 0.75F;
            hx = headX + 0.9F;
            body.xRot -= rear;
            body.y -= rear * 4.0F;
            yaw *= 0.2F;
        }
        neck.xRot = nx;
        neck.yRot = yaw * 0.4F;
        head.xRot = hx;
        head.yRot = yaw * 0.6F;

        // ears: quick flicks, laid back at a run
        float leftFlick = pulse(age * 0.055F + seed), rightFlick = pulse(age * 0.047F + seed + 2.0F);
        leftEar.zRot = -earZ + leftFlick * Mth.sin(age * 1.8F) * 0.4F;
        rightEar.zRot = earZ - rightFlick * Mth.sin(age * 2.0F) * 0.4F;
        leftEar.yRot = 0.3F + run * 0.5F;
        rightEar.yRot = -0.3F - run * 0.5F;

        // short tail: twitches, flicks up when fleeing
        float wag = pulse(age * 0.07F + seed * 1.3F);
        tail.zRot = wag * Mth.sin(age * 1.5F) * 0.5F;
        tail.xRot = 0.3F + run * 1.0F;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }
}
