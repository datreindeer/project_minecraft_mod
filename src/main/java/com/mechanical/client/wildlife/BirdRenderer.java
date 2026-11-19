package com.mechanical.client.wildlife;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The flocking birds: the wildlife renderer with the flight state (wing beat, glide, climb angle) added. */
public class BirdRenderer extends WildAnimalRenderer<FlockBird> {
    public BirdRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, float flightNeck, float beat, float shadow) {
        this(context, new BirdModel(context.bakeLayer(layer), flightNeck, beat), shadow);
    }

    private BirdRenderer(EntityRendererProvider.Context context, BirdModel model, float shadow) {
        super(context, model, model, 1.0F, shadow);
    }

    @Override
    public BirdRenderState createRenderState() {
        return new BirdRenderState();
    }

    @Override
    public void extractRenderState(FlockBird bird, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(bird, state, partialTicks);
        BirdRenderState s = (BirdRenderState) state;
        s.flying = bird.isFlying();
        s.flap = bird.flapPhase(partialTicks);
        s.glide = bird.glideAmount(partialTicks);
        s.swimming = bird.isInWater() && !s.flying;
        double dx = bird.getX() - bird.xo, dy = bird.getY() - bird.yo, dz = bird.getZ() - bird.zo;
        s.pitch = s.flying ? (float) Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz) + 0.05) : 0.0F;
    }
}
