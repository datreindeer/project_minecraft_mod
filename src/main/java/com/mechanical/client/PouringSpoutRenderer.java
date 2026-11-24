package com.mechanical.client;

import com.mechanical.HighFurnace;
import com.mechanical.Mechanical;
import com.mechanical.Molten;
import com.mechanical.PouringSpoutBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/**
 * The stream out of an open Pouring Spout ({@link PouringSpoutBlockEntity}), drawn the way vanilla draws lava: the metal
 * running along the trough to the lip, over the lip and down in a plain 2 px glowing column to the cast, its pixels
 * rolling downward, a bright splash where it lands. Fullbright with the beacon beam's render type.
 */
public class PouringSpoutRenderer implements BlockEntityRenderer<PouringSpoutBlockEntity, PouringSpoutRenderer.State> {
    /** The stream: lava-like pixels in three tones, tinted by the melt (scripts/gen_high_furnace_textures.py). */
    static final RenderType STREAM = RenderTypes.beaconBeam(Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/molten_stream.png"), false);

    public static class State extends BlockEntityRenderState {
        @Nullable Molten stream;
        float time;
        Direction facing = Direction.NORTH;
        Vec3 lip = Vec3.ZERO, target = Vec3.ZERO;
    }

    public PouringSpoutRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PouringSpoutBlockEntity spout, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(spout, state, partialTicks, camera, breakProgress);
        state.stream = spout.stream();
        state.facing = spout.facing();
        state.lip = spout.lip();
        state.target = spout.target();
        state.time = WaterPowerRenderers.time(spout.getLevel(), partialTicks);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.stream == null) return;
        int c = 0xFF000000 | state.stream.color;
        float scroll = state.time * 0.08F;
        // in the spout's own frame (-z is where it pours): the metal along the trough to the lip, then the stream, a plain
        // 2 px column like a lava fall: over the lip's top and straight down its front face to what it fills, with a
        // brighter 4 px splash where it lands (the sinks' targets only give the height it falls to)
        int bright = net.minecraft.util.ARGB.srgbLerp(0.3F, c, 0xFFFFF2B0) | 0xFF000000;
        float y0 = (float) state.target.y, top = 6.1F / 16;
        pose.pushPose();
        pose.translate(0.5F, 0, 0.5F);
        pose.rotateDegrees(com.mojang.math.Axis.YP, -state.facing.toYRot() + 180);
        collector.submitCustomGeometry(pose, MoltenRenderer.CRUST, (p, buf) -> quad(p, buf, c, -2.5F / 16, top, 8F / 16, 2.5F / 16, top, 1F / 16, -scroll * 0.6F));
        collector.submitCustomGeometry(pose, STREAM, (p, buf) -> {
            box(p, buf, bright, -2F / 16, 5.4F / 16, 0.6F / 16, 2F / 16, top, 1.05F / 16, scroll); // over the lip, narrowing to the fall
            box(p, buf, bright, -1F / 16, y0, -1.4F / 16, 1F / 16, top, 0.6F / 16, scroll); // the fall
            quad(p, buf, bright, -2F / 16, y0 + 0.004F, 1.6F / 16, 2F / 16, y0 + 0.004F, -2.4F / 16, -scroll * 0.3F); // the splash
        });
        pose.popPose();
    }

    /** A box of melt from (x0, y0, z0) to (x1, y1, z1), every face seen from both sides, one texel per pixel, rolling down. */
    static void box(PoseStack.Pose p, VertexConsumer buf, int c, float x0, float y0, float z0, float x1, float y1, float z1, float scroll) {
        float[][][] faces = {
                {{x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}}, {{x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}},
                {{x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}}, {{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}},
                {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}}};
        for (int f = 0; f < faces.length; f++)
            for (int s = 0; s < 2; s++)
                for (int k = 0; k < 4; k++) {
                    float[] v = faces[f][s == 0 ? k : 3 - k];
                    float u = f < 2 ? v[0] : f < 4 ? v[2] : v[0];
                    vertex(p, buf, c, v[0], v[1], v[2], u + 0.5F, (f < 4 ? -v[1] : v[2]) - scroll);
                }
    }

    static void quad(PoseStack.Pose p, VertexConsumer buf, int c, float x0, float y, float z0, float x1, float y1, float z1, float scroll) {
        float[][] q = {{x0, y, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y, z0}};
        for (int s = 0; s < 2; s++)
            for (int k = 0; k < 4; k++) {
                float[] v = q[s == 0 ? k : 3 - k];
                vertex(p, buf, c, v[0], v[1], v[2], v[0] + 0.5F, v[2] + scroll);
            }
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer buf, int colour, float x, float y, float z, float u, float v) {
        buf.addVertex(p, x, y, z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 1, 0);
    }

    @Override
    public AABB getRenderBoundingBox(PouringSpoutBlockEntity spout) {
        return new AABB(spout.getBlockPos()).inflate(0.5).expandTowards(0, -1.5, 0);
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.POURING_SPOUT_ENTITY.get(), PouringSpoutRenderer::new);
        }
    }
}
