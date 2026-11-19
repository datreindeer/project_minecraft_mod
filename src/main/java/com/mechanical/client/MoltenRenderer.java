package com.mechanical.client;

import com.mechanical.ClayChannelBlockEntity;
import com.mechanical.FurnaceMelt;
import com.mechanical.FurnaceTapBlockEntity;
import com.mechanical.Mechanical;
import com.mechanical.Metal;
import com.mechanical.Smelting;
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
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * Glowing molten metal, drawn full-bright with the beacon beam's render type (unlit, the texture repeats so it can
 * scroll): the strip running along a Clay Channel (in from one side, out the other, the texture moving with the flow)
 * and the pool standing in a Smelting Furnace's shaft, with the unmelted charge on top of it.
 */
public final class MoltenRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/molten_flow.png");
    private static final RenderType TYPE = RenderTypes.beaconBeam(TEXTURE, false);
    /** Metal surface in a channel (between the trough floor and wall top), half its width, flow speed in blocks per tick. */
    private static final float SURFACE = 3.5F / 16, HALF = 2.5F / 16, SPEED = 0.04F;
    private static final int CHARGE = 0xFF6A5040;

    /**
     * A flat quad at height y between (x0, z0) and (x1, z1), seen from both sides (the pipeline culls back faces and the
     * corners may come in either order); u runs across, v from v0 at the first end to v1 at the other, along z when
     * {@code alongZ}, else along x.
     */
    private static void flat(PoseStack.Pose pose, VertexConsumer buf, int colour, float x0, float z0, float x1, float z1, float y,
            float u0, float u1, float v0, float v1, boolean alongZ) {
        float[][] c = {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}};
        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < 4; i++) {
                float[] p = c[side == 0 ? i : 3 - i];
                boolean first = alongZ ? p[1] == z0 : p[0] == x0, left = alongZ ? p[0] == x0 : p[1] == z0;
                buf.addVertex(pose, p[0], y, p[1]).setColor(colour).setUv(left ? u0 : u1, first ? v0 : v1).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
            }
        }
    }

    /**
     * One arm of the channel strip, from the block's edge on {@code side} to just past the middle. Along the metal's path
     * s runs from -0.5 (the edge it comes in at) through 0 (the middle) to 0.5 (the edge it leaves by); v = s - t, so the
     * texture moves with the flow as t grows. {@code in}: the arm the metal comes in by.
     */
    private static void arm(PoseStack.Pose pose, VertexConsumer buf, int colour, Direction side, boolean in, float t, float y) {
        float sign = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        float edge = 0.5F + 0.5F * sign, inner = 0.5F - HALF * sign, lo = 0.5F - HALF, hi = 0.5F + HALF;
        float vEdge = (in ? -0.5F : 0.5F) - t, vInner = (in ? HALF : -HALF) - t;
        if (side.getAxis() == Direction.Axis.Z) flat(pose, buf, colour, lo, edge, hi, inner, y, 0.25F, 0.75F, vEdge, vInner, true);
        else flat(pose, buf, colour, edge, lo, inner, hi, y, 0.25F, 0.75F, vEdge, vInner, false);
    }

    // ---- Clay Channel ----

    public static final class ChannelState extends BlockEntityRenderState {
        @Nullable Metal metal;
        Direction from = Direction.NORTH, to = Direction.SOUTH;
        float time;
    }

    public static final class Channel implements BlockEntityRenderer<ClayChannelBlockEntity, ChannelState> {
        public Channel(BlockEntityRendererProvider.Context context) {}

        @Override
        public ChannelState createRenderState() {
            return new ChannelState();
        }

        @Override
        public void extractRenderState(ClayChannelBlockEntity channel, ChannelState state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(channel, state, partialTicks, camera, breakProgress);
            long now = channel.getLevel() == null ? 0 : channel.getLevel().getGameTime();
            state.metal = channel.metal(now);
            state.from = channel.from();
            state.to = channel.to();
            state.time = ((now % 1000) + partialTicks) * SPEED;
        }

        @Override
        public void submit(ChannelState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.metal == null) return;
            int colour = FurnaceScreen.colour(state.metal);
            collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
                arm(p, buf, colour, state.from, true, state.time, SURFACE);
                arm(p, buf, colour, state.to, false, state.time, SURFACE + 0.002F); // a hair higher: no flicker where they overlap
            });
        }
    }

    // ---- the pool in the furnace shaft ----

    public static final class TapState extends BlockEntityRenderState {
        int pool, charge, colour;
        Direction back = Direction.SOUTH;
        float time;
    }

    public static final class Tap implements BlockEntityRenderer<FurnaceTapBlockEntity, TapState> {
        /** The shaft is 3 blocks deep; a full furnace fills this much of it. */
        private static final float DEPTH = 2.5F;

        public Tap(BlockEntityRendererProvider.Context context) {}

        @Override
        public TapState createRenderState() {
            return new TapState();
        }

        @Override
        public void extractRenderState(FurnaceTapBlockEntity tap, TapState state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(tap, state, partialTicks, camera, breakProgress);
            FurnaceMelt melt = tap.melt();
            state.pool = melt.poolTotal();
            state.charge = melt.chargeTotal();
            Metal metal = melt.alloy() != null ? melt.alloy() : Metal.TIN;
            state.colour = FurnaceScreen.colour(metal);
            state.back = tap.facing().getOpposite();
            long now = tap.getLevel() == null ? 0 : tap.getLevel().getGameTime();
            state.time = ((now % 4000) + partialTicks) * 0.005F;
        }

        @Override
        public void submit(TapState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.pool + state.charge == 0) return;
            float poolTop = 0.05F + DEPTH * state.pool / FurnaceMelt.CAPACITY;
            float chargeTop = poolTop + DEPTH * state.charge / FurnaceMelt.CAPACITY;
            float dx = state.back.getStepX(), dz = state.back.getStepZ();
            collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
                // the shaft's floor area, one block behind the tap; the melt slowly churns
                if (state.pool > 0) flat(p, buf, state.colour, dx, dz, dx + 1, dz + 1, poolTop, 0, 1, state.time, state.time + 1, true);
                if (state.charge > 0) flat(p, buf, CHARGE, dx, dz, dx + 1, dz + 1, chargeTop, 0, 1, -state.time, 1 - state.time, false);
            });
        }

        @Override
        public AABB getRenderBoundingBox(FurnaceTapBlockEntity tap) {
            Direction back = tap.facing().getOpposite();
            return new AABB(tap.getBlockPos()).expandTowards(back.getStepX(), 3, back.getStepZ());
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Smelting.CLAY_CHANNEL_ENTITY.get(), Channel::new);
            event.registerBlockEntityRenderer(Smelting.FURNACE_TAP_ENTITY.get(), Tap::new);
        }

        private Registration() {}
    }

    private MoltenRenderer() {}
}
