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
 * Steppe bison: massive woolly forequarters under a tall hump, slim hindquarters, a big shaggy head hung low with a beard,
 * and horns that curve out and up (long and thick on the bull, slim on the cow, whose hump is lower). Shaggy "chaps" on
 * the forelegs, a tufted tail. The calf is the model scaled down with a bigger head and no horns.
 * Geometry and skins: scripts/gen_pred_models.py (GiantsGeometry.bison, textures/entity/wildlife/giants/steppe_bison_{bull,cow,calf}.png).
 * The wisent (TemperateGeometry.wisent) uses this animation too: keep the parts looked up here in both.
 */
public class BisonModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "steppe_bison"), "main");
    private static final float HEAD_X = 0.5F, GRAZE_HEAD_X = 1.05F, CHARGE_HEAD_X = 1.2F, TAIL_X = 0.1F;
    private static final float LIE_DROP = 7.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body, hump, head, bullHorns, cowHorns, tail, leftEar, rightEar, beard;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public BisonModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        hump = body.getChild("hump");
        head = body.getChild("head");
        bullHorns = head.getChild("bull_horns");
        cowHorns = head.getChild("cow_horns");
        beard = head.getChild("beard");
        tail = body.getChild("tail");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return GiantsGeometry.bison();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks, seed = state.entityId * 0.37F;
        ModelPart[] front = {leftFrontLeg, rightFrontLeg}, hind = {leftHindLeg, rightHindLeg};
        bullHorns.visible = state.showsHorns;
        cowHorns.visible = !state.showsHorns && !state.isBaby;
        if (!state.showsHorns) hump.yScale = 0.7F; // the cow's hump is lower
        if (state.isBaby) {
            GiantPose.scale(head, 1.35F);
            beard.visible = false;
            hump.yScale = 0.5F;
        }

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            head.xRot = 0.15F;
            leftEar.yRot = 1.0F;
            rightEar.yRot = -1.0F;
            tail.xRot = -1.9F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            GiantPose.sag(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
            head.xRot = 0.5F;
            head.yRot = state.yRot * DEG * 0.7F;
            leftEar.zRot = 1.2F;
            rightEar.zRot = -1.2F;
            tail.xRot = 0.6F;
            return;
        }
        if (state.dozing) {
            GiantPose.lie(body, LIE_DROP, LIE_LEG_Y, age, front, hind);
            head.xRot = 0.85F;
            head.yRot = 0.3F;
            head.zRot = 0.12F;
            leftEar.zRot = 0.8F;
            rightEar.zRot = -0.8F;
            tail.xRot = 0.5F;
            tail.zRot = 0.4F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = GiantPose.walk(state.walkAnimationPos, speed, 0.6F, 0.85F, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
        body.y += Mth.cos(phase * 2.0F) * 0.4F * speed;
        body.xRot = Mth.sin(phase * 2.0F) * 0.02F * speed; // the heavy front rocks over the forelegs

        float yaw = Mth.clamp(state.yRot, -45.0F, 45.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float headX = HEAD_X + pitch * 0.5F + Mth.cos(phase * 2.0F) * 0.07F * speed;
        float graze = state.grazeAmount;
        if (state.charging) {
            headX = CHARGE_HEAD_X + Mth.sin(age * 0.5F) * 0.08F; // forehead down, horns hooking
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
            yaw *= 1.0F - graze;
        }
        head.xRot = headX;
        head.yRot = yaw;
        beard.xRot = -headX * 0.4F + Mth.sin(phase) * 0.1F * speed; // hangs down whatever the head does

        float pinned = state.charging ? 0.6F : 0.0F;
        leftEar.zRot = 0.4F - GiantPose.pulse(age * 0.05F + seed) * Mth.sin(age * 1.6F) * 0.4F - pinned;
        rightEar.zRot = -0.4F + GiantPose.pulse(age * 0.041F + seed + 2.0F) * Mth.sin(age * 1.8F) * 0.4F + pinned;
        tail.zRot = GiantPose.pulse(age * 0.07F + seed * 1.3F) * Mth.sin(age * 0.9F) * 0.55F + Mth.sin(phase) * 0.1F * speed;
        tail.xRot = TAIL_X + speed * 0.6F + (state.charging ? 0.9F : 0.0F); // tail up when running or charging
    }
}
