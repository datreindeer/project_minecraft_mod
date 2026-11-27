package com.mechanical.client.map;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

        icons();
    }

    /** Icon textures are this many pixels square, showing a 32x32 drawing box (the dashboard's 24x24 shapes plus their rim). */
    static final int ICON_SIZE = 64;
    static final Identifier PIT_RING = id("pit_ring"), GLOW = id("glow");
    private static final int HALO = 0xEB120D09;

    static Identifier icon(String kind) {
        return id("icon_" + kind);
    }

    /** The same shapes the web dashboard draws (dashboard/app.js MARKS and fires). */
    private static void icons() {
        shapes("ancient_oak", new String[][] {
                {"M-2.5 11 L-1.8 2 L-6 -1 L-4 -2 L-1 0 L1 0 L4 -2 L6 -1 L1.8 2 L2.5 11 L6 12 L-6 12 Z", "8a6238"},
                {"M-5 3 C-10 4 -13 -1 -10 -4 C-12 -8 -7 -11 -4 -9 C-3 -13 4 -13 5 -9 C9 -11 13 -7 10 -4 C13 0 9 4 5 2 C3 4 -3 4 -5 3 Z", "5f9a48"},
                {"M-6 -5 C-6 -8 -2 -9 -1 -7 C-1 -5 -4 -4 -6 -5 Z M2 -9 C4 -10 7 -8 5 -6 C3 -6 2 -7 2 -9 Z", "9cc96a"}});
        shapes("waystone", new String[][] {
                {"M-8 12 L-6 8 L6 8 L8 12 Z", "6f675b"},
                {"M-5 8 L-4.5 -7 L0 -12 L4.5 -7 L5 8 Z", "b9b1a2"},
                {"M0 -7 L2.8 -3 L0 1 L-2.8 -3 Z M-1 2 L1 2 L1 6 L-1 6 Z", "7fe0e0"}});
        shapes("tribe_village", new String[][] {
                {"M-11 10 L0 -9 L11 10 Z", "c9955c"},
                {"M-4 10 L0 1 L4 10 Z", "3b2a1c"},
                {"M0 -9 L-4 -13 L-2.8 -13.6 L0 -10.5 L2.8 -13.6 L4 -13 Z", "e8dcc0"}});

        paint(icon("campfire"), g -> {
            rim(g, flame(0, 0, 9));
            fire(g, 0, 0, 9, 2.7);
        });
        // a Fire Pit: the flame in a ring of stones on a dark disc; the big one has more of them
        for (int stones : new int[] {8, 10})
            paint(icon(stones == 10 ? "big_fire_pit" : "fire_pit"), g -> {
                g.setColor(new java.awt.Color(HALO, true));
                g.fill(new java.awt.geom.Ellipse2D.Double(-15, -15, 30, 30));
                for (int i = 0; i < stones; i++) {
                    double a = i * Math.PI * 2 / stones, r = stones == 10 ? 2.4 : 2.8;
                    var stone = new java.awt.geom.Ellipse2D.Double(Math.cos(a) * 9 - r, Math.sin(a) * 9 - r, r * 2, r * 2);
                    g.setColor(new java.awt.Color(0xA59C8D));
                    g.fill(stone);
                    g.setColor(new java.awt.Color(0x3B332A));
                    g.setStroke(new java.awt.BasicStroke(0.8F));
                    g.draw(stone);
                }
                fire(g, 0, 0.8, 5.5, 2.4);
            });
        // white, tinted with the tribe's colour when drawn
        paint(PIT_RING, g -> {
            g.setColor(java.awt.Color.WHITE);
            g.setStroke(new java.awt.BasicStroke(2.2F));
            g.draw(new java.awt.geom.Ellipse2D.Double(-13.6, -13.6, 27.2, 27.2));
        });
        // a soft ring of light round the mark under the cursor
        paint(GLOW, g -> {
            g.setColor(new java.awt.Color(255, 216, 112, 34));
            for (int r = 16; r > 9; r--) g.fill(new java.awt.geom.Ellipse2D.Double(-r, -r, r * 2, r * 2));
        });
    }

    private static void rim(java.awt.Graphics2D g, java.awt.Shape shape) {
        g.setColor(new java.awt.Color(HALO, true));
        g.setStroke(new java.awt.BasicStroke(3.5F, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    private static void fire(java.awt.Graphics2D g, double x, double y, double r, double drop) {
        g.setColor(new java.awt.Color(0xE2683C));
        g.fill(flame(x, y, r));
        g.setColor(new java.awt.Color(0xF2C25A));
        g.fill(flame(x, y + drop, r * 0.5));
    }

    private static java.awt.geom.Path2D flame(double x, double y, double r) {
        var p = new java.awt.geom.Path2D.Double();
        p.moveTo(x, y - r * 1.25);
        p.curveTo(x + r * .35, y - r * .55, x + r * .9, y - r * .2, x + r * .8, y + r * .35);
        p.curveTo(x + r * .8, y + r * 1.4, x - r * .8, y + r * 1.4, x - r * .8, y + r * .3);
        p.curveTo(x - r * .9, y - r * .1, x - r * .35, y - r * .5, x - r * .15, y - r * .9);
        p.curveTo(x, y - r * .7, x, y - r, x, y - r * 1.25);
        p.closePath();
        return p;
    }

    /** SVG path data using only absolute M, L, C and Z, each part {data, rrggbb}, all rimmed dark under the fills. */
    private static void shapes(String kind, String[][] parts) {
        List<java.awt.geom.Path2D> paths = new ArrayList<>();
        for (String[] part : parts) {
            var p = new java.awt.geom.Path2D.Double();
            var words = new java.util.StringTokenizer(part[0].replaceAll("([MLCZ])", " $1 "));
            char command = ' ';
            while (words.hasMoreTokens()) {
                String word = words.nextToken();
                if (Character.isLetter(word.charAt(0))) {
                    command = word.charAt(0);
                    if (command == 'Z') p.closePath();
                    continue;
                }
                double x = Double.parseDouble(word), y = Double.parseDouble(words.nextToken());
                if (command == 'M') p.moveTo(x, y);
                else if (command == 'L') p.lineTo(x, y);
                else p.curveTo(x, y, Double.parseDouble(words.nextToken()), Double.parseDouble(words.nextToken()),
                        Double.parseDouble(words.nextToken()), Double.parseDouble(words.nextToken()));
            }
            paths.add(p);
        }
        paint(icon(kind), g -> {
            paths.forEach(p -> rim(g, p));
            for (int i = 0; i < parts.length; i++) {
                g.setColor(new java.awt.Color(Integer.parseInt(parts[i][1], 16)));
                g.fill(paths.get(i));
            }
        });
    }

    private static void paint(Identifier id, Consumer<java.awt.Graphics2D> drawing) {
        var picture = new java.awt.image.BufferedImage(ICON_SIZE, ICON_SIZE, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g = picture.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(ICON_SIZE / 2.0, ICON_SIZE / 2.0);
        g.scale(ICON_SIZE / 32.0, ICON_SIZE / 32.0);
        drawing.accept(g);
        g.dispose();
        NativeImage image = new NativeImage(ICON_SIZE, ICON_SIZE, true);
        for (int y = 0; y < ICON_SIZE; y++)
            for (int x = 0; x < ICON_SIZE; x++) image.setPixel(x, y, picture.getRGB(x, y));
        register(id, image);
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
