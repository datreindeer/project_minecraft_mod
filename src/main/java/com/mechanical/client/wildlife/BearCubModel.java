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
 * Brown bear cub, built at baby size (rendered at scale 1): a round, fluffy 7x6x10 body, a big head with a short muzzle
 * and large round ears, chubby short legs, a stub tail. No hump yet.
 * Texture 64x32: textures/entity/wildlife/brown_bear_cub.png (made by scripts/gen_brown_bear_texture.py).
 */
public class BearCubModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "bear_cub"), "main");

    private static final float GAIT = 0.9F;

    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart[] legs;

    public BearCubModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.head = root.getChild("head");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.0F, -5.0F, 7.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(54, 3).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 5.0F, 0.4F, 0.0F, 0.0F));

        // big round head, short muzzle with the nose at its tip
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(34, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 5.0F, 4.0F)
                        .texOffs(34, 9).addBox(-1.5F, -0.5F, -6.0F, 3.0F, 2.0F, 2.0F)
                        .texOffs(44, 9).addBox(-1.0F, -0.6F, -6.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 16.0F, -4.5F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(54, 0).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-2.2F, -2.5F, -1.5F, 0.0F, 0.0F, -0.3F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().mirror().texOffs(54, 0).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(2.2F, -2.5F, -1.5F, 0.0F, 0.0F, 0.3F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F);
        CubeListBuilder legMirror = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F);
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.0F, 20.0F, -3.0F));
        root.addOrReplaceChild("left_front_leg", legMirror, PartPose.offset(2.0F, 20.0F, -3.0F));
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.0F, 20.0F, 3.5F));
        root.addOrReplaceChild("left_hind_leg", legMirror, PartPose.offset(2.0F, 20.0F, 3.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot = 0.3F;
            return;
        }

        float age = state.ageInTicks;
        if (state.dozing) {
            // curled on its belly: body (bottom y 21) dropped onto y = 24, forelegs forward under the chin,
            // hind legs back; leg pivots 1.5 px (half a leg) above the ground
            this.body.y += 3.0F + Mth.sin(age * 0.09F) * 0.12F;
            for (ModelPart leg : legs) leg.y = 22.5F;
            this.rightFrontLeg.xRot = -Mth.HALF_PI;
            this.leftFrontLeg.xRot = -Mth.HALF_PI;
            this.rightHindLeg.xRot = Mth.HALF_PI;
            this.leftHindLeg.xRot = Mth.HALF_PI;
            // chin resting on the forepaws (top at y 21)
            this.head.y += 3.0F;
            this.head.xRot = 0.1F;
            this.head.yRot = -0.2F;
            this.rightEar.zRot -= 0.2F;
            this.leftEar.zRot += 0.2F;
            return;
        }

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float graze = state.grazeAmount;

        // bouncy little amble, the whole cub bobs
        float swing = Mth.cos(pos * GAIT) * speed * 1.1F;
        this.rightFrontLeg.xRot = swing;
        this.leftHindLeg.xRot = swing;
        this.leftFrontLeg.xRot = -swing;
        this.rightHindLeg.xRot = -swing;
        float bob = Mth.abs(Mth.sin(pos * GAIT)) * 0.7F * speed;
        this.body.y -= bob;
        this.head.y -= bob;
        this.body.zRot = Mth.sin(pos * GAIT) * 0.06F * speed;

        // curious: looks around a lot, head tilted
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * (1.0F - graze);
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * (1.0F - graze);
        this.head.zRot = Mth.sin(age * 0.05F) * 0.12F;
        float flick = Mth.sin(age * 0.13F) > 0.9F ? Mth.sin(age * 1.5F) * 0.25F : 0.0F;
        this.rightEar.zRot -= flick;
        this.leftEar.zRot += flick * 0.6F;

        if (graze > 0.0F) {
            this.head.xRot = Mth.lerp(graze, this.head.xRot, 0.9F);
            this.head.y += graze * 1.5F;
        }

        this.tail.zRot = Mth.sin(age * 0.25F) * 0.15F;
    }
}
