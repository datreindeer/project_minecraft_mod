package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.wildlife.Reindeer;

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
 * Reindeer, bull and cow in one model: stocky body on shortish legs with big splayed hooves, a pale shaggy throat mane,
 * a broad furred muzzle and long C-shaped antlers with a forward "shovel" over the face. Both sexes carry antlers;
 * the cow's are drawn at {@link #COW_ANTLER_SCALE}. Shoulder ~18 px.
 * Textures: textures/entity/wildlife/reindeer_{bull,cow}.png (128x64), painted by scripts/gen_reindeer_texture.py
 * whose BOXES table mirrors the cubes below (keep UVs in sync).
 */
public class ReindeerModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "reindeer"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float COW_ANTLER_SCALE = 0.68F;
    private static final float NECK_X = 0.85F;
    private static final float HEAD_X = -0.5F;
    private static final float GRAZE_NECK_X = 2.4F;
    private static final float GRAZE_HEAD_X = -1.1F;
    private static final float DOZE_NECK_X = 1.5F;
    private static final float DOZE_HEAD_X = -0.6F;
    private static final float DOZE_DROP = 7.5F;
    private static final float EAR_Z = 0.85F;
    private static final float TAIL_X = 0.3F;

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

    public ReindeerModel(ModelPart root) {
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

        // Deep, round barrel and a broad chest; belly at y=15.5, so the legs are short for a deer.
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -4.5F, -9.0F, 9.0F, 9.0F, 18.0F)
                .texOffs(54, 0).addBox(-5.0F, -5.0F, -10.0F, 10.0F, 10.0F, 7.0F),
            PartPose.offset(0.0F, 11.0F, 0.0F)
        );
        body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(72, 17).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -4.0F, 9.0F, TAIL_X, 0.0F, 0.0F)
        );
        PartDefinition neck = body.addOrReplaceChild(
            "neck",
            CubeListBuilder.create().texOffs(88, 0).addBox(-2.5F, -8.0F, -2.5F, 5.0F, 9.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, -2.5F, -8.5F, NECK_X, 0.0F, 0.0F)
        );
        // Long pale hair hanging from the underside of the neck (its -z face once the neck tips forward).
        neck.addOrReplaceChild(
            "mane", CubeListBuilder.create().texOffs(54, 17).addBox(-2.0F, -7.0F, -4.5F, 4.0F, 8.0F, 2.0F), PartPose.ZERO
        );
        PartDefinition head = neck.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(108, 0).addBox(-2.5F, -3.0F, -4.0F, 5.0F, 5.0F, 5.0F)
                .texOffs(108, 10).addBox(-2.0F, -2.0F, -8.0F, 4.0F, 4.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -7.5F, 0.0F, HEAD_X, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
            "left_ear",
            CubeListBuilder.create().texOffs(66, 17).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
            PartPose.offsetAndRotation(2.0F, -2.5F, 0.5F, 0.0F, 0.0F, EAR_Z)
        );
        head.addOrReplaceChild(
            "right_ear",
            CubeListBuilder.create().texOffs(66, 17).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
            PartPose.offsetAndRotation(-2.0F, -2.5F, 0.5F, 0.0F, 0.0F, -EAR_Z)
        );

        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(), PartPose.ZERO);
        addAntler(antlers, "left_", 1.0F, true);
        addAntler(antlers, "right_", -1.0F, false);

        addLeg(root, "left_front_leg", 2.5F, -7.0F, false);
        addLeg(root, "right_front_leg", -2.5F, -7.0F, false);
        addLeg(root, "left_hind_leg", 2.5F, 6.5F, true);
        addLeg(root, "right_hind_leg", -2.5F, 6.5F, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * One antler: the beam sweeps up and back, then curves forward into a small palmate crown with three points; a bez
     * tine reaches forward and one point back. The left side carries the palmate brow "shovel" over the face, the right
     * a plain brow tine (most reindeer have just one shovel). side = +1 left, -1 right.
     */
    private static void addAntler(PartDefinition antlers, String p, float side, boolean shovel) {
        CubeListBuilder lowBeam = CubeListBuilder.create().texOffs(78, 17).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F);
        PartDefinition beam = antlers.addOrReplaceChild(p + "antler", lowBeam, PartPose.offsetAndRotation(1.5F * side, -3.0F, 0.5F, -0.9F, 0.0F, 0.5F * side));
        if (shovel) {
            beam.addOrReplaceChild(p + "shovel", CubeListBuilder.create().texOffs(96, 21).addBox(-0.5F, -2.0F, -5.0F, 1.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -1.2F, 0.0F, 0.7F, -0.25F * side, -0.35F * side));
        } else {
            beam.addOrReplaceChild(p + "brow_tine", longTine(), PartPose.offsetAndRotation(0.0F, -1.2F, 0.0F, 1.5F, 0.0F, -0.2F * side));
        }
        beam.addOrReplaceChild(p + "bez_tine", longTine(), PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 1.4F, -0.15F * side, 0.0F));
        CubeListBuilder highBeam = CubeListBuilder.create().texOffs(82, 17).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F);
        PartDefinition upper = beam.addOrReplaceChild(p + "upper_beam", highBeam, PartPose.offsetAndRotation(0.0F, -6.5F, 0.0F, 0.9F, 0.0F, 0.25F * side));
        upper.addOrReplaceChild(p + "back_tine", tine(), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, -1.3F, 0.0F, 0.2F * side));
        PartDefinition crown = upper.addOrReplaceChild(p + "crown", CubeListBuilder.create().texOffs(86, 17).addBox(-0.5F, -4.0F, -2.0F, 1.0F, 4.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, -6.5F, 0.0F, 0.6F, 0.0F, -0.15F * side));
        crown.addOrReplaceChild(p + "crown_1", tine(), PartPose.offsetAndRotation(0.0F, -3.5F, -1.5F, 0.5F, 0.0F, 0.2F * side));
        crown.addOrReplaceChild(p + "crown_2", tine(), PartPose.offsetAndRotation(0.0F, -3.5F, -0.25F, -0.1F, 0.0F, 0.0F));
        crown.addOrReplaceChild(p + "crown_3", tine(), PartPose.offsetAndRotation(0.0F, -3.5F, 0.5F, -0.6F, 0.0F, -0.2F * side));
    }

    private static CubeListBuilder tine() {
        return CubeListBuilder.create().texOffs(94, 17).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F);
    }

    private static CubeListBuilder longTine() {
        return CubeListBuilder.create().texOffs(110, 18).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F);
    }

    /** Upper leg, then a short cannon with the big, wide hoof (toes reaching forward) as a child. */
    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        PartDefinition leg;
        if (hind) {
            leg = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(32, 27).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 8.0F, 4.0F), PartPose.offset(x, 14.0F, z));
            leg.addOrReplaceChild("lower", CubeListBuilder.create()
                    .texOffs(46, 27).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                    .texOffs(20, 27).addBox(-1.5F, 1.0F, -2.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 7.0F, 0.5F));
        } else {
            leg = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 27).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(x, 14.0F, z));
            leg.addOrReplaceChild("lower", CubeListBuilder.create()
                    .texOffs(12, 27).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F)
                    .texOffs(20, 27).addBox(-1.5F, 2.0F, -2.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        }
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.antlers.visible = state.showsHorns;
        if (!Reindeer.BULL_TEXTURE.equals(state.texture)) {
            this.antlers.xScale = COW_ANTLER_SCALE;
            this.antlers.yScale = COW_ANTLER_SCALE;
            this.antlers.zScale = COW_ANTLER_SCALE;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) {
                leg.zRot = CarcassRenderer.LIMB_SAG;
            }
            this.neck.xRot = 1.5F;
            this.head.xRot = 0.4F;
            this.leftEar.zRot = 1.3F;
            this.rightEar.zRot = -1.3F;
            this.tail.xRot = 0.1F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            this.body.y += DOZE_DROP + Mth.sin(age * 0.08F + seed) * 0.12F;
            foldLeg(this.leftFrontLeg, -1.3F, 2.75F, 2.5F, 0.15F);
            foldLeg(this.rightFrontLeg, -1.3F, 2.75F, 2.5F, -0.15F);
            foldLeg(this.leftHindLeg, -1.35F, 2.9F, 0.0F, 0.25F);
            foldLeg(this.rightHindLeg, -1.35F, 2.9F, 0.0F, -0.2F);
            this.neck.xRot = DOZE_NECK_X;
            this.neck.yRot = -0.3F;
            this.head.xRot = DOZE_HEAD_X;
            this.head.yRot = -0.25F;
            this.head.zRot = -0.12F;
            this.leftEar.zRot = EAR_Z + 0.3F;
            this.rightEar.zRot = -EAR_Z - 0.3F;
            this.leftEar.xRot = -0.4F + pulse(age * 0.03F + seed) * Mth.sin(age * 1.5F) * 0.3F;
            this.rightEar.xRot = -0.4F;
            this.tail.xRot = 0.05F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6662F;
        float swing = Mth.cos(phase) * 1.0F * speed;
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

        float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * DEG;
        float pitch = state.xRot * DEG;
        float nod = Mth.cos(phase * 2.0F) * 0.06F * speed;
        float neckX = NECK_X + pitch * 0.4F + nod;
        float headX = HEAD_X + pitch * 0.6F;
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.6F) * 0.04F); // chewing lichen
            this.neck.y += graze * 2.0F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.4F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.6F;
        // the throat mane swings with the stride
        this.mane.xRot = Mth.sin(phase) * 0.06F * speed;

        float leftFlick = pulse(age * 0.045F + seed);
        float rightFlick = pulse(age * 0.037F + seed + 2.0F);
        this.leftEar.zRot = EAR_Z + leftFlick * Mth.sin(age * 1.7F) * 0.35F;
        this.rightEar.zRot = -EAR_Z - rightFlick * Mth.sin(age * 1.9F) * 0.35F;
        this.leftEar.xRot = leftFlick * 0.25F - speed * 0.5F;
        this.rightEar.xRot = rightFlick * 0.25F - speed * 0.5F;

        // short white tail flicks up when the herd runs
        this.tail.xRot = TAIL_X + speed * 1.0F;
        this.tail.zRot = pulse(age * 0.06F + seed * 1.3F) * Mth.sin(age * 1.3F) * 0.4F;
    }

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
