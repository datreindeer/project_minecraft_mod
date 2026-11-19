package com.mechanical.client.wildlife.coast;

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
 * Pike: long torpedo body, a flat duck-bill snout on a long head, the dorsal and anal fins set far back near the forked
 * tail (the pike's mark), small pectoral fins. Hangs nearly still with just the fins fanning; the tail beats when it
 * swims and lashes when it darts. Texture: textures/entity/wildlife/pike.png (64x32), scripts/gen_fauna_coast_textures.py.
 */
public class PikeModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "pike"), "main");
    private static final float FIN_Z = 0.5F;

    private final ModelPart tail, caudal, rightFin, leftFin;

    public PikeModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        tail = body.getChild("tail");
        caudal = tail.getChild("caudal");
        rightFin = body.getChild("right_fin");
        leftFin = body.getChild("left_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -2.0F, -4.0F, 3.0F, 4.0F, 9.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(46, 0).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F)
                .texOffs(8, 13).addBox(-1.0F, -0.5F, -7.0F, 2.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 3.0F, 5.0F)
                .texOffs(0, 13).addBox(0.0F, -3.5F, 1.0F, 0.0F, 2.0F, 4.0F)
                .texOffs(18, 13).addBox(0.0F, 1.5F, 1.0F, 0.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 5.0F));
        tail.addOrReplaceChild("caudal", CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(24, 13).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F),
                PartPose.offsetAndRotation(-1.5F, 1.5F, -3.0F, 0.0F, 0.0F, FIN_Z));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(24, 13).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F),
                PartPose.offsetAndRotation(1.5F, 1.5F, -3.0F, 0.0F, 0.0F, -FIN_Z));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return; // limp
        float t = state.ageInTicks;
        float beat = state.isInWater ? 0.15F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.8F : 1.4F;
        float rate = state.isInWater ? 0.35F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.6F : 1.7F;
        tail.yRot = -Mth.sin(t * rate) * 0.35F * beat;
        caudal.yRot = -Mth.sin(t * rate - 0.9F) * 0.4F * beat;
        // the pectorals keep fanning even when it hangs still
        rightFin.zRot = FIN_Z + Mth.sin(t * 0.4F) * 0.3F;
        leftFin.zRot = -FIN_Z - Mth.sin(t * 0.4F) * 0.3F;
    }
}
