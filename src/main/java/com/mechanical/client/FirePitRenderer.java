package com.mechanical.client;

import com.mechanical.FirePit;
import com.mechanical.FirePitBlockEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/** Food on the Fire Pit's two spits: skewered upright on the sticks, turned a little each so a full spit looks hand-loaded. */
public class FirePitRenderer implements BlockEntityRenderer<FirePitBlockEntity, FirePitRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[FirePit.SLOTS];

        State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }

    private final ItemModelResolver resolver;

    public FirePitRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FirePitBlockEntity pit, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pit, state, partialTicks, camera, breakProgress);
        int seed = (int) pit.getBlockPos().asLong();
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            resolver.updateForTopItem(state.items[slot], pit.items().get(slot), ItemDisplayContext.FIXED, pit.getLevel(), null, seed + slot);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            ItemStackRenderState item = state.items[slot];
            if (item.isEmpty()) continue;
            pose.pushPose();
            pose.translate(FirePitBlockEntity.slotX(slot) / 16, FirePitBlockEntity.SPIT_Y / 16, FirePitBlockEntity.slotZ(slot) / 16);
            pose.rotateDegrees(Axis.YP, (slot * 37 % 5 - 2) * 6); // -12..12 degrees round the stick
            pose.scale(0.4F, 0.4F, 0.4F);
            item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    /** The spits reach over all four blocks of the pit, so it must not be culled by the corner block alone. */
    @Override
    public AABB getRenderBoundingBox(FirePitBlockEntity pit) {
        return new AABB(pit.getBlockPos()).expandTowards(1, 1, 1);
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(FirePit.FIRE_PIT_ENTITY.get(), FirePitRenderer::new);
        }
    }
}
