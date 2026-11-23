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
 * Water monitor: a long low body on sprawled legs (upper leg out to the side, lower leg down to a clawed foot), a long
 * neck, a narrow head with a long snout and a forked tongue, a long tail flattened from side to side in four segments.
 * Walks with the sprawler's diagonal trot and S-wave through body and tail (as DesertLizardModel), the tongue flicking
 * out to taste the air; when still it raises the head on the long neck and looks about. Geometry and skin:
 * scripts/gen_jungle_models.py (monitor(), JungleGeometry.monitor()).
 */
public class MonitorModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "monitor_lizard"), "main");
    private final ModelPart body, neck, head, jaw, tongue;
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs, lowers;

    public MonitorModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        jaw = head.getChild("jaw");
        tongue = head.getChild("tongue");
        ModelPart t = body;
        for (int i = 0; i < 4; i++) tail[i] = t = t.getChild("tail" + i);
        rightFrontLeg = body.getChild("right_front_leg");
        leftFrontLeg = body.getChild("left_front_leg");
        rightHindLeg = body.getChild("right_hind_leg");
        leftHindLeg = body.getChild("left_hind_leg");
        legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
        lowers = new ModelPart[] {rightFrontLeg.getChild("right_front_lower"), leftFrontLeg.getChild("left_front_lower"),
                rightHindLeg.getChild("right_hind_lower"), leftHindLeg.getChild("left_hind_lower")};
    }

    public static LayerDefinition createBodyLayer() {
        return JungleGeometry.monitor();
    }

    /** +1 for the left legs, -1 for the right. */
    private static float side(ModelPart leg) {
        return Math.signum(leg.x);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        tongue.visible = false;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            for (ModelPart l : lowers) l.zRot = 0.0F;
            head.xRot = 0.2F;
            tail[0].xRot = 2.8F; // the limp tail flops down over the back
            for (int i = 1; i < 4; i++) tail[i].xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (int i = 0; i < 4; i++) {
                legs[i].zRot = CarcassRenderer.LIMB_SAG;
                lowers[i].zRot = 0.0F;
            }
            head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
            jaw.xRot = 0.3F;
            tongue.visible = true;
            for (int i = 0; i < 4; i++) tail[i].yRot = 0.2F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.4F, 1.0F);
        float phase = state.walkAnimationPos * 1.1F;
        for (int i = 0; i < 4; i++) {
            ModelPart leg = legs[i];
            float p = (leg == rightFrontLeg || leg == leftHindLeg) ? phase : phase + Mth.PI;
            float fwd = Mth.cos(p) * 0.8F * speed, lift = Math.max(0.0F, -Mth.sin(p)) * 0.5F * speed;
            leg.xRot = -fwd;
            leg.zRot -= side(leg) * lift;
            lowers[i].zRot += side(leg) * lift * 0.6F;
        }
        float wave = Mth.sin(phase) * speed;
        body.yRot = wave * 0.25F;
        neck.yRot = -wave * 0.2F + Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.6F;
        head.yRot = -wave * 0.1F + Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.4F;
        head.xRot = Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.6F;
        for (int i = 0; i < 4; i++) tail[i].yRot += Mth.sin(phase - 1.0F - i * 0.8F) * (0.25F + i * 0.06F) * speed + Mth.sin(age * 0.04F + i) * 0.04F;
        // the tongue: long flicks every second or so, more on the move
        float f = (age + state.entityId * 13) % (speed > 0.1F ? 18.0F : 34.0F);
        if (f < 5.0F) {
            tongue.visible = true;
            tongue.z = -Mth.sin(f / 5.0F * Mth.PI) * 2.5F;
        }
        // standing still: neck raised, head up, looking about
        float still = 1.0F - speed;
        neck.xRot -= 0.15F * still;
        head.xRot += 0.12F * still;
        float g = state.grazeAmount; // nosing the ground for food
        if (g > 0) {
            neck.xRot = Mth.lerp(g, neck.xRot, 0.4F);
            head.xRot = Mth.lerp(g, head.xRot, 0.3F + Mth.sin(age * 0.7F) * 0.08F);
            jaw.xRot = g * Math.max(0.0F, Mth.sin(age * 0.5F)) * 0.3F;
        }
    }
}
