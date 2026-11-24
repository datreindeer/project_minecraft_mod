package com.mechanical.client;

import com.mechanical.FoundryBasinBlock;
import com.mechanical.HighFurnace;
import com.mechanical.Mechanical;
import com.mechanical.Molten;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/** The melt rising in a Foundry Basin while it is poured (glowing, crusted), cooling to a dull skin as it sets. */
public class FoundryBasinRenderer implements BlockEntityRenderer<FoundryBasinBlock.Entity, FoundryBasinRenderer.State> {
    public static class State extends BlockEntityRenderState {
        @Nullable Molten molten;
        float fill, cooled, time;
    }

    public FoundryBasinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FoundryBasinBlock.Entity b, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(b, state, partialTicks, camera, breakProgress);
        long now = b.getLevel() == null ? 0 : b.getLevel().getGameTime();
        state.molten = b.molten();
        state.fill = b.fill();
        state.cooled = b.cooled(now);
        state.time = (now % 2000 + partialTicks) * 0.003F;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.molten == null || state.fill <= 0) return;
        int c = state.cooled < 0.05F ? TroughRenderer.glow(state.molten, 0.7F) : TroughRenderer.cooling(state.molten, state.cooled);
        float y = (3 + 10.8F * state.fill) / 16, lo = 2 / 16F, hi = 14 / 16F, t = state.time;
        int light = state.cooled >= 1 ? state.lightCoords : LightCoordsUtil.FULL_BRIGHT;
        collector.submitCustomGeometry(pose, MoltenRenderer.CRUST, (p, buf) -> {
            float[][] v = {{lo, lo}, {lo, hi}, {hi, hi}, {hi, lo}};
            for (int k = 0; k < 8; k++) {
                float[] a = v[k < 4 ? k : 7 - k];
                buf.addVertex(p, a[0], y, a[1]).setColor(c).setUv(a[0] + t, a[1] - t * 0.6F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
            }
        });
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.FOUNDRY_BASIN_ENTITY.get(), FoundryBasinRenderer::new);
        }
    }
}
