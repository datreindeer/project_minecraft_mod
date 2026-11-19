package com.mechanical.client.wildlife.waterfowl;

import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.waterfowl.CommonCrane;
import com.mechanical.wildlife.waterfowl.GreyHeron;
import com.mechanical.wildlife.waterfowl.Mallard;
import com.mechanical.wildlife.waterfowl.WadingBird;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The waterfowl: like BirdRenderer (flight state: wing beat, glide, climb angle) plus dabbling, the heron's fishing and the
 * crane's dance. {@code flapScale} slows the wing beat of the big birds (the flock-bird clock is tuned for crows).
 */
public class WaterfowlRenderer extends WildAnimalRenderer<FlockBird> {
    private final float flapScale;

    public WaterfowlRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> model, float shadow, float flapScale) {
        super(context, model, model, 1.0F, shadow);
        this.flapScale = flapScale;
    }

    @Override
    public WaterfowlRenderState createRenderState() {
        return new WaterfowlRenderState();
    }

    @Override
    public void extractRenderState(FlockBird bird, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(bird, state, partialTicks);
        WaterfowlRenderState s = (WaterfowlRenderState) state;
        s.flying = bird.isFlying();
        s.flap = bird.flapPhase(partialTicks) * flapScale;
        s.glide = bird.glideAmount(partialTicks);
        boolean wader = bird instanceof WadingBird;
        s.wading = wader && bird.isInWater() && !s.flying;
        s.swimming = !wader && bird.isInWater() && !s.flying;
        double dx = bird.getX() - bird.xo, dy = bird.getY() - bird.yo, dz = bird.getZ() - bird.zo;
        s.pitch = s.flying ? (float) Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz) + 0.05) : 0.0F;
        s.dabble = bird instanceof Mallard duck ? duck.dabbleAmount(partialTicks) : 0.0F;
        s.strike = bird instanceof GreyHeron heron ? heron.strikeAmount(partialTicks) : 0.0F;
        s.hunting = bird instanceof GreyHeron heron && heron.isHunting();
        s.dance = bird instanceof CommonCrane crane ? crane.danceAmount(partialTicks) : 0.0F;
    }
}
