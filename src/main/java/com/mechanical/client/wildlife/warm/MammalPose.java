package com.mechanical.client.wildlife.warm;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Shared poses for the warm-country hoofed animals (onager, water buffalo, tapir and their young). {@link #lie} is the
 * same sleep pose as the open-country wildlife's LyingPose (package-private there): body on the ground, legs folded
 * under it, slow breathing. Legs must have a "lower" child.
 */
final class MammalPose {
    private MammalPose() {}

    /**
     * @param drop how far (px) the body sinks so the belly rests on the ground
     * @param legY pivot height (px) of the folded legs, about half a leg's thickness above the ground (y = 24)
     */
    static void lie(ModelPart body, float drop, float legY, float ageInTicks, ModelPart[] front, ModelPart[] hind) {
        body.y += drop + Mth.sin(ageInTicks * 0.08F) * 0.15F;
        for (ModelPart leg : front) fold(leg, legY, -1.45F, 2.8F, 1.5F);
        for (ModelPart leg : hind) fold(leg, legY, -1.35F, 2.65F, 0.0F);
    }

    private static void fold(ModelPart leg, float legY, float upper, float lower, float back) {
        leg.y = legY;
        leg.z += back;
        leg.xRot = upper;
        leg.getChild("lower").xRot = lower;
    }

    /** Diagonal walk: front-left with hind-right; knees (front) and hocks (hind) flex as each leg swings forward. */
    static void walk(ModelPart lf, ModelPart rf, ModelPart lh, ModelPart rh, float phase, float amp, float speed) {
        float swing = Mth.cos(phase) * amp;
        float liftA = Math.max(0.0F, Mth.sin(phase)) * speed;
        float liftB = Math.max(0.0F, -Mth.sin(phase)) * speed;
        lf.xRot = swing;
        rh.xRot = swing;
        rf.xRot = -swing;
        lh.xRot = -swing;
        lf.getChild("lower").xRot = liftA * 1.1F;
        rh.getChild("lower").xRot = liftA * 0.6F;
        rf.getChild("lower").xRot = liftB * 1.1F;
        lh.getChild("lower").xRot = liftB * 0.6F;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }

    /** Short flick 0..1..0 lasting len ticks, once every period ticks. */
    static float flick(float age, float period, float len) {
        float t = age % period;
        return t < len ? Mth.sin(t / len * Mth.PI) : 0.0F;
    }
}
