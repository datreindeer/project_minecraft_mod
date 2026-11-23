package com.mechanical.client.wildlife.coldhunters;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.PredatorModel;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Wolverine: a small bear of a weasel - heavy, low and humped (the back rises to the rump), short thick legs on broad
 * clawed paws, a broad head with a short muzzle and small round ears, and a long bushy tail. Animation in
 * {@link PredatorModel}, plus the rolling lope of a mustelid (the body rocks). Geometry and texture (128x64,
 * textures/entity/wildlife/wolverine.png) from one table in scripts/gen_hunt_models.py (HuntGeometry).
 */
public class WolverineModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wolverine"), "main");

    public WolverineModel(ModelPart root) {
        super(root, 0.9F, 2.5F, 4.5F, 1.0F);
    }

    public static LayerDefinition createBodyLayer() {
        return HuntGeometry.wolverine();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (!state.dozing && !CarcassRenderer.isCarcass(state) && !CarcassRenderer.isHung(state))
            body.zRot += Mth.sin(state.walkAnimationPos * 0.9F) * 0.06F * Math.min(state.walkAnimationSpeed, 1.0F);
    }
}
