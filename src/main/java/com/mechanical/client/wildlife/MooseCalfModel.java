package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Moose calf, built at baby size and rendered at scale 1: short body perched on very long stick legs, no hump yet,
 * big ears and a long, still slim muzzle. Shoulder ~15.5 px.
 * Texture textures/entity/wildlife/moose_calf.png (64x64), painted by scripts/gen_moose_texture.py (CALF_BOXES).
 */
public class MooseCalfModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "moose_calf"), "main");

    private static final float NECK_X = 0.9F;
    private static final float HEAD_X = -0.55F;
    private static final float GRAZE_NECK_X = 2.3F;
    private static final float GRAZE_HEAD_X = -0.9F;
    private static final float DOZE_DROP = 8.0F;
    private static final float EAR_Z = 0.9F;

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

    public MooseCalfModel(ModelPart root) {
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
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.5F, -6.0F, 6.0F, 7.0F, 12.0F),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        body.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(58, 0).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -3.0F, 6.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(36, 0).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 6.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -2.0F, -5.5F, NECK_X, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(36, 10).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 4.0F)
                .texOffs(0, 19).addBox(-1.5F, -1.5F, -7.5F, 3.0F, 4.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear",
            CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(1.5F, -1.5F, 0.0F, -0.2F, 0.0F, EAR_Z));
        head.addOrReplaceChild("right_ear",
            CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(-1.5F, -1.5F, 0.0F, -0.2F, 0.0F, -EAR_Z));

        // long stick legs: upper 2x6, cannon 1x5, hoof at y=24
        for (float side : new float[] {1.0F, -1.0F}) {
            String s = side > 0 ? "left_" : "right_";
            PartDefinition front = root.addOrReplaceChild(s + "front_leg",
                CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(1.6F * side, 14.0F, -4.0F));
            front.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(8, 27).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(s + "hind_leg",
                CubeListBuilder.create().texOffs(12, 27).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 6.0F, 3.0F),
                PartPose.offset(1.6F * side, 14.0F, 4.0F));
            hind.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(22, 27).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 5.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
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
            this.body.y += DOZE_DROP + Mth.sin(age * 0.09F) * 0.12F;
            foldLeg(this.leftFrontLeg, -1.3F, 2.75F, 2.0F, 0.15F);
            foldLeg(this.rightFrontLeg, -1.3F, 2.75F, 2.0F, -0.15F);
            foldLeg(this.leftHindLeg, -1.35F, 2.9F, 0.0F, 0.25F);
            foldLeg(this.rightHindLeg, -1.35F, 2.9F, 0.0F, -0.2F);
            this.neck.xRot = 1.5F;
            this.neck.yRot = 0.35F;
            this.head.xRot = -0.6F;
            this.head.yRot = 0.3F;
            this.head.zRot = 0.15F;
            this.leftEar.zRot = EAR_Z + 0.4F;
            this.rightEar.zRot = -EAR_Z - 0.4F;
            this.leftEar.xRot = -0.6F;
            this.rightEar.xRot = -0.6F;
            return;
        }

        // gangly diagonal gait; when running the hind pair catches up (a bounding canter) and the body hops
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.7F;
        float amp = Math.min(0.8F, speed * 1.3F);
        float front = Mth.cos(phase) * amp;
        float hind = Mth.cos(phase + Mth.PI * (1.0F - run * 0.75F)) * amp;
        this.rightFrontLeg.xRot = front;
        this.leftFrontLeg.xRot = -front * (1.0F - run) + front * run * 0.8F;
        this.leftHindLeg.xRot = -hind;
        this.rightHindLeg.xRot = hind * (1.0F - run) - hind * run * 0.8F;
        this.leftFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.leftFrontLeg.xRot) * 0.8F;
        this.rightFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.rightFrontLeg.xRot) * 0.8F;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.25F * speed + 0.6F * run);

        float graze = Mth.clamp(state.grazeAmount, 0.0F, 1.0F);
        float bob = Mth.cos(phase * 2.0F) * 0.07F * speed;
        this.neck.xRot = Mth.lerp(graze, NECK_X + pitch * 0.3F + bob, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.4F * (1.0F - graze);
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.7F, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.06F);
        this.head.yRot = yaw * 0.6F * (1.0F - graze);

        // fidgety ears, laid back when running
        float tl = (age + 11.0F) % 57.0F;
        if (tl < 6.0F) this.leftEar.xRot -= Mth.sin(tl / 6.0F * Mth.PI) * 0.5F;
        float tr = age % 79.0F;
        if (tr < 6.0F) this.rightEar.xRot -= Mth.sin(tr / 6.0F * Mth.PI) * 0.5F;
        this.leftEar.xRot -= run * 0.5F;
        this.rightEar.xRot -= run * 0.5F;
        this.tail.xRot += run * 0.6F;
    }

    private static void foldLeg(ModelPart leg, float upper, float lower, float back, float splay) {
        leg.y += DOZE_DROP;
        leg.z += back;
        leg.xRot = upper;
        leg.zRot = splay;
        leg.getChild("lower").xRot = lower;
    }
}
