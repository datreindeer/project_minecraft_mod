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
 * The stream out of an open Pouring Spout ({@link PouringSpoutBlockEntity}): a thick glowing column in the melt's colour
 * from the lip down to the cast, its surface rolling downward, a little wider where it leaves the lip and where it
 * splashes; the metal running along the trough to the lip. Fullbright with the beacon beam's render type.
 */
public class PouringSpoutRenderer implements BlockEntityRenderer<PouringSpoutBlockEntity, PouringSpoutRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/molten_flow.png");
    private static final RenderType TYPE = RenderTypes.beaconBeam(TEXTURE, false);

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
        Vec3 a = state.lip, b = state.target;
        collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
            pose.pushPose();
            pose.translate(0.5F, 0, 0.5F);
            pose.rotateDegrees(com.mojang.math.Axis.YP, -state.facing.toYRot() + 180); // the metal along the trough to the lip
            quad(pose.last(), buf, c, -2.5F / 16, 6.1F / 16, 8F / 16, 2.5F / 16, 6.1F / 16, -1.5F / 16, -scroll * 0.6F);
            pose.popPose();
            // the stream: a round column (8 faces, the ones turned to the light brighter, a white-hot core line), 2.6 px at the
            // lip thinning to 1.8 px, leaving the lip in a short arc and then falling to the metal it lands in
            Vec3 out = new Vec3(state.facing.getStepX(), 0, state.facing.getStepZ());
            Vec3 arc = a.add(out.scale(0.07)).add(0, -0.12, 0), bend = new Vec3(b.x, a.y - 0.3, b.z).lerp(arc, 0.35);
            int hot = net.minecraft.util.ARGB.srgbLerp(0.45F, c, 0xFFFFF6D8) | 0xFF000000;
            round(p, buf, c, hot, a, arc, 1.3F / 16, 1.2F / 16, scroll);
            round(p, buf, c, hot, arc, bend, 1.2F / 16, 1.0F / 16, scroll);
            round(p, buf, c, hot, bend, b, 1.0F / 16, 0.9F / 16, scroll);
            // where it lands: a small bright ring on the surface
            round(p, buf, c, hot, b.add(0, 0.03, 0), b, 1.6F / 16, 1.6F / 16, scroll * 1.5F);
        });
    }

    /** A round (8-sided) segment from {@code a} (half-width ra) to {@code b} (half-width rb), its faces shaded round. */
    private static void round(PoseStack.Pose p, VertexConsumer buf, int c, int hot, Vec3 a, Vec3 b, float ra, float rb, float scroll) {
        int n = 8;
        for (int i = 0; i < n; i++) {
            double t0 = i * Math.PI * 2 / n, t1 = (i + 1) * Math.PI * 2 / n;
            // faces turned toward the light (north-west, up) brighter, the back darker: it reads as a round, glowing core
            float lightness = (float) (0.5 + 0.5 * Math.cos((t0 + t1) / 2 - Math.PI * 0.75));
            int col = net.minecraft.util.ARGB.srgbLerp(lightness, net.minecraft.util.ARGB.srgbLerp(0.35F, 0xFF000000, c) | 0xFF000000, hot) | 0xFF000000;
            float[][] q = {{(float) (b.x + Math.cos(t0) * rb), (float) b.y, (float) (b.z + Math.sin(t0) * rb), 0},
                    {(float) (a.x + Math.cos(t0) * ra), (float) a.y, (float) (a.z + Math.sin(t0) * ra), 0},
                    {(float) (a.x + Math.cos(t1) * ra), (float) a.y, (float) (a.z + Math.sin(t1) * ra), 0.125F},
                    {(float) (b.x + Math.cos(t1) * rb), (float) b.y, (float) (b.z + Math.sin(t1) * rb), 0.125F}};
            for (int s = 0; s < 2; s++)
                for (int k = 0; k < 4; k++) {
                    float[] v = q[s == 0 ? k : 3 - k];
                    vertex(p, buf, col, v[0], v[1], v[2], v[3] + i * 0.125F, v[1] * 2 - scroll);
                }
        }
    }

    private static void quad(PoseStack.Pose p, VertexConsumer buf, int c, float x0, float y, float z0, float x1, float y1, float z1, float scroll) {
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
