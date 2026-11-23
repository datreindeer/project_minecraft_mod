package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Badger: low, wedge-shaped (narrow shoulders, broad rump) on short black legs with long digging claws in front,
 * shaggy guard hairs hanging below the flanks, a tapering striped head and a short grey tail. Waddling walk with the
 * head swinging low; digs with the forepaws and snout when grazing; head thrust forward when it goes for you.
 * Texture: textures/entity/wildlife/badger.png, painted by scripts/gen_small_models.py (with the geometry, SmallGeometry).
 */
public class BadgerModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "badger"), "main");
    private static final float TAIL_X = -0.5F;

    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart[] legs;

    public BadgerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = body.getChild("tail");
        this.head = body.getChild("head");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return SmallGeometry.badger(); // scripts/gen_small_models.py
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.25F; // in line with the body, snout down
            tail.xRot = 2.6F; // sticks back (+z): folds down over the rump
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            tail.xRot = 0.2F;
            return;
        }

        if (state.dozing) {
            // flat on its belly (body bottom ~21.5 onto 24), legs folded under, chin on the forepaws
            body.y += 2.5F;
            for (ModelPart leg : legs) leg.y += 2.2F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -1.4F;
            rightHindLeg.xRot = leftHindLeg.xRot = 1.4F;
            head.xRot = 0.15F;
            head.y += 0.5F;
            tail.xRot = 0.1F;
            body.y += Mth.sin(t * 0.08F) * 0.1F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.3F);
        float phase = state.walkAnimationPos * 1.0F;
        // Short-legged diagonal walk with a rolling waddle.
        float swing = Mth.cos(phase) * 0.8F * speed;
        rightFrontLeg.xRot = leftHindLeg.xRot = swing;
        leftFrontLeg.xRot = rightHindLeg.xRot = -swing;
        body.zRot = Mth.sin(phase) * 0.06F * speed;
        body.y -= Math.abs(Mth.sin(phase)) * 0.4F * speed;

        // Head carried low, swinging side to side as it noses along.
        head.yRot = Mth.clamp(state.yRot, -45.0F, 45.0F) * Mth.DEG_TO_RAD + Mth.sin(phase * 0.5F) * 0.15F * speed;
        head.xRot = Mth.clamp(state.xRot, -20.0F, 20.0F) * Mth.DEG_TO_RAD + 0.1F;

        // Digging (graze): snout in the earth, a forepaw raking in turn.
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            head.xRot = Mth.lerp(graze, head.xRot, 0.75F + Mth.sin(t * 0.7F) * 0.06F);
            head.y += graze * 1.2F;
            float rake = Mth.sin(t * 0.9F);
            rightFrontLeg.xRot += graze * (-0.6F + Math.max(0.0F, rake) * 0.9F);
            leftFrontLeg.xRot += graze * (-0.6F + Math.max(0.0F, -rake) * 0.9F);
        }

        // Going for you: head thrust forward and low, snapping.
        if (state.charging) {
            head.z -= 1.0F;
            head.xRot = 0.2F + Math.max(0.0F, Mth.sin(t * 0.8F)) * 0.15F;
            tail.xRot = TAIL_X - 0.6F; // bristling up
        } else {
            tail.xRot = TAIL_X + 0.15F * speed;
        }
        tail.yRot = Mth.sin(t * 0.2F) * 0.1F;
    }
}
