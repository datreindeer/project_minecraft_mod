package com.mechanical.client.wildlife.savanna;

import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.savanna.Ostrich;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The wildlife renderer with the ostrich's kick filled in ({@link KickState}). */
public class SavannaAnimalRenderer<T extends WildAnimal> extends WildAnimalRenderer<T> {
    /** The wildlife state plus the ostrich's kick, 0..1 with the leg thrown forward. */
    public static class KickState extends WildAnimalRenderState {
        public float kick;
    }

    public SavannaAnimalRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> adult,
            EntityModel<WildAnimalRenderState> baby, float babyScale, float shadow) {
        super(context, adult, baby, babyScale, shadow);
    }

    @Override
    public KickState createRenderState() {
        return new KickState();
    }

    @Override
    public void extractRenderState(T entity, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((KickState) state).kick = entity instanceof Ostrich o ? o.kickAmount() : 0.0F;
    }
}
