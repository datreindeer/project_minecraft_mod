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
 * Moose, bull and cow in one model (the bull shows the palmate antlers). Tall legs, humped withers, a short thick neck
 * with a dark ridge, a long head with a bulbous overhanging nose and the hairy dewlap ("bell") under the throat.
 * Withers ~29 px, nose ~29 px ahead of the shoulder.
 * Textures: textures/entity/wildlife/moose_{bull,cow}.png (128x64), painted by scripts/gen_moose_texture.py whose
 * BOXES table mirrors the cubes below (keep UVs in sync).
 */
public class MooseModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "moose"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    /** Neck juts forward-up from the hump; the head hangs nose-down from it. */
    private static final float NECK_X = 1.15F;
    private static final float HEAD_X = -0.55F;
    private static final float GRAZE_NECK_X = 2.3F;
    private static final float GRAZE_HEAD_X = -1.0F;
    /** Charging: neck level, forehead forward so the palms face the target. */
    private static final float CHARGE_NECK_X = 1.5F;
    private static final float CHARGE_HEAD_X = 0.35F;
    /** Asleep: neck stretched out low, nose resting on the ground, turned a little to the side. */
    private static final float DOZE_NECK_X = 1.55F;
    private static final float DOZE_HEAD_X = -0.55F;
    /** Body and leg pivots drop this far when lying down (chest on the ground). */
    private static final float DOZE_DROP = 12.0F;
    private static final float EAR_Z = 1.05F;
    private static final float DEWLAP_X = -0.6F;
    private static final float TAIL_X = 0.3F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart dewlap;
    private final ModelPart antlers;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public MooseModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.dewlap = this.head.getChild("dewlap");
        this.antlers = this.head.getChild("antlers");
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

        // Short barrel and a deeper chest that hangs below the belly line; belly at y=10, so 14 px of leg shows.
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -5.0F, -11.0F, 10.0F, 11.0F, 22.0F)
                .texOffs(64, 0).addBox(-5.5F, -5.5F, -12.0F, 11.0F, 13.0F, 9.0F),
            PartPose.offset(0.0F, 4.0F, 0.0F)
        );
        // Shoulder hump, tipped so it slopes down towards the back.
        body.addOrReplaceChild(
            "hump",
            CubeListBuilder.create().texOffs(64, 22).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 4.0F, 10.0F),
            PartPose.offsetAndRotation(0.0F, -4.5F, -6.0F, -0.15F, 0.0F, 0.0F)
        );
        body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(106, 32).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -4.0F, 11.0F, TAIL_X, 0.0F, 0.0F)
        );

        // Thick neck; its +z face is the top once tipped forward, where the dark hair ridge stands up.
        PartDefinition neck = body.addOrReplaceChild(
            "neck",
            CubeListBuilder.create()
                .texOffs(0, 33).addBox(-3.0F, -8.0F, -3.5F, 6.0F, 9.0F, 7.0F)
                .texOffs(112, 36).addBox(-1.0F, -8.5F, 3.5F, 2.0F, 9.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -3.0F, -10.5F, NECK_X, 0.0F, 0.0F)
        );

        // Cranium, long muzzle and the wider, drooping nose that overhangs the lower jaw.
        PartDefinition head = neck.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(104, 0).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(104, 12).addBox(-2.5F, -2.5F, -11.0F, 5.0F, 5.0F, 6.0F)
                .texOffs(100, 23).addBox(-3.0F, -2.5F, -14.0F, 6.0F, 6.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, -7.5F, 0.0F, HEAD_X, 0.0F, 0.0F)
        );
        // The bell: a hairy flap under the throat, pitched back so it hangs straight down.
        head.addOrReplaceChild(
            "dewlap",
            CubeListBuilder.create().texOffs(118, 23).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 6.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, 2.5F, -2.0F, DEWLAP_X, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
            "left_ear",
            CubeListBuilder.create().texOffs(100, 32).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 5.0F, 1.0F),
            PartPose.offsetAndRotation(2.5F, -2.5F, 0.5F, -0.2F, 0.0F, EAR_Z)
        );
        head.addOrReplaceChild(
            "right_ear",
            CubeListBuilder.create().texOffs(100, 32).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 5.0F, 1.0F),
            PartPose.offsetAndRotation(-2.5F, -2.5F, 0.5F, -0.2F, 0.0F, -EAR_Z)
        );

        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(), PartPose.ZERO);
        addAntler(antlers, "left_", 1.0F);
        addAntler(antlers, "right_", -1.0F);

        addLeg(root, "left_front_leg", 3.0F, -8.0F, false);
        addLeg(root, "right_front_leg", -3.0F, -8.0F, false);
        addLeg(root, "left_hind_leg", 3.0F, 8.0F, true);
        addLeg(root, "right_hind_leg", -3.0F, 8.0F, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * One antler: a short beam out sideways, the broad main palm rising outward with points along its far and rear
     * rims, and a smaller brow palm reaching forward. side = +1 left, -1 right. The beam's x tilt cancels the head's
     * nose-down pitch so the palms stand up instead of leaning over the face.
     */
    private static void addAntler(PartDefinition antlers, String p, float side) {
        PartDefinition beam = antlers.addOrReplaceChild(p + "antler",
            CubeListBuilder.create().texOffs(64, 36).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(2.0F * side, -2.5F, -1.0F, -0.6F, 0.0F, 1.25F * side));
        PartDefinition palm = beam.addOrReplaceChild(p + "palm",
            CubeListBuilder.create().texOffs(72, 36).addBox(-0.5F, -8.0F, -3.0F, 1.0F, 8.0F, 7.0F),
            PartPose.offsetAndRotation(0.0F, -3.5F, 0.0F, 0.0F, 0.0F, -0.1F * side));
        // points fanned along the far rim, then two on the rear rim
        float[][] rim = {{-2.5F, 0.35F, 3.0F}, {-0.5F, 0.1F, 2.0F}, {1.5F, -0.15F, 3.0F}, {3.5F, -0.4F, 2.0F}};
        for (int i = 0; i < rim.length; i++) {
            palm.addOrReplaceChild(p + "point_" + i, rim[i][2] > 2.0F ? longTine() : tine(),
                PartPose.offsetAndRotation(0.0F, -7.5F, rim[i][0], rim[i][1], 0.0F, -0.7F * side));
        }
        palm.addOrReplaceChild(p + "rear_point_0", tine(), PartPose.offsetAndRotation(0.0F, -3.0F, 3.5F, -1.0F, 0.0F, -0.8F * side));
        palm.addOrReplaceChild(p + "rear_point_1", longTine(), PartPose.offsetAndRotation(0.0F, -5.5F, 3.5F, -0.8F, 0.0F, -0.8F * side));
        PartDefinition brow = beam.addOrReplaceChild(p + "brow_palm",
            CubeListBuilder.create().texOffs(88, 36).addBox(-0.5F, -4.0F, -5.0F, 1.0F, 4.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, -1.5F, -0.5F, 0.5F, 0.0F, -0.3F * side));
        brow.addOrReplaceChild(p + "brow_point_0", tine(), PartPose.offsetAndRotation(0.0F, -3.5F, -4.5F, 0.9F, 0.0F, 0.0F));
        brow.addOrReplaceChild(p + "brow_point_1", tine(), PartPose.offsetAndRotation(0.0F, -1.0F, -4.5F, 1.4F, 0.0F, 0.1F * side));
    }

    private static CubeListBuilder tine() {
        return CubeListBuilder.create().texOffs(100, 38).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);
    }

    private static CubeListBuilder longTine() {
        return CubeListBuilder.create().texOffs(104, 38).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F);
    }

    /** Upper leg (forearm / thigh) with the long cannon bone and hoof as a child, so the knee/hock can flex. */
    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        if (hind) {
            PartDefinition leg = root.addOrReplaceChild(
                name, CubeListBuilder.create().texOffs(40, 33).addBox(-1.5F, -1.0F, -2.5F, 3.0F, 9.0F, 5.0F), PartPose.offset(x, 8.0F, z)
            );
            leg.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(56, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(0.0F, 8.0F, 1.0F));
        } else {
            PartDefinition leg = root.addOrReplaceChild(
                name, CubeListBuilder.create().texOffs(26, 33).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 9.0F, 4.0F), PartPose.offset(x, 8.0F, z)
            );
            leg.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(56, 33).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        }
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.antlers.visible = state.showsHorns;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // neck straight down in line with the body (it rises from the chest when standing)
            head.xRot = -1.25F; // head hanging limp below it, nose a little towards the belly
            leftEar.xRot = rightEar.xRot = 1.2F;
            tail.xRot = -1.9F; // flops down over the back
            dewlap.xRot = -1.8F; // hangs along the face
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) {
                leg.zRot = CarcassRenderer.LIMB_SAG;
            }
            this.neck.xRot = 1.5F;
            this.head.xRot = 0.3F;
            this.dewlap.xRot = 0.0F;
            this.leftEar.zRot = 1.4F;
            this.rightEar.zRot = -1.4F;
            this.tail.xRot = 0.1F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            float breath = Mth.sin(age * 0.07F + seed);
            this.body.y += DOZE_DROP + breath * 0.15F;
            // forearms forward with the cannons folded back under them; thighs tucked forward along the belly
            foldLeg(this.leftFrontLeg, -1.3F, 2.75F, 3.0F, 0.15F);
            foldLeg(this.rightFrontLeg, -1.3F, 2.75F, 3.0F, -0.15F);
            foldLeg(this.leftHindLeg, -1.35F, 2.9F, 0.0F, 0.25F);
            foldLeg(this.rightHindLeg, -1.35F, 2.9F, 0.0F, -0.2F);
            this.neck.xRot = DOZE_NECK_X;
            this.neck.yRot = 0.3F;
            this.head.xRot = DOZE_HEAD_X;
            this.head.yRot = 0.25F;
            this.head.zRot = 0.12F;
            this.dewlap.xRot = -0.2F;
            this.leftEar.zRot = EAR_Z + 0.35F;
            this.rightEar.zRot = -EAR_Z - 0.35F;
            this.leftEar.xRot = -0.5F;
            this.rightEar.xRot = -0.5F + pulse(age * 0.03F + seed) * Mth.sin(age * 1.5F) * 0.3F; // a twitch in its sleep
            this.tail.xRot = 0.05F;
            return;
        }

        // Gait: long slow diagonal strides; knee/hock flex while a leg swings forward.
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.5F;
        float swing = Mth.cos(phase) * 0.9F * speed;
        float liftA = Math.max(0.0F, Mth.sin(phase)) * speed;
        float liftB = Math.max(0.0F, -Mth.sin(phase)) * speed;
        this.leftFrontLeg.xRot = swing;
        this.rightHindLeg.xRot = swing;
        this.rightFrontLeg.xRot = -swing;
        this.leftHindLeg.xRot = -swing;
        this.leftFrontLeg.getChild("lower").xRot = liftA * 1.2F;
        this.rightHindLeg.getChild("lower").xRot = liftA * 0.6F;
        this.rightFrontLeg.getChild("lower").xRot = liftB * 1.2F;
        this.leftHindLeg.getChild("lower").xRot = liftB * 0.6F;
        this.body.y += Mth.cos(phase * 2.0F) * 0.5F * speed;

        float yaw = Mth.clamp(state.yRot, -50.0F, 50.0F) * DEG;
        float pitch = state.xRot * DEG;
        float nod = Mth.cos(phase * 2.0F) * 0.07F * speed;
        float neckX = NECK_X + pitch * 0.3F + nod;
        float headX = HEAD_X + pitch * 0.5F;
        float graze = state.grazeAmount;
        if (state.charging) {
            neckX = CHARGE_NECK_X;
            headX = CHARGE_HEAD_X;
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F); // chewing
            this.neck.y += graze * 2.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.4F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.6F;
        // the bell hangs plumb whatever the head does, and swings with the stride
        this.dewlap.xRot = DEWLAP_X - (neckX - NECK_X) - (headX - HEAD_X) + Mth.sin(phase) * 0.2F * speed;
        this.dewlap.zRot = Mth.cos(phase) * 0.15F * speed;

        float leftFlick = pulse(age * 0.04F + seed);
        float rightFlick = pulse(age * 0.033F + seed + 2.0F);
        this.leftEar.zRot = EAR_Z + leftFlick * Mth.sin(age * 1.6F) * 0.3F;
        this.rightEar.zRot = -EAR_Z - rightFlick * Mth.sin(age * 1.8F) * 0.3F;
        this.leftEar.xRot = -0.2F + leftFlick * 0.25F - speed * 0.4F;
        this.rightEar.xRot = -0.2F + rightFlick * 0.25F - speed * 0.4F;

        this.tail.xRot = TAIL_X + speed * 0.5F;
        this.tail.zRot = pulse(age * 0.05F + seed * 1.3F) * Mth.sin(age * 1.2F) * 0.35F;

        if (state.swimming) {
            // a strong swimmer: neck stretched out level on the water, muzzle lifted clear, the bell floating back,
            // ears laid back; long slow reaching strokes, forelegs pulling deep under the chest
            float stroke = age * 0.22F;
            this.neck.xRot = 1.4F + Mth.sin(stroke * 2.0F) * 0.03F;
            this.head.xRot = -0.95F;
            this.dewlap.xRot = DEWLAP_X + 0.15F;
            this.leftEar.xRot = this.rightEar.xRot = -0.7F;
            ModelPart[][] pairs = {{leftFrontLeg, rightHindLeg}, {rightFrontLeg, leftHindLeg}};
            for (int i = 0; i < 2; i++) {
                float s = Mth.sin(stroke + i * Mth.PI);
                for (ModelPart leg : pairs[i]) {
                    leg.xRot = -0.2F + s * 0.75F;
                    leg.getChild("lower").xRot = Math.max(0.0F, -s) * 1.3F; // knee folds on the recovery
                }
            }
            this.tail.xRot = TAIL_X + 0.9F;
        }
    }

    /** Lying pose for one leg: pivot dropped (and moved back by {@code back}), upper and lower bone folded, splayed by {@code splay}. */
    private static void foldLeg(ModelPart leg, float upper, float lower, float back, float splay) {
        leg.y += DOZE_DROP;
        leg.z += back;
        leg.xRot = upper;
        leg.zRot = splay;
        leg.getChild("lower").xRot = lower;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    private static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }
}
