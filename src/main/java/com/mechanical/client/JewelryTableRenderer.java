package com.mechanical.client;

import com.mechanical.Jewelry;
import com.mechanical.JewelryTableBlockEntity;
import com.mechanical.Mechanical;
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

/** What lies on a Steel Casting Table or Jeweler's Bench: the two items flat on the work top, side by side. */
public class JewelryTableRenderer implements BlockEntityRenderer<JewelryTableBlockEntity, JewelryTableRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = {new ItemStackRenderState(), new ItemStackRenderState()};
    }

    private final ItemModelResolver resolver;

    public JewelryTableRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(JewelryTableBlockEntity table, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, camera, breakProgress);
        int seed = (int) table.getBlockPos().asLong();
        for (int i = 0; i < 2; i++) resolver.updateForTopItem(state.items[i], table.items().get(i), ItemDisplayContext.FIXED, table.getLevel(), null, seed + i);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < 2; i++) {
            if (state.items[i].isEmpty()) continue;
            pose.pushPose();
            pose.translate(i == 0 ? 0.33 : 0.67, 14.1 / 16, 0.5);
            pose.rotateDegrees(Axis.XP, 90); // lying flat on the top
            pose.scale(0.42F, 0.42F, 0.42F);
            state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Jewelry.TABLE_ENTITY.get(), JewelryTableRenderer::new);
        }
    }
}
