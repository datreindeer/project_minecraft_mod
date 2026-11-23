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
 * Wild horse (tarpan / Przewalski type): stocky barrel, thick neck with a short erect mane and no forelock, a big
 * crested neck, a belly curving up into the flank, a heavy head with a long face and broad jowls, a dark tail with a short-haired dock. Back ~19 px.
 * Texture textures/entity/wildlife/wild_horse.png (128x64), painted by scripts/gen_wild_horse_texture.py whose BOXES
 * table mirrors the cubes below (keep them in sync).
 */
public class WildHorseModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wild_horse"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    /** Neck leans forward from the withers; the head hangs from the poll, face about 50 degrees down. */
    private static final float NECK_X = 0.6F;
    private static final float HEAD_X = 0.35F;
    private static final float GRAZE_NECK_X = 2.2F;
    private static final float GRAZE_HEAD_X = -0.15F;
    /** Alert: head up high, ears forward (when running away). */
    private static final float ALERT_NECK_X = 0.3F;
    private static final float TAIL_X = 0.35F;
    private static final float LIE_DROP = 8.0F, LIE_LEG_Y = 22.5F;

    private final ModelPart body;
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

    public WildHorseModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
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
        return createBodyLayer(CubeDeformation.NONE);
    }

    /** Inflated: the Hide Saddle Blanket and leather armour layers of the tamed horse (TameHorseRenderer), drawn over the same pose. */
    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        return LayerDefinition.create(mesh(g), 128, 64);
    }

    /**
     * Saddlebags: a Leather Satchel hanging on each flank behind the rider, children of the (inflated, so its strap
     * clears the blanket) body. Texture tame_horse_satchels.png paints only the bags and the strap over the back.
     */
    public static LayerDefinition createSatchelLayer() {
        MeshDefinition mesh = mesh(new CubeDeformation(0.45F));
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild(LEFT_SATCHEL, CubeListBuilder.create().texOffs(40, 30).addBox(5.0F, -3.0F, 1.0F, 3.0F, 7.0F, 7.0F), PartPose.ZERO);
        body.addOrReplaceChild(RIGHT_SATCHEL, CubeListBuilder.create().texOffs(40, 30).mirror().addBox(-8.0F, -3.0F, 1.0F, 3.0F, 7.0F, 7.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final String LEFT_SATCHEL = "left_satchel", RIGHT_SATCHEL = "right_satchel";

    private static MeshDefinition mesh(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The bare horse's barrel is a pixel shallower (the girth boxes below hang the belly back down); the inflated
        // blanket / armour / satchel copies keep the full box, so their textures still fit.
        boolean bare = g == CubeDeformation.NONE;
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -5.0F, -10.0F, 9.0F, bare ? 9.0F : 10.0F, 20.0F, g),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        // Build, only on the bare horse: withers and croup on top (hidden under the blanket or armour), chest, buttocks
        // and a belly that is deepest at the girth and curves up into the flank (hidden under armour only).
        body.addOrReplaceChild("build_top", bare ? CubeListBuilder.create()
                .texOffs(0, 40).addBox(-3.0F, -6.0F, -9.0F, 6.0F, 1.0F, 6.0F)
                .texOffs(24, 40).addBox(-4.0F, -6.0F, 3.0F, 8.0F, 1.0F, 6.0F) : CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("build", bare ? CubeListBuilder.create()
                .texOffs(52, 40).addBox(-3.5F, -3.0F, -11.0F, 7.0F, 8.0F, 1.0F)
                .texOffs(68, 40).addBox(-3.5F, -4.0F, 10.0F, 7.0F, 8.0F, 1.0F)
                .texOffs(0, 48).addBox(-4.0F, 4.0F, -9.0F, 8.0F, 2.0F, 10.0F)
                .texOffs(40, 50).addBox(-3.5F, 4.0F, 1.0F, 7.0F, 1.0F, 4.0F) : CubeListBuilder.create(), PartPose.ZERO);
        // Tail: short-haired dock, then the long dark hair hanging from it.
        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(114, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, g)
                        .texOffs(108, 6).addBox(-1.5F, 3.0F, -1.5F, 3.0F, 9.0F, 3.0F, g),
                PartPose.offsetAndRotation(0.0F, -4.0F, 9.5F, TAIL_X, 0.0F, 0.0F));

        // Neck built upwards (-y) and leaned forward; the erect mane stands on its crest (+z face) up to the poll.
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(58, 0).addBox(-2.0F, -9.0F, -3.0F, 4.0F, 10.0F, 6.0F, g)
                        .texOffs(78, 0).addBox(-1.0F, -10.0F, 2.5F, 2.0F, 10.0F, 2.0F, g),
                PartPose.offsetAndRotation(0.0F, -2.5F, -8.5F, NECK_X, 0.0F, 0.0F));
        // the crest: the upper neck thickens under the mane
        neck.addOrReplaceChild("crest", bare ? CubeListBuilder.create().texOffs(84, 48).addBox(-2.5F, -8.0F, 0.5F, 5.0F, 6.0F, 2.0F)
                : CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(86, 0).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 6.0F, g)
                        .texOffs(86, 11).addBox(-2.0F, -1.5F, -11.0F, 4.0F, 4.0F, 6.0F, g),
                PartPose.offsetAndRotation(0.0F, -8.5F, 0.0F, HEAD_X, 0.0F, 0.0F));
        // the broad jowls under the eyes
        head.addOrReplaceChild("jowls", bare ? CubeListBuilder.create().texOffs(84, 40).addBox(-3.0F, -0.5F, -4.5F, 6.0F, 3.0F, 4.0F)
                : CubeListBuilder.create(), PartPose.ZERO);
        // Ears point up against the head's downward tilt.
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(108, 0).mirror().addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F, g),
                PartPose.offsetAndRotation(1.6F, -2.0F, 0.0F, -0.85F, 0.0F, 0.15F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(108, 0).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F, g),
                PartPose.offsetAndRotation(-1.6F, -2.0F, 0.0F, -0.85F, 0.0F, -0.15F));

        addLeg(root, "left_front_leg", 2.7F, -7.5F, false, g);
        addLeg(root, "right_front_leg", -2.7F, -7.5F, false, g);
        addLeg(root, "left_hind_leg", 2.7F, 7.5F, true, g);
        addLeg(root, "right_hind_leg", -2.7F, 7.5F, true, g);
        return mesh;
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind, CubeDeformation g) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(12, 30).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 4.0F, g)
                : CubeListBuilder.create().mirror(left).texOffs(0, 30).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F, g);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 14.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(26, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, g),
                PartPose.offset(0.0F, 5.0F, hind ? 0.5F : 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        boolean armoured = state.armorColor != 0;
        body.getChild("build_top").visible = !armoured && !state.saddled;
        body.getChild("build").visible = !armoured;
        neck.getChild("crest").visible = !armoured;
        head.getChild("jowls").visible = !armoured;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F; // neck straight down in line with the body (it rises from the chest when standing)
            head.xRot = -1.25F; // head hanging limp below it, nose a little towards the belly
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
            this.tail.xRot = 0.5F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);

        if (state.dozing) {
            LyingPose.apply(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            // neck stretched forward and low, muzzle resting near the ground, turned a little to one side
            this.neck.xRot = 1.3F;
            this.neck.yRot = 0.4F;
            this.head.xRot = -0.5F;
            this.head.yRot = 0.2F;
            this.head.zRot = 0.15F;
            this.leftEar.xRot = -0.3F;
            this.rightEar.xRot = -0.3F;
            this.leftEar.zRot = 0.6F;
            this.rightEar.zRot = -0.6F;
            this.tail.xRot = 0.15F;
            this.tail.zRot = 0.3F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float run = Mth.clamp((speed - 0.45F) / 0.4F, 0.0F, 1.0F); // 0 walk/trot .. 1 gallop
        float phase = state.walkAnimationPos * 0.62F;

        // Trot: diagonal pairs. Gallop: each pair moves almost together, front and hind half a stride apart,
        // with the knees snapping up and the back rocking.
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
        this.body.y -= Mth.abs(Mth.sin(phase)) * (0.3F * speed + 0.9F * run);
        this.body.xRot = Mth.sin(phase) * 0.07F * run;

        // Head and neck: looking around; up and alert when running, pumping with the gallop.
        float yaw = Mth.clamp(state.yRot, -55.0F, 55.0F) * DEG;
        float pitch = state.xRot * DEG;
        float graze = state.grazeAmount;
        float neckX = Mth.lerp(run, NECK_X, ALERT_NECK_X) + pitch * 0.3F + Mth.cos(phase * 2.0F) * (0.05F * speed + 0.12F * run);
        float headX = HEAD_X + pitch * 0.6F;
        if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.55F) * 0.05F); // cropping grass
            this.neck.y += graze * 1.5F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.45F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.55F;

        // Ears: swivel on their own, laid back at a gallop.
        float leftFlick = pulse(age * 0.05F + seed);
        float rightFlick = pulse(age * 0.043F + seed + 2.5F);
        this.leftEar.yRot = leftFlick * Mth.sin(age * 0.8F) * 0.6F;
        this.rightEar.yRot = -rightFlick * Mth.sin(age * 0.9F) * 0.6F;
        this.leftEar.xRot -= run * 0.7F;
        this.rightEar.xRot -= run * 0.7F;

        // Tail: swishes at flies; streams out behind at a gallop.
        float swish = pulse(age * 0.06F + seed * 1.3F);
        this.tail.zRot = swish * Mth.sin(age * 1.1F) * 0.5F + Mth.sin(phase) * 0.12F * speed;
        this.tail.xRot = TAIL_X + speed * 0.3F + run * 0.7F;
    }

    /** 0 most of the time, a short smooth bump to 1 once per 2*PI of x. */
    private static float pulse(float x) {
        float s = Math.max(0.0F, Mth.sin(x));
        s *= s;
        s *= s;
        return s * s * s * s;
    }
}
