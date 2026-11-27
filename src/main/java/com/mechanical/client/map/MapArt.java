package com.mechanical.client.map;

import com.mechanical.Mechanical;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** The map's painted-in-code pictures: the parchment, the markers, and the inking of a region's picture. */
final class MapArt {
    static final int PARCHMENT_SIZE = 128, MARKER_SIZE = 48;
    private static final int PARCHMENT = 0xFFD9C397, INK = 0xFF4A3620;
    static final Identifier PAPER = id("paper"), ARROW = id("arrow"), PIN_RIM = id("pin_rim"), PIN_CORE = id("pin_core");
    private static boolean made;

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "map_art/" + name);
    }

    static void register(Identifier id, NativeImage image) {
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
    }

    /** Makes the fixed textures the first time a map is opened. */
    static void make() {
        if (made) return;
        made = true;
        NativeImage paper = new NativeImage(PARCHMENT_SIZE, PARCHMENT_SIZE, false);
        for (int y = 0; y < PARCHMENT_SIZE; y++)
            for (int x = 0; x < PARCHMENT_SIZE; x++) {
                // broad stains, finer mottling and grain; every layer wraps, so the sheet tiles without seams
                float n = (noise(x, y, 32) - 0.5F) * 18 + (noise(x, y, 8) - 0.5F) * 9 + (hash(x, y) - 0.5F) * 8;
                if (hash(x * 7 + 3, y * 13 + 1) > 0.992F) n -= 18; // flecks in the fibre
                paper.setPixel(x, y, shade(PARCHMENT, Math.round(n)));
            }
        register(PAPER, paper);

        // a navigation arrow pointing up: dark rim, cream edge, two red facets
        float[][] arrow = {{24, 2}, {42, 43}, {24, 33}, {6, 43}};
        NativeImage a = new NativeImage(MARKER_SIZE, MARKER_SIZE, true);
        for (int y = 0; y < MARKER_SIZE; y++)
            for (int x = 0; x < MARKER_SIZE; x++) {
                if (!inside(arrow, x + 0.5F, y + 0.5F)) continue;
                float edge = edgeDistance(arrow, x + 0.5F, y + 0.5F);
                a.setPixel(x, y, edge < 2.6F ? 0xFF1A0E08 : edge < 5.4F ? 0xFFFFF4DC : x < 24 ? 0xFFE05A38 : 0xFFAE3422);
            }
        register(ARROW, a);

        // a tribe mate: a rim that stays dark and cream, and a white core tinted with the tribe's colour when drawn
        NativeImage rim = new NativeImage(MARKER_SIZE, MARKER_SIZE, true), core = new NativeImage(MARKER_SIZE, MARKER_SIZE, true);
        for (int y = 0; y < MARKER_SIZE; y++)
            for (int x = 0; x < MARKER_SIZE; x++) {
                double r = Math.hypot(x + 0.5 - 24, y + 0.5 - 24);
                if (r <= 21 && r > 18) rim.setPixel(x, y, 0xFF1A0E08);
                else if (r <= 18 && r > 13) rim.setPixel(x, y, 0xFFFFF4DC);
                else if (r <= 13) core.setPixel(x, y, x + y < 44 ? 0xFFFFFFFF : 0xFFD8D8D8);
            }
        register(PIN_RIM, rim);
        register(PIN_CORE, core);
    }

    /**
     * Turns a region picture (map colours, transparent where unknown) into ink on parchment, in place: the colours are
     * warmed towards the paper, and where they meet the unknown the edge breaks up raggedly with a darker waterline.
     * shortcut: edges are only found within one region, so known land ending exactly on a region border stays straight;
     * pass the neighbours' edges in if that shows.
     */
    static void ink(NativeImage image, int worldX, int worldZ) {
        int w = image.getWidth(), h = image.getHeight(), far = 12;
        int[] d = new int[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) d[y * w + x] = ARGB.alpha(image.getPixel(x, y)) == 0 ? 0 : far;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (x > 0) d[i] = Math.min(d[i], d[i - 1] + 1);
                if (y > 0) d[i] = Math.min(d[i], d[i - w] + 1);
            }
        for (int y = h - 1; y >= 0; y--)
            for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x;
                if (x < w - 1) d[i] = Math.min(d[i], d[i + 1] + 1);
                if (y < h - 1) d[i] = Math.min(d[i], d[i + w] + 1);
            }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int dist = d[y * w + x];
                if (dist == 0) continue;
                int c = mix(image.getPixel(x, y), PARCHMENT, 0.16F);
                if (dist < far) {
                    float ragged = dist + (noise(worldX + x, worldZ + y, 6) - 0.5F) * 10 + (hash(worldX + x, worldZ + y) - 0.5F) * 2;
                    if (ragged < 3) {
                        image.setPixel(x, y, 0);
                        continue;
                    }
                    if (ragged < 5) c = mix(c, INK, 0.7F);
                    else if (ragged < 10) c = mix(c, PARCHMENT, (10 - ragged) / 7);
                }
                image.setPixel(x, y, c);
            }
    }

    private static int mix(int from, int to, float t) {
        return ARGB.color(255, Math.round(ARGB.red(from) + (ARGB.red(to) - ARGB.red(from)) * t),
                Math.round(ARGB.green(from) + (ARGB.green(to) - ARGB.green(from)) * t),
                Math.round(ARGB.blue(from) + (ARGB.blue(to) - ARGB.blue(from)) * t));
    }

    private static int shade(int c, int by) {
        return ARGB.color(255, Math.clamp(ARGB.red(c) + by, 0, 255), Math.clamp(ARGB.green(c) + by, 0, 255), Math.clamp(ARGB.blue(c) + by, 0, 255));
    }

    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) & 0xFFFF) / 65535F;
    }

    /** Smooth value noise with {@code cell}-pixel cells; on the parchment it wraps when the cell divides the sheet. */
    private static float noise(int x, int y, int cell) {
        int period = PARCHMENT_SIZE % cell == 0 ? PARCHMENT_SIZE / cell : 1 << 20;
        int gx = Math.floorDiv(x, cell), gy = Math.floorDiv(y, cell);
        float tx = Math.floorMod(x, cell) / (float) cell, ty = Math.floorMod(y, cell) / (float) cell;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        int x0 = Math.floorMod(gx, period), x1 = Math.floorMod(gx + 1, period), y0 = Math.floorMod(gy, period), y1 = Math.floorMod(gy + 1, period);
        float top = hash(x0, y0) + (hash(x1, y0) - hash(x0, y0)) * tx, bottom = hash(x0, y1) + (hash(x1, y1) - hash(x0, y1)) * tx;
        return top + (bottom - top) * ty;
    }

    private static boolean inside(float[][] poly, float x, float y) {
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++)
            if (poly[i][1] > y != poly[j][1] > y && x < (poly[j][0] - poly[i][0]) * (y - poly[i][1]) / (poly[j][1] - poly[i][1]) + poly[i][0]) in = !in;
        return in;
    }

    private static float edgeDistance(float[][] poly, float x, float y) {
        float best = Float.MAX_VALUE;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            float ax = poly[j][0], ay = poly[j][1], bx = poly[i][0] - ax, by = poly[i][1] - ay;
            float t = Math.clamp(((x - ax) * bx + (y - ay) * by) / (bx * bx + by * by), 0, 1);
            best = Math.min(best, (float) Math.hypot(x - ax - t * bx, y - ay - t * by));
        }
        return best;
    }

    private MapArt() {}
}
