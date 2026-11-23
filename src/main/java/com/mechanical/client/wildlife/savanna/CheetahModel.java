package com.mechanical.client.wildlife.savanna;

import com.mechanical.client.wildlife.PredatorModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/**
 * Cheetah: built for speed - deep narrow chest, a wasp waist, very long thin legs (back at 13.5 px, higher than the
 * leopard's though the body is slighter), a small round head with small ears, and a long tail. Animation in
 * {@link PredatorModel} (its bound is the sprint; the crouch is the stalk). Texture 64x64:
 * textures/entity/wildlife/cheetah.png, painted by scripts/gen_fauna_savanna_textures.py whose CHEETAH table mirrors the
 * cubes below (keep them in sync).
 */
public class CheetahModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "cheetah"), "main");

    public CheetahModel(ModelPart root) {
        super(root, 0.62F, 3.5F, 8.0F, 0.5F);
    }

    public static LayerDefinition createBodyLayer() {
        return SavannaGeometry.cheetah();
    }
}
