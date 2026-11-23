package com.mechanical.client.wildlife.coldhunters;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Lemming: a round little ball of fur - stout body, blunt head sunk into it with the ears hidden in the fur, a stub of a
 * tail, tiny legs. Scurries with quick short steps and a bobbing head, sits up nibbling when it grazes, lies flat asleep.
 * Geometry and texture (textures/entity/wildlife/lemming.png) from one table in scripts/gen_hunt_models.py (HuntGeometry).
 */
public class LemmingModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "lemming"), "main");

    /** Built at twice its size (drawn at half, ColdhuntersClient): offsets in model px are doubled. */
    private static final float S = 2.0F;
    private final ModelPart body, head, tail;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public LemmingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return HuntGeometry.lemming();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.3F; // tucked into the chest, as the head of a hung rodent flops
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.zRot = 0.2F;
            return;
        }
        if (state.dozing) {
            // flattened into a fur ball, legs hidden, a slow breath
            body.y += S * (1.0F + Mth.sin(t * 0.1F) * 0.1F);
            head.y += S * 1.2F;
            head.xRot = 0.2F;
            for (ModelPart leg : legs) leg.y -= S * 0.5F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float phase = state.walkAnimationPos * 2.2F; // quick, short scurrying steps
        float swing = Mth.cos(phase) * 1.0F * speed;
        rightFrontLeg.xRot = leftHindLeg.xRot = swing;
        leftFrontLeg.xRot = rightHindLeg.xRot = -swing;
        float bob = Math.abs(Mth.sin(phase)) * 0.5F * S * speed;
        body.y -= bob;
        head.y -= bob;
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -20.0F, 20.0F) * Mth.DEG_TO_RAD;
        tail.yRot = Mth.sin(t * 0.3F) * 0.2F;

        // nibbling: sits back on its haunches, head and forepaws up, jaws working
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            body.xRot = -0.5F * graze;
            body.y += S * 0.5F * graze;
            head.y -= S * 1.2F * graze;
            head.z += S * graze;
            head.xRot += graze * (Mth.sin(t * 1.6F) * 0.12F);
            rightFrontLeg.y -= S * 1.5F * graze;
            leftFrontLeg.y -= S * 1.5F * graze;
            rightFrontLeg.z += S * graze;
            leftFrontLeg.z += S * graze;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -0.8F * graze;
        }
    }
}
