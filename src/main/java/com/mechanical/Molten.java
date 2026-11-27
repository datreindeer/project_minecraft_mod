package com.mechanical;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Every metal and melt in the mod, from the one table {@link #TABLE} (written by scripts/gen_metals.py, docs/metals.md):
 * melting point, density, colour, its ingot/nugget/block/plate, what melts into it and how much, the clay-mold and
 * Molding Flask casts, and the alloys. Read once at start-up: items and block states are fixed then.
 * {@link #values()} is the density order, heaviest first: a High Furnace pool's layers ({@link MeltPool}) lie in it from the
 * bottom up, Slag (the lightest) on top. The bronze-age crucible, Smelting Furnace and Clay Molds know the melts that have a
 * {@link Metal} constant ({@link #metal()}). Amounts are in mB: 1 unit = 100 mB, 1 quarter = 25, 1 ingot = 200,
 * 1 nugget 22 (rounded down), 1 block 1800.
 */
public final class Molten implements Comparable<Molten> {
    public static final int INGOT = 200, NUGGET = 22, BLOCK = 1800, UNIT = 100, QUARTER = 25;
    public static final String TABLE = "/data/mechanical/metals.json";

    private static final List<Molten> ALL = new ArrayList<>();
    private static final List<MeltPool.Alloy> ALLOYS = new ArrayList<>();
    private static @Nullable Crucible crucible;
    private static int registered;

    static {
        try (Reader r = new InputStreamReader(Molten.class.getResourceAsStream(TABLE), StandardCharsets.UTF_8)) {
            load(JsonParser.parseReader(r).getAsJsonObject());
        } catch (Exception e) {
            throw new IllegalStateException("could not read the metal table " + TABLE, e);
        }
    }

    // the melts the code names (everything else is only in the table)
    public static final Molten GOLD = get("gold"), LEAD = get("lead"), SILVER = get("silver"), COPPER = get("copper"), BRONZE = get("bronze"),
            POOR_BRONZE = get("poor_bronze"), STEEL = get("steel"), WROUGHT_IRON = get("wrought_iron"), TIN = get("tin"), PIG_IRON = get("pig_iron"),
            ELECTRUM = get("electrum"),
            BASALT = get("basalt"), GLASS = get("glass"), OBSIDIAN = get("obsidian"), SLAG = get("slag");

    public final String id;
    /** Degrees it needs to melt (and to stay runny enough to pour). */
    public final int melt;
    /** g/cm³: the heavier lies lower in a pool. */
    public final float density;
    /** Its glowing colour, ARGB. */
    public final int color;
    /** Glass, basalt, obsidian and slag: not metals (no ingots, no nuggets; a basin casts 250 mB of it). */
    public final boolean nonMetal;
    /** Poor Bronze: what it casts is marked {@link Bronze#POOR}. */
    public final boolean poor;
    /** Iron ore is reduced into it at this heat (0: none is). */
    public final int reduceHeat;
    private final @Nullable Identifier ingot, nugget, block, plate, coin;
    /** Items that melt into it in a High Furnace: mB each. */
    private final Map<Identifier, Integer> melts = new HashMap<>();
    /** Iron ores reduced into it: {mB, mB with flux}. */
    private final Map<Identifier, int[]> reduce = new HashMap<>();
    /** Ore items a Crucible / Smelting Furnace takes for it: units each. */
    private final Map<Identifier, Integer> crucibleOre = new java.util.LinkedHashMap<>(); // table order: crushed ore, concentrate, ...
    private final Map<String, Identifier> casts = new HashMap<>(), flask = new HashMap<>();
    private final int order;

    /** The crucible's alloy rule (bronze): by share of the second part, in percent. */
    public record Crucible(Molten base, Molten addition, Molten result, Molten poorResult, double goodMin, double goodMax, double poorMin, double poorMax) {}

    private Molten(JsonObject o) {
        id = o.get("id").getAsString();
        melt = o.get("melt").getAsInt();
        density = o.get("density").getAsFloat();
        color = 0xFF000000 | Integer.parseInt(o.get("color").getAsString(), 16);
        nonMetal = o.has("nonmetal") && o.get("nonmetal").getAsBoolean();
        poor = o.has("poor") && o.get("poor").getAsBoolean();
        reduceHeat = o.has("reduce_heat") ? o.get("reduce_heat").getAsInt() : 0;
        ingot = id(o, "ingot");
        nugget = id(o, "nugget");
        block = id(o, "block");
        plate = id(o, "plate");
        coin = id(o, "coin");
        ints(o, "melts", melts);
        ints(o, "crucible", crucibleOre);
        if (o.has("reduce")) for (var e : o.getAsJsonObject("reduce").entrySet()) {
            var v = e.getValue().getAsJsonArray();
            reduce.put(Identifier.parse(e.getKey()), new int[] {v.get(0).getAsInt(), v.get(1).getAsInt()});
        }
        ids(o, "casts", casts);
        ids(o, "flask", flask);
        order = registered++;
    }

    private static @Nullable Identifier id(JsonObject o, String key) {
        return o.has(key) ? Identifier.parse(o.get(key).getAsString()) : null;
    }

    private static void ints(JsonObject o, String key, Map<Identifier, Integer> into) {
        if (o.has(key)) for (var e : o.getAsJsonObject(key).entrySet()) into.put(Identifier.parse(e.getKey()), e.getValue().getAsInt());
    }

    private static void ids(JsonObject o, String key, Map<String, Identifier> into) {
        if (o.has(key)) for (var e : o.getAsJsonObject(key).entrySet()) into.put(e.getKey(), Identifier.parse(e.getValue().getAsString()));
    }

    /** What {@link #load} added (to take it out again: {@link #unload}, game tests). */
    public record Added(List<Molten> metals, List<MeltPool.Alloy> alloys) {}

    /** Adds the metals and alloys of a table (the start-up table, or a game test's). */
    static synchronized Added load(JsonObject table) {
        List<Molten> metals = new ArrayList<>();
        for (JsonElement e : table.getAsJsonArray("metals")) {
            Molten m = new Molten(e.getAsJsonObject());
            if (byId(m.id) != null) throw new IllegalStateException("metal " + m.id + " is in the table twice");
            metals.add(m);
            ALL.add(m);
        }
        Collections.sort(ALL);
        List<MeltPool.Alloy> alloys = new ArrayList<>();
        for (JsonElement e : table.getAsJsonArray("alloys")) {
            JsonObject a = e.getAsJsonObject();
            var first = a.getAsJsonArray("parts").get(0).getAsJsonArray();
            var second = a.getAsJsonArray("parts").get(1).getAsJsonArray();
            MeltPool.Alloy alloy = new MeltPool.Alloy(get(first.get(0).getAsString()), first.get(1).getAsInt(), get(second.get(0).getAsString()),
                    second.get(1).getAsInt(), get(a.get("result").getAsString()), a.get("heat").getAsInt());
            alloys.add(alloy);
            ALLOYS.add(alloy);
            if (a.has("crucible")) {
                if (crucible != null) throw new IllegalStateException("only one crucible alloy is supported (CrucibleContents, FurnaceMelt)");
                JsonObject c = a.getAsJsonObject("crucible");
                var good = c.getAsJsonArray("good");
                var bad = c.getAsJsonArray("poor");
                crucible = new Crucible(alloy.a(), alloy.b(), alloy.result(), get(c.get("poor_result").getAsString()),
                        good.get(0).getAsDouble(), good.get(1).getAsDouble(), bad.get(0).getAsDouble(), bad.get(1).getAsDouble());
            }
        }
        return new Added(metals, alloys);
    }

    static synchronized void unload(Added added) {
        ALL.removeAll(added.metals());
        ALLOYS.removeAll(added.alloys());
    }

    /** Every melt, heaviest first. */
    public static List<Molten> values() {
        return Collections.unmodifiableList(ALL);
    }

    /** Its place in {@link #values()} (the screen's layer buttons). */
    public int ordinal() {
        return ALL.indexOf(this);
    }

    public static List<MeltPool.Alloy> alloys() {
        return Collections.unmodifiableList(ALLOYS);
    }

    public static Crucible crucibleAlloy() {
        if (crucible == null) throw new IllegalStateException("the metal table has no crucible alloy");
        return crucible;
    }

    public static @Nullable Molten byId(String id) {
        for (Molten m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    private static Molten get(String id) {
        Molten m = byId(id);
        if (m == null) throw new IllegalStateException("the metal table " + TABLE + " has no " + id);
        return m;
    }

    /** The High Furnace melt for a bronze-age metal. */
    public static Molten of(Metal metal) {
        return get(metal.getSerializedName());
    }

    /** Its crucible metal (copper, tin, bronze, poor bronze), or null. */
    public @Nullable Metal metal() {
        return Metal.byId(id);
    }

    static Item item(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new IllegalStateException("the metal table names an unknown item " + id));
    }

    private ItemStack stack(@Nullable Identifier item) {
        if (item == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(item(item));
        if (poor) Bronze.markPoor(out);
        return out;
    }

    /** Every item this metal casts into. */
    public List<Item> castItems() {
        return casts.values().stream().map(Molten::item).toList();
    }

    /** One ingot (marked poor for Poor Bronze), or empty. */
    public ItemStack ingot() {
        return stack(ingot);
    }

    /** One nugget, or empty. */
    public ItemStack nugget() {
        return stack(nugget);
    }

    /** What the Foundry Basin casts it into, or empty. */
    public ItemStack block() {
        return stack(block);
    }

    /** What the Iron Plate Mold casts it into, or empty. */
    public ItemStack plate() {
        return stack(plate);
    }

    /** The coin its ingot mints into, or null. */
    public @Nullable Item coin() {
        return coin == null ? null : item(coin);
    }

    /** Is this its ingot (a poor ingot is Poor Bronze's, a plain one Bronze's)? */
    public boolean isIngot(ItemStack s) {
        return ingot != null && s.is(item(ingot)) && Bronze.isPoor(s) == poor;
    }

    /** mB one of these items melts into in a High Furnace (0: it doesn't melt into this). A piece marked poor (ingot,
     * nugget) is Poor Bronze's, an unmarked one Bronze's. */
    public int melts(ItemStack s) {
        Integer mb = melts.get(BuiltInRegistries.ITEM.getKey(s.getItem()));
        if (mb == null || Bronze.isPoor(s) != poor) return 0;
        return mb;
    }

    /** Iron ore: {mB, mB with flux}, or null. */
    public int @Nullable [] reduces(ItemStack s) {
        return reduce.get(BuiltInRegistries.ITEM.getKey(s.getItem()));
    }

    /** Units of this metal one ore item gives a Crucible (0: none). */
    public int crucibleUnits(ItemStack s) {
        return crucibleOre.getOrDefault(BuiltInRegistries.ITEM.getKey(s.getItem()), 0);
    }

    /** The ore item worth {@code units} in a Crucible (an emptied crucible gives its ore back), or null. */
    public @Nullable Item crucibleOre(int units) {
        for (var e : crucibleOre.entrySet()) if (e.getValue() == units) return item(e.getKey());
        return null;
    }

    /** The item a clay mold of this shape casts (rough), or null if it can't be cast in it. */
    public @Nullable Item cast(ClayMoldBlock.Shape shape) {
        Identifier i = casts.get(shape.getSerializedName());
        return i == null ? null : item(i);
    }

    public boolean castsAnything() {
        return !casts.isEmpty();
    }

    /** What a Molding Flask pattern without a clay shape casts of it (cast iron pieces), or null. */
    public @Nullable Item flask(String pattern) {
        Identifier i = flask.get(pattern);
        return i == null ? null : item(i);
    }

    /** Every item the table names for it (a game test checks they all exist). */
    public List<Identifier> items() {
        List<Identifier> out = new ArrayList<>(melts.keySet());
        out.addAll(reduce.keySet());
        out.addAll(crucibleOre.keySet());
        out.addAll(casts.values());
        out.addAll(flask.values());
        for (Identifier i : new Identifier[] {ingot, nugget, block, plate, coin}) if (i != null) out.add(i);
        return out;
    }

    /** Heaviest first; equal densities keep the table's order. */
    @Override
    public int compareTo(Molten o) {
        int c = Float.compare(o.density, density);
        return c != 0 ? c : Integer.compare(order, o.order);
    }

    @Override
    public String toString() {
        return id;
    }

    /** Every Metal constant has its row, and every crucible / clay-mold metal in the table has its constant (run at start-up). */
    static void selfCheck() {
        for (Metal m : Metal.values()) get(m.getSerializedName());
        for (Molten m : ALL)
            if ((m.castsAnything() || !m.crucibleOre.isEmpty()) && m.metal() == null)
                throw new IllegalStateException("metal " + m.id + " casts in clay molds or goes in a crucible: add it to the Metal enum");
    }
}
