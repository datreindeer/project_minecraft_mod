package com.mechanical.client.map;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** What the server marks on the map ({@code MapNetwork.Features}), with each land cut into the rectangles and edges it is drawn from. */
record MapMarks(List<Land> lands, List<Fire> fires, List<Place> places) {
    static final MapMarks NONE = new MapMarks(List.of(), List.of(), List.of());
    private static final int SAND = 0xE8DCC0;

    enum Kind { TRIBE, TRIBE_VILLAGE, TRIBE_CAMP, VILLAGE }

    /** A rectangle in blocks, from (x0, z0) up to (x1, z1). */
    record Rect(int x0, int z0, int x1, int z1) {}

    /** A straight bit of outline; {@code in} is +1 or -1: which way along the other axis the land lies. */
    record Edge(int x0, int z0, int x1, int z1, int in) {}

    record Shape(List<int[]> rings, List<Rect> fill, List<Edge> edges) {
        static Shape of(List<int[]> rings) {
            List<Edge> edges = new ArrayList<>();
            for (int[] r : rings)
                for (int i = 0; i < r.length; i += 2) {
                    int ax = r[i], az = r[i + 1], bx = r[(i + 2) % r.length], bz = r[(i + 3) % r.length];
                    int x0 = Math.min(ax, bx), x1 = Math.max(ax, bx), z0 = Math.min(az, bz), z1 = Math.max(az, bz);
                    boolean flat = z0 == z1;
                    double mx = (x0 + x1) / 2.0, mz = (z0 + z1) / 2.0;
                    edges.add(new Edge(x0, z0, x1, z1, contains(rings, flat ? mx : mx + 0.5, flat ? mz + 0.5 : mz) ? 1 : -1));
                }
            // the outlines only turn at right angles: between two neighbouring corner rows the land is plain strips
            int[] rows = rings.stream().flatMapToInt(r -> java.util.stream.IntStream.range(0, r.length / 2).map(i -> r[i * 2 + 1])).sorted().distinct().toArray();
            List<Rect> fill = new ArrayList<>();
            for (int k = 0; k + 1 < rows.length; k++) {
                int top = rows[k], bottom = rows[k + 1];
                int[] cuts = edges.stream().filter(e -> e.x0 == e.x1 && e.z0 <= top && e.z1 >= bottom).mapToInt(Edge::x0).sorted().toArray();
                for (int i = 0; i + 1 < cuts.length; i += 2) fill.add(new Rect(cuts[i], top, cuts[i + 1], bottom));
            }
            return new Shape(rings, fill, edges);
        }

        /** Even-odd over every ring, so holes are outside. */
        static boolean contains(List<int[]> rings, double x, double z) {
            boolean in = false;
            for (int[] r : rings)
                for (int i = 0, j = r.length - 2; i < r.length; j = i, i += 2)
                    if (r[i + 1] > z != r[j + 1] > z && x < (r[j] - r[i]) * (z - r[i + 1]) / (double) (r[j + 1] - r[i + 1]) + r[i]) in = !in;
            return in;
        }

        boolean contains(double x, double z) {
            return contains(rings, x, z);
        }

        /** {x0, z0, x1, z1} round the whole land. */
        int[] bounds() {
            int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (Rect r : fill) {
                b[0] = Math.min(b[0], r.x0);
                b[1] = Math.min(b[1], r.z0);
                b[2] = Math.max(b[2], r.x1);
                b[3] = Math.max(b[3], r.z1);
            }
            return b;
        }
    }

    /** {@code colour} is 0xRRGGBB; {@code loaded} may be null. */
    record Land(Kind kind, String id, String name, int colour, int chunks, Shape shape, Shape loaded, int labelX, int labelZ) {}

    record Fire(int x, int z, String kind, int colour) {}

    record Place(String kind, int x, int z) {}

    /** Lands in drawing order, the later on top: villages, NPC lands, then tribes. */
    static MapMarks parse(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        List<Land> lands = new ArrayList<>();
        for (JsonElement e : array(o, "villages")) lands.add(land(e.getAsJsonObject(), Kind.VILLAGE, "", SAND));
        for (JsonElement e : array(o, "tribeVillages")) {
            JsonObject v = e.getAsJsonObject();
            String id = v.get("id").getAsString();
            lands.add(land(v, id.startsWith("camp:") ? Kind.TRIBE_CAMP : Kind.TRIBE_VILLAGE, id, colour(v)));
        }
        for (JsonElement e : array(o, "tribes")) {
            JsonObject v = e.getAsJsonObject();
            lands.add(land(v, Kind.TRIBE, v.get("id").getAsString(), colour(v)));
        }
        List<Fire> fires = new ArrayList<>();
        for (JsonElement e : array(o, "fires")) {
            JsonObject f = e.getAsJsonObject();
            String tribe = f.get("tribe").isJsonNull() ? null : f.get("tribe").getAsString();
            int colour = lands.stream().filter(l -> l.id.equals(tribe)).mapToInt(Land::colour).findFirst().orElse(-1);
            fires.add(new Fire(f.get("x").getAsInt(), f.get("z").getAsInt(), f.get("kind").getAsString(), colour));
        }
        List<Place> places = new ArrayList<>();
        for (JsonElement e : array(o, "landmarks")) {
            JsonObject p = e.getAsJsonObject();
            places.add(new Place(p.get("kind").getAsString(), p.get("x").getAsInt(), p.get("z").getAsInt()));
        }
        return new MapMarks(lands, fires, places);
    }

    private static Land land(JsonObject o, Kind kind, String id, int colour) {
        JsonObject label = o.getAsJsonObject("label");
        List<int[]> loaded = rings(array(o, "loaded"));
        return new Land(kind, id, o.get("name").getAsString(), colour, o.has("chunks") ? o.get("chunks").getAsInt() : 0,
                Shape.of(rings(array(o, "rings"))), loaded.isEmpty() ? null : Shape.of(loaded), label.get("x").getAsInt(), label.get("z").getAsInt());
    }

    private static JsonArray array(JsonObject o, String name) {
        return o.has(name) ? o.getAsJsonArray(name) : new JsonArray();
    }

    private static List<int[]> rings(JsonArray in) {
        List<int[]> out = new ArrayList<>();
        for (JsonElement ring : in) out.add(ring.getAsJsonArray().asList().stream().mapToInt(JsonElement::getAsInt).toArray());
        return out;
    }

    private static int colour(JsonObject o) {
        return Integer.parseInt(o.get("colour").getAsString().substring(1), 16);
    }
}
