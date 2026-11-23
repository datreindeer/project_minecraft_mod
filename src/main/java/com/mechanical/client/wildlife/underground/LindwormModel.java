package com.mechanical.client.wildlife.underground;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.wildlife.underground.Lindworm;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Lindworm: a horned, long-jawed head, {@link #BODY} thick body segments with a spine ridge (the second carries the two
 * clawed forelegs), {@link #TAIL} thinner tail segments and a whip-thin tip, each hinged on the one before. It slithers
 * with a wave running down the body; hunting it rears its head and gapes; lunging it stretches out straight, jaws wide;
 * coiled round its prey every hinge bends the same way and the body winds into a ring. As a carcass it lies in a loose
 * S. Geometry and texture (128x64: textures/entity/wildlife/lindworm.png) from one table in scripts/gen_hunt_models.py
 * (UndergroundGeometry).
 */
public class LindwormModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "lindworm"), "main");
    static final int BODY = 7, TAIL = 4, ALL = BODY + TAIL + 1;

    private final ModelPart head, jaw, rightLeg, leftLeg;
    private final ModelPart[] seg = new ModelPart[ALL];

    public LindwormModel(ModelPart root) {
        super(root);
        seg[0] = root.getChild("seg0");
        for (int i = 1; i < ALL; i++) seg[i] = seg[i - 1].getChild("seg" + i);
        head = seg[0].getChild("head");
        jaw = head.getChild("jaw");
        rightLeg = seg[1].getChild("right_leg");
        leftLeg = seg[1].getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return UndergroundGeometry.lindworm();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isHung(state)) return;
        if (CarcassRenderer.isCarcass(state)) {
            for (int i = 1; i < ALL; i++) seg[i].yRot = (i / 3 % 2 == 0 ? 0.3F : -0.3F);
            rightLeg.zRot = 1.2F;
            leftLeg.zRot = -1.2F;
            jaw.xRot = 0.3F;
            return;
        }
        Integer pose = state.getRenderData(UndergroundClient.LINDWORM_POSE);
        float t = state.ageInTicks;
        if (pose != null && pose == Lindworm.COILING) { // wound round its prey, squeezing
            for (int i = 1; i < ALL; i++) seg[i].yRot = 0.62F + Mth.sin(t * 0.3F + i) * 0.04F;
            seg[0].xRot = -0.3F;
            jaw.xRot = 0.5F;
            return;
        }
        if (pose != null && pose == Lindworm.LUNGING) { // straight as a spear, jaws wide, claws forward
            jaw.xRot = 0.8F;
            seg[0].xRot = -0.1F;
            rightLeg.xRot = leftLeg.xRot = -1.0F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
        float phase = state.walkAnimationPos * 0.9F + t * 0.04F;
        float amp = 0.05F + 0.22F * speed, prev = 0.0F;
        for (int i = 0; i < ALL; i++) {
            float theta = amp * Mth.sin(phase - i * 0.7F);
            if (i > 0) seg[i].yRot = theta - prev;
            prev = theta;
        }
        float step = Mth.sin(state.walkAnimationPos * 1.6F) * 0.8F * speed;
        rightLeg.xRot = step;
        leftLeg.xRot = -step;
        head.yRot = Mth.clamp(state.yRot, -35.0F, 35.0F) * Mth.DEG_TO_RAD - seg[0].yRot;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        jaw.xRot = 0.05F + Mth.abs(Mth.sin(t * 0.05F)) * 0.08F;
        if (state.charging) { // reared, gaping
            seg[0].xRot = -0.35F;
            seg[1].xRot = 0.15F;
            head.xRot += 0.3F;
            jaw.xRot = 0.3F + Mth.abs(Mth.sin(t * 0.25F)) * 0.4F;
        }
    }
}
