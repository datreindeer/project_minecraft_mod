package com.mechanical.client.wildlife.desert;

import com.mechanical.wildlife.mountain.client.BovidModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Bighorn Sheep: a stocky wild sheep with a short thick neck, on the mountain grazers' animation ({@link BovidModel}:
 * butting = the charge). The ram's horns: a heavy boss at the poll, then a curl of six thinning segments turning about the
 * head's own x axis, back, down behind the ear and forward past the cheek, flaring outward; the ewe has short, slightly
 * curved horns. Lambs: the same model scaled down. Geometry and the 128x64 skins
 * (textures/entity/wildlife/bighorn_{ram,ewe,lamb}.png) come from scripts/gen_desert_models.py (DesertGeometry).
 */
public class BighornModel extends BovidModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "bighorn_sheep"), "main");

    public BighornModel(ModelPart root) {
        super(root, 0.5F, -0.15F, 7.5F, 22.8F, 0.3F);
    }

    public static LayerDefinition createBodyLayer() {
        return DesertGeometry.bighorn();
    }
}
