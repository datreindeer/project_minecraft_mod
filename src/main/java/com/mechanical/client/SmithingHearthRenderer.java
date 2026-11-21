package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.Smithing;
import com.mechanical.SmithingHearthBlockEntity;
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

/** The pieces in a Smithing Hearth's coals: four spots, 2x2, lying flat on the top. */
public class SmithingHearthRenderer implements BlockEntityRenderer<SmithingHearthBlockEntity, SmithingHearthRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[SmithingHearthBlockEntity.SLOTS];

        State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }

    private final ItemModelResolver resolver;

    public SmithingHearthRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SmithingHearthBlockEntity hearth, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(hearth, state, partialTicks, camera, breakProgress);
        for (int i = 0; i < SmithingHearthBlockEntity.SLOTS; i++) {
            resolver.updateForTopItem(state.items[i], hearth.item(i), ItemDisplayContext.FIXED, hearth.getLevel(), null, (int) hearth.getBlockPos().asLong() + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < SmithingHearthBlockEntity.SLOTS; i++) {
            if (state.items[i].isEmpty()) continue;
            pose.pushPose();
            pose.translate(0.28F + (i % 2) * 0.44F, 1.01F, 0.28F + (i / 2) * 0.44F);
            pose.rotateDegrees(Axis.YP, i * 37.0F);
            pose.rotateDegrees(Axis.XP, 90.0F);
            pose.scale(0.38F, 0.38F, 0.38F);
            state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Smithing.SMITHING_HEARTH_ENTITY.get(), SmithingHearthRenderer::new);
        }

        private Registration() {}
    }
}
