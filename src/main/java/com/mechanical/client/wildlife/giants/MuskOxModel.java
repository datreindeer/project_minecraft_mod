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
 * Musk ox: a squat, humped body almost hidden under a curtain of long guard hair that hangs nearly to the ground on all
 * sides, short pale-stockinged legs below it, and a low head with a shaggy chin. The horns meet in a broad boss across
 * the forehead, sweep down beside the face and hook forward and up at the tips. The calf is the model scaled down with a
 * bigger head, shorter hair and no horns.
 * Geometry and skin: scripts/gen_pred_models.py (GiantsGeometry.muskOx, textures/entity/wildlife/giants/musk_ox(_calf).png).
 */
public class MuskOxModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "musk_ox"), "main");
    private static final float HEAD_X = 0.6F, GRAZE_HEAD_X = 1.1F, GUARD_HEAD_X = 0.95F;
    private static final float LIE_DROP = 6.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body, head, horns, leftSkirt, rightSkirt, rearSkirt, frontSkirt, chin;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public MuskOxModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        horns = head.getChild("horns");
        chin = head.getChild("chin");
        leftSkirt = body.getChild("left_skirt");
        rightSkirt = body.getChild("right_skirt");
        rearSkirt = body.getChild("rear_skirt");
        frontSkirt = body.getChild("front_skirt");
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return GiantsGeometry.muskOx();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        ModelPart[] front = {leftFrontLeg, rightFrontLeg}, hind = {leftHindLeg, rightHindLeg};
        ModelPart[] skirts = {leftSkirt, rightSkirt, rearSkirt, frontSkirt};
        horns.visible = state.showsHorns;
        if (state.isBaby) {
            GiantPose.scale(head, 1.3F);
            for (ModelPart s : skirts) s.yScale = 0.6F;
            chin.visible = false;
        }

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            head.xRot = 0.1F;
            // the long hair falls towards the ground (-z here)
            leftSkirt.xRot = rightSkirt.xRot = -1.2F;
            frontSkirt.xRot = -0.4F;
            rearSkirt.xRot = -1.5F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            GiantPose.sag(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
            head.xRot = 0.5F;
            head.yRot = state.yRot * DEG * 0.7F;
            leftSkirt.zRot = -0.6F;
            rightSkirt.zRot = 0.6F;
            return;
        }
        if (state.dozing) {
            GiantPose.lie(body, LIE_DROP, LIE_LEG_Y, age, front, hind);
            head.xRot = 0.9F;
            head.yRot = 0.25F;
            // the hair spreads out on the ground around it
            for (ModelPart s : skirts) s.yScale *= 0.5F;
            leftSkirt.zRot = -0.9F;
            rightSkirt.zRot = 0.9F;
            frontSkirt.xRot = 0.8F;
            rearSkirt.xRot = -0.8F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = GiantPose.walk(state.walkAnimationPos, speed, 0.8F, 0.8F, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
        body.y += Mth.cos(phase * 2.0F) * 0.3F * speed;
        // the curtain swings with every step
        float swing = Mth.sin(phase * 2.0F) * 0.07F * speed;
        leftSkirt.zRot = -Math.abs(swing);
        rightSkirt.zRot = Math.abs(swing);
        frontSkirt.xRot = swing - speed * 0.15F;
        rearSkirt.xRot = -swing + speed * 0.2F;

        float yaw = Mth.clamp(state.yRot, -40.0F, 40.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float headX = HEAD_X + pitch * 0.5F + Mth.cos(phase * 2.0F) * 0.06F * speed;
        float graze = state.grazeAmount;
        if (state.charging) {
            // in the ring: head down, horns out, swinging now and then
            headX = GUARD_HEAD_X + GiantPose.pulse(age * 0.08F + state.entityId) * 0.25F;
            yaw *= 0.4F;
            head.zRot = Mth.sin(age * 0.15F + state.entityId) * 0.08F;
        } else if (graze > 0.0F) {
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
            yaw *= 1.0F - graze;
        }
        head.xRot = headX;
        head.yRot = yaw;
        chin.xRot = -headX * 0.5F;
    }
}
