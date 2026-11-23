package com.mechanical.client.wildlife.coldhunters;

import com.mechanical.client.wildlife.PredatorModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Ermine (stoat): long, slim body low on very short legs, a small flat head with round ears, and a thin tail with its
 * black tip. Animation in {@link PredatorModel}: its gallop is the stoat's bounding run. Built at twice its size and drawn at
 * half (ColdhuntersClient); geometry and textures (ermine.png brown, ermine_winter.png white) from one table in
 * scripts/gen_hunt_models.py (HuntGeometry).
 */
public class ErmineModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "ermine"), "main");

    public ErmineModel(ModelPart root) {
        super(root, 1.2F, 2.0F, 4.0F, 1.3F); // built at twice its size (drawn at half): drops in model px doubled
    }

    public static LayerDefinition createBodyLayer() {
        return HuntGeometry.ermine();
    }
}
