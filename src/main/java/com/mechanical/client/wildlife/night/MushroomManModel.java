package com.mechanical.client.wildlife.night;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mushroom Man: a pale stem of a body on two stubby feet, two thin arms, and a broad brown cap with a knob on top (the
 * head, which turns). Geometry and texture (128x64, textures/entity/night/mushroom_man.png) from one table in
 * scripts/gen_hunt_models.py (MushroomGeometry): a tiered, warty cap with gills under it and a torn veil, a stem with its
 * skirt and bulb, root feet.
 */
public class MushroomManModel extends EntityModel<CreeperRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "mushroom_man"), "main");
    private final ModelPart cap, rightLeg, leftLeg, rightArm, leftArm;

    public MushroomManModel(ModelPart root) {
        super(root);
        cap = root.getChild("cap");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        return MushroomGeometry.mushroomMan();
    }

    @Override
    public void setupAnim(CreeperRenderState state) {
        super.setupAnim(state);
        cap.yRot = state.yRot * Mth.DEG_TO_RAD;
        cap.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed;
        rightLeg.xRot = Mth.cos(pos * 0.8F) * 1.2F * speed;
        leftLeg.xRot = Mth.cos(pos * 0.8F + Mth.PI) * 1.2F * speed;
        // a slow shambling sway of the arms; held out stiff while it swells
        float sway = Mth.cos(pos * 0.8F + Mth.PI) * 0.6F * speed + Mth.sin(state.ageInTicks * 0.05F) * 0.05F;
        float out = Mth.clamp(state.swelling, 0.0F, 1.0F) * 1.2F;
        rightArm.xRot = sway - out;
        leftArm.xRot = -sway - out;
    }
}
