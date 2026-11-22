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

/**
 * Food on the Fire Pit's two spits: skewered upright on the sticks, turned a little each so a full spit looks hand-loaded.
 * On a big pit the same spits, spread over the 3x3, and a whole roast (three big cuts of its meat) turning on a centre spit.
 */
public class FirePitRenderer implements BlockEntityRenderer<FirePitBlockEntity, FirePitRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[FirePit.SLOTS];
        final ItemStackRenderState roast = new ItemStackRenderState();
        boolean big;
        float time;

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
        state.big = pit.big();
        resolver.updateForTopItem(state.roast, pit.roast(), ItemDisplayContext.FIXED, pit.getLevel(), null, seed + 99);
        state.time = (pit.getLevel() == null ? 0 : pit.getLevel().getGameTime() % 3600) + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            ItemStackRenderState item = state.items[slot];
            if (item.isEmpty()) continue;
            pose.pushPose();
            float x = FirePitBlockEntity.slotX(slot), z = FirePitBlockEntity.slotZ(slot);
            if (state.big) pose.translate(FirePitBlockEntity.bigX(x) / 16, FirePitBlockEntity.BIG_SPIT_Y / 16, FirePitBlockEntity.bigX(z) / 16);
            else pose.translate(x / 16, FirePitBlockEntity.SPIT_Y / 16, z / 16);
            pose.rotateDegrees(Axis.YP, (slot * 37 % 5 - 2) * 6); // -12..12 degrees round the stick
            pose.scale(0.4F, 0.4F, 0.4F);
            item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (!state.roast.isEmpty()) {
            // the carcass roasts on a spit along x through the middle, a little above the side spits, slowly turned
            for (int i = -1; i <= 1; i++) {
                pose.pushPose();
                pose.translate((24 + i * 9) / 16F, (FirePitBlockEntity.BIG_SPIT_Y + 4) / 16, 24 / 16F);
                pose.rotateDegrees(Axis.XP, state.time * 0.8F + i * 40);
                pose.rotateDegrees(Axis.YP, 90);
                pose.scale(1.1F, 1.1F, 1.1F);
                state.roast.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
    }

    /** The spits reach over all four blocks of the pit, so it must not be culled by the corner block alone. */
    @Override
    public AABB getRenderBoundingBox(FirePitBlockEntity pit) {
        return new AABB(pit.getBlockPos()).expandTowards(pit.big() ? 2 : 1, pit.big() ? 2 : 1, pit.big() ? 2 : 1);
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(FirePit.FIRE_PIT_ENTITY.get(), FirePitRenderer::new);
        }
    }
}
