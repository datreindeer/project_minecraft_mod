package com.mechanical.client.wildlife.coldhunters;

import com.mechanical.client.wildlife.PredatorModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Pine marten: a long-bodied tree weasel, bigger and leggier than the ermine, with a pointed face, broad rounded
 * cream-edged ears, a creamy-orange throat bib and a long bushy tail. Animation in {@link PredatorModel}; clinging to a
 * trunk the renderer stands the whole model upright (ColdhuntersClient). Built at
 * twice its size and drawn at half (ColdhuntersClient); geometry and texture from one table in
 * scripts/gen_hunt_models.py (HuntGeometry).
 */
public class PineMartenModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "pine_marten"), "main");

    public PineMartenModel(ModelPart root) {
        super(root, 1.1F, 3.0F, 5.0F, 1.25F); // built at twice its size (drawn at half): drops in model px doubled
    }

    public static LayerDefinition createBodyLayer() {
        return HuntGeometry.pineMarten();
    }
}
