package com.mechanical.client.wildlife.warm;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Water buffalo: a massive deep body (about 1.6 m at the withers) with the hump of the withers and a sagging belly, a
 * thick wrinkled neck carrying the long head low and forward, a broad wet muzzle, droopy ears under the horns, and (grown
 * animals, {@code showsHorns}) the huge flat ridged horns sweeping out and back from the brow in a wide crescent, four
 * segments under "horns". Short thick legs on big splayed hooves; a thin tasselled tail.
 * {@link WarmRenderState#wallowing}: lying in shallow water or mud, the head stretched out low with the chin in the water.
 * Parts, boxes and the skin textures/entity/wildlife/water_buffalo.png (128x128, plus the skinned carcass skin) come from
 * scripts/gen_owarm_hoofed_models.py ({@link WarmHoofGeometry}).
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
    /** Lying: body down 10.5 px (belly on the ground), folded legs at y = 21.5. */
    private static final float LIE_DROP = 10.5F, LIE_LEG_Y = 21.5F;

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
        return WarmHoofGeometry.waterBuffalo();
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
                this.neck.xRot = 0.1F;
                this.neck.yRot = Mth.sin(age * 0.02F + seed) * 0.12F;
                this.head.xRot = Mth.sin(age * 0.05F) * 0.03F;
                this.head.yRot = 0.0F;
                this.leftEar.zRot = EAR_Z - MammalPose.flick(age + seed * 7.0F, 90.0F, 14.0F) * 0.6F;
                this.rightEar.zRot = -EAR_Z + MammalPose.flick(age + 40.0F, 110.0F, 14.0F) * 0.6F;
                this.tail.xRot = 0.2F;
                this.tail.zRot = MammalPose.flick(age + 20.0F, 70.0F, 20.0F) * 0.4F;
                return;
            }
            // asleep: head down on the ground, turned a little to one side
            this.neck.xRot = 0.1F; // the long head stretched out, chin on the ground
            this.neck.yRot = 0.35F;
            this.head.xRot = 0.1F;
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

        if (state.swimming) {
            // swims low like a hippo: only the head and the swept horns above water, nose held high so the horns lie back,
            // droopy ears floating out flat; short churning strokes, tail trailing on the water
            float stroke = age * 0.3F;
            MammalPose.walk(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg, stroke, 0.6F, 1.0F);
            this.neck.xRot = NECK_X - 0.3F;
            this.head.xRot = HEAD_X - 0.75F + Mth.sin(stroke * 2.0F) * 0.03F;
            this.leftEar.zRot = EAR_Z + 0.5F;
            this.rightEar.zRot = -EAR_Z - 0.5F;
            this.tail.xRot = 1.3F;
            this.tail.zRot = Mth.sin(stroke) * 0.25F;
        }
    }
}
