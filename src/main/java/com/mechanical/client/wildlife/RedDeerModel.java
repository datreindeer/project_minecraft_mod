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
 * Red deer, stag and hind in one model: the stag shows the antlers (a 12-point "royal" rack) and the shaggy neck mane.
 * Textures: textures/entity/wildlife/red_deer_{stag,hind}.png, painted by scripts/gen_red_deer_texture.py (keep UVs in sync).
 */
public class RedDeerModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "red_deer"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    /** Rest pitch of the neck (forward lean) and of the head relative to it: nose slightly down. */
    private static final float NECK_X = 0.7F;
    private static final float HEAD_X = -0.55F;
    /** Grazing: neck reaches down-forward, head points at the ground. */
    private static final float GRAZE_NECK_X = 2.45F;
    private static final float GRAZE_HEAD_X = -1.15F;
    /** Charging: neck level, nose straight down so the antlers point forward. */
    private static final float CHARGE_NECK_X = 1.3F;
    private static final float CHARGE_HEAD_X = 0.2F;
    private static final float EAR_Z = 0.9F;
    private static final float TAIL_X = 0.35F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart mane;
    private final ModelPart head;
    private final ModelPart antlers;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public RedDeerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.mane = this.neck.getChild("mane");
        this.head = this.neck.getChild("head");
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

        // Barrel plus a wider, deeper chest in front: gives the withers hump and a belly line rising towards the flank.
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -9.0F, 8.0F, 8.0F, 18.0F)
                .texOffs(52, 0).addBox(-4.5F, -4.5F, -10.0F, 9.0F, 10.0F, 7.0F),
            PartPose.offset(0.0F, 10.0F, 0.0F)
        );
        body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(122, 11).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -3.5F, 9.0F, TAIL_X, 0.0F, 0.0F)
        );

        PartDefinition neck = body.addOrReplaceChild(
            "neck",
            CubeListBuilder.create().texOffs(84, 0).addBox(-2.0F, -9.0F, -2.5F, 4.0F, 11.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, -2.0F, -8.0F, NECK_X, 0.0F, 0.0F)
        );
        // Stag's shaggy throat mane, hidden on the hind.
        neck.addOrReplaceChild(
            "mane", CubeListBuilder.create().texOffs(0, 26).addBox(-3.0F, -6.0F, -3.5F, 6.0F, 8.0F, 6.0F), PartPose.ZERO
        );

        PartDefinition head = neck.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(102, 0).addBox(-2.5F, -3.0F, -4.0F, 5.0F, 5.0F, 6.0F)
                .texOffs(102, 11).addBox(-1.5F, -1.0F, -8.0F, 3.0F, 3.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -8.5F, 0.0F, HEAD_X, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
            "left_ear",
            CubeListBuilder.create().texOffs(116, 11).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(2.0F, -2.5F, 1.0F, 0.0F, 0.0F, EAR_Z)
        );
        head.addOrReplaceChild(
            "right_ear",
            CubeListBuilder.create().texOffs(116, 11).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
            PartPose.offsetAndRotation(-2.0F, -2.5F, 1.0F, 0.0F, 0.0F, -EAR_Z)
        );

        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(), PartPose.ZERO);
        addAntler(antlers, "left_", 1.0F);
        addAntler(antlers, "right_", -1.0F);

        addLeg(root, "left_front_leg", 2.5F, -7.0F, false);
        addLeg(root, "right_front_leg", -2.5F, -7.0F, false);
        addLeg(root, "left_hind_leg", 2.5F, 6.5F, true);
        addLeg(root, "right_hind_leg", -2.5F, 6.5F, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** One antler: main beam sweeping up and back with brow, bez and trez tines forward and a three-point crown. side = +1 left, -1 right. */
    private static void addAntler(PartDefinition antlers, String p, float side) {
        CubeListBuilder lowBeam = CubeListBuilder.create().texOffs(66, 26).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F);
        PartDefinition beam = antlers.addOrReplaceChild(p + "antler", lowBeam, PartPose.offsetAndRotation(1.5F * side, -3.0F, 0.0F, -0.4F, 0.0F, 0.62F * side));
        beam.addOrReplaceChild(p + "brow_tine", tine(4.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 1.35F, -0.3F * side, 0.0F));
        beam.addOrReplaceChild(p + "bez_tine", tine(3.0F), PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 1.15F, -0.15F * side, 0.0F));
        CubeListBuilder highBeam = CubeListBuilder.create().texOffs(70, 26).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F);
        PartDefinition upper = beam.addOrReplaceChild(p + "upper_beam", highBeam, PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.15F, 0.0F, -0.2F * side));
        upper.addOrReplaceChild(p + "trez_tine", tine(4.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 1.0F, -0.2F * side, 0.0F));
        upper.addOrReplaceChild(p + "crown_1", tine(4.0F), PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.55F, 0.0F, 0.35F * side));
        upper.addOrReplaceChild(p + "crown_2", tine(3.0F), PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, -0.35F, 0.0F, 0.1F * side));
        upper.addOrReplaceChild(p + "crown_3", tine(3.0F), PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.15F, 0.0F, -0.45F * side));
    }

    private static CubeListBuilder tine(float length) {
        return CubeListBuilder.create().texOffs(74, 26).addBox(-0.5F, -length, -0.5F, 1.0F, length, 1.0F);
    }

    /** Upper leg (forearm / thigh) with the cannon bone and hoof as a child, so the knee/hock can flex while walking. */
    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        if (hind) {
            PartDefinition leg = root.addOrReplaceChild(
                name, CubeListBuilder.create().texOffs(44, 26).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 8.0F, 4.0F), PartPose.offset(x, 13.0F, z)
            );
            leg.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(58, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 7.0F, 0.5F));
        } else {
            PartDefinition leg = root.addOrReplaceChild(
                name, CubeListBuilder.create().texOffs(24, 26).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(x, 13.0F, z)
            );
            leg.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(36, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        }
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.antlers.visible = state.showsHorns;
        this.mane.visible = state.showsHorns;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // neck straight down in line with the body (it rises from the chest when standing)
            head.xRot = -1.25F; // head hanging limp below it, nose a little towards the belly
            leftEar.xRot = rightEar.xRot = 1.2F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) {
                leg.zRot = CarcassRenderer.LIMB_SAG;
            }
            this.neck.xRot = 1.5F;
            this.head.xRot = 0.5F;
            this.leftEar.zRot = 1.3F;
            this.rightEar.zRot = -1.3F;
            this.tail.xRot = 0.1F;
            return;
        }

        if (state.dozing) {
            // lying down: chest (box bottom 15.5) dropped onto y = 24. Each leg folds at the knee/hock: the upper part
            // points forward, the lower part tucks back beneath it; pivots lowered so the folded legs touch the ground.
            this.body.y += 8.5F;
            this.leftFrontLeg.y += 8.5F;
            this.rightFrontLeg.y += 8.5F;
            this.leftHindLeg.y += 7.84F;
            this.rightHindLeg.y += 7.84F;
            for (ModelPart leg : this.legs) {
                leg.xRot = -1.4F;
                leg.getChild("lower").xRot = 2.9F;
            }
            // hind legs splay out so the thighs show along the flanks
            this.leftHindLeg.yRot = -0.35F;
            this.rightHindLeg.yRot = 0.35F;
            // neck bent down and turned aside, chin resting on the ground
            this.neck.xRot = 1.8F;
            this.neck.yRot = 0.45F;
            this.head.xRot = -1.3F;
            this.leftEar.xRot = -0.6F;
            this.rightEar.xRot = -0.6F;
            return;
        }

        float age = state.ageInTicks;
        // Per-animal phase so a herd does not twitch in unison.
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        // Gait: diagonal pairs (left front with right hind); the knee/hock flexes while a leg swings forward.
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6662F;
        float swing = Mth.cos(phase) * 1.1F * speed;
        float liftA = Math.max(0.0F, Mth.sin(phase)) * speed;
        float liftB = Math.max(0.0F, -Mth.sin(phase)) * speed;
        this.leftFrontLeg.xRot = swing;
        this.rightHindLeg.xRot = swing;
        this.rightFrontLeg.xRot = -swing;
        this.leftHindLeg.xRot = -swing;
        this.leftFrontLeg.getChild("lower").xRot = liftA * 1.3F;
        this.rightHindLeg.getChild("lower").xRot = liftA * 0.7F;
        this.rightFrontLeg.getChild("lower").xRot = liftB * 1.3F;
        this.leftHindLeg.getChild("lower").xRot = liftB * 0.7F;
        this.body.y += Mth.cos(phase * 2.0F) * 0.4F * speed;

        // Head: look direction split between neck and head, nodding with the stride.
        float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * DEG;
        float pitch = state.xRot * DEG;
        float nod = Mth.cos(phase * 2.0F) * 0.06F * speed;
        float neckX = NECK_X + pitch * 0.4F + nod;
        float headX = HEAD_X + pitch * 0.6F;
        float graze = state.grazeAmount;
        if (state.charging) {
            neckX = CHARGE_NECK_X;
            headX = CHARGE_HEAD_X;
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.6F) * 0.04F); // chewing
            this.neck.y += graze * 2.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.4F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.6F;

        // Ears: an occasional flick each, folded back when running.
        float leftFlick = pulse(age * 0.045F + seed);
        float rightFlick = pulse(age * 0.037F + seed + 2.0F);
        this.leftEar.zRot = EAR_Z + leftFlick * Mth.sin(age * 1.7F) * 0.35F;
        this.rightEar.zRot = -EAR_Z - rightFlick * Mth.sin(age * 1.9F) * 0.35F;
        this.leftEar.xRot = leftFlick * 0.25F - speed * 0.5F;
        this.rightEar.xRot = rightFlick * 0.25F - speed * 0.5F;

        // Tail: a short swish now and then, lifted when fleeing.
        this.tail.xRot = TAIL_X + speed * 0.9F;
        this.tail.zRot = pulse(age * 0.06F + seed * 1.3F) * Mth.sin(age * 1.3F) * 0.4F;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    private static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }
}
