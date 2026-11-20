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
 * Onager (Asiatic wild ass): slighter than the wild horse, a short lean body (back ~18 px) high on long thin legs, a thin
 * neck with a short upright mane, a long narrow head with long ears, and a thin ass's tail with a tuft at the end.
 * The tamed one (TameOnagerRenderer) can carry Pack Baskets: two bark panniers on the flanks joined by a hide strap
 * over the back, shown when {@link WarmRenderState#packed}.
 * Texture textures/entity/wildlife/onager.png (128x64), painted by scripts/gen_fauna_warm_tex_mammals.py whose
 * ONAGER_BOXES table mirrors the cubes below (keep them in sync).
 */
public class OnagerModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "onager"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    /** Neck leans forward from the withers; the head hangs from the poll. */
    private static final float NECK_X = 0.55F;
    private static final float HEAD_X = 0.4F;
    private static final float GRAZE_NECK_X = 2.25F;
    private static final float GRAZE_HEAD_X = -0.2F;
    private static final float ALERT_NECK_X = 0.25F;
    private static final float TAIL_X = 0.25F;
    /** Long ears stand up and a little back against the head's tilt. */
    private static final float EAR_X = -0.75F, EAR_Z = 0.2F;
    private static final float LIE_DROP = 10.0F, LIE_LEG_Y = 22.5F;

    private final ModelPart body;
    private final ModelPart packs;
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

    public OnagerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.packs = this.body.getChild("packs");
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
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.5F, -9.0F, 7.0F, 7.0F, 18.0F),
                PartPose.offset(0.0F, 9.5F, 0.0F));
        // Thin cow-like tail: bare stalk, dark tuft at the end.
        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(116, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .texOffs(120, 0).addBox(-1.0F, 7.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 9.0F, TAIL_X, 0.0F, 0.0F));

        // Pack Baskets (tamed only): hide strap over the back, a bark pannier hanging on each flank.
        PartDefinition packs = body.addOrReplaceChild("packs",
                CubeListBuilder.create().texOffs(0, 40).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 1.0F, 3.0F), PartPose.ZERO);
        packs.addOrReplaceChild("left_basket",
                CubeListBuilder.create().texOffs(0, 46).mirror().addBox(0.0F, 0.0F, -3.5F, 3.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(3.5F, -3.0F, -0.5F, 0.0F, 0.0F, -0.08F));
        packs.addOrReplaceChild("right_basket",
                CubeListBuilder.create().texOffs(0, 46).addBox(-3.0F, 0.0F, -3.5F, 3.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(-3.5F, -3.0F, -0.5F, 0.0F, 0.0F, 0.08F));

        // Thin neck built upwards (-y) and leaned forward; the short dark mane stands upright on its crest.
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(52, 0).addBox(-1.5F, -8.0F, -2.5F, 3.0F, 9.0F, 5.0F)
                        .texOffs(68, 0).addBox(-0.5F, -9.0F, 2.0F, 1.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, -7.5F, NECK_X, 0.0F, 0.0F));
        // Narrow skull and a long, slim face.
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(76, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 5.0F)
                        .texOffs(94, 0).addBox(-1.5F, -1.0F, -9.0F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -7.5F, 0.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(110, 0).mirror().addBox(-1.0F, -5.0F, -0.5F, 2.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(1.2F, -1.8F, -0.2F, EAR_X, 0.0F, EAR_Z));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(110, 0).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(-1.2F, -1.8F, -0.2F, EAR_X, 0.0F, -EAR_Z));

        addLeg(root, "left_front_leg", 2.2F, -7.0F, false);
        addLeg(root, "right_front_leg", -2.2F, -7.0F, false);
        addLeg(root, "left_hind_leg", 2.2F, 7.0F, true);
        addLeg(root, "right_hind_leg", -2.2F, 7.0F, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(12, 27).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 4.0F)
                : CubeListBuilder.create().mirror(left).texOffs(0, 27).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 13.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(26, 27).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 5.0F, hind ? 0.5F : 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.packs.visible = state instanceof WarmRenderState w && w.packed && !state.isBaby;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // neck straight down in line with the body (it rises from the chest when standing)
            head.xRot = -1.25F; // head hanging limp below it
            leftEar.xRot = rightEar.xRot = 1.2F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            this.neck.xRot = 1.1F;
            this.neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F; // neck points up, so the opposite roll sags it
            this.head.xRot = 0.1F;
            this.head.yRot = state.yRot * DEG * 0.5F;
            this.leftEar.xRot = this.rightEar.xRot = 0.4F;
            this.tail.xRot = 0.5F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            MammalPose.lie(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            this.neck.xRot = 1.3F;
            this.neck.yRot = 0.4F;
            this.head.xRot = -0.5F;
            this.head.yRot = 0.2F;
            this.head.zRot = 0.15F;
            this.leftEar.xRot = this.rightEar.xRot = 0.1F; // long ears laid back along the neck
            this.leftEar.zRot = 0.5F;
            this.rightEar.zRot = -0.5F;
            this.tail.xRot = 0.15F;
            this.tail.zRot = 0.3F;
            return;
        }

        // Light, quick-footed: trots in diagonal pairs and breaks into a fast gallop (front and hind pairs half a stride apart).
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float run = Mth.clamp((speed - 0.45F) / 0.4F, 0.0F, 1.0F);
        float phase = state.walkAnimationPos * 0.66F;
        float amp = Math.min(1.0F, speed * 1.1F);
        float a = Mth.cos(phase) * amp;
        float frontLead = Mth.cos(phase - 0.5F * run) * amp;
        float hindTrail = Mth.cos(phase + Mth.PI * (1.0F - run * 0.55F)) * amp;
        this.leftFrontLeg.xRot = a;
        this.rightFrontLeg.xRot = Mth.lerp(run, -a, frontLead);
        this.rightHindLeg.xRot = Mth.lerp(run, a, hindTrail);
        this.leftHindLeg.xRot = Mth.lerp(run, -a, Mth.cos(phase + Mth.PI * (1.0F - run * 0.55F) - 0.5F * run) * amp);
        for (ModelPart leg : new ModelPart[] {leftFrontLeg, rightFrontLeg}) leg.getChild("lower").xRot = Math.max(0.0F, -leg.xRot) * 1.3F;
        for (ModelPart leg : new ModelPart[] {leftHindLeg, rightHindLeg}) leg.getChild("lower").xRot = Math.max(0.0F, leg.xRot) * 0.6F;
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.3F * speed + 0.8F * run);
        this.body.xRot = Mth.sin(phase) * 0.07F * run;

        // Tamed and rearing (the renderer tips the body up): forelegs tucked and pawing.
        if (state.stand > 0.0F) {
            float paw = Mth.sin(age * 0.6F) * 0.3F;
            this.leftFrontLeg.xRot = Mth.lerp(state.stand, this.leftFrontLeg.xRot, -0.9F + paw);
            this.rightFrontLeg.xRot = Mth.lerp(state.stand, this.rightFrontLeg.xRot, -0.9F - paw);
            this.leftFrontLeg.getChild("lower").xRot = Mth.lerp(state.stand, this.leftFrontLeg.getChild("lower").xRot, 1.6F);
            this.rightFrontLeg.getChild("lower").xRot = Mth.lerp(state.stand, this.rightFrontLeg.getChild("lower").xRot, 1.6F);
        }

        float yaw = Mth.clamp(state.yRot, -55.0F, 55.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -30.0F, 30.0F) * DEG;
        float graze = state.grazeAmount;
        float neckX = Mth.lerp(run, NECK_X, ALERT_NECK_X) + pitch * 0.3F + Mth.cos(phase * 2.0F) * (0.05F * speed + 0.12F * run)
                - state.stand * 0.4F;
        float headX = HEAD_X + pitch * 0.6F;
        if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.6F) * 0.05F); // nibbling dry grass
            this.neck.y += graze * 1.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.45F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.55F;

        // Long ears: swivel on their own, laid flat back at a gallop.
        float leftFlick = MammalPose.pulse(age * 0.05F + seed);
        float rightFlick = MammalPose.pulse(age * 0.043F + seed + 2.5F);
        this.leftEar.yRot = leftFlick * Mth.sin(age * 0.8F) * 0.7F;
        this.rightEar.yRot = -rightFlick * Mth.sin(age * 0.9F) * 0.7F;
        this.leftEar.xRot = EAR_X - run * 0.8F;
        this.rightEar.xRot = EAR_X - run * 0.8F;

        // Thin tail flicks at flies; held out at a gallop.
        float swish = MammalPose.pulse(age * 0.06F + seed * 1.3F);
        this.tail.zRot = swish * Mth.sin(age * 1.2F) * 0.55F + Mth.sin(phase) * 0.12F * speed;
        this.tail.xRot = TAIL_X + speed * 0.3F + run * 0.6F;
    }
}
