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
 * Aurochs calf, built at baby size: a short body on long legs, a big hornless head carried higher than the adult's,
 * floppy sideways ears. Back ~12.5 px. Texture textures/entity/wildlife/aurochs_calf.png (64x32), painted by
 * scripts/gen_aurochs_texture.py (CALF_BOXES mirrors the cubes below).
 */
public class AurochsCalfModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "aurochs_calf"), "main");

    private static final float NECK_X = -0.2F;
    private static final float HEAD_X = 0.6F;
    private static final float GRAZE_NECK_X = 0.9F;
    private static final float GRAZE_HEAD_X = 0.9F;
    private static final float LIE_DROP = 6.0F, LIE_LEG_Y = 22.5F;

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
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -5.5F, 6.0F, 6.0F, 11.0F),
                PartPose.offset(0.0F, 14.5F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(34, 0).addBox(-1.5F, -2.0F, -4.0F, 3.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -5.0F, NECK_X, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(34, 9).addBox(-2.5F, -2.5F, -4.0F, 5.0F, 5.0F, 4.0F)
                        .texOffs(50, 0).addBox(-1.5F, -1.0F, -6.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, -3.5F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(52, 6).mirror().addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, -1.5F, -1.0F, 0.0F, 0.0F, 0.5F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(52, 6).addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, -1.5F, -1.0F, 0.0F, 0.0F, -0.5F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(52, 9).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 5.3F, 0.15F, 0.0F, 0.0F));

        addLeg(root, "left_front_leg", 1.8F, -3.5F, false);
        addLeg(root, "right_front_leg", -1.8F, -3.5F, false);
        addLeg(root, "left_hind_leg", 1.8F, 3.5F, true);
        addLeg(root, "right_hind_leg", -1.8F, 3.5F, true);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(20, 17).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 4.0F, 4.0F)
                : CubeListBuilder.create().mirror(left).texOffs(0, 17).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 4.0F, 3.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 17.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(12, 17).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 3.0F, 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float yaw = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        float pitch = state.xRot * Mth.DEG_TO_RAD;
        float age = state.ageInTicks;

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
