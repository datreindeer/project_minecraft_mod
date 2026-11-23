package com.mechanical.client.wildlife.warm;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Water buffalo calf, built at baby size: a stout, hairier brownish-grey body on comparatively long legs, a big head with
 * big floppy ears and horn buds, a thin tasselled tail. Back ~14 px. Parts, boxes and the skin
 * textures/entity/wildlife/water_buffalo_calf.png (128x64) come from scripts/gen_owarm_hoofed_models.py
 * ({@link WarmHoofGeometry}).
 */
public class WaterBuffaloCalfModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "water_buffalo_calf"), "main");

    private static final float NECK_X = 0.25F;
    private static final float HEAD_X = 0.45F;
    private static final float GRAZE_NECK_X = 1.0F;
    private static final float GRAZE_HEAD_X = 0.7F;
    private static final float EAR_Z = 0.55F;
    private static final float LIE_DROP = 6.5F, LIE_LEG_Y = 22.5F;

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

    public WaterBuffaloCalfModel(ModelPart root) {
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
        return WarmHoofGeometry.waterBuffaloCalf();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float yaw = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        float pitch = Mth.clamp(state.xRot, -25.0F, 25.0F) * Mth.DEG_TO_RAD;
        float age = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 0.0F;
            head.xRot = 0.1F;
            leftEar.zRot = 1.3F;
            rightEar.zRot = -1.3F;
            tail.xRot = -1.9F;
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            this.leftFrontLeg.zRot = this.rightFrontLeg.zRot = this.leftHindLeg.zRot = this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.yRot = -0.4F;
            this.head.yRot = yaw * 0.6F;
            this.leftEar.zRot = 1.3F;
            this.rightEar.zRot = -1.3F;
            return;
        }

        boolean wallowing = state instanceof WarmRenderState w && w.wallowing;
        if (state.dozing || wallowing) {
            MammalPose.lie(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 0.2F; // chin on the ground
            this.neck.yRot = wallowing ? 0.0F : 0.45F;
            this.head.xRot = wallowing ? 0.0F : 0.15F;
            this.head.yRot = wallowing ? 0.0F : 0.25F;
            this.head.zRot = wallowing ? 0.0F : 0.15F;
            this.leftEar.zRot = 0.9F;
            this.rightEar.zRot = -0.9F;
            return;
        }

        // sturdy little trot; at a run it bucks along with its tail up
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.75F;
        MammalPose.walk(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg, phase, Math.min(0.9F, speed * 1.3F), speed);
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.2F * speed + 0.5F * run);
        this.body.xRot = Mth.sin(phase) * 0.06F * run;

        float graze = state.grazeAmount;
        float look = 1.0F - graze;
        this.neck.xRot = Mth.lerp(graze, NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.4F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.6F, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
        this.head.yRot = yaw * 0.6F * look;
        this.neck.y += graze * 1.5F;

        // floppy ears flick on their own rhythm
        this.leftEar.zRot = EAR_Z - MammalPose.flick(age + 11.0F, 57.0F, 6.0F) * 0.5F - Mth.sin(phase) * 0.15F * speed;
        this.rightEar.zRot = -EAR_Z + MammalPose.flick(age, 71.0F, 6.0F) * 0.5F - Mth.sin(phase) * 0.15F * speed;

        float tt = (age + 23.0F) % 45.0F;
        this.tail.zRot = tt < 10.0F ? Mth.sin(tt * 1.7F) * 0.45F * Mth.sin(tt / 10.0F * Mth.PI) : 0.0F;
        this.tail.xRot += speed * 0.4F + run * 0.9F;
    }
}
