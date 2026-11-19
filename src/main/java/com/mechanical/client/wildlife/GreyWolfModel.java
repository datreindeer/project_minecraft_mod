package com.mechanical.client.wildlife;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Grey wolf: deep chest under a thick shoulder mane, narrow waist, long legs (back ~14 px), a long head with a narrow
 * muzzle, pointed upright ears and cheek ruffs, and a bushy tail carried low. Animation in {@link PredatorModel}.
 * Texture 64x64: textures/entity/wildlife/grey_wolf.png, painted by scripts/gen_predator_textures.py whose WOLF table
 * mirrors the cubes below (keep them in sync).
 */
public class GreyWolfModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "grey_wolf"), "main");

    public GreyWolfModel(ModelPart root) {
        super(root, 0.75F, 3.0F, 7.0F, 0.75F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -7.0F, 6.0F, 6.0F, 8.0F)
                        .texOffs(0, 14).addBox(-2.5F, -2.5F, 1.0F, 5.0F, 5.0F, 7.0F)
                        .texOffs(28, 0).addBox(-3.5F, -4.0F, -8.0F, 7.0F, 7.0F, 4.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(52, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 7.5F, 0.75F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(28, 11).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 5.0F, 4.0F)
                        .texOffs(24, 20).addBox(-3.5F, -1.0F, -1.5F, 7.0F, 4.0F, 2.0F)
                        .texOffs(50, 0).addBox(-1.5F, -1.0F, -8.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, -2.0F, -8.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(50, 6).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 1.0F, -4.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(48, 11).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-1.8F, -2.8F, -1.5F, 0.0F, 0.0F, -0.15F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(48, 11).mirror().addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.8F, -2.8F, -1.5F, 0.0F, 0.0F, 0.15F));
        for (float x : new float[] {-1.6F, 1.6F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 26).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                    PartPose.offset(x, 15.0F, -4.5F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 4.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(28, 26).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 4.0F),
                    PartPose.offset(x, 14.0F, 6.0F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 5.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(10, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(18, 26).addBox(-1.0F, 4.0F, -2.0F, 2.0F, 1.0F, 3.0F);
    }
}
