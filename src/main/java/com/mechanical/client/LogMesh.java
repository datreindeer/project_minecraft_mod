package com.mechanical.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mechanical.FallenLogBlock;
import com.mechanical.Mechanical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A felled log's block drawn as textured boxes (FelledLogRenderer), so a cut can be carved into it: the block's own face
 * textures (from its model) on whatever is left of each outer face, the saw kerf's texture on the walls and floor of the
 * cut. The cut is a slot 1/8 wide through the middle of the block, its plane across the log (normal along local z) or
 * along it (normal along local x), opening from the top or a side and reaching {@code depth} (0..1) in.
 */
public final class LogMesh {
    /** Half the kerf's width. */
    private static final float W = 1 / 16.0F;
    /** Per block state: each face's four corners (position, uv) and sprite, from the model; rebuilt if the model changes (a resource reload). */
    private record Face(float[][] pos, float[][] uv) {}
    private record Faces(BlockStateModel model, Face[] faces) {}
    private static final Map<BlockState, Faces> FACES = new HashMap<>();
    private static TextureAtlasSprite kerf;
    private static BlockStateModel kerfFrom;

    /** The way a cut opens: up, +x, -x, +z, -z (index in a cut code, see {@link #draw}). */
    private static final Direction[] OPENINGS = {Direction.UP, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

    /**
     * Draws the unit block of {@code state} at the pose's origin with a cut: {@code code} = plane * 8 + opening (plane 0:
     * across the log, normal z; 1: along it, normal x; opening an index into {@link #OPENINGS}), {@code depth} 0..1.
     */
    public static void draw(PoseStack.Pose pose, VertexConsumer buffer, BlockState state, int code, float depth, int light) {
        Face[] faces = faces(state);
        if (faces == null) return;
        int n = (code / 8) == 1 ? 0 : 2; // the axis normal to the kerf's plane: x for a cut along the log, z across it
        Direction open = OPENINGS[Math.min(OPENINGS.length - 1, code % 8)];
        if (open.getAxis().ordinal() == n) open = Direction.UP; // can't open along its own normal
        int o = open.getAxis().ordinal();
        boolean oPos = open.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        if (depth <= 0.001F) {
            box(pose, buffer, faces, light, new float[] {0, 0, 0}, new float[] {1, 1, 1}, -1, null, 0);
            return;
        }
        float lo = 0.5F - W, hi = 0.5F + W;
        float cutFrom = oPos ? 1 - depth : 0, cutTo = oPos ? 1 : depth; // the cut's extent along o
        // A and B: either side of the slot, whole; their faces into the slot are kerf where the slot is open
        float[] aMin = {0, 0, 0}, aMax = {1, 1, 1}, bMin = {0, 0, 0}, bMax = {1, 1, 1};
        aMax[n] = lo;
        bMin[n] = hi;
        box(pose, buffer, faces, light, aMin, aMax, n, new float[] {cutFrom, cutTo}, o);
        box(pose, buffer, faces, light, bMin, bMax, n, new float[] {cutFrom, cutTo}, o);
        // C: the slot's uncut floor part; its face towards the opening is the kerf's floor
        float[] cMin = {0, 0, 0}, cMax = {1, 1, 1};
        cMin[n] = lo;
        cMax[n] = hi;
        if (oPos) cMax[o] = 1 - depth;
        else cMin[o] = depth;
        if (cMax[o] - cMin[o] > 0.001F) boxC(pose, buffer, faces, light, cMin, cMax, n, o, oPos);
    }

    /**
     * Faces of the box min..max: those on the block's outside with the block's texture; those at the kerf's walls (normal
     * {@code wallAxis}, not on the outside) with the kerf texture over the open part {@code open} along axis {@code o}.
     */
    private static void box(PoseStack.Pose pose, VertexConsumer buffer, Face[] faces, int light, float[] min, float[] max, int wallAxis, float[] open, int o) {
        for (Direction d : Direction.values()) {
            int k = d.getAxis().ordinal();
            boolean pos = d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            float plane = pos ? max[k] : min[k];
            boolean outer = plane <= 0.0001F || plane >= 0.9999F;
            if (outer) {
                quad(pose, buffer, light, d, plane, min, max, faces[d.ordinal()], null);
            } else if (k == wallAxis && open != null) { // a wall of the slot, where it is cut open
                float[] wMin = min.clone(), wMax = max.clone();
                wMin[o] = Math.max(min[o], open[0]);
                wMax[o] = Math.min(max[o], open[1]);
                if (wMax[o] - wMin[o] > 0.001F) quad(pose, buffer, light, d, plane, wMin, wMax, null, kerf());
            }
        }
    }

    /** The slot's floor part: outside faces textured, the face towards the opening kerf, its sides against A and B hidden. */
    private static void boxC(PoseStack.Pose pose, VertexConsumer buffer, Face[] faces, int light, float[] min, float[] max, int n, int o, boolean oPos) {
        for (Direction d : Direction.values()) {
            int k = d.getAxis().ordinal();
            if (k == n) continue; // against A and B
            boolean pos = d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            float plane = pos ? max[k] : min[k];
            boolean outer = plane <= 0.0001F || plane >= 0.9999F;
            if (outer) quad(pose, buffer, light, d, plane, min, max, faces[d.ordinal()], null);
            else if (k == o && pos == oPos) quad(pose, buffer, light, d, plane, min, max, null, kerf());
        }
    }

    /** One face: the rect min..max on the plane normal to {@code d}; uv from the block's face (by position) or the whole kerf sprite. */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light, Direction d, float plane, float[] min, float[] max, Face face, TextureAtlasSprite sprite) {
        if (face == null && sprite == null) return;
        int k = d.getAxis().ordinal(), u = (k + 1) % 3, v = (k + 2) % 3;
        boolean pos = d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float[][] corners = {{min[u], min[v]}, {max[u], min[v]}, {max[u], max[v]}, {min[u], max[v]}};
        float nx = d.getStepX(), ny = d.getStepY(), nz = d.getStepZ();
        for (int i = 0; i < 4; i++) {
            float[] c = corners[pos ? i : 3 - i]; // counter-clockwise seen from outside
            float[] p = new float[3];
            p[k] = plane;
            p[u] = c[0];
            p[v] = c[1];
            float tu, tv;
            if (face != null) {
                float[] t = uvAt(face, u, v, c[0], c[1]);
                tu = t[0];
                tv = t[1];
            } else { // the kerf sprite over the face, its height along the cut's depth
                tu = sprite.getU(c[0]);
                tv = sprite.getV(c[1]);
            }
            buffer.addVertex(pose, p[0], p[1], p[2]).setColor(-1).setUv(tu, tv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }

    /** The block face's uv at (a, b) on its plane (axes u, v): bilinear between its corners. */
    private static float[] uvAt(Face f, int u, int v, float a, float b) {
        float aMin = Float.MAX_VALUE, aMax = -Float.MAX_VALUE, bMin = Float.MAX_VALUE, bMax = -Float.MAX_VALUE;
        for (float[] p : f.pos) {
            aMin = Math.min(aMin, p[u]);
            aMax = Math.max(aMax, p[u]);
            bMin = Math.min(bMin, p[v]);
            bMax = Math.max(bMax, p[v]);
        }
        float s = aMax - aMin < 1.0E-4F ? 0 : (a - aMin) / (aMax - aMin), t = bMax - bMin < 1.0E-4F ? 0 : (b - bMin) / (bMax - bMin);
        float[][] corner = new float[4][]; // (aMin,bMin), (aMax,bMin), (aMax,bMax), (aMin,bMax)
        for (int i = 0; i < 4; i++) {
            float[] p = f.pos[i];
            boolean hiA = p[u] > (aMin + aMax) / 2, hiB = p[v] > (bMin + bMax) / 2;
            corner[hiA ? (hiB ? 2 : 1) : (hiB ? 3 : 0)] = f.uv[i];
        }
        for (float[] c : corner) if (c == null) return f.uv[0];
        float u0 = corner[0][0] + (corner[1][0] - corner[0][0]) * s, v0 = corner[0][1] + (corner[1][1] - corner[0][1]) * s;
        float u1 = corner[3][0] + (corner[2][0] - corner[3][0]) * s, v1 = corner[3][1] + (corner[2][1] - corner[3][1]) * s;
        return new float[] {u0 + (u1 - u0) * t, v0 + (v1 - v0) * t};
    }

    private static List<BakedQuad> quads(BlockStateModel model, Direction d) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        List<BakedQuad> out = new ArrayList<>();
        for (BlockStateModelPart part : parts) out.addAll(part.getQuads(d));
        return out;
    }

    /** Each face of the block's model (the face quad covering most of it), or null without a model. */
    private static Face[] faces(BlockState state) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        if (model == null) return null;
        Faces cached = FACES.get(state);
        if (cached != null && cached.model == model) return cached.faces;
        Face[] faces = new Face[6];
        for (Direction d : Direction.values()) {
            List<BakedQuad> qs = new ArrayList<>(quads(model, d));
            if (qs.isEmpty()) for (BakedQuad q : quads(model, null)) if (q.direction() == d) qs.add(q);
            BakedQuad best = null;
            float bestArea = -1;
            for (BakedQuad q : qs) {
                float area = area(q, d);
                if (area > bestArea) {
                    bestArea = area;
                    best = q;
                }
            }
            if (best == null) continue;
            float[][] pos = new float[4][3], uv = new float[4][2];
            for (int i = 0; i < 4; i++) {
                var p = best.position(i);
                pos[i] = new float[] {p.x(), p.y(), p.z()};
                long packed = best.packedUV(i);
                uv[i] = new float[] {UVPair.unpackU(packed), UVPair.unpackV(packed)};
            }
            faces[d.ordinal()] = new Face(pos, uv);
        }
        FACES.put(state, new Faces(model, faces));
        return faces;
    }

    private static float area(BakedQuad q, Direction d) {
        int k = d.getAxis().ordinal(), u = (k + 1) % 3, v = (k + 2) % 3;
        float aMin = Float.MAX_VALUE, aMax = -Float.MAX_VALUE, bMin = Float.MAX_VALUE, bMax = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            var p = q.position(i);
            float[] c = {p.x(), p.y(), p.z()};
            aMin = Math.min(aMin, c[u]);
            aMax = Math.max(aMax, c[u]);
            bMin = Math.min(bMin, c[v]);
            bMax = Math.max(bMax, c[v]);
        }
        return (aMax - aMin) * (bMax - bMin);
    }

    /** The saw kerf's sprite (from the sawn Fallen Log model, which uses it), found once per model set. */
    private static TextureAtlasSprite kerf() {
        BlockState sawn = Mechanical.FALLEN_LOGS.get("oak").get().defaultBlockState().setValue(FallenLogBlock.SAWN, 4);
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(sawn);
        if (kerf != null && kerfFrom == model) return kerf;
        kerfFrom = model;
        kerf = null;
        for (Direction d : Direction.values()) for (BakedQuad q : quads(model, d)) if (isKerf(q)) kerf = q.materialInfo().sprite();
        if (kerf == null) for (BakedQuad q : quads(model, null)) if (isKerf(q)) kerf = q.materialInfo().sprite();
        if (kerf == null) kerf = model.particleMaterial().sprite();
        return kerf;
    }

    private static boolean isKerf(BakedQuad q) {
        return q.materialInfo().sprite().contents().name().getPath().contains("saw_kerf");
    }

    private LogMesh() {}
}
