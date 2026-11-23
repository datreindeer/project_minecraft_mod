package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Wild horse foal, built at baby size: a short body high up on very long, thin legs, a short brush of a mane and a
 * fluffy little tail. Back ~14 px.
 * Geometry and skin: HerdGeometry.wildHorseFoal and the textures from scripts/gen_herd_models.py, built at TWICE baby size
 * (drawn at half by the renderer's baby scale), so the pixel offsets below are doubled.
 */
public class WildHorseFoalModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wild_horse_foal"), "main");

    private static final float NECK_X = 0.45F;
    private static final float HEAD_X = 0.3F;
    private static final float GRAZE_NECK_X = 2.0F;
    private static final float GRAZE_HEAD_X = -0.1F;
    private static final float LIE_DROP = 16.0F, LIE_LEG_Y = 22.0F;

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

    public WildHorseFoalModel(ModelPart root) {
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
        return HerdGeometry.wildHorseFoal();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float yaw = Mth.clamp(state.yRot, -55.0F, 55.0F) * Mth.DEG_TO_RAD;
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
            this.leftFrontLeg.zRot = this.rightFrontLeg.zRot = this.leftHindLeg.zRot = this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F;
            this.head.yRot = yaw * 0.5F;
            return;
        }

        if (state.dozing) {
            // foals sleep stretched out more, but folded legs read better at this size
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 1.35F;
            this.neck.yRot = 0.45F;
            this.head.xRot = -0.45F;
            this.head.yRot = 0.25F;
            this.head.zRot = 0.15F;
            this.leftEar.zRot = 0.7F;
            this.rightEar.zRot = -0.7F;
            return;
        }

        // long-legged trot that breaks into a bouncy, bucking canter
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.75F;
        float amp = Math.min(0.9F, speed * 1.3F);
        float front = Mth.cos(phase) * amp;
        float hind = Mth.cos(phase + Mth.PI * (1.0F - run * 0.7F)) * amp;
        this.leftFrontLeg.xRot = front;
        this.rightFrontLeg.xRot = -front * (1.0F - run) + front * run * 0.8F;
        this.rightHindLeg.xRot = hind;
        this.leftHindLeg.xRot = -hind * (1.0F - run) + hind * run * 0.8F;
        this.leftFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.leftFrontLeg.xRot) * 1.2F;
        this.rightFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.rightFrontLeg.xRot) * 1.2F;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.5F * speed + 1.6F * run);
        this.body.xRot = Mth.sin(phase) * 0.09F * run;

        float graze = state.grazeAmount;
        float look = 1.0F - graze;
        this.neck.xRot = Mth.lerp(graze, NECK_X - run * 0.2F + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.45F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.6F, GRAZE_HEAD_X + Mth.sin(age * 0.55F) * 0.05F);
        this.head.yRot = yaw * 0.55F * look;
        this.neck.y += graze * 2.0F;

        // inquisitive ears, flicking more than the adult's
        float tl = (age + 5.0F) % 53.0F;
        if (tl < 6.0F) this.leftEar.yRot = Mth.sin(tl / 6.0F * Mth.PI) * 0.6F;
        float tr = age % 67.0F;
        if (tr < 6.0F) this.rightEar.yRot = -Mth.sin(tr / 6.0F * Mth.PI) * 0.6F;
        this.leftEar.xRot -= run * 0.5F;
        this.rightEar.xRot -= run * 0.5F;

        float tt = (age + 17.0F) % 40.0F;
        this.tail.zRot = tt < 10.0F ? Mth.sin(tt * 1.9F) * 0.5F * Mth.sin(tt / 10.0F * Mth.PI) : 0.0F;
        this.tail.xRot += speed * 0.4F + run * 0.6F;
    }
}
