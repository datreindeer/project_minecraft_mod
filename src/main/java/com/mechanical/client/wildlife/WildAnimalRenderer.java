package com.mechanical.client.wildlife;

import com.mechanical.wildlife.Pheasant;
import com.mechanical.wildlife.WildAnimal;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * One renderer for all wildlife: adult and baby models are passed in, the texture comes from the entity.
 * Baby models are built at baby size; {@code babyScale} scales them further (or the adult model, when an animal has no
 * baby model of its own, like the hare).
 */
public class WildAnimalRenderer<T extends WildAnimal> extends MobRenderer<T, WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
    private final EntityModel<WildAnimalRenderState> adult, baby;
    private final float babyScale;

    public WildAnimalRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> adult,
            EntityModel<WildAnimalRenderState> baby, float babyScale, float shadow) {
        super(context, adult, shadow);
        this.adult = adult;
        this.baby = baby;
        this.babyScale = babyScale;
        addLayer(new StuckArrowLayer(this, context));
    }

    @Override
    public WildAnimalRenderState createRenderState() {
        return new WildAnimalRenderState();
    }

    @Override
    public void extractRenderState(T entity, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.texture = entity.texture();
        state.grazeAmount = entity.grazeAmount(partialTicks);
        state.showsHorns = entity.showsHorns();
        state.charging = entity.isCharging();
        state.dozing = entity.isDozing();
        state.arrowCount = entity.getArrowCount();
        state.entityId = entity.getId();
        if (entity instanceof com.mechanical.wildlife.Predator predator) {
            state.crouching = predator.isCrouching();
            state.leaping = predator.isLeaping();
            state.calling = predator.callAmount();
        }
        if (entity instanceof Pheasant pheasant) {
            state.flying = !pheasant.onGround();
            state.flap = pheasant.flapPhase(partialTicks);
        }
    }

    @Override
    public void submit(WildAnimalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        model = state.isBaby ? baby : adult;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.isBaby && babyScale != 1.0F) poseStack.scale(babyScale, babyScale, babyScale);
    }

    @Override
    public Identifier getTextureLocation(WildAnimalRenderState state) {
        return state.texture;
    }
}
