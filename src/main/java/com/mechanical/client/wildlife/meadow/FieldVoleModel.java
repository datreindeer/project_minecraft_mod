package com.mechanical.client.wildlife.meadow;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Field vole: a round little body, a blunt face with small ears half hidden in the fur, short legs and a short tail.
 * Scurries in quick bursts, sits nibbling (grazing) with the head bobbing, and sleeps balled up. The young are this
 * model scaled down. Texture: textures/entity/wildlife/field_vole.png (32x32), scripts/gen_fauna_meadow_textures.py.
 */
public class FieldVoleModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "field_vole"), "main");

    private final ModelPart body, head, tail;
    private final ModelPart[] front, hind;

    public FieldVoleModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("tail");
        front = new ModelPart[] {root.getChild("right_front_leg"), root.getChild("left_front_leg")};
        hind = new ModelPart[] {root.getChild("right_hind_leg"), root.getChild("left_hind_leg")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -2.5F, 3.0F, 3.0F, 5.0F), PartPose.offset(0.0F, 21.5F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 0).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F).texOffs(26, 0).addBox(-1.0F, -0.5F, -3.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -0.5F, -2.5F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(6, 8).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, -1.5F, -0.5F, 0.0F, 0.0F, -0.3F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(6, 8).mirror().addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offsetAndRotation(1.0F, -1.5F, -0.5F, 0.0F, 0.0F, 0.3F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(10, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.0F, 23.0F, -1.5F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(10, 8).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offset(1.0F, 23.0F, -1.5F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(10, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.0F, 23.0F, 1.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(10, 8).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offset(1.0F, 23.0F, 1.5F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -0.5F, 2.5F, -0.3F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            tail.xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : front) leg.zRot = CarcassRenderer.LIMB_SAG;
            for (ModelPart leg : hind) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
            return;
        }
        if (state.dozing) {
            // balled up: head tucked down, body lowered and rounded, breathing fast
            body.y += 0.8F;
            body.zScale = 0.85F;
            body.yScale = 1.1F + Mth.sin(t * 0.3F) * 0.03F;
            head.xRot = 0.7F;
            head.z += 0.8F;
            tail.yRot = 1.2F;
            for (ModelPart leg : front) leg.visible = false;
            for (ModelPart leg : hind) leg.visible = false;
            return;
        }
        for (ModelPart leg : front) leg.visible = true;
        for (ModelPart leg : hind) leg.visible = true;
        float speed = Math.min(state.walkAnimationSpeed * 2.0F, 1.0F);
        float phase = state.walkAnimationPos * 2.2F; // quick scurrying steps
        front[0].xRot = hind[1].xRot = Mth.cos(phase) * 1.0F * speed;
        front[1].xRot = hind[0].xRot = -Mth.cos(phase) * 1.0F * speed;
        body.y -= Math.abs(Mth.sin(phase)) * 0.25F * speed;
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        // nibbling: sits back on its haunches, head bobbing quickly over the food
        float graze = state.grazeAmount;
        body.xRot = -graze * 0.35F;
        head.xRot += graze * (0.45F + Mth.sin(t * 2.2F) * 0.12F);
        tail.xRot = -0.3F + graze * 0.4F;
    }
}
