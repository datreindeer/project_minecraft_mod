package com.mechanical.client.wildlife.warm;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

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
 * Tapir calf, built at baby size: a round little body in the striped-and-spotted "watermelon" coat, a big head with a
 * stubby two-segment trunk, round ears, short legs. Back ~12 px. Texture textures/entity/wildlife/tapir_calf.png (64x32),
 * painted by scripts/gen_fauna_warm_tex_mammals.py (TAPIR_CALF_BOXES mirrors the cubes below).
 */
public class TapirCalfModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "tapir_calf"), "main");

    private static final float NECK_X = 0.15F;
    private static final float HEAD_X = 0.3F;
    private static final float GRAZE_NECK_X = 0.7F;
    private static final float GRAZE_HEAD_X = 0.7F;
    private static final float TRUNK_X = 0.3F, TIP_X = 0.35F;
    private static final float LIE_DROP = 5.0F, LIE_LEG_Y = 22.5F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart trunk;
    private final ModelPart trunkTip;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;

    public TapirCalfModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.trunk = this.head.getChild("trunk");
        this.trunkTip = this.trunk.getChild("trunk_tip");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.tail = this.body.getChild("tail");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -5.5F, 6.0F, 6.0F, 11.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(58, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 5.5F, 0.35F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(34, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, -5.0F, NECK_X, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(34, 8).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.3F, -2.5F, HEAD_X, 0.0F, 0.0F));
        PartDefinition trunk = head.addOrReplaceChild("trunk",
                CubeListBuilder.create().texOffs(50, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -0.3F, -3.7F, TRUNK_X, 0.0F, 0.0F));
        trunk.addOrReplaceChild("trunk_tip",
                CubeListBuilder.create().texOffs(50, 4).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.2F, -1.8F, TIP_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(56, 4).mirror().addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(1.5F, -1.8F, -0.8F, -0.3F, 0.0F, 0.35F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(56, 4).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-1.5F, -1.8F, -0.8F, -0.3F, 0.0F, -0.35F));

        addLeg(root, "left_front_leg", 1.8F, -3.5F, false);
        addLeg(root, "right_front_leg", -1.8F, -3.5F, false);
        addLeg(root, "left_hind_leg", 1.8F, 3.5F, true);
        addLeg(root, "right_hind_leg", -1.8F, 3.5F, true);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(8, 17).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 4.0F, 3.0F)
                : CubeListBuilder.create().mirror(left).texOffs(0, 17).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 4.0F, 2.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 17.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(18, 17).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 3.0F, hind ? 0.3F : 0.0F));
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
            trunk.xRot = trunkTip.xRot = 0.15F;
            leftEar.xRot = rightEar.xRot = 0.6F;
            tail.xRot = -1.6F;
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            this.leftFrontLeg.zRot = this.rightFrontLeg.zRot = this.leftHindLeg.zRot = this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.yRot = -0.4F;
            this.head.yRot = yaw * 0.6F;
            this.trunk.yRot = -0.3F;
            return;
        }

        if (state.dozing) {
            MammalPose.lie(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 0.4F;
            this.neck.yRot = 0.4F;
            this.head.xRot = 0.15F;
            this.head.yRot = 0.25F;
            this.head.zRot = 0.12F;
            this.trunk.xRot = 0.15F;
            this.trunkTip.xRot = -0.25F + Mth.sin(age * 0.08F) * 0.05F;
            return;
        }

        // quick little steps, a scamper when it runs after its mother
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.8F;
        MammalPose.walk(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg, phase, Math.min(0.9F, speed * 1.3F), speed);
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.2F * speed + 0.5F * run);

        float graze = state.grazeAmount;
        float look = 1.0F - graze;
        this.neck.xRot = Mth.lerp(graze, NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.05F * speed, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.4F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.6F, GRAZE_HEAD_X);
        this.head.yRot = yaw * 0.6F * look;
        this.neck.y += graze * 1.0F;

        float sniff = Mth.sin(age * 0.27F) * 0.15F + MammalPose.flick(age + 7.0F, 41.0F, 8.0F) * Mth.sin(age * 1.4F) * 0.25F;
        float curl = graze * (0.6F + Mth.sin(age * 0.4F) * 0.35F);
        this.trunk.xRot = TRUNK_X + sniff * 0.6F + curl * 0.5F;
        this.trunkTip.xRot = TIP_X + sniff + curl * 1.3F;
        this.trunk.yRot = Mth.sin(age * 0.13F) * 0.12F;

        this.leftEar.yRot = MammalPose.flick(age + 5.0F, 49.0F, 6.0F) * 0.5F;
        this.rightEar.yRot = -MammalPose.flick(age, 61.0F, 6.0F) * 0.5F;
        this.tail.zRot = MammalPose.flick(age + 13.0F, 37.0F, 9.0F) * Mth.sin(age * 1.6F) * 0.4F;
        this.tail.xRot += speed * 0.3F;
    }
}
