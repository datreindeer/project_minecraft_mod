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

/**
 * What lies on a Stone Anvil or a Bronze Anvil: flat on the top face, turned a little per block so anvils side by side
 * differ, a pile drawn piece by piece (stacked ingots, the grate's bronze ingots). Every blow shows: the piece squashes
 * and springs back ({@link #SQUASH_TICKS}), and the further the work has come the flatter and wider it lies.
 */
public class AnvilRenderer<T extends StoneAnvilBlockEntity> implements BlockEntityRenderer<T, AnvilRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float turn, top, progress, squash;
        int pile;
    }

    /** Ticks a blow's squash takes to spring back. */
    private static final float SQUASH_TICKS = 6.0F;
    private final ItemModelResolver items;

    public AnvilRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T anvil, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(anvil, state, partialTicks, camera, breakProgress);
        items.updateForTopItem(state.item, anvil.item(), ItemDisplayContext.FIXED, anvil.getLevel(), null, (int) anvil.getBlockPos().asLong());
        state.turn = (anvil.getBlockPos().hashCode() & 7) * 45.0F;
        state.top = anvil.top();
        state.pile = Math.min(anvil.pile(), 4);
        state.progress = Math.clamp(anvil.progress(), 0.0F, 1.0F);
        float since = anvil.getLevel() == null ? SQUASH_TICKS : (anvil.getLevel().getGameTime() - anvil.struckAt() + partialTicks) / SQUASH_TICKS;
        state.squash = since >= 0 && since < 1 ? (float) Math.sin(Math.PI * since) : 0.0F;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        // worked: wider and thinner; struck: squashed flat for a moment, pressed down
        float spread = 0.5F * (1 + 0.25F * state.progress) * (1 + 0.15F * state.squash);
        float thick = 0.5F * (1 - 0.4F * state.progress) * (1 - 0.6F * state.squash);
        for (int i = 0; i < Math.max(1, state.pile); i++) {
            pose.pushPose();
            pose.translate(0.5F, state.top + 0.02F + i * 0.04F - 0.015F * state.squash, 0.5F);
            pose.rotateDegrees(Axis.YP, state.turn + i * 14.0F); // a pile lies a little askew
            pose.rotateDegrees(Axis.XP, 90.0F);
            pose.scale(spread, spread, thick);
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BronzeOres.STONE_ANVIL_ENTITY.get(), AnvilRenderer::new);
            event.registerBlockEntityRenderer(com.mechanical.Ironworking.BRONZE_ANVIL_ENTITY.get(), AnvilRenderer::new);
        }
    }
}
