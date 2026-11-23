package com.mechanical.client.wildlife.temperate;

import com.mechanical.client.CarcassRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Eel: a long, round, snake-like body in four segments tapering to a fin-fringed tail, with a small pointed head. It
 * swims by throwing waves down its length (each segment a little behind the one before), lazily when it idles and hard
 * when it flees; out of the water it writhes. Geometry and skin: scripts/gen_temperate_models.py.
 */
public class EelModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "eel"), "main");
    private final ModelPart[] segments = new ModelPart[4];

    public EelModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        segments[0] = body;
        for (int i = 1; i < 4; i++) segments[i] = segments[i - 1].getChild("segment" + i);
    }

    public static LayerDefinition createBodyLayer() {
        return TemperateGeometry.eel(); // scripts/gen_temperate_models.py
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return; // limp
        float t = state.ageInTicks;
        boolean wet = state.isInWater;
        float amp = wet ? 0.18F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.3F : 0.35F;
        float rate = wet ? 0.25F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.5F : 1.2F;
        for (int i = 1; i < 4; i++) segments[i].yRot = Mth.sin(t * rate - i * 0.9F) * amp;
        segments[0].yRot = Mth.sin(t * rate + 0.9F) * amp * 0.4F;
    }
}
