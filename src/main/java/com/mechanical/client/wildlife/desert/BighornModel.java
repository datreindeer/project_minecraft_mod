package com.mechanical.client.wildlife.desert;

import com.mechanical.wildlife.mountain.client.BovidModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Bighorn Sheep: the ibex's build and cubes (IbexModel; the same UV layout, so the textures are painted from its IBEX table
 * in scripts/gen_fauna_desert_textures.py), without the beard. The ram's horns are the bighorn's: heavy at the base,
 * they sweep back from the poll, round down behind the ear and forward again past the cheek in a full curl, flaring
 * outward; the ewe has short, slightly curved horns. Animation in {@link BovidModel} (butting = the charge).
 * Lambs: the same model scaled down. Texture 64x64: textures/entity/wildlife/bighorn_{ram,ewe,lamb}.png.
 */
public class BighornModel extends BovidModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "bighorn_sheep"), "main");

    public BighornModel(ModelPart root) {
        super(root, 0.5F, -0.15F, 8.0F, 22.8F, 0.3F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 9.0F, 16.0F)
                        .texOffs(0, 25).addBox(-3.0F, -5.0F, -8.0F, 6.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(48, 11).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, 7.8F, 0.3F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(26, 25).addBox(-2.0F, -6.0F, -2.5F, 4.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -7.0F, 0.5F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(44, 25).addBox(-2.0F, -2.5F, -4.0F, 4.0F, 4.0F, 5.0F)
                        .texOffs(48, 0).addBox(-1.5F, -1.5F, -7.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, -0.15F, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 6).mirror().addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, -1.8F, 0.3F, 0.0F, 0.3F, -0.3F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 6).addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, -1.8F, 0.3F, 0.0F, -0.3F, 0.3F));

        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition small = head.addOrReplaceChild("small_horns", CubeListBuilder.create(), PartPose.ZERO);
        for (float side : new float[] {1.0F, -1.0F}) {
            String p = side > 0 ? "left_" : "right_";
            // the ram's curl: back from the poll, down behind the ear, forward past the cheek, flaring outward
            PartDefinition a = horns.addOrReplaceChild(p + "horn", CubeListBuilder.create().texOffs(54, 11).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(1.3F * side, -2.2F, -1.4F, -1.1F, 0.0F, 0.35F * side));
            PartDefinition b = a.addOrReplaceChild(p + "horn_2", CubeListBuilder.create().texOffs(54, 11).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, -3.6F, 0.0F, -1.15F, 0.0F, 0.15F * side));
            PartDefinition c = b.addOrReplaceChild(p + "horn_3", CubeListBuilder.create().texOffs(54, 17).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -3.6F, 0.0F, -1.15F, 0.0F, 0.15F * side));
            c.addOrReplaceChild(p + "horn_tip", CubeListBuilder.create().texOffs(54, 17).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -3.6F, 0.0F, -1.0F, 0.0F, 0.2F * side));
            // the ewe's: short, curving back
            PartDefinition s = small.addOrReplaceChild(p + "horn", CubeListBuilder.create().texOffs(58, 17).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(1.0F * side, -2.2F, -1.6F, -0.35F, 0.0F, 0.15F * side));
            s.addOrReplaceChild(p + "horn_tip", CubeListBuilder.create().texOffs(58, 17).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -2.6F, 0.0F, -0.6F, 0.0F, 0.0F));
        }

        for (float x : new float[] {-2.3F, 2.3F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 37).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offset(x, 15.0F, -5.5F));
            front.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(12, 37).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                    PartPose.offset(0.0F, 4.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(20, 37).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 4.0F), PartPose.offset(x, 14.0F, 5.5F));
            hind.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(12, 37).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                    PartPose.offset(0.0F, 5.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
