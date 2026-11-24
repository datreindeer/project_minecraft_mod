package com.mechanical.client;

import com.mechanical.ClayChannelBlock;
import com.mechanical.HighFurnace;
import com.mechanical.Mechanical;
import com.mechanical.Molten;
import com.mechanical.RefractoryTroughBlock;
import com.mechanical.TroughBlockEntity;
import com.mechanical.TroughFlow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/**
 * The melt in a Refractory Trough, Trough Gate or Sand Runner ({@link TroughBlockEntity}), drawn so it reads as running
 * the right way:
 * <ul>
 * <li>its crusted surface drifts toward the sides it is leaving through (the next trough, a mold, a spout) and in from the
 * sides it comes from; where nothing goes out it lies still, and a full, still trough does not drift at all;</li>
 * <li>metal arriving in an empty trough creeps in from the side it came from (or spreads from under a spout) with a
 * darker front;</li>
 * <li>it stops at closed ends with a bright meniscus line, never past the trough's inner rim; at a shut gate it pools
 * against the plate on the side it came from;</li>
 * <li>white-hot near the tap, dimming down the line; setting crusts from the edges inward, then goes dark and solid
 * (frozen metal at the reach limit is dark at once).</li>
 * </ul>
 * Slopes keep their tilted film ({@link MoltenRenderer.Channel#channel}).
 */
public class TroughRenderer implements BlockEntityRenderer<TroughBlockEntity, TroughRenderer.State> {
    /** Inside of the channel (blocks) and how long a front takes to cross a trough (ticks). */
    private static final float LO = 5 / 16F, HI = 11 / 16F, FRONT_TICKS = TroughFlow.STEP_TICKS, RECENT = 9;

    public static class State extends BlockEntityRenderState {
        @Nullable Molten molten;
        float fill, time, cooled, hot, front = 1;
        int sides, out, in;
        /** The metal's height at each side's block edge (blocks), shared with the neighbour there so the surfaces meet. */
        final float[] edge = new float[4];
        float level;
        @Nullable Direction up, arrivedFrom, gateFrom;
        boolean sand, still, shutGate;
        Direction.Axis gateAxis = Direction.Axis.Z;
    }

    public TroughRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TroughBlockEntity t, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(t, state, partialTicks, camera, breakProgress);
        long now = t.getLevel() == null ? 0 : t.getLevel().getGameTime();
        state.molten = t.molten();
        state.fill = Math.min(1, t.amount() / (float) TroughBlockEntity.CAPACITY);
        state.cooled = t.cooled(now);
        state.hot = state.molten == null ? 0 : Math.clamp(1 - t.dist() / (float) Math.max(1, TroughFlow.reach(t.heat(), state.molten) + 1), 0, 1);
        state.time = ((now % 2000) + partialTicks) * 0.05F;
        state.out = 0;
        state.in = 0;
        state.level = level(state.fill);
        BlockState here = t.getBlockState();
        boolean moving = t.running(now) || anyRecent(t, now);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int bit = 1 << d.get2DDataValue();
            state.edge[d.get2DDataValue()] = state.level;
            if (!(here.getBlock() instanceof ClayChannelBlock) || !here.getValue(ClayChannelBlock.SIDES.get(d)) || t.getLevel() == null) continue;
            BlockPos q = com.mechanical.ChannelFlow.across(t.getLevel(), t.getBlockPos(), com.mechanical.ChannelFlow.port(t.getBlockPos(), d, t.getBlockPos().getY()));
            if (q != null && t.getLevel().getBlockEntity(q) instanceof TroughBlockEntity n) {
                // the surfaces meet half way between the two levels at the shared edge
                if (n.molten() != null) state.edge[d.get2DDataValue()] = (state.level + level(Math.min(1, n.amount() / (float) TroughBlockEntity.CAPACITY))) / 2;
                // which way it runs: away from the tap (toward troughs further out), in from those nearer
                if (moving && n.molten() != null && n.dist() > t.dist() || moving && n.molten() == null && now - t.outAt(d) < RECENT) state.out |= bit;
                else if (moving && n.molten() != null && n.dist() < t.dist()) state.in |= bit;
            } else if (moving && now - t.outAt(d) < RECENT) state.out |= bit; // into a mold or a spout
        }
        if (moving && t.arrivedFrom() != null && state.in == 0 && state.out != 0) state.in |= 1 << t.arrivedFrom().get2DDataValue();
        state.still = !moving || state.out == 0 || state.cooled > 0.3F;
        long age = now - t.arrivedAt();
        state.front = age < FRONT_TICKS ? Math.clamp((age + partialTicks) / FRONT_TICKS, 0.08F, 1) : 1;
        state.arrivedFrom = t.arrivedFrom();
        BlockState block = t.getBlockState();
        state.sand = block.getBlock() instanceof RefractoryTroughBlock.SandRunner;
        state.shutGate = block.getBlock() instanceof RefractoryTroughBlock.Gate && !block.getValue(RefractoryTroughBlock.Gate.OPEN);
        if (block.getBlock() instanceof RefractoryTroughBlock.Gate) state.gateAxis = block.getValue(RefractoryTroughBlock.Gate.AXIS);
        state.gateFrom = state.shutGate ? (t.arrivedFrom() != null ? t.arrivedFrom() : firstSide(state.in, state.gateAxis)) : null;
        state.up = null;
        state.sides = 0;
        if (block.getBlock() instanceof ClayChannelBlock) {
            state.up = block.getValue(ClayChannelBlock.SLOPE).up;
            for (Direction d : Direction.Plane.HORIZONTAL) if (block.getValue(ClayChannelBlock.SIDES.get(d))) state.sides |= 1 << d.get2DDataValue();
        }
    }

    /** The metal's surface height (blocks) in a level trough this full; a slope's film matches it at both ends. */
    static float level(float fill) {
        return (ClayChannelBlock.FLOOR + 0.3F + (ClayChannelBlock.WALL - 0.8F - ClayChannelBlock.FLOOR) * fill) / 16;
    }

    private static boolean anyRecent(TroughBlockEntity t, long now) {
        for (Direction d : Direction.Plane.HORIZONTAL) if (now - t.outAt(d) < RECENT || now - t.inAt(d) < RECENT) return true;
        return false;
    }

    private static Direction firstSide(int mask, Direction.Axis axis) {
        for (Direction d : Direction.Plane.HORIZONTAL) if (d.getAxis() == axis && (mask & 1 << d.get2DDataValue()) != 0) return d;
        return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
    }

    /** The melt's colour cooling: its own glow, through orange and dark red to a grey crust tinted by the metal. */
    static int cooling(Molten m, float cooled) {
        int hot = 0xFF000000 | m.color, skin = ARGB.srgbLerp(0.3F, 0xFF4A4642, hot) | 0xFF000000;
        if (cooled < 0.5F) return ARGB.srgbLerp(cooled / 0.5F, hot, 0xFFE0661A) | 0xFF000000;
        if (cooled < 0.8F) return ARGB.srgbLerp((cooled - 0.5F) / 0.3F, 0xFFE0661A, 0xFF8A1E08) | 0xFF000000;
        return ARGB.srgbLerp((cooled - 0.8F) / 0.2F, 0xFF8A1E08, skin) | 0xFF000000;
    }

    /** A melt's glow by how hot it still is: toward white-hot near the source, toward a deep orange-red at the end of its reach. */
    static int glow(Molten m, float hot) {
        int base = 0xFF000000 | m.color;
        return hot > 0.5F ? ARGB.srgbLerp((hot - 0.5F) * 0.4F, base, 0xFFFFF4D2) | 0xFF000000
                : ARGB.srgbLerp((0.5F - hot) * 1.4F, base, 0xFFB8380C) | 0xFF000000;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.molten == null) return;
        int c = state.cooled < 0.05F ? glow(state.molten, state.hot) : cooling(state.molten, state.cooled);
        if (state.sand) pose.translate(0, 11 / 16F, 0); // a runner's groove is at the top of its sand block
        if (state.up != null) { // a slope: the film tilts with it and runs down
            float matched = (state.level * 16 - 0.6F) / 1.6F; // channel() lifts the film (0.6 + 1.6 f) px: make that the level trough's height
            MoltenRenderer.Channel.channel(collector, pose, MoltenRenderer.CRUST, c, state.still ? 0 : state.time * 0.8F, state.up, state.sides, matched);
            return;
        }
        float y = state.level;
        float[] edge = state.edge;
        // where the metal is: the trough's inside (middle and open arms), clipped by a creeping front or a shut gate's plate
        float[] clip = {0, 1, 0, 1}; // x0 x1 z0 z1
        if (state.front < 1) {
            Direction f = state.arrivedFrom;
            if (f == null) { // poured in from above: it spreads from the middle
                float r = 0.5F * state.front;
                clip = new float[] {0.5F - r, 0.5F + r, 0.5F - r, 0.5F + r};
            } else clip = side(f, state.front);
        }
        if (state.shutGate && state.gateFrom != null) clip = intersect(clip, side(state.gateFrom, 7.3F / 16));
        final float[] k = clip;
        int light = state.cooled >= 1 ? state.lightCoords : LightCoordsUtil.FULL_BRIGHT;
        float t = state.still ? 0 : state.time;
        int out = state.out, in = state.in, sides = state.sides;
        collector.submitCustomGeometry(pose, MoltenRenderer.CRUST, (p, buf) -> {
            // the middle drifts along the net flow: toward the outlets, away from the inlets
            float mx = 0, mz = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int bit = 1 << d.get2DDataValue();
                if ((out & bit) != 0) {
                    mx += d.getStepX();
                    mz += d.getStepZ();
                }
                if ((in & bit) != 0) {
                    mx -= d.getStepX();
                    mz -= d.getStepZ();
                }
            }
            float len = (float) Math.sqrt(mx * mx + mz * mz);
            if (len > 0) {
                mx /= len;
                mz /= len;
            }
            quad(p, buf, c, light, LO, LO, HI, HI, y, k, -mx * t, -mz * t);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int bit = 1 << d.get2DDataValue();
                if ((sides & bit) == 0) {
                    meniscus(p, buf, c, light, d, y, k);
                    continue;
                }
                float dir = (out & bit) != 0 ? 1 : (in & bit) != 0 ? -1 : 0; // toward the next trough, in from it, or still
                float du = -d.getStepX() * dir * t, dv = -d.getStepZ() * dir * t;
                arm(p, buf, c, light, d, y, edge[d.get2DDataValue()], k, du, dv);
            }
            if (state.front < 0.85F) front(p, buf, c, light, state.arrivedFrom, state.front, y); // gone before it reaches the joint
            // setting starts at the edges: a crust rim grows inward as it cools
            if (state.cooled > 0.2F && state.cooled < 1) {
                int crust = cooling(state.molten, Math.min(1, state.cooled + 0.35F));
                float w = Math.min(0.12F, (state.cooled - 0.2F) * 0.25F);
                quad(p, buf, crust, light, LO, LO, LO + w, HI, y + 0.002F, k, 0, 0);
                quad(p, buf, crust, light, HI - w, LO, HI, HI, y + 0.002F, k, 0, 0);
                quad(p, buf, crust, light, LO, LO, HI, LO + w, y + 0.002F, k, 0, 0);
                quad(p, buf, crust, light, LO, HI - w, HI, HI, y + 0.002F, k, 0, 0);
            }
        });
    }

    /** The part of the block from side {@code d} reaching {@code f} (0..1) into it. */
    private static float[] side(Direction d, float f) {
        return switch (d) {
            case NORTH -> new float[] {0, 1, 0, f};
            case SOUTH -> new float[] {0, 1, 1 - f, 1};
            case WEST -> new float[] {0, f, 0, 1};
            default -> new float[] {1 - f, 1, 0, 1};
        };
    }

    private static float[] intersect(float[] a, float[] b) {
        return new float[] {Math.max(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[2], b[2]), Math.min(a[3], b[3])};
    }

    /** A flat quad of melt from (x0, z0) to (x1, z1), clipped to {@code k}, its texture shifted by (du, dv), seen from both sides. */
    private static void quad(PoseStack.Pose p, VertexConsumer buf, int c, int light, float x0, float z0, float x1, float z1, float y, float[] k, float du, float dv) {
        x0 = Math.max(x0, k[0]);
        x1 = Math.min(x1, k[1]);
        z0 = Math.max(z0, k[2]);
        z1 = Math.min(z1, k[3]);
        if (x1 - x0 < 0.001F || z1 - z0 < 0.001F) return;
        float[][] v = {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}};
        for (int i = 0; i < 8; i++) {
            float[] a = v[i < 4 ? i : 7 - i];
            buf.addVertex(p, a[0], y, a[1]).setColor(c).setUv(a[0] + du, a[1] + dv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
        }
    }

    /** An arm of melt out to the block edge on side {@code d}: at the middle's height {@code yi} inside, {@code yo} at the edge. */
    private static void arm(PoseStack.Pose p, VertexConsumer buf, int c, int light, Direction d, float yi, float yo, float[] k, float du, float dv) {
        float x0, x1, z0, z1;
        switch (d) {
            case NORTH -> { x0 = LO; x1 = HI; z0 = 0; z1 = LO; }
            case SOUTH -> { x0 = LO; x1 = HI; z0 = HI; z1 = 1; }
            case WEST -> { x0 = 0; x1 = LO; z0 = LO; z1 = HI; }
            default -> { x0 = HI; x1 = 1; z0 = LO; z1 = HI; }
        }
        x0 = Math.max(x0, k[0]);
        x1 = Math.min(x1, k[1]);
        z0 = Math.max(z0, k[2]);
        z1 = Math.min(z1, k[3]);
        if (x1 - x0 < 0.001F || z1 - z0 < 0.001F) return;
        float[][] v = {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}};
        for (int i = 0; i < 8; i++) {
            float[] a = v[i < 4 ? i : 7 - i];
            float along = switch (d) { // 0 at the middle's edge, 1 at the block edge
                case NORTH -> (LO - a[1]) / LO;
                case SOUTH -> (a[1] - HI) / (1 - HI);
                case WEST -> (LO - a[0]) / LO;
                default -> (a[0] - HI) / (1 - HI);
            };
            float y = yi + (yo - yi) * along;
            buf.addVertex(p, a[0], y, a[1]).setColor(c).setUv(a[0] + du, a[1] + dv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
        }
    }

    /** At a closed end: a thin bright line where the melt climbs the wall (the meniscus). */
    private static void meniscus(PoseStack.Pose p, VertexConsumer buf, int c, int light, Direction d, float y, float[] k) {
        int bright = ARGB.srgbLerp(0.35F, c, 0xFFFFFFFF) | 0xFF000000;
        float w = 0.4F / 16, lift = 0.25F / 16;
        switch (d) {
            case NORTH -> quad(p, buf, bright, light, LO, LO, HI, LO + w, y + lift, k, 0, 0);
            case SOUTH -> quad(p, buf, bright, light, LO, HI - w, HI, HI, y + lift, k, 0, 0);
            case WEST -> quad(p, buf, bright, light, LO, LO, LO + w, HI, y + lift, k, 0, 0);
            default -> quad(p, buf, bright, light, HI - w, LO, HI, HI, y + lift, k, 0, 0);
        }
    }

    /** The leading edge of metal creeping in from {@code from}: a darker band across the channel, stopping short of the walls. */
    private static void front(PoseStack.Pose p, VertexConsumer buf, int c, int light, @Nullable Direction from, float f, float y) {
        int dark = ARGB.srgbLerp(0.45F, c, 0xFF3A1406) | 0xFF000000;
        float w = 0.6F / 16;
        float[] all = {0, 1, 0, 1};
        if (from == null) return; // poured in from above: it just spreads (clipped to the channel by the caller)
        float in = 0.4F / 16; // rounded: the band stops short of the walls
        switch (from) {
            case NORTH -> quad(p, buf, dark, light, LO + in, f - w, HI - in, f, y + 0.003F, all, 0, 0);
            case SOUTH -> quad(p, buf, dark, light, LO + in, 1 - f, HI - in, 1 - f + w, y + 0.003F, all, 0, 0);
            case WEST -> quad(p, buf, dark, light, f - w, LO + in, f, HI - in, y + 0.003F, all, 0, 0);
            default -> quad(p, buf, dark, light, 1 - f, LO + in, 1 - f + w, HI - in, y + 0.003F, all, 0, 0);
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.TROUGH_ENTITY.get(), TroughRenderer::new);
        }
    }
}
