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
 * Pine marten: a long-bodied tree weasel, bigger and leggier than the ermine, with a pointed face, broad rounded
 * cream-edged ears, a creamy-orange throat bib and a long bushy tail. Animation in {@link PredatorModel}; clinging to a
 * trunk the renderer stands the whole model upright (ColdhuntersClient). Texture 64x32:
 * textures/entity/wildlife/pine_marten.png, painted by scripts/gen_fauna_coldhunters_textures.py (MARTEN table).
 */
public class PineMartenModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "pine_marten"), "main");

    public PineMartenModel(ModelPart root) {
        super(root, 1.1F, 1.5F, 2.5F, 1.25F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 4.0F, 5.0F)
                        .texOffs(0, 9).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 18.5F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 5.5F, 1.25F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(28, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 3.0F, 3.0F)
                        .texOffs(42, 0).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, -1.0F, -5.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(42, 3).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -3.0F));
        for (float x : new float[] {-1.3F, 1.3F}) {
            boolean left = x > 0;
            head.addOrReplaceChild(left ? "left_ear" : "right_ear", CubeListBuilder.create().mirror(left)
                            .texOffs(50, 0).addBox(-1.0F, -1.5F, -0.5F, 2.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(x * 1.15F, -1.8F, -1.5F, 0.0F, 0.0F, left ? 0.25F : -0.25F));
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 19).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(x, 20.5F, -3.0F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 1.5F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(26, 19).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 3.0F, 3.0F),
                    PartPose.offset(x, 20.0F, 4.0F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 2.0F, 0.3F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(8, 19).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(16, 19).addBox(-1.0F, 1.0F, -2.0F, 2.0F, 1.0F, 3.0F);
    }
}
