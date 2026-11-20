package com.mechanical.client.wildlife;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Night sleep pose for the open-country wildlife models (aurochs, wild horse, mouflon and their young): body lowered onto
 * the ground, legs folded under it (forearm / thigh forward, cannon bone tucked back), slow breathing. Legs must have a
 * "lower" child. Neck and head are posed by each model.
 */
public final class LyingPose {
    private LyingPose() {}

    /**
     * @param drop how far (px) the body sinks so the belly rests on the ground
     * @param legY pivot height (px) of the folded legs, about half a leg's thickness above the ground (y = 24)
     */
    public static void apply(ModelPart body, float drop, float legY, float ageInTicks, ModelPart[] front, ModelPart[] hind) {
        body.y += drop + Mth.sin(ageInTicks * 0.08F) * 0.15F;
        for (ModelPart leg : front) fold(leg, legY, -1.45F, 2.8F, 1.5F);
        for (ModelPart leg : hind) fold(leg, legY, -1.35F, 2.65F, 0.0F);
    }

    private static void fold(ModelPart leg, float legY, float upper, float lower, float back) {
        leg.y = legY;
        leg.z += back; // front knees end up just ahead of the chest, not far out in front
        leg.xRot = upper;
        leg.getChild("lower").xRot = lower;
    }
}
