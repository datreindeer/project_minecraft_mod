package com.mechanical.client.wildlife.savanna;

import com.mechanical.client.wildlife.PredatorModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Cheetah: built for speed - deep narrow chest, a wasp waist, very long thin legs (back at 13.5 px, higher than the
 * leopard's though the body is slighter), a small round head with small ears, and a long tail. Animation in
 * {@link PredatorModel} (its bound is the sprint; the crouch is the stalk). Texture 64x64:
 * textures/entity/wildlife/cheetah.png, painted by scripts/gen_fauna_savanna_textures.py whose CHEETAH table mirrors the
 * cubes below (keep them in sync).
 */
public class CheetahModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "cheetah"), "main");

    public CheetahModel(ModelPart root) {
        super(root, 0.62F, 3.5F, 8.0F, 0.5F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.5F, -8.0F, 6.0F, 7.0F, 9.0F)    // deep chest
                        .texOffs(0, 16).addBox(-2.5F, -3.0F, 1.0F, 5.0F, 6.0F, 9.0F)    // narrow waist and hips
                        .texOffs(30, 0).addBox(-2.0F, -3.0F, -11.0F, 4.0F, 5.0F, 4.0F), // neck
                PartPose.offset(0.0F, 10.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(56, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 9.5F, 0.5F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(46, 0).addBox(-2.5F, -3.0F, -4.0F, 5.0F, 4.0F, 4.0F)
                        .texOffs(46, 8).addBox(-1.5F, -1.0F, -6.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -2.0F, -10.5F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(46, 12).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 1.0F, -4.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 8).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-1.8F, -2.8F, -1.5F, 0.0F, 0.0F, -0.3F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 8).mirror().addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(1.8F, -2.8F, -1.5F, 0.0F, 0.0F, 0.3F));
        for (float x : new float[] {-1.8F, 1.8F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 31).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 6.0F, 3.0F),
                    PartPose.offset(x, 12.0F, -5.5F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 5.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(28, 31).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 7.0F, 4.0F),
                    PartPose.offset(x, 11.0F, 7.0F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 6.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(10, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F)
                .texOffs(18, 31).addBox(-1.0F, 6.0F, -2.0F, 2.0F, 1.0F, 3.0F);
    }
}
