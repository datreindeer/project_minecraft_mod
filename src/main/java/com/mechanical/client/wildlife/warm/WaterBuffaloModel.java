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
 * Water buffalo: a huge, low-slung barrel (back ~20 px) on short thick legs with big splayed hooves, a short thick neck
 * carrying the long head low and forward, droopy ears, and (grown animals, {@code showsHorns}) the huge horns: broad
 * flat ridged bases sweeping out sideways and back in a wide crescent, the tips turned up and in.
 * {@link WarmRenderState#wallowing}: lying in shallow water or mud, the head stretched out low with the chin in the water.
 * Texture textures/entity/wildlife/water_buffalo.png (128x64), painted by scripts/gen_fauna_warm_tex_mammals.py whose
 * BUFFALO_BOXES table mirrors the cubes below (keep them in sync).
 */
public class WaterBuffaloModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "water_buffalo"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    /** Neck runs forward out of the chest, barely dropping; the head is carried low, face forward more than down. */
    private static final float NECK_X = 0.2F;
    private static final float HEAD_X = 0.35F;
    private static final float GRAZE_NECK_X = 0.95F;
    private static final float GRAZE_HEAD_X = 0.75F;
    /** Charging: head down, forehead forward so the horns sweep at the target. */
    private static final float CHARGE_NECK_X = 0.35F;
    private static final float CHARGE_HEAD_X = 1.0F;
    private static final float TAIL_X = 0.1F;
    private static final float EAR_Z = 0.6F;
    /** Lying: body down 8 px (belly on the ground), folded legs at y = 22. */
    private static final float LIE_DROP = 8.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart horns;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart[] legs;

    public WaterBuffaloModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.horns = this.head.getChild("horns");
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

        // One massive barrel, wider than it is tall, low on the legs.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -6.0F, -11.0F, 14.0F, 12.0F, 22.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        // Long thin tail with a small dark switch.
        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(122, 16).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 11.0F, 1.0F)
                        .texOffs(100, 26).addBox(-1.0F, 10.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, 11.0F, TAIL_X, 0.0F, 0.0F));

        // Short, thick neck running forward (-z) out of the chest.
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(72, 0).addBox(-3.5F, -4.5F, -6.0F, 7.0F, 8.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, -10.5F, NECK_X, 0.0F, 0.0F));
        // Broad skull and a long muzzle in front of it.
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(100, 0).addBox(-3.5F, -3.5F, -5.0F, 7.0F, 7.0F, 5.0F)
                        .texOffs(72, 16).addBox(-2.5F, -1.0F, -9.0F, 5.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, HEAD_X, 0.0F, 0.0F));
        // Ears stick out sideways under the horn roots and droop.
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(90, 16).mirror().addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(4.5F, -0.5F, -1.5F, 0.0F, 0.0F, EAR_Z));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(90, 16).addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-4.5F, -0.5F, -1.5F, 0.0F, 0.0F, -EAR_Z));

        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        addHorn(horns, "left_", 1.0F);
        addHorn(horns, "right_", -1.0F);

        addLeg(root, "left_front_leg", 4.5F, -8.0F, false);
        addLeg(root, "right_front_leg", -4.5F, -8.0F, false);
        addLeg(root, "left_hind_leg", 4.5F, 8.0F, true);
        addLeg(root, "right_hind_leg", -4.5F, 8.0F, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * Horn: three segments, each built along -y and bent at its root. The base is broad front-to-back and flat (a ridged
     * plate) and points out sideways, just above level; the middle sweeps back; the thin tip turns up and in.
     * side = +1 left, -1 right.
     */
    private static void addHorn(PartDefinition horns, String p, float side) {
        PartDefinition base = horns.addOrReplaceChild(p + "horn",
                CubeListBuilder.create().texOffs(100, 16).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(2.5F * side, -3.0F, -2.5F, -0.15F, 0.0F, 1.5F * side));
        PartDefinition mid = base.addOrReplaceChild(p + "horn_mid",
                CubeListBuilder.create().texOffs(110, 16).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, -0.75F, 0.0F, -0.35F * side));
        mid.addOrReplaceChild(p + "horn_tip",
                CubeListBuilder.create().texOffs(118, 16).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, -0.45F, 0.0F, -0.85F * side));
    }

    /** Thick forearm / thigh, the short cannon bone with the big splayed hoof as a child (knee / hock flex, folds for lying). */
    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(20, 36).addBox(-2.5F, -1.0F, -3.0F, 5.0F, 5.0F, 6.0F)
                : CubeListBuilder.create().mirror(left).texOffs(0, 36).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 5.0F, 5.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 15.0F, z));
        leg.addOrReplaceChild("lower",
                CubeListBuilder.create().mirror(left)
                        .texOffs(42, 36).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                        .texOffs(58, 36).addBox(-2.5F, 4.0F, -3.0F, 5.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 4.0F, hind ? 0.5F : 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.horns.visible = state.showsHorns;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 0.0F; // the neck runs forward out of the chest: straight down
            head.xRot = 0.1F;
            leftEar.zRot = 1.3F; // ears hang limp
            rightEar.zRot = -1.3F;
            tail.xRot = -1.9F; // flops down over the back
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : this.legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            // the neck runs along -z, so a negative yaw drops it towards the ground (+x); the renderer's head yaw does the rest
            this.neck.xRot = 0.25F;
            this.neck.yRot = -0.35F;
            this.head.xRot = 0.25F;
            this.head.yRot = state.yRot * DEG * 0.6F;
            this.leftEar.zRot = 1.3F;
            this.rightEar.zRot = -1.3F;
            this.tail.xRot = 0.6F;
            return;
        }

        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);
        boolean wallowing = state instanceof WarmRenderState w && w.wallowing;

        if (state.dozing || wallowing) {
            MammalPose.lie(this.body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg},
                    new ModelPart[] {leftHindLeg, rightHindLeg});
            if (wallowing) {
                // neck stretched straight out, chin down in the water, now and then a slow ear flick at the flies
                this.body.y += 0.5F;
                this.neck.xRot = 0.3F;
                this.neck.yRot = Mth.sin(age * 0.02F + seed) * 0.12F;
                this.head.xRot = 0.05F + Mth.sin(age * 0.05F) * 0.03F;
                this.head.yRot = 0.0F;
                this.leftEar.zRot = EAR_Z - MammalPose.flick(age + seed * 7.0F, 90.0F, 14.0F) * 0.6F;
                this.rightEar.zRot = -EAR_Z + MammalPose.flick(age + 40.0F, 110.0F, 14.0F) * 0.6F;
                this.tail.xRot = 0.2F;
                this.tail.zRot = MammalPose.flick(age + 20.0F, 70.0F, 20.0F) * 0.4F;
                return;
            }
            // asleep: head down on the ground, turned a little to one side
            this.neck.xRot = 0.55F;
            this.neck.yRot = 0.35F;
            this.head.xRot = 0.3F;
            this.head.yRot = 0.2F;
            this.head.zRot = 0.12F;
            this.leftEar.zRot = 0.9F;
            this.rightEar.zRot = -0.9F;
            this.tail.xRot = 0.5F;
            this.tail.zRot = 0.4F;
            return;
        }

        // Ponderous walk: short steps, diagonal pairs, the huge body rolling with the stride.
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6F;
        MammalPose.walk(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg, phase, 0.8F * speed, speed);
        this.body.y += Mth.cos(phase * 2.0F) * 0.35F * speed;
        this.body.zRot = Mth.sin(phase) * 0.03F * speed;

        float yaw = Mth.clamp(state.yRot, -40.0F, 40.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float neckX = NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed;
        float headX = HEAD_X + pitch * 0.5F;
        float graze = state.grazeAmount;
        if (state.charging) {
            neckX = CHARGE_NECK_X;
            headX = CHARGE_HEAD_X + Mth.sin(age * 0.45F) * 0.1F; // tossing the horns
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F); // chewing
            this.neck.y += graze * 2.0F;
            yaw *= 1.0F - graze;
        }
        this.neck.xRot = neckX;
        this.neck.yRot = yaw * 0.3F;
        this.head.xRot = headX;
        this.head.yRot = yaw * 0.7F;

        // Droopy ears flap at flies, pinned back when charging.
        float leftFlick = MammalPose.pulse(age * 0.05F + seed);
        float rightFlick = MammalPose.pulse(age * 0.041F + seed + 2.0F);
        float pinned = state.charging ? 0.5F : 0.0F;
        this.leftEar.zRot = EAR_Z - leftFlick * Mth.sin(age * 1.5F) * 0.45F - pinned;
        this.rightEar.zRot = -EAR_Z + rightFlick * Mth.sin(age * 1.7F) * 0.45F + pinned;
        this.leftEar.yRot = pinned * 0.6F;
        this.rightEar.yRot = -pinned * 0.6F;

        float swish = MammalPose.pulse(age * 0.07F + seed * 1.3F);
        this.tail.zRot = swish * Mth.sin(age * 0.9F) * 0.55F + Mth.sin(phase) * 0.1F * speed;
        this.tail.xRot = TAIL_X + speed * 0.4F + (state.charging ? 0.7F : 0.0F);
    }
}
