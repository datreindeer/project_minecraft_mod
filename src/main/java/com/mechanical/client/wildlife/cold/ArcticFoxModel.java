package com.mechanical.client.wildlife.cold;

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
 * Arctic fox: a compact, round little fox built against the cold - short legs, a short blunt muzzle, small rounded ears
 * close to a broad furry head, a deep fluffy chest, and a long, very thick tail. Animation in {@link PredatorModel}
 * (trot, nose to the ground, curled asleep with the tail round, limp carcass, hung). Texture 64x32:
 * textures/entity/wildlife/arctic_fox.png (winter) and arctic_fox_summer.png, painted by
 * scripts/gen_fauna_cold_textures.py whose FOX table mirrors these cubes.
 */
public class ArcticFoxModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "arctic_fox"), "main");

    public ArcticFoxModel(ModelPart root) {
        super(root, 0.9F, 2.0F, 4.0F, 1.2F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 4.5F, 1.2F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-2.5F, -2.5F, -3.5F, 5.0F, 4.0F, 4.0F)
                        .texOffs(18, 15).addBox(-1.0F, -0.5F, -5.5F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -1.5F, -5.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(26, 15).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 1.0F, -3.5F));
        for (float x : new float[] {-1.5F, 1.5F}) {
            boolean left = x > 0;
            head.addOrReplaceChild(left ? "left_ear" : "right_ear", CubeListBuilder.create().mirror(left)
                            .texOffs(42, 0).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(x, -2.3F, -1.0F, 0.0F, 0.0F, left ? 0.2F : -0.2F));
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 23).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offset(x, 19.0F, -3.5F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 2.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(8, 23).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 3.0F, 3.0F),
                    PartPose.offset(x, 19.0F, 3.5F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 2.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Short furred shank and a furry paw. */
    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(18, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(26, 23).addBox(-1.0F, 2.0F, -1.5F, 2.0F, 1.0F, 2.0F);
    }
}
