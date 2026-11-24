package com.mechanical.client;

import com.mechanical.ClayChannelBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * Glowing molten metal, drawn full-bright with the beacon beam's render type (unlit, the texture repeats so it can
 * scroll): the metal standing in a Clay Channel (rising with the amount, streaming while it moves, dark once set, a
 * film down a slope) and the pool in a Smelting Furnace's chamber, with the unmelted charge on top of it.
 */
public final class MoltenRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/molten_flow.png");
    private static final RenderType TYPE = RenderTypes.beaconBeam(TEXTURE, false);
    private static final int CHARGE = 0xFF6A5040;
    /** The trough's inside, across: 5..11 px. */
    private static final float LO = 5F / 16, HI = 11F / 16;

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
                vertex(pose, buf, colour, p[0], y, p[1], left ? u0 : u1, first ? v0 : v1);
            }
        }
    }

    /** A vertical quad from (x0, z0) to (x1, z1), from y0 up to y1, seen from both sides: the metal's edge down to the trough floor. */
    private static void side(PoseStack.Pose pose, VertexConsumer buf, int colour, float x0, float z0, float x1, float z1, float y0, float y1, float t) {
        float[][] c = {{x0, y0, z0}, {x0, y1, z0}, {x1, y1, z1}, {x1, y0, z1}};
        float len = Math.abs(x1 - x0) + Math.abs(z1 - z0);
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 4; i++) {
                float[] q = c[s == 0 ? i : 3 - i];
                boolean start = q[0] == x0 && q[2] == z0;
                vertex(pose, buf, colour, q[0], q[1], q[2], start ? 0.25F : 0.25F + len, (q[1] == y1 ? 0 : y1 - y0) - t);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buf, int colour, float x, float y, float z, float u, float v) {
        buf.addVertex(pose, x, y, z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }

    // ---- Clay Channel ----

    public static final class ChannelState extends BlockEntityRenderState {
        @Nullable Metal metal;
        float fill, time;
        boolean solid;
        int sides;
        @Nullable Direction up;
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
            state.metal = channel.metal();
            state.fill = channel.amount() / (float) ClayChannelBlockEntity.CAPACITY;
            state.solid = channel.solid(now);
            // running metal streams; standing metal only churns slowly
            state.time = ((now % 1000) + partialTicks) * (channel.running(now) ? 0.04F : 0.004F);
            BlockState block = channel.getBlockState();
            state.up = null;
            state.sides = 0;
            if (block.getBlock() instanceof ClayChannelBlock) {
                state.up = block.getValue(ClayChannelBlock.SLOPE).up;
                for (Direction d : Direction.Plane.HORIZONTAL) if (block.getValue(ClayChannelBlock.SIDES.get(d))) state.sides |= 1 << d.get2DDataValue();
            }
        }

        @Override
        public void submit(ChannelState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.metal == null) return;
            int c = FurnaceScreen.colour(state.metal);
            // set metal: dull and dark
            int colour = !state.solid ? c : 0xFF000000 | ((c >> 16 & 0xFF) * 2 / 5) << 16 | ((c >> 8 & 0xFF) * 2 / 5) << 8 | (c & 0xFF) * 2 / 5;
            float t = state.solid ? 0 : state.time;
            channel(collector, pose, colour, t, state.up, state.sides, state.fill);
        }

        /** The metal in a channel-shaped trough (Clay Channel, Refractory Trough): flat with arms to its open sides, or a film on a slope. */
        public static void channel(SubmitNodeCollector collector, PoseStack pose, int colour, float t, @Nullable Direction up, int sides, float fill) {
            collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
                if (up != null) {
                    slope(p, buf, colour, up, (0.6F + 1.6F * fill) / 16, t);
                    return;
                }
                float y = (ClayChannelBlock.FLOOR + 0.3F + (ClayChannelBlock.WALL - 0.8F - ClayChannelBlock.FLOOR) * fill) / 16;
                flat(p, buf, colour, LO, LO, HI, HI, y, 0.25F, 0.75F, LO - t, HI - t, true);
                float y2 = y + 0.001F; // a hair above the middle: no flicker where they meet
                float floor = ClayChannelBlock.FLOOR / 16F;
                // the metal's edges down to the floor, so it reads as a pool and not a floating sheet
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    boolean open = (sides & 1 << d.get2DDataValue()) != 0;
                    switch (d) {
                        case NORTH -> {
                            if (open) { side(p, buf, colour, LO, 0, LO, LO, floor, y2, t); side(p, buf, colour, HI, 0, HI, LO, floor, y2, t); side(p, buf, colour, LO, 0, HI, 0, floor, y2, t); }
                            else side(p, buf, colour, LO, LO, HI, LO, floor, y, t);
                        }
                        case SOUTH -> {
                            if (open) { side(p, buf, colour, LO, HI, LO, 1, floor, y2, t); side(p, buf, colour, HI, HI, HI, 1, floor, y2, t); side(p, buf, colour, LO, 1, HI, 1, floor, y2, t); }
                            else side(p, buf, colour, LO, HI, HI, HI, floor, y, t);
                        }
                        case WEST -> {
                            if (open) { side(p, buf, colour, 0, LO, LO, LO, floor, y2, t); side(p, buf, colour, 0, HI, LO, HI, floor, y2, t); side(p, buf, colour, 0, LO, 0, HI, floor, y2, t); }
                            else side(p, buf, colour, LO, LO, LO, HI, floor, y, t);
                        }
                        default -> {
                            if (open) { side(p, buf, colour, HI, LO, 1, LO, floor, y2, t); side(p, buf, colour, HI, HI, 1, HI, floor, y2, t); side(p, buf, colour, 1, LO, 1, HI, floor, y2, t); }
                            else side(p, buf, colour, HI, LO, HI, HI, floor, y, t);
                        }
                    }
                }
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if ((sides & 1 << d.get2DDataValue()) == 0) continue;
                    switch (d) {
                        case NORTH -> flat(p, buf, colour, LO, 0, HI, LO, y2, 0.25F, 0.75F, -t, LO - t, true);
                        case SOUTH -> flat(p, buf, colour, LO, HI, HI, 1, y2, 0.25F, 0.75F, HI - t, 1 - t, true);
                        case WEST -> flat(p, buf, colour, 0, LO, LO, HI, y2, 0.25F, 0.75F, -t, LO - t, false);
                        default -> flat(p, buf, colour, HI, LO, 1, HI, y2, 0.25F, 0.75F, HI - t, 1 - t, false);
                    }
                }
            });
        }

        /** A film on the slope's floor (the block's diagonal: low side at height 0, high side at 1), {@code lift} above it, streaming down. */
        private static void slope(PoseStack.Pose pose, VertexConsumer buf, int colour, Direction up, float lift, float t) {
            float[][] corners = {{0, LO}, {0, HI}, {1, HI}, {1, LO}}; // (along from the low side, across)
            for (int side = 0; side < 2; side++) {
                for (int i = 0; i < 4; i++) {
                    float[] c = corners[side == 0 ? i : 3 - i];
                    float a = c[0], s = c[1];
                    float x = switch (up) {
                        case NORTH, SOUTH -> s;
                        case EAST -> a;
                        default -> 1 - a;
                    };
                    float z = switch (up) {
                        case NORTH -> 1 - a;
                        case SOUTH -> a;
                        default -> s;
                    };
                    vertex(pose, buf, colour, x, a + lift, z, s == LO ? 0.25F : 0.75F, a * 1.41F + t);
                }
            }
        }
    }

    // ---- the pool in the furnace's chamber ----

    public static final class TapState extends BlockEntityRenderState {
        int pool, charge, colour;
        Direction back = Direction.SOUTH;
        float time;
    }

    public static final class Tap implements BlockEntityRenderer<FurnaceTapBlockEntity, TapState> {
        /** The chamber (the middle block, one up behind the tap): its floor, and how much of it a full furnace fills. */
        private static final float BOTTOM = 1.05F, DEPTH = 0.85F;

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
            float poolTop = BOTTOM + DEPTH * state.pool / FurnaceMelt.CAPACITY;
            float chargeTop = poolTop + DEPTH * state.charge / FurnaceMelt.CAPACITY;
            float dx = state.back.getStepX(), dz = state.back.getStepZ();
            collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
                // the chamber's floor area, one block behind the tap; the melt slowly churns
                if (state.pool > 0) flat(p, buf, state.colour, dx, dz, dx + 1, dz + 1, poolTop, 0, 1, state.time, state.time + 1, true);
                if (state.charge > 0) flat(p, buf, CHARGE, dx, dz, dx + 1, dz + 1, chargeTop, 0, 1, -state.time, 1 - state.time, false);
            });
        }

        @Override
        public AABB getRenderBoundingBox(FurnaceTapBlockEntity tap) {
            Direction back = tap.facing().getOpposite();
            return new AABB(tap.getBlockPos()).expandTowards(back.getStepX(), 2, back.getStepZ());
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
