package com.mechanical.client.people;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.people.TribeRole;
import com.mechanical.people.Tribesperson;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The capes of the elders, the caravan leaders and the Oak Keeper: the sculpted shoulder pieces and collar
 * (TribeCapeModel) and the cloth, a mesh rebuilt every frame so it can flow. It is pinned across the shoulders, hangs
 * down with soft folds, billows and ripples with the walk, is pushed out by whatever of the body, arms and legs would
 * come through it, and lies back along the ground when its wearer sits. Textures: scripts/gen_capes.py.
 */
public class TribeCapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** What cape a tribesperson wears: the style, its sheet, and the tribe's dye for the sheet's tinted parts (-1: none). */
    public record Cape(Style style, Identifier texture, int dye) {}

    public enum Style {
        ELDER(5.7F, 7.2F, 21.0F), TRADER(5.7F, 7.3F, 21.0F), OAK(5.8F, 7.6F, 22.0F);

        final float topHalf, hemHalf, length;

        Style(float topHalf, float hemHalf, float length) {
            this.topHalf = topHalf;
            this.hemHalf = hemHalf;
            this.length = length;
        }
    }

    static final ContextKey<Cape> CAPE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_cape"));

    /** The cape this person wears, or null: elders, Trader Caravan leaders (in their kind's colours) and the Oak Keeper. */
    static Cape of(Tribesperson t) {
        TribeRole role = t.role();
        if (role == TribeRole.ELDER) return cape(Style.ELDER, Mechanical.MODID, "elder_" + t.climate().id(), t.colour());
        if (role == TribeRole.OAK_KEEPER) return cape(Style.OAK, Mechanical.MODID, "oak_keeper", -1);
        if (role != TribeRole.TRADER) return null;
        Identifier kind = t.caravanKind();
        return kind == null ? cape(Style.TRADER, Mechanical.MODID, "trader_plain", t.colour())
                : cape(Style.TRADER, kind.getNamespace(), "trader_" + kind.getPath(), -1);
    }

    private static Cape cape(Style style, String namespace, String name, int dye) {
        Identifier texture = Identifier.fromNamespaceAndPath(namespace, "textures/entity/tribesperson/cape/" + name + ".png");
        // a caravan kind added by a data pack without a cape of its own wears the plain one
        if (style == Style.TRADER && !EXISTS.computeIfAbsent(texture, t -> net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(t).isPresent()))
            texture = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/tribesperson/cape/trader_foraging.png");
        return new Cape(style, texture, dye);
    }

    private static final java.util.Map<Identifier, Boolean> EXISTS = new java.util.HashMap<>();

    private final ModelPart[] parts = new ModelPart[Style.values().length];

    public TribeCapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet models) {
        super(renderer);
        parts[Style.ELDER.ordinal()] = models.bakeLayer(TribeCapeModel.ELDER);
        parts[Style.TRADER.ordinal()] = models.bakeLayer(TribeCapeModel.TRADER);
        parts[Style.OAK.ordinal()] = models.bakeLayer(TribeCapeModel.OAK);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Cape cape = state.getRenderData(CAPE);
        if (cape == null || state.isInvisible) return;
        PlayerModel model = getParentModel(); // already posed for this state (LivingEntityRenderer#submit)
        RenderType type = RenderTypes.entityCutout(TintedSkin.of(cape.texture(), cape.dye()));
        ModelPart pieces = parts[cape.style().ordinal()];
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);

        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        part(collector, pieces.getChild("collar"), poseStack, type, light, state);
        poseStack.popPose();
        shoulder(collector, pieces.getChild("right"), model.rightArm, poseStack, type, light, state);
        shoulder(collector, pieces.getChild("left"), model.leftArm, poseStack, type, light, state);

        List<float[]> quads = cloth(cape.style(), model, state);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            for (float[] v : quads) {
                buffer.addVertex(pose, v[0] / 16, v[1] / 16, v[2] / 16).setColor(-1).setUv(v[3], v[4])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, v[5], v[6], v[7]);
            }
        });
        poseStack.popPose();
    }

    private static void part(SubmitNodeCollector collector, ModelPart part, PoseStack pose, RenderType type, int light, AvatarRenderState state) {
        collector.submitModelPart(part, pose, type, light, OverlayTexture.NO_OVERLAY, null, -1, state.outlineColor);
    }

    /**
     * A shoulder piece rides on the arm but swings less than it, and no further than a stiff pad would: with the arm
     * raised high it stays on the shoulder instead of turning over into the cape and the head.
     */
    private static void shoulder(SubmitNodeCollector collector, ModelPart piece, ModelPart arm, PoseStack pose, RenderType type, int light, AvatarRenderState state) {
        pose.pushPose();
        pose.translate(arm.x / 16, arm.y / 16, arm.z / 16);
        pose.rotate(new Quaternionf().rotationZYX(Mth.clamp(arm.zRot * 0.7F, -0.35F, 0.35F), arm.yRot * 0.5F, Mth.clamp(arm.xRot * 0.8F, -0.6F, 0.7F)));
        part(collector, piece, pose, type, light, state);
        pose.popPose();
    }

    // ---------------------------------------------------------------- the cloth

    private static final int COLS = 18, ROWS = 24;
    private static final float THICK = 0.3F, MARGIN = 0.75F;

    /** The cloth's vertices, four a quad: x, y, z (model units, in the model root's space), u, v, normal. */
    private static List<float[]> cloth(Style style, PlayerModel model, AvatarRenderState state) {
        // the backs of the body, arms and legs as they are posed now: the cloth stays behind these points
        List<Vector3f> backs = new ArrayList<>();
        limb(backs, model.body, -4.25F, 4.25F, 0.0F, 12.25F, 2.25F);
        limb(backs, model.rightLeg, -2.25F, 2.25F, 0.0F, 12.25F, 2.25F);
        limb(backs, model.leftLeg, -2.25F, 2.25F, 0.0F, 12.25F, 2.25F);
        limb(backs, model.rightArm, -3.25F, 1.25F, 0.0F, 10.25F, 2.25F);
        limb(backs, model.leftArm, -1.25F, 3.25F, 0.0F, 10.25F, 2.25F);
        Matrix4f body = matrix(model.body);
        float ground = 23.6F - model.root().y; // sitting, the whole model is let down: the ground comes up to meet the hem
        float walk = Math.min(1.0F, state.walkAnimationSpeed * 1.6F), step = state.walkAnimationPos * 0.6662F, age = state.ageInTicks;

        Vector3f[][] p = new Vector3f[COLS + 1][ROWS + 1];
        for (int i = 0; i <= COLS; i++) {
            float u = i * 2.0F / COLS - 1, au = Math.abs(u);
            // pinned across the shoulders, the corners wrapped a little forward over them
            Vector3f top = body.transformPosition(new Vector3f(u * style.topHalf, -0.45F + 0.9F * (1 - u * u), 2.95F - 1.5F * au * au * au * au));
            for (int k = 0; k <= ROWS; k++) {
                float v = k / (float) ROWS, spread = Mth.sqrt(v);
                float x = top.x + u * (style.hemHalf - style.topHalf) * spread
                        + walk * v * v * 0.9F * Mth.sin(step - v * 2.0F); // swinging from side to side with the steps
                float y = top.y + v * style.length - walk * v * v * 1.6F;
                float z = top.z + v * 1.5F - au * au * au * v * 0.9F // falling a little out, its edges curling in
                        + walk * v * v * (4.0F + 0.9F * Mth.sin(step * 2 - v * 4.5F + u)) // billowing, a ripple running down
                        + v * 0.22F * Mth.sin(age * 0.055F - v * 2.6F + u * 1.5F); // never quite still
                p[i][k] = new Vector3f(x, y, z);
            }
        }
        drape(p, backs);
        for (int i = 0; i <= COLS; i++) {
            float u = i * 2.0F / COLS - 1;
            for (int k = 1; k <= ROWS; k++) {
                float v = k / (float) ROWS;
                Vector3f q = p[i][k];
                // folds, only ever outward, deepening toward the hem
                q.z += (float) Math.pow(v, 0.7) * 0.95F * (0.5F - 0.5F * Mth.cos(u * Mth.PI * 3.0F + walk * 0.5F * Mth.sin(step - v * 2)));
                // on the ground: what is left of the length lies back along it
                if (q.y > ground) {
                    float over = q.y - ground;
                    q.y = ground + Math.min(0.25F, over * 0.04F) - 0.2F * Mth.cos(u * Mth.PI * 3.0F);
                    q.z += over * 0.92F;
                }
            }
        }

        Vector3f[][] n = new Vector3f[COLS + 1][ROWS + 1], in = new Vector3f[COLS + 1][ROWS + 1], nIn = new Vector3f[COLS + 1][ROWS + 1];
        for (int i = 0; i <= COLS; i++) {
            for (int k = 0; k <= ROWS; k++) {
                Vector3f du = new Vector3f(p[Math.min(COLS, i + 1)][k]).sub(p[Math.max(0, i - 1)][k]);
                Vector3f dv = new Vector3f(p[i][Math.min(ROWS, k + 1)]).sub(p[i][Math.max(0, k - 1)]);
                n[i][k] = du.cross(dv).normalize();
                if (!n[i][k].isFinite()) n[i][k] = new Vector3f(0, 0, 1);
                in[i][k] = new Vector3f(p[i][k]).sub(n[i][k].x * THICK, n[i][k].y * THICK, n[i][k].z * THICK);
                nIn[i][k] = new Vector3f(n[i][k]).negate();
            }
        }
        List<float[]> quads = new ArrayList<>();
        for (int i = 0; i < COLS; i++) {
            for (int k = 0; k < ROWS; k++) {
                float u0 = i * 28.0F / COLS / 128, u1 = (i + 1) * 28.0F / COLS / 128, v0 = k * 44.0F / ROWS / 128, v1 = (k + 1) * 44.0F / ROWS / 128;
                vertex(quads, p[i][k], u0, v0, n[i][k]);
                vertex(quads, p[i + 1][k], u1, v0, n[i + 1][k]);
                vertex(quads, p[i + 1][k + 1], u1, v1, n[i + 1][k + 1]);
                vertex(quads, p[i][k + 1], u0, v1, n[i][k + 1]);
                float s = 32.0F / 128; // the lining
                vertex(quads, in[i][k], s + u0, v0, nIn[i][k]);
                vertex(quads, in[i][k + 1], s + u0, v1, nIn[i][k + 1]);
                vertex(quads, in[i + 1][k + 1], s + u1, v1, nIn[i + 1][k + 1]);
                vertex(quads, in[i + 1][k], s + u1, v0, nIn[i + 1][k]);
            }
        }
        // the cloth's thickness down both sides and across the top (the hem is cut ragged by the texture)
        for (int k = 0; k < ROWS; k++) {
            for (int i : new int[] {0, COLS}) {
                float u0 = (i == 0 ? 0.0F : 27.0F) / 128, u1 = u0 + 1.0F / 128, v0 = k * 44.0F / ROWS / 128, v1 = (k + 1) * 44.0F / ROWS / 128;
                single(quads, p[i][k], u0, v0, in[i][k], u1, v0, in[i][k + 1], u1, v1, p[i][k + 1], u0, v1, new Vector3f(i == 0 ? -1 : 1, 0, 0));
            }
        }
        for (int i = 0; i < COLS; i++) {
            float u0 = i * 28.0F / COLS / 128, u1 = (i + 1) * 28.0F / COLS / 128;
            single(quads, p[i][0], u0, 0, in[i][0], u0, 1.0F / 128, in[i + 1][0], u1, 1.0F / 128, p[i + 1][0], u1, 0, new Vector3f(0, -1, 0));
        }
        for (float[] s : sprites(style)) sprite(quads, p, n, s);
        return quads;
    }

    /** Pushes the cloth out behind the body, arms and legs, then lets it fall smoothly round what pushes it. */
    private static void drape(Vector3f[][] p, List<Vector3f> backs) {
        for (int i = 0; i <= COLS; i++) {
            for (int k = 1; k <= ROWS; k++) {
                Vector3f q = p[i][k];
                for (Vector3f b : backs) {
                    if (Math.abs(b.x - q.x) < 1.3F && Math.abs(b.y - q.y) < 1.5F && q.z < b.z + MARGIN) q.z = b.z + MARGIN;
                }
            }
            // above a bulge the cloth slopes down onto it; below it, it falls back only slowly
            for (int k = ROWS; k > 1; k--) p[i][k - 1].z = Math.max(p[i][k - 1].z, p[i][k].z - 0.85F);
            for (int k = 1; k < ROWS; k++) p[i][k + 1].z = Math.max(p[i][k + 1].z, p[i][k].z - 0.3F);
        }
        for (int k = 1; k <= ROWS; k++) { // and across: no sharp step between neighbouring columns
            for (int i = 1; i <= COLS; i++) p[i][k].z = Math.max(p[i][k].z, p[i - 1][k].z - 0.7F);
            for (int i = COLS - 1; i >= 0; i--) p[i][k].z = Math.max(p[i][k].z, p[i + 1][k].z - 0.7F);
        }
    }

    private static Matrix4f matrix(ModelPart part) {
        return new Matrix4f().translate(part.x, part.y, part.z).rotate(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
    }

    private static void limb(List<Vector3f> out, ModelPart part, float x0, float x1, float y0, float y1, float back) {
        Matrix4f m = matrix(part);
        for (int a = 0; a <= 4; a++) {
            for (int b = 0; b <= 10; b++) out.add(m.transformPosition(new Vector3f(Mth.lerp(a / 4.0F, x0, x1), Mth.lerp(b / 10.0F, y0, y1), back)));
        }
    }

    private static void vertex(List<float[]> quads, Vector3f p, float u, float v, Vector3f n) {
        quads.add(new float[] {p.x, p.y, p.z, u, v, n.x, n.y, n.z});
    }

    /** A single quad: the entity pipeline culls nothing and lights each face by the side seen (a second copy would only z-fight). */
    private static void single(List<float[]> quads, Vector3f a, float au, float av, Vector3f b, float bu, float bv, Vector3f c, float cu, float cv,
            Vector3f d, float du, float dv, Vector3f n) {
        vertex(quads, a, au, av, n);
        vertex(quads, b, bu, bv, n);
        vertex(quads, c, cu, cv, n);
        vertex(quads, d, du, dv, n);
    }

    // ---------------- the sprites laid over the cloth: {u -1..1, v 0..1, half width in u, height in v, lift, sprite}

    private static final float[][][] SPRITES = new float[Style.values().length][][];

    private static float[][] sprites(Style style) {
        float[][] s = SPRITES[style.ordinal()];
        if (s != null) return s;
        List<float[]> out = new ArrayList<>();
        if (style == Style.OAK) {
            // a mantle of leaves over the shoulders, three rows lapping, then leaves down both edges
            for (int row = 2; row >= 0; row--) {
                int count = 7 - row % 2;
                for (int j = 0; j < count; j++) {
                    float u = (j - (count - 1) / 2.0F) * 0.31F;
                    out.add(new float[] {u, 0.015F + row * 0.075F + 0.02F * Math.abs(u) + (row == 2 ? 0.05F * Math.abs(u) : 0), 0.19F, row == 2 ? 0.115F : 0.13F,
                            1.05F - row * 0.12F, (j * 2 + row) % 5});
                }
            }
            for (int j = 0; j < 7; j++) {
                for (int side : new int[] {-1, 1}) out.add(new float[] {side * (0.9F - (j % 2) * 0.05F), 0.27F + j * 0.092F, 0.15F, 0.12F, 0.55F, (j + side + 5) % 5});
            }
        } else if (style == Style.ELDER) {
            // a ruff of tufts across the shoulders
            for (int j = 0; j < 7; j++) {
                float u = (j - 3) * 0.29F;
                out.add(new float[] {u, 0.01F + 0.015F * (j % 2), 0.17F, 0.065F, 0.75F, j % 5});
            }
        }
        s = out.toArray(float[][]::new);
        SPRITES[style.ordinal()] = s;
        return s;
    }

    private static void sprite(List<float[]> quads, Vector3f[][] p, Vector3f[][] n, float[] s) {
        Vector3f normal = new Vector3f(at(n, s[0], s[1])).normalize();
        Vector3f a = off(p, n, s[0] - s[2], s[1], 0.12F), b = off(p, n, s[0] + s[2], s[1], 0.12F);
        // each hangs a little askew, its own way
        float tilt = s[2] * 0.55F * Mth.sin(s[0] * 23.0F + s[1] * 41.0F);
        Vector3f c = off(p, n, s[0] + s[2] + tilt, s[1] + s[3], s[4]), d = off(p, n, s[0] - s[2] + tilt, s[1] + s[3], s[4]);
        float u0 = s[5] * 5 / 128, u1 = u0 + 5.0F / 128, v0 = 48.0F / 128, v1 = 54.0F / 128;
        single(quads, a, u0, v0, b, u1, v0, c, u1, v1, d, u0, v1, normal);
    }

    private static Vector3f off(Vector3f[][] p, Vector3f[][] n, float u, float v, float lift) {
        Vector3f normal = at(n, u, v);
        return at(p, u, v).add(normal.x * lift, normal.y * lift, normal.z * lift);
    }

    /** The grid's value at u (-1..1 across), v (0..1 down). */
    private static Vector3f at(Vector3f[][] grid, float u, float v) {
        float x = Mth.clamp((u + 1) / 2 * COLS, 0, COLS), y = Mth.clamp(v * ROWS, 0, ROWS);
        int i = Math.min(COLS - 1, (int) x), k = Math.min(ROWS - 1, (int) y);
        Vector3f top = new Vector3f(grid[i][k]).lerp(grid[i + 1][k], x - i);
        return top.lerp(new Vector3f(grid[i][k + 1]).lerp(grid[i + 1][k + 1], x - i), y - k);
    }
}
