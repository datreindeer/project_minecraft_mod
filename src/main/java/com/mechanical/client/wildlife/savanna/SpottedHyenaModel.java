package com.mechanical.client.wildlife.savanna;

import com.mechanical.client.wildlife.PredatorModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Spotted hyena: deep, high forequarters with a dark bristly crest over the shoulders and neck, the back sloping down to a
 * low rump, a big head with heavy jaw muscles and a blunt dark muzzle, round ears, a short tail with a black tuft, and
 * spotted legs. Animation in {@link PredatorModel} (the cave hyena's cadence). Geometry and skin:
 * scripts/gen_savanna_models.py (SavannaGeometry.hyena, textures/entity/wildlife/spotted_hyena.png).
 */
public class SpottedHyenaModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "spotted_hyena"), "main");

    public SpottedHyenaModel(ModelPart root) {
        super(root, 0.7F, 3.0F, 7.0F, 0.55F);
    }

    public static LayerDefinition createBodyLayer() {
        return SavannaGeometry.hyena();
    }
}
