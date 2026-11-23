package com.mechanical.client.wildlife.coldhunters;

import com.mechanical.client.wildlife.PredatorModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Lynx: a tall, short-bodied cat on long legs with big furry paws, a bobbed black-tipped tail, a broad face framed by
 * hanging cheek ruffs, and black ear tufts. Animation in {@link PredatorModel} (stalking crouch, pounce, curled sleep,
 * limp carcass, hung by the hind legs). Geometry and texture (128x64: textures/entity/wildlife/lynx.png and
 * lynx_kitten.png) from one table in scripts/gen_hunt_models.py (HuntGeometry).
 */
public class LynxModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "lynx"), "main");

    public LynxModel(ModelPart root) {
        super(root, 0.7F, 4.0F, 8.5F, 1.1F);
    }

    public static LayerDefinition createBodyLayer() {
        return HuntGeometry.lynx();
    }
}
