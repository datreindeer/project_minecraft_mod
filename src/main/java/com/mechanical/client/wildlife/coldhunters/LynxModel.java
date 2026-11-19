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
 * Lynx: a tall, short-bodied cat on long legs with big furry paws, a bobbed black-tipped tail, a broad face framed by
 * hanging cheek ruffs, and black ear tufts. Animation in {@link PredatorModel} (stalking crouch, pounce, curled sleep,
 * limp carcass, hung by the hind legs). Texture 64x64: textures/entity/wildlife/lynx.png (and lynx_kitten.png), painted
 * by scripts/gen_fauna_coldhunters_textures.py whose LYNX table mirrors these cubes.
 */
public class LynxModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "lynx"), "main");

    public LynxModel(ModelPart root) {
        super(root, 0.7F, 4.0F, 8.5F, 1.1F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.5F, -7.0F, 6.0F, 7.0F, 8.0F)
                        .texOffs(0, 15).addBox(-2.5F, -3.0F, 1.0F, 5.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 7.5F, 1.1F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(28, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 5.0F, 4.0F)
                        .texOffs(28, 9).addBox(-4.0F, -1.0F, -2.0F, 8.0F, 4.0F, 2.0F)
                        .texOffs(48, 9).addBox(-1.5F, -0.5F, -6.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -2.5F, -7.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(48, 13).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 1.5F, -4.0F));
        for (float x : new float[] {-2.0F, 2.0F}) {
            boolean left = x > 0;
            head.addOrReplaceChild(left ? "left_ear" : "right_ear", CubeListBuilder.create().mirror(left)
                            .texOffs(58, 6).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F)
                            .texOffs(58, 10).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(x, -2.8F, -1.5F, 0.0F, 0.0F, left ? 0.12F : -0.12F));
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 28).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 6.0F, 3.0F),
                    PartPose.offset(x * 0.9F, 14.0F, -4.5F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 5.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(30, 28).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 7.0F, 4.0F),
                    PartPose.offset(x * 0.9F, 13.0F, 6.0F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 6.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Cannon bone and the big snowshoe paw. */
    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(10, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(18, 28).addBox(-1.5F, 4.0F, -2.0F, 3.0F, 1.0F, 3.0F);
    }
}
