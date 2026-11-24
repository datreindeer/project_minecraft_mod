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
        float fall, time;
        Direction facing = Direction.NORTH;
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
        state.fall = spout.fall();
        state.facing = spout.facing();
        state.time = WaterPowerRenderers.time(spout.getLevel(), partialTicks);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.stream == null) return;
        int c = 0xFF000000 | state.stream.color;
        float scroll = state.time * 0.08F;
        pose.pushPose();
        pose.translate(0.5F, 0, 0.5F);
        pose.rotateDegrees(com.mojang.math.Axis.YP, -state.facing.toYRot() + 180); // local -z: the way it pours
        collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
            // along the trough to the lip
            quad(p, buf, c, -2.5F / 16, 6.1F / 16, 8F / 16, 2.5F / 16, 6.1F / 16, -1.5F / 16, -scroll * 0.6F);
            // the falling column: 3 px thick, from the lip (y 6/16) down to the cast's surface
            float x0 = -1.6F / 16, x1 = 1.6F / 16, z0 = -3.1F / 16, z1 = 0.1F / 16, top = 6.2F / 16, bottom = -state.fall + 0.32F;
            column(p, buf, c, x0, z0, x1, z1, bottom, top, scroll);
            // the splash: a short wider skirt where it lands
            column(p, buf, c, x0 - 0.04F, z0 - 0.04F, x1 + 0.04F, z1 + 0.04F, bottom, bottom + 0.08F, scroll * 1.5F);
        });
        pose.popPose();
    }

    private static void column(PoseStack.Pose p, VertexConsumer buf, int c, float x0, float z0, float x1, float z1, float y0, float y1, float scroll) {
        float[][] corners = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        for (int i = 0; i < 4; i++) {
            float[] a = corners[i], b = corners[(i + 1) % 4];
            float[][] q = {{a[0], y0, a[1], 0}, {a[0], y1, a[1], 0}, {b[0], y1, b[1], 0.25F}, {b[0], y0, b[1], 0.25F}};
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
        return new AABB(spout.getBlockPos()).expandTowards(0, -1.5, 0);
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.POURING_SPOUT_ENTITY.get(), PouringSpoutRenderer::new);
        }
    }
}
