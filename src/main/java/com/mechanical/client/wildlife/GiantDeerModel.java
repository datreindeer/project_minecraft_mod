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
 * Giant deer (Megaloceros): a deer a third bigger than the red deer stag, with a big shoulder hump, a shaggy throat
 * collar and enormous palmate antlers: each a burr, a beam sweeping out sideways with brow and bez tines forward, and a
 * broad palm with tines along its front, outer and back edges (span almost three blocks).
 * Texture: textures/entity/wildlife/giant_deer.png (128x128), painted by scripts/gen_giant_deer_texture.py, whose part
 * tree mirrors the cubes below (keep them in sync).
 */
public class GiantDeerModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "giant_deer"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float NECK_X = 0.75F, HEAD_X = -0.65F;
    private static final float GRAZE_NECK_X = 2.45F, GRAZE_HEAD_X = -1.15F;
    /** Charging: neck lowered, nose down, so the palms point at the target like a plough. */
    private static final float CHARGE_NECK_X = 1.3F, CHARGE_HEAD_X = 0.2F;
    private static final float EAR_Z = 1.0F, TAIL_X = 0.3F;
    /** Long, slow strides. */
    private static final float GAIT = 0.5F;

    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public GiantDeerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = body.getChild("tail");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.legs = new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Barrel, a deeper and wider chest in front, and the shoulder hump on top of it.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -5.0F, -11.0F, 10.0F, 10.0F, 22.0F)
                        .texOffs(64, 0).addBox(-5.5F, -5.5F, -12.0F, 11.0F, 12.0F, 8.0F)
                        .texOffs(46, 47).addBox(-4.0F, -8.0F, -11.0F, 8.0F, 3.0F, 9.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 59).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, 11.0F, TAIL_X, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(102, 0).addBox(-2.5F, -12.0F, -3.0F, 5.0F, 13.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -10.0F, NECK_X, 0.0F, 0.0F));
        neck.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(0, 32).addBox(-3.5F, -8.0F, -4.5F, 7.0F, 9.0F, 6.0F), PartPose.ZERO);
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(44, 32).addBox(-3.0F, -4.0F, -5.0F, 6.0F, 6.0F, 7.0F)
                        .texOffs(80, 47).addBox(-2.0F, -1.5F, -11.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -11.5F, 0.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(48, 59).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, -3.0F, 1.5F, 0.0F, 0.0F, EAR_Z));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(48, 59).mirror().addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, -3.0F, 1.5F, 0.0F, 0.0F, -EAR_Z));
        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, -1.0F));
        addAntler(antlers, "left_", 1.0F);
        addAntler(antlers, "right_", -1.0F);

        addLeg(root, "left_front_leg", 3.2F, -8.5F, false, false);
        addLeg(root, "right_front_leg", -3.2F, -8.5F, false, true);
        addLeg(root, "left_hind_leg", 2.9F, 8.4F, true, false); // 0.1 inside the barrel sides so the faces do not z-fight
        addLeg(root, "right_hind_leg", -2.9F, 8.4F, true, true);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** x of a box on this side: the right antler is the left one mirrored. */
    private static float x(float side, float x0, float w) {
        return side > 0 ? x0 : -(x0 + w);
    }

    private static CubeListBuilder box(float side, int u, int v, float x0, float y0, float z0, float w, float h, float d) {
        CubeListBuilder b = CubeListBuilder.create().texOffs(u, v);
        if (side < 0) b.mirror();
        return b.addBox(x(side, x0, w), y0, z0, w, h, d);
    }

    private static void addAntler(PartDefinition antlers, String p, float side) {
        CubeListBuilder beamCubes = box(side, 12, 59, -0.5F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F); // burr
        beamCubes.texOffs(54, 59).addBox(x(side, 0.0F, 8.0F), -1.0F, -1.0F, 8.0F, 2.0F, 2.0F);
        PartDefinition beam = antlers.addOrReplaceChild(p + "beam", beamCubes, PartPose.offsetAndRotation(2.5F * side, 0.0F, 0.0F, -0.15F, 0.0F, -0.22F * side));
        beam.addOrReplaceChild(p + "brow_tine", box(side, 100, 47, -0.5F, -0.5F, -6.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(2.0F * side, 0.0F, -0.5F, -0.4F, 0.25F * side, 0.0F));
        beam.addOrReplaceChild(p + "bez_tine", box(side, 28, 59, -0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(5.0F * side, 0.0F, -0.5F, -0.7F, 0.1F * side, 0.0F));
        CubeListBuilder palm = box(side, 0, 47, 0.0F, -0.5F, -5.0F, 12.0F, 1.0F, 11.0F);
        palm.texOffs(82, 59).addBox(x(side, 3.0F, 1.0F), -0.5F, -8.0F, 1.0F, 1.0F, 3.0F)
                .texOffs(38, 59).addBox(x(side, 6.0F, 1.0F), -0.5F, -8.5F, 1.0F, 1.0F, 4.0F)
                .texOffs(90, 59).addBox(x(side, 9.0F, 1.0F), -0.5F, -8.0F, 1.0F, 1.0F, 3.0F)
                .texOffs(108, 59).addBox(x(side, 12.0F, 3.0F), -0.5F, -4.0F, 3.0F, 1.0F, 1.0F)
                .texOffs(98, 59).addBox(x(side, 12.0F, 4.0F), -0.5F, -0.5F, 4.0F, 1.0F, 1.0F)
                .texOffs(116, 59).addBox(x(side, 12.0F, 3.0F), -0.5F, 3.0F, 3.0F, 1.0F, 1.0F)
                .texOffs(74, 59).addBox(x(side, 10.0F, 1.0F), -0.5F, 6.0F, 1.0F, 1.0F, 3.0F);
        beam.addOrReplaceChild(p + "palm", palm, PartPose.offsetAndRotation(8.0F * side, 0.0F, 0.0F, 0.45F, 0.0F, 0.0F));
    }

    /** Upper leg (forearm / thigh) with the cannon bone and hoof as the "lower" child. */
    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind, boolean mirror) {
        CubeListBuilder upper = CubeListBuilder.create().texOffs(hind ? 26 : 70, 32);
        CubeListBuilder lower = CubeListBuilder.create().texOffs(hind ? 0 : 114, hind ? 59 : 47);
        if (mirror) {
            upper.mirror();
            lower.mirror();
        }
        if (hind) {
            PartDefinition leg = root.addOrReplaceChild(name, upper.addBox(-2.0F, -1.0F, -2.5F, 4.0F, 10.0F, 5.0F), PartPose.offset(x, 12.0F, z));
            leg.addOrReplaceChild("lower", lower.addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 9.0F, 0.5F));
        } else {
            PartDefinition leg = root.addOrReplaceChild(name, upper.addBox(-2.0F, -1.0F, -2.0F, 4.0F, 9.0F, 4.0F), PartPose.offset(x, 12.0F, z));
            leg.addOrReplaceChild("lower", lower.addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        }
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            neck.xRot = 1.5F;
            head.xRot = 0.5F;
            leftEar.zRot = 1.3F;
            rightEar.zRot = -1.3F;
            tail.xRot = 0.1F;
            return;
        }

        if (state.dozing) {
            // lying down: chest (bottom 20.5) dropped onto the ground, legs folded at knee and hock, the heavy head
            // and antlers resting low and turned aside
            body.y += 10.0F;
            for (ModelPart leg : legs) {
                leg.y += 9.5F;
                leg.xRot = -1.4F;
                leg.getChild("lower").xRot = 2.9F;
            }
            leftHindLeg.yRot = -0.35F;
            rightHindLeg.yRot = 0.35F;
            neck.xRot = 1.5F;
            neck.yRot = 0.4F;
            head.xRot = -1.1F;
            body.y += Mth.sin(state.ageInTicks * 0.06F) * 0.2F;
            return;
        }

        float age = state.ageInTicks;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * GAIT;
        float swing = Mth.cos(phase) * 1.0F * speed;
        float liftA = Math.max(0.0F, Mth.sin(phase)) * speed;
        float liftB = Math.max(0.0F, -Mth.sin(phase)) * speed;
        leftFrontLeg.xRot = swing;
        rightHindLeg.xRot = swing;
        rightFrontLeg.xRot = -swing;
        leftHindLeg.xRot = -swing;
        leftFrontLeg.getChild("lower").xRot = liftA * 1.2F;
        rightHindLeg.getChild("lower").xRot = liftA * 0.6F;
        rightFrontLeg.getChild("lower").xRot = liftB * 1.2F;
        leftHindLeg.getChild("lower").xRot = liftB * 0.6F;
        body.y += Mth.cos(phase * 2.0F) * 0.5F * speed;

        // The weight of the antlers: head turns are slow and mostly from the neck, with a heavy nod in the stride.
        float yaw = Mth.clamp(state.yRot, -45.0F, 45.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float nod = Mth.cos(phase * 2.0F) * 0.07F * speed;
        float neckX = NECK_X + pitch * 0.4F + nod;
        float headX = HEAD_X + pitch * 0.5F;
        float graze = state.grazeAmount;
        if (state.charging) {
            neckX = CHARGE_NECK_X + Mth.sin(age * 0.5F) * 0.05F;
            headX = CHARGE_HEAD_X;
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.04F);
            neck.y += graze * 3.0F;
            yaw *= 1.0F - graze;
        }
        neck.xRot = neckX;
        neck.yRot = yaw * 0.6F;
        head.xRot = headX;
        head.yRot = yaw * 0.4F;

        float flick = Mth.sin(age * 0.05F) > 0.95F ? Mth.sin(age * 1.6F) * 0.3F : 0.0F;
        leftEar.zRot = EAR_Z + flick;
        rightEar.zRot = -EAR_Z - flick;
        leftEar.xRot = rightEar.xRot = -speed * 0.4F - (state.charging ? 0.6F : 0.0F);
        tail.xRot = TAIL_X + speed * 0.6F;
        tail.zRot = Mth.sin(age * 0.2F) * 0.15F;
    }
}
