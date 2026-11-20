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
 * Sturgeon: a long armoured body with a ridge of bony scutes down the back, a wedge head running out into a long
 * pointed snout with four barbels underneath, a narrow tail stock and the shark-like tail whose upper lobe sweeps up and
 * back. Slow, deep tail beats; the pectoral fins held out like wings. Rendered at 0.85x. Texture:
 * textures/entity/wildlife/sturgeon.png (64x64), scripts/gen_fauna_coast_textures.py.
 */
public class SturgeonModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "sturgeon"), "main");
    private static final float FIN_Z = 0.3F;

    private final ModelPart head, tail, tailTip, rightFin, leftFin;

    public SturgeonModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("tail");
        tailTip = tail.getChild("tail_tip");
        rightFin = body.getChild("right_fin");
        leftFin = body.getChild("left_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -3.5F, -8.0F, 6.0F, 7.0F, 16.0F)
                .texOffs(0, 23).addBox(-0.5F, -4.5F, -7.0F, 1.0F, 1.0F, 14.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(12, 38).addBox(-2.5F, -3.0F, -5.0F, 5.0F, 5.0F, 5.0F)
                .texOffs(32, 38).addBox(-1.5F, -1.0F, -10.0F, 3.0F, 2.0F, 5.0F)
                .texOffs(38, 49).addBox(-1.5F, 1.0F, -9.0F, 3.0F, 2.0F, 0.0F), PartPose.offset(0.0F, 0.5F, -8.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(30, 23).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 8.0F)
                .texOffs(8, 49).addBox(0.0F, -4.5F, 3.0F, 0.0F, 2.0F, 4.0F)
                .texOffs(16, 49).addBox(0.0F, 2.5F, 3.0F, 0.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -0.5F, 8.0F));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(48, 38).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 3.0F, 4.0F)
                .texOffs(0, 38).addBox(0.0F, -6.0F, 2.0F, 0.0F, 5.0F, 6.0F)
                .texOffs(0, 49).addBox(0.0F, 1.5F, 2.0F, 0.0F, 3.0F, 4.0F), PartPose.offset(0.0F, -0.5F, 8.0F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(22, 49).addBox(-4.0F, 0.0F, 0.0F, 4.0F, 0.0F, 4.0F),
                PartPose.offsetAndRotation(-3.0F, 2.5F, -5.0F, 0.0F, 0.0F, FIN_Z));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(22, 49).mirror().addBox(0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 4.0F),
                PartPose.offsetAndRotation(3.0F, 2.5F, -5.0F, 0.0F, 0.0F, -FIN_Z));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isHung(state)) {
            // hung by the tail: the fins fall back along the body
            rightFin.zRot = 1.2F;
            leftFin.zRot = -1.2F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            rightFin.zRot = leftFin.zRot = CarcassRenderer.LIMB_SAG;
            return;
        }
        float t = state.ageInTicks;
        float beat = state.isInWater ? 0.25F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.7F : 1.2F;
        float rate = state.isInWater ? 0.18F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.3F : 1.2F;
        tail.yRot = -Mth.sin(t * rate) * 0.2F * beat;
        tailTip.yRot = -Mth.sin(t * rate - 1.0F) * 0.35F * beat;
        head.yRot = Mth.sin(t * rate) * 0.04F * beat;
        rightFin.zRot = FIN_Z + Mth.sin(t * 0.12F) * 0.08F;
        leftFin.zRot = -FIN_Z - Mth.sin(t * 0.12F) * 0.08F;
    }
}
