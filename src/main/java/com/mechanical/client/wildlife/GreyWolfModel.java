package com.mechanical.client.wildlife;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Grey wolf: deep chest under a thick shoulder mane, narrow waist, long legs (back ~14 px), a long head with a narrow
 * muzzle, pointed upright ears and cheek ruffs, and a bushy tail carried low. Animation in {@link PredatorModel}.
 * Geometry and skin: scripts/gen_pred_models.py (PredGeometry.greyWolf, textures/entity/wildlife/grey_wolf.png).
 */
public class GreyWolfModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "grey_wolf"), "main");

    public GreyWolfModel(ModelPart root) {
        super(root, 0.75F, 3.0F, 7.0F, 0.75F);
    }

    public static LayerDefinition createBodyLayer() {
        return PredGeometry.greyWolf();
    }
}
