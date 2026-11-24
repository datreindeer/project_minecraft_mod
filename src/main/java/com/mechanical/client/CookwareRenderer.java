package com.mechanical.client;

import com.mechanical.CookwareBlockEntity;
import com.mechanical.Kitchen;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * What lies on a Bronze Griddle or Iron Skillet (Kitchen): up to four items flat on the cooking surface (y 1 px,
 * scripts/herbs_cookware.py), each in its own quarter, turned a little. The barrel and press hide what is inside.
 */
public class CookwareRenderer implements BlockEntityRenderer<CookwareBlockEntity, CookwareRenderer.State> {
    private static final int SLOTS = 4;
    /** Item centres in pixels and their turn. */
    private static final float[][] SPOTS = {{5.2F, 5.2F, 15}, {10.8F, 5.4F, 100}, {5.4F, 10.8F, 200}, {10.6F, 10.6F, 290}};
    private static final float SURFACE = 1.15F;

    public static class State extends BlockEntityRenderState {
        boolean show;
        final ItemStackRenderState[] items = new ItemStackRenderState[SLOTS];

        State() {
            for (int i = 0; i < SLOTS; i++) items[i] = new ItemStackRenderState();
        }
    }

    private final ItemModelResolver resolver;

    public CookwareRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CookwareBlockEntity pan, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pan, state, partialTicks, camera, breakProgress);
        state.show = pan.kind().fire;
        int seed = (int) pan.getBlockPos().asLong();
        var items = pan.items();
        for (int i = 0; i < SLOTS; i++) {
            // a finished stack of several shows as several pieces
            ItemStack s = ItemStack.EMPTY;
            if (state.show && !items.isEmpty()) {
                if (items.size() > 1) s = i < items.size() ? items.get(i) : ItemStack.EMPTY;
                else if (i < Math.min(SLOTS, items.getFirst().getCount())) s = items.getFirst();
            }
            resolver.updateForTopItem(state.items[i], s, ItemDisplayContext.FIXED, pan.getLevel(), null, seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.show) return;
        for (int i = 0; i < SLOTS; i++) {
            ItemStackRenderState item = state.items[i];
            if (item.isEmpty()) continue;
            pose.pushPose();
            pose.translate(SPOTS[i][0] / 16, (SURFACE + 0.1F * i) / 16, SPOTS[i][1] / 16);
            pose.rotateDegrees(Axis.YP, SPOTS[i][2]);
            pose.rotateDegrees(Axis.XP, 90); // lying flat
            pose.scale(0.36F, 0.36F, 0.36F);
            item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Kitchen.COOKWARE_ENTITY.get(), CookwareRenderer::new);
        }
    }
}
