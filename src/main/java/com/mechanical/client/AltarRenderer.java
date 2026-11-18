package com.mechanical.client;

import com.mechanical.AncientOak;
import com.mechanical.Mechanical;
import com.mechanical.OfferingAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The item on the Offering Altar: lies on the top, slowly turning and bobbing. During the ritual the offering stands up,
 * rises and spins faster and faster, then shrinks away as it dissolves.
 */
public class AltarRenderer implements BlockEntityRenderer<OfferingAltarBlockEntity, AltarRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time, ritual;
    }

    private final ItemModelResolver items;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(OfferingAltarBlockEntity altar, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(altar, state, partialTicks, camera, breakProgress);
        items.updateForTopItem(state.item, altar.item(), ItemDisplayContext.FIXED, altar.getLevel(), null, (int) altar.getBlockPos().asLong());
        state.time = (altar.getLevel() != null ? altar.getLevel().getGameTime() : 0) + partialTicks;
        state.ritual = altar.ritual() > 0 ? Math.min(altar.ritual() + partialTicks, OfferingAltarBlockEntity.RITUAL_TICKS) : 0;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        float t = state.ritual;
        if (t <= 0) {
            // lying on the stone, slowly turning, with a faint bob
            pose.translate(0.5F, 0.78F + (float) Math.sin(state.time * 0.05F) * 0.015F, 0.5F);
            pose.rotateDegrees(Axis.YP, state.time * 0.6F);
            pose.rotateDegrees(Axis.XP, 90.0F);
            pose.scale(0.5F, 0.5F, 0.5F);
        } else {
            float stand = Math.min(1, t / 30.0F);
            float shrink = 1 - Math.max(0, (t - (OfferingAltarBlockEntity.RITUAL_TICKS - 25)) / 25.0F);
            pose.translate(0.5F, 0.78F + OfferingAltarBlockEntity.floatHeight(t) + (float) Math.sin(t * 0.2F) * 0.04F, 0.5F);
            // spin speeds up: angle grows with t squared
            pose.rotateDegrees(Axis.YP, state.time * 0.6F + t * t * 0.09F);
            pose.rotateDegrees(Axis.XP, 90.0F * (1 - stand));
            float s = (0.5F + 0.25F * stand) * shrink;
            pose.scale(s, s, s);
        }
        // full brightness during the ritual
        state.item.submit(pose, collector, t > 0 ? 15728880 : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(AncientOak.ALTAR_ENTITY.get(), AltarRenderer::new);
        }
    }
}
