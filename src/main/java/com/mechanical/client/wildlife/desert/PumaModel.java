package com.mechanical.client.wildlife.desert;

import com.mechanical.client.wildlife.warm.LeopardModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Puma: the leopard's part tree and animation (LeopardModel: stalk, pounce, call, asleep draped on a ledge) on its own
 * build: a small round head with short rounded ears, a long neck, a lean body with heavy haunches standing higher than
 * the shoulders, big paws and a long thick tail with a dark tip. Geometry and the 128x64 skin (plain tawny, a pale
 * belly and muzzle, black moustache marks, dark ear backs) come from scripts/gen_desert_models.py (DesertGeometry).
 */
public class PumaModel extends LeopardModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "puma"), "main");

    public PumaModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return DesertGeometry.puma();
    }
}
