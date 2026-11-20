package com.mechanical.client.wildlife.giants;

import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.giants.WoollyMammoth;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * The giants' renderer: the young are the adult model scaled down (the models enlarge the head and hide horns and tusks
 * for them), the whole model can be drawn bigger ({@code modelScale}: the mammoth's is built at half size), and the
 * mammoth's sweep and trumpet go to the model through {@link GiantRenderState}. The mammoth herd leader is bigger by its
 * scale attribute, which the vanilla renderer applies on its own.
 */
public class GiantRenderer<T extends WildAnimal> extends WildAnimalRenderer<T> {
    private final float modelScale;

    public GiantRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> model, float babyScale, float shadow, float modelScale) {
        super(context, model, model, babyScale, shadow);
        this.modelScale = modelScale;
    }

    @Override
    public WildAnimalRenderState createRenderState() {
        return new GiantRenderState();
    }

    @Override
    public void extractRenderState(T entity, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof GiantRenderState giant && entity instanceof WoollyMammoth mammoth) {
            giant.swipe = mammoth.swipeAmount(partialTicks);
            giant.trumpet = mammoth.trumpetAmount(partialTicks);
        }
    }

    @Override
    protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (modelScale != 1.0F) poseStack.scale(modelScale, modelScale, modelScale);
    }
}
