package com.mechanical.client;

import com.mechanical.HighFurnace;
import com.mechanical.HighFurnaceBlockEntity;
import com.mechanical.Mechanical;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/**
 * The inside of a hot High Furnace, seen down its open top: the burning charge at the bottom (a fullbright churning bed,
 * dark red at 500°, orange, then yellow-white at 1500°) and the glow it throws up the inner walls, higher and brighter the
 * hotter it is. Phase 3 puts the melt's layers on top of it. Drawn with the beacon beam's render type like
 * {@link MoltenRenderer} (no light source: the walls outside stay lit by their own glow).
 */
public class HighFurnaceRenderer implements BlockEntityRenderer<HighFurnaceBlockEntity, HighFurnaceRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/molten_flow.png");
    private static final RenderType BED = RenderTypes.beaconBeam(TEXTURE, false), GLOW = RenderTypes.beaconBeam(TEXTURE, true);

    public static class State extends BlockEntityRenderState {
        int @Nullable [] box;
        float temp, time;
        /** The pool's layers from the bottom: colour and height in blocks. */
        final java.util.List<int[]> layers = new java.util.ArrayList<>();
        float poolTop;
        /** Items still melting, floating on the surface (at most {@link #FLOATING}). */
        final java.util.List<net.minecraft.client.renderer.item.ItemStackRenderState> floating = new java.util.ArrayList<>();
    }

    private static final int FLOATING = 16;
    private final net.minecraft.client.renderer.item.ItemModelResolver items;

    public HighFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HighFurnaceBlockEntity furnace, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(furnace, state, partialTicks, camera, breakProgress);
        state.box = furnace.clientBox();
        state.temp = furnace.temperature();
        state.time = WaterPowerRenderers.time(furnace.getLevel(), partialTicks);
        state.layers.clear();
        state.floating.clear();
        state.poolTop = 0;
        int[] b = state.box;
        com.mechanical.MeltPool pool = furnace.pool();
        if (b == null || pool.capacity() <= 0) return;
        float height = b[4] - b[1] + 1 - 0.4F, y = 0;
        for (var e : pool.layers()) {
            float h = Math.max(0.03F, e.getValue() * height / pool.capacity());
            state.layers.add(new int[] {e.getKey().color, Float.floatToIntBits(h)});
            y += h;
        }
        state.poolTop = y;
        int seed = (int) furnace.getBlockPos().asLong();
        for (net.minecraft.world.item.ItemStack it : furnace.slots()) {
            if (it.isEmpty() || state.floating.size() >= FLOATING) continue;
            var r = new net.minecraft.client.renderer.item.ItemStackRenderState();
            items.updateForTopItem(r, it, net.minecraft.world.item.ItemDisplayContext.GROUND, furnace.getLevel(), null, seed + state.floating.size());
            state.floating.add(r);
        }
    }

    /** Fire's colour by heat: dull red, cherry, orange, gold, a pale yellow at the top (kept saturated: white reads as grey). */
    private static final int[][] RAMP = {{450, 0x5A1206}, {800, 0xB02A08}, {1100, 0xFF5A12}, {1350, 0xFF9A22}, {1600, 0xFFD25A}, {1800, 0xFFF0A8}};

    static int colour(float t, int alpha) {
        int rgb = RAMP[RAMP.length - 1][1];
        for (int i = 1; i < RAMP.length; i++)
            if (t < RAMP[i][0]) {
                float f = Math.max(0, (t - RAMP[i - 1][0]) / (RAMP[i][0] - RAMP[i - 1][0]));
                rgb = net.minecraft.util.ARGB.srgbLerp(f, RAMP[i - 1][1], RAMP[i][1]) & 0xFFFFFF;
                break;
            }
        return alpha << 24 | rgb;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int[] b = state.box;
        if (b == null || state.temp < 450) return;
        // the inside, relative to the Front
        float x0 = b[0] - state.blockPos.getX(), y0 = b[1] - state.blockPos.getY(), z0 = b[2] - state.blockPos.getZ();
        float x1 = b[3] + 1 - state.blockPos.getX(), z1 = b[5] + 1 - state.blockPos.getZ();
        float heat = Math.min(1, (state.temp - 450) / 1150F);
        float bedY = y0 + 0.35F, scroll = state.time * 0.004F;
        int bed = colour(state.temp, 255);
        float surface = bedY + state.poolTop;
        collector.submitCustomGeometry(pose, BED, (p, buf) -> {
            // the burning bed under the melt, then the melt's layers up the inner walls and its surface in the top layer's colour
            quadUp(p, buf, bed, x0, z0, x1, z1, bedY, scroll, scroll * 0.6F);
            float ya = bedY;
            int topColour = bed;
            for (int[] l : state.layers) {
                float yb = ya + Float.intBitsToFloat(l[1]);
                int c = 0xFF000000 | l[0];
                float in = 0.02F;
                band(p, buf, c, x0 + in, z0 + in, x1 - in, z0 + in, ya, yb);
                band(p, buf, c, x1 - in, z0 + in, x1 - in, z1 - in, ya, yb);
                band(p, buf, c, x1 - in, z1 - in, x0 + in, z1 - in, ya, yb);
                band(p, buf, c, x0 + in, z1 - in, x0 + in, z0 + in, ya, yb);
                ya = yb;
                topColour = c;
            }
            if (!state.layers.isEmpty()) quadUp(p, buf, topColour, x0, z0, x1, z1, surface, scroll * 0.7F, -scroll * 0.4F);
        });
        // the unmelted items floating on it, turning slowly, bobbing
        for (int i = 0; i < state.floating.size(); i++) {
            float fx = x0 + 0.3F + ((i * 0.61F) % 1) * (x1 - x0 - 0.6F), fz = z0 + 0.3F + ((i * 0.37F + 0.2F) % 1) * (z1 - z0 - 0.6F);
            pose.pushPose();
            pose.translate(fx, surface + 0.05F + 0.03F * net.minecraft.util.Mth.sin(state.time * 0.05F + i), fz);
            pose.rotateDegrees(com.mojang.math.Axis.YP, i * 47 + state.time * 0.3F);
            pose.rotateDegrees(com.mojang.math.Axis.XP, 90);
            pose.scale(0.6F, 0.6F, 0.6F);
            state.floating.get(i).submit(pose, collector, net.minecraft.util.LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        float glowTop = Math.min(b[4] + 1 - state.blockPos.getY() - 0.05F, bedY + state.poolTop + 0.6F + heat * 2.6F); // never above the throat
        int low = colour(state.temp, Math.round(110 + 110 * heat)), high = colour(state.temp, 0);
        collector.submitCustomGeometry(pose, GLOW, (p, buf) -> {
            float in = 0.01F;
            wall(p, buf, low, high, x0 + in, z0 + in, x1 - in, z0 + in, bedY - 0.3F, glowTop, scroll);
            wall(p, buf, low, high, x1 - in, z0 + in, x1 - in, z1 - in, bedY - 0.3F, glowTop, scroll);
            wall(p, buf, low, high, x1 - in, z1 - in, x0 + in, z1 - in, bedY - 0.3F, glowTop, scroll);
            wall(p, buf, low, high, x0 + in, z1 - in, x0 + in, z0 + in, bedY - 0.3F, glowTop, scroll);
            quadUp(p, buf, colour(state.temp, Math.round(90 * heat)), x0, z0, x1, z1, bedY + 0.04F, -scroll * 1.7F, scroll * 1.3F);
        });
    }

    /** An opaque band of a layer on the inner wall from (ax, az) to (bx, bz), from ya up to yb. */
    private static void band(PoseStack.Pose pose, VertexConsumer buf, int c, float ax, float az, float bx, float bz, float ya, float yb) {
        float len = Math.abs(bx - ax) + Math.abs(bz - az);
        float[][] q = {{ax, ya, az, 0}, {ax, yb, az, 0}, {bx, yb, bz, len}, {bx, ya, bz, len}};
        for (int s = 0; s < 2; s++)
            for (int i = 0; i < 4; i++) {
                float[] v = q[s == 0 ? i : 3 - i];
                vertex(pose, buf, c, v[0], v[1], v[2], v[3] * 0.5F, v[1] * 0.5F);
            }
    }

    private static void quadUp(PoseStack.Pose pose, VertexConsumer buf, int c, float x0, float z0, float x1, float z1, float y, float du, float dv) {
        float[][] q = {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}};
        for (int s = 0; s < 2; s++)
            for (int i = 0; i < 4; i++) {
                float[] v = q[s == 0 ? i : 3 - i];
                vertex(pose, buf, c, v[0], y, v[1], v[0] * 0.5F + du, v[1] * 0.5F + dv);
            }
    }

    /** A vertical glow sheet along the inner wall from (ax, az) to (bx, bz), colour {@code low} at the bottom fading to {@code high}. */
    private static void wall(PoseStack.Pose pose, VertexConsumer buf, int low, int high, float ax, float az, float bx, float bz, float y0, float y1, float scroll) {
        float len = Math.abs(bx - ax) + Math.abs(bz - az);
        float[][] q = {{ax, y0, az, 0}, {ax, y1, az, 0}, {bx, y1, bz, len}, {bx, y0, bz, len}};
        for (int s = 0; s < 2; s++)
            for (int i = 0; i < 4; i++) {
                float[] v = q[s == 0 ? i : 3 - i];
                vertex(pose, buf, v[1] == y0 ? low : high, v[0], v[1], v[2], v[3] * 0.5F, v[1] * 0.25F - scroll * 2);
            }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buf, int colour, float x, float y, float z, float u, float v) {
        buf.addVertex(pose, x, y, z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    public AABB getRenderBoundingBox(HighFurnaceBlockEntity furnace) {
        int[] b = furnace.clientBox();
        return b == null ? new AABB(furnace.getBlockPos()) : new AABB(b[0] - 1, b[1] - 1, b[2] - 1, b[3] + 2, b[4] + 2, b[5] + 2).minmax(new AABB(furnace.getBlockPos()));
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.HIGH_FURNACE_ENTITY.get(), HighFurnaceRenderer::new);
        }
    }
}
