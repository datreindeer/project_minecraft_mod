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
 * Cave lion: a big maneless cat - long, deep-chested body (back ~20 px), thick neck with a faint ruff, broad short-muzzled
 * head with small round ears, heavy forelegs on wide paws, and a long tail with a dark tuft. Animation in
 * {@link PredatorModel}: the deep crouch and the pounce matter most for it.
 * Texture 128x64: textures/entity/wildlife/cave_lion.png, painted by scripts/gen_predator_textures.py whose LION table
 * mirrors the cubes below (keep them in sync).
 */
public class CaveLionModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "cave_lion"), "main");

    public CaveLionModel(ModelPart root) {
        super(root, 0.6F, 5.0F, 9.0F, 0.45F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -5.0F, -10.0F, 8.0F, 10.0F, 11.0F)
                        .texOffs(0, 21).addBox(-3.5F, -4.5F, 1.0F, 7.0F, 9.0F, 10.0F)
                        .texOffs(38, 0).addBox(-3.0F, -4.0F, -13.0F, 6.0F, 7.0F, 4.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(104, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, 10.5F, 0.45F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(112, 0).addBox(-1.5F, 13.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(58, 0).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 6.0F, 5.0F)
                        .texOffs(82, 0).addBox(-2.0F, -1.5F, -9.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, -2.0F, -12.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(82, 7).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 1.5F, -5.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(98, 0).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, -3.8F, -2.0F, 0.0F, 0.0F, -0.2F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(98, 0).mirror().addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, -3.8F, -2.0F, 0.0F, 0.0F, 0.2F));
        for (float x : new float[] {-2.5F, 2.5F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(38, 11).addBox(-1.5F, -2.0F, -2.0F, 3.0F, 7.0F, 4.0F),
                    PartPose.offset(x, 13.0F, -6.5F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 5.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(34, 22).addBox(-2.0F, -2.0F, -2.5F, 4.0F, 8.0F, 5.0F),
                    PartPose.offset(x, 12.0F, 8.0F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 6.0F, 1.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(52, 11).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F)
                .texOffs(64, 11).addBox(-1.5F, 5.0F, -2.5F, 3.0F, 1.0F, 4.0F);
    }
}
