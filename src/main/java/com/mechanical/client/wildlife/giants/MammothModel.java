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
 * Woolly mammoth, built at half size and drawn twice as big (GiantRenderer): pillar legs, a short deep body under a high
 * shoulder hump that slopes down to the rump, a domed skull, small ears, a long four-part trunk that sways and curls,
 * and long spiralling tusks (down and forward, then out, up and in at the tips). A shaggy skirt of long hair hangs from
 * the flanks and a bib from the throat (jagged, transparent ends in the texture).
 * The calf is the same model scaled down with a bigger head and no tusks.
 * Geometry and skin (two texels per model pixel, as it is drawn twice as big): scripts/gen_pred_models.py, which writes
 * GiantsGeometry.mammoth and textures/entity/wildlife/giants/woolly_mammoth(_calf).png. Part names are what this class looks up.
 */
public class MammothModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "woolly_mammoth"), "main");
    private static final float HEAD_X = 0.2F, TRUNK_X = -0.2F, TRUNK_TIP_X = -0.4F, TAIL_X = 0.15F;
    /** Sleeping: body down so the belly rests on the ground, folded legs at y = 21. */
    private static final float LIE_DROP = 8.5F, LIE_LEG_Y = 21.0F;

    private final ModelPart body, head, tusks, tail, leftSkirt, rightSkirt, bib, leftEar, rightEar;
    private final ModelPart[] trunk;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public MammothModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        tusks = head.getChild("tusks");
        tail = body.getChild("tail");
        leftSkirt = body.getChild("left_skirt");
        rightSkirt = body.getChild("right_skirt");
        bib = body.getChild("bib");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        ModelPart t1 = head.getChild("trunk1"), t2 = t1.getChild("trunk2"), t3 = t2.getChild("trunk3");
        trunk = new ModelPart[] {t1, t2, t3, t3.getChild("trunk4")};
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return GiantsGeometry.mammoth();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float seed = state.entityId * 0.37F;
        ModelPart[] front = {leftFrontLeg, rightFrontLeg}, hind = {leftHindLeg, rightHindLeg};
        tusks.visible = state.showsHorns;
        if (state.isBaby) {
            GiantPose.scale(head, 1.35F);
            leftSkirt.yScale = rightSkirt.yScale = bib.yScale = 0.6F;
        }

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            head.xRot = 0.1F;
            for (ModelPart t : trunk) t.xRot = 0.05F; // hangs straight down from the head
            leftSkirt.xRot = rightSkirt.xRot = -1.2F; // the long hair falls towards the ground
            tail.xRot = -1.9F;
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            GiantPose.sag(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
            head.xRot = 0.3F;
            head.yRot = state.yRot * DEG * 0.6F;
            // the trunk lies curled on the ground, the hair spread out
            trunk[0].zRot = 0.3F;
            trunk[1].zRot = 0.5F;
            trunk[2].zRot = 0.5F;
            trunk[3].zRot = 0.6F;
            leftSkirt.zRot = -0.5F;
            rightSkirt.zRot = 0.5F;
            leftEar.yRot = -0.8F;
            rightEar.yRot = 0.8F;
            tail.xRot = 0.6F;
            return;
        }

        if (state.dozing) {
            GiantPose.lie(body, LIE_DROP, LIE_LEG_Y, age, front, hind);
            head.xRot = 0.55F;
            head.yRot = 0.15F;
            // trunk curled up on the ground in front, hair spread over the folded legs
            trunk[0].xRot = -0.5F;
            trunk[1].xRot = -0.6F;
            trunk[2].xRot = -0.5F;
            trunk[3].xRot = -0.6F;
            leftSkirt.zRot = -0.45F;
            rightSkirt.zRot = 0.45F;
            bib.xRot = -0.9F;
            tail.xRot = 0.5F;
            tail.zRot = 0.3F;
            return;
        }

        // slow, swaying walk; the hair skirts swing with it
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = GiantPose.walk(state.walkAnimationPos, speed, 0.5F, 0.6F, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
        body.y += Mth.cos(phase * 2.0F) * 0.4F * speed;
        body.zRot = Mth.sin(phase) * 0.03F * speed;
        leftSkirt.zRot = -Math.max(0.0F, Mth.sin(phase)) * 0.12F * speed;
        rightSkirt.zRot = Math.max(0.0F, -Mth.sin(phase)) * 0.12F * speed;
        leftSkirt.xRot = rightSkirt.xRot = Mth.sin(phase * 2.0F) * 0.04F * speed;

        float yaw = Mth.clamp(state.yRot, -35.0F, 35.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float headX = HEAD_X + pitch * 0.5F + Mth.cos(phase * 2.0F) * 0.05F * speed;
        // the trunk swings slowly from side to side and curls a little now and then
        float sway = Mth.sin(age * 0.06F + seed) * 0.12F + Mth.sin(phase) * 0.15F * speed;
        float curl = GiantPose.pulse(age * 0.03F + seed) * 0.5F;
        float[] tx = {TRUNK_X, 0.0F, 0.0F, TRUNK_TIP_X};
        for (int i = 0; i < 4; i++) {
            trunk[i].xRot = tx[i] - curl * (i + 1) * 0.25F;
            trunk[i].zRot = sway * (0.6F + i * 0.25F);
        }

        GiantRenderState giant = state instanceof GiantRenderState g ? g : null;
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            // head down, the trunk reaching to the ground and curling up to the mouth
            headX = Mth.lerp(graze, headX, 0.6F);
            float pluck = Mth.sin(age * 0.25F) * 0.15F;
            trunk[0].xRot = Mth.lerp(graze, trunk[0].xRot, -0.5F + pluck);
            trunk[1].xRot = Mth.lerp(graze, trunk[1].xRot, -0.3F);
            trunk[3].xRot = Mth.lerp(graze, trunk[3].xRot, -1.2F - pluck);
            yaw *= 1.0F - graze;
        }
        if (state.charging) headX = Mth.lerp(0.8F, headX, 0.45F); // tusks lowered at the target
        if (giant != null && giant.trumpet > 0.0F) {
            // head flung up, the trunk raised high in an S
            float t = giant.trumpet;
            headX = Mth.lerp(t, headX, -0.45F);
            trunk[0].xRot = Mth.lerp(t, trunk[0].xRot, -2.3F);
            trunk[1].xRot = Mth.lerp(t, trunk[1].xRot, -0.6F);
            trunk[2].xRot = Mth.lerp(t, trunk[2].xRot, -0.4F);
            trunk[3].xRot = Mth.lerp(t, trunk[3].xRot, 0.5F);
            leftEar.yRot = Mth.lerp(t, leftEar.yRot, -1.1F); // ears spread
            rightEar.yRot = Mth.lerp(t, rightEar.yRot, 1.1F);
        }
        if (giant != null && giant.swipe > 0.0F) {
            // the sweep: head and tusks swing hard from one side to the other, the trunk lashing behind
            float s = giant.swipe;
            float swing = Mth.sin((s - 0.5F) * Mth.PI); // -1 .. 1
            yaw = -swing * 0.7F;
            headX = Mth.lerp(Mth.sin(s * Mth.PI), headX, 0.5F);
            for (int i = 0; i < 4; i++) trunk[i].zRot = swing * (0.5F + i * 0.2F);
        }
        head.xRot = headX;
        head.yRot = yaw;

        // ears: slow fanning, a flick now and then; tail swishes
        leftEar.yRot += -Mth.sin(age * 0.08F + seed) * 0.1F - GiantPose.pulse(age * 0.05F + seed) * 0.4F;
        rightEar.yRot += Mth.sin(age * 0.08F + seed + 1.0F) * 0.1F + GiantPose.pulse(age * 0.043F + seed + 2.0F) * 0.4F;
        tail.zRot = GiantPose.pulse(age * 0.07F + seed * 1.3F) * Mth.sin(age * 0.9F) * 0.5F + Mth.sin(phase) * 0.1F * speed;
        tail.xRot = TAIL_X + speed * 0.3F;
    }
}
