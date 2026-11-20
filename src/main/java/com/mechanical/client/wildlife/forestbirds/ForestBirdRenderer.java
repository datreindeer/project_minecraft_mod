package com.mechanical.client.wildlife.forestbirds;

import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.forestbirds.Capercaillie;
import com.mechanical.wildlife.forestbirds.GoldenEagle;
import com.mechanical.wildlife.forestbirds.SoaringBird;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The forest birds: the flock-bird renderer (wing beat, glide, climb angle) plus the lek display, hen size, soaring and carrying. */
public class ForestBirdRenderer extends WildAnimalRenderer<FlockBird> {
    /** How much smaller the capercaillie hen is than the cock. */
    static final float HEN_SCALE = 0.8F;

    public ForestBirdRenderer(EntityRendererProvider.Context context, ForestBirdModel model, float shadow) {
        super(context, model, model, 1.0F, shadow);
    }

    @Override
    public ForestBirdRenderState createRenderState() {
        return new ForestBirdRenderState();
    }

    @Override
    public void extractRenderState(FlockBird bird, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(bird, state, partialTicks);
        ForestBirdRenderState s = (ForestBirdRenderState) state;
        s.flying = bird.isFlying();
        s.flap = bird.flapPhase(partialTicks);
        s.glide = bird.glideAmount(partialTicks);
        s.swimming = bird.isInWater() && !s.flying;
        double dx = bird.getX() - bird.xo, dy = bird.getY() - bird.yo, dz = bird.getZ() - bird.zo;
        s.pitch = s.flying ? (float) Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz) + 0.05) : 0.0F;
        // the big soarers ride the air on still wings, beating only to climb
        if (bird instanceof SoaringBird && s.flying) s.glide = Math.max(s.glide, dy > 0.08 ? 0.2F : 0.85F);
        s.hen = bird instanceof Capercaillie c && c.isHen();
        s.display = bird instanceof Capercaillie c && c.isDisplaying() ? 1.0F : 0.0F;
        s.carrying = bird instanceof GoldenEagle e && e.isCarrying();
    }

    @Override
    protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (((ForestBirdRenderState) state).hen) poseStack.scale(HEN_SCALE, HEN_SCALE, HEN_SCALE);
    }
}
