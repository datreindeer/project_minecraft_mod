package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Wild boar: deep shoulders and a lower, narrower rump (sloping back), a big wedge head held low (skull, muzzle bridge,
 * long snout with a flat disc nose), lower jaw with upturned tusks, bristle crest along the spine, short legs, tufted tail.
 * Texture 64x64: textures/entity/wildlife/wild_boar.png (made by scripts/gen_wild_boar_texture.py).
 */
public class WildBoarModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wild_boar"), "main");

    private static final float HEAD_X_ROT = 0.35F;
    private static final float RUMP_X_ROT = -0.12F;
    private static final float EAR_X_ROT = -0.7F;
    private static final float EAR_Z_ROT = 0.45F;
    private static final float TAIL_X_ROT = 0.25F;
    /** Short, quick steps: faster than the vanilla 0.6662 quadruped cadence. */
    private static final float GAIT = 0.9F;

    private final ModelPart body;
    private final ModelPart rump;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart tusks;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public WildBoarModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.rump = this.body.getChild("rump");
        this.tail = this.rump.getChild("tail");
        this.head = this.body.getChild("head");
        this.snout = this.head.getChild("snout");
        this.tusks = this.head.getChild("tusks");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // zero-width bristle planes, like the hoglin mane
        CubeDeformation plane = new CubeDeformation(0.001F);

        // Shoulders/chest: y 10..18, z -9..0, with a 1 px hump over the withers. The crest (bottom row buried)
        // stands up to 2 px above the back and runs forward onto the nape.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -9.0F, 10.0F, 8.0F, 9.0F)
                        .texOffs(0, 56).addBox(-4.0F, -5.0F, -9.0F, 8.0F, 1.0F, 6.0F)
                        .texOffs(0, 41).addBox(0.0F, -7.0F, -10.0F, 0.0F, 3.0F, 8.0F, plane)
                        .texOffs(16, 44).addBox(0.0F, -6.0F, -3.0F, 0.0F, 3.0F, 5.0F, plane),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        // Hindquarters: lower and narrower, drooping towards the tail; belly tucks up behind the chest.
        PartDefinition rump = body.addOrReplaceChild("rump",
                CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-4.5F, 0.0F, -1.0F, 9.0F, 6.0F, 10.0F)
                        .texOffs(0, 46).addBox(0.0F, -2.0F, -1.0F, 0.0F, 3.0F, 7.0F, plane),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, RUMP_X_ROT, 0.0F, 0.0F));
        rump.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(60, 4).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F)
                        .texOffs(52, 9).addBox(-1.0F, 4.5F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 1.0F, 8.8F, TAIL_X_ROT, 0.0F, 0.0F));

        // Head hangs from the front of the shoulders, held low (wedge: skull 7 -> bridge 5 -> snout 4 wide).
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 33).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -8.0F, HEAD_X_ROT, 0.0F, 0.0F));
        head.addOrReplaceChild("snout",
                CubeListBuilder.create()
                        .texOffs(40, 25).addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 2.0F)
                        .texOffs(40, 16).addBox(-2.0F, -1.5F, -6.0F, 4.0F, 4.0F, 4.0F)
                        // flat disc nose, flared half a pixel past the snout on each side
                        .texOffs(40, 37).addBox(-2.5F, -1.5F, -7.0F, 5.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -5.0F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(40, 32).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 2.5F, -7.0F));
        // Lower tusks: out of the lip beside the snout, then up and back.
        head.addOrReplaceChild("tusks",
                CubeListBuilder.create()
                        .texOffs(56, 4).addBox(-3.0F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(56, 4).addBox(-3.5F, -3.0F, 0.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(56, 4).addBox(2.0F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(56, 4).addBox(2.5F, -3.0F, 0.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 2.5F, -9.5F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(56, 0).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, -3.5F, -1.0F, EAR_X_ROT, 0.0F, -EAR_Z_ROT));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().mirror().texOffs(56, 0).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, -3.5F, -1.0F, EAR_X_ROT, 0.0F, EAR_Z_ROT));

        // Legs: thick upper leg tucked into the body, slim lower leg with the hoof; ground at y = 24.
        CubeListBuilder rightLeg = CubeListBuilder.create()
                .texOffs(40, 0).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F)
                .texOffs(40, 9).addBox(-1.5F, 4.0F, -1.5F, 3.0F, 4.0F, 3.0F);
        CubeListBuilder leftLeg = CubeListBuilder.create().mirror()
                .texOffs(40, 0).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F)
                .texOffs(40, 9).addBox(-1.5F, 4.0F, -1.5F, 3.0F, 4.0F, 3.0F);
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-2.8F, 16.0F, -5.5F));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(2.8F, 16.0F, -5.5F));
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-2.5F, 16.0F, 6.5F));
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(2.5F, 16.0F, 6.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.tusks.visible = state.showsHorns;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.25F; // in line with the body, snout down
            leftEar.xRot = rightEar.xRot = 1.0F;
            tail.xRot = -1.8F; // flops down over the back (the rump is tilted)
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg}) leg.zRot = CarcassRenderer.LIMB_SAG;
            // the carcass renderer puts a head yaw in yRot that points the head at the ground
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += 0.25F;
            this.rightEar.xRot += 0.6F;
            this.leftEar.xRot += 0.6F;
            this.tail.xRot = 0.9F;
            return;
        }

        if (state.dozing) {
            // lying on its belly: body/rump (bottom ~18) dropped onto y = 24, legs tucked under it (front back, hind
            // forward), pivots lowered so the folded legs touch the ground; head level-ish with the jaw on the ground
            this.body.y += 6.0F;
            for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg}) leg.y += 5.16F;
            this.rightFrontLeg.xRot = 1.4F;
            this.leftFrontLeg.xRot = 1.4F;
            this.rightHindLeg.xRot = -1.4F;
            this.leftHindLeg.xRot = -1.4F;
            this.head.xRot = 0.14F;
            this.rightEar.xRot += 0.4F;
            this.leftEar.xRot += 0.4F;
            this.tail.xRot = 0.9F; // lies on the ground instead of hanging into it
            return;
        }

        float age = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float graze = state.grazeAmount;
        float charge = state.charging ? 1.0F : 0.0F;

        // Trot: diagonal pairs; short swing at a walk that opens up when running.
        float swing = Mth.cos(pos * GAIT) * speed * (0.7F + 0.45F * speed);
        this.rightFrontLeg.xRot = swing;
        this.leftHindLeg.xRot = swing;
        this.leftFrontLeg.xRot = -swing;
        this.rightHindLeg.xRot = -swing;
        // body bobs once per step, the head nods with it
        float step = Mth.abs(Mth.sin(pos * GAIT));
        this.body.y -= step * 0.6F * speed;
        this.head.xRot += Mth.sin(pos * GAIT * 2.0F) * 0.06F * speed;

        // A boar can barely turn its head on that thick neck.
        this.head.yRot = Mth.clamp(state.yRot, -30.0F, 30.0F) * 0.8F * Mth.DEG_TO_RAD * (1.0F - graze);
        this.head.xRot += Mth.clamp(state.xRot, -20.0F, 20.0F) * 0.5F * Mth.DEG_TO_RAD * (1.0F - graze) * (1.0F - charge);
        // idle: slow sniffing nod and ear flicks
        this.head.xRot += Mth.sin(age * 0.07F) * 0.03F * (1.0F - speed);
        float flick = Mth.sin(age * 0.11F) > 0.93F ? Mth.sin(age * 1.7F) * 0.25F : 0.0F;
        this.rightEar.zRot -= flick + Mth.sin(pos * GAIT) * 0.15F * speed;
        this.leftEar.zRot += Mth.sin(pos * GAIT) * 0.15F * speed;

        // Rooting: snout pushed into the ground, shovelling forward and flicking up.
        if (graze > 0.0F) {
            float dig = Mth.sin(age * 0.6F);
            this.head.xRot = Mth.lerp(graze, this.head.xRot, 0.95F + dig * 0.08F);
            this.head.y += graze * 1.5F;
            this.head.z -= graze * (0.5F + Mth.cos(age * 0.6F) * 0.4F);
            this.snout.xRot -= graze * (0.08F + Mth.sin(age * 0.6F + 1.0F) * 0.07F);
        }

        // Charging: head low and thrust forward with the tusks leading, ears pinned back, tail up.
        if (state.charging) {
            this.head.xRot = Mth.lerp(0.8F, this.head.xRot, 0.65F);
            this.head.y += 1.0F;
            this.head.z -= 1.5F;
            this.rightEar.xRot -= 0.5F;
            this.leftEar.xRot -= 0.5F;
        }

        // Tail: constant lazy swish, faster on the move; raised straight up when charging.
        this.tail.zRot = Mth.sin(age * 0.3F) * 0.2F + Mth.sin(pos * GAIT) * 0.35F * speed;
        this.tail.xRot = state.charging ? 2.1F : TAIL_X_ROT + step * 0.15F * speed;
    }
}
