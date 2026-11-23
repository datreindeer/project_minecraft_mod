package com.mechanical.client.wildlife.savanna;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Aardvark: a stocky, arched, sparsely haired body on short thick legs with big digging claws, a long tubular snout, tall
 * narrow ears and a thick tapering tail. Waddles, digs with its head down and the forefeet scratching (grazeAmount),
 * sleeps curled on its side by day, lies limp as a carcass. Young are the adult scaled down. Texture 64x32:
 * textures/entity/wildlife/aardvark.png, painted by scripts/gen_fauna_savanna_textures.py whose AARDVARK table mirrors
 * the cubes below (keep them in sync).
 */
public class AardvarkModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "aardvark"), "main");
    private static final float TAIL_REST = -0.55F; // the thick tail droops back to the ground

    private final ModelPart body, head, rightEar, leftEar, tail, rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;

    public AardvarkModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightEar = head.getChild("right_ear");
        this.leftEar = head.getChild("left_ear");
        this.tail = body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return SavannaGeometry.aardvark();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        ModelPart[] legs = {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.2F;
            rightEar.xRot = leftEar.xRot = 1.2F;
            tail.xRot = -1.6F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            rightEar.zRot -= 0.8F;
            leftEar.zRot += 0.4F;
            tail.xRot = 0.1F;
            return;
        }
        if (state.dozing) {
            // curled up: body down on the ground, legs tucked, snout under the tail
            body.y += 5.0F + Mth.sin(age * 0.07F) * 0.15F;
            for (ModelPart leg : legs) {
                leg.y += 4.5F;
                leg.xRot = leg == rightHindLeg || leg == leftHindLeg ? -1.4F : 1.4F;
            }
            head.xRot = 0.6F;
            head.yRot = 0.6F;
            rightEar.xRot = leftEar.xRot = 1.0F; // laid flat
            tail.xRot = 0.0F;
            tail.yRot = 0.9F;
            return;
        }

        float pos = state.walkAnimationPos, speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float swing = Mth.cos(pos * 0.8F) * speed * 0.9F;
        rightFrontLeg.xRot = swing;
        leftHindLeg.xRot = swing;
        leftFrontLeg.xRot = -swing;
        rightHindLeg.xRot = -swing;
        body.zRot = Mth.sin(pos * 0.8F) * 0.05F * speed; // a waddle
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot += Mth.clamp(state.xRot, -20.0F, 20.0F) * 0.5F * Mth.DEG_TO_RAD + Mth.sin(age * 0.3F) * 0.03F; // sniffing
        tail.xRot = TAIL_REST + Mth.sin(pos * 0.8F) * 0.05F * speed;
        tail.yRot = Mth.sin(age * 0.06F) * 0.1F;
        float flick = Mth.sin(age * 0.05F) > 0.92F ? Mth.sin(age * 1.2F) * 0.3F : 0.0F;
        rightEar.xRot += flick;

        // digging: snout in the dirt, forefeet scratching fast in turn
        if (state.grazeAmount > 0) {
            float g = state.grazeAmount;
            head.xRot = Mth.lerp(g, head.xRot, 0.9F);
            body.xRot = Mth.lerp(g, body.xRot, 0.15F);
            rightFrontLeg.xRot = Mth.lerp(g, rightFrontLeg.xRot, -0.6F + Mth.sin(age * 1.6F) * 0.6F);
            leftFrontLeg.xRot = Mth.lerp(g, leftFrontLeg.xRot, -0.6F - Mth.sin(age * 1.6F) * 0.6F);
            rightEar.xRot = leftEar.xRot = Mth.lerp(g, rightEar.xRot, 0.6F);
        }
    }
}
