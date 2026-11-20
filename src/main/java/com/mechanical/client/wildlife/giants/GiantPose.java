package com.mechanical.client.wildlife.giants;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Shared poses for the giants' models. Legs must have a "lower" child (knee / hock). */
final class GiantPose {
    static final float DEG = Mth.DEG_TO_RAD;

    private GiantPose() {}

    /** Night sleep: body sunk {@code drop} px onto the ground, legs folded under it at pivot height {@code legY}, slow breathing (as the wildlife LyingPose). */
    static void lie(ModelPart body, float drop, float legY, float age, ModelPart[] front, ModelPart[] hind) {
        body.y += drop + Mth.sin(age * 0.08F) * 0.15F;
        for (ModelPart leg : front) fold(leg, legY, -1.45F, 2.8F, 1.5F);
        for (ModelPart leg : hind) fold(leg, legY, -1.35F, 2.65F, 0.0F);
    }

    private static void fold(ModelPart leg, float legY, float upper, float lower, float back) {
        leg.y = legY;
        leg.z += back;
        leg.xRot = upper;
        leg.getChild("lower").xRot = lower;
    }

    /** Lying dead on its side: legs roll out stiff towards the ground. */
    static void sag(ModelPart... legs) {
        for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
    }

    /** Heavy diagonal walk (front-left with hind-right), knees and hocks flexing on the forward swing. Returns the stride phase. */
    static float walk(float pos, float speed, float rate, float amount, ModelPart lf, ModelPart rf, ModelPart lh, ModelPart rh) {
        float phase = pos * rate;
        float swing = Mth.cos(phase) * amount * speed;
        float liftA = Math.max(0.0F, Mth.sin(phase)) * speed, liftB = Math.max(0.0F, -Mth.sin(phase)) * speed;
        lf.xRot = swing;
        rh.xRot = swing;
        rf.xRot = -swing;
        lh.xRot = -swing;
        lf.getChild("lower").xRot = liftA * 1.0F;
        rh.getChild("lower").xRot = liftA * 0.6F;
        rf.getChild("lower").xRot = liftB * 1.0F;
        lh.getChild("lower").xRot = liftB * 0.6F;
        return phase;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x (ear flicks, tail swishes). */
    static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }

    static void scale(ModelPart part, float s) {
        part.xScale = s;
        part.yScale = s;
        part.zScale = s;
    }
}
