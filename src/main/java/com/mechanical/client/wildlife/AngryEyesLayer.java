package com.mechanical.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** A full-bright glow texture over the model (like spider eyes), shown while {@code when} holds: angry red eyes, glowing antler runes. */
public class AngryEyesLayer extends EyesLayer<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
    private final RenderType eyes;
    private final java.util.function.Predicate<WildAnimalRenderState> when;

    public AngryEyesLayer(RenderLayerParent<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> parent, Identifier texture) {
        this(parent, texture, state -> state.charging && !state.dozing);
    }

    public AngryEyesLayer(RenderLayerParent<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> parent, Identifier texture,
            java.util.function.Predicate<WildAnimalRenderState> when) {
        super(parent);
        this.eyes = RenderTypes.eyes(texture);
        this.when = when;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, WildAnimalRenderState state, float yRot, float xRot) {
        if (when.test(state)) super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
    }

    @Override
    public RenderType renderType() {
        return eyes;
    }
}
