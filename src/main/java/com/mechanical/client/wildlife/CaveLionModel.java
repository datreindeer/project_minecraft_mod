package com.mechanical.client.wildlife;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Cave lion: a big maneless cat - deep chest and heavy shoulders (back ~19 px), a faint ruff down the throat, a loose belly
 * fold, a broad head with a heavy brow, wide jowls and a white muzzle, small round dark-backed ears, thick legs on wide
 * paws, ghost stripes, and a long tail with a dark tuft. Animation in {@link PredatorModel}: the deep crouch and the
 * pounce matter most for it. Geometry and skin: scripts/gen_savanna_models.py (SavannaGeometry.caveLion,
 * textures/entity/wildlife/cave_lion.png).
 */
public class CaveLionModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "cave_lion"), "main");

    public CaveLionModel(ModelPart root) {
        super(root, 0.6F, 5.0F, 9.0F, 0.45F);
    }

    public static LayerDefinition createBodyLayer() {
        return com.mechanical.client.wildlife.savanna.SavannaGeometry.caveLion();
    }
}
