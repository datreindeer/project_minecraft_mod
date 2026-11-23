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
 * Leopard: a lithe cat, smaller and lower than the cave lion - long body with a deep chest and muscular shoulders, round
 * head with a short muzzle and small round ears, big paws, and a long thick tail in three segments that hangs in a low curve with the tip
 * turned up. Moves and hunts like the cave lion (same states as the shared PredatorModel: trot and bound, nose to the
 * ground, crouched stalk, pounce, the rasping call), but sleeps draped over a branch with all four legs and the tail
 * dangling. Parts, boxes and the 128x64 rosetted skin (textures/entity/wildlife/leopard.png plus the skinned carcass skin)
 * come from one table in scripts/gen_owarm_reptiles_models.py ({@link WarmDryGeometry}).
 */
public class LeopardModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "leopard"), "main");
    private static final float GAIT = 0.7F, CROUCH_DROP = 4.0F;
    /** Body drop (px) that puts the belly (y 17) on the ground. */
    private static final float LIE_DROP = 7.0F;
    /** Resting pitch of the three tail segments: down from the rump, along low, tip curled up. */
    private static final float[] TAIL_REST = {0.5F, 0.5F, 1.0F};

    private final ModelPart body, head, jaw, rightEar, leftEar;
    private final ModelPart[] tail = new ModelPart[3];
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public LeopardModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = head.getChild("jaw");
        this.rightEar = head.getChild("right_ear");
        this.leftEar = head.getChild("left_ear");
        ModelPart t = body;
        for (int i = 0; i < 3; i++) tail[i] = t = t.getChild("tail" + i);
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return WarmDryGeometry.leopard();
    }

    private static ModelPart lower(ModelPart leg) {
        return leg.getChild("lower");
    }

    private boolean hind(ModelPart leg) {
        return leg == rightHindLeg || leg == leftHindLeg;
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.25F; // in line with the body, muzzle down
            jaw.xRot = 0.35F;
            leftEar.xRot = rightEar.xRot = 1.1F;
            tail[0].xRot = -1.9F; // flops down over the back, hanging straight
            tail[1].xRot = tail[2].xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            head.xRot += 0.2F;
            jaw.xRot = 0.35F;
            tail[0].xRot = 1.3F;
            tail[1].xRot = 0.2F;
            tail[2].xRot = 0.1F;
            return;
        }
        if (state.dozing) {
            drape(age);
            return;
        }

        float pos = state.walkAnimationPos, speed = Math.min(state.walkAnimationSpeed, 1.5F);
        boolean gallop = speed > 0.9F && !state.crouching;
        if (gallop) {
            // bound: forelegs together, hind legs together half a stride later; the supple back flexes hard
            float c = pos * GAIT * 0.8F;
            float f = Mth.cos(c) * speed, h = Mth.cos(c + Mth.PI * 0.9F) * speed;
            rightFrontLeg.xRot = f * 1.0F;
            leftFrontLeg.xRot = f * 0.85F;
            rightHindLeg.xRot = h * 1.0F;
            leftHindLeg.xRot = h * 0.85F;
            body.xRot = Mth.sin(c) * 0.11F * speed;
            body.y -= Mth.abs(Mth.sin(c)) * 1.2F * speed;
            tail[0].xRot = TAIL_REST[0] - 0.4F * speed; // tail streams out behind as a counterweight
            tail[1].xRot = TAIL_REST[1] - 0.35F + Mth.sin(c - 1.0F) * 0.2F * speed;
        } else {
            // walk/trot: diagonal pairs, the head low and level, shoulder blades rolling
            float swing = Mth.cos(pos * GAIT) * speed * 1.0F;
            rightFrontLeg.xRot = swing;
            leftHindLeg.xRot = swing;
            leftFrontLeg.xRot = -swing;
            rightHindLeg.xRot = -swing;
            body.y += Mth.abs(Mth.cos(pos * GAIT)) * 0.35F * speed;
            body.zRot = Mth.sin(pos * GAIT) * 0.03F * speed;
        }
        for (ModelPart leg : legs) lower(leg).xRot = Math.max(0, -leg.xRot) * (hind(leg) ? -0.6F : 1.2F);

        head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD * (1.0F - state.grazeAmount);
        head.xRot += Mth.clamp(state.xRot, -30.0F, 30.0F) * 0.6F * Mth.DEG_TO_RAD;
        float flick = Mth.sin(age * 0.07F) > 0.9F ? Mth.sin(age * 1.4F) * 0.2F : 0.0F;
        leftEar.zRot += flick;
        // the long tail sways slowly, the curled tip twitching on its own
        tail[0].xRot += Mth.sin(pos * GAIT) * 0.08F * speed;
        tail[0].zRot = Mth.sin(age * 0.08F) * 0.12F;
        tail[1].zRot = Mth.sin(age * 0.08F - 0.8F) * 0.15F;
        tail[2].zRot = Mth.sin(age * 0.08F - 1.6F) * 0.2F;
        tail[2].xRot += Mth.sin(age * 0.23F) * 0.12F;

        // nose to the ground, following a scent
        if (state.grazeAmount > 0) {
            float g = state.grazeAmount;
            head.xRot = Mth.lerp(g, head.xRot, 0.9F + Mth.sin(age * 0.8F) * 0.05F);
            head.y += g * 2.0F;
        }
        // hunting: head low and level with the back, ears flat, lips drawn back
        if (state.charging) {
            head.xRot = Mth.lerp(0.7F, head.xRot, 0.25F);
            head.y += 1.0F;
            rightEar.xRot = leftEar.xRot = -0.7F;
            jaw.xRot = 0.25F + Mth.sin(age * 0.4F) * 0.05F;
        }
        if (state.crouching) crouch(age, pos, speed);
        if (state.leaping) leap();
        if (state.calling > 0) call(state.calling, age);
    }

    /** Stalking: belly almost on the ground, shoulder blades jutting up, head low and pushed forward, tail low with a twitching tip. */
    private void crouch(float age, float pos, float speed) {
        body.y += CROUCH_DROP;
        body.xRot = 0.05F; // the shoulders ride higher than the hips
        head.y += CROUCH_DROP * 0.25F;
        head.z -= 0.8F;
        head.xRot = 0.1F;
        float creep = Mth.cos(pos * GAIT * 1.3F) * speed * 0.55F;
        for (ModelPart leg : legs) {
            boolean hind = hind(leg);
            float c = (leg == rightFrontLeg || leg == leftHindLeg) ? creep : -creep;
            leg.y += CROUCH_DROP * 0.75F;
            leg.xRot = (hind ? 0.9F : -0.85F) + c;
            lower(leg).xRot = hind ? -1.2F : 1.35F;
        }
        tail[0].xRot = TAIL_REST[0] + 0.6F;
        tail[1].xRot = TAIL_REST[1] - 0.2F;
        tail[2].xRot = TAIL_REST[2] - 0.3F + Mth.sin(age * 0.6F) * 0.3F;
        tail[2].zRot = Mth.sin(age * 0.5F) * 0.25F;
    }

    /** Pounce: body stretched long, forelegs reaching out in front with the paws spread, hind legs trailing, tail out behind. */
    private void leap() {
        body.xRot = -0.2F;
        rightFrontLeg.xRot = leftFrontLeg.xRot = -1.35F;
        lower(rightFrontLeg).xRot = lower(leftFrontLeg).xRot = 0.25F;
        rightFrontLeg.zRot = 0.15F;
        leftFrontLeg.zRot = -0.15F;
        rightHindLeg.xRot = leftHindLeg.xRot = 1.15F;
        lower(rightHindLeg).xRot = lower(leftHindLeg).xRot = 0.45F;
        jaw.xRot = 0.7F;
        rightEar.xRot = leftEar.xRot = -0.6F;
        tail[0].xRot = TAIL_REST[0] + 0.7F;
        tail[1].xRot = 0.0F;
        tail[2].xRot = 0.3F;
    }

    /** The rasping, sawing call: head raised, mouth opening and closing with each breath. */
    private void call(float amount, float age) {
        head.xRot = Mth.lerp(amount, head.xRot, -0.6F);
        head.yRot *= 1.0F - amount;
        jaw.xRot = Mth.lerp(amount, jaw.xRot, 0.3F + Math.abs(Mth.sin(age * 0.45F)) * 0.3F);
        rightEar.xRot = leftEar.xRot = -0.2F * amount;
        body.xRot -= 0.06F * amount;
    }

    /**
     * Asleep up a tree: body lying along the bough (belly at the feet's level), all four legs hanging limp to either side,
     * the tail dangling straight down, chin resting on the branch turned a little to one side. On a canopy the legs hang
     * down into the leaves.
     */
    private void drape(float age) {
        float breathe = Mth.sin(age * 0.07F);
        body.y += LIE_DROP + breathe * 0.15F;
        for (ModelPart leg : legs) {
            boolean hind = hind(leg);
            leg.y += LIE_DROP + (hind ? 1.0F : 0.0F); // hips sit lower in the body than the shoulders
            leg.zRot = Math.signum(leg.x) * -0.3F; // splayed so the legs hang either side of the bough
            leg.xRot = (hind ? 0.12F : -0.08F) + breathe * 0.02F;
            lower(leg).xRot = hind ? -0.05F : 0.05F;
        }
        leftFrontLeg.xRot = -0.35F; // one forepaw tucked forward under the chin
        lower(leftFrontLeg).xRot = 0.25F;
        head.y += 3.0F; // chin down on the branch
        head.xRot = 0.35F;
        head.yRot = -0.3F;
        head.zRot = 0.15F;
        rightEar.xRot = leftEar.xRot = -0.25F;
        tail[0].xRot = 0.05F;
        tail[1].xRot = -0.05F;
        tail[2].xRot = 0.15F + Mth.sin(age * 0.03F) * 0.1F; // the tip still curls and stirs
    }
}
