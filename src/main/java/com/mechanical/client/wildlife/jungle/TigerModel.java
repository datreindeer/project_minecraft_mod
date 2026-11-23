package com.mechanical.client.wildlife.jungle;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Tiger: a heavy great cat - deep chest under jutting shoulder blades, long loins, a broad round head with flaring white
 * sideburns and a short wide muzzle, small round ears, thick legs on big paws, a long ringed tail hanging in a low curve.
 * Moves and hunts like the leopard (LeopardModel: trot and bound, nose to the ground, crouched stalk, pounce, the call);
 * asleep it lies on its belly, forepaws out in front, head down on them. Geometry and skin:
 * scripts/gen_jungle_models.py (tiger(), JungleGeometry.tiger()).
 */
public class TigerModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "tiger"), "main");
    private static final float GAIT = 0.65F, CROUCH_DROP = 4.0F;
    /** Body drop (px) that puts the belly (y 17) on the ground. */
    private static final float LIE_DROP = 6.5F;
    /** Resting pitch of the three tail segments (as built): down from the rump, along low, the tip curled up. */
    private static final float[] TAIL_REST = {0.55F, 0.45F, 0.9F};

    private final ModelPart body, head, jaw, rightEar, leftEar;
    private final ModelPart[] tail = new ModelPart[3];
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs, lowers;

    public TigerModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        rightEar = head.getChild("right_ear");
        leftEar = head.getChild("left_ear");
        ModelPart t = body;
        for (int i = 0; i < 3; i++) tail[i] = t = t.getChild("tail" + i);
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
        lowers = new ModelPart[] {rightFrontLeg.getChild("right_front_lower"), leftFrontLeg.getChild("left_front_lower"),
                rightHindLeg.getChild("right_hind_lower"), leftHindLeg.getChild("left_hind_lower")};
    }

    public static LayerDefinition createBodyLayer() {
        return JungleGeometry.tiger();
    }

    private ModelPart lower(ModelPart leg) {
        for (int i = 0; i < 4; i++) if (legs[i] == leg) return lowers[i];
        throw new IllegalArgumentException();
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
            head.xRot = 0.25F;
            jaw.xRot = 0.35F;
            leftEar.xRot = rightEar.xRot = 1.0F;
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
            lie(age);
            return;
        }

        float pos = state.walkAnimationPos, speed = Math.min(state.walkAnimationSpeed, 1.5F);
        boolean gallop = speed > 0.9F && !state.crouching;
        if (gallop) {
            // bound: forelegs together, hind legs together half a stride later; the back flexing
            float c = pos * GAIT * 0.8F;
            float f = Mth.cos(c) * speed, h = Mth.cos(c + Mth.PI * 0.9F) * speed;
            rightFrontLeg.xRot = f;
            leftFrontLeg.xRot = f * 0.85F;
            rightHindLeg.xRot = h;
            leftHindLeg.xRot = h * 0.85F;
            body.xRot = Mth.sin(c) * 0.09F * speed;
            body.y -= Mth.abs(Mth.sin(c)) * 1.2F * speed;
            tail[0].xRot = TAIL_REST[0] - 0.4F * speed;
            tail[1].xRot = TAIL_REST[1] - 0.35F + Mth.sin(c - 1.0F) * 0.2F * speed;
        } else {
            // the heavy prowl: diagonal pairs, head low and level, the shoulder blades rolling
            float swing = Mth.cos(pos * GAIT) * speed * 0.9F;
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
        leftEar.zRot += Mth.sin(age * 0.07F) > 0.9F ? Mth.sin(age * 1.4F) * 0.2F : 0.0F;
        tail[0].xRot += Mth.sin(pos * GAIT) * 0.08F * speed;
        tail[0].zRot = Mth.sin(age * 0.08F) * 0.12F;
        tail[1].zRot = Mth.sin(age * 0.08F - 0.8F) * 0.15F;
        tail[2].zRot = Mth.sin(age * 0.08F - 1.6F) * 0.2F;
        tail[2].xRot += Mth.sin(age * 0.23F) * 0.12F;

        if (state.grazeAmount > 0) { // nose to the ground, following a scent
            float g = state.grazeAmount;
            head.xRot = Mth.lerp(g, head.xRot, 0.9F + Mth.sin(age * 0.8F) * 0.05F);
            head.y += g * 2.0F;
        }
        if (state.charging) { // hunting: head low and level with the back, ears flat, lips drawn back
            head.xRot = Mth.lerp(0.7F, head.xRot, 0.25F);
            head.y += 1.0F;
            rightEar.xRot = leftEar.xRot = -0.7F;
            jaw.xRot = 0.25F + Mth.sin(age * 0.4F) * 0.05F;
        }
        if (state.crouching) crouch(age, pos, speed);
        if (state.leaping) leap();
        if (state.calling > 0) call(state.calling, age);
    }

    /** Stalking: belly almost on the ground, shoulder blades jutting, head low and pushed forward, the tail tip twitching. */
    private void crouch(float age, float pos, float speed) {
        body.y += CROUCH_DROP;
        body.xRot = 0.05F;
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

    /** Pounce: stretched long, forelegs reaching with the paws spread, hind legs trailing, jaws open. */
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

    /** The roar: head raised, the jaw dropping with each breath. */
    private void call(float amount, float age) {
        head.xRot = Mth.lerp(amount, head.xRot, -0.5F);
        head.yRot *= 1.0F - amount;
        jaw.xRot = Mth.lerp(amount, jaw.xRot, 0.4F + Math.abs(Mth.sin(age * 0.45F)) * 0.35F);
        rightEar.xRot = leftEar.xRot = -0.3F * amount;
        body.xRot -= 0.06F * amount;
    }

    /** Asleep on its belly: forelegs stretched out in front, hind legs folded under, chin on the paws, the tail curled round. */
    private void lie(float age) {
        float breathe = Mth.sin(age * 0.07F);
        body.y += LIE_DROP + breathe * 0.15F;
        for (ModelPart leg : legs) {
            boolean hind = hind(leg);
            leg.y += LIE_DROP + 0.5F;
            leg.xRot = hind ? -1.45F : -1.5F;
            lower(leg).xRot = hind ? 0.1F : 0.05F;
            leg.zRot = Math.signum(leg.x) * -0.15F;
        }
        head.y += 3.5F;
        head.z -= 1.0F;
        head.xRot = 0.15F;
        head.yRot = -0.2F;
        rightEar.xRot = leftEar.xRot = -0.25F;
        tail[0].xRot = 1.2F;
        tail[0].yRot = 0.6F;
        tail[1].xRot = 0.2F;
        tail[2].xRot = 0.1F + Mth.sin(age * 0.03F) * 0.1F;
    }
}
