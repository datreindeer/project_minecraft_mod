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
 * Saltwater crocodile: a broad armoured body with rows of raised scutes, a wide skull table with eye bosses, a long heavy
 * snout with ragged teeth over a lower jaw that gapes, a five-part tail with its crest of scutes, sprawled legs on webbed
 * clawed feet. Walks in the sprawling side-to-side wave; swimming the legs fold back and the tail sculls; lurking it lies
 * dead still, jaws shut; seizing it gapes and the renderer rolls the whole animal (JungleRenderer); basking it gapes now
 * and then. Geometry and skin: scripts/gen_jungle_models.py (crocodile(), JungleGeometry.crocodile()).
 */
public class CrocodileModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "saltwater_crocodile"), "main");
    private static final int TAIL = 5;
    private final ModelPart body, head, jaw;
    private final ModelPart[] tail = new ModelPart[TAIL];
    private final ModelPart rightFront, leftFront, rightHind, leftHind;
    private final ModelPart[] legs, lowers;

    public CrocodileModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        ModelPart t = body;
        for (int i = 0; i < TAIL; i++) tail[i] = t = t.getChild("tail" + i);
        rightFront = body.getChild("right_front_leg");
        leftFront = body.getChild("left_front_leg");
        rightHind = body.getChild("right_hind_leg");
        leftHind = body.getChild("left_hind_leg");
        legs = new ModelPart[] {rightFront, leftFront, rightHind, leftHind};
        lowers = new ModelPart[] {rightFront.getChild("right_front_lower"), leftFront.getChild("left_front_lower"),
                rightHind.getChild("right_hind_lower"), leftHind.getChild("left_hind_lower")};
    }

    public static LayerDefinition createBodyLayer() {
        return JungleGeometry.crocodile();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFront, rightFront}, new ModelPart[] {leftHind, rightHind});
            for (ModelPart l : lowers) l.zRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot *= 1.25F; // splayed flat
            jaw.xRot = 0.3F;
            for (int i = 0; i < TAIL; i++) tail[i].yRot = 0.1F;
            return;
        }
        boolean rolling = Boolean.TRUE.equals(state.getRenderData(JungleRenderer.ROLLING));
        boolean lurking = Boolean.TRUE.equals(state.getRenderData(JungleRenderer.LURKING));
        if (rolling) {
            jaw.xRot = 0.5F; // jaws clamped round the prey, a gap showing
            for (int i = 0; i < TAIL; i++) tail[i].yRot = Mth.sin(age * 0.6F - i) * 0.2F;
            for (ModelPart leg : legs) leg.xRot = 1.2F * Math.signum(leg.z); // legs tucked to the body
            return;
        }
        if (state.swimming || lurking) {
            // legs folded back along the flanks, the tail sculling (lurking: barely)
            for (ModelPart leg : legs) {
                leg.xRot = 1.3F;
                leg.zRot *= 0.5F;
            }
            float amp = lurking ? 0.03F : 0.3F;
            for (int i = 0; i < TAIL; i++) tail[i].yRot = Mth.sin(age * (lurking ? 0.05F : 0.3F) - i * 0.8F) * amp;
            head.yRot = lurking ? 0.0F : Mth.clamp(state.yRot, -20.0F, 20.0F) * Mth.DEG_TO_RAD;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.4F, 1.0F);
        float phase = state.walkAnimationPos * 0.8F;
        for (int i = 0; i < 4; i++) {
            ModelPart leg = legs[i];
            boolean a = leg == rightFront || leg == leftHind;
            float p = a ? phase : phase + Mth.PI;
            float lift = Math.max(0.0F, -Mth.sin(p)) * 0.4F * speed;
            leg.xRot = Mth.cos(p) * 0.6F * speed;
            leg.zRot -= Math.signum(leg.x) * lift;
            lowers[i].zRot += Math.signum(leg.x) * lift * 0.7F;
        }
        float wave = Mth.sin(phase) * speed;
        body.yRot = wave * 0.1F;
        for (int i = 0; i < TAIL; i++) tail[i].yRot = Mth.sin(phase - 1.0F - i * 0.7F) * 0.2F * speed + Mth.sin(age * 0.04F + i) * 0.03F;
        head.yRot = -wave * 0.1F + Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        // the gape: basking crocodiles open their jaws now and then
        float c = (age + state.entityId * 53) % 300.0F;
        jaw.xRot = speed < 0.1F && c < 80 ? Math.max(0.0F, Mth.sin(Math.min(c, 80 - c) / 20.0F * Mth.HALF_PI)) * 0.55F : 0.0F;
    }
}
