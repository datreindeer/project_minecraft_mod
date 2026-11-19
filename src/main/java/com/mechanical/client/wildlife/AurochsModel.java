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
 * Aurochs, bull and cow in one model: a long, deep-chested body on long legs (withers ~23 px, the bull's hump ~25 px),
 * a thick neck carried forward and a heavy head held low. The bull shows the hump and the big horns (out, forward and up,
 * tips turned in); the cow shows smaller lyre-shaped horns.
 * Textures: textures/entity/wildlife/aurochs_{bull,cow}.png (128x64), painted by scripts/gen_aurochs_texture.py whose
 * BOXES table mirrors the cubes below (keep them in sync).
 */
public class AurochsModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "aurochs"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    /** Neck runs forward from the chest, slightly down; the head hangs nose-down from it (steep cattle face). */
    private static final float NECK_X = 0.3F;
    private static final float HEAD_X = 0.55F;
    private static final float GRAZE_NECK_X = 1.15F;
    private static final float GRAZE_HEAD_X = 0.6F;
    /** Charging: neck level, forehead down so the horns point at the target. */
    private static final float CHARGE_NECK_X = 0.45F;
    private static final float CHARGE_HEAD_X = 1.15F;
    private static final float TAIL_X = 0.12F;
    /** Sleeping: body down 9 px (belly on the ground), folded legs at y = 22. */
    private static final float LIE_DROP = 9.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body;
    private final ModelPart hump;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart bullHorns;
    private final ModelPart cowHorns;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public AurochsModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.hump = this.body.getChild("hump");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.bullHorns = this.head.getChild("bull_horns");
        this.cowHorns = this.head.getChild("cow_horns");
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

        // Barrel plus a taller, deeper chest in front: the high withers and the belly line rising to the flank.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -5.0F, -10.0F, 12.0F, 10.0F, 20.0F)
                        .texOffs(64, 0).addBox(-6.5F, -7.0F, -12.0F, 13.0F, 13.0F, 9.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        // Bull's muscular shoulder hump, hidden on the cow.
        body.addOrReplaceChild("hump",
                CubeListBuilder.create().texOffs(64, 22).addBox(-4.0F, -9.0F, -11.5F, 8.0F, 3.0F, 7.0F), PartPose.ZERO);
        // Long cattle tail with a dark switch at the end.
        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(60, 46).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F)
                        .texOffs(14, 46).addBox(-1.0F, 9.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, 10.0F, TAIL_X, 0.0F, 0.0F));

        // Neck runs forward (-z) out of the chest, with the dewlap hanging under it.
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(0, 30).addBox(-3.0F, -4.0F, -7.0F, 6.0F, 8.0F, 8.0F)
                        .texOffs(0, 46).addBox(-1.0F, 4.0F, -6.0F, 2.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -11.0F, NECK_X, 0.0F, 0.0F));

        // Broad skull and a long, deep muzzle in front of it.
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(28, 30).addBox(-3.5F, -3.5F, -5.5F, 7.0F, 7.0F, 6.0F)
                        .texOffs(100, 32).addBox(-2.5F, -1.5F, -9.5F, 5.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.5F, -6.5F, HEAD_X, 0.0F, 0.0F));
        // Ears stick out sideways just below the horn roots.
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(50, 46).mirror().addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(4.5F, -1.5F, -1.0F, 0.0F, 0.0F, 0.35F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(50, 46).addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-4.5F, -1.5F, -1.0F, 0.0F, 0.0F, -0.35F));

        PartDefinition bullHorns = head.addOrReplaceChild("bull_horns", CubeListBuilder.create(), PartPose.ZERO);
        addBullHorn(bullHorns, "left_", 1.0F);
        addBullHorn(bullHorns, "right_", -1.0F);
        PartDefinition cowHorns = head.addOrReplaceChild("cow_horns", CubeListBuilder.create(), PartPose.ZERO);
        addCowHorn(cowHorns, "left_", 1.0F);
        addCowHorn(cowHorns, "right_", -1.0F);

        addLeg(root, "left_front_leg", 3.8F, -8.0F, false);
        addLeg(root, "right_front_leg", -3.8F, -8.0F, false);
        addLeg(root, "left_hind_leg", 3.5F, 7.5F, true);
        addLeg(root, "right_hind_leg", -3.5F, 7.5F, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * Bull horn: three segments, each built along -y and bent at its root: out sideways from the poll, then forward and
     * up, the thin tip turned in and up. side = +1 left, -1 right.
     */
    private static void addBullHorn(PartDefinition horns, String p, float side) {
        PartDefinition base = horns.addOrReplaceChild(p + "horn",
                CubeListBuilder.create().texOffs(22, 46).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F * side, -3.0F, -2.0F, 0.1F, 0.0F, 1.4F * side));
        PartDefinition mid = base.addOrReplaceChild(p + "horn_mid",
                CubeListBuilder.create().texOffs(30, 46).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, 0.0F, 0.95F, 0.0F, -0.5F * side));
        mid.addOrReplaceChild(p + "horn_tip",
                CubeListBuilder.create().texOffs(46, 46).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, 0.0F, 0.45F, 0.0F, -0.65F * side));
    }

    /** Cow horn: shorter and thinner, out and then up (lyre shape). */
    private static void addCowHorn(PartDefinition horns, String p, float side) {
        PartDefinition base = horns.addOrReplaceChild(p + "horn",
                CubeListBuilder.create().texOffs(38, 46).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(2.8F * side, -3.0F, -2.0F, 0.25F, 0.0F, 1.1F * side));
        base.addOrReplaceChild(p + "horn_tip",
                CubeListBuilder.create().texOffs(46, 46).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 0.45F, 0.0F, -0.95F * side));
    }

    /** Forearm / thigh with the cannon bone and hoof as a child, so the knee/hock can flex while walking and fold for lying. */
    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0; // left legs use the mirrored texture
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(70, 32).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 7.0F, 5.0F)
                : CubeListBuilder.create().mirror(left).texOffs(54, 32).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 13.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(88, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, hind ? 0.5F : 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.bullHorns.visible = state.showsHorns;
        this.hump.visible = state.showsHorns;
        this.cowHorns.visible = !state.showsHorns;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 0.1F; // the neck runs forward out of the chest: straight down
            head.xRot = 0.2F;
            leftEar.yRot = 1.0F; // sideways ears swung down
            rightEar.yRot = -1.0F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            // the neck runs along -z, so a negative yaw drops it towards the ground (+x); the renderer's head yaw does the rest
            this.neck.xRot = 0.3F;
            this.neck.yRot = -0.35F;
            this.head.xRot = 0.3F;
            this.head.yRot = state.yRot * DEG * 0.6F;
            this.leftEar.zRot = 1.2F;
            this.rightEar.zRot = -1.2F;
            this.tail.xRot = 0.6F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            // head down, chin near the ground, turned a little to one side
            this.neck.xRot = 0.65F;
            this.neck.yRot = 0.35F;
            this.head.xRot = 0.35F;
            this.head.yRot = 0.2F;
            this.head.zRot = 0.12F;
            this.leftEar.zRot = 0.8F;
            this.rightEar.zRot = -0.8F;
            this.tail.xRot = 0.55F;
            this.tail.zRot = 0.4F;
            return;
        }

        // Heavy, deliberate walk: diagonal pairs, knees and hocks flex as each leg swings forward.
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6F;
        float swing = Mth.cos(phase) * 0.9F * speed;
        float liftA = Math.max(0.0F, Mth.sin(phase)) * speed;
        float liftB = Math.max(0.0F, -Mth.sin(phase)) * speed;
        this.leftFrontLeg.xRot = swing;
        this.rightHindLeg.xRot = swing;
        this.rightFrontLeg.xRot = -swing;
        this.leftHindLeg.xRot = -swing;
        this.leftFrontLeg.getChild("lower").xRot = liftA * 1.1F;
        this.rightHindLeg.getChild("lower").xRot = liftA * 0.6F;
        this.rightFrontLeg.getChild("lower").xRot = liftB * 1.1F;
        this.leftHindLeg.getChild("lower").xRot = liftB * 0.6F;
        this.body.y += Mth.cos(phase * 2.0F) * 0.35F * speed;
        this.body.zRot = Mth.sin(phase) * 0.025F * speed; // the big body rolls with the stride

        // Head: a thick neck, so most of the turn is in the head; nods with the stride.
        float yaw = Mth.clamp(state.yRot, -45.0F, 45.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -25.0F, 25.0F) * DEG;
        float neckX = NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.07F * speed;
        float headX = HEAD_X + pitch * 0.5F;
        float graze = state.grazeAmount;
        if (state.charging) {
            neckX = CHARGE_NECK_X;
            headX = CHARGE_HEAD_X + Mth.sin(age * 0.5F) * 0.08F; // hooking with the horns
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F); // chewing
            this.neck.y += graze * 2.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.35F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.65F;

        // Ears: flick at flies now and then, pinned back when charging.
        float leftFlick = pulse(age * 0.05F + seed);
        float rightFlick = pulse(age * 0.041F + seed + 2.0F);
        float pinned = state.charging ? 0.6F : 0.0F;
        this.leftEar.zRot = 0.35F - leftFlick * Mth.sin(age * 1.6F) * 0.4F - pinned;
        this.rightEar.zRot = -0.35F + rightFlick * Mth.sin(age * 1.8F) * 0.4F + pinned;
        this.leftEar.yRot = pinned * 0.6F;
        this.rightEar.yRot = -pinned * 0.6F;

        // Tail: lazy swishing against flies, more often when standing; raised when running or charging.
        float swish = pulse(age * 0.07F + seed * 1.3F);
        this.tail.zRot = swish * Mth.sin(age * 0.9F) * 0.55F + Mth.sin(phase) * 0.1F * speed;
        this.tail.xRot = TAIL_X + speed * 0.5F + (state.charging ? 0.6F : 0.0F);
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    private static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }
}
