package com.mechanical.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Glowing red eyes (full bright, like a spider's) that only show while the animal is angry and charging. */
public class AngryEyesLayer extends EyesLayer<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
    private final RenderType eyes;

    public AngryEyesLayer(RenderLayerParent<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> parent, Identifier texture) {
        super(parent);
        this.eyes = RenderTypes.eyes(texture);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, WildAnimalRenderState state, float yRot, float xRot) {
        if (state.charging && !state.dozing) super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
    }

    @Override
    public RenderType renderType() {
        return eyes;
    }
}
