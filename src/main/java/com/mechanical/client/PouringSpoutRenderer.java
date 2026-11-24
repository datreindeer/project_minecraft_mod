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
            // the stream: thicker at the lip, thinning as it falls, down to the metal it lands in, and a splash ring there
            float h0 = 1.8F / 16, h1 = 1.2F / 16;
            column(p, buf, c, (float) a.x, (float) a.z, (float) b.x, (float) b.z, h0, h1, (float) b.y, (float) a.y, scroll);
            column(p, buf, c, (float) b.x, (float) b.z, (float) b.x, (float) b.z, h1 + 0.04F, h1 + 0.04F, (float) b.y, (float) b.y + 0.05F, scroll * 1.5F);
        });
    }

    /** A column from (ax, az) at height ya (top, half-width ha) down to (bx, bz) at yb (bottom, half-width hb), seen from both sides. */
    private static void column(PoseStack.Pose p, VertexConsumer buf, int c, float ax, float az, float bx, float bz, float ha, float hb, float yb, float ya, float scroll) {
        float[][] dirs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int i = 0; i < 4; i++) {
            float[] d0 = dirs[i], d1 = dirs[(i + 1) % 4];
            float[][] q = {{bx + d0[0] * hb, yb, bz + d0[1] * hb, 0}, {ax + d0[0] * ha, ya, az + d0[1] * ha, 0},
                    {ax + d1[0] * ha, ya, az + d1[1] * ha, 0.25F}, {bx + d1[0] * hb, yb, bz + d1[1] * hb, 0.25F}};
            for (int s = 0; s < 2; s++)
                for (int k = 0; k < 4; k++) {
                    float[] v = q[s == 0 ? k : 3 - k];
                    vertex(p, buf, c, v[0], v[1], v[2], v[3] + i * 0.25F, v[1] - scroll);
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
