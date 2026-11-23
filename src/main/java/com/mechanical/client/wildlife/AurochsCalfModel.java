package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Aurochs calf, built at baby size: a short body on long legs, a big hornless head carried higher than the adult's,
 * floppy sideways ears. Back ~12.5 px.
 * Geometry and skin: HerdGeometry.aurochsCalf and the textures from scripts/gen_herd_models.py, built at TWICE baby size
 * (drawn at half by the renderer's baby scale), so the pixel offsets below are doubled.
 */
public class AurochsCalfModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "aurochs_calf"), "main");

    private static final float NECK_X = -0.2F;
    private static final float HEAD_X = 0.6F;
    private static final float GRAZE_NECK_X = 0.9F;
    private static final float GRAZE_HEAD_X = 0.9F;
    private static final float LIE_DROP = 12.0F, LIE_LEG_Y = 21.0F;

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

    public AurochsCalfModel(ModelPart root) {
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
        return HerdGeometry.aurochsCalf();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float yaw = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        float pitch = state.xRot * Mth.DEG_TO_RAD;
        float age = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 0.1F; // the neck runs forward out of the chest: straight down
            head.xRot = 0.2F;
            leftEar.yRot = 1.0F; // sideways ears swung down
            rightEar.yRot = -1.0F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            this.leftFrontLeg.zRot = this.rightFrontLeg.zRot = this.leftHindLeg.zRot = this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.yRot = -0.4F;
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
            return;
        }

        if (state.dozing) {
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 0.5F;
            this.neck.yRot = 0.45F;
            this.head.xRot = 0.4F;
            this.head.yRot = 0.25F;
            this.head.zRot = 0.15F;
            this.leftEar.zRot = 0.9F;
            this.rightEar.zRot = -0.9F;
            return;
        }

        // gangly diagonal walk; at a run the calf bucks along with its tail up
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.75F;
        float swing = Mth.cos(phase) * Math.min(0.9F, speed * 1.3F);
        this.leftFrontLeg.xRot = swing;
        this.rightHindLeg.xRot = swing;
        this.rightFrontLeg.xRot = -swing;
        this.leftHindLeg.xRot = -swing;
        this.leftFrontLeg.getChild("lower").xRot = Math.max(0.0F, Mth.sin(phase)) * speed;
        this.rightFrontLeg.getChild("lower").xRot = Math.max(0.0F, -Mth.sin(phase)) * speed;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.4F * speed + 1.0F * run);
        this.body.xRot = Mth.sin(phase) * 0.06F * run;

        float graze = state.grazeAmount;
        float look = 1.0F - graze;
        this.neck.xRot = Mth.lerp(graze, NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.4F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.6F, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
        this.head.yRot = yaw * 0.6F * look;
        this.neck.y += graze * 3.0F;

        // floppy ears flick on their own rhythm
        float tl = (age + 11.0F) % 57.0F;
        if (tl < 6.0F) this.leftEar.zRot -= Mth.sin(tl / 6.0F * Mth.PI) * 0.5F;
        float tr = age % 71.0F;
        if (tr < 6.0F) this.rightEar.zRot += Mth.sin(tr / 6.0F * Mth.PI) * 0.5F;
        this.leftEar.zRot -= Mth.sin(phase) * 0.15F * speed;
        this.rightEar.zRot -= Mth.sin(phase) * 0.15F * speed;

        float tt = (age + 23.0F) % 45.0F;
        this.tail.zRot = tt < 10.0F ? Mth.sin(tt * 1.7F) * 0.45F * Mth.sin(tt / 10.0F * Mth.PI) : 0.0F;
        this.tail.xRot += speed * 0.4F + run * 0.9F;
    }
}
