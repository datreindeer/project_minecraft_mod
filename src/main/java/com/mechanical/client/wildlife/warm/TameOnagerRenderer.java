package com.mechanical.client.wildlife.warm;

import com.mechanical.client.wildlife.StuckArrowLayer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.wildlife.warm.TameOnager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * The tamed onager keeps the wild onager's looks ({@link OnagerModel} / {@link OnagerFoalModel}, onager textures).
 * Vanilla's eating animation lowers the head as in grazing, rearing tips the body up on the hind legs, and with the
 * donkey's chest on it wears the Pack Baskets (parts of {@link OnagerModel}, {@link WarmRenderState#packed}).
 * The state type stays {@link WildAnimalRenderState} (a {@link WarmRenderState} is created) so {@link StuckArrowLayer} fits.
 */
public class TameOnagerRenderer extends MobRenderer<TameOnager, WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
    /** Hind leg pivot (blocks behind the middle) and how far up it rears. */
    private static final float HIND_Z = 7.0F / 16.0F, REAR_DEGREES = 40.0F;
    private final EntityModel<WildAnimalRenderState> adult, foal;

    public TameOnagerRenderer(EntityRendererProvider.Context context) {
        super(context, new OnagerModel(context.bakeLayer(OnagerModel.LAYER)), 0.7F);
        adult = model;
        foal = new OnagerFoalModel(context.bakeLayer(OnagerFoalModel.LAYER));
        addLayer(new StuckArrowLayer(this, context));
    }

    @Override
    public WildAnimalRenderState createRenderState() {
        return new WarmRenderState();
    }

    @Override
    public void extractRenderState(TameOnager onager, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(onager, state, partialTicks);
        state.texture = onager.texture();
        state.grazeAmount = onager.getEatAnim(partialTicks);
        state.stand = onager.getStandAnim(partialTicks);
        state.arrowCount = onager.getArrowCount();
        state.entityId = onager.getId();
        ((WarmRenderState) state).packed = onager.hasChest();
    }

    @Override
    public void submit(WildAnimalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        model = state.isBaby ? foal : adult;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected void setupRotations(WildAnimalRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (state.stand <= 0 || state.deathTime > 0) return;
        poseStack.translate(0, 0, HIND_Z);
        poseStack.rotateDegrees(Axis.XP, state.stand * REAR_DEGREES);
        poseStack.translate(0, 0, -HIND_Z);
    }

    @Override
    public Identifier getTextureLocation(WildAnimalRenderState state) {
        return state.texture;
    }
}
