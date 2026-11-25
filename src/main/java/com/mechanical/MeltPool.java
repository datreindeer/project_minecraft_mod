package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.nbt.CompoundTag;

import org.jspecify.annotations.Nullable;

/**
 * A High Furnace's pool (docs/steel-furnace-plan.md section 5): every melt in it is its own layer, in mB, lying by
 * density ({@link Molten#values()} order, heaviest at the bottom, Slag on top). Alloys ({@link Molten#alloys()}, the metal
 * table) mix themselves while both parts are in and the furnace is hot enough, {@link #MIX_PER_TICK} mB a tick per recipe,
 * in fixed proportions, and what is left over of one part stays as its own layer: 9 Copper + 1 Tin -> 10 Bronze (950°),
 * 1 Pig Iron + 2 Wrought Iron -> 3 Steel (1500°). Saved by melt id ({@link #save}); pools saved before 0.208.0 by the old
 * enum's order load through {@link #loadLegacy}.
 * There is no Poor Bronze from mixing here: the furnace mixes exactly. Everything only moves (add, drain, mix trades
 * parts for the same total), so nothing can be made from nothing; {@link #selfCheck} proves it at start-up.
 * The Smelting Furnace keeps its own rules ({@link FurnaceMelt}).
 */
public final class MeltPool {
    /** mB mixed per tick per recipe (a multiple of every recipe's parts). */
    public static final int MIX_PER_TICK = 20;

    /** An alloy recipe: {@code a} parts of one and {@code b} of the other make {@code a + b} of the result, at {@code heat}°. */
    public record Alloy(Molten a, int partsA, Molten b, int partsB, Molten result, int heat) {}

    /** The Molten enum's order before 0.208.0: what the old saves' numbers mean. */
    private static final String[] LEGACY = {"gold", "lead", "silver", "copper", "bronze", "poor_bronze", "pig_iron", "steel", "wrought_iron",
            "tin", "glass", "basalt", "obsidian", "slag"};

    private final TreeMap<Molten, Integer> mb = new TreeMap<>();
    private int capacity;

    public MeltPool(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    /** The pool's size changes with the furnace's; what is in it stays (it may then be over-full and takes nothing). */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int get(Molten m) {
        return mb.getOrDefault(m, 0);
    }

    public int total() {
        int t = 0;
        for (int v : mb.values()) t += v;
        return t;
    }

    public int room() {
        return Math.max(0, capacity - total());
    }

    public boolean isEmpty() {
        return mb.isEmpty();
    }

    /** The layers from the bottom up, each with its mB. */
    public List<Map.Entry<Molten, Integer>> layers() {
        return new ArrayList<>(mb.entrySet());
    }

    /** The bottom layer (what a taphole pours unless another is chosen), or null. */
    public @Nullable Molten bottom() {
        return mb.isEmpty() ? null : mb.keySet().iterator().next();
    }

    /** Puts in up to {@code amount} mB (as much as there is room for); returns how much went in. */
    public int add(Molten m, int amount) {
        int in = Math.min(amount, room());
        if (in > 0) mb.merge(m, in, Integer::sum);
        return in;
    }

    /** Takes out up to {@code amount} mB of one layer; returns how much came out. */
    public int drain(Molten m, int amount) {
        int have = get(m), out = Math.min(have, Math.max(0, amount));
        if (out == have) mb.remove(m);
        else if (out > 0) mb.put(m, have - out);
        return out;
    }

    /** One tick of mixing at {@code temperature}°: every recipe whose parts are both in mixes up to {@link #MIX_PER_TICK} mB. */
    public boolean mix(float temperature) {
        boolean any = false;
        for (Alloy r : Molten.alloys()) {
            if (temperature < r.heat) continue;
            int parts = r.partsA + r.partsB, batch = MIX_PER_TICK / parts;
            int n = Math.min(batch, Math.min(get(r.a) / r.partsA, get(r.b) / r.partsB));
            if (n <= 0) continue;
            drain(r.a, n * r.partsA);
            drain(r.b, n * r.partsB);
            mb.merge(r.result, n * parts, Integer::sum);
            any = true;
        }
        return any;
    }

    /** id -> mB (also a Furnace Salamander's form). */
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        mb.forEach((m, v) -> t.putInt(m.id, v));
        return t;
    }

    /** A melt no longer in the table is dropped. */
    public void load(CompoundTag t) {
        mb.clear();
        for (String id : t.keySet()) {
            Molten m = Molten.byId(id);
            int v = t.getIntOr(id, 0);
            if (m != null && v > 0) mb.put(m, v);
        }
    }

    /** A pool saved before 0.208.0: {ordinal, mB} pairs in the old fixed order ({@link #LEGACY}). */
    public void loadLegacy(int[] data) {
        mb.clear();
        for (int i = 0; i + 1 < data.length; i += 2) {
            Molten m = data[i] >= 0 && data[i] < LEGACY.length ? Molten.byId(LEGACY[data[i]]) : null;
            if (m != null && data[i + 1] > 0) mb.merge(m, data[i + 1], Integer::sum);
        }
    }

    /** Layers, both alloy recipes, leftovers and conservation (run at start-up from {@link HighFurnace#init}). */
    public static void selfCheck() {
        MeltPool p = new MeltPool(8000);
        p.add(Molten.SLAG, 100);
        p.add(Molten.TIN, 200);
        p.add(Molten.COPPER, 2000); // 10 ingots of copper, 1 of tin: 1800 + 200 -> 2000 Bronze, 200 Copper left
        check(p.bottom() == Molten.COPPER && p.layers().getLast().getKey() == Molten.SLAG, "layers lie by density, slag on top");
        int before = p.total();
        for (int t = 0; t < 1000; t++) p.mix(900); // too cold for bronze
        check(p.get(Molten.BRONZE) == 0, "no bronze below 950°");
        for (int t = 0; t < 1000; t++) p.mix(1000);
        check(p.get(Molten.BRONZE) == 2000 && p.get(Molten.COPPER) == 200 && p.get(Molten.TIN) == 0, "9:1 bronze, copper left: " + p.layers());
        check(p.total() == before, "mixing makes and loses nothing");
        MeltPool s = new MeltPool(8000);
        s.add(Molten.PIG_IRON, 200);
        s.add(Molten.WROUGHT_IRON, 500);
        for (int t = 0; t < 1000; t++) s.mix(1500);
        check(s.get(Molten.STEEL) == 600 && s.get(Molten.WROUGHT_IRON) == 100 && s.get(Molten.PIG_IRON) == 0, "1:2 steel, leftovers stay: " + s.layers());
        check(s.total() == 700, "steel: nothing made or lost");
        MeltPool f = new MeltPool(1000);
        check(f.add(Molten.GOLD, 1500) == 1000 && f.add(Molten.LEAD, 1) == 0, "a full pool takes nothing more");
        check(f.drain(Molten.GOLD, 400) == 400 && f.get(Molten.GOLD) == 600, "drain takes what it says");
        MeltPool l = new MeltPool(1000);
        l.load(f.save());
        check(l.get(Molten.GOLD) == 600 && l.total() == 600, "save and load");
        l.loadLegacy(new int[] {6, 300, 13, 50, 9, 100}); // pig iron, slag, tin in the old numbers
        check(l.get(Molten.PIG_IRON) == 300 && l.get(Molten.SLAG) == 50 && l.get(Molten.TIN) == 100, "old saves load: " + l.layers());
        check(l.layers().getLast().getKey() == Molten.SLAG && l.bottom() == Molten.TIN, "layers by density: tin (7.29) under pig iron (7.1): " + l.layers());
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException("MeltPool self-check failed: " + what);
    }
}
