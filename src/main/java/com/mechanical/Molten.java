package com.mechanical;

import org.jspecify.annotations.Nullable;

/**
 * What can lie molten in a High Furnace's pool ({@link MeltPool}, docs/steel-furnace-plan.md section 5). Its own enum,
 * not new {@link Metal} values: Metal is also the METAL state of every Clay Mold, and the plan keeps that state small.
 * The declaration order is the density order, heaviest first: the pool's layers lie in this order from the bottom up,
 * Slag on top. {@link #metal} links the four the bronze-age crucible, channels and clay molds already know.
 * Amounts are in mB: 1 unit = 100 mB, 1 quarter = 25, 1 ingot = 200, 1 nugget 22 (rounded down), 1 block 1800.
 */
public enum Molten {
    GOLD("gold", 1100, 0xFFFFC21E, false, null),
    LEAD("lead", 330, 0xFF8C93A8, false, null),
    SILVER("silver", 1000, 0xFFF4F6FF, false, null),
    COPPER("copper", 1085, 0xFFFF7A2A, false, Metal.COPPER),
    BRONZE("bronze", 950, 0xFFFFB13B, false, Metal.BRONZE),
    POOR_BRONZE("poor_bronze", 950, 0xFFE09A44, false, Metal.POOR_BRONZE),
    PIG_IRON("pig_iron", 1200, 0xFFFFA33A, false, null),
    STEEL("steel", 1500, 0xFFFFF3C4, false, null),
    WROUGHT_IRON("wrought_iron", 1550, 0xFFFFE9A8, false, null),
    TIN("tin", 232, 0xFFE8ECF2, false, Metal.TIN),
    GLASS("glass", 1400, 0xFFB8F0A0, true, null),
    BASALT("basalt", 1250, 0xFFC0402A, true, null),
    OBSIDIAN("obsidian", 1700, 0xFF6A2AA8, true, null),
    SLAG("slag", 1200, 0xFF3A4A2A, true, null);

    public static final int INGOT = 200, NUGGET = 22, BLOCK = 1800, UNIT = 100, QUARTER = 25;

    public final String id;
    /** Degrees it needs to melt (and to stay runny enough to pour). */
    public final int melt;
    /** Its glowing colour, ARGB. */
    public final int color;
    /** Glass, basalt, obsidian and slag: not metals (no ingots, no nuggets). */
    public final boolean nonMetal;
    public final @Nullable Metal metal;

    Molten(String id, int melt, int color, boolean nonMetal, @Nullable Metal metal) {
        this.id = id;
        this.melt = melt;
        this.color = color;
        this.nonMetal = nonMetal;
        this.metal = metal;
    }

    public static @Nullable Molten byId(String id) {
        for (Molten m : values()) if (m.id.equals(id)) return m;
        return null;
    }

    /** The High Furnace melt for a bronze-age metal. */
    public static Molten of(Metal metal) {
        for (Molten m : values()) if (m.metal == metal) return m;
        throw new IllegalArgumentException(metal.getSerializedName());
    }
}
