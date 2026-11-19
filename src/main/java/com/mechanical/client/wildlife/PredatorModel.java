package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Shared animation for the four-legged night predators (GreyWolfModel, CaveLionModel). Parts: body (with head, which has a
 * jaw and two ears, and a tail) and four legs, each with a "lower" child (forearm / hock). Poses: trot and gallop,
 * nose-down sniffing (graze), head low and snarling when hunting, crouched stalking, the pounce, head thrown back to
 * howl/roar, curled up asleep, and limp as a carcass.
 */
abstract class PredatorModel extends EntityModel<WildAnimalRenderState> {
    protected final ModelPart body, head, jaw, rightEar, leftEar, tail;
    protected final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;
    /** Gait cadence, how far the body sinks crouching / lying (px), and the tail's resting angle. */
    private final float gait, crouchDrop, lieDrop, tailRest;

    PredatorModel(ModelPart root, float gait, float crouchDrop, float lieDrop, float tailRest) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = head.getChild("jaw");
        this.rightEar = head.getChild("right_ear");
        this.leftEar = head.getChild("left_ear");
        this.tail = body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
        this.gait = gait;
        this.crouchDrop = crouchDrop;
        this.lieDrop = lieDrop;
        this.tailRest = tailRest;
    }

    private static ModelPart lower(ModelPart leg) {
        return leg.getChild("lower");
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            head.xRot += 0.2F;
            jaw.xRot = 0.35F; // tongue lolling
            tail.xRot = 1.3F;
            return;
        }
        if (state.dozing) {
            sleep(age);
            return;
        }

        float pos = state.walkAnimationPos, speed = Math.min(state.walkAnimationSpeed, 1.5F);
        boolean gallop = speed > 0.9F && !state.crouching;
        if (gallop) {
            // bound: forelegs together, hind legs together half a stride later; the back flexes
            float f = Mth.cos(pos * gait * 0.8F) * speed, h = Mth.cos(pos * gait * 0.8F + Mth.PI * 0.9F) * speed;
            rightFrontLeg.xRot = f * 0.9F;
            leftFrontLeg.xRot = f * 0.8F;
            rightHindLeg.xRot = h * 0.9F;
            leftHindLeg.xRot = h * 0.8F;
            body.xRot = Mth.sin(pos * gait * 0.8F) * 0.08F * speed;
            body.y -= Mth.abs(Mth.sin(pos * gait * 0.8F)) * 1.0F * speed;
        } else {
            // trot: diagonal pairs swing together
            float swing = Mth.cos(pos * gait) * speed * 1.1F;
            rightFrontLeg.xRot = swing;
            leftHindLeg.xRot = swing;
            leftFrontLeg.xRot = -swing;
            rightHindLeg.xRot = -swing;
            body.y += Mth.abs(Mth.cos(pos * gait)) * 0.4F * speed;
        }
        // lower legs fold as the foot lifts forward
        for (ModelPart leg : legs) lower(leg).xRot = Math.max(0, -leg.xRot) * (leg == rightHindLeg || leg == leftHindLeg ? -0.6F : 1.2F);

        head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD * (1.0F - state.grazeAmount);
        head.xRot += Mth.clamp(state.xRot, -30.0F, 30.0F) * 0.6F * Mth.DEG_TO_RAD;
        float flick = Mth.sin(age * 0.08F) > 0.9F ? Mth.sin(age * 1.4F) * 0.2F : 0.0F;
        rightEar.zRot -= flick;
        tail.xRot = tailRest + Mth.sin(pos * gait) * 0.1F * speed;
        tail.zRot = Mth.sin(age * 0.1F) * 0.08F;

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
            rightEar.xRot = -0.7F;
            leftEar.xRot = -0.7F;
            jaw.xRot = 0.25F + Mth.sin(age * 0.4F) * 0.05F;
            tail.xRot = tailRest + 0.25F;
        }
        if (state.crouching) crouch(age, pos, speed);
        if (state.leaping) leap();
        if (state.calling > 0) call(state.calling, age);
    }

    /** Belly low, legs bent under it, head stretched forward along the ground, tail held low and twitching at the tip. */
    private void crouch(float age, float pos, float speed) {
        body.y += crouchDrop;
        head.y += crouchDrop * 0.4F;
        head.xRot = 0.15F;
        float creep = Mth.cos(pos * gait * 1.3F) * speed * 0.6F;
        for (ModelPart leg : legs) {
            boolean hind = leg == rightHindLeg || leg == leftHindLeg;
            float c = (leg == rightFrontLeg || leg == leftHindLeg) ? creep : -creep;
            leg.y += crouchDrop * 0.75F;
            leg.xRot = (hind ? 0.9F : -0.9F) + c;
            lower(leg).xRot = hind ? -1.2F : 1.4F;
        }
        tail.xRot = tailRest + 0.35F;
        tail.zRot = Mth.sin(age * 0.6F) * 0.12F;
    }

    /** In the air: forelegs reaching out in front, hind legs trailing, body stretched. */
    private void leap() {
        body.xRot = -0.2F;
        rightFrontLeg.xRot = leftFrontLeg.xRot = -1.3F;
        lower(rightFrontLeg).xRot = lower(leftFrontLeg).xRot = 0.3F;
        rightHindLeg.xRot = leftHindLeg.xRot = 1.1F;
        lower(rightHindLeg).xRot = lower(leftHindLeg).xRot = 0.4F;
        jaw.xRot = 0.7F;
        rightEar.xRot = leftEar.xRot = -0.6F;
        tail.xRot = tailRest - 0.5F;
    }

    /** Howl / roar: muzzle up to the sky, mouth open in a long call. */
    private void call(float amount, float age) {
        head.xRot = Mth.lerp(amount, head.xRot, -1.05F);
        head.yRot *= 1.0F - amount;
        jaw.xRot = Mth.lerp(amount, jaw.xRot, 0.45F + Mth.sin(age * 0.25F) * 0.08F);
        rightEar.xRot = leftEar.xRot = -0.3F * amount;
        body.xRot -= 0.1F * amount;
    }

    /** Curled up on its side-belly: legs tucked, the tail wrapped round the front, chin resting on the forepaws. */
    private void sleep(float age) {
        body.y += lieDrop + Mth.sin(age * 0.07F) * 0.15F;
        float legY = 24.0F - 1.5F;
        for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg}) {
            leg.y = legY;
            leg.xRot = -Mth.HALF_PI;
            lower(leg).xRot = 0.0F;
        }
        for (ModelPart leg : new ModelPart[] {rightHindLeg, leftHindLeg}) {
            leg.y = legY;
            leg.z -= 1.0F;
            leg.xRot = -1.35F;
            lower(leg).xRot = 2.6F;
        }
        rightHindLeg.yRot = 0.35F;
        leftHindLeg.yRot = -0.35F;
        head.y += lieDrop * 0.55F;
        head.xRot = 0.15F;
        head.yRot = 0.35F;
        head.zRot = 0.1F;
        rightEar.xRot = leftEar.xRot = -0.35F;
        tail.xRot = 1.45F;
        tail.yRot = 0.9F; // curled round towards the head
    }
}
