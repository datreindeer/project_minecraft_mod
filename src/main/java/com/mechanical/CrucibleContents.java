package com.mechanical;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a crucible holds: units per metal id ({@link Metal}). On a Crucible it is ore (by the metal it yields), on a
 * Crucible of Molten/Solid Metal it is the melted, alloyed metal. One unit = one Crushed Copper Ore.
 */
public record CrucibleContents(Map<String, Integer> units) {
    /** 10 units, so 9 copper : 1 tin (10 % tin) fits. */
    public static final int CAPACITY = 10;
    /** Units in one washed Copper / Tin Concentrate (crushed ore is 1). */
    public static final int CONCENTRATE = 2;
    public static final CrucibleContents EMPTY = new CrucibleContents(Map.of());
    public static final Codec<CrucibleContents> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(CrucibleContents::new, CrucibleContents::units);
    public static final StreamCodec<ByteBuf, CrucibleContents> STREAM_CODEC = ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new,
            ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT).map(CrucibleContents::new, CrucibleContents::units);

    public CrucibleContents {
        units = Map.copyOf(new TreeMap<>(units)); // sorted copy: equal contents compare (and stack) equal
    }

    public int total() {
        return units.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    public CrucibleContents add(Metal metal, int n) {
        Map<String, Integer> out = new HashMap<>(units);
        out.merge(metal.getSerializedName(), n, Integer::sum);
        out.values().removeIf(v -> v <= 0);
        return new CrucibleContents(out);
    }

    public int units(Metal metal) {
        return units.getOrDefault(metal.getSerializedName(), 0);
    }

    /** Can ore / ingots of this metal go in? Copper and tin mix; melted bronze only takes more of the same bronze. */
    public boolean canAdd(Metal metal) {
        if (isEmpty()) return true;
        if (metal.isAlloy()) return units.size() == 1 && units(metal) > 0;
        return units.keySet().stream().noneMatch(k -> Metal.byId(k).isAlloy());
    }

    /** Tin share of copper + tin in percent (0 without tin): the share of the crucible alloy's second part. */
    public float tinPercent() {
        Molten.Crucible r = Molten.crucibleAlloy();
        int cu = units(r.base().metal()), sn = units(r.addition().metal());
        return cu + sn == 0 ? 0 : sn * 100F / (cu + sn);
    }

    /**
     * The metal this melts into, or null if it makes nothing castable (tin alone). Copper + tin by the table's alloy rule
     * ({@link Molten#crucibleAlloy}): 9-11.5 % tin is Bronze, 8-15 % Poor Bronze (both melt at 950°); any other share
     * melts as Copper at 1085° and the tin is lost as slag (see {@link #melted}).
     */
    public Metal alloy() {
        if (units.size() == 1) {
            Metal only = Metal.byId(units.keySet().iterator().next());
            return only.castsAnything() ? only : null;
        }
        Molten.Crucible r = Molten.crucibleAlloy();
        int cu = units(r.base().metal()), sn = units(r.addition().metal());
        if (cu == 0 || cu + sn != total()) return null;
        return alloy(cu, sn);
    }

    /** The crucible alloy rule on {@code copper} units of its first part and {@code tin} of its second. */
    public static Metal alloy(int copper, int tin) {
        Molten.Crucible r = Molten.crucibleAlloy();
        double total = copper + tin, pct = tin * 100.0;
        if (pct >= r.goodMin() * total && pct <= r.goodMax() * total) return r.result().metal();
        if (pct >= r.poorMin() * total && pct <= r.poorMax() * total) return r.poorResult().metal();
        return r.base().metal();
    }

    /** What the Crucible of Molten Metal holds: all units as the alloy; copper with the wrong share of tin loses the tin. */
    public CrucibleContents melted() {
        Metal metal = alloy();
        if (metal == null) return EMPTY;
        int n = metal == Molten.crucibleAlloy().base().metal() ? units(metal) : total();
        return new CrucibleContents(Map.of(metal.getSerializedName(), n));
    }

    /** Thrown on load if the alloy rules drift from what the docs promise. */
    static void selfCheck() {
        check(alloy(9, 1) == Metal.BRONZE && alloy(8, 1) == Metal.BRONZE, "9:1 and 8:1 are bronze");
        check(alloy(7, 1) == Metal.POOR_BRONZE && alloy(6, 1) == Metal.POOR_BRONZE, "7:1 and 6:1 are poor bronze");
        check(alloy(5, 1) == Metal.COPPER && alloy(12, 1) == Metal.COPPER, "16.7 % and 7.7 % tin are no bronze");
        check(alloy(23, 2) == Metal.POOR_BRONZE && alloy(92, 8) == Metal.POOR_BRONZE && alloy(85, 15) == Metal.POOR_BRONZE, "8 % and 15 % are inside");
        CrucibleContents slag = EMPTY.add(Metal.COPPER, 5).add(Metal.TIN, 1);
        check(slag.alloy() == Metal.COPPER && slag.melted().equals(EMPTY.add(Metal.COPPER, 5)), "slag: the tin is lost");
        CrucibleContents good = EMPTY.add(Metal.COPPER, 9).add(Metal.TIN, 1);
        check(good.melted().equals(EMPTY.add(Metal.BRONZE, 10)) && good.alloy().melt() == 950, "9:1 melts into 10 bronze at 950");
        check(EMPTY.add(Metal.TIN, 2).alloy() == null && EMPTY.add(Metal.COPPER, 3).alloy() == Metal.COPPER, "tin alone / copper alone");
        CrucibleContents washed = EMPTY.add(Metal.COPPER, 4 * CONCENTRATE).add(Metal.TIN, 1);
        check(washed.alloy() == Metal.BRONZE && washed.melted().total() == 9, "4 copper concentrate + 1 crushed tin = 9 bronze");
        check(EMPTY.add(Metal.COPPER, 4 * CONCENTRATE).add(Metal.TIN, CONCENTRATE).alloy() == Metal.COPPER, "4 + 1 concentrate is 20 % tin: slag");
        check(CAPACITY % CONCENTRATE == 0, "a crucible fills up exactly with concentrate");
        check(!EMPTY.add(Metal.BRONZE, 2).canAdd(Metal.COPPER) && !good.canAdd(Metal.BRONZE) && good.canAdd(Metal.TIN), "bronze doesn't mix with ore");
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException("Crucible alloy self-check failed: " + what);
    }
}
