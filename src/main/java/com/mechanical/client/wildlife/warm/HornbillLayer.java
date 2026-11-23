package com.mechanical.client.wildlife.warm;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Great hornbill, drawn by {@link com.mechanical.client.wildlife.BirdModel} (same part tree as the other flocking birds):
 * black body with a white belly and thighs, shaggy cream neck and collar, black face with a red eye, the long down-curved
 * yellow bill in three steps under the big upturned casque (black front and back edges), black wings with the cream bar
 * and white-tipped flight feathers, and a long white tail with a black band. Geometry ({@link WarmBirdGeometry}, 64x64)
 * and the skins (wildlife/hornbill.png, skinned/wildlife/hornbill.png) come from scripts/gen_owarm_birds_models.py.
 */
public final class HornbillLayer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "hornbill"), "main");

    private HornbillLayer() {
    }

    public static LayerDefinition create() {
        return WarmBirdGeometry.hornbill();
    }
}
