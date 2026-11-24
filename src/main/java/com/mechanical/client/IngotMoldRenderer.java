package com.mechanical.client;

import com.mechanical.HighFurnace;
import com.mechanical.IngotMoldBlock;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/**
 * The melt in an Iron Ingot Mold's four pockets: glowing in its colour when just poured, then cooling (white, yellow,
 * orange, dark red) to a dull metal skin as it sets ({@link IngotMoldBlock.Entity#cooled}).
 */
public class IngotMoldRenderer implements BlockEntityRenderer<IngotMoldBlock.Entity, IngotMoldRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/molten_flow.png");
    private static final RenderType TYPE = RenderTypes.beaconBeam(TEXTURE, false);

    public static class State extends BlockEntityRenderState {
        @Nullable Molten molten;
        float cooled;
        float[] level = new float[0];
        IngotMoldBlock.Kind kind = IngotMoldBlock.Kind.INGOT;
        /** The sides a trough joins (their gates are cut): the melt shows in the gate too, so it reads as one run. */
        final java.util.List<net.minecraft.core.Direction> gates = new java.util.ArrayList<>(4);
    }

    public IngotMoldRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(IngotMoldBlock.Entity mold, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mold, state, partialTicks, camera, breakProgress);
        state.molten = mold.molten() != null ? mold.molten() : mold.filling();
        state.cooled = mold.molten() == null || mold.getLevel() == null ? 0 : mold.cooled(mold.getLevel().getGameTime());
        state.kind = mold.kind();
        if (state.level.length != state.kind.pockets.length) state.level = new float[state.kind.pockets.length];
        for (int i = 0; i < state.level.length; i++) state.level[i] = mold.pocket(i);
        state.gates.clear();
        var bs = mold.getBlockState();
        for (var e : com.mechanical.ClayChannelBlock.SIDES.entrySet()) if (bs.hasProperty(e.getValue()) && bs.getValue(e.getValue())) state.gates.add(e.getKey());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.molten == null) return;
        // hot: the melt's glow; cooling: through orange and dark red to a grey skin tinted by the metal
        int hot = 0xFF000000 | state.molten.color, skin = ARGB.srgbLerp(0.35F, 0xFF4A4642, hot) | 0xFF000000;
        int c = state.cooled < 0.6F ? ARGB.srgbLerp(state.cooled / 0.6F, hot, 0xFFB02A08) | 0xFF000000 : ARGB.srgbLerp((state.cooled - 0.6F) / 0.4F, 0xFFB02A08, skin) | 0xFF000000;
        boolean glow = state.cooled < 1;
        collector.submitCustomGeometry(pose, MoltenRenderer.CRUST, (p, buf) -> {
            for (int i = 0; i < state.kind.pockets.length; i++) {
                if (state.level[i] <= 0) continue;
                float[] q = state.kind.pockets[i];
                float y = (state.kind.floor + (state.kind.top - state.kind.floor) * state.level[i]) / 16;
                quad(p, buf, c, q[0] / 16, q[2] / 16, q[1] / 16, q[3] / 16, y, glow ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords);
            }
            // the gates a trough runs in through: melt over the gate's floor at the pockets' level (running in: just above the floor)
            float lv = state.level.length == 0 ? 0 : state.level[0];
            for (net.minecraft.core.Direction d : state.gates) {
                float[] g = state.kind.gate(d);
                float y = Math.max(state.kind.floor + (state.kind.top - state.kind.floor) * lv, g[4] + 0.3F) / 16;
                quad(p, buf, c, g[0] / 16, g[2] / 16, g[1] / 16, g[3] / 16, y, glow ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords);
            }
        });
    }

    private static void quad(PoseStack.Pose p, VertexConsumer buf, int c, float x0, float z0, float x1, float z1, float y, int light) {
        float[][] v = {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}};
        for (int k = 0; k < 8; k++) {
            float[] a = v[k < 4 ? k : 7 - k];
            buf.addVertex(p, a[0], y, a[1]).setColor(c).setUv(a[0] * 2, a[1] * 2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.INGOT_MOLD_ENTITY.get(), IngotMoldRenderer::new);
        }
    }
}
