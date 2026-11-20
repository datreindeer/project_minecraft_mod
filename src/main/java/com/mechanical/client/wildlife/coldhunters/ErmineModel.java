package com.mechanical.client.wildlife.coldhunters;

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
 * Ermine (stoat): long, slim body low on very short legs, a small flat head with round ears, and a thin tail with its
 * black tip. Animation in {@link PredatorModel}: its gallop is the stoat's bounding run. Texture 64x32:
 * textures/entity/wildlife/ermine.png (brown) and ermine_winter.png (white), painted by
 * scripts/gen_fauna_coldhunters_textures.py (ERMINE table mirrors these cubes).
 */
public class ErmineModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "ermine"), "main");

    public ErmineModel(ModelPart root) {
        super(root, 1.2F, 1.0F, 2.0F, 1.3F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F)
                        .texOffs(0, 7).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 19.5F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 4.5F, 1.3F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(20, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 3.0F)
                        .texOffs(32, 0).addBox(-1.0F, -0.5F, -4.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -0.5F, -4.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(32, 2).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.5F, -3.0F));
        for (float x : new float[] {-1.0F, 1.0F}) {
            boolean left = x > 0;
            head.addOrReplaceChild(left ? "left_ear" : "right_ear", CubeListBuilder.create().mirror(left)
                            .texOffs(38, 0).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(x * 1.1F, -1.4F, -1.2F, 0.0F, 0.0F, left ? 0.2F : -0.2F));
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offset(x, 20.5F, -2.5F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 1.5F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(14, 15).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 3.0F, 2.0F),
                    PartPose.offset(x, 20.0F, 3.0F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 2.0F, 0.3F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(4, 15).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(8, 15).addBox(-0.5F, 1.0F, -1.0F, 1.0F, 1.0F, 2.0F);
    }
}
