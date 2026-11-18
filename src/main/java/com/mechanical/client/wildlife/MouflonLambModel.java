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
 * Mouflon lamb, built at baby size: small woolly body on thin legs, a round hornless head with sideways ears.
 * Back ~10 px. Texture textures/entity/wildlife/mouflon_lamb.png (64x32), painted by scripts/gen_mouflon_texture.py
 * (LAMB_BOXES mirrors the cubes below).
 */
public class MouflonLambModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "mouflon_lamb"), "main");

    private static final float NECK_X = 0.5F;
    private static final float HEAD_X = -0.2F;
    private static final float GRAZE_NECK_X = 2.2F;
    private static final float GRAZE_HEAD_X = -0.9F;
    private static final float LIE_DROP = 5.5F, LIE_LEG_Y = 23.0F;

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

    public MouflonLambModel(ModelPart root) {
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
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, -3.5F, NECK_X, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 4.0F)
                        .texOffs(46, 0).addBox(-1.0F, -0.5F, -5.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(54, 0).mirror().addBox(-1.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, -1.0F, 0.0F, 0.0F, 0.3F, 0.2F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(54, 0).addBox(-1.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, -1.0F, 0.0F, 0.0F, -0.3F, -0.2F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(54, 2).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -1.7F, 3.8F, 0.3F, 0.0F, 0.0F));

        addLeg(root, "left_front_leg", 1.1F, -2.6F, false);
        addLeg(root, "right_front_leg", -1.1F, -2.6F, false);
        addLeg(root, "left_hind_leg", 1.1F, 2.6F, true);
        addLeg(root, "right_hind_leg", -1.1F, 2.6F, true);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(12, 12).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 3.0F, 3.0F)
                : CubeListBuilder.create().mirror(left).texOffs(0, 12).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 17.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(8, 12).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 2.0F, hind ? 0.3F : 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        float pitch = state.xRot * Mth.DEG_TO_RAD;
        float age = state.ageInTicks;

        if (CarcassRenderer.isCarcass(state)) {
            this.leftFrontLeg.zRot = this.rightFrontLeg.zRot = this.leftHindLeg.zRot = this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F;
            this.head.yRot = yaw * 0.5F;
            return;
        }

        if (state.dozing) {
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 1.3F;
            this.neck.yRot = 0.5F;
            this.head.xRot = -0.6F;
            this.head.yRot = 0.3F;
            this.head.zRot = 0.15F;
            this.leftEar.zRot = 0.5F;
            this.rightEar.zRot = -0.5F;
            return;
        }

        // springy little steps; a running lamb bounces stiff-legged ("pronking")
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.8F;
        float swing = Mth.cos(phase) * Math.min(0.9F, speed * 1.3F) * (1.0F - run * 0.6F);
        this.leftFrontLeg.xRot = swing;
        this.rightHindLeg.xRot = swing;
        this.rightFrontLeg.xRot = -swing;
        this.leftHindLeg.xRot = -swing;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.25F * speed + 1.2F * run);
        for (ModelPart leg : new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg}) {
            leg.y -= Mth.abs(Mth.sin(phase)) * 1.2F * run; // the whole lamb leaves the ground
        }

        float graze = state.grazeAmount;
        float look = 1.0F - graze;
        this.neck.xRot = Mth.lerp(graze, NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed, GRAZE_NECK_X);
        this.neck.yRot = yaw * 0.4F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_X + pitch * 0.7F, GRAZE_HEAD_X + Mth.sin(age * 0.7F) * 0.06F);
        this.head.yRot = yaw * 0.6F * look;
        this.neck.y += graze * 1.0F;

        float tl = (age + 9.0F) % 47.0F;
        if (tl < 6.0F) this.leftEar.zRot -= Mth.sin(tl / 6.0F * Mth.PI) * 0.5F;
        float tr = age % 59.0F;
        if (tr < 6.0F) this.rightEar.zRot += Mth.sin(tr / 6.0F * Mth.PI) * 0.5F;

        float tt = (age + 13.0F) % 30.0F;
        this.tail.zRot = tt < 8.0F ? Mth.sin(tt * 2.2F) * 0.6F * Mth.sin(tt / 8.0F * Mth.PI) : 0.0F;
        this.tail.xRot += run * 0.8F;
    }
}
