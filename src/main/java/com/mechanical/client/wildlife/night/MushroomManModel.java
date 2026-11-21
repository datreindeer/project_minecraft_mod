package com.mechanical.client.wildlife.night;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mushroom Man: a pale stem of a body on two stubby feet, two thin arms, and a broad brown cap with a knob on top (the
 * head, which turns). Texture 64x64: textures/entity/night/mushroom_man.png (scripts/gen_fauna_night_textures.py, whose
 * box table mirrors these cubes).
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
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -5.0F, -7.0F, 14.0F, 5.0F, 14.0F)
                .texOffs(0, 19).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 2.0F, 8.0F), PartPose.offset(0.0F, 9.0F, 0.0F));
        root.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(0, 29).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 10.0F, 5.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(24, 29).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.6F, 19.0F, 0.0F));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.6F, 19.0F, 0.0F));
        CubeListBuilder arm = CubeListBuilder.create().texOffs(36, 29).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F);
        root.addOrReplaceChild("right_arm", arm, PartPose.offsetAndRotation(-4.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        root.addOrReplaceChild("left_arm", arm, PartPose.offsetAndRotation(4.0F, 10.0F, 0.0F, 0.0F, 0.0F, -0.15F));
        return LayerDefinition.create(mesh, 64, 64);
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
