package com.mechanical.client.wildlife.jungle;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.client.wildlife.meadow.SnakeModel;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.jungle.Crocodile;
import com.mechanical.wildlife.jungle.FiddlerCrab;
import com.mechanical.wildlife.jungle.Python;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/**
 * The wildlife renderer for the jungle fauna, with a whole-model {@code scale} (the tiger is a big leopard, the python a
 * big snake, the monitor a big lizard, the proboscis monkey a small ape) and their render flags: the python's coil, the
 * crocodile lurking and rolling (the whole body spins round its length), the crab down its hole.
 */
public class JungleRenderer<T extends WildAnimal> extends WildAnimalRenderer<T> {
    public static final ContextKey<Boolean> ROLLING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "croc_rolling"));
    public static final ContextKey<Boolean> LURKING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "croc_lurking"));
    public static final ContextKey<Boolean> HIDING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "crab_hiding"));
    private final float scale;

    public JungleRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> model, float scale, float babyScale, float shadow) {
        super(context, model, model, babyScale, shadow * scale);
        this.scale = scale;
    }

    @Override
    public void extractRenderState(T entity, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity instanceof Python p && p.isCoiled()) state.setRenderData(SnakeModel.COILED, true);
        if (entity instanceof Crocodile c) {
            if (c.isRolling()) state.setRenderData(ROLLING, true);
            if (c.isLurking()) state.setRenderData(LURKING, true);
        }
        if (entity instanceof FiddlerCrab crab && crab.isHiding()) state.setRenderData(HIDING, true);
    }

    @Override
    protected void setupRotations(WildAnimalRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (Boolean.TRUE.equals(state.getRenderData(ROLLING))) {
            // the death roll: spinning round its long axis, about the middle of the body
            poseStack.translate(0.0F, 0.35F, 0.0F);
            poseStack.rotateDegrees(Axis.ZP, state.ageInTicks * 28.0F);
            poseStack.translate(0.0F, -0.35F, 0.0F);
        }
    }

    @Override
    protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (scale != 1.0F) poseStack.scale(scale, scale, scale);
    }
}
