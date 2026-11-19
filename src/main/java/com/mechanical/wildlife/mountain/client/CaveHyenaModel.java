package com.mechanical.wildlife.mountain.client;

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
 * Cave hyena: the sloping back of a hyena - deep, high forequarters on long forelegs and a low, narrow rump on shorter
 * hind legs (back ~16 px), a bristly crest along the neck and shoulders, a thick neck, a broad head with a blunt, heavy
 * bone-crushing muzzle and big rounded ears, a short tail ending in a dark tuft. Animation in {@link PredatorModel}.
 * Texture 64x64: textures/entity/wildlife/cave_hyena.png (scripts/gen_fauna_mountain_textures.py, HYENA table).
 */
public class CaveHyenaModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "cave_hyena"), "main");

    public CaveHyenaModel(ModelPart root) {
        super(root, 0.7F, 3.0F, 7.0F, 0.55F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -4.0F, -8.0F, 7.0F, 8.0F, 9.0F)
                        .texOffs(0, 17).addBox(-3.0F, -2.0F, 1.0F, 6.0F, 6.0F, 7.0F)
                        .texOffs(32, 0).addBox(-1.0F, -5.5F, -9.0F, 2.0F, 2.0F, 9.0F)
                        .texOffs(26, 17).addBox(-2.5F, -3.5F, -11.0F, 5.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 7.5F, 0.55F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(24, 30).addBox(-1.5F, 5.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(44, 11).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 5.0F, 4.0F)
                        .texOffs(44, 20).addBox(-2.0F, -1.0F, -8.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, -1.5F, -11.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 30).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 2.0F, -4.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(54, 0).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-2.2F, -2.6F, -1.5F, 0.0F, 0.0F, -0.2F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(54, 0).mirror().addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(2.2F, -2.6F, -1.5F, 0.0F, 0.0F, 0.2F));
        for (float x : new float[] {-2.2F, 2.2F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 38).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(x, 15.0F, -5.0F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 4.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(30, 38).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 5.0F, 4.0F), PartPose.offset(x * 0.9F, 15.0F, 5.5F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 4.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(12, 38).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(20, 38).addBox(-1.0F, 4.0F, -2.0F, 2.0F, 1.0F, 3.0F);
    }
}
