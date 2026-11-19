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
 * Deer fawn (roe deer fawn and red deer calf share it, different textures), built at baby size, rendered at scale 1:
 * short body on long stick legs, big head and ears, short muzzle. Shoulder ~9.5 px, ear tips ~14 px.
 * Texture 64x32, painted by scripts/gen_fawn_textures.py whose CUBES table mirrors the boxes below (keep them in sync).
 * Hierarchy as RoeDeerModel: body -> neck -> head -> ears (snout is a cube of the head), body -> tail; legs hang from the root.
 */
public class FawnModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "fawn"), "main");

    /** Neck leans forward so the big head stays low enough (ear tips <= 14 px). */
    private static final float NECK_REST = 0.85F;
    /** Counters the neck tilt so the head is level at rest. */
    private static final float HEAD_REST = -0.85F;
    private static final float NECK_GRAZE = 2.4F;
    private static final float HEAD_GRAZE = -0.9F;
    /** Fawn ears stick out sideways more than the adult's. */
    private static final float EAR_SPREAD = 1.0F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart tail;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public FawnModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.tail = this.body.getChild("tail");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // back at y=14.5 (shoulder 9.5 px), belly at y=18.5: 5.5 px of leg showing under a 4 px deep body
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 16.5F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(24, 7).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, -3.0F, NECK_REST, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 3.0F, 4.0F)
                        .texOffs(38, 0).addBox(-1.0F, -1.5F, -5.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, HEAD_REST, 0.0F, 0.0F));
        // big ears, splayed out to the sides with the pale inner side turned forwards
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(46, 0).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -1.5F, 0.0F, -0.4F, 0.45F, -EAR_SPREAD));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(46, 0).mirror().addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, -1.5F, 0.0F, -0.4F, -0.45F, EAR_SPREAD));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -1.8F, 3.8F, 0.35F, 0.0F, 0.0F));

        // stick legs: a short 2x2 upper part and a 1x1 cannon bone, hoof at y=24
        CubeListBuilder rightFront = CubeListBuilder.create()
                .texOffs(0, 12).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(8, 12).addBox(-0.5F, 2.5F, -0.5F, 1.0F, 4.0F, 1.0F);
        CubeListBuilder leftFront = CubeListBuilder.create().mirror()
                .texOffs(0, 12).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(8, 12).addBox(-0.5F, 2.5F, -0.5F, 1.0F, 4.0F, 1.0F);
        // hind leg: deeper ham, cannon set back (hock)
        CubeListBuilder rightHind = CubeListBuilder.create()
                .texOffs(12, 12).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 4.0F, 3.0F)
                .texOffs(32, 7).addBox(-0.5F, 2.5F, -0.2F, 1.0F, 4.0F, 1.0F);
        CubeListBuilder leftHind = CubeListBuilder.create().mirror()
                .texOffs(12, 12).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 4.0F, 3.0F)
                .texOffs(32, 7).addBox(-0.5F, 2.5F, -0.2F, 1.0F, 4.0F, 1.0F);
        root.addOrReplaceChild("right_front_leg", rightFront, PartPose.offset(-1.1F, 17.5F, -2.6F));
        root.addOrReplaceChild("left_front_leg", leftFront, PartPose.offset(1.1F, 17.5F, -2.6F));
        root.addOrReplaceChild("right_hind_leg", rightHind, PartPose.offset(-1.1F, 17.5F, 2.6F));
        root.addOrReplaceChild("left_hind_leg", leftHind, PartPose.offset(1.1F, 17.5F, 2.6F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
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
            // lying on its side: legs, neck and ears fall towards the ground (+x in model space)
            this.rightHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.leftHindLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.rightFrontLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.leftFrontLeg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.xRot = 0.3F;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F; // neck points up, so the opposite roll sags it
            this.neck.yRot = yaw * 0.4F;
            this.head.yRot = yaw * 0.6F;
            this.head.xRot = -0.3F;
            this.rightEar.zRot -= CarcassRenderer.LIMB_SAG * 0.7F;
            this.leftEar.zRot -= CarcassRenderer.LIMB_SAG * 0.7F;
            return;
        }

        if (state.dozing) {
            // curled up: belly (box bottom 18.5) dropped onto y = 24; front legs folded back under the chest, hind legs
            // forward along the flanks, pivots lowered so the folded legs just touch the ground
            this.body.y += 5.5F;
            this.rightFrontLeg.y += 4.9F;
            this.leftFrontLeg.y += 4.9F;
            this.rightHindLeg.y += 4.6F;
            this.leftHindLeg.y += 4.6F;
            this.rightFrontLeg.xRot = 1.4F;
            this.leftFrontLeg.xRot = 1.4F;
            this.rightFrontLeg.yRot = -0.2F;
            this.leftFrontLeg.yRot = 0.2F;
            this.rightHindLeg.xRot = -1.4F;
            this.leftHindLeg.xRot = -1.4F;
            this.rightHindLeg.yRot = 0.35F;
            this.leftHindLeg.yRot = -0.35F;
            // neck stretched down and turned aside, muzzle resting on the ground, ears laid back
            this.neck.xRot = 1.6F;
            this.neck.yRot = 0.5F;
            this.head.xRot = -1.2F;
            this.rightEar.xRot -= 0.6F;
            this.leftEar.xRot -= 0.6F;
            return;
        }

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float age = state.ageInTicks;
        float run = Mth.clamp((speed - 0.4F) / 0.4F, 0.0F, 1.0F); // 0 walking .. 1 full run

        // diagonal gait with big, gangly strides; when running the hind pair catches up with the front pair
        // (a bounding skip) and the whole body hops
        float phase = pos * 0.75F;
        float amp = Math.min(0.85F, speed * 1.4F);
        float front = Mth.cos(phase) * amp;
        float hind = Mth.cos(phase + Mth.PI * (1.0F - run * 0.75F)) * amp;
        this.rightFrontLeg.xRot = front;
        this.leftFrontLeg.xRot = -front * (1.0F - run) + front * run * 0.8F;
        this.leftHindLeg.xRot = -hind;
        this.rightHindLeg.xRot = hind * (1.0F - run) - hind * run * 0.8F;
        float hop = Mth.abs(Mth.sin(phase)) * (0.25F * speed + 0.6F * run);
        this.body.y -= hop;
        this.body.xRot = Mth.sin(phase) * 0.08F * run;

        // grazing takes over from looking around: blend the two poses
        float graze = Mth.clamp(state.grazeAmount, 0.0F, 1.0F);
        float look = 1.0F - graze;
        float bob = Mth.cos(phase * 2.0F) * 0.07F * speed;
        this.body.xRot += graze * 0.08F;
        this.neck.xRot = Mth.lerp(graze, NECK_REST + pitch * 0.3F + bob, NECK_GRAZE);
        this.neck.yRot = yaw * 0.4F * look;
        this.head.xRot = Mth.lerp(graze, HEAD_REST + pitch * 0.7F, HEAD_GRAZE + Mth.sin(age * 0.5F) * 0.06F);
        this.head.yRot = yaw * 0.6F * look;

        // ear flicks, each ear on its own rhythm, a bit more fidgety than the adult
        float tl = (age + 7.0F) % 61.0F;
        if (tl < 6.0F) {
            float k = Mth.sin(tl / 6.0F * Mth.PI);
            this.leftEar.xRot -= k * 0.5F;
            this.leftEar.zRot += k * 0.2F;
        }
        float tr = age % 83.0F;
        if (tr < 6.0F) {
            float k = Mth.sin(tr / 6.0F * Mth.PI);
            this.rightEar.xRot -= k * 0.5F;
            this.rightEar.zRot -= k * 0.2F;
        }
        // ears laid back a little when running
        this.leftEar.xRot -= run * 0.4F;
        this.rightEar.xRot -= run * 0.4F;

        // little tail wag now and then, raised when running
        float tt = (age + 29.0F) % 50.0F;
        if (tt < 10.0F) {
            this.tail.yRot = Mth.sin(tt * 1.9F) * 0.5F * Mth.sin(tt / 10.0F * Mth.PI);
            this.tail.xRot += Mth.sin(tt / 10.0F * Mth.PI) * 0.4F;
        }
        this.tail.xRot += run * 0.7F;
    }
}
