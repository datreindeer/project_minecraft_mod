package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Reindeer calf, built at baby size and rendered at scale 1: round, woolly body, short neck, short blunt muzzle and
 * already oversized hooves. No antlers yet. Shoulder ~11.5 px.
 * Geometry and skin: HerdGeometry.reindeerCalf and the textures from scripts/gen_herd_models.py, built at TWICE baby size
 * (drawn at half by the renderer's baby scale), so the pixel offsets below are doubled.
 */
public class ReindeerCalfModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "reindeer_calf"), "main");

    private static final float NECK_X = 0.8F;
    private static final float HEAD_X = -0.7F;
    private static final float GRAZE_NECK_X = 2.4F;
    private static final float GRAZE_HEAD_X = -1.0F;
    private static final float DOZE_DROP = 10.0F;
    private static final float EAR_Z = 1.0F;

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
    private final ModelPart[] legs;

    public ReindeerCalfModel(ModelPart root) {
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
        this.legs = new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return HerdGeometry.reindeerCalf();
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
            leftEar.xRot = rightEar.xRot = 1.2F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) {
                leg.zRot = CarcassRenderer.LIMB_SAG;
            }
            this.neck.xRot = 1.4F;
            this.head.xRot = 0.2F;
            this.leftEar.zRot = 1.3F;
            this.rightEar.zRot = -1.3F;
            return;
        }

        if (state.dozing) {
            this.body.y += DOZE_DROP + Mth.sin(age * 0.1F) * 0.2F;
            foldLeg(this.leftFrontLeg, -1.3F, 2.75F, 3.0F, 0.15F);
            foldLeg(this.rightFrontLeg, -1.3F, 2.75F, 3.0F, -0.15F);
            foldLeg(this.leftHindLeg, -1.35F, 2.9F, 0.0F, 0.25F);
            foldLeg(this.rightHindLeg, -1.35F, 2.9F, 0.0F, -0.2F);
            this.neck.xRot = 1.5F;
            this.neck.yRot = -0.35F;
            this.head.xRot = -0.6F;
            this.head.yRot = -0.3F;
            this.head.zRot = -0.15F;
            this.leftEar.zRot = EAR_Z + 0.3F;
            this.rightEar.zRot = -EAR_Z - 0.3F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.8F;
        float amp = Math.min(0.85F, speed * 1.4F);
        float front = Mth.cos(phase) * amp;
        float hind = Mth.cos(phase + Mth.PI * (1.0F - run * 0.75F)) * amp;
        this.rightFrontLeg.xRot = front;
        this.leftFrontLeg.xRot = -front * (1.0F - run) + front * run * 0.8F;
        this.leftHindLeg.xRot = -hind;
        this.rightHindLeg.xRot = hind * (1.0F - run) - hind * run * 0.8F;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.4F * speed + 1.0F * run);

        float graze = Mth.clamp(state.grazeAmount, 0.0F, 1.0F);
        float bob = Mth.cos(phase * 2.0F) * 0.07F * speed;
        this.neck.xRot = Mth.lerp(graze, NECK_X + pitch * 0.3F + bob, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.4F * (1.0F - graze);
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.7F, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.06F);
        this.head.yRot = yaw * 0.6F * (1.0F - graze);

        float tl = (age + 5.0F) % 53.0F;
        if (tl < 6.0F) this.leftEar.xRot -= Mth.sin(tl / 6.0F * Mth.PI) * 0.5F;
        float tr = age % 71.0F;
        if (tr < 6.0F) this.rightEar.xRot -= Mth.sin(tr / 6.0F * Mth.PI) * 0.5F;
        this.leftEar.xRot -= run * 0.4F;
        this.rightEar.xRot -= run * 0.4F;
        this.tail.xRot += run * 0.8F;
    }

    private static void foldLeg(ModelPart leg, float upper, float lower, float back, float splay) {
        leg.y += DOZE_DROP;
        leg.z += back;
        leg.xRot = upper;
        leg.zRot = splay;
        leg.getChild("lower").xRot = lower;
    }
}
