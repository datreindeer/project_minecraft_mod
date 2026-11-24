package com.mechanical;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * What melts in a High Furnace and into how much (docs/steel-furnace-plan.md section 5, "Utbyte"): base-metal ore gives
 * twice the crucible's (Crushed Ore 200 mB, Concentrate and Native Copper 400), precious-metal ore 1.5x (Raw Gold 300,
 * Raw Galena 300 Lead), ingots exactly 200 and nuggets 22 (rounded down, so 9 nuggets never make an ingot), blocks 1800.
 * Each item melts at its melt's heat in {@link #TICKS_PER_100} ticks per 100 mB, faster when hotter (as the kiln).
 * Iron, steel, glass, basalt and obsidian come in later phases.
 */
public final class HighFurnaceMelting {
    public static final int TICKS_PER_100 = 40;

    /** One item's worth of melt. */
    public record Melt(Molten molten, int mb) {
        public int ticks() {
            return Math.max(1, mb * TICKS_PER_100 / 100);
        }
    }

    public static @Nullable Melt of(ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.is(BronzeOres.CRUSHED_COPPER_ORE.get())) return new Melt(Molten.COPPER, 200);
        if (s.is(BronzeOres.CRUSHED_TIN_ORE.get())) return new Melt(Molten.TIN, 200);
        if (s.is(BronzeOres.COPPER_CONCENTRATE.get()) || s.is(BronzeOres.NATIVE_COPPER.get()) || s.is(Items.RAW_COPPER)) return new Melt(Molten.COPPER, 400);
        if (s.is(BronzeOres.TIN_CONCENTRATE.get())) return new Melt(Molten.TIN, 400);
        if (s.is(Items.COPPER_INGOT)) return new Melt(Molten.COPPER, Molten.INGOT);
        if (s.is(Mechanical.TIN_INGOT.get())) return new Melt(Molten.TIN, Molten.INGOT);
        if (s.is(Bronze.BRONZE_INGOT.get())) return new Melt(Bronze.isPoor(s) ? Molten.POOR_BRONZE : Molten.BRONZE, Molten.INGOT);
        if (s.is(Items.RAW_GOLD)) return new Melt(Molten.GOLD, 300);
        if (s.is(Items.GOLD_INGOT)) return new Melt(Molten.GOLD, Molten.INGOT);
        if (s.is(Items.GOLD_NUGGET)) return new Melt(Molten.GOLD, Molten.NUGGET);
        if (s.is(Items.GOLD_BLOCK)) return new Melt(Molten.GOLD, Molten.BLOCK);
        if (s.is(Items.COPPER_BLOCK.weathering().unaffected())) return new Melt(Molten.COPPER, Molten.BLOCK);
        if (s.is(IronOres.RAW_GALENA.get())) return new Melt(Molten.LEAD, 300);
        if (s.is(IronOres.LEAD_INGOT.get())) return new Melt(Molten.LEAD, Molten.INGOT);
        if (s.is(IronOres.SILVER_INGOT.get())) return new Melt(Molten.SILVER, Molten.INGOT);
        if (s.is(IronOres.SILVER_NUGGET.get())) return new Melt(Molten.SILVER, Molten.NUGGET);
        return null;
    }

    private HighFurnaceMelting() {}
}
