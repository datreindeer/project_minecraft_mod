package com.mechanical.client.wildlife.giants;

import static com.mechanical.client.wildlife.giants.GiantPose.DEG;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Woolly rhinoceros: a long, low, barrel body on short thick legs, a big shoulder hump, a thick neck and a long head
 * carried low. The great front horn sweeps forward and curves up from the snout (three segments), a short second horn
 * stands behind it. Shaggy hair hangs along the flanks. The calf is the model scaled down with a bigger head, no horns.
 * Geometry and skin: scripts/gen_pred_models.py (GiantsGeometry.rhino, textures/entity/wildlife/giants/woolly_rhino(_calf).png).
 */
public class RhinoModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "woolly_rhino"), "main");
    private static final float NECK_X = 0.25F, HEAD_X = 0.55F, GRAZE_NECK_X = 0.75F, GRAZE_HEAD_X = 0.75F, TAIL_X = 0.2F;
    private static final float LIE_DROP = 6.0F, LIE_LEG_Y = 21.0F;

    private final ModelPart body, neck, head, horns, tail, leftSkirt, rightSkirt, leftEar, rightEar;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public RhinoModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        horns = head.getChild("horns");
        tail = body.getChild("tail");
        leftSkirt = body.getChild("left_skirt");
        rightSkirt = body.getChild("right_skirt");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return GiantsGeometry.rhino();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks, seed = state.entityId * 0.37F;
        ModelPart[] front = {leftFrontLeg, rightFrontLeg}, hind = {leftHindLeg, rightHindLeg};
        horns.visible = state.showsHorns;
        if (state.isBaby) {
            GiantPose.scale(head, 1.35F);
            leftSkirt.yScale = rightSkirt.yScale = 0.6F;
        }

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            neck.xRot = 0.05F;
            head.xRot = 0.1F;
            leftSkirt.xRot = rightSkirt.xRot = -1.2F;
            leftEar.xRot = rightEar.xRot = 0.6F;
            tail.xRot = -1.9F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            GiantPose.sag(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
            neck.xRot = 0.3F;
            neck.yRot = -0.35F;
            head.xRot = 0.35F;
            head.yRot = state.yRot * DEG * 0.6F;
            leftSkirt.zRot = -0.5F;
            rightSkirt.zRot = 0.5F;
            leftEar.zRot = 1.1F;
            rightEar.zRot = -1.1F;
            tail.xRot = 0.6F;
            return;
        }
        if (state.dozing) {
            GiantPose.lie(body, LIE_DROP, LIE_LEG_Y, age, front, hind);
            neck.xRot = 0.55F;
            neck.yRot = 0.25F;
            head.xRot = 0.6F; // chin and horn resting on the ground
            leftSkirt.zRot = -0.5F;
            rightSkirt.zRot = 0.5F;
            leftEar.zRot = 0.8F;
            rightEar.zRot = -0.8F;
            tail.xRot = 0.5F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = GiantPose.walk(state.walkAnimationPos, speed, 0.65F, 0.75F, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
        body.y += Mth.cos(phase * 2.0F) * 0.35F * speed;
        body.zRot = Mth.sin(phase) * 0.03F * speed;
        leftSkirt.xRot = rightSkirt.xRot = Mth.sin(phase * 2.0F) * 0.05F * speed;

        float yaw = Mth.clamp(state.yRot, -40.0F, 40.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float neckX = NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed, headX = HEAD_X + pitch * 0.4F;
        float graze = state.grazeAmount;
        if (state.charging) {
            // horn levelled at the target, head tossing
            neckX = 0.35F;
            headX = 0.9F + Mth.sin(age * 0.6F) * 0.1F;
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
            yaw *= 1.0F - graze;
        }
        neck.xRot = neckX;
        neck.yRot = yaw * 0.4F;
        head.xRot = headX;
        head.yRot = yaw * 0.6F;

        float pinned = state.charging ? 0.7F : 0.0F;
        leftEar.xRot = -0.4F - pinned + GiantPose.pulse(age * 0.05F + seed) * Mth.sin(age * 1.5F) * 0.4F;
        rightEar.xRot = -0.4F - pinned + GiantPose.pulse(age * 0.043F + seed + 2.0F) * Mth.sin(age * 1.7F) * 0.4F;
        tail.zRot = GiantPose.pulse(age * 0.07F + seed * 1.3F) * Mth.sin(age * 0.9F) * 0.5F;
        tail.xRot = TAIL_X + speed * 0.4F + (state.charging ? 0.9F : 0.0F);
    }
}
