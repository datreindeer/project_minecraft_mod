package com.mechanical;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What melts in a High Furnace and into how much (docs/steel-furnace-plan.md section 5, "Utbyte"): base-metal ore gives
 * twice the crucible's (Crushed Ore 200 mB, Concentrate and Native Copper 400), precious-metal ore 1.5x (Raw Gold 300,
 * Raw Galena 300 Lead), ingots exactly 200 and nuggets 22 (rounded down, so 9 nuggets never make an ingot), blocks 1800.
 * Iron ore (phase 5) is reduced at {@link #IRON_ORE_HEAT}° into Pig Iron, twice the Bloomery's: Raw Iron 100 mB (200 with
 * flux), Roasted Iron Ore 200 (300), bog iron two thirds of that; each ore leaves {@link #SLAG} mB Slag ({@link #SLAG_FLUX}
 * with flux) and burns half a Charcoal out of the Fireboxes. Blister Steel melts into 200 mB Steel (no folding).
 * Which item melts into what and how much is in the metal table ({@link Molten}, scripts/gen_metals.py).
 * Each item melts in {@link #TICKS_PER_100} ticks per 100 mB at its heat, faster when hotter (as the kiln).
 */
public final class HighFurnaceMelting {
    public static final int TICKS_PER_100 = 40, SLAG = 75, SLAG_FLUX = 25;
    /** Iron ore is reduced into Pig Iron at this heat (the metal table's reduce_heat). */
    public static final int IRON_ORE_HEAT = Molten.PIG_IRON.reduceHeat;
    /** Iron ores one Flux serves. */
    public static final int ORES_PER_FLUX = 4;

    /** One item's worth of melt: into what, how much, at what heat, and (iron ore) how much with flux. */
    public record Melt(Molten molten, int mb, int heat, int withFlux) {
        Melt(Molten molten, int mb) {
            this(molten, mb, molten.melt, 0);
        }

        public boolean ironOre() {
            return withFlux > 0;
        }

        public int ticks() {
            return Math.max(1, Math.max(mb, withFlux) * TICKS_PER_100 / 100);
        }
    }

    /** What one item melts into: from the metal table ({@link Molten#melts}, {@link Molten#reduces}), or null. */
    public static @Nullable Melt of(ItemStack s) {
        if (s.isEmpty()) return null;
        for (Molten m : Molten.values()) {
            int mb = m.melts(s);
            if (mb > 0) return new Melt(m, mb);
            int[] ore = m.reduces(s);
            if (ore != null) return new Melt(m, ore[0], m.reduceHeat, ore[1]);
        }
        return null;
    }

    /** The nugget of a melt, or null (knocked out of set metal: 22 mB each). */
    public static @Nullable ItemStack nugget(Molten m) {
        ItemStack n = m.nugget();
        return n.isEmpty() ? null : n;
    }

    /** Flux goes on the furnace's flux count, not in a place. */
    public static boolean isFlux(ItemStack s) {
        return s.is(IronOres.FLUX.get());
    }

    private HighFurnaceMelting() {}
}
