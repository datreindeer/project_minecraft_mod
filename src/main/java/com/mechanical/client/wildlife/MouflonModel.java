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
 * Mouflon, ram and ewe in one model: a compact short-coated sheep on slim legs (back ~16 px), short tail. The ram shows
 * the heavy horns curling back, down and forward around the ear; the ewe shows two short spikes instead.
 * Textures: textures/entity/wildlife/mouflon_{ram,ewe}.png (64x64), painted by scripts/gen_mouflon_texture.py whose
 * BOXES table mirrors the cubes below (keep them in sync).
 */
public class MouflonModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "mouflon"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float NECK_X = 0.55F;
    private static final float HEAD_X = -0.1F;
    private static final float GRAZE_NECK_X = 2.3F;
    private static final float GRAZE_HEAD_X = -0.9F;
    private static final float EAR_Z = 0.3F;
    private static final float LIE_DROP = 8.0F, LIE_LEG_Y = 22.8F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart ramHorns;
    private final ModelPart eweHorns;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public MouflonModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.ramHorns = this.head.getChild("ram_horns");
        this.eweHorns = this.head.getChild("ewe_horns");
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
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.5F, -7.0F, 7.0F, 7.0F, 14.0F),
                PartPose.offset(0.0F, 11.5F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(56, 2).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 6.8F, 0.3F, 0.0F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(42, 0).addBox(-1.5F, -6.0F, -2.0F, 3.0F, 7.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -6.0F, NECK_X, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(42, 11).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 5.0F)
                        .texOffs(0, 21).addBox(-1.5F, -1.0F, -7.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, -1.2F, 0.3F, 0.0F, 0.3F, -EAR_Z));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(56, 0).addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, -1.2F, 0.3F, 0.0F, -0.3F, EAR_Z));

        PartDefinition ramHorns = head.addOrReplaceChild("ram_horns", CubeListBuilder.create(), PartPose.ZERO);
        addRamHorn(ramHorns, "left_", 1.0F);
        addRamHorn(ramHorns, "right_", -1.0F);
        PartDefinition eweHorns = head.addOrReplaceChild("ewe_horns", CubeListBuilder.create(), PartPose.ZERO);
        for (float side : new float[] {1.0F, -1.0F}) {
            eweHorns.addOrReplaceChild(side > 0 ? "left_horn" : "right_horn",
                    CubeListBuilder.create().texOffs(36, 21).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(1.1F * side, -1.8F, -1.0F, -0.6F, 0.0F, 0.35F * side));
        }

        addLeg(root, "left_front_leg", 2.0F, -5.0F, false);
        addLeg(root, "right_front_leg", -2.0F, -5.0F, false);
        addLeg(root, "left_hind_leg", 2.0F, 5.0F, true);
        addLeg(root, "right_hind_leg", -2.0F, 5.0F, true);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Ram horn: four segments built along -y, each bent back at its root so the horn sweeps up and back from the
     * poll, outwards, down behind the ear and forwards beside the cheek (about 3/4 of a turn). side = +1 left, -1 right.
     */
    private static void addRamHorn(PartDefinition horns, String p, float side) {
        PartDefinition base = horns.addOrReplaceChild(p + "horn",
                CubeListBuilder.create().texOffs(12, 21).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(1.3F * side, -1.6F, -0.8F, -0.55F, 0.0F, 0.5F * side));
        PartDefinition second = base.addOrReplaceChild(p + "horn_2",
                CubeListBuilder.create().texOffs(12, 21).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, -1.15F, 0.0F, 0.3F * side));
        PartDefinition third = second.addOrReplaceChild(p + "horn_3",
                CubeListBuilder.create().texOffs(24, 21).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, -1.15F, 0.0F, 0.1F * side));
        third.addOrReplaceChild(p + "horn_tip",
                CubeListBuilder.create().texOffs(32, 21).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, -1.0F, 0.0F, -0.25F * side));
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(12, 27).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 5.0F, 4.0F)
                : CubeListBuilder.create().mirror(left).texOffs(0, 27).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 5.0F, 3.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 14.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(26, 27).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 4.0F, hind ? 0.5F : 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.ramHorns.visible = state.showsHorns;
        this.eweHorns.visible = !state.showsHorns;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // neck straight down in line with the body (it rises from the chest when standing)
            head.xRot = -1.25F; // head hanging limp below it, nose a little towards the belly
            leftEar.yRot = 1.0F; // sideways ears swung down
            rightEar.yRot = -1.0F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.xRot = 1.0F;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F;
            this.head.xRot = 0.2F;
            this.head.yRot = state.yRot * DEG * 0.5F;
            this.leftEar.zRot = 0.9F;
            this.rightEar.zRot = -0.9F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            // chin resting low, head turned a little to one side
            this.neck.xRot = 1.25F;
            this.neck.yRot = 0.4F;
            this.head.xRot = -0.55F;
            this.head.yRot = 0.25F;
            this.head.zRot = 0.15F;
            this.leftEar.zRot = 0.4F;
            this.rightEar.zRot = -0.4F;
            return;
        }

        // Nimble walk on diagonal pairs; bounding at a run (hind legs push together, body pitches).
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float run = Mth.clamp((speed - 0.45F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.7F;
        float amp = Math.min(0.95F, speed * 1.2F);
        float front = Mth.cos(phase) * amp;
        float hind = Mth.cos(phase + Mth.PI * (1.0F - run * 0.75F)) * amp;
        this.leftFrontLeg.xRot = front;
        this.rightFrontLeg.xRot = -front * (1.0F - run) + front * run * 0.85F;
        this.rightHindLeg.xRot = hind;
        this.leftHindLeg.xRot = -hind * (1.0F - run) + hind * run * 0.85F;
        this.leftFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.leftFrontLeg.xRot) * 1.2F;
        this.rightFrontLeg.getChild("lower").xRot = Math.max(0.0F, -this.rightFrontLeg.xRot) * 1.2F;
        this.leftHindLeg.getChild("lower").xRot = Math.max(0.0F, this.leftHindLeg.xRot) * 0.5F;
        this.rightHindLeg.getChild("lower").xRot = Math.max(0.0F, this.rightHindLeg.xRot) * 0.5F;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.25F * speed + 0.7F * run);
        this.body.xRot = Mth.sin(phase) * 0.08F * run;

        float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * DEG;
        float pitch = state.xRot * DEG;
        float graze = state.grazeAmount;
        float neckX = NECK_X - run * 0.15F + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed;
        float headX = HEAD_X + pitch * 0.7F;
        if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.7F) * 0.06F); // nibbling
            this.neck.y += graze * 1.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.4F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.6F;

        // Ears: quick flicks, back when running.
        float leftFlick = pulse(age * 0.055F + seed);
        float rightFlick = pulse(age * 0.047F + seed + 2.0F);
        this.leftEar.zRot = -EAR_Z + leftFlick * Mth.sin(age * 1.8F) * 0.4F;
        this.rightEar.zRot = EAR_Z - rightFlick * Mth.sin(age * 2.0F) * 0.4F;
        this.leftEar.yRot = 0.3F + run * 0.5F;
        this.rightEar.yRot = -0.3F - run * 0.5F;

        // Short tail: wags now and then, flicked up when fleeing.
        float wag = pulse(age * 0.07F + seed * 1.3F);
        this.tail.zRot = wag * Mth.sin(age * 1.5F) * 0.5F;
        this.tail.xRot = 0.3F + run * 1.0F;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    private static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }
}
