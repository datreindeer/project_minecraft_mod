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
 * Malayan tapir: a heavy, rounded pear-shaped body that rises to a high rump (back ~17 px), short stout legs with splayed
 * hoofed toes, a thick neck, rounded white-rimmed ears, a stub tail and the short prehensile trunk (three segments) that
 * wiggles while it looks around and curls under while it browses. Black with the soft-edged off-white saddle.
 * Parts, boxes and the skin textures/entity/wildlife/tapir.png (128x64, plus the skinned carcass skin) come from
 * scripts/gen_owarm_hoofed_models.py ({@link WarmHoofGeometry}).
 */
public class TapirModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "tapir"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float NECK_X = 0.15F;
    private static final float HEAD_X = 0.35F;
    private static final float GRAZE_NECK_X = 0.75F;
    private static final float GRAZE_HEAD_X = 0.75F;
    /** Trunk droops a little at rest, each segment more than the one before. */
    private static final float TRUNK_X = 0.25F, TRUNK2_X = 0.3F, TIP_X = 0.35F;
    private static final float TAIL_X = 0.3F;
    private static final float LIE_DROP = 5.2F, LIE_LEG_Y = 22.0F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart trunk;
    private final ModelPart trunk2;
    private final ModelPart trunkTip;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public TapirModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.trunk = this.head.getChild("trunk");
        this.trunk2 = this.trunk.getChild("trunk2");
        this.trunkTip = this.trunk2.getChild("trunk_tip");
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
        return WarmHoofGeometry.tapir();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 0.0F; // the neck runs forward out of the chest: straight down
            head.xRot = 0.1F;
            trunk.xRot = trunk2.xRot = trunkTip.xRot = 0.15F;
            leftEar.xRot = rightEar.xRot = 0.6F;
            tail.xRot = -1.6F;
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.xRot = 0.2F;
            this.neck.yRot = -0.35F;
            this.head.xRot = 0.25F;
            this.head.yRot = state.yRot * DEG * 0.6F;
            this.trunk.yRot = this.trunk2.yRot = -0.3F; // limp trunk flops towards the ground
            this.leftEar.xRot = this.rightEar.xRot = 0.5F;
            this.tail.xRot = 0.6F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            MammalPose.lie(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 0.2F; // chin on the ground
            this.neck.yRot = 0.3F;
            this.head.xRot = 0.05F;
            this.head.yRot = 0.2F;
            this.head.zRot = 0.1F;
            this.trunk.xRot = 0.2F; // trunk resting on the ground, the tip turned up a little
            this.trunk2.xRot = 0.1F;
            this.trunkTip.xRot = -0.3F + Mth.sin(age * 0.08F) * 0.05F; // breathing
            this.leftEar.xRot = this.rightEar.xRot = 0.2F;
            this.tail.xRot = 0.6F;
            return;
        }

        // Steady, short-stepped walk in diagonal pairs; a jog when fleeing to water.
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.7F;
        MammalPose.walk(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg, phase, 0.85F * speed, speed);
        this.body.y += Mth.cos(phase * 2.0F) * 0.3F * speed;
        this.body.zRot = Mth.sin(phase) * 0.03F * speed;

        float yaw = Mth.clamp(state.yRot, -45.0F, 45.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -25.0F, 25.0F) * DEG;
        float graze = state.grazeAmount;
        float neckX = NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.05F * speed;
        float headX = HEAD_X + pitch * 0.5F;
        if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X);
            this.neck.y += graze * 1.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.4F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.6F;

        // Trunk: always sniffing and wiggling; while browsing it curls under to pull plants into the mouth.
        float sniff = Mth.sin(age * 0.23F + seed) * 0.12F + MammalPose.pulse(age * 0.07F + seed) * Mth.sin(age * 1.3F) * 0.2F;
        float curl = graze * (0.55F + Mth.sin(age * 0.35F) * 0.35F);
        this.trunk.xRot = TRUNK_X + sniff * 0.5F + curl * 0.4F;
        this.trunk2.xRot = TRUNK2_X + sniff + curl;
        this.trunkTip.xRot = TIP_X + sniff * 1.2F + curl * 1.3F;
        float sway = Mth.sin(age * 0.11F + seed * 2.0F) * 0.15F;
        this.trunk.yRot = sway * 0.5F;
        this.trunk2.yRot = sway;
        this.trunkTip.yRot = sway;

        float leftFlick = MammalPose.pulse(age * 0.05F + seed);
        float rightFlick = MammalPose.pulse(age * 0.043F + seed + 2.0F);
        this.leftEar.yRot = leftFlick * Mth.sin(age * 0.9F) * 0.5F;
        this.rightEar.yRot = -rightFlick * Mth.sin(age * 1.0F) * 0.5F;

        this.tail.zRot = MammalPose.pulse(age * 0.06F + seed * 1.3F) * Mth.sin(age * 1.2F) * 0.4F;
        this.tail.xRot = TAIL_X + speed * 0.3F;

        if (state.swimming) {
            // at home in the water: body almost under, head up and the trunk curled high like a snorkel, still sniffing;
            // quick dog-paddle strokes
            float stroke = age * 0.4F;
            MammalPose.walk(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg, stroke, 0.8F, 1.0F);
            this.neck.xRot = NECK_X - 0.35F;
            this.head.xRot = HEAD_X - 0.4F;
            this.trunk.xRot = TRUNK_X - 0.9F + sniff * 0.3F;
            this.trunk2.xRot = TRUNK2_X - 0.7F + sniff;
            this.trunkTip.xRot = TIP_X - 0.5F + sniff;
            this.tail.xRot = TAIL_X + 0.8F;
        }
    }
}
