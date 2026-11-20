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
 * Chamois: a slight goat-antelope (back ~13 px) on long thin legs, a slender upright neck, a short wedge head with long
 * pointed ears and a tiny tail. Both sexes carry short black horns that rise straight from above the eyes and hook sharply
 * back at the tip; kids show nubs ("small_horns"). Animation in {@link BovidModel}.
 * Texture 64x64: textures/entity/wildlife/chamois{,_kid}.png (scripts/gen_fauna_mountain_textures.py, CHAMOIS table).
 */
public class ChamoisModel extends BovidModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "chamois"), "main");

    public ChamoisModel(ModelPart root) {
        super(root, 0.4F, -0.1F, 7.5F, 22.9F, 0.15F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.5F, -6.5F, 6.0F, 7.0F, 13.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(52, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 6.3F, 0.3F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(38, 0).addBox(-1.5F, -6.0F, -2.0F, 3.0F, 7.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -5.5F, 0.4F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(38, 11).addBox(-1.5F, -2.0F, -3.5F, 3.0F, 3.0F, 4.0F)
                        .texOffs(52, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, -0.1F, 0.0F, 0.0F));
        // long pointed ears held out and a little up
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 5).mirror().addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, -1.6F, 0.0F, 0.0F, 0.3F, -0.15F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 5).addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, -1.6F, 0.0F, 0.0F, -0.3F, 0.15F));
        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition small = head.addOrReplaceChild("small_horns", CubeListBuilder.create(), PartPose.ZERO);
        for (float side : new float[] {1.0F, -1.0F}) {
            String p = side > 0 ? "left_" : "right_";
            // straight up from above the eyes, then the tip hooks hard back
            PartDefinition h = horns.addOrReplaceChild(p + "horn", CubeListBuilder.create().texOffs(56, 7).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.8F * side, -1.8F, -1.6F, -0.1F, 0.0F, 0.06F * side));
            h.addOrReplaceChild(p + "hook", CubeListBuilder.create().texOffs(60, 7).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -2.7F, 0.0F, -1.7F, 0.0F, 0.0F));
            small.addOrReplaceChild(p + "nub", CubeListBuilder.create().texOffs(60, 7).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(0.8F * side, -1.8F, -1.6F));
        }
        for (float x : new float[] {-1.7F, 1.7F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 20).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(x, 15.0F, -4.5F));
            front.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(8, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                    PartPose.offset(0.0F, 4.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(16, 20).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 5.0F, 3.0F), PartPose.offset(x, 15.0F, 4.5F));
            hind.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(8, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                    PartPose.offset(0.0F, 4.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
