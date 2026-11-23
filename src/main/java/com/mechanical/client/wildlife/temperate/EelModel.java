package com.mechanical.client.wildlife.temperate;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Eel: a long, round, snake-like body in four segments tapering to a fin-fringed tail, with a small pointed head. It
 * swims by throwing waves down its length (each segment a little behind the one before), lazily when it idles and hard
 * when it flees; out of the water it writhes. Texture 64x32: textures/entity/wildlife/temperate/eel.png.
 */
public class EelModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "eel"), "main");
    private final ModelPart[] segments = new ModelPart[4];

    public EelModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        segments[0] = body;
        for (int i = 1; i < 4; i++) segments[i] = segments[i - 1].getChild("segment" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 6.0F)
                .texOffs(16, 8).addBox(-0.75F, -0.75F, -4.0F, 1.5F, 1.5F, 2.0F), PartPose.offset(0.0F, 22.5F, -4.0F));
        PartDefinition s1 = body.addOrReplaceChild("segment1", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F)
                .texOffs(0, 16).addBox(0.0F, -2.0F, 1.0F, 0.0F, 1.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 4.0F));
        PartDefinition s2 = s1.addOrReplaceChild("segment2", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-0.75F, -0.85F, 0.0F, 1.5F, 1.7F, 6.0F)
                .texOffs(10, 16).addBox(0.0F, -1.85F, 0.0F, 0.0F, 1.0F, 6.0F)
                .texOffs(22, 16).addBox(0.0F, 0.85F, 1.0F, 0.0F, 1.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 6.0F));
        s2.addOrReplaceChild("segment3", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-0.5F, -0.7F, 0.0F, 1.0F, 1.4F, 5.0F)
                .texOffs(32, 16).addBox(0.0F, -1.7F, 0.0F, 0.0F, 3.4F, 6.0F), PartPose.offset(0.0F, 0.0F, 6.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return; // limp
        float t = state.ageInTicks;
        boolean wet = state.isInWater;
        float amp = wet ? 0.18F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.3F : 0.55F;
        float rate = wet ? 0.25F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.5F : 1.2F;
        for (int i = 1; i < 4; i++) segments[i].yRot = Mth.sin(t * rate - i * 0.9F) * amp;
        segments[0].yRot = Mth.sin(t * rate + 0.9F) * amp * 0.4F;
    }
}
