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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/**
 * The melt rising in a Foundry Basin while it is poured (glowing, crusted), cooling to a dull skin as it sets. With a
 * Loam Mold set in, the mold stands in the basin as a stepped dome of fired loam above the rim, and the melt shows only
 * in its pouring hole on top.
 */
public class FoundryBasinRenderer implements BlockEntityRenderer<FoundryBasinBlock.Entity, FoundryBasinRenderer.State> {
    private static final RenderType LOAM = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/loam_mold_side.png")),
            COPE = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/loam_mold_cope.png"));
    /** The dome's steps (px): from, to; the last one carries the pouring hole. */
    private static final float[][] DOME = {{3, 3, 3, 13, 13, 13}, {4, 13, 4, 12, 15, 12}, {6, 15, 6, 10, 16, 10}};

    public static class State extends BlockEntityRenderState {
        @Nullable Molten molten;
        float fill, cooled, time;
        boolean loam;
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
        state.loam = b.loam() != null;
        state.fill = b.fill();
        state.cooled = b.cooled(now);
        state.time = (now % 2000 + partialTicks) * 0.003F;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.loam) {
            int l = state.lightCoords;
            collector.submitCustomGeometry(pose, LOAM, (p, buf) -> {
                for (int i = 0; i < DOME.length; i++) box(p, buf, l, DOME[i], i < DOME.length - 1);
            });
            collector.submitCustomGeometry(pose, COPE, (p, buf) -> top(p, buf, l, DOME[DOME.length - 1]));
        }
        if (state.molten == null || state.fill <= 0) return;
        int c = state.cooled < 0.05F ? TroughRenderer.glow(state.molten, 0.7F) : TroughRenderer.cooling(state.molten, state.cooled);
        // in a loam mold the melt is seen only in the pouring hole (2 px), standing in it once the mold is full
        float y = state.loam ? 16.02F / 16 : (3 + 10.8F * state.fill) / 16, lo = (state.loam ? 7 : 2) / 16F, hi = (state.loam ? 9 : 14) / 16F, t = state.time;
        int light = state.cooled >= 1 ? state.lightCoords : LightCoordsUtil.FULL_BRIGHT;
        collector.submitCustomGeometry(pose, MoltenRenderer.CRUST, (p, buf) -> {
            float[][] v = {{lo, lo}, {lo, hi}, {hi, hi}, {hi, lo}};
            for (int k = 0; k < 8; k++) {
                float[] a = v[k < 4 ? k : 7 - k];
                buf.addVertex(p, a[0], y, a[1]).setColor(c).setUv(a[0] + t, a[1] - t * 0.6F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
            }
        });
    }

    /** A box of loam (px), its sides (and its top when {@code withTop}) lit as blocks are, one texel per pixel. */
    private static void box(PoseStack.Pose p, VertexConsumer buf, int light, float[] b, boolean withTop) {
        float x0 = b[0] / 16, y0 = b[1] / 16, z0 = b[2] / 16, x1 = b[3] / 16, y1 = b[4] / 16, z1 = b[5] / 16;
        face(p, buf, light, new float[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}}, 0, 0, -1, true);
        face(p, buf, light, new float[][] {{x1, y1, z1}, {x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}}, 0, 0, 1, true);
        face(p, buf, light, new float[][] {{x0, y1, z1}, {x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}}, -1, 0, 0, false);
        face(p, buf, light, new float[][] {{x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}}, 1, 0, 0, false);
        if (withTop) top(p, buf, light, b);
    }

    private static void top(PoseStack.Pose p, VertexConsumer buf, int light, float[] b) {
        float x0 = b[0] / 16, z0 = b[2] / 16, x1 = b[3] / 16, y1 = b[4] / 16, z1 = b[5] / 16;
        for (float[] v : new float[][] {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}})
            buf.addVertex(p, v[0], y1, v[1]).setColor(-1).setUv(v[0], v[1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
    }

    /** A side face (corners top-left, top-right, bottom-right, bottom-left seen from outside); u along it, v down. */
    private static void face(PoseStack.Pose p, VertexConsumer buf, int light, float[][] q, float nx, float ny, float nz, boolean alongX) {
        for (float[] v : q)
            buf.addVertex(p, v[0], v[1], v[2]).setColor(-1).setUv(alongX ? v[0] : v[2], 1 - v[1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.FOUNDRY_BASIN_ENTITY.get(), FoundryBasinRenderer::new);
        }
    }
}
