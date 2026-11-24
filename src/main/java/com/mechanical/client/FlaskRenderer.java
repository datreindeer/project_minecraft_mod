package com.mechanical.client;

import com.mechanical.HighFurnace;
import com.mechanical.Mechanical;
import com.mechanical.Molten;
import com.mechanical.MoldingFlaskBlock;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/** The melt in a Molding Flask's pouring cup: rising while the stream fills it, glowing, then crusting dark as it sets. */
public class FlaskRenderer implements BlockEntityRenderer<MoldingFlaskBlock.Entity, FlaskRenderer.State> {
    public static class State extends BlockEntityRenderState {
        @Nullable Molten molten;
        float fill, cooled, time;
    }

    public FlaskRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MoldingFlaskBlock.Entity f, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(f, state, partialTicks, camera, breakProgress);
        long now = f.getLevel() == null ? 0 : f.getLevel().getGameTime();
        var stage = f.getBlockState().hasProperty(MoldingFlaskBlock.STAGE) ? f.getBlockState().getValue(MoldingFlaskBlock.STAGE) : MoldingFlaskBlock.Stage.EMPTY;
        state.molten = stage == MoldingFlaskBlock.Stage.MOLTEN || stage == MoldingFlaskBlock.Stage.SET || f.filled() > 0 ? f.molten() : null;
        state.fill = stage == MoldingFlaskBlock.Stage.READY && f.shape() != null ? Math.min(1, f.filled() / (float) f.shape().mb) : 1;
        state.cooled = stage == MoldingFlaskBlock.Stage.SET ? 1 : stage == MoldingFlaskBlock.Stage.MOLTEN ? f.cooled(now) : 0;
        state.time = (now % 2000 + partialTicks) * 0.004F;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.molten == null) return;
        int c = state.cooled < 0.05F ? 0xFF000000 | state.molten.color : TroughRenderer.cooling(state.molten, state.cooled);
        float y = (12.3F + 0.7F * state.fill) / 16, lo = 5.6F / 16, hi = 10.4F / 16, t = state.time; // rises to just under the cup's 13.2 px rim
        int light = state.cooled >= 1 ? state.lightCoords : LightCoordsUtil.FULL_BRIGHT;
        collector.submitCustomGeometry(pose, MoltenRenderer.CRUST, (p, buf) -> {
            float[][] v = {{lo, lo}, {lo, hi}, {hi, hi}, {hi, lo}};
            for (int k = 0; k < 8; k++) {
                float[] a = v[k < 4 ? k : 7 - k];
                buf.addVertex(p, a[0], y, a[1]).setColor(c).setUv(a[0] + t, a[1] - t).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
            }
        });
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.MOLDING_FLASK_ENTITY.get(), FlaskRenderer::new);
        }
    }
}
