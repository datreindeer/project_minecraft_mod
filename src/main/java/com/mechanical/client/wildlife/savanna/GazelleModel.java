package com.mechanical.client.wildlife.savanna;

import com.mechanical.client.wildlife.WildAnimalRenderState;

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
 * Gazelle: the roe deer's build (RoeDeerModel, whose animation this copies) made lighter - slim body, long thin legs,
 * an upright neck, big ears - with ringed lyre-shaped horns (two segments each: back and out, then the tips curving
 * forward and in), hidden on fawns. Texture 64x32: textures/entity/wildlife/gazelle.png, painted by
 * scripts/gen_fauna_savanna_textures.py whose GAZELLE table mirrors the boxes below (keep them in sync).
 * Hierarchy: body -> neck -> head -> ears, horns (snout is a cube of the head), body -> tail; the legs hang from the root.
 */
public class GazelleModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "gazelle"), "main");

    private static final float NECK_REST = 0.35F;
    /** Counters the neck tilt so the head is level at rest. */
    private static final float HEAD_REST = -0.35F;
    private static final float NECK_GRAZE = 2.45F;
    private static final float HEAD_GRAZE = -0.95F;
    private static final float EAR_SPREAD = 0.55F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart tail;
    private final ModelPart rightHorn;
    private final ModelPart leftHorn;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public GazelleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.tail = this.body.getChild("tail");
        this.rightHorn = this.head.getChild("right_horn");
        this.leftHorn = this.head.getChild("left_horn");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // back at y=10 (shoulder height ~0.87 block), belly at y=16
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -7.0F, 6.0F, 6.0F, 13.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(50, 9).addBox(-1.5F, -6.0F, -2.0F, 3.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -5.0F, NECK_REST, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(38, 0).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 4.0F, 5.0F)
                        .texOffs(38, 9).addBox(-1.5F, -1.5F, -6.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, HEAD_REST, 0.0F, 0.0F));
        // ears splay outwards and turn their inner side partly sideways
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(56, 0).addBox(-1.5F, -4.0F, -0.5F, 3.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-1.5F, -2.5F, 1.0F, -0.2F, 0.5F, -EAR_SPREAD));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1.5F, -4.0F, -0.5F, 3.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(1.5F, -2.5F, 1.0F, -0.2F, -0.5F, EAR_SPREAD));
        // lyre horns: base swept back and out, tip curving forward
        for (float x : new float[] {-1.0F, 1.0F}) {
            boolean left = x > 0;
            PartDefinition horn = head.addOrReplaceChild(left ? "left_horn" : "right_horn",
                    CubeListBuilder.create().texOffs(42, 19).mirror(left).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                    PartPose.offsetAndRotation(x, -2.5F, -0.5F, -0.45F, 0.0F, left ? 0.2F : -0.2F));
            horn.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(46, 19).mirror(left).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -3.8F, 0.0F, 0.6F, 0.0F, left ? -0.3F : 0.3F));
        }
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(56, 5).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 6.0F, 0.3F, 0.0F, 0.0F));

        // front leg: forearm + thin cannon bone, hoof at y=24
        CubeListBuilder rightFront = CubeListBuilder.create()
                .texOffs(0, 19).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 5.0F, 3.0F)
                .texOffs(12, 19).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 6.0F, 2.0F);
        CubeListBuilder leftFront = CubeListBuilder.create().mirror()
                .texOffs(0, 19).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 5.0F, 3.0F)
                .texOffs(12, 19).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 6.0F, 2.0F);
        // hind leg: muscular ham + shank set slightly back (hock)
        CubeListBuilder rightHind = CubeListBuilder.create()
                .texOffs(20, 19).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 4.0F)
                .texOffs(34, 19).addBox(-1.0F, 5.0F, -0.5F, 2.0F, 6.0F, 2.0F);
        CubeListBuilder leftHind = CubeListBuilder.create().mirror()
                .texOffs(20, 19).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 4.0F)
                .texOffs(34, 19).addBox(-1.0F, 5.0F, -0.5F, 2.0F, 6.0F, 2.0F);
        root.addOrReplaceChild("right_front_leg", rightFront, PartPose.offset(-1.8F, 14.0F, -4.5F));
        root.addOrReplaceChild("left_front_leg", leftFront, PartPose.offset(1.8F, 14.0F, -4.5F));
        root.addOrReplaceChild("right_hind_leg", rightHind, PartPose.offset(-1.8F, 13.0F, 4.0F));
        root.addOrReplaceChild("left_hind_leg", leftHind, PartPose.offset(1.8F, 13.0F, 4.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.rightHorn.visible = this.leftHorn.visible = state.showsHorns;
        float yaw = state.yRot * Mth.DEG_TO_RAD;
        float pitch = state.xRot * Mth.DEG_TO_RAD;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // neck straight down in line with the body (it rises from the chest when standing)
            head.xRot = -1.25F; // head hanging limp below it, nose a little towards the belly
            leftEar.xRot = rightEar.xRot = 1.2F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            // lying on its side: legs, neck and ears fall towards the ground (+x in model space), head turned down
            this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.leftHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.rightFrontLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.leftFrontLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.xRot = 0.15F;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F; // neck points up, so the opposite roll sags it
            this.neck.yRot = yaw * 0.4F;
            this.head.yRot = yaw * 0.6F;
            this.head.xRot = -0.1F;
            this.rightEar.zRot -= CarcassRenderer.LIMB_SAG * 0.7F;
            this.leftEar.zRot -= CarcassRenderer.LIMB_SAG * 0.7F;
            return;
        }

        if (state.dozing) {
            // lying down: belly (box bottom 16) dropped onto y = 24; front legs folded back under the chest, hind legs
            // forward along the flanks, pivots lowered so the folded legs just touch the ground
            this.body.y += 8.0F;
            this.rightFrontLeg.y += 7.3F;
            this.leftFrontLeg.y += 7.3F;
            this.rightHindLeg.y += 7.65F;
            this.leftHindLeg.y += 7.65F;
            this.rightFrontLeg.xRot = 1.4F;
            this.leftFrontLeg.xRot = 1.4F;
            this.rightFrontLeg.yRot = -0.2F;
            this.leftFrontLeg.yRot = 0.2F;
            this.rightHindLeg.xRot = -1.4F;
            this.leftHindLeg.xRot = -1.4F;
            this.rightHindLeg.yRot = 0.35F;
            this.leftHindLeg.yRot = -0.35F;
            // neck bent down and turned aside, muzzle resting on the ground
            this.neck.xRot = 1.75F;
            this.neck.yRot = 0.5F;
            this.head.xRot = -1.5F;
            this.rightEar.xRot -= 0.5F;
            this.leftEar.xRot -= 0.5F;
            return;
        }

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float age = state.ageInTicks;
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F); // 0 walking .. 1 full run

        // diagonal gait: right front with left hind; bigger strides and a bounding body when running
        float phase = pos * 0.6662F;
        float swing = Mth.cos(phase) * Math.min(0.9F, speed * 1.2F);
        this.rightFrontLeg.xRot = swing;
        this.leftHindLeg.xRot = swing;
        this.leftFrontLeg.xRot = -swing;
        this.rightHindLeg.xRot = -swing;
        this.body.y -= Mth.abs(Mth.sin(phase)) * 0.8F * run;
        this.body.xRot = Mth.sin(phase) * 0.05F * run;

        // grazing takes over from looking around: blend the two poses
        float graze = Mth.clamp(state.grazeAmount, 0.0F, 1.0F);
        float look = 1.0F - graze;
        float bob = Mth.cos(phase * 2.0F) * 0.05F * speed;
        this.body.xRot += graze * 0.06F;
        this.neck.xRot = Mth.lerp(graze, NECK_REST + pitch * 0.3F + bob, NECK_GRAZE);
        this.neck.yRot = yaw * 0.4F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_REST + pitch * 0.7F, HEAD_GRAZE + Mth.sin(age * 0.5F) * 0.06F);
        this.head.yRot = yaw * 0.6F * look;

        // occasional ear twitches, each ear on its own rhythm
        float tl = (age + 13.0F) % 90.0F;
        if (tl < 6.0F) {
            float k = Mth.sin(tl / 6.0F * Mth.PI);
            this.leftEar.xRot -= k * 0.5F;
            this.leftEar.zRot += k * 0.15F;
        }
        float tr = age % 131.0F;
        if (tr < 6.0F) {
            float k = Mth.sin(tr / 6.0F * Mth.PI);
            this.rightEar.xRot -= k * 0.5F;
            this.rightEar.zRot -= k * 0.15F;
        }
        // ears laid back a little when running
        this.leftEar.xRot -= run * 0.4F;
        this.rightEar.xRot -= run * 0.4F;

        // tail flick now and then; raised (rump patch flared) when fleeing
        float tt = (age + 41.0F) % 70.0F;
        if (tt < 8.0F) {
            this.tail.xRot += Mth.sin(tt / 8.0F * Mth.PI) * 0.9F;
            this.tail.yRot = Mth.sin(tt * 1.6F) * 0.3F;
        }
        this.tail.xRot += run * 0.8F;
    }
}
