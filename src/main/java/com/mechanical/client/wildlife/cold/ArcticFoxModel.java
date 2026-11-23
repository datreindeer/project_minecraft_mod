package com.mechanical.client.wildlife.cold;

import com.mechanical.client.wildlife.PredatorModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;

/**
 * Arctic fox: a compact, round little fox built against the cold - short legs, a short muzzle under a broad head with
 * fluffy cheeks and small rounded ears, a thick ruff round the neck, a deep chest, and a long, very bushy tail in three
 * segments. Animation in {@link PredatorModel} (trot, nose to the ground, curled asleep with the tail round, limp carcass,
 * hung). Geometry: ColdGeometry.arcticFox; skins arctic_fox.png (winter) and arctic_fox_summer.png (64x64), both from
 * scripts/gen_cold_models.py.
 */
public class ArcticFoxModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "arctic_fox"), "main");

    public ArcticFoxModel(ModelPart root) {
        super(root, 0.9F, 2.0F, 4.0F, 1.15F);
    }
}
