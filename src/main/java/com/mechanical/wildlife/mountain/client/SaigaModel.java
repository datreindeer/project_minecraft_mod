package com.mechanical.wildlife.mountain.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Saiga: a barrel-bodied steppe antelope (back ~14 px) on thin legs, the neck carried low and forward, a large head ending
 * in the swollen, drooping proboscis nose with a soft lip hanging under it, short rounded ears. Bucks carry upright,
 * slightly lyre-shaped amber horns whose tips turn forward; does are hornless. Animation in {@link BovidModel}.
 * Texture 64x64: textures/entity/wildlife/saiga_{buck,doe,calf}.png (scripts/gen_fauna_mountain_textures.py, SAIGA table).
 */
public class SaigaModel extends BovidModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "saiga"), "main");

    public SaigaModel(ModelPart root) {
        super(root, 0.75F, 0.2F, 7.5F, 22.9F, 0.4F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.5F, -7.0F, 7.0F, 7.0F, 14.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(36, 21).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 6.8F, 0.3F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(42, 0).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -6.0F, 0.75F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(42, 10).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 5.0F)
                        // the proboscis: swollen and deep, drooping past the mouth, and the soft lip hanging under it
                        .texOffs(0, 21).addBox(-1.5F, -1.5F, -7.0F, 3.0F, 4.0F, 3.0F)
                        .texOffs(12, 21).addBox(-1.0F, 2.0F, -6.5F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.2F, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(28, 21).mirror().addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.8F, -1.5F, 0.3F, 0.0F, 0.3F, -0.4F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(28, 21).addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-2.8F, -1.5F, 0.3F, 0.0F, -0.3F, 0.4F));
        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("small_horns", CubeListBuilder.create(), PartPose.ZERO); // does are hornless
        for (float side : new float[] {1.0F, -1.0F}) {
            String p = side > 0 ? "left_" : "right_";
            PartDefinition h = horns.addOrReplaceChild(p + "horn", CubeListBuilder.create().texOffs(20, 21).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                    PartPose.offsetAndRotation(0.9F * side, -1.8F, -1.6F, -0.2F, 0.0F, 0.12F * side));
            h.addOrReplaceChild(p + "horn_tip", CubeListBuilder.create().texOffs(24, 21).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -4.6F, 0.0F, 0.45F, 0.0F, -0.2F * side));
        }
        for (float x : new float[] {-2.0F, 2.0F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 28).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(x, 15.0F, -5.0F));
            front.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(8, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                    PartPose.offset(0.0F, 4.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(16, 28).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 5.0F, 3.0F), PartPose.offset(x, 15.0F, 5.0F));
            hind.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(8, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                    PartPose.offset(0.0F, 4.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
