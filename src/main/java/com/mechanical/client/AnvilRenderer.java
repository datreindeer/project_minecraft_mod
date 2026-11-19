package com.mechanical.client;

import com.mechanical.BronzeOres;
import com.mechanical.Mechanical;
import com.mechanical.StoneAnvilBlockEntity;
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

/** The ore on a Stone Anvil, lying flat on the top face, turned a little per block so anvils side by side differ. */
public class AnvilRenderer implements BlockEntityRenderer<StoneAnvilBlockEntity, AnvilRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float turn;
    }

    private final ItemModelResolver items;

    public AnvilRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StoneAnvilBlockEntity anvil, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(anvil, state, partialTicks, camera, breakProgress);
        items.updateForTopItem(state.item, anvil.item(), ItemDisplayContext.FIXED, anvil.getLevel(), null, (int) anvil.getBlockPos().asLong());
        state.turn = (anvil.getBlockPos().hashCode() & 7) * 45.0F;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5F, StoneAnvilBlockEntity.TOP + 0.02F, 0.5F);
        pose.rotateDegrees(Axis.YP, state.turn);
        pose.rotateDegrees(Axis.XP, 90.0F);
        pose.scale(0.5F, 0.5F, 0.5F);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BronzeOres.STONE_ANVIL_ENTITY.get(), AnvilRenderer::new);
        }
    }
}
